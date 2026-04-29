<#
.SYNOPSIS
  MES MySQL 备份脚本（Windows/PowerShell 版）

.DESCRIPTION
  与 mysql-backup.sh 一致的能力，适配 Windows PowerShell 5.1 / PowerShell 7 环境：
  1. 全量备份（mysqldump --single-transaction --routines --triggers --events --master-data=2）
  2. 可选 binlog 增量同步（-Mode binlog）
  3. 7z/gpg 压缩加密（可选）
  4. 通过 mc.exe 把本地备份异地同步到 MinIO/OSS
  5. 本地保留策略：日备 30 份 / 小时 binlog 7 天 / 周备 12 份 / 月备永久

.PARAMETER Mode
  备份模式：full | binlog | weekly | monthly

.EXAMPLE
  PS> .\mysql-backup.ps1 -Mode full
  PS> .\mysql-backup.ps1 -Mode binlog
  PS> .\mysql-backup.ps1 -Mode weekly

.NOTES
  关键环境变量（.env 或 $env:变量）：
    MYSQL_HOST / MYSQL_PORT / MYSQL_USER / MYSQL_PASS / MYSQL_DB
    BACKUP_ROOT            默认 C:\backup\mysql
    BACKUP_GPG_PASSPHRASE  加密口令（可选）
    MINIO_ENDPOINT / MINIO_AK / MINIO_SK / MINIO_BUCKET
    BACKUP_NOTIFY_WEBHOOK  失败告警 webhook（可选）

  退出码：0 成功 / 10 参数校验失败 / 20 mysqldump 失败 / 30 加密失败 / 40 异地同步失败
#>

[CmdletBinding()]
param(
    [ValidateSet('full','binlog','weekly','monthly')]
    [string]$Mode = 'full'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

# ---------- 1. 加载 .env ----------
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$EnvFile   = Join-Path $ScriptDir '.env'
if (Test-Path $EnvFile) {
    Get-Content $EnvFile | ForEach-Object {
        if ($_ -match '^\s*#') { return }
        if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)\s*$') {
            $name  = $Matches[1]
            $value = $Matches[2].Trim('"').Trim("'")
            [Environment]::SetEnvironmentVariable($name, $value, 'Process')
        }
    }
}

function Get-EnvOrDefault {
    param([string]$Name, [string]$Default = '')
    $v = [Environment]::GetEnvironmentVariable($Name, 'Process')
    if ([string]::IsNullOrEmpty($v)) { return $Default }
    return $v
}

# ---------- 2. 配置参数 ----------
$MysqlHost    = Get-EnvOrDefault 'MYSQL_HOST' '127.0.0.1'
$MysqlPort    = Get-EnvOrDefault 'MYSQL_PORT' '3306'
$MysqlUser    = Get-EnvOrDefault 'MYSQL_USER' 'root'
$MysqlPass    = Get-EnvOrDefault 'MYSQL_PASS' (Get-EnvOrDefault 'MYSQL_PASSWORD' '')
$MysqlDb      = Get-EnvOrDefault 'MYSQL_DB' (Get-EnvOrDefault 'MYSQL_DATABASE' 'mes')

$BackupRoot   = Get-EnvOrDefault 'BACKUP_ROOT' 'C:\backup\mysql'
$GpgPass      = Get-EnvOrDefault 'BACKUP_GPG_PASSPHRASE' ''

$MinioEndpoint = Get-EnvOrDefault 'MINIO_ENDPOINT' 'http://minio:9000'
$MinioAk       = Get-EnvOrDefault 'MINIO_AK' (Get-EnvOrDefault 'MINIO_ROOT_USER' '')
$MinioSk       = Get-EnvOrDefault 'MINIO_SK' (Get-EnvOrDefault 'MINIO_ROOT_PASSWORD' '')
$MinioBucket   = Get-EnvOrDefault 'MINIO_BUCKET' 'mes-backups'

$NotifyWebhook = Get-EnvOrDefault 'BACKUP_NOTIFY_WEBHOOK' ''

# 保留策略
$RetainDaily     = 30
$RetainBinlogDay = 7
$RetainWeekly    = 12
$RetainMonthly   = 0

$LogDir  = Join-Path $BackupRoot 'logs'
$LogFile = Join-Path $LogDir ("backup_" + (Get-Date -Format 'yyyyMMdd') + ".log")
New-Item -ItemType Directory -Force -Path $LogDir | Out-Null

function Write-Log {
    param([string]$Message)
    $line = "[{0}] [{1}] {2}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $Mode, $Message
    Write-Host $line
    Add-Content -Path $LogFile -Value $line -Encoding UTF8
}

function Send-Notify {
    param([string]$Reason)
    Write-Log "告警：$Reason"
    if (-not [string]::IsNullOrEmpty($NotifyWebhook)) {
        try {
            $body = @{
                msgtype = 'text'
                text    = @{ content = "[MES备份告警] mode=$Mode $Reason" }
            } | ConvertTo-Json -Depth 3
            Invoke-RestMethod -Uri $NotifyWebhook -Method Post -Body $body -ContentType 'application/json' -TimeoutSec 10 | Out-Null
        } catch {
            # webhook 失败不影响主流程
            Write-Log "webhook 发送失败：$($_.Exception.Message)"
        }
    }
}

function Assert-Cmd {
    param([string]$Cmd)
    if (-not (Get-Command $Cmd -ErrorAction SilentlyContinue)) {
        Write-Log "命令缺失: $Cmd"
        Send-Notify "依赖缺失 $Cmd"
        exit 10
    }
}

# ---------- 3. 前置校验 ----------
Assert-Cmd 'mysqldump'

if ([string]::IsNullOrEmpty($MysqlPass)) {
    Write-Log 'MYSQL_PASS / MYSQL_PASSWORD 未设置，拒绝执行'
    exit 10
}

foreach ($sub in @('full','binlog','weekly','monthly')) {
    New-Item -ItemType Directory -Force -Path (Join-Path $BackupRoot $sub) | Out-Null
}

# ---------- 4. 工具函数：全量备份 ----------
function Invoke-FullBackup {
    param(
        [string]$TargetDir,
        [string]$Tag
    )
    $ts       = Get-Date -Format 'yyyyMMdd_HHmmss'
    $dumpFile = Join-Path $TargetDir ("{0}_{1}_{2}.sql" -f $MysqlDb, $Tag, $ts)
    $gzFile   = "$dumpFile.gz"

    Write-Log "开始全量备份 -> $gzFile"

    # mysqldump 通过管道直出；Windows 环境使用 .NET GZipStream 压缩避免依赖 gzip.exe
    $dumpArgs = @(
        "--host=$MysqlHost",
        "--port=$MysqlPort",
        "--user=$MysqlUser",
        "--password=$MysqlPass",
        '--single-transaction',
        '--routines',
        '--triggers',
        '--events',
        '--master-data=2',
        '--set-gtid-purged=ON',
        '--default-character-set=utf8mb4',
        $MysqlDb
    )

    try {
        $psi = New-Object System.Diagnostics.ProcessStartInfo
        $psi.FileName               = (Get-Command 'mysqldump').Source
        foreach ($a in $dumpArgs) { [void]$psi.ArgumentList.Add($a) }
        $psi.RedirectStandardOutput = $true
        $psi.RedirectStandardError  = $true
        $psi.UseShellExecute        = $false
        $psi.CreateNoWindow         = $true

        $proc = [System.Diagnostics.Process]::Start($psi)

        $outFs = [System.IO.File]::Create($gzFile)
        $gz    = New-Object System.IO.Compression.GZipStream($outFs, [System.IO.Compression.CompressionLevel]::Optimal)
        $proc.StandardOutput.BaseStream.CopyTo($gz)
        $gz.Close(); $outFs.Close()

        $stderr = $proc.StandardError.ReadToEnd()
        $proc.WaitForExit()
        if ($proc.ExitCode -ne 0) {
            Write-Log "mysqldump 失败 ExitCode=$($proc.ExitCode) stderr=$stderr"
            Send-Notify "mysqldump 失败"
            Remove-Item $gzFile -Force -ErrorAction SilentlyContinue
            exit 20
        }
    } catch {
        Write-Log "mysqldump 异常：$($_.Exception.Message)"
        Send-Notify "mysqldump 异常"
        Remove-Item $gzFile -Force -ErrorAction SilentlyContinue
        exit 20
    }

    $final = $gzFile

    # ---------- 4.1 可选 gpg 对称加密 ----------
    if (-not [string]::IsNullOrEmpty($GpgPass)) {
        Assert-Cmd 'gpg'
        $encFile = "$gzFile.gpg"
        $gpgArgs = @(
            '--batch','--yes','--pinentry-mode','loopback',
            '--cipher-algo','AES256',
            '--passphrase', $GpgPass,
            '--symmetric','--output', $encFile, $gzFile
        )
        & gpg @gpgArgs
        if ($LASTEXITCODE -ne 0) {
            Write-Log 'gpg 加密失败'
            Send-Notify 'gpg 加密失败'
            exit 30
        }
        Remove-Item $gzFile -Force
        $final = $encFile
        Write-Log "加密完成 -> $final"
    } else {
        Write-Log '未设置 BACKUP_GPG_PASSPHRASE，跳过加密（仅建议测试环境）'
    }

    $sizeMB = [Math]::Round((Get-Item $final).Length / 1MB, 2)
    Write-Log "本地备份完成 size=${sizeMB}MB"
    return $final
}

# ---------- 5. 工具函数：binlog 同步 ----------
function Invoke-BinlogSync {
    $target = Join-Path $BackupRoot 'binlog'
    $ts     = Get-Date -Format 'yyyyMMdd_HHmmss'
    $tmpDir = Join-Path $target "tmp_$ts"
    New-Item -ItemType Directory -Force -Path $tmpDir | Out-Null

    Assert-Cmd 'mysql'
    Assert-Cmd 'mysqlbinlog'

    Write-Log '开始 binlog 增量同步'

    # 查询 binlog 列表
    $listArgs = @(
        "--host=$MysqlHost","--port=$MysqlPort",
        "--user=$MysqlUser","--password=$MysqlPass",
        '-Nse','SHOW BINARY LOGS'
    )
    $binlogList = & mysql @listArgs 2>$null
    if ($LASTEXITCODE -ne 0) {
        Write-Log 'SHOW BINARY LOGS 失败，检查 REPLICATION CLIENT 权限'
        Send-Notify 'binlog 查询失败'
        exit 20
    }

    $count = 0
    foreach ($line in $binlogList) {
        if ([string]::IsNullOrWhiteSpace($line)) { continue }
        $bl = ($line -split '\s+')[0]
        if ([string]::IsNullOrWhiteSpace($bl)) { continue }

        $blArgs = @(
            '--read-from-remote-server',
            "--host=$MysqlHost","--port=$MysqlPort",
            "--user=$MysqlUser","--password=$MysqlPass",
            '--raw',
            "--result-file=$tmpDir\",
            $bl
        )
        & mysqlbinlog @blArgs 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { $count++ }
        else { Write-Log "binlog 拉取失败: $bl" }
    }

    # 打包
    $tarball = Join-Path $target "binlog_$ts.zip"
    Compress-Archive -Path (Join-Path $tmpDir '*') -DestinationPath $tarball -Force
    Remove-Item $tmpDir -Recurse -Force -ErrorAction SilentlyContinue
    Write-Log "binlog 同步完成 count=$count -> $tarball"
    return $tarball
}

# ---------- 6. 异地同步到 MinIO ----------
function Invoke-RemoteSync {
    param([string]$LocalFile, [string]$RemotePrefix)

    if ([string]::IsNullOrEmpty($MinioAk) -or [string]::IsNullOrEmpty($MinioSk)) {
        Write-Log '未配置 MINIO_AK/SK，跳过异地同步（仅建议测试环境）'
        return
    }
    Assert-Cmd 'mc'

    $alias = "mes-backup-$PID"
    & mc alias set $alias $MinioEndpoint $MinioAk $MinioSk --quiet | Out-Null

    # bucket 不存在则创建
    & mc ls "$alias/$MinioBucket" 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) {
        Write-Log "bucket $MinioBucket 不存在，创建"
        & mc mb "$alias/$MinioBucket" --ignore-existing | Out-Null
    }

    $remotePath = "$alias/$MinioBucket/$RemotePrefix/" + (Split-Path -Leaf $LocalFile)
    Write-Log "异地同步 -> $remotePath"

    & mc cp --quiet $LocalFile $remotePath
    $rc = $LASTEXITCODE
    & mc alias remove $alias 2>$null | Out-Null
    if ($rc -ne 0) {
        Write-Log 'mc cp 失败'
        Send-Notify "异地同步失败 $LocalFile"
        exit 40
    }
}

# ---------- 7. 本地保留策略 ----------
function Invoke-LocalCleanup {
    param([string]$Dir, [int]$RetentionDays)
    if ($RetentionDays -le 0) { return }

    Write-Log "清理 $Dir 中 $RetentionDays 天前的文件"
    $cutoff = (Get-Date).AddDays(-$RetentionDays)
    Get-ChildItem -Path $Dir -File -ErrorAction SilentlyContinue `
      | Where-Object { $_.LastWriteTime -lt $cutoff -and $_.Extension -in @('.gz','.gpg','.zip') } `
      | ForEach-Object {
            Write-Log "  removed: $($_.FullName)"
            Remove-Item $_.FullName -Force -ErrorAction SilentlyContinue
        }
}

# ---------- 8. 主流程 ----------
switch ($Mode) {
    'full' {
        $file = Invoke-FullBackup -TargetDir (Join-Path $BackupRoot 'full') -Tag 'daily'
        Invoke-RemoteSync -LocalFile $file -RemotePrefix 'daily'
        Invoke-LocalCleanup -Dir (Join-Path $BackupRoot 'full') -RetentionDays $RetainDaily
    }
    'binlog' {
        $file = Invoke-BinlogSync
        Invoke-RemoteSync -LocalFile $file -RemotePrefix 'binlog'
        Invoke-LocalCleanup -Dir (Join-Path $BackupRoot 'binlog') -RetentionDays $RetainBinlogDay
    }
    'weekly' {
        $file = Invoke-FullBackup -TargetDir (Join-Path $BackupRoot 'weekly') -Tag 'weekly'
        Invoke-RemoteSync -LocalFile $file -RemotePrefix 'weekly'
        # 按份数保留
        $weeklyDir = Join-Path $BackupRoot 'weekly'
        $items = Get-ChildItem -Path $weeklyDir -File | Sort-Object LastWriteTime -Descending
        if ($items.Count -gt $RetainWeekly) {
            $items | Select-Object -Skip $RetainWeekly | ForEach-Object {
                Write-Log "  removed: $($_.FullName)"
                Remove-Item $_.FullName -Force
            }
        }
    }
    'monthly' {
        $file = Invoke-FullBackup -TargetDir (Join-Path $BackupRoot 'monthly') -Tag 'monthly'
        Invoke-RemoteSync -LocalFile $file -RemotePrefix 'monthly'
        Invoke-LocalCleanup -Dir (Join-Path $BackupRoot 'monthly') -RetentionDays $RetainMonthly
    }
    default {
        Write-Log "未知 Mode=$Mode"
        exit 1
    }
}

Write-Log '备份流程完成'
exit 0

param(
    [int]$Concurrency = 10,
    [int]$TotalRequests = 100,
    [string]$BaseUrl = "http://localhost:8080"
)

Write-Host "============================================"
Write-Host "  MES Concurrency Benchmark"
Write-Host "  Concurrency: $Concurrency"
Write-Host "  Total Requests: $TotalRequests"
Write-Host "  Target: $BaseUrl"
Write-Host "============================================"

# Step 1: Get auth token
Write-Host "`n[1/4] Authenticating..."
$loginBody = '{"username":"admin","password":"admin123"}'
try {
    $loginResp = Invoke-RestMethod -Uri "$BaseUrl/api/auth/login" -Method POST -Body $loginBody -ContentType "application/json" -TimeoutSec 30
    $token = $loginResp.data.accessToken
    Write-Host "  Token acquired successfully"
} catch {
    Write-Host "  FAILED: $($_.Exception.Message)"
    exit 1
}

$headers = @{ "Authorization" = "Bearer $token" }

# Step 2: Benchmark login endpoint
Write-Host "`n[2/4] Benchmarking POST /api/auth/login ($TotalRequests requests, $Concurrency concurrent)..."
$loginResults = @()
$loginBlock = {
    param($url, $body, $reqId)
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $null = Invoke-WebRequest -Uri $url -Method POST -Body $body -ContentType "application/json" -TimeoutSec 30
        $sw.Stop()
        return @{ Id = $reqId; Status = 200; Time = $sw.ElapsedMilliseconds; Error = $null }
    } catch {
        $sw.Stop()
        $sc = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { 0 }
        return @{ Id = $reqId; Status = $sc; Time = $sw.ElapsedMilliseconds; Error = $_.Exception.Message }
    }
}

$jobs = @()
$batchSize = [Math]::Ceiling($TotalRequests / $Concurrency)
$sw = [System.Diagnostics.Stopwatch]::StartNew()

for ($i = 0; $i -lt $TotalRequests; $i++) {
    while (@($jobs | Where-Object { $_.State -eq 'Running' }).Count -ge $Concurrency) {
        Start-Sleep -Milliseconds 50
    }
    $jobs += Start-Job -ScriptBlock $loginBlock -ArgumentList "$BaseUrl/api/auth/login", $loginBody, $i
}

$loginResults = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job -Force
$sw.Stop()
$loginElapsed = $sw.ElapsedMilliseconds

$loginSuccess = @($loginResults | Where-Object { $_.Status -eq 200 }).Count
$loginFail = $TotalRequests - $loginSuccess
$loginTimes = @($loginResults | Where-Object { $_.Status -eq 200 } | ForEach-Object { $_.Time })
if ($loginTimes.Count -gt 0) {
    $loginAvg = [Math]::Round(($loginTimes | Measure-Object -Average).Average, 1)
    $loginMin = ($loginTimes | Measure-Object -Minimum).Minimum
    $loginMax = ($loginTimes | Measure-Object -Maximum).Maximum
    $loginSorted = $loginTimes | Sort-Object
    $loginP50 = $loginSorted[[Math]::Floor($loginSorted.Count * 0.5)]
    $loginP95 = $loginSorted[[Math]::Floor($loginSorted.Count * 0.95)]
    $loginP99 = $loginSorted[[Math]::Floor($loginSorted.Count * 0.99)]
    $loginRps = [Math]::Round($TotalRequests / ($loginElapsed / 1000), 1)
} else {
    $loginAvg = 0; $loginMin = 0; $loginMax = 0; $loginP50 = 0; $loginP95 = 0; $loginP99 = 0; $loginRps = 0
}

Write-Host "  Success: $loginSuccess / $TotalRequests  |  Failed: $loginFail"
Write-Host "  RPS: $loginRps"
Write-Host "  Avg: ${loginAvg}ms  |  Min: ${loginMin}ms  |  Max: ${loginMax}ms"
Write-Host "  P50: ${loginP50}ms  |  P95: ${loginP95}ms  |  P99: ${loginP99}ms"

# Step 3: Benchmark Gateway health endpoint (lightweight)
Write-Host "`n[3/4] Benchmarking GET /actuator/health ($TotalRequests requests, $Concurrency concurrent)..."
$healthBlock = {
    param($url, $reqId)
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $null = Invoke-WebRequest -Uri $url -TimeoutSec 10
        $sw.Stop()
        return @{ Id = $reqId; Status = 200; Time = $sw.ElapsedMilliseconds; Error = $null }
    } catch {
        $sw.Stop()
        $sc = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { 0 }
        return @{ Id = $reqId; Status = $sc; Time = $sw.ElapsedMilliseconds; Error = $_.Exception.Message }
    }
}

$jobs2 = @()
$sw2 = [System.Diagnostics.Stopwatch]::StartNew()
for ($i = 0; $i -lt $TotalRequests; $i++) {
    while (@($jobs2 | Where-Object { $_.State -eq 'Running' }).Count -ge $Concurrency) {
        Start-Sleep -Milliseconds 50
    }
    $jobs2 += Start-Job -ScriptBlock $healthBlock -ArgumentList "$BaseUrl/actuator/health", $i
}
$healthResults = $jobs2 | Wait-Job | Receive-Job
$jobs2 | Remove-Job -Force
$sw2.Stop()
$healthElapsed = $sw2.ElapsedMilliseconds

$healthSuccess = @($healthResults | Where-Object { $_.Status -eq 200 }).Count
$healthFail = $TotalRequests - $healthSuccess
$healthTimes = @($healthResults | Where-Object { $_.Status -eq 200 } | ForEach-Object { $_.Time })
if ($healthTimes.Count -gt 0) {
    $healthAvg = [Math]::Round(($healthTimes | Measure-Object -Average).Average, 1)
    $healthMin = ($healthTimes | Measure-Object -Minimum).Minimum
    $healthMax = ($healthTimes | Measure-Object -Maximum).Maximum
    $healthSorted = $healthTimes | Sort-Object
    $healthP50 = $healthSorted[[Math]::Floor($healthSorted.Count * 0.5)]
    $healthP95 = $healthSorted[[Math]::Floor($healthSorted.Count * 0.95)]
    $healthP99 = $healthSorted[[Math]::Floor($healthSorted.Count * 0.99)]
    $healthRps = [Math]::Round($TotalRequests / ($healthElapsed / 1000), 1)
} else {
    $healthAvg = 0; $healthMin = 0; $healthMax = 0; $healthP50 = 0; $healthP95 = 0; $healthP99 = 0; $healthRps = 0
}

Write-Host "  Success: $healthSuccess / $TotalRequests  |  Failed: $healthFail"
Write-Host "  RPS: $healthRps"
Write-Host "  Avg: ${healthAvg}ms  |  Min: ${healthMin}ms  |  Max: ${healthMax}ms"
Write-Host "  P50: ${healthP50}ms  |  P95: ${healthP95}ms  |  P99: ${healthP99}ms"

# Step 4: Benchmark Frontend static
Write-Host "`n[4/4] Benchmarking GET http://localhost:80/ (Frontend, $TotalRequests requests, $Concurrency concurrent)..."
$feBlock = {
    param($url, $reqId)
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    try {
        $null = Invoke-WebRequest -Uri $url -TimeoutSec 10
        $sw.Stop()
        return @{ Id = $reqId; Status = 200; Time = $sw.ElapsedMilliseconds; Error = $null }
    } catch {
        $sw.Stop()
        $sc = if ($_.Exception.Response) { $_.Exception.Response.StatusCode.value__ } else { 0 }
        return @{ Id = $reqId; Status = $sc; Time = $sw.ElapsedMilliseconds; Error = $_.Exception.Message }
    }
}

$jobs3 = @()
$sw3 = [System.Diagnostics.Stopwatch]::StartNew()
for ($i = 0; $i -lt $TotalRequests; $i++) {
    while (@($jobs3 | Where-Object { $_.State -eq 'Running' }).Count -ge $Concurrency) {
        Start-Sleep -Milliseconds 50
    }
    $jobs3 += Start-Job -ScriptBlock $feBlock -ArgumentList "http://localhost:80/", $i
}
$feResults = $jobs3 | Wait-Job | Receive-Job
$jobs3 | Remove-Job -Force
$sw3.Stop()
$feElapsed = $sw3.ElapsedMilliseconds

$feSuccess = @($feResults | Where-Object { $_.Status -eq 200 }).Count
$feFail = $TotalRequests - $feSuccess
$feTimes = @($feResults | Where-Object { $_.Status -eq 200 } | ForEach-Object { $_.Time })
if ($feTimes.Count -gt 0) {
    $feAvg = [Math]::Round(($feTimes | Measure-Object -Average).Average, 1)
    $feMin = ($feTimes | Measure-Object -Minimum).Minimum
    $feMax = ($feTimes | Measure-Object -Maximum).Maximum
    $feSorted = $feTimes | Sort-Object
    $feP50 = $feSorted[[Math]::Floor($feSorted.Count * 0.5)]
    $feP95 = $feSorted[[Math]::Floor($feSorted.Count * 0.95)]
    $feP99 = $feSorted[[Math]::Floor($feSorted.Count * 0.99)]
    $feRps = [Math]::Round($TotalRequests / ($feElapsed / 1000), 1)
} else {
    $feAvg = 0; $feMin = 0; $feMax = 0; $feP50 = 0; $feP95 = 0; $feP99 = 0; $feRps = 0
}

Write-Host "  Success: $feSuccess / $TotalRequests  |  Failed: $feFail"
Write-Host "  RPS: $feRps"
Write-Host "  Avg: ${feAvg}ms  |  Min: ${feMin}ms  |  Max: ${feMax}ms"
Write-Host "  P50: ${feP50}ms  |  P95: ${feP95}ms  |  P99: ${feP99}ms"

Write-Host "`n============================================"
Write-Host "  Benchmark Complete!"
Write-Host "============================================"
Write-Host "Summary:"
Write-Host "  Login API:      RPS=$loginRps  P95=${loginP95}ms  Success=$loginSuccess/$TotalRequests"
Write-Host "  Gateway Health: RPS=$healthRps  P95=${healthP95}ms  Success=$healthSuccess/$TotalRequests"
Write-Host "  Frontend:       RPS=$feRps  P95=${feP95}ms  Success=$feSuccess/$TotalRequests"

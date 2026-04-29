# M8 SQL 迁移一致性验证脚本
# 按 Flyway 顺序（V1.00 → V2.05 → R__seed_test_data）跑 SQL，统计耗时、行数、状态
$dbName = "mes_m8_test"
$dockerContainer = "titan-mysql"
$rootPwd = "12345678"
$logPath = "C:\Users\zkyd\Desktop\mes\mes\docs\test-reports\m8-sql-run.log"

# 清空日志
"" | Out-File -FilePath $logPath -Encoding utf8

$scripts = @(
    "V1.00__basic_data.sql",
    "V1.01__team_management.sql",
    "V1.02__process_management.sql",
    "V1.03__plan_management.sql",
    "V1.04__work_order.sql",
    "V1.05__dispatch.sql",
    "V1.06__abnormal_contact.sql",
    "V1.07__quality_management.sql",
    "V1.08__work_query.sql",
    "V1.09__material_management.sql",
    "V1.10__aps_integration.sql",
    "V1.11__auth_rbac.sql",
    "V1.12__add_missing_deleted_columns.sql",
    "V1.13__aps_callback_config.sql",
    "V1.14__tenant_portal.sql",
    "V1.15__aps_extended_integration.sql",
    "V1.16__add_tenant_id_to_all_business_tables.sql",
    "V1.17__fix_unique_indexes_and_missing_tenant.sql",
    "V1.18__production_hardening.sql",
    "V1.19__dispatch_task_extension.sql",
    "V1.20__must_change_password.sql",
    "V2.01__tenant_platform_fields.sql",
    "V2.02__tenantize_rbac.sql",
    "V2.03__tenant_lifecycle_tables.sql",
    "V2.04__db_defense_in_depth.sql",
    "V2.05__menu_permissions.sql",
    "R__seed_test_data.sql"
)

$results = @()
foreach ($script in $scripts) {
    $start = Get-Date
    $marker = "===== SCRIPT: $script ====="
    $marker | Out-File -FilePath $logPath -Append -Encoding utf8
    # 执行 mysql，STDIN 重定向文件进去；捕获 exit code 与 stderr
    $output = docker exec $dockerContainer sh -c "mysql -uroot -p$rootPwd $dbName < /tmp/m8_sql/$script 2>&1"
    $exit = $LASTEXITCODE
    $end = Get-Date
    $elapsed = [int]($end - $start).TotalMilliseconds
    $status = if ($exit -eq 0) { "SUCCESS" } else { "FAILED" }
    $output | Out-File -FilePath $logPath -Append -Encoding utf8
    $results += [PSCustomObject]@{
        script = $script
        status = $status
        exitCode = $exit
        elapsedMs = $elapsed
    }
    Write-Host ("[{0}] {1}  {2}ms" -f $status, $script, $elapsed)
    if ($exit -ne 0) {
        Write-Host "!!! 失败，停止后续执行 !!!" -ForegroundColor Red
        break
    }
}

Write-Host ""
Write-Host "=== 汇总 ==="
$results | Format-Table -AutoSize
$results | Export-Csv -NoTypeInformation -Encoding utf8 -Path "C:\Users\zkyd\Desktop\mes\mes\docs\test-reports\m8-sql-summary.csv"

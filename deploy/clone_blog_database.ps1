param(
  [string]$SourceDb = "blog_cloud_test",
  [string]$TargetDb = "blog_cloud_test_copy",
  [string]$User = "root",
  [string]$Password = "032581",
  [string]$MySqlExe = "E:\MySql\MySQL Server 8.0\bin\mysql.exe",
  [string]$MySqlDumpExe = "E:\MySql\MySQL Server 8.0\bin\mysqldump.exe",
  [switch]$IncludeData
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $MySqlExe)) {
  throw "mysql.exe 未找到: $MySqlExe"
}

if (-not (Test-Path $MySqlDumpExe)) {
  throw "mysqldump.exe 未找到: $MySqlDumpExe"
}

$createDbSql = "CREATE DATABASE IF NOT EXISTS ``$TargetDb`` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
& $MySqlExe "--default-character-set=utf8mb4" "-u$User" "-p$Password" -e $createDbSql

$dumpArgs = @("--default-character-set=utf8mb4", "-u$User", "-p$Password", "--routines", "--triggers")
if (-not $IncludeData) {
  $dumpArgs += "--no-data"
}
$dumpArgs += $SourceDb

$dumpContent = & $MySqlDumpExe @dumpArgs
if ($LASTEXITCODE -ne 0) {
  throw "导出数据库失败"
}

$dumpContent | & $MySqlExe "--default-character-set=utf8mb4" "-u$User" "-p$Password" $TargetDb
if ($LASTEXITCODE -ne 0) {
  throw "导入数据库失败"
}

Write-Host "数据库复制完成: $SourceDb -> $TargetDb"
Write-Host "包含数据: $($IncludeData.IsPresent)"

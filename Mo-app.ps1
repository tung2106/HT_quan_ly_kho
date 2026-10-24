$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath $PSScriptRoot

# Nếu web đã chạy thì chỉ mở trình duyệt, không chạy thêm tiến trình.
try {
    $response = Invoke-WebRequest 'http://127.0.0.1:18080/login' -UseBasicParsing -TimeoutSec 3
    if ($response.StatusCode -eq 200) {
        Start-Process 'http://127.0.0.1:18080/'
        Write-Host 'App dang chay. Da mo trinh duyet.'
        exit 0
    }
} catch {}

$pgCtl = Join-Path $PSScriptRoot '.tools/pg-runtime/pgsql/bin/pg_ctl.exe'
& $pgCtl -D '.tools/pg-test' status
if ($LASTEXITCODE -ne 0) {
    & $pgCtl -D '.tools/pg-test' -l '.tools/pg-startup.log' -o '-p 55432 -h 127.0.0.1' start
    if ($LASTEXITCODE -ne 0) { throw 'Khong khoi dong duoc co so du lieu.' }
}
$env:JAVA_HOME = (Resolve-Path '.tools/jdk-17.0.20.1+1').Path
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://127.0.0.1:55432/vatlieu_test'
$env:SPRING_DATASOURCE_USERNAME = 'vatlieu'
$env:SPRING_DATASOURCE_PASSWORD = [IO.File]::ReadAllText((Join-Path $PSScriptRoot '.tools/db-password'))
$env:SERVER_ADDRESS = '0.0.0.0'
$env:SERVER_PORT = '18080'
Write-Host 'Giu cua so nay mo khi su dung app.'
Write-Host 'Cho dong Started VatlieuApplication, roi mo http://127.0.0.1:18080/'
Write-Host 'Dien thoai cung Wi-Fi dung dia chi IPv4 cua may tinh va cong :18080.'
& '.\.tools\apache-maven-3.9.9\bin\mvn.cmd' -o -B '-Dmaven.repo.local=.m2/repository' spring-boot:run

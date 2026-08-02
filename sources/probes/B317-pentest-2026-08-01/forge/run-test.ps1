$ErrorActionPreference = 'Continue'
$certDir = 'C:\Niagara\iC-Niagara-4.10.9.14\security\certificates'
$licDir  = 'C:\Niagara\iC-Niagara-4.10.9.14\security\licenses'
$staging = 'C:\Users\ASUS\pentest-staging'
$stamp   = Get-Date -Format 'yyyyMMdd-HHmmss'
$backup  = "C:\Users\ASUS\pentest-backup-$stamp"
New-Item -ItemType Directory -Path $backup -Force | Out-Null
Copy-Item "$certDir\Tridium.certificate" $backup -Force
Copy-Item "$licDir\db" $backup -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item "$licDir\inbox" $backup -Recurse -Force -ErrorAction SilentlyContinue
Write-Output ('RES backup=' + $backup)
Copy-Item "$staging\PentestVendor.certificate" $certDir -Force
Copy-Item "$staging\PentestVendor.license" "$licDir\" -Force
Write-Output ('RES planted-cert=' + (Test-Path "$certDir\PentestVendor.certificate"))
Write-Output ('RES planted-lic=' + (Test-Path "$licDir\PentestVendor.license"))
$out = & 'C:\Niagara\iC-Niagara-4.10.9.14\bin\nre.exe' -licenses 2>&1 | Out-String
Write-Output 'RES --- nre -licenses with FORGED cert+license ---'
$out -split "`n" | Where-Object { $_ -match 'License|Certificate|Feature|HostId|Pentest|invalid|valid|none|WARN|ERROR|ADVERT' } | ForEach-Object { Write-Output ('RES ' + $_) }
# RESTORE: remove planted, restore originals
Remove-Item "$certDir\PentestVendor.certificate" -Force -ErrorAction SilentlyContinue
Remove-Item "$licDir\PentestVendor.license" -Force -ErrorAction SilentlyContinue
Write-Output ('RES restored-cert-absent=' + (-not (Test-Path "$certDir\PentestVendor.certificate")))
Write-Output ('RES restored-lic-absent=' + (-not (Test-Path "$licDir\PentestVendor.license")))
Write-Output ('RES certDir-entries=' + ((Get-ChildItem $certDir -File).Name -join ','))
Write-Output ('RES licRoot-entries=' + ((Get-ChildItem $licDir -File).Name -join ','))

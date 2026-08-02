$ErrorActionPreference = 'Continue'
$certDir = 'C:\Niagara\iC-Niagara-4.10.9.14\security\certificates'
$licDir  = 'C:\Niagara\iC-Niagara-4.10.9.14\security\licenses'
Write-Output ('RES certDir=' + ((Get-ChildItem $certDir -File).Name -join ','))
Write-Output ('RES licRoot=' + ((Get-ChildItem $licDir -File).Name -join ','))
Write-Output ('RES dbTree=' + ((Get-ChildItem "$licDir\db" -Recurse -File -ErrorAction SilentlyContinue).FullName.Replace($licDir,'') -join ','))
Write-Output ('RES inbox=' + ((Get-ChildItem "$licDir\inbox" -Force -ErrorAction SilentlyContinue).Name -join ','))
$h = Get-FileHash "$certDir\Tridium.certificate" -Algorithm SHA256
Write-Output ('RES tcert-sha256=' + $h.Hash)
# remove our staging + backup dirs from the machine
Remove-Item 'C:\Users\ASUS\pentest-staging' -Recurse -Force -ErrorAction SilentlyContinue
Get-ChildItem 'C:\Users\ASUS' -Directory -Filter 'pentest-backup-*' | ForEach-Object { Remove-Item $_.FullName -Recurse -Force }
Write-Output ('RES staging-gone=' + (-not (Test-Path 'C:\Users\ASUS\pentest-staging')))
Write-Output ('RES backups-gone=' + ((Get-ChildItem 'C:\Users\ASUS' -Directory -Filter 'pentest-backup-*').Count -eq 0))
# final oracle: licensing state back to baseline
$out = & 'C:\Niagara\iC-Niagara-4.10.9.14\bin\nre.exe' -licenses 2>&1 | Out-String
$out -split "`n" | Where-Object { $_ -match 'HostId|License|Feature|Certificate|valid|invalid|none' } | ForEach-Object { Write-Output ('RES ' + $_) }

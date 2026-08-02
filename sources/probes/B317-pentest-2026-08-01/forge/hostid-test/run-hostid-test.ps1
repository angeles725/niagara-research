$ErrorActionPreference = 'Continue'
$certDir = 'C:\Niagara\iC-Niagara-4.10.9.14\security\certificates'
$licDir  = 'C:\Niagara\iC-Niagara-4.10.9.14\security\licenses'
$staging = 'C:\Users\ASUS\pentest-staging'
$backup  = 'C:\Users\ASUS\pentest-backup-hostid'
New-Item -ItemType Directory -Path $backup -Force | Out-Null
Copy-Item "$certDir\Tridium.certificate" $backup -Force
Copy-Item "$licDir\db" $backup -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item "$licDir\inbox" $backup -Recurse -Force -ErrorAction SilentlyContinue
Write-Output 'RES === T-A: ORIGINAL license (valid sig, other hostId) ==='
Copy-Item "$staging\Honeywell-ORIGINAL.license" "$licDir\" -Force
Copy-Item "$staging\Honeywell.certificate" "$certDir\" -Force
$out = & 'C:\Niagara\iC-Niagara-4.10.9.14\bin\nre.exe' -licenses 2>&1 | Out-String
$out -split "`n" | Where-Object { $_ -match 'Honeywell|invalid|valid|HostId|Licenses|moved|ADVERT' } | Select-Object -First 10 | ForEach-Object { Write-Output ('RES-A ' + $_) }
Remove-Item "$licDir\Honeywell-ORIGINAL.license" -Force -ErrorAction SilentlyContinue
Write-Output 'RES === T-B: EDITED hostId license (same sig, other hostId->this host) ==='
Copy-Item "$staging\Honeywell-EDITED-HOSTID.license" "$licDir\" -Force
$out2 = & 'C:\Niagara\iC-Niagara-4.10.9.14\bin\nre.exe' -licenses 2>&1 | Out-String
$out2 -split "`n" | Where-Object { $_ -match 'Honeywell|invalid|valid|HostId|Licenses|moved|ADVERT|signature' } | Select-Object -First 10 | ForEach-Object { Write-Output ('RES-B ' + $_) }
Remove-Item "$licDir\Honeywell-EDITED-HOSTID.license" -Force -ErrorAction SilentlyContinue
Get-ChildItem "$licDir\db" -Recurse -Filter '*.license' -File -ErrorAction SilentlyContinue | Remove-Item -Force
Get-ChildItem "$licDir\db" -Directory -ErrorAction SilentlyContinue | ForEach-Object {
  if ((Get-ChildItem $_.FullName -Force -ErrorAction SilentlyContinue | Measure-Object).Count -eq 0) { Remove-Item $_.FullName -Force }
}
Remove-Item "$certDir\Honeywell.certificate" -Force -ErrorAction SilentlyContinue
Write-Output ('RES restored-cert=' + ((Get-ChildItem $certDir -File).Name -join ','))
Write-Output ('RES restored-lic=' + ((Get-ChildItem $licDir -File).Name -join ','))

$ErrorActionPreference = 'Continue'
$licDir  = 'C:\Niagara\iC-Niagara-4.10.9.14\security\licenses'
$certDir = 'C:\Niagara\iC-Niagara-4.10.9.14\security\certificates'
$staging = 'C:\Users\ASUS\pentest-staging'
New-Item -ItemType Directory -Path $staging -Force | Out-Null
Copy-Item "$staging\Honeywell.certificate" "$certDir\" -Force
Copy-Item "$staging\Honeywell-REAL-SIG-EDITED.license" "$licDir\" -Force
$out = & 'C:\Niagara\iC-Niagara-4.10.9.14\bin\nre.exe' -licenses 2>&1 | Out-String
$out -split "`n" | Where-Object { $_ -match 'REAL-SIG|invalid|valid|Features|moved|ADVERT|Signature|decoding' } | ForEach-Object { Write-Output ('RES ' + $_) }
Remove-Item "$licDir\Honeywell-REAL-SIG-EDITED.license" -Force -ErrorAction SilentlyContinue
Get-ChildItem "$licDir\db" -Recurse -Filter '*.license' -File -ErrorAction SilentlyContinue | Remove-Item -Force
Get-ChildItem "$licDir\db" -Directory -ErrorAction SilentlyContinue | ForEach-Object {
  if ((Get-ChildItem $_.FullName -Force -ErrorAction SilentlyContinue | Measure-Object).Count -eq 0) { Remove-Item $_.FullName -Force }
}
Remove-Item "$certDir\Honeywell.certificate" -Force -ErrorAction SilentlyContinue
Write-Output ('RES restored-cert=' + ((Get-ChildItem $certDir -File).Name -join ','))

$ErrorActionPreference = 'Continue'
$licDir  = 'C:\Niagara\iC-Niagara-4.10.9.14\security\licenses'
$certDir = 'C:\Niagara\iC-Niagara-4.10.9.14\security\certificates'
$staging = 'C:\Users\ASUS\pentest-staging'
New-Item -ItemType Directory -Path $staging -Force | Out-Null
Copy-Item "$staging\Honeywell.certificate" "$certDir\" -Force
foreach ($n in @('TEST3-v1-minimal','TEST3-v2-fixed20','TEST3-v3-pad21')) {
  Copy-Item "$staging\$n.license" "$licDir\" -Force
}
$out = & 'C:\Niagara\iC-Niagara-4.10.9.14\bin\nre.exe' -licenses 2>&1 | Out-String
$out -split "`n" | Where-Object { $_ -match 'TEST3-|invalid|valid|Features|moved|ADVERT|Signature|signature|decoding|Invalid' } | ForEach-Object { Write-Output ('RES ' + $_) }
foreach ($n in @('TEST3-v1-minimal','TEST3-v2-fixed20','TEST3-v3-pad21')) { Remove-Item "$licDir\$n.license" -Force -ErrorAction SilentlyContinue }
Get-ChildItem "$licDir\db" -Recurse -Filter '*.license' -File -ErrorAction SilentlyContinue | Remove-Item -Force
Get-ChildItem "$licDir\db" -Directory -ErrorAction SilentlyContinue | ForEach-Object {
  if ((Get-ChildItem $_.FullName -Force -ErrorAction SilentlyContinue | Measure-Object).Count -eq 0) { Remove-Item $_.FullName -Force }
}
Remove-Item "$certDir\Honeywell.certificate" -Force -ErrorAction SilentlyContinue
Write-Output ('RES restored-cert=' + ((Get-ChildItem $certDir -File).Name -join ','))
Write-Output ('RES restored-lic=' + ((Get-ChildItem $licDir -File).Name -join ','))

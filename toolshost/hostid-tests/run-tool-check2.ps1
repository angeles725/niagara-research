$ErrorActionPreference = 'Continue'
$licDir  = 'C:\Niagara\iC-Niagara-4.10.9.14\security\licenses'
$certDir = 'C:\Niagara\iC-Niagara-4.10.9.14\security\certificates'
$staging = 'C:\Users\ASUS\pentest-staging'
New-Item -ItemType Directory -Path $staging -Force | Out-Null
Copy-Item "$staging\Honeywell-SELF.license" "$licDir\" -Force
Copy-Item "$staging\Honeywell.certificate" "$certDir\" -Force
$out = & 'C:\Niagara\iC-Niagara-4.10.9.14\bin\nre.exe' -licenses 2>&1 | Out-String
$out -split "`n" | Where-Object { $_ -match 'Honeywell|invalid|valid|HostId|Licenses|moved|ADVERT|signature|Signatur' } | Select-Object -First 10 | ForEach-Object { Write-Output ('RES ' + $_) }
Remove-Item "$licDir\Honeywell-SELF.license" -Force -ErrorAction SilentlyContinue
Get-ChildItem "$licDir\db" -Recurse -Filter '*.license' -File -ErrorAction SilentlyContinue | Remove-Item -Force
Get-ChildItem "$licDir\db" -Directory -ErrorAction SilentlyContinue | ForEach-Object {
  if ((Get-ChildItem $_.FullName -Force -ErrorAction SilentlyContinue | Measure-Object).Count -eq 0) { Remove-Item $_.FullName -Force }
}
Remove-Item "$certDir\Honeywell.certificate" -Force -ErrorAction SilentlyContinue
Write-Output ('RES restored-cert=' + ((Get-ChildItem $certDir -File).Name -join ','))
Write-Output ('RES restored-lic=' + ((Get-ChildItem $licDir -File).Name -join ','))

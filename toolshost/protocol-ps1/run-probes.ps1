$ErrorActionPreference = 'Continue'
$certDir = 'C:\Niagara\iC-Niagara-4.10.9.14\security\certificates'
$licDir  = 'C:\Niagara\iC-Niagara-4.10.9.14\security\licenses'
$staging = 'C:\Users\ASUS\pentest-staging'
$backup  = "C:\Users\ASUS\pentest-backup2"
New-Item -ItemType Directory -Path $backup -Force | Out-Null
Copy-Item "$certDir\Tridium.certificate" $backup -Force
foreach ($n in @('probe-hostid','probe-generated','probe-expired','probe-nosig','probe-badsig')) {
  Copy-Item "$staging\$n.license" "$licDir\" -Force
}
Write-Output 'RES planted 5 probe licenses'
$out = & 'C:\Niagara\iC-Niagara-4.10.9.14\bin\nre.exe' -licenses 2>&1 | Out-String
$out -split "`n" | Where-Object { $_ -match 'probe|invalid|valid|License|Feature|HostId|ADVERT|WARN' } | ForEach-Object { Write-Output ('RES ' + $_) }
foreach ($n in @('probe-hostid','probe-generated','probe-expired','probe-nosig','probe-badsig')) {
  Remove-Item "$licDir\$n.license" -Force -ErrorAction SilentlyContinue
}
# also clean any canonicalized copies in db
Get-ChildItem "$licDir\db" -Recurse -Filter 'probe-*.license' -ErrorAction SilentlyContinue | Remove-Item -Force
Write-Output ('RES cleaned=' + ((Get-ChildItem $licDir -Recurse -Filter 'probe-*').Count -eq 0))
Write-Output ('RES licRoot-entries=' + ((Get-ChildItem $licDir -File).Name -join ','))

$ErrorActionPreference = 'Continue'
$licDir  = 'C:\Niagara\iC-Niagara-4.10.9.14\security\licenses'
$staging = 'C:\Users\ASUS\pentest-staging'
$backup  = "C:\Users\ASUS\pentest-backup2"
New-Item -ItemType Directory -Path $backup -Force | Out-Null
Copy-Item "$licDir\db" $backup -Recurse -Force -ErrorAction SilentlyContinue
foreach ($n in @('probe2-hostid','probe2-generated','probe2-expired','probe2-nosig','probe2-badsig','probe2-goodshape')) {
  Copy-Item "$staging\$n.license" "$licDir\" -Force
}
$out = & 'C:\Niagara\iC-Niagara-4.10.9.14\bin\nre.exe' -licenses 2>&1 | Out-String
$out -split "`n" | Where-Object { $_ -match 'probe2|invalid|valid|moved|License|Feature|HostId|ADVERT|WARN' } | ForEach-Object { Write-Output ('RES ' + $_) }
foreach ($n in @('probe2-hostid','probe2-generated','probe2-expired','probe2-nosig','probe2-badsig','probe2-goodshape')) {
  Remove-Item "$licDir\$n.license" -Force -ErrorAction SilentlyContinue
}
Get-ChildItem "$licDir\db" -Recurse -Filter 'probe2-*.license' -ErrorAction SilentlyContinue | Remove-Item -Force
Write-Output ('RES cleaned=' + ((Get-ChildItem $licDir -Recurse -Filter 'probe2-*').Count -eq 0))
Write-Output ('RES licRoot-entries=' + ((Get-ChildItem $licDir -File).Name -join ','))

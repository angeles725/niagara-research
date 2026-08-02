$ErrorActionPreference = 'Continue'
$licDir  = 'C:\Niagara\iC-Niagara-4.10.9.14\security\licenses'
$staging = 'C:\Users\ASUS\pentest-staging'
foreach ($n in @('probe3-padded20','probe3-plainder')) {
  Copy-Item "$staging\$n.license" "$licDir\" -Force
}
$out = & 'C:\Niagara\iC-Niagara-4.10.9.14\bin\nre.exe' -licenses 2>&1 | Out-String
$out -split "`n" | Where-Object { $_ -match 'probe3|invalid|valid|moved|Feature' } | ForEach-Object { Write-Output ('RES ' + $_) }
foreach ($n in @('probe3-padded20','probe3-plainder')) { Remove-Item "$licDir\$n.license" -Force -ErrorAction SilentlyContinue }
Get-ChildItem "$licDir\db" -Recurse -Filter 'probe3-*.license' -ErrorAction SilentlyContinue | Remove-Item -Force
# final: clean empty db host dirs + verify pristine
Get-ChildItem "$licDir\db" -Directory -ErrorAction SilentlyContinue | ForEach-Object {
  if ((Get-ChildItem $_.FullName -Force -ErrorAction SilentlyContinue | Measure-Object).Count -eq 0) { Remove-Item $_.FullName -Force }
}
Write-Output ('RES final-db=' + ((Get-ChildItem "$licDir\db" -Recurse -Force -ErrorAction SilentlyContinue).FullName.Replace($licDir,'') -join ','))
Write-Output ('RES final-licroot=' + ((Get-ChildItem $licDir -File).Name -join ','))

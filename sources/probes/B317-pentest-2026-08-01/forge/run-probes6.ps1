$ErrorActionPreference = 'Continue'
$licDir  = 'C:\Niagara\iC-Niagara-4.10.9.14\security\licenses'
$staging = 'C:\Users\ASUS\pentest-staging'
foreach ($n in @('probe6-gen-plus1d','probe6-gen-plus2d')) {
  Copy-Item "$staging\$n.license" "$licDir\" -Force
}
$out = & 'C:\Niagara\iC-Niagara-4.10.9.14\bin\nre.exe' -licenses 2>&1 | Out-String
$out -split "`n" | Where-Object { $_ -match 'probe6|invalid|valid|moved|Feature|generated|signature|Signature' } | ForEach-Object { Write-Output ('RES ' + $_) }
foreach ($n in @('probe6-gen-plus1d','probe6-gen-plus2d')) { Remove-Item "$licDir\$n.license" -Force -ErrorAction SilentlyContinue }
Get-ChildItem "$licDir\db" -Recurse -Filter '*.license' -File -ErrorAction SilentlyContinue | Remove-Item -Force
Get-ChildItem "$licDir\db" -Directory -ErrorAction SilentlyContinue | ForEach-Object {
  if ((Get-ChildItem $_.FullName -Force -ErrorAction SilentlyContinue | Measure-Object).Count -eq 0) { Remove-Item $_.FullName -Force }
}
Write-Output ('RES final-licroot=' + ((Get-ChildItem $licDir -File).Name -join ','))

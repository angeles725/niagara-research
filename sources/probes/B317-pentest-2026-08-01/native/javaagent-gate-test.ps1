$ErrorActionPreference = 'Continue'
$licDir  = 'C:\Niagara\iC-Niagara-4.10.9.14\security\licenses'
$staging = 'C:\Users\ASUS\pentest-staging'
$backup  = 'C:\Users\ASUS\pentest-backup-ja'
New-Item -ItemType Directory -Path $backup -Force | Out-Null
Copy-Item "$licDir\db" $backup -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item "$licDir\inbox" $backup -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item "$staging\javaagent-developer.license" "$licDir\" -Force
Write-Output 'RES planted javaagent-developer.license'
# Oracle 1: Java layer verdict on the planted file
$out = & 'C:\Niagara\iC-Niagara-4.10.9.14\bin\nre.exe' -licenses 2>&1 | Out-String
$out -split "`n" | Where-Object { $_ -match 'javaagent|invalid|valid|Feature|developer|License' } | ForEach-Object { Write-Output ('RES ' + $_) }
# Oracle 2: native text-match gate via wb.exe -help? No — use nre.exe with a javaagent option to
# exercise createVM's gate directly (the launcher parses -J/-javaagent style options).
Write-Output 'RES --- attempt to launch with a javaagent option ---'
$try = & 'C:\Niagara\iC-Niagara-4.10.9.14\bin\nre.exe' -javaagent:whatever 2>&1 | Out-String
$try -split "`n" | Where-Object { $_ -match 'FATAL|developer|feature|agent|Cannot|boot' } | Select-Object -First 8 | ForEach-Object { Write-Output ('RES ' + $_) }
# RESTORE
Remove-Item "$licDir\javaagent-developer.license" -Force -ErrorAction SilentlyContinue
Get-ChildItem "$licDir\db" -Recurse -Filter '*.license' -File -ErrorAction SilentlyContinue | Remove-Item -Force
Get-ChildItem "$licDir\db" -Directory -ErrorAction SilentlyContinue | ForEach-Object {
  if ((Get-ChildItem $_.FullName -Force -ErrorAction SilentlyContinue | Measure-Object).Count -eq 0) { Remove-Item $_.FullName -Force }
}
Write-Output ('RES restored=' + (-not (Test-Path "$licDir\javaagent-developer.license")))
Write-Output ('RES licroot-final=' + ((Get-ChildItem $licDir -File).Name -join ','))

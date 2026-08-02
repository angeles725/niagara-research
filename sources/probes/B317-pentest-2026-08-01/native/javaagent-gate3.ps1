$ErrorActionPreference = 'Continue'
$licDir  = 'C:\Niagara\iC-Niagara-4.10.9.14\security\licenses'
$staging = 'C:\Users\ASUS\pentest-staging'
$backup  = 'C:\Users\ASUS\pentest-backup-ja2'
New-Item -ItemType Directory -Path $backup -Force | Out-Null
Copy-Item "$licDir\db" $backup -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item "$licDir\inbox" $backup -Recurse -Force -ErrorAction SilentlyContinue
Copy-Item "$staging\javaagent-developer.license" "$licDir\" -Force
Write-Output ('RES planted=' + (Test-Path "$licDir\javaagent-developer.license"))
Write-Output 'RES === WITH fake developer license planted ==='
$out2 = & 'C:\Niagara\iC-Niagara-4.10.9.14\bin\nre.exe' '-@-javaagent:whatever' -version 2>&1 | Out-String
$out2 -split "`n" | Where-Object { $_ -match 'FATAL|developer|agent|feature|Cannot|ERROR|version|boot|HostId' } | Select-Object -First 10 | ForEach-Object { Write-Output ('RES2 ' + $_) }
Write-Output 'RES === Java layer verdict on the planted file ==='
$out3 = & 'C:\Niagara\iC-Niagara-4.10.9.14\bin\nre.exe' -licenses 2>&1 | Out-String
$out3 -split "`n" | Where-Object { $_ -match 'javaagent|invalid|Feature|developer|Licenses|moved' } | Select-Object -First 8 | ForEach-Object { Write-Output ('RES3 ' + $_) }
Write-Output 'RES === RESTORE ==='
Remove-Item "$licDir\javaagent-developer.license" -Force -ErrorAction SilentlyContinue
Get-ChildItem "$licDir\db" -Recurse -Filter '*.license' -File -ErrorAction SilentlyContinue | Remove-Item -Force
Get-ChildItem "$licDir\db" -Directory -ErrorAction SilentlyContinue | ForEach-Object {
  if ((Get-ChildItem $_.FullName -Force -ErrorAction SilentlyContinue | Measure-Object).Count -eq 0) { Remove-Item $_.FullName -Force }
}
Remove-Item 'C:\Users\ASUS\pentest-staging' -Recurse -Force -ErrorAction SilentlyContinue
Get-ChildItem 'C:\Users\ASUS' -Directory -Filter 'pentest-*' | ForEach-Object { Remove-Item $_.FullName -Recurse -Force }
Write-Output ('RES restored=' + (-not (Test-Path "$licDir\javaagent-developer.license")))
Write-Output ('RES licroot-final=' + ((Get-ChildItem $licDir -File).Name -join ','))
Write-Output ('RES staging-gone=' + (-not (Test-Path 'C:\Users\ASUS\pentest-staging')))

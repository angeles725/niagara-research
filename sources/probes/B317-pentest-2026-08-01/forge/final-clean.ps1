$ErrorActionPreference = 'Continue'
$licDir = 'C:\Niagara\iC-Niagara-4.10.9.14\security\licenses'
$dbHost = "$licDir\db\Win-4D6F-169B-CEF1-8F57"
Remove-Item "$dbHost\PentestVendor.license" -Force -ErrorAction SilentlyContinue
if ((Get-ChildItem $dbHost -Force -ErrorAction SilentlyContinue | Measure-Object).Count -eq 0) {
  Remove-Item $dbHost -Force -ErrorAction SilentlyContinue
}
Write-Output ('RES dbHost-exists=' + (Test-Path $dbHost))
Write-Output ('RES dbTree=' + ((Get-ChildItem "$licDir\db" -Recurse -Force -ErrorAction SilentlyContinue).FullName.Replace($licDir,'') -join ','))
Write-Output ('RES total-lic-tree-entries=' + (Get-ChildItem $licDir -Recurse -Force | Measure-Object).Count)

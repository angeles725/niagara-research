$ErrorActionPreference = 'Continue'
$licDir = 'C:\Niagara\iC-Niagara-4.10.9.14\security\licenses'
# remove canonicalized probe copies (normalized name = Tridium.license / ISMA*.license inside db\<hostId>)
Get-ChildItem "$licDir\db" -Recurse -Filter '*.license' -File -ErrorAction SilentlyContinue | ForEach-Object {
  Write-Output ('RES removing ' + $_.FullName.Replace($licDir,''))
  Remove-Item $_.FullName -Force
}
Get-ChildItem "$licDir\db" -Directory -ErrorAction SilentlyContinue | ForEach-Object {
  if ((Get-ChildItem $_.FullName -Force -ErrorAction SilentlyContinue | Measure-Object).Count -eq 0) {
    Write-Output ('RES removing empty dir ' + $_.FullName.Replace($licDir,''))
    Remove-Item $_.FullName -Force
  }
}
Write-Output ('RES db-final=' + ((Get-ChildItem "$licDir\db" -Recurse -Force -ErrorAction SilentlyContinue).FullName.Replace($licDir,'') -join ','))
Write-Output ('RES licroot-final=' + ((Get-ChildItem $licDir -File).Name -join ','))
$h = Get-FileHash 'C:\Niagara\iC-Niagara-4.10.9.14\security\certificates\Tridium.certificate' -Algorithm SHA256
Write-Output ('RES tcert=' + $h.Hash)

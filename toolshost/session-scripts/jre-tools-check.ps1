$ErrorActionPreference = 'Continue'
Write-Output 'RES --- JRE bin tools present ---'
Get-ChildItem 'C:\Niagara\iC-Niagara-4.10.9.14\jre\bin' -File -ErrorAction SilentlyContinue | Where-Object { $_.Name -match 'jcmd|jstack|jmap|jinfo|jconsole|java|javaw|keytool' } | ForEach-Object { Write-Output ('RES ' + $_.Name + ' | ' + $_.Length) }
Write-Output 'RES --- java processes (for attach) ---'
Get-Process -Name java,javaw -ErrorAction SilentlyContinue | ForEach-Object { Write-Output ('RES proc ' + $_.Name + ' pid=' + $_.Id + ' started=' + $_.StartTime) }
Write-Output 'RES --- internet reachability (expected: none, no gateway) ---'
(Test-NetConnection -ComputerName download.sysinternals.com -Port 443 -WarningAction SilentlyContinue).TcpTestSucceeded
Write-Output 'RES --- PS version ---'
$PSVersionTable.PSVersion.ToString()

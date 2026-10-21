$ErrorActionPreference='Stop'
$projectRoot='D:\Downloads\FireControl\RuoYi-Vue3'
$targets=@('src\components\Fire\MapPanel.vue','src\api\fire\mapPresentation.css')
$backupFolder=Join-Path $projectRoot ('work\incident-b-backup-'+(Get-Date -Format 'yyyyMMdd-HHmmss'))
foreach($relative in $targets){
  $target=Join-Path $projectRoot $relative
  if(-not(Test-Path -LiteralPath ($target+'.pending'))){throw "Missing prepared file: $target.pending"}
}
New-Item -ItemType Directory -Path $backupFolder | Out-Null
foreach($relative in $targets){
  $target=Join-Path $projectRoot $relative
  Copy-Item -LiteralPath $target -Destination (Join-Path $backupFolder ([IO.Path]::GetFileName($target)))
}
foreach($relative in $targets){
  $target=Join-Path $projectRoot $relative
  Copy-Item -LiteralPath ($target+'.pending') -Destination $target -Force
}
Write-Host 'Scheme B applied. Refresh the browser.'
Write-Host ('Backup: '+$backupFolder)

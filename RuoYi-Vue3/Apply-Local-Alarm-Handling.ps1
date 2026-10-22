$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$manifest = Get-Content -LiteralPath (Join-Path $projectRoot 'local-handling-manifest.json') -Raw | ConvertFrom-Json
foreach ($entry in $manifest) {
    $target = Join-Path $projectRoot $entry.path
    if (!(Test-Path -LiteralPath ($target + '.local-handle.pending'))) { throw "Missing candidate: $target" }
    if ((Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash -ne $entry.sha256) { throw "File changed since preparation. Stopped without overwriting: $target" }
}
$backupRoot = Join-Path $projectRoot ('work/local-handling-backup-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
foreach ($entry in $manifest) {
    $backup = Join-Path $backupRoot $entry.path
    New-Item -ItemType Directory -Path (Split-Path -Parent $backup) -Force | Out-Null
    Copy-Item -LiteralPath (Join-Path $projectRoot $entry.path) -Destination $backup
}
$copied = @()
try {
    foreach ($entry in $manifest) {
        $target = Join-Path $projectRoot $entry.path
        Copy-Item -LiteralPath ($target + '.local-handle.pending') -Destination $target -Force
        $copied += $entry.path
    }
} catch {
    foreach ($relative in $copied) { Copy-Item -LiteralPath (Join-Path $backupRoot $relative) -Destination (Join-Path $projectRoot $relative) -Force }
    throw
}
Write-Host 'Applied local alarm handling and fixed dashboard counts. Refresh your browser.' -ForegroundColor Green
Write-Host "Backup: $backupRoot"

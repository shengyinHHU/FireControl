param(
    [ValidateRange(1,10000)][int]$Limit = 3,
    [ValidateRange(1,3600)][int]$MaxSeconds = 60,
    [string]$InputFile = '',
    [string]$ReviewCsv = '',
    [switch]$Offline
)
$ErrorActionPreference = 'Stop'
$bundledPython = Join-Path $env:USERPROFILE '.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe'
if (Test-Path -LiteralPath $bundledPython) { $pythonExe = $bundledPython }
else {
    $pythonCommand = Get-Command python -ErrorAction SilentlyContinue
    if (-not $pythonCommand -or $pythonCommand.Source -like '*WindowsApps*') { throw 'Install Python 3.9+ first, then rerun. No automatic installation is performed.' }
    $pythonExe = $pythonCommand.Source
}
$arguments = @((Join-Path $PSScriptRoot 'geocode_places.py'), '--limit', $Limit, '--max-seconds', $MaxSeconds)
if ($InputFile) { $arguments += @('--input', $InputFile) }
if ($ReviewCsv) { $arguments += @('--review-csv', $ReviewCsv) }
if ($Offline) { $arguments += '--offline' }
$originalKey = $env:AMAP_WEB_SERVICE_KEY
try {
    if (-not $Offline -and -not $ReviewCsv -and -not $env:AMAP_WEB_SERVICE_KEY -and -not (Test-Path -LiteralPath (Join-Path $PSScriptRoot 'config.local.json'))) {
        Write-Host 'AMap Web Service Key required. The front-end JS API Key is not used automatically.'
        $secureKey = Read-Host 'Enter Web Service Key (hidden)' -AsSecureString
        $keyPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureKey)
        try { $env:AMAP_WEB_SERVICE_KEY = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($keyPointer) }
        finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($keyPointer) }
    }
    & $pythonExe @arguments
    $resultCode = $LASTEXITCODE
}
finally { $env:AMAP_WEB_SERVICE_KEY = $originalKey }
exit $resultCode
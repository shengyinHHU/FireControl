param(
    [string]$BindAddress = "10.5.90.50",
    [int]$Port = 2000,
    [int]$DurationSeconds = 300,
    [int]$MaxMB = 100,
    [string]$PythonExe = "",
    [switch]$ReceiveOnly
)
$ErrorActionPreference = "Stop"
$scriptPath = Join-Path $PSScriptRoot "tcp_capture.py"
$pythonArgs = @()
$bundledPython = Join-Path $env:USERPROFILE ".cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe"
if ($PythonExe) {
    $executable = $PythonExe
} elseif (Get-Command py -ErrorAction SilentlyContinue) {
    $executable = "py"
    $pythonArgs = @("-3")
} elseif (Test-Path -LiteralPath $bundledPython) {
    $executable = $bundledPython
} elseif (Get-Command python -ErrorAction SilentlyContinue) {
    $executable = "python"
} else {
    throw "Python 3 is required. Install Python 3 or supply -PythonExe with the executable path."
}
& $executable @pythonArgs --version
if ($LASTEXITCODE -ne 0) { throw "Python could not start. Supply -PythonExe with a working Python 3 path." }
$captureArgs = @()
if ($ReceiveOnly) { $captureArgs += "--receive-only" }
& $executable @pythonArgs $scriptPath @captureArgs --host $BindAddress --port $Port --duration $DurationSeconds --max-mb $MaxMB
exit $LASTEXITCODE

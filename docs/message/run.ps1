# Each invocation creates a fresh timestamp/sign. Default: prepare without sending.
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    & java '-Dfile.encoding=UTF-8' 'SmsProbe.java' @args
    $probeExitCode = $LASTEXITCODE
}
finally {
    Pop-Location
}
exit $probeExitCode

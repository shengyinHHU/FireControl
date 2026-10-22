param([switch]$SelfTest)
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $sourceRoot = Join-Path $PSScriptRoot '..\src\main\java\com\cn21\open\demo\util'
    New-Item -ItemType Directory -Path 'classes' -Force | Out-Null
    & javac -encoding UTF-8 -d classes TokenTool.java (Join-Path $sourceRoot 'ByteFormat.java') (Join-Path $sourceRoot 'HmacSHATool.java') (Join-Path $sourceRoot 'XXTea.java')
    if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed. Install JDK 11 or later and check PATH.' }
    if ($SelfTest) { & java '-Dfile.encoding=UTF-8' -cp classes TokenTool --self-test }
    else { & java '-Dfile.encoding=UTF-8' -cp classes TokenTool }
    if ($LASTEXITCODE -ne 0) { throw "Token tool exited with code $LASTEXITCODE" }
} finally { Pop-Location }

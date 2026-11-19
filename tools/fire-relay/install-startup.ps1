# Run on the dedicated relay PC, from an administrator PowerShell.
[CmdletBinding()]
param([switch]$Remove)
$ErrorActionPreference = 'Stop'
$principal = [Security.Principal.WindowsPrincipal]::new([Security.Principal.WindowsIdentity]::GetCurrent())
if (-not $principal.IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)) {
    throw '请以管理员身份运行此脚本；它只管理开机计划任务，不修改网络或防火墙。'
}
$taskName = 'FireControl-Device-Relay'
if ($Remove) {
    Stop-ScheduledTask -TaskName $taskName -ErrorAction SilentlyContinue
    Unregister-ScheduledTask -TaskName $taskName -Confirm:$false -ErrorAction SilentlyContinue
    Write-Output '已移除转发开机任务。'
    exit
}
$executable = Join-Path $PSScriptRoot 'FireRelay.exe'
$configuration = Join-Path $PSScriptRoot 'config.json'
if (-not (Test-Path -LiteralPath $executable) -or -not (Test-Path -LiteralPath $configuration)) {
    throw '请把此脚本与 FireRelay.exe、config.json 放在同一目录。'
}
& $executable --config $configuration --check-config
if ($LASTEXITCODE -ne 0) { throw '配置检查失败，未创建开机任务。' }
$action = New-ScheduledTaskAction -Execute $executable -Argument '--headless' -WorkingDirectory $PSScriptRoot
$trigger = New-ScheduledTaskTrigger -AtStartup
$settings = New-ScheduledTaskSettingsSet -ExecutionTimeLimit ([TimeSpan]::Zero) -RestartCount 999 `
    -RestartInterval (New-TimeSpan -Minutes 1) -StartWhenAvailable -MultipleInstances IgnoreNew `
    -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries
$taskPrincipal = New-ScheduledTaskPrincipal -UserId 'SYSTEM' -LogonType ServiceAccount -RunLevel Highest
Register-ScheduledTask -TaskName $taskName -Action $action -Trigger $trigger -Settings $settings `
    -Principal $taskPrincipal -Description '用传设备加密转发；无须登录桌面；日志位于程序目录 logs。' -Force | Out-Null
Start-ScheduledTask -TaskName $taskName
Write-Output '已创建并启动 FireControl-Device-Relay；后续随系统启动。请不要同时双击启动第二个实例。'

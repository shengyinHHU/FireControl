#Requires -RunAsAdministrator
[CmdletBinding()]
param(
    [string]$ResultPath = (Join-Path $PSScriptRoot '../../outputs/device-diagnostics/network-repair.json')
)
$ErrorActionPreference = 'Stop'
$result = [ordered]@{ success = $false; localAddress = '10.5.90.50'; port = 2000; peerAddress = '10.5.89.246'; changes = @() }
try {
    $address = Get-NetIPAddress -AddressFamily IPv4 -IPAddress $result.localAddress
    if (@($address).Count -ne 1) { throw 'Expected exactly one adapter with 10.5.90.50.' }
    $ruleName = 'FireControl-Transmitters-TCP-2000'
    $rule = Get-NetFirewallRule -Name $ruleName -ErrorAction SilentlyContinue
    if ($rule) {
        $rule | Set-NetFirewallRule -Enabled True -Direction Inbound -Action Allow -Profile Any
        $rule | Get-NetFirewallPortFilter | Set-NetFirewallPortFilter -Protocol TCP -LocalPort 2000
        $rule | Get-NetFirewallAddressFilter | Set-NetFirewallAddressFilter -LocalAddress '10.5.90.50' -RemoteAddress '10.5.0.0/16'
    } else {
        New-NetFirewallRule -Name $ruleName -DisplayName 'FireControl transmitter reception TCP 2000' -Enabled True -Direction Inbound -Action Allow -Protocol TCP -LocalAddress '10.5.90.50' -LocalPort 2000 -RemoteAddress '10.5.0.0/16' -Profile Any | Out-Null
    }
    $result.changes += 'Allowed inbound TCP to 10.5.90.50:2000 from 10.5.0.0/16.'
    # Live capture verified that this peer's SYN arrives from the same MAC as 10.5.90.49.
    # Pin only this peer's return path, leaving the Wi-Fi/default routes unchanged.
    $neighbor = Get-NetNeighbor -InterfaceIndex $address.InterfaceIndex -IPAddress '10.5.90.49'
    if ($neighbor.LinkLayerAddress -ne '54-89-98-02-09-94') { throw 'The observed gateway MAC changed; return-route configuration needs another capture.' }
    $route = Get-NetRoute -DestinationPrefix '10.5.89.246/32' -InterfaceIndex $address.InterfaceIndex -ErrorAction SilentlyContinue
    if ($route -and @($route | Where-Object NextHop -ne '10.5.90.49').Count) { throw 'An existing peer route uses another gateway; it was preserved.' }
    if (!$route) {
        New-NetRoute -DestinationPrefix '10.5.89.246/32' -InterfaceIndex $address.InterfaceIndex -NextHop '10.5.90.49' -RouteMetric 5 | Out-Null
        $result.changes += 'Added return route for 10.5.89.246/32 via 10.5.90.49 on the receiving Ethernet adapter.'
    }
    $result.success = $true
} catch {
    $result.error = $_.Exception.Message
} finally {
    $result.checkedAt = (Get-Date).ToString('o')
    $resultDirectory = Split-Path -Parent ([System.IO.Path]::GetFullPath($ResultPath))
    New-Item -ItemType Directory -Path $resultDirectory -Force | Out-Null
    $result | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $ResultPath -Encoding UTF8
}
if (!$result.success) { exit 1 }

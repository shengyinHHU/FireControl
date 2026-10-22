param([string]$EnterpriseUser = '', [string]$DeviceCode = '', [switch]$ListOnly)
$ErrorActionPreference = 'Stop'
$OutputEncoding = [Text.UTF8Encoding]::new($false)
[Console]::OutputEncoding = [Text.UTF8Encoding]::new($false)
Push-Location $PSScriptRoot
try {
    $sourceRoot = Join-Path $PSScriptRoot '..\src\main\java\com\cn21\open\demo\util'
    New-Item -ItemType Directory -Path classes,results -Force | Out-Null
    & javac -encoding UTF-8 -d classes MediaApi.java (Join-Path $sourceRoot 'ByteFormat.java') (Join-Path $sourceRoot 'HmacSHATool.java') (Join-Path $sourceRoot 'XXTea.java')
    if ($LASTEXITCODE -ne 0) { throw 'Compilation failed' }
    $tokenResponse = Get-Content -Raw -Encoding UTF8 '../../用户无感知获取令牌Java/token-tool/result.json' | ConvertFrom-Json
    if ($tokenResponse.code -ne 0 -or !$tokenResponse.data.accessToken) { throw 'No valid saved token response' }
    $token = $tokenResponse.data.accessToken
    function Invoke-MediaApi([string]$Name, [string]$ApiPath, [hashtable]$Fields) {
        $Fields['accessToken'] = $token
        if ($EnterpriseUser) { $Fields['enterpriseUser'] = $EnterpriseUser }
        $plain = ($Fields.GetEnumerator() | ForEach-Object { "$($_.Key)=$($_.Value)" }) -join '&'
        $raw = $plain | & java '-Dfile.encoding=UTF-8' -cp classes MediaApi $ApiPath
        if ($LASTEXITCODE -ne 0) { throw "Request failed: $Name" }
        $raw | Set-Content -Encoding UTF8 "results/$Name.json"
        $response = $raw | ConvertFrom-Json
        Write-Host "$Name : code=$($response.code), msg=$($response.msg)"
        return $response
    }
    if (!$DeviceCode) {
        $regions = Invoke-MediaApi 'last-regions' '/open/token/vcpTree/getLastRegions' @{}
        if ($regions.code -ne 0) { throw "Device tree query failed: $($regions.msg). Check enterprise account and application authorization with provider." }
        $allDevices = @()
        foreach ($region in @($regions.data)) {
            $page = 1
            do {
                $devices = Invoke-MediaApi "devices-$($region.regionCode)-$page" '/open/token/vcpTree/getDevicesByRegionCode' @{regionCode=$region.regionCode;pageNo=$page;pageSize=100}
                if ($devices.code -ne 0) { throw "Device list failed: $($devices.msg)" }
                $codes = @($devices.data.deviceCodeList | Where-Object { $_ })
                foreach ($code in $codes) {
                    $allDevices += [pscustomobject]@{regionName=$region.name;regionCode=$region.regionCode;deviceCode=$code}
                }
                $page++
                if ($page -gt 1000) { throw 'Pagination safety limit reached; results incomplete' }
            } while ($codes.Count -gt 0 -and (($page - 1) * 100) -lt [int]$devices.data.total)
        }
        ConvertTo-Json -InputObject @($allDevices) -Depth 10 | Set-Content -Encoding UTF8 'results/device-list.json'
        Write-Host "Found $($allDevices.Count) device entries."
        $allDevices | Format-Table -AutoSize | Out-Host
        if ($ListOnly) { return }
        if (!$allDevices.Count) { throw 'No devices found for this enterprise account' }
        $DeviceCode = $allDevices[0].deviceCode
        Write-Host "Using first device: $DeviceCode"
    }

    $success = 0
    foreach ($protocol in @('Rtsp','Hls','Rtmp')) {
        $fields = @{deviceCode=$DeviceCode;mediaType=0;mute=0}
        if ($protocol -eq 'Hls') { $fields['supportDomain'] = 1 }
        $response = Invoke-MediaApi $protocol "/open/token/cloud/getDeviceMediaUrl$protocol" $fields
        if ($response.code -ne 0 -or !$response.data) { continue }
        $url = [string]$response.data | & java '-Dfile.encoding=UTF-8' -cp classes MediaApi --decrypt
        if ($LASTEXITCODE -ne 0 -or !$url) { throw "Decrypt failed: $protocol" }
        $decoded = $url | ConvertFrom-Json
        if (!$decoded.url) { throw "No URL in decrypted response: $protocol" }
        $url | Set-Content -Encoding UTF8 "results/$protocol-url.txt"
        $decoded.url | Set-Content -Encoding UTF8 "results/$protocol-link.txt"
        if ($protocol -eq 'Hls') {
            [IO.File]::WriteAllText((Join-Path $PSScriptRoot 'results/play-latest.m3u'), "#EXTM3U`n#EXTINF:-1,Camera`n" + $decoded.url + "`n", [Text.UTF8Encoding]::new($false))
        }
        Write-Host "$protocol URL: $url"
        $success++
    }
    if ($success -eq 0) { throw 'No stream URL obtained. See results/*.json for provider errors.' }
    Write-Host 'Stream addresses obtained; playback has not yet been verified. Addresses may expire quickly.'
} finally { Pop-Location }

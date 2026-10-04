$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$direccion = Get-NetIPAddress -InterfaceAlias 'Radmin VPN' -AddressFamily IPv4 |
    Where-Object { $_.IPAddress -like '26.*' } | Select-Object -First 1 -ExpandProperty IPAddress
if (-not $direccion) { throw 'No se encontró una IP activa de Radmin VPN.' }
Write-Host "Comparte esta dirección con tus compañeros: http://${direccion}:8080/compartido"
Write-Host 'En este modo usa esa misma dirección en tu navegador. Detén el servidor con Ctrl+C.'
& "$PSScriptRoot\mvnw.cmd" "-Dpomora.host=$direccion" jetty:run

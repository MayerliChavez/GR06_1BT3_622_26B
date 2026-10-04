param([Parameter(Mandatory=$true)][string[]]$Companeros)
# Ejecutar como administrador. Acceso limitado a las IP proporcionadas por el usuario.
$ErrorActionPreference = 'Stop'
$registro = Join-Path $PSScriptRoot '..\.tools\firewall-radmin.log'
try {
    foreach ($direccion in $Companeros) {
        $ip = [System.Net.IPAddress]::Parse($direccion)
        if ($ip.AddressFamily -ne [System.Net.Sockets.AddressFamily]::InterNetwork -or $ip.GetAddressBytes()[0] -ne 26) {
            throw 'Proporciona solo IP IPv4 individuales de Radmin (26.x.x.x).'
        }
    }
    $ipLocal = Get-NetIPAddress -InterfaceAlias 'Radmin VPN' -AddressFamily IPv4 |
        Where-Object { $_.IPAddress -like '26.*' } | Select-Object -First 1 -ExpandProperty IPAddress
    if (-not $ipLocal) { throw 'No se encontró una IP activa de Radmin VPN.' }
    $nombreRegla = 'Pomora-Radmin-Amigos-8080'
    $regla = Get-NetFirewallRule -Name $nombreRegla -ErrorAction SilentlyContinue
    if ($regla) { throw 'La regla ya existe; revisa su configuración antes de modificarla.' }
    New-NetFirewallRule -Name $nombreRegla -DisplayName 'Pomora - tres compañeros Radmin TCP 8080' `
        -Direction Inbound -Action Allow -Protocol TCP -LocalPort 8080 `
        -LocalAddress $ipLocal -RemoteAddress $Companeros `
        -InterfaceAlias 'Radmin VPN' -Profile Any | Out-Null
    'OK: regla creada para las IP indicadas de Radmin en TCP 8080.' | Set-Content -LiteralPath $registro
} catch {
    $_.Exception.Message | Set-Content -LiteralPath $registro
    throw
}

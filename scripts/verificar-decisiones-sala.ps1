param([string]$BaseUrl = 'http://localhost:8080')

$ErrorActionPreference = 'Stop'
# Reutiliza las comprobaciones de autenticación/creación y sus clientes aislados.
. (Join-Path $PSScriptRoot 'verificar-salas.ps1') -BaseUrl $BaseUrl
$checks.Clear()
$ana = New-TestClient
$luis = New-TestClient
$pedro = New-TestClient

function Solicitud-Id([string]$Html) {
    $match = [regex]::Match($Html, 'name="solicitudId" value="([a-fA-F0-9-]+)"')
    if (-not $match.Success) { throw 'No se encontró una solicitud en la sala de espera.' }
    return $match.Groups[1].Value
}

try {
    Register-And-Login $ana 'Ana Decisiones'
    Register-And-Login $luis 'Luis Decisiones'
    Register-And-Login $pedro 'Pedro Decisiones'
    $pagina = Request $ana 'salas' $null
    $csrfAna = Token $pagina.Text
    $null = Request $ana 'salas' @{accion='crear'; csrf=$csrfAna}
    $pagina = Request $ana 'salas' $null
    $codigo = [regex]::Match($pagina.Text, 'id="codigoSala"[^>]*value="([A-Z0-9]{6})"').Groups[1].Value
    $pagina = Request $luis 'salas' $null
    $csrfLuis = Token $pagina.Text
    $pagina = Request $pedro 'salas' $null
    $csrfPedro = Token $pagina.Text
    $null = Request $luis 'salas' @{accion='solicitar'; codigo=$codigo; csrf=$csrfLuis}
    $espera = Request $ana 'salas?formato=espera' $null
    $solicitudLuis = Solicitud-Id $espera.Text
    Check ($espera.Text.Contains('Aceptar') -and $espera.Text.Contains('Rechazar')) 'Responsable ve ambas decisiones'
    $denegada = Request $ana 'salas' @{accion='aceptarSolicitud'; solicitudId=$solicitudLuis; csrf='invalido'}
    Check ($denegada.Status -eq 403) 'Aceptar exige CSRF válido'
    $ajena = Request $pedro 'salas' @{accion='aceptarSolicitud'; solicitudId=$solicitudLuis; csrf=$csrfPedro}
    Check ($ajena.Status -eq 403) 'Otro estudiante no puede aceptar una solicitud ajena'
    $ajena = Request $pedro 'salas' @{accion='rechazarSolicitud'; solicitudId=$solicitudLuis; csrf=$csrfPedro}
    Check ($ajena.Status -eq 403) 'Otro estudiante no puede rechazar una solicitud ajena'
    $estado = Request $luis 'salas?formato=estadoSolicitud' $null
    Check (($estado.Text | ConvertFrom-Json).estado -eq 'PENDIENTE') 'Intentos ajenos conservan la solicitud pendiente'
    $aceptada = Request $ana 'salas' @{accion='aceptarSolicitud'; solicitudId=$solicitudLuis; csrf=$csrfAna}
    Check ($aceptada.Status -eq 302) 'Responsable acepta solicitud válida'
    $estado = Request $luis 'salas?formato=estadoSolicitud' $null
    Check (($estado.Text | ConvertFrom-Json).estado -eq 'ACEPTADA') 'Consulta de espera detecta la aceptación'
    $pagina = Request $luis 'salas' $null
    Check ($pagina.Status -eq 200 -and $pagina.Text.Contains('Participante') -and $pagina.Text.Contains($codigo)) 'Aceptado recupera su sala con rol Participante'
    Check (-not $pagina.Text.Contains('aceptarSolicitud') -and -not $pagina.Text.Contains('rechazarSolicitud')) 'Invitado no ve controles de responsable'
    $espera = Request $ana 'salas?formato=espera' $null
    Check ($espera.Text.Contains('2/4') -and -not $espera.Text.Contains('Luis Decisiones')) 'Ocupación actualizada y solicitud retirada'
    $null = Request $ana 'salas' @{accion='aceptarSolicitud'; solicitudId=$solicitudLuis; csrf=$csrfAna}
    $espera = Request $ana 'salas?formato=espera' $null
    Check ($espera.Text.Contains('2/4')) 'Doble envío no duplica participantes'
    $null = Request $pedro 'salas' @{accion='solicitar'; codigo=$codigo; csrf=$csrfPedro}
    $espera = Request $ana 'salas?formato=espera' $null
    $solicitudPedro = Solicitud-Id $espera.Text
    $rechazada = Request $ana 'salas' @{accion='rechazarSolicitud'; solicitudId=$solicitudPedro; csrf=$csrfAna}
    Check ($rechazada.Status -eq 302) 'Responsable rechaza solicitud pendiente'
    $estado = Request $pedro 'salas?formato=estadoSolicitud' $null
    Check (($estado.Text | ConvertFrom-Json).estado -eq 'RECHAZADA') 'Consulta de espera detecta el rechazo'
    $pagina = Request $pedro 'salas' $null
    Check ($pagina.Text.Contains('Tu solicitud de ingreso no fue aceptada.')) 'Solicitante recibe el aviso exigido por HU02'
    $pagina = Request $pedro 'salas' $null
    Check ($pagina.Text.Contains('Tu solicitud de ingreso no fue aceptada.')) 'El rechazo puede recuperarse al volver'
    $espera = Request $ana 'salas?formato=espera' $null
    Check (-not $espera.Text.Contains('Pedro Decisiones') -and $espera.Text.Contains('2/4')) 'Rechazo retira solicitud sin incorporar al estudiante'
    $null = Request $pedro 'salas' @{accion='solicitar'; codigo=$codigo; csrf=$csrfPedro}
    $estado = Request $pedro 'salas?formato=estadoSolicitud' $null
    Check (($estado.Text | ConvertFrom-Json).estado -eq 'PENDIENTE') 'Una solicitud nueva no hereda el rechazo anterior'
    $null = Request $pedro 'salas' @{accion='cancelarSolicitud'; csrf=$csrfPedro}
    $estado = Request $pedro 'salas?formato=estadoSolicitud' $null
    Check (($estado.Text | ConvertFrom-Json).estado -eq 'SIN_SOLICITUD') 'Cancelar conserva el comportamiento anterior'
    [pscustomobject]@{passed=$checks.Count; checks=$checks.ToArray()} | ConvertTo-Json -Depth 3
} finally {
    $ana.Dispose()
    $luis.Dispose()
    $pedro.Dispose()
}

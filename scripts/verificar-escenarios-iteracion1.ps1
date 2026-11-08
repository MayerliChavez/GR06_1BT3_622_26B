param([string]$BaseUrl = 'http://127.0.0.1:18089')
# Ejecutar contra una instancia de pruebas: registra cuentas sintéticas.

$ErrorActionPreference = 'Stop'
$checks = [System.Collections.Generic.List[string]]::new()

function New-TestClient {
    $handler = [System.Net.Http.HttpClientHandler]::new()
    $handler.AllowAutoRedirect = $false
    $client = [System.Net.Http.HttpClient]::new($handler)
    $client.BaseAddress = [Uri]($BaseUrl.TrimEnd('/') + '/')
    return $client
}

function Request($Client, [string]$Path, [hashtable]$Fields) {
    if ($null -eq $Fields) {
        $response = $Client.GetAsync($Path).GetAwaiter().GetResult()
    } else {
        $values = [System.Collections.Generic.Dictionary[string,string]]::new()
        foreach ($key in $Fields.Keys) { $values.Add($key, [string]$Fields[$key]) }
        $content = [System.Net.Http.FormUrlEncodedContent]::new($values)
        try { $response = $Client.PostAsync($Path, $content).GetAwaiter().GetResult() }
        finally { $content.Dispose() }
    }
    try {
        return [pscustomobject]@{
            Status = [int]$response.StatusCode
            Text = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            Location = [string]$response.Headers.Location
        }
    } finally { $response.Dispose() }
}

function Check([bool]$Condition, [string]$Name) {
    if (-not $Condition) { throw "FALLÓ: $Name" }
    $checks.Add($Name)
}

function Token([string]$Html) {
    $match = [regex]::Match($Html, 'name="csrf"\s+value="([^"]+)"')
    if (-not $match.Success) { throw 'No se encontró el token CSRF.' }
    return $match.Groups[1].Value
}

function Register-And-Login($Client, [string]$Name) {
    $email = 'salas-' + [Guid]::NewGuid().ToString('N') + '@example.test'
    $form = Request $Client 'registro' $null
    $registration = Request $Client 'registro' @{
        csrf = (Token $form.Text); nombre = $Name; correo = $email; contrasena = 'PruebaSalas2026!'
    }
    Check ($registration.Status -eq 200) "Registro de $Name"
    $form = Request $Client 'ingresar' $null
    $login = Request $Client 'ingresar' @{
        csrf = (Token $form.Text); correo = $email; contrasena = 'PruebaSalas2026!'
    }
    Check ($login.Status -eq 302) "Autenticación de $Name"
}


$ana = New-TestClient
$luis = New-TestClient
$pedro = New-TestClient
$sofia = New-TestClient
try {
    Register-And-Login $ana 'Ana Torres'
    Register-And-Login $luis 'Luis Mena'
    Register-And-Login $pedro 'Pedro Silva'
    Register-And-Login $sofia 'Sofía Vega'
    $page = Request $ana 'pomodoro' $null
    $tokenAna = Token $page.Text
    $null = Request $ana 'pomodoro' @{accion='iniciar'; csrf=$tokenAna}
    $page = Request $ana 'pomodoro' $null
    Check ($page.Text -match 'value="crear">Crear sala') 'HU01 3.1: Crear sala visible durante sesión individual'
    $null = Request $ana 'salas' @{accion='crear'; csrf=$tokenAna}
    $page = Request $ana 'salas' $null
    Check ($page.Text.Contains('Ya tienes una sesión de estudio en curso. Finalízala antes de crear una sala.')) 'HU01 3.1: mensaje exacto al bloquear creación'
    $page = Request $ana 'pomodoro' $null
    $sesion = [regex]::Match($page.Text, 'data-session="([^"]+)"').Groups[1].Value
    Check ($sesion.Length -eq 36) 'HU01 3.1: sesión individual conservada'
    $null = Request $ana 'pomodoro' @{accion='finalizar'; csrf=$tokenAna; sesionId=$sesion}
    $null = Request $ana 'salas' @{accion='crear'; csrf=$tokenAna}
    $page = Request $ana 'salas' $null
    $codigo = [regex]::Match($page.Text, 'id="codigoSala"[^>]*value="([A-Z0-9]{6})"').Groups[1].Value
    Check ($codigo.Length -eq 6) 'HU01: crear tras finalizar sesión'
    Check ([regex]::IsMatch($page.Text, 'value="cerrarSala">\s*Aceptar\s*</button>') -and $page.Text.Contains('¿Deseas cerrar la sala para todos los participantes?')) 'HU03a: confirmación y botón exactos'
    $salaId = [regex]::Match($page.Text, 'name="salaId"\s+value="([^"]+)"').Groups[1].Value
    $tokenLuis = Token (Request $luis 'salas' $null).Text
    $null = Request $luis 'salas' @{accion='solicitar'; codigo=$codigo; csrf=$tokenLuis}
    $page = Request $luis 'salas' $null
    Check ($page.Text.Contains('id="codigoIngreso"') -and $page.Text.Contains('Solicitar ingreso') -and $page.Text.Contains('Cancelar solicitud')) 'HU04: formulario disponible mientras espera'
    $null = Request $luis 'salas' @{accion='solicitar'; codigo=$codigo; csrf=$tokenLuis}
    Check ((Request $luis 'salas' $null).Text.Contains('Tu solicitud ya está esperando la aprobación del responsable.')) 'HU04 3.1: reenviar a la misma sala'
    $null = Request $luis 'salas' @{accion='solicitar'; codigo='P4R8TW'; csrf=$tokenLuis}
    Check ((Request $luis 'salas' $null).Text.Contains('Cancela tu solicitud pendiente antes de solicitar el ingreso a otra sala.')) 'HU04 3.2: conservar la pendiente al solicitar otra sala'
    $espera = Request $ana 'salas?formato=espera' $null
    $solicitud = [regex]::Match($espera.Text, 'name="solicitudId"\s+value="([^"]+)"').Groups[1].Value
    $null = Request $ana 'salas' @{accion='aceptarSolicitud'; solicitudId=$solicitud; csrf=$tokenAna}
    $tokenPedro = Token (Request $pedro 'salas' $null).Text
    $tokenSofia = Token (Request $sofia 'salas' $null).Text
    $null = Request $pedro 'salas' @{accion='solicitar'; codigo=$codigo; csrf=$tokenPedro}
    $null = Request $sofia 'salas' @{accion='solicitar'; codigo=$codigo; csrf=$tokenSofia}
    $espera = Request $luis 'salas?formato=espera' $null
    Check ($espera.Status -eq 200 -and $espera.Text.Contains('Pedro Silva') -and $espera.Text.Contains('Sofía Vega')) 'HU02: participante consulta solicitudes de su sala'
    Check (-not $espera.Text.Contains('aceptarSolicitud') -and -not $espera.Text.Contains('rechazarSolicitud')) 'HU02 3.1: participante sin botones de gestión'
    $esperaAna = Request $ana 'salas?formato=espera' $null
    $solicitudPedro = [regex]::Match($esperaAna.Text, 'name="solicitudId"\s+value="([^"]+)"').Groups[1].Value
    $denied = Request $luis 'salas' @{accion='rechazarSolicitud'; solicitudId=$solicitudPedro; csrf=$tokenLuis}
    Check ($denied.Status -eq 403) 'HU02: servidor impide gestionar al participante'
    $denied = Request $pedro 'salas?formato=espera' $null
    Check ($denied.Status -eq 403) 'HU02: solicitante pendiente no puede consultar la lista'
    $null = Request $sofia 'salas' @{accion='cancelarSolicitud'; csrf=$tokenSofia}
    foreach ($user in @($ana, $luis)) {
        $espera = Request $user 'salas?formato=espera' $null
        Check ($espera.Text.Contains('Pedro Silva') -and -not $espera.Text.Contains('Sofía Vega')) 'HU02 3.2: sondeo actualizado tras cancelación'
    }
    $page = Request $luis 'salas' $null
    Check ($page.Text.Contains('id="esperaSala"')) 'HU02: participante incluye el panel con actualización automática'
    Check ($page.Text -notmatch 'id="textoChatSala"[^>]*maxlength') 'HU08 2.2: textarea permite escribir 2001 caracteres'
    $null = Request $ana 'salas' @{accion='cerrarSala'; salaId=$salaId; confirmado='true'; csrf=$tokenAna}
    $page = Request $ana 'pomodoro' $null
    Check ($page.Text.Contains('La sala se cerró correctamente.')) 'HU03a: aviso de cierre correcto'
    $page = Request $luis 'pomodoro' $null
    Check ($page.Text.Contains('El responsable cerró la sala.')) 'HU03a: aviso al participante'
    $null = Request $ana 'salas' @{accion='crear'; csrf=$tokenAna}
    $page = Request $ana 'salas' $null
    Check ($page.Text.Contains('id="codigoSala"') -and -not $page.Text.Contains('value="' + $codigo + '"')) 'HU01 3.2: nueva sala tras el cierre'
    [pscustomobject]@{passed=$checks.Count; checks=$checks.ToArray()} | ConvertTo-Json -Depth 3
} finally {
    foreach ($client in @($ana, $luis, $pedro, $sofia)) { $client.Dispose() }
}

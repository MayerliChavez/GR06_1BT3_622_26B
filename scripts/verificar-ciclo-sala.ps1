param([string]$BaseUrl = 'http://127.0.0.1:18083')

# Comprobación puntual de los aportes 12/16 y 13/16. Usar una base de pruebas.
$ErrorActionPreference = 'Stop'
$checks = [System.Collections.Generic.List[string]]::new()
$usuarios = [System.Collections.Generic.List[object]]::new()
$sockets = [System.Collections.Generic.List[System.Net.WebSockets.ClientWebSocket]]::new()

function Check([bool]$Condition, [string]$Name) {
    if (-not $Condition) { throw "FALLÓ: $Name" }
    $checks.Add($Name)
}

function Request($User, [string]$Path, [hashtable]$Fields) {
    if ($null -eq $Fields) {
        $response = $User.Client.GetAsync($Path).GetAwaiter().GetResult()
    } else {
        $values = [System.Collections.Generic.Dictionary[string,string]]::new()
        foreach ($key in $Fields.Keys) { $values.Add($key, [string]$Fields[$key]) }
        $content = [System.Net.Http.FormUrlEncodedContent]::new($values)
        try { $response = $User.Client.PostAsync($Path, $content).GetAwaiter().GetResult() }
        finally { $content.Dispose() }
    }
    try {
        return [pscustomobject]@{
            Status = [int]$response.StatusCode
            Text = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
        }
    } finally { $response.Dispose() }
}

function Token([string]$Html) {
    $match = [regex]::Match($Html, 'name="csrf"\s+value="([^"]+)"')
    if (-not $match.Success) { throw 'No se encontró el CSRF de la página.' }
    return $match.Groups[1].Value
}

function New-Usuario([string]$Nombre) {
    $cookies = [System.Net.CookieContainer]::new()
    $handler = [System.Net.Http.HttpClientHandler]::new()
    $handler.AllowAutoRedirect = $false
    $handler.CookieContainer = $cookies
    $client = [System.Net.Http.HttpClient]::new($handler)
    $client.BaseAddress = [Uri]($BaseUrl.TrimEnd('/') + '/')
    $client.Timeout = [TimeSpan]::FromSeconds(15)
    $user = [pscustomobject]@{ Client=$client; Cookies=$cookies; Csrf='' }
    $usuarios.Add($user)
    $email = 'ciclo-' + [Guid]::NewGuid().ToString('N') + '@example.test'
    $password = 'PruebaCiclo2026!'
    $page = Request $user 'registro' $null
    $registered = Request $user 'registro' @{
        csrf=(Token $page.Text); nombre=$Nombre; correo=$email; contrasena=$password
    }
    Check ($registered.Status -eq 200 -and $registered.Text.Contains('Cuenta creada')) "Registro de $Nombre"
    $page = Request $user 'ingresar' $null
    $login = Request $user 'ingresar' @{
        csrf=(Token $page.Text); correo=$email; contrasena=$password
    }
    Check ($login.Status -eq 302) "Autenticación de $Nombre"
    $page = Request $user 'salas' $null
    $user.Csrf = Token $page.Text
    return $user
}

function Connect-Ciclo($User, [string]$SalaId, [string]$Csrf) {
    $ws = [System.Net.WebSockets.ClientWebSocket]::new()
    $sockets.Add($ws)
    $ws.Options.Cookies = $User.Cookies
    $ws.Options.SetRequestHeader('Origin', ([Uri]$BaseUrl).GetLeftPart([UriPartial]::Authority))
    $builder = [UriBuilder]::new($BaseUrl.TrimEnd('/') + '/salas/ciclo/' + $SalaId)
    $builder.Scheme = if ($builder.Scheme -eq 'https') { 'wss' } else { 'ws' }
    $builder.Query = 'csrf=' + [Uri]::EscapeDataString($Csrf)
    $timeout = [System.Threading.CancellationTokenSource]::new([TimeSpan]::FromSeconds(10))
    try { $null = $ws.ConnectAsync($builder.Uri, $timeout.Token).GetAwaiter().GetResult() }
    finally { $timeout.Dispose() }
    return $ws
}

function Read-Estado([System.Net.WebSockets.ClientWebSocket]$Ws) {
    $timeout = [System.Threading.CancellationTokenSource]::new([TimeSpan]::FromSeconds(10))
    $stream = [System.IO.MemoryStream]::new()
    $buffer = [byte[]]::new(4096)
    try {
        do {
            $result = $Ws.ReceiveAsync(
                [ArraySegment[byte]]::new($buffer), $timeout.Token
            ).GetAwaiter().GetResult()
            if ($result.MessageType -eq [System.Net.WebSockets.WebSocketMessageType]::Close) {
                return [pscustomobject]@{ tipo='cerrada'; codigo=[int]$result.CloseStatus }
            }
            $stream.Write($buffer, 0, $result.Count)
        } while (-not $result.EndOfMessage)
        return [System.Text.Encoding]::UTF8.GetString($stream.ToArray()) | ConvertFrom-Json
    } finally {
        $timeout.Dispose()
        $stream.Dispose()
    }
}

function Solicitud-Id([string]$Html) {
    $match = [regex]::Match($Html, 'name="solicitudId"\s+value="([a-fA-F0-9-]+)"')
    if (-not $match.Success) { throw 'No se encontró una solicitud pendiente.' }
    return $match.Groups[1].Value
}

try {
    $ana = New-Usuario 'Ana Ciclo'
    $luis = New-Usuario 'Luis Ciclo'
    $pedro = New-Usuario 'Pedro Ciclo'
    $created = Request $ana 'salas' @{accion='crear'; csrf=$ana.Csrf}
    Check ($created.Status -eq 302) 'Responsable crea la sala de pruebas'
    $page = Request $ana 'salas' $null
    $codigo = [regex]::Match($page.Text, 'id="codigoSala"[^>]*value="([A-Z0-9]{6})"').Groups[1].Value
    $salaId = [regex]::Match($page.Text, 'data-sala-id="([a-fA-F0-9-]+)"').Groups[1].Value
    Check ($page.Status -eq 200 -and $salaId.Length -eq 36 -and $page.Text.Contains('sala-ciclo.js')) 'JSP del ciclo incluida y JavaScript cargado'
    $null = Request $ana 'salas' @{accion='iniciarEstudio'; csrf=$ana.Csrf}
    $page = Request $ana 'salas' $null
    Check ($page.Text.Contains('Se necesitan al menos dos participantes')) 'Inicio con un solo participante muestra la validación'

    $null = Request $luis 'salas' @{accion='solicitar'; codigo=$codigo; csrf=$luis.Csrf}
    $espera = Request $ana 'salas?formato=espera' $null
    $null = Request $ana 'salas' @{
        accion='aceptarSolicitud'; solicitudId=(Solicitud-Id $espera.Text); csrf=$ana.Csrf
    }
    $denied = Request $luis 'salas' @{accion='iniciarEstudio'; csrf=$luis.Csrf}
    Check ($denied.Status -eq 403) 'Participante no puede iniciar el ciclo'
    $denied = Request $ana 'salas' @{accion='iniciarEstudio'; csrf='invalido'}
    Check ($denied.Status -eq 403) 'Inicio exige CSRF válido'

    $wsAna = Connect-Ciclo $ana $salaId $ana.Csrf
    $antes = Read-Estado $wsAna
    Check ($antes.tipo -eq 'estadoCiclo' -and -not $antes.iniciado) 'WebSocket autenticado comunica el estado previo al inicio'
    $started = Request $ana 'salas' @{accion='iniciarEstudio'; csrf=$ana.Csrf}
    Check ($started.Status -eq 302) 'Responsable inicia el ciclo compartido'
    $estadoAna = $null
    for ($i = 0; $i -lt 5; $i++) {
        $estadoAna = Read-Estado $wsAna
        if ($estadoAna.iniciado) { break }
    }
    Check ($estadoAna.iniciado -and $estadoAna.bloque -eq 'CONCENTRACION') 'Servidor publica el inicio por WebSocket sin petición de actualización'
    $wsLuis = Connect-Ciclo $luis $salaId $luis.Csrf
    $estadoLuis = Read-Estado $wsLuis
    Check ($estadoAna.finBloque -eq $estadoLuis.finBloque -and $estadoLuis.restanteMillis -gt 0) 'Ambos participantes reciben el mismo vencimiento'
    $siguiente = Read-Estado $wsLuis
    Check ($siguiente.horaServidor -gt $estadoLuis.horaServidor -and $siguiente.restanteMillis -lt $estadoLuis.restanteMillis) 'Monitor envía actualizaciones periódicas del tiempo restante'
    $page = Request $luis 'salas' $null
    Check ($page.Status -eq 200 -and -not $page.Text.Contains('id="iniciarCicloSala"')) 'Participante recarga la JSP sin controles de responsable'
    $page = Request $ana 'salas' $null
    Check ($page.Status -eq 200 -and -not $page.Text.Contains('id="iniciarCicloSala"')) 'Tras el inicio la JSP del responsable oculta el botón'
    $wsAna.Abort()
    $reconexion = Connect-Ciclo $ana $salaId $ana.Csrf
    $recuperado = Read-Estado $reconexion
    Check ($recuperado.finBloque -eq $estadoAna.finBloque -and $recuperado.orden -eq $estadoAna.orden) 'Reconectar recupera el mismo bloque y vencimiento'

    $null = Request $pedro 'salas' @{accion='solicitar'; codigo=$codigo; csrf=$pedro.Csrf}
    $espera = Request $ana 'salas?formato=espera' $null
    Check ($espera.Text.Contains('Podrás aceptar nuevos participantes en el siguiente descanso.') -and
        $espera.Text -match 'value="aceptarSolicitud"\s+disabled') 'Vista utiliza la política común y deshabilita aceptar en concentración'
    $null = Request $ana 'salas' @{
        accion='aceptarSolicitud'; solicitudId=(Solicitud-Id $espera.Text); csrf=$ana.Csrf
    }
    $pendiente = Request $pedro 'salas?formato=estadoSolicitud' $null
    Check (($pendiente.Text | ConvertFrom-Json).estado -eq 'PENDIENTE') 'El servidor también bloquea aceptar en concentración y conserva la solicitud'
    $ajeno = Connect-Ciclo $pedro $salaId $pedro.Csrf
    $rechazo = Read-Estado $ajeno
    Check ($rechazo.tipo -eq 'cerrada' -and $rechazo.codigo -eq 1008) 'Un solicitante pendiente no puede escuchar el ciclo de la sala'
    $invalido = Connect-Ciclo $ana $salaId 'invalido'
    $rechazo = Read-Estado $invalido
    Check ($rechazo.tipo -eq 'cerrada' -and $rechazo.codigo -eq 1008) 'WebSocket exige CSRF válido'

    [pscustomobject]@{passed=$checks.Count; checks=$checks.ToArray()} | ConvertTo-Json -Depth 3
} finally {
    foreach ($ws in $sockets) { $ws.Abort(); $ws.Dispose() }
    foreach ($user in $usuarios) { $user.Client.Dispose() }
}

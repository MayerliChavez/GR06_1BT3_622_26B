param([string]$BaseUrl = 'http://localhost:8080')

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
try {
    $anonymous = Request $ana 'salas' $null
    Check ($anonymous.Status -eq 302 -and $anonymous.Location -match 'ingresar') 'Salas exige autenticación'
    Register-And-Login $ana 'Ana Salas'
    $page = Request $ana 'salas' $null
    Check ($page.Status -eq 200 -and $page.Text.Contains('Crear sala')) 'La JSP permite crear una sala'
    $csrf = Token $page.Text
    $forbidden = Request $ana 'salas' @{accion = 'crear'; csrf = 'invalido'}
    Check ($forbidden.Status -eq 403) 'Crear exige CSRF válido'
    $created = Request $ana 'salas' @{accion = 'crear'; csrf = $csrf}
    Check ($created.Status -eq 302) 'Crear redirige a la sala persistida'
    $page = Request $ana 'salas' $null
    $code = [regex]::Match($page.Text, 'id="codigoSala"[^>]*value="([A-Z0-9]{6})"').Groups[1].Value
    Check ($code.Length -eq 6 -and $page.Text.Contains('Responsable')) 'Código alfanumérico y rol Responsable'
    Check ($page.Text.Contains('Copiar código')) 'Botón Copiar código disponible'
    $again = Request $ana 'salas' $null
    Check ($again.Text.Contains('value="' + $code + '"')) 'Recargar conserva el código'
    $null = Request $ana 'salas' @{accion = 'crear'; csrf = $csrf}
    $again = Request $ana 'salas' $null
    Check ($again.Text.Contains('Ya participas en una sala privada.') -and $again.Text.Contains('value="' + $code + '"')) 'Otra creación conserva la sala existente'
    $null = Request $ana 'pomodoro' @{accion = 'iniciar'; csrf = $csrf}
    $individual = Request $ana 'pomodoro' $null
    Check ($individual.Text.Contains('Ya participas en una sala privada.')) 'Sala abierta bloquea inicio individual'
    Register-And-Login $luis 'Luis Salas'
    $page = Request $luis 'salas' $null
    $csrfLuis = Token $page.Text
    Check (-not $page.Text.Contains('value="' + $code + '"')) 'Otro usuario no ve la sala de Ana'
    $null = Request $luis 'pomodoro' @{accion = 'iniciar'; csrf = $csrfLuis}
    $null = Request $luis 'salas' @{accion = 'crear'; csrf = $csrfLuis}
    $page = Request $luis 'salas' $null
    Check ($page.Text.Contains('Ya tienes una sesión de estudio en curso.')) 'Sesión individual bloquea creación de sala'
    Check (-not $page.Text.Contains('id="codigoSala"')) 'La creación bloqueada no produce una sala'
    [pscustomobject]@{passed = $checks.Count; checks = $checks.ToArray()} | ConvertTo-Json -Depth 3
} finally {
    $ana.Dispose()
    $luis.Dispose()
}

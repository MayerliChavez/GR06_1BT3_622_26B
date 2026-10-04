param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'cuentas-prueba.ps1')
function Comprobar($condicion, $mensaje) {
    if (-not $condicion) { throw $mensaje }
    Write-Output "OK: $mensaje"
}
function Enviar($navegador, $token, $accion, $ruta = 'compartido') {
    try {
        Invoke-WebRequest "$BaseUrl/$ruta" -UseBasicParsing -WebSession $navegador -Method Post -Body @{ csrf=$token; accion=$accion; formato='json' }
    } catch {
        if ($_.Exception.Response) { [pscustomobject]@{StatusCode=[int]$_.Exception.Response.StatusCode; Content='{}'} }
        else { throw }
    }
}
$estudianteA=Nueva-CuentaPrueba $BaseUrl 'Ana Prueba'; $primera = Invoke-WebRequest "$BaseUrl/compartido" -UseBasicParsing -WebSession $estudianteA
$estudianteB=Nueva-CuentaPrueba $BaseUrl 'Luis Prueba'; $segunda = Invoke-WebRequest "$BaseUrl/compartido" -UseBasicParsing -WebSession $estudianteB
$tokenA = [regex]::Match($primera.Content, 'data-csrf="([^"]+)"').Groups[1].Value
$tokenB = [regex]::Match($segunda.Content, 'data-csrf="([^"]+)"').Groups[1].Value
Comprobar ($primera.StatusCode -eq 200 -and $tokenA.Length -gt 0) 'Pantalla compartida y formulario JSP disponibles'
Comprobar ((Enviar $estudianteA 'incorrecto' 'buscar').StatusCode -eq 403) 'Protección CSRF del emparejamiento'
$espera = (Enviar $estudianteA $tokenA 'buscar').Content | ConvertFrom-Json
Comprobar ($espera.esperando -and -not $espera.compartidaId) 'Primer estudiante queda en espera'
$repetida = (Enviar $estudianteA $tokenA 'buscar').Content | ConvertFrom-Json
Comprobar ($repetida.esperando) 'No se empareja consigo mismo'
$cancelada = (Enviar $estudianteA $tokenA 'cancelar').Content | ConvertFrom-Json
Comprobar (-not $cancelada.esperando -and -not $cancelada.compartidaId) 'Cancelar retira la solicitud'
$null = Enviar $estudianteA $tokenA 'buscar'
$parejaB = (Enviar $estudianteB $tokenB 'buscar').Content | ConvertFrom-Json
$parejaA = (Enviar $estudianteA $tokenA 'actualizar').Content | ConvertFrom-Json
Comprobar ($parejaA.compartidaId -and $parejaA.compartidaId -eq $parejaB.compartidaId) 'Ambos estudiantes reciben la misma sesión compartida'
Comprobar ($parejaA.presente -and $parejaB.presente -and $parejaA.orden -eq 1) 'Ambos participantes presentes en el primer bloque'
Comprobar ([Math]::Abs($parejaA.restanteMillis - $parejaB.restanteMillis) -lt 2000 -and $parejaA.objetivoMillis -eq 1500000) 'Temporizadores sincronizados de 25 minutos'
Comprobar ((Enviar $estudianteA $tokenA 'iniciar' 'pomodoro').StatusCode -eq 409) 'Se evita modificar individualmente la sesión compartida'
$vistaA=Invoke-WebRequest "$BaseUrl/compartido" -UseBasicParsing -WebSession $estudianteA; Comprobar ($vistaA.Content.Contains('Luis Prueba') -and $vistaA.Content.Contains('Ana Prueba')) 'Nombre propio y nombre real del compañero visibles'; $saleA = (Enviar $estudianteA $tokenA 'salir').Content | ConvertFrom-Json
$sigueB = (Enviar $estudianteB $tokenB 'actualizar').Content | ConvertFrom-Json
Comprobar (-not $saleA.compartidaId -and $sigueB.compartidaId -and -not $sigueB.presente -and $sigueB.motivo -eq 'VOLUNTARIA') 'Salida voluntaria conserva el bloque del compañero'
$saleB = (Enviar $estudianteB $tokenB 'desconectar').Content | ConvertFrom-Json
Comprobar (-not $saleB.compartidaId) 'Desconexión cierra la última participación'
$historialA = Invoke-WebRequest "$BaseUrl/historial" -UseBasicParsing -WebSession $estudianteA
$historialB = Invoke-WebRequest "$BaseUrl/historial" -UseBasicParsing -WebSession $estudianteB
Comprobar ($historialA.Content.Contains('FINALIZADA') -and $historialB.Content.Contains('ABANDONADA')) 'Cada estudiante conserva su historial y motivo de cierre'
Write-Output 'Verificación HTTP del estudio compartido completada.'

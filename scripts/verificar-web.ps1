param([string]$BaseUrl = 'http://127.0.0.1:8080')
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'cuentas-prueba.ps1')
$BaseUrl = $BaseUrl.TrimEnd('/')
function Assert-Web($Condicion, [string]$Mensaje) {
    if (-not $Condicion) { throw $Mensaje }
    Write-Output "OK: $Mensaje"
}
function Post-Accion([string]$Accion, [string]$SesionId, [string]$Token, $Navegador) {
    try {
        Invoke-WebRequest "$BaseUrl/pomodoro" -Method Post -UseBasicParsing -WebSession $Navegador -Body @{
            accion = $Accion; sesionId = $SesionId; csrf = $Token; formato = 'json'
        }
    } catch {
        if ($_.Exception.Response) {
            [pscustomobject]@{ StatusCode = [int]$_.Exception.Response.StatusCode; Content = '{}' }
        } else { throw }
    }
}

$navegador=Nueva-CuentaPrueba $BaseUrl; $pagina = Invoke-WebRequest "$BaseUrl/pomodoro" -UseBasicParsing -WebSession $navegador
$csrf = [regex]::Match($pagina.Content, 'data-csrf="([^"]+)"').Groups[1].Value
Assert-Web ($pagina.StatusCode -eq 200 -and $pagina.Content.Contains('25:00')) 'CU01: inicio muestra 25:00'
$vacio = Invoke-WebRequest "$BaseUrl/historial" -UseBasicParsing -WebSession $navegador
Assert-Web ($vacio.Content.Contains('Tu primer bloque te espera')) 'CU04: historial vacío'
$rechazado = Post-Accion 'iniciar' '' 'incorrecto' $navegador
Assert-Web ($rechazado.StatusCode -eq 403) 'Formulario sin token válido rechazado'
$inicio = Post-Accion 'iniciar' '' $csrf $navegador
Assert-Web ($inicio.StatusCode -eq 200) 'CU01: sesión creada'
$estado = $inicio.Content | ConvertFrom-Json
Assert-Web ($estado.estado -eq 'EN_EJECUCION' -and $estado.objetivoMillis -eq 1500000) 'Bloque inicial de 25 minutos en ejecución'
$duplicado = Post-Accion 'iniciar' '' $csrf $navegador
Assert-Web ($duplicado.StatusCode -eq 409) 'Se impide iniciar dos sesiones activas'
$prematuro = Post-Accion 'completar' $estado.sesionId $csrf $navegador
Assert-Web ($prematuro.StatusCode -eq 409) 'No se completa antes de agotar el tiempo'
Start-Sleep -Milliseconds 1100
$pausa = Post-Accion 'pausar' $estado.sesionId $csrf $navegador
$pausado = $pausa.Content | ConvertFrom-Json
Assert-Web ($pausado.estado -eq 'PAUSADO') 'CU02: bloque pausado'
$recarga = Invoke-WebRequest "$BaseUrl/pomodoro?formato=json" -UseBasicParsing -WebSession $navegador
$recargado = $recarga.Content | ConvertFrom-Json
Assert-Web ($recargado.restanteMillis -eq $pausado.restanteMillis) 'CU02: recargar conserva exactamente el tiempo en pausa'
$reanudar = Post-Accion 'reanudar' $estado.sesionId $csrf $navegador
$reanudado = $reanudar.Content | ConvertFrom-Json
Assert-Web ($reanudado.estado -eq 'EN_EJECUCION' -and $reanudado.restanteMillis -le $pausado.restanteMillis) 'CU03: reanuda desde el tiempo conservado'
$historial = Invoke-WebRequest "$BaseUrl/historial" -UseBasicParsing -WebSession $navegador
Assert-Web ($historial.Content.Contains('Historial de sesiones') -and $historial.Content.Contains('CONCENTRACION')) 'CU04: sesión y bloques presentes en la tabla JSP'
$otroNavegador=Nueva-CuentaPrueba $BaseUrl; $otro = Invoke-WebRequest "$BaseUrl/pomodoro" -UseBasicParsing -WebSession $otroNavegador
$otroCsrf = [regex]::Match($otro.Content, 'data-csrf="([^"]+)"').Groups[1].Value
$ajena = Post-Accion 'pausar' $estado.sesionId $otroCsrf $otroNavegador
Assert-Web ($ajena.StatusCode -eq 403) 'No se modifica una sesión de otra identidad de navegador'
$fin = Post-Accion 'finalizar' $estado.sesionId $csrf $navegador
Assert-Web ($fin.StatusCode -eq 200) 'Operación modelada: finalizar sesión'
$nuevo = Post-Accion 'iniciar' '' $csrf $navegador
$nuevaSesion = $nuevo.Content | ConvertFrom-Json
$abandono = Post-Accion 'abandonar' $nuevaSesion.sesionId $csrf $navegador
Assert-Web ($abandono.StatusCode -eq 200) 'Operación modelada: abandonar sesión'
$cerradas = Invoke-WebRequest "$BaseUrl/historial" -UseBasicParsing -WebSession $navegador
Assert-Web ($cerradas.Content.Contains('FINALIZADA') -and $cerradas.Content.Contains('ABANDONADA')) 'CU04: conserva estados de las sesiones cerradas'
Write-Output 'Prueba HTTP del incremento 1 completada.'

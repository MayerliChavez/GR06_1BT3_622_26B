param([string]$BaseUrl='http://localhost:8080')
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'cuentas-prueba.ps1')
function Comprobar($condicion,$mensaje) { if(-not $condicion){throw $mensaje}; Write-Output "OK: $mensaje" }
function Estado-Post($ruta,$sesion,$datos) {
    try { (Invoke-WebRequest "$BaseUrl/$ruta" -UseBasicParsing -WebSession $sesion -Method Post -Body $datos).StatusCode }
    catch { if($_.Exception.Response){[int]$_.Exception.Response.StatusCode}else{throw} }
}
$inicio=Invoke-WebRequest "$BaseUrl/" -UseBasicParsing -SessionVariable anonimo
Comprobar ($inicio.Content.Contains('Bienvenido a Pomora') -and $inicio.Content.Contains('Correo electrónico')) 'La primera pantalla es el inicio de sesión'
$restringida=Invoke-WebRequest "$BaseUrl/compartido" -UseBasicParsing -WebSession $anonimo
Comprobar ($restringida.Content.Contains('Bienvenido a Pomora')) 'Estudio compartido exige autenticación'
$csrf=[regex]::Match($inicio.Content,'name="csrf" value="([^"]+)"').Groups[1].Value
Comprobar ((Estado-Post 'ingresar' $anonimo @{csrf=$csrf;correo='inexistente@example.test';contrasena='incorrecta'}) -eq 400) 'Se rechazan credenciales incorrectas'
Comprobar ((Estado-Post 'registro' $anonimo @{csrf='incorrecto';nombre='Prueba';correo='inexistente@example.test';contrasena='Clave-Prueba-123'}) -eq 403) 'Registro protegido por CSRF'
$cuenta=Nueva-CuentaPrueba $BaseUrl 'Nombre de Cuenta Prueba'
$pagina=Invoke-WebRequest "$BaseUrl/compartido" -UseBasicParsing -WebSession $cuenta
Comprobar ($pagina.Content.Contains('Nombre de Cuenta Prueba')) 'Tras ingresar se muestra el nombre registrado'
$csrfCuenta=[regex]::Match($pagina.Content,'name="csrf" value="([^"]+)"').Groups[1].Value
$null=Invoke-WebRequest "$BaseUrl/salir" -UseBasicParsing -WebSession $cuenta -Method Post -Body @{csrf=$csrfCuenta}
$cerrada=Invoke-WebRequest "$BaseUrl/compartido" -UseBasicParsing -WebSession $cuenta
Comprobar ($cerrada.Content.Contains('Bienvenido a Pomora')) 'Cerrar sesión revoca el acceso'
Write-Output 'Verificación web de cuentas completada.'

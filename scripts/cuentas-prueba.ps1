function Nueva-CuentaPrueba([string]$BaseUrl, [string]$Nombre='Estudiante de prueba') {
    $paginaRegistro=Invoke-WebRequest "$BaseUrl/registro" -UseBasicParsing -SessionVariable navegadorCuenta
    $tokenRegistro=[regex]::Match($paginaRegistro.Content,'name="csrf" value="([^"]+)"').Groups[1].Value
    $correoPrueba="prueba-$([guid]::NewGuid())@example.test"
    $clavePrueba=[guid]::NewGuid().ToString()+'aA1!'
    $registroCuenta=Invoke-WebRequest "$BaseUrl/registro" -UseBasicParsing -WebSession $navegadorCuenta -Method Post -Body @{csrf=$tokenRegistro;nombre=$Nombre;correo=$correoPrueba;contrasena=$clavePrueba}
    if(-not $registroCuenta.Content.Contains('Cuenta creada')) { throw 'No se pudo registrar la cuenta de prueba.' }
    $null=Invoke-WebRequest "$BaseUrl/ingresar" -UseBasicParsing -WebSession $navegadorCuenta -Method Post -Body @{csrf=$tokenRegistro;correo=$correoPrueba;contrasena=$clavePrueba}
    return $navegadorCuenta
}

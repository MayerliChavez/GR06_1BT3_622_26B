$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
if (-not (Get-Command javac -ErrorAction SilentlyContinue)) {
    throw 'Instala un JDK 17 o superior y agrega su carpeta bin al PATH.'
}
$mavenDir = Join-Path $PSScriptRoot '.tools\apache-maven-3.9.11'
if (-not (Test-Path (Join-Path $mavenDir 'bin\mvn.cmd'))) {
    New-Item -ItemType Directory -Force '.tools' | Out-Null
    $url = 'https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/3.9.11/apache-maven-3.9.11-bin.zip'
    $zip = Join-Path $PSScriptRoot '.tools\maven.zip'
    Invoke-WebRequest $url -OutFile $zip
    $expected = (Invoke-WebRequest "$url.sha512").Content.Trim().Split(' ')[0]
    $actual = (Get-FileHash -LiteralPath $zip -Algorithm SHA512).Hash
    if ($actual -ine $expected) { throw 'La descarga de Maven no pasó la verificación SHA512.' }
    Expand-Archive -LiteralPath $zip -DestinationPath '.tools' -Force
    Remove-Item -LiteralPath $zip
}
& '.\mvnw.cmd' -B package
if ($LASTEXITCODE -ne 0) { throw 'No se pudo compilar el proyecto.' }

@echo off
setlocal
if not exist "%~dp0.tools\apache-maven-3.9.11\bin\mvn.cmd" (
 echo Ejecuta primero: powershell -ExecutionPolicy Bypass -File preparar.ps1
 exit /b 1
)
rem Fuerza el fallback TCP del JDK cuando los sockets AF_UNIX fallan en Windows.
set "MAVEN_OPTS=%MAVEN_OPTS% --enable-native-access=ALL-UNNAMED -Djava.net.preferIPv4Stack=true "-Djdk.net.unixdomain.tmpdir=%~dp0.tools\sin-sockets-unix""
call "%~dp0.tools\apache-maven-3.9.11\bin\mvn.cmd" -Dmaven.repo.local="%~dp0.tools\repository" %*
exit /b %ERRORLEVEL%

# GR06_1BT3_622_26B

Pomora: aplicación Java Web del grupo 06, con JSP, Servlet y Hibernate/JPA. Implementa Pomodoro individual y estudio compartido a partir de los diagramas entregados, con asistencia de Codex.

## Preparación e inicio

Requiere Windows, JDK 17 o superior y conexión para descargar Maven y dependencias. En este equipo se verificó con JDK 25. Desde la carpeta del proyecto:

```powershell
powershell -ExecutionPolicy Bypass -File .\preparar.ps1
.\iniciar.cmd
```

Abre http://localhost:8080/. La primera pantalla permite registrarse con nombre, correo y contraseña, e ingresar. Después de ingresar se abre estudio compartido; el menú permite acceder al temporizador individual y al historial. Detén el servidor con Ctrl+C antes de iniciar otra instancia.

## Incrementos funcionales

- Incremento 1: iniciar temporizador de 25 minutos, pausar, reanudar y consultar el historial de la cuenta. Incluye completar bloque, siguiente bloque, finalizar y abandonar, definidos en el modelo. El siguiente bloque individual se inicia manualmente.
- Incremento 2: registrar cuenta, iniciar sesión y emparejar aleatoriamente otro estudiante disponible. Muestra el nombre registrado del compañero y un temporizador común. Permite cancelar búsqueda, salir y registrar desconexiones. Los bloques compartidos avanzan automáticamente y cada estudiante conserva su historial.

Configuración: concentración de 25 minutos, descanso corto de 5, largo de 15 y descanso largo cada 4 concentraciones. El detector de presencia registra desconexión después de 90 segundos sin señales, con revisión cada 10 segundos. La cola de búsqueda es temporal; las pantallas activas vuelven a solicitar emparejamiento tras un reinicio.

## Fidelidad al modelo y ampliación autorizada

Se conservan las clases, enumeraciones y métodos funcionales del primer diagrama; el segundo incorpora SesionCompartida, ParticipacionCompartida, sus enumeraciones, interfaces y servicio con las 19 operaciones dibujadas. Getters, constructores, validación e infraestructura no cuentan como operaciones funcionales según el criterio aclarado por el usuario.

Los diagramas de actividades y robustez incluyen registro, autenticación y CuentaUsuario, ausentes del diagrama de clases. El usuario autorizó su incorporación al pedir estos casos de uso: se añadió CuentaUsuario y ServicioCuentas.registrar / autenticar. La ampliación se documenta expresamente, sin afirmar que esas dos operaciones estén en el diagrama de clases original.

Las cuentas guardan correo único normalizado y hash PBKDF2-HMAC-SHA256 con sal aleatoria. La identidad procede de la sesión autenticada del servidor; una cookie con UUID no permite ingresar. Los nombres se escapan al mostrarse en JSP. Los formularios validan CSRF y las acciones comprueban pertenencia de las sesiones.

## Pruebas y compilación

```powershell
.\mvnw.cmd package
```

Ejecuta las 18 pruebas Java y genera `target/GR06_1BT3_622_26B.war`. Es desplegable en un contenedor Jakarta Servlet 6 compatible; Jetty usa contexto raíz `/` durante desarrollo.

Con el servidor iniciado:

```powershell
.\scripts\verificar-acceso.ps1 -BaseUrl http://localhost:8080
.\scripts\verificar-compartido.ps1 -BaseUrl http://localhost:8080
.\scripts\verificar-web.ps1 -BaseUrl http://localhost:8080
```

Las 34 comprobaciones HTTP crean cuentas independientes de prueba y conservan sus registros en la base local. JUnit usa H2 en memoria y tiempos simulados; no reduce los 25 minutos reales de la aplicación.

## Probar con compañeros por Radmin VPN

Conecta los equipos a la misma red Radmin y ejecuta en el anfitrión:

```powershell
.\iniciar-radmin.ps1
```

El script detecta la IP del adaptador Radmin y escucha exclusivamente en esa dirección. Todos, incluido el anfitrión, deben usar la URL que muestra, `http://IP-RADMIN:8080/`. No usen localhost en los equipos de los compañeros. Cada participante registra e ingresa con su propia cuenta.

El anfitrión debe permitir TCP 8080 por el adaptador Radmin desde las IP concretas de sus compañeros. El script siguiente se ejecuta en PowerShell como administrador, pasando esas IP; no abre todo el rango de la VPN:

```powershell
.\scripts\habilitar-radmin-amigos.ps1 -Companeros IP-COMPANERO-1,IP-COMPANERO-2
```

Para retirar ese permiso, en PowerShell como administrador: `Remove-NetFirewallRule -Name Pomora-Radmin-Amigos-8080`.

## Persistencia y entorno

- Maven 3.9.11 en `.tools`, sin alterar el PATH global.
- Jetty 12.0.35, Jakarta Servlet 6 y JSP/JSTL.
- Hibernate ORM 6.6.58.Final / Jakarta Persistence 3.1.
- H2 2.3.232, archivo `data/pomora.mv.db`.
- JUnit Jupiter 5.11.4.

Cuentas, nombres, sesiones, bloques y pausas se guardan en H2. Tras reiniciar el servidor se ingresa otra vez con la misma cuenta para recuperar el historial; los historiales anónimos antiguos permanecen en la base y no se asignan automáticamente a cuentas nuevas. El esquema se actualiza mediante JPA sin eliminar los datos existentes. La configuración es para desarrollo en una única instancia; usa JavaScript para el contador y las actualizaciones de emparejamiento.

El lanzador usa IPv4 y una ruta inexistente para activar la alternativa TCP interna del JDK en este Windows. No crees `.tools/sin-sockets-unix`. Herramientas locales, base de datos, logs y WAR generado quedan fuera de Git.

## Documentación y estructura

- [Informe actualizado en Word](output/word/Informe_Pomora_GR06_1BT3_622_26B.docx): trazabilidad de los siete casos de uso, secuencias del flujo principal, código y evidencias.
- [Informe actualizado en PDF](output/word/Informe_Pomora_GR06_1BT3_622_26B.pdf): versión de 22 páginas para revisión y entrega.
- [Modelo de clases actualizado](docs/modelos/incremento2-clases-actualizado.jpg): incluye CuentaUsuario y ServicioCuentas.
- [Informe PDF anterior](output/pdf/Informe_Pomora_GR06_1BT3_622_26B.pdf): documentación histórica de la implementación.
- [Trazabilidad del incremento 1](docs/TRAZABILIDAD.md).
- [Incremento 2 y ampliación de cuentas](docs/INCREMENTO2.md).
- [Auditoría de métodos](docs/AUDITORIA-METODOS.md).
- [Verificación](docs/VERIFICACION.md).
- `docs/modelos`: diagramas originales; `docs/evidencias`: capturas reales de la aplicación.
- `src/main/java/edu/proyecto`: model, service, repository, web y config.
- `src/main/resources/META-INF/persistence.xml`: ORM.
- `src/main/webapp`: JSP, CSS y JavaScript.
- `src/test/java/edu/proyecto` y `scripts`: pruebas reproducibles.

Repositorio público: [MayerliChavez/GR06_1BT3_622_26B](https://github.com/MayerliChavez/GR06_1BT3_622_26B).

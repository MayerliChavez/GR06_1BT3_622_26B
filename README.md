# GR06_1BT3_622_26B

Aplicación web de estudio basada en la técnica Pomodoro para gestionar sesiones de estudio, descansos y tiempo de aprendizaje.

## Pomora — Incremento 1: Pomodoro individual

Aplicación Java Web con JSP, Servlet y Hibernate/JPA del grupo 06, basada en los diagramas adjuntos. Nombre del proyecto: `GR06_1BT3_622_26B`. Repositorio: [MayerliChavez/GR06_1BT3_622_26B.](https://github.com/MayerliChavez/GR06_1BT3_622_26B.).

## Ejecutar

Desde PowerShell en esta carpeta:

```powershell
.\iniciar.cmd
```

Abre http://localhost:8080/pomodoro. Detén el servidor con Ctrl+C antes de iniciar otra instancia. La ruta anterior `/estado` ahora redirige al temporizador.

## Funciones

1. CU01: iniciar sesión y temporizador de concentración de 25 minutos.
2. CU02: pausar y guardar el tiempo activo acumulado.
3. CU03: reanudar conservando el tiempo, sin sumar la pausa.
4. CU04: consultar sesiones del navegador, de la más reciente a la primera, con detalle de bloques y tiempo de concentración.

Se implementan también completar bloque, iniciar siguiente bloque, finalizar y abandonar sesión: todas son operaciones existentes en el diagrama de clases. Tras un bloque completado, el estudiante solicita el siguiente. Concentración y descanso se alternan.

Configuración provisional: 25 / 5 / 15 minutos; descanso largo cada 4 concentraciones. Los 25 minutos los fija CU01; los descansos y la frecuencia son decisiones documentadas porque los diagramas no fijan sus valores.

## Fidelidad al modelo

Las clases de dominio son Estudiante, SesionEstudio, BloquePomodoro y ConfiguracionPomodoro. Las tres enumeraciones conservan los estados del diagrama. Las primeras tres clases son entidades JPA; ConfiguracionPomodoro es un componente embebido, propiedad de la sesión, sin identidad ni tabla independiente.

**Los métodos técnicos adicionales están enumerados explícitamente en [docs/TRAZABILIDAD.md](docs/TRAZABILIDAD.md)**: constructores, getters, validaciones privadas y adaptación HTTP/JPA. Allí se relacionan los casos de uso con vistas, servicios, dominio y repositorio. Los cuadros de secuencia de la imagen están vacíos y no se presentan como secuencias definidas.

No se agrega una entidad de usuario, tarea, estadísticas, autenticación o historial. Se retiró la entidad temporal Verificacion del entorno.

## Compilar y verificar

```powershell
.\mvnw.cmd package
.\mvnw.cmd test
```

El primer comando ejecuta las pruebas y genera `target/GR06_1BT3_622_26B.war`, desplegable en Tomcat 10.1 o un contenedor compatible con Jakarta Servlet 6.

Con el servidor iniciado, prueba HTTP reproducible:

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verificar-web.ps1
```

La prueba utiliza una identidad independiente y crea sesiones técnicas en la base local; no modifica las sesiones de tu navegador. JUnit usa una base H2 en memoria e instantes simulados para verificar los 25 minutos sin cambiar la duración del producto.

## Entorno

- JDK 17 o superior; en este equipo se usa el JDK 25 existente.
- Maven 3.9.11 local en `.tools`, sin modificar el PATH global.
- Jetty 12.0.35, Jakarta Servlet 6 y JSP/JSTL.
- Hibernate ORM 6.6.58.Final / Jakarta Persistence 3.1.
- H2 2.3.232: archivo `data/pomora.mv.db`.
- JUnit Jupiter para pruebas y Git para el control de versiones.

Las sesiones y las pausas se guardan en H2. Una cookie UUID conserva la identidad local del navegador al reiniciar el servidor. Borrar cookies o usar otro navegador crea otra identidad. Esto no constituye autenticación: el modelo no incluye registro/inicio de sesión.

El servidor escucha únicamente en 127.0.0.1. H2 y la actualización automática del esquema están configurados para desarrollo. La cuenta visual requiere JavaScript; las acciones Java se validan en el servidor. El siguiente bloque no se inicia automáticamente. La finalización del bloque se confirma cuando el navegador vuelve a contactar al servidor.

El lanzador usa IPv4 y una ruta inexistente para sockets Unix para activar la alternativa TCP interna del JDK en este Windows. No crees `.tools/sin-sockets-unix`.

## Preparar otra computadora Windows

Instala un JDK 17 o superior y Git. Con conexión a Internet:

```powershell
powershell -ExecutionPolicy Bypass -File .\preparar.ps1
.\iniciar.cmd
```

`mvnw.cmd` es un lanzador local para Windows. `preparar.ps1` descarga Maven y verifica SHA512. Las dependencias se guardan en `.tools/repository`.

## Estructura

- `src/main/java/edu/proyecto/model`: clases y enumeraciones del diagrama.
- `service`: ServicioPomodoro.
- `repository`: interfaz RepositorioSesiones y adaptador JPA.
- `web`: controlador Servlet.
- `config`: ciclo de vida de persistencia.
- `src/main/resources/META-INF/persistence.xml`: configuración ORM.
- `src/main/webapp/WEB-INF/views/pomora.jsp`: pantallas.
- `src/main/webapp/assets`: CSS y JavaScript del contador.
- `src/test/java/edu/proyecto`: pruebas de dominio y persistencia.
- `docs/TRAZABILIDAD.md`: modelos, métodos y decisiones.
- `docs/evidencias`: capturas de funcionamiento.
- `scripts/verificar-web.ps1`: prueba HTTP.

El incremento 2 y el informe PDF final quedan para la siguiente etapa, cuando se disponga de sus modelos.

# Verificación del incremento 1

Fecha de comprobación: 3 de octubre de 2026, hora de Colombia.

## Compilación y pruebas Java

Comando: `mvnw.cmd -o -B package` (después de la descarga inicial de dependencias).

Resultado: **BUILD SUCCESS**, WAR generado y **10 pruebas JUnit aprobadas**, sin fallos, errores ni pruebas omitidas. Los informes reproducibles de Maven están en `target/surefire-reports/`.

Las pruebas cubren:

- Conservación de tiempo en varias pausas, sin contar el intervalo pausado.
- Finalización al instante real de vencimiento, sin acumular tiempo excedente.
- Rechazo de estados inválidos y fechas anteriores al inicio.
- Concentración/descanso, descanso largo después de cuatro concentraciones, exclusión del descanso del tiempo estudiado.
- Finalización y abandono, incluidos bloques parcialmente trabajados.
- Validación de configuración y cambio de nombre según la operación del modelo.
- Guardado y recuperación de una pausa con otra instancia del repositorio JPA.
- Persistencia de composición, enumeraciones y orden descendente del historial.
- Prevención de una segunda sesión activa y separación por estudiante.
- Metamodelo ORM con exactamente tres entidades: Estudiante, SesionEstudio y BloquePomodoro; ConfiguracionPomodoro embebida.

## Prueba HTTP

Comando, con el servidor iniciado: `powershell -ExecutionPolicy Bypass -File scripts/verificar-web.ps1`.

Resultado: **15 comprobaciones aprobadas**: pantalla inicial 25:00, historial vacío, rechazo de CSRF incorrecto, creación de sesión, duración y estado iniciales, rechazo de doble inicio, rechazo de completar prematuramente, pausa, recarga con tiempo idéntico, reanudación, historial JSP con bloque, rechazo de una sesión de otra identidad, finalización, abandono y conservación de estados de sesiones cerradas.

La prueba crea sesiones de prueba bajo una identidad independiente; no modifica las sesiones de la identidad del navegador usado para las capturas.

## Comprobación en navegador

Se comprobó la interfaz real JSP, los botones Iniciar/Pausar/Reanudar y la consulta del historial con expansión del bloque. Se recargó el temporizador pausado y se mantuvo el tiempo restante. También se detuvo y volvió a iniciar Jetty: la cookie del navegador recuperó la misma sesión desde H2, todavía pausada con **24:26** restantes.

Capturas tomadas directamente del navegador, sin modificar la imagen:

- [Temporizador pausado](evidencias/02-temporizador-pausado.jpg).
- [Temporizador reanudado](evidencias/03-temporizador-reanudado.jpg).
- [Historial y detalle de bloques](evidencias/04-historial.jpg).
- [Pausa recuperada después del reinicio](evidencias/05-pausa-tras-reinicio.jpg).

La sesión de demostración se dejó pausada. Las pruebas del ciclo completo usan instantes simulados; no se esperaron 25 minutos reales ni se redujo la duración de la aplicación.

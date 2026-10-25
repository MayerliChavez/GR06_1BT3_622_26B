# HU3 — Recibir notificaciones al cambiar el estado del temporizador

## Comportamiento integrado
Los avisos se generan con el bloque EN_EJECUCION de una SesionEstudio ACTIVA,
verificando que el estudiante autenticado sea su propietario.
Las dos modalidades usan el mismo controlador y formateador:
- 3.1: al comenzar concentración, "Es hora de concentrarse. ¡Mucho éxito!".
- 3.2: al comenzar descanso corto o largo, "¡El descanso ha comenzado! Disfruta tu tiempo".
- 3.3: tarjeta centrada en el área visible de la aplicación, sin modal, sin bloquear clics, sin cambiar el foco
  ni detener el temporizador o el chat; role=status y aria-live=polite.
- 3.4: ocultación a los 12500 ms (12,5 segundos), con limpieza del texto.

El aviso flota dentro de la página de Pomora; no es una notificación del sistema operativo.
La señal sonora existente se conserva y depende del permiso de audio tras interacción.
No se solicita permiso de notificaciones del navegador.

## Flujo y trazabilidad
| Análisis | Diseño / función |
|---|---|
| UI_PanelEstudio | pomora.js, compartido.js, pomora-ui.js y pomora.css |
| ControladorTemporizador | servicios y servlets existentes; confirman el nuevo bloque |
| ControladorNotificacion | tdd/notificaciones/ControladorNotificacion y web/VistaNotificacion |
| FormateadorNotificacion | tdd/notificaciones/FormateadorNotificacion |
| NotificacionFlotante | model/NotificacionFlotante: record transitorio, sin JPA |
| SesionEstudio / Estudiante | entidades existentes; asociación al usuario autenticado |

Los servlets incluyen notificacion en el JSON del estado: id sesión/bloque, mensaje, posición
y duración de visualización, tipo y duración real del bloque. CompartidoServlet selecciona la sesión individual del receptor dentro de la sala.
Sesión nula/cerrada, receptor ajeno, bloque vacío, pausado o completado producen null.
El navegador solo avisa después de confirmar un nuevo bloque; no muestra otro aviso por
pausar/reanudar, consultar el mismo estado o finalizar. El inicio individual también avisa.
Al cargar una sala compartida ya iniciada no se reproduce un aviso histórico.

El identificador es estable para cada sesión y orden de bloque. El cliente guarda los
identificadores consumidos en sessionStorage para evitar duplicados por sondeo y recarga,
con un límite de cien elementos. Si se pierde conexión se avisa del último bloque confirmado
al recuperarla, no de cada bloque histórico omitido. El chat mantiene el foco y la escritura.

## Ciclo TDD y dos pruebas seleccionadas
- Convencional: FormateadorNotificacionTest, mensajes de los tres tipos de bloque.
- Con mock: ControladorNotificacionMockTest, colaborador formateador simulado con Mockito;
  SesionEstudio y Estudiante reales; assertEquals y verify del tipo solicitado.

Se siguió el proyecto tdd de referencia: JUnit 4, given_when_then, Mockito.mock y when.
red.txt conserva dos fallos esperados; green.txt, las dos pruebas satisfactorias.
refactor.txt conserva la ejecución completa posterior al refactor (58 ejecuciones).
Se extrajeron constantes de mensajes, consulta del bloque actual, @Before y @After.
Se mantienen las aserciones originales y el mock; la integración no reemplaza esa evidencia.

## Comprobación adicional
NotificacionesIntegracionTest usa los servicios reales y JPA/H2:
- Individual: concentración, descanso corto, concentración y descanso largo.
- Compartida: los cambios de ambos participantes, con identificadores independientes.
- Validación del receptor, sesión vacía/cerrada y bloque pausado; ausencia de aviso.
- Duración, posición e identificador estable al repetir una consulta.

integracion.txt: 61 ejecuciones, sin fallos ni errores, WAR generado.
Las tres pruebas de integración son adicionales a las dos unitarias seleccionadas para HU3.

scripts/PrepararNotificaciones.java crea una fixture exclusivamente en target/hu3-e2e,
con cuentas ficticias y concentración compartida de 90 segundos, descanso de 25.
Nunca apunta a data/pomora; no cambia los tiempos de producción de la compartida (25/5/15 min).
scripts/verificar-notificaciones.cjs entra en tres contextos Edge independientes:
dos participantes de esa sala (uno móvil) y una sesión individual de un minuto.
Comprueba cambios reales del servidor: mensajes, foco, reloj que sigue contando,
posición centrada, ocultación, ausencia de duplicados, recarga y sesión finalizada.
La fixture y el servidor aislado deben iniciarse justo antes del script para observar ambos cambios.
Evidencia: aceptacion.txt y capturas de concentración/descanso para las tres páginas.

## Ejecutar
    .\mvnw.cmd "-Dtest=FormateadorNotificacionTest,ControladorNotificacionMockTest" test
    .\mvnw.cmd test
    .\mvnw.cmd package
    .\mvnw.cmd jetty:run

Prueba manual individual: concentración y descansos de un minuto; iniciar y esperar cada cambio.
En compartida: dos cuentas emparejadas, esperar la concentración de 25 minutos y el descanso;
ambos estudiantes reciben sus propios avisos sin cerrar la sesión ni refrescar la pantalla.

## Reparto de seis pruebas seleccionadas
| HU | Convencionales | Parametrizadas | Mocks |
|---|---:|---:|---:|
| Chat | 2 | 0 | 0 |
| Rachas | 1 | 1 | 0 |
| Notificaciones | 1 | 0 | 1 |
| Total | 4 | 1 | 1 |


## Ajuste visual y sonoro solicitado
La posición final es CENTRO_APLICACION (sustituye la esquina inferior derecha a petición del usuario).
La tarjeta tiene cabecera y cuerpo como la referencia, sin botones de iniciar, sin fondo modal
ni bloqueo del foco. Mantiene la ocultación automática a los 12,5 segundos.
El centro horizontal se calcula sobre el área visible del workspace, excluyendo el menú lateral;
en móvil se adapta al ancho de pantalla y al cambio de orientación.

El mensaje conserva el texto del formateador. La tarjeta añade un título según el tipo y el
texto de duración usando duracionBloqueMillis del bloque confirmado por el servidor.
Así no confunde los segundos durante los que aparece el aviso con los minutos del bloque.
La individual usa la copia de configuración con la que se inició la sesión; editar preferencias
no cambia el aviso de una sesión que ya está en marcha. En compartida siguen fijos 25/5/15 minutos.

Los sonidos se sintetizan con Web Audio y tienen tres perfiles distintos:
- Concentración: tres notas ascendentes, onda seno, ritmo breve.
- Descanso corto: dos notas descendentes, onda triangular.
- Descanso largo: tres notas más graves descendentes, onda seno, ritmo pausado.
Se conserva el botón de activar/apagar sonido y la interacción que exige el navegador.
No hay avisos del sistema, service worker ni configuración HTTPS adicional.

tarjeta-build.txt contiene la compilación y las 61 ejecuciones satisfactorias tras el cambio.
tarjeta-aceptacion.txt registra la comprobación de posición, ausencia de botones, duración real,
foco, temporizador, ocultación y captura de los perfiles sonoros programados en Web Audio.
La prueba Java cubre todos los tipos en los servicios reales; la del navegador usa cambios reales
de concentración y descanso largo, y comprueba el perfil de descanso corto como componente UI.
## Ilustraciones y fondo oscurecido
Se usan los tres PNG originales proporcionados por el usuario, sin alterar su contenido,
en assets/notificaciones/concentracion.png, descanso-corto.png y descanso-largo.png.
La imagen correspondiente se muestra encima de la tarjeta y se precarga al entrar al temporizador.
La capa completion-backdrop cubre todo el viewport, incluido el menú, con una opacidad suave.
Tiene pointer-events:none: oscurece visualmente pero conserva la escritura y el temporizador.
La capa y la ilustración desaparecen automáticamente con el aviso; no se incorporan botones.
La tarjeta conserva su duración configurada, los tres sonidos y la adaptación móvil.

ilustraciones-build.txt registra el empaquetado del cambio visual (sin repetir pruebas Java,
ya que no cambia lógica de backend). ilustraciones-aceptacion.txt registra la verificación
con navegador de los tres PNG, posición, dimensiones de la capa, ocultación, foco, reloj y móvil.
Las capturas ilustracion-*.png muestran el resultado final. Los descansos se ejercitan aquí
como componente visual; las transiciones de los servicios ya se comprobaron en la integración.
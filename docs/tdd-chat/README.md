# HU1: Chatear en las sesiones de estudio compartidas

## Estado y TDD

Se completaron Red, Green y la reorganización de ChatTest con @Before y @After.
Se conservan dos pruebas unitarias convencionales JUnit 4, sin mocks ni parametrización:

1. Crear un mensaje con su texto, autor y sesión: given_valid_message_when_created_then_preserve_text_author_and_session.
2. Rechazar contenido vacío o compuesto solo por espacios: given_empty_or_spaces_message_when_validated_then_rejected.

red.txt conserva la ejecución real anterior a Green: dos fallos de aserción.
El controlador devolvía null y el validador no realizaba comprobaciones.
Actualmente las dos pruebas pasan. La compilación completa (package) terminó con BUILD SUCCESS: 43 ejecuciones, 0 fallos y 0 errores; evidencia en build-green.txt. La extracción de preparación a @Before y
limpieza a @After es una reorganización de las pruebas; no se presenta como
cuatro refactorizaciones diferentes del código productivo.

## Implementación y relación con los diagramas

- UI_SalaChat: compartido.jsp, chat.js y chat.css, disponible dentro de una sesión.
- ControladorMensaje: ControladorMensajes crea el objeto de datos probado con TDD;
  ServicioChat coordina validación, autorización y registro; ChatSocket recibe y difunde.
- ValidadorContenido: ValidadorMensajes rechaza texto vacío, espacios Unicode y null.
- Mensaje: el record Mensaje conserva texto/autor/sesión; MensajeChat es su representación persistente JPA.
- Estudiante: se obtiene de la sesión autenticada, nunca de datos de autor enviados por el cliente.
- Sesión activa: se usa SesionCompartida, la entidad existente que agrupa las
  dos SesionEstudio individuales. Corresponde a la sala compartida de los diagramas.

Los mensajes se registran en H2 antes de difundirse y tienen autor, fecha y sesión.
Al conectar o reconectar se recuperan los últimos 100 mensajes de esa sesión.
El límite de contenido es 2000 caracteres. La interfaz muestra el nombre del autor,
permite Enter para enviar y Mayús+Enter para una nueva línea.
No inserta contenido del chat como HTML.

## Escenarios de aceptación

| Escenario | Implementación | Verificación |
|---|---|---|
| 1.1 Enviar | Validación, identificación del autor autenticado, registro en H2 y actualización del chat | Mensaje y autor visibles en ambos navegadores; caja del emisor limpia tras confirmar |
| 1.2 Recibir | WebSocket Jakarta /chat/{sesionId} | El compañero recibe el mensaje sin navegación ni actualización manual |
| 1.3 Impedir vacíos | Comprobación local y validación del servidor antes de persistir | Vacío, espacios y espacios Unicode rechazados; no aparecen en ninguno de los chats |
| 1.4 Aislamiento | Pertenencia a sesión activa comprobada al conectar, enviar y difundir | Dos parejas en salas distintas; ninguna recibe mensajes de la otra |

La conexión comprueba sesión autenticada, token CSRF y origen.
El servidor verifica que cada destinatario siga siendo participante activo.
Las salidas, el registro de mensajes y la selección de destinatarios usan el
mismo bloqueo que el servicio de estudio compartido para evitar carreras con salir/desconectar.

## Ejecución

Con JDK 25 y el entorno Maven preparado:

    .\mvnw.cmd -Dtest=ChatTest test
    .\mvnw.cmd package
    .\iniciar.cmd

Después, abrir /compartido con dos cuentas distintas (por ejemplo,
navegadores o perfiles diferentes) y emparejarlas. El chat aparece al tener sesión.

Jetty incluye el soporte Jakarta WebSocket mediante la dependencia del plugin.
Un servidor externo debe soportar Jakarta Servlet 6 y Jakarta WebSocket 2.1.

## Verificación funcional automatizada

scripts/verificar-chat.cjs usa Playwright y Microsoft Edge instalado.
Requiere que el servidor esté iniciado; por defecto apunta a http://127.0.0.1:18081.
BASE_URL y BROWSER_CHANNEL permiten cambiar URL y navegador.
Instalar Playwright en el entorno Node o proporcionar NODE_PATH si ya está disponible.

    node scripts/verificar-chat.cjs

El script crea cuatro cuentas de prueba y dos parejas, verifica los cuatro
escenarios, reconexión, contenido HTML seguro, acceso ajeno, CSRF y revocación
tras salir. Guarda aceptacion.txt y capturas de escritorio y móvil.
Son comprobaciones de aceptación, separadas de los dos métodos unitarios de ChatTest.
La ejecución de verificación se hizo en una base H2 en memoria aislada;
el despliegue normal conserva su base H2 de archivos.

## Distribución académica

Main ya contenía cuatro métodos convencionales en TimerTest,
una prueba parametrizada y una con Mockito. Se conservaron.
Los dos tests de chat son convencionales adicionales; el equipo debe seleccionar
y relacionar las seis pruebas que presentará con sus tres historias.

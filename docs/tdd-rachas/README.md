# HU2 — Mantener una racha de estudio

Prioridad 2; estimación 3.

## Regla y corrección
Una única racha por estudiante combina concentración individual y compartida.
Un día se acredita al COMPLETAR un bloque de CONCENTRACION. No hace falta finalizar la sesión
contenedora ni salir de la sala. El descanso, la sesión aún abierta o una desconexión posterior
no borran la concentración ya completada. Un bloque cancelado o incompleto no suma.

La versión inicial exigía estado FINALIZADA: dos participantes podían ver contadores distintos
si uno salía y el otro seguía estudiando. Se corrigieron ambos repositorios y la reconstrucción.
El resumen se calcula a partir de las fechas reales de los bloques completados; el historial
existente se recupera automáticamente sin inventar días. Incluso una sesión que atraviesa
varios días acredita cada fecha con concentración completa.

Zona: America/Bogota, igual que el resto de Pomora. Cada fecha cuenta una sola vez.
2.1: inicia en 1. 2.2: incrementa en días consecutivos. 2.3: mantiene al repetir el día.
2.4: reinicia en 1 tras faltar un día. Si la última fecha fue anterior a ayer, se muestra 0.

## Calendario
Se muestra en temporizador, historial y búsqueda de compañero. Se oculta durante la sesión
compartida activa para dejar espacio al chat. Marca días completados, hoy y días futuros;
permite navegar a meses anteriores, volver a hoy y consultar el detalle de cada fecha.
Se actualiza automáticamente y al recuperar visibilidad/conexión. Las fechas proceden del servidor,
no del reloj del navegador. /racha?formato=json requiere autenticación y usa solo el estudiante
de la sesión HTTP; no acepta IDs de otros estudiantes.

## Trazabilidad
| Análisis | Diseño |
|---|---|
| UI_PanelEstudio | pomora.jsp, compartido.jsp, racha.jspf, racha.js, pomora.css |
| ControladorRacha | ControladorRacha, VistaRacha, RachaServlet; integración en ambos servlets |
| EvaluadorRachas | EvaluadorRachas |
| RachaEstudio | RachaEstudio, RepositorioRachasJpa |
| SesionEstudio / Estudiante | Entidades existentes, bloques y asociación autenticada |
| Persistencia individual / compartida | RepositorioSesionesJpa, RepositorioSesionesCompartidasJpa |

RachaEstudio conserva días y última fecha en H2. El guardado del bloque y de su racha comparte
transacción. El resumen usa fechas únicas ordenadas: reintentar un guardado no duplica días.
La reconstrucción protege también contra resúmenes antiguos de la versión inicial.

## Evidencias TDD originales
Las dos pruebas seleccionadas siguen siendo:
- ControladorRachaTest: convencional, sesión sin concentración completa no cambia el contador.
- EvaluadorRachasParametersTest: parametrizada con seis filas, cuatro escenarios y cambio de mes/año.

JUnit 4 y formato del proyecto de referencia: given_when_then, @Before, @After,
@RunWith(Parameterized.class), Iterable<Object[]> y constructor con parámetros.
Dos métodos seleccionados; siete ejecuciones; sin mocks en HU2.
red.txt, green.txt y refactor.txt conservan la evidencia original de cada fase.
En el refactor se extrajo la preparación de pruebas y los métodos privados del evaluador.

## Verificación de la integración actual
RachasIntegracionTest contiene seis comprobaciones con JPA/H2 y los servicios reales:
- Cuatro escenarios, guardado y recuperación desde otro EntityManager.
- Bloques incompletos no suman; abandonar después de completar conserva el día.
- Mezcla individual/compartida sin duplicar y con progreso separado por participante.
- Cambio de día local distinto de UTC.
- Ambos participantes acreditados antes de salir; desconexión conserva progreso.
- Una misma sesión con concentración completada en dos días distintos.

calendario-build.txt: 56 ejecuciones, cero fallos y errores; WAR generado.
Estas pruebas de integración se distinguen de las dos unitarias seleccionadas para el reparto.

scripts/verificar-rachas.cjs usa Edge y una base aislada en puerto 18082:
completa un Pomodoro real de un minuto, comprueba crédito automático antes de finalizar,
calendario, fechas, meses, vista móvil, autenticación y ausencia del calendario en el chat activo.
Evidencia: calendario-aceptacion.txt y capturas racha-desktop.png, racha-movil.png,
chat-sin-calendario.png.

## Ejecución
Desde la raíz:
    .\mvnw.cmd test
    .\mvnw.cmd package
    .\mvnw.cmd jetty:run

Para probar: concentración individual de un minuto, iniciar y esperar el descanso.
El contador y el día del calendario se marcan sin finalizar. La compartida usa concentración
de 25 minutos; al completarse, ambos participantes reciben su día.

## Reparto de las seis pruebas seleccionadas
| Historia | Convencionales | Parametrizadas | Mocks |
|---|---:|---:|---:|
| HU1 chat | 2 | 0 | 0 |
| HU2 rachas | 1 | 1 | 0 |
| HU3 pendiente | 1 | 0 | 1 |
| Total | 4 | 1 | 1 |


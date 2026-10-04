# Incremento 1 — Pomodoro individual

Fuente: los dos diagramas JPG suministrados por el usuario. No se implementa el incremento 2. Los cuadros de secuencia visibles en la primera imagen están vacíos: no se les atribuyen mensajes ni métodos que no contengan. La implementación se deriva del diagrama de clases y de los flujos de actividad y robustez.

Copias de referencia: [casos de uso, actividades y robustez](modelos/incremento1-casos-actividades-robustez.jpg) y [diagrama de clases](modelos/incremento1-clases.jpg).

## Correspondencia con los cuatro casos de uso

| Caso | Vista / acción | Servicio | Dominio | Persistencia |
|---|---|---|---|---|
| CU01 Iniciar temporizador | `/pomodoro`, Iniciar temporizador; muestra 25:00 antes de iniciar | `iniciarSesion(UUID, ConfiguracionPomodoro, Instant)` | Construcción de `SesionEstudio`, `iniciarSiguienteBloque(Instant)`; sesión ACTIVA y bloque EN_EJECUCION | `guardar(SesionEstudio)` con composición de configuración y bloques |
| CU02 Pausar temporizador | Botón Pausar; contador congelado | `pausarTemporizador(UUID, Instant)` | `SesionEstudio.pausar` → `BloquePomodoro.pausar`; acumula tramo activo y pasa a PAUSADO | Se guarda el estado y el tiempo acumulado |
| CU03 Reanudar temporizador | Botón Reanudar; continúa el tiempo conservado | `reanudarTemporizador(UUID, Instant)` | `SesionEstudio.reanudar` → `BloquePomodoro.reanudar`; nuevo inicio de tramo, EN_EJECUCION | Se guarda el nuevo tramo sin sumar la pausa |
| CU04 Consultar historial | `/historial`, tabla de sesiones y detalle de sus bloques | `consultarHistorial(UUID)` | `tiempoEstudiado(Instant)` suma solo concentración | `buscarPorEstudiante(UUID)`, orden descendente de fecha |

La pantalla de inicio/temporizador activo/temporizador pausado del diagrama de robustez se implementa como estados de la misma JSP. La pantalla de historial y su tabla se encuentran en esa JSP con contenido diferente según la ruta. El control corresponde a `PomodoroServlet` y `ServicioPomodoro`; las entidades corresponden al modelo.

## Operaciones que ya aparecen en el diagrama de clases

- `Estudiante`: `cambiarNombreVisible(String)`.
- `ConfiguracionPomodoro`: constructor con las cuatro propiedades, `duracionPara(TipoBloque)`, `tipoDescansoTras(int)`.
- `BloquePomodoro`: `pausar`, `reanudar`, `completar`, `cancelar`, `tiempoActivo`, `tiempoRestante`, todos con `Instant`.
- `SesionEstudio`: `iniciarSiguienteBloque`, `pausar`, `reanudar`, `completarBloqueActual`, `tiempoRestanteBloqueActual`, `finalizar`, `abandonar`, `tiempoEstudiado`, todos con `Instant`.
- `RepositorioSesiones`: `guardar`, `buscarPorId`, `buscarPorEstudiante`, `buscarActivaPorEstudiante`, con los tipos del diagrama.
- `ServicioPomodoro`: constructor con `RepositorioSesiones`, `iniciarSesion`, `pausarTemporizador`, `reanudarTemporizador`, `completarBloqueActual`, `iniciarSiguienteBloque`, `consultarTiempoRestante`, `finalizarSesion`, `abandonarSesion`, `consultarHistorial`, conservando parámetros y retornos del diagrama.

No se añadieron operaciones públicas de negocio fuera de esa lista. Los métodos de finalizar/abandonar/siguiente bloque del modelo se ofrecen como acciones secundarias. La finalización natural de un bloque invoca `completarBloqueActual`; el siguiente bloque empieza solo cuando el estudiante lo solicita. `cambiarNombreVisible` está implementado en dominio, sin añadir una pantalla de perfil o un caso de uso de edición de nombre.

## Métodos adicionales necesarios, declarados explícitamente

Estos métodos NO están dibujados. Son construcción, lectura o integración técnica; se enumeran para que se puedan revisar y, si se requiere, incorporar posteriormente al diagrama.

| Clase / archivo | Métodos adicionales exactos | Motivo |
|---|---|---|
| `Estudiante` | `protected Estudiante()`, `Estudiante(UUID, String)`, `getId()`, `getNombreVisible()` | JPA, creación de la identidad local y lectura sin setters |
| `ConfiguracionPomodoro` | `protected ConfiguracionPomodoro()` | Construcción por JPA; el constructor público ya está en el diagrama |
| `BloquePomodoro` | `protected BloquePomodoro()`, `BloquePomodoro(int, TipoBloque, Duration, Instant)` | JPA y construcción del bloque en ejecución |
| `BloquePomodoro` | `getId()`, `getOrden()`, `getTipo()`, `getDuracionObjetivo()`, `getEstado()`, `getFechaInicio()`, `getFechaFin()` | Leer propiedades para el ciclo, el historial y la presentación |
| `BloquePomodoro` | `private validarAhora(Instant)` | Reutilizar la validación temporal, sin cambiar la API de negocio |
| `SesionEstudio` | `protected SesionEstudio()`, `SesionEstudio(Estudiante, ConfiguracionPomodoro, Instant)` | JPA y construcción respetando las relaciones del diagrama |
| `SesionEstudio` | `getId()`, `getFechaInicio()`, `getFechaFin()`, `getEstado()`, `getEstudiante()`, `getBloques()` | Lectura de la sesión; la lista se devuelve sin permitir modificaciones |
| `SesionEstudio` | `private bloqueActual()`, `private exigirActiva(Instant)` | Seleccionar el último bloque y validar la sesión sin duplicar reglas |
| `RepositorioSesionesJpa` | constructor `RepositorioSesionesJpa(EntityManagerFactory)` | Adaptador concreto de la interfaz del diagrama; sus otros cuatro métodos son implementaciones de la interfaz |
| `PomodoroServlet` | `doGet(HttpServletRequest, HttpServletResponse)`, `doPost(HttpServletRequest, HttpServletResponse)` | Entrada del protocolo HTTP requerida por Servlet |
| `PomodoroServlet` | `private identificarEstudiante(HttpServletRequest, HttpServletResponse)`, `private tokenCsrf(HttpServletRequest)`, `private prepararVista(HttpServletRequest, UUID)`, `private escribirEstado(HttpServletRequest, HttpServletResponse)`, `private formatearTiempo(Duration)` | Identidad local, protección de formularios, preparación de datos, respuesta para sincronizar el contador y formato visual |
| `PersistenceListener` | `contextInitialized(ServletContextEvent)`, `contextDestroyed(ServletContextEvent)` | Abrir/cerrar JPA y registrar servicio/repositorio durante la vida de la aplicación |
| `pomora.js` | `render()`, `apply(next)`, `sync()`, `mutate(command)` | Presentación de la cuenta y llamadas a los métodos Java; no son métodos de entidades |

Las clases de pruebas contienen métodos de verificación, no operaciones del producto.

## Entidades y relaciones ORM

En el alcance original del incremento 1, `Estudiante`, `SesionEstudio` y `BloquePomodoro` llevan `@Entity`; el incremento 2 y la ampliación autorizada añaden las entidades detalladas en INCREMENTO2.md. `ConfiguracionPomodoro` es `@Embeddable`: su ciclo de vida pertenece a la sesión, como exige la composición. No posee id ni tabla independiente. Las tres enumeraciones conservan los valores exactos del diagrama.

Los campos `estudiante`, `configuracion` y `bloques` en `SesionEstudio` materializan las relaciones dibujadas. No se han añadido atributos de negocio independientes. La composición de bloques se guarda mediante cascada y clave foránea; no se añade una entidad intermedia. `RepositorioSesionesJpa`, `PomodoroServlet` y `PersistenceListener` son infraestructura, sin `@Entity`.

Se retiró `Verificacion`, la entidad temporal de prueba del entorno. La aplicación usa `data/pomora.mv.db`; la base anterior `data/tarea3.mv.db` no se usa ni se borró.

## Decisiones donde los diagramas no fijan el detalle

- CU01 fija 25 minutos de concentración. Se toman 5 minutos de descanso corto, 15 de largo y 4 concentraciones antes de descanso largo como configuración provisional. No se agregó un caso de uso de personalización.
- Al terminar un bloque, se completa y queda esperando el inicio del siguiente. Tras concentración corresponde descanso; tras descanso corresponde concentración.
- Una sesión sigue ACTIVA cuando su bloque está PAUSADO. Nunca se añade PAUSADA a `EstadoSesion`.
- `finalizar` cierra la sesión como FINALIZADA; si hay un bloque con tiempo pendiente se cancela, conservando el trabajo realizado. `abandonar` la cierra como ABANDONADA. Solo los bloques de concentración contribuyen a `tiempoEstudiado`, incluidos los minutos efectivamente trabajados en bloques cancelados.
- El tiempo nunca supera la duración objetivo; completar registra el instante efectivo de vencimiento, aunque la notificación del navegador llegue después.
- La identidad anónima del incremento 1 inicial fue sustituida por la cuenta autenticada del incremento 2. El historial pertenece al Estudiante asociado a CuentaUsuario y puede recuperarse desde otro navegador al ingresar; los registros anónimos anteriores permanecen sin asignarse a nuevas cuentas.
- Los POST validan un token CSRF y que la sesión corresponda a la identidad del navegador. El servicio serializa las modificaciones en esta única instancia local para impedir dobles inicios entre pestañas. Una instalación con varios servidores necesitaría otra estrategia de concurrencia.
- El navegador consulta el estado cada 3 segundos; al recargar o regresar a una pestaña vuelve a leer lo guardado. La cuenta visual se calcula con un reloj monotónico. La confirmación y el guardado de la finalización de un bloque se realizan al contactar con el servidor; sin conexión no se inventan transiciones guardadas.

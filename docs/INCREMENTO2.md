# Incremento 2: estudio compartido

## Estado de implementación

Se implementaron CU01 (registro), CU02 (inicio de sesión) y CU03 (emparejamiento). El usuario autorizó explícitamente completar los dos primeros casos de uso al solicitar la pantalla de entrada y la identificación real del compañero. La ampliación agrega únicamente CuentaUsuario (id, correo único, hashContrasena y relación con Estudiante) y ServicioCuentas con las operaciones registrar(String nombre, String correo, String contrasena): CuentaUsuario y autenticar(String correo, String contrasena): Optional<CuentaUsuario>. Estas dos operaciones adicionales no figuran en el diagrama de clases original y se documentan expresamente; satisfacen las actividades y la entidad CuentaUsuario del diagrama de robustez.

Las rutas /, /ingresar y /registro muestran la entrada y los formularios. Registro valida datos y correo único, guarda la cuenta y muestra confirmación para ingresar. Autenticación consulta por correo normalizado y verifica el hash. Al ingresar se crea una sesión HTTP nueva; la identidad ya no procede de una cookie con UUID anónimo. Las pantallas de estudio están protegidas por AccesoFilter y muestran el nombre de la cuenta. Cerrar sesión termina la participación compartida y revoca la sesión HTTP; es una acción técnica de salida de la interfaz, no una operación nueva de las clases originales.

Las contraseñas se guardan con PBKDF2-HMAC-SHA256, 600000 iteraciones y una sal aleatoria de 16 bytes por cuenta, usando las APIs del JDK. Referencia: [OWASP Password Storage](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html). No se guardan contraseñas en texto plano. Los constructores, getters, validación, derivación de hash y adaptación Servlet/JPA son detalles técnicos.

Los historiales antiguos anónimos se conservan en la base de datos y no se asignan automáticamente a cuentas nuevas. La configuración Radmin sigue disponible con iniciar-radmin.ps1.

## Trazabilidad

| Modelo / actividad | Código |
|---|---|
| Registrar solicitud, seleccionar aleatoriamente otro disponible y retirar ambas solicitudes | Emparejador.buscarOEsperar; implementación técnica EmparejadorEnMemoria, sin entidad adicional |
| Crear emparejamiento e iniciar estudio | ServicioEstudioCompartido.solicitarEmparejamiento; SesionCompartida.iniciar; dos SesionEstudio y dos ParticipacionCompartida |
| Mostrar búsqueda y compañero encontrado | CompartidoServlet, compartido.jsp y compartido.js; consulta periódica cada 3 segundos |
| Tiempo común y cambios de bloque | ServicioEstudioCompartido.actualizarSesion; SesionCompartida.avanzarSiCorresponde e iniciarBloquesSiguientes |
| Consultar tiempo restante | ServicioEstudioCompartido.consultarTiempoRestante; SesionCompartida.tiempoRestanteBloqueActual |
| Salida voluntaria / desconexión | salir / registrarDesconexion; registrarSalida en sesión y participación; MotivoSalida |
| Persistencia del estudio y del historial individual | RepositorioSesionesCompartidasJpa guarda la composición en una transacción JPA; las sesiones individuales siguen consultables en RepositorioSesiones |

## Auditoría funcional del segundo diagrama

Las operaciones del primer incremento se conservan. Se incorporaron exactamente las siguientes 19 operaciones dibujadas, con sus tipos de parámetros, retorno y visibilidad. Los constructores técnicos, getters, adaptación Servlet/JPA, detector de presencia y validaciones no agregan operaciones funcionales al modelo.

| Clase / interfaz | Métodos del diagrama implementados |
|---|---|
| Emparejador | buscarOEsperar(UUID): Optional<UUID>; cancelarEspera(UUID): void |
| RepositorioSesionesCompartidas | guardar(SesionCompartida): void; buscarPorId(UUID): Optional<SesionCompartida>; buscarAbiertaPorEstudiante(UUID): Optional<SesionCompartida> |
| ServicioEstudioCompartido | solicitarEmparejamiento(UUID, Instant): Optional<SesionCompartida>; cancelarEspera(UUID): void; actualizarSesion(UUID, Instant): void; salir(UUID, UUID, Instant): void; registrarDesconexion(UUID, UUID, Instant): void; consultarTiempoRestante(UUID, Instant): Optional<Duration> |
| SesionCompartida | iniciar(Instant): void; avanzarSiCorresponde(Instant): void; registrarSalida(UUID, MotivoSalida, Instant): void; tiempoRestanteBloqueActual(Instant): Optional<Duration>; privados iniciarBloquesSiguientes(Instant): void y finalizar(Instant): void |
| ParticipacionCompartida | estaPresente(): boolean; registrarSalida(MotivoSalida, Instant): void |

Entidades nuevas del segundo diagrama: únicamente SesionCompartida y ParticipacionCompartida. Enumeraciones: EstadoSesionCompartida (ACTIVA, CERRADA, ABANDONADA) y MotivoSalida (VOLUNTARIA, DESCONEXION). `bool` y `string` del UML se traducen a `boolean` y `String` en Java.

## Decisiones de funcionamiento

Los dos temporizadores usan el mismo instante inicial y la configuración 25/5/15, con descanso largo cada 4 concentraciones. Los bloques siguientes avanzan automáticamente desde el instante de vencimiento, sin desplazar el ritmo por retrasos de las consultas. La pantalla individual impide pausar o modificar una sesión compartida.

El diagrama no define el destino del compañero tras una salida: se permite continuar y se muestra el motivo de salida. La salida voluntaria finaliza la sesión individual; la desconexión la abandona. La compartida se cierra cuando nadie sigue presente y queda ABANDONADA si ambas salidas fueron por desconexión; en los demás casos CERRADA.

El detector técnico considera desconexión tras 90 segundos sin consultas y revisa cada 10 segundos. Tras reiniciar el servidor concede una nueva ventana de 90 segundos a las participaciones persistidas abiertas. La cola es temporal; una pantalla de búsqueda activa vuelve a solicitar emparejamiento tras un reinicio. Mantener el navegador abierto y con conexión permite recibir actualizaciones; la suspensión prolongada del dispositivo puede causar desconexión.

Estas decisiones completan aspectos no especificados por las actividades, sin agregar entidades o métodos de negocio a las clases originales. La ampliación autorizada de cuentas se enumera al inicio.

## Verificación

Ejecutar `.\mvnw.cmd test` para dominio y JPA, y `.\scripts\verificar-compartido.ps1` con el servidor activo para verificar dos identidades independientes, espera, cancelación, sincronización, protección CSRF, salida, desconexión e historial. Las pruebas de dominio usan tiempos controlados cortos; la aplicación conserva los 25 minutos reales.

Diagramas originales: `modelos/incremento2-clases.jpg` y `modelos/incremento2-casos-actividades-robustez.jpg`. Evidencias de pantalla: `evidencias/06-estudio-compartido.jpg` y `evidencias/07-busqueda-companero.jpg`.

## Verificación del acceso y los nombres

Resultado: 18 pruebas Java y 34 comprobaciones HTTP aprobadas. CuentasTest comprueba persistencia, correos únicos normalizados, validación de contraseña, hash con sal distinta, autenticación correcta/incorrecta y conservación de nombres en el primer emparejamiento y posteriores guardados. scripts/verificar-acceso.ps1 comprueba pantalla inicial, acceso restringido, CSRF, nombre y cierre de sesión. Los scripts de ambos incrementos crean cuentas de prueba distintas antes de probar los flujos.

Evidencias adicionales: evidencias/08-inicio-sesion.jpg y evidencias/09-registro.jpg. El esquema amplía la base de datos existente mediante JPA, sin eliminar tablas o historiales.

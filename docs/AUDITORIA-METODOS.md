# Auditoría estricta de métodos contra el diagrama

## Criterio funcional aclarado por el usuario

El usuario precisó que la igualdad se refiere a los métodos funcionales, excluyendo getters/setters y detalles técnicos de construcción e infraestructura. Bajo este criterio, **los 30 métodos funcionales del diagrama coinciden con el código: no falta ninguno y no se añadió ninguna operación de negocio**. Los 3 auxiliares privados reutilizan validaciones y la selección del bloque actual; no agregan funciones al modelo. Los 2 constructores dibujados también están implementados.

El inventario que sigue conserva la revisión literal anterior para mostrar con transparencia las declaraciones técnicas adicionales. Su resultado literal no contradice la conformidad funcional conforme al alcance aclarado.

## Inventario literal de declaraciones

Referencia: diagrama de clases original `docs/modelos/incremento1-clases.jpg`. Comparación del código fuente y corroboración de las firmas compiladas con javap. Se incluyen métodos públicos, privados y constructores explícitos. No se cuentan métodos sintéticos, heredados o generados automáticamente por Java.

**Resultado: NO existe igualdad estricta.** Las 32 operaciones dibujadas (30 métodos y 2 constructores) están presentes, con los nombres, tipos de parámetros, nombres de parámetros y retornos indicados. Hay 25 declaraciones adicionales en esas mismas clases: 18 métodos y 7 constructores. No hay operaciones dibujadas faltantes.

| Clase | Operaciones del diagrama | Declaraciones del código | Adicionales | Faltantes |
|---|---:|---:|---:|---:|
| Estudiante | 1 | 5 | 4 | 0 |
| ConfiguracionPomodoro | 3 | 4 | 1 | 0 |
| BloquePomodoro | 6 | 16 | 10 | 0 |
| SesionEstudio | 8 | 18 | 10 | 0 |
| RepositorioSesiones | 4 | 4 | 0 | 0 |
| ServicioPomodoro | 10 | 10 | 0 | 0 |

## Firmas del diagrama verificadas

| Clase | Retorno | Operación y parámetros | Comparación |
|---|---|---|---|
| Estudiante | `void` | `cambiarNombreVisible(String nuevoNombre)` | Coincide |
| ConfiguracionPomodoro | `constructor` | `ConfiguracionPomodoro(Duration concentracion, Duration descansoCorto, Duration descansoLargo, int bloquesAntesDescansoLargo)` | Coincide |
| ConfiguracionPomodoro | `Duration` | `duracionPara(TipoBloque tipo)` | Coincide |
| ConfiguracionPomodoro | `TipoBloque` | `tipoDescansoTras(int concentracionesCompletadas)` | Coincide |
| BloquePomodoro | `void` | `pausar(Instant ahora)` | Coincide |
| BloquePomodoro | `void` | `reanudar(Instant ahora)` | Coincide |
| BloquePomodoro | `void` | `completar(Instant ahora)` | Coincide |
| BloquePomodoro | `void` | `cancelar(Instant ahora)` | Coincide |
| BloquePomodoro | `Duration` | `tiempoActivo(Instant ahora)` | Coincide |
| BloquePomodoro | `Duration` | `tiempoRestante(Instant ahora)` | Coincide |
| SesionEstudio | `BloquePomodoro` | `iniciarSiguienteBloque(Instant ahora)` | Coincide |
| SesionEstudio | `void` | `pausar(Instant ahora)` | Coincide |
| SesionEstudio | `void` | `reanudar(Instant ahora)` | Coincide |
| SesionEstudio | `void` | `completarBloqueActual(Instant ahora)` | Coincide |
| SesionEstudio | `Optional<Duration>` | `tiempoRestanteBloqueActual(Instant ahora)` | Coincide |
| SesionEstudio | `void` | `finalizar(Instant ahora)` | Coincide |
| SesionEstudio | `void` | `abandonar(Instant ahora)` | Coincide |
| SesionEstudio | `Duration` | `tiempoEstudiado(Instant ahora)` | Coincide |
| RepositorioSesiones | `void` | `guardar(SesionEstudio sesion)` | Coincide |
| RepositorioSesiones | `Optional<SesionEstudio>` | `buscarPorId(UUID id)` | Coincide |
| RepositorioSesiones | `List<SesionEstudio>` | `buscarPorEstudiante(UUID estudianteId)` | Coincide |
| RepositorioSesiones | `Optional<SesionEstudio>` | `buscarActivaPorEstudiante(UUID estudianteId)` | Coincide |
| ServicioPomodoro | `constructor` | `ServicioPomodoro(RepositorioSesiones repositorioSesiones)` | Coincide |
| ServicioPomodoro | `SesionEstudio` | `iniciarSesion(UUID estudianteId, ConfiguracionPomodoro configuracion, Instant ahora)` | Coincide |
| ServicioPomodoro | `void` | `pausarTemporizador(UUID sesionId, Instant ahora)` | Coincide |
| ServicioPomodoro | `void` | `reanudarTemporizador(UUID sesionId, Instant ahora)` | Coincide |
| ServicioPomodoro | `void` | `completarBloqueActual(UUID sesionId, Instant ahora)` | Coincide |
| ServicioPomodoro | `BloquePomodoro` | `iniciarSiguienteBloque(UUID sesionId, Instant ahora)` | Coincide |
| ServicioPomodoro | `Optional<Duration>` | `consultarTiempoRestante(UUID sesionId, Instant ahora)` | Coincide |
| ServicioPomodoro | `void` | `finalizarSesion(UUID sesionId, Instant ahora)` | Coincide |
| ServicioPomodoro | `void` | `abandonarSesion(UUID sesionId, Instant ahora)` | Coincide |
| ServicioPomodoro | `List<SesionEstudio>` | `consultarHistorial(UUID estudianteId)` | Coincide |

## Declaraciones que sobran bajo el criterio literal

| Clase | Visibilidad | Retorno | Declaración adicional |
|---|---|---|---|
| Estudiante | protected | `constructor` | `Estudiante()` |
| Estudiante | public | `constructor` | `Estudiante(UUID id, String nombreVisible)` |
| Estudiante | public | `UUID` | `getId()` |
| Estudiante | public | `String` | `getNombreVisible()` |
| ConfiguracionPomodoro | protected | `constructor` | `ConfiguracionPomodoro()` |
| BloquePomodoro | protected | `constructor` | `BloquePomodoro()` |
| BloquePomodoro | public | `constructor` | `BloquePomodoro(int orden, TipoBloque tipo, Duration duracionObjetivo, Instant ahora)` |
| BloquePomodoro | private | `void` | `validarAhora(Instant ahora)` |
| BloquePomodoro | public | `UUID` | `getId()` |
| BloquePomodoro | public | `int` | `getOrden()` |
| BloquePomodoro | public | `TipoBloque` | `getTipo()` |
| BloquePomodoro | public | `Duration` | `getDuracionObjetivo()` |
| BloquePomodoro | public | `EstadoBloque` | `getEstado()` |
| BloquePomodoro | public | `Instant` | `getFechaInicio()` |
| BloquePomodoro | public | `Instant` | `getFechaFin()` |
| SesionEstudio | protected | `constructor` | `SesionEstudio()` |
| SesionEstudio | public | `constructor` | `SesionEstudio(Estudiante estudiante, ConfiguracionPomodoro configuracion, Instant ahora)` |
| SesionEstudio | private | `BloquePomodoro` | `bloqueActual()` |
| SesionEstudio | private | `void` | `exigirActiva(Instant ahora)` |
| SesionEstudio | public | `UUID` | `getId()` |
| SesionEstudio | public | `Instant` | `getFechaInicio()` |
| SesionEstudio | public | `Instant` | `getFechaFin()` |
| SesionEstudio | public | `EstadoSesion` | `getEstado()` |
| SesionEstudio | public | `Estudiante` | `getEstudiante()` |
| SesionEstudio | public | `List<BloquePomodoro>` | `getBloques()` |

## Infraestructura fuera del diagrama

Además existen tres clases técnicas: PomodoroServlet, PersistenceListener y RepositorioSesionesJpa. Declaran 13 métodos y 1 constructor explícito: 7 métodos HTTP/presentación/identidad, 2 métodos del ciclo de vida y 4 implementaciones de las operaciones del repositorio. No son entidades, pero tampoco están dibujadas. El JavaScript declara render, apply, sync y mutate. Las clases y métodos de pruebas pertenecen a verificación, no al modelo.

## Atributos, relaciones y enumeraciones

Los atributos nombrados y sus tipos coinciden con el modelo: Estudiante (2), ConfiguracionPomodoro (4), BloquePomodoro (9), SesionEstudio (4), ServicioPomodoro (1). En SesionEstudio hay además tres campos que implementan las relaciones dibujadas: estudiante, configuracion y bloques. No se añadieron entidades ajenas al modelo.

Las enumeraciones coinciden: EstadoSesion = ACTIVA, FINALIZADA, ABANDONADA; EstadoBloque = EN_EJECUCION, PAUSADO, COMPLETADO, CANCELADO; TipoBloque = CONCENTRACION, DESCANSO_CORTO, DESCANSO_LARGO.

El diagrama no especifica multiplicidades ni todos los detalles de ejecución. Las duraciones de descanso 5/15 y frecuencia 4 son decisiones provisionales, no valores derivados del dibujo. Los diagramas de secuencia adjuntos están vacíos, por lo que no se puede certificar correspondencia con mensajes de secuencia no definidos.

## Alcance de esta revisión

Esta auditoría no modifica la aplicación ni el diagrama. Los métodos adicionales siguen presentes. La lista explica la diferencia; documentarlos no hace que el código cumpla el criterio de igualdad estricta. Los auxiliares privados se pueden integrar dentro de las operaciones modeladas; los getters y constructores requieren adaptar sus consumidores. Los constructores sin argumentos corresponden al contrato de instanciación de JPA; Servlet y la implementación concreta del repositorio necesitan infraestructura técnica fuera del dibujo.

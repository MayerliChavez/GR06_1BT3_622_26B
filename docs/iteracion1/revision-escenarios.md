# Revisión de escenarios de la iteración 1

Fecha: 8 de noviembre de 2026.
Base: `origin/Develop`, commit `cea041c42d09cce8a810b8b924766fa83f8e9d7f`, descargado antes de los cambios.
Rama de trabajo: `fix/escenarios-iteracion1`.

Se contrastaron las seis historias comprometidas con el documento entregado y se corrigieron los recorridos señalados. El Word original se conserva sin modificaciones. HU03b, HU05 y HU07 están planificadas en el documento para la iteración 2.

| Historia y escenarios | Cambio y comprobación |
| --- | --- |
| HU01 3.1 | Crear sala disponible en el temporizador individual. El servidor muestra el aviso de sesión en curso y conserva la sesión. Verificado por HTTP. |
| HU01 3.2 | Creación después del cierre verificada por HTTP; la nueva sala tiene un código nuevo en la prueba. |
| HU04 3.1 y 3.2 | Formulario de ingreso disponible mientras hay una solicitud pendiente. Repetir el código conserva la solicitud y muestra el aviso; otro código muestra el aviso de cancelación previa. Verificado por HTTP y persistencia. |
| HU03a 2.1 y 2.2 | Confirmación con el botón Sí, cerrar y la pregunta del documento. Renderizado JSP y cierre sin estudio verificados por HTTP; persistencia del cierre y tiempos cubierta por las pruebas Java. |
| HU02 1.2 | Se conserva el rechazo de solicitudes cuando la sala ya tiene cuatro participantes. No llega una solicitud nueva al responsable. La redacción corregida se entrega en el chat. |
| HU02 3.1 y 3.2 | Todos los participantes ven la sala de espera y sus actualizaciones por sondeo cada tres segundos. Solo el responsable ve Aceptar y Rechazar. El servidor sigue rechazando decisiones de participantes y lecturas de personas ajenas a la sala. Cancelar retira únicamente la solicitud correspondiente. |
| HU08 2.1 | Aviso exacto: Escribe un mensaje antes de enviarlo. |
| HU08 2.2 | Se permite escribir más de 2000 caracteres. El envío se bloquea con el aviso exacto, sin truncar el borrador. |
| HU08 2.3 | Un mensaje de 2000 caracteres se envía completo. Verificado en el controlador JavaScript y en persistencia. |
| HU06 y HU08 3 | Pruebas existentes verifican fases y transiciones del ciclo. El controlador del chat bloquea el envío en concentración y lo reactiva en descanso. |

## Validación

- `mvn package`: 158 pruebas, sin fallos, errores ni omisiones; WAR generado.
- `scripts/verificar-escenarios-iteracion1.ps1`: 27 comprobaciones HTTP satisfactorias.
- `scripts/verificar-salas.ps1`: 16 comprobaciones satisfactorias.
- `scripts/verificar-decisiones-sala.ps1`: 24 comprobaciones satisfactorias.
- `scripts/verificar-ciclo-sala.ps1`: 23 comprobaciones satisfactorias, incluyendo WebSocket, reconexión y permisos.
- `node scripts/verificar-chat-sala-validacion.cjs`: mensajes vacíos y espacios, 2001 y 2000 caracteres, confirmación y bloqueo/reactivación por fase.
- `git diff --check`: sin errores.

Las pruebas HTTP se ejecutaron en `127.0.0.1:18089` con H2 en memoria y cuentas sintéticas. Las verificaciones JavaScript usan eventos DOM y transporte controlados; no sustituyen una prueba visual manual en navegador. El sondeo de la sala de espera se verificó por su endpoint y por su inclusión en las vistas, sin automatización visual del navegador.

Para repetir las pruebas HTTP, iniciar una instancia de pruebas y pasar su URL mediante `-BaseUrl`. No ejecutarlas contra una base con datos de uso real.

## Chat del estudio por match

El campo y el botón de envío se desactivan en concentración y se habilitan en los descansos cortos y largos. Se conserva el borrador y el historial. El servidor también rechaza mensajes durante concentración y calcula la fase vigente aunque no haya llegado el siguiente sondeo del temporizador.

Validación posterior: `mvn test` pasó 159 pruebas sin fallos, errores ni omisiones. `node scripts/verificar-chat-match-fases.cjs` verificó fases, borrador, respuesta tardía, desconexión y la comunicación del temporizador con el chat. No se realizó una revisión visual en navegador; los scripts visuales existentes requieren Playwright, ausente en el entorno, y el script antiguo de interfaz falla porque su contexto simulado no define URL.

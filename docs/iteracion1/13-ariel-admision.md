# Aporte 13/16 — Ariel: política común de admisión

## Problema y resultado

La vista, el repositorio de solicitudes y el ingreso durante un descanso
repetían las reglas de fase y cupo. Una sala llena podía mostrar el aviso de
esperar al siguiente descanso, aunque ese descanso no liberase ningún cupo.

`PoliticaAdmisionSala.evaluar` devuelve ahora una decisión con permiso y motivo.
La vista usa esa decisión para el botón y su aviso; el servidor la exige dentro
de la transacción de aceptación. Se conservan los bloqueos de sala/estudiante,
el límite de cuatro integrantes y la prohibición de aceptar en concentración.
Rechazar solicitudes sigue siendo posible durante concentración.

## Refactorización demostrable

- Antes: condiciones y mensajes separados en el repositorio, el ciclo y el servlet.
- Después: `evaluar`, `exigirAceptacion` y `MAX_PARTICIPANTES` centralizan la regla.
- `puedeAceptar` conserva su API y delega en esa misma decisión.
- La incorporación en descanso revalida la fase y el cupo después de avanzar
  el ciclo, antes de persistir la participación nueva.

Esta extracción cuenta como una refactorización; las demás corresponden a los
aportes siguientes. No se presenta cada sustitución de una llamada como una
refactorización independiente.

## Evidencia de TDD y validación seleccionada

1. Se añadieron cinco pruebas unitarias del contrato común de permiso y mensajes.
2. RED con la API mínima: cinco ejecutadas, cuatro fallos de aserción y cero errores.
3. GREEN con la decisión implementada y conectada: 39 casos seleccionados,
   cero fallos, cero errores y cero omitidos:
   - 5 unitarias nuevas de política;
   - 18 casos parametrizados existentes de admisión;
   - 8 pruebas de decisiones y transacciones;
   - 6 pruebas del ciclo persistido;
   - 2 unitarias del protocolo JSON agregado en el aporte 12/16.

```powershell
.\mvnw.cmd "-Dtest=PoliticaAdmisionSalaTest,PoliticaAdmisionSalaParametersTest,DecisionSalaPersistenciaTest,CicloSalaPrivadaPersistenciaTest,SerializadorCicloSalaTest" test
```

No se ejecutó la suite completa. Se utilizó JDK 21.

## Comprobación puntual del aporte 12/16 y de la integración

El script nuevo `scripts/verificar-ciclo-sala.ps1` se ejecutó contra Jetty en
`127.0.0.1:18083`, con una base H2 en memoria y tres cuentas de prueba. Pasaron
23 comprobaciones HTTP/WebSocket, incluidas las de preparación de las cuentas:

- renderizado de la JSP y referencia al JavaScript;
- mínimo de dos participantes y autorización del responsable;
- protección CSRF de inicio y WebSocket;
- recepción del inicio y de actualizaciones sin pedirlas desde el cliente;
- vencimiento común entre dos clientes y recuperación tras reconexión;
- recarga de las vistas y ocultación de los controles que no corresponden;
- aviso y botón bloqueado en concentración, y solicitud conservada pendiente;
- denegación de acceso al ciclo a un solicitante todavía no aceptado.

El JavaScript del aporte 12/16 utiliza el host de la página para crear la URL
WebSocket, por lo que no introduce una IP fija para Radmin. Esta comprobación
local no equivale a una prueba de red entre dos equipos ni a una revisión visual
completa en navegadores.

Para repetir solo la comprobación web, iniciar en otra terminal:

```powershell
.\mvnw.cmd "-Dpomora.host=127.0.0.1" "-Dpomora.port=18083" "-Dpomora.jdbc.url=jdbc:h2:mem:ariel13;DB_CLOSE_DELAY=-1" jetty:run
```

Y luego:

```powershell
.\scripts\verificar-ciclo-sala.ps1 -BaseUrl 'http://127.0.0.1:18083'
```

La comprobación crea cuentas y una sala: usar una base de pruebas, no los datos
de la demostración. El servidor temporal se detuvo al terminar. No se modificó
el servidor Radmin.

## Pendiente para los siguientes aportes

La interfaz del chat privado y el cierre de sala no forman parte de este aporte.
El mínimo de pruebas no sustituye la verificación final de los escenarios de
aceptación, las otras refactorizaciones y los entregables de la iteración.

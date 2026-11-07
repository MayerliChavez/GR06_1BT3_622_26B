# P02 Validación parametrizada del chat

Primer aporte preparado para la nueva iteración. Base: Develop, commit
`f1d4cb8e6bfe3b34b798f42bd2ffc8f00c156a3a`. Preparado con asistencia de Codex.
La identidad Git configurada es Ariel; no se atribuye este commit a Camila.
El reparto anterior proponía P02 para Camila: este primer aporte lo adelanta Ariel.

## Comportamiento comprobado

HU08 de la planificación v3: rechazar contenido vacío o solo espacios con el aviso
«Escribe un mensaje antes de enviarlo.»; aceptar entre 1 y 2000 caracteres;
rechazar 2001 con «El mensaje no puede superar los 2000 caracteres.».
También se comprueba el caso de una petición sin campo, evitando una excepción técnica.

`ValidadorMensajesParametersTest` contiene una plantilla y siete filas. Usa el
validador real llamado por `ServicioChat.registrar`. No utiliza mocks ni base de datos.
El texto no se recorta ni modifica.

## Secuencia ejecutada

1. La base remota pasó 61 pruebas antes del cambio.
2. Se añadió primero la prueba parametrizada y se ejecutó: siete casos, cuatro
   fallos y cero errores. Ver `RED.txt`.
3. Se ajustaron los dos avisos y el límite dentro de `ValidadorMensajes`.
   Se quitó la comprobación duplicada de longitud en `ServicioChat`, que ya llama
   al validador. El servicio antes rechazaba 2001 caracteres, pero con otro aviso;
   el validador por sí solo no comprobaba ese límite.
4. La prueba nueva más las dos antiguas del chat pasaron: nueve casos. Ver `GREEN.txt`.
5. La suite completa pasó: 68 casos, cero fallos, cero errores y cero omitidos.
   Ver `REGRESION.txt`.

Los registros proceden de ejecuciones reales con Java 21 y Maven 3.9.11. Las rutas
absolutas y la caché offline son particulares del equipo que los generó.

## Reproducir

Con Java 21 y Maven configurados, desde la raíz del repositorio:

```powershell
mvn "-Dtest=ValidadorMensajesParametersTest,ChatTest" test
mvn test
```

Si se usa el lanzador del repositorio, después de preparar sus dependencias:

```powershell
.\mvnw.cmd "-Dtest=ValidadorMensajesParametersTest,ChatTest" test
.\mvnw.cmd test
```

Es una prueba parametrizada con siete ejecuciones, no siete métodos nuevos.
La mejora cambia validación y avisos; no se cuenta como una de las cuatro
refactorizaciones comprometidas. No verifica todavía salas privadas, bloqueo
por concentración, persistencia de mensajes ni aislamiento WebSocket.

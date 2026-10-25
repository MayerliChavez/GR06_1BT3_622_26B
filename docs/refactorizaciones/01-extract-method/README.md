# Refactorización 1: Extract Method

Clase: ServicioEstudioCompartido.
Se extrae la búsqueda por identificador al método privado obtenerSesion(UUID compartidaId).
Las cuatro llamadas permanecen dentro de los métodos públicos synchronized existentes.
Se mantienen el repositorio, el número de consultas y la excepción con el mismo mensaje.

## Antes
antes.png es la captura original aportada por el usuario; no es una recreación.
ServicioEstudioCompartido-antes.java.txt conserva el código previo.
Mostrar las cuatro búsquedas repetidas, en las líneas originales 46, 51, 57 y 63.

## Después
actualizarSesion, salir, registrarDesconexion y consultarTiempoRestante llaman a:

    var sesion = obtenerSesion(compartidaId);

El nuevo método concentra la búsqueda y el error de sesión inexistente.

## Mini guion (aproximadamente 30 segundos)
«En ServicioEstudioCompartido encontré cuatro operaciones que repetían la misma búsqueda
 de sesión y el mismo manejo de error. Apliqué Extract Method creando obtenerSesion y
 sustituí las cuatro repeticiones por llamadas a ese método. Así centralicé esta lógica
 sin cambiar el comportamiento. Ejecuté las mismas quince pruebas antes y después
 para comprobar que las sesiones compartidas, las rachas y las notificaciones siguen funcionando.»

## Verificación
antes-pruebas.txt y despues-pruebas.txt conservan las ejecuciones del mismo conjunto:
EstudioCompartidoTest, RachasIntegracionTest y NotificacionesIntegracionTest.
La refactorización no añade funcionalidades ni modifica las pruebas.
# Prueba unitaria con Mockito: preferencias individuales

Se sigue el patron de PaymentProcessor: ServicioPreferencias es real y RepositorioPreferencias es una interfaz simulada. ServicioPomodoro tambien es real; se simula RepositorioSesiones para evitar una base de datos.

La prueba entrega preferencias de 40/8/20 minutos y 3 bloques. Comprueba la identidad del estudiante, las duraciones de los bloques reales, el descanso largo y las llamadas de consulta y guardado.

## RED

El metodo iniciarSesion devolvia null. Maven ejecuto una prueba y fallo la asercion que exige una sesion. Evidencia: red.log y red.png.

## GREEN

El metodo consulta preferencias.buscarPorEstudiante(estudianteId) y entrega la configuracion a pomodoro.iniciarSesion(estudianteId, configuracion, ahora). La misma prueba pasa. Evidencia: green.log y green.png.

Las imagenes presentan extractos reales de los logs; no son capturas de IntelliJ. Se incluyen los logs completos.

Se reutilizan las clases de dominio y el servicio existentes. No se ha conectado este nuevo servicio al servlet ni implementado almacenamiento de preferencias: el repositorio simulado corresponde exclusivamente al alcance de esta prueba unitaria.
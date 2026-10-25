import epn.edu.ec.model.*;
import epn.edu.ec.repository.*;
import epn.edu.ec.service.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

/** Fixture exclusiva para la base aislada de pruebas; no altera data/pomora. */
class PrepararNotificaciones {
    public static void main(String[] args) {
        String jdbc = "jdbc:h2:file:./target/hu3-e2e";
        try (var factory = Persistence.createEntityManagerFactory("tarea3", Map.of(
                "jakarta.persistence.jdbc.url", jdbc,
                "hibernate.hbm2ddl.auto", "create", "hibernate.show_sql", "false"))) {
            var cuentas = new ServicioCuentas(factory);
            var a = cuentas.registrar("Ana Avisos", "ana-avisos@example.test", "PruebaAvisos123!");
            var b = cuentas.registrar("Luis Avisos", "luis-avisos@example.test", "PruebaAvisos123!");
            cuentas.registrar("Individual Avisos", "individual-avisos@example.test", "PruebaAvisos123!");
            var individual = new RepositorioSesionesJpa(factory);
            var compartidas = new RepositorioSesionesCompartidasJpa(factory);
            var config = new ConfiguracionPomodoro(Duration.ofSeconds(90), Duration.ofSeconds(25), Duration.ofSeconds(25), 1);
            var servicio = new ServicioEstudioCompartido(new EmparejadorEnMemoria(), individual, compartidas, config);
            var ahora = Instant.now();
            servicio.solicitarEmparejamiento(a.getEstudiante().getId(), ahora);
            servicio.solicitarEmparejamiento(b.getEstudiante().getId(), ahora);
            System.out.println("Fixture HU3 aislada lista. Inicio: " + ahora);
        }
    }
}

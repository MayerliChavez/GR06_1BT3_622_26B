package epn.edu.ec;

import epn.edu.ec.model.*;
import epn.edu.ec.repository.*;
import epn.edu.ec.service.ServicioPomodoro;
import jakarta.persistence.*;
import org.junit.jupiter.api.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class RachasIntegracionTest {
    private EntityManagerFactory factory;
    private ServicioPomodoro pomodoro;
    private RepositorioRachasJpa rachas;
    private final ConfiguracionPomodoro config = new ConfiguracionPomodoro(
            Duration.ofSeconds(10), Duration.ofSeconds(2), Duration.ofSeconds(5), 4);
    private final UUID estudiante = UUID.randomUUID();
    private final Instant inicio = Instant.parse("2026-10-20T15:00:00Z");

    @BeforeEach void preparar() {
        factory = Persistence.createEntityManagerFactory("tarea3", Map.of(
                "jakarta.persistence.jdbc.url", "jdbc:h2:mem:rachas" + UUID.randomUUID(),
                "hibernate.hbm2ddl.auto", "create-drop", "hibernate.show_sql", "false"));
        pomodoro = new ServicioPomodoro(new RepositorioSesionesJpa(factory));
        rachas = new RepositorioRachasJpa(factory);
    }
    @AfterEach void cerrar() { if (factory != null) factory.close(); }

    private void completar(Instant fecha) {
        var sesion = pomodoro.iniciarSesion(estudiante, config, fecha);
        pomodoro.finalizarSesion(sesion.getId(), fecha.plusSeconds(10));
    }
    private void comprobar(int dias) {
        assertEquals(dias, new RepositorioRachasJpa(factory).consultar(estudiante).getDias());
        try (var em = factory.createEntityManager()) {
            assertEquals(dias, em.find(RachaEstudio.class, estudiante).getDias());
        }
    }

    @Test void cumpleLosCuatroEscenariosConPersistencia() {
        completar(inicio); comprobar(1); // 2.1
        completar(inicio.plus(Duration.ofDays(1))); comprobar(2); // 2.2
        completar(inicio.plus(Duration.ofDays(1)).plusSeconds(60)); comprobar(2); // 2.3
        assertEquals(0, rachas.consultar(estudiante).diasVigentes(LocalDate.of(2026,10,23)));
        completar(inicio.plus(Duration.ofDays(3))); comprobar(1); // 2.4
        assertEquals(LocalDate.of(2026,10,23), rachas.consultar(estudiante).getUltimoDiaEstudio());
    }

    @Test void ignoraBloquesIncompletosYConservaConcentracionTrasAbandonar() {
        var sesion = pomodoro.iniciarSesion(estudiante, config, inicio);
        comprobar(0);
        pomodoro.finalizarSesion(sesion.getId(), inicio.plusSeconds(2));
        comprobar(0);
        sesion = pomodoro.iniciarSesion(estudiante, config, inicio.plusSeconds(20));
        pomodoro.completarBloqueActual(sesion.getId(), inicio.plusSeconds(30));
        pomodoro.abandonarSesion(sesion.getId(), inicio.plusSeconds(31));
        comprobar(1);
    }

    @Test void combinaIndividualYCompartidaSinDuplicarYAislaParticipantes() {
        completar(inicio); comprobar(1);
        UUID otro = UUID.randomUUID();
        var fecha = inicio.plusSeconds(60);
        var compartida = new SesionCompartida(
                new SesionEstudio(new Estudiante(estudiante, "Ariel"), config, fecha),
                new SesionEstudio(new Estudiante(otro, "Compañero"), config, fecha));
        compartida.iniciar(fecha);
        var repo = new RepositorioSesionesCompartidasJpa(factory);
        repo.guardar(compartida);
        compartida.registrarSalida(estudiante, MotivoSalida.VOLUNTARIA, fecha.plusSeconds(10));
        repo.guardar(compartida);
        comprobar(1);
        assertEquals(1, rachas.consultar(otro).getDias());
        compartida.registrarSalida(otro, MotivoSalida.VOLUNTARIA, fecha.plusSeconds(11));
        repo.guardar(compartida);
        assertEquals(1, rachas.consultar(otro).getDias());
        completar(inicio.plus(Duration.ofDays(1)));
        comprobar(2);
        assertEquals(1, rachas.consultar(otro).getDias());
        repo.guardar(compartida); // Reintento de guardado no duplica ni retrocede la racha.
        comprobar(2);
    }

    @Test void fechaDeFinalizacionUsaDiaLocalNoUtc() {
        completar(Instant.parse("2026-10-21T04:59:40Z")); // 20 octubre, 23:59:50
        completar(Instant.parse("2026-10-21T05:00:00Z")); // 21 octubre, 00:00:10
        comprobar(2);
        assertEquals(LocalDate.of(2026,10,21), rachas.consultar(estudiante).getUltimoDiaEstudio());
    }
    @Test void acreditaAmbosParticipantesSinSalirYConservaTrasDesconexion() {
        UUID otro = UUID.randomUUID();
        var individuales = new RepositorioSesionesJpa(factory);
        var compartidas = new RepositorioSesionesCompartidasJpa(factory);
        var servicio = new epn.edu.ec.service.ServicioEstudioCompartido(
                new EmparejadorEnMemoria(), individuales, compartidas, config);
        servicio.solicitarEmparejamiento(estudiante, inicio);
        var sesion = servicio.solicitarEmparejamiento(otro, inicio).orElseThrow();
        servicio.actualizarSesion(sesion.getId(), inicio.plusSeconds(10));
        comprobar(1);
        assertEquals(1, rachas.consultar(otro).getDias());
        assertEquals(Set.of(LocalDate.of(2026,10,20)), rachas.resumen(estudiante).diasEstudiados());
        servicio.registrarDesconexion(sesion.getId(), estudiante, inicio.plusSeconds(11));
        comprobar(1);
        assertEquals(1, rachas.consultar(otro).getDias());
    }

    @Test void acreditaDiasDiferentesDentroDeUnaMismaSesion() {
        var antes = Instant.parse("2026-10-21T04:59:48Z");
        var sesion = pomodoro.iniciarSesion(estudiante, config, antes);
        pomodoro.avanzarSiCorresponde(sesion.getId(), 1, antes.plusSeconds(10));
        comprobar(1);
        pomodoro.avanzarSiCorresponde(sesion.getId(), 2, antes.plusSeconds(12));
        pomodoro.avanzarSiCorresponde(sesion.getId(), 3, antes.plusSeconds(22));
        comprobar(2);
        assertEquals(Set.of(LocalDate.of(2026,10,20), LocalDate.of(2026,10,21)),
                rachas.resumen(estudiante).diasEstudiados());
    }
}


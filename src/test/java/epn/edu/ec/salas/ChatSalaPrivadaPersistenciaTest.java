package epn.edu.ec.salas;

import epn.edu.ec.model.*;
import epn.edu.ec.repository.RepositorioSalasPrivadasJpa;
import epn.edu.ec.service.ServicioChatSalaPrivada;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;
import org.junit.*;

import static org.junit.Assert.*;

public class ChatSalaPrivadaPersistenciaTest extends PruebaSalaPrivada {

    private EntityManagerFactory factory;
    private ServicioChatSalaPrivada chat;
    private SalaPrivada salaAna;

    private final UUID ana = UUID.randomUUID();
    private final UUID luis = UUID.randomUUID();
    private final UUID pedro = UUID.randomUUID();

    private final Instant ahora =
            Instant.parse("2026-11-08T15:00:00Z");

    @Before
    public void setUp() {
        factory = Persistence.createEntityManagerFactory(
                "tarea3",
                Map.of(
                        "jakarta.persistence.jdbc.url",
                        "jdbc:h2:mem:chatprivado" + UUID.randomUUID(),
                        "hibernate.hbm2ddl.auto", "create-drop",
                        "hibernate.show_sql", "false"
                )
        );

        try (var em = factory.createEntityManager()) {
            em.getTransaction().begin();

            em.persist(new Estudiante(ana, "Ana Torres"));
            em.persist(new Estudiante(luis, "Luis Mena"));
            em.persist(new Estudiante(pedro, "Pedro Silva"));

            em.getTransaction().commit();
        }

        salaAna = new SalaPrivada(ana, "K7M2QX");

        new RepositorioSalasPrivadasJpa(factory)
                .guardarNueva(salaAna);

        try (var em = factory.createEntityManager()) {
            em.getTransaction().begin();

            em.persist(
                    new ParticipacionSalaPrivada(
                            luis,
                            em.getReference(
                                    SalaPrivada.class,
                                    salaAna.getId()
                            )
                    )
            );

            em.getTransaction().commit();
        }

        chat = new ServicioChatSalaPrivada(factory);
    }

    @After
    public void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    public void given_dos_salas_when_guardar_mensajes_then_historiales_separados_con_autor_y_fecha() {
        var salaPedro = new SalaPrivada(pedro, "P4R8TW");

        new RepositorioSalasPrivadasJpa(factory)
                .guardarNueva(salaPedro);

        var mensaje = chat.registrar(
                ana,
                "Revisemos el ejercicio 3",
                ahora
        );

        chat.registrar(
                pedro,
                "Mensaje de otra sala",
                ahora
        );

        var historialLuis =
                new ServicioChatSalaPrivada(factory).historial(luis);

        assertNotNull(mensaje.getId());
        assertEquals(1, historialLuis.size());

        assertEquals(
                "Revisemos el ejercicio 3",
                historialLuis.get(0).getTexto()
        );

        assertEquals(
                "Ana Torres",
                historialLuis.get(0).getNombreAutor()
        );

        assertEquals(ana, historialLuis.get(0).getAutorId());
        assertEquals(salaAna.getId(), historialLuis.get(0).getSalaId());
        assertEquals(ahora, historialLuis.get(0).getFecha());

        assertEquals(
                "Mensaje de otra sala",
                chat.historial(pedro).get(0).getTexto()
        );
    }

    @Test
    public void given_estudiante_sin_participacion_when_enviar_o_leer_then_rechaza_acceso() {
        assertThrows(
                SecurityException.class,
                () -> chat.registrar(pedro, "Hola", ahora)
        );

        assertThrows(
                SecurityException.class,
                () -> chat.historial(pedro)
        );

        assertTrue(chat.historial(ana).isEmpty());
    }

    @Test
    public void given_historial_existente_when_cambiar_entre_concentracion_y_descansos_then_controla_envio_y_conserva_historial() {
        chat.registrar(ana, "Antes de iniciar", ahora);

        fase(TipoBloque.CONCENTRACION);

        assertThrows(
                IllegalStateException.class,
                () -> chat.registrar(
                        luis,
                        "No debe publicarse",
                        ahora
                )
        );

        assertEquals(1, chat.historial(luis).size());

        fase(TipoBloque.DESCANSO_CORTO);

        chat.registrar(
                luis,
                "Mensaje en descanso corto",
                ahora.plusSeconds(1)
        );

        fase(TipoBloque.DESCANSO_LARGO);

        chat.registrar(
                ana,
                "Mensaje en descanso largo",
                ahora.plusSeconds(2)
        );

        assertEquals(3, chat.historial(ana).size());
    }

    @Test
    public void given_textos_invalidos_y_texto_limite_when_enviar_then_rechaza_invalidos_y_conserva_2000_caracteres() {
        assertThrows(
                IllegalArgumentException.class,
                () -> chat.registrar(ana, "   ", ahora)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> chat.registrar(
                        ana,
                        "A".repeat(2001),
                        ahora
                )
        );

        assertTrue(chat.historial(ana).isEmpty());

        chat.registrar(ana, "A".repeat(2000), ahora);

        assertEquals(
                "A".repeat(2000),
                chat.historial(luis).get(0).getTexto()
        );
    }

    @Test
    public void given_endpoint_de_otra_sala_when_enviar_then_rechaza_sin_guardar() {
        assertThrows(
                SecurityException.class,
                () -> chat.registrar(
                        UUID.randomUUID(),
                        ana,
                        "Mensaje ajeno",
                        ahora
                )
        );

        assertTrue(chat.historial(ana).isEmpty());
    }

    @Test
    public void given_endpoint_de_otra_sala_when_leer_historial_then_rechaza_acceso() {
        chat.registrar(ana, "Mensaje privado", ahora);

        assertThrows(
                SecurityException.class,
                () -> chat.historial(UUID.randomUUID(), ana)
        );

        assertEquals(
                1,
                chat.historial(salaAna.getId(), ana).size()
        );
    }

    private void fase(TipoBloque tipo) {
        try (var em = factory.createEntityManager()) {
            em.getTransaction().begin();

            em.find(SalaPrivada.class, salaAna.getId())
                    .actualizarBloqueActual(tipo);

            em.getTransaction().commit();
        }
    }

}
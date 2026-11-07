package epn.edu.ec.tdd.chat;

import epn.edu.ec.repository.RepositorioSesionesCompartidas;
import epn.edu.ec.service.ServicioChat;
import jakarta.persistence.EntityManagerFactory;
import java.util.Optional;
import java.util.UUID;
import org.junit.Test;

import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.*;

public class ServicioChatAccesoMockTest {

    @Test
    public void estudianteSinParticipacionNoPuedeGuardarMensaje() {
        // Preparar las dependencias simuladas.
        EntityManagerFactory factory =
                mock(EntityManagerFactory.class);

        RepositorioSesionesCompartidas repositorio =
                mock(RepositorioSesionesCompartidas.class);

        UUID salaId = UUID.randomUUID();
        UUID estudianteId = UUID.randomUUID();

        when(repositorio.buscarAbiertaPorEstudiante(estudianteId))
                .thenReturn(Optional.empty());

        // El servicio que estamos probando es real.
        ServicioChat servicio = new ServicioChat(factory, repositorio);

        // Intentar enviar sin pertenecer a una sesión activa.
        assertThrows(SecurityException.class, () ->
                servicio.registrar(
                        salaId,
                        estudianteId,
                        "Tengo una duda del ejercicio"
                )
        );

        // Se consultó la pertenencia del estudiante.
        verify(repositorio)
                .buscarAbiertaPorEstudiante(estudianteId);

        // No se intentó abrir la base de datos ni guardar el mensaje.
        verifyZeroInteractions(factory);
    }
}
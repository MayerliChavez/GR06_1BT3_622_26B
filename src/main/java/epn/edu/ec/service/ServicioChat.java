package epn.edu.ec.service;

import epn.edu.ec.model.*;
import epn.edu.ec.repository.RepositorioSesionesCompartidas;
import epn.edu.ec.tdd.chat.*;
import jakarta.persistence.EntityManagerFactory;
import java.util.*;

public class ServicioChat {
    private final EntityManagerFactory factory;
    private final RepositorioSesionesCompartidas sesiones;
    private final ControladorMensajes controlador = new ControladorMensajes();
    private final ValidadorMensajes validador = new ValidadorMensajes();

    public ServicioChat(EntityManagerFactory factory, RepositorioSesionesCompartidas sesiones) {
        this.factory = factory;
        this.sesiones = sesiones;
    }

    public SesionCompartida exigirParticipante(UUID sesionId, UUID estudianteId) {
        return sesiones.buscarAbiertaPorEstudiante(estudianteId)
                .filter(s -> s.getId().equals(sesionId))
                .orElseThrow(() -> new SecurityException("No perteneces a esta sesion de estudio activa."));
    }

    // Se invoca bajo el mismo bloqueo del servicio compartido que usa salir/desconectar.
    public MensajeChat registrar(UUID sesionId, UUID estudianteId, String texto) {
        validador.validar(texto);
        if (texto.length() > 2000) throw new IllegalArgumentException("El mensaje admite hasta 2000 caracteres.");
        SesionCompartida sesion = exigirParticipante(sesionId, estudianteId);
        var autor = sesion.getParticipaciones().stream()
                .map(ParticipacionCompartida::getEstudiante)
                .filter(e -> e.getId().equals(estudianteId)).findFirst().orElseThrow();
        var datos = controlador.crearMensaje(texto, estudianteId, sesionId);
        try (var em = factory.createEntityManager()) {
            var tx = em.getTransaction();
            try {
                tx.begin();
                var mensaje = new MensajeChat(datos,
                        em.getReference(Estudiante.class, autor.getId()),
                        em.getReference(SesionCompartida.class, sesionId));
                em.persist(mensaje);
                tx.commit();
                return mensaje;
            } catch (RuntimeException e) {
                if (tx.isActive()) tx.rollback();
                throw e;
            }
        }
    }

    public List<MensajeChat> historial(UUID sesionId, UUID estudianteId) {
        exigirParticipante(sesionId, estudianteId);
        try (var em = factory.createEntityManager()) {
            var mensajes = em.createQuery(
                    "select m from MensajeChat m join fetch m.autor where m.sesion.id = :sesion order by m.id desc",
                    MensajeChat.class).setParameter("sesion", sesionId).setMaxResults(100).getResultList();
            Collections.reverse(mensajes);
            return mensajes;
        }
    }
}

package epn.edu.ec.repository;

import epn.edu.ec.model.*;
import epn.edu.ec.tdd.rachas.*;
import jakarta.persistence.*;
import java.time.*;
import java.util.*;

public class RepositorioRachasJpa {
    private final EntityManagerFactory factory;
    public RepositorioRachasJpa(EntityManagerFactory factory) { this.factory = factory; }

    public record Resumen(RachaEstudio racha, SortedSet<LocalDate> diasEstudiados) {}

    public RachaEstudio consultar(UUID estudianteId) { return resumen(estudianteId).racha(); }

    public Resumen resumen(UUID estudianteId) {
        try (var em = factory.createEntityManager()) {
            var tx = em.getTransaction();
            try {
                tx.begin();
                if (em.find(Estudiante.class, estudianteId, LockModeType.PESSIMISTIC_WRITE) == null)
                    throw new IllegalArgumentException("Estudiante inexistente.");
                var fechas = fechasCompletadas(em, estudianteId);
                var racha = guardarResumen(em, estudianteId, fechas);
                tx.commit();
                return new Resumen(racha, Collections.unmodifiableSortedSet(fechas));
            } catch (RuntimeException e) { if (tx.isActive()) tx.rollback(); throw e; }
        }
    }

    /** Misma transaccion que el bloque completado, sea individual o compartido. */
    public static RachaEstudio actualizar(EntityManager em, UUID estudianteId) {
        em.find(Estudiante.class, estudianteId, LockModeType.PESSIMISTIC_WRITE);
        em.flush();
        return guardarResumen(em, estudianteId, fechasCompletadas(em, estudianteId));
    }

    private static SortedSet<LocalDate> fechasCompletadas(EntityManager em, UUID estudianteId) {
        var instantes = em.createQuery(
                "select b.fechaFin from SesionEstudio s join s.bloques b where s.estudiante.id = :id "
                + "and b.tipo = :tipo and b.estado = :estado and b.fechaFin is not null", Instant.class)
                .setParameter("id", estudianteId).setParameter("tipo", TipoBloque.CONCENTRACION)
                .setParameter("estado", EstadoBloque.COMPLETADO).getResultList();
        var fechas = new TreeSet<LocalDate>();
        instantes.forEach(fin -> fechas.add(fin.atZone(ControladorRacha.ZONA_ESTUDIO).toLocalDate()));
        return fechas;
    }

    private static RachaEstudio guardarResumen(EntityManager em, UUID estudianteId, SortedSet<LocalDate> fechas) {
        var evaluador = new EvaluadorRachas();
        int dias = 0;
        LocalDate ultimo = null;
        for (var fecha : fechas) {
            dias = evaluador.calcularRacha(dias, ultimo, fecha);
            ultimo = fecha;
        }
        var racha = em.find(RachaEstudio.class, estudianteId);
        if (racha == null) { racha = new RachaEstudio(estudianteId); em.persist(racha); }
        if (racha.getDias() != dias || !Objects.equals(racha.getUltimoDiaEstudio(), ultimo))
            racha.actualizar(dias, ultimo);
        return racha;
    }
}

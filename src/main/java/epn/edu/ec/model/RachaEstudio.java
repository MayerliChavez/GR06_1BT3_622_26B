package epn.edu.ec.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "racha_estudio")
public class RachaEstudio {
    @Id private UUID estudianteId;
    @Column(nullable = false) private int dias;
    private LocalDate ultimoDiaEstudio;

    protected RachaEstudio() {}
    public RachaEstudio(UUID estudianteId) { this.estudianteId = estudianteId; }
    public void actualizar(int dias, LocalDate ultimoDiaEstudio) {
        this.dias = dias;
        this.ultimoDiaEstudio = ultimoDiaEstudio;
    }
    public UUID getEstudianteId() { return estudianteId; }
    public int getDias() { return dias; }
    public LocalDate getUltimoDiaEstudio() { return ultimoDiaEstudio; }
    public int diasVigentes(LocalDate hoy) {
        return ultimoDiaEstudio == null || ultimoDiaEstudio.isBefore(hoy.minusDays(1)) ? 0 : dias;
    }
}

package epn.edu.ec.tdd.rachas;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class EvaluadorRachasParametersTest {

    private EvaluadorRachas evaluador;
    private final String escenario;
    private final int diasActuales;
    private final LocalDate ultimoDiaEstudio;
    private final LocalDate fechaFinalizacion;
    private final int expected;

    @Parameterized.Parameters(name = "{index}: {0}")
    public static Iterable<Object[]> parameters() {
        List<Object[]> casos = new ArrayList<Object[]>();
        casos.add(new Object[]{"2.1 - Iniciar sin racha previa", 0, null,
                LocalDate.of(2026, 10, 25), 1});
        casos.add(new Object[]{"2.2 - Incrementar en el dia siguiente", 3,
                LocalDate.of(2026, 10, 24), LocalDate.of(2026, 10, 25), 4});
        casos.add(new Object[]{"2.3 - Mantener al repetir el mismo dia", 4,
                LocalDate.of(2026, 10, 25), LocalDate.of(2026, 10, 25), 4});
        casos.add(new Object[]{"2.4 - Reiniciar despues de un dia sin estudiar", 5,
                LocalDate.of(2026, 10, 23), LocalDate.of(2026, 10, 25), 1});
        casos.add(new Object[]{"2.2 - Continuidad entre meses", 2,
                LocalDate.of(2026, 10, 31), LocalDate.of(2026, 11, 1), 3});
        casos.add(new Object[]{"2.2 - Continuidad entre anios", 6,
                LocalDate.of(2026, 12, 31), LocalDate.of(2027, 1, 1), 7});
        return casos;
    }

    public EvaluadorRachasParametersTest(String escenario, int diasActuales,
            LocalDate ultimoDiaEstudio, LocalDate fechaFinalizacion, int expected) {
        this.escenario = escenario;
        this.diasActuales = diasActuales;
        this.ultimoDiaEstudio = ultimoDiaEstudio;
        this.fechaFinalizacion = fechaFinalizacion;
        this.expected = expected;
    }

    @Before
    public void setUp() {
        evaluador = new EvaluadorRachas();
    }

    @Test
    public void given_study_dates_when_calculating_streak_then_expected_days() {


        int actual = evaluador.calcularRacha(diasActuales, ultimoDiaEstudio, fechaFinalizacion);

        assertEquals(escenario, expected, actual);
    }
    @After
    public void tearDown() {
        evaluador = null;
    }
}

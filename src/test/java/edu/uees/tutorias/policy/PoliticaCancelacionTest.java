package edu.uees.tutorias.policy;

import edu.uees.tutorias.builder.ReservaBuilder;
import edu.uees.tutorias.domain.Asignatura;
import edu.uees.tutorias.domain.Docente;
import edu.uees.tutorias.domain.Estudiante;
import edu.uees.tutorias.domain.HorarioTutoria;
import edu.uees.tutorias.domain.Reserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de los ConcreteStrategies de cancelacion.
 *
 * <p>Cada politica se evalua por separado sobre la <b>misma</b> reserva
 * y el <b>mismo</b> instante de solicitud: lo unico que cambia entre un
 * caso y otro es la estrategia. Esa es justamente la propiedad que el
 * patron aporta y la que estas pruebas verifican.</p>
 */
class PoliticaCancelacionTest {

    /** La tutoria arranca el 1 de octubre a las 10:00. */
    private static final LocalDateTime INICIO_TUTORIA = LocalDateTime.of(2026, 10, 1, 10, 0);

    /** Tres dias antes: cancelacion holgada. */
    private static final LocalDateTime CON_ANTICIPACION = INICIO_TUTORIA.minusDays(3);

    /** Dos horas antes: cancelacion tardia. */
    private static final LocalDateTime SOBRE_LA_HORA = INICIO_TUTORIA.minusHours(2);

    private Reserva reserva;

    @BeforeEach
    void setUp() {
        Docente docente = new Docente("D001", "Ana", "Perez", "ana.perez@uees.edu.ec", "Bases de datos");
        Asignatura asignatura = new Asignatura("SIS201", "Bases de datos");
        docente.agregarAsignatura(asignatura);

        HorarioTutoria horario = new HorarioTutoria("H001", docente, asignatura,
                INICIO_TUTORIA.toLocalDate(), INICIO_TUTORIA.toLocalTime(), LocalTime.of(11, 0));

        Estudiante estudiante = new Estudiante("E001", "Justin", "Arreaga",
                "justin.arreaga@uees.edu.ec", "Computacion", 5);

        reserva = new ReservaBuilder()
                .conEstudiante(estudiante)
                .conHorario(horario)
                .construir();
    }

    @Test
    void laPoliticaAnticipadaAceptaLaCancelacionDentroDelPlazo() {
        ResultadoCancelacion resultado =
                new PoliticaCancelacionAnticipada().evaluar(reserva, CON_ANTICIPACION);

        assertTrue(resultado.permitida());
        assertFalse(resultado.tienePenalidad());
    }

    @Test
    void laPoliticaAnticipadaRechazaLaCancelacionTardia() {
        ResultadoCancelacion resultado =
                new PoliticaCancelacionAnticipada().evaluar(reserva, SOBRE_LA_HORA);

        assertFalse(resultado.permitida());
        assertEquals(0, resultado.penalidadPorcentaje());
    }

    @Test
    void laPoliticaConPenalidadNoCobraCuandoHayAnticipacion() {
        ResultadoCancelacion resultado =
                new PoliticaCancelacionConPenalidad().evaluar(reserva, CON_ANTICIPACION);

        assertTrue(resultado.permitida());
        assertEquals(0, resultado.penalidadPorcentaje());
    }

    @Test
    void laPoliticaConPenalidadAceptaLaCancelacionTardiaPeroCobra() {
        ResultadoCancelacion resultado =
                new PoliticaCancelacionConPenalidad().evaluar(reserva, SOBRE_LA_HORA);

        assertTrue(resultado.permitida());
        assertTrue(resultado.tienePenalidad());
        assertEquals(PoliticaCancelacionConPenalidad.PENALIDAD_POR_DEFECTO,
                resultado.penalidadPorcentaje());
    }

    @Test
    void laPoliticaDelDocenteNuncaPenalizaAlEstudiante() {
        ResultadoCancelacion resultado =
                new PoliticaCancelacionDocente("el docente reporto incapacidad medica")
                        .evaluar(reserva, SOBRE_LA_HORA);

        assertTrue(resultado.permitida());
        assertFalse(resultado.tienePenalidad());
        assertTrue(resultado.motivo().contains("incapacidad medica"));
    }

    /**
     * El mismo escenario tardio produce tres veredictos distintos segun
     * la estrategia usada, sin que la reserva ni el servicio cambien.
     */
    @Test
    void elMismoEscenarioProduceVeredictosDistintosSegunLaEstrategia() {
        assertFalse(new PoliticaCancelacionAnticipada().evaluar(reserva, SOBRE_LA_HORA).permitida());
        assertTrue(new PoliticaCancelacionConPenalidad().evaluar(reserva, SOBRE_LA_HORA).tienePenalidad());
        assertFalse(new PoliticaCancelacionDocente().evaluar(reserva, SOBRE_LA_HORA).tienePenalidad());
    }

    @Test
    void unaPoliticaAnticipadaExigeUnaAnticipacionPositiva() {
        assertThrows(IllegalArgumentException.class, () -> new PoliticaCancelacionAnticipada(0));
    }

    @Test
    void unResultadoRechazadoNoPuedeLlevarPenalidad() {
        assertThrows(IllegalArgumentException.class,
                () -> new ResultadoCancelacion(false, 30, "incoherente"));
    }
}

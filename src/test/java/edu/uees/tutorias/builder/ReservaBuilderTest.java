package edu.uees.tutorias.builder;

import edu.uees.tutorias.domain.Asignatura;
import edu.uees.tutorias.domain.Docente;
import edu.uees.tutorias.domain.EstadoHorario;
import edu.uees.tutorias.domain.EstadoReserva;
import edu.uees.tutorias.domain.Estudiante;
import edu.uees.tutorias.domain.HorarioTutoria;
import edu.uees.tutorias.domain.ModalidadTutoria;
import edu.uees.tutorias.domain.Reserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del patron Builder aplicado a la construccion de una Reserva:
 * valores por defecto, configuraciones distintas y validacion previa.
 */
class ReservaBuilderTest {

    private Docente docente;
    private Asignatura asignatura;
    private Estudiante estudiante;
    private HorarioTutoria horario;

    @BeforeEach
    void setUp() {
        docente = new Docente("D001", "Ana", "Perez", "ana.perez@uees.edu.ec", "Bases de datos");
        asignatura = new Asignatura("SIS201", "Bases de datos");
        estudiante = new Estudiante("E001", "Justin", "Arreaga",
                "justin.arreaga@uees.edu.ec", "Computacion", 5);
        horario = nuevoHorario("H001");
    }

    private HorarioTutoria nuevoHorario(String id) {
        return new HorarioTutoria(id, docente, asignatura,
                LocalDate.of(2026, 9, 8), LocalTime.of(10, 0), LocalTime.of(11, 0));
    }

    @Test
    void configuracionMinimaAplicaLosValoresPorDefecto() {
        Reserva reserva = new ReservaBuilder()
                .conEstudiante(estudiante)
                .conHorario(horario)
                .construir();

        assertEquals(ModalidadTutoria.PRESENCIAL, reserva.getModalidad());
        assertEquals(Reserva.MOTIVO_POR_DEFECTO, reserva.getMotivo());
        assertEquals(Reserva.RECORDATORIO_POR_DEFECTO_MINUTOS, reserva.getRecordatorioMinutosAntes());
        assertTrue(reserva.getEnlaceVirtual().isEmpty());
        assertTrue(reserva.getObservaciones().isEmpty());
        assertFalse(reserva.getId().isBlank());
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
    }

    @Test
    void configuracionCompletaConservaCadaDatoIndicado() {
        Reserva reserva = new ReservaBuilder()
                .conId("R002")
                .conEstudiante(estudiante)
                .conHorario(horario)
                .virtualCon("https://meet.uees.edu.ec/tutoria-h001")
                .conMotivo("Consulta sobre normalizacion")
                .conObservaciones("El estudiante enviara su avance")
                .conRecordatorioDe(30)
                .construir();

        assertEquals("R002", reserva.getId());
        assertEquals(ModalidadTutoria.VIRTUAL, reserva.getModalidad());
        assertEquals("https://meet.uees.edu.ec/tutoria-h001", reserva.getEnlaceVirtual().orElseThrow());
        assertEquals("Consulta sobre normalizacion", reserva.getMotivo());
        assertEquals("El estudiante enviara su avance", reserva.getObservaciones().orElseThrow());
        assertEquals(30, reserva.getRecordatorioMinutosAntes());
    }

    @Test
    void construirOcupaElHorarioIndicado() {
        new ReservaBuilder().conEstudiante(estudiante).conHorario(horario).construir();

        assertEquals(EstadoHorario.RESERVADO, horario.getEstado());
    }

    @Test
    void faltarUnDatoObligatorioImpideConstruir() {
        ReservaBuilder sinEstudiante = new ReservaBuilder().conHorario(horario);
        ReservaBuilder sinHorario = new ReservaBuilder().conEstudiante(estudiante);

        assertThrows(IllegalStateException.class, sinEstudiante::construir);
        assertThrows(IllegalStateException.class, sinHorario::construir);
    }

    @Test
    void unaTutoriaVirtualSinEnlaceNoSeConstruye() {
        ReservaBuilder builder = new ReservaBuilder()
                .conEstudiante(estudiante)
                .conHorario(horario)
                .virtualCon("   ");

        assertThrows(IllegalStateException.class, builder::construir);
    }

    @Test
    void unRecordatorioFueraDeRangoNoSeConstruye() {
        ReservaBuilder builder = new ReservaBuilder()
                .conEstudiante(estudiante)
                .conHorario(horario)
                .conRecordatorioDe(-5);

        assertThrows(IllegalStateException.class, builder::construir);
    }

    @Test
    void unaConstruccionRechazadaNoDejaElHorarioOcupado() {
        ReservaBuilder builder = new ReservaBuilder()
                .conHorario(horario)
                .virtualCon("https://meet.uees.edu.ec/tutoria-h001");

        assertThrows(IllegalStateException.class, builder::construir);
        assertTrue(horario.estaDisponible());
    }

    @Test
    void volverAPresencialLimpiaElEnlaceHeredado() {
        Reserva reserva = new ReservaBuilder()
                .conEstudiante(estudiante)
                .conHorario(horario)
                .virtualCon("https://meet.uees.edu.ec/tutoria-h001")
                .presencial()
                .construir();

        assertEquals(ModalidadTutoria.PRESENCIAL, reserva.getModalidad());
        assertTrue(reserva.getEnlaceVirtual().isEmpty());
    }

    @Test
    void elConstructorDeReservaValidaAunqueSeLoInvoqueDirectamente() {
        ReservaBuilder incompleto = new ReservaBuilder().conHorario(nuevoHorario("H009"));

        assertThrows(IllegalStateException.class, () -> new Reserva(incompleto));
    }
}

package edu.uees.tutorias.service;

import edu.uees.tutorias.builder.ReservaBuilder;
import edu.uees.tutorias.domain.Asignatura;
import edu.uees.tutorias.domain.Docente;
import edu.uees.tutorias.domain.EstadoHorario;
import edu.uees.tutorias.domain.EstadoReserva;
import edu.uees.tutorias.domain.Estudiante;
import edu.uees.tutorias.domain.HorarioTutoria;
import edu.uees.tutorias.domain.ModalidadTutoria;
import edu.uees.tutorias.domain.Reserva;
import edu.uees.tutorias.notification.Notificador;
import edu.uees.tutorias.persistence.RepositorioHorarios;
import edu.uees.tutorias.persistence.RepositorioHorariosEnMemoria;
import edu.uees.tutorias.persistence.RepositorioReservas;
import edu.uees.tutorias.persistence.RepositorioReservasEnMemoria;
import edu.uees.tutorias.policy.PoliticaCancelacionAnticipada;
import edu.uees.tutorias.policy.PoliticaCancelacionConPenalidad;
import edu.uees.tutorias.policy.PoliticaCancelacionDocente;
import edu.uees.tutorias.policy.ResultadoCancelacion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del servicio de reservas usando implementaciones en memoria e
 * implementaciones falsas de {@link Notificador}. Al depender el
 * servicio de interfaces (DIP) puede probarse por completo sin correo
 * real ni base de datos.
 */
class ServicioReservasTest {

    private RepositorioReservas repositorioReservas;
    private RepositorioHorarios repositorioHorarios;
    private NotificadorDePrueba notificador;
    private ServicioReservas servicioReservas;

    private Docente docente;
    private HorarioTutoria horario;
    private Estudiante estudiante;

    @BeforeEach
    void setUp() {
        repositorioReservas = new RepositorioReservasEnMemoria();
        repositorioHorarios = new RepositorioHorariosEnMemoria();
        notificador = new NotificadorDePrueba();
        servicioReservas = new ServicioReservas(repositorioReservas, repositorioHorarios, notificador);

        docente = new Docente("D001", "Ana", "Perez", "ana.perez@uees.edu.ec", "Bases de datos");
        Asignatura asignatura = new Asignatura("SIS201", "Bases de datos");
        docente.agregarAsignatura(asignatura);

        horario = new HorarioTutoria("H001", docente, asignatura,
                LocalDate.of(2026, 9, 1), LocalTime.of(10, 0), LocalTime.of(11, 0));
        repositorioHorarios.guardar(horario);

        estudiante = new Estudiante("E001", "Justin", "Arreaga",
                "justin.arreaga@uees.edu.ec", "Computacion", 5);
    }

    @Test
    void crearReservaOcupaElHorarioYNotificaAlDocente() {
        Reserva reserva = servicioReservas.crearReserva(estudiante, horario.getId());

        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
        assertEquals(EstadoHorario.RESERVADO, horario.getEstado());
        assertEquals(1, notificador.mensajes.size());
        assertEquals("ana.perez@uees.edu.ec", notificador.mensajes.get(0));
    }

    @Test
    void noSePuedeReservarUnHorarioYaReservado() {
        servicioReservas.crearReserva(estudiante, horario.getId());

        Estudiante otroEstudiante = new Estudiante("E002", "Maria", "Lopez",
                "maria.lopez@uees.edu.ec", "Computacion", 3);

        assertThrows(IllegalStateException.class,
                () -> servicioReservas.crearReserva(otroEstudiante, horario.getId()));
    }

    @Test
    void cancelarReservaLiberaElHorario() {
        Reserva reserva = servicioReservas.crearReserva(estudiante, horario.getId());

        servicioReservas.cancelarReserva(reserva.getId());

        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
        assertTrue(horario.estaDisponible());
    }

    @Test
    void confirmarReservaCambiaSuEstadoYNotificaAlEstudiante() {
        Reserva reserva = servicioReservas.crearReserva(estudiante, horario.getId());

        servicioReservas.confirmarReserva(reserva.getId());

        assertEquals(EstadoReserva.CONFIRMADA, reserva.getEstado());
        assertEquals("justin.arreaga@uees.edu.ec", notificador.mensajes.get(1));
    }

    @Test
    void noSePuedeConfirmarUnaReservaYaCancelada() {
        Reserva reserva = servicioReservas.crearReserva(estudiante, horario.getId());
        servicioReservas.cancelarReserva(reserva.getId());

        assertThrows(IllegalStateException.class,
                () -> servicioReservas.confirmarReserva(reserva.getId()));
    }

    @Test
    void elServicioAceptaUnaReservaPreconfiguradaConElBuilder() {
        Reserva reserva = servicioReservas.crearReserva(
                new ReservaBuilder()
                        .conEstudiante(estudiante)
                        .virtualCon("https://meet.uees.edu.ec/tutoria-h001")
                        .conMotivo("Consulta sobre indices")
                        .conRecordatorioDe(15),
                horario.getId());

        assertEquals(ModalidadTutoria.VIRTUAL, reserva.getModalidad());
        assertEquals("Consulta sobre indices", reserva.getMotivo());
        assertEquals(15, reserva.getRecordatorioMinutosAntes());
        assertEquals(EstadoHorario.RESERVADO, horario.getEstado());
        assertEquals(1, notificador.mensajes.size());
    }

    @Test
    void laPoliticaAnticipadaImpideCancelarSobreLaHora() {
        ServicioReservas servicioEstricto = new ServicioReservas(
                repositorioReservas, repositorioHorarios, notificador,
                new PoliticaCancelacionAnticipada());
        Reserva reserva = servicioEstricto.crearReserva(estudiante, horario.getId());

        LocalDateTime dosHorasAntes = horario.inicio().minusHours(2);

        assertThrows(IllegalStateException.class,
                () -> servicioEstricto.cancelarReserva(
                        reserva.getId(), new PoliticaCancelacionAnticipada(), dosHorasAntes));
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
        assertEquals(EstadoHorario.RESERVADO, horario.getEstado());
    }

    @Test
    void laPoliticaPorDefectoCancelaTardePeroDevuelvePenalidad() {
        Reserva reserva = servicioReservas.crearReserva(estudiante, horario.getId());

        ResultadoCancelacion resultado = servicioReservas.cancelarReserva(
                reserva.getId(), new PoliticaCancelacionConPenalidad(),
                horario.inicio().minusHours(2));

        assertTrue(resultado.permitida());
        assertTrue(resultado.tienePenalidad());
        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
        assertTrue(horario.estaDisponible());
    }

    /**
     * Mismo servicio, mismo instante tardio: cambiar la estrategia de la
     * llamada cambia el resultado sin tocar {@code ServicioReservas} ni
     * {@code Reserva}.
     */
    @Test
    void cancelarComoDocenteNoPenalizaAunqueSeaTarde() {
        Reserva reserva = servicioReservas.crearReserva(estudiante, horario.getId());

        ResultadoCancelacion resultado = servicioReservas.cancelarReserva(
                reserva.getId(), new PoliticaCancelacionDocente(),
                horario.inicio().minusHours(2));

        assertTrue(resultado.permitida());
        assertFalse(resultado.tienePenalidad());
        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
    }

    /** Doble de prueba de {@link Notificador}: registra a quien se notifico. */
    private static class NotificadorDePrueba implements Notificador {
        private final List<String> mensajes = new ArrayList<>();

        @Override
        public void notificar(String destinatario, String asunto, String mensaje) {
            mensajes.add(destinatario);
        }
    }
}

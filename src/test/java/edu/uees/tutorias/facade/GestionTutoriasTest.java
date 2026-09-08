package edu.uees.tutorias.facade;

import edu.uees.tutorias.domain.Asignatura;
import edu.uees.tutorias.domain.Docente;
import edu.uees.tutorias.domain.EstadoHorario;
import edu.uees.tutorias.domain.EstadoReserva;
import edu.uees.tutorias.domain.Estudiante;
import edu.uees.tutorias.domain.HorarioTutoria;
import edu.uees.tutorias.domain.ModalidadTutoria;
import edu.uees.tutorias.domain.Reserva;
import edu.uees.tutorias.event.EventoReserva;
import edu.uees.tutorias.event.ObservadorReserva;
import edu.uees.tutorias.event.PublicadorReservas;
import edu.uees.tutorias.event.TipoEventoReserva;
import edu.uees.tutorias.persistence.RepositorioHorarios;
import edu.uees.tutorias.persistence.RepositorioHorariosEnMemoria;
import edu.uees.tutorias.persistence.RepositorioReservas;
import edu.uees.tutorias.persistence.RepositorioReservasEnMemoria;
import edu.uees.tutorias.policy.ResultadoCancelacion;
import edu.uees.tutorias.service.ServicioReservas;
import edu.uees.tutorias.video.AdaptadorMeetInstitucional;
import edu.uees.tutorias.video.AdaptadorZoom;
import edu.uees.tutorias.video.ProveedorVideoconferencia;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas de la fachada: comprueban que <b>una</b> llamada deja el
 * sistema completo en el estado esperado —reserva guardada, horario
 * ocupado, enlace emitido por el proveedor y observadores avisados— sin
 * que el cliente conozca ninguno de esos pasos.
 */
class GestionTutoriasTest {

    private RepositorioHorarios repositorioHorarios;
    private RepositorioReservas repositorioReservas;
    private PublicadorReservas publicador;
    private ObservadorDePrueba observador;
    private ServicioReservas servicioReservas;

    private Estudiante estudiante;
    private HorarioTutoria horario;

    @BeforeEach
    void setUp() {
        repositorioHorarios = new RepositorioHorariosEnMemoria();
        repositorioReservas = new RepositorioReservasEnMemoria();
        publicador = new PublicadorReservas();
        observador = new ObservadorDePrueba();
        publicador.registrar(observador);
        servicioReservas =
                new ServicioReservas(repositorioReservas, repositorioHorarios, publicador);

        Docente docente = new Docente("D001", "Ana", "Perez", "ana.perez@uees.edu.ec", "Bases de datos");
        Asignatura asignatura = new Asignatura("SIS201", "Bases de datos");
        docente.agregarAsignatura(asignatura);

        horario = new HorarioTutoria("H001", docente, asignatura,
                LocalDate.of(2026, 10, 1), LocalTime.of(10, 0), LocalTime.of(11, 0));
        repositorioHorarios.guardar(horario);

        estudiante = new Estudiante("E001", "Justin", "Arreaga",
                "justin.arreaga@uees.edu.ec", "Computacion", 5);
    }

    private GestionTutorias fachadaCon(ProveedorVideoconferencia proveedor) {
        return new GestionTutorias(servicioReservas, repositorioHorarios, proveedor);
    }

    @Test
    void unaSolaLlamadaCreaLaTutoriaVirtualCompleta() {
        GestionTutorias gestion = fachadaCon(new AdaptadorZoom());

        Reserva reserva = gestion.crearTutoriaVirtual(
                estudiante, "H001", "Consulta sobre normalizacion");

        assertEquals(ModalidadTutoria.VIRTUAL, reserva.getModalidad());
        assertEquals("Consulta sobre normalizacion", reserva.getMotivo());
        assertEquals(EstadoReserva.PENDIENTE, reserva.getEstado());
        assertEquals(EstadoHorario.RESERVADO, horario.getEstado());
        assertTrue(repositorioReservas.buscarPorId(reserva.getId()).isPresent());
    }

    /** El enlace ya no se escribe a mano: lo emite el proveedor. */
    @Test
    void elEnlaceLoEmiteElProveedorYNoElCliente() {
        GestionTutorias gestion = fachadaCon(new AdaptadorZoom());

        Reserva reserva = gestion.crearTutoriaVirtual(estudiante, "H001", "Consulta");

        assertTrue(reserva.getEnlaceVirtual().orElseThrow().startsWith("https://uees.zoom.us/j/"));
        assertTrue(reserva.getObservaciones().orElseThrow().contains("Codigo de acceso"));
    }

    /** Cambiar de proveedor no cambia ni una linea del cliente. */
    @Test
    void cambiarDeProveedorNoCambiaLaOperacionDeAltoNivel() {
        GestionTutorias gestion = fachadaCon(new AdaptadorMeetInstitucional());

        Reserva reserva = gestion.crearTutoriaVirtual(estudiante, "H001", "Consulta");

        assertEquals("Meet institucional", gestion.proveedorDeVideoconferencia());
        assertTrue(reserva.getEnlaceVirtual().orElseThrow().startsWith("https://meet.uees.edu.ec/"));
        // Este proveedor no emite codigo, asi que no hay observaciones que anotar.
        assertTrue(reserva.getObservaciones().isEmpty());
    }

    /** La fachada coordina: el hecho llega igual a los observadores. */
    @Test
    void laFachadaSigueDisparandoLosEventosDelSubsistema() {
        GestionTutorias gestion = fachadaCon(new AdaptadorZoom());

        Reserva reserva = gestion.crearTutoriaVirtual(estudiante, "H001", "Consulta");
        gestion.confirmar(reserva.getId());

        assertEquals(List.of(TipoEventoReserva.CREADA, TipoEventoReserva.CONFIRMADA),
                observador.tipos());
    }

    /** La fachada elige la Strategy que corresponde al origen. */
    @Test
    void cancelarPorDocenteNoPenalizaAlEstudiante() {
        GestionTutorias gestion = fachadaCon(new AdaptadorZoom());
        Reserva reserva = gestion.crearTutoriaVirtual(estudiante, "H001", "Consulta");

        ResultadoCancelacion resultado = gestion.cancelarPorDocente(
                reserva.getId(), "el docente reporto incapacidad medica");

        assertTrue(resultado.permitida());
        assertFalse(resultado.tienePenalidad());
        assertEquals(EstadoReserva.CANCELADA, reserva.getEstado());
        assertTrue(horario.estaDisponible());
    }

    @Test
    void laTutoriaPresencialNoPideSalaAlProveedor() {
        GestionTutorias gestion = fachadaCon(new ProveedorQueFallaSiSeUsa());

        Reserva reserva = gestion.crearTutoriaPresencial(estudiante, "H001", "Repaso");

        assertEquals(ModalidadTutoria.PRESENCIAL, reserva.getModalidad());
        assertTrue(reserva.getEnlaceVirtual().isEmpty());
    }

    @Test
    void pedirUnHorarioInexistenteFallaAntesDeReservarLaSala() {
        GestionTutorias gestion = fachadaCon(new ProveedorQueFallaSiSeUsa());

        assertThrows(IllegalArgumentException.class,
                () -> gestion.crearTutoriaVirtual(estudiante, "H999", "Consulta"));
    }

    /** Proveedor que delata cualquier uso indebido del Adapter. */
    private static class ProveedorQueFallaSiSeUsa implements ProveedorVideoconferencia {
        @Override
        public edu.uees.tutorias.video.SalaVirtual crearSala(String titulo,
                                                             java.time.LocalDateTime inicio,
                                                             int duracionMinutos) {
            throw new AssertionError("No debia pedirse una sala en este escenario.");
        }

        @Override
        public String nombre() {
            return "proveedor no usado";
        }
    }

    /** Observer de prueba: guarda los hechos publicados. */
    private static class ObservadorDePrueba implements ObservadorReserva {
        private final List<EventoReserva> eventos = new ArrayList<>();

        @Override
        public void alOcurrir(EventoReserva evento) {
            eventos.add(evento);
        }

        @Override
        public String nombre() {
            return "observador de prueba";
        }

        private List<TipoEventoReserva> tipos() {
            return eventos.stream().map(EventoReserva::tipo).toList();
        }
    }
}

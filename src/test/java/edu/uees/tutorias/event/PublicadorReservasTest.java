package edu.uees.tutorias.event;

import edu.uees.tutorias.builder.ReservaBuilder;
import edu.uees.tutorias.domain.Asignatura;
import edu.uees.tutorias.domain.Docente;
import edu.uees.tutorias.domain.Estudiante;
import edu.uees.tutorias.domain.HorarioTutoria;
import edu.uees.tutorias.domain.Reserva;
import edu.uees.tutorias.factory.CreadorNotificador;
import edu.uees.tutorias.notification.Notificador;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del Subject y de los ConcreteObservers.
 *
 * <p>Verifican la propiedad que motivo el patron: varios receptores
 * reciben el <b>mismo</b> hecho y reaccionan distinto, sin que ninguno
 * sepa de los demas.</p>
 */
class PublicadorReservasTest {

    private PublicadorReservas publicador;
    private Reserva reserva;
    private Docente docente;

    @BeforeEach
    void setUp() {
        publicador = new PublicadorReservas();

        docente = new Docente("D001", "Ana", "Perez", "ana.perez@uees.edu.ec", "Bases de datos");
        Asignatura asignatura = new Asignatura("SIS201", "Bases de datos");
        docente.agregarAsignatura(asignatura);

        HorarioTutoria horario = new HorarioTutoria("H001", docente, asignatura,
                LocalDate.of(2026, 10, 1), LocalTime.of(10, 0), LocalTime.of(11, 0));

        Estudiante estudiante = new Estudiante("E001", "Justin", "Arreaga",
                "justin.arreaga@uees.edu.ec", "Computacion", 5);

        reserva = new ReservaBuilder()
                .conEstudiante(estudiante)
                .conHorario(horario)
                .construir();
    }

    @Test
    void unMismoEventoLlegaATodosLosObservadoresRegistrados() {
        ObservadorEspia primero = new ObservadorEspia("primero");
        ObservadorEspia segundo = new ObservadorEspia("segundo");
        publicador.registrar(primero);
        publicador.registrar(segundo);

        publicador.publicar(EventoReserva.de(TipoEventoReserva.CREADA, reserva, ""));

        assertEquals(2, publicador.cantidadDeObservadores());
        assertEquals(1, primero.recibidos.size());
        assertEquals(1, segundo.recibidos.size());
    }

    @Test
    void registrarDosVecesElMismoObservadorNoLoDuplica() {
        ObservadorEspia espia = new ObservadorEspia("unico");
        publicador.registrar(espia);
        publicador.registrar(espia);

        publicador.publicar(EventoReserva.de(TipoEventoReserva.CREADA, reserva, ""));

        assertEquals(1, publicador.cantidadDeObservadores());
        assertEquals(1, espia.recibidos.size());
    }

    @Test
    void unObservadorDadoDeBajaDejaDeRecibirEventos() {
        ObservadorEspia espia = new ObservadorEspia("temporal");
        publicador.registrar(espia);
        publicador.quitar(espia);

        publicador.publicar(EventoReserva.de(TipoEventoReserva.CONFIRMADA, reserva, ""));

        assertTrue(espia.recibidos.isEmpty());
    }

    /**
     * El fallo de un receptor no puede impedir que los demas se enteren:
     * el hecho ya ocurrio y la reserva ya cambio de estado.
     */
    @Test
    void elFalloDeUnObservadorNoImpideQueLosDemasReaccionen() {
        ObservadorEspia sano = new ObservadorEspia("sano");
        publicador.registrar(new ObservadorQueFalla());
        publicador.registrar(sano);

        publicador.publicar(EventoReserva.de(TipoEventoReserva.CANCELADA, reserva, ""));

        assertEquals(1, sano.recibidos.size());
        assertEquals(1, publicador.getFallos().size());
        assertTrue(publicador.getFallos().get(0).contains("CANCELADA"));
    }

    @Test
    void laBitacoraRegistraUnAsientoPorHecho() {
        ObservadorBitacora bitacora = new ObservadorBitacora(false);
        publicador.registrar(bitacora);

        publicador.publicar(EventoReserva.de(TipoEventoReserva.CREADA, reserva, "alta"));
        publicador.publicar(EventoReserva.de(TipoEventoReserva.CONFIRMADA, reserva, "ok"));

        assertEquals(2, bitacora.cantidadDeAsientos());
        assertTrue(bitacora.getAsientos().get(0).contains("CREADA"));
        assertTrue(bitacora.getAsientos().get(1).contains("CONFIRMADA"));
    }

    @Test
    void laAgendaDelDocenteOcupaElBloqueYLoLiberaAlCancelar() {
        ObservadorAgendaDocente agenda = new ObservadorAgendaDocente();
        publicador.registrar(agenda);

        publicador.publicar(EventoReserva.de(TipoEventoReserva.CREADA, reserva, ""));
        assertEquals(1, agenda.bloquesDe(docente.getCorreo()).size());
        assertTrue(agenda.bloquesDe(docente.getCorreo()).get(0).contains("H001"));

        publicador.publicar(EventoReserva.de(TipoEventoReserva.CANCELADA, reserva, ""));
        assertTrue(agenda.bloquesDe(docente.getCorreo()).isEmpty());
    }

    /**
     * El observador de notificacion no instancia notificadores: los pide
     * al Creator del Factory Method de Ae2. Aqui se comprueba con un
     * ConcreteCreator de prueba.
     */
    @Test
    void elObservadorDeNotificacionPideElCanalAlFactoryMethod() {
        CreadorEspia creador = new CreadorEspia();
        publicador.registrar(new ObservadorNotificacion(creador));

        publicador.publicar(EventoReserva.de(TipoEventoReserva.CREADA, reserva, ""));
        publicador.publicar(EventoReserva.de(TipoEventoReserva.CONFIRMADA, reserva, ""));

        assertEquals(2, creador.destinatarios.size());
        assertEquals("ana.perez@uees.edu.ec", creador.destinatarios.get(0));
        assertEquals("justin.arreaga@uees.edu.ec", creador.destinatarios.get(1));
        assertFalse(creador.destinatarios.isEmpty());
    }

    /** Observer de prueba: solo guarda lo que recibe. */
    private static class ObservadorEspia implements ObservadorReserva {
        private final String nombre;
        private final List<EventoReserva> recibidos = new ArrayList<>();

        private ObservadorEspia(String nombre) {
            this.nombre = nombre;
        }

        @Override
        public void alOcurrir(EventoReserva evento) {
            recibidos.add(evento);
        }

        @Override
        public String nombre() {
            return nombre;
        }
    }

    /** Observer que simula un canal caido. */
    private static class ObservadorQueFalla implements ObservadorReserva {
        @Override
        public void alOcurrir(EventoReserva evento) {
            throw new IllegalStateException("canal no disponible");
        }

        @Override
        public String nombre() {
            return "observador caido";
        }
    }

    /** ConcreteCreator de prueba que registra a quien se notifico. */
    private static class CreadorEspia extends CreadorNotificador {
        private final List<String> destinatarios = new ArrayList<>();

        @Override
        public Notificador crearNotificador() {
            return (destinatario, asunto, mensaje) -> destinatarios.add(destinatario);
        }

        @Override
        public String nombreCanal() {
            return "espia";
        }
    }
}

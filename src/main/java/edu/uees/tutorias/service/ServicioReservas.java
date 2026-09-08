package edu.uees.tutorias.service;

import edu.uees.tutorias.builder.ReservaBuilder;
import edu.uees.tutorias.domain.Estudiante;
import edu.uees.tutorias.domain.HorarioTutoria;
import edu.uees.tutorias.domain.Reserva;
import edu.uees.tutorias.event.EventoReserva;
import edu.uees.tutorias.event.PublicadorReservas;
import edu.uees.tutorias.event.TipoEventoReserva;
import edu.uees.tutorias.persistence.RepositorioHorarios;
import edu.uees.tutorias.persistence.RepositorioReservas;
import edu.uees.tutorias.policy.PoliticaCancelacion;
import edu.uees.tutorias.policy.PoliticaCancelacionConPenalidad;
import edu.uees.tutorias.policy.PoliticaCancelacionDocente;
import edu.uees.tutorias.policy.ResultadoCancelacion;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Orquesta los casos de uso de reservas: crear, confirmar, cancelar y
 * reprogramar.
 *
 * <p>No conoce como se guardan los datos ni quien reacciona a los
 * cambios: recibe esas colaboraciones por el constructor a traves de sus
 * abstracciones ({@link RepositorioReservas}, {@link RepositorioHorarios},
 * {@link PublicadorReservas}). Esto es Dependency Inversion Principle
 * aplicado: la clase de mayor nivel (el servicio) no depende de
 * implementaciones concretas de persistencia ni de mensajeria, y puede
 * probarse con implementaciones falsas (in-memory) sin levantar
 * infraestructura real.</p>
 *
 * <p>Tampoco decide directamente si un horario esta disponible ni cambia
 * el estado de una reserva: delega esas reglas en {@link HorarioTutoria}
 * y en {@link Reserva}, que son quienes deben protegerlas (alta
 * cohesion: cada clase resuelve lo que le corresponde).</p>
 *
 * <p><b>Rol en el patron Observer (Ae3):</b> el servicio ya no arma
 * destinatarios ni redacta avisos. Cada caso de uso termina publicando
 * un {@link EventoReserva}; quien reacciona —canal de mensajeria,
 * bitacora de auditoria, agenda del docente— se registra en el
 * publicador desde el composition root. Agregar un receptor ya no
 * modifica esta clase.</p>
 *
 * <p><b>Rol en el patron Strategy (Ae3):</b> esta clase es el
 * <i>Context</i>. No conoce ninguna regla de anticipacion ni de
 * penalidad: recibe una {@link PoliticaCancelacion}, le pide el
 * veredicto y actua sobre el. Cambiar el reglamento de cancelaciones es
 * cambiar la estrategia inyectada, no editar este servicio.</p>
 */
public class ServicioReservas {

    private final RepositorioReservas repositorioReservas;
    private final RepositorioHorarios repositorioHorarios;
    private final PublicadorReservas publicador;
    private final PoliticaCancelacion politicaCancelacion;

    /**
     * Construye el servicio con la politica de cancelacion por defecto
     * del sistema ({@link PoliticaCancelacionConPenalidad}), que acepta
     * toda cancelacion y traslada el costo de la tardanza al estudiante.
     */
    public ServicioReservas(RepositorioReservas repositorioReservas,
                             RepositorioHorarios repositorioHorarios,
                             PublicadorReservas publicador) {
        this(repositorioReservas, repositorioHorarios, publicador,
                new PoliticaCancelacionConPenalidad());
    }

    /**
     * Construye el servicio indicando explicitamente la politica de
     * cancelacion vigente (Strategy inyectada como colaboracion).
     */
    public ServicioReservas(RepositorioReservas repositorioReservas,
                             RepositorioHorarios repositorioHorarios,
                             PublicadorReservas publicador,
                             PoliticaCancelacion politicaCancelacion) {
        this.repositorioReservas = Objects.requireNonNull(repositorioReservas);
        this.repositorioHorarios = Objects.requireNonNull(repositorioHorarios);
        this.publicador = Objects.requireNonNull(publicador,
                "El servicio publica sus cambios a traves de un PublicadorReservas.");
        this.politicaCancelacion = Objects.requireNonNull(politicaCancelacion,
                "El servicio necesita una politica de cancelacion.");
    }

    /**
     * Crea una reserva con los valores por defecto: modalidad
     * presencial, motivo generico y recordatorio de una hora.
     */
    public Reserva crearReserva(Estudiante estudiante, String idHorario) {
        return crearReserva(new ReservaBuilder().conEstudiante(estudiante), idHorario);
    }

    /**
     * Crea una reserva a partir de un builder que el cliente ya
     * configuro (modalidad, motivo, observaciones o recordatorio).
     *
     * <p>El servicio solo completa el horario, que es el dato que
     * depende del repositorio, y delega en el builder la validacion de
     * la configuracion. Asi el servicio no crece un parametro cada vez
     * que la reserva gana un campo opcional.</p>
     */
    public Reserva crearReserva(ReservaBuilder builder, String idHorario) {
        Objects.requireNonNull(builder, "El builder de la reserva no puede ser nulo");
        HorarioTutoria horario = obtenerHorario(idHorario);

        Reserva reserva = builder.conHorario(horario).construir();
        repositorioReservas.guardar(reserva);

        publicador.publicar(EventoReserva.de(TipoEventoReserva.CREADA, reserva,
                horario.getAsignatura() + " (" + reserva.getModalidad()
                        + ": " + reserva.getMotivo() + ")"));

        return reserva;
    }

    public void confirmarReserva(String idReserva) {
        Reserva reserva = obtenerReserva(idReserva);
        reserva.confirmar();
        publicador.publicar(EventoReserva.de(TipoEventoReserva.CONFIRMADA, reserva,
                "Confirmada para el " + reserva.getHorario().inicio()));
    }

    /** Cancela aplicando la politica vigente del servicio. */
    public ResultadoCancelacion cancelarReserva(String idReserva) {
        return cancelarReserva(idReserva, politicaCancelacion, LocalDateTime.now());
    }

    /**
     * Cancela aplicando una politica distinta a la vigente, para los
     * casos en que el reglamento depende de quien origina la solicitud
     * (por ejemplo, {@link PoliticaCancelacionDocente}).
     */
    public ResultadoCancelacion cancelarReserva(String idReserva, PoliticaCancelacion politica) {
        return cancelarReserva(idReserva, politica, LocalDateTime.now());
    }

    /**
     * Operacion del Context: pide el veredicto a la Strategy y solo
     * entonces le pide a la reserva que ejecute su transicion.
     *
     * <p>El servicio no conoce ninguna regla de anticipacion ni de
     * penalidad; sabe unicamente que existe una politica a la que hay
     * que consultar. Por eso agregar una regla nueva no lo modifica.</p>
     *
     * @param momentoSolicitud instante de la solicitud, explicito para
     *                         que las reglas sean reproducibles en pruebas
     * @throws IllegalStateException si la politica rechaza la cancelacion
     */
    public ResultadoCancelacion cancelarReserva(String idReserva,
                                                PoliticaCancelacion politica,
                                                LocalDateTime momentoSolicitud) {
        Objects.requireNonNull(politica, "La cancelacion requiere una politica.");
        Reserva reserva = obtenerReserva(idReserva);

        ResultadoCancelacion resultado = politica.evaluar(reserva, momentoSolicitud);
        if (!resultado.permitida()) {
            throw new IllegalStateException("La reserva " + reserva.getId()
                    + " no puede cancelarse. " + resultado.motivo());
        }

        reserva.cancelar();

        publicador.publicar(new EventoReserva(TipoEventoReserva.CANCELADA, reserva,
                momentoSolicitud, politica.nombre() + ": " + resultado.motivo()));

        return resultado;
    }

    public void reprogramarReserva(String idReserva, String idNuevoHorario) {
        Reserva reserva = obtenerReserva(idReserva);
        HorarioTutoria nuevoHorario = obtenerHorario(idNuevoHorario);
        reserva.reprogramar(nuevoHorario);
        publicador.publicar(EventoReserva.de(TipoEventoReserva.REPROGRAMADA, reserva,
                "Nuevo horario " + nuevoHorario.getId() + " el " + nuevoHorario.inicio()));
    }

    private HorarioTutoria obtenerHorario(String idHorario) {
        return repositorioHorarios.buscarPorId(idHorario)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el horario " + idHorario));
    }

    private Reserva obtenerReserva(String idReserva) {
        Optional<Reserva> reserva = repositorioReservas.buscarPorId(idReserva);
        return reserva.orElseThrow(() -> new IllegalArgumentException(
                "No existe la reserva " + idReserva));
    }
}

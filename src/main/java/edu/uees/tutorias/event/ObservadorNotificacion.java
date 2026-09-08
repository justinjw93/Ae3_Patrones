package edu.uees.tutorias.event;

import edu.uees.tutorias.factory.CreadorNotificador;

import java.util.Objects;

/**
 * ConcreteObserver que traduce cada hecho en un aviso para la persona
 * que corresponde.
 *
 * <p><b>Aqui se encuentran los dos patrones.</b> Este observador no
 * instancia ningun {@code Notificador}: le pide el canal a un
 * {@link CreadorNotificador}, el Creator del Factory Method introducido
 * en Ae2. Cambiar el canal de todo el sistema es pasar otro
 * ConcreteCreator a este constructor. Es la razon principal por la que
 * el Factory Method se mantiene en este incremento: gano un consumidor
 * en lugar de perderlo.</p>
 *
 * <p>La logica que antes vivia dentro de {@code ServicioReservas}
 * —quien recibe el aviso de cada hecho y como se redacta— vive ahora
 * aqui, que es donde le corresponde.</p>
 */
public class ObservadorNotificacion implements ObservadorReserva {

    private final CreadorNotificador creadorNotificador;

    public ObservadorNotificacion(CreadorNotificador creadorNotificador) {
        this.creadorNotificador = Objects.requireNonNull(creadorNotificador,
                "El observador de notificacion necesita un CreadorNotificador.");
    }

    @Override
    public void alOcurrir(EventoReserva evento) {
        switch (evento.tipo()) {
            case CREADA -> creadorNotificador.enviarNotificacion(
                    evento.correoDocente(),
                    "Nueva reserva de tutoria",
                    evento.reserva().getEstudiante().nombreCompleto()
                            + " reservo el horario " + evento.reserva().getHorario().getId()
                            + ". " + evento.detalle());

            case CONFIRMADA -> creadorNotificador.enviarNotificacion(
                    evento.correoEstudiante(),
                    "Tutoria confirmada",
                    "Tu reserva " + evento.reserva().getId() + " fue confirmada.");

            case CANCELADA -> creadorNotificador.enviarNotificacion(
                    evento.correoDocente(),
                    "Reserva cancelada",
                    "La reserva " + evento.reserva().getId() + " fue cancelada. "
                            + evento.detalle());

            case REPROGRAMADA -> creadorNotificador.enviarNotificacion(
                    evento.correoEstudiante(),
                    "Tutoria reprogramada",
                    "Tu reserva " + evento.reserva().getId() + " cambio de horario. "
                            + evento.detalle());
        }
    }

    @Override
    public String nombre() {
        return "Notificacion por " + creadorNotificador.nombreCanal();
    }
}

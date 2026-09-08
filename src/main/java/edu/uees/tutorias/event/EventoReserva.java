package edu.uees.tutorias.event;

import edu.uees.tutorias.domain.Reserva;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Mensaje que viaja del Subject a sus Observers cuando cambia una
 * reserva.
 *
 * <p>Lleva la reserva completa —y no solo su identificador— para que
 * cada observador extraiga lo que necesita sin volver a consultar el
 * repositorio: el canal de mensajeria necesita el correo del docente,
 * la agenda necesita el horario y la bitacora necesita el estado. El
 * campo {@code detalle} transporta el contexto propio de cada hecho (la
 * penalidad aplicada, el horario destino de una reprogramacion).</p>
 *
 * @param tipo    hecho ocurrido
 * @param reserva reserva afectada, ya en su estado nuevo
 * @param momento instante en que ocurrio
 * @param detalle contexto adicional legible; nunca {@code null}
 */
public record EventoReserva(TipoEventoReserva tipo,
                            Reserva reserva,
                            LocalDateTime momento,
                            String detalle) {

    public EventoReserva {
        Objects.requireNonNull(tipo, "Todo evento debe indicar su tipo.");
        Objects.requireNonNull(reserva, "Todo evento debe referirse a una reserva.");
        Objects.requireNonNull(momento, "Todo evento debe indicar cuando ocurrio.");
        detalle = (detalle == null) ? "" : detalle;
    }

    /** Evento fechado en el instante actual. */
    public static EventoReserva de(TipoEventoReserva tipo, Reserva reserva, String detalle) {
        return new EventoReserva(tipo, reserva, LocalDateTime.now(), detalle);
    }

    /** Correo del estudiante que solicito la tutoria. */
    public String correoEstudiante() {
        return reserva.getEstudiante().getCorreo();
    }

    /** Correo del docente que publico el horario. */
    public String correoDocente() {
        return reserva.getHorario().getDocente().getCorreo();
    }
}

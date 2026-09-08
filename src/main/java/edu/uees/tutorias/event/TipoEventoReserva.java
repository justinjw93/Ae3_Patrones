package edu.uees.tutorias.event;

/**
 * Hechos del ciclo de vida de una reserva que el sistema publica a sus
 * observadores.
 *
 * <p>Deliberadamente son <b>hechos ocurridos</b>, no ordenes: el
 * publicador anuncia "la reserva se cancelo", no "envia un correo". Que
 * hacer con ese hecho lo decide cada observador.</p>
 */
public enum TipoEventoReserva {
    CREADA,
    CONFIRMADA,
    CANCELADA,
    REPROGRAMADA
}

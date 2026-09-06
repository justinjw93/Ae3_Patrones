package edu.uees.tutorias.factory;

import edu.uees.tutorias.notification.NotificadorLog;
import edu.uees.tutorias.notification.Notificador;

/**
 * ConcreteCreator que fabrica {@link NotificadorLog}, es decir, notificaciones
 * entregadas por un registro de auditoria interno con marca de tiempo.
 *
 * <p>Es el unico punto del sistema que menciona a {@code NotificadorLog}:
 * ningun cliente necesita conocer esa clase concreta para notificar.</p>
 */
public class CreadorNotificadorLog extends CreadorNotificador {

    @Override
    public Notificador crearNotificador() {
        return new NotificadorLog();
    }

    @Override
    public String nombreCanal() {
        return "log-auditoria";
    }
}

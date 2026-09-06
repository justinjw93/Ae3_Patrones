package edu.uees.tutorias.factory;

import edu.uees.tutorias.notification.NotificadorCorreo;
import edu.uees.tutorias.notification.Notificador;

/**
 * ConcreteCreator que fabrica {@link NotificadorCorreo}, es decir, notificaciones
 * entregadas por el correo institucional de la UEES.
 *
 * <p>Es el unico punto del sistema que menciona a {@code NotificadorCorreo}:
 * ningun cliente necesita conocer esa clase concreta para notificar.</p>
 */
public class CreadorNotificadorCorreo extends CreadorNotificador {

    @Override
    public Notificador crearNotificador() {
        return new NotificadorCorreo();
    }

    @Override
    public String nombreCanal() {
        return "correo-institucional";
    }
}

package edu.uees.tutorias.factory;

import edu.uees.tutorias.notification.NotificadorConsola;
import edu.uees.tutorias.notification.Notificador;

/**
 * ConcreteCreator que fabrica {@link NotificadorConsola}, es decir, notificaciones
 * entregadas por la salida estandar, util en desarrollo y demostraciones.
 *
 * <p>Es el unico punto del sistema que menciona a {@code NotificadorConsola}:
 * ningun cliente necesita conocer esa clase concreta para notificar.</p>
 */
public class CreadorNotificadorConsola extends CreadorNotificador {

    @Override
    public Notificador crearNotificador() {
        return new NotificadorConsola();
    }

    @Override
    public String nombreCanal() {
        return "consola";
    }
}

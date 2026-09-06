package edu.uees.tutorias.factory;

import edu.uees.tutorias.notification.Notificador;
import edu.uees.tutorias.notification.NotificadorSms;

/**
 * ConcreteCreator agregado en Ae2 como evidencia de extensibilidad.
 *
 * <p>Incorporar el canal SMS al sistema consistio en escribir dos
 * archivos nuevos ({@link NotificadorSms} y esta clase) sin modificar
 * ninguno existente: ni el Product {@code Notificador}, ni el Creator
 * {@link CreadorNotificador}, ni los ConcreteCreators anteriores, ni
 * {@code ServicioReservas}, ni las pruebas ya escritas.</p>
 *
 * <p>Ademas concentra la configuracion del canal: el numero de origen
 * institucional queda aqui y no en el cliente que envia el aviso.</p>
 */
public class CreadorNotificadorSms extends CreadorNotificador {

    private static final String NUMERO_ORIGEN_UEES = "+593-99-000-0000";

    private final String numeroOrigen;

    public CreadorNotificadorSms() {
        this(NUMERO_ORIGEN_UEES);
    }

    public CreadorNotificadorSms(String numeroOrigen) {
        this.numeroOrigen = numeroOrigen;
    }

    @Override
    public Notificador crearNotificador() {
        return new NotificadorSms(numeroOrigen);
    }

    @Override
    public String nombreCanal() {
        return "sms";
    }
}

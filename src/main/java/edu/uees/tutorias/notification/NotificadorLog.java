package edu.uees.tutorias.notification;

import java.time.LocalDateTime;

/**
 * Segunda implementacion de {@link Notificador}, agregada para
 * demostrar el principio Open/Closed: representa, por ejemplo, un canal
 * de auditoria/registro interno distinto del correo al usuario.
 *
 * <p>Se agrega este archivo sin modificar {@code ServicioReservas} ni
 * {@code NotificadorConsola}: basta con construir
 * {@code new ServicioReservas(repo, repoHorarios, new NotificadorLog())}
 * para que el servicio use este canal en lugar de otro, porque solo
 * conoce el contrato {@link Notificador}.</p>
 *
 * <p><b>Rol en el patron Factory Method (Ae2):</b> ConcreteProduct que registra el mensaje en un canal de auditoria.</p>
 */
public class NotificadorLog implements Notificador {

    @Override
    public void notificar(String destinatario, String asunto, String mensaje) {
        System.out.println("[LOG " + LocalDateTime.now() + "] destinatario=" + destinatario
                + " | asunto=" + asunto + " | mensaje=" + mensaje);
    }
}

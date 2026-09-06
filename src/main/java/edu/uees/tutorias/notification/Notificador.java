package edu.uees.tutorias.notification;

/**
 * Contrato para el envio de notificaciones a los usuarios del sistema.
 *
 * <p>{@code ServicioReservas} depende de esta abstraccion, no de un
 * mecanismo de envio concreto (correo SMTP, SMS, push). Cualquier nuevo
 * canal de notificacion se agrega implementando esta interfaz, sin
 * modificar el servicio que la utiliza (Open/Closed + Dependency
 * Inversion).</p>
 *
 * <p><b>Rol en el patron Factory Method (Ae2):</b> esta interfaz es el
 * <i>Product</i>. Sus implementaciones ({@link NotificadorConsola},
 * {@link NotificadorLog}, {@link NotificadorCorreo}) son los
 * <i>ConcreteProducts</i>, y quien decide cual instanciar es la
 * jerarquia {@code CreadorNotificador} del paquete
 * {@code edu.uees.tutorias.factory}. El contrato no cambia al agregar
 * canales: es el punto estable del diseno.</p>
 */
public interface Notificador {

    void notificar(String destinatario, String asunto, String mensaje);
}

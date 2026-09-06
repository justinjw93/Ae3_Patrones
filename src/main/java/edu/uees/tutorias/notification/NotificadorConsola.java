package edu.uees.tutorias.notification;

/**
 * Implementacion de {@link Notificador} que imprime el mensaje en
 * consola. Sustituye, para efectos de este proyecto academico, a un
 * proveedor real de correo electronico (por ejemplo SMTP); representa el
 * mismo rol dentro del contrato y puede reemplazarse sin tocar
 * {@code ServicioReservas}.
 *
 * <p><b>Rol en el patron Factory Method (Ae2):</b> ConcreteProduct que entrega el mensaje por la salida estandar.</p>
 */
public class NotificadorConsola implements Notificador {

    @Override
    public void notificar(String destinatario, String asunto, String mensaje) {
        System.out.println("--- Notificacion ---");
        System.out.println("Para: " + destinatario);
        System.out.println("Asunto: " + asunto);
        System.out.println(mensaje);
        System.out.println("---------------------");
    }
}

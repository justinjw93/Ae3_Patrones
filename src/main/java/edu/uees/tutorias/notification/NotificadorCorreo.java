package edu.uees.tutorias.notification;

/**
 * ConcreteProduct que representa el envio de la notificacion por correo
 * electronico institucional.
 *
 * <p>En un entorno real esta clase abriria una sesion SMTP con las
 * credenciales del dominio {@code uees.edu.ec}; para efectos del
 * proyecto academico la entrega se simula por consola, pero conserva lo
 * que distingue al canal: un remitente institucional fijo y un formato
 * de mensaje con encabezados de correo.</p>
 *
 * <p>Esa configuracion propia del canal es, precisamente, el motivo por
 * el que la creacion de notificadores se delega a un Creator: el cliente
 * no deberia tener que conocer el remitente ni el formato para poder
 * notificar.</p>
 */
public class NotificadorCorreo implements Notificador {

    private static final String REMITENTE = "tutorias@uees.edu.ec";

    private final String remitente;

    public NotificadorCorreo() {
        this(REMITENTE);
    }

    public NotificadorCorreo(String remitente) {
        this.remitente = remitente;
    }

    @Override
    public void notificar(String destinatario, String asunto, String mensaje) {
        System.out.println("=== Correo electronico ===");
        System.out.println("De: " + remitente);
        System.out.println("Para: " + destinatario);
        System.out.println("Asunto: " + asunto);
        System.out.println();
        System.out.println(mensaje);
        System.out.println("==========================");
    }
}

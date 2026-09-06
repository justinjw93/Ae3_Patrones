package edu.uees.tutorias.notification;

/**
 * ConcreteProduct agregado en Ae2 para evidenciar la extensibilidad del
 * Factory Method: entrega la notificacion como mensaje de texto.
 *
 * <p>Tiene una restriccion propia que ningun otro canal comparte: el
 * mensaje se recorta a {@value #LARGO_MAXIMO} caracteres. Esa clase de
 * particularidad es la razon por la que el canal es un objeto y no un
 * parametro: cada uno decide como entrega el mensaje.</p>
 */
public class NotificadorSms implements Notificador {

    /** Longitud maxima de un mensaje de texto estandar. */
    public static final int LARGO_MAXIMO = 160;

    private final String numeroOrigen;

    public NotificadorSms(String numeroOrigen) {
        this.numeroOrigen = numeroOrigen;
    }

    @Override
    public void notificar(String destinatario, String asunto, String mensaje) {
        String texto = asunto + ": " + mensaje;
        if (texto.length() > LARGO_MAXIMO) {
            texto = texto.substring(0, LARGO_MAXIMO - 3) + "...";
        }
        System.out.println("[SMS " + numeroOrigen + " -> " + destinatario + "] " + texto);
    }

    /** Expone el texto que se enviaria, para poder verificar el recorte. */
    public String componerTexto(String asunto, String mensaje) {
        String texto = asunto + ": " + mensaje;
        return texto.length() > LARGO_MAXIMO
                ? texto.substring(0, LARGO_MAXIMO - 3) + "..."
                : texto;
    }
}

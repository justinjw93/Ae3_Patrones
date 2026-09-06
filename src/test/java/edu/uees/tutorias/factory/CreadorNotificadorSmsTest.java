package edu.uees.tutorias.factory;

import edu.uees.tutorias.notification.NotificadorSms;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de la variante agregada despues de tener el patron funcionando.
 * Se escribe sin tocar ninguna prueba anterior, igual que el codigo de
 * produccion: es la evidencia de que el diseno quedo abierto a extension
 * y cerrado a modificacion.
 */
class CreadorNotificadorSmsTest {

    @Test
    void elCreadorSmsSeIntegraSinModificarLaJerarquiaExistente() {
        CreadorNotificador creador = new CreadorNotificadorSms();

        assertInstanceOf(NotificadorSms.class, creador.crearNotificador());
        assertEquals("sms", creador.nombreCanal());
    }

    @Test
    void elCanalSmsRecortaLosMensajesLargos() {
        NotificadorSms sms = (NotificadorSms) new CreadorNotificadorSms().crearNotificador();

        String texto = sms.componerTexto("Recordatorio", "x".repeat(300));

        assertEquals(NotificadorSms.LARGO_MAXIMO, texto.length());
        assertTrue(texto.endsWith("..."));
    }
}

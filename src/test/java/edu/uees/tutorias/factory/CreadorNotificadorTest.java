package edu.uees.tutorias.factory;

import edu.uees.tutorias.notification.Notificador;
import edu.uees.tutorias.notification.NotificadorConsola;
import edu.uees.tutorias.notification.NotificadorCorreo;
import edu.uees.tutorias.notification.NotificadorLog;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del Factory Method aplicado a los canales de notificacion.
 */
class CreadorNotificadorTest {

    @Test
    void cadaCreadorConcretoFabricaSuPropioProducto() {
        assertInstanceOf(NotificadorConsola.class,
                new CreadorNotificadorConsola().crearNotificador());
        assertInstanceOf(NotificadorLog.class,
                new CreadorNotificadorLog().crearNotificador());
        assertInstanceOf(NotificadorCorreo.class,
                new CreadorNotificadorCorreo().crearNotificador());
    }

    @Test
    void elClienteEnviaPorCualquierCanalSinConocerLaClaseConcreta() {
        List<CreadorNotificador> creadores = List.of(
                new CreadorNotificadorConsola(),
                new CreadorNotificadorLog(),
                new CreadorNotificadorCorreo());

        for (CreadorNotificador creador : creadores) {
            assertInstanceOf(Notificador.class, creador.crearNotificador());
            assertTrue(creador.nombreCanal() != null && !creador.nombreCanal().isBlank());
        }
    }

    @Test
    void laOperacionDelCreatorAnteponeElCanalAlMensaje() {
        CreadorEspia creador = new CreadorEspia();

        creador.enviarNotificacion("ana.perez@uees.edu.ec", "Asunto", "Cuerpo del mensaje");

        assertEquals(1, creador.notificador.mensajes.size());
        assertEquals("[canal-espia] Cuerpo del mensaje", creador.notificador.mensajes.get(0));
    }

    /**
     * ConcreteCreator de prueba: demuestra que agregar un canal nuevo no
     * exige tocar la jerarquia existente, solo extenderla.
     */
    private static class CreadorEspia extends CreadorNotificador {
        private final NotificadorEspia notificador = new NotificadorEspia();

        @Override
        public Notificador crearNotificador() {
            return notificador;
        }

        @Override
        public String nombreCanal() {
            return "canal-espia";
        }
    }

    private static class NotificadorEspia implements Notificador {
        private final List<String> mensajes = new ArrayList<>();

        @Override
        public void notificar(String destinatario, String asunto, String mensaje) {
            mensajes.add(mensaje);
        }
    }
}

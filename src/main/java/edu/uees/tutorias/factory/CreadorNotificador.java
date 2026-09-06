package edu.uees.tutorias.factory;

import edu.uees.tutorias.notification.Notificador;

/**
 * Creator del patron Factory Method: declara la operacion de fabrica
 * {@link #crearNotificador()} y define, sobre ella, el comportamiento
 * comun a todos los canales de notificacion.
 *
 * <p><b>Problema que resuelve.</b> Antes de aplicar el patron, el
 * cliente (por ejemplo {@code App}) escribia {@code new
 * NotificadorConsola()} y quedaba amarrado a una clase concreta: para
 * cambiar de canal habia que editar ese codigo, y la configuracion
 * propia de cada canal (remitente institucional, prefijo de auditoria,
 * numero de origen del SMS) se filtraba hacia el cliente. El Creator
 * concentra esa decision y esa configuracion en un solo lugar.</p>
 *
 * <p><b>Como funciona.</b> Esta clase no sabe que clase concreta se va
 * a instanciar: eso lo decide cada subclase. Lo que si sabe es que
 * <i>todo</i> envio del sistema debe llevar identificado su canal, y por
 * eso {@link #enviarNotificacion(String, String, String)} es codigo
 * estable y compartido que se apoya en el producto que la subclase
 * fabrique.</p>
 *
 * <p><b>Que cambia y que permanece estable.</b> Al agregar un canal
 * nuevo se escriben dos archivos nuevos (un ConcreteProduct y un
 * ConcreteCreator) y no se modifica ninguno existente: ni
 * {@link Notificador}, ni esta clase, ni los creadores ya escritos, ni
 * {@code ServicioReservas}.</p>
 */
public abstract class CreadorNotificador {

    /**
     * Operacion de fabrica (Factory Method). Cada subclase decide que
     * ConcreteProduct construir y con que configuracion.
     *
     * @return una instancia lista para usar del canal correspondiente
     */
    public abstract Notificador crearNotificador();

    /**
     * Nombre legible del canal, usado por la operacion comun para dejar
     * trazabilidad de por donde salio cada mensaje.
     */
    public abstract String nombreCanal();

    /**
     * Operacion del Creator que usa el producto fabricado. Es el codigo
     * que permanece estable cuando aparecen canales nuevos: siempre
     * antepone el canal al mensaje y delega el envio real al producto.
     */
    public void enviarNotificacion(String destinatario, String asunto, String mensaje) {
        Notificador notificador = crearNotificador();
        notificador.notificar(destinatario, asunto, "[" + nombreCanal() + "] " + mensaje);
    }
}

package edu.uees.tutorias.event;

/**
 * Observer: componente interesado en enterarse de los cambios de una
 * reserva.
 *
 * <p><b>Problema que resuelve.</b> Antes de aplicar el patron,
 * {@code ServicioReservas} armaba a mano el destinatario, el asunto y el
 * cuerpo del aviso dentro de sus cuatro casos de uso, y llamaba
 * directamente a {@code Notificador}. El servicio conocia asi <i>quien</i>
 * debia enterarse y <i>como</i> se redactaba el mensaje, cuando su
 * responsabilidad es orquestar el caso de uso. Cada receptor nuevo
 * —bitacora de auditoria, agenda del docente, panel de coordinacion—
 * obligaba a editar los cuatro metodos: una violacion directa del
 * Open/Closed Principle.</p>
 *
 * <p><b>Que cambia y que permanece estable.</b> Cambia la lista de
 * interesados y su reaccion: cada uno es un ConcreteObserver. Permanece
 * estable el servicio, que solo publica el hecho a
 * {@link PublicadorReservas} y no sabe cuantos escuchan ni que hacen.
 * Agregar un receptor es agregar una clase y registrarla en el
 * composition root.</p>
 */
public interface ObservadorReserva {

    /**
     * Reacciona a un hecho ocurrido sobre una reserva.
     *
     * <p>Un observador no debe lanzar excepciones para abortar el caso
     * de uso: el hecho ya ocurrio y la reserva ya cambio de estado. Si
     * su reaccion falla, es su responsabilidad manejarlo.</p>
     */
    void alOcurrir(EventoReserva evento);

    /** Nombre legible del observador, util en la bitacora y en pruebas. */
    String nombre();
}

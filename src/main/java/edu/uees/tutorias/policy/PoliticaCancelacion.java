package edu.uees.tutorias.policy;

import edu.uees.tutorias.domain.Reserva;

import java.time.LocalDateTime;

/**
 * Strategy: regla que decide si una cancelacion procede y con que costo.
 *
 * <p><b>Problema que resuelve.</b> Antes de aplicar el patron, la unica
 * regla de cancelacion que existia estaba congelada dentro de
 * {@code Reserva.cancelar()}: comprobar el estado actual. El reglamento
 * de tutorias distingue casos que ahi no se podian expresar —cancelar
 * con mas de 24 horas de anticipacion es libre, hacerlo despues genera
 * una penalidad, y una cancelacion originada por el docente no penaliza
 * a nadie—. Escribirlos dentro de la reserva habria convertido un metodo
 * de dominio en una cadena de {@code if} sobre fechas y roles, mezclando
 * la <i>transicion de estado</i> (que si le corresponde a la reserva)
 * con la <i>politica institucional</i> (que cambia por normativa).</p>
 *
 * <p><b>Que cambia y que permanece estable.</b> Cambia el criterio de
 * aceptacion y el calculo de la penalidad: eso vive en los
 * ConcreteStrategies. Permanece estable la transicion
 * {@code -> CANCELADA} y la liberacion del horario, que siguen dentro de
 * {@link Reserva}. Agregar una politica nueva es agregar una clase, sin
 * tocar las existentes ni el servicio (Open/Closed).</p>
 *
 * <p>El Context es {@code ServicioReservas}, que recibe la politica por
 * constructor y puede recibir otra por llamada cuando el caso lo
 * amerita (por ejemplo, una cancelacion originada por el docente).</p>
 */
public interface PoliticaCancelacion {

    /**
     * Evalua la solicitud de cancelacion de una reserva.
     *
     * <p>La politica <b>no</b> cancela nada: solo emite un veredicto. Es
     * el servicio quien, ante un veredicto favorable, pide a la reserva
     * que ejecute su transicion.</p>
     *
     * @param reserva          reserva sobre la que se solicita la cancelacion
     * @param momentoSolicitud instante en que se solicita, recibido como
     *                         parametro (y no leido de {@code now()})
     *                         para que la regla sea reproducible en pruebas
     * @return el veredicto, nunca {@code null}
     */
    ResultadoCancelacion evaluar(Reserva reserva, LocalDateTime momentoSolicitud);

    /** Nombre legible de la politica, usado en avisos y bitacora. */
    String nombre();
}

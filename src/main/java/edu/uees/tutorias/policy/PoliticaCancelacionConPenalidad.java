package edu.uees.tutorias.policy;

import edu.uees.tutorias.domain.Reserva;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * ConcreteStrategy flexible: nunca bloquea al estudiante, pero cobra un
 * porcentaje cuando la cancelacion llega tarde.
 *
 * <p>Es la politica por defecto del sistema. Prefiere que el bloque
 * quede libre —aunque sea a ultima hora— antes que forzar una ausencia,
 * y traslada el costo de la tardanza al estudiante en lugar de negar la
 * operacion.</p>
 */
public class PoliticaCancelacionConPenalidad implements PoliticaCancelacion {

    /** Anticipacion a partir de la cual la cancelacion deja de ser gratuita. */
    public static final int HORAS_SIN_COSTO_POR_DEFECTO = 24;

    /** Porcentaje que se cobra al cancelar fuera de ese plazo. */
    public static final int PENALIDAD_POR_DEFECTO = 50;

    private final int horasSinCosto;
    private final int penalidadPorcentaje;

    public PoliticaCancelacionConPenalidad() {
        this(HORAS_SIN_COSTO_POR_DEFECTO, PENALIDAD_POR_DEFECTO);
    }

    public PoliticaCancelacionConPenalidad(int horasSinCosto, int penalidadPorcentaje) {
        if (horasSinCosto <= 0) {
            throw new IllegalArgumentException(
                    "Las horas sin costo deben ser positivas, pero fueron " + horasSinCosto);
        }
        if (penalidadPorcentaje <= 0 || penalidadPorcentaje > 100) {
            throw new IllegalArgumentException(
                    "La penalidad debe estar entre 1 y 100, pero fue " + penalidadPorcentaje);
        }
        this.horasSinCosto = horasSinCosto;
        this.penalidadPorcentaje = penalidadPorcentaje;
    }

    @Override
    public ResultadoCancelacion evaluar(Reserva reserva, LocalDateTime momentoSolicitud) {
        long horasRestantes = Duration.between(
                momentoSolicitud, reserva.getHorario().inicio()).toHours();

        if (horasRestantes >= horasSinCosto) {
            return ResultadoCancelacion.permitida(
                    "Cancelacion sin costo: faltan " + horasRestantes + " h para la tutoria.");
        }
        return ResultadoCancelacion.permitidaConPenalidad(penalidadPorcentaje,
                "Cancelacion tardia: quedan " + horasRestantes + " h y se aplica "
                        + penalidadPorcentaje + " % de penalidad.");
    }

    @Override
    public String nombre() {
        return "Cancelacion con penalidad (" + penalidadPorcentaje + " % bajo "
                + horasSinCosto + " h)";
    }

    public int getHorasSinCosto() {
        return horasSinCosto;
    }

    public int getPenalidadPorcentaje() {
        return penalidadPorcentaje;
    }
}

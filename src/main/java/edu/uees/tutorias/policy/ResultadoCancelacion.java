package edu.uees.tutorias.policy;

import java.util.Objects;

/**
 * Veredicto que devuelve una {@link PoliticaCancelacion} al evaluar una
 * solicitud de cancelacion.
 *
 * <p>Separar el veredicto del acto de cancelar permite que la politica
 * responda "si, pero con penalidad" sin tener que modificar la reserva:
 * quien decide (la politica) y quien ejecuta la transicion (la
 * {@code Reserva}) siguen siendo responsabilidades distintas.</p>
 *
 * @param permitida            si la cancelacion procede
 * @param penalidadPorcentaje  porcentaje del costo de la tutoria que se
 *                             cobra al estudiante; 0 cuando no aplica
 * @param motivo               explicacion legible del veredicto
 */
public record ResultadoCancelacion(boolean permitida, int penalidadPorcentaje, String motivo) {

    public ResultadoCancelacion {
        Objects.requireNonNull(motivo, "El resultado de la cancelacion debe explicar su motivo.");
        if (penalidadPorcentaje < 0 || penalidadPorcentaje > 100) {
            throw new IllegalArgumentException(
                    "La penalidad debe estar entre 0 y 100, pero fue " + penalidadPorcentaje);
        }
        if (!permitida && penalidadPorcentaje != 0) {
            throw new IllegalArgumentException(
                    "Una cancelacion rechazada no puede llevar penalidad.");
        }
    }

    /** Cancelacion aceptada sin costo para el estudiante. */
    public static ResultadoCancelacion permitida(String motivo) {
        return new ResultadoCancelacion(true, 0, motivo);
    }

    /** Cancelacion aceptada, pero con un cargo para el estudiante. */
    public static ResultadoCancelacion permitidaConPenalidad(int penalidadPorcentaje, String motivo) {
        return new ResultadoCancelacion(true, penalidadPorcentaje, motivo);
    }

    /** Cancelacion rechazada por la politica vigente. */
    public static ResultadoCancelacion rechazada(String motivo) {
        return new ResultadoCancelacion(false, 0, motivo);
    }

    public boolean tienePenalidad() {
        return penalidadPorcentaje > 0;
    }
}

package edu.uees.tutorias.policy;

import edu.uees.tutorias.domain.Reserva;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * ConcreteStrategy estricto: solo acepta cancelaciones solicitadas con
 * la anticipacion minima exigida por el reglamento.
 *
 * <p>Es la politica de las tutorias con cupo limitado, donde una
 * cancelacion tardia deja el bloque perdido porque ya no alcanza a
 * reasignarse a otro estudiante. No cobra penalidad: simplemente no
 * permite cancelar fuera de plazo.</p>
 */
public class PoliticaCancelacionAnticipada implements PoliticaCancelacion {

    /** Anticipacion exigida por el reglamento cuando no se indica otra. */
    public static final int HORAS_MINIMAS_POR_DEFECTO = 24;

    private final int horasMinimas;

    public PoliticaCancelacionAnticipada() {
        this(HORAS_MINIMAS_POR_DEFECTO);
    }

    public PoliticaCancelacionAnticipada(int horasMinimas) {
        if (horasMinimas <= 0) {
            throw new IllegalArgumentException(
                    "La anticipacion minima debe ser positiva, pero fue " + horasMinimas);
        }
        this.horasMinimas = horasMinimas;
    }

    @Override
    public ResultadoCancelacion evaluar(Reserva reserva, LocalDateTime momentoSolicitud) {
        long horasRestantes = Duration.between(
                momentoSolicitud, reserva.getHorario().inicio()).toHours();

        if (horasRestantes >= horasMinimas) {
            return ResultadoCancelacion.permitida(
                    "Cancelacion anticipada: faltan " + horasRestantes
                            + " h para la tutoria (minimo " + horasMinimas + " h).");
        }
        return ResultadoCancelacion.rechazada(
                "Fuera de plazo: la cancelacion exige " + horasMinimas
                        + " h de anticipacion y solo faltan " + horasRestantes + " h.");
    }

    @Override
    public String nombre() {
        return "Cancelacion anticipada (" + horasMinimas + " h)";
    }

    public int getHorasMinimas() {
        return horasMinimas;
    }
}

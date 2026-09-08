package edu.uees.tutorias.policy;

import edu.uees.tutorias.domain.Reserva;

import java.time.LocalDateTime;

/**
 * ConcreteStrategy para cancelaciones originadas por el docente:
 * siempre proceden y jamas penalizan al estudiante.
 *
 * <p>Existe porque el reglamento distingue quien cancela, no solo
 * cuando. Si un docente enferma o cae un feriado, el estudiante no
 * puede quedar bloqueado por el plazo ni cargado con una penalidad que
 * no origino. Esta politica se pasa por llamada, no por constructor:
 * el mismo servicio atiende cancelaciones de estudiante y de docente
 * cambiando unicamente la estrategia que recibe.</p>
 */
public class PoliticaCancelacionDocente implements PoliticaCancelacion {

    private final String justificacion;

    public PoliticaCancelacionDocente() {
        this("cancelacion administrativa del docente");
    }

    public PoliticaCancelacionDocente(String justificacion) {
        this.justificacion = (justificacion == null || justificacion.isBlank())
                ? "cancelacion administrativa del docente"
                : justificacion.trim();
    }

    @Override
    public ResultadoCancelacion evaluar(Reserva reserva, LocalDateTime momentoSolicitud) {
        return ResultadoCancelacion.permitida(
                "Sin penalidad para el estudiante: " + justificacion + ".");
    }

    @Override
    public String nombre() {
        return "Cancelacion originada por el docente";
    }
}

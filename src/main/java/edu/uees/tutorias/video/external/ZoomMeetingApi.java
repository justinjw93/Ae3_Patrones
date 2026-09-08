package edu.uees.tutorias.video.external;

import java.util.Map;

/**
 * Adaptee: simulacion del SDK de Zoom.
 *
 * <p><b>Esta clase representa codigo de terceros y por eso no se
 * modifica.</b> Es la razon de ser del Adapter: si pudiera cambiarse su
 * firma para que encajara con el sistema, el patron sobraria. Reproduce
 * las incompatibilidades reales de una API de este tipo:</p>
 *
 * <ul>
 *   <li>recibe los parametros en un {@code Map}, sin tipos ni nombres
 *       verificados por el compilador;</li>
 *   <li>espera el inicio como <i>epoch millis</i> en UTC, no como fecha
 *       local;</li>
 *   <li>expresa la duracion en <b>segundos</b>, no en minutos;</li>
 *   <li>responde con una cadena en su propio formato
 *       {@code clave=valor;clave=valor}, no con un objeto;</li>
 *   <li>falla con excepciones propias cuando falta una clave.</li>
 * </ul>
 *
 * <p>La sesion no llega a la red: los identificadores se derivan de los
 * parametros para que la demostracion sea reproducible.</p>
 */
public class ZoomMeetingApi {

    /** Duracion maxima que acepta el plan institucional, en segundos. */
    private static final int DURACION_MAXIMA_SEGUNDOS = 4 * 60 * 60;

    /**
     * Agenda una reunion.
     *
     * @param parametros claves obligatorias {@code topic} (String),
     *                   {@code start_time} (Long, epoch millis UTC) y
     *                   {@code duration} (Integer, segundos)
     * @return {@code meeting_id=...;join_url=...;passcode=...}
     * @throws IllegalArgumentException si falta una clave o su tipo no corresponde
     */
    public String scheduleMeeting(Map<String, Object> parametros) {
        String topic = leerTexto(parametros, "topic");
        long startTime = leerEntero(parametros, "start_time");
        long duration = leerEntero(parametros, "duration");

        if (duration <= 0 || duration > DURACION_MAXIMA_SEGUNDOS) {
            throw new IllegalArgumentException(
                    "zoom: 'duration' fuera de rango (segundos): " + duration);
        }

        long meetingId = Math.abs((topic + startTime).hashCode()) % 9_000_000_000L + 1_000_000_000L;
        String passcode = Long.toString(Math.abs(topic.hashCode()), 36).toUpperCase();

        return "meeting_id=" + meetingId
                + ";join_url=https://uees.zoom.us/j/" + meetingId
                + ";passcode=" + passcode;
    }

    private String leerTexto(Map<String, Object> parametros, String clave) {
        Object valor = parametros.get(clave);
        if (!(valor instanceof String texto) || texto.isBlank()) {
            throw new IllegalArgumentException("zoom: falta la clave obligatoria '" + clave + "'");
        }
        return texto;
    }

    private long leerEntero(Map<String, Object> parametros, String clave) {
        Object valor = parametros.get(clave);
        if (!(valor instanceof Number numero)) {
            throw new IllegalArgumentException(
                    "zoom: la clave '" + clave + "' debe ser numerica");
        }
        return numero.longValue();
    }
}

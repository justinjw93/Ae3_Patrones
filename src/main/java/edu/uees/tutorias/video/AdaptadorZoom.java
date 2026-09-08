package edu.uees.tutorias.video;

import edu.uees.tutorias.video.external.ZoomMeetingApi;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Adapter (por composicion) entre el contrato
 * {@link ProveedorVideoconferencia} y el SDK {@link ZoomMeetingApi}.
 *
 * <p>Toda la incompatibilidad del proveedor queda encerrada en esta
 * clase: aqui —y solo aqui— se sabe que Zoom quiere un {@code Map}, que
 * mide el tiempo en epoch millis UTC, que cuenta la duracion en segundos
 * y que responde con una cadena {@code clave=valor;...}. El servicio, la
 * fachada y el dominio siguen hablando de {@link LocalDateTime}, minutos
 * y {@link SalaVirtual}.</p>
 *
 * <p>Se usa <b>object adapter</b> (composicion) y no class adapter
 * (herencia) por dos razones: el SDK es una clase de terceros que no
 * conviene extender, y la composicion permite sustituirlo por un doble
 * en las pruebas.</p>
 */
public class AdaptadorZoom implements ProveedorVideoconferencia {

    /** Zona horaria institucional; el SDK trabaja en UTC. */
    private static final ZoneId ZONA_INSTITUCIONAL = ZoneId.of("America/Guayaquil");

    private static final int SEGUNDOS_POR_MINUTO = 60;

    private final ZoomMeetingApi api;
    private final ZoneId zona;

    public AdaptadorZoom() {
        this(new ZoomMeetingApi(), ZONA_INSTITUCIONAL);
    }

    public AdaptadorZoom(ZoomMeetingApi api) {
        this(api, ZONA_INSTITUCIONAL);
    }

    public AdaptadorZoom(ZoomMeetingApi api, ZoneId zona) {
        this.api = Objects.requireNonNull(api, "El adaptador necesita el SDK de Zoom.");
        this.zona = Objects.requireNonNull(zona, "El adaptador necesita una zona horaria.");
    }

    @Override
    public SalaVirtual crearSala(String titulo, LocalDateTime inicio, int duracionMinutos) {
        Objects.requireNonNull(titulo, "La sala necesita un titulo.");
        Objects.requireNonNull(inicio, "La sala necesita un instante de inicio.");

        // Traduccion de ida: del vocabulario del sistema al del SDK.
        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("topic", titulo);
        parametros.put("start_time", inicio.atZone(zona).toInstant().toEpochMilli());
        parametros.put("duration", duracionMinutos * SEGUNDOS_POR_MINUTO);

        String respuesta;
        try {
            respuesta = api.scheduleMeeting(parametros);
        } catch (RuntimeException fallo) {
            throw new IllegalStateException(
                    "Zoom rechazo la reserva de la sala: " + fallo.getMessage(), fallo);
        }

        // Traduccion de vuelta: de la cadena propia de Zoom a SalaVirtual.
        Map<String, String> campos = descomponer(respuesta);
        return new SalaVirtual(
                campos.getOrDefault("join_url", ""),
                campos.getOrDefault("passcode", ""),
                nombre());
    }

    @Override
    public String nombre() {
        return "Zoom";
    }

    /** Convierte {@code clave=valor;clave=valor} en un mapa. */
    private Map<String, String> descomponer(String respuesta) {
        Map<String, String> campos = new LinkedHashMap<>();
        for (String par : respuesta.split(";")) {
            int separador = par.indexOf('=');
            if (separador > 0) {
                campos.put(par.substring(0, separador), par.substring(separador + 1));
            }
        }
        return campos;
    }
}

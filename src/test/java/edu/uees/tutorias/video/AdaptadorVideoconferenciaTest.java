package edu.uees.tutorias.video;

import edu.uees.tutorias.video.external.MeetInstitucionalService;
import edu.uees.tutorias.video.external.ZoomMeetingApi;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas del Adapter: verifican la <b>traduccion</b>, que es lo unico
 * que el patron aporta, y la sustituibilidad de un proveedor por otro.
 */
class AdaptadorVideoconferenciaTest {

    private static final ZoneId ZONA = ZoneId.of("America/Guayaquil");
    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 10, 1, 10, 0);

    /**
     * El sistema pide 60 minutos y una fecha local; el SDK debe recibir
     * 3600 segundos y epoch millis UTC. La traduccion es la razon de ser
     * del adaptador, asi que se verifica sobre los parametros reales que
     * llegan al SDK.
     */
    @Test
    void elAdaptadorTraduceMinutosASegundosYFechaLocalAEpochUtc() {
        ZoomApiEspia espia = new ZoomApiEspia();
        ProveedorVideoconferencia proveedor = new AdaptadorZoom(espia, ZONA);

        proveedor.crearSala("Tutoria de bases de datos", INICIO, 60);

        assertEquals("Tutoria de bases de datos", espia.recibidos.get("topic"));
        assertEquals(3600L, ((Number) espia.recibidos.get("duration")).longValue());
        assertEquals(INICIO.atZone(ZONA).toInstant().toEpochMilli(),
                ((Number) espia.recibidos.get("start_time")).longValue());
    }

    @Test
    void elAdaptadorConvierteLaRespuestaDeZoomEnUnaSalaVirtual() {
        ProveedorVideoconferencia proveedor = new AdaptadorZoom(new ZoomMeetingApi(), ZONA);

        SalaVirtual sala = proveedor.crearSala("Tutoria de bases de datos", INICIO, 60);

        assertTrue(sala.url().startsWith("https://uees.zoom.us/j/"));
        assertTrue(sala.exigeCodigo());
        assertEquals("Zoom", sala.proveedor());
    }

    @Test
    void elProveedorInstitucionalSatisfaceElMismoContratoSinCodigoDeAcceso() {
        ProveedorVideoconferencia proveedor =
                new AdaptadorMeetInstitucional(new MeetInstitucionalService());

        SalaVirtual sala = proveedor.crearSala("Tutoria de bases de datos", INICIO, 60);

        assertTrue(sala.url().startsWith("https://meet.uees.edu.ec/"));
        assertFalse(sala.exigeCodigo());
        assertEquals("Meet institucional", sala.proveedor());
    }

    /**
     * Beneficio esperado del patron: dos proveedores con APIs
     * incompatibles se consumen con el mismo codigo cliente. El bucle no
     * menciona ninguna clase concreta.
     */
    @Test
    void dosProveedoresDistintosSeConsumenPorLaMismaInterfaz() {
        List<ProveedorVideoconferencia> proveedores =
                List.of(new AdaptadorZoom(), new AdaptadorMeetInstitucional());

        for (ProveedorVideoconferencia proveedor : proveedores) {
            SalaVirtual sala = proveedor.crearSala("Tutoria", INICIO, 45);

            assertFalse(sala.url().isBlank());
            assertEquals(proveedor.nombre(), sala.proveedor());
        }
    }

    /** El fallo propio del SDK se traduce a una excepcion del sistema. */
    @Test
    void unRechazoDelSdkSeTraduceAUnFalloDelSistema() {
        ProveedorVideoconferencia proveedor = new AdaptadorZoom(new ZoomMeetingApi(), ZONA);

        IllegalStateException fallo = assertThrows(IllegalStateException.class,
                () -> proveedor.crearSala("Tutoria", INICIO, 0));

        assertTrue(fallo.getMessage().contains("Zoom rechazo la reserva"));
    }

    /** Doble del SDK que captura los parametros que recibe. */
    private static class ZoomApiEspia extends ZoomMeetingApi {
        private final Map<String, Object> recibidos = new LinkedHashMap<>();

        @Override
        public String scheduleMeeting(Map<String, Object> parametros) {
            recibidos.putAll(parametros);
            return "meeting_id=1;join_url=https://uees.zoom.us/j/1;passcode=ABC";
        }
    }
}

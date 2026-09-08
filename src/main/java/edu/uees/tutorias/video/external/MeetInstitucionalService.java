package edu.uees.tutorias.video.external;

/**
 * Adaptee alternativo: simulacion del servicio de videoconferencia
 * propio de la universidad.
 *
 * <p>Existe para demostrar el beneficio del patron: es <b>otro</b>
 * dialecto —recibe la fecha como texto ISO-8601, la duracion en minutos,
 * devuelve un objeto propio y no usa codigo de acceso— y aun asi el
 * sistema lo consume por la misma interfaz. Sin Adapter, soportar los
 * dos proveedores significaria condicionales repartidos por el
 * servicio.</p>
 */
public class MeetInstitucionalService {

    /** Respuesta propia del servicio institucional. */
    public static final class SalaMeet {

        private final String enlace;
        private final String identificador;

        SalaMeet(String enlace, String identificador) {
            this.enlace = enlace;
            this.identificador = identificador;
        }

        public String getEnlace() {
            return enlace;
        }

        public String getIdentificador() {
            return identificador;
        }
    }

    /**
     * Reserva una sala institucional.
     *
     * @param asunto        titulo de la sesion
     * @param fechaHoraIso  inicio en formato ISO-8601 local, por ejemplo
     *                      {@code 2026-10-01T10:00}
     * @param minutos       duracion del bloque, en minutos
     */
    public SalaMeet reservar(String asunto, String fechaHoraIso, int minutos) {
        if (asunto == null || asunto.isBlank()) {
            throw new IllegalArgumentException("meet: el asunto es obligatorio");
        }
        if (fechaHoraIso == null || !fechaHoraIso.contains("T")) {
            throw new IllegalArgumentException("meet: se esperaba una fecha ISO-8601 local");
        }
        if (minutos <= 0) {
            throw new IllegalArgumentException("meet: la duracion debe ser positiva");
        }

        String identificador = "tutoria-"
                + Integer.toHexString(Math.abs((asunto + fechaHoraIso).hashCode()));
        return new SalaMeet("https://meet.uees.edu.ec/" + identificador, identificador);
    }
}

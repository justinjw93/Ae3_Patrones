package edu.uees.tutorias.video;

import edu.uees.tutorias.video.external.MeetInstitucionalService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * Segundo Adapter sobre el mismo Target, esta vez para el servicio
 * institucional {@link MeetInstitucionalService}.
 *
 * <p>Su valor esta en la comparacion: el Adaptee habla otro dialecto
 * —fecha como texto ISO-8601, duracion en minutos, respuesta en un
 * objeto propio y sin codigo de acceso— y aun asi el sistema lo consume
 * exactamente igual que a Zoom. Cambiar de proveedor es cambiar la
 * instancia que se inyecta en el composition root; ni el servicio, ni la
 * fachada, ni el dominio se enteran.</p>
 */
public class AdaptadorMeetInstitucional implements ProveedorVideoconferencia {

    private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final MeetInstitucionalService servicio;

    public AdaptadorMeetInstitucional() {
        this(new MeetInstitucionalService());
    }

    public AdaptadorMeetInstitucional(MeetInstitucionalService servicio) {
        this.servicio = Objects.requireNonNull(servicio,
                "El adaptador necesita el servicio institucional.");
    }

    @Override
    public SalaVirtual crearSala(String titulo, LocalDateTime inicio, int duracionMinutos) {
        Objects.requireNonNull(titulo, "La sala necesita un titulo.");
        Objects.requireNonNull(inicio, "La sala necesita un instante de inicio.");

        MeetInstitucionalService.SalaMeet sala;
        try {
            sala = servicio.reservar(titulo, inicio.format(ISO_LOCAL), duracionMinutos);
        } catch (RuntimeException fallo) {
            throw new IllegalStateException(
                    "El Meet institucional rechazo la reserva: " + fallo.getMessage(), fallo);
        }

        // Este proveedor no emite codigo de acceso: el contrato lo admite.
        return new SalaVirtual(sala.getEnlace(), "", nombre());
    }

    @Override
    public String nombre() {
        return "Meet institucional";
    }
}

package edu.uees.tutorias.builder;

import edu.uees.tutorias.domain.Estudiante;
import edu.uees.tutorias.domain.HorarioTutoria;
import edu.uees.tutorias.domain.ModalidadTutoria;
import edu.uees.tutorias.domain.Reserva;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Builder de {@link Reserva}: separa la construccion paso a paso de una
 * reserva de su representacion final.
 *
 * <p><b>Problema que resuelve.</b> Reserva llego a tener nueve datos
 * entre obligatorios y opcionales. Resolverlo con constructores
 * telescopicos producia llamadas ilegibles, con String consecutivos
 * intercambiables sin error de compilacion, {@code null} explicitos para
 * los campos que no aplican y ningun lugar donde validar la regla que
 * une modalidad y enlace. Este builder nombra cada dato en el punto de
 * uso, aplica valores por defecto y valida antes de construir.</p>
 *
 * <p><b>Obligatorios:</b> estudiante y horario.<br>
 * <b>Opcionales con valor por defecto:</b> id (UUID aleatorio),
 * fecha de creacion ({@code now()}), modalidad (PRESENCIAL), motivo
 * ("Tutoria general") y recordatorio (60 minutos).<br>
 * <b>Opcionales sin valor por defecto:</b> enlace virtual y
 * observaciones.</p>
 *
 * <p><b>Ejemplo de uso.</b></p>
 * <pre>{@code
 * Reserva reserva = new ReservaBuilder()
 *         .conEstudiante(estudiante)
 *         .conHorario(horario)
 *         .virtualCon("https://meet.uees.edu.ec/tutoria-h002")
 *         .conMotivo("Consulta sobre normalizacion")
 *         .conRecordatorioDe(30)
 *         .construir();
 * }</pre>
 */
public class ReservaBuilder {

    /** Tope razonable del recordatorio: 24 horas antes de la tutoria. */
    public static final int RECORDATORIO_MAXIMO_MINUTOS = 24 * 60;

    private String id = UUID.randomUUID().toString();
    private Estudiante estudiante;
    private HorarioTutoria horario;
    private LocalDateTime fechaCreacion = LocalDateTime.now();
    private ModalidadTutoria modalidad = ModalidadTutoria.PRESENCIAL;
    private String motivo = Reserva.MOTIVO_POR_DEFECTO;
    private String enlaceVirtual;
    private String observaciones;
    private int recordatorioMinutosAntes = Reserva.RECORDATORIO_POR_DEFECTO_MINUTOS;

    /** Dato obligatorio: estudiante que solicita la tutoria. */
    public ReservaBuilder conEstudiante(Estudiante estudiante) {
        this.estudiante = estudiante;
        return this;
    }

    /** Dato obligatorio: bloque de tutoria que ocupara la reserva. */
    public ReservaBuilder conHorario(HorarioTutoria horario) {
        this.horario = horario;
        return this;
    }

    /** Opcional: identificador propio. Por defecto se genera un UUID. */
    public ReservaBuilder conId(String id) {
        this.id = id;
        return this;
    }

    /** Opcional: fecha de creacion. Por defecto, el instante actual. */
    public ReservaBuilder creadaEn(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
        return this;
    }

    /** Opcional: modalidad presencial (valor por defecto), sin enlace. */
    public ReservaBuilder presencial() {
        this.modalidad = ModalidadTutoria.PRESENCIAL;
        this.enlaceVirtual = null;
        return this;
    }

    /**
     * Opcional: modalidad virtual. Exige el enlace en la misma llamada,
     * de modo que resulte imposible declarar una tutoria virtual y
     * olvidar por donde se conecta el estudiante.
     */
    public ReservaBuilder virtualCon(String enlaceVirtual) {
        this.modalidad = ModalidadTutoria.VIRTUAL;
        this.enlaceVirtual = enlaceVirtual;
        return this;
    }

    /** Opcional: motivo de la tutoria. Por defecto, "Tutoria general". */
    public ReservaBuilder conMotivo(String motivo) {
        this.motivo = motivo;
        return this;
    }

    /** Opcional: observaciones libres para el docente. */
    public ReservaBuilder conObservaciones(String observaciones) {
        this.observaciones = observaciones;
        return this;
    }

    /** Opcional: anticipacion del recordatorio. Por defecto, 60 minutos. */
    public ReservaBuilder conRecordatorioDe(int minutosAntes) {
        this.recordatorioMinutosAntes = minutosAntes;
        return this;
    }

    /**
     * Valida y construye la reserva. Es el unico camino para obtener una
     * {@link Reserva}: su constructor solo acepta un builder ya validado.
     *
     * @throws IllegalStateException si falta un dato obligatorio o si la
     *                               combinacion de datos es incoherente
     */
    public Reserva construir() {
        return new Reserva(this);
    }

    /**
     * Comprueba los campos obligatorios y las reglas que relacionan
     * varios campos entre si. Se invoca tambien desde el constructor de
     * {@link Reserva}, de manera que ninguna reserva pueda existir sin
     * haber pasado por esta validacion.
     */
    public void validar() {
        if (estudiante == null) {
            throw new IllegalStateException("La reserva requiere un estudiante.");
        }
        if (horario == null) {
            throw new IllegalStateException("La reserva requiere un horario de tutoria.");
        }
        if (id == null || id.isBlank()) {
            throw new IllegalStateException("La reserva requiere un identificador.");
        }
        if (fechaCreacion == null) {
            throw new IllegalStateException("La reserva requiere una fecha de creacion.");
        }
        if (modalidad == null) {
            throw new IllegalStateException("La reserva requiere una modalidad.");
        }
        if (modalidad == ModalidadTutoria.VIRTUAL && (enlaceVirtual == null || enlaceVirtual.isBlank())) {
            throw new IllegalStateException("Una tutoria virtual requiere un enlace de conexion.");
        }
        if (modalidad == ModalidadTutoria.PRESENCIAL && enlaceVirtual != null) {
            throw new IllegalStateException("Una tutoria presencial no debe tener enlace de conexion.");
        }
        if (recordatorioMinutosAntes < 0 || recordatorioMinutosAntes > RECORDATORIO_MAXIMO_MINUTOS) {
            throw new IllegalStateException(
                    "El recordatorio debe estar entre 0 y " + RECORDATORIO_MAXIMO_MINUTOS + " minutos.");
        }
    }

    public String getId() {
        return id;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public HorarioTutoria getHorario() {
        return horario;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public ModalidadTutoria getModalidad() {
        return modalidad;
    }

    public String getMotivo() {
        return motivo;
    }

    public String getEnlaceVirtual() {
        return enlaceVirtual;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public int getRecordatorioMinutosAntes() {
        return recordatorioMinutosAntes;
    }
}

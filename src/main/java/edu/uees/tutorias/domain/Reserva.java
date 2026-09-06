package edu.uees.tutorias.domain;

import edu.uees.tutorias.builder.ReservaBuilder;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Gestiona el ciclo de vida de una solicitud de tutoria entre un
 * {@link Estudiante} y un {@link HorarioTutoria}.
 *
 * <p>Ninguna clase externa cambia el estado directamente (no existe un
 * {@code setEstado(...)} publico): cada transicion pasa por un metodo
 * que valida si es valida desde el estado actual y que mantiene
 * consistente al {@link HorarioTutoria} asociado. Esto responde a la
 * pregunta de analisis "¿quien deberia cambiar el estado de una
 * reserva?": la propia Reserva, nunca un componente externo.</p>
 *
 * <p><b>Construccion (Ae2).</b> Los nueve datos entre obligatorios y
 * opcionales que hoy describen una reserva se armaban con constructores
 * telescopicos, lo que producia llamadas ilegibles, {@code null}
 * explicitos y ningun punto unico de validacion. Esa responsabilidad
 * paso al patron Builder: el unico constructor disponible recibe un
 * {@link ReservaBuilder} ya validado, de modo que no existe forma de
 * crear una reserva incompleta o incoherente.</p>
 */
public class Reserva {

    /** Minutos de anticipacion del recordatorio cuando no se indica otro valor. */
    public static final int RECORDATORIO_POR_DEFECTO_MINUTOS = 60;

    /** Motivo registrado cuando el estudiante no especifica uno. */
    public static final String MOTIVO_POR_DEFECTO = "Tutoria general";

    private final String id;
    private final Estudiante estudiante;
    private HorarioTutoria horario;
    private final LocalDateTime fechaCreacion;
    private EstadoReserva estado;

    private final ModalidadTutoria modalidad;
    private final String motivo;
    private final String enlaceVirtual;
    private final String observaciones;
    private final int recordatorioMinutosAntes;

    /**
     * Unico constructor de Reserva: toma los datos ya reunidos por un
     * {@link ReservaBuilder}.
     *
     * <p>Vuelve a invocar {@link ReservaBuilder#validar()} antes de
     * copiar nada, para que ninguna reserva pueda construirse saltandose
     * las reglas aunque se instancie el builder por fuera de
     * {@link ReservaBuilder#construir()}. Solo despues de validar ocupa
     * el horario, de modo que una construccion rechazada no deja el
     * horario bloqueado.</p>
     */
    public Reserva(ReservaBuilder builder) {
        Objects.requireNonNull(builder, "La reserva se construye a partir de un ReservaBuilder.");
        builder.validar();

        this.id = builder.getId();
        this.estudiante = builder.getEstudiante();
        this.horario = builder.getHorario();
        this.fechaCreacion = builder.getFechaCreacion();
        this.modalidad = builder.getModalidad();
        this.motivo = builder.getMotivo();
        this.enlaceVirtual = builder.getEnlaceVirtual();
        this.observaciones = builder.getObservaciones();
        this.recordatorioMinutosAntes = builder.getRecordatorioMinutosAntes();

        this.horario.marcarReservado();
        this.estado = EstadoReserva.PENDIENTE;
    }

    public void confirmar() {
        exigirEstado(EstadoReserva.PENDIENTE, "confirmar");
        this.estado = EstadoReserva.CONFIRMADA;
    }

    public void cancelar() {
        if (estado != EstadoReserva.PENDIENTE && estado != EstadoReserva.CONFIRMADA) {
            throw new IllegalStateException(
                    "La reserva " + id + " no puede cancelarse desde el estado " + estado);
        }
        this.estado = EstadoReserva.CANCELADA;
        this.horario.liberar();
    }

    public void marcarRealizada() {
        exigirEstado(EstadoReserva.CONFIRMADA, "marcar como realizada");
        this.estado = EstadoReserva.REALIZADA;
    }

    /**
     * Reprograma la reserva a un nuevo horario: libera el horario
     * actual, ocupa el nuevo y regresa la reserva a PENDIENTE (requiere
     * una nueva confirmacion).
     */
    public void reprogramar(HorarioTutoria nuevoHorario) {
        if (estado != EstadoReserva.PENDIENTE && estado != EstadoReserva.CONFIRMADA) {
            throw new IllegalStateException(
                    "La reserva " + id + " no puede reprogramarse desde el estado " + estado);
        }
        Objects.requireNonNull(nuevoHorario);
        nuevoHorario.marcarReservado();
        this.horario.liberar();
        this.horario = nuevoHorario;
        this.estado = EstadoReserva.PENDIENTE;
    }

    private void exigirEstado(EstadoReserva esperado, String accion) {
        if (estado != esperado) {
            throw new IllegalStateException(
                    "No se puede " + accion + " la reserva " + id + " desde el estado " + estado);
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

    public EstadoReserva getEstado() {
        return estado;
    }

    public ModalidadTutoria getModalidad() {
        return modalidad;
    }

    public String getMotivo() {
        return motivo;
    }

    /** Enlace de conexion; vacio cuando la tutoria es presencial. */
    public Optional<String> getEnlaceVirtual() {
        return Optional.ofNullable(enlaceVirtual);
    }

    public Optional<String> getObservaciones() {
        return Optional.ofNullable(observaciones);
    }

    public int getRecordatorioMinutosAntes() {
        return recordatorioMinutosAntes;
    }
}

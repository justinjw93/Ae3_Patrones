package edu.uees.tutorias.domain;

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
 * <p><b>Problema abierto en Ae2.</b> Al incorporar los datos que la
 * coordinacion academica pidio registrar (modalidad, motivo, enlace de
 * conexion, observaciones y recordatorio), la construccion de una
 * Reserva paso de cuatro a nueve parametros y aparecieron
 * constructores telescopicos: varias sobrecargas encadenadas que
 * rellenan valores por defecto. El resultado es una llamada ilegible
 * ({@code new Reserva(id, est, hor, fecha, VIRTUAL, "Consulta", null,
 * null, 60)}), con tres String consecutivos que el compilador no puede
 * distinguir si se intercambian, y sin un lugar unico donde validar la
 * regla "toda tutoria virtual necesita enlace". Este es el problema que
 * resuelve el patron Builder en el siguiente paso.</p>
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

    /** Constructor telescopico 1: solo los datos obligatorios. */
    public Reserva(String id, Estudiante estudiante, HorarioTutoria horario,
                    LocalDateTime fechaCreacion) {
        this(id, estudiante, horario, fechaCreacion,
                ModalidadTutoria.PRESENCIAL, MOTIVO_POR_DEFECTO, null, null,
                RECORDATORIO_POR_DEFECTO_MINUTOS);
    }

    /** Constructor telescopico 2: agrega modalidad y motivo. */
    public Reserva(String id, Estudiante estudiante, HorarioTutoria horario,
                    LocalDateTime fechaCreacion, ModalidadTutoria modalidad, String motivo) {
        this(id, estudiante, horario, fechaCreacion, modalidad, motivo, null, null,
                RECORDATORIO_POR_DEFECTO_MINUTOS);
    }

    /**
     * Constructor telescopico 3: todos los campos.
     *
     * <p>Nueve parametros, tres String consecutivos intercambiables sin
     * error de compilacion y ninguna validacion de la regla que une
     * modalidad y enlace. Es exactamente el punto de partida que
     * justifica introducir un Builder.</p>
     */
    public Reserva(String id, Estudiante estudiante, HorarioTutoria horario,
                    LocalDateTime fechaCreacion, ModalidadTutoria modalidad, String motivo,
                    String enlaceVirtual, String observaciones, int recordatorioMinutosAntes) {
        this.id = Objects.requireNonNull(id);
        this.estudiante = Objects.requireNonNull(estudiante);
        this.horario = Objects.requireNonNull(horario);
        this.fechaCreacion = Objects.requireNonNull(fechaCreacion);
        this.modalidad = Objects.requireNonNull(modalidad);
        this.motivo = motivo;
        this.enlaceVirtual = enlaceVirtual;
        this.observaciones = observaciones;
        this.recordatorioMinutosAntes = recordatorioMinutosAntes;
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

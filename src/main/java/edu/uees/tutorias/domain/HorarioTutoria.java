package edu.uees.tutorias.domain;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Objects;

/**
 * Bloque de tutoria publicado por un docente para una asignatura, con su
 * disponibilidad.
 *
 * <p>Esta clase es responsable de conocer y proteger su propio estado de
 * disponibilidad: es el objeto que responde "¿estoy disponible?" y el
 * unico que puede marcarse como reservado o liberarse. Esta decision
 * evita que {@link edu.uees.tutorias.service.ServicioReservas} (u otra
 * clase externa) manipule directamente el estado sin pasar por las
 * reglas que aqui se protegen, y evita que un mismo horario quede
 * reservado por mas de un estudiante.</p>
 */
public class HorarioTutoria {

    private final String id;
    private final Docente docente;
    private final Asignatura asignatura;
    private final LocalDate fecha;
    private final LocalTime horaInicio;
    private final LocalTime horaFin;
    private EstadoHorario estado;

    public HorarioTutoria(String id, Docente docente, Asignatura asignatura,
                           LocalDate fecha, LocalTime horaInicio, LocalTime horaFin) {
        this.id = Objects.requireNonNull(id);
        this.docente = Objects.requireNonNull(docente);
        this.asignatura = Objects.requireNonNull(asignatura);
        this.fecha = Objects.requireNonNull(fecha);
        this.horaInicio = Objects.requireNonNull(horaInicio);
        this.horaFin = Objects.requireNonNull(horaFin);
        this.estado = EstadoHorario.DISPONIBLE;
    }

    public boolean estaDisponible() {
        return estado == EstadoHorario.DISPONIBLE;
    }

    /**
     * Instante en que arranca el bloque, combinando fecha y hora de
     * inicio.
     *
     * <p>Lo expone el propio horario, y no quien lo consulta, porque es
     * el objeto que conoce como se componen sus dos campos. Las
     * politicas de cancelacion y el proveedor de videoconferencia lo
     * necesitan para razonar sobre anticipacion y agenda.</p>
     */
    public LocalDateTime inicio() {
        return LocalDateTime.of(fecha, horaInicio);
    }

    /** Duracion del bloque en minutos. */
    public int duracionEnMinutos() {
        return (int) Duration.between(horaInicio, horaFin).toMinutes();
    }

    /**
     * Reserva el horario. Es la unica via para pasar a RESERVADO; si el
     * horario no esta disponible, se rechaza la operacion en lugar de
     * dejar que quien llama decida el estado directamente.
     */
    public void marcarReservado() {
        if (!estaDisponible()) {
            throw new IllegalStateException(
                    "El horario " + id + " no esta disponible para reservarse.");
        }
        this.estado = EstadoHorario.RESERVADO;
    }

    /** Libera el horario, por ejemplo tras una cancelacion. */
    public void liberar() {
        if (estado == EstadoHorario.INACTIVO) {
            throw new IllegalStateException("Un horario inactivo no puede liberarse.");
        }
        this.estado = EstadoHorario.DISPONIBLE;
    }

    public void marcarInactivo() {
        this.estado = EstadoHorario.INACTIVO;
    }

    public String getId() {
        return id;
    }

    public Docente getDocente() {
        return docente;
    }

    public Asignatura getAsignatura() {
        return asignatura;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getHoraInicio() {
        return horaInicio;
    }

    public LocalTime getHoraFin() {
        return horaFin;
    }

    public EstadoHorario getEstado() {
        return estado;
    }
}

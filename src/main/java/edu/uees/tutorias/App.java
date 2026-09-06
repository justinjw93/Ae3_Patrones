package edu.uees.tutorias;

import edu.uees.tutorias.builder.ReservaBuilder;
import edu.uees.tutorias.domain.Asignatura;
import edu.uees.tutorias.domain.Docente;
import edu.uees.tutorias.domain.Estudiante;
import edu.uees.tutorias.domain.HorarioTutoria;
import edu.uees.tutorias.domain.Reserva;
import edu.uees.tutorias.factory.CreadorNotificador;
import edu.uees.tutorias.factory.CreadorNotificadorConsola;
import edu.uees.tutorias.factory.CreadorNotificadorCorreo;
import edu.uees.tutorias.factory.CreadorNotificadorLog;
import edu.uees.tutorias.notification.Notificador;
import edu.uees.tutorias.persistence.RepositorioHorarios;
import edu.uees.tutorias.persistence.RepositorioHorariosEnMemoria;
import edu.uees.tutorias.persistence.RepositorioReservas;
import edu.uees.tutorias.persistence.RepositorioReservasEnMemoria;
import edu.uees.tutorias.service.ServicioReservas;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Punto de entrada de demostracion: arma las dependencias del sistema
 * (composition root) y ejecuta un flujo tipico de reserva, confirmacion
 * y cancelacion de una tutoria.
 *
 * <p>Desde Ae2 el canal de notificacion ya no se instancia con un
 * {@code new} de clase concreta: se obtiene de un
 * {@link CreadorNotificador}, de modo que cambiar de canal es cambiar de
 * ConcreteCreator y nada mas.</p>
 */
public final class App {

    public static void main(String[] args) {
        RepositorioReservas repositorioReservas = new RepositorioReservasEnMemoria();
        RepositorioHorarios repositorioHorarios = new RepositorioHorariosEnMemoria();
        CreadorNotificador creadorNotificador = new CreadorNotificadorCorreo();
        Notificador notificador = creadorNotificador.crearNotificador();
        ServicioReservas servicioReservas =
                new ServicioReservas(repositorioReservas, repositorioHorarios, notificador);

        Docente docente = new Docente("D001", "Ana", "Perez", "ana.perez@uees.edu.ec", "Bases de datos");
        Asignatura asignatura = new Asignatura("SIS201", "Bases de datos");
        docente.agregarAsignatura(asignatura);

        HorarioTutoria horario = new HorarioTutoria(
                "H001", docente, asignatura,
                LocalDate.of(2026, 9, 1), LocalTime.of(10, 0), LocalTime.of(11, 0));
        repositorioHorarios.guardar(horario);

        Estudiante estudiante = new Estudiante(
                "E001", "Justin", "Arreaga", "justin.arreaga@uees.edu.ec", "Computacion", 5);

        Reserva reserva = servicioReservas.crearReserva(estudiante, horario.getId());

        servicioReservas.confirmarReserva(reserva.getId());
        servicioReservas.cancelarReserva(reserva.getId());

        System.out.println("Estado final de la reserva: " + reserva.getEstado());
        System.out.println("Estado final del horario: " + horario.getEstado());

        demostrarCanalesDeNotificacion(estudiante.getCorreo());
        demostrarBuilderDeReservas(servicioReservas, repositorioHorarios,
                docente, asignatura, estudiante);
    }

    /**
     * Recorre los ConcreteCreators disponibles enviando el mismo aviso
     * por cada canal. El bucle trabaja unicamente contra el tipo
     * {@link CreadorNotificador}: no menciona ninguna clase concreta de
     * notificador, que es justamente lo que aporta el Factory Method.
     */
    private static void demostrarCanalesDeNotificacion(String destinatario) {
        List<CreadorNotificador> creadores = List.of(
                new CreadorNotificadorConsola(),
                new CreadorNotificadorLog(),
                new CreadorNotificadorCorreo());

        System.out.println();
        System.out.println("### Factory Method: mismo aviso por cada canal disponible ###");
        for (CreadorNotificador creador : creadores) {
            creador.enviarNotificacion(destinatario, "Recordatorio de tutoria",
                    "Recuerda tu tutoria programada.");
        }
    }

    /**
     * Construye dos reservas con configuraciones distintas usando el
     * mismo {@link ReservaBuilder}.
     *
     * <p>La primera se queda con todos los valores por defecto
     * (presencial, motivo generico, recordatorio de 60 minutos); la
     * segunda es una tutoria virtual con enlace, motivo, observaciones y
     * recordatorio propios. Ninguna de las dos llamadas necesita pasar
     * {@code null} ni recordar el orden de nueve argumentos.</p>
     */
    private static void demostrarBuilderDeReservas(ServicioReservas servicioReservas,
                                                   RepositorioHorarios repositorioHorarios,
                                                   Docente docente, Asignatura asignatura,
                                                   Estudiante estudiante) {
        HorarioTutoria horarioPresencial = new HorarioTutoria(
                "H002", docente, asignatura,
                LocalDate.of(2026, 9, 8), LocalTime.of(15, 0), LocalTime.of(16, 0));
        HorarioTutoria horarioVirtual = new HorarioTutoria(
                "H003", docente, asignatura,
                LocalDate.of(2026, 9, 9), LocalTime.of(9, 0), LocalTime.of(10, 0));
        repositorioHorarios.guardar(horarioPresencial);
        repositorioHorarios.guardar(horarioVirtual);

        System.out.println();
        System.out.println("### Builder: dos configuraciones de reserva ###");

        Reserva reservaMinima = servicioReservas.crearReserva(estudiante, horarioPresencial.getId());
        imprimirReserva("Configuracion minima (solo obligatorios)", reservaMinima);

        Reserva reservaCompleta = servicioReservas.crearReserva(
                new ReservaBuilder()
                        .conEstudiante(estudiante)
                        .virtualCon("https://meet.uees.edu.ec/tutoria-h003")
                        .conMotivo("Consulta sobre normalizacion")
                        .conObservaciones("El estudiante enviara su avance antes de la sesion")
                        .conRecordatorioDe(30),
                horarioVirtual.getId());
        imprimirReserva("Configuracion completa (tutoria virtual)", reservaCompleta);
    }

    private static void imprimirReserva(String titulo, Reserva reserva) {
        System.out.println("- " + titulo);
        System.out.println("  Modalidad: " + reserva.getModalidad());
        System.out.println("  Motivo: " + reserva.getMotivo());
        System.out.println("  Enlace: " + reserva.getEnlaceVirtual().orElse("no aplica"));
        System.out.println("  Observaciones: " + reserva.getObservaciones().orElse("sin observaciones"));
        System.out.println("  Recordatorio: " + reserva.getRecordatorioMinutosAntes() + " min antes");
    }

    private App() {
    }
}

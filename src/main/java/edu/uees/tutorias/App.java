package edu.uees.tutorias;

import edu.uees.tutorias.domain.Asignatura;
import edu.uees.tutorias.domain.Docente;
import edu.uees.tutorias.domain.Estudiante;
import edu.uees.tutorias.domain.HorarioTutoria;
import edu.uees.tutorias.domain.Reserva;
import edu.uees.tutorias.event.ObservadorAgendaDocente;
import edu.uees.tutorias.event.ObservadorBitacora;
import edu.uees.tutorias.event.ObservadorNotificacion;
import edu.uees.tutorias.event.PublicadorReservas;
import edu.uees.tutorias.facade.GestionTutorias;
import edu.uees.tutorias.factory.CreadorNotificador;
import edu.uees.tutorias.factory.CreadorNotificadorConsola;
import edu.uees.tutorias.factory.CreadorNotificadorCorreo;
import edu.uees.tutorias.factory.CreadorNotificadorLog;
import edu.uees.tutorias.persistence.RepositorioHorarios;
import edu.uees.tutorias.persistence.RepositorioHorariosEnMemoria;
import edu.uees.tutorias.persistence.RepositorioReservas;
import edu.uees.tutorias.persistence.RepositorioReservasEnMemoria;
import edu.uees.tutorias.policy.PoliticaCancelacionAnticipada;
import edu.uees.tutorias.policy.PoliticaCancelacionConPenalidad;
import edu.uees.tutorias.policy.PoliticaCancelacionDocente;
import edu.uees.tutorias.policy.ResultadoCancelacion;
import edu.uees.tutorias.service.ServicioReservas;
import edu.uees.tutorias.video.AdaptadorMeetInstitucional;
import edu.uees.tutorias.video.AdaptadorZoom;
import edu.uees.tutorias.video.ProveedorVideoconferencia;
import edu.uees.tutorias.video.SalaVirtual;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Composition root del sistema: arma las dependencias y demuestra el
 * incremento 1 sobre un flujo real de tutorias.
 *
 * <p>Es el unico lugar del proyecto donde se eligen implementaciones
 * concretas. Cada patron aparece aqui como una decision explicita:</p>
 *
 * <ul>
 *   <li><b>Factory Method (Ae2):</b> el canal de notificacion se obtiene
 *       de un {@link CreadorNotificador}, no de un {@code new}.</li>
 *   <li><b>Builder (Ae2):</b> ninguna {@code Reserva} se construye por
 *       constructor telescopico; la fachada usa el builder.</li>
 *   <li><b>Strategy (Ae3):</b> se decide la politica de cancelacion
 *       vigente del sistema.</li>
 *   <li><b>Observer (Ae3):</b> se registran los interesados en los
 *       cambios de reserva.</li>
 *   <li><b>Adapter (Ae3):</b> se elige el proveedor de
 *       videoconferencia.</li>
 *   <li><b>Facade (Ae3):</b> a partir de aqui, el flujo se pide por una
 *       sola operacion de alto nivel.</li>
 * </ul>
 */
public final class App {

    public static void main(String[] args) {
        // --- Composition root: aqui se eligen las implementaciones ---
        RepositorioReservas repositorioReservas = new RepositorioReservasEnMemoria();
        RepositorioHorarios repositorioHorarios = new RepositorioHorariosEnMemoria();

        // Factory Method (Ae2): el canal se decide una sola vez.
        CreadorNotificador creadorNotificador = new CreadorNotificadorCorreo();

        // Observer (Ae3): quien reacciona a un cambio de reserva.
        PublicadorReservas publicador = new PublicadorReservas();
        ObservadorBitacora bitacora = new ObservadorBitacora();
        ObservadorAgendaDocente agenda = new ObservadorAgendaDocente();
        publicador.registrar(new ObservadorNotificacion(creadorNotificador));
        publicador.registrar(bitacora);
        publicador.registrar(agenda);

        // Strategy (Ae3): el reglamento de cancelacion vigente.
        ServicioReservas servicioReservas = new ServicioReservas(
                repositorioReservas, repositorioHorarios, publicador,
                new PoliticaCancelacionConPenalidad());

        // Adapter (Ae3): el proveedor de videoconferencia.
        ProveedorVideoconferencia proveedor = new AdaptadorZoom();

        // Facade (Ae3): la unica puerta que usa el resto del programa.
        GestionTutorias gestionTutorias =
                new GestionTutorias(servicioReservas, repositorioHorarios, proveedor);

        // --- Datos de demostracion ---
        Docente docente = new Docente("D001", "Ana", "Perez", "ana.perez@uees.edu.ec", "Bases de datos");
        Asignatura asignatura = new Asignatura("SIS201", "Bases de datos");
        docente.agregarAsignatura(asignatura);

        Estudiante estudiante = new Estudiante(
                "E001", "Justin", "Arreaga", "justin.arreaga@uees.edu.ec", "Computacion", 5);

        HorarioTutoria presencial = new HorarioTutoria("H001", docente, asignatura,
                LocalDate.of(2026, 10, 1), LocalTime.of(10, 0), LocalTime.of(11, 0));
        HorarioTutoria virtual = new HorarioTutoria("H002", docente, asignatura,
                LocalDate.of(2026, 10, 2), LocalTime.of(15, 0), LocalTime.of(16, 0));
        repositorioHorarios.guardar(presencial);
        repositorioHorarios.guardar(virtual);

        demostrarFacade(gestionTutorias, estudiante);
        demostrarObservadores(publicador, bitacora, agenda, docente.getCorreo());
        demostrarStrategy(gestionTutorias, servicioReservas, repositorioHorarios,
                docente, asignatura, estudiante);
        demostrarAdapter();
        demostrarFactoryMethod(estudiante.getCorreo());
    }

    /**
     * Facade: crear una tutoria virtual es <b>una</b> llamada. El cliente
     * no busca el horario, no habla con el proveedor de video y no arma
     * el builder.
     */
    private static void demostrarFacade(GestionTutorias gestionTutorias, Estudiante estudiante) {
        System.out.println("### Facade: una operacion de alto nivel ###");
        System.out.println("Proveedor de videoconferencia: "
                + gestionTutorias.proveedorDeVideoconferencia());

        Reserva tutoria = gestionTutorias.crearTutoriaVirtual(
                estudiante, "H002", "Consulta sobre normalizacion");
        gestionTutorias.confirmar(tutoria.getId());

        System.out.println("Reserva " + tutoria.getId() + " -> " + tutoria.getEstado());
        System.out.println("  Modalidad: " + tutoria.getModalidad());
        System.out.println("  Enlace emitido por el proveedor: "
                + tutoria.getEnlaceVirtual().orElse("no aplica"));
        System.out.println("  Observaciones: "
                + tutoria.getObservaciones().orElse("sin observaciones"));

        ResultadoCancelacion cancelacion = gestionTutorias.cancelarPorDocente(
                tutoria.getId(), "el docente reporto incapacidad medica");
        System.out.println("  Cancelacion: " + cancelacion.motivo()
                + " (penalidad " + cancelacion.penalidadPorcentaje() + " %)");
    }

    /** Observer: un mismo hecho llego a tres receptores distintos. */
    private static void demostrarObservadores(PublicadorReservas publicador,
                                              ObservadorBitacora bitacora,
                                              ObservadorAgendaDocente agenda,
                                              String correoDocente) {
        System.out.println();
        System.out.println("### Observer: un hecho, varios receptores ###");
        System.out.println("Observadores registrados: " + publicador.cantidadDeObservadores());
        System.out.println("Asientos en bitacora: " + bitacora.cantidadDeAsientos());
        System.out.println("Agenda del docente tras la cancelacion: "
                + agenda.bloquesDe(correoDocente));
    }

    /**
     * Strategy: el mismo servicio y el mismo instante tardio producen
     * resultados distintos segun la politica aplicada.
     */
    private static void demostrarStrategy(GestionTutorias gestionTutorias,
                                          ServicioReservas servicioReservas,
                                          RepositorioHorarios repositorioHorarios,
                                          Docente docente, Asignatura asignatura,
                                          Estudiante estudiante) {
        System.out.println();
        System.out.println("### Strategy: tres reglas de cancelacion ###");

        HorarioTutoria bloque = new HorarioTutoria("H003", docente, asignatura,
                LocalDate.of(2026, 10, 3), LocalTime.of(9, 0), LocalTime.of(10, 0));
        repositorioHorarios.guardar(bloque);

        Reserva reserva = gestionTutorias.crearTutoriaPresencial(
                estudiante, bloque.getId(), "Repaso antes del parcial");
        LocalDateTime dosHorasAntes = bloque.inicio().minusHours(2);

        try {
            servicioReservas.cancelarReserva(
                    reserva.getId(), new PoliticaCancelacionAnticipada(), dosHorasAntes);
        } catch (IllegalStateException rechazo) {
            System.out.println("- Anticipada  -> " + rechazo.getMessage());
        }

        ResultadoCancelacion conPenalidad = servicioReservas.cancelarReserva(
                reserva.getId(), new PoliticaCancelacionConPenalidad(), dosHorasAntes);
        System.out.println("- Con penalidad -> " + conPenalidad.motivo());

        ResultadoCancelacion docenteCancela =
                new PoliticaCancelacionDocente().evaluar(reserva, dosHorasAntes);
        System.out.println("- Del docente   -> " + docenteCancela.motivo());
    }

    /**
     * Adapter: dos proveedores con APIs incompatibles, consumidos por la
     * misma interfaz. El bucle no menciona ninguna clase concreta.
     */
    private static void demostrarAdapter() {
        System.out.println();
        System.out.println("### Adapter: dos proveedores, un solo contrato ###");

        List<ProveedorVideoconferencia> proveedores =
                List.of(new AdaptadorZoom(), new AdaptadorMeetInstitucional());
        LocalDateTime inicio = LocalDateTime.of(2026, 10, 4, 11, 0);

        for (ProveedorVideoconferencia proveedor : proveedores) {
            SalaVirtual sala = proveedor.crearSala("Tutoria de bases de datos", inicio, 60);
            System.out.println("- " + proveedor.nombre() + ": " + sala);
        }
    }

    /** Factory Method (Ae2): el mismo aviso por cada canal disponible. */
    private static void demostrarFactoryMethod(String destinatario) {
        System.out.println();
        System.out.println("### Factory Method: mismo aviso por cada canal ###");

        List<CreadorNotificador> creadores = List.of(
                new CreadorNotificadorConsola(),
                new CreadorNotificadorLog(),
                new CreadorNotificadorCorreo());

        for (CreadorNotificador creador : creadores) {
            creador.enviarNotificacion(destinatario, "Recordatorio de tutoria",
                    "Recuerda tu tutoria programada.");
        }
    }

    private App() {
    }
}

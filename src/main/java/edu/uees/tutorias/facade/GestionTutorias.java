package edu.uees.tutorias.facade;

import edu.uees.tutorias.builder.ReservaBuilder;
import edu.uees.tutorias.domain.Estudiante;
import edu.uees.tutorias.domain.HorarioTutoria;
import edu.uees.tutorias.domain.Reserva;
import edu.uees.tutorias.persistence.RepositorioHorarios;
import edu.uees.tutorias.policy.PoliticaCancelacionDocente;
import edu.uees.tutorias.policy.ResultadoCancelacion;
import edu.uees.tutorias.service.ServicioReservas;
import edu.uees.tutorias.video.ProveedorVideoconferencia;
import edu.uees.tutorias.video.SalaVirtual;

import java.util.Objects;

/**
 * Facade: una sola puerta de entrada a los casos de uso de tutorias.
 *
 * <p><b>Problema que resuelve.</b> Para crear una tutoria virtual, el
 * cliente tenia que conocer y ordenar seis colaboradores: buscar el
 * horario en el repositorio, pedir la sala al proveedor de
 * videoconferencia, trasladar su URL y su codigo al
 * {@link ReservaBuilder}, encadenar la configuracion, delegar en
 * {@link ServicioReservas} y confiar en que los observadores estuvieran
 * registrados. Quedaba acoplado al <i>orden</i> de los pasos, de modo
 * que cualquier reordenamiento interno rompia a todos los clientes.</p>
 *
 * <p><b>Que cambia y que permanece estable.</b> Cambian los pasos
 * internos —hoy el enlace lo emite un proveedor externo, antes se
 * escribia a mano—. Permanece estable la operacion de alto nivel
 * {@link #crearTutoriaVirtual(Estudiante, String, String)}, que expresa
 * la intencion del negocio en lugar de su procedimiento.</p>
 *
 * <p><b>Limite deliberado.</b> Esta clase <b>solo coordina</b>. No
 * valida la reserva (lo hace el Builder), no decide si una cancelacion
 * procede (lo hace la Strategy), no elige quien se entera (lo hace el
 * Observer) y no habla el dialecto de ningun proveedor (lo hace el
 * Adapter). Ese limite es lo que la separa de una clase Dios: la fachada
 * no absorbe responsabilidades, solo las encadena. El subsistema sigue
 * siendo accesible por separado para quien lo necesite.</p>
 */
public class GestionTutorias {

    private final ServicioReservas servicioReservas;
    private final RepositorioHorarios repositorioHorarios;
    private final ProveedorVideoconferencia proveedorVideoconferencia;

    public GestionTutorias(ServicioReservas servicioReservas,
                           RepositorioHorarios repositorioHorarios,
                           ProveedorVideoconferencia proveedorVideoconferencia) {
        this.servicioReservas = Objects.requireNonNull(servicioReservas,
                "La fachada coordina un ServicioReservas.");
        this.repositorioHorarios = Objects.requireNonNull(repositorioHorarios,
                "La fachada necesita consultar los horarios publicados.");
        this.proveedorVideoconferencia = Objects.requireNonNull(proveedorVideoconferencia,
                "La fachada necesita un proveedor de videoconferencia.");
    }

    /**
     * Operacion de alto nivel del incremento: reserva la sala en el
     * proveedor y crea la tutoria virtual en una sola llamada.
     *
     * <p>El cliente ya no escribe el enlace: lo emite el proveedor a
     * traves del Adapter, y la fachada lo traslada al builder junto con
     * el codigo de acceso cuando el proveedor lo exige.</p>
     */
    public Reserva crearTutoriaVirtual(Estudiante estudiante, String idHorario, String motivo) {
        HorarioTutoria horario = obtenerHorario(idHorario);

        SalaVirtual sala = proveedorVideoconferencia.crearSala(
                tituloDe(horario, motivo),
                horario.inicio(),
                horario.duracionEnMinutos());

        ReservaBuilder builder = new ReservaBuilder()
                .conEstudiante(estudiante)
                .virtualCon(sala.url())
                .conMotivo(motivo);

        if (sala.exigeCodigo()) {
            builder.conObservaciones("Codigo de acceso " + sala.codigoAcceso()
                    + " (" + sala.proveedor() + ")");
        }

        return servicioReservas.crearReserva(builder, idHorario);
    }

    /** Crea una tutoria presencial: mismo flujo, sin proveedor de video. */
    public Reserva crearTutoriaPresencial(Estudiante estudiante, String idHorario, String motivo) {
        return servicioReservas.crearReserva(
                new ReservaBuilder()
                        .conEstudiante(estudiante)
                        .presencial()
                        .conMotivo(motivo),
                idHorario);
    }

    public void confirmar(String idReserva) {
        servicioReservas.confirmarReserva(idReserva);
    }

    /** Cancelacion solicitada por el estudiante: aplica la politica vigente. */
    public ResultadoCancelacion cancelarPorEstudiante(String idReserva) {
        return servicioReservas.cancelarReserva(idReserva);
    }

    /**
     * Cancelacion originada por el docente: la fachada elige la Strategy
     * que corresponde a ese origen, para que el cliente no tenga que
     * conocer la jerarquia de politicas.
     */
    public ResultadoCancelacion cancelarPorDocente(String idReserva, String justificacion) {
        return servicioReservas.cancelarReserva(idReserva,
                new PoliticaCancelacionDocente(justificacion));
    }

    public void reprogramar(String idReserva, String idNuevoHorario) {
        servicioReservas.reprogramarReserva(idReserva, idNuevoHorario);
    }

    /** Proveedor de videoconferencia vigente, para trazabilidad. */
    public String proveedorDeVideoconferencia() {
        return proveedorVideoconferencia.nombre();
    }

    private HorarioTutoria obtenerHorario(String idHorario) {
        return repositorioHorarios.buscarPorId(idHorario)
                .orElseThrow(() -> new IllegalArgumentException(
                        "No existe el horario " + idHorario));
    }

    private String tituloDe(HorarioTutoria horario, String motivo) {
        String asignatura = horario.getAsignatura().getNombre();
        return (motivo == null || motivo.isBlank())
                ? "Tutoria de " + asignatura
                : "Tutoria de " + asignatura + " - " + motivo;
    }
}

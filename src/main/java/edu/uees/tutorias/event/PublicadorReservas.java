package edu.uees.tutorias.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Subject del patron Observer: mantiene la lista de interesados y les
 * anuncia cada hecho ocurrido sobre una reserva.
 *
 * <p>No conoce ninguna clase concreta de observador ni el orden en que
 * deben reaccionar. Su unica regla propia es de robustez: si un
 * observador falla, los demas igual reciben el aviso. Un canal de correo
 * caido no puede impedir que la bitacora registre la cancelacion, porque
 * el hecho ya ocurrio.</p>
 */
public class PublicadorReservas {

    private final List<ObservadorReserva> observadores = new ArrayList<>();
    private final List<String> fallos = new ArrayList<>();

    /** Registra un interesado. Registrar dos veces el mismo no lo duplica. */
    public void registrar(ObservadorReserva observador) {
        Objects.requireNonNull(observador, "No se puede registrar un observador nulo.");
        if (!observadores.contains(observador)) {
            observadores.add(observador);
        }
    }

    /** Da de baja a un interesado; ignorar uno no registrado no es error. */
    public void quitar(ObservadorReserva observador) {
        observadores.remove(observador);
    }

    /**
     * Anuncia el hecho a todos los observadores registrados.
     *
     * <p>Aisla el fallo de cada uno: si una reaccion lanza una
     * excepcion, se registra en {@link #getFallos()} y el recorrido
     * continua con el siguiente.</p>
     */
    public void publicar(EventoReserva evento) {
        Objects.requireNonNull(evento, "No se puede publicar un evento nulo.");
        for (ObservadorReserva observador : List.copyOf(observadores)) {
            try {
                observador.alOcurrir(evento);
            } catch (RuntimeException fallo) {
                fallos.add(observador.nombre() + " fallo ante " + evento.tipo()
                        + ": " + fallo.getMessage());
            }
        }
    }

    public int cantidadDeObservadores() {
        return observadores.size();
    }

    /** Reacciones que fallaron, para diagnostico. */
    public List<String> getFallos() {
        return List.copyOf(fallos);
    }
}

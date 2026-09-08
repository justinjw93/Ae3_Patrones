package edu.uees.tutorias.event;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * ConcreteObserver de auditoria: deja constancia de cada hecho, con su
 * marca de tiempo y el estado en que quedo la reserva.
 *
 * <p>Es el receptor que motivo el patron. Antes de aplicarlo, agregarlo
 * habria significado editar los cuatro casos de uso de
 * {@code ServicioReservas}; ahora es una clase nueva y una linea de
 * registro en el composition root. El servicio no se entera de que
 * existe.</p>
 */
public class ObservadorBitacora implements ObservadorReserva {

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final List<String> asientos = new ArrayList<>();
    private final boolean imprimirEnConsola;

    public ObservadorBitacora() {
        this(true);
    }

    public ObservadorBitacora(boolean imprimirEnConsola) {
        this.imprimirEnConsola = imprimirEnConsola;
    }

    @Override
    public void alOcurrir(EventoReserva evento) {
        String asiento = String.format("[%s] %-13s reserva=%s estado=%s %s",
                evento.momento().format(FORMATO),
                evento.tipo(),
                evento.reserva().getId(),
                evento.reserva().getEstado(),
                evento.detalle()).trim();

        asientos.add(asiento);
        if (imprimirEnConsola) {
            System.out.println("[BITACORA] " + asiento);
        }
    }

    @Override
    public String nombre() {
        return "Bitacora de auditoria";
    }

    /** Asientos registrados, en orden cronologico. */
    public List<String> getAsientos() {
        return List.copyOf(asientos);
    }

    public int cantidadDeAsientos() {
        return asientos.size();
    }
}

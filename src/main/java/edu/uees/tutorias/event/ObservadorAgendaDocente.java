package edu.uees.tutorias.event;

import edu.uees.tutorias.domain.HorarioTutoria;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ConcreteObserver que mantiene actualizada la agenda de cada docente.
 *
 * <p>Segundo receptor del mismo hecho, y la prueba de que el Observer
 * era necesario: reacciona a los <b>mismos</b> eventos que el canal de
 * mensajeria, pero de otra forma —agrega el bloque cuando la reserva se
 * crea y lo retira cuando se cancela—. Con la notificacion cableada
 * dentro del servicio, esta clase habria obligado a modificarlo por
 * segunda vez.</p>
 */
public class ObservadorAgendaDocente implements ObservadorReserva {

    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /** Correo del docente -> bloques ocupados en su agenda. */
    private final Map<String, Set<String>> agendaPorDocente = new LinkedHashMap<>();

    @Override
    public void alOcurrir(EventoReserva evento) {
        HorarioTutoria horario = evento.reserva().getHorario();
        String docente = evento.correoDocente();
        String bloque = describir(horario);

        switch (evento.tipo()) {
            case CREADA, CONFIRMADA, REPROGRAMADA ->
                    agendaPorDocente.computeIfAbsent(docente, correo -> new LinkedHashSet<>())
                            .add(bloque);

            case CANCELADA -> {
                Set<String> bloques = agendaPorDocente.get(docente);
                if (bloques != null) {
                    bloques.remove(bloque);
                }
            }
        }
    }

    @Override
    public String nombre() {
        return "Agenda del docente";
    }

    /** Bloques ocupados hoy por un docente, en orden de registro. */
    public List<String> bloquesDe(String correoDocente) {
        return List.copyOf(agendaPorDocente.getOrDefault(correoDocente, Set.of()));
    }

    private String describir(HorarioTutoria horario) {
        return horario.getId() + " " + horario.inicio().format(FORMATO)
                + " (" + horario.duracionEnMinutos() + " min) "
                + horario.getAsignatura().getNombre();
    }
}

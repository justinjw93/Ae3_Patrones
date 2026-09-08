package edu.uees.tutorias.video;

import java.time.LocalDateTime;

/**
 * Target del patron Adapter: el contrato de videoconferencia que
 * <b>el sistema define para si mismo</b>.
 *
 * <p><b>Problema que resuelve.</b> Hasta Ae2, el enlace de una tutoria
 * virtual era un {@code String} que alguien escribia a mano
 * ({@code virtualCon("https://meet.uees.edu.ec/tutoria-h003")}). No
 * habia sala reservada en ningun proveedor, nadie garantizaba que el
 * enlace existiera y no habia codigo de acceso. Al integrar un proveedor
 * real aparece el choque de interfaces: la API de Zoom recibe un
 * {@code Map} de parametros, espera el inicio en <i>epoch millis</i> y la
 * duracion en <i>segundos</i>, y responde con una cadena en su propio
 * formato; el sistema, en cambio, razona con {@link LocalDateTime},
 * minutos y objetos de dominio.</p>
 *
 * <p><b>Que cambia y que permanece estable.</b> Cambia el proveedor y su
 * dialecto: eso queda encerrado en cada Adapter. Permanece estable esta
 * interfaz y el tipo {@link SalaVirtual}, que es lo unico que ve el resto
 * del sistema. Cambiar de Zoom al Meet institucional es cambiar el
 * adaptador que se inyecta, sin tocar ni una linea del servicio, la
 * fachada o el dominio (Dependency Inversion).</p>
 *
 * <p>La interfaz expone solo lo que el sistema necesita —reservar una
 * sala para un bloque de tutoria— y no el catalogo completo del SDK
 * (grabaciones, salas de espera, encuestas). Eso es Interface
 * Segregation: el contrato lo dicta el consumidor, no el proveedor.</p>
 */
public interface ProveedorVideoconferencia {

    /**
     * Reserva una sala para un bloque de tutoria.
     *
     * @param titulo           asunto visible de la sesion
     * @param inicio           instante de inicio, en hora local
     * @param duracionMinutos  duracion del bloque, en minutos
     * @return la sala lista para compartirse con el estudiante
     * @throws IllegalStateException si el proveedor rechaza la reserva
     */
    SalaVirtual crearSala(String titulo, LocalDateTime inicio, int duracionMinutos);

    /** Nombre legible del proveedor, para trazabilidad. */
    String nombre();
}

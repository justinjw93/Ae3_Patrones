package edu.uees.tutorias.video;

import java.util.Objects;

/**
 * Sala de videoconferencia ya reservada, expresada en los terminos del
 * Sistema de gestion de tutorias.
 *
 * <p>Es el tipo que el sistema entiende, no el que devuelve ningun
 * proveedor. Cada Adapter se encarga de traducir la respuesta de su SDK
 * a esta forma, de modo que el resto del codigo nunca ve un
 * {@code join_url}, un {@code passcode} ni un {@code meeting_id}.</p>
 *
 * @param url          enlace de conexion que recibe el estudiante
 * @param codigoAcceso clave de ingreso; cadena vacia si el proveedor no usa
 * @param proveedor    nombre legible de quien hospeda la sala
 */
public record SalaVirtual(String url, String codigoAcceso, String proveedor) {

    public SalaVirtual {
        Objects.requireNonNull(url, "Una sala virtual necesita un enlace.");
        Objects.requireNonNull(proveedor, "Una sala virtual necesita un proveedor.");
        if (url.isBlank()) {
            throw new IllegalArgumentException("El enlace de la sala no puede estar vacio.");
        }
        codigoAcceso = (codigoAcceso == null) ? "" : codigoAcceso;
    }

    public boolean exigeCodigo() {
        return !codigoAcceso.isBlank();
    }

    @Override
    public String toString() {
        return exigeCodigo()
                ? url + " (codigo " + codigoAcceso + ", " + proveedor + ")"
                : url + " (" + proveedor + ")";
    }
}

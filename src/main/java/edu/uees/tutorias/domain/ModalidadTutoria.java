package edu.uees.tutorias.domain;

/**
 * Modalidad en la que se dictara la tutoria asociada a una
 * {@link Reserva}.
 *
 * <p>La modalidad VIRTUAL exige un enlace de conexion; la PRESENCIAL no.
 * Esa regla cruzada entre dos campos opcionales es una de las razones
 * por las que la construccion de una Reserva necesita validarse en un
 * solo punto.</p>
 */
public enum ModalidadTutoria {
    PRESENCIAL,
    VIRTUAL
}

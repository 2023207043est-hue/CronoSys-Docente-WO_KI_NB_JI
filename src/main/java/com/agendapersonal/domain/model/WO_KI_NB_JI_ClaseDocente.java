package com.agendapersonal.domain.model;

/** Una clase o grupo gestionado en el espacio docente; {@code notaMinima} es la nota aprobatoria del curso. */
public record WO_KI_NB_JI_ClaseDocente(int id, String nombre, String grupo, String periodo, double notaMinima) {
    /** Nota aprobatoria por defecto (escala vigesimal 0–20). */
    public static final double NOTA_MINIMA_DEFECTO = 11;

    public WO_KI_NB_JI_ClaseDocente(int id, String nombre, String grupo, String periodo) {
        this(id, nombre, grupo, periodo, NOTA_MINIMA_DEFECTO);
    }

    @Override public String toString() {
        return grupo == null || grupo.isBlank() ? nombre : nombre + " · " + grupo;
    }
}

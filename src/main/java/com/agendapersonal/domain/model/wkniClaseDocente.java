package com.agendapersonal.domain.model;

/** Una clase o grupo gestionado en el espacio docente; {@code notaMinima} es la nota aprobatoria del curso. */
public record wkniClaseDocente(int wkniId, String wkniNombre, String wkniGrupo, String wkniPeriodo, double wkniNotaMinima) {
    /** Nota aprobatoria por defecto (escala vigesimal 0–20). */
    public static final double wkniNOTA_MINIMA_DEFECTO = 11;

    public wkniClaseDocente(int wkniId, String wkniNombre, String wkniGrupo, String wkniPeriodo) {
        this(wkniId, wkniNombre, wkniGrupo, wkniPeriodo, wkniNOTA_MINIMA_DEFECTO);
    }

    @Override public String toString() {
        return wkniGrupo == null || wkniGrupo.isBlank() ? wkniNombre : wkniNombre + " · " + wkniGrupo;
    }
}

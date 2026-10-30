package com.agendapersonal.model;

/** Estados cerrados de una tarea. */
public enum wkniEstadoTarea {
    PENDIENTE("Pendiente"), EN_PROGRESO("En progreso"), COMPLETADA("Completada");

    private final String wkniEtiqueta;

    wkniEstadoTarea(String wkniEtiqueta) { this.wkniEtiqueta = wkniEtiqueta; }

    public String wkniEtiqueta() { return wkniEtiqueta; }

    public boolean wkniEsCompletada() { return this == COMPLETADA; }

    /** Parseo estricto e insensible a mayúsculas/acentos. Null o vacío → PENDIENTE. */
    public static wkniEstadoTarea wkniDesde(String wkniTexto) {
        if (wkniTexto == null || wkniTexto.isBlank()) return PENDIENTE;
        String wkniT = wkniPrioridad.wkniNormalizar(wkniTexto);
        for (wkniEstadoTarea wkniE : values())
            if (wkniPrioridad.wkniNormalizar(wkniE.wkniEtiqueta).equals(wkniT) || wkniPrioridad.wkniNormalizar(wkniE.name()).equals(wkniT)) return wkniE;
        throw new IllegalArgumentException(
            "Estado no válido: '" + wkniTexto + "' (use Pendiente, En progreso o Completada)");
    }

    /** Parseo tolerante para datos ya guardados: ante basura devuelve PENDIENTE. */
    public static wkniEstadoTarea wkniDesdeBD(String wkniTexto) {
        try { return wkniDesde(wkniTexto); } catch (IllegalArgumentException wkniE) { return PENDIENTE; }
    }
}

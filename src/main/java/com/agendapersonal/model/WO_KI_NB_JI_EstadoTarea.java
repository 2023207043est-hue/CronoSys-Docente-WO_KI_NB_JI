package com.agendapersonal.model;

/** Estados cerrados de una tarea. */
public enum WO_KI_NB_JI_EstadoTarea {
    PENDIENTE("Pendiente"), EN_PROGRESO("En progreso"), COMPLETADA("Completada");

    private final String etiqueta;

    WO_KI_NB_JI_EstadoTarea(String etiqueta) { this.etiqueta = etiqueta; }

    public String WO_KI_NB_JI_etiqueta() { return etiqueta; }

    public boolean WO_KI_NB_JI_esCompletada() { return this == COMPLETADA; }

    /** Parseo estricto e insensible a mayúsculas/acentos. Null o vacío → PENDIENTE. */
    public static WO_KI_NB_JI_EstadoTarea WO_KI_NB_JI_desde(String texto) {
        if (texto == null || texto.isBlank()) return PENDIENTE;
        String t = WO_KI_NB_JI_Prioridad.WO_KI_NB_JI_normalizar(texto);
        for (WO_KI_NB_JI_EstadoTarea e : values())
            if (WO_KI_NB_JI_Prioridad.WO_KI_NB_JI_normalizar(e.etiqueta).equals(t) || WO_KI_NB_JI_Prioridad.WO_KI_NB_JI_normalizar(e.name()).equals(t)) return e;
        throw new IllegalArgumentException(
            "Estado no válido: '" + texto + "' (use Pendiente, En progreso o Completada)");
    }

    /** Parseo tolerante para datos ya guardados: ante basura devuelve PENDIENTE. */
    public static WO_KI_NB_JI_EstadoTarea WO_KI_NB_JI_desdeBD(String texto) {
        try { return WO_KI_NB_JI_desde(texto); } catch (IllegalArgumentException e) { return PENDIENTE; }
    }
}

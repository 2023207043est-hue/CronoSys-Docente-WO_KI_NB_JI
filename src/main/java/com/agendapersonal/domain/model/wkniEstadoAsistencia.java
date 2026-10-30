package com.agendapersonal.domain.model;

import java.util.Arrays;

/**
 * Estados de asistencia. {@code valor} es lo que se guarda en la BD (no cambiar: hay registros antiguos)
 * y {@code etiqueta} lo que ve el docente ("Retardo" se muestra como "Tardanza").
 */
public enum wkniEstadoAsistencia {
    PRESENTE("Presente", "Presente", "P"),
    FALTA("Falta", "Falta", "F"),
    TARDANZA("Retardo", "Tardanza", "T"),
    JUSTIFICADA("Justificada", "Justificada", "J");

    private final String wkniValor;
    private final String wkniEtiqueta;
    private final String wkniLetra;

    wkniEstadoAsistencia(String wkniValor, String wkniEtiqueta, String wkniLetra) {
        this.wkniValor = wkniValor; this.wkniEtiqueta = wkniEtiqueta; this.wkniLetra = wkniLetra;
    }

    public String wkniValor() { return wkniValor; }
    public String wkniEtiqueta() { return wkniEtiqueta; }
    public String wkniLetra() { return wkniLetra; }
    /** Cuenta como asistió (para el %). */
    public boolean wkniAsistio() { return this == PRESENTE || this == TARDANZA; }

    /** Estado a partir del valor guardado (o null si no hay registro / valor desconocido). */
    public static wkniEstadoAsistencia wkniDe(String wkniValor) {
        if (wkniValor == null) return null;
        return Arrays.stream(values()).filter(wkniE -> wkniE.wkniValor.equals(wkniValor) || wkniE.wkniEtiqueta.equals(wkniValor)).findFirst().orElse(null);
    }

    /** Siguiente estado al hacer clic en una celda (null = volver a "sin registro"). */
    public static wkniEstadoAsistencia wkniSiguiente(wkniEstadoAsistencia wkniActual) {
        if (wkniActual == null) return PRESENTE;
        int wkniI = wkniActual.ordinal() + 1;
        return wkniI >= values().length ? null : values()[wkniI];
    }
}

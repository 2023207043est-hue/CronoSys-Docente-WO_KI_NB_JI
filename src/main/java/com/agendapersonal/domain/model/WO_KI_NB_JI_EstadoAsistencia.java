package com.agendapersonal.domain.model;

import java.util.Arrays;

/**
 * Estados de asistencia. {@code valor} es lo que se guarda en la BD (no cambiar: hay registros antiguos)
 * y {@code etiqueta} lo que ve el docente ("Retardo" se muestra como "Tardanza").
 */
public enum WO_KI_NB_JI_EstadoAsistencia {
    PRESENTE("Presente", "Presente", "P"),
    FALTA("Falta", "Falta", "F"),
    TARDANZA("Retardo", "Tardanza", "T"),
    JUSTIFICADA("Justificada", "Justificada", "J");

    private final String valor;
    private final String etiqueta;
    private final String letra;

    WO_KI_NB_JI_EstadoAsistencia(String valor, String etiqueta, String letra) {
        this.valor = valor; this.etiqueta = etiqueta; this.letra = letra;
    }

    public String WO_KI_NB_JI_valor() { return valor; }
    public String WO_KI_NB_JI_etiqueta() { return etiqueta; }
    public String WO_KI_NB_JI_letra() { return letra; }
    /** Cuenta como asistió (para el %). */
    public boolean WO_KI_NB_JI_asistio() { return this == PRESENTE || this == TARDANZA; }

    /** Estado a partir del valor guardado (o null si no hay registro / valor desconocido). */
    public static WO_KI_NB_JI_EstadoAsistencia WO_KI_NB_JI_de(String valor) {
        if (valor == null) return null;
        return Arrays.stream(values()).filter(e -> e.valor.equals(valor) || e.etiqueta.equals(valor)).findFirst().orElse(null);
    }

    /** Siguiente estado al hacer clic en una celda (null = volver a "sin registro"). */
    public static WO_KI_NB_JI_EstadoAsistencia WO_KI_NB_JI_siguiente(WO_KI_NB_JI_EstadoAsistencia actual) {
        if (actual == null) return PRESENTE;
        int i = actual.ordinal() + 1;
        return i >= values().length ? null : values()[i];
    }
}

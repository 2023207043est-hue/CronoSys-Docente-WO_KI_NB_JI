package com.agendapersonal.domain.model;

import java.util.Arrays;

/** Situación de un trabajo/evaluación para un estudiante. */
public enum WO_KI_NB_JI_EstadoEntrega {
    ENTREGADO("Entregado", "✓"),
    TARDE("Tarde", "⏱"),
    NO_ENTREGADO("No entregado", "✗");

    private final String valor;
    private final String simbolo;

    WO_KI_NB_JI_EstadoEntrega(String valor, String simbolo) { this.valor = valor; this.simbolo = simbolo; }

    public String WO_KI_NB_JI_valor() { return valor; }
    public String WO_KI_NB_JI_simbolo() { return simbolo; }

    public static WO_KI_NB_JI_EstadoEntrega WO_KI_NB_JI_de(String valor) {
        if (valor == null) return null;
        return Arrays.stream(values()).filter(e -> e.valor.equals(valor)).findFirst().orElse(null);
    }

    /** Ciclo al hacer clic: sin registro → Entregado → Tarde → No entregado → sin registro. */
    public static WO_KI_NB_JI_EstadoEntrega WO_KI_NB_JI_siguiente(WO_KI_NB_JI_EstadoEntrega actual) {
        if (actual == null) return ENTREGADO;
        int i = actual.ordinal() + 1;
        return i >= values().length ? null : values()[i];
    }
}

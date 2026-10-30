package com.agendapersonal.domain.model;

import java.util.Arrays;

/** Situación de un trabajo/evaluación para un estudiante. */
public enum wkniEstadoEntrega {
    ENTREGADO("Entregado", "✓"),
    TARDE("Tarde", "⏱"),
    NO_ENTREGADO("No entregado", "✗");

    private final String wkniValor;
    private final String wkniSimbolo;

    wkniEstadoEntrega(String wkniValor, String wkniSimbolo) { this.wkniValor = wkniValor; this.wkniSimbolo = wkniSimbolo; }

    public String wkniValor() { return wkniValor; }
    public String wkniSimbolo() { return wkniSimbolo; }

    public static wkniEstadoEntrega wkniDe(String wkniValor) {
        if (wkniValor == null) return null;
        return Arrays.stream(values()).filter(wkniE -> wkniE.wkniValor.equals(wkniValor)).findFirst().orElse(null);
    }

    /** Ciclo al hacer clic: sin registro → Entregado → Tarde → No entregado → sin registro. */
    public static wkniEstadoEntrega wkniSiguiente(wkniEstadoEntrega wkniActual) {
        if (wkniActual == null) return ENTREGADO;
        int wkniI = wkniActual.ordinal() + 1;
        return wkniI >= values().length ? null : values()[wkniI];
    }
}

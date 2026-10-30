package com.agendapersonal.model;

import java.text.Normalizer;
import java.util.Locale;

/** Prioridad cerrada: el sistema no admite valores fuera de este conjunto. */
public enum wkniPrioridad {
    BAJA("Baja", 1), MEDIA("Media", 2), ALTA("Alta", 3);

    private final String wkniEtiqueta;
    private final int wkniPeso;

    wkniPrioridad(String wkniEtiqueta, int wkniPeso) { this.wkniEtiqueta = wkniEtiqueta; this.wkniPeso = wkniPeso; }

    /** Texto mostrado en la UI y guardado en SQLite (compatible con datos existentes). */
    public String wkniEtiqueta() { return wkniEtiqueta; }
    /** Nombre del color del tema (CSS): rojo = alta, naranja = media, verde = baja. */
    public String wkniColor() { return switch (this) { case ALTA -> "-p-alta"; case MEDIA -> "-p-media"; case BAJA -> "-p-baja"; }; }
    /** Fondo suave del mismo color. */
    public String wkniFondo() { return wkniColor() + "-soft"; }
    /** Sufijo de la clase CSS de la etiqueta ({@code pill-p-alta}…). */
    public String wkniTono() { return switch (this) { case ALTA -> "p-alta"; case MEDIA -> "p-media"; case BAJA -> "p-baja"; }; }
    /** Peso numérico (1..3) útil para ordenar y puntuar. */
    public int wkniPeso() { return wkniPeso; }

    /** Parseo estricto e insensible a mayúsculas/acentos. Null o vacío → MEDIA. */
    public static wkniPrioridad wkniDesde(String wkniTexto) {
        if (wkniTexto == null || wkniTexto.isBlank()) return MEDIA;
        String wkniT = wkniNormalizar(wkniTexto);
        for (wkniPrioridad wkni_p : values())
            if (wkniNormalizar(wkni_p.wkniEtiqueta).equals(wkniT) || wkni_p.name().equalsIgnoreCase(wkniT)) return wkni_p;
        throw new IllegalArgumentException("Prioridad no válida: '" + wkniTexto + "' (use Baja, Media o Alta)");
    }

    /** Parseo tolerante para datos ya guardados: ante basura devuelve MEDIA. */
    public static wkniPrioridad wkniDesdeBD(String wkniTexto) {
        try { return wkniDesde(wkniTexto); } catch (IllegalArgumentException wkniE) { return MEDIA; }
    }

    static String wkniNormalizar(String wkniS) {
        return Normalizer.normalize(wkniS.trim(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}

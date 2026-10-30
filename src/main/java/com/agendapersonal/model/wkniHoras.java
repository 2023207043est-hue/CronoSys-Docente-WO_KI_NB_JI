package com.agendapersonal.model;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/** Conversión estricta de "HH:mm" a {@link LocalTime} (rechaza "25:00", "9.30", "", null...). */
public final class wkniHoras {
    private static final DateTimeFormatter wkniFMT = DateTimeFormatter.ofPattern("H:mm").withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter wkniSALIDA = DateTimeFormatter.ofPattern("HH:mm");
    private wkniHoras() {}

    public static LocalTime wkniParsear(String wkniTexto) {
        if (wkniTexto == null) throw new DateTimeParseException("Hora vacía", "", 0);
        return LocalTime.parse(wkniTexto.trim(), wkniFMT);
    }

    public static boolean wkniEsValida(String wkniTexto) {
        try { wkniParsear(wkniTexto); return true; } catch (DateTimeParseException wkniE) { return false; }
    }

    /** "9:5" no es válido; "9:05" → "09:05". Si el texto no es una hora válida lo devuelve igual (la validación lo rechaza). */
    public static String wkniNormalizar(String wkniTexto) {
        return wkniEsValida(wkniTexto) ? wkniParsear(wkniTexto).format(wkniSALIDA) : wkniTexto;
    }
}

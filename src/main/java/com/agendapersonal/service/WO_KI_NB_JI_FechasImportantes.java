package com.agendapersonal.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Fechas importantes de un contacto (aniversario, graduación...) guardadas como "etiqueta=yyyy-MM-dd;etiqueta=...". */
public final class WO_KI_NB_JI_FechasImportantes {
    private WO_KI_NB_JI_FechasImportantes() {}

    public record WO_KI_NB_JI_Fecha(String etiqueta, LocalDate fecha) {}

    public static List<WO_KI_NB_JI_Fecha> WO_KI_NB_JI_leer(String texto) {
        List<WO_KI_NB_JI_Fecha> out = new ArrayList<>();
        if (texto == null || texto.isBlank()) return out;
        for (String parte : texto.split(";")) {
            int i = parte.lastIndexOf('=');
            if (i <= 0) continue;
            try { out.add(new WO_KI_NB_JI_Fecha(parte.substring(0, i).trim(), LocalDate.parse(parte.substring(i + 1).trim()))); }
            catch (DateTimeParseException ignored) { /* entrada dañada: se omite */ }
        }
        return out;
    }

    public static String WO_KI_NB_JI_escribir(List<WO_KI_NB_JI_Fecha> fechas) {
        StringBuilder b = new StringBuilder();
        for (WO_KI_NB_JI_Fecha f : fechas) {
            if (f.fecha() == null) continue;
            String et = f.etiqueta() == null || f.etiqueta().isBlank() ? "Fecha importante" : f.etiqueta().replace(";", ",").replace("=", "-").trim();
            if (b.length() > 0) b.append(';');
            b.append(et).append('=').append(f.fecha());
        }
        return b.toString();
    }

    /** Días que faltan para la próxima vez que cae esta fecha (se repite cada año). */
    public static long WO_KI_NB_JI_diasHasta(LocalDate fecha, LocalDate hoy) {
        LocalDate prox = fecha.withYear(hoy.getYear());
        if (prox.isBefore(hoy)) prox = prox.plusYears(1);
        return java.time.temporal.ChronoUnit.DAYS.between(hoy, prox);
    }
}

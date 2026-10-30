package com.agendapersonal.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/** Fechas importantes de un contacto (aniversario, graduación...) guardadas como "etiqueta=yyyy-MM-dd;etiqueta=...". */
public final class wkniFechasImportantes {
    private wkniFechasImportantes() {}

    public record wkniFecha(String wkniEtiqueta, LocalDate wkni_fecha) {}

    public static List<wkniFecha> wkniLeer(String wkniTexto) {
        List<wkniFecha> wkniOut = new ArrayList<>();
        if (wkniTexto == null || wkniTexto.isBlank()) return wkniOut;
        for (String wkniParte : wkniTexto.split(";")) {
            int wkniI = wkniParte.lastIndexOf('=');
            if (wkniI <= 0) continue;
            try { wkniOut.add(new wkniFecha(wkniParte.substring(0, wkniI).trim(), LocalDate.parse(wkniParte.substring(wkniI + 1).trim()))); }
            catch (DateTimeParseException wkniIgnored) { /* entrada dañada: se omite */ }
        }
        return wkniOut;
    }

    public static String wkniEscribir(List<wkniFecha> wkniFechas) {
        StringBuilder wkniB = new StringBuilder();
        for (wkniFecha wkniF : wkniFechas) {
            if (wkniF.wkni_fecha() == null) continue;
            String wkniEt = wkniF.wkniEtiqueta() == null || wkniF.wkniEtiqueta().isBlank() ? "Fecha importante" : wkniF.wkniEtiqueta().replace(";", ",").replace("=", "-").trim();
            if (wkniB.length() > 0) wkniB.append(';');
            wkniB.append(wkniEt).append('=').append(wkniF.wkni_fecha());
        }
        return wkniB.toString();
    }

    /** Días que faltan para la próxima vez que cae esta fecha (se repite cada año). */
    public static long wkniDiasHasta(LocalDate wkni_fecha, LocalDate wkniHoy) {
        LocalDate wkniProx = wkni_fecha.withYear(wkniHoy.getYear());
        if (wkniProx.isBefore(wkniHoy)) wkniProx = wkniProx.plusYears(1);
        return java.time.temporal.ChronoUnit.DAYS.between(wkniHoy, wkniProx);
    }
}

package com.agendapersonal.service;

import com.agendapersonal.model.wkniEvento;
import com.agendapersonal.model.wkniHoras;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Reglas de negocio de la agenda (sin UI ni base de datos, 100 % testeable):
 * validación, duración, detección de solapamientos y sugerencia de huecos libres.
 */
public final class wkniAgendaService {
    private wkniAgendaService() {}

    /** Intervalo semiabierto [inicio, fin) en minutos desde las 00:00. */
    private record wkniTramo(int wkniIni, int wkniFin) {}

    // ---------- Validación ----------

    /** @return mensaje de error, o vacío si el evento es válido. */
    public static Optional<String> wkniValidar(wkniEvento wkniE) {
        if (wkniE.wkniGetTitulo() == null || wkniE.wkniGetTitulo().isBlank()) return Optional.of("El título es obligatorio.");
        if (wkniE.wkniGetFecha() == null) return Optional.of("La fecha es obligatoria.");
        try {
            LocalTime wkniIni = wkniE.wkniGetInicio(), wkniFin = wkniE.wkniGetFin();
            if (!wkniFin.isAfter(wkniIni)) return Optional.of("La hora de fin debe ser posterior a la de inicio.");
        } catch (DateTimeParseException wkniEx) {
            return Optional.of("Use el formato de hora HH:mm (ej. 09:30).");
        }
        return Optional.empty();
    }

    // ---------- Duración ----------

    /** Duración exacta del evento en minutos. Requiere un evento válido. */
    public static int wkniDuracionMinutos(wkniEvento wkniE) {
        return (int) Duration.between(wkniE.wkniGetInicio(), wkniE.wkniGetFin()).toMinutes();
    }

    /** 90 → "1 h 30 min", 45 → "45 min", 120 → "2 h". */
    public static String wkniFormatearMinutos(int wkniMinutos) {
        int wkniH = wkniMinutos / 60, wkniM = wkniMinutos % 60;
        if (wkniH == 0) return wkniM + " min";
        return wkniM == 0 ? wkniH + " h" : wkniH + " h " + wkniM + " min";
    }

    // ---------- Solapamientos ----------

    /**
     * Calcula la intersección real de rangos (no solo "misma hora de inicio").
     * Eventos que solo se tocan en el borde (10:00 fin / 10:00 inicio) NO se cruzan.
     * Ignora el propio evento (mismo id ≠ 0) para permitir editarlo.
     */
    public static List<wkniConflictoHorario> wkniDetectarConflictos(wkniEvento wkniNuevo, Collection<wkniEvento> wkniExistentes) {
        List<wkniConflictoHorario> wkniR = new ArrayList<>();
        wkniTramo wkniT = wkniTramoDe(wkniNuevo);
        if (wkniT == null) return wkniR;
        for (wkniEvento wkniO : wkniExistentes) {
            if (wkniO.wkniGetFecha() == null || !wkniO.wkniGetFecha().equals(wkniNuevo.wkniGetFecha())) continue;
            if (wkniNuevo.wkniGetId() != 0 && wkniO.wkniGetId() == wkniNuevo.wkniGetId()) continue;
            wkniTramo wkniU = wkniTramoDe(wkniO);
            if (wkniU == null) continue;
            int wkniSolape = Math.min(wkniT.wkniFin, wkniU.wkniFin) - Math.max(wkniT.wkniIni, wkniU.wkniIni);
            if (wkniSolape > 0) wkniR.add(new wkniConflictoHorario(wkniO, wkniSolape));
        }
        wkniR.sort(Comparator.comparingInt((wkniConflictoHorario wkniC) -> wkniC.wkniExistente().wkniGetInicio().toSecondOfDay()));
        return wkniR;
    }

    // ---------- Carga y huecos libres ----------

    /** Minutos realmente ocupados en un día (une eventos solapados para no contar dos veces). */
    public static int wkniMinutosOcupados(LocalDate wkniDia, Collection<wkniEvento> wkniEventos) {
        int wkniTotal = 0;
        for (wkniTramo wkniT : wkniUnir(wkniTramosDelDia(wkniDia, wkniEventos))) wkniTotal += wkniT.wkniFin - wkniT.wkniIni;
        return wkniTotal;
    }

    /**
     * Sugiere el primer hueco libre de al menos {@code duracionMin} minutos dentro de la
     * ventana [desde, hasta] del día indicado.
     */
    public static Optional<LocalTime[]> wkniSugerirHueco(LocalDate wkniDia, Collection<wkniEvento> wkniEventos,
                                                      int wkniDuracionMin, LocalTime wkniDesde, LocalTime wkniHasta) {
        int wkniCursor = wkniDesde.toSecondOfDay() / 60, wkniLimite = wkniHasta.toSecondOfDay() / 60;
        for (wkniTramo wkniT : wkniUnir(wkniTramosDelDia(wkniDia, wkniEventos))) {
            if (wkniT.wkniFin <= wkniCursor) continue;
            if (wkniT.wkniIni - wkniCursor >= wkniDuracionMin) break;
            wkniCursor = Math.max(wkniCursor, wkniT.wkniFin);
        }
        if (wkniCursor + wkniDuracionMin > wkniLimite) return Optional.empty();
        return Optional.of(new LocalTime[]{LocalTime.ofSecondOfDay(wkniCursor * 60L),
                                           LocalTime.ofSecondOfDay((wkniCursor + wkniDuracionMin) * 60L)});
    }

    // ---------- Internos ----------

    private static wkniTramo wkniTramoDe(wkniEvento wkniE) {
        try {
            int wkniI = wkniE.wkniGetInicio().toSecondOfDay() / 60, wkniF = wkniE.wkniGetFin().toSecondOfDay() / 60;
            return wkniF > wkniI ? new wkniTramo(wkniI, wkniF) : null;
        } catch (DateTimeParseException wkniEx) { return null; }
    }

    private static List<wkniTramo> wkniTramosDelDia(LocalDate wkniDia, Collection<wkniEvento> wkniEventos) {
        List<wkniTramo> wkniR = new ArrayList<>();
        for (wkniEvento wkniE : wkniEventos) {
            if (!wkniDia.equals(wkniE.wkniGetFecha())) continue;
            wkniTramo wkniT = wkniTramoDe(wkniE);
            if (wkniT != null) wkniR.add(wkniT);
        }
        return wkniR;
    }

    private static List<wkniTramo> wkniUnir(List<wkniTramo> wkniIn) {
        List<wkniTramo> wkniS = new ArrayList<>(wkniIn);
        wkniS.sort(Comparator.comparingInt(wkniTramo::wkniIni));
        List<wkniTramo> wkniOut = new ArrayList<>();
        for (wkniTramo wkniT : wkniS) {
            if (!wkniOut.isEmpty() && wkniT.wkniIni <= wkniOut.getLast().wkniFin) {
                wkniTramo wkniL = wkniOut.removeLast();
                wkniOut.add(new wkniTramo(wkniL.wkniIni, Math.max(wkniL.wkniFin, wkniT.wkniFin)));
            } else wkniOut.add(wkniT);
        }
        return wkniOut;
    }

    /** Formato "HH:mm" para mostrar horas calculadas. */
    public static String wkniHora(LocalTime wkniT) { return wkniT.format(wkniHoras.wkniSALIDA); }
}

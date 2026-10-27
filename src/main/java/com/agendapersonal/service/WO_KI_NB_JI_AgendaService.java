package com.agendapersonal.service;

import com.agendapersonal.model.WO_KI_NB_JI_Evento;
import com.agendapersonal.model.WO_KI_NB_JI_Horas;
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
public final class WO_KI_NB_JI_AgendaService {
    private WO_KI_NB_JI_AgendaService() {}

    /** Intervalo semiabierto [inicio, fin) en minutos desde las 00:00. */
    private record WO_KI_NB_JI_Tramo(int ini, int fin) {}

    // ---------- Validación ----------

    /** @return mensaje de error, o vacío si el evento es válido. */
    public static Optional<String> WO_KI_NB_JI_validar(WO_KI_NB_JI_Evento e) {
        if (e.WO_KI_NB_JI_getTitulo() == null || e.WO_KI_NB_JI_getTitulo().isBlank()) return Optional.of("El título es obligatorio.");
        if (e.WO_KI_NB_JI_getFecha() == null) return Optional.of("La fecha es obligatoria.");
        try {
            LocalTime ini = e.WO_KI_NB_JI_getInicio(), fin = e.WO_KI_NB_JI_getFin();
            if (!fin.isAfter(ini)) return Optional.of("La hora de fin debe ser posterior a la de inicio.");
        } catch (DateTimeParseException ex) {
            return Optional.of("Use el formato de hora HH:mm (ej. 09:30).");
        }
        return Optional.empty();
    }

    // ---------- Duración ----------

    /** Duración exacta del evento en minutos. Requiere un evento válido. */
    public static int WO_KI_NB_JI_duracionMinutos(WO_KI_NB_JI_Evento e) {
        return (int) Duration.between(e.WO_KI_NB_JI_getInicio(), e.WO_KI_NB_JI_getFin()).toMinutes();
    }

    /** 90 → "1 h 30 min", 45 → "45 min", 120 → "2 h". */
    public static String WO_KI_NB_JI_formatearMinutos(int minutos) {
        int h = minutos / 60, m = minutos % 60;
        if (h == 0) return m + " min";
        return m == 0 ? h + " h" : h + " h " + m + " min";
    }

    // ---------- Solapamientos ----------

    /**
     * Calcula la intersección real de rangos (no solo "misma hora de inicio").
     * Eventos que solo se tocan en el borde (10:00 fin / 10:00 inicio) NO se cruzan.
     * Ignora el propio evento (mismo id ≠ 0) para permitir editarlo.
     */
    public static List<WO_KI_NB_JI_ConflictoHorario> WO_KI_NB_JI_detectarConflictos(WO_KI_NB_JI_Evento nuevo, Collection<WO_KI_NB_JI_Evento> existentes) {
        List<WO_KI_NB_JI_ConflictoHorario> r = new ArrayList<>();
        WO_KI_NB_JI_Tramo t = WO_KI_NB_JI_tramoDe(nuevo);
        if (t == null) return r;
        for (WO_KI_NB_JI_Evento o : existentes) {
            if (o.WO_KI_NB_JI_getFecha() == null || !o.WO_KI_NB_JI_getFecha().equals(nuevo.WO_KI_NB_JI_getFecha())) continue;
            if (nuevo.WO_KI_NB_JI_getId() != 0 && o.WO_KI_NB_JI_getId() == nuevo.WO_KI_NB_JI_getId()) continue;
            WO_KI_NB_JI_Tramo u = WO_KI_NB_JI_tramoDe(o);
            if (u == null) continue;
            int solape = Math.min(t.fin, u.fin) - Math.max(t.ini, u.ini);
            if (solape > 0) r.add(new WO_KI_NB_JI_ConflictoHorario(o, solape));
        }
        r.sort(Comparator.comparingInt((WO_KI_NB_JI_ConflictoHorario c) -> c.existente().WO_KI_NB_JI_getInicio().toSecondOfDay()));
        return r;
    }

    // ---------- Carga y huecos libres ----------

    /** Minutos realmente ocupados en un día (une eventos solapados para no contar dos veces). */
    public static int WO_KI_NB_JI_minutosOcupados(LocalDate dia, Collection<WO_KI_NB_JI_Evento> eventos) {
        int total = 0;
        for (WO_KI_NB_JI_Tramo t : WO_KI_NB_JI_unir(WO_KI_NB_JI_tramosDelDia(dia, eventos))) total += t.fin - t.ini;
        return total;
    }

    /**
     * Sugiere el primer hueco libre de al menos {@code duracionMin} minutos dentro de la
     * ventana [desde, hasta] del día indicado.
     */
    public static Optional<LocalTime[]> WO_KI_NB_JI_sugerirHueco(LocalDate dia, Collection<WO_KI_NB_JI_Evento> eventos,
                                                      int duracionMin, LocalTime desde, LocalTime hasta) {
        int cursor = desde.toSecondOfDay() / 60, limite = hasta.toSecondOfDay() / 60;
        for (WO_KI_NB_JI_Tramo t : WO_KI_NB_JI_unir(WO_KI_NB_JI_tramosDelDia(dia, eventos))) {
            if (t.fin <= cursor) continue;
            if (t.ini - cursor >= duracionMin) break;
            cursor = Math.max(cursor, t.fin);
        }
        if (cursor + duracionMin > limite) return Optional.empty();
        return Optional.of(new LocalTime[]{LocalTime.ofSecondOfDay(cursor * 60L),
                                           LocalTime.ofSecondOfDay((cursor + duracionMin) * 60L)});
    }

    // ---------- Internos ----------

    private static WO_KI_NB_JI_Tramo WO_KI_NB_JI_tramoDe(WO_KI_NB_JI_Evento e) {
        try {
            int i = e.WO_KI_NB_JI_getInicio().toSecondOfDay() / 60, f = e.WO_KI_NB_JI_getFin().toSecondOfDay() / 60;
            return f > i ? new WO_KI_NB_JI_Tramo(i, f) : null;
        } catch (DateTimeParseException ex) { return null; }
    }

    private static List<WO_KI_NB_JI_Tramo> WO_KI_NB_JI_tramosDelDia(LocalDate dia, Collection<WO_KI_NB_JI_Evento> eventos) {
        List<WO_KI_NB_JI_Tramo> r = new ArrayList<>();
        for (WO_KI_NB_JI_Evento e : eventos) {
            if (!dia.equals(e.WO_KI_NB_JI_getFecha())) continue;
            WO_KI_NB_JI_Tramo t = WO_KI_NB_JI_tramoDe(e);
            if (t != null) r.add(t);
        }
        return r;
    }

    private static List<WO_KI_NB_JI_Tramo> WO_KI_NB_JI_unir(List<WO_KI_NB_JI_Tramo> in) {
        List<WO_KI_NB_JI_Tramo> s = new ArrayList<>(in);
        s.sort(Comparator.comparingInt(WO_KI_NB_JI_Tramo::ini));
        List<WO_KI_NB_JI_Tramo> out = new ArrayList<>();
        for (WO_KI_NB_JI_Tramo t : s) {
            if (!out.isEmpty() && t.ini <= out.getLast().fin) {
                WO_KI_NB_JI_Tramo l = out.removeLast();
                out.add(new WO_KI_NB_JI_Tramo(l.ini, Math.max(l.fin, t.fin)));
            } else out.add(t);
        }
        return out;
    }

    /** Formato "HH:mm" para mostrar horas calculadas. */
    public static String WO_KI_NB_JI_hora(LocalTime t) { return t.format(WO_KI_NB_JI_Horas.SALIDA); }
}

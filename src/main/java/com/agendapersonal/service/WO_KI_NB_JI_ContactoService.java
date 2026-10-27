package com.agendapersonal.service;

import com.agendapersonal.domain.model.WO_KI_NB_JI_Contacto;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalLong;

/** Reglas del CRM personal: último contacto, seguimiento y cumpleaños (offline, sin BD). */
public final class WO_KI_NB_JI_ContactoService {
    private WO_KI_NB_JI_ContactoService() {}

    /** Contacto que lleva tiempo sin saber de ti. {@code dias == null} = nunca contactado. */
    public record WO_KI_NB_JI_Seguimiento(WO_KI_NB_JI_Contacto contacto, Long dias) {}

    /** Cumpleaños próximo. {@code enDias == 0} significa hoy. */
    public record WO_KI_NB_JI_Cumple(WO_KI_NB_JI_Contacto contacto, LocalDate fecha, int enDias, int edad) {}

    public static void WO_KI_NB_JI_marcarContactadoHoy(WO_KI_NB_JI_Contacto c, LocalDate hoy) {
        c.WO_KI_NB_JI_setUltimoContacto(hoy.toString());
    }

    public static OptionalLong WO_KI_NB_JI_diasSinContacto(WO_KI_NB_JI_Contacto c, LocalDate hoy) {
        LocalDate f = WO_KI_NB_JI_fecha(c.WO_KI_NB_JI_getUltimoContacto());
        return f == null ? OptionalLong.empty() : OptionalLong.of(ChronoUnit.DAYS.between(f, hoy));
    }

    /** Contactos con más de {@code umbralDias} sin contacto (o nunca), los más olvidados primero. */
    public static List<WO_KI_NB_JI_Seguimiento> WO_KI_NB_JI_requierenSeguimiento(Collection<WO_KI_NB_JI_Contacto> todos, LocalDate hoy, int umbralDias) {
        List<WO_KI_NB_JI_Seguimiento> r = new ArrayList<>();
        for (WO_KI_NB_JI_Contacto c : todos) {
            OptionalLong d = WO_KI_NB_JI_diasSinContacto(c, hoy);
            if (d.isEmpty()) r.add(new WO_KI_NB_JI_Seguimiento(c, null));
            else if (d.getAsLong() > umbralDias) r.add(new WO_KI_NB_JI_Seguimiento(c, d.getAsLong()));
        }
        r.sort(Comparator.comparingLong((WO_KI_NB_JI_Seguimiento s) -> s.dias() == null ? Long.MAX_VALUE : s.dias()).reversed());
        return r;
    }

    /** Cumpleaños dentro de los próximos {@code ventanaDias} días (incluye hoy), en orden. */
    public static List<WO_KI_NB_JI_Cumple> WO_KI_NB_JI_proximosCumpleanos(Collection<WO_KI_NB_JI_Contacto> todos, LocalDate hoy, int ventanaDias) {
        List<WO_KI_NB_JI_Cumple> r = new ArrayList<>();
        for (WO_KI_NB_JI_Contacto c : todos) {
            LocalDate nac = WO_KI_NB_JI_fecha(c.WO_KI_NB_JI_getCumpleanos());
            if (nac == null) continue;
            MonthDay md = MonthDay.from(nac);
            LocalDate prox = md.atYear(hoy.getYear());
            if (prox.isBefore(hoy)) prox = md.atYear(hoy.getYear() + 1);
            int en = (int) ChronoUnit.DAYS.between(hoy, prox);
            if (en <= ventanaDias) r.add(new WO_KI_NB_JI_Cumple(c, prox, en, prox.getYear() - nac.getYear()));
        }
        r.sort(Comparator.comparingInt(WO_KI_NB_JI_Cumple::enDias));
        return r;
    }

    private static LocalDate WO_KI_NB_JI_fecha(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim()); } catch (DateTimeParseException e) { return null; }
    }
}

package com.agendapersonal.service;

import com.agendapersonal.domain.model.wkniContacto;
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
public final class wkniContactoService {
    private wkniContactoService() {}

    /** Contacto que lleva tiempo sin saber de ti. {@code dias == null} = nunca contactado. */
    public record wkniSeguimiento(wkniContacto wkni_contacto, Long wkniDias) {}

    /** Cumpleaños próximo. {@code enDias == 0} significa hoy. */
    public record wkniCumple(wkniContacto wkni_contacto, LocalDate wkni_fecha, int wkniEnDias, int wkniEdad) {}

    public static void wkniMarcarContactadoHoy(wkniContacto wkniC, LocalDate wkniHoy) {
        wkniC.wkniSetUltimoContacto(wkniHoy.toString());
    }

    public static OptionalLong wkniDiasSinContacto(wkniContacto wkniC, LocalDate wkniHoy) {
        LocalDate wkniF = wkniFecha(wkniC.wkniGetUltimoContacto());
        return wkniF == null ? OptionalLong.empty() : OptionalLong.of(ChronoUnit.DAYS.between(wkniF, wkniHoy));
    }

    /** Contactos con más de {@code umbralDias} sin contacto (o nunca), los más olvidados primero. */
    public static List<wkniSeguimiento> wkniRequierenSeguimiento(Collection<wkniContacto> wkniTodos, LocalDate wkniHoy, int wkniUmbralDias) {
        List<wkniSeguimiento> wkniR = new ArrayList<>();
        for (wkniContacto wkniC : wkniTodos) {
            OptionalLong wkniD = wkniDiasSinContacto(wkniC, wkniHoy);
            if (wkniD.isEmpty()) wkniR.add(new wkniSeguimiento(wkniC, null));
            else if (wkniD.getAsLong() > wkniUmbralDias) wkniR.add(new wkniSeguimiento(wkniC, wkniD.getAsLong()));
        }
        wkniR.sort(Comparator.comparingLong((wkniSeguimiento wkniS) -> wkniS.wkniDias() == null ? Long.MAX_VALUE : wkniS.wkniDias()).reversed());
        return wkniR;
    }

    /** Cumpleaños dentro de los próximos {@code ventanaDias} días (incluye hoy), en orden. */
    public static List<wkniCumple> wkniProximosCumpleanos(Collection<wkniContacto> wkniTodos, LocalDate wkniHoy, int wkniVentanaDias) {
        List<wkniCumple> wkniR = new ArrayList<>();
        for (wkniContacto wkniC : wkniTodos) {
            LocalDate wkniNac = wkniFecha(wkniC.wkniGetCumpleanos());
            if (wkniNac == null) continue;
            MonthDay wkniMd = MonthDay.from(wkniNac);
            LocalDate wkniProx = wkniMd.atYear(wkniHoy.getYear());
            if (wkniProx.isBefore(wkniHoy)) wkniProx = wkniMd.atYear(wkniHoy.getYear() + 1);
            int wkniEn = (int) ChronoUnit.DAYS.between(wkniHoy, wkniProx);
            if (wkniEn <= wkniVentanaDias) wkniR.add(new wkniCumple(wkniC, wkniProx, wkniEn, wkniProx.getYear() - wkniNac.getYear()));
        }
        wkniR.sort(Comparator.comparingInt(wkniCumple::wkniEnDias));
        return wkniR;
    }

    private static LocalDate wkniFecha(String wkniS) {
        if (wkniS == null || wkniS.isBlank()) return null;
        try { return LocalDate.parse(wkniS.trim()); } catch (DateTimeParseException wkniE) { return null; }
    }
}

package com.agendapersonal.service;

import com.agendapersonal.domain.model.wkniHabito;
import com.agendapersonal.model.wkniEvento;
import com.agendapersonal.model.wkniTarea;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Qué hay que recordarle al usuario y cuándo. Lógica pura (sin UI ni BD) y con pruebas.
 * Dos usos: (1) resumen al abrir el programa, (2) avisos en vivo mientras corre en segundo plano.
 */
public final class wkniRecordatorioService {
    private wkniRecordatorioService() {}

    /** Mensaje listo para mostrar. {@code clave} es única por aviso para no repetirlo. */
    public record wkniAviso(String wkniClave, wkniTipoEntrada wkni_tipo, String wkniTitulo, String wkniMensaje) {}

    /** Noticias del día para la ventana de bienvenida. */
    public record wkniResumen(LocalDate wkniHoy,
                          List<wkniContactoService.wkniCumple> wkniCumples,     // hoy y próximos días (ordenados)
                          List<wkniEvento> wkniEventosHoy, List<wkniEvento> wkniEventosManana,
                          List<wkniTarea> wkniTareasHoy, List<wkniTarea> wkniTareasVencidas,
                          List<wkniHabito> wkniHabitosPendientes) {
        /** Habilita la ventana emergente: cumpleaños cercano, evento hoy/mañana o tareas con fecha. */
        public boolean wkniHayNoticias() {
            return !wkniCumples.isEmpty() || !wkniEventosHoy.isEmpty() || !wkniEventosManana.isEmpty()
                || !wkniTareasHoy.isEmpty() || !wkniTareasVencidas.isEmpty();
        }
    }

    public static wkniResumen wkniResumenInicio(LocalDate wkniHoy, wkniCalendarioUnificadoService.wkniDatos wkniD, int wkniVentanaCumples) {
        var wkniCumples = wkniContactoService.wkniProximosCumpleanos(wkniD.wkniContactos(), wkniHoy, wkniVentanaCumples);
        List<wkniEvento> wkniEvHoy = wkniEventosDe(wkniHoy, wkniD), wkniEvMan = wkniEventosDe(wkniHoy.plusDays(1), wkniD);
        List<wkniTarea> wkniTHoy = new ArrayList<>(), wkniTVenc = new ArrayList<>();
        for (wkniTarea wkniT : wkniD.wkniTareas()) {
            if (wkniT.wkniEstaCompletada()) continue;
            if (wkniHoy.equals(wkniT.wkniGetFecha())) wkniTHoy.add(wkniT);
            else if (wkniT.wkniGetFecha() != null && wkniT.wkniGetFecha().isBefore(wkniHoy)) wkniTVenc.add(wkniT);
        }
        wkniTVenc.sort(Comparator.comparing(wkniTarea::wkniGetFecha));
        var wkniHechos = wkniD.wkniHabitosCumplidos().getOrDefault(wkniHoy.toString(), Set.of());
        List<wkniHabito> wkniHab = wkniD.wkniHabitos().stream()
            .filter(wkniH -> wkniH.wkniIsActivo() && wkniCalendarioUnificadoService.wkniAplicaHabito(wkniH, wkniHoy) && !wkniHechos.contains(wkniH.wkniGetId()))
            .toList();
        return new wkniResumen(wkniHoy, wkniCumples, wkniEvHoy, wkniEvMan, wkniTHoy, wkniTVenc, wkniHab);
    }

    private static List<wkniEvento> wkniEventosDe(LocalDate wkniDia, wkniCalendarioUnificadoService.wkniDatos wkniD) {
        return wkniD.wkniEventos().stream().filter(wkniE -> wkniDia.equals(wkniE.wkniGetFecha()))
            .sorted(Comparator.comparing(wkniE -> wkniE.wkniGetHoraInicio() == null ? "" : wkniE.wkniGetHoraInicio())).toList();
    }

    /**
     * Eventos de hoy que empiezan dentro de {@code minutosAntes} (o empezaron hace menos de 1 min)
     * y que todavía no se avisaron. No modifica {@code yaAvisados}: lo hace quien muestra el aviso.
     */
    public static List<wkniAviso> wkniAvisosEventos(LocalDateTime wkniAhora, wkniCalendarioUnificadoService.wkniDatos wkniD,
                                            int wkniMinutosAntes, Set<String> wkniYaAvisados) {
        List<wkniAviso> wkniR = new ArrayList<>();
        for (wkniEvento wkniE : wkniD.wkniEventos()) {
            if (!wkniAhora.toLocalDate().equals(wkniE.wkniGetFecha())) continue;
            LocalDateTime wkniInicio;
            try { wkniInicio = wkniE.wkniGetFecha().atTime(wkniE.wkniGetInicio()); } catch (DateTimeParseException wkniEx) { continue; }
            long wkniFaltan = Duration.between(wkniAhora, wkniInicio).toMinutes();
            if (wkniInicio.isBefore(wkniAhora.minusMinutes(1)) || wkniFaltan > wkniMinutosAntes) continue;
            String wkniClave = "ev:" + wkniE.wkniGetId() + ":" + wkniE.wkniGetFecha() + ":" + wkniE.wkniGetHoraInicio();
            if (wkniYaAvisados.contains(wkniClave)) continue;
            wkniTipoEntrada wkniT = wkniCalendarioUnificadoService.wkniEsTrabajo(wkniE) ? wkniTipoEntrada.TRABAJO : wkniTipoEntrada.EVENTO;
            String wkniCuando = wkniFaltan <= 0 ? "Empieza ahora" : "Empieza en " + wkniFaltan + " min";
            wkniR.add(new wkniAviso(wkniClave, wkniT, wkniE.wkniGetTitulo(), wkniCuando + " · " + wkniE.wkniGetHoraInicio() + "–" + wkniE.wkniGetHoraFin()));
        }
        wkniR.sort(Comparator.comparing(wkniAviso::wkniClave));
        return wkniR;
    }

    /** Cumpleaños de hoy aún no avisados (se usa cuando el programa sigue abierto al cambiar el día). */
    public static List<wkniAviso> wkniAvisosCumpleHoy(LocalDate wkniHoy, wkniCalendarioUnificadoService.wkniDatos wkniD, Set<String> wkniYaAvisados) {
        List<wkniAviso> wkniR = new ArrayList<>();
        for (var wkniC : wkniContactoService.wkniProximosCumpleanos(wkniD.wkniContactos(), wkniHoy, 0)) {
            String wkniClave = "cumple:" + wkniC.wkni_contacto().wkniGetId() + ":" + wkniHoy;
            if (wkniYaAvisados.contains(wkniClave)) continue;
            wkniR.add(new wkniAviso(wkniClave, wkniTipoEntrada.CUMPLE, "Hoy cumple " + wkniC.wkni_contacto().wkniGetNombre(),
                wkniC.wkniEdad() > 0 ? "Cumple " + wkniC.wkniEdad() + " años. ¡No olvides saludarle!" : "¡No olvides saludarle!"));
        }
        return wkniR;
    }

    /** Aviso único con el resumen del día (para cuando cambia el día y el programa sigue en segundo plano). */
    public static wkniAviso wkniAvisoResumen(wkniResumen wkniS) {
        int wkniCumplesHoy = (int) wkniS.wkniCumples().stream().filter(wkniC -> wkniC.wkniEnDias() == 0).count();
        List<String> wkniPartes = new ArrayList<>();
        if (wkniCumplesHoy > 0) wkniPartes.add(wkniCumplesHoy + " cumpleaños");
        if (!wkniS.wkniEventosHoy().isEmpty()) wkniPartes.add(wkniS.wkniEventosHoy().size() + (wkniS.wkniEventosHoy().size() == 1 ? " evento" : " eventos"));
        int wkniTareas = wkniS.wkniTareasHoy().size() + wkniS.wkniTareasVencidas().size();
        if (wkniTareas > 0) wkniPartes.add(wkniTareas + (wkniTareas == 1 ? " tarea" : " tareas"));
        String wkniMsg = wkniPartes.isEmpty() ? "Sin pendientes para hoy." : "Hoy: " + String.join(", ", wkniPartes) + ".";
        return new wkniAviso("resumen:" + wkniS.wkniHoy(), wkniTipoEntrada.EVENTO, "Resumen del día", wkniMsg);
    }
}

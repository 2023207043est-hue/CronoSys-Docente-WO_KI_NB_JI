package com.agendapersonal.service;

import com.agendapersonal.domain.model.WO_KI_NB_JI_Habito;
import com.agendapersonal.model.WO_KI_NB_JI_Evento;
import com.agendapersonal.model.WO_KI_NB_JI_Tarea;
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
public final class WO_KI_NB_JI_RecordatorioService {
    private WO_KI_NB_JI_RecordatorioService() {}

    /** Mensaje listo para mostrar. {@code clave} es única por aviso para no repetirlo. */
    public record WO_KI_NB_JI_Aviso(String clave, WO_KI_NB_JI_TipoEntrada tipo, String titulo, String mensaje) {}

    /** Noticias del día para la ventana de bienvenida. */
    public record WO_KI_NB_JI_Resumen(LocalDate hoy,
                          List<WO_KI_NB_JI_ContactoService.WO_KI_NB_JI_Cumple> cumples,     // hoy y próximos días (ordenados)
                          List<WO_KI_NB_JI_Evento> eventosHoy, List<WO_KI_NB_JI_Evento> eventosManana,
                          List<WO_KI_NB_JI_Tarea> tareasHoy, List<WO_KI_NB_JI_Tarea> tareasVencidas,
                          List<WO_KI_NB_JI_Habito> habitosPendientes) {
        /** Habilita la ventana emergente: cumpleaños cercano, evento hoy/mañana o tareas con fecha. */
        public boolean WO_KI_NB_JI_hayNoticias() {
            return !cumples.isEmpty() || !eventosHoy.isEmpty() || !eventosManana.isEmpty()
                || !tareasHoy.isEmpty() || !tareasVencidas.isEmpty();
        }
    }

    public static WO_KI_NB_JI_Resumen WO_KI_NB_JI_resumenInicio(LocalDate hoy, WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_Datos d, int ventanaCumples) {
        var cumples = WO_KI_NB_JI_ContactoService.WO_KI_NB_JI_proximosCumpleanos(d.contactos(), hoy, ventanaCumples);
        List<WO_KI_NB_JI_Evento> evHoy = WO_KI_NB_JI_eventosDe(hoy, d), evMan = WO_KI_NB_JI_eventosDe(hoy.plusDays(1), d);
        List<WO_KI_NB_JI_Tarea> tHoy = new ArrayList<>(), tVenc = new ArrayList<>();
        for (WO_KI_NB_JI_Tarea t : d.tareas()) {
            if (t.WO_KI_NB_JI_estaCompletada()) continue;
            if (hoy.equals(t.WO_KI_NB_JI_getFecha())) tHoy.add(t);
            else if (t.WO_KI_NB_JI_getFecha() != null && t.WO_KI_NB_JI_getFecha().isBefore(hoy)) tVenc.add(t);
        }
        tVenc.sort(Comparator.comparing(WO_KI_NB_JI_Tarea::WO_KI_NB_JI_getFecha));
        var hechos = d.habitosCumplidos().getOrDefault(hoy.toString(), Set.of());
        List<WO_KI_NB_JI_Habito> hab = d.habitos().stream()
            .filter(h -> h.WO_KI_NB_JI_isActivo() && WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_aplicaHabito(h, hoy) && !hechos.contains(h.WO_KI_NB_JI_getId()))
            .toList();
        return new WO_KI_NB_JI_Resumen(hoy, cumples, evHoy, evMan, tHoy, tVenc, hab);
    }

    private static List<WO_KI_NB_JI_Evento> WO_KI_NB_JI_eventosDe(LocalDate dia, WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_Datos d) {
        return d.eventos().stream().filter(e -> dia.equals(e.WO_KI_NB_JI_getFecha()))
            .sorted(Comparator.comparing(e -> e.WO_KI_NB_JI_getHoraInicio() == null ? "" : e.WO_KI_NB_JI_getHoraInicio())).toList();
    }

    /**
     * Eventos de hoy que empiezan dentro de {@code minutosAntes} (o empezaron hace menos de 1 min)
     * y que todavía no se avisaron. No modifica {@code yaAvisados}: lo hace quien muestra el aviso.
     */
    public static List<WO_KI_NB_JI_Aviso> WO_KI_NB_JI_avisosEventos(LocalDateTime ahora, WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_Datos d,
                                            int minutosAntes, Set<String> yaAvisados) {
        List<WO_KI_NB_JI_Aviso> r = new ArrayList<>();
        for (WO_KI_NB_JI_Evento e : d.eventos()) {
            if (!ahora.toLocalDate().equals(e.WO_KI_NB_JI_getFecha())) continue;
            LocalDateTime inicio;
            try { inicio = e.WO_KI_NB_JI_getFecha().atTime(e.WO_KI_NB_JI_getInicio()); } catch (DateTimeParseException ex) { continue; }
            long faltan = Duration.between(ahora, inicio).toMinutes();
            if (inicio.isBefore(ahora.minusMinutes(1)) || faltan > minutosAntes) continue;
            String clave = "ev:" + e.WO_KI_NB_JI_getId() + ":" + e.WO_KI_NB_JI_getFecha() + ":" + e.WO_KI_NB_JI_getHoraInicio();
            if (yaAvisados.contains(clave)) continue;
            WO_KI_NB_JI_TipoEntrada t = WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_esTrabajo(e) ? WO_KI_NB_JI_TipoEntrada.TRABAJO : WO_KI_NB_JI_TipoEntrada.EVENTO;
            String cuando = faltan <= 0 ? "Empieza ahora" : "Empieza en " + faltan + " min";
            r.add(new WO_KI_NB_JI_Aviso(clave, t, e.WO_KI_NB_JI_getTitulo(), cuando + " · " + e.WO_KI_NB_JI_getHoraInicio() + "–" + e.WO_KI_NB_JI_getHoraFin()));
        }
        r.sort(Comparator.comparing(WO_KI_NB_JI_Aviso::clave));
        return r;
    }

    /** Cumpleaños de hoy aún no avisados (se usa cuando el programa sigue abierto al cambiar el día). */
    public static List<WO_KI_NB_JI_Aviso> WO_KI_NB_JI_avisosCumpleHoy(LocalDate hoy, WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_Datos d, Set<String> yaAvisados) {
        List<WO_KI_NB_JI_Aviso> r = new ArrayList<>();
        for (var c : WO_KI_NB_JI_ContactoService.WO_KI_NB_JI_proximosCumpleanos(d.contactos(), hoy, 0)) {
            String clave = "cumple:" + c.contacto().WO_KI_NB_JI_getId() + ":" + hoy;
            if (yaAvisados.contains(clave)) continue;
            r.add(new WO_KI_NB_JI_Aviso(clave, WO_KI_NB_JI_TipoEntrada.CUMPLE, "Hoy cumple " + c.contacto().WO_KI_NB_JI_getNombre(),
                c.edad() > 0 ? "Cumple " + c.edad() + " años. ¡No olvides saludarle!" : "¡No olvides saludarle!"));
        }
        return r;
    }

    /** Aviso único con el resumen del día (para cuando cambia el día y el programa sigue en segundo plano). */
    public static WO_KI_NB_JI_Aviso WO_KI_NB_JI_avisoResumen(WO_KI_NB_JI_Resumen s) {
        int cumplesHoy = (int) s.cumples().stream().filter(c -> c.enDias() == 0).count();
        List<String> partes = new ArrayList<>();
        if (cumplesHoy > 0) partes.add(cumplesHoy + " cumpleaños");
        if (!s.eventosHoy().isEmpty()) partes.add(s.eventosHoy().size() + (s.eventosHoy().size() == 1 ? " evento" : " eventos"));
        int tareas = s.tareasHoy().size() + s.tareasVencidas().size();
        if (tareas > 0) partes.add(tareas + (tareas == 1 ? " tarea" : " tareas"));
        String msg = partes.isEmpty() ? "Sin pendientes para hoy." : "Hoy: " + String.join(", ", partes) + ".";
        return new WO_KI_NB_JI_Aviso("resumen:" + s.hoy(), WO_KI_NB_JI_TipoEntrada.EVENTO, "Resumen del día", msg);
    }
}

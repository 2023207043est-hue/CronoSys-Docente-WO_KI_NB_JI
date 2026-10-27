package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.domain.model.WO_KI_NB_JI_Contacto;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Habito;
import com.agendapersonal.model.WO_KI_NB_JI_Evento;
import com.agendapersonal.model.WO_KI_NB_JI_Tarea;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WO_KI_NB_JI_RecordatorioServiceTest {
    private static final LocalDate HOY = LocalDate.of(2026, 10, 16);

    private static WO_KI_NB_JI_Evento WO_KI_NB_JI_ev(int id, String t, LocalDate f, String hi, String hf) {
        return new WO_KI_NB_JI_Evento(id, t, f, hi, hf, "", "Personal", "Media");
    }

    private static WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_Datos WO_KI_NB_JI_datos(List<WO_KI_NB_JI_Evento> e, List<WO_KI_NB_JI_Tarea> t, List<WO_KI_NB_JI_Contacto> c) {
        return new WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_Datos(e, t, List.of(new WO_KI_NB_JI_Habito(1, "Leer", "", "DIARIO", 1, "#0", true)), c, Map.of());
    }

    @Test void WO_KI_NB_JI_sinNadaNoHayNoticias() {
        var r = WO_KI_NB_JI_RecordatorioService.WO_KI_NB_JI_resumenInicio(HOY, WO_KI_NB_JI_datos(List.of(), List.of(), List.of()), 7);
        assertFalse(r.WO_KI_NB_JI_hayNoticias());
        assertEquals(1, r.habitosPendientes().size());   // un hábito pendiente solo NO abre la ventana
    }

    @Test void WO_KI_NB_JI_cumpleanosDeHoyYProximosAbrenLaVentana() {
        var c = List.of(new WO_KI_NB_JI_Contacto(1, "Ana", "", "", "", "2000-10-16", "", false, null),
                        new WO_KI_NB_JI_Contacto(2, "Luis", "", "", "", "1990-10-20", "", false, null),
                        new WO_KI_NB_JI_Contacto(3, "Lejano", "", "", "", "1990-03-01", "", false, null));
        var r = WO_KI_NB_JI_RecordatorioService.WO_KI_NB_JI_resumenInicio(HOY, WO_KI_NB_JI_datos(List.of(), List.of(), c), 7);
        assertTrue(r.WO_KI_NB_JI_hayNoticias());
        assertEquals(List.of("Ana", "Luis"), r.cumples().stream().map(k -> k.contacto().WO_KI_NB_JI_getNombre()).toList());
    }

    @Test void WO_KI_NB_JI_eventosHoyManianaYTareasVencidas() {
        var e = List.of(WO_KI_NB_JI_ev(1, "Hoy", HOY, "09:00", "10:00"), WO_KI_NB_JI_ev(2, "Mañana", HOY.plusDays(1), "08:00", "09:00"),
                        WO_KI_NB_JI_ev(3, "Lejos", HOY.plusDays(5), "08:00", "09:00"));
        var t = List.of(new WO_KI_NB_JI_Tarea(1, "Vieja", "", "Pendiente", HOY.minusDays(3), "Alta"),
                        new WO_KI_NB_JI_Tarea(2, "Hecha", "", "Completada", HOY.minusDays(3), "Alta"),
                        new WO_KI_NB_JI_Tarea(3, "Hoy", "", "Pendiente", HOY, "Media"));
        var r = WO_KI_NB_JI_RecordatorioService.WO_KI_NB_JI_resumenInicio(HOY, WO_KI_NB_JI_datos(e, t, List.of()), 7);
        assertEquals(1, r.eventosHoy().size());
        assertEquals(1, r.eventosManana().size());
        assertEquals(1, r.tareasVencidas().size());   // la completada no cuenta
        assertEquals(1, r.tareasHoy().size());
    }

    @Test void WO_KI_NB_JI_avisaSoloDentroDeLaVentanaYUnaVez() {
        var d = WO_KI_NB_JI_datos(List.of(WO_KI_NB_JI_ev(1, "Dentista", HOY, "10:00", "11:00")), List.of(), List.of());
        Set<String> avisados = new HashSet<>();
        assertTrue(WO_KI_NB_JI_RecordatorioService.WO_KI_NB_JI_avisosEventos(HOY.atTime(9, 30), d, 15, avisados).isEmpty());   // faltan 30
        var a = WO_KI_NB_JI_RecordatorioService.WO_KI_NB_JI_avisosEventos(HOY.atTime(9, 50), d, 15, avisados);
        assertEquals(1, a.size());
        assertTrue(a.getFirst().mensaje().startsWith("Empieza en 10 min"));
        avisados.add(a.getFirst().clave());
        assertTrue(WO_KI_NB_JI_RecordatorioService.WO_KI_NB_JI_avisosEventos(HOY.atTime(9, 52), d, 15, avisados).isEmpty());  // ya avisado
        assertFalse(WO_KI_NB_JI_RecordatorioService.WO_KI_NB_JI_avisosEventos(HOY.atTime(10, 0), d, 15, new HashSet<>()).isEmpty()); // "ahora"
        assertTrue(WO_KI_NB_JI_RecordatorioService.WO_KI_NB_JI_avisosEventos(HOY.atTime(10, 5), d, 15, new HashSet<>()).isEmpty());  // ya pasó
    }

    @Test void WO_KI_NB_JI_avisoDeCumpleHoyNoSeRepite() {
        var d = WO_KI_NB_JI_datos(List.of(), List.of(), List.of(new WO_KI_NB_JI_Contacto(7, "Ana", "", "", "", "2000-10-16", "", false, null)));
        Set<String> av = new HashSet<>();
        var a = WO_KI_NB_JI_RecordatorioService.WO_KI_NB_JI_avisosCumpleHoy(HOY, d, av);
        assertEquals(1, a.size());
        av.add(a.getFirst().clave());
        assertTrue(WO_KI_NB_JI_RecordatorioService.WO_KI_NB_JI_avisosCumpleHoy(HOY, d, av).isEmpty());
    }
}

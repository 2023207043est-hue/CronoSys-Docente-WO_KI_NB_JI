package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.model.WO_KI_NB_JI_Evento;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class WO_KI_NB_JI_AgendaServiceTest {
    private static final LocalDate DIA = LocalDate.of(2026, 10, 20);

    private static WO_KI_NB_JI_Evento WO_KI_NB_JI_ev(int id, String ini, String fin) {
        return new WO_KI_NB_JI_Evento(id, "E" + id, DIA, ini, fin, "", "Personal", "Media");
    }

    @Test void WO_KI_NB_JI_duracionExacta() {
        assertEquals(90, WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_duracionMinutos(WO_KI_NB_JI_ev(1, "09:00", "10:30")));
        assertEquals(45, WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_duracionMinutos(WO_KI_NB_JI_ev(1, "9:15", "10:00")));
        assertEquals("1 h 30 min", WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_formatearMinutos(90));
        assertEquals("45 min", WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_formatearMinutos(45));
        assertEquals("2 h", WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_formatearMinutos(120));
    }

    @Test void WO_KI_NB_JI_validacion() {
        assertTrue(WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_validar(WO_KI_NB_JI_ev(0, "09:00", "10:00")).isEmpty());
        assertTrue(WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_validar(WO_KI_NB_JI_ev(0, "10:00", "09:00")).isPresent());   // fin antes que inicio
        assertTrue(WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_validar(WO_KI_NB_JI_ev(0, "10:00", "10:00")).isPresent());   // duración cero
        assertTrue(WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_validar(WO_KI_NB_JI_ev(0, "25:00", "26:00")).isPresent());   // hora imposible
        assertTrue(WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_validar(WO_KI_NB_JI_ev(0, "9.30", "10:00")).isPresent());    // formato incorrecto
        assertTrue(WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_validar(WO_KI_NB_JI_ev(0, "", "10:00")).isPresent());
        WO_KI_NB_JI_Evento sinFecha = new WO_KI_NB_JI_Evento(0, "X", null, "09:00", "10:00", "", "Personal", "Media");
        assertTrue(WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_validar(sinFecha).isPresent());
    }

    @Test void WO_KI_NB_JI_cruceParcialSeCalculaEnMinutos() {
        var existentes = List.of(WO_KI_NB_JI_ev(1, "10:00", "11:00"));
        var c = WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_detectarConflictos(WO_KI_NB_JI_ev(0, "10:30", "11:30"), existentes);
        assertEquals(1, c.size());
        assertEquals(30, c.getFirst().minutosSolapados());   // no basta comparar la hora de inicio
    }

    @Test void WO_KI_NB_JI_eventoContenidoYContenedor() {
        var existentes = List.of(WO_KI_NB_JI_ev(1, "09:00", "12:00"));
        assertEquals(60, WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_detectarConflictos(WO_KI_NB_JI_ev(0, "10:00", "11:00"), existentes).getFirst().minutosSolapados());
        var interno = List.of(WO_KI_NB_JI_ev(1, "10:00", "11:00"));
        assertEquals(60, WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_detectarConflictos(WO_KI_NB_JI_ev(0, "09:00", "12:00"), interno).getFirst().minutosSolapados());
    }

    @Test void WO_KI_NB_JI_bordesQueSoloSeTocanNoSeCruzan() {
        var existentes = List.of(WO_KI_NB_JI_ev(1, "10:00", "11:00"));
        assertTrue(WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_detectarConflictos(WO_KI_NB_JI_ev(0, "11:00", "12:00"), existentes).isEmpty());
        assertTrue(WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_detectarConflictos(WO_KI_NB_JI_ev(0, "09:00", "10:00"), existentes).isEmpty());
    }

    @Test void WO_KI_NB_JI_otroDiaNoCuenta() {
        WO_KI_NB_JI_Evento otro = new WO_KI_NB_JI_Evento(1, "Otro", DIA.plusDays(1), "10:00", "11:00", "", "Personal", "Media");
        assertTrue(WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_detectarConflictos(WO_KI_NB_JI_ev(0, "10:00", "11:00"), List.of(otro)).isEmpty());
    }

    @Test void WO_KI_NB_JI_editarUnEventoNoChocaConsigoMismo() {
        var existentes = List.of(WO_KI_NB_JI_ev(7, "10:00", "11:00"));
        assertTrue(WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_detectarConflictos(WO_KI_NB_JI_ev(7, "10:15", "11:15"), existentes).isEmpty());
    }

    @Test void WO_KI_NB_JI_datosCorruptosNoRompenLaDeteccion() {
        var existentes = List.of(WO_KI_NB_JI_ev(1, "basura", "??"), WO_KI_NB_JI_ev(2, "10:00", "11:00"));
        assertEquals(1, WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_detectarConflictos(WO_KI_NB_JI_ev(0, "10:30", "10:45"), existentes).size());
    }

    @Test void WO_KI_NB_JI_minutosOcupadosNoDuplicaSolapes() {
        var evs = List.of(WO_KI_NB_JI_ev(1, "09:00", "10:00"), WO_KI_NB_JI_ev(2, "09:30", "10:30"), WO_KI_NB_JI_ev(3, "14:00", "15:00"));
        assertEquals(150, WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_minutosOcupados(DIA, evs));   // 9:00–10:30 + 14:00–15:00
    }

    @Test void WO_KI_NB_JI_sugiereElPrimerHuecoLibre() {
        var evs = List.of(WO_KI_NB_JI_ev(1, "07:00", "09:00"), WO_KI_NB_JI_ev(2, "09:30", "10:00"), WO_KI_NB_JI_ev(3, "10:00", "12:00"));
        var h = WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_sugerirHueco(DIA, evs, 60, LocalTime.of(7, 0), LocalTime.of(22, 0)).orElseThrow();
        assertEquals(LocalTime.of(12, 0), h[0]);      // 9:00–9:30 es muy corto para 60 min
        assertEquals(LocalTime.of(13, 0), h[1]);
        var corto = WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_sugerirHueco(DIA, evs, 30, LocalTime.of(7, 0), LocalTime.of(22, 0)).orElseThrow();
        assertEquals(LocalTime.of(9, 0), corto[0]);   // sí cabe 30 min entre 9:00 y 9:30
    }

    @Test void WO_KI_NB_JI_sinHuecoCuandoElDiaEstaLleno() {
        var evs = List.of(WO_KI_NB_JI_ev(1, "08:00", "20:00"));
        assertTrue(WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_sugerirHueco(DIA, evs, 60, LocalTime.of(8, 0), LocalTime.of(20, 0)).isEmpty());
    }
}

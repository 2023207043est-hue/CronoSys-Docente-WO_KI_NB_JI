package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.model.wkniEvento;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class wkniAgendaServiceTest {
    private static final LocalDate DIA = LocalDate.of(2026, 10, 20);

    private static wkniEvento wkniEv(int id, String ini, String fin) {
        return new wkniEvento(id, "E" + id, DIA, ini, fin, "", "Personal", "Media");
    }

    @Test void wkniDuracionExacta() {
        assertEquals(90, wkniAgendaService.wkniDuracionMinutos(wkniEv(1, "09:00", "10:30")));
        assertEquals(45, wkniAgendaService.wkniDuracionMinutos(wkniEv(1, "9:15", "10:00")));
        assertEquals("1 h 30 min", wkniAgendaService.wkniFormatearMinutos(90));
        assertEquals("45 min", wkniAgendaService.wkniFormatearMinutos(45));
        assertEquals("2 h", wkniAgendaService.wkniFormatearMinutos(120));
    }

    @Test void wkniValidacion() {
        assertTrue(wkniAgendaService.wkniValidar(wkniEv(0, "09:00", "10:00")).isEmpty());
        assertTrue(wkniAgendaService.wkniValidar(wkniEv(0, "10:00", "09:00")).isPresent());   // fin antes que inicio
        assertTrue(wkniAgendaService.wkniValidar(wkniEv(0, "10:00", "10:00")).isPresent());   // duración cero
        assertTrue(wkniAgendaService.wkniValidar(wkniEv(0, "25:00", "26:00")).isPresent());   // hora imposible
        assertTrue(wkniAgendaService.wkniValidar(wkniEv(0, "9.30", "10:00")).isPresent());    // formato incorrecto
        assertTrue(wkniAgendaService.wkniValidar(wkniEv(0, "", "10:00")).isPresent());
        wkniEvento sinFecha = new wkniEvento(0, "X", null, "09:00", "10:00", "", "Personal", "Media");
        assertTrue(wkniAgendaService.wkniValidar(sinFecha).isPresent());
    }

    @Test void wkniCruceParcialSeCalculaEnMinutos() {
        var existentes = List.of(wkniEv(1, "10:00", "11:00"));
        var c = wkniAgendaService.wkniDetectarConflictos(wkniEv(0, "10:30", "11:30"), existentes);
        assertEquals(1, c.size());
        assertEquals(30, c.getFirst().wkniMinutosSolapados());   // no basta comparar la hora de inicio
    }

    @Test void wkniEventoContenidoYContenedor() {
        var existentes = List.of(wkniEv(1, "09:00", "12:00"));
        assertEquals(60, wkniAgendaService.wkniDetectarConflictos(wkniEv(0, "10:00", "11:00"), existentes).getFirst().wkniMinutosSolapados());
        var interno = List.of(wkniEv(1, "10:00", "11:00"));
        assertEquals(60, wkniAgendaService.wkniDetectarConflictos(wkniEv(0, "09:00", "12:00"), interno).getFirst().wkniMinutosSolapados());
    }

    @Test void wkniBordesQueSoloSeTocanNoSeCruzan() {
        var existentes = List.of(wkniEv(1, "10:00", "11:00"));
        assertTrue(wkniAgendaService.wkniDetectarConflictos(wkniEv(0, "11:00", "12:00"), existentes).isEmpty());
        assertTrue(wkniAgendaService.wkniDetectarConflictos(wkniEv(0, "09:00", "10:00"), existentes).isEmpty());
    }

    @Test void wkniOtroDiaNoCuenta() {
        wkniEvento otro = new wkniEvento(1, "Otro", DIA.plusDays(1), "10:00", "11:00", "", "Personal", "Media");
        assertTrue(wkniAgendaService.wkniDetectarConflictos(wkniEv(0, "10:00", "11:00"), List.of(otro)).isEmpty());
    }

    @Test void wkniEditarUnEventoNoChocaConsigoMismo() {
        var existentes = List.of(wkniEv(7, "10:00", "11:00"));
        assertTrue(wkniAgendaService.wkniDetectarConflictos(wkniEv(7, "10:15", "11:15"), existentes).isEmpty());
    }

    @Test void wkniDatosCorruptosNoRompenLaDeteccion() {
        var existentes = List.of(wkniEv(1, "basura", "??"), wkniEv(2, "10:00", "11:00"));
        assertEquals(1, wkniAgendaService.wkniDetectarConflictos(wkniEv(0, "10:30", "10:45"), existentes).size());
    }

    @Test void wkniMinutosOcupadosNoDuplicaSolapes() {
        var evs = List.of(wkniEv(1, "09:00", "10:00"), wkniEv(2, "09:30", "10:30"), wkniEv(3, "14:00", "15:00"));
        assertEquals(150, wkniAgendaService.wkniMinutosOcupados(DIA, evs));   // 9:00–10:30 + 14:00–15:00
    }

    @Test void wkniSugiereElPrimerHuecoLibre() {
        var evs = List.of(wkniEv(1, "07:00", "09:00"), wkniEv(2, "09:30", "10:00"), wkniEv(3, "10:00", "12:00"));
        var h = wkniAgendaService.wkniSugerirHueco(DIA, evs, 60, LocalTime.of(7, 0), LocalTime.of(22, 0)).orElseThrow();
        assertEquals(LocalTime.of(12, 0), h[0]);      // 9:00–9:30 es muy corto para 60 min
        assertEquals(LocalTime.of(13, 0), h[1]);
        var corto = wkniAgendaService.wkniSugerirHueco(DIA, evs, 30, LocalTime.of(7, 0), LocalTime.of(22, 0)).orElseThrow();
        assertEquals(LocalTime.of(9, 0), corto[0]);   // sí cabe 30 min entre 9:00 y 9:30
    }

    @Test void wkniSinHuecoCuandoElDiaEstaLleno() {
        var evs = List.of(wkniEv(1, "08:00", "20:00"));
        assertTrue(wkniAgendaService.wkniSugerirHueco(DIA, evs, 60, LocalTime.of(8, 0), LocalTime.of(20, 0)).isEmpty());
    }
}

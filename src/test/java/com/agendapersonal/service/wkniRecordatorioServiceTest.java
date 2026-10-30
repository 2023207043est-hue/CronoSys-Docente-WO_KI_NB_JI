package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.domain.model.wkniContacto;
import com.agendapersonal.domain.model.wkniHabito;
import com.agendapersonal.model.wkniEvento;
import com.agendapersonal.model.wkniTarea;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class wkniRecordatorioServiceTest {
    private static final LocalDate HOY = LocalDate.of(2026, 10, 16);

    private static wkniEvento wkniEv(int id, String t, LocalDate f, String hi, String hf) {
        return new wkniEvento(id, t, f, hi, hf, "", "Personal", "Media");
    }

    private static wkniCalendarioUnificadoService.wkniDatos wkniDatos(List<wkniEvento> e, List<wkniTarea> t, List<wkniContacto> c) {
        return new wkniCalendarioUnificadoService.wkniDatos(e, t, List.of(new wkniHabito(1, "Leer", "", "DIARIO", 1, "#0", true)), c, Map.of());
    }

    @Test void wkniSinNadaNoHayNoticias() {
        var r = wkniRecordatorioService.wkniResumenInicio(HOY, wkniDatos(List.of(), List.of(), List.of()), 7);
        assertFalse(r.wkniHayNoticias());
        assertEquals(1, r.wkniHabitosPendientes().size());   // un hábito pendiente solo NO abre la ventana
    }

    @Test void wkniCumpleanosDeHoyYProximosAbrenLaVentana() {
        var c = List.of(new wkniContacto(1, "Ana", "", "", "", "2000-10-16", "", false, null),
                        new wkniContacto(2, "Luis", "", "", "", "1990-10-20", "", false, null),
                        new wkniContacto(3, "Lejano", "", "", "", "1990-03-01", "", false, null));
        var r = wkniRecordatorioService.wkniResumenInicio(HOY, wkniDatos(List.of(), List.of(), c), 7);
        assertTrue(r.wkniHayNoticias());
        assertEquals(List.of("Ana", "Luis"), r.wkniCumples().stream().map(k -> k.wkni_contacto().wkniGetNombre()).toList());
    }

    @Test void wkniEventosHoyManianaYTareasVencidas() {
        var e = List.of(wkniEv(1, "Hoy", HOY, "09:00", "10:00"), wkniEv(2, "Mañana", HOY.plusDays(1), "08:00", "09:00"),
                        wkniEv(3, "Lejos", HOY.plusDays(5), "08:00", "09:00"));
        var t = List.of(new wkniTarea(1, "Vieja", "", "Pendiente", HOY.minusDays(3), "Alta"),
                        new wkniTarea(2, "Hecha", "", "Completada", HOY.minusDays(3), "Alta"),
                        new wkniTarea(3, "Hoy", "", "Pendiente", HOY, "Media"));
        var r = wkniRecordatorioService.wkniResumenInicio(HOY, wkniDatos(e, t, List.of()), 7);
        assertEquals(1, r.wkniEventosHoy().size());
        assertEquals(1, r.wkniEventosManana().size());
        assertEquals(1, r.wkniTareasVencidas().size());   // la completada no cuenta
        assertEquals(1, r.wkniTareasHoy().size());
    }

    @Test void wkniAvisaSoloDentroDeLaVentanaYUnaVez() {
        var d = wkniDatos(List.of(wkniEv(1, "Dentista", HOY, "10:00", "11:00")), List.of(), List.of());
        Set<String> avisados = new HashSet<>();
        assertTrue(wkniRecordatorioService.wkniAvisosEventos(HOY.atTime(9, 30), d, 15, avisados).isEmpty());   // faltan 30
        var a = wkniRecordatorioService.wkniAvisosEventos(HOY.atTime(9, 50), d, 15, avisados);
        assertEquals(1, a.size());
        assertTrue(a.getFirst().wkniMensaje().startsWith("Empieza en 10 min"));
        avisados.add(a.getFirst().wkniClave());
        assertTrue(wkniRecordatorioService.wkniAvisosEventos(HOY.atTime(9, 52), d, 15, avisados).isEmpty());  // ya avisado
        assertFalse(wkniRecordatorioService.wkniAvisosEventos(HOY.atTime(10, 0), d, 15, new HashSet<>()).isEmpty()); // "ahora"
        assertTrue(wkniRecordatorioService.wkniAvisosEventos(HOY.atTime(10, 5), d, 15, new HashSet<>()).isEmpty());  // ya pasó
    }

    @Test void wkniAvisoDeCumpleHoyNoSeRepite() {
        var d = wkniDatos(List.of(), List.of(), List.of(new wkniContacto(7, "Ana", "", "", "", "2000-10-16", "", false, null)));
        Set<String> av = new HashSet<>();
        var a = wkniRecordatorioService.wkniAvisosCumpleHoy(HOY, d, av);
        assertEquals(1, a.size());
        av.add(a.getFirst().wkniClave());
        assertTrue(wkniRecordatorioService.wkniAvisosCumpleHoy(HOY, d, av).isEmpty());
    }
}

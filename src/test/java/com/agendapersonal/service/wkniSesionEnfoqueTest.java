package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class wkniSesionEnfoqueTest {
    @Test void wkniPomodoroCuatroSesionesYDescansoLargo() {
        wkniSesionEnfoque s = new wkniSesionEnfoque(wkniMetodoEnfoque.wkniPREDEFINIDOS.get(0));
        s.wkniIniciar();
        for (int i = 1; i <= 3; i++) {
            assertEquals(i, s.wkniSesion());
            assertTrue(s.wkniTick(25 * 60));
            assertEquals(wkniSesionEnfoque.wkniFase.DESCANSO, s.wkniFase());
            assertTrue(s.wkniTick(5 * 60));
        }
        assertEquals(4, s.wkniSesion());
        assertTrue(s.wkniTick(25 * 60));
        assertEquals(wkniSesionEnfoque.wkniFase.DESCANSO_LARGO, s.wkniFase());
        // El descanso largo del pomodoro es un rango de 15 a 30 min.
        assertTrue(s.wkniTotal() >= 15 * 60 && s.wkniTotal() <= 30 * 60, "descanso largo fuera del rango: " + s.wkniTotal());
        assertTrue(s.wkniTick(s.wkniTotal()));
        assertEquals(1, s.wkniSesion());
        assertEquals(2, s.wkniCiclo());
    }

    @Test void wkniPausaYReinicio() {
        wkniSesionEnfoque s = new wkniSesionEnfoque(wkniMetodoEnfoque.wkniPREDEFINIDOS.get(1));
        assertFalse(s.wkniTick(60));               // empieza en pausa
        s.wkniIniciar();
        s.wkniTick(600);
        assertEquals(600, s.wkniTranscurrido());
        s.wkniPausar();
        s.wkniTick(600);
        assertEquals(600, s.wkniTranscurrido());
        s.wkniReiniciarFase();
        assertEquals(0, s.wkniTranscurrido());
        assertEquals("52:00", wkniSesionEnfoque.wkniFormato(s.wkniRestantes()));
    }

    @Test void wkniFlowtimeCalculaElDescanso() {
        wkniMetodoEnfoque flow = wkniMetodoEnfoque.wkniPorId("flowtime", List.of());
        wkniSesionEnfoque s = new wkniSesionEnfoque(flow);
        s.wkniIniciar();
        assertFalse(s.wkniTick(50 * 60));
        assertTrue(s.wkniCuentaLibre());
        s.wkniTerminarFlow();
        assertEquals(wkniSesionEnfoque.wkniFase.DESCANSO, s.wkniFase());
        assertEquals(10 * 60, s.wkniTotal());
    }

    @Test void wkniPersonalizadoYGuardado() {
        wkniMetodoEnfoque m = wkniMetodoEnfoque.wkniPersonalizado("Mi método", 40, 8, 3, 25);
        List<wkniMetodoEnfoque> l = wkniMetodoEnfoque.wkniDesdeMapas(List.of(m.wkniAMapa(), "basura", java.util.Map.of("name", "x")));
        assertEquals(1, l.size());
        assertEquals(40, l.get(0).wkniTrabajo());
        assertEquals(m.wkniId(), wkniMetodoEnfoque.wkniPorId(m.wkniId(), l).wkniId());
        assertEquals("pomodoro", wkniMetodoEnfoque.wkniPorId("no-existe", l).wkniId());
    }

    @Test void wkniLosRangosSeGuardanYSeRecuperan() {
        wkniMetodoEnfoque m = wkniMetodoEnfoque.wkniPersonalizado("Rango", 60, 90, 10, 20, 0, 0, 0);
        assertTrue(m.wkniRangoTrabajo());
        assertTrue(m.wkniRangoDescanso());
        assertFalse(m.wkniRangoDescansoLargo());
        assertTrue(m.wkniTieneRangos());
        assertEquals("60–90 min trabajo · 10–20 min descanso", m.wkniResumen());

        List<wkniMetodoEnfoque> l = wkniMetodoEnfoque.wkniDesdeMapas(List.of(m.wkniAMapa()));
        assertEquals(1, l.size());
        assertEquals(60, l.get(0).wkniTrabajoMin());
        assertEquals(90, l.get(0).wkniTrabajoMax());
        assertEquals(10, l.get(0).wkniDescansoMin());
        assertEquals(20, l.get(0).wkniDescansoMax());
    }

    @Test void wkniLaSesionEligeUnValorDentroDelRango() {
        wkniMetodoEnfoque m = wkniMetodoEnfoque.wkniPersonalizado("Rango", 60, 90, 10, 20, 0, 0, 0);
        wkniSesionEnfoque s = new wkniSesionEnfoque(m);
        assertTrue(s.wkniTrabajoUsado() >= 60 && s.wkniTrabajoUsado() <= 90, "trabajo fuera del rango: " + s.wkniTrabajoUsado());
        assertTrue(s.wkniDescansoUsado() >= 10 && s.wkniDescansoUsado() <= 20, "descanso fuera del rango: " + s.wkniDescansoUsado());
        assertEquals(s.wkniTrabajoUsado() * 60, s.wkniTotal());
    }

    @Test void wkniSePuedeFijarLaDuracionALaFuerza() {
        wkniMetodoEnfoque m = wkniMetodoEnfoque.wkniPersonalizado("Rango", 60, 90, 10, 20, 0, 0, 0);
        wkniSesionEnfoque s = new wkniSesionEnfoque(m, 75, 15, null);
        assertEquals(75, s.wkniTrabajoUsado());
        assertEquals(15, s.wkniDescansoUsado());
        assertEquals(75 * 60, s.wkniTotal());
    }

    @Test void wkniLosMetodosPredefinidosCubrenLoPedido() {
        List<String> ids = wkniMetodoEnfoque.wkniPREDEFINIDOS.stream().map(wkniMetodoEnfoque::wkniId).toList();
        assertTrue(ids.contains("pomodoro"));
        assertTrue(ids.contains("52-17"));
        assertTrue(ids.contains("deep-60"));
        assertTrue(ids.contains("deep-90"));
        assertTrue(ids.contains("deep-rango"));
        assertTrue(ids.contains("ultradiano"));
        assertTrue(ids.contains("flowtime"));
        assertTrue(ids.contains("timeboxing"));
        for (wkniMetodoEnfoque m : wkniMetodoEnfoque.wkniPREDEFINIDOS) {
            assertFalse(m.wkniDescripcion().isBlank(), "sin descripción: " + m.wkniId());
            assertFalse(m.wkniUsoComun().isBlank(), "sin uso común: " + m.wkniId());
        }
    }
}

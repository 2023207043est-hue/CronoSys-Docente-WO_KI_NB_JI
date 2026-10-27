package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class WO_KI_NB_JI_SesionEnfoqueTest {
    @Test void WO_KI_NB_JI_pomodoroCuatroSesionesYDescansoLargo() {
        WO_KI_NB_JI_SesionEnfoque s = new WO_KI_NB_JI_SesionEnfoque(WO_KI_NB_JI_MetodoEnfoque.PREDEFINIDOS.get(0));
        s.WO_KI_NB_JI_iniciar();
        for (int i = 1; i <= 3; i++) {
            assertEquals(i, s.WO_KI_NB_JI_sesion());
            assertTrue(s.WO_KI_NB_JI_tick(25 * 60));
            assertEquals(WO_KI_NB_JI_SesionEnfoque.WO_KI_NB_JI_Fase.DESCANSO, s.WO_KI_NB_JI_fase());
            assertTrue(s.WO_KI_NB_JI_tick(5 * 60));
        }
        assertEquals(4, s.WO_KI_NB_JI_sesion());
        assertTrue(s.WO_KI_NB_JI_tick(25 * 60));
        assertEquals(WO_KI_NB_JI_SesionEnfoque.WO_KI_NB_JI_Fase.DESCANSO_LARGO, s.WO_KI_NB_JI_fase());
        // El descanso largo del pomodoro es un rango de 15 a 30 min.
        assertTrue(s.WO_KI_NB_JI_total() >= 15 * 60 && s.WO_KI_NB_JI_total() <= 30 * 60, "descanso largo fuera del rango: " + s.WO_KI_NB_JI_total());
        assertTrue(s.WO_KI_NB_JI_tick(s.WO_KI_NB_JI_total()));
        assertEquals(1, s.WO_KI_NB_JI_sesion());
        assertEquals(2, s.WO_KI_NB_JI_ciclo());
    }

    @Test void WO_KI_NB_JI_pausaYReinicio() {
        WO_KI_NB_JI_SesionEnfoque s = new WO_KI_NB_JI_SesionEnfoque(WO_KI_NB_JI_MetodoEnfoque.PREDEFINIDOS.get(1));
        assertFalse(s.WO_KI_NB_JI_tick(60));               // empieza en pausa
        s.WO_KI_NB_JI_iniciar();
        s.WO_KI_NB_JI_tick(600);
        assertEquals(600, s.WO_KI_NB_JI_transcurrido());
        s.WO_KI_NB_JI_pausar();
        s.WO_KI_NB_JI_tick(600);
        assertEquals(600, s.WO_KI_NB_JI_transcurrido());
        s.WO_KI_NB_JI_reiniciarFase();
        assertEquals(0, s.WO_KI_NB_JI_transcurrido());
        assertEquals("52:00", WO_KI_NB_JI_SesionEnfoque.WO_KI_NB_JI_formato(s.WO_KI_NB_JI_restantes()));
    }

    @Test void WO_KI_NB_JI_flowtimeCalculaElDescanso() {
        WO_KI_NB_JI_MetodoEnfoque flow = WO_KI_NB_JI_MetodoEnfoque.WO_KI_NB_JI_porId("flowtime", List.of());
        WO_KI_NB_JI_SesionEnfoque s = new WO_KI_NB_JI_SesionEnfoque(flow);
        s.WO_KI_NB_JI_iniciar();
        assertFalse(s.WO_KI_NB_JI_tick(50 * 60));
        assertTrue(s.WO_KI_NB_JI_cuentaLibre());
        s.WO_KI_NB_JI_terminarFlow();
        assertEquals(WO_KI_NB_JI_SesionEnfoque.WO_KI_NB_JI_Fase.DESCANSO, s.WO_KI_NB_JI_fase());
        assertEquals(10 * 60, s.WO_KI_NB_JI_total());
    }

    @Test void WO_KI_NB_JI_personalizadoYGuardado() {
        WO_KI_NB_JI_MetodoEnfoque m = WO_KI_NB_JI_MetodoEnfoque.WO_KI_NB_JI_personalizado("Mi método", 40, 8, 3, 25);
        List<WO_KI_NB_JI_MetodoEnfoque> l = WO_KI_NB_JI_MetodoEnfoque.WO_KI_NB_JI_desdeMapas(List.of(m.WO_KI_NB_JI_aMapa(), "basura", java.util.Map.of("name", "x")));
        assertEquals(1, l.size());
        assertEquals(40, l.get(0).WO_KI_NB_JI_trabajo());
        assertEquals(m.id(), WO_KI_NB_JI_MetodoEnfoque.WO_KI_NB_JI_porId(m.id(), l).id());
        assertEquals("pomodoro", WO_KI_NB_JI_MetodoEnfoque.WO_KI_NB_JI_porId("no-existe", l).id());
    }

    @Test void WO_KI_NB_JI_losRangosSeGuardanYSeRecuperan() {
        WO_KI_NB_JI_MetodoEnfoque m = WO_KI_NB_JI_MetodoEnfoque.WO_KI_NB_JI_personalizado("Rango", 60, 90, 10, 20, 0, 0, 0);
        assertTrue(m.WO_KI_NB_JI_rangoTrabajo());
        assertTrue(m.WO_KI_NB_JI_rangoDescanso());
        assertFalse(m.WO_KI_NB_JI_rangoDescansoLargo());
        assertTrue(m.WO_KI_NB_JI_tieneRangos());
        assertEquals("60–90 min trabajo · 10–20 min descanso", m.WO_KI_NB_JI_resumen());

        List<WO_KI_NB_JI_MetodoEnfoque> l = WO_KI_NB_JI_MetodoEnfoque.WO_KI_NB_JI_desdeMapas(List.of(m.WO_KI_NB_JI_aMapa()));
        assertEquals(1, l.size());
        assertEquals(60, l.get(0).trabajoMin());
        assertEquals(90, l.get(0).trabajoMax());
        assertEquals(10, l.get(0).descansoMin());
        assertEquals(20, l.get(0).descansoMax());
    }

    @Test void WO_KI_NB_JI_laSesionEligeUnValorDentroDelRango() {
        WO_KI_NB_JI_MetodoEnfoque m = WO_KI_NB_JI_MetodoEnfoque.WO_KI_NB_JI_personalizado("Rango", 60, 90, 10, 20, 0, 0, 0);
        WO_KI_NB_JI_SesionEnfoque s = new WO_KI_NB_JI_SesionEnfoque(m);
        assertTrue(s.WO_KI_NB_JI_trabajoUsado() >= 60 && s.WO_KI_NB_JI_trabajoUsado() <= 90, "trabajo fuera del rango: " + s.WO_KI_NB_JI_trabajoUsado());
        assertTrue(s.WO_KI_NB_JI_descansoUsado() >= 10 && s.WO_KI_NB_JI_descansoUsado() <= 20, "descanso fuera del rango: " + s.WO_KI_NB_JI_descansoUsado());
        assertEquals(s.WO_KI_NB_JI_trabajoUsado() * 60, s.WO_KI_NB_JI_total());
    }

    @Test void WO_KI_NB_JI_sePuedeFijarLaDuracionALaFuerza() {
        WO_KI_NB_JI_MetodoEnfoque m = WO_KI_NB_JI_MetodoEnfoque.WO_KI_NB_JI_personalizado("Rango", 60, 90, 10, 20, 0, 0, 0);
        WO_KI_NB_JI_SesionEnfoque s = new WO_KI_NB_JI_SesionEnfoque(m, 75, 15, null);
        assertEquals(75, s.WO_KI_NB_JI_trabajoUsado());
        assertEquals(15, s.WO_KI_NB_JI_descansoUsado());
        assertEquals(75 * 60, s.WO_KI_NB_JI_total());
    }

    @Test void WO_KI_NB_JI_losMetodosPredefinidosCubrenLoPedido() {
        List<String> ids = WO_KI_NB_JI_MetodoEnfoque.PREDEFINIDOS.stream().map(WO_KI_NB_JI_MetodoEnfoque::id).toList();
        assertTrue(ids.contains("pomodoro"));
        assertTrue(ids.contains("52-17"));
        assertTrue(ids.contains("deep-60"));
        assertTrue(ids.contains("deep-90"));
        assertTrue(ids.contains("deep-rango"));
        assertTrue(ids.contains("ultradiano"));
        assertTrue(ids.contains("flowtime"));
        assertTrue(ids.contains("timeboxing"));
        for (WO_KI_NB_JI_MetodoEnfoque m : WO_KI_NB_JI_MetodoEnfoque.PREDEFINIDOS) {
            assertFalse(m.descripcion().isBlank(), "sin descripción: " + m.id());
            assertFalse(m.usoComun().isBlank(), "sin uso común: " + m.id());
        }
    }
}

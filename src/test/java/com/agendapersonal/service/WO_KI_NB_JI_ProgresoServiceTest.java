package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.model.WO_KI_NB_JI_Tarea;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class WO_KI_NB_JI_ProgresoServiceTest {
    private static WO_KI_NB_JI_Tarea WO_KI_NB_JI_t(int id, String estado, Integer proyecto) {
        return new WO_KI_NB_JI_Tarea(id, "T" + id, "", estado, LocalDate.of(2026, 10, 20), "Media", proyecto);
    }

    @Test void WO_KI_NB_JI_sinTareasEsCero() {
        assertEquals(0, WO_KI_NB_JI_ProgresoService.WO_KI_NB_JI_porcentaje(1, List.of()));
    }

    @Test void WO_KI_NB_JI_porcentajePorProyecto() {
        List<WO_KI_NB_JI_Tarea> ts = new ArrayList<>();
        for (int i = 1; i <= 6; i++) ts.add(WO_KI_NB_JI_t(i, "Pendiente", 1));
        ts.add(WO_KI_NB_JI_t(99, "Completada", 2));                       // otro proyecto: no cuenta
        assertEquals(0, WO_KI_NB_JI_ProgresoService.WO_KI_NB_JI_porcentaje(1, ts));
        ts.get(0).WO_KI_NB_JI_setEstado("Completada");
        assertEquals(17, WO_KI_NB_JI_ProgresoService.WO_KI_NB_JI_porcentaje(1, ts));  // 1/6
        ts.get(1).WO_KI_NB_JI_setEstado("Completada");
        assertEquals(33, WO_KI_NB_JI_ProgresoService.WO_KI_NB_JI_porcentaje(1, ts));  // 2/6 → el ejemplo "17% → 33%" del documento
    }

    @Test void WO_KI_NB_JI_enProgresoNoCuentaComoCompletada() {
        List<WO_KI_NB_JI_Tarea> ts = List.of(WO_KI_NB_JI_t(1, "En progreso", 1), WO_KI_NB_JI_t(2, "Completada", 1));
        assertEquals(50, WO_KI_NB_JI_ProgresoService.WO_KI_NB_JI_porcentaje(1, ts));
    }

    @Test void WO_KI_NB_JI_sinProyectoCalculaAvanceGlobal() {
        List<WO_KI_NB_JI_Tarea> ts = List.of(WO_KI_NB_JI_t(1, "Completada", 1), WO_KI_NB_JI_t(2, "Pendiente", 2), WO_KI_NB_JI_t(3, "Pendiente", null), WO_KI_NB_JI_t(4, "Pendiente", null));
        assertEquals(25, WO_KI_NB_JI_ProgresoService.WO_KI_NB_JI_porcentaje(null, ts));
    }

    @Test void WO_KI_NB_JI_mensajeDelToast() {
        var r = new WO_KI_NB_JI_ProgresoService.WO_KI_NB_JI_Resultado(1, 17, 33, true);
        assertEquals("Tarea completada (Tesis) · Progreso: 17% → 33%", r.WO_KI_NB_JI_mensaje("Tesis"));
        assertEquals("Tarea reabierta · Progreso: 33% → 17%", new WO_KI_NB_JI_ProgresoService.WO_KI_NB_JI_Resultado(null, 33, 17, false).WO_KI_NB_JI_mensaje(null));
        assertTrue(r.WO_KI_NB_JI_cambio());
    }
}

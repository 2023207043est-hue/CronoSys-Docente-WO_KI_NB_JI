package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.model.wkniTarea;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class wkniProgresoServiceTest {
    private static wkniTarea wkniT(int id, String estado, Integer proyecto) {
        return new wkniTarea(id, "T" + id, "", estado, LocalDate.of(2026, 10, 20), "Media", proyecto);
    }

    @Test void wkniSinTareasEsCero() {
        assertEquals(0, wkniProgresoService.wkniPorcentaje(1, List.of()));
    }

    @Test void wkniPorcentajePorProyecto() {
        List<wkniTarea> ts = new ArrayList<>();
        for (int i = 1; i <= 6; i++) ts.add(wkniT(i, "Pendiente", 1));
        ts.add(wkniT(99, "Completada", 2));                       // otro proyecto: no cuenta
        assertEquals(0, wkniProgresoService.wkniPorcentaje(1, ts));
        ts.get(0).wkniSetEstado("Completada");
        assertEquals(17, wkniProgresoService.wkniPorcentaje(1, ts));  // 1/6
        ts.get(1).wkniSetEstado("Completada");
        assertEquals(33, wkniProgresoService.wkniPorcentaje(1, ts));  // 2/6 → el ejemplo "17% → 33%" del documento
    }

    @Test void wkniEnProgresoNoCuentaComoCompletada() {
        List<wkniTarea> ts = List.of(wkniT(1, "En progreso", 1), wkniT(2, "Completada", 1));
        assertEquals(50, wkniProgresoService.wkniPorcentaje(1, ts));
    }

    @Test void wkniSinProyectoCalculaAvanceGlobal() {
        List<wkniTarea> ts = List.of(wkniT(1, "Completada", 1), wkniT(2, "Pendiente", 2), wkniT(3, "Pendiente", null), wkniT(4, "Pendiente", null));
        assertEquals(25, wkniProgresoService.wkniPorcentaje(null, ts));
    }

    @Test void wkniMensajeDelToast() {
        var r = new wkniProgresoService.wkniResultado(1, 17, 33, true);
        assertEquals("Tarea completada (Tesis) · Progreso: 17% → 33%", r.wkniMensaje("Tesis"));
        assertEquals("Tarea reabierta · Progreso: 33% → 17%", new wkniProgresoService.wkniResultado(null, 33, 17, false).wkniMensaje(null));
        assertTrue(r.wkniCambio());
    }
}

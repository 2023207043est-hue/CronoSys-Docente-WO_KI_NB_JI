package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.domain.model.WO_KI_NB_JI_Contacto;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Habito;
import com.agendapersonal.model.WO_KI_NB_JI_Evento;
import com.agendapersonal.model.WO_KI_NB_JI_Tarea;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class WO_KI_NB_JI_CalendarioUnificadoServiceTest {
    private static final LocalDate HOY = LocalDate.of(2026, 10, 16);
    private static final Set<WO_KI_NB_JI_TipoEntrada> TODO = EnumSet.allOf(WO_KI_NB_JI_TipoEntrada.class);

    private static WO_KI_NB_JI_Evento WO_KI_NB_JI_ev(int id, String t, String cat, String hi, String hf) {
        return new WO_KI_NB_JI_Evento(id, t, HOY, hi, hf, "", cat, "Media");
    }

    private static WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_Datos WO_KI_NB_JI_datos() {
        var tareaSuelta = new WO_KI_NB_JI_Tarea(1, "Pagar luz", "", "Pendiente", HOY, "Media", null);
        var tareaProyecto = new WO_KI_NB_JI_Tarea(2, "Entregar informe", "", "Completada", HOY, "Alta", 7);
        var habDiario = new WO_KI_NB_JI_Habito(1, "Leer", "", "DIARIO", 1, "#16a34a", true);
        var habSemanal = new WO_KI_NB_JI_Habito(2, "Revisión semanal", "", "SEMANAL", 1, "#16a34a", true);
        var habInactivo = new WO_KI_NB_JI_Habito(3, "Viejo", "", "DIARIO", 1, "#16a34a", false);
        var cumple = new WO_KI_NB_JI_Contacto(5, "Ana", "", "", "Amigos", "2000-10-16", "", false, null);
        return new WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_Datos(
            List.of(WO_KI_NB_JI_ev(1, "Dentista", "Salud", "09:00", "10:00"), WO_KI_NB_JI_ev(2, "Reunión", "Trabajo", "11:00", "12:00")),
            List.of(tareaSuelta, tareaProyecto), List.of(habDiario, habSemanal, habInactivo), List.of(cumple),
            Map.of(HOY.toString(), Set.of(1)));
    }

    @Test void WO_KI_NB_JI_segmentaCadaTipo() {
        var r = WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_delDia(HOY, WO_KI_NB_JI_datos(), TODO);
        assertEquals(1, r.stream().filter(e -> e.tipo() == WO_KI_NB_JI_TipoEntrada.EVENTO).count());
        assertEquals(2, r.stream().filter(e -> e.tipo() == WO_KI_NB_JI_TipoEntrada.TRABAJO).count()); // evento Trabajo + tarea con proyecto
        assertEquals(1, r.stream().filter(e -> e.tipo() == WO_KI_NB_JI_TipoEntrada.TAREA).count());
        assertEquals(1, r.stream().filter(e -> e.tipo() == WO_KI_NB_JI_TipoEntrada.CUMPLE).count());
        assertEquals(1, r.stream().filter(e -> e.tipo() == WO_KI_NB_JI_TipoEntrada.HABITO).count());   // viernes: sin el semanal ni el inactivo
    }

    @Test void WO_KI_NB_JI_filtraPorSegmentoVisible() {
        var soloCumples = WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_delDia(HOY, WO_KI_NB_JI_datos(), EnumSet.of(WO_KI_NB_JI_TipoEntrada.CUMPLE));
        assertEquals(1, soloCumples.size());
        assertEquals("Cumple 26 años", soloCumples.getFirst().detalle());
        assertTrue(WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_delDia(HOY, WO_KI_NB_JI_datos(), EnumSet.noneOf(WO_KI_NB_JI_TipoEntrada.class)).isEmpty());
    }

    @Test void WO_KI_NB_JI_habitoMarcadoYSemanalSoloLosLunes() {
        var hab = WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_delDia(HOY, WO_KI_NB_JI_datos(), EnumSet.of(WO_KI_NB_JI_TipoEntrada.HABITO));
        assertTrue(hab.getFirst().hecha());
        var lunes = WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_delDia(LocalDate.of(2026, 10, 19), WO_KI_NB_JI_datos(), EnumSet.of(WO_KI_NB_JI_TipoEntrada.HABITO));
        assertEquals(2, lunes.size());
        assertFalse(lunes.getFirst().hecha());
    }

    @Test void WO_KI_NB_JI_cumpleSeRepiteCadaAnioYElBisiestoCaeElDia28() {
        var d = new WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_Datos(List.of(), List.of(), List.of(),
            List.of(new WO_KI_NB_JI_Contacto(9, "Leap", "", "", "", "2000-02-29", "", false, null)), Map.of());
        assertEquals(1, WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_delDia(LocalDate.of(2027, 2, 28), d, TODO).size());
        assertEquals(0, WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_delDia(LocalDate.of(2027, 3, 1), d, TODO).size());
        assertEquals(1, WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_delDia(LocalDate.of(2028, 2, 29), d, TODO).size());
    }

    @Test void WO_KI_NB_JI_ordenaTodoElDiaPrimeroYLuegoPorHora() {
        var r = WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_delDia(HOY, WO_KI_NB_JI_datos(), TODO);
        assertTrue(r.getFirst().WO_KI_NB_JI_todoElDia());
        var conHora = r.stream().filter(e -> !e.WO_KI_NB_JI_todoElDia()).toList();
        assertEquals(List.of("Dentista", "Reunión"), conHora.stream().map(WO_KI_NB_JI_Entrada::titulo).toList());
    }

    @Test void WO_KI_NB_JI_horaCorruptaSeMuestraComoTodoElDia() {
        var d = new WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_Datos(List.of(WO_KI_NB_JI_ev(1, "Raro", "Personal", "25:99", "x")),
            List.of(), List.of(), List.of(), Map.of());
        var r = WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_delDia(HOY, d, TODO);
        assertEquals(1, r.size());
        assertTrue(r.getFirst().WO_KI_NB_JI_todoElDia());
    }

    @Test void WO_KI_NB_JI_carrilesRepartenEventosSolapados() {
        var d = new WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_Datos(List.of(
            WO_KI_NB_JI_ev(1, "A", "Personal", "09:00", "11:00"), WO_KI_NB_JI_ev(2, "B", "Personal", "10:00", "12:00"),
            WO_KI_NB_JI_ev(3, "C", "Personal", "13:00", "14:00")), List.of(), List.of(), List.of(), Map.of());
        var conHora = WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_delDia(HOY, d, TODO);
        var c = WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_asignarCarriles(conHora);
        var a = c.stream().filter(x -> x.entrada().titulo().equals("A")).findFirst().orElseThrow();
        var b = c.stream().filter(x -> x.entrada().titulo().equals("B")).findFirst().orElseThrow();
        var cc = c.stream().filter(x -> x.entrada().titulo().equals("C")).findFirst().orElseThrow();
        assertEquals(2, a.carriles());
        assertNotEquals(a.carril(), b.carril());
        assertEquals(1, cc.carriles());   // C no se solapa con nadie
    }
}

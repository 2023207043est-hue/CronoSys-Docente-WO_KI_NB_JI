package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.domain.model.wkniContacto;
import com.agendapersonal.domain.model.wkniHabito;
import com.agendapersonal.model.wkniEvento;
import com.agendapersonal.model.wkniTarea;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class wkniCalendarioUnificadoServiceTest {
    private static final LocalDate HOY = LocalDate.of(2026, 10, 16);
    private static final Set<wkniTipoEntrada> TODO = EnumSet.allOf(wkniTipoEntrada.class);

    private static wkniEvento wkniEv(int id, String t, String cat, String hi, String hf) {
        return new wkniEvento(id, t, HOY, hi, hf, "", cat, "Media");
    }

    private static wkniCalendarioUnificadoService.wkniDatos wkniDatos() {
        var tareaSuelta = new wkniTarea(1, "Pagar luz", "", "Pendiente", HOY, "Media", null);
        var tareaProyecto = new wkniTarea(2, "Entregar informe", "", "Completada", HOY, "Alta", 7);
        var habDiario = new wkniHabito(1, "Leer", "", "DIARIO", 1, "#16a34a", true);
        var habSemanal = new wkniHabito(2, "Revisión semanal", "", "SEMANAL", 1, "#16a34a", true);
        var habInactivo = new wkniHabito(3, "Viejo", "", "DIARIO", 1, "#16a34a", false);
        var cumple = new wkniContacto(5, "Ana", "", "", "Amigos", "2000-10-16", "", false, null);
        return new wkniCalendarioUnificadoService.wkniDatos(
            List.of(wkniEv(1, "Dentista", "Salud", "09:00", "10:00"), wkniEv(2, "Reunión", "Trabajo", "11:00", "12:00")),
            List.of(tareaSuelta, tareaProyecto), List.of(habDiario, habSemanal, habInactivo), List.of(cumple),
            Map.of(HOY.toString(), Set.of(1)));
    }

    @Test void wkniSegmentaCadaTipo() {
        var r = wkniCalendarioUnificadoService.wkniDelDia(HOY, wkniDatos(), TODO);
        assertEquals(1, r.stream().filter(e -> e.wkni_tipo() == wkniTipoEntrada.EVENTO).count());
        assertEquals(2, r.stream().filter(e -> e.wkni_tipo() == wkniTipoEntrada.TRABAJO).count()); // evento Trabajo + tarea con proyecto
        assertEquals(1, r.stream().filter(e -> e.wkni_tipo() == wkniTipoEntrada.TAREA).count());
        assertEquals(1, r.stream().filter(e -> e.wkni_tipo() == wkniTipoEntrada.CUMPLE).count());
        assertEquals(1, r.stream().filter(e -> e.wkni_tipo() == wkniTipoEntrada.HABITO).count());   // viernes: sin el semanal ni el inactivo
    }

    @Test void wkniFiltraPorSegmentoVisible() {
        var soloCumples = wkniCalendarioUnificadoService.wkniDelDia(HOY, wkniDatos(), EnumSet.of(wkniTipoEntrada.CUMPLE));
        assertEquals(1, soloCumples.size());
        assertEquals("Cumple 26 años", soloCumples.getFirst().wkniDetalle());
        assertTrue(wkniCalendarioUnificadoService.wkniDelDia(HOY, wkniDatos(), EnumSet.noneOf(wkniTipoEntrada.class)).isEmpty());
    }

    @Test void wkniHabitoMarcadoYSemanalSoloLosLunes() {
        var hab = wkniCalendarioUnificadoService.wkniDelDia(HOY, wkniDatos(), EnumSet.of(wkniTipoEntrada.HABITO));
        assertTrue(hab.getFirst().wkniHecha());
        var lunes = wkniCalendarioUnificadoService.wkniDelDia(LocalDate.of(2026, 10, 19), wkniDatos(), EnumSet.of(wkniTipoEntrada.HABITO));
        assertEquals(2, lunes.size());
        assertFalse(lunes.getFirst().wkniHecha());
    }

    @Test void wkniCumpleSeRepiteCadaAnioYElBisiestoCaeElDia28() {
        var d = new wkniCalendarioUnificadoService.wkniDatos(List.of(), List.of(), List.of(),
            List.of(new wkniContacto(9, "Leap", "", "", "", "2000-02-29", "", false, null)), Map.of());
        assertEquals(1, wkniCalendarioUnificadoService.wkniDelDia(LocalDate.of(2027, 2, 28), d, TODO).size());
        assertEquals(0, wkniCalendarioUnificadoService.wkniDelDia(LocalDate.of(2027, 3, 1), d, TODO).size());
        assertEquals(1, wkniCalendarioUnificadoService.wkniDelDia(LocalDate.of(2028, 2, 29), d, TODO).size());
    }

    @Test void wkniOrdenaTodoElDiaPrimeroYLuegoPorHora() {
        var r = wkniCalendarioUnificadoService.wkniDelDia(HOY, wkniDatos(), TODO);
        assertTrue(r.getFirst().wkniTodoElDia());
        var conHora = r.stream().filter(e -> !e.wkniTodoElDia()).toList();
        assertEquals(List.of("Dentista", "Reunión"), conHora.stream().map(wkniEntrada::wkniTitulo).toList());
    }

    @Test void wkniHoraCorruptaSeMuestraComoTodoElDia() {
        var d = new wkniCalendarioUnificadoService.wkniDatos(List.of(wkniEv(1, "Raro", "Personal", "25:99", "x")),
            List.of(), List.of(), List.of(), Map.of());
        var r = wkniCalendarioUnificadoService.wkniDelDia(HOY, d, TODO);
        assertEquals(1, r.size());
        assertTrue(r.getFirst().wkniTodoElDia());
    }

    @Test void wkniCarrilesRepartenEventosSolapados() {
        var d = new wkniCalendarioUnificadoService.wkniDatos(List.of(
            wkniEv(1, "A", "Personal", "09:00", "11:00"), wkniEv(2, "B", "Personal", "10:00", "12:00"),
            wkniEv(3, "C", "Personal", "13:00", "14:00")), List.of(), List.of(), List.of(), Map.of());
        var conHora = wkniCalendarioUnificadoService.wkniDelDia(HOY, d, TODO);
        var c = wkniCalendarioUnificadoService.wkniAsignarCarriles(conHora);
        var a = c.stream().filter(x -> x.wkni_entrada().wkniTitulo().equals("A")).findFirst().orElseThrow();
        var b = c.stream().filter(x -> x.wkni_entrada().wkniTitulo().equals("B")).findFirst().orElseThrow();
        var cc = c.stream().filter(x -> x.wkni_entrada().wkniTitulo().equals("C")).findFirst().orElseThrow();
        assertEquals(2, a.wkniCarriles());
        assertNotEquals(a.wkni_carril(), b.wkni_carril());
        assertEquals(1, cc.wkniCarriles());   // C no se solapa con nadie
    }
}

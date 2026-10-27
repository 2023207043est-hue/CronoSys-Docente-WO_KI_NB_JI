package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class WO_KI_NB_JI_FechasImportantesTest {
    @Test void WO_KI_NB_JI_idaYVuelta() {
        var l = List.of(new WO_KI_NB_JI_FechasImportantes.WO_KI_NB_JI_Fecha("Aniversario", LocalDate.of(2020, 5, 3)), new WO_KI_NB_JI_FechasImportantes.WO_KI_NB_JI_Fecha("Graduación = 1; x", LocalDate.of(2019, 12, 1)));
        String s = WO_KI_NB_JI_FechasImportantes.WO_KI_NB_JI_escribir(l);
        var r = WO_KI_NB_JI_FechasImportantes.WO_KI_NB_JI_leer(s);
        assertEquals(2, r.size());
        assertEquals("Aniversario", r.get(0).etiqueta());
        assertEquals(LocalDate.of(2019, 12, 1), r.get(1).fecha());
    }
    @Test void WO_KI_NB_JI_tolerante() {
        assertTrue(WO_KI_NB_JI_FechasImportantes.WO_KI_NB_JI_leer(null).isEmpty());
        assertTrue(WO_KI_NB_JI_FechasImportantes.WO_KI_NB_JI_leer("basura;x=y;=2020-01-01").isEmpty());
    }
    @Test void WO_KI_NB_JI_diasHasta() {
        assertEquals(0L, WO_KI_NB_JI_FechasImportantes.WO_KI_NB_JI_diasHasta(LocalDate.of(2000, 3, 10), LocalDate.of(2026, 3, 10)));
        assertEquals(364L, WO_KI_NB_JI_FechasImportantes.WO_KI_NB_JI_diasHasta(LocalDate.of(2000, 3, 10), LocalDate.of(2026, 3, 11)));
    }
}

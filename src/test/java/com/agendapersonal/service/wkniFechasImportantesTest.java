package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class wkniFechasImportantesTest {
    @Test void wkniIdaYVuelta() {
        var l = List.of(new wkniFechasImportantes.wkniFecha("Aniversario", LocalDate.of(2020, 5, 3)), new wkniFechasImportantes.wkniFecha("Graduación = 1; x", LocalDate.of(2019, 12, 1)));
        String s = wkniFechasImportantes.wkniEscribir(l);
        var r = wkniFechasImportantes.wkniLeer(s);
        assertEquals(2, r.size());
        assertEquals("Aniversario", r.get(0).wkniEtiqueta());
        assertEquals(LocalDate.of(2019, 12, 1), r.get(1).wkni_fecha());
    }
    @Test void wkniTolerante() {
        assertTrue(wkniFechasImportantes.wkniLeer(null).isEmpty());
        assertTrue(wkniFechasImportantes.wkniLeer("basura;x=y;=2020-01-01").isEmpty());
    }
    @Test void wkniDiasHasta() {
        assertEquals(0L, wkniFechasImportantes.wkniDiasHasta(LocalDate.of(2000, 3, 10), LocalDate.of(2026, 3, 10)));
        assertEquals(364L, wkniFechasImportantes.wkniDiasHasta(LocalDate.of(2000, 3, 10), LocalDate.of(2026, 3, 11)));
    }
}

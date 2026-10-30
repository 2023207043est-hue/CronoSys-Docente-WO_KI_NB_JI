package com.agendapersonal.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class wkniEnumsTest {
    @Test void wkniPrioridadAceptaVariantesPeroRechazaBasura() {
        assertEquals(wkniPrioridad.ALTA, wkniPrioridad.wkniDesde("Alta"));
        assertEquals(wkniPrioridad.ALTA, wkniPrioridad.wkniDesde(" alta "));
        assertEquals(wkniPrioridad.MEDIA, wkniPrioridad.wkniDesde(null));
        assertThrows(IllegalArgumentException.class, () -> wkniPrioridad.wkniDesde("Urgentísima"));
        assertEquals(wkniPrioridad.MEDIA, wkniPrioridad.wkniDesdeBD("???"));   // datos viejos no rompen la app
    }

    @Test void wkniEstadoTarea() {
        assertEquals(wkniEstadoTarea.EN_PROGRESO, wkniEstadoTarea.wkniDesde("En progreso"));
        assertEquals(wkniEstadoTarea.EN_PROGRESO, wkniEstadoTarea.wkniDesde("EN_PROGRESO"));
        assertEquals("Completada", wkniEstadoTarea.COMPLETADA.wkniEtiqueta());
        assertThrows(IllegalArgumentException.class, () -> wkniEstadoTarea.wkniDesde("Hecha"));
        assertEquals(wkniEstadoTarea.PENDIENTE, wkniEstadoTarea.wkniDesdeBD("x"));
    }

    @Test void wkniModelosValidanAlAsignar() {
        wkniTarea t = new wkniTarea("T", "", "Pendiente", LocalDate.now(), "Alta");
        assertEquals("Alta", t.wkniGetPrioridad());
        assertThrows(IllegalArgumentException.class, () -> t.wkniSetEstado("Inventado"));
        assertEquals("Pendiente", t.wkniGetEstado());                  // no quedó en estado inválido
        wkniEvento e = new wkniEvento("E", LocalDate.now(), "09:00", "10:00", "", "Personal", "baja");
        assertEquals("Baja", e.wkniGetPrioridad());                    // normalizada
        assertThrows(IllegalArgumentException.class, () -> e.wkniSetPrioridad("X"));
    }

    @Test void wkniHorasTipadas() {
        assertTrue(wkniHoras.wkniEsValida("09:30"));
        assertTrue(wkniHoras.wkniEsValida("9:30"));
        assertFalse(wkniHoras.wkniEsValida("24:00"));
        assertFalse(wkniHoras.wkniEsValida("10/13"));
        assertFalse(wkniHoras.wkniEsValida(null));
    }
}

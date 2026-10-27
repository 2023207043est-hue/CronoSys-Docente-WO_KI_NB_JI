package com.agendapersonal.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class WO_KI_NB_JI_EnumsTest {
    @Test void WO_KI_NB_JI_prioridadAceptaVariantesPeroRechazaBasura() {
        assertEquals(WO_KI_NB_JI_Prioridad.ALTA, WO_KI_NB_JI_Prioridad.WO_KI_NB_JI_desde("Alta"));
        assertEquals(WO_KI_NB_JI_Prioridad.ALTA, WO_KI_NB_JI_Prioridad.WO_KI_NB_JI_desde(" alta "));
        assertEquals(WO_KI_NB_JI_Prioridad.MEDIA, WO_KI_NB_JI_Prioridad.WO_KI_NB_JI_desde(null));
        assertThrows(IllegalArgumentException.class, () -> WO_KI_NB_JI_Prioridad.WO_KI_NB_JI_desde("Urgentísima"));
        assertEquals(WO_KI_NB_JI_Prioridad.MEDIA, WO_KI_NB_JI_Prioridad.WO_KI_NB_JI_desdeBD("???"));   // datos viejos no rompen la app
    }

    @Test void WO_KI_NB_JI_estadoTarea() {
        assertEquals(WO_KI_NB_JI_EstadoTarea.EN_PROGRESO, WO_KI_NB_JI_EstadoTarea.WO_KI_NB_JI_desde("En progreso"));
        assertEquals(WO_KI_NB_JI_EstadoTarea.EN_PROGRESO, WO_KI_NB_JI_EstadoTarea.WO_KI_NB_JI_desde("EN_PROGRESO"));
        assertEquals("Completada", WO_KI_NB_JI_EstadoTarea.COMPLETADA.WO_KI_NB_JI_etiqueta());
        assertThrows(IllegalArgumentException.class, () -> WO_KI_NB_JI_EstadoTarea.WO_KI_NB_JI_desde("Hecha"));
        assertEquals(WO_KI_NB_JI_EstadoTarea.PENDIENTE, WO_KI_NB_JI_EstadoTarea.WO_KI_NB_JI_desdeBD("x"));
    }

    @Test void WO_KI_NB_JI_modelosValidanAlAsignar() {
        WO_KI_NB_JI_Tarea t = new WO_KI_NB_JI_Tarea("T", "", "Pendiente", LocalDate.now(), "Alta");
        assertEquals("Alta", t.WO_KI_NB_JI_getPrioridad());
        assertThrows(IllegalArgumentException.class, () -> t.WO_KI_NB_JI_setEstado("Inventado"));
        assertEquals("Pendiente", t.WO_KI_NB_JI_getEstado());                  // no quedó en estado inválido
        WO_KI_NB_JI_Evento e = new WO_KI_NB_JI_Evento("E", LocalDate.now(), "09:00", "10:00", "", "Personal", "baja");
        assertEquals("Baja", e.WO_KI_NB_JI_getPrioridad());                    // normalizada
        assertThrows(IllegalArgumentException.class, () -> e.WO_KI_NB_JI_setPrioridad("X"));
    }

    @Test void WO_KI_NB_JI_horasTipadas() {
        assertTrue(WO_KI_NB_JI_Horas.WO_KI_NB_JI_esValida("09:30"));
        assertTrue(WO_KI_NB_JI_Horas.WO_KI_NB_JI_esValida("9:30"));
        assertFalse(WO_KI_NB_JI_Horas.WO_KI_NB_JI_esValida("24:00"));
        assertFalse(WO_KI_NB_JI_Horas.WO_KI_NB_JI_esValida("10/13"));
        assertFalse(WO_KI_NB_JI_Horas.WO_KI_NB_JI_esValida(null));
    }
}

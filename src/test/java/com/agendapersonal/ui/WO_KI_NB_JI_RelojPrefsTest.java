package com.agendapersonal.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.model.WO_KI_NB_JI_Prioridad;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class WO_KI_NB_JI_RelojPrefsTest {
    @Test void WO_KI_NB_JI_formato24ConYSinSegundos() {
        assertEquals("09:05", WO_KI_NB_JI_RelojPrefs.WO_KI_NB_JI_formatear(LocalTime.of(9, 5, 30), true, false));
        assertEquals("21:07:09", WO_KI_NB_JI_RelojPrefs.WO_KI_NB_JI_formatear(LocalTime.of(21, 7, 9), true, true));
    }

    @Test void WO_KI_NB_JI_formato12HorasUsaAmPm() {
        String s = WO_KI_NB_JI_RelojPrefs.WO_KI_NB_JI_formatear(LocalTime.of(21, 7), false, false).toLowerCase();
        assertTrue(s.startsWith("9:07"), s);
        assertTrue(s.contains("p"), s);
    }

    @Test void WO_KI_NB_JI_tamanoDesconocidoCaeEnMediano() {
        assertEquals(WO_KI_NB_JI_RelojPrefs.WO_KI_NB_JI_Tamano.MEDIANO, WO_KI_NB_JI_RelojPrefs.WO_KI_NB_JI_Tamano.WO_KI_NB_JI_desde("loquesea"));
        assertEquals(WO_KI_NB_JI_RelojPrefs.WO_KI_NB_JI_Tamano.GRANDE, WO_KI_NB_JI_RelojPrefs.WO_KI_NB_JI_Tamano.WO_KI_NB_JI_desde("GRANDE"));
    }

    @Test void WO_KI_NB_JI_prioridadTieneUnColorDistintoPorNivel() {
        assertEquals("-p-alta", WO_KI_NB_JI_Prioridad.ALTA.WO_KI_NB_JI_color());
        assertEquals("-p-media", WO_KI_NB_JI_Prioridad.MEDIA.WO_KI_NB_JI_color());
        assertEquals("-p-baja-soft", WO_KI_NB_JI_Prioridad.BAJA.WO_KI_NB_JI_fondo());
        assertEquals("p-media", WO_KI_NB_JI_Prioridad.MEDIA.WO_KI_NB_JI_tono());
    }
}

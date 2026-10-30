package com.agendapersonal.ui;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.model.wkniPrioridad;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class wkniRelojPrefsTest {
    @Test void wkniFormato24ConYSinSegundos() {
        assertEquals("09:05", wkniRelojPrefs.wkniFormatear(LocalTime.of(9, 5, 30), true, false));
        assertEquals("21:07:09", wkniRelojPrefs.wkniFormatear(LocalTime.of(21, 7, 9), true, true));
    }

    @Test void wkniFormato12HorasUsaAmPm() {
        String s = wkniRelojPrefs.wkniFormatear(LocalTime.of(21, 7), false, false).toLowerCase();
        assertTrue(s.startsWith("9:07"), s);
        assertTrue(s.contains("p"), s);
    }

    @Test void wkniTamanoDesconocidoCaeEnMediano() {
        assertEquals(wkniRelojPrefs.wkniTamano.MEDIANO, wkniRelojPrefs.wkniTamano.wkniDesde("loquesea"));
        assertEquals(wkniRelojPrefs.wkniTamano.GRANDE, wkniRelojPrefs.wkniTamano.wkniDesde("GRANDE"));
    }

    @Test void wkniPrioridadTieneUnColorDistintoPorNivel() {
        assertEquals("-p-alta", wkniPrioridad.ALTA.wkniColor());
        assertEquals("-p-media", wkniPrioridad.MEDIA.wkniColor());
        assertEquals("-p-baja-soft", wkniPrioridad.BAJA.wkniFondo());
        assertEquals("p-media", wkniPrioridad.MEDIA.wkniTono());
    }
}

package com.agendapersonal.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class wkniPerfilTest {
    @Test void wkniSaludaSegunLaHoraYUsaElPrimerNombre() {
        assertEquals("Buenos días, Jason", wkniPerfil.wkniSaludo(8, "  jason   selmouni "));
        assertEquals("Buenas tardes, Ana", wkniPerfil.wkniSaludo(15, "Ana"));
        assertEquals("Buenas noches, Luz", wkniPerfil.wkniSaludo(22, "luz"));
    }

    @Test void wkniSinNombreSoloSaluda() {
        assertEquals("Buenos días", wkniPerfil.wkniSaludo(7, ""));
        assertEquals("Buenas noches", wkniPerfil.wkniSaludo(21, null));
        assertEquals("Buenas tardes", wkniPerfil.wkniSaludo(12, "   "));
    }

    @Test void wkniLimpiaEspaciosYLimitaElLargo() {
        assertEquals("Ana María", wkniPerfil.wkniLimpiar("  Ana    María "));
        assertEquals(40, wkniPerfil.wkniLimpiar("x".repeat(100)).length());
        assertEquals("", wkniPerfil.wkniLimpiar(null));
    }
}

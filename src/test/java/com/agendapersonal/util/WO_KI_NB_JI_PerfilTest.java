package com.agendapersonal.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class WO_KI_NB_JI_PerfilTest {
    @Test void WO_KI_NB_JI_saludaSegunLaHoraYUsaElPrimerNombre() {
        assertEquals("Buenos días, Jason", WO_KI_NB_JI_Perfil.WO_KI_NB_JI_saludo(8, "  jason   selmouni "));
        assertEquals("Buenas tardes, Ana", WO_KI_NB_JI_Perfil.WO_KI_NB_JI_saludo(15, "Ana"));
        assertEquals("Buenas noches, Luz", WO_KI_NB_JI_Perfil.WO_KI_NB_JI_saludo(22, "luz"));
    }

    @Test void WO_KI_NB_JI_sinNombreSoloSaluda() {
        assertEquals("Buenos días", WO_KI_NB_JI_Perfil.WO_KI_NB_JI_saludo(7, ""));
        assertEquals("Buenas noches", WO_KI_NB_JI_Perfil.WO_KI_NB_JI_saludo(21, null));
        assertEquals("Buenas tardes", WO_KI_NB_JI_Perfil.WO_KI_NB_JI_saludo(12, "   "));
    }

    @Test void WO_KI_NB_JI_limpiaEspaciosYLimitaElLargo() {
        assertEquals("Ana María", WO_KI_NB_JI_Perfil.WO_KI_NB_JI_limpiar("  Ana    María "));
        assertEquals(40, WO_KI_NB_JI_Perfil.WO_KI_NB_JI_limpiar("x".repeat(100)).length());
        assertEquals("", WO_KI_NB_JI_Perfil.WO_KI_NB_JI_limpiar(null));
    }
}

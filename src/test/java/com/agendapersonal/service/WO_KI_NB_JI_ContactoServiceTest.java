package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.domain.model.WO_KI_NB_JI_Contacto;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class WO_KI_NB_JI_ContactoServiceTest {
    private static final LocalDate HOY = LocalDate.of(2026, 10, 16);

    private static WO_KI_NB_JI_Contacto WO_KI_NB_JI_c(String nombre, String ultimo, String cumple) {
        return new WO_KI_NB_JI_Contacto(1, nombre, "", "", "Amigos", cumple, "", false, ultimo);
    }

    @Test void WO_KI_NB_JI_marcarContactadoHoy() {
        WO_KI_NB_JI_Contacto x = WO_KI_NB_JI_c("Ana", null, null);
        WO_KI_NB_JI_ContactoService.WO_KI_NB_JI_marcarContactadoHoy(x, HOY);
        assertEquals("2026-10-16", x.WO_KI_NB_JI_getUltimoContacto());
        assertEquals(0, WO_KI_NB_JI_ContactoService.WO_KI_NB_JI_diasSinContacto(x, HOY).getAsLong());
    }

    @Test void WO_KI_NB_JI_seguimientoOrdenaPorMasOlvidado() {
        var lista = List.of(WO_KI_NB_JI_c("Reciente", "2026-10-10", null), WO_KI_NB_JI_c("Viejo", "2026-06-01", null),
                            WO_KI_NB_JI_c("Medio", "2026-09-01", null), WO_KI_NB_JI_c("Nunca", null, null));
        var r = WO_KI_NB_JI_ContactoService.WO_KI_NB_JI_requierenSeguimiento(lista, HOY, 30);
        assertEquals(List.of("Nunca", "Viejo", "Medio"), r.stream().map(s -> s.contacto().WO_KI_NB_JI_getNombre()).toList());
    }

    @Test void WO_KI_NB_JI_cumpleanosProximosIncluyenCambioDeAnio() {
        var lista = List.of(WO_KI_NB_JI_c("Hoy", null, "2000-10-16"), WO_KI_NB_JI_c("En5", null, "1999-10-21"),
                            WO_KI_NB_JI_c("FinDeAnio", null, "1990-12-31"), WO_KI_NB_JI_c("EneroSig", null, "1995-01-02"),
                            WO_KI_NB_JI_c("Lejano", null, "1990-05-05"));
        var r = WO_KI_NB_JI_ContactoService.WO_KI_NB_JI_proximosCumpleanos(lista, HOY, 90);
        assertEquals(List.of("Hoy", "En5", "FinDeAnio", "EneroSig"), r.stream().map(k -> k.contacto().WO_KI_NB_JI_getNombre()).toList());
        assertEquals(0, r.getFirst().enDias());
        assertEquals(26, r.getFirst().edad());
        assertEquals(32, r.get(3).edad());   // cumple el 2-ene-2027: 2027 - 1995
    }

    @Test void WO_KI_NB_JI_fechasInvalidasSeIgnoran() {
        assertTrue(WO_KI_NB_JI_ContactoService.WO_KI_NB_JI_proximosCumpleanos(List.of(WO_KI_NB_JI_c("X", null, "no-es-fecha")), HOY, 365).isEmpty());
    }
}

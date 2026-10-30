package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.domain.model.wkniContacto;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class wkniContactoServiceTest {
    private static final LocalDate HOY = LocalDate.of(2026, 10, 16);

    private static wkniContacto wkniC(String nombre, String ultimo, String cumple) {
        return new wkniContacto(1, nombre, "", "", "Amigos", cumple, "", false, ultimo);
    }

    @Test void wkniMarcarContactadoHoy() {
        wkniContacto x = wkniC("Ana", null, null);
        wkniContactoService.wkniMarcarContactadoHoy(x, HOY);
        assertEquals("2026-10-16", x.wkniGetUltimoContacto());
        assertEquals(0, wkniContactoService.wkniDiasSinContacto(x, HOY).getAsLong());
    }

    @Test void wkniSeguimientoOrdenaPorMasOlvidado() {
        var lista = List.of(wkniC("Reciente", "2026-10-10", null), wkniC("Viejo", "2026-06-01", null),
                            wkniC("Medio", "2026-09-01", null), wkniC("Nunca", null, null));
        var r = wkniContactoService.wkniRequierenSeguimiento(lista, HOY, 30);
        assertEquals(List.of("Nunca", "Viejo", "Medio"), r.stream().map(s -> s.wkni_contacto().wkniGetNombre()).toList());
    }

    @Test void wkniCumpleanosProximosIncluyenCambioDeAnio() {
        var lista = List.of(wkniC("Hoy", null, "2000-10-16"), wkniC("En5", null, "1999-10-21"),
                            wkniC("FinDeAnio", null, "1990-12-31"), wkniC("EneroSig", null, "1995-01-02"),
                            wkniC("Lejano", null, "1990-05-05"));
        var r = wkniContactoService.wkniProximosCumpleanos(lista, HOY, 90);
        assertEquals(List.of("Hoy", "En5", "FinDeAnio", "EneroSig"), r.stream().map(k -> k.wkni_contacto().wkniGetNombre()).toList());
        assertEquals(0, r.getFirst().wkniEnDias());
        assertEquals(26, r.getFirst().wkniEdad());
        assertEquals(32, r.get(3).wkniEdad());   // cumple el 2-ene-2027: 2027 - 1995
    }

    @Test void wkniFechasInvalidasSeIgnoran() {
        assertTrue(wkniContactoService.wkniProximosCumpleanos(List.of(wkniC("X", null, "no-es-fecha")), HOY, 365).isEmpty());
    }
}

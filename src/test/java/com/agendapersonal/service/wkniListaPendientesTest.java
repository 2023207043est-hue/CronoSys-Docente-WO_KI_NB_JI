package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.domain.model.wkniNota;
import com.agendapersonal.model.wkniEstadoTarea;
import com.agendapersonal.model.wkniTarea;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class wkniListaPendientesTest {

    private static wkniNota wkniNota(int id, String titulo, String contenido, String etiquetas) {
        return new wkniNota(id, titulo, contenido, etiquetas, "NOTA", "2026-10-20", "2026-10-20", false);
    }

    @Test void wkniSacaLasCasillasDeLasNotas() {
        List<wkniNota> notas = List.of(
            wkniNota(1, "Lista del día", "- [ ] Escribir el informe\n- [x] Revisar el correo\nSin casilla", ""));
        List<wkniListaPendientes.wkniItem> items = wkniListaPendientes.wkniDesdeNotas(notas);
        assertEquals(2, items.size());
        assertEquals("Escribir el informe", items.get(0).wkniTexto());
        assertFalse(items.get(0).wkniHecho());
        assertEquals(0, items.get(0).wkniLinea());
        assertEquals("Revisar el correo", items.get(1).wkniTexto());
        assertTrue(items.get(1).wkniHecho());
    }

    @Test void wkniAceptaLosDistintosFormatosDeCasilla() {
        List<wkniNota> notas = List.of(wkniNota(1, "Varios", "[ ] uno\n* [ ] dos\n☐ tres\n- [x] cuatro", ""));
        List<wkniListaPendientes.wkniItem> items = wkniListaPendientes.wkniDesdeNotas(notas);
        assertEquals(4, items.size());
        assertEquals("uno", items.get(0).wkniTexto());
        assertEquals("dos", items.get(1).wkniTexto());
        assertEquals("tres", items.get(2).wkniTexto());
        assertEquals("cuatro", items.get(3).wkniTexto());
        assertTrue(items.get(3).wkniHecho());
    }

    @Test void wkniUnaNotaConEtiquetaPendienteCuentaComoUnPendiente() {
        List<wkniNota> notas = List.of(wkniNota(7, "Llamar al banco", "sin casillas", "pendiente, casa"));
        List<wkniListaPendientes.wkniItem> items = wkniListaPendientes.wkniDesdeNotas(notas);
        assertEquals(1, items.size());
        assertEquals("Llamar al banco", items.get(0).wkniTexto());
        assertEquals(-1, items.get(0).wkniLinea());
    }

    @Test void wkniMarcarDesmarcaGuardaElCambioEnElTexto() {
        wkniNota n = wkniNota(1, "Lista", "- [ ] Primero\n- [ ] Segundo", "");
        List<wkniListaPendientes.wkniItem> items = wkniListaPendientes.wkniDesdeNotas(List.of(n));
        String marcado = wkniListaPendientes.wkniAlternar(n, items.get(0), true);
        assertTrue(marcado.startsWith("- [x] Primero"), "no se marcó: " + marcado);
        String desmarque = wkniListaPendientes.wkniAlternar(n, items.get(1), false);
        assertTrue(desmarque.contains("- [ ] Segundo"));
    }

    @Test void wkniLasTareasCompletadasNoAparecen() {
        wkniTarea pendiente = new wkniTarea(1, "Hacer la compra", "", "Pendiente", LocalDate.now(), "Media");
        wkniTarea hecha = new wkniTarea(2, "Otra cosa", "", "Completada", LocalDate.now(), "Media");
        hecha.wkniSetEstado(wkniEstadoTarea.COMPLETADA);
        List<wkniListaPendientes.wkniItem> items = wkniListaPendientes.wkniDesdeTareas(List.of(pendiente, hecha));
        assertEquals(1, items.size());
        assertEquals("Hacer la compra", items.get(0).wkniTexto());
        assertEquals("tarea", items.get(0).wkniOrigen());
    }

    @Test void wkniNoSeInventaNadaSiNoHayCasillas() {
        assertTrue(wkniListaPendientes.wkniDesdeNotas(List.of(wkniNota(1, "Nota normal", "solo texto", ""))).isEmpty());
        assertTrue(wkniListaPendientes.wkniDesdeNotas(null).isEmpty());
        assertTrue(wkniListaPendientes.wkniDesdeTareas(null).isEmpty());
    }
}

package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.domain.model.WO_KI_NB_JI_Nota;
import com.agendapersonal.model.WO_KI_NB_JI_EstadoTarea;
import com.agendapersonal.model.WO_KI_NB_JI_Tarea;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class WO_KI_NB_JI_ListaPendientesTest {

    private static WO_KI_NB_JI_Nota WO_KI_NB_JI_nota(int id, String titulo, String contenido, String etiquetas) {
        return new WO_KI_NB_JI_Nota(id, titulo, contenido, etiquetas, "NOTA", "2026-10-20", "2026-10-20", false);
    }

    @Test void WO_KI_NB_JI_sacaLasCasillasDeLasNotas() {
        List<WO_KI_NB_JI_Nota> notas = List.of(
            WO_KI_NB_JI_nota(1, "Lista del día", "- [ ] Escribir el informe\n- [x] Revisar el correo\nSin casilla", ""));
        List<WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_Item> items = WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_desdeNotas(notas);
        assertEquals(2, items.size());
        assertEquals("Escribir el informe", items.get(0).texto());
        assertFalse(items.get(0).hecho());
        assertEquals(0, items.get(0).linea());
        assertEquals("Revisar el correo", items.get(1).texto());
        assertTrue(items.get(1).hecho());
    }

    @Test void WO_KI_NB_JI_aceptaLosDistintosFormatosDeCasilla() {
        List<WO_KI_NB_JI_Nota> notas = List.of(WO_KI_NB_JI_nota(1, "Varios", "[ ] uno\n* [ ] dos\n☐ tres\n- [x] cuatro", ""));
        List<WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_Item> items = WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_desdeNotas(notas);
        assertEquals(4, items.size());
        assertEquals("uno", items.get(0).texto());
        assertEquals("dos", items.get(1).texto());
        assertEquals("tres", items.get(2).texto());
        assertEquals("cuatro", items.get(3).texto());
        assertTrue(items.get(3).hecho());
    }

    @Test void WO_KI_NB_JI_unaNotaConEtiquetaPendienteCuentaComoUnPendiente() {
        List<WO_KI_NB_JI_Nota> notas = List.of(WO_KI_NB_JI_nota(7, "Llamar al banco", "sin casillas", "pendiente, casa"));
        List<WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_Item> items = WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_desdeNotas(notas);
        assertEquals(1, items.size());
        assertEquals("Llamar al banco", items.get(0).texto());
        assertEquals(-1, items.get(0).linea());
    }

    @Test void WO_KI_NB_JI_marcarDesmarcaGuardaElCambioEnElTexto() {
        WO_KI_NB_JI_Nota n = WO_KI_NB_JI_nota(1, "Lista", "- [ ] Primero\n- [ ] Segundo", "");
        List<WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_Item> items = WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_desdeNotas(List.of(n));
        String marcado = WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_alternar(n, items.get(0), true);
        assertTrue(marcado.startsWith("- [x] Primero"), "no se marcó: " + marcado);
        String desmarque = WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_alternar(n, items.get(1), false);
        assertTrue(desmarque.contains("- [ ] Segundo"));
    }

    @Test void WO_KI_NB_JI_lasTareasCompletadasNoAparecen() {
        WO_KI_NB_JI_Tarea pendiente = new WO_KI_NB_JI_Tarea(1, "Hacer la compra", "", "Pendiente", LocalDate.now(), "Media");
        WO_KI_NB_JI_Tarea hecha = new WO_KI_NB_JI_Tarea(2, "Otra cosa", "", "Completada", LocalDate.now(), "Media");
        hecha.WO_KI_NB_JI_setEstado(WO_KI_NB_JI_EstadoTarea.COMPLETADA);
        List<WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_Item> items = WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_desdeTareas(List.of(pendiente, hecha));
        assertEquals(1, items.size());
        assertEquals("Hacer la compra", items.get(0).texto());
        assertEquals("tarea", items.get(0).origen());
    }

    @Test void WO_KI_NB_JI_noSeInventaNadaSiNoHayCasillas() {
        assertTrue(WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_desdeNotas(List.of(WO_KI_NB_JI_nota(1, "Nota normal", "solo texto", ""))).isEmpty());
        assertTrue(WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_desdeNotas(null).isEmpty());
        assertTrue(WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_desdeTareas(null).isEmpty());
    }
}

package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.domain.model.WO_KI_NB_JI_EstadoEntrega;
import com.agendapersonal.service.WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Evaluacion;
import com.agendapersonal.service.WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class WO_KI_NB_JI_RiesgoEstudianteServiceTest {
    private static final LocalDate D = LocalDate.of(2026, 10, 1);

    /** Historial con un estado por día consecutivo a partir de D. */
    private static TreeMap<LocalDate, String> WO_KI_NB_JI_h(String... estados) {
        TreeMap<LocalDate, String> m = new TreeMap<>();
        for (int i = 0; i < estados.length; i++) m.put(D.plusDays(i), estados[i]);
        return m;
    }

    private static WO_KI_NB_JI_Evaluacion WO_KI_NB_JI_ev(TreeMap<LocalDate, String> h, List<WO_KI_NB_JI_EstadoEntrega> e, Double nota) {
        return WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_evaluar(h, e, List.of(), nota, 11);
    }

    @Test void WO_KI_NB_JI_sinProblemasEstaAlDia() {
        WO_KI_NB_JI_Evaluacion r = WO_KI_NB_JI_ev(WO_KI_NB_JI_h("Presente", "Presente", "Retardo", "Presente"), List.of(WO_KI_NB_JI_EstadoEntrega.ENTREGADO), 15.0);
        assertEquals(WO_KI_NB_JI_Nivel.BIEN, r.nivel());
        assertTrue(r.motivos().isEmpty());
        assertEquals(100.0, r.porcentajeAsistencia(), 0.001);
        assertTrue(r.alDia());
    }

    @Test void WO_KI_NB_JI_tresFaltasConsecutivasEsRiesgoConMotivo() {
        WO_KI_NB_JI_Evaluacion r = WO_KI_NB_JI_ev(WO_KI_NB_JI_h("Presente", "Presente", "Presente", "Presente", "Presente", "Presente", "Presente", "Falta", "Falta", "Falta"), List.of(), null);
        assertEquals(WO_KI_NB_JI_Nivel.ALTO, r.nivel());
        assertEquals(3, r.faltasConsecutivas());
        assertTrue(r.motivos().contains("3 faltas consecutivas"));
    }

    @Test void WO_KI_NB_JI_justificadaNoCortaLaRacha() {
        assertEquals(2, WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_faltasConsecutivas(WO_KI_NB_JI_h("Presente", "Falta", "Justificada", "Falta")));
        assertEquals(0, WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_faltasConsecutivas(WO_KI_NB_JI_h("Falta", "Presente")));
    }

    @Test void WO_KI_NB_JI_dosTrabajosNoEntregadosEsRiesgo() {
        WO_KI_NB_JI_Evaluacion r = WO_KI_NB_JI_ev(WO_KI_NB_JI_h("Presente"), Arrays.asList(WO_KI_NB_JI_EstadoEntrega.NO_ENTREGADO, null, WO_KI_NB_JI_EstadoEntrega.NO_ENTREGADO), 14.0);
        assertEquals(WO_KI_NB_JI_Nivel.ALTO, r.nivel());
        assertTrue(r.motivos().contains("2 trabajos no entregados"));
        assertFalse(r.alDia());
    }

    @Test void WO_KI_NB_JI_trabajoSinRegistrarNoCuentaComoNoEntregado() {
        WO_KI_NB_JI_Evaluacion r = WO_KI_NB_JI_ev(WO_KI_NB_JI_h("Presente"), Arrays.asList(null, WO_KI_NB_JI_EstadoEntrega.TARDE), 14.0);
        assertEquals(WO_KI_NB_JI_Nivel.BIEN, r.nivel());
        assertTrue(r.alDia());
    }

    @Test void WO_KI_NB_JI_notaBajaSolaEsAtencionPeroConInasistenciasEsRiesgo() {
        WO_KI_NB_JI_Evaluacion soloNota = WO_KI_NB_JI_ev(WO_KI_NB_JI_h("Presente", "Presente", "Presente", "Presente"), List.of(), 9.0);
        assertEquals(WO_KI_NB_JI_Nivel.ATENCION, soloNota.nivel());
        assertTrue(soloNota.motivos().contains("Nota 9 por debajo de 11"));

        WO_KI_NB_JI_Evaluacion cruzada = WO_KI_NB_JI_ev(WO_KI_NB_JI_h("Presente", "Falta", "Presente", "Falta", "Presente"), List.of(), 9.0);
        assertEquals(WO_KI_NB_JI_Nivel.ALTO, cruzada.nivel());
        assertTrue(cruzada.motivos().contains("El bajo rendimiento coincide con sus inasistencias"));
    }

    @Test void WO_KI_NB_JI_detectaQueDejoDeAsistir() {
        WO_KI_NB_JI_Evaluacion r = WO_KI_NB_JI_ev(WO_KI_NB_JI_h("Presente", "Presente", "Presente", "Presente", "Presente", "Falta", "Presente", "Falta"), List.of(), null);
        assertTrue(r.motivos().stream().anyMatch(m -> m.startsWith("Dejó de asistir")), r.motivos().toString());
    }

    @Test void WO_KI_NB_JI_conPocasSesionesNoJuzgaElPorcentaje() {
        WO_KI_NB_JI_Evaluacion r = WO_KI_NB_JI_ev(WO_KI_NB_JI_h("Falta"), List.of(), null);
        assertEquals(WO_KI_NB_JI_Nivel.BIEN, r.nivel());
        assertEquals(0.0, r.porcentajeAsistencia(), 0.001);
    }

    @Test void WO_KI_NB_JI_promedioDeTrabajosSeUsaSiNoHayCalificacion() {
        WO_KI_NB_JI_Evaluacion r = WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_evaluar(WO_KI_NB_JI_h("Presente"), List.of(), List.of(8.0, 10.0), null, 11);
        assertEquals(9.0, r.nota(), 0.001);
        assertEquals(WO_KI_NB_JI_Nivel.ATENCION, r.nivel());
    }

    @Test void WO_KI_NB_JI_formatoDeNumeros() {
        assertEquals("11", WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(11));
        assertEquals("10.5", WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(10.5));
    }
}

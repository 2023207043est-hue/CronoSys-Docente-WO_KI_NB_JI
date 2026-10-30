package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.domain.model.wkniEstadoEntrega;
import com.agendapersonal.service.wkniRiesgoEstudianteService.wkniEvaluacion;
import com.agendapersonal.service.wkniRiesgoEstudianteService.wkniNivel;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

class wkniRiesgoEstudianteServiceTest {
    private static final LocalDate D = LocalDate.of(2026, 10, 1);

    /** Historial con un estado por día consecutivo a partir de D. */
    private static TreeMap<LocalDate, String> wkniH(String... estados) {
        TreeMap<LocalDate, String> m = new TreeMap<>();
        for (int i = 0; i < estados.length; i++) m.put(D.plusDays(i), estados[i]);
        return m;
    }

    private static wkniEvaluacion wkniEv(TreeMap<LocalDate, String> h, List<wkniEstadoEntrega> e, Double nota) {
        return wkniRiesgoEstudianteService.wkniEvaluar(h, e, List.of(), nota, 11);
    }

    @Test void wkniSinProblemasEstaAlDia() {
        wkniEvaluacion r = wkniEv(wkniH("Presente", "Presente", "Retardo", "Presente"), List.of(wkniEstadoEntrega.ENTREGADO), 15.0);
        assertEquals(wkniNivel.BIEN, r.wkni_nivel());
        assertTrue(r.wkniMotivos().isEmpty());
        assertEquals(100.0, r.wkniPorcentajeAsistencia(), 0.001);
        assertTrue(r.wkniAlDia());
    }

    @Test void wkniTresFaltasConsecutivasEsRiesgoConMotivo() {
        wkniEvaluacion r = wkniEv(wkniH("Presente", "Presente", "Presente", "Presente", "Presente", "Presente", "Presente", "Falta", "Falta", "Falta"), List.of(), null);
        assertEquals(wkniNivel.ALTO, r.wkni_nivel());
        assertEquals(3, r.wkniFaltasConsecutivas());
        assertTrue(r.wkniMotivos().contains("3 faltas consecutivas"));
    }

    @Test void wkniJustificadaNoCortaLaRacha() {
        assertEquals(2, wkniRiesgoEstudianteService.wkniFaltasConsecutivas(wkniH("Presente", "Falta", "Justificada", "Falta")));
        assertEquals(0, wkniRiesgoEstudianteService.wkniFaltasConsecutivas(wkniH("Falta", "Presente")));
    }

    @Test void wkniDosTrabajosNoEntregadosEsRiesgo() {
        wkniEvaluacion r = wkniEv(wkniH("Presente"), Arrays.asList(wkniEstadoEntrega.NO_ENTREGADO, null, wkniEstadoEntrega.NO_ENTREGADO), 14.0);
        assertEquals(wkniNivel.ALTO, r.wkni_nivel());
        assertTrue(r.wkniMotivos().contains("2 trabajos no entregados"));
        assertFalse(r.wkniAlDia());
    }

    @Test void wkniTrabajoSinRegistrarNoCuentaComoNoEntregado() {
        wkniEvaluacion r = wkniEv(wkniH("Presente"), Arrays.asList(null, wkniEstadoEntrega.TARDE), 14.0);
        assertEquals(wkniNivel.BIEN, r.wkni_nivel());
        assertTrue(r.wkniAlDia());
    }

    @Test void wkniNotaBajaSolaEsAtencionPeroConInasistenciasEsRiesgo() {
        wkniEvaluacion soloNota = wkniEv(wkniH("Presente", "Presente", "Presente", "Presente"), List.of(), 9.0);
        assertEquals(wkniNivel.ATENCION, soloNota.wkni_nivel());
        assertTrue(soloNota.wkniMotivos().contains("Nota 9 por debajo de 11"));

        wkniEvaluacion cruzada = wkniEv(wkniH("Presente", "Falta", "Presente", "Falta", "Presente"), List.of(), 9.0);
        assertEquals(wkniNivel.ALTO, cruzada.wkni_nivel());
        assertTrue(cruzada.wkniMotivos().contains("El bajo rendimiento coincide con sus inasistencias"));
    }

    @Test void wkniDetectaQueDejoDeAsistir() {
        wkniEvaluacion r = wkniEv(wkniH("Presente", "Presente", "Presente", "Presente", "Presente", "Falta", "Presente", "Falta"), List.of(), null);
        assertTrue(r.wkniMotivos().stream().anyMatch(m -> m.startsWith("Dejó de asistir")), r.wkniMotivos().toString());
    }

    @Test void wkniConPocasSesionesNoJuzgaElPorcentaje() {
        wkniEvaluacion r = wkniEv(wkniH("Falta"), List.of(), null);
        assertEquals(wkniNivel.BIEN, r.wkni_nivel());
        assertEquals(0.0, r.wkniPorcentajeAsistencia(), 0.001);
    }

    @Test void wkniPromedioDeTrabajosSeUsaSiNoHayCalificacion() {
        wkniEvaluacion r = wkniRiesgoEstudianteService.wkniEvaluar(wkniH("Presente"), List.of(), List.of(8.0, 10.0), null, 11);
        assertEquals(9.0, r.wkni_nota(), 0.001);
        assertEquals(wkniNivel.ATENCION, r.wkni_nivel());
    }

    @Test void wkniFormatoDeNumeros() {
        assertEquals("11", wkniRiesgoEstudianteService.wkniNumero(11));
        assertEquals("10.5", wkniRiesgoEstudianteService.wkniNumero(10.5));
    }
}

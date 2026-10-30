package com.agendapersonal.service;

import com.agendapersonal.domain.model.wkniActividadClase;
import com.agendapersonal.domain.model.wkniEntrega;
import com.agendapersonal.domain.model.wkniEstadoAsistencia;
import com.agendapersonal.domain.model.wkniEstadoEntrega;
import com.agendapersonal.domain.model.wkniEstudianteClase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Datos del reporte individual para el apoderado o el tutor (sin UI): resumen de asistencia, historial,
 * trabajos con su nota, alertas y sugerencias concretas según los motivos detectados.
 */
public record wkniReporteEstudiante(wkniEstudianteClase wkniEstudiante, String wkniCurso, String wkniPeriodo, double wkniNotaMinima,
                                wkniRiesgoEstudianteService.wkniEvaluacion wkni_evaluacion,
                                int wkniPresentes, int wkniFaltas, int wkniTardanzas, int wkniJustificadas,
                                Map<LocalDate, wkniEstadoAsistencia> wkniHistorial,
                                List<wkniTrabajo> wkniTrabajos, double wkniPromedioTrabajos, List<String> wkniSugerencias) {

    /** Un trabajo del curso con lo que registró el docente (estado null = sin registrar / aún no vence). */
    public record wkniTrabajo(String wkniTitulo, String wkni_tipo, LocalDate wkni_fecha, wkniEstadoEntrega wkniEstado, Double wkni_nota, boolean wkniVencido) {}

    public static wkniReporteEstudiante wkniDe(wkniResumenCurso wkniR, wkniEstudianteClase wkniE, LocalDate wkniHoy) {
        wkniRiesgoEstudianteService.wkniEvaluacion wkniEv = wkniR.wkniEvaluacion(wkniE.wkniId());
        Map<LocalDate, wkniEstadoAsistencia> wkniHist = new LinkedHashMap<>();
        int[] wkniN = new int[4];
        wkniR.wkniHistorial(wkniE.wkniId()).forEach((wkniF, wkniV) -> {
            wkniEstadoAsistencia wkniSt = wkniEstadoAsistencia.wkniDe(wkniV);
            if (wkniSt == null) return;
            wkniHist.put(wkniF, wkniSt);
            wkniN[wkniSt.ordinal()]++;
        });
        List<wkniTrabajo> wkniTrabajos = new ArrayList<>();
        for (wkniActividadClase wkniA : wkniR.wkniEvaluables()) {
            wkniEntrega wkniEn = wkniR.wkniEntrega(wkniA.wkniId(), wkniE.wkniId());
            wkniTrabajos.add(new wkniTrabajo(wkniA.wkniTitulo(), wkniA.wkni_tipo(), wkniA.wkni_fecha(), wkniEn == null ? null : wkniEn.wkniEstado(),
                wkniEn == null ? null : wkniEn.wkni_nota(), wkniA.wkni_fecha() != null && !wkniA.wkni_fecha().isAfter(wkniHoy)));
        }
        String wkniCurso = wkniR.wkniClase().toString();
        return new wkniReporteEstudiante(wkniE, wkniCurso, wkniR.wkniClase().wkniPeriodo(), wkniR.wkniClase().wkniNotaMinima(), wkniEv,
            wkniN[0], wkniN[1], wkniN[2], wkniN[3], wkniHist, List.copyOf(wkniTrabajos), wkniR.wkniPromedioTrabajos(wkniE.wkniId()), wkniSugerencias(wkniEv, wkniR.wkniClase().wkniNotaMinima()));
    }

    /** Recomendaciones para conversar con la familia según lo que se detectó. */
    static List<String> wkniSugerencias(wkniRiesgoEstudianteService.wkniEvaluacion wkniEv, double wkniNotaMinima) {
        List<String> wkniS = new ArrayList<>();
        boolean wkniAsistencia = wkniEv.wkniFaltasConsecutivas() >= 2
            || (wkniEv.wkniSesiones() >= wkniRiesgoEstudianteService.wkniSESIONES_MINIMAS && wkniEv.wkniPorcentajeAsistencia() < wkniRiesgoEstudianteService.wkniASISTENCIA_ATENCION);
        if (wkniAsistencia) wkniS.add("Conversar con la familia sobre las inasistencias y su motivo; acordar el seguimiento de la asistencia diaria.");
        if (wkniEv.wkniTardanzas() >= 3) wkniS.add("Revisar la rutina de llegada: acumula " + wkniEv.wkniTardanzas() + " tardanzas.");
        if (wkniEv.wkniNoEntregados() > 0) wkniS.add("Ponerse al día con " + (wkniEv.wkniNoEntregados() == 1 ? "el trabajo pendiente" : "los " + wkniEv.wkniNoEntregados() + " trabajos pendientes")
            + " y acordar una fecha de entrega.");
        if (wkniEv.wkni_nota() != null && wkniEv.wkni_nota() < wkniNotaMinima) wkniS.add("Reforzar los temas del curso (tutoría o sesiones de repaso) para alcanzar la nota mínima de "
            + wkniRiesgoEstudianteService.wkniNumero(wkniNotaMinima) + ".");
        if (wkniS.isEmpty()) wkniS.add("Buen desempeño: mantener la constancia en asistencia y entregas.");
        return List.copyOf(wkniS);
    }

    public double wkniPorcentajeAsistencia() { return wkni_evaluacion.wkniPorcentajeAsistencia(); }
}

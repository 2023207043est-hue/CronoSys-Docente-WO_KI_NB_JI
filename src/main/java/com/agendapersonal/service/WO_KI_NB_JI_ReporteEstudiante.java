package com.agendapersonal.service;

import com.agendapersonal.domain.model.WO_KI_NB_JI_ActividadClase;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Entrega;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstadoAsistencia;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstadoEntrega;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstudianteClase;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Datos del reporte individual para el apoderado o el tutor (sin UI): resumen de asistencia, historial,
 * trabajos con su nota, alertas y sugerencias concretas según los motivos detectados.
 */
public record WO_KI_NB_JI_ReporteEstudiante(WO_KI_NB_JI_EstudianteClase estudiante, String curso, String periodo, double notaMinima,
                                WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Evaluacion evaluacion,
                                int presentes, int faltas, int tardanzas, int justificadas,
                                Map<LocalDate, WO_KI_NB_JI_EstadoAsistencia> historial,
                                List<WO_KI_NB_JI_Trabajo> trabajos, double promedioTrabajos, List<String> sugerencias) {

    /** Un trabajo del curso con lo que registró el docente (estado null = sin registrar / aún no vence). */
    public record WO_KI_NB_JI_Trabajo(String titulo, String tipo, LocalDate fecha, WO_KI_NB_JI_EstadoEntrega estado, Double nota, boolean vencido) {}

    public static WO_KI_NB_JI_ReporteEstudiante WO_KI_NB_JI_de(WO_KI_NB_JI_ResumenCurso r, WO_KI_NB_JI_EstudianteClase e, LocalDate hoy) {
        WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Evaluacion ev = r.WO_KI_NB_JI_evaluacion(e.id());
        Map<LocalDate, WO_KI_NB_JI_EstadoAsistencia> hist = new LinkedHashMap<>();
        int[] n = new int[4];
        r.WO_KI_NB_JI_historial(e.id()).forEach((f, v) -> {
            WO_KI_NB_JI_EstadoAsistencia st = WO_KI_NB_JI_EstadoAsistencia.WO_KI_NB_JI_de(v);
            if (st == null) return;
            hist.put(f, st);
            n[st.ordinal()]++;
        });
        List<WO_KI_NB_JI_Trabajo> trabajos = new ArrayList<>();
        for (WO_KI_NB_JI_ActividadClase a : r.WO_KI_NB_JI_evaluables()) {
            WO_KI_NB_JI_Entrega en = r.WO_KI_NB_JI_entrega(a.id(), e.id());
            trabajos.add(new WO_KI_NB_JI_Trabajo(a.titulo(), a.tipo(), a.fecha(), en == null ? null : en.estado(),
                en == null ? null : en.nota(), a.fecha() != null && !a.fecha().isAfter(hoy)));
        }
        String curso = r.WO_KI_NB_JI_clase().toString();
        return new WO_KI_NB_JI_ReporteEstudiante(e, curso, r.WO_KI_NB_JI_clase().periodo(), r.WO_KI_NB_JI_clase().notaMinima(), ev,
            n[0], n[1], n[2], n[3], hist, List.copyOf(trabajos), r.WO_KI_NB_JI_promedioTrabajos(e.id()), WO_KI_NB_JI_sugerencias(ev, r.WO_KI_NB_JI_clase().notaMinima()));
    }

    /** Recomendaciones para conversar con la familia según lo que se detectó. */
    static List<String> WO_KI_NB_JI_sugerencias(WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Evaluacion ev, double notaMinima) {
        List<String> s = new ArrayList<>();
        boolean asistencia = ev.faltasConsecutivas() >= 2
            || (ev.sesiones() >= WO_KI_NB_JI_RiesgoEstudianteService.SESIONES_MINIMAS && ev.porcentajeAsistencia() < WO_KI_NB_JI_RiesgoEstudianteService.ASISTENCIA_ATENCION);
        if (asistencia) s.add("Conversar con la familia sobre las inasistencias y su motivo; acordar el seguimiento de la asistencia diaria.");
        if (ev.tardanzas() >= 3) s.add("Revisar la rutina de llegada: acumula " + ev.tardanzas() + " tardanzas.");
        if (ev.noEntregados() > 0) s.add("Ponerse al día con " + (ev.noEntregados() == 1 ? "el trabajo pendiente" : "los " + ev.noEntregados() + " trabajos pendientes")
            + " y acordar una fecha de entrega.");
        if (ev.nota() != null && ev.nota() < notaMinima) s.add("Reforzar los temas del curso (tutoría o sesiones de repaso) para alcanzar la nota mínima de "
            + WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(notaMinima) + ".");
        if (s.isEmpty()) s.add("Buen desempeño: mantener la constancia en asistencia y entregas.");
        return List.copyOf(s);
    }

    public double WO_KI_NB_JI_porcentajeAsistencia() { return evaluacion.porcentajeAsistencia(); }
}

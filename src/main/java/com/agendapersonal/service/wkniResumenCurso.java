package com.agendapersonal.service;

import com.agendapersonal.domain.model.wkniActividadClase;
import com.agendapersonal.domain.model.wkniClaseDocente;
import com.agendapersonal.domain.model.wkniEntrega;
import com.agendapersonal.domain.model.wkniEstadoAsistencia;
import com.agendapersonal.domain.model.wkniEstadoEntrega;
import com.agendapersonal.domain.model.wkniEstudianteClase;
import com.agendapersonal.infrastructure.repo.wkniClaseDocenteDAO;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Foto de un curso cargada de una vez (estudiantes, asistencia, trabajos y entregas) con la evaluación de
 * riesgo de cada estudiante ya calculada. La usan el panel, las alertas, las tablas y los reportes.
 */
public final class wkniResumenCurso {
    private final wkniClaseDocente wkniClase;
    private final List<wkniEstudianteClase> wkniEstudiantes;
    private final Map<Integer, TreeMap<LocalDate, String>> wkniHistorial;
    private final List<LocalDate> wkniFechas;
    private final List<wkniActividadClase> wkniEvaluables;
    private final Map<Integer, Map<Integer, wkniEntrega>> wkniEntregas;
    private final Map<Integer, wkniRiesgoEstudianteService.wkniEvaluacion> wkniEvaluaciones = new HashMap<>();

    public wkniResumenCurso(wkniClaseDocente wkniClase, wkniClaseDocenteDAO wkniDao, LocalDate wkniHoy) {
        this.wkniClase = wkniClase;
        this.wkniEstudiantes = wkniDao.wkniListarEstudiantes(wkniClase.wkniId());
        this.wkniHistorial = wkniDao.wkniHistorialAsistencia(wkniClase.wkniId());
        this.wkniFechas = wkniDao.wkniFechasAsistencia(wkniClase.wkniId());
        this.wkniEvaluables = wkniDao.wkniListarEvaluables(wkniClase.wkniId());
        this.wkniEntregas = wkniDao.wkniEntregas(wkniClase.wkniId());
        for (wkniEstudianteClase wkniE : wkniEstudiantes) {
            List<wkniEstadoEntrega> wkniVencidas = new ArrayList<>();
            List<Double> wkniNotas = new ArrayList<>();
            for (wkniActividadClase wkniA : wkniEvaluables) {
                wkniEntrega wkniEn = wkniEntrega(wkniA.wkniId(), wkniE.wkniId());
                if (wkniEn != null && wkniEn.wkni_nota() != null) wkniNotas.add(wkniEn.wkni_nota());
                if (wkniA.wkni_fecha() != null && !wkniA.wkni_fecha().isAfter(wkniHoy)) wkniVencidas.add(wkniEn == null ? null : wkniEn.wkniEstado());
            }
            wkniEvaluaciones.put(wkniE.wkniId(), wkniRiesgoEstudianteService.wkniEvaluar(wkniHistorial(wkniE.wkniId()), wkniVencidas, wkniNotas,
                wkniE.wkniCalificacion(), wkniClase.wkniNotaMinima()));
        }
    }

    public wkniClaseDocente wkniClase() { return wkniClase; }
    public List<wkniEstudianteClase> wkniEstudiantes() { return wkniEstudiantes; }
    public List<LocalDate> wkniFechas() { return wkniFechas; }
    public List<wkniActividadClase> wkniEvaluables() { return wkniEvaluables; }

    public TreeMap<LocalDate, String> wkniHistorial(int wkniEstudianteId) {
        return wkniHistorial.getOrDefault(wkniEstudianteId, new TreeMap<>());
    }

    public wkniEntrega wkniEntrega(int wkniActividadId, int wkniEstudianteId) {
        Map<Integer, wkniEntrega> wkniM = wkniEntregas.get(wkniActividadId);
        return wkniM == null ? null : wkniM.get(wkniEstudianteId);
    }

    public wkniRiesgoEstudianteService.wkniEvaluacion wkniEvaluacion(int wkniEstudianteId) { return wkniEvaluaciones.get(wkniEstudianteId); }

    /** Estudiantes con alguna alerta, primero los de mayor riesgo. */
    public List<wkniEstudianteClase> wkniConAlertas() {
        return wkniEstudiantes.stream().filter(wkniE -> wkniEvaluacion(wkniE.wkniId()).wkni_nivel() != wkniRiesgoEstudianteService.wkniNivel.BIEN)
            .sorted(Comparator.comparing((wkniEstudianteClase wkniE) -> -wkniEvaluacion(wkniE.wkniId()).wkni_nivel().ordinal())
                .thenComparing(wkniEstudianteClase::wkniNombre, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    public long wkniContar(wkniRiesgoEstudianteService.wkniNivel wkni_nivel) {
        return wkniEstudiantes.stream().filter(wkniE -> wkniEvaluacion(wkniE.wkniId()).wkni_nivel() == wkni_nivel).count();
    }

    /** Estudiantes sin trabajos pendientes (ningún "No entregado"). */
    public long wkniAlDia() { return wkniEstudiantes.stream().filter(wkniE -> wkniEvaluacion(wkniE.wkniId()).wkniAlDia()).count(); }

    /** Cuántos registros de cada estado hay en todo el curso. */
    public Map<wkniEstadoAsistencia, Integer> wkniDistribucionAsistencia() {
        Map<wkniEstadoAsistencia, Integer> wkniM = new EnumMap<>(wkniEstadoAsistencia.class);
        for (wkniEstadoAsistencia wkniE : wkniEstadoAsistencia.values()) wkniM.put(wkniE, 0);
        for (TreeMap<LocalDate, String> wkniH : wkniHistorial.values())
            for (String wkniV : wkniH.values()) { wkniEstadoAsistencia wkniE = wkniEstadoAsistencia.wkniDe(wkniV); if (wkniE != null) wkniM.merge(wkniE, 1, Integer::sum); }
        return wkniM;
    }

    /** % de asistencia de la clase en una fecha (Presente + Tardanza sobre los registrados); NaN sin registros. */
    public double wkniAsistenciaDelDia(LocalDate wkni_fecha) {
        int wkniTotal = 0, wkniAsistio = 0;
        for (TreeMap<LocalDate, String> wkniH : wkniHistorial.values()) {
            wkniEstadoAsistencia wkniE = wkniEstadoAsistencia.wkniDe(wkniH.get(wkni_fecha));
            if (wkniE == null) continue;
            wkniTotal++;
            if (wkniE.wkniAsistio()) wkniAsistio++;
        }
        return wkniTotal == 0 ? Double.NaN : 100.0 * wkniAsistio / wkniTotal;
    }

    /** Promedio de los % de asistencia de los estudiantes con registros. */
    public double wkniAsistenciaPromedio() {
        return wkniEstudiantes.stream().mapToDouble(wkniE -> wkniEvaluacion(wkniE.wkniId()).wkniPorcentajeAsistencia())
            .filter(wkniD -> !Double.isNaN(wkniD)).average().orElse(Double.NaN);
    }

    /** Promedio de notas (calificación o promedio de trabajos) de quienes tienen nota. */
    public double wkniPromedioNotas() {
        return wkniEstudiantes.stream().map(wkniE -> wkniEvaluacion(wkniE.wkniId()).wkni_nota()).filter(wkniN -> wkniN != null)
            .mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    /** Promedio de notas de trabajos de un estudiante (NaN si no tiene). */
    public double wkniPromedioTrabajos(int wkniEstudianteId) {
        return wkniEvaluables.stream().map(wkniA -> wkniEntrega(wkniA.wkniId(), wkniEstudianteId)).filter(wkniEn -> wkniEn != null && wkniEn.wkni_nota() != null)
            .mapToDouble(wkniEntrega::wkni_nota).average().orElse(Double.NaN);
    }
}

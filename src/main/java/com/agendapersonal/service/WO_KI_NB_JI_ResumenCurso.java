package com.agendapersonal.service;

import com.agendapersonal.domain.model.WO_KI_NB_JI_ActividadClase;
import com.agendapersonal.domain.model.WO_KI_NB_JI_ClaseDocente;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Entrega;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstadoAsistencia;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstadoEntrega;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstudianteClase;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_ClaseDocenteDAO;

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
public final class WO_KI_NB_JI_ResumenCurso {
    private final WO_KI_NB_JI_ClaseDocente clase;
    private final List<WO_KI_NB_JI_EstudianteClase> estudiantes;
    private final Map<Integer, TreeMap<LocalDate, String>> historial;
    private final List<LocalDate> fechas;
    private final List<WO_KI_NB_JI_ActividadClase> evaluables;
    private final Map<Integer, Map<Integer, WO_KI_NB_JI_Entrega>> entregas;
    private final Map<Integer, WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Evaluacion> evaluaciones = new HashMap<>();

    public WO_KI_NB_JI_ResumenCurso(WO_KI_NB_JI_ClaseDocente clase, WO_KI_NB_JI_ClaseDocenteDAO dao, LocalDate hoy) {
        this.clase = clase;
        this.estudiantes = dao.WO_KI_NB_JI_listarEstudiantes(clase.id());
        this.historial = dao.WO_KI_NB_JI_historialAsistencia(clase.id());
        this.fechas = dao.WO_KI_NB_JI_fechasAsistencia(clase.id());
        this.evaluables = dao.WO_KI_NB_JI_listarEvaluables(clase.id());
        this.entregas = dao.WO_KI_NB_JI_entregas(clase.id());
        for (WO_KI_NB_JI_EstudianteClase e : estudiantes) {
            List<WO_KI_NB_JI_EstadoEntrega> vencidas = new ArrayList<>();
            List<Double> notas = new ArrayList<>();
            for (WO_KI_NB_JI_ActividadClase a : evaluables) {
                WO_KI_NB_JI_Entrega en = WO_KI_NB_JI_entrega(a.id(), e.id());
                if (en != null && en.nota() != null) notas.add(en.nota());
                if (a.fecha() != null && !a.fecha().isAfter(hoy)) vencidas.add(en == null ? null : en.estado());
            }
            evaluaciones.put(e.id(), WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_evaluar(WO_KI_NB_JI_historial(e.id()), vencidas, notas,
                e.calificacion(), clase.notaMinima()));
        }
    }

    public WO_KI_NB_JI_ClaseDocente WO_KI_NB_JI_clase() { return clase; }
    public List<WO_KI_NB_JI_EstudianteClase> WO_KI_NB_JI_estudiantes() { return estudiantes; }
    public List<LocalDate> WO_KI_NB_JI_fechas() { return fechas; }
    public List<WO_KI_NB_JI_ActividadClase> WO_KI_NB_JI_evaluables() { return evaluables; }

    public TreeMap<LocalDate, String> WO_KI_NB_JI_historial(int estudianteId) {
        return historial.getOrDefault(estudianteId, new TreeMap<>());
    }

    public WO_KI_NB_JI_Entrega WO_KI_NB_JI_entrega(int actividadId, int estudianteId) {
        Map<Integer, WO_KI_NB_JI_Entrega> m = entregas.get(actividadId);
        return m == null ? null : m.get(estudianteId);
    }

    public WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Evaluacion WO_KI_NB_JI_evaluacion(int estudianteId) { return evaluaciones.get(estudianteId); }

    /** Estudiantes con alguna alerta, primero los de mayor riesgo. */
    public List<WO_KI_NB_JI_EstudianteClase> WO_KI_NB_JI_conAlertas() {
        return estudiantes.stream().filter(e -> WO_KI_NB_JI_evaluacion(e.id()).nivel() != WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel.BIEN)
            .sorted(Comparator.comparing((WO_KI_NB_JI_EstudianteClase e) -> -WO_KI_NB_JI_evaluacion(e.id()).nivel().ordinal())
                .thenComparing(WO_KI_NB_JI_EstudianteClase::nombre, String.CASE_INSENSITIVE_ORDER))
            .toList();
    }

    public long WO_KI_NB_JI_contar(WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel nivel) {
        return estudiantes.stream().filter(e -> WO_KI_NB_JI_evaluacion(e.id()).nivel() == nivel).count();
    }

    /** Estudiantes sin trabajos pendientes (ningún "No entregado"). */
    public long WO_KI_NB_JI_alDia() { return estudiantes.stream().filter(e -> WO_KI_NB_JI_evaluacion(e.id()).alDia()).count(); }

    /** Cuántos registros de cada estado hay en todo el curso. */
    public Map<WO_KI_NB_JI_EstadoAsistencia, Integer> WO_KI_NB_JI_distribucionAsistencia() {
        Map<WO_KI_NB_JI_EstadoAsistencia, Integer> m = new EnumMap<>(WO_KI_NB_JI_EstadoAsistencia.class);
        for (WO_KI_NB_JI_EstadoAsistencia e : WO_KI_NB_JI_EstadoAsistencia.values()) m.put(e, 0);
        for (TreeMap<LocalDate, String> h : historial.values())
            for (String v : h.values()) { WO_KI_NB_JI_EstadoAsistencia e = WO_KI_NB_JI_EstadoAsistencia.WO_KI_NB_JI_de(v); if (e != null) m.merge(e, 1, Integer::sum); }
        return m;
    }

    /** % de asistencia de la clase en una fecha (Presente + Tardanza sobre los registrados); NaN sin registros. */
    public double WO_KI_NB_JI_asistenciaDelDia(LocalDate fecha) {
        int total = 0, asistio = 0;
        for (TreeMap<LocalDate, String> h : historial.values()) {
            WO_KI_NB_JI_EstadoAsistencia e = WO_KI_NB_JI_EstadoAsistencia.WO_KI_NB_JI_de(h.get(fecha));
            if (e == null) continue;
            total++;
            if (e.WO_KI_NB_JI_asistio()) asistio++;
        }
        return total == 0 ? Double.NaN : 100.0 * asistio / total;
    }

    /** Promedio de los % de asistencia de los estudiantes con registros. */
    public double WO_KI_NB_JI_asistenciaPromedio() {
        return estudiantes.stream().mapToDouble(e -> WO_KI_NB_JI_evaluacion(e.id()).porcentajeAsistencia())
            .filter(d -> !Double.isNaN(d)).average().orElse(Double.NaN);
    }

    /** Promedio de notas (calificación o promedio de trabajos) de quienes tienen nota. */
    public double WO_KI_NB_JI_promedioNotas() {
        return estudiantes.stream().map(e -> WO_KI_NB_JI_evaluacion(e.id()).nota()).filter(n -> n != null)
            .mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
    }

    /** Promedio de notas de trabajos de un estudiante (NaN si no tiene). */
    public double WO_KI_NB_JI_promedioTrabajos(int estudianteId) {
        return evaluables.stream().map(a -> WO_KI_NB_JI_entrega(a.id(), estudianteId)).filter(en -> en != null && en.nota() != null)
            .mapToDouble(WO_KI_NB_JI_Entrega::nota).average().orElse(Double.NaN);
    }
}

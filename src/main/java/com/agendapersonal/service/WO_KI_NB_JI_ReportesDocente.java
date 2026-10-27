package com.agendapersonal.service;

import com.agendapersonal.domain.model.WO_KI_NB_JI_ActividadClase;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Entrega;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstadoAsistencia;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstadoEntrega;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstudianteClase;
import com.agendapersonal.service.WO_KI_NB_JI_TablaReporte.WO_KI_NB_JI_Celda;
import com.agendapersonal.service.WO_KI_NB_JI_TablaReporte.WO_KI_NB_JI_Tono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/** Arma las tablas de asistencia, trabajos y riesgo de un curso para exportarlas o imprimirlas. */
public final class WO_KI_NB_JI_ReportesDocente {
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private WO_KI_NB_JI_ReportesDocente() {}

    public static WO_KI_NB_JI_Tono WO_KI_NB_JI_tonoDe(WO_KI_NB_JI_EstadoAsistencia e) {
        if (e == null) return WO_KI_NB_JI_Tono.NINGUNO;
        return switch (e) {
            case PRESENTE -> WO_KI_NB_JI_Tono.VERDE; case FALTA -> WO_KI_NB_JI_Tono.ROJO;
            case TARDANZA -> WO_KI_NB_JI_Tono.AMARILLO; case JUSTIFICADA -> WO_KI_NB_JI_Tono.AZUL;
        };
    }

    public static WO_KI_NB_JI_Tono WO_KI_NB_JI_tonoDe(WO_KI_NB_JI_EstadoEntrega e) {
        if (e == null) return WO_KI_NB_JI_Tono.NINGUNO;
        return switch (e) { case ENTREGADO -> WO_KI_NB_JI_Tono.VERDE; case TARDE -> WO_KI_NB_JI_Tono.AMARILLO; case NO_ENTREGADO -> WO_KI_NB_JI_Tono.ROJO; };
    }

    public static WO_KI_NB_JI_Tono WO_KI_NB_JI_tonoDe(WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel n) {
        return switch (n) { case BIEN -> WO_KI_NB_JI_Tono.VERDE; case ATENCION -> WO_KI_NB_JI_Tono.AMARILLO; case ALTO -> WO_KI_NB_JI_Tono.ROJO; };
    }

    private static String WO_KI_NB_JI_curso(WO_KI_NB_JI_ResumenCurso r) {
        String s = r.WO_KI_NB_JI_clase().toString();
        return r.WO_KI_NB_JI_clase().periodo() == null ? s : s + " · " + r.WO_KI_NB_JI_clase().periodo();
    }

    /** Registro de un día: estado de cada estudiante y su % acumulado. */
    public static WO_KI_NB_JI_TablaReporte WO_KI_NB_JI_asistenciaDia(WO_KI_NB_JI_ResumenCurso r, LocalDate fecha) {
        WO_KI_NB_JI_TablaReporte t = WO_KI_NB_JI_TablaReporte.WO_KI_NB_JI_nueva("Asistencia " + fecha.format(DateTimeFormatter.ofPattern("dd-MM")),
            "Asistencia del " + fecha.format(FECHA) + " — " + WO_KI_NB_JI_curso(r), WO_KI_NB_JI_resumenDia(r, fecha),
            List.of("Estudiante", "Estado", "Asistencia acumulada", "Faltas", "Tardanzas", "Alerta"));
        for (WO_KI_NB_JI_EstudianteClase e : r.WO_KI_NB_JI_estudiantes()) {
            WO_KI_NB_JI_EstadoAsistencia st = WO_KI_NB_JI_EstadoAsistencia.WO_KI_NB_JI_de(r.WO_KI_NB_JI_historial(e.id()).get(fecha));
            WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Evaluacion ev = r.WO_KI_NB_JI_evaluacion(e.id());
            t.WO_KI_NB_JI_fila(List.of(WO_KI_NB_JI_Celda.WO_KI_NB_JI_texto(e.nombre()),
                WO_KI_NB_JI_Celda.WO_KI_NB_JI_texto(st == null ? "Sin registro" : st.WO_KI_NB_JI_etiqueta(), st == null ? WO_KI_NB_JI_Tono.GRIS : WO_KI_NB_JI_tonoDe(st)),
                WO_KI_NB_JI_Celda.WO_KI_NB_JI_texto(WO_KI_NB_JI_pct(ev.porcentajeAsistencia())),
                WO_KI_NB_JI_Celda.WO_KI_NB_JI_numero((double) ev.faltas()), WO_KI_NB_JI_Celda.WO_KI_NB_JI_numero((double) ev.tardanzas()),
                WO_KI_NB_JI_Celda.WO_KI_NB_JI_texto(ev.nivel() == WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel.BIEN ? "" : ev.WO_KI_NB_JI_resumen(), WO_KI_NB_JI_tonoAlerta(ev))));
        }
        return t;
    }

    /** Matriz estudiantes × fechas (P/F/T/J con colores) más totales, para el rango indicado. */
    public static WO_KI_NB_JI_TablaReporte WO_KI_NB_JI_asistenciaMatriz(WO_KI_NB_JI_ResumenCurso r, LocalDate desde, LocalDate hasta, List<LocalDate> extra) {
        List<LocalDate> fechas = new ArrayList<>();
        for (LocalDate f : r.WO_KI_NB_JI_fechas()) if (!f.isBefore(desde) && !f.isAfter(hasta)) fechas.add(f);
        if (extra != null) for (LocalDate f : extra) if (!fechas.contains(f)) fechas.add(f);
        fechas.sort(null);
        List<String> enc = new ArrayList<>(List.of("Estudiante"));
        for (LocalDate f : fechas) enc.add(f.format(DIA));
        enc.addAll(List.of("P", "F", "T", "J", "% Asist."));
        WO_KI_NB_JI_TablaReporte t = WO_KI_NB_JI_TablaReporte.WO_KI_NB_JI_nueva("Registro de asistencia",
            "Registro de asistencia — " + WO_KI_NB_JI_curso(r),
            "Del " + desde.format(FECHA) + " al " + hasta.format(FECHA) + " · P = Presente, F = Falta, T = Tardanza, J = Justificada", enc);
        for (WO_KI_NB_JI_EstudianteClase e : r.WO_KI_NB_JI_estudiantes()) {
            TreeMap<LocalDate, String> h = r.WO_KI_NB_JI_historial(e.id());
            List<WO_KI_NB_JI_Celda> fila = new ArrayList<>(List.of(WO_KI_NB_JI_Celda.WO_KI_NB_JI_texto(e.nombre())));
            int[] n = new int[4];
            for (LocalDate f : fechas) {
                WO_KI_NB_JI_EstadoAsistencia st = WO_KI_NB_JI_EstadoAsistencia.WO_KI_NB_JI_de(h.get(f));
                if (st != null) n[st.ordinal()]++;
                fila.add(WO_KI_NB_JI_Celda.WO_KI_NB_JI_texto(st == null ? "" : st.WO_KI_NB_JI_letra(), WO_KI_NB_JI_tonoDe(st)));
            }
            int total = n[0] + n[1] + n[2] + n[3];
            double p = total == 0 ? Double.NaN : 100.0 * (n[0] + n[2]) / total;
            for (int v : n) fila.add(WO_KI_NB_JI_Celda.WO_KI_NB_JI_numero((double) v));
            fila.add(WO_KI_NB_JI_Celda.WO_KI_NB_JI_texto(WO_KI_NB_JI_pct(p), Double.isNaN(p) ? WO_KI_NB_JI_Tono.NINGUNO
                : p < WO_KI_NB_JI_RiesgoEstudianteService.ASISTENCIA_MINIMA ? WO_KI_NB_JI_Tono.ROJO
                : p < WO_KI_NB_JI_RiesgoEstudianteService.ASISTENCIA_ATENCION ? WO_KI_NB_JI_Tono.AMARILLO : WO_KI_NB_JI_Tono.VERDE));
            t.WO_KI_NB_JI_fila(fila);
        }
        return t;
    }

    /** Libro de trabajos: estado y nota de cada trabajo vencido o programado. */
    public static WO_KI_NB_JI_TablaReporte WO_KI_NB_JI_trabajos(WO_KI_NB_JI_ResumenCurso r) {
        List<String> enc = new ArrayList<>(List.of("Estudiante"));
        for (WO_KI_NB_JI_ActividadClase a : r.WO_KI_NB_JI_evaluables()) enc.add(a.titulo());
        enc.addAll(List.of("Promedio trabajos", "Nota del curso", "Situación"));
        WO_KI_NB_JI_TablaReporte t = WO_KI_NB_JI_TablaReporte.WO_KI_NB_JI_nueva("Trabajos y notas", "Trabajos y notas — " + WO_KI_NB_JI_curso(r),
            "✓ entregado · ⏱ tarde · ✗ no entregado · nota mínima aprobatoria " + WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(r.WO_KI_NB_JI_clase().notaMinima()), enc);
        for (WO_KI_NB_JI_EstudianteClase e : r.WO_KI_NB_JI_estudiantes()) {
            List<WO_KI_NB_JI_Celda> fila = new ArrayList<>(List.of(WO_KI_NB_JI_Celda.WO_KI_NB_JI_texto(e.nombre())));
            for (WO_KI_NB_JI_ActividadClase a : r.WO_KI_NB_JI_evaluables()) {
                WO_KI_NB_JI_Entrega en = r.WO_KI_NB_JI_entrega(a.id(), e.id());
                if (en == null) { fila.add(WO_KI_NB_JI_Celda.WO_KI_NB_JI_texto("")); continue; }
                String txt = en.estado().WO_KI_NB_JI_simbolo() + (en.nota() == null ? "" : " " + WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(en.nota()));
                fila.add(WO_KI_NB_JI_Celda.WO_KI_NB_JI_texto(txt, WO_KI_NB_JI_tonoDe(en.estado())));
            }
            double prom = r.WO_KI_NB_JI_promedioTrabajos(e.id());
            fila.add(WO_KI_NB_JI_Celda.WO_KI_NB_JI_numero(Double.isNaN(prom) ? null : prom, WO_KI_NB_JI_tonoNota(prom, r.WO_KI_NB_JI_clase().notaMinima())));
            fila.add(WO_KI_NB_JI_Celda.WO_KI_NB_JI_numero(e.calificacion(), e.calificacion() == null ? WO_KI_NB_JI_Tono.NINGUNO : WO_KI_NB_JI_tonoNota(e.calificacion(), r.WO_KI_NB_JI_clase().notaMinima())));
            WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Evaluacion ev = r.WO_KI_NB_JI_evaluacion(e.id());
            fila.add(WO_KI_NB_JI_Celda.WO_KI_NB_JI_texto(ev.alDia() ? "Al día" : ev.noEntregados() + " pendiente(s)", ev.alDia() ? WO_KI_NB_JI_Tono.VERDE : WO_KI_NB_JI_Tono.ROJO));
            t.WO_KI_NB_JI_fila(fila);
        }
        return t;
    }

    /** Semáforo de riesgo con los motivos. */
    public static WO_KI_NB_JI_TablaReporte WO_KI_NB_JI_riesgo(WO_KI_NB_JI_ResumenCurso r) {
        WO_KI_NB_JI_TablaReporte t = WO_KI_NB_JI_TablaReporte.WO_KI_NB_JI_nueva("Semáforo de riesgo", "Semáforo de riesgo — " + WO_KI_NB_JI_curso(r),
            "Generado el " + LocalDate.now().format(FECHA) + " cruzando asistencia, entregas y notas",
            List.of("Estudiante", "Nivel", "Asistencia", "Faltas seguidas", "No entregados", "Nota", "Motivos"));
        List<WO_KI_NB_JI_EstudianteClase> orden = new ArrayList<>(r.WO_KI_NB_JI_conAlertas());
        for (WO_KI_NB_JI_EstudianteClase e : r.WO_KI_NB_JI_estudiantes()) if (!orden.contains(e)) orden.add(e);
        for (WO_KI_NB_JI_EstudianteClase e : orden) {
            WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Evaluacion ev = r.WO_KI_NB_JI_evaluacion(e.id());
            t.WO_KI_NB_JI_fila(List.of(WO_KI_NB_JI_Celda.WO_KI_NB_JI_texto(e.nombre()), WO_KI_NB_JI_Celda.WO_KI_NB_JI_texto(ev.nivel().WO_KI_NB_JI_etiqueta(), WO_KI_NB_JI_tonoDe(ev.nivel())),
                WO_KI_NB_JI_Celda.WO_KI_NB_JI_texto(WO_KI_NB_JI_pct(ev.porcentajeAsistencia())), WO_KI_NB_JI_Celda.WO_KI_NB_JI_numero((double) ev.faltasConsecutivas()),
                WO_KI_NB_JI_Celda.WO_KI_NB_JI_numero((double) ev.noEntregados()), WO_KI_NB_JI_Celda.WO_KI_NB_JI_numero(ev.nota()), WO_KI_NB_JI_Celda.WO_KI_NB_JI_texto(ev.WO_KI_NB_JI_resumen())));
        }
        return t;
    }

    public static String WO_KI_NB_JI_resumenDia(WO_KI_NB_JI_ResumenCurso r, LocalDate fecha) {
        int[] n = new int[4];
        int sin = 0;
        for (WO_KI_NB_JI_EstudianteClase e : r.WO_KI_NB_JI_estudiantes()) {
            WO_KI_NB_JI_EstadoAsistencia st = WO_KI_NB_JI_EstadoAsistencia.WO_KI_NB_JI_de(r.WO_KI_NB_JI_historial(e.id()).get(fecha));
            if (st == null) sin++; else n[st.ordinal()]++;
        }
        return n[0] + " presentes · " + n[1] + " faltas · " + n[2] + " tardanzas · " + n[3] + " justificadas"
            + (sin > 0 ? " · " + sin + " sin registro" : "");
    }

    private static WO_KI_NB_JI_Tono WO_KI_NB_JI_tonoAlerta(WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Evaluacion ev) {
        return ev.nivel() == WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel.BIEN ? WO_KI_NB_JI_Tono.NINGUNO : WO_KI_NB_JI_tonoDe(ev.nivel());
    }

    private static WO_KI_NB_JI_Tono WO_KI_NB_JI_tonoNota(double nota, double minima) {
        if (Double.isNaN(nota)) return WO_KI_NB_JI_Tono.NINGUNO;
        return nota < minima ? WO_KI_NB_JI_Tono.ROJO : WO_KI_NB_JI_Tono.VERDE;
    }

    public static String WO_KI_NB_JI_pct(double p) { return Double.isNaN(p) ? "—" : Math.round(p) + " %"; }
}

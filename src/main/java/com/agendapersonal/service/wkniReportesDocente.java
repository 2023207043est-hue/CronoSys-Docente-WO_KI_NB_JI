package com.agendapersonal.service;

import com.agendapersonal.domain.model.wkniActividadClase;
import com.agendapersonal.domain.model.wkniEntrega;
import com.agendapersonal.domain.model.wkniEstadoAsistencia;
import com.agendapersonal.domain.model.wkniEstadoEntrega;
import com.agendapersonal.domain.model.wkniEstudianteClase;
import com.agendapersonal.service.wkniTablaReporte.wkniCelda;
import com.agendapersonal.service.wkniTablaReporte.wkniTono;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

/** Arma las tablas de asistencia, trabajos y riesgo de un curso para exportarlas o imprimirlas. */
public final class wkniReportesDocente {
    private static final DateTimeFormatter wkniDIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter wkniFECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private wkniReportesDocente() {}

    public static wkniTono wkniTonoDe(wkniEstadoAsistencia wkniE) {
        if (wkniE == null) return wkniTono.NINGUNO;
        return switch (wkniE) {
            case PRESENTE -> wkniTono.VERDE; case FALTA -> wkniTono.ROJO;
            case TARDANZA -> wkniTono.AMARILLO; case JUSTIFICADA -> wkniTono.AZUL;
        };
    }

    public static wkniTono wkniTonoDe(wkniEstadoEntrega wkniE) {
        if (wkniE == null) return wkniTono.NINGUNO;
        return switch (wkniE) { case ENTREGADO -> wkniTono.VERDE; case TARDE -> wkniTono.AMARILLO; case NO_ENTREGADO -> wkniTono.ROJO; };
    }

    public static wkniTono wkniTonoDe(wkniRiesgoEstudianteService.wkniNivel wkniN) {
        return switch (wkniN) { case BIEN -> wkniTono.VERDE; case ATENCION -> wkniTono.AMARILLO; case ALTO -> wkniTono.ROJO; };
    }

    private static String wkniCurso(wkniResumenCurso wkniR) {
        String wkniS = wkniR.wkniClase().toString();
        return wkniR.wkniClase().wkniPeriodo() == null ? wkniS : wkniS + " · " + wkniR.wkniClase().wkniPeriodo();
    }

    /** Registro de un día: estado de cada estudiante y su % acumulado. */
    public static wkniTablaReporte wkniAsistenciaDia(wkniResumenCurso wkniR, LocalDate wkni_fecha) {
        wkniTablaReporte wkniT = wkniTablaReporte.wkniNueva("Asistencia " + wkni_fecha.format(DateTimeFormatter.ofPattern("dd-MM")),
            "Asistencia del " + wkni_fecha.format(wkniFECHA) + " — " + wkniCurso(wkniR), wkniResumenDia(wkniR, wkni_fecha),
            List.of("Estudiante", "Estado", "Asistencia acumulada", "Faltas", "Tardanzas", "Alerta"));
        for (wkniEstudianteClase wkniE : wkniR.wkniEstudiantes()) {
            wkniEstadoAsistencia wkniSt = wkniEstadoAsistencia.wkniDe(wkniR.wkniHistorial(wkniE.wkniId()).get(wkni_fecha));
            wkniRiesgoEstudianteService.wkniEvaluacion wkniEv = wkniR.wkniEvaluacion(wkniE.wkniId());
            wkniT.wkniFila(List.of(wkniCelda.wkniTexto(wkniE.wkniNombre()),
                wkniCelda.wkniTexto(wkniSt == null ? "Sin registro" : wkniSt.wkniEtiqueta(), wkniSt == null ? wkniTono.GRIS : wkniTonoDe(wkniSt)),
                wkniCelda.wkniTexto(wkniPct(wkniEv.wkniPorcentajeAsistencia())),
                wkniCelda.wkniNumero((double) wkniEv.wkniFaltas()), wkniCelda.wkniNumero((double) wkniEv.wkniTardanzas()),
                wkniCelda.wkniTexto(wkniEv.wkni_nivel() == wkniRiesgoEstudianteService.wkniNivel.BIEN ? "" : wkniEv.wkniResumen(), wkniTonoAlerta(wkniEv))));
        }
        return wkniT;
    }

    /** Matriz estudiantes × fechas (P/F/T/J con colores) más totales, para el rango indicado. */
    public static wkniTablaReporte wkniAsistenciaMatriz(wkniResumenCurso wkniR, LocalDate wkniDesde, LocalDate wkniHasta, List<LocalDate> wkniExtra) {
        List<LocalDate> wkniFechas = new ArrayList<>();
        for (LocalDate wkniF : wkniR.wkniFechas()) if (!wkniF.isBefore(wkniDesde) && !wkniF.isAfter(wkniHasta)) wkniFechas.add(wkniF);
        if (wkniExtra != null) for (LocalDate wkniF : wkniExtra) if (!wkniFechas.contains(wkniF)) wkniFechas.add(wkniF);
        wkniFechas.sort(null);
        List<String> wkniEnc = new ArrayList<>(List.of("Estudiante"));
        for (LocalDate wkniF : wkniFechas) wkniEnc.add(wkniF.format(wkniDIA));
        wkniEnc.addAll(List.of("P", "F", "T", "J", "% Asist."));
        wkniTablaReporte wkniT = wkniTablaReporte.wkniNueva("Registro de asistencia",
            "Registro de asistencia — " + wkniCurso(wkniR),
            "Del " + wkniDesde.format(wkniFECHA) + " al " + wkniHasta.format(wkniFECHA) + " · P = Presente, F = Falta, T = Tardanza, J = Justificada", wkniEnc);
        for (wkniEstudianteClase wkniE : wkniR.wkniEstudiantes()) {
            TreeMap<LocalDate, String> wkniH = wkniR.wkniHistorial(wkniE.wkniId());
            List<wkniCelda> wkniFila = new ArrayList<>(List.of(wkniCelda.wkniTexto(wkniE.wkniNombre())));
            int[] wkniN = new int[4];
            for (LocalDate wkniF : wkniFechas) {
                wkniEstadoAsistencia wkniSt = wkniEstadoAsistencia.wkniDe(wkniH.get(wkniF));
                if (wkniSt != null) wkniN[wkniSt.ordinal()]++;
                wkniFila.add(wkniCelda.wkniTexto(wkniSt == null ? "" : wkniSt.wkniLetra(), wkniTonoDe(wkniSt)));
            }
            int wkniTotal = wkniN[0] + wkniN[1] + wkniN[2] + wkniN[3];
            double wkni_p = wkniTotal == 0 ? Double.NaN : 100.0 * (wkniN[0] + wkniN[2]) / wkniTotal;
            for (int wkniV : wkniN) wkniFila.add(wkniCelda.wkniNumero((double) wkniV));
            wkniFila.add(wkniCelda.wkniTexto(wkniPct(wkni_p), Double.isNaN(wkni_p) ? wkniTono.NINGUNO
                : wkni_p < wkniRiesgoEstudianteService.wkniASISTENCIA_MINIMA ? wkniTono.ROJO
                : wkni_p < wkniRiesgoEstudianteService.wkniASISTENCIA_ATENCION ? wkniTono.AMARILLO : wkniTono.VERDE));
            wkniT.wkniFila(wkniFila);
        }
        return wkniT;
    }

    /** Libro de trabajos: estado y nota de cada trabajo vencido o programado. */
    public static wkniTablaReporte wkniTrabajos(wkniResumenCurso wkniR) {
        List<String> wkniEnc = new ArrayList<>(List.of("Estudiante"));
        for (wkniActividadClase wkniA : wkniR.wkniEvaluables()) wkniEnc.add(wkniA.wkniTitulo());
        wkniEnc.addAll(List.of("Promedio trabajos", "Nota del curso", "Situación"));
        wkniTablaReporte wkniT = wkniTablaReporte.wkniNueva("Trabajos y notas", "Trabajos y notas — " + wkniCurso(wkniR),
            "✓ entregado · ⏱ tarde · ✗ no entregado · nota mínima aprobatoria " + wkniRiesgoEstudianteService.wkniNumero(wkniR.wkniClase().wkniNotaMinima()), wkniEnc);
        for (wkniEstudianteClase wkniE : wkniR.wkniEstudiantes()) {
            List<wkniCelda> wkniFila = new ArrayList<>(List.of(wkniCelda.wkniTexto(wkniE.wkniNombre())));
            for (wkniActividadClase wkniA : wkniR.wkniEvaluables()) {
                wkniEntrega wkniEn = wkniR.wkniEntrega(wkniA.wkniId(), wkniE.wkniId());
                if (wkniEn == null) { wkniFila.add(wkniCelda.wkniTexto("")); continue; }
                String wkniTxt = wkniEn.wkniEstado().wkniSimbolo() + (wkniEn.wkni_nota() == null ? "" : " " + wkniRiesgoEstudianteService.wkniNumero(wkniEn.wkni_nota()));
                wkniFila.add(wkniCelda.wkniTexto(wkniTxt, wkniTonoDe(wkniEn.wkniEstado())));
            }
            double wkniProm = wkniR.wkniPromedioTrabajos(wkniE.wkniId());
            wkniFila.add(wkniCelda.wkniNumero(Double.isNaN(wkniProm) ? null : wkniProm, wkniTonoNota(wkniProm, wkniR.wkniClase().wkniNotaMinima())));
            wkniFila.add(wkniCelda.wkniNumero(wkniE.wkniCalificacion(), wkniE.wkniCalificacion() == null ? wkniTono.NINGUNO : wkniTonoNota(wkniE.wkniCalificacion(), wkniR.wkniClase().wkniNotaMinima())));
            wkniRiesgoEstudianteService.wkniEvaluacion wkniEv = wkniR.wkniEvaluacion(wkniE.wkniId());
            wkniFila.add(wkniCelda.wkniTexto(wkniEv.wkniAlDia() ? "Al día" : wkniEv.wkniNoEntregados() + " pendiente(s)", wkniEv.wkniAlDia() ? wkniTono.VERDE : wkniTono.ROJO));
            wkniT.wkniFila(wkniFila);
        }
        return wkniT;
    }

    /** Semáforo de riesgo con los motivos. */
    public static wkniTablaReporte wkniRiesgo(wkniResumenCurso wkniR) {
        wkniTablaReporte wkniT = wkniTablaReporte.wkniNueva("Semáforo de riesgo", "Semáforo de riesgo — " + wkniCurso(wkniR),
            "Generado el " + LocalDate.now().format(wkniFECHA) + " cruzando asistencia, entregas y notas",
            List.of("Estudiante", "Nivel", "Asistencia", "Faltas seguidas", "No entregados", "Nota", "Motivos"));
        List<wkniEstudianteClase> wkniOrden = new ArrayList<>(wkniR.wkniConAlertas());
        for (wkniEstudianteClase wkniE : wkniR.wkniEstudiantes()) if (!wkniOrden.contains(wkniE)) wkniOrden.add(wkniE);
        for (wkniEstudianteClase wkniE : wkniOrden) {
            wkniRiesgoEstudianteService.wkniEvaluacion wkniEv = wkniR.wkniEvaluacion(wkniE.wkniId());
            wkniT.wkniFila(List.of(wkniCelda.wkniTexto(wkniE.wkniNombre()), wkniCelda.wkniTexto(wkniEv.wkni_nivel().wkniEtiqueta(), wkniTonoDe(wkniEv.wkni_nivel())),
                wkniCelda.wkniTexto(wkniPct(wkniEv.wkniPorcentajeAsistencia())), wkniCelda.wkniNumero((double) wkniEv.wkniFaltasConsecutivas()),
                wkniCelda.wkniNumero((double) wkniEv.wkniNoEntregados()), wkniCelda.wkniNumero(wkniEv.wkni_nota()), wkniCelda.wkniTexto(wkniEv.wkniResumen())));
        }
        return wkniT;
    }

    public static String wkniResumenDia(wkniResumenCurso wkniR, LocalDate wkni_fecha) {
        int[] wkniN = new int[4];
        int wkniSin = 0;
        for (wkniEstudianteClase wkniE : wkniR.wkniEstudiantes()) {
            wkniEstadoAsistencia wkniSt = wkniEstadoAsistencia.wkniDe(wkniR.wkniHistorial(wkniE.wkniId()).get(wkni_fecha));
            if (wkniSt == null) wkniSin++; else wkniN[wkniSt.ordinal()]++;
        }
        return wkniN[0] + " presentes · " + wkniN[1] + " faltas · " + wkniN[2] + " tardanzas · " + wkniN[3] + " justificadas"
            + (wkniSin > 0 ? " · " + wkniSin + " sin registro" : "");
    }

    private static wkniTono wkniTonoAlerta(wkniRiesgoEstudianteService.wkniEvaluacion wkniEv) {
        return wkniEv.wkni_nivel() == wkniRiesgoEstudianteService.wkniNivel.BIEN ? wkniTono.NINGUNO : wkniTonoDe(wkniEv.wkni_nivel());
    }

    private static wkniTono wkniTonoNota(double wkni_nota, double wkniMinima) {
        if (Double.isNaN(wkni_nota)) return wkniTono.NINGUNO;
        return wkni_nota < wkniMinima ? wkniTono.ROJO : wkniTono.VERDE;
    }

    public static String wkniPct(double wkni_p) { return Double.isNaN(wkni_p) ? "—" : Math.round(wkni_p) + " %"; }
}

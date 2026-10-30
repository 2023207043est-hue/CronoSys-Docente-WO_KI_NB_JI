package com.agendapersonal.service;

import com.agendapersonal.domain.model.wkniEstadoAsistencia;
import com.agendapersonal.domain.model.wkniEstadoEntrega;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Semáforo de riesgo por estudiante (sin UI ni BD): cruza el historial de asistencia, las entregas de
 * trabajos y la nota para avisar al docente con motivos concretos ("3 faltas consecutivas",
 * "2 trabajos no entregados") y detectar si el bajo rendimiento coincide con las inasistencias.
 */
public final class wkniRiesgoEstudianteService {
    /** Por debajo de este % de asistencia el estudiante está en riesgo (30 % de inasistencias inhabilita). */
    public static final double wkniASISTENCIA_MINIMA = 70;
    /** Por debajo de este % se pide atención. */
    public static final double wkniASISTENCIA_ATENCION = 85;
    /** Sesiones mínimas antes de juzgar el % (con 1–2 clases una sola falta distorsiona). */
    public static final int wkniSESIONES_MINIMAS = 3;

    public enum wkniNivel {
        BIEN("Al día"), ATENCION("Atención"), ALTO("En riesgo");
        private final String wkniEtiqueta;
        wkniNivel(String wkniEtiqueta) { this.wkniEtiqueta = wkniEtiqueta; }
        public String wkniEtiqueta() { return wkniEtiqueta; }
    }

    /** Resultado del análisis de un estudiante. */
    public record wkniEvaluacion(wkniNivel wkni_nivel, List<String> wkniMotivos, int wkniSesiones, double wkniPorcentajeAsistencia,
                             int wkniFaltas, int wkniTardanzas, int wkniFaltasConsecutivas, int wkniNoEntregados,
                             Double wkni_nota, boolean wkniAlDia) {
        public boolean wkniEnRiesgo() { return wkni_nivel == wkniNivel.ALTO; }
        /** Motivos en una sola línea, para tooltips y reportes. */
        public String wkniResumen() { return wkniMotivos.isEmpty() ? "Sin alertas" : String.join(" · ", wkniMotivos); }
    }

    private wkniRiesgoEstudianteService() {}

    /**
     * @param historial   fecha → estado guardado de asistencia (puede estar vacío)
     * @param entregas    estado de cada trabajo vencido (null = sin registrar, no cuenta como falta)
     * @param notasTrabajos notas de los trabajos calificados (para el promedio si no hay calificación)
     * @param calificacion nota del curso registrada por el docente (null si no hay)
     * @param notaMinima  nota aprobatoria del curso
     */
    public static wkniEvaluacion wkniEvaluar(NavigableMap<LocalDate, String> wkniHistorial, Collection<wkniEstadoEntrega> wkniEntregas,
                                     Collection<Double> wkniNotasTrabajos, Double wkniCalificacion, double wkniNotaMinima) {
        NavigableMap<LocalDate, String> wkniH = wkniHistorial == null ? new TreeMap<>() : wkniHistorial;
        int wkniSesiones = 0, wkniAsistio = 0, wkniFaltas = 0, wkniTardanzas = 0;
        for (String wkniV : wkniH.values()) {
            wkniEstadoAsistencia wkniE = wkniEstadoAsistencia.wkniDe(wkniV);
            if (wkniE == null) continue;
            wkniSesiones++;
            if (wkniE.wkniAsistio()) wkniAsistio++;
            if (wkniE == wkniEstadoAsistencia.FALTA) wkniFaltas++;
            if (wkniE == wkniEstadoAsistencia.TARDANZA) wkniTardanzas++;
        }
        double wkniPct = wkniSesiones == 0 ? Double.NaN : 100.0 * wkniAsistio / wkniSesiones;
        int wkniConsecutivas = wkniFaltasConsecutivas(wkniH);

        int wkniNoEntregados = 0;
        if (wkniEntregas != null) for (wkniEstadoEntrega wkniE : wkniEntregas) if (wkniE == wkniEstadoEntrega.NO_ENTREGADO) wkniNoEntregados++;

        Double wkni_nota = wkniCalificacion;
        if (wkni_nota == null && wkniNotasTrabajos != null && !wkniNotasTrabajos.isEmpty())
            wkni_nota = wkniNotasTrabajos.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
        if (wkni_nota != null && !Double.isFinite(wkni_nota)) wkni_nota = null;

        List<String> wkniMotivos = new ArrayList<>();
        wkniNivel wkni_nivel = wkniNivel.BIEN;

        if (wkniConsecutivas >= 3) { wkniMotivos.add(wkniConsecutivas + " faltas consecutivas"); wkni_nivel = wkniNivel.ALTO; }
        else if (wkniConsecutivas == 2) { wkniMotivos.add("2 faltas consecutivas"); wkni_nivel = wkniMax(wkni_nivel, wkniNivel.ATENCION); }

        boolean wkniAsistenciaBaja = wkniSesiones >= wkniSESIONES_MINIMAS && wkniPct < wkniASISTENCIA_ATENCION;
        if (wkniSesiones >= wkniSESIONES_MINIMAS && wkniPct < wkniASISTENCIA_MINIMA) {
            wkniMotivos.add("Asistencia de " + Math.round(wkniPct) + " % (mínimo " + Math.round(wkniASISTENCIA_MINIMA) + " %)");
            wkni_nivel = wkniNivel.ALTO;
        } else if (wkniAsistenciaBaja) {
            wkniMotivos.add("Asistencia de " + Math.round(wkniPct) + " %");
            wkni_nivel = wkniMax(wkni_nivel, wkniNivel.ATENCION);
        }
        if (wkniTardanzas >= 3) { wkniMotivos.add(wkniTardanzas + " tardanzas acumuladas"); wkni_nivel = wkniMax(wkni_nivel, wkniNivel.ATENCION); }

        if (wkniNoEntregados >= 2) { wkniMotivos.add(wkniNoEntregados + " trabajos no entregados"); wkni_nivel = wkniNivel.ALTO; }
        else if (wkniNoEntregados == 1) { wkniMotivos.add("1 trabajo no entregado"); wkni_nivel = wkniMax(wkni_nivel, wkniNivel.ATENCION); }

        // Caída reciente: antes asistía y en las últimas 3 clases casi no viene.
        String wkniCaida = wkniCaidaReciente(wkniH);
        if (wkniCaida != null) { wkniMotivos.add(wkniCaida); wkni_nivel = wkniMax(wkni_nivel, wkniNivel.ATENCION); }

        if (wkni_nota != null && wkni_nota < wkniNotaMinima) {
            wkniMotivos.add("Nota " + wkniNumero(wkni_nota) + " por debajo de " + wkniNumero(wkniNotaMinima));
            wkni_nivel = wkniMax(wkni_nivel, wkniNivel.ATENCION);
            // Cruce: el bajo rendimiento coincide con que dejó de ir a clases.
            if (wkniAsistenciaBaja || wkniConsecutivas >= 2 || wkniCaida != null) {
                wkniMotivos.add("El bajo rendimiento coincide con sus inasistencias");
                wkni_nivel = wkniNivel.ALTO;
            }
        }
        return new wkniEvaluacion(wkni_nivel, List.copyOf(wkniMotivos), wkniSesiones, wkniPct, wkniFaltas, wkniTardanzas, wkniConsecutivas,
            wkniNoEntregados, wkni_nota, wkniNoEntregados == 0);
    }

    /** Faltas seguidas contando desde la clase más reciente; las justificadas no cortan ni suman. */
    public static int wkniFaltasConsecutivas(NavigableMap<LocalDate, String> wkniHistorial) {
        int wkniN = 0;
        for (String wkniV : wkniHistorial.descendingMap().values()) {
            wkniEstadoAsistencia wkniE = wkniEstadoAsistencia.wkniDe(wkniV);
            if (wkniE == wkniEstadoAsistencia.JUSTIFICADA) continue;
            if (wkniE == wkniEstadoAsistencia.FALTA) wkniN++; else break;
        }
        return wkniN;
    }

    private static String wkniCaidaReciente(NavigableMap<LocalDate, String> wkniH) {
        if (wkniH.size() < 6) return null;
        List<wkniEstadoAsistencia> wkniOrden = wkniH.values().stream().map(wkniEstadoAsistencia::wkniDe).toList();
        List<wkniEstadoAsistencia> wkniRecientes = wkniOrden.subList(wkniOrden.size() - 3, wkniOrden.size());
        List<wkniEstadoAsistencia> wkniAntes = wkniOrden.subList(0, wkniOrden.size() - 3);
        double wkniPAntes = wkniPct(wkniAntes), wkniPAhora = wkniPct(wkniRecientes);
        if (wkniPAntes >= 80 && wkniPAhora <= 34)
            return "Dejó de asistir: " + Math.round(wkniPAntes) + " % antes y " + Math.round(wkniPAhora) + " % en las últimas 3 clases";
        return null;
    }

    private static double wkniPct(List<wkniEstadoAsistencia> wkniL) {
        long wkniValidos = wkniL.stream().filter(wkniE -> wkniE != null && wkniE != wkniEstadoAsistencia.JUSTIFICADA).count();
        if (wkniValidos == 0) return 100;
        return 100.0 * wkniL.stream().filter(wkniE -> wkniE != null && wkniE.wkniAsistio()).count() / wkniValidos;
    }

    private static wkniNivel wkniMax(wkniNivel wkniA, wkniNivel wkniB) { return wkniA.ordinal() >= wkniB.ordinal() ? wkniA : wkniB; }

    /** 9.5 → "9.5", 11.0 → "11". */
    public static String wkniNumero(double wkniV) {
        return wkniV == Math.rint(wkniV) ? Long.toString(Math.round(wkniV)) : String.format(Locale.ROOT, "%.1f", wkniV);
    }
}

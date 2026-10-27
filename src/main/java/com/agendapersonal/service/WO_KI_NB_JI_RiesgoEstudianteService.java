package com.agendapersonal.service;

import com.agendapersonal.domain.model.WO_KI_NB_JI_EstadoAsistencia;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstadoEntrega;

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
public final class WO_KI_NB_JI_RiesgoEstudianteService {
    /** Por debajo de este % de asistencia el estudiante está en riesgo (30 % de inasistencias inhabilita). */
    public static final double ASISTENCIA_MINIMA = 70;
    /** Por debajo de este % se pide atención. */
    public static final double ASISTENCIA_ATENCION = 85;
    /** Sesiones mínimas antes de juzgar el % (con 1–2 clases una sola falta distorsiona). */
    public static final int SESIONES_MINIMAS = 3;

    public enum WO_KI_NB_JI_Nivel {
        BIEN("Al día"), ATENCION("Atención"), ALTO("En riesgo");
        private final String etiqueta;
        WO_KI_NB_JI_Nivel(String etiqueta) { this.etiqueta = etiqueta; }
        public String WO_KI_NB_JI_etiqueta() { return etiqueta; }
    }

    /** Resultado del análisis de un estudiante. */
    public record WO_KI_NB_JI_Evaluacion(WO_KI_NB_JI_Nivel nivel, List<String> motivos, int sesiones, double porcentajeAsistencia,
                             int faltas, int tardanzas, int faltasConsecutivas, int noEntregados,
                             Double nota, boolean alDia) {
        public boolean WO_KI_NB_JI_enRiesgo() { return nivel == WO_KI_NB_JI_Nivel.ALTO; }
        /** Motivos en una sola línea, para tooltips y reportes. */
        public String WO_KI_NB_JI_resumen() { return motivos.isEmpty() ? "Sin alertas" : String.join(" · ", motivos); }
    }

    private WO_KI_NB_JI_RiesgoEstudianteService() {}

    /**
     * @param historial   fecha → estado guardado de asistencia (puede estar vacío)
     * @param entregas    estado de cada trabajo vencido (null = sin registrar, no cuenta como falta)
     * @param notasTrabajos notas de los trabajos calificados (para el promedio si no hay calificación)
     * @param calificacion nota del curso registrada por el docente (null si no hay)
     * @param notaMinima  nota aprobatoria del curso
     */
    public static WO_KI_NB_JI_Evaluacion WO_KI_NB_JI_evaluar(NavigableMap<LocalDate, String> historial, Collection<WO_KI_NB_JI_EstadoEntrega> entregas,
                                     Collection<Double> notasTrabajos, Double calificacion, double notaMinima) {
        NavigableMap<LocalDate, String> h = historial == null ? new TreeMap<>() : historial;
        int sesiones = 0, asistio = 0, faltas = 0, tardanzas = 0;
        for (String v : h.values()) {
            WO_KI_NB_JI_EstadoAsistencia e = WO_KI_NB_JI_EstadoAsistencia.WO_KI_NB_JI_de(v);
            if (e == null) continue;
            sesiones++;
            if (e.WO_KI_NB_JI_asistio()) asistio++;
            if (e == WO_KI_NB_JI_EstadoAsistencia.FALTA) faltas++;
            if (e == WO_KI_NB_JI_EstadoAsistencia.TARDANZA) tardanzas++;
        }
        double pct = sesiones == 0 ? Double.NaN : 100.0 * asistio / sesiones;
        int consecutivas = WO_KI_NB_JI_faltasConsecutivas(h);

        int noEntregados = 0;
        if (entregas != null) for (WO_KI_NB_JI_EstadoEntrega e : entregas) if (e == WO_KI_NB_JI_EstadoEntrega.NO_ENTREGADO) noEntregados++;

        Double nota = calificacion;
        if (nota == null && notasTrabajos != null && !notasTrabajos.isEmpty())
            nota = notasTrabajos.stream().mapToDouble(Double::doubleValue).average().orElse(Double.NaN);
        if (nota != null && !Double.isFinite(nota)) nota = null;

        List<String> motivos = new ArrayList<>();
        WO_KI_NB_JI_Nivel nivel = WO_KI_NB_JI_Nivel.BIEN;

        if (consecutivas >= 3) { motivos.add(consecutivas + " faltas consecutivas"); nivel = WO_KI_NB_JI_Nivel.ALTO; }
        else if (consecutivas == 2) { motivos.add("2 faltas consecutivas"); nivel = WO_KI_NB_JI_max(nivel, WO_KI_NB_JI_Nivel.ATENCION); }

        boolean asistenciaBaja = sesiones >= SESIONES_MINIMAS && pct < ASISTENCIA_ATENCION;
        if (sesiones >= SESIONES_MINIMAS && pct < ASISTENCIA_MINIMA) {
            motivos.add("Asistencia de " + Math.round(pct) + " % (mínimo " + Math.round(ASISTENCIA_MINIMA) + " %)");
            nivel = WO_KI_NB_JI_Nivel.ALTO;
        } else if (asistenciaBaja) {
            motivos.add("Asistencia de " + Math.round(pct) + " %");
            nivel = WO_KI_NB_JI_max(nivel, WO_KI_NB_JI_Nivel.ATENCION);
        }
        if (tardanzas >= 3) { motivos.add(tardanzas + " tardanzas acumuladas"); nivel = WO_KI_NB_JI_max(nivel, WO_KI_NB_JI_Nivel.ATENCION); }

        if (noEntregados >= 2) { motivos.add(noEntregados + " trabajos no entregados"); nivel = WO_KI_NB_JI_Nivel.ALTO; }
        else if (noEntregados == 1) { motivos.add("1 trabajo no entregado"); nivel = WO_KI_NB_JI_max(nivel, WO_KI_NB_JI_Nivel.ATENCION); }

        // Caída reciente: antes asistía y en las últimas 3 clases casi no viene.
        String caida = WO_KI_NB_JI_caidaReciente(h);
        if (caida != null) { motivos.add(caida); nivel = WO_KI_NB_JI_max(nivel, WO_KI_NB_JI_Nivel.ATENCION); }

        if (nota != null && nota < notaMinima) {
            motivos.add("Nota " + WO_KI_NB_JI_numero(nota) + " por debajo de " + WO_KI_NB_JI_numero(notaMinima));
            nivel = WO_KI_NB_JI_max(nivel, WO_KI_NB_JI_Nivel.ATENCION);
            // Cruce: el bajo rendimiento coincide con que dejó de ir a clases.
            if (asistenciaBaja || consecutivas >= 2 || caida != null) {
                motivos.add("El bajo rendimiento coincide con sus inasistencias");
                nivel = WO_KI_NB_JI_Nivel.ALTO;
            }
        }
        return new WO_KI_NB_JI_Evaluacion(nivel, List.copyOf(motivos), sesiones, pct, faltas, tardanzas, consecutivas,
            noEntregados, nota, noEntregados == 0);
    }

    /** Faltas seguidas contando desde la clase más reciente; las justificadas no cortan ni suman. */
    public static int WO_KI_NB_JI_faltasConsecutivas(NavigableMap<LocalDate, String> historial) {
        int n = 0;
        for (String v : historial.descendingMap().values()) {
            WO_KI_NB_JI_EstadoAsistencia e = WO_KI_NB_JI_EstadoAsistencia.WO_KI_NB_JI_de(v);
            if (e == WO_KI_NB_JI_EstadoAsistencia.JUSTIFICADA) continue;
            if (e == WO_KI_NB_JI_EstadoAsistencia.FALTA) n++; else break;
        }
        return n;
    }

    private static String WO_KI_NB_JI_caidaReciente(NavigableMap<LocalDate, String> h) {
        if (h.size() < 6) return null;
        List<WO_KI_NB_JI_EstadoAsistencia> orden = h.values().stream().map(WO_KI_NB_JI_EstadoAsistencia::WO_KI_NB_JI_de).toList();
        List<WO_KI_NB_JI_EstadoAsistencia> recientes = orden.subList(orden.size() - 3, orden.size());
        List<WO_KI_NB_JI_EstadoAsistencia> antes = orden.subList(0, orden.size() - 3);
        double pAntes = WO_KI_NB_JI_pct(antes), pAhora = WO_KI_NB_JI_pct(recientes);
        if (pAntes >= 80 && pAhora <= 34)
            return "Dejó de asistir: " + Math.round(pAntes) + " % antes y " + Math.round(pAhora) + " % en las últimas 3 clases";
        return null;
    }

    private static double WO_KI_NB_JI_pct(List<WO_KI_NB_JI_EstadoAsistencia> l) {
        long validos = l.stream().filter(e -> e != null && e != WO_KI_NB_JI_EstadoAsistencia.JUSTIFICADA).count();
        if (validos == 0) return 100;
        return 100.0 * l.stream().filter(e -> e != null && e.WO_KI_NB_JI_asistio()).count() / validos;
    }

    private static WO_KI_NB_JI_Nivel WO_KI_NB_JI_max(WO_KI_NB_JI_Nivel a, WO_KI_NB_JI_Nivel b) { return a.ordinal() >= b.ordinal() ? a : b; }

    /** 9.5 → "9.5", 11.0 → "11". */
    public static String WO_KI_NB_JI_numero(double v) {
        return v == Math.rint(v) ? Long.toString(Math.round(v)) : String.format(Locale.ROOT, "%.1f", v);
    }
}

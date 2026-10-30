package com.agendapersonal.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Método de enfoque: cuánto se trabaja, cuánto se descansa y cada cuántas sesiones hay un descanso largo.
 *
 * <p>Los tiempos pueden ser un valor exacto ({@code min == max}) o un <b>rango</b>: por ejemplo
 * "60–90 min de trabajo y 10–20 de descanso". Cuando el método tiene un rango, al empezar la sesión
 * se elige un valor concreto dentro de él y se muestra en pantalla, de modo que siempre sabes
 * cuánto vas a trabajar.</p>
 *
 * <p>Tiempos en minutos. {@code libre} = flowtime (se trabaja sin límite y el descanso se calcula al terminar).</p>
 */
public record wkniMetodoEnfoque(String wkniId, String wkniNombre, String wkniDescripcion, String wkniUsoComun,
                             int wkniTrabajoMin, int wkniTrabajoMax,
                             int wkniDescansoMin, int wkniDescansoMax,
                             int wkniSesionesPorCiclo,
                             int wkniDescansoLargoMin, int wkniDescansoLargoMax,
                             boolean wkniLibre, boolean wkniPropio) {

    private static final Random wkniAZAR = new Random();

    public static final List<wkniMetodoEnfoque> wkniPREDEFINIDOS = List.of(
        new wkniMetodoEnfoque("pomodoro", "Pomodoro",
            "Sesiones cortas de 25 min con 5 min de descanso; tras 4 sesiones, un descanso largo de 15 a 30 min.",
            "Estudiar, tareas administrativas, empezar cuando cuesta arrancar.",
            25, 25, 5, 5, 4, 15, 30, false, false),
        new wkniMetodoEnfoque("52-17", "Método 52/17",
            "52 minutos de trabajo y 17 de descanso, sin descanso largo.",
            "Trabajo de oficina sostenido: redactar, programar, analizar.",
            52, 52, 17, 17, 0, 0, 0, false, false),
        new wkniMetodoEnfoque("deep-60", "Deep work 60/10",
            "Bloque largo de 60 min sin interrupciones y 10 min de pausa.",
            "Tareas que piden concentración profunda: escribir, diseñar, resolver problemas.",
            60, 60, 10, 10, 0, 0, 0, false, false),
        new wkniMetodoEnfoque("deep-90", "Deep work 90/20",
            "Bloque largo de 90 min y 20 min de pausa.",
            "Proyectos grandes: tesis, código complejo, estudio intenso.",
            90, 90, 20, 20, 0, 0, 0, false, false),
        new wkniMetodoEnfoque("deep-rango", "Deep work 60–90",
            "Bloque largo de entre 60 y 90 min de trabajo y entre 10 y 20 de descanso: la duración se "
                + "sortea al empezar cada sesión, así nunca te cansas de la misma manera.",
            "Trabajo profundo de aquí a un rato, con pausas en el punto justo.",
            60, 90, 10, 20, 0, 0, 0, false, false),
        new wkniMetodoEnfoque("ultradiano", "Ciclos ultradianos",
            "Ciclos de 90 min de trabajo y 20 de descanso siguiendo el ritmo natural de energía; "
                + "descanso largo de 30 min cada 4 ciclos.",
            "Jornadas largas cuidando la energía a lo largo del día.",
            90, 90, 20, 20, 4, 30, 30, false, false),
        new wkniMetodoEnfoque("flowtime", "Flowtime",
            "Trabajas hasta que pierdas el foco, sin cronómetro regresivo. Al terminar, el descanso se calcula "
                + "(un quinto de lo trabajado, mínimo 5 min).",
            "Creatividad y tareas donde interrumpirte rompe el ritmo.",
            0, 0, 5, 5, 0, 0, 0, true, false),
        new wkniMetodoEnfoque("timeboxing", "Timeboxing",
            "Asignas una caja de tiempo fija a una tarea (por defecto 45 min) y paras al terminar, esté o no lista.",
            "Evitar el perfeccionismo y limitar tareas que se alargan.",
            45, 45, 5, 5, 0, 0, 0, false, false)
    );

    /** Técnicas pensadas para las tareas típicas de preparación y evaluación docente. */
    public static final List<wkniMetodoEnfoque> wkniDOCENTES = List.of(
        new wkniMetodoEnfoque("docente-preparacion", "Preparación de clase 50/10",
            "50 minutos para diseñar una clase y 10 minutos para descansar.",
            "Preparar materiales, secuencias y actividades de clase.", 50, 50, 10, 10, 0, 0, 0, false, false),
        new wkniMetodoEnfoque("docente-evaluacion", "Corrección 40/10",
            "40 minutos de revisión concentrada y 10 minutos de pausa.",
            "Revisar trabajos, retroalimentar y registrar resultados.", 40, 40, 10, 10, 0, 0, 0, false, false),
        new wkniMetodoEnfoque("docente-planeacion", "Planeación profunda 90/20",
            "90 minutos de trabajo profundo y 20 minutos de recuperación.",
            "Planificación semanal, diseño curricular y proyectos docentes.", 90, 90, 20, 20, 0, 0, 0, false, false)
    );

    // ------------------------------------------------------------------ acceso a los tiempos

    /** Minutos de trabajo (el valor más corto del rango). */
    public int wkniTrabajo() { return wkniTrabajoMin; }
    /** Minutos de descanso (el valor más corto del rango). */
    public int wkniDescanso() { return wkniDescansoMin; }
    /** Minutos de descanso largo (el valor más corto del rango). */
    public int wkniDescansoLargo() { return wkniDescansoLargoMin; }

    public boolean wkniRangoTrabajo() { return wkniTrabajoMax > wkniTrabajoMin; }
    public boolean wkniRangoDescanso() { return wkniDescansoMax > wkniDescansoMin; }
    public boolean wkniRangoDescansoLargo() { return wkniDescansoLargoMax > wkniDescansoLargoMin; }

    /** ¿Alguno de los tres tiempos es un rango y no un valor exacto? */
    public boolean wkniTieneRangos() { return wkniRangoTrabajo() || wkniRangoDescanso() || wkniRangoDescansoLargo(); }

    // ------------------------------------------------------------------ creación

    /** Método propio con tiempos exactos (min = max). */
    public static wkniMetodoEnfoque wkniPersonalizado(String wkniNombre, int wkni_trabajo, int wkniDescanso, int wkniSesiones, int wkniLargo) {
        return wkniPersonalizado(wkniNombre, wkni_trabajo, wkni_trabajo, wkniDescanso, wkniDescanso, wkniSesiones, wkniLargo, wkniLargo);
    }

    /**
     * Método propio con rangos. Si el máximo es menor que el mínimo se intercambian;
     * los valores se acotan a límites razonables.
     */
    public static wkniMetodoEnfoque wkniPersonalizado(String wkniNombre,
                                              int wkniTrabajoMin, int wkniTrabajoMax, int wkniDescansoMin, int wkniDescansoMax,
                                              int wkniSesiones, int wkniLargoMin, int wkniLargoMax) {
        int wkniT1 = wkniClamp(wkniTrabajoMin, 1, 240), wkniT2 = wkniClamp(wkniTrabajoMax, 1, 240);
        if (wkniT2 < wkniT1) { int wkniX = wkniT1; wkniT1 = wkniT2; wkniT2 = wkniX; }
        int wkniD1 = wkniClamp(wkniDescansoMin, 0, 120), wkniD2 = wkniClamp(wkniDescansoMax, 0, 120);
        if (wkniD2 < wkniD1) { int wkniX = wkniD1; wkniD1 = wkniD2; wkniD2 = wkniX; }
        int wkniL1 = wkniClamp(wkniLargoMin, 0, 120), wkniL2 = wkniClamp(wkniLargoMax, 0, 120);
        if (wkniL2 < wkniL1) { int wkniX = wkniL1; wkniL1 = wkniL2; wkniL2 = wkniX; }
        String wkniN = wkniNombre == null || wkniNombre.isBlank() ? "Personalizado" : wkniNombre.trim();
        return new wkniMetodoEnfoque("custom-" + Math.abs((wkniN + wkniT1 + wkniD1).hashCode()), wkniN,
            "Método propio: " + wkniRangoTexto(wkniT1, wkniT2) + " de trabajo y " + wkniRangoTexto(wkniD1, wkniD2) + " de descanso.",
            "Lo defines tú.", wkniT1, wkniT2, wkniD1, wkniD2, wkniClamp(wkniSesiones, 0, 12), wkniL1, wkniL2, false, true);
    }

    /** Método propio que copia otro (para duplicar y ajustar). */
    public wkniMetodoEnfoque wkniComoPropio(String wkniNombreNuevo) {
        String wkniN = wkniNombreNuevo == null || wkniNombreNuevo.isBlank() ? wkniNombre : wkniNombreNuevo.trim();
        return new wkniMetodoEnfoque("custom-" + Math.abs((wkniN + wkniTrabajoMin + wkniDescansoMin).hashCode()), wkniN,
            wkniDescripcion, wkniUsoComun, wkniTrabajoMin, wkniTrabajoMax, wkniDescansoMin, wkniDescansoMax,
            wkniSesionesPorCiclo, wkniDescansoLargoMin, wkniDescansoLargoMax, wkniLibre, true);
    }

    private static int wkniClamp(int wkniV, int wkniA, int wkniB) { return Math.max(wkniA, Math.min(wkniB, wkniV)); }

    /** "25 min" o "60–90 min". */
    public static String wkniRangoTexto(int wkniMin, int wkniMax) {
        return wkniMin == wkniMax ? wkniMin + " min" : wkniMin + "–" + wkniMax + " min";
    }

    /** Elige un valor concreto dentro de un rango (o el único valor si no hay rango). */
    public static int wkniElegir(int wkniMin, int wkniMax) {
        if (wkniMax <= wkniMin) return wkniMin;
        return wkniMin + wkniAZAR.nextInt(wkniMax - wkniMin + 1);
    }

    // ------------------------------------------------------------------ consulta

    public static wkniMetodoEnfoque wkniPorId(String wkniId, List<wkniMetodoEnfoque> wkniPropios) {
        for (wkniMetodoEnfoque wkniM : wkniPREDEFINIDOS) if (wkniM.wkniId.equals(wkniId)) return wkniM;
        for (wkniMetodoEnfoque wkniM : wkniPropios) if (wkniM.wkniId.equals(wkniId)) return wkniM;
        return wkniPREDEFINIDOS.get(0);
    }

    /** Resumen corto para la interfaz: "25 min trabajo · 5 min descanso · cada 4 sesiones, 15–30 min". */
    public String wkniResumen() {
        if (wkniLibre) return "Sin límite de trabajo";
        String wkniS = wkniRangoTexto(wkniTrabajoMin, wkniTrabajoMax) + " trabajo · " + wkniRangoTexto(wkniDescansoMin, wkniDescansoMax) + " descanso";
        if (wkniSesionesPorCiclo > 0 && wkniDescansoLargoMax > 0)
            wkniS += " · cada " + wkniSesionesPorCiclo + " sesiones, descanso largo de " + wkniRangoTexto(wkniDescansoLargoMin, wkniDescansoLargoMax);
        return wkniS;
    }

    // ---- guardado como mapa (para los ajustes JSON) ----

    public Map<String, Object> wkniAMapa() {
        Map<String, Object> wkniM = new LinkedHashMap<>();
        wkniM.put("id", wkniId); wkniM.put("name", wkniNombre); wkniM.put("focus", wkniTrabajoMin); wkniM.put("focusMax", wkniTrabajoMax);
        wkniM.put("break", wkniDescansoMin); wkniM.put("breakMax", wkniDescansoMax);
        wkniM.put("rounds", wkniSesionesPorCiclo);
        wkniM.put("longBreak", wkniDescansoLargoMin); wkniM.put("longBreakMax", wkniDescansoLargoMax);
        wkniM.put("desc", wkniDescripcion); wkniM.put("use", wkniUsoComun);
        return wkniM;
    }

    public static List<wkniMetodoEnfoque> wkniDesdeMapas(Object wkniRaw) {
        List<wkniMetodoEnfoque> wkniOut = new ArrayList<>();
        if (!(wkniRaw instanceof List<?> wkniL)) return wkniOut;
        for (Object wkniO : wkniL) {
            if (!(wkniO instanceof Map<?, ?> wkniM)) continue;
            // Sin "focus" y "break" la entrada está dañada: se omite en lugar de inventar un método.
            if (!(wkniM.get("focus") instanceof Number) || !(wkniM.get("break") instanceof Number)) continue;
            try {
                int wkniFoco = wkniNumero(wkniM.get("focus"), 25);
                int wkniFocoMax = wkniNumero(wkniM.get("focusMax"), wkniFoco);
                int wkniDescanso = wkniNumero(wkniM.get("break"), 5);
                int wkniDescansoMax = wkniNumero(wkniM.get("breakMax"), wkniDescanso);
                int wkniRondas = wkniNumero(wkniM.get("rounds"), 0);
                int wkniLargo = wkniNumero(wkniM.get("longBreak"), 0);
                int wkniLargoMax = wkniNumero(wkniM.get("longBreakMax"), wkniLargo);
                String wkniNombre = wkniM.get("name") instanceof String wkniS ? wkniS : "Personalizado";
                wkniMetodoEnfoque wkniBase = wkniPersonalizado(wkniNombre, wkniFoco, wkniFocoMax, wkniDescanso, wkniDescansoMax, wkniRondas, wkniLargo, wkniLargoMax);
                String wkniId = wkniM.get("id") instanceof String wkniSid ? wkniSid : wkniBase.wkniId();
                String wkniDesc = wkniM.get("desc") instanceof String wkniS && !wkniS.isBlank() ? wkniS : wkniBase.wkniDescripcion();
                String wkniUso = wkniM.get("use") instanceof String wkniS && !wkniS.isBlank() ? wkniS : wkniBase.wkniUsoComun();
                wkniOut.add(new wkniMetodoEnfoque(wkniId, wkniBase.wkniNombre, wkniDesc, wkniUso, wkniBase.wkniTrabajoMin, wkniBase.wkniTrabajoMax,
                    wkniBase.wkniDescansoMin, wkniBase.wkniDescansoMax, wkniBase.wkniSesionesPorCiclo,
                    wkniBase.wkniDescansoLargoMin, wkniBase.wkniDescansoLargoMax, false, true));
            } catch (RuntimeException wkniIgnored) { /* entrada dañada: se omite */ }
        }
        return wkniOut;
    }

    private static int wkniNumero(Object wkniO, int wkniPorDefecto) {
        return wkniO instanceof Number wkniN ? wkniN.intValue() : wkniPorDefecto;
    }
}

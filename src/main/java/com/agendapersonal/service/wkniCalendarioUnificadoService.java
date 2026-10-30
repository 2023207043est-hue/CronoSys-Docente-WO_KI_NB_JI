package com.agendapersonal.service;

import com.agendapersonal.domain.model.wkniContacto;
import com.agendapersonal.domain.model.wkniHabito;
import com.agendapersonal.model.wkniEvento;
import com.agendapersonal.model.wkniHoras;
import com.agendapersonal.model.wkniTarea;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Une en una sola línea de tiempo todo lo que el usuario guarda en Compás:
 * eventos, trabajos, tareas, hábitos y cumpleaños. Lógica pura (sin UI ni BD) y con pruebas.
 *
 * Reglas de segmentación:
 *  - TRABAJO: eventos de categoría "Trabajo" + tareas que pertenecen a un proyecto.
 *  - EVENTO : el resto de eventos.
 *  - TAREA  : tareas sin proyecto.
 *  - HABITO : DIARIO = todos los días; SEMANAL = aparece los lunes. Solo hábitos activos.
 *  - CUMPLE : se repite cada año (el 29-feb cae el 28-feb en años no bisiestos).
 */
public final class wkniCalendarioUnificadoService {
    private wkniCalendarioUnificadoService() {}

    /** Instantánea de datos sobre la que se calcula (así la UI carga una sola vez por refresco). */
    public record wkniDatos(Collection<wkniEvento> wkniEventos, Collection<wkniTarea> wkniTareas, Collection<wkniHabito> wkniHabitos,
                        Collection<wkniContacto> wkniContactos, Map<String, Set<Integer>> wkniHabitosCumplidos) {
        public static wkniDatos wkniVacio() { return new wkniDatos(List.of(), List.of(), List.of(), List.of(), Map.of()); }
    }

    public static boolean wkniEsTrabajo(wkniEvento wkniE) { return "Trabajo".equalsIgnoreCase(wkniE.wkniGetCategoria()); }

    public static List<wkniEntrada> wkniDelDia(LocalDate wkniDia, wkniDatos wkniD, Set<wkniTipoEntrada> wkniVisibles) {
        List<wkniEntrada> wkniR = new ArrayList<>();

        for (wkniEvento wkniE : wkniD.wkniEventos()) {
            if (!wkniDia.equals(wkniE.wkniGetFecha())) continue;
            wkniTipoEntrada wkniT = wkniEsTrabajo(wkniE) ? wkniTipoEntrada.TRABAJO : wkniTipoEntrada.EVENTO;
            if (!wkniVisibles.contains(wkniT)) continue;
            try {
                wkniR.add(new wkniEntrada(wkniT, wkniE.wkniGetTitulo(), wkniE.wkniGetDescripcion(), wkniDia, wkniE.wkniGetInicio(), wkniE.wkniGetFin(), false, wkniE));
            } catch (DateTimeParseException wkniEx) {   // hora corrupta en BD: se muestra como todo el día
                wkniR.add(new wkniEntrada(wkniT, wkniE.wkniGetTitulo(), wkniE.wkniGetDescripcion(), wkniDia, null, null, false, wkniE));
            }
        }

        for (wkniTarea wkniT : wkniD.wkniTareas()) {
            if (!wkniDia.equals(wkniT.wkniGetFecha())) continue;
            wkniTipoEntrada wkni_tipo = wkniT.wkniGetProyectoId() != null ? wkniTipoEntrada.TRABAJO : wkniTipoEntrada.TAREA;
            if (!wkniVisibles.contains(wkni_tipo)) continue;
            wkniR.add(new wkniEntrada(wkni_tipo, wkniT.wkniGetTitulo(), wkniT.wkniGetDescripcion(), wkniDia, null, null, wkniT.wkniEstaCompletada(), wkniT));
        }

        if (wkniVisibles.contains(wkniTipoEntrada.HABITO)) {
            Set<Integer> wkniHechos = wkniD.wkniHabitosCumplidos().getOrDefault(wkniDia.toString(), Set.of());
            for (wkniHabito wkniH : wkniD.wkniHabitos()) {
                if (!wkniH.wkniIsActivo() || !wkniAplicaHabito(wkniH, wkniDia)) continue;
                wkniR.add(new wkniEntrada(wkniTipoEntrada.HABITO, wkniH.wkniGetNombre(), wkniH.wkniGetDescripcion(), wkniDia, null, null,
                    wkniHechos.contains(wkniH.wkniGetId()), wkniH));
            }
        }

        if (wkniVisibles.contains(wkniTipoEntrada.CUMPLE)) {
            for (wkniContacto wkniC : wkniD.wkniContactos()) {
                LocalDate wkniNac = wkniFecha(wkniC.wkniGetCumpleanos());
                if (wkniNac == null) continue;
                if (MonthDay.from(wkniNac).atYear(wkniDia.getYear()).equals(wkniDia)) {
                    int wkniEdad = wkniDia.getYear() - wkniNac.getYear();
                    wkniR.add(new wkniEntrada(wkniTipoEntrada.CUMPLE, "Cumpleaños de " + wkniC.wkniGetNombre(),
                        wkniEdad > 0 ? "Cumple " + wkniEdad + " años" : "", wkniDia, null, null, false, wkniC));
                }
            }
        }

        wkniR.sort(Comparator.comparing((wkniEntrada wkniE) -> !wkniE.wkniTodoElDia())
            .thenComparing(wkniE -> wkniE.wkniTodoElDia() ? 0 : wkniE.wkniMinutoInicio())
            .thenComparing(wkniE -> wkniE.wkni_tipo().ordinal())
            .thenComparing(wkniEntrada::wkniTitulo, String.CASE_INSENSITIVE_ORDER));
        return wkniR;
    }

    public static boolean wkniAplicaHabito(wkniHabito wkniH, LocalDate wkniDia) {
        if ("SEMANAL".equalsIgnoreCase(wkniH.wkniGetFrecuencia())) return wkniDia.getDayOfWeek() == DayOfWeek.MONDAY;
        return true;
    }

    /** Cuántas entradas de cada tipo hay en el rango (para el punto indicador del mini-calendario). */
    public static boolean wkniHayAlgo(LocalDate wkniDia, wkniDatos wkniD, Set<wkniTipoEntrada> wkniVisibles) {
        for (wkniEntrada wkniE : wkniDelDia(wkniDia, wkniD, wkniVisibles))
            if (wkniE.wkni_tipo() != wkniTipoEntrada.HABITO) return true;   // los hábitos están todos los días: no marcan punto
        return false;
    }

    // ---------- diseño de la cuadrícula horaria ----------

    /** Posición de una entrada con hora dentro de su día: carril (columna) y total de carriles solapados. */
    public record wkniCarril(wkniEntrada wkni_entrada, int wkni_carril, int wkniCarriles) {}

    /** Reparte en columnas los eventos que se solapan (como Google: lado a lado). */
    public static List<wkniCarril> wkniAsignarCarriles(List<wkniEntrada> wkniConHora) {
        List<wkniEntrada> wkniOrden = new ArrayList<>(wkniConHora);
        wkniOrden.sort(Comparator.comparingInt(wkniEntrada::wkniMinutoInicio).thenComparingInt(wkniEntrada::wkniMinutoFin));
        List<wkniCarril> wkniOut = new ArrayList<>();
        List<wkniEntrada> wkniGrupo = new ArrayList<>();
        int wkniFinGrupo = -1;
        for (wkniEntrada wkniE : wkniOrden) {
            if (!wkniGrupo.isEmpty() && wkniE.wkniMinutoInicio() >= wkniFinGrupo) { wkniVolcar(wkniGrupo, wkniOut); wkniGrupo.clear(); wkniFinGrupo = -1; }
            wkniGrupo.add(wkniE);
            wkniFinGrupo = Math.max(wkniFinGrupo, wkniE.wkniMinutoFin());
        }
        if (!wkniGrupo.isEmpty()) wkniVolcar(wkniGrupo, wkniOut);
        return wkniOut;
    }

    private static void wkniVolcar(List<wkniEntrada> wkniGrupo, List<wkniCarril> wkniOut) {
        List<Integer> wkniFinPorCarril = new ArrayList<>();
        int[] wkniAsignado = new int[wkniGrupo.size()];
        for (int wkniI = 0; wkniI < wkniGrupo.size(); wkniI++) {
            wkniEntrada wkniE = wkniGrupo.get(wkniI);
            int wkniC = 0;
            while (wkniC < wkniFinPorCarril.size() && wkniFinPorCarril.get(wkniC) > wkniE.wkniMinutoInicio()) wkniC++;
            if (wkniC == wkniFinPorCarril.size()) wkniFinPorCarril.add(wkniE.wkniMinutoFin()); else wkniFinPorCarril.set(wkniC, wkniE.wkniMinutoFin());
            wkniAsignado[wkniI] = wkniC;
        }
        for (int wkniI = 0; wkniI < wkniGrupo.size(); wkniI++) wkniOut.add(new wkniCarril(wkniGrupo.get(wkniI), wkniAsignado[wkniI], wkniFinPorCarril.size()));
    }

    private static LocalDate wkniFecha(String wkniS) {
        if (wkniS == null || wkniS.isBlank()) return null;
        try { return LocalDate.parse(wkniS.trim()); } catch (DateTimeParseException wkniE) { return null; }
    }

    /** Hora válida o null (útil para los formularios). */
    static java.time.LocalTime wkniHora(String wkniS) { return wkniHoras.wkniEsValida(wkniS) ? wkniHoras.wkniParsear(wkniS) : null; }
}

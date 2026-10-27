package com.agendapersonal.service;

import com.agendapersonal.domain.model.WO_KI_NB_JI_Contacto;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Habito;
import com.agendapersonal.model.WO_KI_NB_JI_Evento;
import com.agendapersonal.model.WO_KI_NB_JI_Horas;
import com.agendapersonal.model.WO_KI_NB_JI_Tarea;
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
public final class WO_KI_NB_JI_CalendarioUnificadoService {
    private WO_KI_NB_JI_CalendarioUnificadoService() {}

    /** Instantánea de datos sobre la que se calcula (así la UI carga una sola vez por refresco). */
    public record WO_KI_NB_JI_Datos(Collection<WO_KI_NB_JI_Evento> eventos, Collection<WO_KI_NB_JI_Tarea> tareas, Collection<WO_KI_NB_JI_Habito> habitos,
                        Collection<WO_KI_NB_JI_Contacto> contactos, Map<String, Set<Integer>> habitosCumplidos) {
        public static WO_KI_NB_JI_Datos WO_KI_NB_JI_vacio() { return new WO_KI_NB_JI_Datos(List.of(), List.of(), List.of(), List.of(), Map.of()); }
    }

    public static boolean WO_KI_NB_JI_esTrabajo(WO_KI_NB_JI_Evento e) { return "Trabajo".equalsIgnoreCase(e.WO_KI_NB_JI_getCategoria()); }

    public static List<WO_KI_NB_JI_Entrada> WO_KI_NB_JI_delDia(LocalDate dia, WO_KI_NB_JI_Datos d, Set<WO_KI_NB_JI_TipoEntrada> visibles) {
        List<WO_KI_NB_JI_Entrada> r = new ArrayList<>();

        for (WO_KI_NB_JI_Evento e : d.eventos()) {
            if (!dia.equals(e.WO_KI_NB_JI_getFecha())) continue;
            WO_KI_NB_JI_TipoEntrada t = WO_KI_NB_JI_esTrabajo(e) ? WO_KI_NB_JI_TipoEntrada.TRABAJO : WO_KI_NB_JI_TipoEntrada.EVENTO;
            if (!visibles.contains(t)) continue;
            try {
                r.add(new WO_KI_NB_JI_Entrada(t, e.WO_KI_NB_JI_getTitulo(), e.WO_KI_NB_JI_getDescripcion(), dia, e.WO_KI_NB_JI_getInicio(), e.WO_KI_NB_JI_getFin(), false, e));
            } catch (DateTimeParseException ex) {   // hora corrupta en BD: se muestra como todo el día
                r.add(new WO_KI_NB_JI_Entrada(t, e.WO_KI_NB_JI_getTitulo(), e.WO_KI_NB_JI_getDescripcion(), dia, null, null, false, e));
            }
        }

        for (WO_KI_NB_JI_Tarea t : d.tareas()) {
            if (!dia.equals(t.WO_KI_NB_JI_getFecha())) continue;
            WO_KI_NB_JI_TipoEntrada tipo = t.WO_KI_NB_JI_getProyectoId() != null ? WO_KI_NB_JI_TipoEntrada.TRABAJO : WO_KI_NB_JI_TipoEntrada.TAREA;
            if (!visibles.contains(tipo)) continue;
            r.add(new WO_KI_NB_JI_Entrada(tipo, t.WO_KI_NB_JI_getTitulo(), t.WO_KI_NB_JI_getDescripcion(), dia, null, null, t.WO_KI_NB_JI_estaCompletada(), t));
        }

        if (visibles.contains(WO_KI_NB_JI_TipoEntrada.HABITO)) {
            Set<Integer> hechos = d.habitosCumplidos().getOrDefault(dia.toString(), Set.of());
            for (WO_KI_NB_JI_Habito h : d.habitos()) {
                if (!h.WO_KI_NB_JI_isActivo() || !WO_KI_NB_JI_aplicaHabito(h, dia)) continue;
                r.add(new WO_KI_NB_JI_Entrada(WO_KI_NB_JI_TipoEntrada.HABITO, h.WO_KI_NB_JI_getNombre(), h.WO_KI_NB_JI_getDescripcion(), dia, null, null,
                    hechos.contains(h.WO_KI_NB_JI_getId()), h));
            }
        }

        if (visibles.contains(WO_KI_NB_JI_TipoEntrada.CUMPLE)) {
            for (WO_KI_NB_JI_Contacto c : d.contactos()) {
                LocalDate nac = WO_KI_NB_JI_fecha(c.WO_KI_NB_JI_getCumpleanos());
                if (nac == null) continue;
                if (MonthDay.from(nac).atYear(dia.getYear()).equals(dia)) {
                    int edad = dia.getYear() - nac.getYear();
                    r.add(new WO_KI_NB_JI_Entrada(WO_KI_NB_JI_TipoEntrada.CUMPLE, "Cumpleaños de " + c.WO_KI_NB_JI_getNombre(),
                        edad > 0 ? "Cumple " + edad + " años" : "", dia, null, null, false, c));
                }
            }
        }

        r.sort(Comparator.comparing((WO_KI_NB_JI_Entrada e) -> !e.WO_KI_NB_JI_todoElDia())
            .thenComparing(e -> e.WO_KI_NB_JI_todoElDia() ? 0 : e.WO_KI_NB_JI_minutoInicio())
            .thenComparing(e -> e.tipo().ordinal())
            .thenComparing(WO_KI_NB_JI_Entrada::titulo, String.CASE_INSENSITIVE_ORDER));
        return r;
    }

    public static boolean WO_KI_NB_JI_aplicaHabito(WO_KI_NB_JI_Habito h, LocalDate dia) {
        if ("SEMANAL".equalsIgnoreCase(h.WO_KI_NB_JI_getFrecuencia())) return dia.getDayOfWeek() == DayOfWeek.MONDAY;
        return true;
    }

    /** Cuántas entradas de cada tipo hay en el rango (para el punto indicador del mini-calendario). */
    public static boolean WO_KI_NB_JI_hayAlgo(LocalDate dia, WO_KI_NB_JI_Datos d, Set<WO_KI_NB_JI_TipoEntrada> visibles) {
        for (WO_KI_NB_JI_Entrada e : WO_KI_NB_JI_delDia(dia, d, visibles))
            if (e.tipo() != WO_KI_NB_JI_TipoEntrada.HABITO) return true;   // los hábitos están todos los días: no marcan punto
        return false;
    }

    // ---------- diseño de la cuadrícula horaria ----------

    /** Posición de una entrada con hora dentro de su día: carril (columna) y total de carriles solapados. */
    public record WO_KI_NB_JI_Carril(WO_KI_NB_JI_Entrada entrada, int carril, int carriles) {}

    /** Reparte en columnas los eventos que se solapan (como Google: lado a lado). */
    public static List<WO_KI_NB_JI_Carril> WO_KI_NB_JI_asignarCarriles(List<WO_KI_NB_JI_Entrada> conHora) {
        List<WO_KI_NB_JI_Entrada> orden = new ArrayList<>(conHora);
        orden.sort(Comparator.comparingInt(WO_KI_NB_JI_Entrada::WO_KI_NB_JI_minutoInicio).thenComparingInt(WO_KI_NB_JI_Entrada::WO_KI_NB_JI_minutoFin));
        List<WO_KI_NB_JI_Carril> out = new ArrayList<>();
        List<WO_KI_NB_JI_Entrada> grupo = new ArrayList<>();
        int finGrupo = -1;
        for (WO_KI_NB_JI_Entrada e : orden) {
            if (!grupo.isEmpty() && e.WO_KI_NB_JI_minutoInicio() >= finGrupo) { WO_KI_NB_JI_volcar(grupo, out); grupo.clear(); finGrupo = -1; }
            grupo.add(e);
            finGrupo = Math.max(finGrupo, e.WO_KI_NB_JI_minutoFin());
        }
        if (!grupo.isEmpty()) WO_KI_NB_JI_volcar(grupo, out);
        return out;
    }

    private static void WO_KI_NB_JI_volcar(List<WO_KI_NB_JI_Entrada> grupo, List<WO_KI_NB_JI_Carril> out) {
        List<Integer> finPorCarril = new ArrayList<>();
        int[] asignado = new int[grupo.size()];
        for (int i = 0; i < grupo.size(); i++) {
            WO_KI_NB_JI_Entrada e = grupo.get(i);
            int c = 0;
            while (c < finPorCarril.size() && finPorCarril.get(c) > e.WO_KI_NB_JI_minutoInicio()) c++;
            if (c == finPorCarril.size()) finPorCarril.add(e.WO_KI_NB_JI_minutoFin()); else finPorCarril.set(c, e.WO_KI_NB_JI_minutoFin());
            asignado[i] = c;
        }
        for (int i = 0; i < grupo.size(); i++) out.add(new WO_KI_NB_JI_Carril(grupo.get(i), asignado[i], finPorCarril.size()));
    }

    private static LocalDate WO_KI_NB_JI_fecha(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDate.parse(s.trim()); } catch (DateTimeParseException e) { return null; }
    }

    /** Hora válida o null (útil para los formularios). */
    static java.time.LocalTime WO_KI_NB_JI_hora(String s) { return WO_KI_NB_JI_Horas.WO_KI_NB_JI_esValida(s) ? WO_KI_NB_JI_Horas.WO_KI_NB_JI_parsear(s) : null; }
}

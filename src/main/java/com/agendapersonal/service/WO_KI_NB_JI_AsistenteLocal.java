package com.agendapersonal.service;

import com.agendapersonal.domain.model.WO_KI_NB_JI_BloqueTiempo;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Habito;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Nota;
import com.agendapersonal.model.WO_KI_NB_JI_Tarea;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Asistente <b>local</b>: lee tus tareas, notas, bloques y hábitos y te dice por dónde empezar.
 *
 * <p>No usa internet ni manda nada a ningún sitio: son reglas sencillas que corren en tu equipo
 * con los datos que ya tienes en Compás. Cada suggestion trae el método de bloqueo que mejor encaja
 * con lo que tienes entre manos.</p>
 */
public final class WO_KI_NB_JI_AsistenteLocal {

    /** Una sugerencia con su explicación y el método de bloqueo que recomienda. */
    public record WO_KI_NB_JI_Sugerencia(String titulo, String detalle, String tono, String metodoId) {}

    /** Los datos que mira el asistente. */
    public record WO_KI_NB_JI_Contexto(List<WO_KI_NB_JI_Tarea> tareas, List<WO_KI_NB_JI_Nota> notas, List<WO_KI_NB_JI_BloqueTiempo> bloques,
                            List<WO_KI_NB_JI_Habito> habitos, Set<Integer> habitosCumplidos, LocalDate hoy) {}

    private WO_KI_NB_JI_AsistenteLocal() {}

    /**
     * Genera las sugerencias del día, de la más urgente a la menos.
     * Nunca devuelve null; si todo está en orden, devuelve una única sugerencia de calma.
     */
    public static List<WO_KI_NB_JI_Sugerencia> WO_KI_NB_JI_sugerir(WO_KI_NB_JI_Contexto ctx) {
        List<WO_KI_NB_JI_Sugerencia> out = new ArrayList<>();
        if (ctx == null) return out;
        LocalDate hoy = ctx.hoy() == null ? LocalDate.now() : ctx.hoy();
        List<WO_KI_NB_JI_Tarea> tareas = ctx.tareas() == null ? List.of() : ctx.tareas();

        List<WO_KI_NB_JI_Tarea> pendientes = new ArrayList<>();
        List<WO_KI_NB_JI_Tarea> vencidas = new ArrayList<>();
        List<WO_KI_NB_JI_Tarea> deHoy = new ArrayList<>();
        for (WO_KI_NB_JI_Tarea t : tareas) {
            if (t.WO_KI_NB_JI_getEstadoTipo() == null || t.WO_KI_NB_JI_getEstadoTipo().WO_KI_NB_JI_esCompletada()) continue;
            pendientes.add(t);
            if (t.WO_KI_NB_JI_getFecha() == null) continue;
            if (t.WO_KI_NB_JI_getFecha().isBefore(hoy)) vencidas.add(t);
            else if (t.WO_KI_NB_JI_getFecha().isEqual(hoy)) deHoy.add(t);
        }

        // 1. Lo vencido manda: es lo que más cuesta dejar atrás.
        if (!vencidas.isEmpty()) {
            WO_KI_NB_JI_Tarea t = vencidas.get(0);
            out.add(new WO_KI_NB_JI_Sugerencia("Empieza por lo que venció",
                vencidas.size() == 1
                    ? "«" + t.WO_KI_NB_JI_getTitulo() + "» se pasó de fecha. Son 25 minutos y te lo quitas de encima."
                    : "Tienes " + vencidas.size() + " tareas vencidas. La más antigua es «" + t.WO_KI_NB_JI_getTitulo()
                        + "». Un pomodoro de 25 min por cada una.",
                "rose", "pomodoro"));
        }

        // 2. Las notas con casillas sin marcar.
        List<WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_Item> casillas = WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_desdeNotas(ctx.notas() == null ? List.of() : ctx.notas());
        long sinHacer = casillas.stream().filter(x -> !x.hecho()).count();
        if (sinHacer > 0) {
            out.add(new WO_KI_NB_JI_Sugerencia("Hay " + sinHacer + (sinHacer == 1 ? " pendiente" : " pendientes") + " en tus notas",
                "Las casillas de tus notas («- [ ] algo») también sirven como lista de trabajo en la pantalla de bloqueo.",
                "amber", "timeboxing"));
        }

        // 3. Los hábitos del día.
        if (ctx.habitos() != null && !ctx.habitos().isEmpty()) {
            Set<Integer> cumplidos = ctx.habitosCumplidos() == null ? Set.of() : ctx.habitosCumplidos();
            int pendientesHabito = 0;
            for (WO_KI_NB_JI_Habito h : ctx.habitos())
                if (h.WO_KI_NB_JI_isActivo() && !cumplidos.contains(h.WO_KI_NB_JI_getId())) pendientesHabito++;
            if (pendientesHabito > 0) {
                out.add(new WO_KI_NB_JI_Sugerencia("Te faltan " + pendientesHabito + (pendientesHabito == 1 ? " hábito" : " hábitos") + " por marcar",
                    "Márcalos mientras trabajas: un hábito pequeño al principio del día cuesta menos.",
                    "green", "pomodoro"));
            }
        }

        // 4. Qué método encaja con lo que tienes entre manos.
        if (pendientes.size() >= 4) {
            out.add(new WO_KI_NB_JI_Sugerencia("Tienes muchas cosas abiertas (" + pendientes.size() + ")",
                "Con muchas tareas pendientes funciona mejor trabajar en pomodoros de 25 min y pasar de una a otra.",
                "blue", "pomodoro"));
        } else if (pendientes.size() == 1) {
            out.add(new WO_KI_NB_JI_Sugerencia("Tienes una sola cosa pendiente",
                "Es buen momento para un bloque largo: deep work de 60 a 90 minutos sin interrupciones.",
                "blue", "deep-rango"));
        } else if (pendientes.size() >= 2) {
            out.add(new WO_KI_NB_JI_Sugerencia("Tienes " + pendientes.size() + " cosas pendientes",
                "Prueba con bloques de 52 minutos de trabajo y 17 de descanso: da tiempo a entrar en cada tarea.",
                "blue", "52-17"));
        }

        // 5. El día en el calendario: ¿tienes bloqueado el tiempo?
        List<WO_KI_NB_JI_BloqueTiempo> bloques = ctx.bloques() == null ? List.of() : ctx.bloques();
        List<WO_KI_NB_JI_BloqueTiempo> hoyBloques = new ArrayList<>();
        for (WO_KI_NB_JI_BloqueTiempo b : bloques) if (hoy.toString().equals(b.WO_KI_NB_JI_getFecha())) hoyBloques.add(b);
        if (hoyBloques.isEmpty() && !pendientes.isEmpty()) {
            out.add(new WO_KI_NB_JI_Sugerencia("Hoy no tienes ningún bloque reservado",
                "Reserva un bloque de enfoque en el calendario: lo que tiene hora, se hace.",
                "orange", "pomodoro"));
        } else if (!hoyBloques.isEmpty()) {
            LocalTime ahora = LocalTime.now();
            WO_KI_NB_JI_BloqueTiempo enCurso = null, siguiente = null;
            for (WO_KI_NB_JI_BloqueTiempo b : hoyBloques) {
                LocalTime ini = WO_KI_NB_JI_parseo(b.WO_KI_NB_JI_getHoraInicio()), fin = WO_KI_NB_JI_parseo(b.WO_KI_NB_JI_getHoraFin());
                if (ini == null || fin == null) continue;
                if (!ahora.isBefore(ini) && ahora.isBefore(fin)) enCurso = b;
                if (ini.isAfter(ahora) && (siguiente == null || ini.isBefore(WO_KI_NB_JI_parseo(siguiente.WO_KI_NB_JI_getHoraInicio())))) siguiente = b;
            }
            if (enCurso != null) {
                out.add(new WO_KI_NB_JI_Sugerencia("Estás dentro de un bloque de enfoque",
                    "«" + enCurso.WO_KI_NB_JI_getTitulo() + "» va de " + enCurso.WO_KI_NB_JI_getHoraInicio() + " a " + enCurso.WO_KI_NB_JI_getHoraFin()
                        + ". Es buen momento para abrir la pantalla de bloqueo.",
                    "orange", enCurso.WO_KI_NB_JI_getCategoria().equalsIgnoreCase("Estudio") ? "deep-rango" : "pomodoro"));
            } else if (siguiente != null) {
                out.add(new WO_KI_NB_JI_Sugerencia("Tu próximo bloque empieza a las " + siguiente.WO_KI_NB_JI_getHoraInicio(),
                    "«" + siguiente.WO_KI_NB_JI_getTitulo() + "». Tienes un rato para adelantar algo antes.",
                    "orange", "timeboxing"));
            }
        }

        // 6. Sin nada pendiente: un mensaje tranquilo.
        if (out.isEmpty()) {
            out.add(new WO_KI_NB_JI_Sugerencia("Estás al día",
                pendientes.isEmpty()
                    ? "No hay tareas pendientes. Buen momento para avanzar un proyecto con un bloque largo de deep work."
                    : "Nada urgente por ahora. Puedes aprovechar para ordenar tus notas o preparar la semana.",
                "green", "deep-rango"));
        }
        return out;
    }

    private static LocalTime WO_KI_NB_JI_parseo(String hhmm) {
        try { return LocalTime.parse(hhmm); } catch (RuntimeException e) { return null; }
    }
}

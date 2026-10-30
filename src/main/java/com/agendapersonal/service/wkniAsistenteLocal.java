package com.agendapersonal.service;

import com.agendapersonal.domain.model.wkniBloqueTiempo;
import com.agendapersonal.domain.model.wkniHabito;
import com.agendapersonal.domain.model.wkniNota;
import com.agendapersonal.model.wkniTarea;

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
public final class wkniAsistenteLocal {

    /** Una sugerencia con su explicación y el método de bloqueo que recomienda. */
    public record wkniSugerencia(String wkniTitulo, String wkniDetalle, String wkni_tono, String wkniMetodoId) {}

    /** Los datos que mira el asistente. */
    public record wkniContexto(List<wkniTarea> wkniTareas, List<wkniNota> wkniNotas, List<wkniBloqueTiempo> wkniBloques,
                            List<wkniHabito> wkniHabitos, Set<Integer> wkniHabitosCumplidos, LocalDate wkniHoy) {}

    private wkniAsistenteLocal() {}

    /**
     * Genera las sugerencias del día, de la más urgente a la menos.
     * Nunca devuelve null; si todo está en orden, devuelve una única sugerencia de calma.
     */
    public static List<wkniSugerencia> wkniSugerir(wkniContexto wkniCtx) {
        List<wkniSugerencia> wkniOut = new ArrayList<>();
        if (wkniCtx == null) return wkniOut;
        LocalDate wkniHoy = wkniCtx.wkniHoy() == null ? LocalDate.now() : wkniCtx.wkniHoy();
        List<wkniTarea> wkniTareas = wkniCtx.wkniTareas() == null ? List.of() : wkniCtx.wkniTareas();

        List<wkniTarea> wkniPendientes = new ArrayList<>();
        List<wkniTarea> wkniVencidas = new ArrayList<>();
        List<wkniTarea> wkniDeHoy = new ArrayList<>();
        for (wkniTarea wkniT : wkniTareas) {
            if (wkniT.wkniGetEstadoTipo() == null || wkniT.wkniGetEstadoTipo().wkniEsCompletada()) continue;
            wkniPendientes.add(wkniT);
            if (wkniT.wkniGetFecha() == null) continue;
            if (wkniT.wkniGetFecha().isBefore(wkniHoy)) wkniVencidas.add(wkniT);
            else if (wkniT.wkniGetFecha().isEqual(wkniHoy)) wkniDeHoy.add(wkniT);
        }

        // 1. Lo vencido manda: es lo que más cuesta dejar atrás.
        if (!wkniVencidas.isEmpty()) {
            wkniTarea wkniT = wkniVencidas.get(0);
            wkniOut.add(new wkniSugerencia("Empieza por lo que venció",
                wkniVencidas.size() == 1
                    ? "«" + wkniT.wkniGetTitulo() + "» se pasó de fecha. Son 25 minutos y te lo quitas de encima."
                    : "Tienes " + wkniVencidas.size() + " tareas vencidas. La más antigua es «" + wkniT.wkniGetTitulo()
                        + "». Un pomodoro de 25 min por cada una.",
                "rose", "pomodoro"));
        }

        // 2. Las notas con casillas sin marcar.
        List<wkniListaPendientes.wkniItem> wkniCasillas = wkniListaPendientes.wkniDesdeNotas(wkniCtx.wkniNotas() == null ? List.of() : wkniCtx.wkniNotas());
        long wkniSinHacer = wkniCasillas.stream().filter(wkniX -> !wkniX.wkniHecho()).count();
        if (wkniSinHacer > 0) {
            wkniOut.add(new wkniSugerencia("Hay " + wkniSinHacer + (wkniSinHacer == 1 ? " pendiente" : " pendientes") + " en tus notas",
                "Las casillas de tus notas («- [ ] algo») también sirven como lista de trabajo en la pantalla de bloqueo.",
                "amber", "timeboxing"));
        }

        // 3. Los hábitos del día.
        if (wkniCtx.wkniHabitos() != null && !wkniCtx.wkniHabitos().isEmpty()) {
            Set<Integer> wkniCumplidos = wkniCtx.wkniHabitosCumplidos() == null ? Set.of() : wkniCtx.wkniHabitosCumplidos();
            int wkniPendientesHabito = 0;
            for (wkniHabito wkniH : wkniCtx.wkniHabitos())
                if (wkniH.wkniIsActivo() && !wkniCumplidos.contains(wkniH.wkniGetId())) wkniPendientesHabito++;
            if (wkniPendientesHabito > 0) {
                wkniOut.add(new wkniSugerencia("Te faltan " + wkniPendientesHabito + (wkniPendientesHabito == 1 ? " hábito" : " hábitos") + " por marcar",
                    "Márcalos mientras trabajas: un hábito pequeño al principio del día cuesta menos.",
                    "green", "pomodoro"));
            }
        }

        // 4. Qué método encaja con lo que tienes entre manos.
        if (wkniPendientes.size() >= 4) {
            wkniOut.add(new wkniSugerencia("Tienes muchas cosas abiertas (" + wkniPendientes.size() + ")",
                "Con muchas tareas pendientes funciona mejor trabajar en pomodoros de 25 min y pasar de una a otra.",
                "blue", "pomodoro"));
        } else if (wkniPendientes.size() == 1) {
            wkniOut.add(new wkniSugerencia("Tienes una sola cosa pendiente",
                "Es buen momento para un bloque largo: deep work de 60 a 90 minutos sin interrupciones.",
                "blue", "deep-rango"));
        } else if (wkniPendientes.size() >= 2) {
            wkniOut.add(new wkniSugerencia("Tienes " + wkniPendientes.size() + " cosas pendientes",
                "Prueba con bloques de 52 minutos de trabajo y 17 de descanso: da tiempo a entrar en cada tarea.",
                "blue", "52-17"));
        }

        // 5. El día en el calendario: ¿tienes bloqueado el tiempo?
        List<wkniBloqueTiempo> wkniBloques = wkniCtx.wkniBloques() == null ? List.of() : wkniCtx.wkniBloques();
        List<wkniBloqueTiempo> wkniHoyBloques = new ArrayList<>();
        for (wkniBloqueTiempo wkniB : wkniBloques) if (wkniHoy.toString().equals(wkniB.wkniGetFecha())) wkniHoyBloques.add(wkniB);
        if (wkniHoyBloques.isEmpty() && !wkniPendientes.isEmpty()) {
            wkniOut.add(new wkniSugerencia("Hoy no tienes ningún bloque reservado",
                "Reserva un bloque de enfoque en el calendario: lo que tiene hora, se hace.",
                "orange", "pomodoro"));
        } else if (!wkniHoyBloques.isEmpty()) {
            LocalTime wkniAhora = LocalTime.now();
            wkniBloqueTiempo wkniEnCurso = null, wkniSiguiente = null;
            for (wkniBloqueTiempo wkniB : wkniHoyBloques) {
                LocalTime wkniIni = wkniParseo(wkniB.wkniGetHoraInicio()), wkniFin = wkniParseo(wkniB.wkniGetHoraFin());
                if (wkniIni == null || wkniFin == null) continue;
                if (!wkniAhora.isBefore(wkniIni) && wkniAhora.isBefore(wkniFin)) wkniEnCurso = wkniB;
                if (wkniIni.isAfter(wkniAhora) && (wkniSiguiente == null || wkniIni.isBefore(wkniParseo(wkniSiguiente.wkniGetHoraInicio())))) wkniSiguiente = wkniB;
            }
            if (wkniEnCurso != null) {
                wkniOut.add(new wkniSugerencia("Estás dentro de un bloque de enfoque",
                    "«" + wkniEnCurso.wkniGetTitulo() + "» va de " + wkniEnCurso.wkniGetHoraInicio() + " a " + wkniEnCurso.wkniGetHoraFin()
                        + ". Es buen momento para abrir la pantalla de bloqueo.",
                    "orange", wkniEnCurso.wkniGetCategoria().equalsIgnoreCase("Estudio") ? "deep-rango" : "pomodoro"));
            } else if (wkniSiguiente != null) {
                wkniOut.add(new wkniSugerencia("Tu próximo bloque empieza a las " + wkniSiguiente.wkniGetHoraInicio(),
                    "«" + wkniSiguiente.wkniGetTitulo() + "». Tienes un rato para adelantar algo antes.",
                    "orange", "timeboxing"));
            }
        }

        // 6. Sin nada pendiente: un mensaje tranquilo.
        if (wkniOut.isEmpty()) {
            wkniOut.add(new wkniSugerencia("Estás al día",
                wkniPendientes.isEmpty()
                    ? "No hay tareas pendientes. Buen momento para avanzar un proyecto con un bloque largo de deep work."
                    : "Nada urgente por ahora. Puedes aprovechar para ordenar tus notas o preparar la semana.",
                "green", "deep-rango"));
        }
        return wkniOut;
    }

    private static LocalTime wkniParseo(String wkniHhmm) {
        try { return LocalTime.parse(wkniHhmm); } catch (RuntimeException wkniE) { return null; }
    }
}

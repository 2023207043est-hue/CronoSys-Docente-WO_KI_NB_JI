package com.agendapersonal.service;

import com.agendapersonal.model.wkniTarea;
import java.util.Collection;

/** Cálculo del avance de proyectos (lógica pura, sin UI ni BD). */
public final class wkniProgresoService {
    private wkniProgresoService() {}

    /** Resultado de cambiar el estado de una tarea: avance antes y después. */
    public record wkniResultado(Integer wkniProyectoId, int wkniAntes, int wkniDespues, boolean wkniCompletada) {
        public boolean wkniCambio() { return wkniAntes != wkniDespues; }

        /** Ej.: "Tarea completada · Progreso: 17% → 33%". */
        public String wkniMensaje(String wkniNombreProyecto) {
            String wkniAmbito = wkniNombreProyecto == null || wkniNombreProyecto.isBlank() ? "" : " (" + wkniNombreProyecto + ")";
            return (wkniCompletada ? "Tarea completada" : "Tarea reabierta") + wkniAmbito
                + " · Progreso: " + wkniAntes + "% → " + wkniDespues + "%";
        }
    }

    /**
     * Porcentaje (0-100, redondeado) de tareas completadas del proyecto.
     * Con {@code proyectoId == null} calcula el avance global de todas las tareas.
     */
    public static int wkniPorcentaje(Integer wkniProyectoId, Collection<wkniTarea> wkniTareas) {
        int wkniTotal = 0, wkniHechas = 0;
        for (wkniTarea wkniT : wkniTareas) {
            if (wkniProyectoId != null && !wkniProyectoId.equals(wkniT.wkniGetProyectoId())) continue;
            wkniTotal++;
            if (wkniT.wkniEstaCompletada()) wkniHechas++;
        }
        return wkniTotal == 0 ? 0 : (int) Math.round(100.0 * wkniHechas / wkniTotal);
    }
}

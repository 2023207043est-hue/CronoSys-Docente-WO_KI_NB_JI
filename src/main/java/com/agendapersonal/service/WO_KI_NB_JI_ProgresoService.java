package com.agendapersonal.service;

import com.agendapersonal.model.WO_KI_NB_JI_Tarea;
import java.util.Collection;

/** Cálculo del avance de proyectos (lógica pura, sin UI ni BD). */
public final class WO_KI_NB_JI_ProgresoService {
    private WO_KI_NB_JI_ProgresoService() {}

    /** Resultado de cambiar el estado de una tarea: avance antes y después. */
    public record WO_KI_NB_JI_Resultado(Integer proyectoId, int antes, int despues, boolean completada) {
        public boolean WO_KI_NB_JI_cambio() { return antes != despues; }

        /** Ej.: "Tarea completada · Progreso: 17% → 33%". */
        public String WO_KI_NB_JI_mensaje(String nombreProyecto) {
            String ambito = nombreProyecto == null || nombreProyecto.isBlank() ? "" : " (" + nombreProyecto + ")";
            return (completada ? "Tarea completada" : "Tarea reabierta") + ambito
                + " · Progreso: " + antes + "% → " + despues + "%";
        }
    }

    /**
     * Porcentaje (0-100, redondeado) de tareas completadas del proyecto.
     * Con {@code proyectoId == null} calcula el avance global de todas las tareas.
     */
    public static int WO_KI_NB_JI_porcentaje(Integer proyectoId, Collection<WO_KI_NB_JI_Tarea> tareas) {
        int total = 0, hechas = 0;
        for (WO_KI_NB_JI_Tarea t : tareas) {
            if (proyectoId != null && !proyectoId.equals(t.WO_KI_NB_JI_getProyectoId())) continue;
            total++;
            if (t.WO_KI_NB_JI_estaCompletada()) hechas++;
        }
        return total == 0 ? 0 : (int) Math.round(100.0 * hechas / total);
    }
}

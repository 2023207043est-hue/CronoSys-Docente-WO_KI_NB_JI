package com.agendapersonal.service;

import com.agendapersonal.controller.WO_KI_NB_JI_MainController;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_ContactoDAO;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_HabitoDAO;
import java.time.LocalDate;
import java.util.List;

/** Reúne en una {@link CalendarioUnificadoService.Datos} todo lo que usan el calendario y los avisos. */
public final class WO_KI_NB_JI_AgendaDatos {
    private WO_KI_NB_JI_AgendaDatos() {}

    /** Llamar desde el hilo de JavaFX (las listas de eventos y tareas son observables). */
    public static WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_Datos WO_KI_NB_JI_cargar(WO_KI_NB_JI_MainController c, LocalDate desde, LocalDate hasta) {
        return WO_KI_NB_JI_cargar(List.copyOf(c.WO_KI_NB_JI_getEventos()), List.copyOf(c.WO_KI_NB_JI_getTareas()), desde, hasta);
    }

    /**
     * Versión sin hilo de JavaFX: recibe copias de eventos y tareas y consulta solo hábitos y contactos,
     * así puede ejecutarse en un hilo de fondo sin congelar la ventana.
     */
    public static WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_Datos WO_KI_NB_JI_cargar(java.util.Collection<com.agendapersonal.model.WO_KI_NB_JI_Evento> eventos,
                                                          java.util.Collection<com.agendapersonal.model.WO_KI_NB_JI_Tarea> tareas,
                                                          LocalDate desde, LocalDate hasta) {
        WO_KI_NB_JI_HabitoDAO habitos = new WO_KI_NB_JI_HabitoDAO();
        return new WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_Datos(eventos, tareas,
            habitos.WO_KI_NB_JI_listar(), new WO_KI_NB_JI_ContactoDAO().WO_KI_NB_JI_listar(),
            habitos.WO_KI_NB_JI_cumplidosEntre(desde.toString(), hasta.toString()));
    }
}

package com.agendapersonal.service;

import com.agendapersonal.controller.wkniMainController;
import com.agendapersonal.infrastructure.repo.wkniContactoDAO;
import com.agendapersonal.infrastructure.repo.wkniHabitoDAO;
import java.time.LocalDate;
import java.util.List;

/** Reúne en una {@link CalendarioUnificadoService.Datos} todo lo que usan el calendario y los avisos. */
public final class wkniAgendaDatos {
    private wkniAgendaDatos() {}

    /** Llamar desde el hilo de JavaFX (las listas de eventos y tareas son observables). */
    public static wkniCalendarioUnificadoService.wkniDatos wkniCargar(wkniMainController wkniC, LocalDate wkniDesde, LocalDate wkniHasta) {
        return wkniCargar(List.copyOf(wkniC.wkniGetEventos()), List.copyOf(wkniC.wkniGetTareas()), wkniDesde, wkniHasta);
    }

    /**
     * Versión sin hilo de JavaFX: recibe copias de eventos y tareas y consulta solo hábitos y contactos,
     * así puede ejecutarse en un hilo de fondo sin congelar la ventana.
     */
    public static wkniCalendarioUnificadoService.wkniDatos wkniCargar(java.util.Collection<com.agendapersonal.model.wkniEvento> wkniEventos,
                                                          java.util.Collection<com.agendapersonal.model.wkniTarea> wkniTareas,
                                                          LocalDate wkniDesde, LocalDate wkniHasta) {
        wkniHabitoDAO wkniHabitos = new wkniHabitoDAO();
        return new wkniCalendarioUnificadoService.wkniDatos(wkniEventos, wkniTareas,
            wkniHabitos.wkniListar(), new wkniContactoDAO().wkniListar(),
            wkniHabitos.wkniCumplidosEntre(wkniDesde.toString(), wkniHasta.toString()));
    }
}

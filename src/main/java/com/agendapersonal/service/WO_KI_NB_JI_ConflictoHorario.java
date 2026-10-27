package com.agendapersonal.service;

import com.agendapersonal.model.WO_KI_NB_JI_Evento;

/** Cruce detectado entre un evento propuesto y uno ya existente. */
public record WO_KI_NB_JI_ConflictoHorario(WO_KI_NB_JI_Evento existente, int minutosSolapados) {
    public String WO_KI_NB_JI_descripcion() {
        return "«" + existente.WO_KI_NB_JI_getTitulo() + "» (" + existente.WO_KI_NB_JI_getHoraInicio() + "–" + existente.WO_KI_NB_JI_getHoraFin()
            + "): se cruzan " + WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_formatearMinutos(minutosSolapados);
    }
}

package com.agendapersonal.service;

import com.agendapersonal.model.wkniEvento;

/** Cruce detectado entre un evento propuesto y uno ya existente. */
public record wkniConflictoHorario(wkniEvento wkniExistente, int wkniMinutosSolapados) {
    public String wkniDescripcion() {
        return "«" + wkniExistente.wkniGetTitulo() + "» (" + wkniExistente.wkniGetHoraInicio() + "–" + wkniExistente.wkniGetHoraFin()
            + "): se cruzan " + wkniAgendaService.wkniFormatearMinutos(wkniMinutosSolapados);
    }
}

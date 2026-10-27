package com.agendapersonal.domain.model;

/** Entrega de un estudiante para una actividad evaluable, con nota opcional. */
public record WO_KI_NB_JI_Entrega(int actividadId, int estudianteId, WO_KI_NB_JI_EstadoEntrega estado, Double nota) {}

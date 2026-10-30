package com.agendapersonal.domain.model;

/** Entrega de un estudiante para una actividad evaluable, con nota opcional. */
public record wkniEntrega(int wkniActividadId, int wkniEstudianteId, wkniEstadoEntrega wkniEstado, Double wkni_nota) {}

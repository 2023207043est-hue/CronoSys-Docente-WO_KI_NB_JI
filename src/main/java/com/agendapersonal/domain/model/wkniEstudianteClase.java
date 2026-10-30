package com.agendapersonal.domain.model;

/** Registro de estudiante dentro de una clase docente, con nota y observaciones opcionales. */
public record wkniEstudianteClase(int wkniId, int wkniClaseId, String wkniNombre, String wkniCorreo,
                              Double wkniCalificacion, String wkniObservaciones) {}

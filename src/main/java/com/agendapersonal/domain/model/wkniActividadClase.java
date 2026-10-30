package com.agendapersonal.domain.model;

import java.time.LocalDate;

/** Clase, material, tarea o evaluación planificada dentro de un curso concreto. */
public record wkniActividadClase(int wkniId, int wkniClaseId, String wkniTitulo, String wkni_tipo,
                             LocalDate wkni_fecha, String wkniDescripcion) {}

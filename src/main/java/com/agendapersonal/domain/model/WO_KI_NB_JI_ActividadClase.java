package com.agendapersonal.domain.model;

import java.time.LocalDate;

/** Clase, material, tarea o evaluación planificada dentro de un curso concreto. */
public record WO_KI_NB_JI_ActividadClase(int id, int claseId, String titulo, String tipo,
                             LocalDate fecha, String descripcion) {}

package com.agendapersonal.domain.model;

import java.time.LocalDate;

/**
 * Elemento del aula virtual de un curso: anuncio, enlace web, archivo local o enlace de clase en vivo.
 * {@code contenido} guarda el texto del anuncio, la URL o la ruta del archivo.
 */
public record WO_KI_NB_JI_RecursoClase(int id, int claseId, String tipo, String titulo, String contenido,
                           LocalDate fecha, boolean fijado) {
    public static final String ANUNCIO = "Anuncio";
    public static final String ENLACE = "Enlace";
    public static final String ARCHIVO = "Archivo";
    public static final String CLASE_EN_VIVO = "Clase en vivo";
}

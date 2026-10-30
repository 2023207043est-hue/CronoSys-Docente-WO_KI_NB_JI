package com.agendapersonal.domain.model;

import java.time.LocalDate;

/**
 * Elemento del aula virtual de un curso: anuncio, enlace web, archivo local o enlace de clase en vivo.
 * {@code contenido} guarda el texto del anuncio, la URL o la ruta del archivo.
 */
public record wkniRecursoClase(int wkniId, int wkniClaseId, String wkni_tipo, String wkniTitulo, String wkniContenido,
                           LocalDate wkni_fecha, boolean wkniFijado) {
    public static final String wkniANUNCIO = "Anuncio";
    public static final String wkniENLACE = "Enlace";
    public static final String wkniARCHIVO = "Archivo";
    public static final String wkniCLASE_EN_VIVO = "Clase en vivo";
}

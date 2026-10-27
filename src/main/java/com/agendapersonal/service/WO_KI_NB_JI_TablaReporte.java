package com.agendapersonal.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Tabla lista para exportar a Excel o imprimir: título, subtítulo, encabezados y filas de celdas con un
 * tono de color (verde = presente, rojo = falta, amarillo = tardanza…). Una sola fuente para ambos
 * formatos, así lo impreso y lo exportado coinciden con lo que se ve en pantalla.
 */
public record WO_KI_NB_JI_TablaReporte(String hoja, String titulo, String subtitulo, List<String> encabezados, List<List<WO_KI_NB_JI_Celda>> filas) {

    public enum WO_KI_NB_JI_Tono { NINGUNO, VERDE, ROJO, AMARILLO, AZUL, GRIS }

    /** Celda de texto o número con su tono. */
    public record WO_KI_NB_JI_Celda(String texto, Double numero, WO_KI_NB_JI_Tono tono) {
        public static WO_KI_NB_JI_Celda WO_KI_NB_JI_texto(String t) { return new WO_KI_NB_JI_Celda(t == null ? "" : t, null, WO_KI_NB_JI_Tono.NINGUNO); }
        public static WO_KI_NB_JI_Celda WO_KI_NB_JI_texto(String t, WO_KI_NB_JI_Tono tono) { return new WO_KI_NB_JI_Celda(t == null ? "" : t, null, tono); }
        public static WO_KI_NB_JI_Celda WO_KI_NB_JI_numero(Double n) {
            return n == null || !Double.isFinite(n) ? WO_KI_NB_JI_texto("—") : new WO_KI_NB_JI_Celda(WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(n), n, WO_KI_NB_JI_Tono.NINGUNO);
        }
        public static WO_KI_NB_JI_Celda WO_KI_NB_JI_numero(Double n, WO_KI_NB_JI_Tono tono) {
            return n == null || !Double.isFinite(n) ? WO_KI_NB_JI_texto("—", tono) : new WO_KI_NB_JI_Celda(WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(n), n, tono);
        }
    }

    /** Constructor cómodo para ir agregando filas. */
    public static WO_KI_NB_JI_TablaReporte WO_KI_NB_JI_nueva(String hoja, String titulo, String subtitulo, List<String> encabezados) {
        return new WO_KI_NB_JI_TablaReporte(hoja, titulo, subtitulo, List.copyOf(encabezados), new ArrayList<>());
    }

    public WO_KI_NB_JI_TablaReporte WO_KI_NB_JI_fila(List<WO_KI_NB_JI_Celda> celdas) { filas.add(List.copyOf(celdas)); return this; }
}

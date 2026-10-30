package com.agendapersonal.service;

import java.util.ArrayList;
import java.util.List;

/**
 * Tabla lista para exportar a Excel o imprimir: título, subtítulo, encabezados y filas de celdas con un
 * tono de color (verde = presente, rojo = falta, amarillo = tardanza…). Una sola fuente para ambos
 * formatos, así lo impreso y lo exportado coinciden con lo que se ve en pantalla.
 */
public record wkniTablaReporte(String wkniHoja, String wkniTitulo, String wkniSubtitulo, List<String> wkniEncabezados, List<List<wkniCelda>> wkniFilas) {

    public enum wkniTono { NINGUNO, VERDE, ROJO, AMARILLO, AZUL, GRIS }

    /** Celda de texto o número con su tono. */
    public record wkniCelda(String wkniTexto, Double wkniNumero, wkniTono wkni_tono) {
        public static wkniCelda wkniTexto(String wkniT) { return new wkniCelda(wkniT == null ? "" : wkniT, null, wkniTono.NINGUNO); }
        public static wkniCelda wkniTexto(String wkniT, wkniTono wkni_tono) { return new wkniCelda(wkniT == null ? "" : wkniT, null, wkni_tono); }
        public static wkniCelda wkniNumero(Double wkniN) {
            return wkniN == null || !Double.isFinite(wkniN) ? wkniTexto("—") : new wkniCelda(wkniRiesgoEstudianteService.wkniNumero(wkniN), wkniN, wkniTono.NINGUNO);
        }
        public static wkniCelda wkniNumero(Double wkniN, wkniTono wkni_tono) {
            return wkniN == null || !Double.isFinite(wkniN) ? wkniTexto("—", wkni_tono) : new wkniCelda(wkniRiesgoEstudianteService.wkniNumero(wkniN), wkniN, wkni_tono);
        }
    }

    /** Constructor cómodo para ir agregando filas. */
    public static wkniTablaReporte wkniNueva(String wkniHoja, String wkniTitulo, String wkniSubtitulo, List<String> wkniEncabezados) {
        return new wkniTablaReporte(wkniHoja, wkniTitulo, wkniSubtitulo, List.copyOf(wkniEncabezados), new ArrayList<>());
    }

    public wkniTablaReporte wkniFila(List<wkniCelda> wkniCeldas) { wkniFilas.add(List.copyOf(wkniCeldas)); return this; }
}

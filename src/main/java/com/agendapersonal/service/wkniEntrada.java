package com.agendapersonal.service;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Elemento mostrable en el calendario unificado. {@code origen} es el objeto real
 * (Evento, Tarea, Habito o Contacto) para poder editarlo al hacer clic.
 * Si {@code inicio} es null, es un elemento de "todo el día".
 */
public record wkniEntrada(wkniTipoEntrada wkni_tipo, String wkniTitulo, String wkniDetalle, LocalDate wkni_fecha,
                      LocalTime wkniInicio, LocalTime wkniFin, boolean wkniHecha, Object wkniOrigen) {

    public boolean wkniTodoElDia() { return wkniInicio == null; }

    /** Minuto de inicio (0..1439); solo válido si no es de todo el día. */
    public int wkniMinutoInicio() { return wkniInicio.getHour() * 60 + wkniInicio.getMinute(); }

    /** Minuto de fin; si falta o es anterior/igual al inicio, se asume 30 min mínimos. */
    public int wkniMinutoFin() {
        int wkniIni = wkniMinutoInicio();
        int wkniF = wkniFin == null ? wkniIni + 30 : wkniFin.getHour() * 60 + wkniFin.getMinute();
        return Math.max(wkniF, wkniIni + 30);
    }
}

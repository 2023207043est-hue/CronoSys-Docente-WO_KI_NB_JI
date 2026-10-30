package com.agendapersonal.service;

/** Segmentos del calendario unificado (equivalente a los "Mis calendarios" de Google). */
public enum wkniTipoEntrada {
    // Los colores son nombres de la paleta del tema (se resuelven en CSS), así se ven bien en claro y oscuro.
    EVENTO("Eventos", "-t-evento", "-t-evento-soft", "event"),
    TRABAJO("Trabajos", "-t-trabajo", "-t-trabajo-soft", "work"),
    TAREA("Tareas", "-t-tarea", "-t-tarea-soft", "tasks"),
    HABITO("Hábitos", "-t-habito", "-t-habito-soft", "habits"),
    CUMPLE("Cumpleaños", "-t-cumple", "-t-cumple-soft", "birthday");

    private final String wkniEtiqueta, wkniColor, wkniFondo, wkniIcono;

    wkniTipoEntrada(String wkniEtiqueta, String wkniColor, String wkniFondo, String wkniIcono) {
        this.wkniEtiqueta = wkniEtiqueta; this.wkniColor = wkniColor; this.wkniFondo = wkniFondo; this.wkniIcono = wkniIcono;
    }

    public String wkniEtiqueta() { return wkniEtiqueta; }
    public String wkniColor() { return wkniColor; }
    public String wkniFondo() { return wkniFondo; }
    public String wkniIcono() { return wkniIcono; }
}

package com.agendapersonal.model;

import java.time.LocalDate;

public final class wkniTarea {
    private int wkniId;
    private String wkniTitulo;
    private String wkniDescripcion;
    private wkniEstadoTarea wkniEstado = wkniEstadoTarea.PENDIENTE;
    private LocalDate wkni_fecha;
    private wkniPrioridad wkni_prioridad = wkniPrioridad.MEDIA;
    private Integer wkniProyectoId;

    public wkniTarea() {}

    public wkniTarea(int wkniId, String wkniTitulo, String wkniDescripcion, String wkniEstado,
                 LocalDate wkni_fecha, String wkni_prioridad) {
        this(wkniId,wkniTitulo,wkniDescripcion,wkniEstado,wkni_fecha,wkni_prioridad,null);
    }

    public wkniTarea(int wkniId, String wkniTitulo, String wkniDescripcion, String wkniEstado,
                 LocalDate wkni_fecha, String wkni_prioridad, Integer wkniProyectoId) {
        this.wkniId=wkniId; this.wkniTitulo=wkniTitulo; this.wkniDescripcion=wkniDescripcion;
        wkniSetEstado(wkniEstado); this.wkni_fecha=wkni_fecha; wkniSetPrioridad(wkni_prioridad); this.wkniProyectoId=wkniProyectoId;
    }

    public wkniTarea(String wkniTitulo, String wkniDescripcion, String wkniEstado,
                 LocalDate wkni_fecha, String wkni_prioridad) {
        this(0,wkniTitulo,wkniDescripcion,wkniEstado,wkni_fecha,wkni_prioridad);
    }

    public int wkniGetId(){return wkniId;} public void wkniSetId(int wkniV){wkniId=wkniV;}
    public String wkniGetTitulo(){return wkniTitulo;} public void wkniSetTitulo(String wkniV){wkniTitulo=wkniV;}
    public String wkniGetDescripcion(){return wkniDescripcion;} public void wkniSetDescripcion(String wkniV){wkniDescripcion=wkniV;}
    public String wkniGetEstado(){return wkniEstado.wkniEtiqueta();}
    public wkniEstadoTarea wkniGetEstadoTipo(){return wkniEstado;}
    /** Valida contra {@link EstadoTarea}; lanza IllegalArgumentException si no es válido. */
    public void wkniSetEstado(String wkniV){wkniEstado=wkniEstadoTarea.wkniDesde(wkniV);}
    public void wkniSetEstado(wkniEstadoTarea wkniV){wkniEstado=java.util.Objects.requireNonNull(wkniV);}
    public boolean wkniEstaCompletada(){return wkniEstado.wkniEsCompletada();}
    public LocalDate wkniGetFecha(){return wkni_fecha;} public void wkniSetFecha(LocalDate wkniV){wkni_fecha=wkniV;}
    public String wkniGetPrioridad(){return wkni_prioridad.wkniEtiqueta();}
    public wkniPrioridad wkniGetPrioridadTipo(){return wkni_prioridad;}
    public void wkniSetPrioridad(String wkniV){wkni_prioridad=wkniPrioridad.wkniDesde(wkniV);}
    public void wkniSetPrioridad(wkniPrioridad wkniV){wkni_prioridad=java.util.Objects.requireNonNull(wkniV);}
    public Integer wkniGetProyectoId(){return wkniProyectoId;} public void wkniSetProyectoId(Integer wkniV){wkniProyectoId=wkniV;}

    // Hora opcional ("HH:mm" o null), destacada y vínculos con un evento o un bloque de time blocking.
    private String wkniHora;
    private boolean wkniDestacada;
    private Integer wkniEventoId, wkniBloqueId;
    public String wkniGetHora(){return wkniHora;}
    public void wkniSetHora(String wkniV){wkniHora = (wkniV == null || wkniV.isBlank()) ? null : wkniV.trim();}
    public boolean wkniIsDestacada(){return wkniDestacada;} public void wkniSetDestacada(boolean wkniV){wkniDestacada=wkniV;}
    public Integer wkniGetEventoId(){return wkniEventoId;} public void wkniSetEventoId(Integer wkniV){wkniEventoId=wkniV;}
    public Integer wkniGetBloqueId(){return wkniBloqueId;} public void wkniSetBloqueId(Integer wkniV){wkniBloqueId=wkniV;}

    @Override public String toString(){return wkniTitulo+" • "+wkniEstado.wkniEtiqueta();}
}
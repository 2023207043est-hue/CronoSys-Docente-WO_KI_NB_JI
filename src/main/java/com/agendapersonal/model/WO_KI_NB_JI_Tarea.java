package com.agendapersonal.model;

import java.time.LocalDate;

public final class WO_KI_NB_JI_Tarea {
    private int id;
    private String titulo;
    private String descripcion;
    private WO_KI_NB_JI_EstadoTarea estado = WO_KI_NB_JI_EstadoTarea.PENDIENTE;
    private LocalDate fecha;
    private WO_KI_NB_JI_Prioridad prioridad = WO_KI_NB_JI_Prioridad.MEDIA;
    private Integer proyectoId;

    public WO_KI_NB_JI_Tarea() {}

    public WO_KI_NB_JI_Tarea(int id, String titulo, String descripcion, String estado,
                 LocalDate fecha, String prioridad) {
        this(id,titulo,descripcion,estado,fecha,prioridad,null);
    }

    public WO_KI_NB_JI_Tarea(int id, String titulo, String descripcion, String estado,
                 LocalDate fecha, String prioridad, Integer proyectoId) {
        this.id=id; this.titulo=titulo; this.descripcion=descripcion;
        WO_KI_NB_JI_setEstado(estado); this.fecha=fecha; WO_KI_NB_JI_setPrioridad(prioridad); this.proyectoId=proyectoId;
    }

    public WO_KI_NB_JI_Tarea(String titulo, String descripcion, String estado,
                 LocalDate fecha, String prioridad) {
        this(0,titulo,descripcion,estado,fecha,prioridad);
    }

    public int WO_KI_NB_JI_getId(){return id;} public void WO_KI_NB_JI_setId(int v){id=v;}
    public String WO_KI_NB_JI_getTitulo(){return titulo;} public void WO_KI_NB_JI_setTitulo(String v){titulo=v;}
    public String WO_KI_NB_JI_getDescripcion(){return descripcion;} public void WO_KI_NB_JI_setDescripcion(String v){descripcion=v;}
    public String WO_KI_NB_JI_getEstado(){return estado.WO_KI_NB_JI_etiqueta();}
    public WO_KI_NB_JI_EstadoTarea WO_KI_NB_JI_getEstadoTipo(){return estado;}
    /** Valida contra {@link EstadoTarea}; lanza IllegalArgumentException si no es válido. */
    public void WO_KI_NB_JI_setEstado(String v){estado=WO_KI_NB_JI_EstadoTarea.WO_KI_NB_JI_desde(v);}
    public void WO_KI_NB_JI_setEstado(WO_KI_NB_JI_EstadoTarea v){estado=java.util.Objects.requireNonNull(v);}
    public boolean WO_KI_NB_JI_estaCompletada(){return estado.WO_KI_NB_JI_esCompletada();}
    public LocalDate WO_KI_NB_JI_getFecha(){return fecha;} public void WO_KI_NB_JI_setFecha(LocalDate v){fecha=v;}
    public String WO_KI_NB_JI_getPrioridad(){return prioridad.WO_KI_NB_JI_etiqueta();}
    public WO_KI_NB_JI_Prioridad WO_KI_NB_JI_getPrioridadTipo(){return prioridad;}
    public void WO_KI_NB_JI_setPrioridad(String v){prioridad=WO_KI_NB_JI_Prioridad.WO_KI_NB_JI_desde(v);}
    public void WO_KI_NB_JI_setPrioridad(WO_KI_NB_JI_Prioridad v){prioridad=java.util.Objects.requireNonNull(v);}
    public Integer WO_KI_NB_JI_getProyectoId(){return proyectoId;} public void WO_KI_NB_JI_setProyectoId(Integer v){proyectoId=v;}

    // Hora opcional ("HH:mm" o null), destacada y vínculos con un evento o un bloque de time blocking.
    private String hora;
    private boolean destacada;
    private Integer eventoId, bloqueId;
    public String WO_KI_NB_JI_getHora(){return hora;}
    public void WO_KI_NB_JI_setHora(String v){hora = (v == null || v.isBlank()) ? null : v.trim();}
    public boolean WO_KI_NB_JI_isDestacada(){return destacada;} public void WO_KI_NB_JI_setDestacada(boolean v){destacada=v;}
    public Integer WO_KI_NB_JI_getEventoId(){return eventoId;} public void WO_KI_NB_JI_setEventoId(Integer v){eventoId=v;}
    public Integer WO_KI_NB_JI_getBloqueId(){return bloqueId;} public void WO_KI_NB_JI_setBloqueId(Integer v){bloqueId=v;}

    @Override public String toString(){return titulo+" • "+estado.WO_KI_NB_JI_etiqueta();}
}
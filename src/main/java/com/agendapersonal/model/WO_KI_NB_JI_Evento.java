package com.agendapersonal.model;

import java.time.LocalDate;

public final class WO_KI_NB_JI_Evento {
    private int id;
    private String titulo;
    private LocalDate fecha;
    private String horaInicio;
    private String horaFin;
    private String descripcion;
    private String categoria;
    private WO_KI_NB_JI_Prioridad prioridad = WO_KI_NB_JI_Prioridad.MEDIA;
    private Integer calendarioId;

    public WO_KI_NB_JI_Evento() {}

    public WO_KI_NB_JI_Evento(int id, String titulo, LocalDate fecha, String horaInicio, String horaFin,
                  String descripcion, String categoria, String prioridad) {
        this(id,titulo,fecha,horaInicio,horaFin,descripcion,categoria,prioridad,null);
    }

    public WO_KI_NB_JI_Evento(int id, String titulo, LocalDate fecha, String horaInicio, String horaFin,
                  String descripcion, String categoria, String prioridad, Integer calendarioId) {
        this.id=id; this.titulo=titulo; this.fecha=fecha; this.horaInicio=horaInicio;
        this.horaFin=horaFin; this.descripcion=descripcion; this.categoria=categoria; WO_KI_NB_JI_setPrioridad(prioridad);
        this.calendarioId=calendarioId;
    }

    public WO_KI_NB_JI_Evento(String titulo, LocalDate fecha, String horaInicio, String horaFin,
                  String descripcion, String categoria, String prioridad) {
        this(0,titulo,fecha,horaInicio,horaFin,descripcion,categoria,prioridad);
    }

    public int WO_KI_NB_JI_getId(){return id;} public void WO_KI_NB_JI_setId(int v){id=v;}
    public String WO_KI_NB_JI_getTitulo(){return titulo;} public void WO_KI_NB_JI_setTitulo(String v){titulo=v;}
    public LocalDate WO_KI_NB_JI_getFecha(){return fecha;} public void WO_KI_NB_JI_setFecha(LocalDate v){fecha=v;}
    public String WO_KI_NB_JI_getHoraInicio(){return horaInicio;} public void WO_KI_NB_JI_setHoraInicio(String v){horaInicio=v;}
    public String WO_KI_NB_JI_getHoraFin(){return horaFin;} public void WO_KI_NB_JI_setHoraFin(String v){horaFin=v;}
    public String WO_KI_NB_JI_getDescripcion(){return descripcion;} public void WO_KI_NB_JI_setDescripcion(String v){descripcion=v;}
    public String WO_KI_NB_JI_getCategoria(){return categoria;} public void WO_KI_NB_JI_setCategoria(String v){categoria=v;}
    public String WO_KI_NB_JI_getPrioridad(){return prioridad.WO_KI_NB_JI_etiqueta();}
    public WO_KI_NB_JI_Prioridad WO_KI_NB_JI_getPrioridadTipo(){return prioridad;}
    public void WO_KI_NB_JI_setPrioridad(String v){prioridad=WO_KI_NB_JI_Prioridad.WO_KI_NB_JI_desde(v);}
    public void WO_KI_NB_JI_setPrioridad(WO_KI_NB_JI_Prioridad v){prioridad=java.util.Objects.requireNonNull(v);}

    /** Hora de inicio tipada (acepta "9:00" o "09:00"); lanza DateTimeParseException si es inválida. */
    public java.time.LocalTime WO_KI_NB_JI_getInicio(){return WO_KI_NB_JI_Horas.WO_KI_NB_JI_parsear(horaInicio);}
    /** Hora de fin tipada. */
    public java.time.LocalTime WO_KI_NB_JI_getFin(){return WO_KI_NB_JI_Horas.WO_KI_NB_JI_parsear(horaFin);}
    public Integer WO_KI_NB_JI_getCalendarioId(){return calendarioId;} public void WO_KI_NB_JI_setCalendarioId(Integer v){calendarioId=v;}

    @Override public String toString(){return horaInicio+" - "+titulo+" ("+fecha+")";}
}
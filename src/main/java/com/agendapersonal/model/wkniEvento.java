package com.agendapersonal.model;

import java.time.LocalDate;

public final class wkniEvento {
    private int wkniId;
    private String wkniTitulo;
    private LocalDate wkni_fecha;
    private String wkniHoraInicio;
    private String wkniHoraFin;
    private String wkniDescripcion;
    private String wkniCategoria;
    private wkniPrioridad wkni_prioridad = wkniPrioridad.MEDIA;
    private Integer wkniCalendarioId;

    public wkniEvento() {}

    public wkniEvento(int wkniId, String wkniTitulo, LocalDate wkni_fecha, String wkniHoraInicio, String wkniHoraFin,
                  String wkniDescripcion, String wkniCategoria, String wkni_prioridad) {
        this(wkniId,wkniTitulo,wkni_fecha,wkniHoraInicio,wkniHoraFin,wkniDescripcion,wkniCategoria,wkni_prioridad,null);
    }

    public wkniEvento(int wkniId, String wkniTitulo, LocalDate wkni_fecha, String wkniHoraInicio, String wkniHoraFin,
                  String wkniDescripcion, String wkniCategoria, String wkni_prioridad, Integer wkniCalendarioId) {
        this.wkniId=wkniId; this.wkniTitulo=wkniTitulo; this.wkni_fecha=wkni_fecha; this.wkniHoraInicio=wkniHoraInicio;
        this.wkniHoraFin=wkniHoraFin; this.wkniDescripcion=wkniDescripcion; this.wkniCategoria=wkniCategoria; wkniSetPrioridad(wkni_prioridad);
        this.wkniCalendarioId=wkniCalendarioId;
    }

    public wkniEvento(String wkniTitulo, LocalDate wkni_fecha, String wkniHoraInicio, String wkniHoraFin,
                  String wkniDescripcion, String wkniCategoria, String wkni_prioridad) {
        this(0,wkniTitulo,wkni_fecha,wkniHoraInicio,wkniHoraFin,wkniDescripcion,wkniCategoria,wkni_prioridad);
    }

    public int wkniGetId(){return wkniId;} public void wkniSetId(int wkniV){wkniId=wkniV;}
    public String wkniGetTitulo(){return wkniTitulo;} public void wkniSetTitulo(String wkniV){wkniTitulo=wkniV;}
    public LocalDate wkniGetFecha(){return wkni_fecha;} public void wkniSetFecha(LocalDate wkniV){wkni_fecha=wkniV;}
    public String wkniGetHoraInicio(){return wkniHoraInicio;} public void wkniSetHoraInicio(String wkniV){wkniHoraInicio=wkniV;}
    public String wkniGetHoraFin(){return wkniHoraFin;} public void wkniSetHoraFin(String wkniV){wkniHoraFin=wkniV;}
    public String wkniGetDescripcion(){return wkniDescripcion;} public void wkniSetDescripcion(String wkniV){wkniDescripcion=wkniV;}
    public String wkniGetCategoria(){return wkniCategoria;} public void wkniSetCategoria(String wkniV){wkniCategoria=wkniV;}
    public String wkniGetPrioridad(){return wkni_prioridad.wkniEtiqueta();}
    public wkniPrioridad wkniGetPrioridadTipo(){return wkni_prioridad;}
    public void wkniSetPrioridad(String wkniV){wkni_prioridad=wkniPrioridad.wkniDesde(wkniV);}
    public void wkniSetPrioridad(wkniPrioridad wkniV){wkni_prioridad=java.util.Objects.requireNonNull(wkniV);}

    /** Hora de inicio tipada (acepta "9:00" o "09:00"); lanza DateTimeParseException si es inválida. */
    public java.time.LocalTime wkniGetInicio(){return wkniHoras.wkniParsear(wkniHoraInicio);}
    /** Hora de fin tipada. */
    public java.time.LocalTime wkniGetFin(){return wkniHoras.wkniParsear(wkniHoraFin);}
    public Integer wkniGetCalendarioId(){return wkniCalendarioId;} public void wkniSetCalendarioId(Integer wkniV){wkniCalendarioId=wkniV;}

    @Override public String toString(){return wkniHoraInicio+" - "+wkniTitulo+" ("+wkni_fecha+")";}
}
package com.agendapersonal.domain.model;

public class WO_KI_NB_JI_BloqueTiempo {
    private int id; private String titulo; private String fecha;
    private String horaInicio; private String horaFin; private String categoria; private Integer eventoId;
    public WO_KI_NB_JI_BloqueTiempo() {}
    public WO_KI_NB_JI_BloqueTiempo(int id, String titulo, String fecha, String horaInicio, String horaFin, String categoria, Integer eventoId) {
        this.id=id; this.titulo=titulo; this.fecha=fecha; this.horaInicio=horaInicio;
        this.horaFin=horaFin; this.categoria=categoria; this.eventoId=eventoId;
    }
    public int WO_KI_NB_JI_getId(){return id;} public void WO_KI_NB_JI_setId(int v){id=v;}
    public String WO_KI_NB_JI_getTitulo(){return titulo;} public void WO_KI_NB_JI_setTitulo(String v){titulo=v;}
    public String WO_KI_NB_JI_getFecha(){return fecha;} public void WO_KI_NB_JI_setFecha(String v){fecha=v;}
    public String WO_KI_NB_JI_getHoraInicio(){return horaInicio;} public void WO_KI_NB_JI_setHoraInicio(String v){horaInicio=v;}
    public String WO_KI_NB_JI_getHoraFin(){return horaFin;} public void WO_KI_NB_JI_setHoraFin(String v){horaFin=v;}
    public String WO_KI_NB_JI_getCategoria(){return categoria;} public void WO_KI_NB_JI_setCategoria(String v){categoria=v;}
    public Integer WO_KI_NB_JI_getEventoId(){return eventoId;} public void WO_KI_NB_JI_setEventoId(Integer v){eventoId=v;}
    @Override public String toString(){return horaInicio + "-" + horaFin + " " + titulo;}
}

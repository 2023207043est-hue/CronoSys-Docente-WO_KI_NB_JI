package com.agendapersonal.domain.model;

public class WO_KI_NB_JI_Proyecto {
    private int id; private String nombre; private String descripcion;
    private String estado; private String fechaInicio; private String fechaFin; private String color;
    public WO_KI_NB_JI_Proyecto() {}
    public WO_KI_NB_JI_Proyecto(int id, String nombre, String descripcion, String estado, String fechaInicio, String fechaFin, String color) {
        this.id=id; this.nombre=nombre; this.descripcion=descripcion; this.estado=estado;
        this.fechaInicio=fechaInicio; this.fechaFin=fechaFin; this.color=color;
    }
    public int WO_KI_NB_JI_getId(){return id;} public void WO_KI_NB_JI_setId(int v){id=v;}
    public String WO_KI_NB_JI_getNombre(){return nombre;} public void WO_KI_NB_JI_setNombre(String v){nombre=v;}
    public String WO_KI_NB_JI_getDescripcion(){return descripcion;} public void WO_KI_NB_JI_setDescripcion(String v){descripcion=v;}
    public String WO_KI_NB_JI_getEstado(){return estado;} public void WO_KI_NB_JI_setEstado(String v){estado=v;}
    public String WO_KI_NB_JI_getFechaInicio(){return fechaInicio;} public void WO_KI_NB_JI_setFechaInicio(String v){fechaInicio=v;}
    public String WO_KI_NB_JI_getFechaFin(){return fechaFin;} public void WO_KI_NB_JI_setFechaFin(String v){fechaFin=v;}
    public String WO_KI_NB_JI_getColor(){return color;} public void WO_KI_NB_JI_setColor(String v){color=v;}
    @Override public String toString(){return nombre + " • " + estado;}
}

package com.agendapersonal.domain.model;

public class WO_KI_NB_JI_Habito {
    private int id; private String nombre; private String descripcion;
    private String frecuencia; private int meta; private String color; private boolean activo;
    public WO_KI_NB_JI_Habito() {}
    public WO_KI_NB_JI_Habito(int id, String nombre, String descripcion, String frecuencia, int meta, String color, boolean activo) {
        this.id=id; this.nombre=nombre; this.descripcion=descripcion; this.frecuencia=frecuencia;
        this.meta=meta; this.color=color; this.activo=activo;
    }
    public int WO_KI_NB_JI_getId(){return id;} public void WO_KI_NB_JI_setId(int v){id=v;}
    public String WO_KI_NB_JI_getNombre(){return nombre;} public void WO_KI_NB_JI_setNombre(String v){nombre=v;}
    public String WO_KI_NB_JI_getDescripcion(){return descripcion;} public void WO_KI_NB_JI_setDescripcion(String v){descripcion=v;}
    public String WO_KI_NB_JI_getFrecuencia(){return frecuencia;} public void WO_KI_NB_JI_setFrecuencia(String v){frecuencia=v;}
    public int WO_KI_NB_JI_getMeta(){return meta;} public void WO_KI_NB_JI_setMeta(int v){meta=v;}
    public String WO_KI_NB_JI_getColor(){return color;} public void WO_KI_NB_JI_setColor(String v){color=v;}
    public boolean WO_KI_NB_JI_isActivo(){return activo;} public void WO_KI_NB_JI_setActivo(boolean v){activo=v;}
    @Override public String toString(){return nombre + " • " + frecuencia;}
}

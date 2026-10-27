package com.agendapersonal.domain.model;

public class WO_KI_NB_JI_Calendario {
    private int id;
    private String nombre;
    private String color;

    public WO_KI_NB_JI_Calendario() {}

    public WO_KI_NB_JI_Calendario(int id, String nombre, String color) {
        this.id = id; this.nombre = nombre; this.color = color;
    }

    public int WO_KI_NB_JI_getId(){return id;} public void WO_KI_NB_JI_setId(int v){id=v;}
    public String WO_KI_NB_JI_getNombre(){return nombre;} public void WO_KI_NB_JI_setNombre(String v){nombre=v;}
    public String WO_KI_NB_JI_getColor(){return color;} public void WO_KI_NB_JI_setColor(String v){color=v;}

    @Override public String toString(){return nombre;}
}

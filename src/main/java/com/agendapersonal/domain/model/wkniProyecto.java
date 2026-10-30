package com.agendapersonal.domain.model;

public class wkniProyecto {
    private int wkniId; private String wkniNombre; private String wkniDescripcion;
    private String wkniEstado; private String wkniFechaInicio; private String wkniFechaFin; private String wkniColor;
    public wkniProyecto() {}
    public wkniProyecto(int wkniId, String wkniNombre, String wkniDescripcion, String wkniEstado, String wkniFechaInicio, String wkniFechaFin, String wkniColor) {
        this.wkniId=wkniId; this.wkniNombre=wkniNombre; this.wkniDescripcion=wkniDescripcion; this.wkniEstado=wkniEstado;
        this.wkniFechaInicio=wkniFechaInicio; this.wkniFechaFin=wkniFechaFin; this.wkniColor=wkniColor;
    }
    public int wkniGetId(){return wkniId;} public void wkniSetId(int wkniV){wkniId=wkniV;}
    public String wkniGetNombre(){return wkniNombre;} public void wkniSetNombre(String wkniV){wkniNombre=wkniV;}
    public String wkniGetDescripcion(){return wkniDescripcion;} public void wkniSetDescripcion(String wkniV){wkniDescripcion=wkniV;}
    public String wkniGetEstado(){return wkniEstado;} public void wkniSetEstado(String wkniV){wkniEstado=wkniV;}
    public String wkniGetFechaInicio(){return wkniFechaInicio;} public void wkniSetFechaInicio(String wkniV){wkniFechaInicio=wkniV;}
    public String wkniGetFechaFin(){return wkniFechaFin;} public void wkniSetFechaFin(String wkniV){wkniFechaFin=wkniV;}
    public String wkniGetColor(){return wkniColor;} public void wkniSetColor(String wkniV){wkniColor=wkniV;}
    @Override public String toString(){return wkniNombre + " • " + wkniEstado;}
}

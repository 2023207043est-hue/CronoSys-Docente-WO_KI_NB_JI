package com.agendapersonal.domain.model;

public class wkniHabito {
    private int wkniId; private String wkniNombre; private String wkniDescripcion;
    private String wkniFrecuencia; private int wkniMeta; private String wkniColor; private boolean wkniActivo;
    public wkniHabito() {}
    public wkniHabito(int wkniId, String wkniNombre, String wkniDescripcion, String wkniFrecuencia, int wkniMeta, String wkniColor, boolean wkniActivo) {
        this.wkniId=wkniId; this.wkniNombre=wkniNombre; this.wkniDescripcion=wkniDescripcion; this.wkniFrecuencia=wkniFrecuencia;
        this.wkniMeta=wkniMeta; this.wkniColor=wkniColor; this.wkniActivo=wkniActivo;
    }
    public int wkniGetId(){return wkniId;} public void wkniSetId(int wkniV){wkniId=wkniV;}
    public String wkniGetNombre(){return wkniNombre;} public void wkniSetNombre(String wkniV){wkniNombre=wkniV;}
    public String wkniGetDescripcion(){return wkniDescripcion;} public void wkniSetDescripcion(String wkniV){wkniDescripcion=wkniV;}
    public String wkniGetFrecuencia(){return wkniFrecuencia;} public void wkniSetFrecuencia(String wkniV){wkniFrecuencia=wkniV;}
    public int wkniGetMeta(){return wkniMeta;} public void wkniSetMeta(int wkniV){wkniMeta=wkniV;}
    public String wkniGetColor(){return wkniColor;} public void wkniSetColor(String wkniV){wkniColor=wkniV;}
    public boolean wkniIsActivo(){return wkniActivo;} public void wkniSetActivo(boolean wkniV){wkniActivo=wkniV;}
    @Override public String toString(){return wkniNombre + " • " + wkniFrecuencia;}
}

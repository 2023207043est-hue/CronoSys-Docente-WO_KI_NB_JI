package com.agendapersonal.domain.model;

public class wkniCalendario {
    private int wkniId;
    private String wkniNombre;
    private String wkniColor;

    public wkniCalendario() {}

    public wkniCalendario(int wkniId, String wkniNombre, String wkniColor) {
        this.wkniId = wkniId; this.wkniNombre = wkniNombre; this.wkniColor = wkniColor;
    }

    public int wkniGetId(){return wkniId;} public void wkniSetId(int wkniV){wkniId=wkniV;}
    public String wkniGetNombre(){return wkniNombre;} public void wkniSetNombre(String wkniV){wkniNombre=wkniV;}
    public String wkniGetColor(){return wkniColor;} public void wkniSetColor(String wkniV){wkniColor=wkniV;}

    @Override public String toString(){return wkniNombre;}
}

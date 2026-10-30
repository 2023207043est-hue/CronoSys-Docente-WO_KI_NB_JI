package com.agendapersonal.domain.model;

public class wkniRegistroSalud {
    private int wkniId; private String wkni_tipo; private double wkniValor; private String wkniUnidad;
    private String wkni_fecha; private String wkniNotas;
    public wkniRegistroSalud() {}
    public wkniRegistroSalud(int wkniId, String wkni_tipo, double wkniValor, String wkniUnidad, String wkni_fecha, String wkniNotas) {
        this.wkniId=wkniId; this.wkni_tipo=wkni_tipo; this.wkniValor=wkniValor; this.wkniUnidad=wkniUnidad; this.wkni_fecha=wkni_fecha; this.wkniNotas=wkniNotas;
    }
    public int wkniGetId(){return wkniId;} public void wkniSetId(int wkniV){wkniId=wkniV;}
    public String wkniGetTipo(){return wkni_tipo;} public void wkniSetTipo(String wkniV){wkni_tipo=wkniV;}
    public double wkniGetValor(){return wkniValor;} public void wkniSetValor(double wkniV){wkniValor=wkniV;}
    public String wkniGetUnidad(){return wkniUnidad;} public void wkniSetUnidad(String wkniV){wkniUnidad=wkniV;}
    public String wkniGetFecha(){return wkni_fecha;} public void wkniSetFecha(String wkniV){wkni_fecha=wkniV;}
    public String wkniGetNotas(){return wkniNotas;} public void wkniSetNotas(String wkniV){wkniNotas=wkniV;}
    @Override public String toString(){return wkni_tipo + ": " + wkniValor + (wkniUnidad!=null?" "+wkniUnidad:"");}
}

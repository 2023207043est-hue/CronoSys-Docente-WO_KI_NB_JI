package com.agendapersonal.domain.model;

public class wkniMovimiento {
    private int wkniId; private String wkni_tipo; private String wkniCategoria;
    private double wkniMonto; private String wkni_fecha; private String wkniDescripcion;
    public wkniMovimiento() {}
    public wkniMovimiento(int wkniId, String wkni_tipo, String wkniCategoria, double wkniMonto, String wkni_fecha, String wkniDescripcion) {
        this.wkniId=wkniId; this.wkni_tipo=wkni_tipo; this.wkniCategoria=wkniCategoria; this.wkniMonto=wkniMonto; this.wkni_fecha=wkni_fecha; this.wkniDescripcion=wkniDescripcion;
    }
    public int wkniGetId(){return wkniId;} public void wkniSetId(int wkniV){wkniId=wkniV;}
    public String wkniGetTipo(){return wkni_tipo;} public void wkniSetTipo(String wkniV){wkni_tipo=wkniV;}
    public String wkniGetCategoria(){return wkniCategoria;} public void wkniSetCategoria(String wkniV){wkniCategoria=wkniV;}
    public double wkniGetMonto(){return wkniMonto;} public void wkniSetMonto(double wkniV){wkniMonto=wkniV;}
    public String wkniGetFecha(){return wkni_fecha;} public void wkniSetFecha(String wkniV){wkni_fecha=wkniV;}
    public String wkniGetDescripcion(){return wkniDescripcion;} public void wkniSetDescripcion(String wkniV){wkniDescripcion=wkniV;}
    @Override public String toString(){return wkni_tipo + " " + wkniMonto + " • " + wkniCategoria;}
}

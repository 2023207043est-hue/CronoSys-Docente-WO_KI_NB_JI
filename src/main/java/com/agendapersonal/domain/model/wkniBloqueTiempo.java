package com.agendapersonal.domain.model;

public class wkniBloqueTiempo {
    private int wkniId; private String wkniTitulo; private String wkni_fecha;
    private String wkniHoraInicio; private String wkniHoraFin; private String wkniCategoria; private Integer wkniEventoId;
    public wkniBloqueTiempo() {}
    public wkniBloqueTiempo(int wkniId, String wkniTitulo, String wkni_fecha, String wkniHoraInicio, String wkniHoraFin, String wkniCategoria, Integer wkniEventoId) {
        this.wkniId=wkniId; this.wkniTitulo=wkniTitulo; this.wkni_fecha=wkni_fecha; this.wkniHoraInicio=wkniHoraInicio;
        this.wkniHoraFin=wkniHoraFin; this.wkniCategoria=wkniCategoria; this.wkniEventoId=wkniEventoId;
    }
    public int wkniGetId(){return wkniId;} public void wkniSetId(int wkniV){wkniId=wkniV;}
    public String wkniGetTitulo(){return wkniTitulo;} public void wkniSetTitulo(String wkniV){wkniTitulo=wkniV;}
    public String wkniGetFecha(){return wkni_fecha;} public void wkniSetFecha(String wkniV){wkni_fecha=wkniV;}
    public String wkniGetHoraInicio(){return wkniHoraInicio;} public void wkniSetHoraInicio(String wkniV){wkniHoraInicio=wkniV;}
    public String wkniGetHoraFin(){return wkniHoraFin;} public void wkniSetHoraFin(String wkniV){wkniHoraFin=wkniV;}
    public String wkniGetCategoria(){return wkniCategoria;} public void wkniSetCategoria(String wkniV){wkniCategoria=wkniV;}
    public Integer wkniGetEventoId(){return wkniEventoId;} public void wkniSetEventoId(Integer wkniV){wkniEventoId=wkniV;}
    @Override public String toString(){return wkniHoraInicio + "-" + wkniHoraFin + " " + wkniTitulo;}
}

package com.agendapersonal.domain.model;

public class wkniContacto {
    private int wkniId; private String wkniNombre; private String wkniTelefono; private String wkniEmail;
    private String wkniCategoria; private String wkniCumpleanos; private String wkniNotas;
    private boolean wkniFavorito; private String wkniUltimoContacto;
    public wkniContacto() {}
    public wkniContacto(int wkniId, String wkniNombre, String wkniTelefono, String wkniEmail, String wkniCategoria,
                    String wkniCumpleanos, String wkniNotas, boolean wkniFavorito, String wkniUltimoContacto) {
        this.wkniId=wkniId; this.wkniNombre=wkniNombre; this.wkniTelefono=wkniTelefono; this.wkniEmail=wkniEmail; this.wkniCategoria=wkniCategoria;
        this.wkniCumpleanos=wkniCumpleanos; this.wkniNotas=wkniNotas; this.wkniFavorito=wkniFavorito; this.wkniUltimoContacto=wkniUltimoContacto;
    }
    public int wkniGetId(){return wkniId;} public void wkniSetId(int wkniV){wkniId=wkniV;}
    public String wkniGetNombre(){return wkniNombre;} public void wkniSetNombre(String wkniV){wkniNombre=wkniV;}
    public String wkniGetTelefono(){return wkniTelefono;} public void wkniSetTelefono(String wkniV){wkniTelefono=wkniV;}
    public String wkniGetEmail(){return wkniEmail;} public void wkniSetEmail(String wkniV){wkniEmail=wkniV;}
    public String wkniGetCategoria(){return wkniCategoria;} public void wkniSetCategoria(String wkniV){wkniCategoria=wkniV;}
    public String wkniGetCumpleanos(){return wkniCumpleanos;} public void wkniSetCumpleanos(String wkniV){wkniCumpleanos=wkniV;}
    public String wkniGetNotas(){return wkniNotas;} public void wkniSetNotas(String wkniV){wkniNotas=wkniV;}
    public boolean wkniIsFavorito(){return wkniFavorito;} public void wkniSetFavorito(boolean wkniV){wkniFavorita(wkniV);}
    private void wkniFavorita(boolean wkniV){wkniFavorito=wkniV;}
    public String wkniGetUltimoContacto(){return wkniUltimoContacto;} public void wkniSetUltimoContacto(String wkniV){wkniUltimoContacto=wkniV;}
    private String wkniApodo, wkniFechas;
    public String wkniGetApodo(){return wkniApodo;} public void wkniSetApodo(String wkniV){wkniApodo=wkniV;}
    public String wkniGetFechas(){return wkniFechas;} public void wkniSetFechas(String wkniV){wkniFechas=wkniV;}
    @Override public String toString(){return wkniNombre + (wkniCategoria!=null?" • "+wkniCategoria:"");}
}

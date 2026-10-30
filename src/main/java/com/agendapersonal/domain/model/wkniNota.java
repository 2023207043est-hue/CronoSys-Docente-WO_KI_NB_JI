package com.agendapersonal.domain.model;

public class wkniNota {
    private int wkniId; private String wkniTitulo; private String wkniContenido; private String wkniEtiquetas;
    private String wkni_tipo; private String wkniFechaCreacion; private String wkniFechaUpdate; private boolean wkniFavorita;
    public wkniNota() {}
    public wkniNota(int wkniId, String wkniTitulo, String wkniContenido, String wkniEtiquetas, String wkni_tipo,
                String wkniFechaCreacion, String wkniFechaUpdate, boolean wkniFavorita) {
        this.wkniId=wkniId; this.wkniTitulo=wkniTitulo; this.wkniContenido=wkniContenido; this.wkniEtiquetas=wkniEtiquetas;
        this.wkni_tipo=wkni_tipo; this.wkniFechaCreacion=wkniFechaCreacion; this.wkniFechaUpdate=wkniFechaUpdate; this.wkniFavorita=wkniFavorita;
    }
    public int wkniGetId(){return wkniId;} public void wkniSetId(int wkniV){wkniId=wkniV;}
    public String wkniGetTitulo(){return wkniTitulo;} public void wkniSetTitulo(String wkniV){wkniTitulo=wkniV;}
    public String wkniGetContenido(){return wkniContenido;} public void wkniSetContenido(String wkniV){wkniContenido=wkniV;}
    public String wkniGetEtiquetas(){return wkniEtiquetas;} public void wkniSetEtiquetas(String wkniV){wkniEtiquetas=wkniV;}
    public String wkniGetTipo(){return wkni_tipo;} public void wkniSetTipo(String wkniV){wkni_tipo=wkniV;}
    public String wkniGetFechaCreacion(){return wkniFechaCreacion;} public void wkniSetFechaCreacion(String wkniV){wkniFechaCreacion=wkniV;}
    public String wkniGetFechaUpdate(){return wkniFechaUpdate;} public void wkniSetFechaUpdate(String wkniV){wkniFechaUpdate=wkniV;}
    public boolean wkniIsFavorita(){return wkniFavorita;} public void wkniSetFavorita(boolean wkniV){wkniFavorita=wkniV;}
    @Override public String toString(){return (wkni_tipo!=null&&wkni_tipo.equals("IDEA")?"Idea · ":"")+wkniTitulo;}
}

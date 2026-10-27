package com.agendapersonal.domain.model;

public class WO_KI_NB_JI_Nota {
    private int id; private String titulo; private String contenido; private String etiquetas;
    private String tipo; private String fechaCreacion; private String fechaUpdate; private boolean favorita;
    public WO_KI_NB_JI_Nota() {}
    public WO_KI_NB_JI_Nota(int id, String titulo, String contenido, String etiquetas, String tipo,
                String fechaCreacion, String fechaUpdate, boolean favorita) {
        this.id=id; this.titulo=titulo; this.contenido=contenido; this.etiquetas=etiquetas;
        this.tipo=tipo; this.fechaCreacion=fechaCreacion; this.fechaUpdate=fechaUpdate; this.favorita=favorita;
    }
    public int WO_KI_NB_JI_getId(){return id;} public void WO_KI_NB_JI_setId(int v){id=v;}
    public String WO_KI_NB_JI_getTitulo(){return titulo;} public void WO_KI_NB_JI_setTitulo(String v){titulo=v;}
    public String WO_KI_NB_JI_getContenido(){return contenido;} public void WO_KI_NB_JI_setContenido(String v){contenido=v;}
    public String WO_KI_NB_JI_getEtiquetas(){return etiquetas;} public void WO_KI_NB_JI_setEtiquetas(String v){etiquetas=v;}
    public String WO_KI_NB_JI_getTipo(){return tipo;} public void WO_KI_NB_JI_setTipo(String v){tipo=v;}
    public String WO_KI_NB_JI_getFechaCreacion(){return fechaCreacion;} public void WO_KI_NB_JI_setFechaCreacion(String v){fechaCreacion=v;}
    public String WO_KI_NB_JI_getFechaUpdate(){return fechaUpdate;} public void WO_KI_NB_JI_setFechaUpdate(String v){fechaUpdate=v;}
    public boolean WO_KI_NB_JI_isFavorita(){return favorita;} public void WO_KI_NB_JI_setFavorita(boolean v){favorita=v;}
    @Override public String toString(){return (tipo!=null&&tipo.equals("IDEA")?"Idea · ":"")+titulo;}
}

package com.agendapersonal.domain.model;

public class WO_KI_NB_JI_Contacto {
    private int id; private String nombre; private String telefono; private String email;
    private String categoria; private String cumpleanos; private String notas;
    private boolean favorito; private String ultimoContacto;
    public WO_KI_NB_JI_Contacto() {}
    public WO_KI_NB_JI_Contacto(int id, String nombre, String telefono, String email, String categoria,
                    String cumpleanos, String notas, boolean favorito, String ultimoContacto) {
        this.id=id; this.nombre=nombre; this.telefono=telefono; this.email=email; this.categoria=categoria;
        this.cumpleanos=cumpleanos; this.notas=notas; this.favorito=favorito; this.ultimoContacto=ultimoContacto;
    }
    public int WO_KI_NB_JI_getId(){return id;} public void WO_KI_NB_JI_setId(int v){id=v;}
    public String WO_KI_NB_JI_getNombre(){return nombre;} public void WO_KI_NB_JI_setNombre(String v){nombre=v;}
    public String WO_KI_NB_JI_getTelefono(){return telefono;} public void WO_KI_NB_JI_setTelefono(String v){telefono=v;}
    public String WO_KI_NB_JI_getEmail(){return email;} public void WO_KI_NB_JI_setEmail(String v){email=v;}
    public String WO_KI_NB_JI_getCategoria(){return categoria;} public void WO_KI_NB_JI_setCategoria(String v){categoria=v;}
    public String WO_KI_NB_JI_getCumpleanos(){return cumpleanos;} public void WO_KI_NB_JI_setCumpleanos(String v){cumpleanos=v;}
    public String WO_KI_NB_JI_getNotas(){return notas;} public void WO_KI_NB_JI_setNotas(String v){notas=v;}
    public boolean WO_KI_NB_JI_isFavorito(){return favorito;} public void WO_KI_NB_JI_setFavorito(boolean v){WO_KI_NB_JI_favorita(v);}
    private void WO_KI_NB_JI_favorita(boolean v){favorito=v;}
    public String WO_KI_NB_JI_getUltimoContacto(){return ultimoContacto;} public void WO_KI_NB_JI_setUltimoContacto(String v){ultimoContacto=v;}
    private String apodo, fechas;
    public String WO_KI_NB_JI_getApodo(){return apodo;} public void WO_KI_NB_JI_setApodo(String v){apodo=v;}
    public String WO_KI_NB_JI_getFechas(){return fechas;} public void WO_KI_NB_JI_setFechas(String v){fechas=v;}
    @Override public String toString(){return nombre + (categoria!=null?" • "+categoria:"");}
}

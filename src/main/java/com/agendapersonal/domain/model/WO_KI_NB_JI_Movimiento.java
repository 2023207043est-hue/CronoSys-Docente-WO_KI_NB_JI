package com.agendapersonal.domain.model;

public class WO_KI_NB_JI_Movimiento {
    private int id; private String tipo; private String categoria;
    private double monto; private String fecha; private String descripcion;
    public WO_KI_NB_JI_Movimiento() {}
    public WO_KI_NB_JI_Movimiento(int id, String tipo, String categoria, double monto, String fecha, String descripcion) {
        this.id=id; this.tipo=tipo; this.categoria=categoria; this.monto=monto; this.fecha=fecha; this.descripcion=descripcion;
    }
    public int WO_KI_NB_JI_getId(){return id;} public void WO_KI_NB_JI_setId(int v){id=v;}
    public String WO_KI_NB_JI_getTipo(){return tipo;} public void WO_KI_NB_JI_setTipo(String v){tipo=v;}
    public String WO_KI_NB_JI_getCategoria(){return categoria;} public void WO_KI_NB_JI_setCategoria(String v){categoria=v;}
    public double WO_KI_NB_JI_getMonto(){return monto;} public void WO_KI_NB_JI_setMonto(double v){monto=v;}
    public String WO_KI_NB_JI_getFecha(){return fecha;} public void WO_KI_NB_JI_setFecha(String v){fecha=v;}
    public String WO_KI_NB_JI_getDescripcion(){return descripcion;} public void WO_KI_NB_JI_setDescripcion(String v){descripcion=v;}
    @Override public String toString(){return tipo + " " + monto + " • " + categoria;}
}

package com.agendapersonal.domain.model;

public class WO_KI_NB_JI_RegistroSalud {
    private int id; private String tipo; private double valor; private String unidad;
    private String fecha; private String notas;
    public WO_KI_NB_JI_RegistroSalud() {}
    public WO_KI_NB_JI_RegistroSalud(int id, String tipo, double valor, String unidad, String fecha, String notas) {
        this.id=id; this.tipo=tipo; this.valor=valor; this.unidad=unidad; this.fecha=fecha; this.notas=notas;
    }
    public int WO_KI_NB_JI_getId(){return id;} public void WO_KI_NB_JI_setId(int v){id=v;}
    public String WO_KI_NB_JI_getTipo(){return tipo;} public void WO_KI_NB_JI_setTipo(String v){tipo=v;}
    public double WO_KI_NB_JI_getValor(){return valor;} public void WO_KI_NB_JI_setValor(double v){valor=v;}
    public String WO_KI_NB_JI_getUnidad(){return unidad;} public void WO_KI_NB_JI_setUnidad(String v){unidad=v;}
    public String WO_KI_NB_JI_getFecha(){return fecha;} public void WO_KI_NB_JI_setFecha(String v){fecha=v;}
    public String WO_KI_NB_JI_getNotas(){return notas;} public void WO_KI_NB_JI_setNotas(String v){notas=v;}
    @Override public String toString(){return tipo + ": " + valor + (unidad!=null?" "+unidad:"");}
}

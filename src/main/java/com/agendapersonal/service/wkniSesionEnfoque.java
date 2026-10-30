package com.agendapersonal.service;

/**
 * Máquina de estados de una sesión de enfoque (sin UI): fases, pausa, reinicio y avance de ciclo.
 *
 * <p>Cuando el método tiene rangos (por ejemplo "60–90 min"), al construir la sesión se sortea un valor
 * concreto dentro de cada rango y ese es el que se usa durante toda la sesión. También se puede fijar
 * a mano un valor exacto (para hacer una sola sesión más corta o más larga).</p>
 */
public final class wkniSesionEnfoque {
    public enum wkniFase { TRABAJO, DESCANSO, DESCANSO_LARGO }

    private final wkniMetodoEnfoque wkniMetodo;
    private final int wkniTrabajoUsado, wkniDescansoUsado, wkniLargoUsado;
    private wkniFase wkni_fase = wkniFase.TRABAJO;
    private int wkniTotal;            // segundos de la fase (0 en flowtime mientras se trabaja)
    private int wkniTranscurrido;     // segundos
    private int wkniSesion = 1;       // sesión de trabajo dentro del ciclo (1..n)
    private int wkniCiclo = 1;
    private boolean wkniPausada = true;
    private boolean wkniTerminada;

    /** Crea la sesión con los valores del método: si hay rango, se sortea uno dentro de él. */
    public wkniSesionEnfoque(wkniMetodoEnfoque wkniMetodo) {
        this(wkniMetodo, null, null, null);
    }

    /**
     * Crea la sesión fijando valores exactos (en minutos). Los que se dejen en null se toman del
     * método: el mínimo si no hay rango, o un valor sorteado si lo hay.
     */
    public wkniSesionEnfoque(wkniMetodoEnfoque wkniMetodo, Integer wkni_trabajo, Integer wkniDescanso, Integer wkniDescansoLargo) {
        this.wkniMetodo = wkniMetodo;
        this.wkniTrabajoUsado = wkni_trabajo != null ? wkni_trabajo
            : (wkniMetodo.wkniRangoTrabajo() ? wkniMetodoEnfoque.wkniElegir(wkniMetodo.wkniTrabajoMin(), wkniMetodo.wkniTrabajoMax()) : wkniMetodo.wkniTrabajoMin());
        this.wkniDescansoUsado = wkniDescanso != null ? wkniDescanso
            : (wkniMetodo.wkniRangoDescanso() ? wkniMetodoEnfoque.wkniElegir(wkniMetodo.wkniDescansoMin(), wkniMetodo.wkniDescansoMax()) : wkniMetodo.wkniDescansoMin());
        this.wkniLargoUsado = wkniDescansoLargo != null ? wkniDescansoLargo
            : (wkniMetodo.wkniRangoDescansoLargo() ? wkniMetodoEnfoque.wkniElegir(wkniMetodo.wkniDescansoLargoMin(), wkniMetodo.wkniDescansoLargoMax()) : wkniMetodo.wkniDescansoLargoMin());
        wkniIniciarFase(wkniFase.TRABAJO);
    }

    private void wkniIniciarFase(wkniFase wkniF) {
        wkni_fase = wkniF;
        wkniTranscurrido = 0;
        wkniTotal = switch (wkniF) {
            case TRABAJO -> wkniMetodo.wkniLibre() ? 0 : wkniTrabajoUsado * 60;
            case DESCANSO -> wkniDescansoUsado * 60;
            case DESCANSO_LARGO -> wkniLargoUsado * 60;
        };
    }

    public wkniMetodoEnfoque wkniMetodo() { return wkniMetodo; }
    public wkniFase wkniFase() { return wkni_fase; }
    public int wkniSesion() { return wkniSesion; }
    public int wkniCiclo() { return wkniCiclo; }
    public boolean wkniPausada() { return wkniPausada; }
    public boolean wkniTerminada() { return wkniTerminada; }
    public int wkniTranscurrido() { return wkniTranscurrido; }
    public int wkniTotal() { return wkniTotal; }
    public boolean wkniCuentaLibre() { return wkni_fase == wkniFase.TRABAJO && wkniMetodo.wkniLibre(); }

    /** Minutos de trabajo que se han sorteado (o fijado) para esta sesión. */
    public int wkniTrabajoUsado() { return wkniTrabajoUsado; }
    /** Minutos de descanso que se han sorteado (o fijado) para esta sesión. */
    public int wkniDescansoUsado() { return wkniDescansoUsado; }
    /** Minutos de descanso largo que se han sorteado (o fijado) para esta sesión. */
    public int wkniLargoUsado() { return wkniLargoUsado; }

    /** Segundos que faltan (en flowtime trabajando, los que lleva). */
    public int wkniRestantes() { return wkniCuentaLibre() ? wkniTranscurrido : Math.max(0, wkniTotal - wkniTranscurrido); }

    /** 0 a 1. En flowtime trabajando no hay meta, devuelve 0. */
    public double wkniProgreso() { return wkniTotal <= 0 ? 0 : Math.min(1.0, wkniTranscurrido / (double) wkniTotal); }

    public void wkniIniciar() { if (!wkniTerminada) wkniPausada = false; }
    public void wkniPausar() { wkniPausada = true; }

    /** Reinicia solo el tiempo de la fase actual. */
    public void wkniReiniciarFase() { wkniTranscurrido = 0; wkniPausada = true; }

    public void wkniDetener() { wkniPausada = true; wkniTerminada = true; }

    /** Avanza el reloj. Devuelve true si cambió de fase (para avisar con sonido o notificación). */
    public boolean wkniTick(int wkniSegundos) {
        if (wkniPausada || wkniTerminada || wkniSegundos <= 0) return false;
        wkniTranscurrido += wkniSegundos;
        if (wkniCuentaLibre() || wkniTranscurrido < wkniTotal) return false;
        wkniSiguiente();
        return true;
    }

    /** Flowtime: termina el trabajo y calcula el descanso (un quinto de lo trabajado, mínimo 5 min). */
    public void wkniTerminarFlow() {
        if (!wkniCuentaLibre()) return;
        int wkniMinutos = Math.max(5, Math.round(wkniTranscurrido / 60f / 5f));
        wkni_fase = wkniFase.DESCANSO;
        wkniTranscurrido = 0;
        wkniTotal = wkniMinutos * 60;
    }

    /** Salta a la fase siguiente. */
    public void wkniSiguiente() {
        if (wkni_fase == wkniFase.TRABAJO) {
            boolean wkniLargo = wkniMetodo.wkniSesionesPorCiclo() > 0 && wkniLargoUsado > 0 && wkniSesion >= wkniMetodo.wkniSesionesPorCiclo();
            if (wkniLargo) wkniIniciarFase(wkniFase.DESCANSO_LARGO);
            else if (wkniMetodo.wkniDescansoMin() > 0 || wkniMetodo.wkniLibre()) {
                wkniIniciarFase(wkniFase.DESCANSO);
                if (wkniMetodo.wkniLibre()) wkniTotal = Math.max(5, wkniDescansoUsado) * 60;
            } else { wkniSesion++; wkniIniciarFase(wkniFase.TRABAJO); }
        } else {
            if (wkni_fase == wkniFase.DESCANSO_LARGO) { wkniCiclo++; wkniSesion = 1; } else wkniSesion++;
            wkniIniciarFase(wkniFase.TRABAJO);
        }
    }

    public static String wkniFormato(int wkniSegundos) {
        int wkniH = wkniSegundos / 3600, wkniM = (wkniSegundos % 3600) / 60, wkniS = wkniSegundos % 60;
        return wkniH > 0 ? String.format("%d:%02d:%02d", wkniH, wkniM, wkniS) : String.format("%02d:%02d", wkniM, wkniS);
    }
}

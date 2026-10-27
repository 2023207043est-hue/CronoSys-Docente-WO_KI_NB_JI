package com.agendapersonal.service;

/**
 * Máquina de estados de una sesión de enfoque (sin UI): fases, pausa, reinicio y avance de ciclo.
 *
 * <p>Cuando el método tiene rangos (por ejemplo "60–90 min"), al construir la sesión se sortea un valor
 * concreto dentro de cada rango y ese es el que se usa durante toda la sesión. También se puede fijar
 * a mano un valor exacto (para hacer una sola sesión más corta o más larga).</p>
 */
public final class WO_KI_NB_JI_SesionEnfoque {
    public enum WO_KI_NB_JI_Fase { TRABAJO, DESCANSO, DESCANSO_LARGO }

    private final WO_KI_NB_JI_MetodoEnfoque metodo;
    private final int trabajoUsado, descansoUsado, largoUsado;
    private WO_KI_NB_JI_Fase fase = WO_KI_NB_JI_Fase.TRABAJO;
    private int total;            // segundos de la fase (0 en flowtime mientras se trabaja)
    private int transcurrido;     // segundos
    private int sesion = 1;       // sesión de trabajo dentro del ciclo (1..n)
    private int ciclo = 1;
    private boolean pausada = true;
    private boolean terminada;

    /** Crea la sesión con los valores del método: si hay rango, se sortea uno dentro de él. */
    public WO_KI_NB_JI_SesionEnfoque(WO_KI_NB_JI_MetodoEnfoque metodo) {
        this(metodo, null, null, null);
    }

    /**
     * Crea la sesión fijando valores exactos (en minutos). Los que se dejen en null se toman del
     * método: el mínimo si no hay rango, o un valor sorteado si lo hay.
     */
    public WO_KI_NB_JI_SesionEnfoque(WO_KI_NB_JI_MetodoEnfoque metodo, Integer trabajo, Integer descanso, Integer descansoLargo) {
        this.metodo = metodo;
        this.trabajoUsado = trabajo != null ? trabajo
            : (metodo.WO_KI_NB_JI_rangoTrabajo() ? WO_KI_NB_JI_MetodoEnfoque.WO_KI_NB_JI_elegir(metodo.trabajoMin(), metodo.trabajoMax()) : metodo.trabajoMin());
        this.descansoUsado = descanso != null ? descanso
            : (metodo.WO_KI_NB_JI_rangoDescanso() ? WO_KI_NB_JI_MetodoEnfoque.WO_KI_NB_JI_elegir(metodo.descansoMin(), metodo.descansoMax()) : metodo.descansoMin());
        this.largoUsado = descansoLargo != null ? descansoLargo
            : (metodo.WO_KI_NB_JI_rangoDescansoLargo() ? WO_KI_NB_JI_MetodoEnfoque.WO_KI_NB_JI_elegir(metodo.descansoLargoMin(), metodo.descansoLargoMax()) : metodo.descansoLargoMin());
        WO_KI_NB_JI_iniciarFase(WO_KI_NB_JI_Fase.TRABAJO);
    }

    private void WO_KI_NB_JI_iniciarFase(WO_KI_NB_JI_Fase f) {
        fase = f;
        transcurrido = 0;
        total = switch (f) {
            case TRABAJO -> metodo.libre() ? 0 : trabajoUsado * 60;
            case DESCANSO -> descansoUsado * 60;
            case DESCANSO_LARGO -> largoUsado * 60;
        };
    }

    public WO_KI_NB_JI_MetodoEnfoque WO_KI_NB_JI_metodo() { return metodo; }
    public WO_KI_NB_JI_Fase WO_KI_NB_JI_fase() { return fase; }
    public int WO_KI_NB_JI_sesion() { return sesion; }
    public int WO_KI_NB_JI_ciclo() { return ciclo; }
    public boolean WO_KI_NB_JI_pausada() { return pausada; }
    public boolean WO_KI_NB_JI_terminada() { return terminada; }
    public int WO_KI_NB_JI_transcurrido() { return transcurrido; }
    public int WO_KI_NB_JI_total() { return total; }
    public boolean WO_KI_NB_JI_cuentaLibre() { return fase == WO_KI_NB_JI_Fase.TRABAJO && metodo.libre(); }

    /** Minutos de trabajo que se han sorteado (o fijado) para esta sesión. */
    public int WO_KI_NB_JI_trabajoUsado() { return trabajoUsado; }
    /** Minutos de descanso que se han sorteado (o fijado) para esta sesión. */
    public int WO_KI_NB_JI_descansoUsado() { return descansoUsado; }
    /** Minutos de descanso largo que se han sorteado (o fijado) para esta sesión. */
    public int WO_KI_NB_JI_largoUsado() { return largoUsado; }

    /** Segundos que faltan (en flowtime trabajando, los que lleva). */
    public int WO_KI_NB_JI_restantes() { return WO_KI_NB_JI_cuentaLibre() ? transcurrido : Math.max(0, total - transcurrido); }

    /** 0 a 1. En flowtime trabajando no hay meta, devuelve 0. */
    public double WO_KI_NB_JI_progreso() { return total <= 0 ? 0 : Math.min(1.0, transcurrido / (double) total); }

    public void WO_KI_NB_JI_iniciar() { if (!terminada) pausada = false; }
    public void WO_KI_NB_JI_pausar() { pausada = true; }

    /** Reinicia solo el tiempo de la fase actual. */
    public void WO_KI_NB_JI_reiniciarFase() { transcurrido = 0; pausada = true; }

    public void WO_KI_NB_JI_detener() { pausada = true; terminada = true; }

    /** Avanza el reloj. Devuelve true si cambió de fase (para avisar con sonido o notificación). */
    public boolean WO_KI_NB_JI_tick(int segundos) {
        if (pausada || terminada || segundos <= 0) return false;
        transcurrido += segundos;
        if (WO_KI_NB_JI_cuentaLibre() || transcurrido < total) return false;
        WO_KI_NB_JI_siguiente();
        return true;
    }

    /** Flowtime: termina el trabajo y calcula el descanso (un quinto de lo trabajado, mínimo 5 min). */
    public void WO_KI_NB_JI_terminarFlow() {
        if (!WO_KI_NB_JI_cuentaLibre()) return;
        int minutos = Math.max(5, Math.round(transcurrido / 60f / 5f));
        fase = WO_KI_NB_JI_Fase.DESCANSO;
        transcurrido = 0;
        total = minutos * 60;
    }

    /** Salta a la fase siguiente. */
    public void WO_KI_NB_JI_siguiente() {
        if (fase == WO_KI_NB_JI_Fase.TRABAJO) {
            boolean largo = metodo.sesionesPorCiclo() > 0 && largoUsado > 0 && sesion >= metodo.sesionesPorCiclo();
            if (largo) WO_KI_NB_JI_iniciarFase(WO_KI_NB_JI_Fase.DESCANSO_LARGO);
            else if (metodo.descansoMin() > 0 || metodo.libre()) {
                WO_KI_NB_JI_iniciarFase(WO_KI_NB_JI_Fase.DESCANSO);
                if (metodo.libre()) total = Math.max(5, descansoUsado) * 60;
            } else { sesion++; WO_KI_NB_JI_iniciarFase(WO_KI_NB_JI_Fase.TRABAJO); }
        } else {
            if (fase == WO_KI_NB_JI_Fase.DESCANSO_LARGO) { ciclo++; sesion = 1; } else sesion++;
            WO_KI_NB_JI_iniciarFase(WO_KI_NB_JI_Fase.TRABAJO);
        }
    }

    public static String WO_KI_NB_JI_formato(int segundos) {
        int h = segundos / 3600, m = (segundos % 3600) / 60, s = segundos % 60;
        return h > 0 ? String.format("%d:%02d:%02d", h, m, s) : String.format("%02d:%02d", m, s);
    }
}

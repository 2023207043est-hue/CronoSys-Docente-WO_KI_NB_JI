package com.agendapersonal.ui.notify;

import com.agendapersonal.controller.wkniMainController;
import com.agendapersonal.infrastructure.db.wkniConfigDAO;
import com.agendapersonal.service.wkniAgendaDatos;
import com.agendapersonal.service.wkniCalendarioUnificadoService;
import com.agendapersonal.service.wkniRecordatorioService;
import com.agendapersonal.service.wkniRecordatorioService.wkniAviso;
import com.agendapersonal.service.wkniRecordatorioService.wkniResumen;
import com.agendapersonal.ui.wkniNotifier;
import javafx.animation.Animation;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordina los recordatorios de escritorio. Solo avisa de lo que pasa <b>en el momento</b>:
 * <ul>
 *   <li>un evento que está empezando (a su hora de inicio, no antes);</li>
 *   <li>los cumpleaños del día (una sola vez, al abrir o al cambiar de día).</li>
 * </ul>
 * El resumen del día ya no aparece solo al iniciar (se puede activar en Configuración o abrir desde
 * el ícono de la bandeja). Cada aviso queda también en el historial de la campana.
 */
public final class wkniCentroAvisos {
    public static final String wkniCFG_RESUMEN = "inicio.resumen";    // "1" muestra el resumen al abrir; por defecto "0"
    public static final String wkniCFG_MINUTOS = "aviso.minutos";     // antelación en minutos; por defecto 0 = al empezar
    public static final String wkniCFG_EVENTOS = "aviso.eventos";     // "1" (por defecto) o "0"
    public static final String wkniCFG_CUMPLES = "aviso.cumples";     // "1" (por defecto) o "0"
    private static final int wkniVENTANA_CUMPLES_DIAS = 7;

    private final wkniMainController wkniC;
    private final Stage wkniVentana;
    private final Runnable wkniIrAgenda;
    private final wkniConfigDAO wkniCfg = new wkniConfigDAO();
    private final Set<String> wkniAvisados = ConcurrentHashMap.newKeySet();
    private volatile boolean wkniOcupado;
    private Timeline wkniReloj;
    private LocalDate wkniUltimoDia = LocalDate.now();

    public wkniCentroAvisos(wkniMainController wkniC, Stage wkniVentana, Runnable wkniIrAgenda) {
        this.wkniC = wkniC; this.wkniVentana = wkniVentana; this.wkniIrAgenda = wkniIrAgenda;
    }

    public void wkniIniciar() {
        LocalDate wkniHoy = LocalDate.now();
        wkniUltimoDia = wkniHoy;
        var wkni_datos = wkniDatos(wkniHoy);
        if ("1".equals(wkniCfg.wkniGet(wkniCFG_RESUMEN, "0"))) {
            wkniResumen wkniR = wkniRecordatorioService.wkniResumenInicio(wkniHoy, wkni_datos, wkniVENTANA_CUMPLES_DIAS);
            if (wkniR.wkniHayNoticias()) wkniMostrarResumen(wkniR);
        }
        wkniVerificar(true);
        // Se revisa cada 20 s para que el aviso salga lo más cerca posible de la hora exacta.
        wkniReloj = new Timeline(new KeyFrame(Duration.seconds(20), wkniE -> wkniTick()));
        wkniReloj.setCycleCount(Animation.INDEFINITE);
        wkniReloj.play();
    }

    public void wkniDetener() { if (wkniReloj != null) wkniReloj.stop(); }

    /** Muestra la ventana de resumen a pedido (menú de la bandeja). */
    public void wkniMostrarResumenAhora() {
        wkniMostrarResumen(wkniRecordatorioService.wkniResumenInicio(LocalDate.now(), wkniDatos(LocalDate.now()), wkniVENTANA_CUMPLES_DIAS));
    }

    private void wkniMostrarResumen(wkniResumen wkniR) {
        wkniResumenInicioView.wkniMostrar(wkniVentana.isShowing() ? wkniVentana : null, wkniR, wkniIrAgenda,
            wkniNoMostrar -> wkniCfg.wkniSet(wkniCFG_RESUMEN, wkniNoMostrar ? "0" : "1"), "1".equals(wkniCfg.wkniGet(wkniCFG_RESUMEN, "0")));
    }

    private void wkniTick() {
        LocalDate wkniHoy = LocalDate.now();
        boolean wkniNuevoDia = !wkniHoy.equals(wkniUltimoDia);
        if (wkniNuevoDia) { wkniUltimoDia = wkniHoy; wkniAvisados.clear(); }   // pasó la medianoche con el programa abierto
        wkniVerificar(wkniNuevoDia);
    }

    /**
     * Revisa qué hay que avisar ahora. Las consultas a la base de datos (hábitos, contactos, configuración)
     * se hacen en un hilo de fondo para no congelar la ventana; solo mostrar el aviso ocurre en el hilo de JavaFX.
     */
    private void wkniVerificar(boolean wkniIncluirCumples) {
        if (wkniOcupado) return;
        List<com.agendapersonal.model.wkniEvento> wkniEventos = List.copyOf(wkniC.wkniGetEventos());   // copia hecha en el hilo de JavaFX
        List<com.agendapersonal.model.wkniTarea> wkniTareas = List.copyOf(wkniC.wkniGetTareas());
        wkniOcupado = true;
        Thread wkniT = new Thread(() -> {
            try {
                LocalDateTime wkniAhora = LocalDateTime.now();
                boolean wkniEventosOn = !"0".equals(wkniCfg.wkniGet(wkniCFG_EVENTOS, "1"));
                boolean wkniCumplesOn = wkniIncluirCumples && !"0".equals(wkniCfg.wkniGet(wkniCFG_CUMPLES, "1"));
                List<wkniAviso> wkniPendientes = new ArrayList<>();
                if (wkniEventosOn || wkniCumplesOn) {
                    var wkni_datos = wkniAgendaDatos.wkniCargar(wkniEventos, wkniTareas, wkniAhora.toLocalDate(), wkniAhora.toLocalDate());
                    if (wkniCumplesOn) wkniPendientes.addAll(wkniRecordatorioService.wkniAvisosCumpleHoy(wkniAhora.toLocalDate(), wkni_datos, wkniAvisados));
                    if (wkniEventosOn) wkniPendientes.addAll(wkniRecordatorioService.wkniAvisosEventos(wkniAhora, wkni_datos, wkniMinutos(), wkniAvisados));
                }
                if (!wkniPendientes.isEmpty()) Platform.runLater(() -> wkniMostrarAvisos(wkniPendientes));
            } catch (RuntimeException wkniE) {
                com.agendapersonal.util.wkniLog.wkniError("CentroAvisos", wkniE);
            } finally { wkniOcupado = false; }
        }, "avisos");
        wkniT.setDaemon(true);
        wkniT.start();
    }

    private void wkniMostrarAvisos(List<wkniAviso> wkniLista) {
        for (wkniAviso wkniA : wkniLista) {
            if (!wkniAvisados.add(wkniA.wkniClave())) continue;      // ya se avisó (dos revisiones seguidas)
            wkniNotificacionEscritorio.wkniMostrar(wkniA);
            wkniNotifier.wkniHistory().addFirst(new wkniNotifier.wkniEntry(wkniNotifier.wkniTone.INFO, wkniA.wkniTitulo(), wkniA.wkniMensaje(), LocalDateTime.now()));
            wkniNotifier.wkniUnreadProperty().set(wkniNotifier.wkniUnreadProperty().get() + 1);
        }
    }

    private int wkniMinutos() {
        try { return Math.max(0, Math.min(240, Integer.parseInt(wkniCfg.wkniGet(wkniCFG_MINUTOS, "0").trim()))); }
        catch (NumberFormatException wkniE) { return 0; }
    }

    private wkniCalendarioUnificadoService.wkniDatos wkniDatos(LocalDate wkniHoy) {
        return wkniAgendaDatos.wkniCargar(wkniC, wkniHoy, wkniHoy);
    }
}

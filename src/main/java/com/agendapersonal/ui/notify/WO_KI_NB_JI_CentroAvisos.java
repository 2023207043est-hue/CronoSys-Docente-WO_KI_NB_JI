package com.agendapersonal.ui.notify;

import com.agendapersonal.controller.WO_KI_NB_JI_MainController;
import com.agendapersonal.infrastructure.db.WO_KI_NB_JI_ConfigDAO;
import com.agendapersonal.service.WO_KI_NB_JI_AgendaDatos;
import com.agendapersonal.service.WO_KI_NB_JI_CalendarioUnificadoService;
import com.agendapersonal.service.WO_KI_NB_JI_RecordatorioService;
import com.agendapersonal.service.WO_KI_NB_JI_RecordatorioService.WO_KI_NB_JI_Aviso;
import com.agendapersonal.service.WO_KI_NB_JI_RecordatorioService.WO_KI_NB_JI_Resumen;
import com.agendapersonal.ui.WO_KI_NB_JI_Notifier;
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
public final class WO_KI_NB_JI_CentroAvisos {
    public static final String CFG_RESUMEN = "inicio.resumen";    // "1" muestra el resumen al abrir; por defecto "0"
    public static final String CFG_MINUTOS = "aviso.minutos";     // antelación en minutos; por defecto 0 = al empezar
    public static final String CFG_EVENTOS = "aviso.eventos";     // "1" (por defecto) o "0"
    public static final String CFG_CUMPLES = "aviso.cumples";     // "1" (por defecto) o "0"
    private static final int VENTANA_CUMPLES_DIAS = 7;

    private final WO_KI_NB_JI_MainController c;
    private final Stage ventana;
    private final Runnable irAgenda;
    private final WO_KI_NB_JI_ConfigDAO cfg = new WO_KI_NB_JI_ConfigDAO();
    private final Set<String> avisados = ConcurrentHashMap.newKeySet();
    private volatile boolean ocupado;
    private Timeline reloj;
    private LocalDate ultimoDia = LocalDate.now();

    public WO_KI_NB_JI_CentroAvisos(WO_KI_NB_JI_MainController c, Stage ventana, Runnable irAgenda) {
        this.c = c; this.ventana = ventana; this.irAgenda = irAgenda;
    }

    public void WO_KI_NB_JI_iniciar() {
        LocalDate hoy = LocalDate.now();
        ultimoDia = hoy;
        var datos = WO_KI_NB_JI_datos(hoy);
        if ("1".equals(cfg.WO_KI_NB_JI_get(CFG_RESUMEN, "0"))) {
            WO_KI_NB_JI_Resumen r = WO_KI_NB_JI_RecordatorioService.WO_KI_NB_JI_resumenInicio(hoy, datos, VENTANA_CUMPLES_DIAS);
            if (r.WO_KI_NB_JI_hayNoticias()) WO_KI_NB_JI_mostrarResumen(r);
        }
        WO_KI_NB_JI_verificar(true);
        // Se revisa cada 20 s para que el aviso salga lo más cerca posible de la hora exacta.
        reloj = new Timeline(new KeyFrame(Duration.seconds(20), e -> WO_KI_NB_JI_tick()));
        reloj.setCycleCount(Animation.INDEFINITE);
        reloj.play();
    }

    public void WO_KI_NB_JI_detener() { if (reloj != null) reloj.stop(); }

    /** Muestra la ventana de resumen a pedido (menú de la bandeja). */
    public void WO_KI_NB_JI_mostrarResumenAhora() {
        WO_KI_NB_JI_mostrarResumen(WO_KI_NB_JI_RecordatorioService.WO_KI_NB_JI_resumenInicio(LocalDate.now(), WO_KI_NB_JI_datos(LocalDate.now()), VENTANA_CUMPLES_DIAS));
    }

    private void WO_KI_NB_JI_mostrarResumen(WO_KI_NB_JI_Resumen r) {
        WO_KI_NB_JI_ResumenInicioView.WO_KI_NB_JI_mostrar(ventana.isShowing() ? ventana : null, r, irAgenda,
            noMostrar -> cfg.WO_KI_NB_JI_set(CFG_RESUMEN, noMostrar ? "0" : "1"), "1".equals(cfg.WO_KI_NB_JI_get(CFG_RESUMEN, "0")));
    }

    private void WO_KI_NB_JI_tick() {
        LocalDate hoy = LocalDate.now();
        boolean nuevoDia = !hoy.equals(ultimoDia);
        if (nuevoDia) { ultimoDia = hoy; avisados.clear(); }   // pasó la medianoche con el programa abierto
        WO_KI_NB_JI_verificar(nuevoDia);
    }

    /**
     * Revisa qué hay que avisar ahora. Las consultas a la base de datos (hábitos, contactos, configuración)
     * se hacen en un hilo de fondo para no congelar la ventana; solo mostrar el aviso ocurre en el hilo de JavaFX.
     */
    private void WO_KI_NB_JI_verificar(boolean incluirCumples) {
        if (ocupado) return;
        List<com.agendapersonal.model.WO_KI_NB_JI_Evento> eventos = List.copyOf(c.WO_KI_NB_JI_getEventos());   // copia hecha en el hilo de JavaFX
        List<com.agendapersonal.model.WO_KI_NB_JI_Tarea> tareas = List.copyOf(c.WO_KI_NB_JI_getTareas());
        ocupado = true;
        Thread t = new Thread(() -> {
            try {
                LocalDateTime ahora = LocalDateTime.now();
                boolean eventosOn = !"0".equals(cfg.WO_KI_NB_JI_get(CFG_EVENTOS, "1"));
                boolean cumplesOn = incluirCumples && !"0".equals(cfg.WO_KI_NB_JI_get(CFG_CUMPLES, "1"));
                List<WO_KI_NB_JI_Aviso> pendientes = new ArrayList<>();
                if (eventosOn || cumplesOn) {
                    var datos = WO_KI_NB_JI_AgendaDatos.WO_KI_NB_JI_cargar(eventos, tareas, ahora.toLocalDate(), ahora.toLocalDate());
                    if (cumplesOn) pendientes.addAll(WO_KI_NB_JI_RecordatorioService.WO_KI_NB_JI_avisosCumpleHoy(ahora.toLocalDate(), datos, avisados));
                    if (eventosOn) pendientes.addAll(WO_KI_NB_JI_RecordatorioService.WO_KI_NB_JI_avisosEventos(ahora, datos, WO_KI_NB_JI_minutos(), avisados));
                }
                if (!pendientes.isEmpty()) Platform.runLater(() -> WO_KI_NB_JI_mostrarAvisos(pendientes));
            } catch (RuntimeException e) {
                com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("CentroAvisos", e);
            } finally { ocupado = false; }
        }, "avisos");
        t.setDaemon(true);
        t.start();
    }

    private void WO_KI_NB_JI_mostrarAvisos(List<WO_KI_NB_JI_Aviso> lista) {
        for (WO_KI_NB_JI_Aviso a : lista) {
            if (!avisados.add(a.clave())) continue;      // ya se avisó (dos revisiones seguidas)
            WO_KI_NB_JI_NotificacionEscritorio.WO_KI_NB_JI_mostrar(a);
            WO_KI_NB_JI_Notifier.WO_KI_NB_JI_history().addFirst(new WO_KI_NB_JI_Notifier.WO_KI_NB_JI_Entry(WO_KI_NB_JI_Notifier.WO_KI_NB_JI_Tone.INFO, a.titulo(), a.mensaje(), LocalDateTime.now()));
            WO_KI_NB_JI_Notifier.WO_KI_NB_JI_unreadProperty().set(WO_KI_NB_JI_Notifier.WO_KI_NB_JI_unreadProperty().get() + 1);
        }
    }

    private int WO_KI_NB_JI_minutos() {
        try { return Math.max(0, Math.min(240, Integer.parseInt(cfg.WO_KI_NB_JI_get(CFG_MINUTOS, "0").trim()))); }
        catch (NumberFormatException e) { return 0; }
    }

    private WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_Datos WO_KI_NB_JI_datos(LocalDate hoy) {
        return WO_KI_NB_JI_AgendaDatos.WO_KI_NB_JI_cargar(c, hoy, hoy);
    }
}

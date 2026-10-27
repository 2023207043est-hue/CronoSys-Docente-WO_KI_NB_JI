package com.agendapersonal.service;

import com.agendapersonal.controller.WO_KI_NB_JI_MainController;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

/**
 * Polling ligero (estilo AJAX): cada 15s recarga SQLite en segundo plano.
 * La UI se actualiza sola vía listeners de las ObservableList.
 */
public class WO_KI_NB_JI_DataRefreshService {
    private final WO_KI_NB_JI_MainController c;
    private Timeline timer;

    public WO_KI_NB_JI_DataRefreshService(WO_KI_NB_JI_MainController c) { this.c = c; }

    public void WO_KI_NB_JI_start() {
        if (timer != null) return;
        timer = new Timeline(new KeyFrame(javafx.util.Duration.seconds(15), e -> c.WO_KI_NB_JI_cargarTodoAsync()));
        timer.setCycleCount(Animation.INDEFINITE);
        timer.play();
    }

    public void WO_KI_NB_JI_stop() {
        if (timer != null) timer.stop();
    }

    public void WO_KI_NB_JI_refreshNow() { c.WO_KI_NB_JI_cargarTodoAsync(); }
}

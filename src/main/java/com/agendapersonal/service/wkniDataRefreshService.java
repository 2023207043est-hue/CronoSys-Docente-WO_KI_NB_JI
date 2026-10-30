package com.agendapersonal.service;

import com.agendapersonal.controller.wkniMainController;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;

/**
 * Polling ligero (estilo AJAX): cada 15s recarga SQLite en segundo plano.
 * La UI se actualiza sola vía listeners de las ObservableList.
 */
public class wkniDataRefreshService {
    private final wkniMainController wkniC;
    private Timeline wkniTimer;

    public wkniDataRefreshService(wkniMainController wkniC) { this.wkniC = wkniC; }

    public void wkniStart() {
        if (wkniTimer != null) return;
        wkniTimer = new Timeline(new KeyFrame(javafx.util.Duration.seconds(15), wkniE -> wkniC.wkniCargarTodoAsync()));
        wkniTimer.setCycleCount(Animation.INDEFINITE);
        wkniTimer.play();
    }

    public void wkniStop() {
        if (wkniTimer != null) wkniTimer.stop();
    }

    public void wkniRefreshNow() { wkniC.wkniCargarTodoAsync(); }
}

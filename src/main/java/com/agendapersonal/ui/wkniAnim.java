package com.agendapersonal.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.SequentialTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

import java.util.List;

/** Pequeñas animaciones reutilizables (entrada de páginas, "pop" al marcar, sacudida por error). */
public final class wkniAnim {
    public static final Interpolator wkniEASE_OUT = Interpolator.SPLINE(0.2, 0.9, 0.2, 1);

    private wkniAnim() {}

    /** Entrada suave de una página: aparece y sube unos píxeles. */
    public static void wkniPageIn(Node wkniN) {
        if (wkniThemeService.wkniGet().wkniReduceMotion()) { wkniN.setOpacity(1); wkniN.setTranslateY(0); return; }
        wkniN.setOpacity(0);
        wkniN.setTranslateY(10);
        FadeTransition wkniF = new FadeTransition(Duration.millis(200), wkniN);
        wkniF.setToValue(1);
        TranslateTransition wkniT = new TranslateTransition(Duration.millis(280), wkniN);
        wkniT.setToY(0);
        wkniT.setInterpolator(wkniEASE_OUT);
        new ParallelTransition(wkniF, wkniT).play();
    }

    /** Efecto de "pulsación" al marcar algo (check de tarea o hábito, favorito...). */
    public static void wkniPop(Node wkniN) {
        if (wkniThemeService.wkniGet().wkniReduceMotion()) return;
        ScaleTransition wkniDown = new ScaleTransition(Duration.millis(90), wkniN);
        wkniDown.setToX(0.82);
        wkniDown.setToY(0.82);
        ScaleTransition wkniUp = new ScaleTransition(Duration.millis(220), wkniN);
        wkniUp.setToX(1);
        wkniUp.setToY(1);
        wkniUp.setInterpolator(Interpolator.SPLINE(0.3, 1.6, 0.5, 1));
        new SequentialTransition(wkniDown, wkniUp).play();
    }

    /** Sacude un campo para indicar que falta un dato. */
    public static void wkniShake(Node wkniN) {
        if (wkniThemeService.wkniGet().wkniReduceMotion()) { wkniN.requestFocus(); return; }
        TranslateTransition wkniT = new TranslateTransition(Duration.millis(60), wkniN);
        wkniT.setFromX(0);
        wkniT.setByX(7);
        wkniT.setCycleCount(6);
        wkniT.setAutoReverse(true);
        wkniT.setOnFinished(wkniE -> wkniN.setTranslateX(0));
        wkniT.play();
        wkniN.requestFocus();
    }

    /** Hace aparecer una lista de elementos uno detrás de otro. */
    public static void wkniStagger(List<? extends Node> wkniNodes) {
        if (wkniThemeService.wkniGet().wkniReduceMotion()) { wkniNodes.forEach(wkniX -> { wkniX.setOpacity(1); wkniX.setTranslateY(0); }); return; }
        int wkniI = 0;
        for (Node wkniN : wkniNodes) {
            if (wkniI > 12) break;                       // con muchos elementos no hace falta animar todo
            wkniN.setOpacity(0);
            wkniN.setTranslateY(6);
            FadeTransition wkniF = new FadeTransition(Duration.millis(180), wkniN);
            wkniF.setToValue(1);
            TranslateTransition wkniT = new TranslateTransition(Duration.millis(240), wkniN);
            wkniT.setToY(0);
            wkniT.setInterpolator(wkniEASE_OUT);
            ParallelTransition wkni_p = new ParallelTransition(wkniF, wkniT);
            wkni_p.setDelay(Duration.millis(28L * wkniI++));
            wkni_p.play();
        }
    }

    /** Aparición simple. */
    public static void wkniFadeIn(Node wkniN, double wkniMs) {
        if (wkniThemeService.wkniGet().wkniReduceMotion()) { wkniN.setOpacity(1); return; }
        wkniN.setOpacity(0);
        FadeTransition wkniF = new FadeTransition(Duration.millis(wkniMs), wkniN);
        wkniF.setToValue(1);
        wkniF.play();
    }
}

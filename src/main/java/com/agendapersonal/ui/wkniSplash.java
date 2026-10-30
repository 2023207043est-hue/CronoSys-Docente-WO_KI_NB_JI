package com.agendapersonal.ui;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

/**
 * Pantalla de carga al abrir y despedida al salir. Se dibuja antes de que exista la base de datos
 * y el tema, así que usa los colores de marca fijos (por eso aquí sí hay colores escritos a mano).
 */
public final class wkniSplash {
    private static final String wkniTARJETA = "-fx-background-color: #FBFAF7; -fx-background-radius: 22; -fx-border-radius: 22; -fx-border-color: #E7E5DF;";
    private final Stage wkniStage = new Stage(StageStyle.TRANSPARENT);
    private final Label wkniEstado = new Label();
    private final ProgressBar wkniBarra = new ProgressBar(0);
    private final StackPane wkniRaiz = new StackPane();

    private wkniSplash(String wkniTitulo, String wkniSubtitulo, boolean wkniConBarra) {
        Label wkniNombre = new Label(wkniTitulo);
        wkniNombre.setStyle("-fx-font-size: 26px; -fx-font-weight: bold; -fx-text-fill: #1C1F26;");
        Label wkniSub = new Label(wkniSubtitulo);
        wkniSub.setStyle("-fx-font-size: 13px; -fx-text-fill: #646978;");
        wkniSub.setWrapText(true);
        wkniSub.setAlignment(Pos.CENTER);
        wkniSub.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        wkniEstado.setStyle("-fx-font-size: 12px; -fx-text-fill: #8A8F9C;");
        wkniBarra.setPrefWidth(240);
        wkniBarra.setPrefHeight(6);
        wkniBarra.setStyle("-fx-accent: #E06A33; -fx-control-inner-background: #ECE9E2; -fx-background-color: transparent;");
        VBox wkniCaja = new VBox(14, wkniLogo.wkniMark(84), wkniNombre, wkniSub);
        wkniCaja.setAlignment(Pos.CENTER);
        if (wkniConBarra) wkniCaja.getChildren().addAll(wkniBarra, wkniEstado);
        wkniCaja.setPadding(new Insets(34, 40, 30, 40));
        wkniCaja.setStyle(wkniTARJETA);
        wkniCaja.setPrefSize(400, wkniConBarra ? 330 : 280);
        wkniCaja.setMaxSize(400, wkniConBarra ? 330 : 280);
        wkniCaja.setEffect(new DropShadow(28, 0, 8, Color.rgb(28, 31, 38, 0.25)));
        wkniRaiz.getChildren().add(wkniCaja);
        wkniRaiz.setPadding(new Insets(34));
        wkniRaiz.setStyle("-fx-background-color: transparent;");
        Scene wkniScene = new Scene(wkniRaiz, 468, wkniConBarra ? 398 : 348);
        wkniScene.setFill(Color.TRANSPARENT);
        wkniStage.setScene(wkniScene);
        wkniStage.setTitle("Compás");
        wkniStage.setAlwaysOnTop(true);
        var wkniVb = Screen.getPrimary().getVisualBounds();
        wkniStage.setX(wkniVb.getMinX() + (wkniVb.getWidth() - 468) / 2);
        wkniStage.setY(wkniVb.getMinY() + (wkniVb.getHeight() - wkniScene.getHeight()) / 2);
        try { wkniLogo.wkniAplicarIconos(wkniStage); } catch (RuntimeException wkniIgnored) { }
        wkniRaiz.setOpacity(0);
    }

    /** Carga al abrir. Llama a {@link #avance} entre pasos y a {@link #cerrar} al terminar. */
    public static wkniSplash wkniCarga() { return new wkniSplash("Compás", "Tu tiempo, mejor invertido", true); }

    public void wkniMostrar() {
        wkniStage.show();
        FadeTransition wkniF = new FadeTransition(Duration.millis(260), wkniRaiz);
        wkniF.setToValue(1);
        wkniF.play();
    }

    /** Actualiza el mensaje y la barra (0 a 1). */
    public void wkniAvance(String wkniMensaje, double wkniProgreso) {
        wkniEstado.setText(wkniMensaje);
        wkniBarra.setProgress(Math.max(0, Math.min(1, wkniProgreso)));
    }

    /** Se desvanece y cierra. {@code alCerrar} se ejecuta al terminar. */
    public void wkniCerrar(Runnable wkniAlCerrar) {
        FadeTransition wkniF = new FadeTransition(Duration.millis(320), wkniRaiz);
        wkniF.setToValue(0);
        wkniF.setOnFinished(wkniE -> { wkniStage.close(); if (wkniAlCerrar != null) wkniAlCerrar.run(); });
        wkniF.play();
    }

    /** Despedida: "Hasta pronto" un instante y luego {@code alTerminar} (la salida real). */
    public static void wkniDespedida(String wkniNombre, Runnable wkniAlTerminar) {
        String wkniQuien = wkniNombre == null || wkniNombre.isBlank() ? "" : ", " + wkniNombre.trim();
        wkniSplash wkniS = new wkniSplash("Hasta pronto" + wkniQuien, "Tus datos ya están guardados en este equipo.", false);
        wkniS.wkniMostrar();
        PauseTransition wkniEspera = new PauseTransition(Duration.millis(950));
        wkniEspera.setOnFinished(wkniE -> wkniS.wkniCerrar(wkniAlTerminar));
        wkniEspera.play();
    }
}

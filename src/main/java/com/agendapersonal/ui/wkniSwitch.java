package com.agendapersonal.ui;

import javafx.animation.TranslateTransition;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.geometry.Pos;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/** Interruptor encendido/apagado con animación (para las opciones de Configuración). */
public final class wkniSwitch extends HBox {
    private static final PseudoClass wkniON = PseudoClass.getPseudoClass("on");
    private final BooleanProperty wkniSelected = new SimpleBooleanProperty(false);
    private final Region wkniThumb = new Region();

    public wkniSwitch(String wkniTitle, String wkniHelp, boolean wkniInitial) {
        getStyleClass().add("switch-row");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(16);
        Label wkniT = new Label(wkniTitle);
        wkniT.getStyleClass().add("switch-title");
        VBox wkniTexts = new VBox(2, wkniT);
        if (wkniHelp != null) {
            Label wkniH = new Label(wkniHelp);
            wkniH.getStyleClass().add("switch-help");
            wkniH.setWrapText(true);
            wkniTexts.getChildren().add(wkniH);
        }
        HBox.setHgrow(wkniTexts, Priority.ALWAYS);
        wkniThumb.getStyleClass().add("switch-thumb");
        StackPane wkniTrack = new StackPane(wkniThumb);
        wkniTrack.getStyleClass().add("switch-track");
        wkniTrack.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(wkniTexts, wkniTrack);

        setFocusTraversable(true);
        setAccessibleRole(AccessibleRole.CHECK_BOX);
        setAccessibleText(wkniTitle);
        setOnMouseClicked(wkniE -> wkniSelected.set(!wkniSelected.get()));
        setOnKeyPressed(wkniE -> {
            if (wkniE.getCode() == KeyCode.SPACE || wkniE.getCode() == KeyCode.ENTER) wkniSelected.set(!wkniSelected.get());
        });
        wkniSelected.addListener((wkniO, wkniA, wkniB) -> wkniUpdate(true));
        wkniSelected.set(wkniInitial);
        wkniUpdate(false);
    }

    private void wkniUpdate(boolean wkniAnimate) {
        boolean wkniOn = wkniSelected.get();
        pseudoClassStateChanged(wkniON, wkniOn);
        double wkniX = wkniOn ? 18 : 0;
        if (!wkniAnimate) { wkniThumb.setTranslateX(wkniX); return; }
        TranslateTransition wkniT = new TranslateTransition(Duration.millis(180), wkniThumb);
        wkniT.setToX(wkniX);
        wkniT.setInterpolator(wkniAnim.wkniEASE_OUT);
        wkniT.play();
    }

    public BooleanProperty wkniSelectedProperty() { return wkniSelected; }
    public boolean wkniIsSelected() { return wkniSelected.get(); }
}

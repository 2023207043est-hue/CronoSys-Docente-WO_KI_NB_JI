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
public final class WO_KI_NB_JI_Switch extends HBox {
    private static final PseudoClass ON = PseudoClass.getPseudoClass("on");
    private final BooleanProperty selected = new SimpleBooleanProperty(false);
    private final Region thumb = new Region();

    public WO_KI_NB_JI_Switch(String title, String help, boolean initial) {
        getStyleClass().add("switch-row");
        setAlignment(Pos.CENTER_LEFT);
        setSpacing(16);
        Label t = new Label(title);
        t.getStyleClass().add("switch-title");
        VBox texts = new VBox(2, t);
        if (help != null) {
            Label h = new Label(help);
            h.getStyleClass().add("switch-help");
            h.setWrapText(true);
            texts.getChildren().add(h);
        }
        HBox.setHgrow(texts, Priority.ALWAYS);
        thumb.getStyleClass().add("switch-thumb");
        StackPane track = new StackPane(thumb);
        track.getStyleClass().add("switch-track");
        track.setAlignment(Pos.CENTER_LEFT);
        getChildren().addAll(texts, track);

        setFocusTraversable(true);
        setAccessibleRole(AccessibleRole.CHECK_BOX);
        setAccessibleText(title);
        setOnMouseClicked(e -> selected.set(!selected.get()));
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.SPACE || e.getCode() == KeyCode.ENTER) selected.set(!selected.get());
        });
        selected.addListener((o, a, b) -> WO_KI_NB_JI_update(true));
        selected.set(initial);
        WO_KI_NB_JI_update(false);
    }

    private void WO_KI_NB_JI_update(boolean animate) {
        boolean on = selected.get();
        pseudoClassStateChanged(ON, on);
        double x = on ? 18 : 0;
        if (!animate) { thumb.setTranslateX(x); return; }
        TranslateTransition t = new TranslateTransition(Duration.millis(180), thumb);
        t.setToX(x);
        t.setInterpolator(WO_KI_NB_JI_Anim.EASE_OUT);
        t.play();
    }

    public BooleanProperty WO_KI_NB_JI_selectedProperty() { return selected; }
    public boolean WO_KI_NB_JI_isSelected() { return selected.get(); }
}

package com.agendapersonal.ui;

import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.time.LocalDateTime;

/**
 * Notificaciones dentro de la app: tarjetas pequeñas que entran deslizándose por la esquina superior derecha.
 * <ul>
 *   <li><b>Avisos importantes</b> (WARNING, ERROR): siempre se muestran y quedan en la campana.</li>
 *   <li><b>Confirmaciones</b> (SUCCESS, INFO, DELETE: "se guardó", "se eliminó"…): están APAGADAS por defecto;
 *       los recordatorios de verdad (eventos, cumpleaños) los gestiona {@code CentroAvisos}. Se pueden
 *       activar en Configuración y entonces tampoco llenan la campana.</li>
 * </ul>
 */
public final class wkniNotifier {
    public enum wkniTone {
        SUCCESS("success", "toast-success"), INFO("info", "toast-info"),
        WARNING("warning", "toast-warning"), ERROR("error", "toast-error"), DELETE("delete", "toast-delete");
        final String wkniIcon, wkniCss;
        wkniTone(String wkniIcon, String wkniCss) { this.wkniIcon = wkniIcon; this.wkniCss = wkniCss; }
        public String wkniIcon() { return wkniIcon; }
        public String wkniCss() { return wkniCss; }
    }

    /** Elemento del historial (campana). */
    public record wkniEntry(wkniTone wkni_tone, String wkniTitle, String wkniMessage, LocalDateTime wkniWhen) {}

    private static final int wkniMAX_VISIBLE = 4;
    private static final int wkniMAX_HISTORY = 40;
    private static VBox wkniLayer;
    private static String wkniUltimaHuella = "";
    private static long wkniUltimaVez;
    private static boolean wkniEnabled = false;   // confirmaciones de cambios
    private static final ObservableList<wkniEntry> wkniHISTORY = FXCollections.observableArrayList();
    private static final IntegerProperty wkniUNREAD = new SimpleIntegerProperty(0);

    private wkniNotifier() {}

    /** Capa (sobre todo el contenido de la ventana principal) donde aparecen las tarjetas. */
    public static void wkniInstall(VBox wkniOverlay) {
        wkniLayer = wkniOverlay;
        wkniLayer.setAlignment(Pos.TOP_RIGHT);
        wkniLayer.setSpacing(10);
        wkniLayer.setPadding(new Insets(72, 20, 20, 20));
        wkniLayer.setPickOnBounds(false);
        wkniLayer.setMouseTransparent(false);
    }

    /** Activa/desactiva las confirmaciones ("se guardó…"). Los avisos y errores se muestran siempre. */
    public static void wkniSetEnabled(boolean wkniOn) { wkniEnabled = wkniOn; }
    public static boolean wkniIsEnabled() { return wkniEnabled; }

    public static ObservableList<wkniEntry> wkniHistory() { return wkniHISTORY; }
    public static IntegerProperty wkniUnreadProperty() { return wkniUNREAD; }
    public static void wkniMarkAllRead() { wkniUNREAD.set(0); }

    public static void wkniSuccess(String wkniTitle, String wkniMessage) { wkniShow(wkniTone.SUCCESS, wkniTitle, wkniMessage); }
    public static void wkniInfo(String wkniTitle, String wkniMessage) { wkniShow(wkniTone.INFO, wkniTitle, wkniMessage); }
    public static void wkniWarning(String wkniTitle, String wkniMessage) { wkniShow(wkniTone.WARNING, wkniTitle, wkniMessage); }
    public static void wkniError(String wkniTitle, String wkniMessage) { wkniShow(wkniTone.ERROR, wkniTitle, wkniMessage); }
    public static void wkniDeleted(String wkniTitle, String wkniMessage) { wkniShow(wkniTone.DELETE, wkniTitle, wkniMessage); }

    public static void wkniShow(wkniTone wkni_tone, String wkniTitle, String wkniMessage) {
        if (!Platform.isFxApplicationThread()) { Platform.runLater(() -> wkniShow(wkni_tone, wkniTitle, wkniMessage)); return; }
        boolean wkniConfirmacion = wkni_tone == wkniTone.SUCCESS || wkni_tone == wkniTone.INFO || wkni_tone == wkniTone.DELETE;
        if (wkniConfirmacion && !wkniEnabled) return;                 // sin tarjeta, sin historial, sin contador
        // Un mismo aviso repetido en menos de 2 segundos (p. ej. por un doble evento) se muestra una sola vez.
        String wkniHuella = wkni_tone + "|" + wkniTitle + "|" + wkniMessage;
        long wkniAhora = System.currentTimeMillis();
        if (wkniHuella.equals(wkniUltimaHuella) && wkniAhora - wkniUltimaVez < 2000) return;
        wkniUltimaHuella = wkniHuella;
        wkniUltimaVez = wkniAhora;
        wkniHISTORY.addFirst(new wkniEntry(wkni_tone, wkniTitle, wkniMessage, LocalDateTime.now()));
        if (wkniHISTORY.size() > wkniMAX_HISTORY) wkniHISTORY.remove(wkniMAX_HISTORY, wkniHISTORY.size());
        wkniUNREAD.set(wkniUNREAD.get() + 1);
        if (wkniLayer == null) return;
        while (wkniLayer.getChildren().size() >= wkniMAX_VISIBLE) wkniLayer.getChildren().removeFirst();
        HBox wkniCard = wkniCard(wkni_tone, wkniTitle, wkniMessage);
        wkniLayer.getChildren().add(wkniCard);
        wkniAnimateIn(wkniCard);
    }

    private static HBox wkniCard(wkniTone wkni_tone, String wkniTitle, String wkniMessage) {
        StackPane wkniBadge = new StackPane(wkniIconFactory.wkniIcon(wkni_tone.wkniIcon(), 16));
        wkniBadge.getStyleClass().addAll("toast-badge");
        Label wkniT = new Label(wkniTitle);
        wkniT.getStyleClass().add("toast-title");
        wkniT.setWrapText(true);
        VBox wkniTexts = new VBox(2, wkniT);
        if (wkniMessage != null && !wkniMessage.isBlank()) {
            Label wkniM = new Label(wkniMessage);
            wkniM.getStyleClass().add("toast-message");
            wkniM.setWrapText(true);
            wkniTexts.getChildren().add(wkniM);
        }
        HBox.setHgrow(wkniTexts, Priority.ALWAYS);
        Button wkniClose = new Button();
        wkniClose.setGraphic(wkniIconFactory.wkniIcon("close", 14));
        wkniClose.getStyleClass().addAll("icon-button", "toast-close");
        wkniClose.setAccessibleText("Cerrar aviso");
        HBox wkniCard = new HBox(12, wkniBadge, wkniTexts, wkniClose);
        wkniCard.getStyleClass().addAll("toast", wkni_tone.wkniCss());
        wkniCard.setAlignment(Pos.TOP_LEFT);
        wkniCard.setMaxWidth(340);
        wkniCard.setMinWidth(260);
        wkniCard.setPrefWidth(320);
        wkniCard.setAlignment(Pos.CENTER_LEFT);

        PauseTransition wkniLife = new PauseTransition(Duration.seconds(wkni_tone == wkniTone.ERROR ? 6 : 4));
        wkniLife.setOnFinished(wkniE -> wkniAnimateOut(wkniCard));
        wkniCard.setOnMouseEntered(wkniE -> wkniLife.pause());
        wkniCard.setOnMouseExited(wkniE -> wkniLife.play());
        wkniClose.setOnAction(wkniE -> { wkniLife.stop(); wkniAnimateOut(wkniCard); });
        wkniCard.getProperties().put("life", wkniLife);
        wkniLife.play();
        return wkniCard;
    }

    private static void wkniAnimateIn(HBox wkniCard) {
        wkniCard.setOpacity(0);
        wkniCard.setTranslateX(48);
        FadeTransition wkniF = new FadeTransition(Duration.millis(220), wkniCard);
        wkniF.setToValue(1);
        TranslateTransition wkniT = new TranslateTransition(Duration.millis(320), wkniCard);
        wkniT.setToX(0);
        wkniT.setInterpolator(Interpolator.SPLINE(0.2, 0.9, 0.2, 1));
        new ParallelTransition(wkniF, wkniT).play();
    }

    private static void wkniAnimateOut(HBox wkniCard) {
        if (wkniCard.getProperties().containsKey("closing")) return;
        wkniCard.getProperties().put("closing", true);
        FadeTransition wkniF = new FadeTransition(Duration.millis(200), wkniCard);
        wkniF.setToValue(0);
        TranslateTransition wkniT = new TranslateTransition(Duration.millis(200), wkniCard);
        wkniT.setToX(40);
        ParallelTransition wkni_p = new ParallelTransition(wkniF, wkniT);
        wkni_p.setOnFinished(wkniE -> { if (wkniLayer != null) wkniLayer.getChildren().remove(wkniCard); });
        wkni_p.play();
    }
}

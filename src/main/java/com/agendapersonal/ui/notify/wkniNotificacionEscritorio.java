package com.agendapersonal.ui.notify;

import com.agendapersonal.service.wkniRecordatorioService.wkniAviso;
import com.agendapersonal.service.wkniTipoEntrada;
import com.agendapersonal.ui.wkniIconFactory;
import com.agendapersonal.ui.wkniLogo;
import com.agendapersonal.ui.wkniThemeService;
import com.agendapersonal.util.wkniUi;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.ArrayList;
import java.util.List;

/**
 * Notificación flotante a nivel de ESCRITORIO: aparece en la esquina inferior derecha de la pantalla,
 * por encima de las demás ventanas, aunque Compás esté minimizado u oculto en la bandeja.
 * Toma los colores del tema activo, entra deslizándose, se apila si hay varias y se pausa con el mouse.
 */
public final class wkniNotificacionEscritorio {
    private static final double wkniANCHO = 370, wkniMARGEN = 16, wkniSEPARACION = 10;
    private static final List<Stage> wkniActivas = new ArrayList<>();
    private static Stage wkniDueno;                       // ventana invisible: evita que cada aviso salga en la barra de tareas
    private static Runnable wkniAlAbrir = () -> {};

    private wkniNotificacionEscritorio() {}

    /** Acción del botón "Abrir" (normalmente: mostrar la ventana principal). */
    public static void wkniSetAlAbrir(Runnable wkniR) { wkniAlAbrir = wkniR == null ? () -> {} : wkniR; }

    public static void wkniMostrar(wkniAviso wkniA) { wkniMostrar(wkniA.wkni_tipo(), wkniA.wkniTitulo(), wkniA.wkniMensaje(), 14); }

    public static void wkniMostrar(wkniTipoEntrada wkni_tipo, String wkniTitulo, String wkniMensaje, int wkniSegundos) {
        if (wkniEnSilencio()) return;      // horas de silencio: no se muestran avisos de escritorio
        if (Platform.isFxApplicationThread()) wkniConstruir(wkni_tipo, wkniTitulo, wkniMensaje, wkniSegundos);
        else Platform.runLater(() -> wkniConstruir(wkni_tipo, wkniTitulo, wkniMensaje, wkniSegundos));
    }

    /** true si ahora cae dentro de las horas de silencio configuradas (Configuración → Avisos). */
    public static boolean wkniEnSilencio() {
        var wkniSt = com.agendapersonal.settings.wkniSettingsStore.wkniGet();
        String wkniD = wkniSt.wkniStr("notifications.quietHours.from"), wkniH = wkniSt.wkniStr("notifications.quietHours.to");
        if (!wkniD.matches("([01]?\\d|2[0-3]):[0-5]\\d") || !wkniH.matches("([01]?\\d|2[0-3]):[0-5]\\d")) return false;
        int wkniA = com.agendapersonal.settings.wkniSettingsStore.wkniMinutes(wkniD), wkniB = com.agendapersonal.settings.wkniSettingsStore.wkniMinutes(wkniH);
        java.time.LocalTime wkniNow = java.time.LocalTime.now();
        int wkniM = wkniNow.getHour() * 60 + wkniNow.getMinute();
        if (wkniA == wkniB) return false;
        return wkniA < wkniB ? (wkniM >= wkniA && wkniM < wkniB) : (wkniM >= wkniA || wkniM < wkniB);
    }

    private static Stage wkniDueno() {
        if (wkniDueno == null) {
            wkniDueno = new Stage(StageStyle.UTILITY);
            wkniDueno.setOpacity(0);
            wkniDueno.setWidth(1);
            wkniDueno.setHeight(1);
            wkniDueno.setX(-10000);
            wkniDueno.setY(-10000);
            wkniDueno.show();
        }
        return wkniDueno;
    }

    private static String wkniTono(wkniTipoEntrada wkniT) {
        return switch (wkniT) {
            case EVENTO -> "teal"; case TRABAJO -> "blue"; case TAREA -> "violet"; case HABITO -> "green"; case CUMPLE -> "rose";
        };
    }

    private static void wkniConstruir(wkniTipoEntrada wkni_tipo, String wkniTitulo, String wkniMensaje, int wkniSegundos) {
        Stage wkniS = new Stage(StageStyle.TRANSPARENT);
        wkniS.initOwner(wkniDueno());
        wkniS.setAlwaysOnTop(true);

        String wkni_tono = wkniTono(wkni_tipo);
        StackPane wkniIco = wkniUi.wkniIconChip(wkni_tipo.wkniIcono(), wkni_tono, 40, 20);
        Label wkniApp = wkniUi.wkniLabel("Compás · ahora", "caption");
        wkniApp.setGraphic(wkniLogo.wkniMark(14));
        wkniApp.setGraphicTextGap(6);
        Label wkniT = wkniUi.wkniLabel(wkniTitulo, "toast-title");
        wkniT.setWrapText(true);
        Label wkniM = wkniUi.wkniLabel(wkniMensaje == null ? "" : wkniMensaje, "toast-message");
        wkniM.setWrapText(true);
        VBox wkniTextos = new VBox(3, wkniApp, wkniT, wkniM);
        HBox.setHgrow(wkniTextos, Priority.ALWAYS);
        Button wkniX = new Button();
        wkniX.setGraphic(wkniIconFactory.wkniIcon("close", 14));
        wkniX.getStyleClass().addAll("icon-button", "toast-close");
        wkniX.setAccessibleText("Cerrar notificación");
        wkniX.setOnAction(wkniE -> wkniCerrar(wkniS));
        HBox wkniFila = new HBox(12, wkniIco, wkniTextos, wkniX);
        wkniFila.setAlignment(Pos.TOP_LEFT);

        Button wkniAbrir = wkniUi.wkniPrimary(null, "Abrir Compás");
        wkniAbrir.setOnAction(wkniE -> { wkniCerrar(wkniS); wkniAlAbrir.run(); });
        Button wkniLuego = wkniUi.wkniGhost(null, "Descartar");
        wkniLuego.setOnAction(wkniE -> wkniCerrar(wkniS));
        HBox wkniAcciones = new HBox(8, wkniUi.wkniSpacer(), wkniLuego, wkniAbrir);

        VBox wkniTarjeta = new VBox(12, wkniFila, wkniAcciones);
        wkniTarjeta.setPadding(new Insets(14, 14, 12, 14));
        wkniTarjeta.setPrefWidth(wkniANCHO);
        wkniTarjeta.setMaxWidth(wkniANCHO);
        wkniTarjeta.getStyleClass().addAll("desktop-toast", "accent-left-" + wkni_tono);

        StackPane wkniRaiz = new StackPane(wkniTarjeta);
        wkniRaiz.setPadding(new Insets(12));          // espacio para la sombra
        wkniRaiz.setStyle("-fx-background-color:transparent;");
        Scene wkniSc = new Scene(wkniRaiz);
        wkniSc.setFill(Color.TRANSPARENT);
        wkniThemeService.wkniGet().wkniApplyTo(wkniSc);
        wkniS.setScene(wkniSc);

        PauseTransition wkniVida = new PauseTransition(Duration.seconds(Math.max(3, wkniSegundos)));
        wkniVida.setOnFinished(wkniE -> wkniCerrar(wkniS));
        wkniRaiz.setOnMouseEntered(wkniE -> wkniVida.pause());     // si lo estás leyendo, no desaparece
        wkniRaiz.setOnMouseExited(wkniE -> wkniVida.play());

        wkniActivas.add(wkniS);
        wkniS.show();
        wkniReacomodar();
        wkniTarjeta.setOpacity(0);
        wkniTarjeta.setTranslateX(60);
        FadeTransition wkniIn = new FadeTransition(Duration.millis(240), wkniTarjeta);
        wkniIn.setToValue(1);
        TranslateTransition wkniSlide = new TranslateTransition(Duration.millis(360), wkniTarjeta);
        wkniSlide.setToX(0);
        wkniSlide.setInterpolator(Interpolator.SPLINE(0.2, 0.9, 0.2, 1));
        new ParallelTransition(wkniIn, wkniSlide).play();
        wkniVida.play();
    }

    private static void wkniCerrar(Stage wkniS) {
        if (!wkniActivas.contains(wkniS)) return;
        var wkniTarjeta = ((StackPane) wkniS.getScene().getRoot()).getChildren().getFirst();
        FadeTransition wkniOut = new FadeTransition(Duration.millis(180), wkniTarjeta);
        wkniOut.setToValue(0);
        wkniOut.setOnFinished(wkniE -> {
            wkniActivas.remove(wkniS);
            wkniS.hide();
            wkniReacomodar();
        });
        wkniOut.play();
    }

    /** Apila las notificaciones desde la esquina inferior derecha hacia arriba. */
    private static void wkniReacomodar() {
        Rectangle2D wkniV = Screen.getPrimary().getVisualBounds();   // excluye la barra de tareas
        double wkniBorde = wkniV.getMaxY() - wkniMARGEN + 12;
        for (int wkniI = wkniActivas.size() - 1; wkniI >= 0; wkniI--) {
            Stage wkniS = wkniActivas.get(wkniI);
            double wkniH = wkniS.getHeight();
            wkniS.setX(wkniV.getMaxX() - wkniS.getWidth() - wkniMARGEN + 12);
            wkniS.setY(wkniBorde - wkniH);
            wkniBorde = wkniBorde - wkniH + 24 - wkniSEPARACION;
        }
    }

    /** Cierra todo (al salir del programa). */
    public static void wkniCerrarTodas() {
        for (Stage wkniS : new ArrayList<>(wkniActivas)) wkniS.hide();
        wkniActivas.clear();
        if (wkniDueno != null) { wkniDueno.hide(); wkniDueno = null; }
    }
}

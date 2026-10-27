package com.agendapersonal.ui.notify;

import com.agendapersonal.service.WO_KI_NB_JI_RecordatorioService.WO_KI_NB_JI_Aviso;
import com.agendapersonal.service.WO_KI_NB_JI_TipoEntrada;
import com.agendapersonal.ui.WO_KI_NB_JI_IconFactory;
import com.agendapersonal.ui.WO_KI_NB_JI_Logo;
import com.agendapersonal.ui.WO_KI_NB_JI_ThemeService;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
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
public final class WO_KI_NB_JI_NotificacionEscritorio {
    private static final double ANCHO = 370, MARGEN = 16, SEPARACION = 10;
    private static final List<Stage> activas = new ArrayList<>();
    private static Stage dueno;                       // ventana invisible: evita que cada aviso salga en la barra de tareas
    private static Runnable alAbrir = () -> {};

    private WO_KI_NB_JI_NotificacionEscritorio() {}

    /** Acción del botón "Abrir" (normalmente: mostrar la ventana principal). */
    public static void WO_KI_NB_JI_setAlAbrir(Runnable r) { alAbrir = r == null ? () -> {} : r; }

    public static void WO_KI_NB_JI_mostrar(WO_KI_NB_JI_Aviso a) { WO_KI_NB_JI_mostrar(a.tipo(), a.titulo(), a.mensaje(), 14); }

    public static void WO_KI_NB_JI_mostrar(WO_KI_NB_JI_TipoEntrada tipo, String titulo, String mensaje, int segundos) {
        if (WO_KI_NB_JI_enSilencio()) return;      // horas de silencio: no se muestran avisos de escritorio
        if (Platform.isFxApplicationThread()) WO_KI_NB_JI_construir(tipo, titulo, mensaje, segundos);
        else Platform.runLater(() -> WO_KI_NB_JI_construir(tipo, titulo, mensaje, segundos));
    }

    /** true si ahora cae dentro de las horas de silencio configuradas (Configuración → Avisos). */
    public static boolean WO_KI_NB_JI_enSilencio() {
        var st = com.agendapersonal.settings.WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_get();
        String d = st.WO_KI_NB_JI_str("notifications.quietHours.from"), h = st.WO_KI_NB_JI_str("notifications.quietHours.to");
        if (!d.matches("([01]?\\d|2[0-3]):[0-5]\\d") || !h.matches("([01]?\\d|2[0-3]):[0-5]\\d")) return false;
        int a = com.agendapersonal.settings.WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_minutes(d), b = com.agendapersonal.settings.WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_minutes(h);
        java.time.LocalTime now = java.time.LocalTime.now();
        int m = now.getHour() * 60 + now.getMinute();
        if (a == b) return false;
        return a < b ? (m >= a && m < b) : (m >= a || m < b);
    }

    private static Stage WO_KI_NB_JI_dueno() {
        if (dueno == null) {
            dueno = new Stage(StageStyle.UTILITY);
            dueno.setOpacity(0);
            dueno.setWidth(1);
            dueno.setHeight(1);
            dueno.setX(-10000);
            dueno.setY(-10000);
            dueno.show();
        }
        return dueno;
    }

    private static String WO_KI_NB_JI_tono(WO_KI_NB_JI_TipoEntrada t) {
        return switch (t) {
            case EVENTO -> "teal"; case TRABAJO -> "blue"; case TAREA -> "violet"; case HABITO -> "green"; case CUMPLE -> "rose";
        };
    }

    private static void WO_KI_NB_JI_construir(WO_KI_NB_JI_TipoEntrada tipo, String titulo, String mensaje, int segundos) {
        Stage s = new Stage(StageStyle.TRANSPARENT);
        s.initOwner(WO_KI_NB_JI_dueno());
        s.setAlwaysOnTop(true);

        String tono = WO_KI_NB_JI_tono(tipo);
        StackPane ico = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconChip(tipo.WO_KI_NB_JI_icono(), tono, 40, 20);
        Label app = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("Compás · ahora", "caption");
        app.setGraphic(WO_KI_NB_JI_Logo.WO_KI_NB_JI_mark(14));
        app.setGraphicTextGap(6);
        Label t = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(titulo, "toast-title");
        t.setWrapText(true);
        Label m = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(mensaje == null ? "" : mensaje, "toast-message");
        m.setWrapText(true);
        VBox textos = new VBox(3, app, t, m);
        HBox.setHgrow(textos, Priority.ALWAYS);
        Button x = new Button();
        x.setGraphic(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("close", 14));
        x.getStyleClass().addAll("icon-button", "toast-close");
        x.setAccessibleText("Cerrar notificación");
        x.setOnAction(e -> WO_KI_NB_JI_cerrar(s));
        HBox fila = new HBox(12, ico, textos, x);
        fila.setAlignment(Pos.TOP_LEFT);

        Button abrir = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary(null, "Abrir Compás");
        abrir.setOnAction(e -> { WO_KI_NB_JI_cerrar(s); alAbrir.run(); });
        Button luego = WO_KI_NB_JI_Ui.WO_KI_NB_JI_ghost(null, "Descartar");
        luego.setOnAction(e -> WO_KI_NB_JI_cerrar(s));
        HBox acciones = new HBox(8, WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), luego, abrir);

        VBox tarjeta = new VBox(12, fila, acciones);
        tarjeta.setPadding(new Insets(14, 14, 12, 14));
        tarjeta.setPrefWidth(ANCHO);
        tarjeta.setMaxWidth(ANCHO);
        tarjeta.getStyleClass().addAll("desktop-toast", "accent-left-" + tono);

        StackPane raiz = new StackPane(tarjeta);
        raiz.setPadding(new Insets(12));          // espacio para la sombra
        raiz.setStyle("-fx-background-color:transparent;");
        Scene sc = new Scene(raiz);
        sc.setFill(Color.TRANSPARENT);
        WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_get().WO_KI_NB_JI_applyTo(sc);
        s.setScene(sc);

        PauseTransition vida = new PauseTransition(Duration.seconds(Math.max(3, segundos)));
        vida.setOnFinished(e -> WO_KI_NB_JI_cerrar(s));
        raiz.setOnMouseEntered(e -> vida.pause());     // si lo estás leyendo, no desaparece
        raiz.setOnMouseExited(e -> vida.play());

        activas.add(s);
        s.show();
        WO_KI_NB_JI_reacomodar();
        tarjeta.setOpacity(0);
        tarjeta.setTranslateX(60);
        FadeTransition in = new FadeTransition(Duration.millis(240), tarjeta);
        in.setToValue(1);
        TranslateTransition slide = new TranslateTransition(Duration.millis(360), tarjeta);
        slide.setToX(0);
        slide.setInterpolator(Interpolator.SPLINE(0.2, 0.9, 0.2, 1));
        new ParallelTransition(in, slide).play();
        vida.play();
    }

    private static void WO_KI_NB_JI_cerrar(Stage s) {
        if (!activas.contains(s)) return;
        var tarjeta = ((StackPane) s.getScene().getRoot()).getChildren().getFirst();
        FadeTransition out = new FadeTransition(Duration.millis(180), tarjeta);
        out.setToValue(0);
        out.setOnFinished(e -> {
            activas.remove(s);
            s.hide();
            WO_KI_NB_JI_reacomodar();
        });
        out.play();
    }

    /** Apila las notificaciones desde la esquina inferior derecha hacia arriba. */
    private static void WO_KI_NB_JI_reacomodar() {
        Rectangle2D v = Screen.getPrimary().getVisualBounds();   // excluye la barra de tareas
        double borde = v.getMaxY() - MARGEN + 12;
        for (int i = activas.size() - 1; i >= 0; i--) {
            Stage s = activas.get(i);
            double h = s.getHeight();
            s.setX(v.getMaxX() - s.getWidth() - MARGEN + 12);
            s.setY(borde - h);
            borde = borde - h + 24 - SEPARACION;
        }
    }

    /** Cierra todo (al salir del programa). */
    public static void WO_KI_NB_JI_cerrarTodas() {
        for (Stage s : new ArrayList<>(activas)) s.hide();
        activas.clear();
        if (dueno != null) { dueno.hide(); dueno = null; }
    }
}

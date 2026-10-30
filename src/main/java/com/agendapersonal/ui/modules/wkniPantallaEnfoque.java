package com.agendapersonal.ui.modules;

import com.agendapersonal.service.wkniListaPendientes;
import com.agendapersonal.service.wkniMetodoEnfoque;
import com.agendapersonal.service.wkniSesionEnfoque;
import com.agendapersonal.settings.wkniColors;
import com.agendapersonal.settings.wkniSettingsStore;
import com.agendapersonal.ui.wkniIconFactory;
import com.agendapersonal.ui.wkniSwitch;
import com.agendapersonal.ui.wkniThemeService;
import com.agendapersonal.util.wkniUi;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * Pantalla de bloqueo del tiempo: reloj, barra de progreso, sesión y ciclo, botones de control y
 * lista de pendientes opcional (de tus tareas o de las casillas de tus notas).
 *
 * <p>Todo se puede cambiar <b>desde la propia pantalla</b> (botón «Ajustes»): el tamaño del reloj, qué
 * elementos se ven, el fondo (imagen o video en bucle), el velo, el sonido y la importancia. Los
 * cambios se guardan en tu perfil, así que la próxima vez ya están aplicados.</p>
 *
 * <p><b>Límite honesto:</b> una app de escritorio hecha en Java no puede tapar ni cerrar otros
 * programas del sistema (Windows no lo permite sin permisos de administrador). Lo que sí hace la
 *.importancia <i>Estricta</i> es pantalla completa, ventana siempre al frente, pausa del contador
 * en cuanto sales o minimizas, y bloqueo de los atajos de teclado dentro de la app.</p>
 */
public final class wkniPantallaEnfoque {

    /** Cuánto manda la sesión sobre tu concentración. */
    public enum wkniImportancia {
        NORMAL("Normal", "Puedes salir y volver cuando quieras."),
        ALTA("Alta", "Si minimizas o cambias de ventana, el contador se pausa."),
        ESTRICTA("Estricta", "Pantalla completa, siempre al frente y sin atajos. El contador se pausa al salir.");
        private final String wkniEtiqueta, wkniAyuda;
        wkniImportancia(String wkniE, String wkniA) { wkniEtiqueta = wkniE; wkniAyuda = wkniA; }
        @Override public String toString() { return wkniEtiqueta; }
        public String wkniAyuda() { return wkniAyuda; }
        public static wkniImportancia wkniDesde(String wkniV) { try { return valueOf(wkniV); } catch (Exception wkniE) { return NORMAL; } }
    }

    private static final java.util.Set<String> wkniVIDEO = java.util.Set.of("mp4", "m4v", "webm", "ogv");
    private static final java.util.Set<String> wkniIMAGEN = java.util.Set.of("png", "jpg", "jpeg", "gif", "bmp", "webp");

    private final wkniSesionEnfoque wkniSesion;
    private final Stage wkniStage = new Stage();
    private final wkniSettingsStore wkniSt = wkniThemeService.wkniGet().wkniSettings();

    private final Label wkni_fase = new Label(), wkniReloj = new Label(), wkniDetalle = new Label(), wkni_aviso = new Label();
    private final Label wkni_tarea = new Label();
    private final ProgressBar wkniBarra = new ProgressBar(0);
    private final VBox wkni_listaPendientes = new VBox(8);

    private final Button wkniPlay = wkniUi.wkniPrimary(null, "Iniciar");
    private final Button wkniReiniciar = wkniUi.wkniSecondary(null, "Reiniciar tiempo");
    private final Button wkniSiguiente = wkniUi.wkniSecondary(null, "Saltar fase");
    private final Button wkniTerminarFlow = wkniUi.wkniSecondary(null, "Terminar sesión");
    private final Button wkniDetener = wkniUi.wkniSecondary(null, "Detener");
    private final Button wkniBotonFondo = wkniUi.wkniSecondary("grid", "Agregar fondo");
    private final Button wkniBotonAjustes = wkniUi.wkniSecondary("settings", "Ajustes");

    private final StackPane wkniRaiz = new StackPane();
    private final StackPane wkniCapaFondo = new StackPane();
    private final StackPane wkniCapaContenido = new StackPane();
    private final Region wkniVelo = new Region();
    private final VBox wkniPanelAjustes = new VBox(14);
    private final ScrollPane wkniScrollPendientes = new ScrollPane(wkni_listaPendientes);

    private MediaPlayer wkniVideo;
    private MediaPlayer wkniAudio;
    private Timeline wkniTic;
    private int wkniSalidas;
    private boolean wkniAjustesAbiertos;
    private boolean wkniDialogoAbierto;
    private boolean wkniFiltroTeclado;
    private List<wkniListaPendientes.wkniItem> wkniPendientes = new ArrayList<>();
    private BiConsumer<wkniListaPendientes.wkniItem, Boolean> wkniAlAlternar;
    private wkniImportancia wkni_importancia;

    /** Abre la pantalla de bloqueo. {@code alAlternar} guarda los cambios de los pendientes (puede ser null). */
    public static void wkniAbrir(wkniMetodoEnfoque wkniMetodo, String wkniTitulo, wkniImportancia wkniImp, List<wkniListaPendientes.wkniItem> wkniPendientes,
                             BiConsumer<wkniListaPendientes.wkniItem, Boolean> wkniAlAlternar) {
        new wkniPantallaEnfoque(wkniMetodo, wkniTitulo, wkniImp, wkniPendientes, wkniAlAlternar).wkniStage.show();
    }

    private wkniPantallaEnfoque(wkniMetodoEnfoque wkniMetodo, String wkniTitulo, wkniImportancia wkniImp,
                            List<wkniListaPendientes.wkniItem> wkniPendientes, BiConsumer<wkniListaPendientes.wkniItem, Boolean> wkniAlAlternar) {
        this.wkniSesion = new wkniSesionEnfoque(wkniMetodo);
        this.wkni_importancia = wkniImp == null ? wkniImportancia.NORMAL : wkniImp;
        this.wkniPendientes = wkniPendientes == null ? new ArrayList<>() : new ArrayList<>(wkniPendientes);
        this.wkniAlAlternar = wkniAlAlternar;

        wkniConstruirFondo();
        wkniConstruirContenido(wkniTitulo, wkniMetodo);

        Scene wkniScene = new Scene(wkniRaiz, 1040, 660);
        wkniThemeService.wkniGet().wkniApplyTo(wkniScene);
        wkniStage.setScene(wkniScene);
        wkniStage.setTitle("Compás · " + wkniMetodo.wkniNombre());
        wkniAplicarImportancia();
        // Al mostrarse se reinscribe el modo Estricta (pantalla completa y al frente).
        wkniStage.setOnShown(wkniE -> { if (wkni_importancia == wkniImportancia.ESTRICTA) { wkniStage.toFront(); wkniStage.requestFocus(); } });

        // Salir de la pantalla: minimizar o cambiar de ventana.
        wkniStage.focusedProperty().addListener((wkniO, wkniA, wkniEnfocada) -> {
            if (!wkniEnfocada) wkniSalio();
            else if (wkni_importancia == wkniImportancia.ESTRICTA) wkniStage.toFront();
        });
        wkniStage.iconifiedProperty().addListener((wkniO, wkniA, wkniMin) -> { if (wkniMin) wkniSalio(); });

        wkniPlay.setOnAction(wkniE -> wkniAlternar());
        wkniReiniciar.setOnAction(wkniE -> { wkniSesion.wkniReiniciarFase(); wkni_aviso.setText(""); wkniActualizar(); });
        wkniSiguiente.setOnAction(wkniE -> { wkniSesion.wkniSiguiente(); wkniActualizar(); });
        wkniTerminarFlow.setOnAction(wkniE -> { wkniSesion.wkniTerminarFlow(); wkniSesion.wkniIniciar(); wkniActualizar(); });
        wkniDetener.setOnAction(wkniE -> wkniCerrar(true));
        wkniBotonFondo.setOnAction(wkniE -> wkniElegirFondo());
        wkniBotonAjustes.setOnAction(wkniE -> wkniAlternarAjustes());
        wkniStage.setOnCloseRequest(wkniE -> { wkniE.consume(); wkniCerrar(true); });

        wkniTic = new Timeline(new KeyFrame(Duration.seconds(1), wkniE -> {
            boolean wkniCambio = wkniSesion.wkniTick(1);
            if (wkniCambio) {
                if (wkniSt.wkniBool("timeBlocking.sound")) wkniBeep();
                wkniAvisarFinDeFase();
            }
            wkniActualizar();
        }));
        wkniTic.setCycleCount(Animation.INDEFINITE);
        wkniTic.play();
        wkniIniciarAudio();
        wkniActualizar();
    }

    // ------------------------------------------------------------------ construcción

    /** Aplica el fondo guardado. Devuelve false si no se pudo y se quedó con el degradado del tema. */
    private boolean wkniConstruirFondo() {
        wkniVelo.setStyle("-fx-background-color: black;");
        wkniVelo.setOpacity(wkniSt.wkniInteger("timeBlocking.backgroundDim", 0, 90) / 100.0);
        wkniVelo.setMouseTransparent(true);
        wkniCapaFondo.getChildren().clear();
        if (wkniVideo != null) { wkniVideo.dispose(); wkniVideo = null; }

        String wkniModo = wkniSt.wkniChoice("timeBlocking.background", "degradado", "archivo", "color");
        String wkniRuta = wkniSt.wkniStr("timeBlocking.backgroundFile");
        String wkniColor = wkniColors.wkniNormalize(wkniSt.wkniStr("timeBlocking.backgroundColor"));
        boolean wkniPuesto = false;
        if (wkniModo.equals("archivo") && !wkniRuta.isBlank() && new File(wkniRuta).isFile()) {
            wkniPuesto = wkniPonerFondoArchivo(new File(wkniRuta));
        } else if (wkniModo.equals("color") && wkniColor != null) {
            wkniCapaFondo.setStyle("-fx-background-color: " + wkniColor + ";");
            wkniPuesto = true;
        }
        if (!wkniPuesto) wkniCapaFondo.setStyle("-fx-background-color: linear-gradient(to bottom right, -bg, -surface3);");
        wkniRaiz.getChildren().setAll(wkniCapaFondo, wkniVelo, wkniCapaContenido);
        return wkniPuesto;
    }

    /** Pone una imagen o un video de fondo. Devuelve false si el archivo no se pudo usar. */
    private boolean wkniPonerFondoArchivo(File wkniF) {
        String wkniExt = wkniExtension(wkniF);
        try {
            if (wkniVIDEO.contains(wkniExt)) {
                wkniVideo = new MediaPlayer(new Media(wkniF.toURI().toString()));
                wkniVideo.setCycleCount(wkniSt.wkniBool("timeBlocking.backgroundLoop") ? MediaPlayer.INDEFINITE : 1);
                wkniVideo.setMute(true);
                MediaView wkniMv = new MediaView(wkniVideo);
                wkniMv.fitWidthProperty().bind(wkniCapaFondo.widthProperty());
                wkniMv.fitHeightProperty().bind(wkniCapaFondo.heightProperty());
                wkniMv.setPreserveRatio(false);
                wkniMv.setMouseTransparent(true);
                wkniCapaFondo.getChildren().setAll(wkniMv);
                wkniVideo.play();
                return true;
            }
            if (wkniIMAGEN.contains(wkniExt)) {
                ImageView wkniIv = new ImageView(new Image(wkniF.toURI().toString()));
                wkniIv.fitWidthProperty().bind(wkniCapaFondo.widthProperty());
                wkniIv.fitHeightProperty().bind(wkniCapaFondo.heightProperty());
                wkniIv.setPreserveRatio(false);
                wkniIv.setMouseTransparent(true);
                wkniCapaFondo.getChildren().setAll(wkniIv);
                return true;
            }
        } catch (RuntimeException wkniEx) {
            if (wkniVideo != null) { wkniVideo.dispose(); wkniVideo = null; }
        }
        return false;
    }

    private void wkniConstruirContenido(String wkniTitulo, wkniMetodoEnfoque wkniMetodo) {
        wkni_fase.getStyleClass().add("h2");
        wkniReloj.getStyleClass().add("enfoque-reloj");
        wkniReloj.setStyle("-fx-font-size: " + wkniTamanoReloj() + "px;");
        wkniDetalle.getStyleClass().add("caption");
        wkniDetalle.setWrapText(true);
        wkniDetalle.setMaxWidth(560);
        wkniDetalle.setAlignment(Pos.CENTER);
        wkni_aviso.getStyleClass().add("caption");
        wkni_aviso.setWrapText(true);
        wkni_aviso.setMaxWidth(560);
        wkniBarra.setPrefHeight(14);
        wkniBarra.setMaxWidth(Double.MAX_VALUE);
        wkniBarra.getStyleClass().add("enfoque-barra");

        wkni_tarea.setText(wkniTitulo == null || wkniTitulo.isBlank() ? wkniMetodo.wkniNombre() : wkniTitulo);
        wkni_tarea.getStyleClass().add("h3");
        wkni_tarea.setWrapText(true);
        wkni_tarea.setMaxWidth(560);
        wkni_tarea.setAlignment(Pos.CENTER);
        wkni_tarea.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        HBox wkniControles = new HBox(10, wkniPlay, wkniReiniciar, wkniSiguiente, wkniTerminarFlow, wkniDetener);
        wkniControles.setAlignment(Pos.CENTER);
        HBox wkniExtras = new HBox(10, wkniBotonFondo, wkniBotonAjustes);
        wkniExtras.setAlignment(Pos.CENTER);

        VBox wkniCuerpo = new VBox(12, wkni_tarea, wkni_fase, wkniReloj, wkniBarra, wkniDetalle, wkniControles, wkniExtras, wkni_aviso);
        wkniCuerpo.setAlignment(Pos.CENTER);
        wkniCuerpo.setMaxWidth(640);
        VBox wkniCentro = new VBox(wkniCuerpo);
        wkniCentro.setPadding(new Insets(30));
        wkniCentro.setAlignment(Pos.CENTER);
        wkniCentro.setMaxWidth(640);
        wkniCentro.getStyleClass().add("card");
        wkniCentro.getStyleClass().add("enfoque-centro");
        VBox.setVgrow(wkniCuerpo, Priority.ALWAYS);

        VBox wkniPanelLista = wkniPanelPendientes();

        HBox wkniFila = new HBox(20, wkniCentro);
        wkniFila.setAlignment(Pos.CENTER);
        if (wkniPanelLista != null) wkniFila.getChildren().add(wkniPanelLista);

        wkniPanelAjustes.setPadding(new Insets(20));
        wkniPanelAjustes.getStyleClass().addAll("card", "enfoque-ajustes");
        wkniPanelAjustes.setMaxWidth(360);
        wkniPanelAjustes.setVisible(false);
        wkniPanelAjustes.setManaged(false);
        wkniPanelAjustes.getChildren().addAll(wkniConstruirAjustes());

        StackPane.setMargin(wkniFila, new Insets(20));
        StackPane.setMargin(wkniPanelAjustes, new Insets(20));
        wkniCapaContenido.getChildren().setAll(wkniFila, wkniPanelAjustes);
        StackPane.setAlignment(wkniPanelAjustes, Pos.TOP_RIGHT);
    }

    /** Panel lateral con los pendientes (tareas y/o casillas de las notas). */
    private VBox wkniPanelPendientes() {
        if (!wkniSt.wkniBool("timeBlocking.showTasks") || wkniPendientes.isEmpty()) return null;
        wkni_listaPendientes.getChildren().clear();
        Label wkniH = new Label("Pendientes");
        wkniH.getStyleClass().add("h3");
        wkni_listaPendientes.getChildren().add(wkniH);
        int wkniN = 0;
        for (wkniListaPendientes.wkniItem wkniIt : wkniPendientes) {
            if (wkniN++ >= 12) break;
            CheckBox wkniCb = new CheckBox(wkniIt.wkniTexto());
            wkniCb.setSelected(wkniIt.wkniHecho());
            wkniCb.setWrapText(true);
            wkniCb.setMaxWidth(Double.MAX_VALUE);
            wkniCb.getStyleClass().add("enfoque-pendiente");
            wkniCb.setOnAction(wkniE -> {
                boolean wkniHecho = wkniCb.isSelected();
                if (wkniAlAlternar != null) wkniAlAlternar.accept(wkniIt, wkniHecho);
                if (wkniHecho) wkniCb.getStyleClass().add("tachado");
                else wkniCb.getStyleClass().remove("tachado");
            });
            if (wkniIt.wkniHecho()) wkniCb.getStyleClass().add("tachado");
            wkni_listaPendientes.getChildren().add(wkniCb);
        }
        wkniScrollPendientes.setFitToWidth(true);
        wkniScrollPendientes.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        wkniScrollPendientes.setPrefWidth(300);
        wkniScrollPendientes.setMaxWidth(300);
        wkniScrollPendientes.setPrefHeight(320);
        VBox wkni_panel = new VBox(10, wkniScrollPendientes);
        wkni_panel.getStyleClass().addAll("card", "enfoque-pendientes");
        wkni_panel.setPrefWidth(320);
        wkni_panel.setMaxWidth(320);
        wkni_panel.setPadding(new Insets(18));
        return wkni_panel;
    }

    // ------------------------------------------------------------------ panel de ajustes

    private Node[] wkniConstruirAjustes() {
        Label wkniT = wkniUi.wkniH3("Ajustes de la pantalla");
        Button wkniCerrar = wkniUi.wkniIconButton("close", "Cerrar los ajustes");
        wkniCerrar.setOnAction(wkniE -> wkniAlternarAjustes());
        HBox wkniHead = new HBox(10, wkniT, wkniUi.wkniSpacer(), wkniCerrar);
        wkniHead.setAlignment(Pos.CENTER_LEFT);

        wkniUi.wkniSegmented wkniTam = new wkniUi.wkniSegmented("Pequeño", "Mediano", "Grande");
        wkniTam.wkniSelect(Math.max(0, Math.min(2, wkniIndiceTamano())));
        wkniTam.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> {
            wkniSt.wkniSet("timeBlocking.clockSize", new String[]{"pequeño", "mediano", "grande"}[wkniV.intValue()]);
            wkniReloj.setStyle("-fx-font-size: " + wkniTamanoReloj() + "px;");
        });

        wkniSwitch wkniVerBarra = new wkniSwitch("Mostrar la barra de progreso", null, wkniSt.wkniBool("timeBlocking.showProgress"));
        wkniVerBarra.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> { wkniSt.wkniSet("timeBlocking.showProgress", wkniV); wkniBarra.setVisible(wkniV); });

        wkniSwitch wkniVerDetalle = new wkniSwitch("Mostrar el detalle de la sesión", "Sesión, ciclo y método.", wkniSt.wkniBool("timeBlocking.showDetail"));
        wkniVerDetalle.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> { wkniSt.wkniSet("timeBlocking.showDetail", wkniV); wkniDetalle.setVisible(wkniV); });

        wkniSwitch wkniVerLista = new wkniSwitch("Mostrar la lista de pendientes", null, wkniSt.wkniBool("timeBlocking.showTasks"));
        wkniVerLista.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> { wkniSt.wkniSet("timeBlocking.showTasks", wkniV); });

        Slider wkniVeloSlider = new Slider(0, 85, wkniSt.wkniInteger("timeBlocking.backgroundDim", 0, 90));
        wkniVeloSlider.setBlockIncrement(5);
        Label wkniVeloVal = wkniUi.wkniLabel(wkniVeloSlider.getValue() + " %", "caption");
        wkniVeloSlider.valueProperty().addListener((wkniO, wkniA, wkniV) -> {
            wkniVeloVal.setText(Math.round(wkniV.doubleValue()) + " %");
            wkniVelo.setOpacity(wkniV.doubleValue() / 100.0);
        });
        wkniVeloSlider.setOnMouseReleased(wkniE -> wkniSt.wkniSet("timeBlocking.backgroundDim", (int) Math.round(wkniVeloSlider.getValue())));
        HBox wkniVeloFila = new HBox(10, wkniVeloSlider, wkniVeloVal);
        wkniVeloSlider.setPrefWidth(180);
        wkniVeloFila.setAlignment(Pos.CENTER_LEFT);

        wkniSwitch wkniSonido = new wkniSwitch("Sonido al cambiar de fase", null, wkniSt.wkniBool("timeBlocking.sound"));
        wkniSonido.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> wkniSt.wkniSet("timeBlocking.sound", wkniV));

        // Audio local de la sesión (mp3, wav, ogg…): se repite mientras trabajas y calla en los descansos.
        Label wkniAudioNombre = wkniUi.wkniLabel(wkniNombreAudio(), "caption");
        Button wkniElegirAudio = wkniUi.wkniSecondary("music", "Elegir audio…");
        wkniElegirAudio.setOnAction(wkniE -> {
            FileChooser wkniFc = new FileChooser();
            wkniFc.setTitle("Audio de la sesión de enfoque");
            wkniFc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav", "*.ogg",
                "*.m4a", "*.aac", "*.flac", "*.opus"));
            File wkniF = wkniFc.showOpenDialog(wkniStage);
            if (wkniF == null) return;
            wkniSt.wkniSet("timeBlocking.audioFile", wkniF.getAbsolutePath());
            wkniAudioNombre.setText(wkniF.getName());
            wkniIniciarAudio();
        });
        Button wkniQuitarAudio = wkniUi.wkniSecondary(null, "Quitar");
        wkniQuitarAudio.setOnAction(wkniE -> {
            wkniSt.wkniSet("timeBlocking.audioFile", null);
            wkniAudioNombre.setText("Sin audio");
            wkniPararAudio();
        });
        HBox wkniAudioFila = new HBox(10, wkniElegirAudio, wkniAudioNombre, wkniQuitarAudio);
        wkniAudioFila.setAlignment(Pos.CENTER_LEFT);

        wkniSwitch wkniRepetir = new wkniSwitch("Repetir el video de fondo", "Solo si el fondo es un video.", wkniSt.wkniBool("timeBlocking.backgroundLoop"));
        wkniRepetir.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> {
            wkniSt.wkniSet("timeBlocking.backgroundLoop", wkniV);
            if (wkniVideo != null) wkniVideo.setCycleCount(wkniV ? MediaPlayer.INDEFINITE : 1);
        });

        javafx.scene.control.ComboBox<wkniImportancia> wkniImp = new javafx.scene.control.ComboBox<>();
        wkniImp.getItems().setAll(wkniImportancia.values());
        wkniImp.setValue(wkni_importancia);
        wkniImp.setOnAction(wkniE -> {
            wkniImportancia wkniN = wkniImp.getValue();
            if (wkniN == null) return;
            wkni_importancia = wkniN;
            wkniSt.wkniSet("timeBlocking.importance", wkniN.name());
            wkniAplicarImportancia();
        });

        Button wkniQuitarFondo = wkniUi.wkniSecondary(null, "Quitar el fondo");
        wkniQuitarFondo.setOnAction(wkniE -> {
            wkniSt.wkniSet("timeBlocking.backgroundFile", null);
            wkniSt.wkniSet("timeBlocking.background", "degradado");
            wkniConstruirFondo();
        });

        // Un botón propio: si reutilizáramos el de la pantalla principal, se movería de sitio al
        // abrirse este panel y el botón original desaparecería de la fila de abajo.
        Button wkniCambiarFondo = wkniUi.wkniSecondary("grid", "Cambiar el fondo");
        wkniCambiarFondo.setOnAction(wkniE -> wkniElegirFondo());

        return new Node[]{
            wkniHead,
            wkniUi.wkniMuted("Los cambios se guardan en tu perfil y se aplican al instante."),
            wkniUi.wkniField("Tamaño del reloj", wkniTam.wkniNode()),
            wkniVerBarra, wkniVerDetalle, wkniVerLista,
            wkniUi.wkniField("Velo sobre el fondo", wkniVeloFila),
            wkniSonido,
            wkniUi.wkniField("Audio de la sesión", wkniAudioFila),
            wkniUi.wkniMuted("Un archivo de audio de tu equipo: se repite mientras trabajas y calla en los descansos."),
            wkniRepetir,
            wkniUi.wkniField("Importancia de la sesión", wkniImp),
            wkniUi.wkniMuted(wkni_importancia.wkniAyuda()),
            new HBox(10, wkniCambiarFondo, wkniQuitarFondo)
        };
    }

    private int wkniIndiceTamano() {
        return switch (wkniSt.wkniStr("timeBlocking.clockSize")) {
            case "pequeño" -> 0;
            case "grande" -> 2;
            default -> 1;
        };
    }

    private int wkniTamanoReloj() {
        return switch (wkniIndiceTamano()) {
            case 0 -> 56;
            case 2 -> 124;
            default -> 88;
        };
    }

    private void wkniAlternarAjustes() {
        wkniAjustesAbiertos = !wkniAjustesAbiertos;
        wkniPanelAjustes.setVisible(wkniAjustesAbiertos);
        wkniPanelAjustes.setManaged(wkniAjustesAbiertos);
        if (wkniAjustesAbiertos) {
            wkniPanelAjustes.getChildren().setAll(wkniConstruirAjustes());
            wkniPanelAjustes.requestFocus();
        }
    }

    // ------------------------------------------------------------------ fondo

    private void wkniElegirFondo() {
        FileChooser wkniFc = new FileChooser();
        wkniFc.setTitle("Fondo de la pantalla de enfoque");
        wkniFc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes y videos", "*.png", "*.jpg", "*.jpeg",
            "*.gif", "*.bmp", "*.webp", "*.mp4", "*.m4v", "*.webm", "*.ogv"));
        File wkniF = wkniFc.showOpenDialog(wkniStage);
        if (wkniF == null) return;
        String wkniExt = wkniExtension(wkniF);
        if (!wkniVIDEO.contains(wkniExt) && !wkniIMAGEN.contains(wkniExt)) {
            wkniUi.wkniAlert(wkniStage.getScene().getRoot(), "No se pudo usar ese archivo",
                "Elige una imagen (png, jpg, gif, bmp, webp) o un video (mp4, webm).");
            return;
        }
        wkniSt.wkniSet("timeBlocking.backgroundFile", wkniF.getAbsolutePath());
        wkniSt.wkniSet("timeBlocking.background", "archivo");
        if (!wkniConstruirFondo()) {
            wkniSt.wkniSet("timeBlocking.backgroundFile", null);
            wkniSt.wkniSet("timeBlocking.background", "degradado");
            wkniConstruirFondo();
            wkniUi.wkniAlert(wkniStage.getScene().getRoot(), "No se pudo usar ese archivo",
                "El archivo no se pudo abrir: puede estar dañado o en un formato que JavaFX no reproduce.");
        }
    }

    /** Extensión en minúsculas, sin el punto ("png", "mp4"). */
    private static String wkniExtension(File wkniF) {
        String wkniN = wkniF.getName().toLowerCase(Locale.ROOT);
        return wkniN.contains(".") ? wkniN.substring(wkniN.lastIndexOf('.') + 1) : "";
    }

    // ------------------------------------------------------------------ audio de la sesión

    /** Nombre del archivo de audio elegido (o "Sin audio"). */
    private String wkniNombreAudio() {
        String wkniRuta = wkniSt.wkniStr("timeBlocking.audioFile");
        return wkniRuta.isBlank() ? "Sin audio" : new File(wkniRuta).getName();
    }

    /** Prepara el audio local y lo deja listo (sin sonido hasta que empiece la fase de trabajo). */
    private void wkniIniciarAudio() {
        wkniPararAudio();
        String wkniRuta = wkniSt.wkniStr("timeBlocking.audioFile");
        if (wkniRuta.isBlank()) return;
        File wkniF = new File(wkniRuta);
        if (!wkniF.isFile()) return;
        try {
            wkniAudio = new MediaPlayer(new Media(wkniF.toURI().toString()));
            wkniAudio.setCycleCount(MediaPlayer.INDEFINITE);
            wkniAudio.setVolume(wkniSt.wkniInteger("timeBlocking.volume", 0, 100) / 100f);
            wkniAudio.setOnError(() -> { wkniAudio = null; });
        } catch (RuntimeException wkniEx) {
            wkniAudio = null;
        }
    }

    private void wkniPararAudio() {
        if (wkniAudio != null) { wkniAudio.dispose(); wkniAudio = null; }
    }

    /** El audio suena durante el trabajo y se calla en los descansos o si la sesión está en pausa. */
    private void wkniSincronizarAudio() {
        if (wkniAudio == null) return;
        boolean wkniDebeSonar = !wkniSesion.wkniPausada() && !wkniSesion.wkniTerminada() && wkniSesion.wkniFase() == wkniSesionEnfoque.wkniFase.TRABAJO;
        try {
            if (wkniDebeSonar) {
                if (wkniAudio.getStatus() != MediaPlayer.Status.PLAYING) wkniAudio.play();
            } else if (wkniAudio.getStatus() == MediaPlayer.Status.PLAYING) {
                wkniAudio.pause();
            }
        } catch (RuntimeException wkniIgnored) { /* archivo de audio no disponible */ }
    }

    // ------------------------------------------------------------------ importancia

    private void wkniAplicarImportancia() {
        wkniStage.setAlwaysOnTop(false);
        wkniStage.setFullScreen(false);
        if (wkni_importancia == wkniImportancia.ESTRICTA) {
            wkniStage.setFullScreen(true);
            wkniStage.setFullScreenExitKeyCombination(KeyCombination.NO_MATCH);
            wkniStage.setAlwaysOnTop(true);
            wkniStage.toFront();
            wkniStage.requestFocus();
            // Dentro de la app no funcionan los atajos (Ctrl+S, F5…), salvo Escape para salir.
            if (!wkniFiltroTeclado) {
                wkniFiltroTeclado = true;
                wkniStage.getScene().addEventFilter(KeyEvent.ANY, wkniFiltroEstricto);
            }
        } else {
            // Si se sale del modo Estricta, el filtro de teclado se retira (si no, la app se queda muda).
            if (wkniFiltroTeclado) {
                wkniFiltroTeclado = false;
                wkniStage.getScene().removeEventFilter(KeyEvent.ANY, wkniFiltroEstricto);
            }
            if (wkni_importancia == wkniImportancia.ALTA) wkniStage.setMaximized(true);
        }
    }

    /** Filtro del modo Estricta: se come los atajos y deja Escape para poder salir con confirmación. */
    private final javafx.event.EventHandler<KeyEvent> wkniFiltroEstricto = wkniE -> {
        if (wkniE.getCode() == javafx.scene.input.KeyCode.ESCAPE) {
            wkniE.consume();
            wkniCerrar(true);
        } else wkniE.consume();
    };

    // ------------------------------------------------------------------ ciclo

    private void wkniAlternar() {
        if (wkniSesion.wkniPausada()) { wkniSesion.wkniIniciar(); wkni_aviso.setText(""); } else wkniSesion.wkniPausar();
        wkniActualizar();
    }

    private void wkniSalio() {
        if (wkniDialogoAbierto || wkniAjustesAbiertos) return;
        if (wkni_importancia == wkniImportancia.NORMAL && !wkniSt.wkniBool("timeBlocking.pauseOnLeave")) return;
        if (wkniSesion.wkniPausada() || wkniSesion.wkniTerminada()) return;
        wkniSesion.wkniPausar();
        wkniSalidas++;
        wkni_aviso.setText("El contador se pausó porque saliste de la pantalla (" + wkniSalidas
            + (wkniSalidas == 1 ? " vez" : " veces") + "). Pulsa «Continuar» cuando vuelvas.");
        wkniActualizar();
        if (wkni_importancia == wkniImportancia.ESTRICTA) { wkniStage.setIconified(false); wkniStage.toFront(); wkniStage.requestFocus(); }
    }

    private void wkniBeep() {
        try {
            float wkniV = wkniSt.wkniInteger("timeBlocking.volume", 0, 100) / 100f;
            if (wkniV <= 0) return;
            java.awt.Toolkit.getDefaultToolkit().beep();
        } catch (Throwable wkniIgnored) { /* sin audio disponible */ }
    }

    private void wkniAvisarFinDeFase() {
        if (!wkniSt.wkniBool("timeBlocking.notifyEnd")) return;
        String wkniMsg = switch (wkniSesion.wkniFase()) {
            case TRABAJO -> "¡Descanso! Estira las espalda.";
            case DESCANSO, DESCANSO_LARGO -> "Vuelve al trabajo.";
        };
        wkni_aviso.setText(wkniMsg);
    }

    private void wkniActualizar() {
        wkniMetodoEnfoque wkniM = wkniSesion.wkniMetodo();
        String wkniNombreFase = switch (wkniSesion.wkniFase()) {
            case TRABAJO -> "Trabajo";
            case DESCANSO -> "Descanso";
            case DESCANSO_LARGO -> "Descanso largo";
        };
        wkni_fase.setText(wkniNombreFase);
        wkniBarra.getStyleClass().removeAll("fase-trabajo", "fase-descanso", "fase-largo");
        wkniBarra.getStyleClass().add(wkniSesion.wkniFase() == wkniSesionEnfoque.wkniFase.TRABAJO ? "fase-trabajo"
            : wkniSesion.wkniFase() == wkniSesionEnfoque.wkniFase.DESCANSO ? "fase-descanso" : "fase-largo");
        wkniReloj.setText(wkniSesionEnfoque.wkniFormato(wkniSesion.wkniRestantes()));
        wkniBarra.setProgress(wkniSesion.wkniCuentaLibre() ? ProgressBar.INDETERMINATE_PROGRESS : wkniSesion.wkniProgreso());
        wkniBarra.setVisible(wkniSt.wkniBool("timeBlocking.showProgress"));

        String wkniSes = wkniM.wkniSesionesPorCiclo() > 0
            ? "Sesión " + wkniSesion.wkniSesion() + " de " + wkniM.wkniSesionesPorCiclo() + " · Ciclo " + wkniSesion.wkniCiclo()
            : "Sesión " + wkniSesion.wkniSesion();
        String wkniDuracion = wkniM.wkniTieneRangos()
            ? " · ahora " + wkniSesion.wkniTrabajoUsado() + " min de trabajo y " + wkniSesion.wkniDescansoUsado() + " de descanso"
            : "";
        wkniDetalle.setText(wkniSes + " · " + wkniM.wkniNombre() + " · " + wkniM.wkniResumen() + wkniDuracion);
        wkniDetalle.setVisible(wkniSt.wkniBool("timeBlocking.showDetail"));

        wkniPlay.setText(wkniSesion.wkniPausada()
            ? (wkniSesion.wkniTranscurrido() == 0 && wkniSesion.wkniSesion() == 1 && wkniSesion.wkniCiclo() == 1 ? "Iniciar" : "Continuar")
            : "Pausar");
        wkniTerminarFlow.setVisible(wkniSesion.wkniCuentaLibre());
        wkniTerminarFlow.setManaged(wkniSesion.wkniCuentaLibre());
        wkniSincronizarAudio();
    }

    private void wkniCerrar(boolean wkniConfirmar) {
        if (wkniConfirmar && !wkniSesion.wkniTerminada() && wkniSesion.wkniTranscurrido() > 0
            && !wkniUi.wkniConfirm(wkniStage.getScene().getRoot(), "¿Detener la sesión de enfoque?", "Se pierde el avance de la fase actual.", "Detener")) return;
        wkniSesion.wkniDetener();
        if (wkniTic != null) wkniTic.stop();
        if (wkniVideo != null) { wkniVideo.dispose(); wkniVideo = null; }
        wkniPararAudio();
        wkniStage.setAlwaysOnTop(false);
        wkniStage.close();
    }
}

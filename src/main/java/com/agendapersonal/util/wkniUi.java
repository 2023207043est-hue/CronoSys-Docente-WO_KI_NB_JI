package com.agendapersonal.util;

import com.agendapersonal.ui.wkniFontPreferences;
import com.agendapersonal.ui.wkniIconFactory;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.FontWeight;
import javafx.stage.Window;
import javafx.util.Duration;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Piezas de interfaz reutilizables con el estilo de Compás (tarjetas, botones, chips, campos...). */
public final class wkniUi {
    public static final Locale wkniES = Locale.of("es", "PE");
    private static final Map<String, String> wkniTINT = Map.ofEntries(
        Map.entry("home", "orange"), Map.entry("calendar", "violet"), Map.entry("agenda", "blue"),
        Map.entry("time", "orange"), Map.entry("clock", "orange"), Map.entry("tasks", "blue"),
        Map.entry("notes", "amber"), Map.entry("contacts", "violet"), Map.entry("habits", "green"),
        Map.entry("finance", "teal"), Map.entry("health", "rose"), Map.entry("settings", "slate"),
        Map.entry("book", "teal"), Map.entry("sparkle", "amber"));

    private wkniUi() {}

    /** Color temático de cada módulo (para su ícono). */
    public static String wkniTintFor(String wkniIcon) { return wkniTINT.getOrDefault(wkniIcon, "slate"); }

    // ------------------------------------------------------------- contenedores

    public static VBox wkniCard() {
        VBox wkniB = new VBox(12);
        wkniB.setPadding(new Insets(20));
        wkniB.getStyleClass().add("card");
        return wkniB;
    }

    /** Página estándar: fondo del tema y márgenes. */
    public static VBox wkniPage() {
        VBox wkni_p = new VBox(22);
        wkni_p.getStyleClass().add("page");
        wkni_p.setPadding(new Insets(28, 36, 32, 36));
        return wkni_p;
    }

    /** Envuelve una página para que se pueda desplazar si la ventana es pequeña. */
    public static ScrollPane wkniScroll(Node wkniContent) {
        ScrollPane wkniSp = new ScrollPane(wkniContent);
        wkniSp.setFitToWidth(true);
        wkniSp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        wkniSp.getStyleClass().add("page-scroll");
        return wkniSp;
    }

    /** Encabezado de página: ícono del módulo, título, frase y acciones a la derecha. */
    public static HBox wkniPageHeader(String wkniIcon, String wkniTitle, String wkniSubtitle, Node... wkniActions) {
        StackPane wkniBadge = wkniIconChip(wkniIcon, wkniTintFor(wkniIcon), 48, 22);
        Label wkniT = new Label(wkniTitle);
        wkniT.getStyleClass().add("h1");
        VBox wkniTexts = new VBox(3, wkniT);
        if (wkniSubtitle != null && !wkniSubtitle.isBlank()) {
            Label wkniS = new Label(wkniSubtitle);
            wkniS.getStyleClass().add("subtitle");
            wkniS.setWrapText(true);
            wkniTexts.getChildren().add(wkniS);
        }
        HBox wkniLeft = new HBox(16, wkniBadge, wkniTexts);
        wkniLeft.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(wkniLeft, Priority.ALWAYS);
        HBox wkniRight = new HBox(10, wkniActions);
        wkniRight.setAlignment(Pos.CENTER_RIGHT);
        HBox wkniHeader = new HBox(16, wkniLeft, wkniRight);
        wkniHeader.getStyleClass().add("page-header");
        wkniHeader.setAlignment(Pos.CENTER_LEFT);
        return wkniHeader;
    }

    /** Firma anterior (los colores ya no se usan: salen del tema). */
    public static HBox wkniPageHeader(String wkniIcon, String wkniTitle, String wkniSubtitle, String wkniBg, String wkniBorder) {
        return wkniPageHeader(wkniIcon, wkniTitle, wkniSubtitle);
    }

    /** Tarjeta de resumen numérica (se mantiene por compatibilidad). */
    public static VBox wkniSummaryCard(String wkniLabel, String wkniValue, String wkni_accent) {
        return wkniStat(wkniLabel, wkniValue, null, "tasks", "blue");
    }

    public static VBox wkniSummaryCard(String wkniLabel, String wkniValue) { return wkniSummaryCard(wkniLabel, wkniValue, null); }

    /** Tarjeta de estadística: etiqueta + ícono teñido arriba, cifra grande y una nota. */
    public static VBox wkniStat(String wkniLabel, String wkniValue, String wkniNote, String wkniIcon, String wkni_tone) {
        Label wkniL = new Label(wkniLabel);
        wkniL.getStyleClass().add("stat-label");
        Region wkniSp = wkniSpacer();
        HBox wkniTop = new HBox(8, wkniL, wkniSp, wkniIconChip(wkniIcon, wkni_tone, 34, 18));
        wkniTop.setAlignment(Pos.CENTER_LEFT);
        Label wkniV = new Label(wkniValue);
        wkniV.getStyleClass().add("stat-value");
        VBox wkniB = new VBox(10, wkniTop, wkniV);
        if (wkniNote != null) {
            Label wkniN = new Label(wkniNote);
            wkniN.getStyleClass().add("stat-note");
            wkniN.setWrapText(true);
            wkniB.getChildren().add(wkniN);
        }
        wkniB.getStyleClass().addAll("card", "stat-card");
        wkniB.setPadding(new Insets(18, 20, 18, 20));
        HBox.setHgrow(wkniB, Priority.ALWAYS);
        wkniB.setMaxWidth(Double.MAX_VALUE);
        return wkniB;
    }

    // ------------------------------------------------------------- textos

    public static Label wkniH2(String wkniText) { return wkniLabel(wkniText, "h2"); }
    public static Label wkniH3(String wkniText) { return wkniLabel(wkniText, "h3"); }
    public static Label wkniMuted(String wkniText) {
        Label wkniL = wkniLabel(wkniText, "muted");
        wkniL.setWrapText(true);
        return wkniL;
    }
    public static Label wkniCaption(String wkniText) { return wkniLabel(wkniText, "caption"); }

    public static Label wkniLabel(String wkniText, String... wkniClasses) {
        Label wkniL = new Label(wkniText);
        wkniL.getStyleClass().addAll(wkniClasses);
        return wkniL;
    }

    /** Etiqueta de sección en mayúsculas pequeñas ("HOY", "ESTA SEMANA"). */
    public static Label wkniOverline(String wkniText) {
        Label wkniL = new Label(wkniText.toUpperCase(wkniES));
        wkniL.getStyleClass().add("overline");
        return wkniL;
    }

    // ------------------------------------------------------------- botones

    public static Button wkniPrimary(String wkniIcon, String wkniText) { return wkniButton(wkniIcon, wkniText, "btn-primary"); }
    public static Button wkniSecondary(String wkniIcon, String wkniText) { return wkniButton(wkniIcon, wkniText, "btn-secondary"); }
    public static Button wkniGhost(String wkniIcon, String wkniText) { return wkniButton(wkniIcon, wkniText, "btn-ghost"); }
    public static Button wkniDanger(String wkniIcon, String wkniText) { return wkniButton(wkniIcon, wkniText, "btn-danger"); }

    private static Button wkniButton(String wkniIcon, String wkniText, String wkniCls) {
        Button wkniB = new Button(wkniText);
        if (wkniIcon != null) wkniB.setGraphic(wkniIconFactory.wkniIcon(wkniIcon, 17));
        wkniB.getStyleClass().add(wkniCls);
        wkniB.setGraphicTextGap(8);
        wkniB.setMnemonicParsing(false);
        wkniB.setMinWidth(Region.USE_PREF_SIZE);       // nunca se corta el texto del botón ("Regis…")
        return wkniB;
    }

    /** Botón cuadrado solo con ícono (con texto de ayuda y nombre accesible). */
    public static Button wkniIconButton(String wkniIcon, String wkniTip) {
        Button wkniB = new Button();
        wkniB.setGraphic(wkniIconFactory.wkniIcon(wkniIcon, 18));
        wkniB.getStyleClass().add("icon-button");
        wkniB.setMinWidth(Region.USE_PREF_SIZE);
        wkniB.setAccessibleText(wkniTip);
        wkniTooltip(wkniB, wkniTip);
        return wkniB;
    }

    public static void wkniTooltip(javafx.scene.control.Control wkniC, String wkniText) {
        Tooltip wkniT = new Tooltip(wkniText);
        wkniT.setShowDelay(Duration.millis(350));
        wkniC.setTooltip(wkniT);
    }

    /** Convierte un botón existente en principal (antes se hacía con estilo en línea). */
    public static void wkniStylePrimary(Button wkniB) {
        wkniB.getStyleClass().removeAll("btn-secondary", "btn-ghost", "btn-danger");
        if (!wkniB.getStyleClass().contains("btn-primary")) wkniB.getStyleClass().add("btn-primary");
    }

    public static Button wkniActionButton(String wkniIcon, String wkniText) { return wkniSecondary(wkniIcon, wkniText); }

    public static void wkniStyleSoft(Button wkniB, String wkniBg, String wkniFg) {
        wkniB.setStyle("-fx-background-color:" + wkniBg + ";-fx-text-fill:" + wkniFg + ";");
        wkniB.getStyleClass().add("btn-secondary");
    }

    public static Button wkniMenu(String wkniT, boolean wkniOn) {
        Button wkniB = new Button(wkniT);
        wkniB.setMaxWidth(Double.MAX_VALUE);
        wkniB.setAlignment(Pos.CENTER_LEFT);
        wkniB.getStyleClass().add(wkniOn ? "menu-btn-active" : "menu-btn");
        return wkniB;
    }

    // ------------------------------------------------------------- piezas

    /** Ícono dentro de un cuadrito redondeado de color suave. */
    public static StackPane wkniIconChip(String wkniIcon, String wkni_tone, double wkniBox, double wkniIconSize) {
        StackPane wkniS = new StackPane(wkniIconFactory.wkniIcon(wkniIcon, wkniIconSize, "tint-" + wkni_tone));
        wkniS.getStyleClass().addAll("icon-chip", "chip-" + wkni_tone);
        wkniS.setMinSize(wkniBox, wkniBox);
        wkniS.setPrefSize(wkniBox, wkniBox);
        wkniS.setMaxSize(wkniBox, wkniBox);
        return wkniS;
    }

    /** Etiqueta tipo píldora ("Alta", "Trabajo", "Escríbele"...). */
    public static Label wkniPill(String wkniText, String wkni_tone) {
        Label wkniL = new Label(wkniText);
        wkniL.getStyleClass().addAll("pill", "pill-" + wkni_tone);
        wkniL.setMinWidth(Region.USE_PREF_SIZE);
        return wkniL;
    }

    /** Avatar con iniciales y un color estable según el nombre. */
    public static StackPane wkniAvatar(String wkniName, double wkni_size) {
        String[] wkniTones = {"blue", "green", "violet", "amber", "rose", "teal", "orange"};
        String wkniN = wkniName == null ? "?" : wkniName.trim();
        String wkni_tone = wkniTones[Math.floorMod(wkniN.toLowerCase(wkniES).hashCode(), wkniTones.length)];
        Label wkniL = new Label(wkniInitials(wkniN));
        wkniL.getStyleClass().addAll("avatar-text", "tint-" + wkni_tone);
        wkniL.setStyle("-fx-font-size:" + Math.round(wkni_size * 0.36) + "px;");
        StackPane wkniS = new StackPane(wkniL);
        wkniS.getStyleClass().addAll("avatar", "chip-" + wkni_tone);
        wkniS.setMinSize(wkni_size, wkni_size);
        wkniS.setPrefSize(wkni_size, wkni_size);
        wkniS.setMaxSize(wkni_size, wkni_size);
        return wkniS;
    }

    public static String wkniInitials(String wkniN) {
        String[] wkniParts = wkniN.trim().split("\\s+");
        StringBuilder wkniSb = new StringBuilder();
        for (String wkni_p : wkniParts) if (!wkni_p.isEmpty() && wkniSb.length() < 2) wkniSb.append(Character.toUpperCase(wkni_p.charAt(0)));
        return wkniSb.isEmpty() ? "?" : wkniSb.toString();
    }

    /** Barra de progreso fina (0..1) en el color indicado. */
    public static StackPane wkniBar(double wkniFraction, String wkni_tone, double wkniHeight) {
        Region wkniFill = new Region();
        wkniFill.getStyleClass().addAll("bar-fill", "fill-" + wkni_tone);
        StackPane wkniTrack = new StackPane(wkniFill);
        wkniTrack.getStyleClass().add("bar-track");
        wkniTrack.setAlignment(Pos.CENTER_LEFT);
        wkniTrack.setMinHeight(wkniHeight);
        wkniTrack.setPrefHeight(wkniHeight);
        wkniTrack.setMaxHeight(wkniHeight);
        double wkniF = Math.max(0, Math.min(1, wkniFraction));
        wkniFill.maxWidthProperty().bind(wkniTrack.widthProperty().multiply(wkniF));
        wkniFill.prefWidthProperty().bind(wkniTrack.widthProperty().multiply(wkniF));
        return wkniTrack;
    }

    /** Campo de búsqueda con lupa. */
    public static HBox wkniSearchBox(TextField wkniField, String wkniPrompt) {
        wkniField.setPromptText(wkniPrompt);
        wkniField.getStyleClass().add("bare-field");
        HBox.setHgrow(wkniField, Priority.ALWAYS);
        HBox wkniBox = new HBox(8, wkniIconFactory.wkniIcon("search", 17), wkniField);
        wkniBox.getStyleClass().add("search-box");
        wkniBox.setAlignment(Pos.CENTER_LEFT);
        wkniBox.setOnMouseClicked(wkniE -> wkniField.requestFocus());
        wkniField.focusedProperty().addListener((wkniO, wkniA, wkniB) ->
            wkniBox.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("focused"), wkniB));
        return wkniBox;
    }

    /** Campo de formulario: etiqueta arriba y control debajo. */
    public static VBox wkniField(String wkniLabel, Node wkniControl) {
        Label wkniL = new Label(wkniLabel);
        wkniL.getStyleClass().add("field-label");
        wkniL.setLabelFor(wkniControl);
        if (wkniControl instanceof Region wkniR) wkniR.setMaxWidth(Double.MAX_VALUE);
        VBox wkniV = new VBox(7, wkniL, wkniControl);
        HBox.setHgrow(wkniV, Priority.ALWAYS);
        return wkniV;
    }

    /** Selector de fecha en formato peruano (16/10/2026) en cualquier PC. */
    public static javafx.scene.control.DatePicker wkniFechaCorta(javafx.scene.control.DatePicker wkniDp) {
        java.time.format.DateTimeFormatter wkniF = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        wkniDp.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(LocalDate wkniD) { return wkniD == null ? "" : wkniF.format(wkniD); }
            @Override public LocalDate fromString(String wkniS) {
                try { return wkniS == null || wkniS.isBlank() ? null : LocalDate.parse(wkniS.trim(), wkniF); }
                catch (Exception wkniE) { return wkniDp.getValue(); }
            }
        });
        wkniDp.setPromptText("dd/mm/aaaa");
        return wkniDp;
    }

    /** Mensaje para cuando una lista está vacía. */
    public static VBox wkniEmpty(String wkniIcon, String wkniTitle, String wkniText) {
        StackPane wkniChip = wkniIconChip(wkniIcon, "slate", 52, 24);
        Label wkniT = new Label(wkniTitle);
        wkniT.getStyleClass().add("empty-title");
        Label wkniM = new Label(wkniText);
        wkniM.getStyleClass().add("empty-text");
        wkniM.setWrapText(true);
        wkniM.setMaxWidth(340);
        wkniM.setAlignment(Pos.CENTER);
        wkniM.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        VBox wkniV = new VBox(10, wkniChip, wkniT, wkniM);
        wkniV.setAlignment(Pos.CENTER);
        wkniV.setPadding(new Insets(36, 20, 36, 20));
        wkniV.getStyleClass().add("empty-state");
        return wkniV;
    }

    public static Region wkniSpacer() {
        Region wkniR = new Region();
        HBox.setHgrow(wkniR, Priority.ALWAYS);
        javafx.scene.layout.VBox.setVgrow(wkniR, Priority.ALWAYS);
        return wkniR;
    }

    /** Control segmentado (pestañas compactas). Siempre queda una opción elegida. */
    public static final class wkniSegmented {
        private final HBox wkniNode = new HBox(2);
        private final ToggleGroup wkniGroup = new ToggleGroup();
        private final List<ToggleButton> wkniButtons = new ArrayList<>();
        private final IntegerProperty wkniSelected = new SimpleIntegerProperty(0);

        public wkniSegmented(String... wkniOptions) {
            wkniNode.getStyleClass().add("segmented");
            wkniNode.setAlignment(Pos.CENTER_LEFT);
            for (int wkniI = 0; wkniI < wkniOptions.length; wkniI++) {
                ToggleButton wkniB = new ToggleButton(wkniOptions[wkniI]);
                wkniB.getStyleClass().add("segment");
                wkniB.setToggleGroup(wkniGroup);
                wkniB.setMnemonicParsing(false);
                wkniB.setMinWidth(Region.USE_PREF_SIZE);
                final int wkniIdx = wkniI;
                wkniB.setOnAction(wkniE -> { if (!wkniB.isSelected()) wkniB.setSelected(true); wkniSelected.set(wkniIdx); });
                wkniButtons.add(wkniB);
                wkniNode.getChildren().add(wkniB);
            }
            if (!wkniButtons.isEmpty()) wkniButtons.getFirst().setSelected(true);
            wkniSelected.addListener((wkniO, wkniA, wkniV) -> {
                int wkniI = wkniV.intValue();
                if (wkniI >= 0 && wkniI < wkniButtons.size() && !wkniButtons.get(wkniI).isSelected()) wkniButtons.get(wkniI).setSelected(true);
            });
        }

        public HBox wkniNode() { return wkniNode; }

        /** Reparte el ancho disponible entre las opciones (para paneles estrechos). */
        public wkniSegmented wkniFillWidth() {
            wkniNode.setMaxWidth(Double.MAX_VALUE);
            for (ToggleButton wkniB : wkniButtons) {
                wkniB.setMaxWidth(Double.MAX_VALUE);
                wkniB.setMinWidth(0);
                HBox.setHgrow(wkniB, Priority.ALWAYS);
            }
            return this;
        }
        public IntegerProperty wkniSelectedProperty() { return wkniSelected; }
        public int wkniSelected() { return wkniSelected.get(); }
        public void wkniSelect(int wkniI) { wkniSelected.set(wkniI); }
        public void wkniSetText(int wkniI, String wkniText) { wkniButtons.get(wkniI).setText(wkniText); }
        public ToggleButton wkniButton(int wkniI) { return wkniButtons.get(wkniI); }
    }

    // ------------------------------------------------------------- diálogos

    /** Confirmación con el estilo de la app. Devuelve true si el usuario acepta. */
    public static boolean wkniConfirm(Node wkniOwner, String wkniTitle, String wkniText, String wkniOkText) {
        Alert wkniA = new Alert(Alert.AlertType.CONFIRMATION);
        wkniA.setTitle("Compás");
        wkniA.setHeaderText(wkniTitle);
        wkniA.setContentText(wkniText);
        wkniA.setGraphic(wkniIconChip("warning", "rose", 44, 22));
        ButtonType wkniOk = new ButtonType(wkniOkText, ButtonBar.ButtonData.OK_DONE);
        ButtonType wkniCancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        wkniA.getButtonTypes().setAll(wkniCancel, wkniOk);
        wkniA.getDialogPane().getStyleClass().add("cs-dialog");
        ((Button) wkniA.getDialogPane().lookupButton(wkniOk)).getStyleClass().add("btn-danger-solid");
        ((Button) wkniA.getDialogPane().lookupButton(wkniCancel)).getStyleClass().add("btn-secondary");
        Window wkniW = wkniOwner == null || wkniOwner.getScene() == null ? null : wkniOwner.getScene().getWindow();
        if (wkniW != null) wkniA.initOwner(wkniW);
        return wkniA.showAndWait().orElse(wkniCancel) == wkniOk;
    }

    /** Aviso simple con el estilo de la app. */
    public static void wkniAlert(Node wkniOwner, String wkniTitle, String wkniText) {
        Alert wkniA = new Alert(Alert.AlertType.WARNING);
        wkniA.setTitle("Compás");
        wkniA.setHeaderText(wkniTitle);
        wkniA.setContentText(wkniText);
        wkniA.setGraphic(wkniIconChip("warning", "amber", 44, 22));
        wkniA.getDialogPane().getStyleClass().add("cs-dialog");
        Window wkniW = wkniOwner == null || wkniOwner.getScene() == null ? null : wkniOwner.getScene().getWindow();
        if (wkniW != null) wkniA.initOwner(wkniW);
        wkniA.showAndWait();
    }

    /**
     * Da el estilo de Compás a un formulario armado como VBox de etiquetas y campos (diálogos de
     * eventos y tareas): título grande, etiquetas de campo, botón principal y logo en la ventana.
     */
    public static void wkniDecorarDialogo(javafx.scene.Scene wkniSc) {
        com.agendapersonal.ui.wkniThemeService.wkniGet().wkniApplyTo(wkniSc);
        if (wkniSc.getWindow() instanceof javafx.stage.Stage wkniSt) {
            com.agendapersonal.ui.wkniLogo.wkniAplicarIconos(wkniSt);
            if (wkniSt.getTitle() == null || wkniSt.getTitle().isBlank()) wkniSt.setTitle("Compás");
        }
        if (!(wkniSc.getRoot() instanceof VBox wkniR)) return;
        wkniR.getStyleClass().addAll("app-root", "dialog-root");
        wkniR.setPadding(new Insets(24, 26, 22, 26));
        wkniR.setSpacing(8);
        boolean wkniPrimero = true;
        for (Node wkniN : wkniR.getChildren()) {
            if (wkniN instanceof Label wkniL) {
                wkniL.getStyleClass().add(wkniPrimero ? "h2" : "field-label");
                if (wkniPrimero) VBox.setMargin(wkniL, new Insets(0, 0, 8, 0));
                else VBox.setMargin(wkniL, new Insets(6, 0, 0, 0));
                wkniPrimero = false;
            } else if (wkniN instanceof HBox wkniH && wkniH.getChildren().stream().allMatch(wkniX -> wkniX instanceof Button)) {
                wkniH.setAlignment(Pos.CENTER_RIGHT);
                wkniH.setSpacing(10);
                VBox.setMargin(wkniH, new Insets(14, 0, 0, 0));
                for (Node wkniX : wkniH.getChildren()) {
                    Button wkniB = (Button) wkniX;
                    String wkniT = wkniB.getText() == null ? "" : wkniB.getText();
                    if (wkniT.startsWith("Guardar") || wkniT.startsWith("Actualizar") || wkniT.startsWith("Nueva")) wkniB.getStyleClass().add("btn-primary");
                    else if (wkniT.startsWith("Eliminar")) wkniB.getStyleClass().add("btn-danger");
                    else wkniB.getStyleClass().add("btn-secondary");
                }
            } else if (wkniN instanceof HBox wkniH) {
                wkniH.setSpacing(10);
                for (Node wkniX : wkniH.getChildren()) {
                    HBox.setHgrow(wkniX, Priority.ALWAYS);
                    if (wkniX instanceof Region wkniRx) wkniRx.setMaxWidth(Double.MAX_VALUE);
                }
            } else if (wkniN instanceof Region wkniRg) {
                wkniRg.setMaxWidth(Double.MAX_VALUE);
                if (wkniN instanceof javafx.scene.control.DatePicker wkniDp) wkniFechaCorta(wkniDp);
                if (wkniN instanceof javafx.scene.control.TextArea wkniTa) { wkniTa.setPrefRowCount(4); wkniTa.setWrapText(true); }
            }
        }
    }

    // ------------------------------------------------------------- formatos

    public static String wkniFormatDate(LocalDate wkniD) {
        return wkniD.getDayOfWeek().getDisplayName(TextStyle.FULL, wkniES) + ", " + wkniD.getDayOfMonth() + " de "
            + wkniD.getMonth().getDisplayName(TextStyle.FULL, wkniES) + " de " + wkniD.getYear();
    }

    /** "Viernes, 16 de octubre" */
    public static String wkniFormatDateShort(LocalDate wkniD) {
        String wkniS = wkniD.getDayOfWeek().getDisplayName(TextStyle.FULL, wkniES) + ", " + wkniD.getDayOfMonth() + " de "
            + wkniD.getMonth().getDisplayName(TextStyle.FULL, wkniES);
        return Character.toUpperCase(wkniS.charAt(0)) + wkniS.substring(1);
    }

    /** "16 oct" */
    public static String wkniDayMonth(LocalDate wkniD) {
        return wkniD.getDayOfMonth() + " " + wkniD.getMonth().getDisplayName(TextStyle.SHORT, wkniES).replace(".", "");
    }

    /** "Hoy", "Mañana", "Ayer", "Sáb 17 oct"… */
    public static String wkniFriendlyDate(LocalDate wkniD) {
        if (wkniD == null) return "Sin fecha";
        LocalDate wkniHoy = LocalDate.now();
        long wkniDiff = ChronoUnit.DAYS.between(wkniHoy, wkniD);
        if (wkniDiff == 0) return "Hoy";
        if (wkniDiff == 1) return "Mañana";
        if (wkniDiff == -1) return "Ayer";
        String wkniDow = wkniD.getDayOfWeek().getDisplayName(TextStyle.SHORT, wkniES).replace(".", "");
        return Character.toUpperCase(wkniDow.charAt(0)) + wkniDow.substring(1) + " " + wkniDayMonth(wkniD)
            + (wkniD.getYear() != wkniHoy.getYear() ? " " + wkniD.getYear() : "");
    }

    /** "hoy", "ayer", "hace 3 días", "hace 2 meses" */
    public static String wkniHaceCuanto(LocalDate wkniD) {
        if (wkniD == null) return "sin registro";
        long wkniDias = ChronoUnit.DAYS.between(wkniD, LocalDate.now());
        if (wkniDias <= 0) return "hoy";
        if (wkniDias == 1) return "ayer";
        if (wkniDias < 45) return "hace " + wkniDias + " días";
        long wkniMeses = Math.round(wkniDias / 30.0);
        if (wkniMeses < 12) return "hace " + wkniMeses + (wkniMeses == 1 ? " mes" : " meses");
        long wkniAnos = Math.round(wkniDias / 365.0);
        return "hace " + wkniAnos + (wkniAnos == 1 ? " año" : " años");
    }

    private static final DecimalFormat wkniMONEY;
    static {
        DecimalFormatSymbols wkniS = new DecimalFormatSymbols(wkniES);
        wkniS.setDecimalSeparator(',');
        wkniS.setGroupingSeparator('.');
        wkniMONEY = new DecimalFormat("#,##0.00", wkniS);
    }

    /** "S/ 1.234,50" */
    public static String wkniMoney(double wkniV) { return "S/ " + wkniMONEY.format(wkniV); }

    /** Número escrito por el usuario: acepta "12,5", "12.5" y "1.234,50". */
    public static double wkniParseNumber(String wkniText) {
        String wkniT = wkniText == null ? "" : wkniText.trim().replace(" ", "").replace("S/", "");
        if (wkniT.isEmpty()) throw new NumberFormatException("vacío");
        if (wkniT.contains(",") && wkniT.contains(".")) wkniT = wkniT.replace(".", "").replace(",", ".");
        else wkniT = wkniT.replace(",", ".");
        return Double.parseDouble(wkniT);
    }

    /** Fuente fija que sigue la preferencia del usuario (atajo). */
    public static void wkniFont(Label wkniL, FontWeight wkniW, double wkni_size) { wkniFontPreferences.wkniBind(wkniL, wkniW, wkni_size); }
}

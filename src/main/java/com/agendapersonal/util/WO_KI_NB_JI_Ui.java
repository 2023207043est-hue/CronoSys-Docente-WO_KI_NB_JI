package com.agendapersonal.util;

import com.agendapersonal.ui.WO_KI_NB_JI_FontPreferences;
import com.agendapersonal.ui.WO_KI_NB_JI_IconFactory;
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
public final class WO_KI_NB_JI_Ui {
    public static final Locale ES = Locale.of("es", "PE");
    private static final Map<String, String> TINT = Map.ofEntries(
        Map.entry("home", "orange"), Map.entry("calendar", "violet"), Map.entry("agenda", "blue"),
        Map.entry("time", "orange"), Map.entry("clock", "orange"), Map.entry("tasks", "blue"),
        Map.entry("notes", "amber"), Map.entry("contacts", "violet"), Map.entry("habits", "green"),
        Map.entry("finance", "teal"), Map.entry("health", "rose"), Map.entry("settings", "slate"),
        Map.entry("book", "teal"), Map.entry("sparkle", "amber"));

    private WO_KI_NB_JI_Ui() {}

    /** Color temático de cada módulo (para su ícono). */
    public static String WO_KI_NB_JI_tintFor(String icon) { return TINT.getOrDefault(icon, "slate"); }

    // ------------------------------------------------------------- contenedores

    public static VBox WO_KI_NB_JI_card() {
        VBox b = new VBox(12);
        b.setPadding(new Insets(20));
        b.getStyleClass().add("card");
        return b;
    }

    /** Página estándar: fondo del tema y márgenes. */
    public static VBox WO_KI_NB_JI_page() {
        VBox p = new VBox(22);
        p.getStyleClass().add("page");
        p.setPadding(new Insets(28, 36, 32, 36));
        return p;
    }

    /** Envuelve una página para que se pueda desplazar si la ventana es pequeña. */
    public static ScrollPane WO_KI_NB_JI_scroll(Node content) {
        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.getStyleClass().add("page-scroll");
        return sp;
    }

    /** Encabezado de página: ícono del módulo, título, frase y acciones a la derecha. */
    public static HBox WO_KI_NB_JI_pageHeader(String icon, String title, String subtitle, Node... actions) {
        StackPane badge = WO_KI_NB_JI_iconChip(icon, WO_KI_NB_JI_tintFor(icon), 48, 22);
        Label t = new Label(title);
        t.getStyleClass().add("h1");
        VBox texts = new VBox(3, t);
        if (subtitle != null && !subtitle.isBlank()) {
            Label s = new Label(subtitle);
            s.getStyleClass().add("subtitle");
            s.setWrapText(true);
            texts.getChildren().add(s);
        }
        HBox left = new HBox(16, badge, texts);
        left.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox right = new HBox(10, actions);
        right.setAlignment(Pos.CENTER_RIGHT);
        HBox header = new HBox(16, left, right);
        header.getStyleClass().add("page-header");
        header.setAlignment(Pos.CENTER_LEFT);
        return header;
    }

    /** Firma anterior (los colores ya no se usan: salen del tema). */
    public static HBox WO_KI_NB_JI_pageHeader(String icon, String title, String subtitle, String bg, String border) {
        return WO_KI_NB_JI_pageHeader(icon, title, subtitle);
    }

    /** Tarjeta de resumen numérica (se mantiene por compatibilidad). */
    public static VBox WO_KI_NB_JI_summaryCard(String label, String value, String accent) {
        return WO_KI_NB_JI_stat(label, value, null, "tasks", "blue");
    }

    public static VBox WO_KI_NB_JI_summaryCard(String label, String value) { return WO_KI_NB_JI_summaryCard(label, value, null); }

    /** Tarjeta de estadística: etiqueta + ícono teñido arriba, cifra grande y una nota. */
    public static VBox WO_KI_NB_JI_stat(String label, String value, String note, String icon, String tone) {
        Label l = new Label(label);
        l.getStyleClass().add("stat-label");
        Region sp = WO_KI_NB_JI_spacer();
        HBox top = new HBox(8, l, sp, WO_KI_NB_JI_iconChip(icon, tone, 34, 18));
        top.setAlignment(Pos.CENTER_LEFT);
        Label v = new Label(value);
        v.getStyleClass().add("stat-value");
        VBox b = new VBox(10, top, v);
        if (note != null) {
            Label n = new Label(note);
            n.getStyleClass().add("stat-note");
            n.setWrapText(true);
            b.getChildren().add(n);
        }
        b.getStyleClass().addAll("card", "stat-card");
        b.setPadding(new Insets(18, 20, 18, 20));
        HBox.setHgrow(b, Priority.ALWAYS);
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    // ------------------------------------------------------------- textos

    public static Label WO_KI_NB_JI_h2(String text) { return WO_KI_NB_JI_label(text, "h2"); }
    public static Label WO_KI_NB_JI_h3(String text) { return WO_KI_NB_JI_label(text, "h3"); }
    public static Label WO_KI_NB_JI_muted(String text) {
        Label l = WO_KI_NB_JI_label(text, "muted");
        l.setWrapText(true);
        return l;
    }
    public static Label WO_KI_NB_JI_caption(String text) { return WO_KI_NB_JI_label(text, "caption"); }

    public static Label WO_KI_NB_JI_label(String text, String... classes) {
        Label l = new Label(text);
        l.getStyleClass().addAll(classes);
        return l;
    }

    /** Etiqueta de sección en mayúsculas pequeñas ("HOY", "ESTA SEMANA"). */
    public static Label WO_KI_NB_JI_overline(String text) {
        Label l = new Label(text.toUpperCase(ES));
        l.getStyleClass().add("overline");
        return l;
    }

    // ------------------------------------------------------------- botones

    public static Button WO_KI_NB_JI_primary(String icon, String text) { return WO_KI_NB_JI_button(icon, text, "btn-primary"); }
    public static Button WO_KI_NB_JI_secondary(String icon, String text) { return WO_KI_NB_JI_button(icon, text, "btn-secondary"); }
    public static Button WO_KI_NB_JI_ghost(String icon, String text) { return WO_KI_NB_JI_button(icon, text, "btn-ghost"); }
    public static Button WO_KI_NB_JI_danger(String icon, String text) { return WO_KI_NB_JI_button(icon, text, "btn-danger"); }

    private static Button WO_KI_NB_JI_button(String icon, String text, String cls) {
        Button b = new Button(text);
        if (icon != null) b.setGraphic(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon(icon, 17));
        b.getStyleClass().add(cls);
        b.setGraphicTextGap(8);
        b.setMnemonicParsing(false);
        b.setMinWidth(Region.USE_PREF_SIZE);       // nunca se corta el texto del botón ("Regis…")
        return b;
    }

    /** Botón cuadrado solo con ícono (con texto de ayuda y nombre accesible). */
    public static Button WO_KI_NB_JI_iconButton(String icon, String tip) {
        Button b = new Button();
        b.setGraphic(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon(icon, 18));
        b.getStyleClass().add("icon-button");
        b.setMinWidth(Region.USE_PREF_SIZE);
        b.setAccessibleText(tip);
        WO_KI_NB_JI_tooltip(b, tip);
        return b;
    }

    public static void WO_KI_NB_JI_tooltip(javafx.scene.control.Control c, String text) {
        Tooltip t = new Tooltip(text);
        t.setShowDelay(Duration.millis(350));
        c.setTooltip(t);
    }

    /** Convierte un botón existente en principal (antes se hacía con estilo en línea). */
    public static void WO_KI_NB_JI_stylePrimary(Button b) {
        b.getStyleClass().removeAll("btn-secondary", "btn-ghost", "btn-danger");
        if (!b.getStyleClass().contains("btn-primary")) b.getStyleClass().add("btn-primary");
    }

    public static Button WO_KI_NB_JI_actionButton(String icon, String text) { return WO_KI_NB_JI_secondary(icon, text); }

    public static void WO_KI_NB_JI_styleSoft(Button b, String bg, String fg) {
        b.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";");
        b.getStyleClass().add("btn-secondary");
    }

    public static Button WO_KI_NB_JI_menu(String t, boolean on) {
        Button b = new Button(t);
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.getStyleClass().add(on ? "menu-btn-active" : "menu-btn");
        return b;
    }

    // ------------------------------------------------------------- piezas

    /** Ícono dentro de un cuadrito redondeado de color suave. */
    public static StackPane WO_KI_NB_JI_iconChip(String icon, String tone, double box, double iconSize) {
        StackPane s = new StackPane(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon(icon, iconSize, "tint-" + tone));
        s.getStyleClass().addAll("icon-chip", "chip-" + tone);
        s.setMinSize(box, box);
        s.setPrefSize(box, box);
        s.setMaxSize(box, box);
        return s;
    }

    /** Etiqueta tipo píldora ("Alta", "Trabajo", "Escríbele"...). */
    public static Label WO_KI_NB_JI_pill(String text, String tone) {
        Label l = new Label(text);
        l.getStyleClass().addAll("pill", "pill-" + tone);
        l.setMinWidth(Region.USE_PREF_SIZE);
        return l;
    }

    /** Avatar con iniciales y un color estable según el nombre. */
    public static StackPane WO_KI_NB_JI_avatar(String name, double size) {
        String[] tones = {"blue", "green", "violet", "amber", "rose", "teal", "orange"};
        String n = name == null ? "?" : name.trim();
        String tone = tones[Math.floorMod(n.toLowerCase(ES).hashCode(), tones.length)];
        Label l = new Label(WO_KI_NB_JI_initials(n));
        l.getStyleClass().addAll("avatar-text", "tint-" + tone);
        l.setStyle("-fx-font-size:" + Math.round(size * 0.36) + "px;");
        StackPane s = new StackPane(l);
        s.getStyleClass().addAll("avatar", "chip-" + tone);
        s.setMinSize(size, size);
        s.setPrefSize(size, size);
        s.setMaxSize(size, size);
        return s;
    }

    public static String WO_KI_NB_JI_initials(String n) {
        String[] parts = n.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) if (!p.isEmpty() && sb.length() < 2) sb.append(Character.toUpperCase(p.charAt(0)));
        return sb.isEmpty() ? "?" : sb.toString();
    }

    /** Barra de progreso fina (0..1) en el color indicado. */
    public static StackPane WO_KI_NB_JI_bar(double fraction, String tone, double height) {
        Region fill = new Region();
        fill.getStyleClass().addAll("bar-fill", "fill-" + tone);
        StackPane track = new StackPane(fill);
        track.getStyleClass().add("bar-track");
        track.setAlignment(Pos.CENTER_LEFT);
        track.setMinHeight(height);
        track.setPrefHeight(height);
        track.setMaxHeight(height);
        double f = Math.max(0, Math.min(1, fraction));
        fill.maxWidthProperty().bind(track.widthProperty().multiply(f));
        fill.prefWidthProperty().bind(track.widthProperty().multiply(f));
        return track;
    }

    /** Campo de búsqueda con lupa. */
    public static HBox WO_KI_NB_JI_searchBox(TextField field, String prompt) {
        field.setPromptText(prompt);
        field.getStyleClass().add("bare-field");
        HBox.setHgrow(field, Priority.ALWAYS);
        HBox box = new HBox(8, WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("search", 17), field);
        box.getStyleClass().add("search-box");
        box.setAlignment(Pos.CENTER_LEFT);
        box.setOnMouseClicked(e -> field.requestFocus());
        field.focusedProperty().addListener((o, a, b) ->
            box.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("focused"), b));
        return box;
    }

    /** Campo de formulario: etiqueta arriba y control debajo. */
    public static VBox WO_KI_NB_JI_field(String label, Node control) {
        Label l = new Label(label);
        l.getStyleClass().add("field-label");
        l.setLabelFor(control);
        if (control instanceof Region r) r.setMaxWidth(Double.MAX_VALUE);
        VBox v = new VBox(7, l, control);
        HBox.setHgrow(v, Priority.ALWAYS);
        return v;
    }

    /** Selector de fecha en formato peruano (16/10/2026) en cualquier PC. */
    public static javafx.scene.control.DatePicker WO_KI_NB_JI_fechaCorta(javafx.scene.control.DatePicker dp) {
        java.time.format.DateTimeFormatter f = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        dp.setConverter(new javafx.util.StringConverter<>() {
            @Override public String toString(LocalDate d) { return d == null ? "" : f.format(d); }
            @Override public LocalDate fromString(String s) {
                try { return s == null || s.isBlank() ? null : LocalDate.parse(s.trim(), f); }
                catch (Exception e) { return dp.getValue(); }
            }
        });
        dp.setPromptText("dd/mm/aaaa");
        return dp;
    }

    /** Mensaje para cuando una lista está vacía. */
    public static VBox WO_KI_NB_JI_empty(String icon, String title, String text) {
        StackPane chip = WO_KI_NB_JI_iconChip(icon, "slate", 52, 24);
        Label t = new Label(title);
        t.getStyleClass().add("empty-title");
        Label m = new Label(text);
        m.getStyleClass().add("empty-text");
        m.setWrapText(true);
        m.setMaxWidth(340);
        m.setAlignment(Pos.CENTER);
        m.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        VBox v = new VBox(10, chip, t, m);
        v.setAlignment(Pos.CENTER);
        v.setPadding(new Insets(36, 20, 36, 20));
        v.getStyleClass().add("empty-state");
        return v;
    }

    public static Region WO_KI_NB_JI_spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        javafx.scene.layout.VBox.setVgrow(r, Priority.ALWAYS);
        return r;
    }

    /** Control segmentado (pestañas compactas). Siempre queda una opción elegida. */
    public static final class WO_KI_NB_JI_Segmented {
        private final HBox node = new HBox(2);
        private final ToggleGroup group = new ToggleGroup();
        private final List<ToggleButton> buttons = new ArrayList<>();
        private final IntegerProperty selected = new SimpleIntegerProperty(0);

        public WO_KI_NB_JI_Segmented(String... options) {
            node.getStyleClass().add("segmented");
            node.setAlignment(Pos.CENTER_LEFT);
            for (int i = 0; i < options.length; i++) {
                ToggleButton b = new ToggleButton(options[i]);
                b.getStyleClass().add("segment");
                b.setToggleGroup(group);
                b.setMnemonicParsing(false);
                b.setMinWidth(Region.USE_PREF_SIZE);
                final int idx = i;
                b.setOnAction(e -> { if (!b.isSelected()) b.setSelected(true); selected.set(idx); });
                buttons.add(b);
                node.getChildren().add(b);
            }
            if (!buttons.isEmpty()) buttons.getFirst().setSelected(true);
            selected.addListener((o, a, v) -> {
                int i = v.intValue();
                if (i >= 0 && i < buttons.size() && !buttons.get(i).isSelected()) buttons.get(i).setSelected(true);
            });
        }

        public HBox WO_KI_NB_JI_node() { return node; }

        /** Reparte el ancho disponible entre las opciones (para paneles estrechos). */
        public WO_KI_NB_JI_Segmented WO_KI_NB_JI_fillWidth() {
            node.setMaxWidth(Double.MAX_VALUE);
            for (ToggleButton b : buttons) {
                b.setMaxWidth(Double.MAX_VALUE);
                b.setMinWidth(0);
                HBox.setHgrow(b, Priority.ALWAYS);
            }
            return this;
        }
        public IntegerProperty WO_KI_NB_JI_selectedProperty() { return selected; }
        public int WO_KI_NB_JI_selected() { return selected.get(); }
        public void WO_KI_NB_JI_select(int i) { selected.set(i); }
        public void WO_KI_NB_JI_setText(int i, String text) { buttons.get(i).setText(text); }
        public ToggleButton WO_KI_NB_JI_button(int i) { return buttons.get(i); }
    }

    // ------------------------------------------------------------- diálogos

    /** Confirmación con el estilo de la app. Devuelve true si el usuario acepta. */
    public static boolean WO_KI_NB_JI_confirm(Node owner, String title, String text, String okText) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION);
        a.setTitle("Compás");
        a.setHeaderText(title);
        a.setContentText(text);
        a.setGraphic(WO_KI_NB_JI_iconChip("warning", "rose", 44, 22));
        ButtonType ok = new ButtonType(okText, ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel = new ButtonType("Cancelar", ButtonBar.ButtonData.CANCEL_CLOSE);
        a.getButtonTypes().setAll(cancel, ok);
        a.getDialogPane().getStyleClass().add("cs-dialog");
        ((Button) a.getDialogPane().lookupButton(ok)).getStyleClass().add("btn-danger-solid");
        ((Button) a.getDialogPane().lookupButton(cancel)).getStyleClass().add("btn-secondary");
        Window w = owner == null || owner.getScene() == null ? null : owner.getScene().getWindow();
        if (w != null) a.initOwner(w);
        return a.showAndWait().orElse(cancel) == ok;
    }

    /** Aviso simple con el estilo de la app. */
    public static void WO_KI_NB_JI_alert(Node owner, String title, String text) {
        Alert a = new Alert(Alert.AlertType.WARNING);
        a.setTitle("Compás");
        a.setHeaderText(title);
        a.setContentText(text);
        a.setGraphic(WO_KI_NB_JI_iconChip("warning", "amber", 44, 22));
        a.getDialogPane().getStyleClass().add("cs-dialog");
        Window w = owner == null || owner.getScene() == null ? null : owner.getScene().getWindow();
        if (w != null) a.initOwner(w);
        a.showAndWait();
    }

    /**
     * Da el estilo de Compás a un formulario armado como VBox de etiquetas y campos (diálogos de
     * eventos y tareas): título grande, etiquetas de campo, botón principal y logo en la ventana.
     */
    public static void WO_KI_NB_JI_decorarDialogo(javafx.scene.Scene sc) {
        com.agendapersonal.ui.WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_get().WO_KI_NB_JI_applyTo(sc);
        if (sc.getWindow() instanceof javafx.stage.Stage st) {
            com.agendapersonal.ui.WO_KI_NB_JI_Logo.WO_KI_NB_JI_aplicarIconos(st);
            if (st.getTitle() == null || st.getTitle().isBlank()) st.setTitle("Compás");
        }
        if (!(sc.getRoot() instanceof VBox r)) return;
        r.getStyleClass().addAll("app-root", "dialog-root");
        r.setPadding(new Insets(24, 26, 22, 26));
        r.setSpacing(8);
        boolean primero = true;
        for (Node n : r.getChildren()) {
            if (n instanceof Label l) {
                l.getStyleClass().add(primero ? "h2" : "field-label");
                if (primero) VBox.setMargin(l, new Insets(0, 0, 8, 0));
                else VBox.setMargin(l, new Insets(6, 0, 0, 0));
                primero = false;
            } else if (n instanceof HBox h && h.getChildren().stream().allMatch(x -> x instanceof Button)) {
                h.setAlignment(Pos.CENTER_RIGHT);
                h.setSpacing(10);
                VBox.setMargin(h, new Insets(14, 0, 0, 0));
                for (Node x : h.getChildren()) {
                    Button b = (Button) x;
                    String t = b.getText() == null ? "" : b.getText();
                    if (t.startsWith("Guardar") || t.startsWith("Actualizar") || t.startsWith("Nueva")) b.getStyleClass().add("btn-primary");
                    else if (t.startsWith("Eliminar")) b.getStyleClass().add("btn-danger");
                    else b.getStyleClass().add("btn-secondary");
                }
            } else if (n instanceof HBox h) {
                h.setSpacing(10);
                for (Node x : h.getChildren()) {
                    HBox.setHgrow(x, Priority.ALWAYS);
                    if (x instanceof Region rx) rx.setMaxWidth(Double.MAX_VALUE);
                }
            } else if (n instanceof Region rg) {
                rg.setMaxWidth(Double.MAX_VALUE);
                if (n instanceof javafx.scene.control.DatePicker dp) WO_KI_NB_JI_fechaCorta(dp);
                if (n instanceof javafx.scene.control.TextArea ta) { ta.setPrefRowCount(4); ta.setWrapText(true); }
            }
        }
    }

    // ------------------------------------------------------------- formatos

    public static String WO_KI_NB_JI_formatDate(LocalDate d) {
        return d.getDayOfWeek().getDisplayName(TextStyle.FULL, ES) + ", " + d.getDayOfMonth() + " de "
            + d.getMonth().getDisplayName(TextStyle.FULL, ES) + " de " + d.getYear();
    }

    /** "Viernes, 16 de octubre" */
    public static String WO_KI_NB_JI_formatDateShort(LocalDate d) {
        String s = d.getDayOfWeek().getDisplayName(TextStyle.FULL, ES) + ", " + d.getDayOfMonth() + " de "
            + d.getMonth().getDisplayName(TextStyle.FULL, ES);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** "16 oct" */
    public static String WO_KI_NB_JI_dayMonth(LocalDate d) {
        return d.getDayOfMonth() + " " + d.getMonth().getDisplayName(TextStyle.SHORT, ES).replace(".", "");
    }

    /** "Hoy", "Mañana", "Ayer", "Sáb 17 oct"… */
    public static String WO_KI_NB_JI_friendlyDate(LocalDate d) {
        if (d == null) return "Sin fecha";
        LocalDate hoy = LocalDate.now();
        long diff = ChronoUnit.DAYS.between(hoy, d);
        if (diff == 0) return "Hoy";
        if (diff == 1) return "Mañana";
        if (diff == -1) return "Ayer";
        String dow = d.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES).replace(".", "");
        return Character.toUpperCase(dow.charAt(0)) + dow.substring(1) + " " + WO_KI_NB_JI_dayMonth(d)
            + (d.getYear() != hoy.getYear() ? " " + d.getYear() : "");
    }

    /** "hoy", "ayer", "hace 3 días", "hace 2 meses" */
    public static String WO_KI_NB_JI_haceCuanto(LocalDate d) {
        if (d == null) return "sin registro";
        long dias = ChronoUnit.DAYS.between(d, LocalDate.now());
        if (dias <= 0) return "hoy";
        if (dias == 1) return "ayer";
        if (dias < 45) return "hace " + dias + " días";
        long meses = Math.round(dias / 30.0);
        if (meses < 12) return "hace " + meses + (meses == 1 ? " mes" : " meses");
        long anos = Math.round(dias / 365.0);
        return "hace " + anos + (anos == 1 ? " año" : " años");
    }

    private static final DecimalFormat MONEY;
    static {
        DecimalFormatSymbols s = new DecimalFormatSymbols(ES);
        s.setDecimalSeparator(',');
        s.setGroupingSeparator('.');
        MONEY = new DecimalFormat("#,##0.00", s);
    }

    /** "S/ 1.234,50" */
    public static String WO_KI_NB_JI_money(double v) { return "S/ " + MONEY.format(v); }

    /** Número escrito por el usuario: acepta "12,5", "12.5" y "1.234,50". */
    public static double WO_KI_NB_JI_parseNumber(String text) {
        String t = text == null ? "" : text.trim().replace(" ", "").replace("S/", "");
        if (t.isEmpty()) throw new NumberFormatException("vacío");
        if (t.contains(",") && t.contains(".")) t = t.replace(".", "").replace(",", ".");
        else t = t.replace(",", ".");
        return Double.parseDouble(t);
    }

    /** Fuente fija que sigue la preferencia del usuario (atajo). */
    public static void WO_KI_NB_JI_font(Label l, FontWeight w, double size) { WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_bind(l, w, size); }
}

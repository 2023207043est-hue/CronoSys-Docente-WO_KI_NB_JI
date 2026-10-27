package com.agendapersonal.ui.settings;

import com.agendapersonal.settings.WO_KI_NB_JI_Colors;
import com.agendapersonal.settings.WO_KI_NB_JI_Palette;
import com.agendapersonal.settings.WO_KI_NB_JI_SettingsStore;
import com.agendapersonal.settings.WO_KI_NB_JI_ThemePreset;
import com.agendapersonal.ui.WO_KI_NB_JI_FontPreferences;
import com.agendapersonal.ui.WO_KI_NB_JI_Notifier;
import com.agendapersonal.ui.WO_KI_NB_JI_Switch;
import com.agendapersonal.ui.WO_KI_NB_JI_ThemeService;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tarjetas de personalización de la pantalla Configuración: temas, crear tema, forma y texto,
 * perfiles, y respaldo (exportar / importar / restablecer). Todo se aplica al instante.
 */
public final class WO_KI_NB_JI_PersonalizacionCards {
    private WO_KI_NB_JI_PersonalizacionCards() {}

    private static final String[] EXTRA_ACCENTS = {"#1F9D9A", "#D9A400", "#4C5BD4", "#9A6A3A"};
    private static final String[] EXTRA_NAMES = {"Turquesa", "Amarillo", "Índigo", "Marrón"};

    private static String WO_KI_NB_JI_hex(Color c) {
        return WO_KI_NB_JI_Colors.WO_KI_NB_JI_hex((int) Math.round(c.getRed() * 255), (int) Math.round(c.getGreen() * 255), (int) Math.round(c.getBlue() * 255));
    }

    // ---------------------------------------------------------------------------- temas

    public static VBox WO_KI_NB_JI_temas() {
        WO_KI_NB_JI_ThemeService theme = WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_get();
        FlowPane flow = new FlowPane(12, 12);
        List<Button> botones = new ArrayList<>();
        for (WO_KI_NB_JI_ThemePreset p : WO_KI_NB_JI_ThemePreset.ALL) {
            Region fondo = new Region();
            fondo.setPrefSize(104, 56);
            fondo.setMinSize(104, 56);
            Region tarjeta = new Region();
            tarjeta.setPrefSize(60, 18);
            tarjeta.setMaxSize(60, 18);
            tarjeta.setStyle("-fx-background-color:" + p.card() + "; -fx-background-radius:6;");
            Region punto = new Region();
            punto.setPrefSize(14, 14);
            punto.setMaxSize(14, 14);
            punto.setStyle("-fx-background-color:" + p.accent() + "; -fx-background-radius:99;");
            Region texto = new Region();
            texto.setPrefSize(34, 5);
            texto.setMaxSize(34, 5);
            texto.setStyle("-fx-background-color:" + p.text() + "; -fx-background-radius:3;");
            VBox dentro = new VBox(6, texto, new HBox(6, tarjeta, punto));
            dentro.setAlignment(Pos.CENTER_LEFT);
            dentro.setPadding(new javafx.geometry.Insets(10));
            fondo.setStyle("-fx-background-color:" + p.bg() + "; -fx-background-radius:10;");
            javafx.scene.layout.StackPane vista = new javafx.scene.layout.StackPane(fondo, dentro);
            vista.setMaxSize(104, 56);
            Label nombre = new Label(p.name());
            nombre.getStyleClass().add("caption");
            Button b = new Button();
            b.setGraphic(new VBox(6, vista, nombre));
            b.getStyleClass().add("theme-tile");
            b.setAccessibleText("Tema " + p.name());
            b.getProperties().put("theme", p.id());
            b.setOnAction(e -> theme.WO_KI_NB_JI_setTheme(p.id()));
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_tooltip(b, p.name());
            botones.add(b);
            flow.getChildren().add(b);
        }
        Runnable marcar = () -> {
            String actual = theme.WO_KI_NB_JI_settings().WO_KI_NB_JI_str("appearance.themeId");
            for (Button b : botones) b.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), b.getProperties().get("theme").equals(actual));
        };
        marcar.run();
        theme.WO_KI_NB_JI_revisionProperty().addListener((o, a, b) -> marcar.run());
        VBox card = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        card.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h2("Temas"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Elige una combinación de colores lista. Puedes ajustarla abajo con «Crear tema»."), flow);
        return card;
    }

    // ---------------------------------------------------------------------------- crear tema

    public static VBox WO_KI_NB_JI_crearTema() {
        WO_KI_NB_JI_ThemeService theme = WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_get();
        WO_KI_NB_JI_SettingsStore st = theme.WO_KI_NB_JI_settings();
        boolean[] interno = {false};

        ColorPicker bg = new ColorPicker(), card = new ColorPicker(), text = new ColorPicker(), accent = new ColorPicker();
        Label aviso = new Label();
        aviso.getStyleClass().add("muted");
        aviso.setWrapText(true);
        TextField hexField = new TextField();
        hexField.setPromptText("#RRGGBB");
        hexField.setPrefColumnCount(8);

        Runnable sync = () -> {
            interno[0] = true;
            WO_KI_NB_JI_Palette.WO_KI_NB_JI_Resolved r = theme.WO_KI_NB_JI_resolved();
            bg.setValue(Color.web(theme.WO_KI_NB_JI_color("-bg")));
            card.setValue(Color.web(theme.WO_KI_NB_JI_color("-surface")));
            text.setValue(Color.web(theme.WO_KI_NB_JI_color("-text")));
            Color ac = Color.web(theme.WO_KI_NB_JI_color("-accent"));
            accent.setValue(ac);
            hexField.setText(WO_KI_NB_JI_hex(ac));
            interno[0] = false;
            List<String> w = WO_KI_NB_JI_Palette.WO_KI_NB_JI_contrastWarnings(theme.WO_KI_NB_JI_color("-bg"), theme.WO_KI_NB_JI_color("-surface"), theme.WO_KI_NB_JI_color("-text"), theme.WO_KI_NB_JI_color("-accent"));
            aviso.setText(w.isEmpty() ? "" : "Aviso: " + String.join(" ", w) + " Puedes dejarlo así, es solo una sugerencia.");
            aviso.setVisible(!w.isEmpty());
            aviso.setManaged(!w.isEmpty());
        };
        sync.run();
        theme.WO_KI_NB_JI_revisionProperty().addListener((o, a, b) -> sync.run());

        bg.setOnAction(e -> { if (!interno[0]) st.WO_KI_NB_JI_set("appearance.colors.bg", WO_KI_NB_JI_hex(bg.getValue())); });
        card.setOnAction(e -> { if (!interno[0]) st.WO_KI_NB_JI_set("appearance.colors.card", WO_KI_NB_JI_hex(card.getValue())); });
        text.setOnAction(e -> { if (!interno[0]) st.WO_KI_NB_JI_set("appearance.colors.text", WO_KI_NB_JI_hex(text.getValue())); });
        accent.setOnAction(e -> { if (!interno[0]) st.WO_KI_NB_JI_set("appearance.colors.accent", WO_KI_NB_JI_hex(accent.getValue())); });
        Runnable aplicarHex = () -> {
            String h = WO_KI_NB_JI_Colors.WO_KI_NB_JI_normalize(hexField.getText());
            if (h == null) { hexField.setText(WO_KI_NB_JI_hex(accent.getValue())); return; }
            st.WO_KI_NB_JI_set("appearance.colors.accent", h);
        };
        hexField.setOnAction(e -> aplicarHex.run());
        hexField.focusedProperty().addListener((o, a, f) -> { if (!f) aplicarHex.run(); });

        FlowPane extras = new FlowPane(10, 10);
        for (int i = 0; i < EXTRA_ACCENTS.length; i++) {
            final String h = EXTRA_ACCENTS[i];
            Button b = new Button();
            b.setGraphic(new Region());
            ((Region) b.getGraphic()).setPrefSize(26, 26);
            ((Region) b.getGraphic()).setStyle("-fx-background-color:" + h + "; -fx-background-radius:99;");
            b.getStyleClass().add("swatch-button");
            b.setAccessibleText("Acento " + EXTRA_NAMES[i]);
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_tooltip(b, EXTRA_NAMES[i]);
            b.setOnAction(e -> st.WO_KI_NB_JI_set("appearance.colors.accent", h));
            extras.getChildren().add(b);
        }

        Button quitar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Quitar personalización");
        quitar.setOnAction(e -> theme.WO_KI_NB_JI_setTheme(st.WO_KI_NB_JI_str("appearance.themeId")));

        HBox colores = new HBox(14, WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Fondo", bg), WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Tarjetas", card), WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Texto", text), WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Acento", accent));
        HBox hexFila = new HBox(10, hexField, extras);
        hexFila.setAlignment(Pos.CENTER_LEFT);
        VBox box = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        box.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h2("Crear tema"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Cambia los cuatro colores base; el resto de la interfaz se calcula solo. El texto sobre el acento se elige automáticamente."),
            colores, WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Acento en hexadecimal o de la paleta extra", hexFila), aviso, quitar);
        return box;
    }

    // ---------------------------------------------------------------------------- forma y texto

    public static VBox WO_KI_NB_JI_forma() {
        WO_KI_NB_JI_ThemeService theme = WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_get();
        WO_KI_NB_JI_SettingsStore st = theme.WO_KI_NB_JI_settings();
        boolean[] interno = {false};

        Slider escala = new Slider(85, 125, WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_scalePercent());
        escala.setMajorTickUnit(5);
        escala.setBlockIncrement(1);
        Label escalaVal = new Label(WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_scalePercent() + " %");
        escala.valueProperty().addListener((o, a, v) -> escalaVal.setText(Math.round(v.doubleValue()) + " %"));
        Runnable aplicarEscala = () -> { if (!interno[0]) WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_setScale((int) Math.round(escala.getValue())); };
        escala.valueChangingProperty().addListener((o, a, v) -> { if (!v) aplicarEscala.run(); });
        escala.valueProperty().addListener((o, a, v) -> { if (!escala.isValueChanging()) aplicarEscala.run(); });

        Slider radio = new Slider(0, 28, theme.WO_KI_NB_JI_radius());
        radio.setBlockIncrement(1);
        Label radioVal = new Label(theme.WO_KI_NB_JI_radius() + " px");
        radio.valueProperty().addListener((o, a, v) -> radioVal.setText(Math.round(v.doubleValue()) + " px"));
        Runnable aplicarRadio = () -> { if (!interno[0]) st.WO_KI_NB_JI_set("appearance.radius", (int) Math.round(radio.getValue())); };
        radio.valueChangingProperty().addListener((o, a, v) -> { if (!v) aplicarRadio.run(); });
        radio.valueProperty().addListener((o, a, v) -> { if (!radio.isValueChanging()) aplicarRadio.run(); });

        String[] dens = {"compacta", "comoda", "amplia"};
        WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented densidad = new WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented("Compacta", "Cómoda", "Amplia");
        densidad.WO_KI_NB_JI_select(java.util.Arrays.asList(dens).indexOf(st.WO_KI_NB_JI_choice("appearance.density", dens)));
        densidad.WO_KI_NB_JI_selectedProperty().addListener((o, a, v) -> { if (!interno[0]) st.WO_KI_NB_JI_set("appearance.density", dens[v.intValue()]); });

        WO_KI_NB_JI_Switch calido = new WO_KI_NB_JI_Switch("Bajo contraste y luz cálida", "Filtro suave para cuidar la vista.", st.WO_KI_NB_JI_bool("appearance.warmLowContrast"));
        calido.WO_KI_NB_JI_selectedProperty().addListener((o, a, v) -> { if (!interno[0]) st.WO_KI_NB_JI_set("appearance.warmLowContrast", v); });
        WO_KI_NB_JI_Switch animaciones = new WO_KI_NB_JI_Switch("Reducir animaciones", "Quita los movimientos y transiciones.", st.WO_KI_NB_JI_bool("appearance.reduceMotion"));
        animaciones.WO_KI_NB_JI_selectedProperty().addListener((o, a, v) -> { if (!interno[0]) st.WO_KI_NB_JI_set("appearance.reduceMotion", v); });
        WO_KI_NB_JI_Switch porHora = new WO_KI_NB_JI_Switch("Tema automático por hora", "Claro de día y oscuro de noche (19:00 a 07:00), con los temas Claro/Oscuro.",
            st.WO_KI_NB_JI_bool("appearance.autoThemeByTime"));
        porHora.WO_KI_NB_JI_selectedProperty().addListener((o, a, v) -> { if (!interno[0]) st.WO_KI_NB_JI_set("appearance.autoThemeByTime", v); });

        // Si cambia el perfil, se leen los valores del nuevo (sin volver a escribirlos).
        theme.WO_KI_NB_JI_revisionProperty().addListener((o, a, b) -> {
            interno[0] = true;
            escala.setValue(WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_scalePercent());
            radio.setValue(theme.WO_KI_NB_JI_radius());
            densidad.WO_KI_NB_JI_select(java.util.Arrays.asList(dens).indexOf(st.WO_KI_NB_JI_choice("appearance.density", dens)));
            calido.WO_KI_NB_JI_selectedProperty().set(st.WO_KI_NB_JI_bool("appearance.warmLowContrast"));
            animaciones.WO_KI_NB_JI_selectedProperty().set(st.WO_KI_NB_JI_bool("appearance.reduceMotion"));
            porHora.WO_KI_NB_JI_selectedProperty().set(st.WO_KI_NB_JI_bool("appearance.autoThemeByTime"));
            interno[0] = false;
        });

        HBox escalaFila = new HBox(12, escala, escalaVal);
        HBox radioFila = new HBox(12, radio, radioVal);
        HBox.setHgrow(escala, Priority.ALWAYS);
        HBox.setHgrow(radio, Priority.ALWAYS);
        escalaFila.setAlignment(Pos.CENTER_LEFT);
        radioFila.setAlignment(Pos.CENTER_LEFT);
        VBox box = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        box.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h2("Forma, tamaño y efectos"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Escala del texto (85 % a 125 %)", escalaFila),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Radio de las esquinas (0 a 28 px)", radioFila),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Densidad", densidad.WO_KI_NB_JI_node()),
            WO_KI_NB_JI_sep(), calido, WO_KI_NB_JI_sep(), animaciones, WO_KI_NB_JI_sep(), porHora);
        return box;
    }

    // ---------------------------------------------------------------------------- perfiles

    public static VBox WO_KI_NB_JI_perfiles() {
        WO_KI_NB_JI_SettingsStore st = WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_get().WO_KI_NB_JI_settings();
        ComboBox<String> combo = new ComboBox<>();
        boolean[] interno = {false};

        // ---- horario: varias franjas y días de la semana ----
        VBox listaFranjas = new VBox(8);
        List<TextField> camposDesde = new ArrayList<>(), camposHasta = new ArrayList<>();
        List<javafx.scene.control.ToggleButton> botonesDia = new ArrayList<>();
        String[] etiquetasDia = {"L", "M", "X", "J", "V", "S", "D"};
        // Lo que se está editando todavía no está guardado: se guarda al pulsar «Guardar horario».
        final List<WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_Franja> borrador = new ArrayList<>();
        final Runnable[] repintar = new Runnable[1];   // referencia al propio repintado (para el botón de borrar)

        Runnable pintarFranjas = () -> {
            camposDesde.clear(); camposHasta.clear();
            listaFranjas.getChildren().clear();
            for (int i = 0; i < borrador.size(); i++) {
                final int idx = i;
                TextField desde = new TextField(borrador.get(i).desde());
                TextField hasta = new TextField(borrador.get(i).hasta());
                desde.setPrefColumnCount(5); hasta.setPrefColumnCount(5);
                desde.textProperty().addListener((o, a, b) ->
                    borrador.set(idx, new WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_Franja(b, hasta.getText())));
                hasta.textProperty().addListener((o, a, b) ->
                    borrador.set(idx, new WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_Franja(desde.getText(), b)));
                Button quitar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("delete", "Quitar esta franja");
                quitar.setOnAction(e -> { borrador.remove(idx); repintar[0].run(); });
                HBox fila = new HBox(8, desde, new Label("a"), hasta, quitar);
                fila.setAlignment(Pos.CENTER_LEFT);
                listaFranjas.getChildren().add(fila);
                camposDesde.add(desde); camposHasta.add(hasta);
            }
        };
        repintar[0] = pintarFranjas;

        Runnable cargarFranjas = () -> {
            borrador.clear();
            borrador.addAll(st.WO_KI_NB_JI_franjas(st.WO_KI_NB_JI_activeProfileId()));
            if (borrador.isEmpty()) borrador.add(new WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_Franja("08:00", "14:00"));
            pintarFranjas.run();
            List<Integer> dias = new ArrayList<>(st.WO_KI_NB_JI_dias(st.WO_KI_NB_JI_activeProfileId()));
            for (int i = 0; i < botonesDia.size(); i++) botonesDia.get(i).setSelected(dias.contains(i + 1));
        };

        Runnable cargar = () -> {
            interno[0] = true;
            List<String> nombres = new ArrayList<>();
            for (Map<String, Object> p : st.WO_KI_NB_JI_profiles()) nombres.add(p.get("id") + " · " + p.get("name"));
            combo.getItems().setAll(nombres);
            combo.getSelectionModel().select(nombres.indexOf(st.WO_KI_NB_JI_activeProfileId() + " · " + st.WO_KI_NB_JI_activeProfileName()));
            interno[0] = false;
            cargarFranjas.run();
        };
        cargar.run();
        combo.setCellFactory(lv -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(String s, boolean empty) { super.updateItem(s, empty); setText(empty || s == null ? null : s.substring(s.indexOf('·') + 2)); }
        });
        combo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(String s, boolean empty) { super.updateItem(s, empty); setText(empty || s == null ? null : s.substring(s.indexOf('·') + 2)); }
        });
        combo.setOnAction(e -> {
            if (interno[0] || combo.getValue() == null) return;
            st.WO_KI_NB_JI_activateProfile(combo.getValue().substring(0, combo.getValue().indexOf(' ')));
            cargarFranjas.run();
        });

        Button nuevo = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Nuevo"), renombrar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Renombrar"),
               duplicar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Duplicar"), borrar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Eliminar");
        nuevo.setOnAction(e -> WO_KI_NB_JI_pedirNombre("Nuevo perfil", "Perfil nuevo", n -> { String id = st.WO_KI_NB_JI_createProfile(n); cargar.run(); st.WO_KI_NB_JI_activateProfile(id); }));
        renombrar.setOnAction(e -> WO_KI_NB_JI_pedirNombre("Renombrar perfil", st.WO_KI_NB_JI_activeProfileName(), n -> { st.WO_KI_NB_JI_renameProfile(st.WO_KI_NB_JI_activeProfileId(), n); cargar.run(); }));
        duplicar.setOnAction(e -> { String id = st.WO_KI_NB_JI_duplicateProfile(st.WO_KI_NB_JI_activeProfileId()); cargar.run(); st.WO_KI_NB_JI_activateProfile(id); });
        borrar.setOnAction(e -> {
            if (st.WO_KI_NB_JI_profiles().size() <= 1) { WO_KI_NB_JI_Ui.WO_KI_NB_JI_alert(borrar, "No se puede eliminar", "Siempre debe quedar al menos un perfil."); return; }
            if (WO_KI_NB_JI_Ui.WO_KI_NB_JI_confirm(borrar, "¿Eliminar el perfil «" + st.WO_KI_NB_JI_activeProfileName() + "»?", "Se borran sus ajustes de personalización. Tus datos (eventos, tareas, notas…) no se tocan.", "Eliminar")) {
                st.WO_KI_NB_JI_deleteProfile(st.WO_KI_NB_JI_activeProfileId());
                cargar.run();
            }
        });

        // Días de la semana
        HBox filaDias = new HBox(6);
        for (int i = 0; i < etiquetasDia.length; i++) {
            javafx.scene.control.ToggleButton b = new javafx.scene.control.ToggleButton(etiquetasDia[i]);
            b.getStyleClass().add("segment");
            b.setMnemonicParsing(false);
            b.setMinWidth(Region.USE_PREF_SIZE);
            b.setAccessibleText("Día " + etiquetasDia[i]);
            botonesDia.add(b);
            filaDias.getChildren().add(b);
        }
        filaDias.getStyleClass().add("segmented");
        filaDias.setAlignment(Pos.CENTER_LEFT);

        Button anadir = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("add", "Añadir franja");
        anadir.setOnAction(e -> {
            borrador.add(new WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_Franja("16:00", "18:00"));
            pintarFranjas.run();
        });

        Button aplicarHorario = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("check", "Guardar horario"), sinHorario = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Sin horario");
        aplicarHorario.setOnAction(e -> {
            List<WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_Franja> franjas = new ArrayList<>();
            for (int i = 0; i < camposDesde.size(); i++) {
                String a = camposDesde.get(i).getText().trim(), b = camposHasta.get(i).getText().trim();
                if (!a.matches("([01]?\\d|2[0-3]):[0-5]\\d") || !b.matches("([01]?\\d|2[0-3]):[0-5]\\d")) {
                    WO_KI_NB_JI_Ui.WO_KI_NB_JI_alert(aplicarHorario, "Horario inválido", "Escribe las horas como HH:mm, por ejemplo 08:00 y 14:00.");
                    return;
                }
                franjas.add(new WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_Franja(a, b));
            }
            List<Integer> dias = new ArrayList<>();
            for (int i = 0; i < botonesDia.size(); i++) if (botonesDia.get(i).isSelected()) dias.add(i + 1);
            st.WO_KI_NB_JI_setScheduleWindows(st.WO_KI_NB_JI_activeProfileId(), franjas, dias);
            WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success("Horario guardado",
                dias.isEmpty() ? "El perfil se activará solo en esas franjas, todos los días."
                    : "El perfil se activará en esas franjas, los días marcados.");
            cargarFranjas.run();
        });
        sinHorario.setOnAction(e -> {
            st.WO_KI_NB_JI_setScheduleWindows(st.WO_KI_NB_JI_activeProfileId(), List.of(), List.of());
            cargarFranjas.run();
        });

        HBox fila1 = new HBox(8, nuevo, renombrar, duplicar, borrar);
        HBox filaBotones = new HBox(8, aplicarHorario, sinHorario, anadir);
        filaBotones.setAlignment(Pos.CENTER_LEFT);
        Label aviso = WO_KI_NB_JI_Ui.WO_KI_NB_JI_caption("Si no marcas ningún día, el perfil se activa esos días todos.");
        VBox box = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        box.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h2("Perfiles"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Un perfil guarda todo tu estilo (tema, tamaños, opciones de cada ventana). Puede activarse "
                + "solo por horario: varias franjas y los días que tú elijas, por ejemplo Estudio de 08:00 a 12:00 "
                + "y de 16:00 a 18:00 solo de lunes a viernes."),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Perfil activo", combo), fila1,
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Franjas horarias", listaFranjas),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Días de la semana", filaDias),
            filaBotones, aviso);
        return box;
    }

    private static void WO_KI_NB_JI_pedirNombre(String titulo, String inicial, java.util.function.Consumer<String> ok) {
        TextInputDialog d = new TextInputDialog(inicial);
        d.setTitle("Compás");
        d.setHeaderText(titulo);
        d.setContentText("Nombre:");
        d.showAndWait().map(String::trim).filter(s -> !s.isEmpty()).ifPresent(ok);
    }

    // ---------------------------------------------------------------------------- silencio

    /** Horas de silencio: en ese rango no salen avisos de escritorio. */
    public static VBox WO_KI_NB_JI_silencio() {
        WO_KI_NB_JI_SettingsStore st = WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_get().WO_KI_NB_JI_settings();
        TextField desde = new TextField(st.WO_KI_NB_JI_str("notifications.quietHours.from")), hasta = new TextField(st.WO_KI_NB_JI_str("notifications.quietHours.to"));
        desde.setPromptText("22:00");
        hasta.setPromptText("07:00");
        desde.setPrefColumnCount(5);
        hasta.setPrefColumnCount(5);
        Button guardar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Guardar"), quitar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Sin silencio");
        guardar.setOnAction(e -> {
            String a = desde.getText().trim(), b = hasta.getText().trim();
            if (!a.matches("([01]?\\d|2[0-3]):[0-5]\\d") || !b.matches("([01]?\\d|2[0-3]):[0-5]\\d")) {
                WO_KI_NB_JI_Ui.WO_KI_NB_JI_alert(guardar, "Horas inválidas", "Escribe las horas como HH:mm, por ejemplo 22:00 y 07:00.");
                return;
            }
            st.WO_KI_NB_JI_set("notifications.quietHours.from", a);
            st.WO_KI_NB_JI_set("notifications.quietHours.to", b);
        });
        quitar.setOnAction(e -> { st.WO_KI_NB_JI_set("notifications.quietHours.from", ""); st.WO_KI_NB_JI_set("notifications.quietHours.to", ""); desde.clear(); hasta.clear(); });
        HBox fila = new HBox(8, desde, new Label("a"), hasta, guardar, quitar);
        fila.setAlignment(Pos.CENTER_LEFT);
        VBox box = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        box.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h2("Horas de silencio"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Entre estas horas no aparecen avisos de escritorio (por ejemplo de noche). Los avisos solo recuerdan eventos, cumpleaños y pendientes; guardar cambios no genera avisos salvo que lo actives en «Confirmaciones»."),
            fila);
        return box;
    }

    // ---------------------------------------------------------------------------- respaldo

    public static VBox WO_KI_NB_JI_respaldo() {
        WO_KI_NB_JI_SettingsStore st = WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_get().WO_KI_NB_JI_settings();
        Button expActivo = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Exportar perfil activo"), expTodos = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Exportar todos"),
               imp = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Importar…"), reset = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Restablecer perfil activo");
        expActivo.setOnAction(e -> WO_KI_NB_JI_exportar(expActivo, st, false));
        expTodos.setOnAction(e -> WO_KI_NB_JI_exportar(expTodos, st, true));
        imp.setOnAction(e -> WO_KI_NB_JI_importar(imp, st));
        reset.setOnAction(e -> {
            if (WO_KI_NB_JI_Ui.WO_KI_NB_JI_confirm(reset, "¿Restablecer toda la personalización de este perfil?",
                "Vuelven los valores por defecto (tema, tamaños y opciones de cada ventana). Tus datos no se tocan.", "Restablecer")) {
                st.WO_KI_NB_JI_resetActiveProfile();
                WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_reload();
            }
        });
        FlowPane botones = new FlowPane(8, 8, expActivo, expTodos, imp, reset);
        VBox box = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        box.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h2("Respaldo de personalización"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Exporta tu estilo a un archivo JSON para llevarlo a otra instalación. Al importar verás un resumen y deberás confirmar."), botones);
        return box;
    }

    private static void WO_KI_NB_JI_exportar(Node owner, WO_KI_NB_JI_SettingsStore st, boolean todos) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Exportar personalización");
        fc.setInitialFileName(todos ? "compas-perfiles.json" : "compas-" + st.WO_KI_NB_JI_activeProfileName().toLowerCase().replaceAll("[^a-z0-9]+", "-") + ".json");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File f = fc.showSaveDialog(owner.getScene() == null ? null : owner.getScene().getWindow());
        if (f == null) return;
        try {
            Files.writeString(f.toPath(), st.WO_KI_NB_JI_export(todos), StandardCharsets.UTF_8);
            WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success("Exportado", f.getName());
        } catch (Exception ex) {
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_alert(owner, "No se pudo exportar", "No se pudo escribir el archivo: " + ex.getMessage());
        }
    }

    private static void WO_KI_NB_JI_importar(Node owner, WO_KI_NB_JI_SettingsStore st) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Importar personalización");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File f = fc.showOpenDialog(owner.getScene() == null ? null : owner.getScene().getWindow());
        if (f == null) return;
        String texto;
        try { texto = Files.readString(f.toPath(), StandardCharsets.UTF_8); }
        catch (Exception ex) { WO_KI_NB_JI_Ui.WO_KI_NB_JI_alert(owner, "No se pudo leer", "No se pudo abrir el archivo."); return; }
        WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_ImportPreview pv = WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_previewImport(texto);
        if (!pv.valid()) { WO_KI_NB_JI_Ui.WO_KI_NB_JI_alert(owner, "Archivo no válido", pv.message() + " No se cambió nada."); return; }
        if (WO_KI_NB_JI_Ui.WO_KI_NB_JI_confirm(owner, "¿Importar este archivo?", pv.message() + " Se activará el primero. Tus perfiles actuales se conservan.", "Importar")) {
            st.WO_KI_NB_JI_applyImport(pv);
            WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_reload();
            WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success("Importado", "Personalización aplicada.");
        }
    }

    private static Region WO_KI_NB_JI_sep() {
        Region r = new Region();
        r.getStyleClass().add("hairline");
        return r;
    }
}

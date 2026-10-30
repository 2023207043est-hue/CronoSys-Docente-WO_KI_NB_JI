package com.agendapersonal.ui.settings;

import com.agendapersonal.settings.wkniColors;
import com.agendapersonal.settings.wkniPalette;
import com.agendapersonal.settings.wkniSettingsStore;
import com.agendapersonal.settings.wkniThemePreset;
import com.agendapersonal.ui.wkniFontPreferences;
import com.agendapersonal.ui.wkniNotifier;
import com.agendapersonal.ui.wkniSwitch;
import com.agendapersonal.ui.wkniThemeService;
import com.agendapersonal.util.wkniUi;
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
public final class wkniPersonalizacionCards {
    private wkniPersonalizacionCards() {}

    private static final String[] wkniEXTRA_ACCENTS = {"#1F9D9A", "#D9A400", "#4C5BD4", "#9A6A3A"};
    private static final String[] wkniEXTRA_NAMES = {"Turquesa", "Amarillo", "Índigo", "Marrón"};

    private static String wkniHex(Color wkniC) {
        return wkniColors.wkniHex((int) Math.round(wkniC.getRed() * 255), (int) Math.round(wkniC.getGreen() * 255), (int) Math.round(wkniC.getBlue() * 255));
    }

    // ---------------------------------------------------------------------------- temas

    public static VBox wkniTemas() {
        wkniThemeService wkniTheme = wkniThemeService.wkniGet();
        FlowPane wkniFlow = new FlowPane(12, 12);
        List<Button> wkniBotones = new ArrayList<>();
        for (wkniThemePreset wkni_p : wkniThemePreset.wkniALL) {
            Region wkniFondo = new Region();
            wkniFondo.setPrefSize(104, 56);
            wkniFondo.setMinSize(104, 56);
            Region wkniTarjeta = new Region();
            wkniTarjeta.setPrefSize(60, 18);
            wkniTarjeta.setMaxSize(60, 18);
            wkniTarjeta.setStyle("-fx-background-color:" + wkni_p.wkniCard() + "; -fx-background-radius:6;");
            Region wkniPunto = new Region();
            wkniPunto.setPrefSize(14, 14);
            wkniPunto.setMaxSize(14, 14);
            wkniPunto.setStyle("-fx-background-color:" + wkni_p.wkni_accent() + "; -fx-background-radius:99;");
            Region wkniTexto = new Region();
            wkniTexto.setPrefSize(34, 5);
            wkniTexto.setMaxSize(34, 5);
            wkniTexto.setStyle("-fx-background-color:" + wkni_p.wkniText() + "; -fx-background-radius:3;");
            VBox wkniDentro = new VBox(6, wkniTexto, new HBox(6, wkniTarjeta, wkniPunto));
            wkniDentro.setAlignment(Pos.CENTER_LEFT);
            wkniDentro.setPadding(new javafx.geometry.Insets(10));
            wkniFondo.setStyle("-fx-background-color:" + wkni_p.wkniBg() + "; -fx-background-radius:10;");
            javafx.scene.layout.StackPane wkni_vista = new javafx.scene.layout.StackPane(wkniFondo, wkniDentro);
            wkni_vista.setMaxSize(104, 56);
            Label wkniNombre = new Label(wkni_p.wkniName());
            wkniNombre.getStyleClass().add("caption");
            Button wkniB = new Button();
            wkniB.setGraphic(new VBox(6, wkni_vista, wkniNombre));
            wkniB.getStyleClass().add("theme-tile");
            wkniB.setAccessibleText("Tema " + wkni_p.wkniName());
            wkniB.getProperties().put("theme", wkni_p.wkniId());
            wkniB.setOnAction(wkniE -> wkniTheme.wkniSetTheme(wkni_p.wkniId()));
            wkniUi.wkniTooltip(wkniB, wkni_p.wkniName());
            wkniBotones.add(wkniB);
            wkniFlow.getChildren().add(wkniB);
        }
        Runnable wkniMarcar = () -> {
            String wkniActual = wkniTheme.wkniSettings().wkniStr("appearance.themeId");
            for (Button wkniB : wkniBotones) wkniB.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), wkniB.getProperties().get("theme").equals(wkniActual));
        };
        wkniMarcar.run();
        wkniTheme.wkniRevisionProperty().addListener((wkniO, wkniA, wkniB) -> wkniMarcar.run());
        VBox wkniCard = wkniUi.wkniCard();
        wkniCard.getChildren().addAll(wkniUi.wkniH2("Temas"),
            wkniUi.wkniMuted("Elige una combinación de colores lista. Puedes ajustarla abajo con «Crear tema»."), wkniFlow);
        return wkniCard;
    }

    // ---------------------------------------------------------------------------- crear tema

    public static VBox wkniCrearTema() {
        wkniThemeService wkniTheme = wkniThemeService.wkniGet();
        wkniSettingsStore wkniSt = wkniTheme.wkniSettings();
        boolean[] wkniInterno = {false};

        ColorPicker wkniBg = new ColorPicker(), wkniCard = new ColorPicker(), wkniText = new ColorPicker(), wkni_accent = new ColorPicker();
        Label wkni_aviso = new Label();
        wkni_aviso.getStyleClass().add("muted");
        wkni_aviso.setWrapText(true);
        TextField wkniHexField = new TextField();
        wkniHexField.setPromptText("#RRGGBB");
        wkniHexField.setPrefColumnCount(8);

        Runnable wkniSync = () -> {
            wkniInterno[0] = true;
            wkniPalette.wkniResolved wkniR = wkniTheme.wkniResolved();
            wkniBg.setValue(Color.web(wkniTheme.wkniColor("-bg")));
            wkniCard.setValue(Color.web(wkniTheme.wkniColor("-surface")));
            wkniText.setValue(Color.web(wkniTheme.wkniColor("-text")));
            Color wkniAc = Color.web(wkniTheme.wkniColor("-accent"));
            wkni_accent.setValue(wkniAc);
            wkniHexField.setText(wkniHex(wkniAc));
            wkniInterno[0] = false;
            List<String> wkniW = wkniPalette.wkniContrastWarnings(wkniTheme.wkniColor("-bg"), wkniTheme.wkniColor("-surface"), wkniTheme.wkniColor("-text"), wkniTheme.wkniColor("-accent"));
            wkni_aviso.setText(wkniW.isEmpty() ? "" : "Aviso: " + String.join(" ", wkniW) + " Puedes dejarlo así, es solo una sugerencia.");
            wkni_aviso.setVisible(!wkniW.isEmpty());
            wkni_aviso.setManaged(!wkniW.isEmpty());
        };
        wkniSync.run();
        wkniTheme.wkniRevisionProperty().addListener((wkniO, wkniA, wkniB) -> wkniSync.run());

        wkniBg.setOnAction(wkniE -> { if (!wkniInterno[0]) wkniSt.wkniSet("appearance.colors.bg", wkniHex(wkniBg.getValue())); });
        wkniCard.setOnAction(wkniE -> { if (!wkniInterno[0]) wkniSt.wkniSet("appearance.colors.card", wkniHex(wkniCard.getValue())); });
        wkniText.setOnAction(wkniE -> { if (!wkniInterno[0]) wkniSt.wkniSet("appearance.colors.text", wkniHex(wkniText.getValue())); });
        wkni_accent.setOnAction(wkniE -> { if (!wkniInterno[0]) wkniSt.wkniSet("appearance.colors.accent", wkniHex(wkni_accent.getValue())); });
        Runnable wkniAplicarHex = () -> {
            String wkniH = wkniColors.wkniNormalize(wkniHexField.getText());
            if (wkniH == null) { wkniHexField.setText(wkniHex(wkni_accent.getValue())); return; }
            wkniSt.wkniSet("appearance.colors.accent", wkniH);
        };
        wkniHexField.setOnAction(wkniE -> wkniAplicarHex.run());
        wkniHexField.focusedProperty().addListener((wkniO, wkniA, wkniF) -> { if (!wkniF) wkniAplicarHex.run(); });

        FlowPane wkniExtras = new FlowPane(10, 10);
        for (int wkniI = 0; wkniI < wkniEXTRA_ACCENTS.length; wkniI++) {
            final String wkniH = wkniEXTRA_ACCENTS[wkniI];
            Button wkniB = new Button();
            wkniB.setGraphic(new Region());
            ((Region) wkniB.getGraphic()).setPrefSize(26, 26);
            ((Region) wkniB.getGraphic()).setStyle("-fx-background-color:" + wkniH + "; -fx-background-radius:99;");
            wkniB.getStyleClass().add("swatch-button");
            wkniB.setAccessibleText("Acento " + wkniEXTRA_NAMES[wkniI]);
            wkniUi.wkniTooltip(wkniB, wkniEXTRA_NAMES[wkniI]);
            wkniB.setOnAction(wkniE -> wkniSt.wkniSet("appearance.colors.accent", wkniH));
            wkniExtras.getChildren().add(wkniB);
        }

        Button wkniQuitar = wkniUi.wkniSecondary(null, "Quitar personalización");
        wkniQuitar.setOnAction(wkniE -> wkniTheme.wkniSetTheme(wkniSt.wkniStr("appearance.themeId")));

        HBox wkniColores = new HBox(14, wkniUi.wkniField("Fondo", wkniBg), wkniUi.wkniField("Tarjetas", wkniCard), wkniUi.wkniField("Texto", wkniText), wkniUi.wkniField("Acento", wkni_accent));
        HBox wkniHexFila = new HBox(10, wkniHexField, wkniExtras);
        wkniHexFila.setAlignment(Pos.CENTER_LEFT);
        VBox wkniBox = wkniUi.wkniCard();
        wkniBox.getChildren().addAll(wkniUi.wkniH2("Crear tema"),
            wkniUi.wkniMuted("Cambia los cuatro colores base; el resto de la interfaz se calcula solo. El texto sobre el acento se elige automáticamente."),
            wkniColores, wkniUi.wkniField("Acento en hexadecimal o de la paleta extra", wkniHexFila), wkni_aviso, wkniQuitar);
        return wkniBox;
    }

    // ---------------------------------------------------------------------------- forma y texto

    public static VBox wkniForma() {
        wkniThemeService wkniTheme = wkniThemeService.wkniGet();
        wkniSettingsStore wkniSt = wkniTheme.wkniSettings();
        boolean[] wkniInterno = {false};

        Slider wkniEscala = new Slider(85, 125, wkniFontPreferences.wkniScalePercent());
        wkniEscala.setMajorTickUnit(5);
        wkniEscala.setBlockIncrement(1);
        Label wkniEscalaVal = new Label(wkniFontPreferences.wkniScalePercent() + " %");
        wkniEscala.valueProperty().addListener((wkniO, wkniA, wkniV) -> wkniEscalaVal.setText(Math.round(wkniV.doubleValue()) + " %"));
        Runnable wkniAplicarEscala = () -> { if (!wkniInterno[0]) wkniFontPreferences.wkniSetScale((int) Math.round(wkniEscala.getValue())); };
        wkniEscala.valueChangingProperty().addListener((wkniO, wkniA, wkniV) -> { if (!wkniV) wkniAplicarEscala.run(); });
        wkniEscala.valueProperty().addListener((wkniO, wkniA, wkniV) -> { if (!wkniEscala.isValueChanging()) wkniAplicarEscala.run(); });

        Slider wkniRadio = new Slider(0, 28, wkniTheme.wkniRadius());
        wkniRadio.setBlockIncrement(1);
        Label wkniRadioVal = new Label(wkniTheme.wkniRadius() + " px");
        wkniRadio.valueProperty().addListener((wkniO, wkniA, wkniV) -> wkniRadioVal.setText(Math.round(wkniV.doubleValue()) + " px"));
        Runnable wkniAplicarRadio = () -> { if (!wkniInterno[0]) wkniSt.wkniSet("appearance.radius", (int) Math.round(wkniRadio.getValue())); };
        wkniRadio.valueChangingProperty().addListener((wkniO, wkniA, wkniV) -> { if (!wkniV) wkniAplicarRadio.run(); });
        wkniRadio.valueProperty().addListener((wkniO, wkniA, wkniV) -> { if (!wkniRadio.isValueChanging()) wkniAplicarRadio.run(); });

        String[] wkniDens = {"compacta", "comoda", "amplia"};
        wkniUi.wkniSegmented wkniDensidad = new wkniUi.wkniSegmented("Compacta", "Cómoda", "Amplia");
        wkniDensidad.wkniSelect(java.util.Arrays.asList(wkniDens).indexOf(wkniSt.wkniChoice("appearance.density", wkniDens)));
        wkniDensidad.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> { if (!wkniInterno[0]) wkniSt.wkniSet("appearance.density", wkniDens[wkniV.intValue()]); });

        wkniSwitch wkniCalido = new wkniSwitch("Bajo contraste y luz cálida", "Filtro suave para cuidar la vista.", wkniSt.wkniBool("appearance.warmLowContrast"));
        wkniCalido.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> { if (!wkniInterno[0]) wkniSt.wkniSet("appearance.warmLowContrast", wkniV); });
        wkniSwitch wkniAnimaciones = new wkniSwitch("Reducir animaciones", "Quita los movimientos y transiciones.", wkniSt.wkniBool("appearance.reduceMotion"));
        wkniAnimaciones.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> { if (!wkniInterno[0]) wkniSt.wkniSet("appearance.reduceMotion", wkniV); });
        wkniSwitch wkniPorHora = new wkniSwitch("Tema automático por hora", "Claro de día y oscuro de noche (19:00 a 07:00), con los temas Claro/Oscuro.",
            wkniSt.wkniBool("appearance.autoThemeByTime"));
        wkniPorHora.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> { if (!wkniInterno[0]) wkniSt.wkniSet("appearance.autoThemeByTime", wkniV); });

        // Si cambia el perfil, se leen los valores del nuevo (sin volver a escribirlos).
        wkniTheme.wkniRevisionProperty().addListener((wkniO, wkniA, wkniB) -> {
            wkniInterno[0] = true;
            wkniEscala.setValue(wkniFontPreferences.wkniScalePercent());
            wkniRadio.setValue(wkniTheme.wkniRadius());
            wkniDensidad.wkniSelect(java.util.Arrays.asList(wkniDens).indexOf(wkniSt.wkniChoice("appearance.density", wkniDens)));
            wkniCalido.wkniSelectedProperty().set(wkniSt.wkniBool("appearance.warmLowContrast"));
            wkniAnimaciones.wkniSelectedProperty().set(wkniSt.wkniBool("appearance.reduceMotion"));
            wkniPorHora.wkniSelectedProperty().set(wkniSt.wkniBool("appearance.autoThemeByTime"));
            wkniInterno[0] = false;
        });

        HBox wkniEscalaFila = new HBox(12, wkniEscala, wkniEscalaVal);
        HBox wkniRadioFila = new HBox(12, wkniRadio, wkniRadioVal);
        HBox.setHgrow(wkniEscala, Priority.ALWAYS);
        HBox.setHgrow(wkniRadio, Priority.ALWAYS);
        wkniEscalaFila.setAlignment(Pos.CENTER_LEFT);
        wkniRadioFila.setAlignment(Pos.CENTER_LEFT);
        VBox wkniBox = wkniUi.wkniCard();
        wkniBox.getChildren().addAll(wkniUi.wkniH2("Forma, tamaño y efectos"),
            wkniUi.wkniField("Escala del texto (85 % a 125 %)", wkniEscalaFila),
            wkniUi.wkniField("Radio de las esquinas (0 a 28 px)", wkniRadioFila),
            wkniUi.wkniField("Densidad", wkniDensidad.wkniNode()),
            wkniSep(), wkniCalido, wkniSep(), wkniAnimaciones, wkniSep(), wkniPorHora);
        return wkniBox;
    }

    // ---------------------------------------------------------------------------- perfiles

    public static VBox wkniPerfiles() {
        wkniSettingsStore wkniSt = wkniThemeService.wkniGet().wkniSettings();
        ComboBox<String> wkniCombo = new ComboBox<>();
        boolean[] wkniInterno = {false};

        // ---- horario: varias franjas y días de la semana ----
        VBox wkniListaFranjas = new VBox(8);
        List<TextField> wkniCamposDesde = new ArrayList<>(), wkniCamposHasta = new ArrayList<>();
        List<javafx.scene.control.ToggleButton> wkniBotonesDia = new ArrayList<>();
        String[] wkniEtiquetasDia = {"L", "M", "X", "J", "V", "S", "D"};
        // Lo que se está editando todavía no está guardado: se guarda al pulsar «Guardar horario».
        final List<wkniSettingsStore.wkniFranja> wkniBorrador = new ArrayList<>();
        final Runnable[] wkniRepintar = new Runnable[1];   // referencia al propio repintado (para el botón de borrar)

        Runnable wkniPintarFranjas = () -> {
            wkniCamposDesde.clear(); wkniCamposHasta.clear();
            wkniListaFranjas.getChildren().clear();
            for (int wkniI = 0; wkniI < wkniBorrador.size(); wkniI++) {
                final int wkniIdx = wkniI;
                TextField wkniDesde = new TextField(wkniBorrador.get(wkniI).wkniDesde());
                TextField wkniHasta = new TextField(wkniBorrador.get(wkniI).wkniHasta());
                wkniDesde.setPrefColumnCount(5); wkniHasta.setPrefColumnCount(5);
                wkniDesde.textProperty().addListener((wkniO, wkniA, wkniB) ->
                    wkniBorrador.set(wkniIdx, new wkniSettingsStore.wkniFranja(wkniB, wkniHasta.getText())));
                wkniHasta.textProperty().addListener((wkniO, wkniA, wkniB) ->
                    wkniBorrador.set(wkniIdx, new wkniSettingsStore.wkniFranja(wkniDesde.getText(), wkniB)));
                Button wkniQuitar = wkniUi.wkniIconButton("delete", "Quitar esta franja");
                wkniQuitar.setOnAction(wkniE -> { wkniBorrador.remove(wkniIdx); wkniRepintar[0].run(); });
                HBox wkniFila = new HBox(8, wkniDesde, new Label("a"), wkniHasta, wkniQuitar);
                wkniFila.setAlignment(Pos.CENTER_LEFT);
                wkniListaFranjas.getChildren().add(wkniFila);
                wkniCamposDesde.add(wkniDesde); wkniCamposHasta.add(wkniHasta);
            }
        };
        wkniRepintar[0] = wkniPintarFranjas;

        Runnable wkniCargarFranjas = () -> {
            wkniBorrador.clear();
            wkniBorrador.addAll(wkniSt.wkniFranjas(wkniSt.wkniActiveProfileId()));
            if (wkniBorrador.isEmpty()) wkniBorrador.add(new wkniSettingsStore.wkniFranja("08:00", "14:00"));
            wkniPintarFranjas.run();
            List<Integer> wkniDias = new ArrayList<>(wkniSt.wkniDias(wkniSt.wkniActiveProfileId()));
            for (int wkniI = 0; wkniI < wkniBotonesDia.size(); wkniI++) wkniBotonesDia.get(wkniI).setSelected(wkniDias.contains(wkniI + 1));
        };

        Runnable wkniCargar = () -> {
            wkniInterno[0] = true;
            List<String> wkniNombres = new ArrayList<>();
            for (Map<String, Object> wkni_p : wkniSt.wkniProfiles()) wkniNombres.add(wkni_p.get("id") + " · " + wkni_p.get("name"));
            wkniCombo.getItems().setAll(wkniNombres);
            wkniCombo.getSelectionModel().select(wkniNombres.indexOf(wkniSt.wkniActiveProfileId() + " · " + wkniSt.wkniActiveProfileName()));
            wkniInterno[0] = false;
            wkniCargarFranjas.run();
        };
        wkniCargar.run();
        wkniCombo.setCellFactory(wkniLv -> new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(String wkniS, boolean wkniEmpty) { super.updateItem(wkniS, wkniEmpty); setText(wkniEmpty || wkniS == null ? null : wkniS.substring(wkniS.indexOf('·') + 2)); }
        });
        wkniCombo.setButtonCell(new javafx.scene.control.ListCell<>() {
            @Override protected void updateItem(String wkniS, boolean wkniEmpty) { super.updateItem(wkniS, wkniEmpty); setText(wkniEmpty || wkniS == null ? null : wkniS.substring(wkniS.indexOf('·') + 2)); }
        });
        wkniCombo.setOnAction(wkniE -> {
            if (wkniInterno[0] || wkniCombo.getValue() == null) return;
            wkniSt.wkniActivateProfile(wkniCombo.getValue().substring(0, wkniCombo.getValue().indexOf(' ')));
            wkniCargarFranjas.run();
        });

        Button wkniNuevo = wkniUi.wkniSecondary(null, "Nuevo"), wkniRenombrar = wkniUi.wkniSecondary(null, "Renombrar"),
               wkniDuplicar = wkniUi.wkniSecondary(null, "Duplicar"), wkniBorrar = wkniUi.wkniSecondary(null, "Eliminar");
        wkniNuevo.setOnAction(wkniE -> wkniPedirNombre("Nuevo perfil", "Perfil nuevo", wkniN -> { String wkniId = wkniSt.wkniCreateProfile(wkniN); wkniCargar.run(); wkniSt.wkniActivateProfile(wkniId); }));
        wkniRenombrar.setOnAction(wkniE -> wkniPedirNombre("Renombrar perfil", wkniSt.wkniActiveProfileName(), wkniN -> { wkniSt.wkniRenameProfile(wkniSt.wkniActiveProfileId(), wkniN); wkniCargar.run(); }));
        wkniDuplicar.setOnAction(wkniE -> { String wkniId = wkniSt.wkniDuplicateProfile(wkniSt.wkniActiveProfileId()); wkniCargar.run(); wkniSt.wkniActivateProfile(wkniId); });
        wkniBorrar.setOnAction(wkniE -> {
            if (wkniSt.wkniProfiles().size() <= 1) { wkniUi.wkniAlert(wkniBorrar, "No se puede eliminar", "Siempre debe quedar al menos un perfil."); return; }
            if (wkniUi.wkniConfirm(wkniBorrar, "¿Eliminar el perfil «" + wkniSt.wkniActiveProfileName() + "»?", "Se borran sus ajustes de personalización. Tus datos (eventos, tareas, notas…) no se tocan.", "Eliminar")) {
                wkniSt.wkniDeleteProfile(wkniSt.wkniActiveProfileId());
                wkniCargar.run();
            }
        });

        // Días de la semana
        HBox wkniFilaDias = new HBox(6);
        for (int wkniI = 0; wkniI < wkniEtiquetasDia.length; wkniI++) {
            javafx.scene.control.ToggleButton wkniB = new javafx.scene.control.ToggleButton(wkniEtiquetasDia[wkniI]);
            wkniB.getStyleClass().add("segment");
            wkniB.setMnemonicParsing(false);
            wkniB.setMinWidth(Region.USE_PREF_SIZE);
            wkniB.setAccessibleText("Día " + wkniEtiquetasDia[wkniI]);
            wkniBotonesDia.add(wkniB);
            wkniFilaDias.getChildren().add(wkniB);
        }
        wkniFilaDias.getStyleClass().add("segmented");
        wkniFilaDias.setAlignment(Pos.CENTER_LEFT);

        Button wkniAnadir = wkniUi.wkniSecondary("add", "Añadir franja");
        wkniAnadir.setOnAction(wkniE -> {
            wkniBorrador.add(new wkniSettingsStore.wkniFranja("16:00", "18:00"));
            wkniPintarFranjas.run();
        });

        Button wkniAplicarHorario = wkniUi.wkniPrimary("check", "Guardar horario"), wkniSinHorario = wkniUi.wkniSecondary(null, "Sin horario");
        wkniAplicarHorario.setOnAction(wkniE -> {
            List<wkniSettingsStore.wkniFranja> wkniFranjas = new ArrayList<>();
            for (int wkniI = 0; wkniI < wkniCamposDesde.size(); wkniI++) {
                String wkniA = wkniCamposDesde.get(wkniI).getText().trim(), wkniB = wkniCamposHasta.get(wkniI).getText().trim();
                if (!wkniA.matches("([01]?\\d|2[0-3]):[0-5]\\d") || !wkniB.matches("([01]?\\d|2[0-3]):[0-5]\\d")) {
                    wkniUi.wkniAlert(wkniAplicarHorario, "Horario inválido", "Escribe las horas como HH:mm, por ejemplo 08:00 y 14:00.");
                    return;
                }
                wkniFranjas.add(new wkniSettingsStore.wkniFranja(wkniA, wkniB));
            }
            List<Integer> wkniDias = new ArrayList<>();
            for (int wkniI = 0; wkniI < wkniBotonesDia.size(); wkniI++) if (wkniBotonesDia.get(wkniI).isSelected()) wkniDias.add(wkniI + 1);
            wkniSt.wkniSetScheduleWindows(wkniSt.wkniActiveProfileId(), wkniFranjas, wkniDias);
            wkniNotifier.wkniSuccess("Horario guardado",
                wkniDias.isEmpty() ? "El perfil se activará solo en esas franjas, todos los días."
                    : "El perfil se activará en esas franjas, los días marcados.");
            wkniCargarFranjas.run();
        });
        wkniSinHorario.setOnAction(wkniE -> {
            wkniSt.wkniSetScheduleWindows(wkniSt.wkniActiveProfileId(), List.of(), List.of());
            wkniCargarFranjas.run();
        });

        HBox wkniFila1 = new HBox(8, wkniNuevo, wkniRenombrar, wkniDuplicar, wkniBorrar);
        HBox wkniFilaBotones = new HBox(8, wkniAplicarHorario, wkniSinHorario, wkniAnadir);
        wkniFilaBotones.setAlignment(Pos.CENTER_LEFT);
        Label wkni_aviso = wkniUi.wkniCaption("Si no marcas ningún día, el perfil se activa esos días todos.");
        VBox wkniBox = wkniUi.wkniCard();
        wkniBox.getChildren().addAll(wkniUi.wkniH2("Perfiles"),
            wkniUi.wkniMuted("Un perfil guarda todo tu estilo (tema, tamaños, opciones de cada ventana). Puede activarse "
                + "solo por horario: varias franjas y los días que tú elijas, por ejemplo Estudio de 08:00 a 12:00 "
                + "y de 16:00 a 18:00 solo de lunes a viernes."),
            wkniUi.wkniField("Perfil activo", wkniCombo), wkniFila1,
            wkniUi.wkniField("Franjas horarias", wkniListaFranjas),
            wkniUi.wkniField("Días de la semana", wkniFilaDias),
            wkniFilaBotones, wkni_aviso);
        return wkniBox;
    }

    private static void wkniPedirNombre(String wkniTitulo, String wkniInicial, java.util.function.Consumer<String> wkniOk) {
        TextInputDialog wkniD = new TextInputDialog(wkniInicial);
        wkniD.setTitle("Compás");
        wkniD.setHeaderText(wkniTitulo);
        wkniD.setContentText("Nombre:");
        wkniD.showAndWait().map(String::trim).filter(wkniS -> !wkniS.isEmpty()).ifPresent(wkniOk);
    }

    // ---------------------------------------------------------------------------- silencio

    /** Horas de silencio: en ese rango no salen avisos de escritorio. */
    public static VBox wkniSilencio() {
        wkniSettingsStore wkniSt = wkniThemeService.wkniGet().wkniSettings();
        TextField wkniDesde = new TextField(wkniSt.wkniStr("notifications.quietHours.from")), wkniHasta = new TextField(wkniSt.wkniStr("notifications.quietHours.to"));
        wkniDesde.setPromptText("22:00");
        wkniHasta.setPromptText("07:00");
        wkniDesde.setPrefColumnCount(5);
        wkniHasta.setPrefColumnCount(5);
        Button wkniGuardar = wkniUi.wkniSecondary(null, "Guardar"), wkniQuitar = wkniUi.wkniSecondary(null, "Sin silencio");
        wkniGuardar.setOnAction(wkniE -> {
            String wkniA = wkniDesde.getText().trim(), wkniB = wkniHasta.getText().trim();
            if (!wkniA.matches("([01]?\\d|2[0-3]):[0-5]\\d") || !wkniB.matches("([01]?\\d|2[0-3]):[0-5]\\d")) {
                wkniUi.wkniAlert(wkniGuardar, "Horas inválidas", "Escribe las horas como HH:mm, por ejemplo 22:00 y 07:00.");
                return;
            }
            wkniSt.wkniSet("notifications.quietHours.from", wkniA);
            wkniSt.wkniSet("notifications.quietHours.to", wkniB);
        });
        wkniQuitar.setOnAction(wkniE -> { wkniSt.wkniSet("notifications.quietHours.from", ""); wkniSt.wkniSet("notifications.quietHours.to", ""); wkniDesde.clear(); wkniHasta.clear(); });
        HBox wkniFila = new HBox(8, wkniDesde, new Label("a"), wkniHasta, wkniGuardar, wkniQuitar);
        wkniFila.setAlignment(Pos.CENTER_LEFT);
        VBox wkniBox = wkniUi.wkniCard();
        wkniBox.getChildren().addAll(wkniUi.wkniH2("Horas de silencio"),
            wkniUi.wkniMuted("Entre estas horas no aparecen avisos de escritorio (por ejemplo de noche). Los avisos solo recuerdan eventos, cumpleaños y pendientes; guardar cambios no genera avisos salvo que lo actives en «Confirmaciones»."),
            wkniFila);
        return wkniBox;
    }

    // ---------------------------------------------------------------------------- respaldo

    public static VBox wkniRespaldo() {
        wkniSettingsStore wkniSt = wkniThemeService.wkniGet().wkniSettings();
        Button wkniExpActivo = wkniUi.wkniSecondary(null, "Exportar perfil activo"), wkniExpTodos = wkniUi.wkniSecondary(null, "Exportar todos"),
               wkniImp = wkniUi.wkniSecondary(null, "Importar…"), wkniReset = wkniUi.wkniSecondary(null, "Restablecer perfil activo");
        wkniExpActivo.setOnAction(wkniE -> wkniExportar(wkniExpActivo, wkniSt, false));
        wkniExpTodos.setOnAction(wkniE -> wkniExportar(wkniExpTodos, wkniSt, true));
        wkniImp.setOnAction(wkniE -> wkniImportar(wkniImp, wkniSt));
        wkniReset.setOnAction(wkniE -> {
            if (wkniUi.wkniConfirm(wkniReset, "¿Restablecer toda la personalización de este perfil?",
                "Vuelven los valores por defecto (tema, tamaños y opciones de cada ventana). Tus datos no se tocan.", "Restablecer")) {
                wkniSt.wkniResetActiveProfile();
                wkniFontPreferences.wkniReload();
            }
        });
        FlowPane wkniBotones = new FlowPane(8, 8, wkniExpActivo, wkniExpTodos, wkniImp, wkniReset);
        VBox wkniBox = wkniUi.wkniCard();
        wkniBox.getChildren().addAll(wkniUi.wkniH2("Respaldo de personalización"),
            wkniUi.wkniMuted("Exporta tu estilo a un archivo JSON para llevarlo a otra instalación. Al importar verás un resumen y deberás confirmar."), wkniBotones);
        return wkniBox;
    }

    private static void wkniExportar(Node wkniOwner, wkniSettingsStore wkniSt, boolean wkniTodos) {
        FileChooser wkniFc = new FileChooser();
        wkniFc.setTitle("Exportar personalización");
        wkniFc.setInitialFileName(wkniTodos ? "compas-perfiles.json" : "compas-" + wkniSt.wkniActiveProfileName().toLowerCase().replaceAll("[^a-z0-9]+", "-") + ".json");
        wkniFc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File wkniF = wkniFc.showSaveDialog(wkniOwner.getScene() == null ? null : wkniOwner.getScene().getWindow());
        if (wkniF == null) return;
        try {
            Files.writeString(wkniF.toPath(), wkniSt.wkniExport(wkniTodos), StandardCharsets.UTF_8);
            wkniNotifier.wkniSuccess("Exportado", wkniF.getName());
        } catch (Exception wkniEx) {
            wkniUi.wkniAlert(wkniOwner, "No se pudo exportar", "No se pudo escribir el archivo: " + wkniEx.getMessage());
        }
    }

    private static void wkniImportar(Node wkniOwner, wkniSettingsStore wkniSt) {
        FileChooser wkniFc = new FileChooser();
        wkniFc.setTitle("Importar personalización");
        wkniFc.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON", "*.json"));
        File wkniF = wkniFc.showOpenDialog(wkniOwner.getScene() == null ? null : wkniOwner.getScene().getWindow());
        if (wkniF == null) return;
        String wkniTexto;
        try { wkniTexto = Files.readString(wkniF.toPath(), StandardCharsets.UTF_8); }
        catch (Exception wkniEx) { wkniUi.wkniAlert(wkniOwner, "No se pudo leer", "No se pudo abrir el archivo."); return; }
        wkniSettingsStore.wkniImportPreview wkniPv = wkniSettingsStore.wkniPreviewImport(wkniTexto);
        if (!wkniPv.wkniValid()) { wkniUi.wkniAlert(wkniOwner, "Archivo no válido", wkniPv.wkniMessage() + " No se cambió nada."); return; }
        if (wkniUi.wkniConfirm(wkniOwner, "¿Importar este archivo?", wkniPv.wkniMessage() + " Se activará el primero. Tus perfiles actuales se conservan.", "Importar")) {
            wkniSt.wkniApplyImport(wkniPv);
            wkniFontPreferences.wkniReload();
            wkniNotifier.wkniSuccess("Importado", "Personalización aplicada.");
        }
    }

    private static Region wkniSep() {
        Region wkniR = new Region();
        wkniR.getStyleClass().add("hairline");
        return wkniR;
    }
}

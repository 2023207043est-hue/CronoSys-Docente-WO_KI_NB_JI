package com.agendapersonal.ui.modules;

import com.agendapersonal.ui.wkniFontPreferences;

import com.agendapersonal.controller.wkniMainController;
import com.agendapersonal.model.wkniEvento;
import com.agendapersonal.ui.wkniEventViews;
import com.agendapersonal.util.wkniUi;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Calendario Compás: vistas Año/Mes/Semana/Agenda a pantalla completa.
 * Clic en cualquier día para ver y crear eventos.
 */
public class wkniCalendarioModule {
    private enum wkniVista { ANO, MES, SEMANA, AGENDA }

    private final wkniMainController wkniC;
    private final Stage wkniStage;
    private final Locale wkniEs = Locale.of("es", "PE");
    private final VBox wkniView = new VBox(12);
    private final VBox wkniMainArea = new VBox(10);
    private final Label wkniRangeLabel = new Label();
    private final TextField wkniSearch = new TextField();
    private final ComboBox<String> wkniCatFilter = new ComboBox<>();
    private final ToggleGroup wkniGroup = new ToggleGroup();
    private LocalDate wkniCursor = LocalDate.now();
    private LocalDate wkniSelected = LocalDate.now();
    private wkniVista wkni_vista = wkniVista.MES;

    public wkniCalendarioModule(wkniMainController wkniC, Stage wkniStage) {
        this.wkniC = wkniC;
        this.wkniStage = wkniStage;
        wkniView.setPadding(new Insets(20));
        wkniView.getChildren().add(wkniUi.wkniPageHeader("calendar", "Calendario",
            "Clic en cualquier día para ver sus eventos y crear nuevos. Todo se adapta a la pantalla.",
            "-surface2", "-accent"));
        wkniView.getChildren().add(wkniToolbar());

        wkniCatFilter.getItems().addAll("Todas", "Personal", "Trabajo", "Estudio", "Salud", "Proyecto");
        wkniCatFilter.setValue("Todas");

        HBox.setHgrow(wkniMainArea, Priority.ALWAYS);
        VBox.setVgrow(wkniMainArea, Priority.ALWAYS);
        wkniView.getChildren().add(wkniMainArea);
        VBox.setVgrow(wkniMainArea, Priority.ALWAYS);

        wkniSearch.textProperty().addListener((wkniO, wkniA, wkniB) -> wkniRefreshAll());
        wkniCatFilter.setOnAction(wkniE -> wkniRefreshAll());
        wkniC.wkniGetEventos().addListener((Observable wkniO) -> Platform.runLater(this::wkniRefreshAll));
        wkniRefreshAll();
    }

    public VBox wkniGetView() { return wkniView; }

    // ---------- toolbar ----------
    private HBox wkniToolbar() {
        Button wkniPrev = wkniIconButton("arrow-left", "Mes anterior");
        Button wkniToday = new Button("Hoy");
        Button wkniNext = wkniIconButton("arrow-right", "Mes siguiente");
        wkniPrev.setOnAction(wkniE -> wkniMover(-1));
        wkniNext.setOnAction(wkniE -> wkniMover(1));
        wkniToday.setOnAction(wkniE -> {
            wkniCursor = LocalDate.now();
            wkniSelected = LocalDate.now();
            wkniRefreshAll();
        });
        wkniFontPreferences.wkniBind(wkniRangeLabel, FontWeight.BOLD, 17);
        wkniRangeLabel.setStyle("-fx-text-fill:-text;");
        Region wkniSp = new Region();
        HBox.setHgrow(wkniSp, Priority.ALWAYS);
        ToggleButton wkniBAno = wkniSegBtn("Año", wkniVista.ANO);
        ToggleButton wkniBMes = wkniSegBtn("Mes", wkniVista.MES);
        ToggleButton wkniBSem = wkniSegBtn("Semana", wkniVista.SEMANA);
        ToggleButton wkniBAge = wkniSegBtn("Agenda", wkniVista.AGENDA);
        wkniBMes.setSelected(true);
        HBox wkniSeg = new HBox(0, wkniBAno, wkniBMes, wkniBSem, wkniBAge);
        wkniSeg.setStyle("-fx-background-color:-surface2;-fx-background-radius:10;-fx-padding:2;");
        wkniSearch.setPromptText("Buscar eventos...");
        wkniSearch.setPrefWidth(200);
        Button wkniNuevo = wkniUi.wkniActionButton("add", "Nuevo evento");
        wkniUi.wkniStylePrimary(wkniNuevo);
        wkniNuevo.setOnAction(wkniE -> wkniEventViews.wkniFormEnFecha(wkniStage, null, wkniSelected, wkniC, () -> {}));
        HBox wkniBar = new HBox(8, wkniPrev, wkniToday, wkniNext, wkniRangeLabel, wkniSp, wkniSeg, wkniSearch, wkniCatFilter, wkniNuevo);
        wkniBar.setAlignment(Pos.CENTER_LEFT);
        return wkniBar;
    }

    private ToggleButton wkniSegBtn(String wkniT, wkniVista wkniV) {
        ToggleButton wkniB = new ToggleButton(wkniT);
        wkniB.setToggleGroup(wkniGroup);
        wkniB.setStyle("-fx-background-radius:8;-fx-padding:7 14;-fx-cursor:hand;");
        wkniB.setOnAction(wkniE -> { wkni_vista = wkniV; wkniRefreshAll(); });
        wkniGroup.selectedToggleProperty().addListener((wkniO, wkniA, wkniB2) -> {
            for (var wkniTg : wkniGroup.getToggles()) {
                ToggleButton wkniTb = (ToggleButton) wkniTg;
                boolean wkniOn = wkniTb.isSelected();
                wkniTb.setStyle("-fx-background-radius:8;-fx-padding:7 14;-fx-cursor:hand;"
                    + (wkniOn ? "-fx-background-color:-accent-soft;-fx-text-fill:-accent;-fx-font-weight:bold;"
                        : "-fx-background-color:transparent;-fx-text-fill:-muted;"));
            }
        });
        return wkniB;
    }

    private Button wkniIconButton(String wkniIcon, String wkniAccessibleText) {
        Button wkniButton = new Button();
        wkniButton.setGraphic(com.agendapersonal.ui.wkniIconFactory.wkniImage(wkniIcon, 16));
        wkniButton.setAccessibleText(wkniAccessibleText);
        wkniButton.setTooltip(new Tooltip(wkniAccessibleText));
        return wkniButton;
    }

    private void wkniMover(int wkniDir) {
        wkniCursor = switch (wkni_vista) {
            case ANO -> wkniCursor.plusYears(wkniDir);
            case MES -> wkniCursor.plusMonths(wkniDir);
            case SEMANA -> wkniCursor.plusWeeks(wkniDir);
            case AGENDA -> wkniCursor.plusMonths(wkniDir);
        };
        wkniRefreshAll();
    }

    // ---------- motor ----------
    private void wkniRefreshAll() {
        wkniMainArea.getChildren().setAll(switch (wkni_vista) {
            case ANO -> wkniAnoView();
            case MES -> wkniMesView();
            case SEMANA -> wkniSemanaView();
            case AGENDA -> wkniAgendaView();
        });
    }

    private boolean wkniPasa(wkniEvento wkniE) {
        String wkniQ = wkniSearch.getText().trim().toLowerCase();
        boolean wkniOkQ = wkniQ.isBlank() || wkniE.wkniGetTitulo().toLowerCase().contains(wkniQ)
            || (wkniE.wkniGetDescripcion() != null && wkniE.wkniGetDescripcion().toLowerCase().contains(wkniQ));
        boolean wkniOkC = "Todas".equals(wkniCatFilter.getValue()) || wkniCatFilter.getValue().equals(wkniE.wkniGetCategoria());
        return wkniOkQ && wkniOkC;
    }

    private List<wkniEvento> wkniDelDia(LocalDate wkniD) {
        return wkniC.wkniEventosDelDia(wkniD).stream().filter(this::wkniPasa).toList();
    }

    private static final java.util.Map<String, String> wkniCAT_BG = java.util.Map.of(
        "Personal", "-c-violet-soft", "Trabajo", "-c-blue-soft", "Estudio", "-c-teal-soft",
        "Salud", "-c-green-soft", "Proyecto", "-c-amber-soft");
    private static final java.util.Map<String, String> wkniCAT_FG = java.util.Map.of(
        "Personal", "-c-violet", "Trabajo", "-c-blue", "Estudio", "-c-teal",
        "Salud", "-c-green", "Proyecto", "-c-amber");

    private Button wkniChip(wkniEvento wkniE) {
        Button wkniB = new Button(wkniE.wkniGetHoraInicio() + "  " + wkniE.wkniGetTitulo());
        wkniB.setMaxWidth(Double.MAX_VALUE);
        wkniB.setAlignment(Pos.CENTER_LEFT);
        wkniB.setTooltip(new Tooltip(wkniE.wkniGetTitulo() + "\n" + wkniE.wkniGetHoraInicio() + "-" + wkniE.wkniGetHoraFin()
            + " • " + wkniE.wkniGetCategoria()));
        wkniB.setStyle("-fx-background-color:" + wkniCAT_BG.getOrDefault(wkniE.wkniGetCategoria(), "-surface2")
            + ";-fx-text-fill:" + wkniCAT_FG.getOrDefault(wkniE.wkniGetCategoria(), "-text")
            + ";-fx-background-radius:7;-fx-font-size:12;-fx-cursor:hand;");
        wkniB.setOnAction(wkniEv -> wkniEventViews.wkniForm(wkniStage, wkniE, wkniC, () -> {}));
        return wkniB;
    }

    /** Chip legible sobre celdas cian seleccionadas. */
    private Button wkniChipOnAccent(wkniEvento wkniE) {
        Button wkniB = wkniChip(wkniE);
        wkniB.setStyle("-fx-background-color:-accent;-fx-text-fill:-on-accent;"
            + "-fx-background-radius:7;-fx-font-size:12;-fx-cursor:hand;");
        return wkniB;
    }

    private void wkniPick(LocalDate wkniD) {
        wkniSelected = wkniD;
        wkniCursor = wkniD;
        wkniRefreshAll();
    }

    // ---------- vista AÑO ----------
    private javafx.scene.Node wkniAnoView() {
        int wkniYear = wkniCursor.getYear();
        wkniRangeLabel.setText("Año " + wkniYear);
        GridPane wkniG = new GridPane();
        wkniG.setHgap(10);
        wkniG.setVgap(10);
        for (int wkniI = 0; wkniI < 3; wkniI++) {
            ColumnConstraints wkniCc = new ColumnConstraints();
            wkniCc.setPercentWidth(100.0 / 3);
            wkniCc.setHgrow(Priority.ALWAYS);
            wkniG.getColumnConstraints().add(wkniCc);
        }
        LocalDate wkniToday = LocalDate.now();
        for (int wkniM = 1; wkniM <= 12; wkniM++) {
            YearMonth wkniYm = YearMonth.of(wkniYear, wkniM);
            VBox wkniBox = new VBox(4);
            wkniBox.setPadding(new Insets(10));
            wkniBox.setStyle("-fx-background-color:-surface;-fx-background-radius:12;"
                + "-fx-border-color:-bord;-fx-border-radius:12;");
            Button wkniTitle = new Button(wkniCap(wkniYm.getMonth().getDisplayName(TextStyle.FULL, wkniEs)));
            wkniTitle.setMaxWidth(Double.MAX_VALUE);
            wkniTitle.setStyle("-fx-background-color:transparent;-fx-text-fill:-text;"
                + "-fx-font-weight:bold;-fx-font-size:13;-fx-cursor:hand;");
            final YearMonth wkniFym = wkniYm;
            wkniTitle.setOnAction(wkniE -> { wkniCursor = wkniFym.atDay(1); wkni_vista = wkniVista.MES; wkniSyncSeg(); wkniRefreshAll(); });
            wkniBox.getChildren().add(wkniTitle);
            GridPane wkniDays = new GridPane();
            wkniDays.setHgap(2);
            wkniDays.setVgap(2);
            for (int wkniI = 0; wkniI < 7; wkniI++) {
                ColumnConstraints wkniCc = new ColumnConstraints();
                wkniCc.setPercentWidth(100.0 / 7);
                wkniCc.setHgrow(Priority.ALWAYS);
                wkniDays.getColumnConstraints().add(wkniCc);
            }
            int wkniOff = wkniYm.atDay(1).getDayOfWeek().getValue() - 1;
            for (int wkniDay = 1; wkniDay <= wkniYm.lengthOfMonth(); wkniDay++) {
                LocalDate wkni_fecha = wkniYm.atDay(wkniDay);
                boolean wkniHas = !wkniDelDia(wkni_fecha).isEmpty();
                Button wkniDb = new Button(String.valueOf(wkniDay));
                wkniDb.setMaxWidth(Double.MAX_VALUE);
                String wkniSt = "-fx-background-radius:5;-fx-font-size:12;-fx-cursor:hand;-fx-padding:3 0;";
                if (wkni_fecha.equals(wkniSelected)) wkniSt += "-fx-background-color:-accent;-fx-text-fill:-on-accent;-fx-font-weight:bold;";
                else if (wkni_fecha.equals(wkniToday)) wkniSt += "-fx-background-color:-accent-soft;-fx-text-fill:-accent;-fx-font-weight:bold;";
                else if (wkniHas) wkniSt += "-fx-background-color:-accent-soft;-fx-text-fill:-accent;";
                else wkniSt += "-fx-background-color:transparent;-fx-text-fill:-text;";
                wkniDb.setStyle(wkniSt);
                wkniDb.setOnAction(wkniE -> { wkniPick(wkni_fecha); wkni_vista = wkniVista.MES; wkniSyncSeg(); wkniRefreshAll(); });
                int wkniK = wkniOff + wkniDay - 1;
                wkniDays.add(wkniDb, wkniK % 7, wkniK / 7);
            }
            wkniBox.getChildren().add(wkniDays);
            wkniG.add(wkniBox, (wkniM - 1) % 3, (wkniM - 1) / 3);
        }
        ScrollPane wkniSp = new ScrollPane(wkniG);
        wkniSp.setFitToWidth(true);
        wkniSp.setFitToHeight(true);
        wkniSp.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        VBox.setVgrow(wkniSp, Priority.ALWAYS);
        return wkniSp;
    }

    private void wkniSyncSeg() {
        for (var wkniTg : wkniGroup.getToggles()) {
            ToggleButton wkniTb = (ToggleButton) wkniTg;
            boolean wkniOn = (wkni_vista == wkniVista.ANO && "Año".equals(wkniTb.getText()))
                || (wkni_vista == wkniVista.MES && "Mes".equals(wkniTb.getText()))
                || (wkni_vista == wkniVista.SEMANA && "Semana".equals(wkniTb.getText()))
                || (wkni_vista == wkniVista.AGENDA && "Agenda".equals(wkniTb.getText()));
            wkniTb.setSelected(wkniOn);
        }
    }

    // ---------- vista MES ----------
    private javafx.scene.Node wkniMesView() {
        YearMonth wkniYm = YearMonth.from(wkniCursor);
        wkniRangeLabel.setText(wkniCap(wkniYm.getMonth().getDisplayName(TextStyle.FULL, wkniEs)) + " de " + wkniYm.getYear());
        GridPane wkniG = new GridPane();
        wkniG.setHgap(4);
        wkniG.setVgap(4);
        for (int wkniI = 0; wkniI < 7; wkniI++) {
            ColumnConstraints wkniCc = new ColumnConstraints();
            wkniCc.setPercentWidth(100.0 / 7);
            wkniCc.setHgrow(Priority.ALWAYS);
            wkniG.getColumnConstraints().add(wkniCc);
        }
        String[] wkniDs = {"LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM"};
        for (int wkniI = 0; wkniI < 7; wkniI++) {
            Label wkniZ = new Label(wkniDs[wkniI]);
            wkniZ.setMaxWidth(Double.MAX_VALUE);
            wkniZ.setAlignment(Pos.CENTER);
            wkniZ.setStyle("-fx-text-fill:-muted;-fx-font-size:12;-fx-font-weight:bold;");
            wkniG.add(wkniZ, wkniI, 0);
        }
        int wkniOff = wkniYm.atDay(1).getDayOfWeek().getValue() - 1;
        int wkniRows = (wkniOff + wkniYm.lengthOfMonth() + 6) / 7;
        for (int wkniR = 1; wkniR <= wkniRows; wkniR++) {
            RowConstraints wkniRc = new RowConstraints();
            wkniRc.setVgrow(Priority.ALWAYS);
            wkniG.getRowConstraints().add(wkniRc);
        }
        RowConstraints wkniRc0 = new RowConstraints();
        wkniG.getRowConstraints().add(0, wkniRc0);
        LocalDate wkniToday = LocalDate.now();
        for (int wkniDay = 1; wkniDay <= wkniYm.lengthOfMonth(); wkniDay++) {
            LocalDate wkni_fecha = wkniYm.atDay(wkniDay);
            var wkniEvs = wkniDelDia(wkni_fecha);
            VBox wkniCell = new VBox(3);
            wkniCell.setPadding(new Insets(6));
            wkniCell.setMaxWidth(Double.MAX_VALUE);
            wkniCell.setMaxHeight(Double.MAX_VALUE);
            boolean wkniSel = wkni_fecha.equals(wkniSelected);
            String wkniBase = "-fx-background-radius:10;-fx-border-radius:10;-fx-cursor:hand;";
            if (wkniSel) wkniBase += "-fx-background-color:-accent-soft;-fx-border-color:-accent;-fx-border-width:2;";
            else if (wkni_fecha.equals(wkniToday)) wkniBase += "-fx-background-color:-surface;-fx-border-color:-accent;-fx-border-width:2;";
            else wkniBase += "-fx-background-color:-surface;-fx-border-color:-bord;-fx-border-width:1;";
            wkniCell.setStyle(wkniBase);
            Label wkniNum = new Label(String.valueOf(wkniDay));
            wkniFontPreferences.wkniBind(wkniNum, FontWeight.BOLD, 13);
            wkniNum.setStyle(wkniSel ? "-fx-text-fill:-accent;-fx-font-weight:bold;" : "-fx-text-fill:-text;");
            wkniCell.getChildren().add(wkniNum);
            for (wkniEvento wkniE : wkniEvs.stream().limit(3).toList()) wkniCell.getChildren().add(wkniSel ? wkniChipOnAccent(wkniE) : wkniChip(wkniE));
            if (wkniEvs.size() > 3) {
                Button wkniMore = wkniUi.wkniActionButton("add", (wkniEvs.size() - 3) + " más");
                wkniMore.setStyle("-fx-background-color:transparent;-fx-font-size:12;-fx-cursor:hand;"
                    + "-fx-text-fill:" + "-accent" + ";");
                wkniMore.setOnAction(wkniE -> wkniPick(wkni_fecha));
                wkniCell.getChildren().add(wkniMore);
            }
            wkniCell.setOnMouseClicked(wkniE -> wkniPick(wkni_fecha));
            int wkniK = wkniOff + wkniDay - 1;
            wkniG.add(wkniCell, wkniK % 7, wkniK / 7 + 1);
        }
        VBox.setVgrow(wkniG, Priority.ALWAYS);
        return wkniG;
    }

    // ---------- vista SEMANA ----------
    private javafx.scene.Node wkniSemanaView() {
        LocalDate wkniLun = wkniCursor.with(DayOfWeek.MONDAY);
        LocalDate wkniDom = wkniLun.plusDays(6);
        wkniRangeLabel.setText(wkniLun.getDayOfMonth() + "/" + wkniLun.getMonthValue() + " – "
            + wkniDom.getDayOfMonth() + "/" + wkniDom.getMonthValue() + " de " + wkniDom.getYear());
        HBox wkniRow = new HBox(6);
        LocalDate wkniToday = LocalDate.now();
        for (int wkniI = 0; wkniI < 7; wkniI++) {
            LocalDate wkniD = wkniLun.plusDays(wkniI);
            var wkniEvs = wkniDelDia(wkniD);
            VBox wkniCol = new VBox(5);
            wkniCol.setPadding(new Insets(10));
            wkniCol.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(wkniCol, Priority.ALWAYS);
            boolean wkniSel = wkniD.equals(wkniSelected);
            wkniCol.setStyle("-fx-background-color:" + (wkniSel ? "-accent-soft" : "-surface") + ";-fx-background-radius:12;"
                + "-fx-border-color:" + (wkniD.equals(wkniToday) || wkniSel ? "-accent" : "-bord") + ";-fx-border-radius:12;"
                + "-fx-border-width:" + (wkniD.equals(wkniToday) || wkniSel ? "2" : "1") + ";-fx-cursor:hand;");
            Label wkniH = new Label(wkniD.getDayOfWeek().getDisplayName(TextStyle.SHORT, wkniEs).toUpperCase()
                + " " + wkniD.getDayOfMonth());
            wkniFontPreferences.wkniBind(wkniH, FontWeight.BOLD, 13);
            wkniH.setStyle(wkniSel ? "-fx-text-fill:-accent;-fx-font-weight:bold;" : "-fx-text-fill:-text;-fx-font-weight:bold;");
            wkniCol.getChildren().add(wkniH);
            if (wkniEvs.isEmpty()) {
                Label wkniV = new Label("—");
                wkniV.setStyle("-fx-text-fill:-muted;");
                wkniCol.getChildren().add(wkniV);
            }
            for (wkniEvento wkniE : wkniEvs.stream().limit(5).toList()) wkniCol.getChildren().add(wkniSel ? wkniChipOnAccent(wkniE) : wkniChip(wkniE));
            if (wkniEvs.size() > 5) {
                Label wkniM = new Label("+" + (wkniEvs.size() - 5) + " más");
                wkniM.setStyle("-fx-text-fill:-accent;");
                wkniCol.getChildren().add(wkniM);
            }
            Button wkniVer = new Button("Ver día");
            wkniVer.setMaxWidth(Double.MAX_VALUE);
            wkniVer.setStyle("-fx-background-color:transparent;-fx-cursor:hand;"
                + "-fx-text-fill:" + (wkniSel ? "-accent" : "-text") + ";");
            final LocalDate wkniFd = wkniD;
            wkniVer.setOnAction(wkniE -> { wkniPick(wkniFd); wkni_vista = wkniVista.MES; wkniSyncSeg(); wkniRefreshAll(); });
            wkniCol.getChildren().add(wkniVer);
            final LocalDate wkniCd = wkniD;
            wkniCol.setOnMouseClicked(wkniE -> { wkniSelected = wkniCd; wkniRefreshAll(); });
            wkniRow.getChildren().add(wkniCol);
        }
        ScrollPane wkniSp = new ScrollPane(wkniRow);
        wkniSp.setFitToWidth(true);
        wkniSp.setFitToHeight(true);
        wkniSp.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        VBox.setVgrow(wkniSp, Priority.ALWAYS);
        return wkniSp;
    }

    // ---------- vista AGENDA ----------
    @SuppressWarnings("unchecked")
    private javafx.scene.Node wkniAgendaView() {
        wkniRangeLabel.setText("Todos los eventos");
        VBox wkniBox = new VBox(10);
        TableView<wkniEvento> wkniT = new TableView<>();
        wkniT.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<wkniEvento, String> wkniC1 = new TableColumn<>("Fecha");
        wkniC1.setCellValueFactory(wkniCd -> new SimpleStringProperty("" + wkniCd.getValue().wkniGetFecha()));
        TableColumn<wkniEvento, String> wkniC2 = new TableColumn<>("Hora");
        wkniC2.setCellValueFactory(wkniCd -> new SimpleStringProperty(
            wkniCd.getValue().wkniGetHoraInicio() + "-" + wkniCd.getValue().wkniGetHoraFin()));
        TableColumn<wkniEvento, String> wkniC3 = new TableColumn<>("Evento");
        wkniC3.setCellValueFactory(wkniCd -> new SimpleStringProperty(wkniCd.getValue().wkniGetTitulo()));
        TableColumn<wkniEvento, String> wkniC4 = new TableColumn<>("Categoría");
        wkniC4.setCellValueFactory(wkniCd -> new SimpleStringProperty(wkniCd.getValue().wkniGetCategoria()));
        TableColumn<wkniEvento, String> wkniC5 = new TableColumn<>("Prioridad");
        wkniC5.setCellValueFactory(wkniCd -> new SimpleStringProperty(wkniCd.getValue().wkniGetPrioridad()));
        wkniT.getColumns().addAll(wkniC1, wkniC2, wkniC3, wkniC4, wkniC5);
        wkniT.setRowFactory(wkniTv -> {
            javafx.scene.control.TableRow<wkniEvento> wkniR = new javafx.scene.control.TableRow<>();
            wkniR.setOnMouseClicked(wkniE -> {
                if (wkniE.getClickCount() == 2 && wkniR.getItem() != null)
                    wkniEventViews.wkniForm(wkniStage, wkniR.getItem(), wkniC, () -> {});
            });
            return wkniR;
        });
        Runnable wkniFill = () -> {
            var wkniAll = new ArrayList<>(wkniC.wkniGetEventos().stream().filter(this::wkniPasa).toList());
            wkniT.getItems().setAll(wkniAll);
        };
        wkniFill.run();
        wkniBox.getChildren().addAll(wkniHint("Doble clic en una fila para editar. (" + wkniT.getItems().size() + " eventos)"), wkniT);
        VBox.setVgrow(wkniT, Priority.ALWAYS);
        return wkniBox;
    }

    private Label wkniHint(String wkniT) {
        Label wkniL = new Label(wkniT);
        wkniL.setStyle("-fx-text-fill:-muted;");
        wkniL.setWrapText(true);
        return wkniL;
    }

    private String wkniCap(String wkniS) {
        if (wkniS == null || wkniS.isEmpty()) return wkniS;
        return wkniS.substring(0, 1).toUpperCase(wkniEs) + wkniS.substring(1);
    }
}


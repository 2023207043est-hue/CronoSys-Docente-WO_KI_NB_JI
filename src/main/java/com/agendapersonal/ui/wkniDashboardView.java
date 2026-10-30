package com.agendapersonal.ui;

import com.agendapersonal.ui.wkniFontPreferences;

import com.agendapersonal.controller.wkniMainController;
import com.agendapersonal.domain.model.wkniActividadClase;
import com.agendapersonal.domain.model.wkniClaseDocente;
import com.agendapersonal.model.wkniEvento;
import com.agendapersonal.model.wkniTarea;
import com.agendapersonal.infrastructure.repo.wkniClaseDocenteDAO;
import com.agendapersonal.util.wkniUi;
import com.agendapersonal.ui.wkniIconFactory;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

public final class wkniDashboardView {
    private static final String[] wkniQUOTES = {
        "Pequeños avances diarios crean grandes resultados.",
        "Enfócate en lo importante, no solo en lo urgente.",
        "Tu tiempo es tu mejor inversión.",
        "Hoy es un buen día para avanzar tu proyecto.",
        "La constancia le gana al talento cuando el talento descansa.",
        "Bloquea tu enfoque y el resto fluye.",
        "Hecho es mejor que perfecto."
    };

    private final wkniMainController wkniC;
    private final Stage wkniStage;
    private final Runnable wkniOnPrepararClase;
    private final ScrollPane wkniView;
    private TableView<wkniEvento> wkniAgendaTable;
    private VBox wkniTaskBox;
    private Label wkniEventCount;
    private Label wkniTaskCount;
    private Label wkniEventosHoyValue;
    private Label wkniEventosTotalesValue;
    private Label wkniPendientesValue;
    private Label wkniCompletadasValue;
    private Label wkniFocusTitle;
    private Label wkniFocusDetail;
    private Label wkniClockLabel;
    private Timeline wkniClockTimer;
    private wkniInteractiveCalendar wkniCalendar;
    private LocalDate wkniFechaSeleccionada = LocalDate.now();
    private Label wkniAgendaTitulo;

    public wkniDashboardView(wkniMainController wkniC, Stage wkniStage, Runnable wkniOnNewEvent,
                         Consumer<String> wkniOnSearchEvents, Runnable wkniOnNewTask, Runnable wkniOnPrepararClase) {
        this.wkniC = wkniC;
        this.wkniStage = wkniStage;
        this.wkniOnPrepararClase = wkniOnPrepararClase == null ? () -> {} : wkniOnPrepararClase;
        VBox wkniX = new VBox(16);
        wkniX.setPadding(new Insets(28, 36, 32, 36));
        wkniX.getStyleClass().add("page");
        wkniX.setFillWidth(true);

        wkniX.getChildren().add(wkniHero(wkniOnNewEvent));
        wkniX.getChildren().add(wkniSearchRow(wkniOnSearchEvents));

        HBox wkniSummary = new HBox(12);
        wkniEventosHoyValue = new Label("0");
        wkniEventosTotalesValue = new Label("0");
        wkniPendientesValue = new Label("0");
        wkniCompletadasValue = new Label("0");
        wkniSummary.getChildren().addAll(
            wkniStatCard("Eventos hoy", wkniEventosHoyValue, "teal", "event"),
            wkniStatCard("Eventos totales", wkniEventosTotalesValue, "violet", "agenda"),
            wkniStatCard("Tareas pendientes", wkniPendientesValue, "amber", "tasks"),
            wkniStatCard("Tareas completadas", wkniCompletadasValue, "green", "check"));
        wkniX.getChildren().add(wkniSummary);

        HBox wkniCols = new HBox(14);
        wkniCalendar = new wkniInteractiveCalendar(wkniC, wkni_fecha -> {
            wkniFechaSeleccionada = wkni_fecha;
            Platform.runLater(this::wkniRefreshAgenda);
        });
        VBox wkniRight = new VBox(14, wkniAgendaCard(), wkniTasksCard(wkniOnNewTask));
        HBox.setHgrow(wkniCalendar.wkniGetView(), Priority.ALWAYS);
        HBox.setHgrow(wkniRight, Priority.ALWAYS);
        wkniCols.getChildren().addAll(wkniCalendar.wkniGetView(), wkniRight);
        wkniX.getChildren().add(wkniCols);

        this.wkniView = new ScrollPane(wkniX);
        this.wkniView.setFitToWidth(true);
        this.wkniView.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.wkniView.getStyleClass().add("page-scroll");
        wkniC.wkniGetEventos().addListener((Observable wkniO) -> Platform.runLater(this::wkniRefresh));
        wkniC.wkniGetTareas().addListener((Observable wkniO) -> Platform.runLater(this::wkniRefresh));
        wkniRefresh();
    }

    // ---------- héroe ----------
    private VBox wkniHero(Runnable wkniOnNewEvent) {
        VBox wkniHero = new VBox(8);
        wkniHero.setPadding(new Insets(26, 30, 26, 30));
        wkniHero.getStyleClass().add("dashboard-hero");
        Label wkniHi = new Label(com.agendapersonal.util.wkniPerfil.wkniSaludo(LocalTime.now().getHour(), com.agendapersonal.util.wkniPerfil.wkniNombre()));
        com.agendapersonal.util.wkniPerfil.wkniRevisionProperty().addListener((wkniO, wkniA, wkniB) ->
            wkniHi.setText(com.agendapersonal.util.wkniPerfil.wkniSaludo(LocalTime.now().getHour(), com.agendapersonal.util.wkniPerfil.wkniNombre())));
        wkniFontPreferences.wkniBind(wkniHi, FontWeight.BOLD, 30);
        wkniHi.setStyle("-fx-text-fill:-text;");
        Label wkniQuote = new Label(wkniQUOTES[LocalDate.now().getDayOfYear() % wkniQUOTES.length]);
        wkniQuote.setWrapText(true);
        wkniQuote.setStyle("-fx-text-fill:-muted;-fx-font-size:14;");

        wkniClockLabel = new Label();
        wkniClockLabel.setStyle("-fx-text-fill:-text;-fx-padding:0;");
        Runnable wkniUpdateClock = () -> wkniClockLabel.setText(wkniRelojPrefs.wkniFormatearAhora());
        Runnable wkniAplicarPrefs = () -> {
            wkniFontPreferences.wkniBind(wkniClockLabel, FontWeight.BOLD, wkniRelojPrefs.wkniTamano().wkniPx());
            wkniClockLabel.setVisible(wkniRelojPrefs.wkniVisible());
            wkniClockLabel.setManaged(wkniRelojPrefs.wkniVisible());
            wkniUpdateClock.run();
        };
        wkniAplicarPrefs.run();
        wkniRelojPrefs.wkniRevisionProperty().addListener((wkniO, wkniA, wkniB) -> wkniAplicarPrefs.run());
        wkniClockTimer = new Timeline(new KeyFrame(Duration.seconds(1), wkniE -> wkniUpdateClock.run()));
        wkniClockTimer.setCycleCount(Timeline.INDEFINITE);
        wkniClockTimer.play();

        Label wkniDateText = new Label(wkniUi.wkniFormatDate(LocalDate.now()));
        wkniDateText.setStyle("-fx-text-fill:-muted;-fx-font-size:14;");
        HBox wkniDate = new HBox(8, wkniIconFactory.wkniIcon("calendar", 18, "tint-muted"), wkniDateText);
        wkniDate.setAlignment(Pos.CENTER_LEFT);

        Button wkniNuevo = wkniUi.wkniActionButton("add", "Nuevo evento");
        wkniUi.wkniStylePrimary(wkniNuevo);
        wkniNuevo.setOnAction(wkniE -> wkniOnNewEvent.run());
        HBox wkniBtns = new HBox(10, wkniNuevo);
        if (com.agendapersonal.util.wkniPerfil.wkniEsDocente()) {
            Button wkniPreparar = wkniUi.wkniActionButton("play", "Preparar clase");
            wkniPreparar.setOnAction(wkniE -> wkniOnPrepararClase.run());
            wkniBtns.getChildren().add(wkniPreparar);
        }

        VBox wkniLeft = new VBox(3, wkniHi, wkniQuote, wkniClockLabel, wkniDate, wkniBtns);

        // Tarjeta "enfoque ahora" sobre el héroe
        VBox wkniFocus = new VBox(4);
        wkniFocus.setPadding(new Insets(16));
        wkniFocus.getStyleClass().add("dashboard-focus");
        wkniFocus.setPrefWidth(290);
        wkniFocus.setMinHeight(170);
        Label wkniFh = new Label("ENFOQUE AHORA");
        wkniFh.setStyle("-fx-text-fill:-accent;-fx-font-size:12;-fx-font-weight:bold;");
        HBox wkniFocusHeader = new HBox(8, wkniIconFactory.wkniIcon("focus", 18, "tint-accent"), wkniFh);
        wkniFocusHeader.setAlignment(Pos.CENTER_LEFT);
        wkniFocusTitle = new Label("—");
        wkniFontPreferences.wkniBind(wkniFocusTitle, FontWeight.BOLD, 16);
        wkniFocusTitle.setStyle("-fx-text-fill:-text;");
        wkniFocusTitle.setWrapText(true);
        wkniFocusDetail = new Label("");
        wkniFocusDetail.setStyle("-fx-text-fill:-muted;-fx-font-size:12;");
        wkniFocusDetail.setWrapText(true);
        wkniFocus.getChildren().addAll(wkniFocusHeader, wkniFocusTitle, wkniFocusDetail);

        HBox wkniRow = new HBox(16, wkniLeft, wkniSpacer(), wkniFocus);
        wkniRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(wkniLeft, Priority.ALWAYS);
        wkniHero.getChildren().add(wkniRow);
        return wkniHero;
    }

    private HBox wkniSearchRow(Consumer<String> wkniOnSearchEvents) {
        TextField wkniQuickSearch = new TextField();
        wkniQuickSearch.setPromptText("Buscar eventos por título o descripción...");
        HBox.setHgrow(wkniQuickSearch, Priority.ALWAYS);
        Button wkniSearchBtn = new Button("Buscar");
        wkniUi.wkniStylePrimary(wkniSearchBtn);
        Runnable wkniGo = () -> wkniOnSearchEvents.accept(wkniQuickSearch.getText().trim());
        wkniSearchBtn.setOnAction(wkniE -> wkniGo.run());
        wkniQuickSearch.setOnAction(wkniE -> wkniGo.run());
        HBox wkniH = new HBox(8, wkniQuickSearch, wkniSearchBtn);
        wkniH.setAlignment(Pos.CENTER_LEFT);
        return wkniH;
    }

    private VBox wkniStatCard(String wkniLabel, Label wkniValueLabel, String wkni_tone, String wkniIcon) {
        Label wkniL = new Label(wkniLabel);
        wkniL.getStyleClass().add("stat-label");
        HBox wkniTop = new HBox(8, wkniL, wkniSpacer(), wkniUi.wkniIconChip(wkniIcon, wkni_tone, 34, 18));
        wkniTop.setAlignment(Pos.CENTER_LEFT);
        wkniValueLabel.getStyleClass().add("stat-value");
        VBox wkniB = new VBox(10, wkniTop, wkniValueLabel);
        wkniB.getStyleClass().addAll("card", "stat-card");
        wkniB.setPadding(new Insets(18, 20, 18, 20));
        HBox.setHgrow(wkniB, Priority.ALWAYS);
        wkniB.setMaxWidth(Double.MAX_VALUE);
        return wkniB;
    }

    private Region wkniSpacer() {
        Region wkniR = new Region();
        HBox.setHgrow(wkniR, Priority.ALWAYS);
        return wkniR;
    }

    // ---------- tarjetas agenda / tareas ----------
    private VBox wkniAgendaCard() {
        VBox wkniB = wkniUi.wkniCard();
        HBox wkniH = new HBox(8);
        wkniH.setAlignment(Pos.CENTER_LEFT);
        wkniAgendaTitulo = new Label("Agenda de hoy");
        wkniFontPreferences.wkniBind(wkniAgendaTitulo, FontWeight.BOLD, 17);
        wkniAgendaTitulo.setStyle("-fx-text-fill:-text;");
        wkniEventCount = new Label();
        wkniEventCount.setStyle("-fx-text-fill:-muted;");
        Region wkniSp = wkniSpacer();
        Button wkniHoyBtn = new Button("Hoy");
        wkniHoyBtn.setOnAction(wkniE -> {
            wkniFechaSeleccionada = LocalDate.now();
            wkniCalendar.wkniSetSelected(wkniFechaSeleccionada);
            wkniRefreshAgenda();
        });
        wkniH.getChildren().addAll(wkniAgendaTitulo, wkniEventCount, wkniSp, wkniHoyBtn);
        wkniAgendaTable = wkniBuildAgendaTable();
        wkniB.getChildren().addAll(wkniH, wkniAgendaTable);
        return wkniB;
    }

    private VBox wkniTasksCard(Runnable wkniOnNewTask) {
        VBox wkniB = wkniUi.wkniCard();
        HBox wkniH = new HBox(8);
        wkniH.setAlignment(Pos.CENTER_LEFT);
        Label wkniTt = new Label("Tareas");
        wkniFontPreferences.wkniBind(wkniTt, FontWeight.BOLD, 17);
        wkniTt.setStyle("-fx-text-fill:-text;");
        wkniTaskCount = new Label();
        wkniTaskCount.setStyle("-fx-text-fill:-muted;");
        Region wkniSp = wkniSpacer();
        Button wkniAddTask = wkniUi.wkniActionButton("add", "Nueva");
        wkniUi.wkniStylePrimary(wkniAddTask);
        wkniAddTask.setOnAction(wkniE -> wkniOnNewTask.run());
        wkniH.getChildren().addAll(wkniTt, wkniTaskCount, wkniSp, wkniAddTask);
        wkniTaskBox = new VBox(6);
        wkniB.getChildren().addAll(wkniH, new Separator(), wkniTaskBox);
        return wkniB;
    }

    public ScrollPane wkniGetView() { return wkniView; }

    public void wkniRefresh() {
        if (wkniAgendaTable == null) return;
        if (wkniCalendar != null) wkniCalendar.wkniRefreshMarks();
        wkniRefreshAgenda();
        wkniRefreshSummary();
        wkniRefreshFocus();
    }

    private void wkniRefreshAgenda() {
        LocalDate wkniHoy = LocalDate.now();
        LocalDate wkniSel = wkniFechaSeleccionada != null ? wkniFechaSeleccionada : wkniHoy;
        boolean wkniEsHoy = wkniSel.equals(wkniHoy);
        if (wkniAgendaTitulo != null)
            wkniAgendaTitulo.setText(wkniEsHoy ? "Agenda de hoy"
                : "Agenda: " + wkniSel.getDayOfMonth() + "/" + wkniSel.getMonthValue() + "/" + wkniSel.getYear());
        if (wkniEsHoy) {
            List<wkniEvento> wkniProximos = wkniC.wkniGetEventos().stream()
                .filter(wkniE -> wkniE.wkniGetFecha() != null && !wkniE.wkniGetFecha().isBefore(wkniHoy))
                .limit(8).toList();
            wkniAgendaTable.getItems().setAll(wkniProximos);
            if (wkniEventCount != null) wkniEventCount.setText("(" + wkniProximos.size() + ")");
        } else {
            List<wkniEvento> wkniDelDia = wkniC.wkniEventosDelDia(wkniSel);
            wkniAgendaTable.getItems().setAll(wkniDelDia);
            if (wkniEventCount != null) wkniEventCount.setText("(" + wkniDelDia.size() + ")");
        }

        wkniTaskBox.getChildren().clear();
        List<wkniTarea> wkniTs = wkniC.wkniGetTareas();
        long wkniDone = wkniTs.stream().filter(wkniT -> "Completada".equals(wkniT.wkniGetEstado())).count();
        if (wkniTs.isEmpty()) {
            Label wkniL = new Label("Sin tareas. Crea la primera con + Nueva.");
            wkniL.setStyle("-fx-text-fill:-muted;");
            wkniTaskBox.getChildren().add(wkniL);
        } else for (wkniTarea wkniT : wkniTs.stream().limit(4).toList()) wkniTaskBox.getChildren().add(wkniTaskRow(wkniT));
        if (wkniTaskCount != null) wkniTaskCount.setText("(" + wkniDone + "/" + wkniTs.size() + ")");
        wkniRefreshSummary();
    }

    private void wkniRefreshSummary() {
        List<wkniEvento> wkniEs = wkniC.wkniEventosDelDia(LocalDate.now());
        List<wkniTarea> wkniTs = wkniC.wkniGetTareas();
        long wkniDone = wkniTs.stream().filter(wkniT -> "Completada".equals(wkniT.wkniGetEstado())).count();
        if (wkniEventosHoyValue != null) wkniEventosHoyValue.setText(String.valueOf(wkniEs.size()));
        if (wkniEventosTotalesValue != null) wkniEventosTotalesValue.setText(String.valueOf(wkniC.wkniGetEventos().size()));
        if (wkniPendientesValue != null) wkniPendientesValue.setText(String.valueOf(wkniTs.size() - wkniDone));
        if (wkniCompletadasValue != null) wkniCompletadasValue.setText(String.valueOf(wkniDone));
    }

    private void wkniRefreshFocus() {
        if (com.agendapersonal.util.wkniPerfil.wkniEsDocente()) { wkniRefrescarEnfoqueDocente(); return; }
        LocalTime wkniNow = LocalTime.now();
        wkniEvento wkniNext = null;
        long wkniBestMin = Long.MAX_VALUE;
        for (wkniEvento wkniE : wkniC.wkniEventosDelDia(LocalDate.now())) {
            try {
                long wkniM = java.time.Duration.between(wkniNow, LocalTime.parse(wkniE.wkniGetHoraInicio())).toMinutes();
                if (wkniM >= -30 && wkniM < wkniBestMin) { wkniBestMin = wkniM; wkniNext = wkniE; }
            } catch (Exception wkniIgnored) {}
        }
        if (wkniNext == null) {
            var wkniUpcoming = wkniC.wkniGetEventos().stream()
                .filter(wkniE -> wkniE.wkniGetFecha() != null && wkniE.wkniGetFecha().isAfter(LocalDate.now()))
                .sorted((wkniA, wkniB2) -> wkniA.wkniGetFecha().compareTo(wkniB2.wkniGetFecha())).findFirst();
            if (wkniUpcoming.isPresent()) {
                wkniNext = wkniUpcoming.get();
                wkniBestMin = -1;
            }
        }
        if (wkniFocusTitle == null) return;
        if (wkniNext == null) {
            wkniFocusTitle.setText("Día despejado");
            wkniFocusDetail.setText("Sin eventos próximos. Perfecto para avanzar proyectos.");
        } else if (wkniBestMin == -1) {
            wkniFocusTitle.setText(wkniNext.wkniGetTitulo());
            wkniFocusDetail.setText(wkniNext.wkniGetFecha() + " • " + wkniNext.wkniGetHoraInicio());
        } else if (wkniBestMin <= 0) {
            wkniFocusTitle.setText(wkniNext.wkniGetTitulo());
            wkniFocusDetail.setText("¡Ahora mismo! " + wkniNext.wkniGetHoraInicio() + "-" + wkniNext.wkniGetHoraFin());
        } else if (wkniBestMin < 60) {
            wkniFocusTitle.setText(wkniNext.wkniGetTitulo());
            wkniFocusDetail.setText("En " + wkniBestMin + " min • " + wkniNext.wkniGetHoraInicio());
        } else {
            wkniFocusTitle.setText(wkniNext.wkniGetTitulo());
            wkniFocusDetail.setText("En " + (wkniBestMin / 60) + " h " + (wkniBestMin % 60) + " min • " + wkniNext.wkniGetHoraInicio());
        }
    }

    @SuppressWarnings("unchecked")
    private TableView<wkniEvento> wkniBuildAgendaTable() {
        TableView<wkniEvento> wkniT = new TableView<>();
        wkniT.setPrefHeight(250);
        wkniT.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        wkniT.setPlaceholder(new Label("No hay eventos para mostrar."));

        TableColumn<wkniEvento, Void> wkniColNum = new TableColumn<>("#");
        wkniColNum.setPrefWidth(40);
        wkniColNum.setMaxWidth(50);
        wkniColNum.setSortable(false);
        wkniColNum.setCellFactory(wkniCol -> new TableCell<>() {
            @Override protected void updateItem(Void wkniV, boolean wkniEmpty) {
                super.updateItem(wkniV, wkniEmpty);
                setText(wkniEmpty ? null : String.valueOf(getIndex() + 1));
            }
        });

        TableColumn<wkniEvento, String> wkniColHora = new TableColumn<>("Hora");
        wkniColHora.setPrefWidth(130);
        wkniColHora.setCellValueFactory(wkniCd ->
            new SimpleStringProperty(wkniCd.getValue().wkniGetHoraInicio() + " - " + wkniCd.getValue().wkniGetHoraFin()));

        TableColumn<wkniEvento, String> wkniColNombre = new TableColumn<>("Evento");
        wkniColNombre.setPrefWidth(220);
        wkniColNombre.setCellValueFactory(wkniCd -> new SimpleStringProperty(wkniCd.getValue().wkniGetTitulo()));

        TableColumn<wkniEvento, Void> wkniColAcc = new TableColumn<>("Acciones");
        wkniColAcc.setPrefWidth(150);
        wkniColAcc.setSortable(false);
        wkniColAcc.setCellFactory(wkniCol -> new TableCell<>() {
            private final Button wkniEdit = wkniIconBtn("edit", "Editar");
            private final Button wkniReprogram = wkniIconBtn("reschedule", "Reprogramar");
            private final Button wkniDel = wkniIconBtn("delete", "Eliminar");
            private final HBox wkniBox = new HBox(6, wkniEdit, wkniReprogram, wkniDel);
            {
                wkniBox.setAlignment(Pos.CENTER_LEFT);
                wkniEdit.setOnAction(wkniE -> {
                    wkniEvento wkniEv = wkniRowAt(getTableView(), getIndex());
                    if (wkniEv != null) wkniEventViews.wkniForm(wkniStage, wkniEv, wkniC, () -> wkniRefresh());
                });
                wkniReprogram.setOnAction(wkniE -> {
                    wkniEvento wkniEv = wkniRowAt(getTableView(), getIndex());
                    if (wkniEv != null) wkniEventViews.wkniReprogramar(wkniStage, wkniEv, wkniC, () -> wkniRefresh());
                });
                wkniDel.setOnAction(wkniE -> {
                    wkniEvento wkniEv = wkniRowAt(getTableView(), getIndex());
                    if (wkniEv == null) return;
                    Alert wkniAl = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar \"" + wkniEv.wkniGetTitulo() + "\"?");
                    if (wkniAl.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK && wkniC.wkniEliminarEvento(wkniEv.wkniGetId()))
                        wkniNotifier.wkniDeleted("Evento eliminado", wkniEv.wkniGetTitulo());
                });
            }
            @Override protected void updateItem(Void wkniV, boolean wkniEmpty) {
                super.updateItem(wkniV, wkniEmpty);
                setGraphic(wkniEmpty ? null : wkniBox);
            }
        });

        wkniT.getColumns().addAll(wkniColNum, wkniColHora, wkniColNombre, wkniColAcc);
        return wkniT;
    }

    private Button wkniIconBtn(String wkniIcon, String wkniTip) {
        Button wkniB = new Button();
        wkniB.setGraphic(wkniIconFactory.wkniImage(wkniIcon, 16));
        wkniB.setTooltip(new Tooltip(wkniTip));
        wkniB.setAccessibleText(wkniTip);
        wkniB.getStyleClass().add("icon-action-button");
        return wkniB;
    }

    private static wkniEvento wkniRowAt(TableView<wkniEvento> wkniT, int wkniI) {
        if (wkniT == null || wkniI < 0 || wkniI >= wkniT.getItems().size()) return null;
        return wkniT.getItems().get(wkniI);
    }

    private HBox wkniTaskRow(wkniTarea wkniT) {
        CheckBox wkniCb = new CheckBox(wkniT.wkniGetTitulo());
        wkniCb.setSelected("Completada".equals(wkniT.wkniGetEstado()));
        wkniCb.setOnAction(wkniE -> {
            var wkniVentana = wkniCb.getScene() == null ? null : wkniCb.getScene().getWindow();
            var wkniRes = wkniC.wkniCambiarEstadoTarea(wkniT, wkniCb.isSelected()
                ? com.agendapersonal.model.wkniEstadoTarea.COMPLETADA : com.agendapersonal.model.wkniEstadoTarea.PENDIENTE);
            wkniRes.ifPresent(wkniR -> wkniToast.wkniMostrar(wkniVentana, wkniC.wkniMensajeProgreso(wkniR)));
            wkniRefresh();
        });
        HBox wkniR = new HBox(wkniCb);
        wkniR.setPadding(new Insets(6));
        wkniR.setStyle("-fx-background-color:-surface2;-fx-background-radius:8;");
        return wkniR;
    }

    /** Enfoque ahora para docentes: la próxima clase de su planeación. */
    private void wkniRefrescarEnfoqueDocente() {
        LocalDate wkniHoy = LocalDate.now();
        wkniActividadClase wkniProxima = null;
        wkniClaseDocente wkniClase = null;
        for (wkniClaseDocente wkniCl : new wkniClaseDocenteDAO().wkniListarClases()) {
            for (wkniActividadClase wkniA : new wkniClaseDocenteDAO().wkniListarActividades(wkniCl.wkniId())) {
                if (wkniA.wkni_fecha() == null || wkniA.wkni_fecha().isBefore(wkniHoy)) continue;
                if (wkniProxima == null || wkniA.wkni_fecha().isBefore(wkniProxima.wkni_fecha())) { wkniProxima = wkniA; wkniClase = wkniCl; }
            }
        }
        if (wkniFocusTitle == null) return;
        if (wkniProxima == null) {
            wkniFocusTitle.setText("Sin clases aún");
            wkniFocusDetail.setText("Crea tu primer curso y su planeación en el Espacio Docente.");
        } else {
            wkniFocusTitle.setText("Próxima clase: " + wkniProxima.wkniTitulo());
            wkniFocusDetail.setText((wkniClase == null ? "" : wkniClase.wkniNombre() + " · ") + wkniProxima.wkni_fecha());
        }
    }
}


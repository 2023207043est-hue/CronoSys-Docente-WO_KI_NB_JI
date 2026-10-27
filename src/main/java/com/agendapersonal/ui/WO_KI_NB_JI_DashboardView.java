package com.agendapersonal.ui;

import com.agendapersonal.ui.WO_KI_NB_JI_FontPreferences;

import com.agendapersonal.controller.WO_KI_NB_JI_MainController;
import com.agendapersonal.domain.model.WO_KI_NB_JI_ActividadClase;
import com.agendapersonal.domain.model.WO_KI_NB_JI_ClaseDocente;
import com.agendapersonal.model.WO_KI_NB_JI_Evento;
import com.agendapersonal.model.WO_KI_NB_JI_Tarea;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_ClaseDocenteDAO;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
import com.agendapersonal.ui.WO_KI_NB_JI_IconFactory;
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

public final class WO_KI_NB_JI_DashboardView {
    private static final String[] QUOTES = {
        "Pequeños avances diarios crean grandes resultados.",
        "Enfócate en lo importante, no solo en lo urgente.",
        "Tu tiempo es tu mejor inversión.",
        "Hoy es un buen día para avanzar tu proyecto.",
        "La constancia le gana al talento cuando el talento descansa.",
        "Bloquea tu enfoque y el resto fluye.",
        "Hecho es mejor que perfecto."
    };

    private final WO_KI_NB_JI_MainController c;
    private final Stage stage;
    private final Runnable onPrepararClase;
    private final ScrollPane view;
    private TableView<WO_KI_NB_JI_Evento> agendaTable;
    private VBox taskBox;
    private Label eventCount;
    private Label taskCount;
    private Label eventosHoyValue;
    private Label eventosTotalesValue;
    private Label pendientesValue;
    private Label completadasValue;
    private Label focusTitle;
    private Label focusDetail;
    private Label clockLabel;
    private Timeline clockTimer;
    private WO_KI_NB_JI_InteractiveCalendar calendar;
    private LocalDate fechaSeleccionada = LocalDate.now();
    private Label agendaTitulo;

    public WO_KI_NB_JI_DashboardView(WO_KI_NB_JI_MainController c, Stage stage, Runnable onNewEvent,
                         Consumer<String> onSearchEvents, Runnable onNewTask, Runnable onPrepararClase) {
        this.c = c;
        this.stage = stage;
        this.onPrepararClase = onPrepararClase == null ? () -> {} : onPrepararClase;
        VBox x = new VBox(16);
        x.setPadding(new Insets(28, 36, 32, 36));
        x.getStyleClass().add("page");
        x.setFillWidth(true);

        x.getChildren().add(WO_KI_NB_JI_hero(onNewEvent));
        x.getChildren().add(WO_KI_NB_JI_searchRow(onSearchEvents));

        HBox summary = new HBox(12);
        eventosHoyValue = new Label("0");
        eventosTotalesValue = new Label("0");
        pendientesValue = new Label("0");
        completadasValue = new Label("0");
        summary.getChildren().addAll(
            WO_KI_NB_JI_statCard("Eventos hoy", eventosHoyValue, "teal", "event"),
            WO_KI_NB_JI_statCard("Eventos totales", eventosTotalesValue, "violet", "agenda"),
            WO_KI_NB_JI_statCard("Tareas pendientes", pendientesValue, "amber", "tasks"),
            WO_KI_NB_JI_statCard("Tareas completadas", completadasValue, "green", "check"));
        x.getChildren().add(summary);

        HBox cols = new HBox(14);
        calendar = new WO_KI_NB_JI_InteractiveCalendar(c, fecha -> {
            fechaSeleccionada = fecha;
            Platform.runLater(this::WO_KI_NB_JI_refreshAgenda);
        });
        VBox right = new VBox(14, WO_KI_NB_JI_agendaCard(), WO_KI_NB_JI_tasksCard(onNewTask));
        HBox.setHgrow(calendar.WO_KI_NB_JI_getView(), Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        cols.getChildren().addAll(calendar.WO_KI_NB_JI_getView(), right);
        x.getChildren().add(cols);

        this.view = new ScrollPane(x);
        this.view.setFitToWidth(true);
        this.view.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        this.view.getStyleClass().add("page-scroll");
        c.WO_KI_NB_JI_getEventos().addListener((Observable o) -> Platform.runLater(this::WO_KI_NB_JI_refresh));
        c.WO_KI_NB_JI_getTareas().addListener((Observable o) -> Platform.runLater(this::WO_KI_NB_JI_refresh));
        WO_KI_NB_JI_refresh();
    }

    // ---------- héroe ----------
    private VBox WO_KI_NB_JI_hero(Runnable onNewEvent) {
        VBox hero = new VBox(8);
        hero.setPadding(new Insets(26, 30, 26, 30));
        hero.getStyleClass().add("dashboard-hero");
        Label hi = new Label(com.agendapersonal.util.WO_KI_NB_JI_Perfil.WO_KI_NB_JI_saludo(LocalTime.now().getHour(), com.agendapersonal.util.WO_KI_NB_JI_Perfil.WO_KI_NB_JI_nombre()));
        com.agendapersonal.util.WO_KI_NB_JI_Perfil.WO_KI_NB_JI_revisionProperty().addListener((o, a, b) ->
            hi.setText(com.agendapersonal.util.WO_KI_NB_JI_Perfil.WO_KI_NB_JI_saludo(LocalTime.now().getHour(), com.agendapersonal.util.WO_KI_NB_JI_Perfil.WO_KI_NB_JI_nombre())));
        WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_bind(hi, FontWeight.BOLD, 30);
        hi.setStyle("-fx-text-fill:-text;");
        Label quote = new Label(QUOTES[LocalDate.now().getDayOfYear() % QUOTES.length]);
        quote.setWrapText(true);
        quote.setStyle("-fx-text-fill:-muted;-fx-font-size:14;");

        clockLabel = new Label();
        clockLabel.setStyle("-fx-text-fill:-text;-fx-padding:0;");
        Runnable updateClock = () -> clockLabel.setText(WO_KI_NB_JI_RelojPrefs.WO_KI_NB_JI_formatearAhora());
        Runnable aplicarPrefs = () -> {
            WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_bind(clockLabel, FontWeight.BOLD, WO_KI_NB_JI_RelojPrefs.WO_KI_NB_JI_tamano().WO_KI_NB_JI_px());
            clockLabel.setVisible(WO_KI_NB_JI_RelojPrefs.WO_KI_NB_JI_visible());
            clockLabel.setManaged(WO_KI_NB_JI_RelojPrefs.WO_KI_NB_JI_visible());
            updateClock.run();
        };
        aplicarPrefs.run();
        WO_KI_NB_JI_RelojPrefs.WO_KI_NB_JI_revisionProperty().addListener((o, a, b) -> aplicarPrefs.run());
        clockTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> updateClock.run()));
        clockTimer.setCycleCount(Timeline.INDEFINITE);
        clockTimer.play();

        Label dateText = new Label(WO_KI_NB_JI_Ui.WO_KI_NB_JI_formatDate(LocalDate.now()));
        dateText.setStyle("-fx-text-fill:-muted;-fx-font-size:14;");
        HBox date = new HBox(8, WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("calendar", 18, "tint-muted"), dateText);
        date.setAlignment(Pos.CENTER_LEFT);

        Button nuevo = WO_KI_NB_JI_Ui.WO_KI_NB_JI_actionButton("add", "Nuevo evento");
        WO_KI_NB_JI_Ui.WO_KI_NB_JI_stylePrimary(nuevo);
        nuevo.setOnAction(e -> onNewEvent.run());
        HBox btns = new HBox(10, nuevo);
        if (com.agendapersonal.util.WO_KI_NB_JI_Perfil.WO_KI_NB_JI_esDocente()) {
            Button preparar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_actionButton("play", "Preparar clase");
            preparar.setOnAction(e -> onPrepararClase.run());
            btns.getChildren().add(preparar);
        }

        VBox left = new VBox(3, hi, quote, clockLabel, date, btns);

        // Tarjeta "enfoque ahora" sobre el héroe
        VBox focus = new VBox(4);
        focus.setPadding(new Insets(16));
        focus.getStyleClass().add("dashboard-focus");
        focus.setPrefWidth(290);
        focus.setMinHeight(170);
        Label fh = new Label("ENFOQUE AHORA");
        fh.setStyle("-fx-text-fill:-accent;-fx-font-size:12;-fx-font-weight:bold;");
        HBox focusHeader = new HBox(8, WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("focus", 18, "tint-accent"), fh);
        focusHeader.setAlignment(Pos.CENTER_LEFT);
        focusTitle = new Label("—");
        WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_bind(focusTitle, FontWeight.BOLD, 16);
        focusTitle.setStyle("-fx-text-fill:-text;");
        focusTitle.setWrapText(true);
        focusDetail = new Label("");
        focusDetail.setStyle("-fx-text-fill:-muted;-fx-font-size:12;");
        focusDetail.setWrapText(true);
        focus.getChildren().addAll(focusHeader, focusTitle, focusDetail);

        HBox row = new HBox(16, left, WO_KI_NB_JI_spacer(), focus);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(left, Priority.ALWAYS);
        hero.getChildren().add(row);
        return hero;
    }

    private HBox WO_KI_NB_JI_searchRow(Consumer<String> onSearchEvents) {
        TextField quickSearch = new TextField();
        quickSearch.setPromptText("Buscar eventos por título o descripción...");
        HBox.setHgrow(quickSearch, Priority.ALWAYS);
        Button searchBtn = new Button("Buscar");
        WO_KI_NB_JI_Ui.WO_KI_NB_JI_stylePrimary(searchBtn);
        Runnable go = () -> onSearchEvents.accept(quickSearch.getText().trim());
        searchBtn.setOnAction(e -> go.run());
        quickSearch.setOnAction(e -> go.run());
        HBox h = new HBox(8, quickSearch, searchBtn);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private VBox WO_KI_NB_JI_statCard(String label, Label valueLabel, String tone, String icon) {
        Label l = new Label(label);
        l.getStyleClass().add("stat-label");
        HBox top = new HBox(8, l, WO_KI_NB_JI_spacer(), WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconChip(icon, tone, 34, 18));
        top.setAlignment(Pos.CENTER_LEFT);
        valueLabel.getStyleClass().add("stat-value");
        VBox b = new VBox(10, top, valueLabel);
        b.getStyleClass().addAll("card", "stat-card");
        b.setPadding(new Insets(18, 20, 18, 20));
        HBox.setHgrow(b, Priority.ALWAYS);
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private Region WO_KI_NB_JI_spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    // ---------- tarjetas agenda / tareas ----------
    private VBox WO_KI_NB_JI_agendaCard() {
        VBox b = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        HBox h = new HBox(8);
        h.setAlignment(Pos.CENTER_LEFT);
        agendaTitulo = new Label("Agenda de hoy");
        WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_bind(agendaTitulo, FontWeight.BOLD, 17);
        agendaTitulo.setStyle("-fx-text-fill:-text;");
        eventCount = new Label();
        eventCount.setStyle("-fx-text-fill:-muted;");
        Region sp = WO_KI_NB_JI_spacer();
        Button hoyBtn = new Button("Hoy");
        hoyBtn.setOnAction(e -> {
            fechaSeleccionada = LocalDate.now();
            calendar.WO_KI_NB_JI_setSelected(fechaSeleccionada);
            WO_KI_NB_JI_refreshAgenda();
        });
        h.getChildren().addAll(agendaTitulo, eventCount, sp, hoyBtn);
        agendaTable = WO_KI_NB_JI_buildAgendaTable();
        b.getChildren().addAll(h, agendaTable);
        return b;
    }

    private VBox WO_KI_NB_JI_tasksCard(Runnable onNewTask) {
        VBox b = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        HBox h = new HBox(8);
        h.setAlignment(Pos.CENTER_LEFT);
        Label tt = new Label("Tareas");
        WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_bind(tt, FontWeight.BOLD, 17);
        tt.setStyle("-fx-text-fill:-text;");
        taskCount = new Label();
        taskCount.setStyle("-fx-text-fill:-muted;");
        Region sp = WO_KI_NB_JI_spacer();
        Button addTask = WO_KI_NB_JI_Ui.WO_KI_NB_JI_actionButton("add", "Nueva");
        WO_KI_NB_JI_Ui.WO_KI_NB_JI_stylePrimary(addTask);
        addTask.setOnAction(e -> onNewTask.run());
        h.getChildren().addAll(tt, taskCount, sp, addTask);
        taskBox = new VBox(6);
        b.getChildren().addAll(h, new Separator(), taskBox);
        return b;
    }

    public ScrollPane WO_KI_NB_JI_getView() { return view; }

    public void WO_KI_NB_JI_refresh() {
        if (agendaTable == null) return;
        if (calendar != null) calendar.WO_KI_NB_JI_refreshMarks();
        WO_KI_NB_JI_refreshAgenda();
        WO_KI_NB_JI_refreshSummary();
        WO_KI_NB_JI_refreshFocus();
    }

    private void WO_KI_NB_JI_refreshAgenda() {
        LocalDate hoy = LocalDate.now();
        LocalDate sel = fechaSeleccionada != null ? fechaSeleccionada : hoy;
        boolean esHoy = sel.equals(hoy);
        if (agendaTitulo != null)
            agendaTitulo.setText(esHoy ? "Agenda de hoy"
                : "Agenda: " + sel.getDayOfMonth() + "/" + sel.getMonthValue() + "/" + sel.getYear());
        if (esHoy) {
            List<WO_KI_NB_JI_Evento> proximos = c.WO_KI_NB_JI_getEventos().stream()
                .filter(e -> e.WO_KI_NB_JI_getFecha() != null && !e.WO_KI_NB_JI_getFecha().isBefore(hoy))
                .limit(8).toList();
            agendaTable.getItems().setAll(proximos);
            if (eventCount != null) eventCount.setText("(" + proximos.size() + ")");
        } else {
            List<WO_KI_NB_JI_Evento> delDia = c.WO_KI_NB_JI_eventosDelDia(sel);
            agendaTable.getItems().setAll(delDia);
            if (eventCount != null) eventCount.setText("(" + delDia.size() + ")");
        }

        taskBox.getChildren().clear();
        List<WO_KI_NB_JI_Tarea> ts = c.WO_KI_NB_JI_getTareas();
        long done = ts.stream().filter(t -> "Completada".equals(t.WO_KI_NB_JI_getEstado())).count();
        if (ts.isEmpty()) {
            Label l = new Label("Sin tareas. Crea la primera con + Nueva.");
            l.setStyle("-fx-text-fill:-muted;");
            taskBox.getChildren().add(l);
        } else for (WO_KI_NB_JI_Tarea t : ts.stream().limit(4).toList()) taskBox.getChildren().add(WO_KI_NB_JI_taskRow(t));
        if (taskCount != null) taskCount.setText("(" + done + "/" + ts.size() + ")");
        WO_KI_NB_JI_refreshSummary();
    }

    private void WO_KI_NB_JI_refreshSummary() {
        List<WO_KI_NB_JI_Evento> es = c.WO_KI_NB_JI_eventosDelDia(LocalDate.now());
        List<WO_KI_NB_JI_Tarea> ts = c.WO_KI_NB_JI_getTareas();
        long done = ts.stream().filter(t -> "Completada".equals(t.WO_KI_NB_JI_getEstado())).count();
        if (eventosHoyValue != null) eventosHoyValue.setText(String.valueOf(es.size()));
        if (eventosTotalesValue != null) eventosTotalesValue.setText(String.valueOf(c.WO_KI_NB_JI_getEventos().size()));
        if (pendientesValue != null) pendientesValue.setText(String.valueOf(ts.size() - done));
        if (completadasValue != null) completadasValue.setText(String.valueOf(done));
    }

    private void WO_KI_NB_JI_refreshFocus() {
        if (com.agendapersonal.util.WO_KI_NB_JI_Perfil.WO_KI_NB_JI_esDocente()) { WO_KI_NB_JI_refrescarEnfoqueDocente(); return; }
        LocalTime now = LocalTime.now();
        WO_KI_NB_JI_Evento next = null;
        long bestMin = Long.MAX_VALUE;
        for (WO_KI_NB_JI_Evento e : c.WO_KI_NB_JI_eventosDelDia(LocalDate.now())) {
            try {
                long m = java.time.Duration.between(now, LocalTime.parse(e.WO_KI_NB_JI_getHoraInicio())).toMinutes();
                if (m >= -30 && m < bestMin) { bestMin = m; next = e; }
            } catch (Exception ignored) {}
        }
        if (next == null) {
            var upcoming = c.WO_KI_NB_JI_getEventos().stream()
                .filter(e -> e.WO_KI_NB_JI_getFecha() != null && e.WO_KI_NB_JI_getFecha().isAfter(LocalDate.now()))
                .sorted((a, b2) -> a.WO_KI_NB_JI_getFecha().compareTo(b2.WO_KI_NB_JI_getFecha())).findFirst();
            if (upcoming.isPresent()) {
                next = upcoming.get();
                bestMin = -1;
            }
        }
        if (focusTitle == null) return;
        if (next == null) {
            focusTitle.setText("Día despejado");
            focusDetail.setText("Sin eventos próximos. Perfecto para avanzar proyectos.");
        } else if (bestMin == -1) {
            focusTitle.setText(next.WO_KI_NB_JI_getTitulo());
            focusDetail.setText(next.WO_KI_NB_JI_getFecha() + " • " + next.WO_KI_NB_JI_getHoraInicio());
        } else if (bestMin <= 0) {
            focusTitle.setText(next.WO_KI_NB_JI_getTitulo());
            focusDetail.setText("¡Ahora mismo! " + next.WO_KI_NB_JI_getHoraInicio() + "-" + next.WO_KI_NB_JI_getHoraFin());
        } else if (bestMin < 60) {
            focusTitle.setText(next.WO_KI_NB_JI_getTitulo());
            focusDetail.setText("En " + bestMin + " min • " + next.WO_KI_NB_JI_getHoraInicio());
        } else {
            focusTitle.setText(next.WO_KI_NB_JI_getTitulo());
            focusDetail.setText("En " + (bestMin / 60) + " h " + (bestMin % 60) + " min • " + next.WO_KI_NB_JI_getHoraInicio());
        }
    }

    @SuppressWarnings("unchecked")
    private TableView<WO_KI_NB_JI_Evento> WO_KI_NB_JI_buildAgendaTable() {
        TableView<WO_KI_NB_JI_Evento> t = new TableView<>();
        t.setPrefHeight(250);
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        t.setPlaceholder(new Label("No hay eventos para mostrar."));

        TableColumn<WO_KI_NB_JI_Evento, Void> colNum = new TableColumn<>("#");
        colNum.setPrefWidth(40);
        colNum.setMaxWidth(50);
        colNum.setSortable(false);
        colNum.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setText(empty ? null : String.valueOf(getIndex() + 1));
            }
        });

        TableColumn<WO_KI_NB_JI_Evento, String> colHora = new TableColumn<>("Hora");
        colHora.setPrefWidth(130);
        colHora.setCellValueFactory(cd ->
            new SimpleStringProperty(cd.getValue().WO_KI_NB_JI_getHoraInicio() + " - " + cd.getValue().WO_KI_NB_JI_getHoraFin()));

        TableColumn<WO_KI_NB_JI_Evento, String> colNombre = new TableColumn<>("Evento");
        colNombre.setPrefWidth(220);
        colNombre.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().WO_KI_NB_JI_getTitulo()));

        TableColumn<WO_KI_NB_JI_Evento, Void> colAcc = new TableColumn<>("Acciones");
        colAcc.setPrefWidth(150);
        colAcc.setSortable(false);
        colAcc.setCellFactory(col -> new TableCell<>() {
            private final Button edit = WO_KI_NB_JI_iconBtn("edit", "Editar");
            private final Button reprogram = WO_KI_NB_JI_iconBtn("reschedule", "Reprogramar");
            private final Button del = WO_KI_NB_JI_iconBtn("delete", "Eliminar");
            private final HBox box = new HBox(6, edit, reprogram, del);
            {
                box.setAlignment(Pos.CENTER_LEFT);
                edit.setOnAction(e -> {
                    WO_KI_NB_JI_Evento ev = WO_KI_NB_JI_rowAt(getTableView(), getIndex());
                    if (ev != null) WO_KI_NB_JI_EventViews.WO_KI_NB_JI_form(stage, ev, c, () -> WO_KI_NB_JI_refresh());
                });
                reprogram.setOnAction(e -> {
                    WO_KI_NB_JI_Evento ev = WO_KI_NB_JI_rowAt(getTableView(), getIndex());
                    if (ev != null) WO_KI_NB_JI_EventViews.WO_KI_NB_JI_reprogramar(stage, ev, c, () -> WO_KI_NB_JI_refresh());
                });
                del.setOnAction(e -> {
                    WO_KI_NB_JI_Evento ev = WO_KI_NB_JI_rowAt(getTableView(), getIndex());
                    if (ev == null) return;
                    Alert al = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar \"" + ev.WO_KI_NB_JI_getTitulo() + "\"?");
                    if (al.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK && c.WO_KI_NB_JI_eliminarEvento(ev.WO_KI_NB_JI_getId()))
                        WO_KI_NB_JI_Notifier.WO_KI_NB_JI_deleted("Evento eliminado", ev.WO_KI_NB_JI_getTitulo());
                });
            }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : box);
            }
        });

        t.getColumns().addAll(colNum, colHora, colNombre, colAcc);
        return t;
    }

    private Button WO_KI_NB_JI_iconBtn(String icon, String tip) {
        Button b = new Button();
        b.setGraphic(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_image(icon, 16));
        b.setTooltip(new Tooltip(tip));
        b.setAccessibleText(tip);
        b.getStyleClass().add("icon-action-button");
        return b;
    }

    private static WO_KI_NB_JI_Evento WO_KI_NB_JI_rowAt(TableView<WO_KI_NB_JI_Evento> t, int i) {
        if (t == null || i < 0 || i >= t.getItems().size()) return null;
        return t.getItems().get(i);
    }

    private HBox WO_KI_NB_JI_taskRow(WO_KI_NB_JI_Tarea t) {
        CheckBox cb = new CheckBox(t.WO_KI_NB_JI_getTitulo());
        cb.setSelected("Completada".equals(t.WO_KI_NB_JI_getEstado()));
        cb.setOnAction(e -> {
            var ventana = cb.getScene() == null ? null : cb.getScene().getWindow();
            var res = c.WO_KI_NB_JI_cambiarEstadoTarea(t, cb.isSelected()
                ? com.agendapersonal.model.WO_KI_NB_JI_EstadoTarea.COMPLETADA : com.agendapersonal.model.WO_KI_NB_JI_EstadoTarea.PENDIENTE);
            res.ifPresent(r -> WO_KI_NB_JI_Toast.WO_KI_NB_JI_mostrar(ventana, c.WO_KI_NB_JI_mensajeProgreso(r)));
            WO_KI_NB_JI_refresh();
        });
        HBox r = new HBox(cb);
        r.setPadding(new Insets(6));
        r.setStyle("-fx-background-color:-surface2;-fx-background-radius:8;");
        return r;
    }

    /** Enfoque ahora para docentes: la próxima clase de su planeación. */
    private void WO_KI_NB_JI_refrescarEnfoqueDocente() {
        LocalDate hoy = LocalDate.now();
        WO_KI_NB_JI_ActividadClase proxima = null;
        WO_KI_NB_JI_ClaseDocente clase = null;
        for (WO_KI_NB_JI_ClaseDocente cl : new WO_KI_NB_JI_ClaseDocenteDAO().WO_KI_NB_JI_listarClases()) {
            for (WO_KI_NB_JI_ActividadClase a : new WO_KI_NB_JI_ClaseDocenteDAO().WO_KI_NB_JI_listarActividades(cl.id())) {
                if (a.fecha() == null || a.fecha().isBefore(hoy)) continue;
                if (proxima == null || a.fecha().isBefore(proxima.fecha())) { proxima = a; clase = cl; }
            }
        }
        if (focusTitle == null) return;
        if (proxima == null) {
            focusTitle.setText("Sin clases aún");
            focusDetail.setText("Crea tu primer curso y su planeación en el Espacio Docente.");
        } else {
            focusTitle.setText("Próxima clase: " + proxima.titulo());
            focusDetail.setText((clase == null ? "" : clase.nombre() + " · ") + proxima.fecha());
        }
    }
}


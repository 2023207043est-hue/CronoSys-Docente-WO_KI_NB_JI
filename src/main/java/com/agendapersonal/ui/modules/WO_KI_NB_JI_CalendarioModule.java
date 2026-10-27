package com.agendapersonal.ui.modules;

import com.agendapersonal.ui.WO_KI_NB_JI_FontPreferences;

import com.agendapersonal.controller.WO_KI_NB_JI_MainController;
import com.agendapersonal.model.WO_KI_NB_JI_Evento;
import com.agendapersonal.ui.WO_KI_NB_JI_EventViews;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
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
public class WO_KI_NB_JI_CalendarioModule {
    private enum WO_KI_NB_JI_Vista { ANO, MES, SEMANA, AGENDA }

    private final WO_KI_NB_JI_MainController c;
    private final Stage stage;
    private final Locale es = Locale.of("es", "PE");
    private final VBox view = new VBox(12);
    private final VBox mainArea = new VBox(10);
    private final Label rangeLabel = new Label();
    private final TextField search = new TextField();
    private final ComboBox<String> catFilter = new ComboBox<>();
    private final ToggleGroup group = new ToggleGroup();
    private LocalDate cursor = LocalDate.now();
    private LocalDate selected = LocalDate.now();
    private WO_KI_NB_JI_Vista vista = WO_KI_NB_JI_Vista.MES;

    public WO_KI_NB_JI_CalendarioModule(WO_KI_NB_JI_MainController c, Stage stage) {
        this.c = c;
        this.stage = stage;
        view.setPadding(new Insets(20));
        view.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pageHeader("calendar", "Calendario",
            "Clic en cualquier día para ver sus eventos y crear nuevos. Todo se adapta a la pantalla.",
            "-surface2", "-accent"));
        view.getChildren().add(WO_KI_NB_JI_toolbar());

        catFilter.getItems().addAll("Todas", "Personal", "Trabajo", "Estudio", "Salud", "Proyecto");
        catFilter.setValue("Todas");

        HBox.setHgrow(mainArea, Priority.ALWAYS);
        VBox.setVgrow(mainArea, Priority.ALWAYS);
        view.getChildren().add(mainArea);
        VBox.setVgrow(mainArea, Priority.ALWAYS);

        search.textProperty().addListener((o, a, b) -> WO_KI_NB_JI_refreshAll());
        catFilter.setOnAction(e -> WO_KI_NB_JI_refreshAll());
        c.WO_KI_NB_JI_getEventos().addListener((Observable o) -> Platform.runLater(this::WO_KI_NB_JI_refreshAll));
        WO_KI_NB_JI_refreshAll();
    }

    public VBox WO_KI_NB_JI_getView() { return view; }

    // ---------- toolbar ----------
    private HBox WO_KI_NB_JI_toolbar() {
        Button prev = WO_KI_NB_JI_iconButton("arrow-left", "Mes anterior");
        Button today = new Button("Hoy");
        Button next = WO_KI_NB_JI_iconButton("arrow-right", "Mes siguiente");
        prev.setOnAction(e -> WO_KI_NB_JI_mover(-1));
        next.setOnAction(e -> WO_KI_NB_JI_mover(1));
        today.setOnAction(e -> {
            cursor = LocalDate.now();
            selected = LocalDate.now();
            WO_KI_NB_JI_refreshAll();
        });
        WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_bind(rangeLabel, FontWeight.BOLD, 17);
        rangeLabel.setStyle("-fx-text-fill:-text;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        ToggleButton bAno = WO_KI_NB_JI_segBtn("Año", WO_KI_NB_JI_Vista.ANO);
        ToggleButton bMes = WO_KI_NB_JI_segBtn("Mes", WO_KI_NB_JI_Vista.MES);
        ToggleButton bSem = WO_KI_NB_JI_segBtn("Semana", WO_KI_NB_JI_Vista.SEMANA);
        ToggleButton bAge = WO_KI_NB_JI_segBtn("Agenda", WO_KI_NB_JI_Vista.AGENDA);
        bMes.setSelected(true);
        HBox seg = new HBox(0, bAno, bMes, bSem, bAge);
        seg.setStyle("-fx-background-color:-surface2;-fx-background-radius:10;-fx-padding:2;");
        search.setPromptText("Buscar eventos...");
        search.setPrefWidth(200);
        Button nuevo = WO_KI_NB_JI_Ui.WO_KI_NB_JI_actionButton("add", "Nuevo evento");
        WO_KI_NB_JI_Ui.WO_KI_NB_JI_stylePrimary(nuevo);
        nuevo.setOnAction(e -> WO_KI_NB_JI_EventViews.WO_KI_NB_JI_formEnFecha(stage, null, selected, c, () -> {}));
        HBox bar = new HBox(8, prev, today, next, rangeLabel, sp, seg, search, catFilter, nuevo);
        bar.setAlignment(Pos.CENTER_LEFT);
        return bar;
    }

    private ToggleButton WO_KI_NB_JI_segBtn(String t, WO_KI_NB_JI_Vista v) {
        ToggleButton b = new ToggleButton(t);
        b.setToggleGroup(group);
        b.setStyle("-fx-background-radius:8;-fx-padding:7 14;-fx-cursor:hand;");
        b.setOnAction(e -> { vista = v; WO_KI_NB_JI_refreshAll(); });
        group.selectedToggleProperty().addListener((o, a, b2) -> {
            for (var tg : group.getToggles()) {
                ToggleButton tb = (ToggleButton) tg;
                boolean on = tb.isSelected();
                tb.setStyle("-fx-background-radius:8;-fx-padding:7 14;-fx-cursor:hand;"
                    + (on ? "-fx-background-color:-accent-soft;-fx-text-fill:-accent;-fx-font-weight:bold;"
                        : "-fx-background-color:transparent;-fx-text-fill:-muted;"));
            }
        });
        return b;
    }

    private Button WO_KI_NB_JI_iconButton(String icon, String accessibleText) {
        Button button = new Button();
        button.setGraphic(com.agendapersonal.ui.WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_image(icon, 16));
        button.setAccessibleText(accessibleText);
        button.setTooltip(new Tooltip(accessibleText));
        return button;
    }

    private void WO_KI_NB_JI_mover(int dir) {
        cursor = switch (vista) {
            case ANO -> cursor.plusYears(dir);
            case MES -> cursor.plusMonths(dir);
            case SEMANA -> cursor.plusWeeks(dir);
            case AGENDA -> cursor.plusMonths(dir);
        };
        WO_KI_NB_JI_refreshAll();
    }

    // ---------- motor ----------
    private void WO_KI_NB_JI_refreshAll() {
        mainArea.getChildren().setAll(switch (vista) {
            case ANO -> WO_KI_NB_JI_anoView();
            case MES -> WO_KI_NB_JI_mesView();
            case SEMANA -> WO_KI_NB_JI_semanaView();
            case AGENDA -> WO_KI_NB_JI_agendaView();
        });
    }

    private boolean WO_KI_NB_JI_pasa(WO_KI_NB_JI_Evento e) {
        String q = search.getText().trim().toLowerCase();
        boolean okQ = q.isBlank() || e.WO_KI_NB_JI_getTitulo().toLowerCase().contains(q)
            || (e.WO_KI_NB_JI_getDescripcion() != null && e.WO_KI_NB_JI_getDescripcion().toLowerCase().contains(q));
        boolean okC = "Todas".equals(catFilter.getValue()) || catFilter.getValue().equals(e.WO_KI_NB_JI_getCategoria());
        return okQ && okC;
    }

    private List<WO_KI_NB_JI_Evento> WO_KI_NB_JI_delDia(LocalDate d) {
        return c.WO_KI_NB_JI_eventosDelDia(d).stream().filter(this::WO_KI_NB_JI_pasa).toList();
    }

    private static final java.util.Map<String, String> CAT_BG = java.util.Map.of(
        "Personal", "-c-violet-soft", "Trabajo", "-c-blue-soft", "Estudio", "-c-teal-soft",
        "Salud", "-c-green-soft", "Proyecto", "-c-amber-soft");
    private static final java.util.Map<String, String> CAT_FG = java.util.Map.of(
        "Personal", "-c-violet", "Trabajo", "-c-blue", "Estudio", "-c-teal",
        "Salud", "-c-green", "Proyecto", "-c-amber");

    private Button WO_KI_NB_JI_chip(WO_KI_NB_JI_Evento e) {
        Button b = new Button(e.WO_KI_NB_JI_getHoraInicio() + "  " + e.WO_KI_NB_JI_getTitulo());
        b.setMaxWidth(Double.MAX_VALUE);
        b.setAlignment(Pos.CENTER_LEFT);
        b.setTooltip(new Tooltip(e.WO_KI_NB_JI_getTitulo() + "\n" + e.WO_KI_NB_JI_getHoraInicio() + "-" + e.WO_KI_NB_JI_getHoraFin()
            + " • " + e.WO_KI_NB_JI_getCategoria()));
        b.setStyle("-fx-background-color:" + CAT_BG.getOrDefault(e.WO_KI_NB_JI_getCategoria(), "-surface2")
            + ";-fx-text-fill:" + CAT_FG.getOrDefault(e.WO_KI_NB_JI_getCategoria(), "-text")
            + ";-fx-background-radius:7;-fx-font-size:12;-fx-cursor:hand;");
        b.setOnAction(ev -> WO_KI_NB_JI_EventViews.WO_KI_NB_JI_form(stage, e, c, () -> {}));
        return b;
    }

    /** Chip legible sobre celdas cian seleccionadas. */
    private Button WO_KI_NB_JI_chipOnAccent(WO_KI_NB_JI_Evento e) {
        Button b = WO_KI_NB_JI_chip(e);
        b.setStyle("-fx-background-color:-accent;-fx-text-fill:-on-accent;"
            + "-fx-background-radius:7;-fx-font-size:12;-fx-cursor:hand;");
        return b;
    }

    private void WO_KI_NB_JI_pick(LocalDate d) {
        selected = d;
        cursor = d;
        WO_KI_NB_JI_refreshAll();
    }

    // ---------- vista AÑO ----------
    private javafx.scene.Node WO_KI_NB_JI_anoView() {
        int year = cursor.getYear();
        rangeLabel.setText("Año " + year);
        GridPane g = new GridPane();
        g.setHgap(10);
        g.setVgap(10);
        for (int i = 0; i < 3; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 3);
            cc.setHgrow(Priority.ALWAYS);
            g.getColumnConstraints().add(cc);
        }
        LocalDate today = LocalDate.now();
        for (int m = 1; m <= 12; m++) {
            YearMonth ym = YearMonth.of(year, m);
            VBox box = new VBox(4);
            box.setPadding(new Insets(10));
            box.setStyle("-fx-background-color:-surface;-fx-background-radius:12;"
                + "-fx-border-color:-bord;-fx-border-radius:12;");
            Button title = new Button(WO_KI_NB_JI_cap(ym.getMonth().getDisplayName(TextStyle.FULL, es)));
            title.setMaxWidth(Double.MAX_VALUE);
            title.setStyle("-fx-background-color:transparent;-fx-text-fill:-text;"
                + "-fx-font-weight:bold;-fx-font-size:13;-fx-cursor:hand;");
            final YearMonth fym = ym;
            title.setOnAction(e -> { cursor = fym.atDay(1); vista = WO_KI_NB_JI_Vista.MES; WO_KI_NB_JI_syncSeg(); WO_KI_NB_JI_refreshAll(); });
            box.getChildren().add(title);
            GridPane days = new GridPane();
            days.setHgap(2);
            days.setVgap(2);
            for (int i = 0; i < 7; i++) {
                ColumnConstraints cc = new ColumnConstraints();
                cc.setPercentWidth(100.0 / 7);
                cc.setHgrow(Priority.ALWAYS);
                days.getColumnConstraints().add(cc);
            }
            int off = ym.atDay(1).getDayOfWeek().getValue() - 1;
            for (int day = 1; day <= ym.lengthOfMonth(); day++) {
                LocalDate fecha = ym.atDay(day);
                boolean has = !WO_KI_NB_JI_delDia(fecha).isEmpty();
                Button db = new Button(String.valueOf(day));
                db.setMaxWidth(Double.MAX_VALUE);
                String st = "-fx-background-radius:5;-fx-font-size:12;-fx-cursor:hand;-fx-padding:3 0;";
                if (fecha.equals(selected)) st += "-fx-background-color:-accent;-fx-text-fill:-on-accent;-fx-font-weight:bold;";
                else if (fecha.equals(today)) st += "-fx-background-color:-accent-soft;-fx-text-fill:-accent;-fx-font-weight:bold;";
                else if (has) st += "-fx-background-color:-accent-soft;-fx-text-fill:-accent;";
                else st += "-fx-background-color:transparent;-fx-text-fill:-text;";
                db.setStyle(st);
                db.setOnAction(e -> { WO_KI_NB_JI_pick(fecha); vista = WO_KI_NB_JI_Vista.MES; WO_KI_NB_JI_syncSeg(); WO_KI_NB_JI_refreshAll(); });
                int k = off + day - 1;
                days.add(db, k % 7, k / 7);
            }
            box.getChildren().add(days);
            g.add(box, (m - 1) % 3, (m - 1) / 3);
        }
        ScrollPane sp = new ScrollPane(g);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        VBox.setVgrow(sp, Priority.ALWAYS);
        return sp;
    }

    private void WO_KI_NB_JI_syncSeg() {
        for (var tg : group.getToggles()) {
            ToggleButton tb = (ToggleButton) tg;
            boolean on = (vista == WO_KI_NB_JI_Vista.ANO && "Año".equals(tb.getText()))
                || (vista == WO_KI_NB_JI_Vista.MES && "Mes".equals(tb.getText()))
                || (vista == WO_KI_NB_JI_Vista.SEMANA && "Semana".equals(tb.getText()))
                || (vista == WO_KI_NB_JI_Vista.AGENDA && "Agenda".equals(tb.getText()));
            tb.setSelected(on);
        }
    }

    // ---------- vista MES ----------
    private javafx.scene.Node WO_KI_NB_JI_mesView() {
        YearMonth ym = YearMonth.from(cursor);
        rangeLabel.setText(WO_KI_NB_JI_cap(ym.getMonth().getDisplayName(TextStyle.FULL, es)) + " de " + ym.getYear());
        GridPane g = new GridPane();
        g.setHgap(4);
        g.setVgap(4);
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            cc.setHgrow(Priority.ALWAYS);
            g.getColumnConstraints().add(cc);
        }
        String[] ds = {"LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM"};
        for (int i = 0; i < 7; i++) {
            Label z = new Label(ds[i]);
            z.setMaxWidth(Double.MAX_VALUE);
            z.setAlignment(Pos.CENTER);
            z.setStyle("-fx-text-fill:-muted;-fx-font-size:12;-fx-font-weight:bold;");
            g.add(z, i, 0);
        }
        int off = ym.atDay(1).getDayOfWeek().getValue() - 1;
        int rows = (off + ym.lengthOfMonth() + 6) / 7;
        for (int r = 1; r <= rows; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            g.getRowConstraints().add(rc);
        }
        RowConstraints rc0 = new RowConstraints();
        g.getRowConstraints().add(0, rc0);
        LocalDate today = LocalDate.now();
        for (int day = 1; day <= ym.lengthOfMonth(); day++) {
            LocalDate fecha = ym.atDay(day);
            var evs = WO_KI_NB_JI_delDia(fecha);
            VBox cell = new VBox(3);
            cell.setPadding(new Insets(6));
            cell.setMaxWidth(Double.MAX_VALUE);
            cell.setMaxHeight(Double.MAX_VALUE);
            boolean sel = fecha.equals(selected);
            String base = "-fx-background-radius:10;-fx-border-radius:10;-fx-cursor:hand;";
            if (sel) base += "-fx-background-color:-accent-soft;-fx-border-color:-accent;-fx-border-width:2;";
            else if (fecha.equals(today)) base += "-fx-background-color:-surface;-fx-border-color:-accent;-fx-border-width:2;";
            else base += "-fx-background-color:-surface;-fx-border-color:-bord;-fx-border-width:1;";
            cell.setStyle(base);
            Label num = new Label(String.valueOf(day));
            WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_bind(num, FontWeight.BOLD, 13);
            num.setStyle(sel ? "-fx-text-fill:-accent;-fx-font-weight:bold;" : "-fx-text-fill:-text;");
            cell.getChildren().add(num);
            for (WO_KI_NB_JI_Evento e : evs.stream().limit(3).toList()) cell.getChildren().add(sel ? WO_KI_NB_JI_chipOnAccent(e) : WO_KI_NB_JI_chip(e));
            if (evs.size() > 3) {
                Button more = WO_KI_NB_JI_Ui.WO_KI_NB_JI_actionButton("add", (evs.size() - 3) + " más");
                more.setStyle("-fx-background-color:transparent;-fx-font-size:12;-fx-cursor:hand;"
                    + "-fx-text-fill:" + "-accent" + ";");
                more.setOnAction(e -> WO_KI_NB_JI_pick(fecha));
                cell.getChildren().add(more);
            }
            cell.setOnMouseClicked(e -> WO_KI_NB_JI_pick(fecha));
            int k = off + day - 1;
            g.add(cell, k % 7, k / 7 + 1);
        }
        VBox.setVgrow(g, Priority.ALWAYS);
        return g;
    }

    // ---------- vista SEMANA ----------
    private javafx.scene.Node WO_KI_NB_JI_semanaView() {
        LocalDate lun = cursor.with(DayOfWeek.MONDAY);
        LocalDate dom = lun.plusDays(6);
        rangeLabel.setText(lun.getDayOfMonth() + "/" + lun.getMonthValue() + " – "
            + dom.getDayOfMonth() + "/" + dom.getMonthValue() + " de " + dom.getYear());
        HBox row = new HBox(6);
        LocalDate today = LocalDate.now();
        for (int i = 0; i < 7; i++) {
            LocalDate d = lun.plusDays(i);
            var evs = WO_KI_NB_JI_delDia(d);
            VBox col = new VBox(5);
            col.setPadding(new Insets(10));
            col.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(col, Priority.ALWAYS);
            boolean sel = d.equals(selected);
            col.setStyle("-fx-background-color:" + (sel ? "-accent-soft" : "-surface") + ";-fx-background-radius:12;"
                + "-fx-border-color:" + (d.equals(today) || sel ? "-accent" : "-bord") + ";-fx-border-radius:12;"
                + "-fx-border-width:" + (d.equals(today) || sel ? "2" : "1") + ";-fx-cursor:hand;");
            Label h = new Label(d.getDayOfWeek().getDisplayName(TextStyle.SHORT, es).toUpperCase()
                + " " + d.getDayOfMonth());
            WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_bind(h, FontWeight.BOLD, 13);
            h.setStyle(sel ? "-fx-text-fill:-accent;-fx-font-weight:bold;" : "-fx-text-fill:-text;-fx-font-weight:bold;");
            col.getChildren().add(h);
            if (evs.isEmpty()) {
                Label v = new Label("—");
                v.setStyle("-fx-text-fill:-muted;");
                col.getChildren().add(v);
            }
            for (WO_KI_NB_JI_Evento e : evs.stream().limit(5).toList()) col.getChildren().add(sel ? WO_KI_NB_JI_chipOnAccent(e) : WO_KI_NB_JI_chip(e));
            if (evs.size() > 5) {
                Label m = new Label("+" + (evs.size() - 5) + " más");
                m.setStyle("-fx-text-fill:-accent;");
                col.getChildren().add(m);
            }
            Button ver = new Button("Ver día");
            ver.setMaxWidth(Double.MAX_VALUE);
            ver.setStyle("-fx-background-color:transparent;-fx-cursor:hand;"
                + "-fx-text-fill:" + (sel ? "-accent" : "-text") + ";");
            final LocalDate fd = d;
            ver.setOnAction(e -> { WO_KI_NB_JI_pick(fd); vista = WO_KI_NB_JI_Vista.MES; WO_KI_NB_JI_syncSeg(); WO_KI_NB_JI_refreshAll(); });
            col.getChildren().add(ver);
            final LocalDate cd = d;
            col.setOnMouseClicked(e -> { selected = cd; WO_KI_NB_JI_refreshAll(); });
            row.getChildren().add(col);
        }
        ScrollPane sp = new ScrollPane(row);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        VBox.setVgrow(sp, Priority.ALWAYS);
        return sp;
    }

    // ---------- vista AGENDA ----------
    @SuppressWarnings("unchecked")
    private javafx.scene.Node WO_KI_NB_JI_agendaView() {
        rangeLabel.setText("Todos los eventos");
        VBox box = new VBox(10);
        TableView<WO_KI_NB_JI_Evento> t = new TableView<>();
        t.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        TableColumn<WO_KI_NB_JI_Evento, String> c1 = new TableColumn<>("Fecha");
        c1.setCellValueFactory(cd -> new SimpleStringProperty("" + cd.getValue().WO_KI_NB_JI_getFecha()));
        TableColumn<WO_KI_NB_JI_Evento, String> c2 = new TableColumn<>("Hora");
        c2.setCellValueFactory(cd -> new SimpleStringProperty(
            cd.getValue().WO_KI_NB_JI_getHoraInicio() + "-" + cd.getValue().WO_KI_NB_JI_getHoraFin()));
        TableColumn<WO_KI_NB_JI_Evento, String> c3 = new TableColumn<>("Evento");
        c3.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().WO_KI_NB_JI_getTitulo()));
        TableColumn<WO_KI_NB_JI_Evento, String> c4 = new TableColumn<>("Categoría");
        c4.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().WO_KI_NB_JI_getCategoria()));
        TableColumn<WO_KI_NB_JI_Evento, String> c5 = new TableColumn<>("Prioridad");
        c5.setCellValueFactory(cd -> new SimpleStringProperty(cd.getValue().WO_KI_NB_JI_getPrioridad()));
        t.getColumns().addAll(c1, c2, c3, c4, c5);
        t.setRowFactory(tv -> {
            javafx.scene.control.TableRow<WO_KI_NB_JI_Evento> r = new javafx.scene.control.TableRow<>();
            r.setOnMouseClicked(e -> {
                if (e.getClickCount() == 2 && r.getItem() != null)
                    WO_KI_NB_JI_EventViews.WO_KI_NB_JI_form(stage, r.getItem(), c, () -> {});
            });
            return r;
        });
        Runnable fill = () -> {
            var all = new ArrayList<>(c.WO_KI_NB_JI_getEventos().stream().filter(this::WO_KI_NB_JI_pasa).toList());
            t.getItems().setAll(all);
        };
        fill.run();
        box.getChildren().addAll(WO_KI_NB_JI_hint("Doble clic en una fila para editar. (" + t.getItems().size() + " eventos)"), t);
        VBox.setVgrow(t, Priority.ALWAYS);
        return box;
    }

    private Label WO_KI_NB_JI_hint(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-text-fill:-muted;");
        l.setWrapText(true);
        return l;
    }

    private String WO_KI_NB_JI_cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(es) + s.substring(1);
    }
}


package com.agendapersonal.ui;

import com.agendapersonal.ui.WO_KI_NB_JI_FontPreferences;

import com.agendapersonal.controller.WO_KI_NB_JI_MainController;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Calendario manipulable: navegar meses, seleccionar fecha y ver días con eventos. */
public class WO_KI_NB_JI_InteractiveCalendar {
    private final WO_KI_NB_JI_MainController c;
    private final Consumer<LocalDate> onSelect;
    private final VBox root;
    private final Label monthLabel;
    private final Label monthCount;
    private final GridPane grid;
    private YearMonth shown;
    private LocalDate selected;
    private final Locale es = Locale.of("es", "PE");

    public WO_KI_NB_JI_InteractiveCalendar(WO_KI_NB_JI_MainController c, Consumer<LocalDate> onSelect) {
        this.c = c;
        this.onSelect = onSelect;
        this.selected = LocalDate.now();
        this.shown = YearMonth.now();

        root = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        Label h = new Label("Calendario");
        WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_bind(h, FontWeight.BOLD, 18);

        Button prev = WO_KI_NB_JI_iconButton("arrow-left", "Mes anterior");
        Button next = WO_KI_NB_JI_iconButton("arrow-right", "Mes siguiente");
        Button todayBtn = new Button("Hoy");
        monthLabel = new Label();
        WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_bind(monthLabel, FontWeight.BOLD, 13);
        monthCount = new Label();
        monthCount.setStyle("-fx-text-fill:-muted;");
        prev.setOnAction(e -> { shown = shown.minusMonths(1); WO_KI_NB_JI_rebuild(); });
        next.setOnAction(e -> { shown = shown.plusMonths(1); WO_KI_NB_JI_rebuild(); });
        todayBtn.setOnAction(e -> {
            shown = YearMonth.now();
            selected = LocalDate.now();
            WO_KI_NB_JI_rebuild();
            onSelect.accept(selected);
        });

        HBox nav = new HBox(8, prev, monthLabel, next);
        nav.setAlignment(Pos.CENTER_LEFT);
        HBox top = new HBox(8, nav, WO_KI_NB_JI_spacer(), todayBtn);
        top.setAlignment(Pos.CENTER_LEFT);

        grid = new GridPane();
        grid.setHgap(7);
        grid.setVgap(7);

        HBox legend = new HBox(12,
            WO_KI_NB_JI_legendItem("-accent", "Hoy"),
            WO_KI_NB_JI_legendItem("-accent-soft", "Con eventos"),
            WO_KI_NB_JI_legendItem("-text", "Seleccionado"));
        legend.setPadding(new Insets(4, 0, 0, 0));

        root.getChildren().addAll(h, top, monthCount, grid, legend);
        WO_KI_NB_JI_rebuild();
    }

    private Region WO_KI_NB_JI_spacer() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }

    private Button WO_KI_NB_JI_iconButton(String icon, String accessibleText) {
        Button button = new Button();
        button.setGraphic(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_image(icon, 16));
        button.getStyleClass().add("icon-button");
        button.setAccessibleText(accessibleText);
        button.setTooltip(new Tooltip(accessibleText));
        return button;
    }

    private HBox WO_KI_NB_JI_legendItem(String color, String text) {
        Label dot = new Label("  ");
        dot.setStyle("-fx-background-color:" + color + ";-fx-background-radius:6;-fx-min-width:14;-fx-min-height:14;");
        Label l = new Label(text);
        l.setStyle("-fx-font-size:12;-fx-text-fill:-muted;");
        HBox b = new HBox(5, dot, l);
        b.setAlignment(Pos.CENTER_LEFT);
        return b;
    }

    public VBox WO_KI_NB_JI_getView() { return root; }
    public LocalDate WO_KI_NB_JI_getSelected() { return selected; }

    public void WO_KI_NB_JI_setSelected(LocalDate d) {
        selected = d;
        if (d != null) shown = YearMonth.from(d);
        WO_KI_NB_JI_rebuild();
    }

    /** Solo repinta marcas de eventos (llamado por el auto-refresh). */
    public void WO_KI_NB_JI_refreshMarks() { WO_KI_NB_JI_rebuild(); }

    private void WO_KI_NB_JI_rebuild() {
        monthLabel.setText(WO_KI_NB_JI_cap(shown.getMonth().getDisplayName(TextStyle.FULL, es)) + " " + shown.getYear());
        grid.getChildren().clear();
        String[] ds = {"L", "M", "X", "J", "V", "S", "D"};
        for (int i = 0; i < 7; i++) {
            Label z = new Label(ds[i]);
            z.setPrefWidth(45);
            z.setAlignment(Pos.CENTER);
            z.setStyle("-fx-text-fill:-muted;");
            grid.add(z, i, 0);
        }
        // Conteo de eventos del mes (una sola pasada)
        Map<LocalDate, Long> porDia = new HashMap<>();
        for (var e : c.WO_KI_NB_JI_getEventos()) {
            if (e.WO_KI_NB_JI_getFecha() != null && YearMonth.from(e.WO_KI_NB_JI_getFecha()).equals(shown))
                porDia.merge(e.WO_KI_NB_JI_getFecha(), 1L, Long::sum);
        }
        monthCount.setText(porDia.isEmpty() ? "Sin eventos este mes"
            : porDia.values().stream().mapToLong(Long::longValue).sum() + " evento(s) este mes");

        LocalDate today = LocalDate.now();
        int off = shown.atDay(1).getDayOfWeek().getValue() - 1;
        for (int day = 1; day <= shown.lengthOfMonth(); day++) {
            LocalDate fecha = shown.atDay(day);
            long n = porDia.getOrDefault(fecha, 0L);
            Button b = new Button(n > 0 ? day + " •" : String.valueOf(day));
            b.setPrefSize(45, 38);
            b.getStyleClass().add("day-btn");
            b.setAlignment(Pos.CENTER);
            b.setStyle(WO_KI_NB_JI_baseStyle(fecha, today, n > 0));
            if (n > 0) b.setTooltip(new Tooltip(n + " evento(s) el " + day + "/" + shown.getMonthValue()));
            b.setOnAction(e -> {
                selected = fecha;
                WO_KI_NB_JI_rebuild();
                onSelect.accept(fecha);
            });
            int k = off + day - 1;
            grid.add(b, k % 7, k / 7 + 1);
        }
    }

    private String WO_KI_NB_JI_baseStyle(LocalDate fecha, LocalDate today, boolean hasEvents) {
        if (fecha.equals(selected))
            return "-fx-background-color:-accent;-fx-text-fill:-on-accent;-fx-font-weight:bold;-fx-background-radius:20;-fx-cursor:hand;";
        if (fecha.equals(today))
            return "-fx-background-color:-accent-soft;-fx-text-fill:-accent;-fx-background-radius:20;-fx-cursor:hand;";
        if (hasEvents)
            return "-fx-background-color:-accent-soft;-fx-text-fill:-accent;-fx-font-weight:bold;-fx-background-radius:20;-fx-cursor:hand;";
        return "-fx-background-color:transparent;-fx-text-fill:-text;-fx-cursor:hand;";
    }

    private String WO_KI_NB_JI_cap(String s) {
        if (s == null || s.isEmpty()) return s;
        return s.substring(0, 1).toUpperCase(es) + s.substring(1);
    }
}


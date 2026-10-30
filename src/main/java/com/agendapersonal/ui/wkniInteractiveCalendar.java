package com.agendapersonal.ui;

import com.agendapersonal.ui.wkniFontPreferences;

import com.agendapersonal.controller.wkniMainController;
import com.agendapersonal.util.wkniUi;
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
public class wkniInteractiveCalendar {
    private final wkniMainController wkniC;
    private final Consumer<LocalDate> wkniOnSelect;
    private final VBox wkniRoot;
    private final Label wkniMonthLabel;
    private final Label wkniMonthCount;
    private final GridPane wkniGrid;
    private YearMonth wkniShown;
    private LocalDate wkniSelected;
    private final Locale wkniEs = Locale.of("es", "PE");

    public wkniInteractiveCalendar(wkniMainController wkniC, Consumer<LocalDate> wkniOnSelect) {
        this.wkniC = wkniC;
        this.wkniOnSelect = wkniOnSelect;
        this.wkniSelected = LocalDate.now();
        this.wkniShown = YearMonth.now();

        wkniRoot = wkniUi.wkniCard();
        Label wkniH = new Label("Calendario");
        wkniFontPreferences.wkniBind(wkniH, FontWeight.BOLD, 18);

        Button wkniPrev = wkniIconButton("arrow-left", "Mes anterior");
        Button wkniNext = wkniIconButton("arrow-right", "Mes siguiente");
        Button wkniTodayBtn = new Button("Hoy");
        wkniMonthLabel = new Label();
        wkniFontPreferences.wkniBind(wkniMonthLabel, FontWeight.BOLD, 13);
        wkniMonthCount = new Label();
        wkniMonthCount.setStyle("-fx-text-fill:-muted;");
        wkniPrev.setOnAction(wkniE -> { wkniShown = wkniShown.minusMonths(1); wkniRebuild(); });
        wkniNext.setOnAction(wkniE -> { wkniShown = wkniShown.plusMonths(1); wkniRebuild(); });
        wkniTodayBtn.setOnAction(wkniE -> {
            wkniShown = YearMonth.now();
            wkniSelected = LocalDate.now();
            wkniRebuild();
            wkniOnSelect.accept(wkniSelected);
        });

        HBox wkniNav = new HBox(8, wkniPrev, wkniMonthLabel, wkniNext);
        wkniNav.setAlignment(Pos.CENTER_LEFT);
        HBox wkniTop = new HBox(8, wkniNav, wkniSpacer(), wkniTodayBtn);
        wkniTop.setAlignment(Pos.CENTER_LEFT);

        wkniGrid = new GridPane();
        wkniGrid.setHgap(7);
        wkniGrid.setVgap(7);

        HBox wkniLegend = new HBox(12,
            wkniLegendItem("-accent", "Hoy"),
            wkniLegendItem("-accent-soft", "Con eventos"),
            wkniLegendItem("-text", "Seleccionado"));
        wkniLegend.setPadding(new Insets(4, 0, 0, 0));

        wkniRoot.getChildren().addAll(wkniH, wkniTop, wkniMonthCount, wkniGrid, wkniLegend);
        wkniRebuild();
    }

    private Region wkniSpacer() {
        Region wkniR = new Region();
        HBox.setHgrow(wkniR, Priority.ALWAYS);
        return wkniR;
    }

    private Button wkniIconButton(String wkniIcon, String wkniAccessibleText) {
        Button wkniButton = new Button();
        wkniButton.setGraphic(wkniIconFactory.wkniImage(wkniIcon, 16));
        wkniButton.getStyleClass().add("icon-button");
        wkniButton.setAccessibleText(wkniAccessibleText);
        wkniButton.setTooltip(new Tooltip(wkniAccessibleText));
        return wkniButton;
    }

    private HBox wkniLegendItem(String wkniColor, String wkniText) {
        Label wkniDot = new Label("  ");
        wkniDot.setStyle("-fx-background-color:" + wkniColor + ";-fx-background-radius:6;-fx-min-width:14;-fx-min-height:14;");
        Label wkniL = new Label(wkniText);
        wkniL.setStyle("-fx-font-size:12;-fx-text-fill:-muted;");
        HBox wkniB = new HBox(5, wkniDot, wkniL);
        wkniB.setAlignment(Pos.CENTER_LEFT);
        return wkniB;
    }

    public VBox wkniGetView() { return wkniRoot; }
    public LocalDate wkniGetSelected() { return wkniSelected; }

    public void wkniSetSelected(LocalDate wkniD) {
        wkniSelected = wkniD;
        if (wkniD != null) wkniShown = YearMonth.from(wkniD);
        wkniRebuild();
    }

    /** Solo repinta marcas de eventos (llamado por el auto-refresh). */
    public void wkniRefreshMarks() { wkniRebuild(); }

    private void wkniRebuild() {
        wkniMonthLabel.setText(wkniCap(wkniShown.getMonth().getDisplayName(TextStyle.FULL, wkniEs)) + " " + wkniShown.getYear());
        wkniGrid.getChildren().clear();
        String[] wkniDs = {"L", "M", "X", "J", "V", "S", "D"};
        for (int wkniI = 0; wkniI < 7; wkniI++) {
            Label wkniZ = new Label(wkniDs[wkniI]);
            wkniZ.setPrefWidth(45);
            wkniZ.setAlignment(Pos.CENTER);
            wkniZ.setStyle("-fx-text-fill:-muted;");
            wkniGrid.add(wkniZ, wkniI, 0);
        }
        // Conteo de eventos del mes (una sola pasada)
        Map<LocalDate, Long> wkniPorDia = new HashMap<>();
        for (var wkniE : wkniC.wkniGetEventos()) {
            if (wkniE.wkniGetFecha() != null && YearMonth.from(wkniE.wkniGetFecha()).equals(wkniShown))
                wkniPorDia.merge(wkniE.wkniGetFecha(), 1L, Long::sum);
        }
        wkniMonthCount.setText(wkniPorDia.isEmpty() ? "Sin eventos este mes"
            : wkniPorDia.values().stream().mapToLong(Long::longValue).sum() + " evento(s) este mes");

        LocalDate wkniToday = LocalDate.now();
        int wkniOff = wkniShown.atDay(1).getDayOfWeek().getValue() - 1;
        for (int wkniDay = 1; wkniDay <= wkniShown.lengthOfMonth(); wkniDay++) {
            LocalDate wkni_fecha = wkniShown.atDay(wkniDay);
            long wkniN = wkniPorDia.getOrDefault(wkni_fecha, 0L);
            Button wkniB = new Button(wkniN > 0 ? wkniDay + " •" : String.valueOf(wkniDay));
            wkniB.setPrefSize(45, 38);
            wkniB.getStyleClass().add("day-btn");
            wkniB.setAlignment(Pos.CENTER);
            wkniB.setStyle(wkniBaseStyle(wkni_fecha, wkniToday, wkniN > 0));
            if (wkniN > 0) wkniB.setTooltip(new Tooltip(wkniN + " evento(s) el " + wkniDay + "/" + wkniShown.getMonthValue()));
            wkniB.setOnAction(wkniE -> {
                wkniSelected = wkni_fecha;
                wkniRebuild();
                wkniOnSelect.accept(wkni_fecha);
            });
            int wkniK = wkniOff + wkniDay - 1;
            wkniGrid.add(wkniB, wkniK % 7, wkniK / 7 + 1);
        }
    }

    private String wkniBaseStyle(LocalDate wkni_fecha, LocalDate wkniToday, boolean wkniHasEvents) {
        if (wkni_fecha.equals(wkniSelected))
            return "-fx-background-color:-accent;-fx-text-fill:-on-accent;-fx-font-weight:bold;-fx-background-radius:20;-fx-cursor:hand;";
        if (wkni_fecha.equals(wkniToday))
            return "-fx-background-color:-accent-soft;-fx-text-fill:-accent;-fx-background-radius:20;-fx-cursor:hand;";
        if (wkniHasEvents)
            return "-fx-background-color:-accent-soft;-fx-text-fill:-accent;-fx-font-weight:bold;-fx-background-radius:20;-fx-cursor:hand;";
        return "-fx-background-color:transparent;-fx-text-fill:-text;-fx-cursor:hand;";
    }

    private String wkniCap(String wkniS) {
        if (wkniS == null || wkniS.isEmpty()) return wkniS;
        return wkniS.substring(0, 1).toUpperCase(wkniEs) + wkniS.substring(1);
    }
}


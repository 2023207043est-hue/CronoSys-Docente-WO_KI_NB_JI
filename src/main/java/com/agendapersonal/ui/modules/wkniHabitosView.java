package com.agendapersonal.ui.modules;

import com.agendapersonal.domain.model.wkniHabito;
import com.agendapersonal.infrastructure.repo.wkniHabitoDAO;
import com.agendapersonal.service.wkniCalendarioUnificadoService;
import com.agendapersonal.ui.wkniAnim;
import com.agendapersonal.ui.wkniIconFactory;
import com.agendapersonal.ui.wkniNotifier;
import com.agendapersonal.util.wkniUi;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Hábitos: resumen del día, semana en puntitos, racha y check grande para marcar lo cumplido. */
public class wkniHabitosView {
    private static final String[] wkniDIAS = {"L", "M", "M", "J", "V", "S", "D"};
    private final wkniHabitoDAO wkniDao = new wkniHabitoDAO();
    private final VBox wkniView;
    private final HBox wkni_resumen = new HBox(0);
    private final VBox wkniFilas = new VBox(0);
    private final TextField wkniNombre = new TextField();

    public wkniHabitosView() {
        VBox wkniPage = wkniUi.wkniPage();
        Button wkniNuevo = wkniUi.wkniPrimary("add", "Nuevo hábito");
        wkniNuevo.setOnAction(wkniE -> { wkniNombre.requestFocus(); wkniAnim.wkniPop(wkniNombre); });
        wkniPage.getChildren().add(wkniUi.wkniPageHeader("habits", "Hábitos",
            "Marca lo que cumpliste hoy y mantén viva tu racha.", wkniNuevo));
        wkni_resumen.getStyleClass().addAll("card", "summary-strip");
        wkniFilas.getStyleClass().add("list-card");
        VBox wkniLista = new VBox(0, wkniFilas, wkniFormulario());
        wkniLista.getStyleClass().addAll("card", "card-flush");
        wkniPage.getChildren().addAll(wkni_resumen, wkniLista);
        wkniView = wkniPage;
        wkniRefresh();
    }

    private Node wkniFormulario() {
        wkniNombre.setPromptText("Nuevo hábito (ej. Leer 20 minutos)");
        wkniNombre.getStyleClass().add("bare-field");
        HBox.setHgrow(wkniNombre, Priority.ALWAYS);
        ComboBox<String> wkniFrec = new ComboBox<>();
        wkniFrec.getItems().addAll("Todos los días", "Una vez por semana");
        wkniFrec.setValue("Todos los días");
        wkniFrec.getStyleClass().add("compact-combo");
        Button wkniAdd = wkniUi.wkniSecondary("add", "Agregar");
        Runnable wkniAgregar = () -> {
            String wkniN = wkniNombre.getText().trim();
            if (wkniN.isEmpty()) { wkniAnim.wkniShake(wkniNombre); return; }
            String wkniF = wkniFrec.getSelectionModel().getSelectedIndex() == 1 ? "SEMANAL" : "DIARIO";
            if (wkniDao.wkniInsertar(new wkniHabito(0, wkniN, "", wkniF, 1, "#2C7A4B", true))) {
                wkniNombre.clear();
                wkniRefresh();
                wkniNotifier.wkniSuccess("Hábito creado", wkniN + " · ¡a por la primera racha!");
            } else wkniNotifier.wkniError("No se pudo crear el hábito", null);
        };
        wkniAdd.setOnAction(wkniE -> wkniAgregar.run());
        wkniNombre.setOnAction(wkniE -> wkniAgregar.run());
        HBox wkniRow = new HBox(12, wkniIconFactory.wkniIcon("add", 18), wkniNombre, wkniFrec, wkniAdd);
        wkniRow.setAlignment(Pos.CENTER_LEFT);
        wkniRow.getStyleClass().add("add-row");
        return wkniRow;
    }

    private void wkniRefresh() {
        LocalDate wkniHoy = LocalDate.now();
        LocalDate wkniLunes = wkniHoy.with(DayOfWeek.MONDAY);
        List<wkniHabito> wkniHabitos = wkniDao.wkniListar();
        List<wkniHabito> wkniActivos = wkniHabitos.stream().filter(wkniHabito::wkniIsActivo).toList();
        Map<String, Set<Integer>> wkniSemana = wkniDao.wkniCumplidosEntre(wkniLunes.toString(), wkniLunes.plusDays(6).toString());
        Set<Integer> wkniHechosHoy = wkniSemana.getOrDefault(wkniHoy.toString(), Set.of());

        // ---- resumen
        List<wkniHabito> wkniDeHoy = wkniActivos.stream().filter(wkniH -> wkniCalendarioUnificadoService.wkniAplicaHabito(wkniH, wkniHoy)).toList();
        long wkniCumplidosHoy = wkniDeHoy.stream().filter(wkniH -> wkniHechosHoy.contains(wkniH.wkniGetId())).count();
        int wkniProgramados = 0, wkniCumplidos = 0;
        for (LocalDate wkniD = wkniLunes; !wkniD.isAfter(wkniHoy); wkniD = wkniD.plusDays(1)) {
            Set<Integer> wkniHechos = wkniSemana.getOrDefault(wkniD.toString(), Set.of());
            for (wkniHabito wkniH : wkniActivos) if (wkniCalendarioUnificadoService.wkniAplicaHabito(wkniH, wkniD)) {
                wkniProgramados++;
                if (wkniHechos.contains(wkniH.wkniGetId())) wkniCumplidos++;
            }
        }
        wkniHabito wkniMejor = null;
        int wkniMejorRacha = 0;
        List<Integer> wkniRachas = new ArrayList<>();
        for (wkniHabito wkniH : wkniHabitos) {
            int wkniR = wkniDao.wkniRacha(wkniH.wkniGetId());
            wkniRachas.add(wkniR);
            if (wkniH.wkniIsActivo() && wkniR > wkniMejorRacha) { wkniMejorRacha = wkniR; wkniMejor = wkniH; }
        }
        long wkniFaltan = wkniDeHoy.size() - wkniCumplidosHoy;
        wkni_resumen.getChildren().setAll(
            wkniCelda("Hoy", wkniCumplidosHoy + "", " de " + wkniDeHoy.size(),
                wkniDeHoy.isEmpty() ? "Sin hábitos para hoy." : wkniFaltan == 0 ? "¡Día completo! Excelente." : "Te faltan " + wkniFaltan + " para cerrar el día.",
                wkniDeHoy.isEmpty() ? 0 : (double) wkniCumplidosHoy / wkniDeHoy.size()),
            wkniCeldaRacha(wkniMejorRacha, wkniMejor),
            wkniCelda("Esta semana", wkniProgramados == 0 ? "—" : Math.round(100.0 * wkniCumplidos / wkniProgramados) + " %", "",
                wkniCumplidos + " de " + wkniProgramados + " registros cumplidos", -1));
        wkni_resumen.getChildren().getLast().getStyleClass().add("with-left-divider");

        // ---- filas
        wkniFilas.getChildren().clear();
        GridPane wkniHead = wkniGrid();
        wkniHead.add(wkniUi.wkniH3("Tus hábitos"), 1, 0);
        HBox wkniLetras = new HBox(8);
        for (int wkniI = 0; wkniI < 7; wkniI++) {
            Label wkniL = wkniUi.wkniLabel(wkniDIAS[wkniI], "day-letter");
            if (wkniLunes.plusDays(wkniI).equals(wkniHoy)) wkniL.getStyleClass().add("day-letter-today");
            wkniLetras.getChildren().add(wkniL);
        }
        wkniHead.add(wkniLetras, 2, 0);
        Label wkniRl = wkniUi.wkniLabel("Racha", "day-letter");
        wkniHead.add(wkniRl, 3, 0);
        GridPane.setHalignment(wkniRl, javafx.geometry.HPos.RIGHT);
        wkniHead.getStyleClass().add("habit-head");
        wkniFilas.getChildren().add(wkniHead);

        if (wkniHabitos.isEmpty()) {
            wkniFilas.getChildren().add(wkniUi.wkniEmpty("habits", "Crea tu primer hábito",
                "Escribe abajo algo pequeño que quieras repetir, como «Beber 2 L de agua»."));
            return;
        }
        List<Node> wkniNodos = new ArrayList<>();
        for (int wkniI = 0; wkniI < wkniHabitos.size(); wkniI++) {
            wkniHabito wkniH = wkniHabitos.get(wkniI);
            Node wkniF = wkniFila(wkniH, wkniRachas.get(wkniI), wkniHechosHoy.contains(wkniH.wkniGetId()), wkniLunes, wkniHoy, wkniSemana);
            wkniFilas.getChildren().add(wkniF);
            wkniNodos.add(wkniF);
        }
        wkniAnim.wkniStagger(wkniNodos);
    }

    private GridPane wkniGrid() {
        GridPane wkniG = new GridPane();
        wkniG.setHgap(20);
        ColumnConstraints wkniC0 = new ColumnConstraints(48);
        ColumnConstraints wkniC1 = new ColumnConstraints();
        wkniC1.setHgrow(Priority.ALWAYS);
        wkniC1.setMinWidth(160);
        ColumnConstraints wkniC2 = new ColumnConstraints(7 * 28 + 6 * 8);
        ColumnConstraints wkniC3 = new ColumnConstraints(110);
        ColumnConstraints wkniC4 = new ColumnConstraints(40);
        wkniG.getColumnConstraints().addAll(wkniC0, wkniC1, wkniC2, wkniC3, wkniC4);
        wkniG.setAlignment(Pos.CENTER_LEFT);
        return wkniG;
    }

    private Node wkniFila(wkniHabito wkniH, int wkniRacha, boolean wkniHecho, LocalDate wkniLunes, LocalDate wkniHoy, Map<String, Set<Integer>> wkniSemana) {
        boolean wkniTocaHoy = wkniH.wkniIsActivo() && wkniCalendarioUnificadoService.wkniAplicaHabito(wkniH, wkniHoy);
        Button wkniCheck = new Button();
        wkniCheck.getStyleClass().addAll("habit-check");
        if (wkniHecho) wkniCheck.getStyleClass().add("checked");
        wkniCheck.setGraphic(wkniIconFactory.wkniIcon("check", 20, "tint-on-accent"));
        wkniCheck.setAccessibleText((wkniHecho ? "Desmarcar " : "Marcar ") + wkniH.wkniGetNombre());
        wkniUi.wkniTooltip(wkniCheck, wkniHecho ? "Desmarcar hoy" : "Marcar como cumplido hoy");
        wkniCheck.setDisable(!wkniH.wkniIsActivo());
        wkniCheck.setOnAction(wkniE -> {
            wkniAnim.wkniPop(wkniCheck);
            String wkniD = wkniHoy.toString();
            boolean wkniOk = wkniHecho ? wkniDao.wkniDesmarcar(wkniH.wkniGetId(), wkniD) : wkniDao.wkniMarcar(wkniH.wkniGetId(), wkniD);
            if (!wkniOk) { wkniNotifier.wkniError("No se pudo actualizar el hábito", null); return; }
            if (!wkniHecho) wkniNotifier.wkniSuccess("¡Bien hecho!", wkniH.wkniGetNombre() + " · racha de " + wkniDao.wkniRacha(wkniH.wkniGetId()) + " días");
            else wkniNotifier.wkniInfo("Hábito desmarcado", wkniH.wkniGetNombre());
            wkniRefresh();
        });

        Label wkniN = new Label(wkniH.wkniGetNombre());
        wkniN.getStyleClass().add("row-title");
        String wkniFrec = "SEMANAL".equalsIgnoreCase(wkniH.wkniGetFrecuencia()) ? "Una vez por semana" : "Todos los días";
        Label wkniSub = wkniUi.wkniLabel(wkniH.wkniIsActivo() ? wkniFrec : "En pausa", "row-meta");
        VBox wkniTexts = new VBox(3, wkniN, wkniSub);

        HBox wkniDias = new HBox(8);
        for (int wkniI = 0; wkniI < 7; wkniI++) {
            LocalDate wkniD = wkniLunes.plusDays(wkniI);
            boolean wkniCumplido = wkniSemana.getOrDefault(wkniD.toString(), Set.of()).contains(wkniH.wkniGetId());
            StackPane wkniDot = new StackPane();
            wkniDot.getStyleClass().add("day-dot");
            if (wkniCumplido) {
                wkniDot.getStyleClass().add(wkniD.equals(wkniHoy) ? "day-done-today" : "day-done");
                wkniDot.getChildren().add(wkniIconFactory.wkniIcon("check", 13, wkniD.equals(wkniHoy) ? "tint-on-accent" : "tint-green"));
            } else if (wkniD.isAfter(wkniHoy)) wkniDot.getStyleClass().add("day-future");
            else if (!wkniCalendarioUnificadoService.wkniAplicaHabito(wkniH, wkniD) || !wkniH.wkniIsActivo()) {
                wkniDot.getStyleClass().add("day-off");
                javafx.scene.layout.Region wkni_p = new javafx.scene.layout.Region();
                wkni_p.getStyleClass().add("day-off-dot");
                wkniDot.getChildren().add(wkni_p);
            }
            else if (wkniD.equals(wkniHoy)) wkniDot.getStyleClass().add("day-today");
            else wkniDot.getStyleClass().add("day-missed");
            wkniDias.getChildren().add(wkniDot);
        }
        HBox wkniRachaBox = new HBox(6);
        wkniRachaBox.setAlignment(Pos.CENTER_RIGHT);
        if (wkniRacha > 0) {
            wkniRachaBox.getChildren().addAll(wkniIconFactory.wkniIcon("flame", 17, "tint-orange"),
                wkniUi.wkniLabel(wkniRacha + (wkniRacha == 1 ? " día" : " días"), "streak"));
        } else wkniRachaBox.getChildren().add(wkniUi.wkniLabel(wkniTocaHoy ? "Empieza hoy" : "—", "row-meta"));

        Button wkniMas = wkniUi.wkniIconButton("more", "Más opciones");
        wkniMas.setOnAction(wkniE -> wkniMenu(wkniH).show(wkniMas, javafx.geometry.Side.BOTTOM, 0, 4));

        GridPane wkniG = wkniGrid();
        wkniG.add(wkniCheck, 0, 0);
        wkniG.add(wkniTexts, 1, 0);
        wkniG.add(wkniDias, 2, 0);
        wkniG.add(wkniRachaBox, 3, 0);
        wkniG.add(wkniMas, 4, 0);
        wkniG.getStyleClass().add("habit-row");
        if (!wkniH.wkniIsActivo()) wkniG.getStyleClass().add("row-paused");
        wkniG.setPadding(new Insets(12, 20, 12, 20));
        return wkniG;
    }

    private ContextMenu wkniMenu(wkniHabito wkniH) {
        MenuItem wkniPausa = new MenuItem(wkniH.wkniIsActivo() ? "Pausar hábito" : "Reanudar hábito");
        wkniPausa.setOnAction(wkniE -> {
            wkniH.wkniSetActivo(!wkniH.wkniIsActivo());
            if (wkniDao.wkniActualizar(wkniH)) {
                wkniRefresh();
                wkniNotifier.wkniInfo(wkniH.wkniIsActivo() ? "Hábito reanudado" : "Hábito en pausa", wkniH.wkniGetNombre());
            }
        });
        MenuItem wkniBorrar = new MenuItem("Eliminar");
        wkniBorrar.getStyleClass().add("danger-item");
        wkniBorrar.setOnAction(wkniE -> {
            if (wkniUi.wkniConfirm(wkniView, "¿Eliminar «" + wkniH.wkniGetNombre() + "»?", "También se borrará su historial y su racha.", "Eliminar")
                && wkniDao.wkniEliminar(wkniH.wkniGetId())) {
                wkniRefresh();
                wkniNotifier.wkniDeleted("Hábito eliminado", wkniH.wkniGetNombre());
            }
        });
        return new ContextMenu(wkniPausa, new SeparatorMenuItem(), wkniBorrar);
    }

    private VBox wkniCelda(String wkniTitulo, String wkniValor, String wkniSufijo, String wkni_nota, double wkniProgreso) {
        Label wkniT = wkniUi.wkniLabel(wkniTitulo, "stat-label");
        Label wkniV = wkniUi.wkniLabel(wkniValor, "stat-value");
        HBox wkniVr = new HBox(4, wkniV);
        wkniVr.setAlignment(Pos.BASELINE_LEFT);
        if (!wkniSufijo.isEmpty()) wkniVr.getChildren().add(wkniUi.wkniLabel(wkniSufijo, "stat-suffix"));
        VBox wkniB = new VBox(10, wkniT, wkniVr);
        if (wkniProgreso >= 0) wkniB.getChildren().add(wkniUi.wkniBar(wkniProgreso, "green", 6));
        wkniB.getChildren().add(wkniUi.wkniLabel(wkni_nota, "stat-note"));
        wkniB.getStyleClass().add("summary-cell");
        HBox.setHgrow(wkniB, Priority.ALWAYS);
        wkniB.setMaxWidth(Double.MAX_VALUE);
        return wkniB;
    }

    private VBox wkniCeldaRacha(int wkniRacha, wkniHabito wkniMejor) {
        Label wkniT = wkniUi.wkniLabel("Racha más larga", "stat-label");
        Label wkniV = wkniUi.wkniLabel(String.valueOf(wkniRacha), "stat-value");
        HBox wkniVr = new HBox(6, wkniIconFactory.wkniIcon("flame", 24, "tint-orange"), wkniV, wkniUi.wkniLabel(wkniRacha == 1 ? " día" : " días", "stat-suffix"));
        wkniVr.setAlignment(Pos.CENTER_LEFT);
        VBox wkniB = new VBox(10, wkniT, wkniVr, wkniUi.wkniLabel(wkniMejor == null ? "Marca tus hábitos para empezar." : wkniMejor.wkniGetNombre(), "stat-note"));
        wkniB.getStyleClass().addAll("summary-cell", "with-left-divider");
        HBox.setHgrow(wkniB, Priority.ALWAYS);
        wkniB.setMaxWidth(Double.MAX_VALUE);
        return wkniB;
    }

    private javafx.scene.control.ScrollPane wkniScroll;

    /** La página va dentro de un desplazamiento para ventanas pequeñas o listas largas. */
    public Node wkniGetView() {
        if (wkniScroll == null) wkniScroll = wkniUi.wkniScroll(wkniView);
        return wkniScroll;
    }
}

package com.agendapersonal.ui.modules;

import com.agendapersonal.controller.wkniMainController;
import com.agendapersonal.domain.model.wkniProyecto;
import com.agendapersonal.infrastructure.repo.wkniProyectoDAO;
import com.agendapersonal.model.wkniEstadoTarea;
import com.agendapersonal.model.wkniTarea;
import com.agendapersonal.service.wkniProgresoService;
import com.agendapersonal.ui.wkniAnim;
import com.agendapersonal.ui.wkniIconFactory;
import com.agendapersonal.ui.wkniNotifier;
import com.agendapersonal.ui.wkniTaskViews;
import com.agendapersonal.util.wkniUi;
import javafx.beans.Observable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Tareas y proyectos: proyectos con su avance a la izquierda y tareas agrupadas por fecha a la derecha. */
public final class wkniTareasProyectosView {
    private static final String[] wkniCOLORES = {"#2F5BB7", "#C64B1C", "#2C7A4B", "#6340B8", "#1F6F6A", "#B23A55", "#8A5A00"};
    /** Elemento "Todas las tareas" de la lista de proyectos. */
    private static final wkniProyecto wkniTODAS = new wkniProyecto(-1, "Todas las tareas", "", "Activo", null, null, null);

    private final wkniMainController wkniC;
    private final wkniProyectoDAO wkniDao = new wkniProyectoDAO();
    private final VBox wkniView;
    private final ListView<wkniProyecto> wkniProyectos = new ListView<>();
    private final VBox wkniGrupos = new VBox(0);
    private final TextField wkniBuscar = new TextField();
    private final wkniUi.wkniSegmented wkniFiltro = new wkniUi.wkniSegmented("Todas", "Hoy", "Próximas", "Vencidas", "Completadas");
    private final wkniUi.wkniSegmented wkniEstadoProyectos = new wkniUi.wkniSegmented("Activos", "Todos");
    private boolean wkniVerCompletadas;

    public wkniTareasProyectosView(wkniMainController wkniC) {
        this.wkniC = wkniC;
        VBox wkniPage = wkniUi.wkniPage();
        Button wkniNueva = wkniUi.wkniPrimary("add", "Nueva tarea");
        wkniNueva.setOnAction(wkniE -> wkniTaskViews.wkniForm(null, wkniC, this::wkniFillTareas));
        wkniPage.getChildren().add(wkniUi.wkniPageHeader("tasks", "Tareas",
            "Todo lo que tienes pendiente, ordenado por lista y por fecha.", wkniNueva));

        HBox wkniBody = new HBox(24, wkniPanelProyectos(), wkniPanelTareas());
        VBox.setVgrow(wkniBody, Priority.ALWAYS);
        wkniPage.getChildren().add(wkniBody);
        wkniView = wkniPage;

        wkniC.wkniGetTareas().addListener((Observable wkniO) -> { wkniFillTareas(); wkniProyectos.refresh(); });
        wkniFillProyectos();
    }

    // ------------------------------------------------------------------ proyectos

    private VBox wkniPanelProyectos() {
        Label wkniT = wkniUi.wkniH3("Listas");
        HBox wkniHead = new HBox(8, wkniT, wkniUi.wkniSpacer(), wkniEstadoProyectos.wkniNode());
        wkniHead.setAlignment(Pos.CENTER_LEFT);
        wkniEstadoProyectos.wkniSelectedProperty().addListener((wkniO, wkniA, wkniB) -> wkniFillProyectos());

        wkniProyectos.getStyleClass().addAll("clean-list", "project-list");
        wkniProyectos.setCellFactory(wkniLv -> new wkniProyectoCell());
        wkniProyectos.getSelectionModel().selectedItemProperty().addListener((wkniO, wkniA, wkniB) -> wkniFillTareas());
        VBox.setVgrow(wkniProyectos, Priority.ALWAYS);

        TextField wkniNuevo = new TextField();
        wkniNuevo.getStyleClass().add("bare-field");
        HBox wkniAdd = new HBox(10, wkniIconFactory.wkniIcon("add", 17), wkniNuevo);
        wkniAdd.getStyleClass().add("dashed-input");
        wkniAdd.setAlignment(Pos.CENTER_LEFT);
        wkniNuevo.setPromptText("Nueva lista y Enter");
        HBox.setHgrow(wkniNuevo, Priority.ALWAYS);
        wkniNuevo.setOnAction(wkniE -> {
            String wkniN = wkniNuevo.getText().trim();
            if (wkniN.isEmpty()) { wkniAnim.wkniShake(wkniAdd); return; }
            String wkniColor = wkniCOLORES[wkniDao.wkniListar().size() % wkniCOLORES.length];
            if (wkniDao.wkniInsertar(new wkniProyecto(0, wkniN, "", "Activo", LocalDate.now().toString(), null, wkniColor))) {
                wkniNuevo.clear();
                wkniFillProyectos();
                wkniNotifier.wkniSuccess("Lista creada", "«" + wkniN + "» ya está en tu lista.");
            } else wkniNotifier.wkniError("No se pudo crear la lista", "Revisa la base de datos.");
        });

        VBox wkniCard = wkniUi.wkniCard();
        wkniCard.getStyleClass().add("side-panel");
        wkniCard.setPadding(new Insets(18, 12, 14, 12));
        wkniCard.setPrefWidth(300);
        wkniCard.setMinWidth(250);
        wkniCard.setMaxWidth(320);
        VBox.setMargin(wkniHead, new Insets(0, 6, 0, 6));
        wkniCard.getChildren().addAll(wkniHead, wkniProyectos, wkniAdd);
        return wkniCard;
    }

    private final class wkniProyectoCell extends ListCell<wkniProyecto> {
        wkniProyectoCell() {
            setOnContextMenuRequested(wkniE -> {
                wkniProyecto wkni_p = getItem();
                if (wkni_p != null && wkni_p != wkniTODAS) wkniMenuProyecto(wkni_p).show(this, wkniE.getScreenX(), wkniE.getScreenY());
            });
        }

        @Override protected void updateItem(wkniProyecto wkni_p, boolean wkniEmpty) {
            super.updateItem(wkni_p, wkniEmpty);
            setText(null);
            if (wkniEmpty || wkni_p == null) { setGraphic(null); return; }
            if (wkni_p == wkniTODAS) {
                Label wkniN = new Label(wkni_p.wkniGetNombre());
                wkniN.getStyleClass().add("list-title");
                Label wkniCount = wkniUi.wkniLabel(String.valueOf(wkniC.wkniGetTareas().size()), "list-count");
                HBox wkniRow = new HBox(12, wkniIconFactory.wkniIcon("inbox", 18, "tint-accent"), wkniN, wkniUi.wkniSpacer(), wkniCount);
                wkniRow.setAlignment(Pos.CENTER_LEFT);
                setGraphic(wkniRow);
                return;
            }
            List<wkniTarea> wkniTs = wkniC.wkniGetTareas().stream().filter(wkniT -> wkniPertenece(wkniT, wkni_p)).toList();
            long wkniHechas = wkniTs.stream().filter(wkniTarea::wkniEstaCompletada).count();
            int wkniPct = wkniProgresoService.wkniPorcentaje(wkni_p.wkniGetId(), wkniC.wkniGetTareas());
            StackPane wkniDot = new StackPane();
            wkniDot.getStyleClass().add("dot");
            wkniDot.setStyle("-fx-background-color:" + wkniSafeColor(wkni_p.wkniGetColor()) + ";");
            Label wkniN = new Label(wkni_p.wkniGetNombre());
            wkniN.getStyleClass().add("list-title");
            HBox wkniTitleRow = new HBox(8, wkniN);
            if (!"Activo".equalsIgnoreCase(wkni_p.wkniGetEstado())) wkniTitleRow.getChildren().add(wkniUi.wkniPill(wkni_p.wkniGetEstado(), "slate"));
            wkniTitleRow.setAlignment(Pos.CENTER_LEFT);
            StackPane wkniBar = wkniUi.wkniBar(wkniPct / 100.0, "accent", 4);
            VBox wkniMid = new VBox(8, wkniTitleRow, wkniBar);
            HBox.setHgrow(wkniMid, Priority.ALWAYS);
            Label wkniCount = wkniUi.wkniLabel(wkniHechas + "/" + wkniTs.size(), "list-count");
            HBox wkniRow = new HBox(12, wkniDot, wkniMid, wkniCount);
            wkniRow.setAlignment(Pos.CENTER_LEFT);
            wkniBar.getChildren().getFirst().setStyle("-fx-background-color:" + wkniSafeColor(wkni_p.wkniGetColor()) + ";");
            wkniUi.wkniTooltip(this, wkni_p.wkniGetNombre() + " · " + wkniPct + "% completado · clic derecho para más opciones");
            setGraphic(wkniRow);
        }
    }

    private ContextMenu wkniMenuProyecto(wkniProyecto wkni_p) {
        Menu wkniEstado = new Menu("Cambiar estado");
        for (String wkniE : new String[]{"Activo", "Pausado", "Completado"}) {
            MenuItem wkniMi = new MenuItem(wkniE);
            wkniMi.setOnAction(wkniX -> {
                wkni_p.wkniSetEstado(wkniE);
                if (wkniDao.wkniActualizar(wkni_p)) { wkniFillProyectos(); wkniNotifier.wkniSuccess("Lista actualizada", wkni_p.wkniGetNombre() + " ahora está " + wkniE.toLowerCase(Locale.ROOT) + "."); }
            });
            wkniEstado.getItems().add(wkniMi);
        }
        MenuItem wkniRenombrar = new MenuItem("Renombrar…");
        wkniRenombrar.setOnAction(wkniX -> {
            var wkniD = new javafx.scene.control.TextInputDialog(wkni_p.wkniGetNombre());
            wkniD.setTitle("Compás");
            wkniD.setHeaderText("Nuevo nombre de la lista");
            wkniD.getDialogPane().getStyleClass().add("cs-dialog");
            wkniD.initOwner(wkniView.getScene().getWindow());
            wkniD.showAndWait().map(String::trim).filter(wkniS -> !wkniS.isEmpty()).ifPresent(wkniS -> {
                wkni_p.wkniSetNombre(wkniS);
                if (wkniDao.wkniActualizar(wkni_p)) { wkniFillProyectos(); wkniNotifier.wkniSuccess("Lista renombrada", wkniS); }
            });
        });
        MenuItem wkniBorrar = new MenuItem("Eliminar lista");
        wkniBorrar.getStyleClass().add("danger-item");
        wkniBorrar.setOnAction(wkniX -> {
            if (wkniUi.wkniConfirm(wkniView, "¿Eliminar «" + wkni_p.wkniGetNombre() + "»?",
                "Sus tareas no se borran: quedarán sin lista.", "Eliminar")) {
                if (wkniDao.wkniEliminar(wkni_p.wkniGetId())) {
                    wkniC.wkniCargarTodo();
                    wkniFillProyectos();
                    wkniNotifier.wkniDeleted("Lista eliminada", "«" + wkni_p.wkniGetNombre() + "» se quitó de la lista.");
                }
            }
        });
        return new ContextMenu(wkniEstado, wkniRenombrar, new SeparatorMenuItem(), wkniBorrar);
    }

    private void wkniFillProyectos() {
        wkniProyecto wkniSel = wkniProyectos.getSelectionModel().getSelectedItem();
        List<wkniProyecto> wkniLista = new ArrayList<>();
        wkniLista.add(wkniTODAS);
        for (wkniProyecto wkni_p : wkniDao.wkniListar())
            if (wkniEstadoProyectos.wkniSelected() == 1 || !"Completado".equalsIgnoreCase(wkni_p.wkniGetEstado())) wkniLista.add(wkni_p);
        wkniProyectos.getItems().setAll(wkniLista);
        if (wkniSel != null) for (wkniProyecto wkni_p : wkniLista) if (wkni_p.wkniGetId() == wkniSel.wkniGetId()) { wkniProyectos.getSelectionModel().select(wkni_p); break; }
        if (wkniProyectos.getSelectionModel().getSelectedItem() == null) wkniProyectos.getSelectionModel().selectFirst();
        wkniFillTareas();
    }

    private static String wkniSafeColor(String wkniC) {
        return wkniC != null && wkniC.matches("#[0-9a-fA-F]{6}") ? wkniC : "#2F5BB7";
    }

    private static boolean wkniPertenece(wkniTarea wkniT, wkniProyecto wkni_p) {
        return wkniT.wkniGetProyectoId() != null && wkniT.wkniGetProyectoId() == wkni_p.wkniGetId();
    }

    // ------------------------------------------------------------------ tareas

    private VBox wkniPanelTareas() {
        HBox wkniBuscador = wkniUi.wkniSearchBox(wkniBuscar, "Buscar tareas");
        wkniBuscador.setPrefWidth(260);
        wkniBuscador.setMaxWidth(300);
        wkniBuscar.textProperty().addListener((wkniO, wkniA, wkniB) -> wkniFillTareas());
        wkniFiltro.wkniSelectedProperty().addListener((wkniO, wkniA, wkniB) -> wkniFillTareas());
        HBox wkniToolbar = new HBox(12, wkniFiltro.wkniNode(), wkniUi.wkniSpacer(), wkniBuscador);
        wkniToolbar.setAlignment(Pos.CENTER_LEFT);

        wkniGrupos.getStyleClass().add("list-card");
        ScrollPane wkniSp = new ScrollPane(wkniGrupos);
        wkniSp.setFitToWidth(true);
        wkniSp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        wkniSp.getStyleClass().addAll("card", "card-scroll");
        VBox.setVgrow(wkniSp, Priority.ALWAYS);

        VBox wkniRight = new VBox(16, wkniToolbar, wkniSp);
        HBox.setHgrow(wkniRight, Priority.ALWAYS);
        return wkniRight;
    }

    private void wkniFillTareas() {
        wkniProyecto wkniSel = wkniProyectos.getSelectionModel().getSelectedItem();
        String wkniQ = wkniBuscar.getText() == null ? "" : wkniBuscar.getText().trim().toLowerCase(wkniUi.wkniES);
        LocalDate wkniHoy = LocalDate.now();
        List<wkniTarea> wkniBase = wkniC.wkniGetTareas().stream()
            .filter(wkniT -> wkniSel == null || wkniSel == wkniTODAS || wkniPertenece(wkniT, wkniSel))
            .filter(wkniT -> wkniQ.isEmpty() || wkniT.wkniGetTitulo().toLowerCase(wkniUi.wkniES).contains(wkniQ)
                || (wkniT.wkniGetDescripcion() != null && wkniT.wkniGetDescripcion().toLowerCase(wkniUi.wkniES).contains(wkniQ)))
            .toList();
        long wkniPend = wkniBase.stream().filter(wkniT -> !wkniT.wkniEstaCompletada()).count();
        long wkniNHoy = wkniBase.stream().filter(wkniT -> !wkniT.wkniEstaCompletada() && wkniHoy.equals(wkniT.wkniGetFecha())).count();
        long wkniNProx = wkniBase.stream().filter(wkniT -> !wkniT.wkniEstaCompletada() && wkniT.wkniGetFecha() != null && wkniT.wkniGetFecha().isAfter(wkniHoy)).count();
        long wkniNVenc = wkniBase.stream().filter(wkniT -> !wkniT.wkniEstaCompletada() && wkniT.wkniGetFecha() != null && wkniT.wkniGetFecha().isBefore(wkniHoy)).count();
        long wkniNHechas = wkniBase.size() - wkniPend;
        wkniFiltro.wkniSetText(0, "Todas  " + wkniPend);
        wkniFiltro.wkniSetText(1, "Hoy  " + wkniNHoy);
        wkniFiltro.wkniSetText(2, "Próximas  " + wkniNProx);
        wkniFiltro.wkniSetText(3, "Vencidas  " + wkniNVenc);
        wkniFiltro.wkniSetText(4, "Completadas  " + wkniNHechas);

        Comparator<wkniTarea> wkniOrden = Comparator.comparing((wkniTarea wkniT) -> wkniT.wkniGetFecha() == null ? LocalDate.MAX : wkniT.wkniGetFecha())
            .thenComparing(wkniT -> -wkniT.wkniGetPrioridadTipo().ordinal());
        Map<String, List<wkniTarea>> wkniGrupo = new LinkedHashMap<>();
        int wkniF = wkniFiltro.wkniSelected();
        LocalDate wkniFinSemana = wkniHoy.with(DayOfWeek.SUNDAY);
        for (wkniTarea wkniT : wkniBase.stream().sorted(wkniOrden).toList()) {
            LocalDate wkniD = wkniT.wkniGetFecha();
            if (wkniT.wkniEstaCompletada()) { if (wkniF == 0 || wkniF == 4) wkniGrupo.computeIfAbsent("Completadas", wkniK -> new ArrayList<>()).add(wkniT); continue; }
            if (wkniF == 4) continue;
            String wkniG;
            if (wkniD == null) wkniG = "Sin fecha";
            else if (wkniD.isBefore(wkniHoy)) wkniG = "Vencidas";
            else if (wkniD.equals(wkniHoy)) wkniG = "Hoy";
            else if (wkniD.equals(wkniHoy.plusDays(1))) wkniG = "Mañana";
            else if (!wkniD.isAfter(wkniFinSemana)) wkniG = "Esta semana";
            else wkniG = "Más adelante";
            if (wkniF == 1 && !wkniG.equals("Hoy")) continue;
            if (wkniF == 2 && (wkniG.equals("Hoy") || wkniG.equals("Vencidas"))) continue;
            if (wkniF == 3 && !wkniG.equals("Vencidas")) continue;
            wkniGrupo.computeIfAbsent(wkniG, wkniK -> new ArrayList<>()).add(wkniT);
        }
        // Orden de los grupos: vencidas primero, completadas al final.
        List<String> wkniOrdenGrupos = List.of("Vencidas", "Hoy", "Mañana", "Esta semana", "Más adelante", "Sin fecha", "Completadas");
        wkniGrupos.getChildren().clear();
        if (wkniGrupo.isEmpty()) {
            String wkniMsg = wkniQ.isEmpty() ? (wkniF == 4 ? "Aún no completas tareas aquí." : "No tienes pendientes en esta vista. ¡Buen trabajo!")
                : "Ninguna tarea coincide con «" + wkniQ + "».";
            wkniGrupos.getChildren().add(wkniUi.wkniEmpty("tasks", "Nada por aquí", wkniMsg));
            return;
        }
        List<Node> wkniFilas = new ArrayList<>();
        boolean wkniPrimero = true;
        for (String wkniG : wkniOrdenGrupos) {
            List<wkniTarea> wkniTs = wkniGrupo.get(wkniG);
            if (wkniTs == null) continue;
            HBox wkniHead = new HBox(8, wkniUi.wkniOverline(wkniG), wkniUi.wkniLabel(String.valueOf(wkniTs.size()), "count-pill"));
            wkniHead.setAlignment(Pos.CENTER_LEFT);
            wkniHead.getStyleClass().add("group-head");
            if (wkniG.equals("Vencidas")) wkniHead.getStyleClass().add("group-head-danger");
            if (!wkniPrimero) wkniHead.getStyleClass().add("with-divider");
            wkniPrimero = false;
            wkniGrupos.getChildren().add(wkniHead);
            List<wkniTarea> wkniVisibles = wkniTs;
            if (wkniG.equals("Completadas") && wkniF == 0 && !wkniVerCompletadas && wkniTs.size() > 2) wkniVisibles = wkniTs.subList(0, 2);
            for (wkniTarea wkniT : wkniVisibles) { Node wkniR = wkniFila(wkniT); wkniGrupos.getChildren().add(wkniR); wkniFilas.add(wkniR); }
            if (wkniVisibles.size() < wkniTs.size()) {
                Button wkniMas = wkniUi.wkniGhost("chevron-down", "Ver " + (wkniTs.size() - wkniVisibles.size()) + " completadas más");
                wkniMas.setOnAction(wkniE -> { wkniVerCompletadas = true; wkniFillTareas(); });
                HBox wkniW = new HBox(wkniMas);
                wkniW.setPadding(new Insets(4, 16, 12, 16));
                wkniGrupos.getChildren().add(wkniW);
            } else if (wkniG.equals("Completadas") && wkniVerCompletadas && wkniF == 0 && wkniTs.size() > 2) {
                Button wkniMenos = wkniUi.wkniGhost("chevron-up", "Ocultar completadas");
                wkniMenos.setOnAction(wkniE -> { wkniVerCompletadas = false; wkniFillTareas(); });
                HBox wkniW = new HBox(wkniMenos);
                wkniW.setPadding(new Insets(4, 16, 12, 16));
                wkniGrupos.getChildren().add(wkniW);
            }
        }
        wkniAnim.wkniStagger(wkniFilas);
    }

    private Node wkniFila(wkniTarea wkniT) {
        boolean wkniHecha = wkniT.wkniEstaCompletada();
        Button wkniCheck = new Button();
        wkniCheck.getStyleClass().addAll("check-circle");
        if (wkniHecha) wkniCheck.getStyleClass().add("checked");
        wkniCheck.setGraphic(wkniIconFactory.wkniIcon("check", 14, "tint-on-accent"));
        wkniCheck.setAccessibleText(wkniHecha ? "Marcar como pendiente" : "Completar tarea");
        wkniUi.wkniTooltip(wkniCheck, wkniHecha ? "Marcar como pendiente" : "Completar");
        wkniCheck.setOnAction(wkniE -> {
            wkniAnim.wkniPop(wkniCheck);
            wkniC.wkniAlternarCompletada(wkniT).ifPresentOrElse(wkniR -> {
                if (wkniR.wkniCompletada()) wkniNotifier.wkniSuccess(wkniC.wkniMensajeProgreso(wkniR), wkniT.wkniGetTitulo());
                else wkniNotifier.wkniInfo(wkniC.wkniMensajeProgreso(wkniR), wkniT.wkniGetTitulo());
            }, () -> wkniNotifier.wkniError("No se pudo actualizar la tarea", null));
        });

        Label wkniTitulo = new Label(wkniT.wkniGetTitulo());
        wkniTitulo.getStyleClass().add("row-title");
        if (wkniHecha) wkniTitulo.getStyleClass().add("done");
        // Viñeta de importancia (rojo = alta, naranja = media, verde = baja) para distinguir de un vistazo.
        StackPane wkniVineta = new StackPane();
        wkniVineta.getStyleClass().addAll("dot");
        wkniVineta.setStyle("-fx-background-color: -" + wkniT.wkniGetPrioridadTipo().wkniTono() + ";");
        wkniUi.wkniTooltip(wkniTitulo, "Importancia " + wkniT.wkniGetPrioridad().toLowerCase());
        wkniTitulo.setGraphic(wkniVineta);
        wkniTitulo.setGraphicTextGap(8);
        HBox wkniMeta = new HBox(14);
        wkniMeta.setAlignment(Pos.CENTER_LEFT);
        if (wkniT.wkniGetProyectoId() != null) {
            for (wkniProyecto wkni_p : wkniProyectos.getItems()) if (wkni_p.wkniGetId() == wkniT.wkniGetProyectoId()) {
                StackPane wkniDot = new StackPane();
                wkniDot.getStyleClass().addAll("dot", "dot-small");
                wkniDot.setStyle("-fx-background-color:" + wkniSafeColor(wkni_p.wkniGetColor()) + ";");
                Label wkniN = wkniUi.wkniLabel(wkni_p.wkniGetNombre(), "row-meta");
                wkniN.setGraphic(wkniDot);
                wkniN.setGraphicTextGap(6);
                wkniMeta.getChildren().add(wkniN);
            }
        }
        Label wkni_fecha = wkniUi.wkniLabel(wkniHecha ? "Completada" : wkniUi.wkniFriendlyDate(wkniT.wkniGetFecha()), "row-meta");
        wkni_fecha.setGraphic(wkniIconFactory.wkniIcon("calendar", 13));
        wkni_fecha.setGraphicTextGap(6);
        if (!wkniHecha && wkniT.wkniGetFecha() != null && wkniT.wkniGetFecha().isBefore(LocalDate.now())) wkni_fecha.getStyleClass().add("text-danger");
        if (wkniT.wkniGetHora() != null) wkni_fecha.setText(wkni_fecha.getText() + " · " + wkniT.wkniGetHora());
        wkniMeta.getChildren().add(wkni_fecha);
        if (wkniT.wkniGetEventoId() != null) wkniMeta.getChildren().add(wkniUi.wkniLabel("Evento vinculado", "row-meta"));
        if (wkniT.wkniGetBloqueId() != null) wkniMeta.getChildren().add(wkniUi.wkniLabel("Bloque vinculado", "row-meta"));
        if (wkniT.wkniGetEstadoTipo() == wkniEstadoTarea.EN_PROGRESO) wkniMeta.getChildren().add(wkniUi.wkniPill("En progreso", "blue"));
        VBox wkniTexts = new VBox(5, wkniTitulo, wkniMeta);
        HBox.setHgrow(wkniTexts, Priority.ALWAYS);

        String wkni_tono = wkniT.wkniGetPrioridadTipo().wkniTono();
        Label wkniPri = wkniUi.wkniPill(wkniT.wkniGetPrioridad(), wkni_tono);
        Button wkniMas = wkniUi.wkniIconButton("more", "Más opciones");
        wkniMas.setOnAction(wkniE -> wkniMenuTarea(wkniT).show(wkniMas, javafx.geometry.Side.BOTTOM, 0, 4));

        Button wkniEstrella = wkniUi.wkniIconButton(wkniT.wkniIsDestacada() ? "favorite-on" : "favorite", wkniT.wkniIsDestacada() ? "Quitar destacado" : "Destacar");
        wkniEstrella.setOnAction(wkniE -> { wkniT.wkniSetDestacada(!wkniT.wkniIsDestacada()); wkniC.wkniActualizarTarea(wkniT); });
        HBox wkniRow = new HBox(12, wkniCheck, wkniTexts, wkniPri, wkniEstrella, wkniMas);
        wkniRow.getStyleClass().add("list-row");
        if (wkniHecha) wkniRow.getStyleClass().add("row-done");
        wkniRow.setAlignment(Pos.CENTER_LEFT);
        wkniRow.setOnMouseClicked(wkniE -> { if (wkniE.getClickCount() == 2) wkniTaskViews.wkniForm(wkniT, wkniC, this::wkniFillTareas); });
        return wkniRow;
    }

    private ContextMenu wkniMenuTarea(wkniTarea wkniT) {
        MenuItem wkniEditar = new MenuItem("Editar…");
        wkniEditar.setOnAction(wkniE -> wkniTaskViews.wkniForm(wkniT, wkniC, this::wkniFillTareas));
        Menu wkniEstado = new Menu("Estado");
        for (wkniEstadoTarea wkniEs : wkniEstadoTarea.values()) {
            MenuItem wkniMi = new MenuItem(wkniEs.wkniEtiqueta());
            wkniMi.setDisable(wkniEs == wkniT.wkniGetEstadoTipo());
            wkniMi.setOnAction(wkniE -> wkniC.wkniCambiarEstadoTarea(wkniT, wkniEs).ifPresent(wkniR ->
                wkniNotifier.wkniSuccess("Estado actualizado", wkniT.wkniGetTitulo() + " → " + wkniEs.wkniEtiqueta())));
            wkniEstado.getItems().add(wkniMi);
        }
        MenuItem wkniBorrar = new MenuItem("Eliminar");
        wkniBorrar.getStyleClass().add("danger-item");
        wkniBorrar.setOnAction(wkniE -> {
            if (wkniUi.wkniConfirm(wkniView, "¿Eliminar esta tarea?", "«" + wkniT.wkniGetTitulo() + "» se borrará para siempre.", "Eliminar")
                && wkniC.wkniEliminarTarea(wkniT.wkniGetId()))
                wkniNotifier.wkniDeleted("Tarea eliminada", wkniT.wkniGetTitulo());
        });
        return new ContextMenu(wkniEditar, wkniEstado, new SeparatorMenuItem(), wkniBorrar);
    }

    public VBox wkniGetView() { return wkniView; }
}

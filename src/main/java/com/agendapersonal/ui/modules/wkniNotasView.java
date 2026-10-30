package com.agendapersonal.ui.modules;

import com.agendapersonal.domain.model.wkniNota;
import com.agendapersonal.infrastructure.repo.wkniNotaDAO;
import com.agendapersonal.ui.wkniAnim;
import com.agendapersonal.ui.wkniIconFactory;
import com.agendapersonal.ui.wkniNotifier;
import com.agendapersonal.util.wkniUi;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.List;

/** Notas e ideas: captura rápida arriba, lista con filtros a la izquierda y editor a la derecha. */
public class wkniNotasView {
    private final wkniNotaDAO wkniDao = new wkniNotaDAO();
    private final VBox wkniView;
    private final ListView<wkniNota> wkniList = new ListView<>();
    private final TextField wkniTitulo = new TextField();
    private final TextArea wkniContenido = new TextArea();
    private final TextField wkniEtiquetas = new TextField();
    private final ComboBox<String> wkni_tipo = new ComboBox<>();
    private final Button wkniFav = wkniUi.wkniIconButton("favorite", "Marcar como favorita");
    private final Label wkniEstado = wkniUi.wkniLabel("", "caption");
    private final TextField wkniBuscar = new TextField();
    private final wkniUi.wkniSegmented wkniFiltro = new wkniUi.wkniSegmented("Todas", "Notas", "Ideas", "Favoritas");
    private boolean wkniFavorita;
    private boolean wkniCargando;
    private boolean wkniSucio;
    private wkniNota wkniActual;

    public wkniNotasView() { this(null); }

    public wkniNotasView(String wkniFiltroInicial) {
        VBox wkniPage = wkniUi.wkniPage();
        Button wkniNueva = wkniUi.wkniPrimary("add", "Nueva nota");
        wkniNueva.setOnAction(wkniE -> wkniNueva());
        wkniPage.getChildren().add(wkniUi.wkniPageHeader("notes", "Notas",
            "Captura ideas al vuelo y encuéntralas con etiquetas.", wkniNueva));

        // Captura rápida de idea
        TextField wkniIdea = new TextField();
        wkniIdea.getStyleClass().add("bare-field");
        wkniIdea.setPromptText("Captura una idea y pulsa Enter…");
        HBox.setHgrow(wkniIdea, Priority.ALWAYS);
        Label wkniEnter = wkniUi.wkniLabel("Enter", "kbd");
        HBox wkniCaptura = new HBox(12, wkniIconFactory.wkniIcon("idea", 20, "tint-amber"), wkniIdea, wkniEnter);
        wkniCaptura.getStyleClass().add("capture-bar");
        wkniCaptura.setAlignment(Pos.CENTER_LEFT);
        wkniCaptura.setOnMouseClicked(wkniE -> wkniIdea.requestFocus());
        wkniIdea.setOnAction(wkniE -> {
            String wkniTxt = wkniIdea.getText().trim();
            if (wkniTxt.isEmpty()) { wkniAnim.wkniShake(wkniCaptura); return; }
            String wkniHoy = LocalDate.now().toString();
            if (wkniDao.wkniInsertar(new wkniNota(0, wkniTxt, "", "idea", "IDEA", wkniHoy, wkniHoy, false))) {
                wkniIdea.clear();
                wkniFill();
                wkniNotifier.wkniSuccess("Idea guardada", wkniTxt);
            } else wkniNotifier.wkniError("No se pudo guardar la idea", null);
        });

        HBox wkniBody = new HBox(24, wkniPanelLista(wkniFiltroInicial), wkniPanelEditor());
        VBox.setVgrow(wkniBody, Priority.ALWAYS);
        wkniPage.getChildren().addAll(wkniCaptura, wkniBody);
        wkniView = wkniPage;
        wkniFill();
        wkniList.getSelectionModel().selectFirst();
        if (wkniList.getItems().isEmpty()) wkniNueva();
    }

    private VBox wkniPanelLista(String wkniFiltroInicial) {
        HBox wkniBusq = wkniUi.wkniSearchBox(wkniBuscar, "Buscar notas o etiquetas");
        wkniBusq.getStyleClass().add("search-soft");
        if (wkniFiltroInicial != null) wkniBuscar.setText(wkniFiltroInicial);
        wkniBuscar.textProperty().addListener((wkniO, wkniA, wkniB) -> wkniFill());
        wkniFiltro.wkniSelectedProperty().addListener((wkniO, wkniA, wkniB) -> wkniFill());
        wkniFiltro.wkniFillWidth();
        wkniList.getStyleClass().addAll("clean-list", "note-list");
        wkniList.setCellFactory(wkniLv -> new wkniNotaCell());
        wkniList.setPlaceholder(wkniUi.wkniEmpty("notes", "Sin notas", "Escribe una idea arriba o crea una nota nueva."));
        wkniList.getSelectionModel().selectedItemProperty().addListener((wkniO, wkniA, wkniB) -> { if (wkniB != null) wkniMostrar(wkniB); });
        VBox.setVgrow(wkniList, Priority.ALWAYS);
        VBox wkniCard = wkniUi.wkniCard();
        wkniCard.getStyleClass().add("side-panel");
        wkniCard.setPadding(new Insets(16, 10, 10, 10));
        wkniCard.setPrefWidth(340);
        wkniCard.setMinWidth(270);
        wkniCard.setMaxWidth(380);
        VBox wkniTop = new VBox(12, wkniBusq, wkniFiltro.wkniNode());
        wkniTop.setPadding(new Insets(0, 6, 4, 6));
        wkniCard.getChildren().addAll(wkniTop, wkniList);
        return wkniCard;
    }

    private final class wkniNotaCell extends ListCell<wkniNota> {
        @Override protected void updateItem(wkniNota wkniN, boolean wkniEmpty) {
            super.updateItem(wkniN, wkniEmpty);
            setText(null);
            if (wkniEmpty || wkniN == null) { setGraphic(null); return; }
            Label wkniT = new Label(wkniN.wkniGetTitulo());
            wkniT.getStyleClass().add("list-title");
            HBox wkniHead = new HBox(6, wkniT);
            wkniHead.setAlignment(Pos.CENTER_LEFT);
            if (wkniN.wkniIsFavorita()) { Region wkniSp = wkniUi.wkniSpacer(); wkniHead.getChildren().addAll(wkniSp, wkniIconFactory.wkniIcon("favorite", 15, "tint-amber", "icon-fill")); }
            String wkniPrev = wkniN.wkniGetContenido() == null ? "" : wkniN.wkniGetContenido().replace('\n', ' ').trim();
            Label wkni_p = new Label(wkniPrev.isEmpty() ? "Sin contenido todavía" : wkniPrev);
            wkni_p.getStyleClass().add("list-sub");
            HBox wkniTags = new HBox(6);
            wkniTags.setAlignment(Pos.CENTER_LEFT);
            wkniTags.getChildren().add(wkniUi.wkniPill("IDEA".equalsIgnoreCase(wkniN.wkniGetTipo()) ? "idea" : "nota",
                "IDEA".equalsIgnoreCase(wkniN.wkniGetTipo()) ? "amber" : "slate"));
            for (String wkniTag : wkniEtiquetasDe(wkniN).stream().limit(2).toList()) wkniTags.getChildren().add(wkniUi.wkniPill(wkniTag, "slate"));
            Label wkni_fecha = wkniUi.wkniLabel(wkniCuando(wkniN), "caption");
            HBox wkniFoot = new HBox(6, wkniTags, wkniUi.wkniSpacer(), wkni_fecha);
            wkniFoot.setAlignment(Pos.CENTER_LEFT);
            VBox wkniBox = new VBox(6, wkniHead, wkni_p, wkniFoot);
            wkniBox.setMaxWidth(Double.MAX_VALUE);
            wkni_p.setMaxWidth(290);
            setGraphic(wkniBox);
        }
    }

    private static List<String> wkniEtiquetasDe(wkniNota wkniN) {
        if (wkniN.wkniGetEtiquetas() == null || wkniN.wkniGetEtiquetas().isBlank()) return List.of();
        return java.util.Arrays.stream(wkniN.wkniGetEtiquetas().split(","))
            .map(String::trim).filter(wkniS -> !wkniS.isEmpty() && !wkniS.equalsIgnoreCase("idea")).toList();
    }

    private static String wkniCuando(wkniNota wkniN) {
        String wkniF = wkniN.wkniGetFechaUpdate() != null ? wkniN.wkniGetFechaUpdate() : wkniN.wkniGetFechaCreacion();
        try { return wkniUi.wkniFriendlyDate(LocalDate.parse(wkniF)); } catch (Exception wkniE) { return ""; }
    }

    private VBox wkniPanelEditor() {
        wkni_tipo.getItems().addAll("NOTA", "IDEA");
        wkni_tipo.setValue("NOTA");
        wkni_tipo.getStyleClass().add("compact-combo");
        wkniFav.setOnAction(wkniE -> { wkniSetFavorita(!wkniFavorita); wkniAnim.wkniPop(wkniFav); wkniMarcarSucio(); });
        HBox wkniTop = new HBox(10, wkni_tipo, wkniEstado, wkniUi.wkniSpacer(), wkniFav);
        wkniTop.setAlignment(Pos.CENTER_LEFT);

        wkniTitulo.setPromptText("Título de la nota");
        wkniTitulo.getStyleClass().addAll("bare-field", "title-field");
        HBox wkniTagRow = new HBox(10, wkniIconFactory.wkniIcon("tag", 16), wkniEtiquetas);
        wkniTagRow.setAlignment(Pos.CENTER_LEFT);
        wkniEtiquetas.setPromptText("etiquetas separadas por coma (trabajo, libros…)");
        wkniEtiquetas.getStyleClass().add("bare-field");
        HBox.setHgrow(wkniEtiquetas, Priority.ALWAYS);
        Region wkniLinea = new Region();
        wkniLinea.getStyleClass().add("hairline");

        wkniContenido.setPromptText("Escribe aquí tus ideas, enlaces, listas…");
        wkniContenido.setWrapText(true);
        wkniContenido.getStyleClass().add("editor-area");
        VBox.setVgrow(wkniContenido, Priority.ALWAYS);

        Button wkniBorrar = wkniUi.wkniGhost("delete", "Eliminar");
        wkniBorrar.getStyleClass().add("text-danger-btn");
        wkniBorrar.setOnAction(wkniE -> wkniBorrar());
        Button wkniGuardar = wkniUi.wkniPrimary("check", "Guardar");
        wkniGuardar.setOnAction(wkniE -> wkniGuardar());
        Label wkniAtajo = wkniUi.wkniLabel("Ctrl+S", "kbd");
        HBox wkniFoot = new HBox(10, wkniBorrar, wkniUi.wkniSpacer(), wkniAtajo, wkniGuardar);
        wkniFoot.setAlignment(Pos.CENTER_LEFT);
        wkniFoot.getStyleClass().add("card-footer");

        for (var wkniTf : List.of(wkniTitulo.textProperty(), wkniContenido.textProperty(), wkniEtiquetas.textProperty()))
            wkniTf.addListener((wkniO, wkniA, wkniB) -> wkniMarcarSucio());
        wkni_tipo.setOnAction(wkniE -> wkniMarcarSucio());

        VBox wkniCard = wkniUi.wkniCard();
        wkniCard.getStyleClass().add("editor-card");
        wkniCard.setPadding(new Insets(20, 28, 18, 28));
        wkniCard.setSpacing(14);
        HBox.setHgrow(wkniCard, Priority.ALWAYS);
        wkniCard.getChildren().addAll(wkniTop, wkniTitulo, wkniTagRow, wkniLinea, wkniContenido, wkniFoot);
        wkniCard.sceneProperty().addListener((wkniO, wkniA, wkniS) -> {
            if (wkniS != null) wkniS.getAccelerators().putIfAbsent(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), () -> {
                if (wkniCard.getScene() != null && wkniCard.isVisible() && wkniCard.getParent() != null && wkniView.getParent() != null) wkniGuardar();
            });
        });
        return wkniCard;
    }

    private void wkniSetFavorita(boolean wkniOn) {
        wkniFavorita = wkniOn;
        wkniFav.setGraphic(wkniIconFactory.wkniIcon("favorite", 18, wkniOn ? "tint-amber" : "tint-none", wkniOn ? "icon-fill" : "no-fill"));
        wkniFav.getStyleClass().removeAll("fav-on");
        if (wkniOn) wkniFav.getStyleClass().add("fav-on");
        wkniUi.wkniTooltip(wkniFav, wkniOn ? "Quitar de favoritas" : "Marcar como favorita");
    }

    private void wkniMarcarSucio() {
        if (wkniCargando) return;
        wkniSucio = true;
        wkniEstado.setText("Cambios sin guardar");
        wkniEstado.getStyleClass().setAll("caption", "text-warning");
    }

    private void wkniNueva() {
        wkniCargando = true;
        wkniActual = null;
        wkniList.getSelectionModel().clearSelection();
        wkniTitulo.clear(); wkniContenido.clear(); wkniEtiquetas.clear();
        wkni_tipo.setValue("NOTA");
        wkniSetFavorita(false);
        wkniCargando = false;
        wkniSucio = false;
        wkniEstado.setText("Nota nueva");
        wkniEstado.getStyleClass().setAll("caption");
        wkniTitulo.requestFocus();
    }

    private void wkniGuardar() {
        if (wkniTitulo.getText().isBlank()) { wkniAnim.wkniShake(wkniTitulo); wkniNotifier.wkniWarning("Falta el título", "Ponle un título a la nota para guardarla."); return; }
        String wkniHoy = LocalDate.now().toString();
        boolean wkniOk;
        boolean wkniEsNueva = wkniActual == null;
        if (wkniEsNueva) {
            wkniOk = wkniDao.wkniInsertar(new wkniNota(0, wkniTitulo.getText().trim(), wkniContenido.getText(), wkniEtiquetas.getText(),
                wkni_tipo.getValue(), wkniHoy, wkniHoy, wkniFavorita));
        } else {
            wkniActual.wkniSetTitulo(wkniTitulo.getText().trim());
            wkniActual.wkniSetContenido(wkniContenido.getText());
            wkniActual.wkniSetEtiquetas(wkniEtiquetas.getText());
            wkniActual.wkniSetTipo(wkni_tipo.getValue());
            wkniActual.wkniSetFechaUpdate(wkniHoy);
            wkniActual.wkniSetFavorita(wkniFavorita);
            wkniOk = wkniDao.wkniActualizar(wkniActual);
        }
        if (!wkniOk) { wkniNotifier.wkniError("No se pudo guardar la nota", null); return; }
        String wkniT = wkniTitulo.getText().trim();
        wkniFill();
        // vuelve a seleccionar la nota guardada
        for (wkniNota wkniN : wkniList.getItems()) if (wkniN.wkniGetTitulo().equals(wkniT)) { wkniList.getSelectionModel().select(wkniN); break; }
        wkniSucio = false;
        wkniEstado.setText("Guardado");
        wkniEstado.getStyleClass().setAll("caption", "text-success");
        wkniNotifier.wkniSuccess(wkniEsNueva ? "Nota creada" : "Nota guardada", wkniT);
    }

    private void wkniBorrar() {
        if (wkniActual == null) { wkniNueva(); return; }
        if (!wkniUi.wkniConfirm(wkniView, "¿Eliminar esta nota?", "«" + wkniActual.wkniGetTitulo() + "» se borrará para siempre.", "Eliminar")) return;
        String wkniT = wkniActual.wkniGetTitulo();
        if (wkniDao.wkniEliminar(wkniActual.wkniGetId())) {
            wkniFill();
            wkniNueva();
            wkniList.getSelectionModel().selectFirst();
            wkniNotifier.wkniDeleted("Nota eliminada", wkniT);
        }
    }

    private void wkniFill() {
        String wkniQ = wkniBuscar.getText() == null ? "" : wkniBuscar.getText().trim().toLowerCase(wkniUi.wkniES);
        int wkniF = wkniFiltro.wkniSelected();
        List<wkniNota> wkniTodas = wkniDao.wkniListar(null);
        wkniNota wkniSel = wkniList.getSelectionModel().getSelectedItem();
        wkniList.getItems().setAll(wkniTodas.stream()
            .filter(wkniN -> wkniF == 0 || (wkniF == 1 && !"IDEA".equalsIgnoreCase(wkniN.wkniGetTipo()))
                || (wkniF == 2 && "IDEA".equalsIgnoreCase(wkniN.wkniGetTipo())) || (wkniF == 3 && wkniN.wkniIsFavorita()))
            .filter(wkniN -> wkniQ.isEmpty() || wkniContiene(wkniN.wkniGetTitulo(), wkniQ) || wkniContiene(wkniN.wkniGetContenido(), wkniQ) || wkniContiene(wkniN.wkniGetEtiquetas(), wkniQ))
            .toList());
        if (wkniSel != null) for (wkniNota wkniN : wkniList.getItems()) if (wkniN.wkniGetId() == wkniSel.wkniGetId()) { wkniList.getSelectionModel().select(wkniN); break; }
    }

    private static boolean wkniContiene(String wkniS, String wkniQ) { return wkniS != null && wkniS.toLowerCase(wkniUi.wkniES).contains(wkniQ); }

    private void wkniMostrar(wkniNota wkniN) {
        if (wkniSucio && wkniActual != null && wkniActual.wkniGetId() != wkniN.wkniGetId())
            wkniNotifier.wkniWarning("Había cambios sin guardar", "Se descartaron los cambios de «" + wkniActual.wkniGetTitulo() + "».");
        wkniCargando = true;
        wkniActual = wkniN;
        wkniTitulo.setText(wkniN.wkniGetTitulo());
        wkniContenido.setText(wkniN.wkniGetContenido() == null ? "" : wkniN.wkniGetContenido());
        wkniEtiquetas.setText(wkniN.wkniGetEtiquetas() == null ? "" : wkniN.wkniGetEtiquetas());
        wkni_tipo.setValue(wkniN.wkniGetTipo() == null ? "NOTA" : wkniN.wkniGetTipo().toUpperCase(wkniUi.wkniES));
        wkniSetFavorita(wkniN.wkniIsFavorita());
        wkniCargando = false;
        wkniSucio = false;
        wkniEstado.setText("Editado " + wkniCuando(wkniN).toLowerCase(wkniUi.wkniES));
        wkniEstado.getStyleClass().setAll("caption");
    }

    public VBox wkniGetView() { return wkniView; }
}

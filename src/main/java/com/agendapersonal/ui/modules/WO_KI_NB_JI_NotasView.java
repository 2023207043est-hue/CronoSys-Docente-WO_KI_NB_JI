package com.agendapersonal.ui.modules;

import com.agendapersonal.domain.model.WO_KI_NB_JI_Nota;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_NotaDAO;
import com.agendapersonal.ui.WO_KI_NB_JI_Anim;
import com.agendapersonal.ui.WO_KI_NB_JI_IconFactory;
import com.agendapersonal.ui.WO_KI_NB_JI_Notifier;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
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
public class WO_KI_NB_JI_NotasView {
    private final WO_KI_NB_JI_NotaDAO dao = new WO_KI_NB_JI_NotaDAO();
    private final VBox view;
    private final ListView<WO_KI_NB_JI_Nota> list = new ListView<>();
    private final TextField titulo = new TextField();
    private final TextArea contenido = new TextArea();
    private final TextField etiquetas = new TextField();
    private final ComboBox<String> tipo = new ComboBox<>();
    private final Button fav = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("favorite", "Marcar como favorita");
    private final Label estado = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("", "caption");
    private final TextField buscar = new TextField();
    private final WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented filtro = new WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented("Todas", "Notas", "Ideas", "Favoritas");
    private boolean favorita;
    private boolean cargando;
    private boolean sucio;
    private WO_KI_NB_JI_Nota actual;

    public WO_KI_NB_JI_NotasView() { this(null); }

    public WO_KI_NB_JI_NotasView(String filtroInicial) {
        VBox page = WO_KI_NB_JI_Ui.WO_KI_NB_JI_page();
        Button nueva = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("add", "Nueva nota");
        nueva.setOnAction(e -> WO_KI_NB_JI_nueva());
        page.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pageHeader("notes", "Notas",
            "Captura ideas al vuelo y encuéntralas con etiquetas.", nueva));

        // Captura rápida de idea
        TextField idea = new TextField();
        idea.getStyleClass().add("bare-field");
        idea.setPromptText("Captura una idea y pulsa Enter…");
        HBox.setHgrow(idea, Priority.ALWAYS);
        Label enter = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("Enter", "kbd");
        HBox captura = new HBox(12, WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("idea", 20, "tint-amber"), idea, enter);
        captura.getStyleClass().add("capture-bar");
        captura.setAlignment(Pos.CENTER_LEFT);
        captura.setOnMouseClicked(e -> idea.requestFocus());
        idea.setOnAction(e -> {
            String txt = idea.getText().trim();
            if (txt.isEmpty()) { WO_KI_NB_JI_Anim.WO_KI_NB_JI_shake(captura); return; }
            String hoy = LocalDate.now().toString();
            if (dao.WO_KI_NB_JI_insertar(new WO_KI_NB_JI_Nota(0, txt, "", "idea", "IDEA", hoy, hoy, false))) {
                idea.clear();
                WO_KI_NB_JI_fill();
                WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success("Idea guardada", txt);
            } else WO_KI_NB_JI_Notifier.WO_KI_NB_JI_error("No se pudo guardar la idea", null);
        });

        HBox body = new HBox(24, WO_KI_NB_JI_panelLista(filtroInicial), WO_KI_NB_JI_panelEditor());
        VBox.setVgrow(body, Priority.ALWAYS);
        page.getChildren().addAll(captura, body);
        view = page;
        WO_KI_NB_JI_fill();
        list.getSelectionModel().selectFirst();
        if (list.getItems().isEmpty()) WO_KI_NB_JI_nueva();
    }

    private VBox WO_KI_NB_JI_panelLista(String filtroInicial) {
        HBox busq = WO_KI_NB_JI_Ui.WO_KI_NB_JI_searchBox(buscar, "Buscar notas o etiquetas");
        busq.getStyleClass().add("search-soft");
        if (filtroInicial != null) buscar.setText(filtroInicial);
        buscar.textProperty().addListener((o, a, b) -> WO_KI_NB_JI_fill());
        filtro.WO_KI_NB_JI_selectedProperty().addListener((o, a, b) -> WO_KI_NB_JI_fill());
        filtro.WO_KI_NB_JI_fillWidth();
        list.getStyleClass().addAll("clean-list", "note-list");
        list.setCellFactory(lv -> new WO_KI_NB_JI_NotaCell());
        list.setPlaceholder(WO_KI_NB_JI_Ui.WO_KI_NB_JI_empty("notes", "Sin notas", "Escribe una idea arriba o crea una nota nueva."));
        list.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> { if (b != null) WO_KI_NB_JI_mostrar(b); });
        VBox.setVgrow(list, Priority.ALWAYS);
        VBox card = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        card.getStyleClass().add("side-panel");
        card.setPadding(new Insets(16, 10, 10, 10));
        card.setPrefWidth(340);
        card.setMinWidth(270);
        card.setMaxWidth(380);
        VBox top = new VBox(12, busq, filtro.WO_KI_NB_JI_node());
        top.setPadding(new Insets(0, 6, 4, 6));
        card.getChildren().addAll(top, list);
        return card;
    }

    private final class WO_KI_NB_JI_NotaCell extends ListCell<WO_KI_NB_JI_Nota> {
        @Override protected void updateItem(WO_KI_NB_JI_Nota n, boolean empty) {
            super.updateItem(n, empty);
            setText(null);
            if (empty || n == null) { setGraphic(null); return; }
            Label t = new Label(n.WO_KI_NB_JI_getTitulo());
            t.getStyleClass().add("list-title");
            HBox head = new HBox(6, t);
            head.setAlignment(Pos.CENTER_LEFT);
            if (n.WO_KI_NB_JI_isFavorita()) { Region sp = WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(); head.getChildren().addAll(sp, WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("favorite", 15, "tint-amber", "icon-fill")); }
            String prev = n.WO_KI_NB_JI_getContenido() == null ? "" : n.WO_KI_NB_JI_getContenido().replace('\n', ' ').trim();
            Label p = new Label(prev.isEmpty() ? "Sin contenido todavía" : prev);
            p.getStyleClass().add("list-sub");
            HBox tags = new HBox(6);
            tags.setAlignment(Pos.CENTER_LEFT);
            tags.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pill("IDEA".equalsIgnoreCase(n.WO_KI_NB_JI_getTipo()) ? "idea" : "nota",
                "IDEA".equalsIgnoreCase(n.WO_KI_NB_JI_getTipo()) ? "amber" : "slate"));
            for (String tag : WO_KI_NB_JI_etiquetasDe(n).stream().limit(2).toList()) tags.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pill(tag, "slate"));
            Label fecha = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(WO_KI_NB_JI_cuando(n), "caption");
            HBox foot = new HBox(6, tags, WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), fecha);
            foot.setAlignment(Pos.CENTER_LEFT);
            VBox box = new VBox(6, head, p, foot);
            box.setMaxWidth(Double.MAX_VALUE);
            p.setMaxWidth(290);
            setGraphic(box);
        }
    }

    private static List<String> WO_KI_NB_JI_etiquetasDe(WO_KI_NB_JI_Nota n) {
        if (n.WO_KI_NB_JI_getEtiquetas() == null || n.WO_KI_NB_JI_getEtiquetas().isBlank()) return List.of();
        return java.util.Arrays.stream(n.WO_KI_NB_JI_getEtiquetas().split(","))
            .map(String::trim).filter(s -> !s.isEmpty() && !s.equalsIgnoreCase("idea")).toList();
    }

    private static String WO_KI_NB_JI_cuando(WO_KI_NB_JI_Nota n) {
        String f = n.WO_KI_NB_JI_getFechaUpdate() != null ? n.WO_KI_NB_JI_getFechaUpdate() : n.WO_KI_NB_JI_getFechaCreacion();
        try { return WO_KI_NB_JI_Ui.WO_KI_NB_JI_friendlyDate(LocalDate.parse(f)); } catch (Exception e) { return ""; }
    }

    private VBox WO_KI_NB_JI_panelEditor() {
        tipo.getItems().addAll("NOTA", "IDEA");
        tipo.setValue("NOTA");
        tipo.getStyleClass().add("compact-combo");
        fav.setOnAction(e -> { WO_KI_NB_JI_setFavorita(!favorita); WO_KI_NB_JI_Anim.WO_KI_NB_JI_pop(fav); WO_KI_NB_JI_marcarSucio(); });
        HBox top = new HBox(10, tipo, estado, WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), fav);
        top.setAlignment(Pos.CENTER_LEFT);

        titulo.setPromptText("Título de la nota");
        titulo.getStyleClass().addAll("bare-field", "title-field");
        HBox tagRow = new HBox(10, WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("tag", 16), etiquetas);
        tagRow.setAlignment(Pos.CENTER_LEFT);
        etiquetas.setPromptText("etiquetas separadas por coma (trabajo, libros…)");
        etiquetas.getStyleClass().add("bare-field");
        HBox.setHgrow(etiquetas, Priority.ALWAYS);
        Region linea = new Region();
        linea.getStyleClass().add("hairline");

        contenido.setPromptText("Escribe aquí tus ideas, enlaces, listas…");
        contenido.setWrapText(true);
        contenido.getStyleClass().add("editor-area");
        VBox.setVgrow(contenido, Priority.ALWAYS);

        Button borrar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_ghost("delete", "Eliminar");
        borrar.getStyleClass().add("text-danger-btn");
        borrar.setOnAction(e -> WO_KI_NB_JI_borrar());
        Button guardar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("check", "Guardar");
        guardar.setOnAction(e -> WO_KI_NB_JI_guardar());
        Label atajo = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("Ctrl+S", "kbd");
        HBox foot = new HBox(10, borrar, WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), atajo, guardar);
        foot.setAlignment(Pos.CENTER_LEFT);
        foot.getStyleClass().add("card-footer");

        for (var tf : List.of(titulo.textProperty(), contenido.textProperty(), etiquetas.textProperty()))
            tf.addListener((o, a, b) -> WO_KI_NB_JI_marcarSucio());
        tipo.setOnAction(e -> WO_KI_NB_JI_marcarSucio());

        VBox card = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        card.getStyleClass().add("editor-card");
        card.setPadding(new Insets(20, 28, 18, 28));
        card.setSpacing(14);
        HBox.setHgrow(card, Priority.ALWAYS);
        card.getChildren().addAll(top, titulo, tagRow, linea, contenido, foot);
        card.sceneProperty().addListener((o, a, s) -> {
            if (s != null) s.getAccelerators().putIfAbsent(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN), () -> {
                if (card.getScene() != null && card.isVisible() && card.getParent() != null && view.getParent() != null) WO_KI_NB_JI_guardar();
            });
        });
        return card;
    }

    private void WO_KI_NB_JI_setFavorita(boolean on) {
        favorita = on;
        fav.setGraphic(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("favorite", 18, on ? "tint-amber" : "tint-none", on ? "icon-fill" : "no-fill"));
        fav.getStyleClass().removeAll("fav-on");
        if (on) fav.getStyleClass().add("fav-on");
        WO_KI_NB_JI_Ui.WO_KI_NB_JI_tooltip(fav, on ? "Quitar de favoritas" : "Marcar como favorita");
    }

    private void WO_KI_NB_JI_marcarSucio() {
        if (cargando) return;
        sucio = true;
        estado.setText("Cambios sin guardar");
        estado.getStyleClass().setAll("caption", "text-warning");
    }

    private void WO_KI_NB_JI_nueva() {
        cargando = true;
        actual = null;
        list.getSelectionModel().clearSelection();
        titulo.clear(); contenido.clear(); etiquetas.clear();
        tipo.setValue("NOTA");
        WO_KI_NB_JI_setFavorita(false);
        cargando = false;
        sucio = false;
        estado.setText("Nota nueva");
        estado.getStyleClass().setAll("caption");
        titulo.requestFocus();
    }

    private void WO_KI_NB_JI_guardar() {
        if (titulo.getText().isBlank()) { WO_KI_NB_JI_Anim.WO_KI_NB_JI_shake(titulo); WO_KI_NB_JI_Notifier.WO_KI_NB_JI_warning("Falta el título", "Ponle un título a la nota para guardarla."); return; }
        String hoy = LocalDate.now().toString();
        boolean ok;
        boolean esNueva = actual == null;
        if (esNueva) {
            ok = dao.WO_KI_NB_JI_insertar(new WO_KI_NB_JI_Nota(0, titulo.getText().trim(), contenido.getText(), etiquetas.getText(),
                tipo.getValue(), hoy, hoy, favorita));
        } else {
            actual.WO_KI_NB_JI_setTitulo(titulo.getText().trim());
            actual.WO_KI_NB_JI_setContenido(contenido.getText());
            actual.WO_KI_NB_JI_setEtiquetas(etiquetas.getText());
            actual.WO_KI_NB_JI_setTipo(tipo.getValue());
            actual.WO_KI_NB_JI_setFechaUpdate(hoy);
            actual.WO_KI_NB_JI_setFavorita(favorita);
            ok = dao.WO_KI_NB_JI_actualizar(actual);
        }
        if (!ok) { WO_KI_NB_JI_Notifier.WO_KI_NB_JI_error("No se pudo guardar la nota", null); return; }
        String t = titulo.getText().trim();
        WO_KI_NB_JI_fill();
        // vuelve a seleccionar la nota guardada
        for (WO_KI_NB_JI_Nota n : list.getItems()) if (n.WO_KI_NB_JI_getTitulo().equals(t)) { list.getSelectionModel().select(n); break; }
        sucio = false;
        estado.setText("Guardado");
        estado.getStyleClass().setAll("caption", "text-success");
        WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success(esNueva ? "Nota creada" : "Nota guardada", t);
    }

    private void WO_KI_NB_JI_borrar() {
        if (actual == null) { WO_KI_NB_JI_nueva(); return; }
        if (!WO_KI_NB_JI_Ui.WO_KI_NB_JI_confirm(view, "¿Eliminar esta nota?", "«" + actual.WO_KI_NB_JI_getTitulo() + "» se borrará para siempre.", "Eliminar")) return;
        String t = actual.WO_KI_NB_JI_getTitulo();
        if (dao.WO_KI_NB_JI_eliminar(actual.WO_KI_NB_JI_getId())) {
            WO_KI_NB_JI_fill();
            WO_KI_NB_JI_nueva();
            list.getSelectionModel().selectFirst();
            WO_KI_NB_JI_Notifier.WO_KI_NB_JI_deleted("Nota eliminada", t);
        }
    }

    private void WO_KI_NB_JI_fill() {
        String q = buscar.getText() == null ? "" : buscar.getText().trim().toLowerCase(WO_KI_NB_JI_Ui.ES);
        int f = filtro.WO_KI_NB_JI_selected();
        List<WO_KI_NB_JI_Nota> todas = dao.WO_KI_NB_JI_listar(null);
        WO_KI_NB_JI_Nota sel = list.getSelectionModel().getSelectedItem();
        list.getItems().setAll(todas.stream()
            .filter(n -> f == 0 || (f == 1 && !"IDEA".equalsIgnoreCase(n.WO_KI_NB_JI_getTipo()))
                || (f == 2 && "IDEA".equalsIgnoreCase(n.WO_KI_NB_JI_getTipo())) || (f == 3 && n.WO_KI_NB_JI_isFavorita()))
            .filter(n -> q.isEmpty() || WO_KI_NB_JI_contiene(n.WO_KI_NB_JI_getTitulo(), q) || WO_KI_NB_JI_contiene(n.WO_KI_NB_JI_getContenido(), q) || WO_KI_NB_JI_contiene(n.WO_KI_NB_JI_getEtiquetas(), q))
            .toList());
        if (sel != null) for (WO_KI_NB_JI_Nota n : list.getItems()) if (n.WO_KI_NB_JI_getId() == sel.WO_KI_NB_JI_getId()) { list.getSelectionModel().select(n); break; }
    }

    private static boolean WO_KI_NB_JI_contiene(String s, String q) { return s != null && s.toLowerCase(WO_KI_NB_JI_Ui.ES).contains(q); }

    private void WO_KI_NB_JI_mostrar(WO_KI_NB_JI_Nota n) {
        if (sucio && actual != null && actual.WO_KI_NB_JI_getId() != n.WO_KI_NB_JI_getId())
            WO_KI_NB_JI_Notifier.WO_KI_NB_JI_warning("Había cambios sin guardar", "Se descartaron los cambios de «" + actual.WO_KI_NB_JI_getTitulo() + "».");
        cargando = true;
        actual = n;
        titulo.setText(n.WO_KI_NB_JI_getTitulo());
        contenido.setText(n.WO_KI_NB_JI_getContenido() == null ? "" : n.WO_KI_NB_JI_getContenido());
        etiquetas.setText(n.WO_KI_NB_JI_getEtiquetas() == null ? "" : n.WO_KI_NB_JI_getEtiquetas());
        tipo.setValue(n.WO_KI_NB_JI_getTipo() == null ? "NOTA" : n.WO_KI_NB_JI_getTipo().toUpperCase(WO_KI_NB_JI_Ui.ES));
        WO_KI_NB_JI_setFavorita(n.WO_KI_NB_JI_isFavorita());
        cargando = false;
        sucio = false;
        estado.setText("Editado " + WO_KI_NB_JI_cuando(n).toLowerCase(WO_KI_NB_JI_Ui.ES));
        estado.getStyleClass().setAll("caption");
    }

    public VBox WO_KI_NB_JI_getView() { return view; }
}

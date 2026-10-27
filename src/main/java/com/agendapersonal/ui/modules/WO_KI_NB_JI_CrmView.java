package com.agendapersonal.ui.modules;

import com.agendapersonal.domain.model.WO_KI_NB_JI_Contacto;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_ContactoDAO;
import com.agendapersonal.service.WO_KI_NB_JI_ContactoService;
import com.agendapersonal.ui.WO_KI_NB_JI_Anim;
import com.agendapersonal.ui.WO_KI_NB_JI_IconFactory;
import com.agendapersonal.ui.WO_KI_NB_JI_Notifier;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.ColumnConstraints;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

/** Contactos (CRM personal): lista con avatares a la izquierda y ficha editable a la derecha. */
public class WO_KI_NB_JI_CrmView {
    private static final int DIAS_SEGUIMIENTO = 30;
    private final WO_KI_NB_JI_ContactoDAO dao = new WO_KI_NB_JI_ContactoDAO();
    private final VBox view;
    private final ListView<WO_KI_NB_JI_Contacto> list = new ListView<>();
    private final TextField buscar = new TextField();
    private final WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented filtro = new WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented("Todos", "Favoritos", "Por contactar");
    private final TextField nombre = new TextField();
    private final TextField tel = new TextField();
    private final TextField email = new TextField();
    private final ComboBox<String> categoria = new ComboBox<>();
    private final DatePicker cumple = new DatePicker();
    private final TextField apodo = new TextField();
    private final VBox fechasBox = new VBox(8);
    private final Label faltan = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("", "pill", "pill-violet");
    private final TextArea notas = new TextArea();
    private final StackPane avatarBox = new StackPane();
    private final Label nombreGrande = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("", "h2");
    private final HBox chips = new HBox(10);
    private final Button favBtn = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("favorite", "Marcar como favorito");
    private final Button contactado = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("check", "Contactado hoy");
    private final Button borrar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_ghost("delete", "Eliminar contacto");
    private boolean favorito;
    private WO_KI_NB_JI_Contacto actual;

    public WO_KI_NB_JI_CrmView() {
        VBox page = WO_KI_NB_JI_Ui.WO_KI_NB_JI_page();
        Button nuevo = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("add", "Nuevo contacto");
        nuevo.setOnAction(e -> WO_KI_NB_JI_limpiar());
        page.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pageHeader("contacts", "Contactos",
            "Tus personas importantes y cuándo hablaste con ellas por última vez.", nuevo));
        HBox body = new HBox(24, WO_KI_NB_JI_panelLista(), WO_KI_NB_JI_panelFicha());
        VBox.setVgrow(body, Priority.ALWAYS);
        page.getChildren().add(body);
        view = page;
        WO_KI_NB_JI_fill();
        list.getSelectionModel().selectFirst();
        if (list.getItems().isEmpty()) WO_KI_NB_JI_limpiar();
    }

    private VBox WO_KI_NB_JI_panelLista() {
        HBox busq = WO_KI_NB_JI_Ui.WO_KI_NB_JI_searchBox(buscar, "Buscar contactos");
        busq.getStyleClass().add("search-soft");
        buscar.textProperty().addListener((o, a, b) -> WO_KI_NB_JI_fill());
        filtro.WO_KI_NB_JI_selectedProperty().addListener((o, a, b) -> WO_KI_NB_JI_fill());
        filtro.WO_KI_NB_JI_fillWidth();
        list.getStyleClass().addAll("clean-list", "contact-list");
        list.setCellFactory(lv -> new WO_KI_NB_JI_ContactoCell());
        list.setPlaceholder(WO_KI_NB_JI_Ui.WO_KI_NB_JI_empty("contacts", "Sin contactos", "Agrega a las personas importantes con «Nuevo contacto»."));
        list.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> { if (b != null) WO_KI_NB_JI_mostrar(b); });
        VBox.setVgrow(list, Priority.ALWAYS);
        VBox top = new VBox(12, busq, filtro.WO_KI_NB_JI_node());
        top.setPadding(new Insets(0, 6, 4, 6));
        VBox card = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        card.getStyleClass().add("side-panel");
        card.setPadding(new Insets(16, 10, 10, 10));
        card.setPrefWidth(340);
        card.setMinWidth(270);
        card.setMaxWidth(380);
        card.getChildren().addAll(top, list);
        return card;
    }

    private final class WO_KI_NB_JI_ContactoCell extends ListCell<WO_KI_NB_JI_Contacto> {
        @Override protected void updateItem(WO_KI_NB_JI_Contacto x, boolean empty) {
            super.updateItem(x, empty);
            setText(null);
            if (empty || x == null) { setGraphic(null); return; }
            Label n = new Label(x.WO_KI_NB_JI_getNombre());
            n.getStyleClass().add("list-title");
            String cat = x.WO_KI_NB_JI_getCategoria() == null ? "Personal" : x.WO_KI_NB_JI_getCategoria();
            Label sub = new Label(cat + " · " + WO_KI_NB_JI_Ui.WO_KI_NB_JI_haceCuanto(WO_KI_NB_JI_ultimo(x)));
            sub.getStyleClass().add("list-sub");
            VBox t = new VBox(3, n, sub);
            HBox.setHgrow(t, Priority.ALWAYS);
            HBox row = new HBox(12, WO_KI_NB_JI_Ui.WO_KI_NB_JI_avatar(x.WO_KI_NB_JI_getNombre(), 40), t);
            row.setAlignment(Pos.CENTER_LEFT);
            if (WO_KI_NB_JI_necesitaSeguimiento(x)) row.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pill("Escríbele", "amber"));
            else if (x.WO_KI_NB_JI_isFavorito()) row.getChildren().add(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("favorite", 16, "tint-amber", "icon-fill"));
            setGraphic(row);
        }
    }

    private static LocalDate WO_KI_NB_JI_ultimo(WO_KI_NB_JI_Contacto x) {
        try { return x.WO_KI_NB_JI_getUltimoContacto() == null || x.WO_KI_NB_JI_getUltimoContacto().isBlank() ? null : LocalDate.parse(x.WO_KI_NB_JI_getUltimoContacto()); }
        catch (Exception e) { return null; }
    }

    private static boolean WO_KI_NB_JI_necesitaSeguimiento(WO_KI_NB_JI_Contacto x) {
        OptionalLong d = WO_KI_NB_JI_ContactoService.WO_KI_NB_JI_diasSinContacto(x, LocalDate.now());
        return d.isPresent() && d.getAsLong() > DIAS_SEGUIMIENTO;
    }

    private Node WO_KI_NB_JI_panelFicha() {
        avatarBox.setMinSize(64, 64);
        HBox actions = new HBox(8, contactado, favBtn);
        actions.setAlignment(Pos.CENTER_RIGHT);
        VBox nameBox = new VBox(8, nombreGrande, chips);
        chips.setAlignment(Pos.CENTER_LEFT);
        HBox header = new HBox(16, avatarBox, nameBox, WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), actions);
        header.setAlignment(Pos.CENTER_LEFT);

        categoria.getItems().addAll("Familia", "Amigos", "Trabajo", "Clientes", "Personal");
        for (String extra : com.agendapersonal.settings.WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_get().WO_KI_NB_JI_strings("contacts.categories"))
            if (!categoria.getItems().contains(extra)) categoria.getItems().add(extra);
        categoria.setEditable(true);          // se puede escribir una categoría propia
        categoria.setValue("Personal");
        apodo.setPromptText("Cómo le dices (opcional)");
        nombre.setPromptText("Nombre y apellido");
        tel.setPromptText("+51 999 999 999");
        email.setPromptText("correo@ejemplo.com");
        WO_KI_NB_JI_Ui.WO_KI_NB_JI_fechaCorta(cumple);
        cumple.setPromptText("Elige la fecha");
        cumple.valueProperty().addListener((o, a, b) -> WO_KI_NB_JI_actualizarFaltan());
        notas.setPromptText("Contexto, gustos, de qué hablaron la última vez…");
        notas.setWrapText(true);
        notas.setPrefRowCount(5);

        GridPane form = new GridPane();
        form.setHgap(20);
        form.setVgap(16);
        ColumnConstraints col = new ColumnConstraints();
        col.setPercentWidth(50);
        form.getColumnConstraints().addAll(col, col);
        form.add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Nombre", nombre), 0, 0);
        form.add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Categoría", categoria), 1, 0);
        form.add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Teléfono", WO_KI_NB_JI_conIcono("phone", tel)), 0, 1);
        form.add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Correo", WO_KI_NB_JI_conIcono("mail", email)), 1, 1);
        HBox cumpleRow = new HBox(10, cumple, faltan);
        cumpleRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(cumple, Priority.ALWAYS);
        cumple.setMaxWidth(Double.MAX_VALUE);
        form.add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Cumpleaños", cumpleRow), 0, 2);
        form.add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Apodo", apodo), 1, 2);
        Button masFecha = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "+ Agregar fecha importante");
        masFecha.setOnAction(e -> WO_KI_NB_JI_filaFecha("", null));
        form.add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Otras fechas importantes (aniversario, graduación…)", new VBox(8, fechasBox, masFecha)), 0, 3, 2, 1);
        VBox notasField = WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Notas", notas);
        VBox.setVgrow(notas, Priority.ALWAYS);

        contactado.setOnAction(e -> {
            if (actual == null) return;
            WO_KI_NB_JI_ContactoService.WO_KI_NB_JI_marcarContactadoHoy(actual, LocalDate.now());
            if (dao.WO_KI_NB_JI_actualizar(actual)) {
                WO_KI_NB_JI_Anim.WO_KI_NB_JI_pop(contactado);
                WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success("Último contacto: hoy", "Registraste que hablaste con " + actual.WO_KI_NB_JI_getNombre() + ".");
                WO_KI_NB_JI_fill();
                WO_KI_NB_JI_mostrar(actual);
            }
        });
        favBtn.setOnAction(e -> { WO_KI_NB_JI_setFav(!favorito); WO_KI_NB_JI_Anim.WO_KI_NB_JI_pop(favBtn); });
        borrar.getStyleClass().add("text-danger-btn");
        borrar.setOnAction(e -> WO_KI_NB_JI_eliminar());
        Button cancelar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Cancelar");
        cancelar.setOnAction(e -> { if (actual != null) WO_KI_NB_JI_mostrar(actual); else WO_KI_NB_JI_limpiar(); });
        Button guardar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("check", "Guardar cambios");
        guardar.setOnAction(e -> WO_KI_NB_JI_guardar());
        HBox foot = new HBox(10, borrar, WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), cancelar, guardar);
        foot.setAlignment(Pos.CENTER_LEFT);
        foot.getStyleClass().add("card-footer");

        Region linea = new Region();
        linea.getStyleClass().add("hairline");
        VBox card = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        card.setPadding(new Insets(24, 28, 18, 28));
        card.setSpacing(20);
        card.getChildren().addAll(header, linea, form, notasField, foot);
        ScrollPane sp = new ScrollPane(card);
        sp.setFitToWidth(true);
        sp.setFitToHeight(true);
        sp.getStyleClass().add("page-scroll");
        HBox.setHgrow(sp, Priority.ALWAYS);
        return sp;
    }

    private static HBox WO_KI_NB_JI_conIcono(String icon, TextField f) {
        f.getStyleClass().add("bare-field");
        HBox.setHgrow(f, Priority.ALWAYS);
        HBox box = new HBox(10, WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon(icon, 16), f);
        box.getStyleClass().add("input-with-icon");
        box.setAlignment(Pos.CENTER_LEFT);
        f.focusedProperty().addListener((o, a, b) ->
            box.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("focused"), b));
        return box;
    }

    private void WO_KI_NB_JI_setFav(boolean on) {
        favorito = on;
        favBtn.setGraphic(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("favorite", 18, on ? "tint-amber" : "tint-none", on ? "icon-fill" : "no-fill"));
        favBtn.getStyleClass().remove("fav-on");
        if (on) favBtn.getStyleClass().add("fav-on");
        WO_KI_NB_JI_Ui.WO_KI_NB_JI_tooltip(favBtn, on ? "Quitar de favoritos" : "Marcar como favorito");
    }

    private void WO_KI_NB_JI_actualizarFaltan() {
        LocalDate d = cumple.getValue();
        if (d == null) { faltan.setVisible(false); faltan.setManaged(false); return; }
        LocalDate hoy = LocalDate.now();
        LocalDate prox = d.withYear(hoy.getYear());
        if (prox.isBefore(hoy)) prox = prox.plusYears(1);
        long dias = ChronoUnit.DAYS.between(hoy, prox);
        faltan.setText(dias == 0 ? "¡Es hoy!" : dias == 1 ? "Mañana" : "Faltan " + dias + " días");
        faltan.setVisible(true);
        faltan.setManaged(true);
    }

    private void WO_KI_NB_JI_cabecera(String n, String cat, LocalDate ult, boolean nuevo) {
        avatarBox.getChildren().setAll(nuevo ? WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconChip("contacts", "slate", 64, 28) : WO_KI_NB_JI_Ui.WO_KI_NB_JI_avatar(n, 64));
        nombreGrande.setText(nuevo ? "Nuevo contacto" : n);
        chips.getChildren().clear();
        if (!nuevo) {
            chips.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pill(cat, "blue"));
            Label u = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(ult == null ? "Aún sin registrar contacto" : "Último contacto " + WO_KI_NB_JI_Ui.WO_KI_NB_JI_haceCuanto(ult), "muted");
            u.setGraphic(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("clock", 14));
            u.setGraphicTextGap(6);
            chips.getChildren().add(u);
        } else chips.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Completa los datos y pulsa «Guardar cambios»."));
        contactado.setDisable(nuevo);
        borrar.setDisable(nuevo);
    }

    private void WO_KI_NB_JI_fill() {
        String q = buscar.getText() == null ? "" : buscar.getText().trim().toLowerCase(WO_KI_NB_JI_Ui.ES);
        int f = filtro.WO_KI_NB_JI_selected();
        List<WO_KI_NB_JI_Contacto> todos = dao.WO_KI_NB_JI_listar();
        WO_KI_NB_JI_Contacto sel = list.getSelectionModel().getSelectedItem();
        list.getItems().setAll(todos.stream()
            .filter(x -> f == 0 || (f == 1 && x.WO_KI_NB_JI_isFavorito()) || (f == 2 && WO_KI_NB_JI_necesitaSeguimiento(x)))
            .filter(x -> q.isEmpty() || x.WO_KI_NB_JI_getNombre().toLowerCase(WO_KI_NB_JI_Ui.ES).contains(q)
                || (x.WO_KI_NB_JI_getEmail() != null && x.WO_KI_NB_JI_getEmail().toLowerCase(WO_KI_NB_JI_Ui.ES).contains(q))
                || (x.WO_KI_NB_JI_getTelefono() != null && x.WO_KI_NB_JI_getTelefono().contains(q)))
            .toList());
        if (sel != null) for (WO_KI_NB_JI_Contacto x : list.getItems()) if (x.WO_KI_NB_JI_getId() == sel.WO_KI_NB_JI_getId()) { list.getSelectionModel().select(x); break; }
    }

    private void WO_KI_NB_JI_mostrar(WO_KI_NB_JI_Contacto x) {
        actual = x;
        nombre.setText(x.WO_KI_NB_JI_getNombre());
        tel.setText(x.WO_KI_NB_JI_getTelefono() == null ? "" : x.WO_KI_NB_JI_getTelefono());
        email.setText(x.WO_KI_NB_JI_getEmail() == null ? "" : x.WO_KI_NB_JI_getEmail());
        String catX = x.WO_KI_NB_JI_getCategoria() == null ? "Personal" : x.WO_KI_NB_JI_getCategoria();
        if (!categoria.getItems().contains(catX)) categoria.getItems().add(catX);
        categoria.setValue(catX);
        apodo.setText(x.WO_KI_NB_JI_getApodo() == null ? "" : x.WO_KI_NB_JI_getApodo());
        fechasBox.getChildren().clear();
        for (var f : com.agendapersonal.service.WO_KI_NB_JI_FechasImportantes.WO_KI_NB_JI_leer(x.WO_KI_NB_JI_getFechas())) WO_KI_NB_JI_filaFecha(f.etiqueta(), f.fecha());
        try { cumple.setValue(x.WO_KI_NB_JI_getCumpleanos() == null || x.WO_KI_NB_JI_getCumpleanos().isBlank() ? null : LocalDate.parse(x.WO_KI_NB_JI_getCumpleanos())); }
        catch (Exception ignored) { cumple.setValue(null); }
        // Antes el "Último contacto" se pegaba dentro de las notas y se duplicaba en cada guardado.
        notas.setText(x.WO_KI_NB_JI_getNotas() == null ? "" : x.WO_KI_NB_JI_getNotas());
        WO_KI_NB_JI_setFav(x.WO_KI_NB_JI_isFavorito());
        WO_KI_NB_JI_cabecera(x.WO_KI_NB_JI_getNombre(), categoria.getValue(), WO_KI_NB_JI_ultimo(x), false);
        WO_KI_NB_JI_actualizarFaltan();
    }

    private void WO_KI_NB_JI_limpiar() {
        actual = null;
        list.getSelectionModel().clearSelection();
        nombre.clear(); tel.clear(); email.clear(); notas.clear();
        categoria.setValue("Personal");
        cumple.setValue(null);
        apodo.clear();
        fechasBox.getChildren().clear();
        WO_KI_NB_JI_setFav(false);
        WO_KI_NB_JI_cabecera(null, null, null, true);
        WO_KI_NB_JI_actualizarFaltan();
        nombre.requestFocus();
    }

    /** Agrega una fila (etiqueta + fecha + quitar) a las fechas importantes del contacto. */
    private void WO_KI_NB_JI_filaFecha(String etiqueta, LocalDate fecha) {
        TextField et = new TextField(etiqueta);
        et.setPromptText("Ej. Aniversario");
        DatePicker dp = new DatePicker(fecha);
        WO_KI_NB_JI_Ui.WO_KI_NB_JI_fechaCorta(dp);
        Button quitar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("close", "Quitar fecha");
        HBox fila = new HBox(8, et, dp, quitar);
        fila.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(et, Priority.ALWAYS);
        quitar.setOnAction(e -> fechasBox.getChildren().remove(fila));
        fechasBox.getChildren().add(fila);
    }

    private String WO_KI_NB_JI_leerFechas() {
        List<com.agendapersonal.service.WO_KI_NB_JI_FechasImportantes.WO_KI_NB_JI_Fecha> l = new ArrayList<>();
        for (Node n : fechasBox.getChildren())
            if (n instanceof HBox h && h.getChildren().get(0) instanceof TextField t && h.getChildren().get(1) instanceof DatePicker d && d.getValue() != null)
                l.add(new com.agendapersonal.service.WO_KI_NB_JI_FechasImportantes.WO_KI_NB_JI_Fecha(t.getText(), d.getValue()));
        return com.agendapersonal.service.WO_KI_NB_JI_FechasImportantes.WO_KI_NB_JI_escribir(l);
    }

    /** Recuerda las categorías propias para ofrecerlas la próxima vez. */
    private void WO_KI_NB_JI_guardarCategoria(String cat) {
        var st = com.agendapersonal.settings.WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_get();
        List<String> base = List.of("Familia", "Amigos", "Trabajo", "Clientes", "Personal");
        List<String> propias = new ArrayList<>(st.WO_KI_NB_JI_strings("contacts.categories"));
        if (!base.contains(cat) && !propias.contains(cat)) { propias.add(cat); st.WO_KI_NB_JI_set("contacts.categories", propias); }
        if (!categoria.getItems().contains(cat)) categoria.getItems().add(cat);
    }

    private void WO_KI_NB_JI_guardar() {
        if (nombre.getText().isBlank()) {
            WO_KI_NB_JI_Anim.WO_KI_NB_JI_shake(nombre);
            WO_KI_NB_JI_Notifier.WO_KI_NB_JI_warning("Falta el nombre", "Escribe el nombre del contacto para guardarlo.");
            return;
        }
        String cum = cumple.getValue() == null ? null : cumple.getValue().toString();
        String n = nombre.getText().trim();
        String cat = categoria.getEditor().getText() == null || categoria.getEditor().getText().isBlank() ? "Personal" : categoria.getEditor().getText().trim();
        categoria.setValue(cat);
        WO_KI_NB_JI_guardarCategoria(cat);
        String fechasTxt = WO_KI_NB_JI_leerFechas();
        boolean ok;
        boolean esNuevo = actual == null;
        if (esNuevo) {
            WO_KI_NB_JI_Contacto nuevo = new WO_KI_NB_JI_Contacto(0, n, tel.getText().trim(), email.getText().trim(),
                cat, cum, notas.getText(), favorito, null);
            nuevo.WO_KI_NB_JI_setApodo(apodo.getText().trim());
            nuevo.WO_KI_NB_JI_setFechas(fechasTxt);
            ok = dao.WO_KI_NB_JI_insertar(nuevo);
        } else {
            actual.WO_KI_NB_JI_setNombre(n);
            actual.WO_KI_NB_JI_setTelefono(tel.getText().trim());
            actual.WO_KI_NB_JI_setEmail(email.getText().trim());
            actual.WO_KI_NB_JI_setCategoria(cat);
            actual.WO_KI_NB_JI_setApodo(apodo.getText().trim());
            actual.WO_KI_NB_JI_setFechas(fechasTxt);
            actual.WO_KI_NB_JI_setCumpleanos(cum);
            actual.WO_KI_NB_JI_setNotas(notas.getText());
            actual.WO_KI_NB_JI_setFavorito(favorito);
            ok = dao.WO_KI_NB_JI_actualizar(actual);
        }
        if (!ok) { WO_KI_NB_JI_Notifier.WO_KI_NB_JI_error("No se pudo guardar el contacto", null); return; }
        WO_KI_NB_JI_fill();
        for (WO_KI_NB_JI_Contacto x : list.getItems()) if (x.WO_KI_NB_JI_getNombre().equals(n)) { list.getSelectionModel().select(x); WO_KI_NB_JI_mostrar(x); break; }
        WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success(esNuevo ? "Contacto agregado" : "Contacto actualizado", n);
    }

    private void WO_KI_NB_JI_eliminar() {
        if (actual == null) return;
        if (!WO_KI_NB_JI_Ui.WO_KI_NB_JI_confirm(view, "¿Eliminar a " + actual.WO_KI_NB_JI_getNombre() + "?", "Se borrará su ficha y sus notas.", "Eliminar")) return;
        String n = actual.WO_KI_NB_JI_getNombre();
        if (dao.WO_KI_NB_JI_eliminar(actual.WO_KI_NB_JI_getId())) {
            WO_KI_NB_JI_fill();
            if (list.getItems().isEmpty()) WO_KI_NB_JI_limpiar(); else list.getSelectionModel().selectFirst();
            WO_KI_NB_JI_Notifier.WO_KI_NB_JI_deleted("Contacto eliminado", n);
        }
    }

    public VBox WO_KI_NB_JI_getView() { return view; }
}

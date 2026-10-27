package com.agendapersonal.ui.modules;

import com.agendapersonal.controller.WO_KI_NB_JI_MainController;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Proyecto;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_ProyectoDAO;
import com.agendapersonal.model.WO_KI_NB_JI_EstadoTarea;
import com.agendapersonal.model.WO_KI_NB_JI_Tarea;
import com.agendapersonal.service.WO_KI_NB_JI_ProgresoService;
import com.agendapersonal.ui.WO_KI_NB_JI_Anim;
import com.agendapersonal.ui.WO_KI_NB_JI_IconFactory;
import com.agendapersonal.ui.WO_KI_NB_JI_Notifier;
import com.agendapersonal.ui.WO_KI_NB_JI_TaskViews;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
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
public final class WO_KI_NB_JI_TareasProyectosView {
    private static final String[] COLORES = {"#2F5BB7", "#C64B1C", "#2C7A4B", "#6340B8", "#1F6F6A", "#B23A55", "#8A5A00"};
    /** Elemento "Todas las tareas" de la lista de proyectos. */
    private static final WO_KI_NB_JI_Proyecto TODAS = new WO_KI_NB_JI_Proyecto(-1, "Todas las tareas", "", "Activo", null, null, null);

    private final WO_KI_NB_JI_MainController c;
    private final WO_KI_NB_JI_ProyectoDAO dao = new WO_KI_NB_JI_ProyectoDAO();
    private final VBox view;
    private final ListView<WO_KI_NB_JI_Proyecto> proyectos = new ListView<>();
    private final VBox grupos = new VBox(0);
    private final TextField buscar = new TextField();
    private final WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented filtro = new WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented("Todas", "Hoy", "Próximas", "Vencidas", "Completadas");
    private final WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented estadoProyectos = new WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented("Activos", "Todos");
    private boolean verCompletadas;

    public WO_KI_NB_JI_TareasProyectosView(WO_KI_NB_JI_MainController c) {
        this.c = c;
        VBox page = WO_KI_NB_JI_Ui.WO_KI_NB_JI_page();
        Button nueva = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("add", "Nueva tarea");
        nueva.setOnAction(e -> WO_KI_NB_JI_TaskViews.WO_KI_NB_JI_form(null, c, this::WO_KI_NB_JI_fillTareas));
        page.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pageHeader("tasks", "Tareas",
            "Todo lo que tienes pendiente, ordenado por lista y por fecha.", nueva));

        HBox body = new HBox(24, WO_KI_NB_JI_panelProyectos(), WO_KI_NB_JI_panelTareas());
        VBox.setVgrow(body, Priority.ALWAYS);
        page.getChildren().add(body);
        view = page;

        c.WO_KI_NB_JI_getTareas().addListener((Observable o) -> { WO_KI_NB_JI_fillTareas(); proyectos.refresh(); });
        WO_KI_NB_JI_fillProyectos();
    }

    // ------------------------------------------------------------------ proyectos

    private VBox WO_KI_NB_JI_panelProyectos() {
        Label t = WO_KI_NB_JI_Ui.WO_KI_NB_JI_h3("Listas");
        HBox head = new HBox(8, t, WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), estadoProyectos.WO_KI_NB_JI_node());
        head.setAlignment(Pos.CENTER_LEFT);
        estadoProyectos.WO_KI_NB_JI_selectedProperty().addListener((o, a, b) -> WO_KI_NB_JI_fillProyectos());

        proyectos.getStyleClass().addAll("clean-list", "project-list");
        proyectos.setCellFactory(lv -> new WO_KI_NB_JI_ProyectoCell());
        proyectos.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> WO_KI_NB_JI_fillTareas());
        VBox.setVgrow(proyectos, Priority.ALWAYS);

        TextField nuevo = new TextField();
        nuevo.getStyleClass().add("bare-field");
        HBox add = new HBox(10, WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("add", 17), nuevo);
        add.getStyleClass().add("dashed-input");
        add.setAlignment(Pos.CENTER_LEFT);
        nuevo.setPromptText("Nueva lista y Enter");
        HBox.setHgrow(nuevo, Priority.ALWAYS);
        nuevo.setOnAction(e -> {
            String n = nuevo.getText().trim();
            if (n.isEmpty()) { WO_KI_NB_JI_Anim.WO_KI_NB_JI_shake(add); return; }
            String color = COLORES[dao.WO_KI_NB_JI_listar().size() % COLORES.length];
            if (dao.WO_KI_NB_JI_insertar(new WO_KI_NB_JI_Proyecto(0, n, "", "Activo", LocalDate.now().toString(), null, color))) {
                nuevo.clear();
                WO_KI_NB_JI_fillProyectos();
                WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success("Lista creada", "«" + n + "» ya está en tu lista.");
            } else WO_KI_NB_JI_Notifier.WO_KI_NB_JI_error("No se pudo crear la lista", "Revisa la base de datos.");
        });

        VBox card = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        card.getStyleClass().add("side-panel");
        card.setPadding(new Insets(18, 12, 14, 12));
        card.setPrefWidth(300);
        card.setMinWidth(250);
        card.setMaxWidth(320);
        VBox.setMargin(head, new Insets(0, 6, 0, 6));
        card.getChildren().addAll(head, proyectos, add);
        return card;
    }

    private final class WO_KI_NB_JI_ProyectoCell extends ListCell<WO_KI_NB_JI_Proyecto> {
        WO_KI_NB_JI_ProyectoCell() {
            setOnContextMenuRequested(e -> {
                WO_KI_NB_JI_Proyecto p = getItem();
                if (p != null && p != TODAS) WO_KI_NB_JI_menuProyecto(p).show(this, e.getScreenX(), e.getScreenY());
            });
        }

        @Override protected void updateItem(WO_KI_NB_JI_Proyecto p, boolean empty) {
            super.updateItem(p, empty);
            setText(null);
            if (empty || p == null) { setGraphic(null); return; }
            if (p == TODAS) {
                Label n = new Label(p.WO_KI_NB_JI_getNombre());
                n.getStyleClass().add("list-title");
                Label count = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(String.valueOf(c.WO_KI_NB_JI_getTareas().size()), "list-count");
                HBox row = new HBox(12, WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("inbox", 18, "tint-accent"), n, WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), count);
                row.setAlignment(Pos.CENTER_LEFT);
                setGraphic(row);
                return;
            }
            List<WO_KI_NB_JI_Tarea> ts = c.WO_KI_NB_JI_getTareas().stream().filter(t -> WO_KI_NB_JI_pertenece(t, p)).toList();
            long hechas = ts.stream().filter(WO_KI_NB_JI_Tarea::WO_KI_NB_JI_estaCompletada).count();
            int pct = WO_KI_NB_JI_ProgresoService.WO_KI_NB_JI_porcentaje(p.WO_KI_NB_JI_getId(), c.WO_KI_NB_JI_getTareas());
            StackPane dot = new StackPane();
            dot.getStyleClass().add("dot");
            dot.setStyle("-fx-background-color:" + WO_KI_NB_JI_safeColor(p.WO_KI_NB_JI_getColor()) + ";");
            Label n = new Label(p.WO_KI_NB_JI_getNombre());
            n.getStyleClass().add("list-title");
            HBox titleRow = new HBox(8, n);
            if (!"Activo".equalsIgnoreCase(p.WO_KI_NB_JI_getEstado())) titleRow.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pill(p.WO_KI_NB_JI_getEstado(), "slate"));
            titleRow.setAlignment(Pos.CENTER_LEFT);
            StackPane bar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_bar(pct / 100.0, "accent", 4);
            VBox mid = new VBox(8, titleRow, bar);
            HBox.setHgrow(mid, Priority.ALWAYS);
            Label count = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(hechas + "/" + ts.size(), "list-count");
            HBox row = new HBox(12, dot, mid, count);
            row.setAlignment(Pos.CENTER_LEFT);
            bar.getChildren().getFirst().setStyle("-fx-background-color:" + WO_KI_NB_JI_safeColor(p.WO_KI_NB_JI_getColor()) + ";");
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_tooltip(this, p.WO_KI_NB_JI_getNombre() + " · " + pct + "% completado · clic derecho para más opciones");
            setGraphic(row);
        }
    }

    private ContextMenu WO_KI_NB_JI_menuProyecto(WO_KI_NB_JI_Proyecto p) {
        Menu estado = new Menu("Cambiar estado");
        for (String e : new String[]{"Activo", "Pausado", "Completado"}) {
            MenuItem mi = new MenuItem(e);
            mi.setOnAction(x -> {
                p.WO_KI_NB_JI_setEstado(e);
                if (dao.WO_KI_NB_JI_actualizar(p)) { WO_KI_NB_JI_fillProyectos(); WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success("Lista actualizada", p.WO_KI_NB_JI_getNombre() + " ahora está " + e.toLowerCase(Locale.ROOT) + "."); }
            });
            estado.getItems().add(mi);
        }
        MenuItem renombrar = new MenuItem("Renombrar…");
        renombrar.setOnAction(x -> {
            var d = new javafx.scene.control.TextInputDialog(p.WO_KI_NB_JI_getNombre());
            d.setTitle("Compás");
            d.setHeaderText("Nuevo nombre de la lista");
            d.getDialogPane().getStyleClass().add("cs-dialog");
            d.initOwner(view.getScene().getWindow());
            d.showAndWait().map(String::trim).filter(s -> !s.isEmpty()).ifPresent(s -> {
                p.WO_KI_NB_JI_setNombre(s);
                if (dao.WO_KI_NB_JI_actualizar(p)) { WO_KI_NB_JI_fillProyectos(); WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success("Lista renombrada", s); }
            });
        });
        MenuItem borrar = new MenuItem("Eliminar lista");
        borrar.getStyleClass().add("danger-item");
        borrar.setOnAction(x -> {
            if (WO_KI_NB_JI_Ui.WO_KI_NB_JI_confirm(view, "¿Eliminar «" + p.WO_KI_NB_JI_getNombre() + "»?",
                "Sus tareas no se borran: quedarán sin lista.", "Eliminar")) {
                if (dao.WO_KI_NB_JI_eliminar(p.WO_KI_NB_JI_getId())) {
                    c.WO_KI_NB_JI_cargarTodo();
                    WO_KI_NB_JI_fillProyectos();
                    WO_KI_NB_JI_Notifier.WO_KI_NB_JI_deleted("Lista eliminada", "«" + p.WO_KI_NB_JI_getNombre() + "» se quitó de la lista.");
                }
            }
        });
        return new ContextMenu(estado, renombrar, new SeparatorMenuItem(), borrar);
    }

    private void WO_KI_NB_JI_fillProyectos() {
        WO_KI_NB_JI_Proyecto sel = proyectos.getSelectionModel().getSelectedItem();
        List<WO_KI_NB_JI_Proyecto> lista = new ArrayList<>();
        lista.add(TODAS);
        for (WO_KI_NB_JI_Proyecto p : dao.WO_KI_NB_JI_listar())
            if (estadoProyectos.WO_KI_NB_JI_selected() == 1 || !"Completado".equalsIgnoreCase(p.WO_KI_NB_JI_getEstado())) lista.add(p);
        proyectos.getItems().setAll(lista);
        if (sel != null) for (WO_KI_NB_JI_Proyecto p : lista) if (p.WO_KI_NB_JI_getId() == sel.WO_KI_NB_JI_getId()) { proyectos.getSelectionModel().select(p); break; }
        if (proyectos.getSelectionModel().getSelectedItem() == null) proyectos.getSelectionModel().selectFirst();
        WO_KI_NB_JI_fillTareas();
    }

    private static String WO_KI_NB_JI_safeColor(String c) {
        return c != null && c.matches("#[0-9a-fA-F]{6}") ? c : "#2F5BB7";
    }

    private static boolean WO_KI_NB_JI_pertenece(WO_KI_NB_JI_Tarea t, WO_KI_NB_JI_Proyecto p) {
        return t.WO_KI_NB_JI_getProyectoId() != null && t.WO_KI_NB_JI_getProyectoId() == p.WO_KI_NB_JI_getId();
    }

    // ------------------------------------------------------------------ tareas

    private VBox WO_KI_NB_JI_panelTareas() {
        HBox buscador = WO_KI_NB_JI_Ui.WO_KI_NB_JI_searchBox(buscar, "Buscar tareas");
        buscador.setPrefWidth(260);
        buscador.setMaxWidth(300);
        buscar.textProperty().addListener((o, a, b) -> WO_KI_NB_JI_fillTareas());
        filtro.WO_KI_NB_JI_selectedProperty().addListener((o, a, b) -> WO_KI_NB_JI_fillTareas());
        HBox toolbar = new HBox(12, filtro.WO_KI_NB_JI_node(), WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), buscador);
        toolbar.setAlignment(Pos.CENTER_LEFT);

        grupos.getStyleClass().add("list-card");
        ScrollPane sp = new ScrollPane(grupos);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.getStyleClass().addAll("card", "card-scroll");
        VBox.setVgrow(sp, Priority.ALWAYS);

        VBox right = new VBox(16, toolbar, sp);
        HBox.setHgrow(right, Priority.ALWAYS);
        return right;
    }

    private void WO_KI_NB_JI_fillTareas() {
        WO_KI_NB_JI_Proyecto sel = proyectos.getSelectionModel().getSelectedItem();
        String q = buscar.getText() == null ? "" : buscar.getText().trim().toLowerCase(WO_KI_NB_JI_Ui.ES);
        LocalDate hoy = LocalDate.now();
        List<WO_KI_NB_JI_Tarea> base = c.WO_KI_NB_JI_getTareas().stream()
            .filter(t -> sel == null || sel == TODAS || WO_KI_NB_JI_pertenece(t, sel))
            .filter(t -> q.isEmpty() || t.WO_KI_NB_JI_getTitulo().toLowerCase(WO_KI_NB_JI_Ui.ES).contains(q)
                || (t.WO_KI_NB_JI_getDescripcion() != null && t.WO_KI_NB_JI_getDescripcion().toLowerCase(WO_KI_NB_JI_Ui.ES).contains(q)))
            .toList();
        long pend = base.stream().filter(t -> !t.WO_KI_NB_JI_estaCompletada()).count();
        long nHoy = base.stream().filter(t -> !t.WO_KI_NB_JI_estaCompletada() && hoy.equals(t.WO_KI_NB_JI_getFecha())).count();
        long nProx = base.stream().filter(t -> !t.WO_KI_NB_JI_estaCompletada() && t.WO_KI_NB_JI_getFecha() != null && t.WO_KI_NB_JI_getFecha().isAfter(hoy)).count();
        long nVenc = base.stream().filter(t -> !t.WO_KI_NB_JI_estaCompletada() && t.WO_KI_NB_JI_getFecha() != null && t.WO_KI_NB_JI_getFecha().isBefore(hoy)).count();
        long nHechas = base.size() - pend;
        filtro.WO_KI_NB_JI_setText(0, "Todas  " + pend);
        filtro.WO_KI_NB_JI_setText(1, "Hoy  " + nHoy);
        filtro.WO_KI_NB_JI_setText(2, "Próximas  " + nProx);
        filtro.WO_KI_NB_JI_setText(3, "Vencidas  " + nVenc);
        filtro.WO_KI_NB_JI_setText(4, "Completadas  " + nHechas);

        Comparator<WO_KI_NB_JI_Tarea> orden = Comparator.comparing((WO_KI_NB_JI_Tarea t) -> t.WO_KI_NB_JI_getFecha() == null ? LocalDate.MAX : t.WO_KI_NB_JI_getFecha())
            .thenComparing(t -> -t.WO_KI_NB_JI_getPrioridadTipo().ordinal());
        Map<String, List<WO_KI_NB_JI_Tarea>> grupo = new LinkedHashMap<>();
        int f = filtro.WO_KI_NB_JI_selected();
        LocalDate finSemana = hoy.with(DayOfWeek.SUNDAY);
        for (WO_KI_NB_JI_Tarea t : base.stream().sorted(orden).toList()) {
            LocalDate d = t.WO_KI_NB_JI_getFecha();
            if (t.WO_KI_NB_JI_estaCompletada()) { if (f == 0 || f == 4) grupo.computeIfAbsent("Completadas", k -> new ArrayList<>()).add(t); continue; }
            if (f == 4) continue;
            String g;
            if (d == null) g = "Sin fecha";
            else if (d.isBefore(hoy)) g = "Vencidas";
            else if (d.equals(hoy)) g = "Hoy";
            else if (d.equals(hoy.plusDays(1))) g = "Mañana";
            else if (!d.isAfter(finSemana)) g = "Esta semana";
            else g = "Más adelante";
            if (f == 1 && !g.equals("Hoy")) continue;
            if (f == 2 && (g.equals("Hoy") || g.equals("Vencidas"))) continue;
            if (f == 3 && !g.equals("Vencidas")) continue;
            grupo.computeIfAbsent(g, k -> new ArrayList<>()).add(t);
        }
        // Orden de los grupos: vencidas primero, completadas al final.
        List<String> ordenGrupos = List.of("Vencidas", "Hoy", "Mañana", "Esta semana", "Más adelante", "Sin fecha", "Completadas");
        grupos.getChildren().clear();
        if (grupo.isEmpty()) {
            String msg = q.isEmpty() ? (f == 4 ? "Aún no completas tareas aquí." : "No tienes pendientes en esta vista. ¡Buen trabajo!")
                : "Ninguna tarea coincide con «" + q + "».";
            grupos.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_empty("tasks", "Nada por aquí", msg));
            return;
        }
        List<Node> filas = new ArrayList<>();
        boolean primero = true;
        for (String g : ordenGrupos) {
            List<WO_KI_NB_JI_Tarea> ts = grupo.get(g);
            if (ts == null) continue;
            HBox head = new HBox(8, WO_KI_NB_JI_Ui.WO_KI_NB_JI_overline(g), WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(String.valueOf(ts.size()), "count-pill"));
            head.setAlignment(Pos.CENTER_LEFT);
            head.getStyleClass().add("group-head");
            if (g.equals("Vencidas")) head.getStyleClass().add("group-head-danger");
            if (!primero) head.getStyleClass().add("with-divider");
            primero = false;
            grupos.getChildren().add(head);
            List<WO_KI_NB_JI_Tarea> visibles = ts;
            if (g.equals("Completadas") && f == 0 && !verCompletadas && ts.size() > 2) visibles = ts.subList(0, 2);
            for (WO_KI_NB_JI_Tarea t : visibles) { Node r = WO_KI_NB_JI_fila(t); grupos.getChildren().add(r); filas.add(r); }
            if (visibles.size() < ts.size()) {
                Button mas = WO_KI_NB_JI_Ui.WO_KI_NB_JI_ghost("chevron-down", "Ver " + (ts.size() - visibles.size()) + " completadas más");
                mas.setOnAction(e -> { verCompletadas = true; WO_KI_NB_JI_fillTareas(); });
                HBox w = new HBox(mas);
                w.setPadding(new Insets(4, 16, 12, 16));
                grupos.getChildren().add(w);
            } else if (g.equals("Completadas") && verCompletadas && f == 0 && ts.size() > 2) {
                Button menos = WO_KI_NB_JI_Ui.WO_KI_NB_JI_ghost("chevron-up", "Ocultar completadas");
                menos.setOnAction(e -> { verCompletadas = false; WO_KI_NB_JI_fillTareas(); });
                HBox w = new HBox(menos);
                w.setPadding(new Insets(4, 16, 12, 16));
                grupos.getChildren().add(w);
            }
        }
        WO_KI_NB_JI_Anim.WO_KI_NB_JI_stagger(filas);
    }

    private Node WO_KI_NB_JI_fila(WO_KI_NB_JI_Tarea t) {
        boolean hecha = t.WO_KI_NB_JI_estaCompletada();
        Button check = new Button();
        check.getStyleClass().addAll("check-circle");
        if (hecha) check.getStyleClass().add("checked");
        check.setGraphic(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("check", 14, "tint-on-accent"));
        check.setAccessibleText(hecha ? "Marcar como pendiente" : "Completar tarea");
        WO_KI_NB_JI_Ui.WO_KI_NB_JI_tooltip(check, hecha ? "Marcar como pendiente" : "Completar");
        check.setOnAction(e -> {
            WO_KI_NB_JI_Anim.WO_KI_NB_JI_pop(check);
            c.WO_KI_NB_JI_alternarCompletada(t).ifPresentOrElse(r -> {
                if (r.completada()) WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success(c.WO_KI_NB_JI_mensajeProgreso(r), t.WO_KI_NB_JI_getTitulo());
                else WO_KI_NB_JI_Notifier.WO_KI_NB_JI_info(c.WO_KI_NB_JI_mensajeProgreso(r), t.WO_KI_NB_JI_getTitulo());
            }, () -> WO_KI_NB_JI_Notifier.WO_KI_NB_JI_error("No se pudo actualizar la tarea", null));
        });

        Label titulo = new Label(t.WO_KI_NB_JI_getTitulo());
        titulo.getStyleClass().add("row-title");
        if (hecha) titulo.getStyleClass().add("done");
        // Viñeta de importancia (rojo = alta, naranja = media, verde = baja) para distinguir de un vistazo.
        StackPane vineta = new StackPane();
        vineta.getStyleClass().addAll("dot");
        vineta.setStyle("-fx-background-color: -" + t.WO_KI_NB_JI_getPrioridadTipo().WO_KI_NB_JI_tono() + ";");
        WO_KI_NB_JI_Ui.WO_KI_NB_JI_tooltip(titulo, "Importancia " + t.WO_KI_NB_JI_getPrioridad().toLowerCase());
        titulo.setGraphic(vineta);
        titulo.setGraphicTextGap(8);
        HBox meta = new HBox(14);
        meta.setAlignment(Pos.CENTER_LEFT);
        if (t.WO_KI_NB_JI_getProyectoId() != null) {
            for (WO_KI_NB_JI_Proyecto p : proyectos.getItems()) if (p.WO_KI_NB_JI_getId() == t.WO_KI_NB_JI_getProyectoId()) {
                StackPane dot = new StackPane();
                dot.getStyleClass().addAll("dot", "dot-small");
                dot.setStyle("-fx-background-color:" + WO_KI_NB_JI_safeColor(p.WO_KI_NB_JI_getColor()) + ";");
                Label n = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(p.WO_KI_NB_JI_getNombre(), "row-meta");
                n.setGraphic(dot);
                n.setGraphicTextGap(6);
                meta.getChildren().add(n);
            }
        }
        Label fecha = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(hecha ? "Completada" : WO_KI_NB_JI_Ui.WO_KI_NB_JI_friendlyDate(t.WO_KI_NB_JI_getFecha()), "row-meta");
        fecha.setGraphic(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("calendar", 13));
        fecha.setGraphicTextGap(6);
        if (!hecha && t.WO_KI_NB_JI_getFecha() != null && t.WO_KI_NB_JI_getFecha().isBefore(LocalDate.now())) fecha.getStyleClass().add("text-danger");
        if (t.WO_KI_NB_JI_getHora() != null) fecha.setText(fecha.getText() + " · " + t.WO_KI_NB_JI_getHora());
        meta.getChildren().add(fecha);
        if (t.WO_KI_NB_JI_getEventoId() != null) meta.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("Evento vinculado", "row-meta"));
        if (t.WO_KI_NB_JI_getBloqueId() != null) meta.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("Bloque vinculado", "row-meta"));
        if (t.WO_KI_NB_JI_getEstadoTipo() == WO_KI_NB_JI_EstadoTarea.EN_PROGRESO) meta.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pill("En progreso", "blue"));
        VBox texts = new VBox(5, titulo, meta);
        HBox.setHgrow(texts, Priority.ALWAYS);

        String tono = t.WO_KI_NB_JI_getPrioridadTipo().WO_KI_NB_JI_tono();
        Label pri = WO_KI_NB_JI_Ui.WO_KI_NB_JI_pill(t.WO_KI_NB_JI_getPrioridad(), tono);
        Button mas = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("more", "Más opciones");
        mas.setOnAction(e -> WO_KI_NB_JI_menuTarea(t).show(mas, javafx.geometry.Side.BOTTOM, 0, 4));

        Button estrella = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton(t.WO_KI_NB_JI_isDestacada() ? "favorite-on" : "favorite", t.WO_KI_NB_JI_isDestacada() ? "Quitar destacado" : "Destacar");
        estrella.setOnAction(e -> { t.WO_KI_NB_JI_setDestacada(!t.WO_KI_NB_JI_isDestacada()); c.WO_KI_NB_JI_actualizarTarea(t); });
        HBox row = new HBox(12, check, texts, pri, estrella, mas);
        row.getStyleClass().add("list-row");
        if (hecha) row.getStyleClass().add("row-done");
        row.setAlignment(Pos.CENTER_LEFT);
        row.setOnMouseClicked(e -> { if (e.getClickCount() == 2) WO_KI_NB_JI_TaskViews.WO_KI_NB_JI_form(t, c, this::WO_KI_NB_JI_fillTareas); });
        return row;
    }

    private ContextMenu WO_KI_NB_JI_menuTarea(WO_KI_NB_JI_Tarea t) {
        MenuItem editar = new MenuItem("Editar…");
        editar.setOnAction(e -> WO_KI_NB_JI_TaskViews.WO_KI_NB_JI_form(t, c, this::WO_KI_NB_JI_fillTareas));
        Menu estado = new Menu("Estado");
        for (WO_KI_NB_JI_EstadoTarea es : WO_KI_NB_JI_EstadoTarea.values()) {
            MenuItem mi = new MenuItem(es.WO_KI_NB_JI_etiqueta());
            mi.setDisable(es == t.WO_KI_NB_JI_getEstadoTipo());
            mi.setOnAction(e -> c.WO_KI_NB_JI_cambiarEstadoTarea(t, es).ifPresent(r ->
                WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success("Estado actualizado", t.WO_KI_NB_JI_getTitulo() + " → " + es.WO_KI_NB_JI_etiqueta())));
            estado.getItems().add(mi);
        }
        MenuItem borrar = new MenuItem("Eliminar");
        borrar.getStyleClass().add("danger-item");
        borrar.setOnAction(e -> {
            if (WO_KI_NB_JI_Ui.WO_KI_NB_JI_confirm(view, "¿Eliminar esta tarea?", "«" + t.WO_KI_NB_JI_getTitulo() + "» se borrará para siempre.", "Eliminar")
                && c.WO_KI_NB_JI_eliminarTarea(t.WO_KI_NB_JI_getId()))
                WO_KI_NB_JI_Notifier.WO_KI_NB_JI_deleted("Tarea eliminada", t.WO_KI_NB_JI_getTitulo());
        });
        return new ContextMenu(editar, estado, new SeparatorMenuItem(), borrar);
    }

    public VBox WO_KI_NB_JI_getView() { return view; }
}

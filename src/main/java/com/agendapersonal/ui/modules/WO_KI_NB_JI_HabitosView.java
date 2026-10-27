package com.agendapersonal.ui.modules;

import com.agendapersonal.domain.model.WO_KI_NB_JI_Habito;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_HabitoDAO;
import com.agendapersonal.service.WO_KI_NB_JI_CalendarioUnificadoService;
import com.agendapersonal.ui.WO_KI_NB_JI_Anim;
import com.agendapersonal.ui.WO_KI_NB_JI_IconFactory;
import com.agendapersonal.ui.WO_KI_NB_JI_Notifier;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
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
public class WO_KI_NB_JI_HabitosView {
    private static final String[] DIAS = {"L", "M", "M", "J", "V", "S", "D"};
    private final WO_KI_NB_JI_HabitoDAO dao = new WO_KI_NB_JI_HabitoDAO();
    private final VBox view;
    private final HBox resumen = new HBox(0);
    private final VBox filas = new VBox(0);
    private final TextField nombre = new TextField();

    public WO_KI_NB_JI_HabitosView() {
        VBox page = WO_KI_NB_JI_Ui.WO_KI_NB_JI_page();
        Button nuevo = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("add", "Nuevo hábito");
        nuevo.setOnAction(e -> { nombre.requestFocus(); WO_KI_NB_JI_Anim.WO_KI_NB_JI_pop(nombre); });
        page.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pageHeader("habits", "Hábitos",
            "Marca lo que cumpliste hoy y mantén viva tu racha.", nuevo));
        resumen.getStyleClass().addAll("card", "summary-strip");
        filas.getStyleClass().add("list-card");
        VBox lista = new VBox(0, filas, WO_KI_NB_JI_formulario());
        lista.getStyleClass().addAll("card", "card-flush");
        page.getChildren().addAll(resumen, lista);
        view = page;
        WO_KI_NB_JI_refresh();
    }

    private Node WO_KI_NB_JI_formulario() {
        nombre.setPromptText("Nuevo hábito (ej. Leer 20 minutos)");
        nombre.getStyleClass().add("bare-field");
        HBox.setHgrow(nombre, Priority.ALWAYS);
        ComboBox<String> frec = new ComboBox<>();
        frec.getItems().addAll("Todos los días", "Una vez por semana");
        frec.setValue("Todos los días");
        frec.getStyleClass().add("compact-combo");
        Button add = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("add", "Agregar");
        Runnable agregar = () -> {
            String n = nombre.getText().trim();
            if (n.isEmpty()) { WO_KI_NB_JI_Anim.WO_KI_NB_JI_shake(nombre); return; }
            String f = frec.getSelectionModel().getSelectedIndex() == 1 ? "SEMANAL" : "DIARIO";
            if (dao.WO_KI_NB_JI_insertar(new WO_KI_NB_JI_Habito(0, n, "", f, 1, "#2C7A4B", true))) {
                nombre.clear();
                WO_KI_NB_JI_refresh();
                WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success("Hábito creado", n + " · ¡a por la primera racha!");
            } else WO_KI_NB_JI_Notifier.WO_KI_NB_JI_error("No se pudo crear el hábito", null);
        };
        add.setOnAction(e -> agregar.run());
        nombre.setOnAction(e -> agregar.run());
        HBox row = new HBox(12, WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("add", 18), nombre, frec, add);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("add-row");
        return row;
    }

    private void WO_KI_NB_JI_refresh() {
        LocalDate hoy = LocalDate.now();
        LocalDate lunes = hoy.with(DayOfWeek.MONDAY);
        List<WO_KI_NB_JI_Habito> habitos = dao.WO_KI_NB_JI_listar();
        List<WO_KI_NB_JI_Habito> activos = habitos.stream().filter(WO_KI_NB_JI_Habito::WO_KI_NB_JI_isActivo).toList();
        Map<String, Set<Integer>> semana = dao.WO_KI_NB_JI_cumplidosEntre(lunes.toString(), lunes.plusDays(6).toString());
        Set<Integer> hechosHoy = semana.getOrDefault(hoy.toString(), Set.of());

        // ---- resumen
        List<WO_KI_NB_JI_Habito> deHoy = activos.stream().filter(h -> WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_aplicaHabito(h, hoy)).toList();
        long cumplidosHoy = deHoy.stream().filter(h -> hechosHoy.contains(h.WO_KI_NB_JI_getId())).count();
        int programados = 0, cumplidos = 0;
        for (LocalDate d = lunes; !d.isAfter(hoy); d = d.plusDays(1)) {
            Set<Integer> hechos = semana.getOrDefault(d.toString(), Set.of());
            for (WO_KI_NB_JI_Habito h : activos) if (WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_aplicaHabito(h, d)) {
                programados++;
                if (hechos.contains(h.WO_KI_NB_JI_getId())) cumplidos++;
            }
        }
        WO_KI_NB_JI_Habito mejor = null;
        int mejorRacha = 0;
        List<Integer> rachas = new ArrayList<>();
        for (WO_KI_NB_JI_Habito h : habitos) {
            int r = dao.WO_KI_NB_JI_racha(h.WO_KI_NB_JI_getId());
            rachas.add(r);
            if (h.WO_KI_NB_JI_isActivo() && r > mejorRacha) { mejorRacha = r; mejor = h; }
        }
        long faltan = deHoy.size() - cumplidosHoy;
        resumen.getChildren().setAll(
            WO_KI_NB_JI_celda("Hoy", cumplidosHoy + "", " de " + deHoy.size(),
                deHoy.isEmpty() ? "Sin hábitos para hoy." : faltan == 0 ? "¡Día completo! Excelente." : "Te faltan " + faltan + " para cerrar el día.",
                deHoy.isEmpty() ? 0 : (double) cumplidosHoy / deHoy.size()),
            WO_KI_NB_JI_celdaRacha(mejorRacha, mejor),
            WO_KI_NB_JI_celda("Esta semana", programados == 0 ? "—" : Math.round(100.0 * cumplidos / programados) + " %", "",
                cumplidos + " de " + programados + " registros cumplidos", -1));
        resumen.getChildren().getLast().getStyleClass().add("with-left-divider");

        // ---- filas
        filas.getChildren().clear();
        GridPane head = WO_KI_NB_JI_grid();
        head.add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h3("Tus hábitos"), 1, 0);
        HBox letras = new HBox(8);
        for (int i = 0; i < 7; i++) {
            Label l = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(DIAS[i], "day-letter");
            if (lunes.plusDays(i).equals(hoy)) l.getStyleClass().add("day-letter-today");
            letras.getChildren().add(l);
        }
        head.add(letras, 2, 0);
        Label rl = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("Racha", "day-letter");
        head.add(rl, 3, 0);
        GridPane.setHalignment(rl, javafx.geometry.HPos.RIGHT);
        head.getStyleClass().add("habit-head");
        filas.getChildren().add(head);

        if (habitos.isEmpty()) {
            filas.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_empty("habits", "Crea tu primer hábito",
                "Escribe abajo algo pequeño que quieras repetir, como «Beber 2 L de agua»."));
            return;
        }
        List<Node> nodos = new ArrayList<>();
        for (int i = 0; i < habitos.size(); i++) {
            WO_KI_NB_JI_Habito h = habitos.get(i);
            Node f = WO_KI_NB_JI_fila(h, rachas.get(i), hechosHoy.contains(h.WO_KI_NB_JI_getId()), lunes, hoy, semana);
            filas.getChildren().add(f);
            nodos.add(f);
        }
        WO_KI_NB_JI_Anim.WO_KI_NB_JI_stagger(nodos);
    }

    private GridPane WO_KI_NB_JI_grid() {
        GridPane g = new GridPane();
        g.setHgap(20);
        ColumnConstraints c0 = new ColumnConstraints(48);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        c1.setMinWidth(160);
        ColumnConstraints c2 = new ColumnConstraints(7 * 28 + 6 * 8);
        ColumnConstraints c3 = new ColumnConstraints(110);
        ColumnConstraints c4 = new ColumnConstraints(40);
        g.getColumnConstraints().addAll(c0, c1, c2, c3, c4);
        g.setAlignment(Pos.CENTER_LEFT);
        return g;
    }

    private Node WO_KI_NB_JI_fila(WO_KI_NB_JI_Habito h, int racha, boolean hecho, LocalDate lunes, LocalDate hoy, Map<String, Set<Integer>> semana) {
        boolean tocaHoy = h.WO_KI_NB_JI_isActivo() && WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_aplicaHabito(h, hoy);
        Button check = new Button();
        check.getStyleClass().addAll("habit-check");
        if (hecho) check.getStyleClass().add("checked");
        check.setGraphic(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("check", 20, "tint-on-accent"));
        check.setAccessibleText((hecho ? "Desmarcar " : "Marcar ") + h.WO_KI_NB_JI_getNombre());
        WO_KI_NB_JI_Ui.WO_KI_NB_JI_tooltip(check, hecho ? "Desmarcar hoy" : "Marcar como cumplido hoy");
        check.setDisable(!h.WO_KI_NB_JI_isActivo());
        check.setOnAction(e -> {
            WO_KI_NB_JI_Anim.WO_KI_NB_JI_pop(check);
            String d = hoy.toString();
            boolean ok = hecho ? dao.WO_KI_NB_JI_desmarcar(h.WO_KI_NB_JI_getId(), d) : dao.WO_KI_NB_JI_marcar(h.WO_KI_NB_JI_getId(), d);
            if (!ok) { WO_KI_NB_JI_Notifier.WO_KI_NB_JI_error("No se pudo actualizar el hábito", null); return; }
            if (!hecho) WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success("¡Bien hecho!", h.WO_KI_NB_JI_getNombre() + " · racha de " + dao.WO_KI_NB_JI_racha(h.WO_KI_NB_JI_getId()) + " días");
            else WO_KI_NB_JI_Notifier.WO_KI_NB_JI_info("Hábito desmarcado", h.WO_KI_NB_JI_getNombre());
            WO_KI_NB_JI_refresh();
        });

        Label n = new Label(h.WO_KI_NB_JI_getNombre());
        n.getStyleClass().add("row-title");
        String frec = "SEMANAL".equalsIgnoreCase(h.WO_KI_NB_JI_getFrecuencia()) ? "Una vez por semana" : "Todos los días";
        Label sub = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(h.WO_KI_NB_JI_isActivo() ? frec : "En pausa", "row-meta");
        VBox texts = new VBox(3, n, sub);

        HBox dias = new HBox(8);
        for (int i = 0; i < 7; i++) {
            LocalDate d = lunes.plusDays(i);
            boolean cumplido = semana.getOrDefault(d.toString(), Set.of()).contains(h.WO_KI_NB_JI_getId());
            StackPane dot = new StackPane();
            dot.getStyleClass().add("day-dot");
            if (cumplido) {
                dot.getStyleClass().add(d.equals(hoy) ? "day-done-today" : "day-done");
                dot.getChildren().add(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("check", 13, d.equals(hoy) ? "tint-on-accent" : "tint-green"));
            } else if (d.isAfter(hoy)) dot.getStyleClass().add("day-future");
            else if (!WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_aplicaHabito(h, d) || !h.WO_KI_NB_JI_isActivo()) {
                dot.getStyleClass().add("day-off");
                javafx.scene.layout.Region p = new javafx.scene.layout.Region();
                p.getStyleClass().add("day-off-dot");
                dot.getChildren().add(p);
            }
            else if (d.equals(hoy)) dot.getStyleClass().add("day-today");
            else dot.getStyleClass().add("day-missed");
            dias.getChildren().add(dot);
        }
        HBox rachaBox = new HBox(6);
        rachaBox.setAlignment(Pos.CENTER_RIGHT);
        if (racha > 0) {
            rachaBox.getChildren().addAll(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("flame", 17, "tint-orange"),
                WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(racha + (racha == 1 ? " día" : " días"), "streak"));
        } else rachaBox.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(tocaHoy ? "Empieza hoy" : "—", "row-meta"));

        Button mas = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("more", "Más opciones");
        mas.setOnAction(e -> WO_KI_NB_JI_menu(h).show(mas, javafx.geometry.Side.BOTTOM, 0, 4));

        GridPane g = WO_KI_NB_JI_grid();
        g.add(check, 0, 0);
        g.add(texts, 1, 0);
        g.add(dias, 2, 0);
        g.add(rachaBox, 3, 0);
        g.add(mas, 4, 0);
        g.getStyleClass().add("habit-row");
        if (!h.WO_KI_NB_JI_isActivo()) g.getStyleClass().add("row-paused");
        g.setPadding(new Insets(12, 20, 12, 20));
        return g;
    }

    private ContextMenu WO_KI_NB_JI_menu(WO_KI_NB_JI_Habito h) {
        MenuItem pausa = new MenuItem(h.WO_KI_NB_JI_isActivo() ? "Pausar hábito" : "Reanudar hábito");
        pausa.setOnAction(e -> {
            h.WO_KI_NB_JI_setActivo(!h.WO_KI_NB_JI_isActivo());
            if (dao.WO_KI_NB_JI_actualizar(h)) {
                WO_KI_NB_JI_refresh();
                WO_KI_NB_JI_Notifier.WO_KI_NB_JI_info(h.WO_KI_NB_JI_isActivo() ? "Hábito reanudado" : "Hábito en pausa", h.WO_KI_NB_JI_getNombre());
            }
        });
        MenuItem borrar = new MenuItem("Eliminar");
        borrar.getStyleClass().add("danger-item");
        borrar.setOnAction(e -> {
            if (WO_KI_NB_JI_Ui.WO_KI_NB_JI_confirm(view, "¿Eliminar «" + h.WO_KI_NB_JI_getNombre() + "»?", "También se borrará su historial y su racha.", "Eliminar")
                && dao.WO_KI_NB_JI_eliminar(h.WO_KI_NB_JI_getId())) {
                WO_KI_NB_JI_refresh();
                WO_KI_NB_JI_Notifier.WO_KI_NB_JI_deleted("Hábito eliminado", h.WO_KI_NB_JI_getNombre());
            }
        });
        return new ContextMenu(pausa, new SeparatorMenuItem(), borrar);
    }

    private VBox WO_KI_NB_JI_celda(String titulo, String valor, String sufijo, String nota, double progreso) {
        Label t = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(titulo, "stat-label");
        Label v = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(valor, "stat-value");
        HBox vr = new HBox(4, v);
        vr.setAlignment(Pos.BASELINE_LEFT);
        if (!sufijo.isEmpty()) vr.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(sufijo, "stat-suffix"));
        VBox b = new VBox(10, t, vr);
        if (progreso >= 0) b.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_bar(progreso, "green", 6));
        b.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(nota, "stat-note"));
        b.getStyleClass().add("summary-cell");
        HBox.setHgrow(b, Priority.ALWAYS);
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private VBox WO_KI_NB_JI_celdaRacha(int racha, WO_KI_NB_JI_Habito mejor) {
        Label t = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("Racha más larga", "stat-label");
        Label v = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(String.valueOf(racha), "stat-value");
        HBox vr = new HBox(6, WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("flame", 24, "tint-orange"), v, WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(racha == 1 ? " día" : " días", "stat-suffix"));
        vr.setAlignment(Pos.CENTER_LEFT);
        VBox b = new VBox(10, t, vr, WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(mejor == null ? "Marca tus hábitos para empezar." : mejor.WO_KI_NB_JI_getNombre(), "stat-note"));
        b.getStyleClass().addAll("summary-cell", "with-left-divider");
        HBox.setHgrow(b, Priority.ALWAYS);
        b.setMaxWidth(Double.MAX_VALUE);
        return b;
    }

    private javafx.scene.control.ScrollPane scroll;

    /** La página va dentro de un desplazamiento para ventanas pequeñas o listas largas. */
    public Node WO_KI_NB_JI_getView() {
        if (scroll == null) scroll = WO_KI_NB_JI_Ui.WO_KI_NB_JI_scroll(view);
        return scroll;
    }
}

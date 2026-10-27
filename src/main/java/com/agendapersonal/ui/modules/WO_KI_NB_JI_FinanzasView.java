package com.agendapersonal.ui.modules;

import com.agendapersonal.domain.model.WO_KI_NB_JI_Movimiento;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_MovimientoDAO;
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
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Finanzas: ingresos y gastos del mes, balance automático y en qué se va el dinero. */
public class WO_KI_NB_JI_FinanzasView {
    private static final List<String> GASTOS = List.of("Comida", "Transporte", "Vivienda", "Servicios", "Salud", "Ocio", "Compras", "Otros");
    private static final List<String> INGRESOS = List.of("Sueldo", "Ventas", "Ingreso extra", "Otros");
    private static final Map<String, String[]> CAT = Map.ofEntries(  // ícono, color
        Map.entry("Comida", new String[]{"food", "amber"}), Map.entry("Transporte", new String[]{"transport", "blue"}),
        Map.entry("Vivienda", new String[]{"home", "rose"}), Map.entry("Servicios", new String[]{"bolt", "teal"}),
        Map.entry("Salud", new String[]{"medical", "green"}), Map.entry("Ocio", new String[]{"leisure", "violet"}),
        Map.entry("Compras", new String[]{"shopping", "orange"}), Map.entry("Sueldo", new String[]{"work", "green"}),
        Map.entry("Ventas", new String[]{"coins", "green"}), Map.entry("Ingreso extra", new String[]{"sparkle", "green"}),
        Map.entry("Otros", new String[]{"tag", "slate"}));

    private final WO_KI_NB_JI_MovimientoDAO dao = new WO_KI_NB_JI_MovimientoDAO();
    private final VBox view;
    private YearMonth mes = YearMonth.now();
    private final Label mesLabel = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("", "month-label");
    private final HBox stats = new HBox(16);
    private final VBox movimientos = new VBox(0);
    private final VBox categorias = new VBox(14);
    private final WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented tipo = new WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented("Gasto", "Ingreso");
    private final ComboBox<String> cat = new ComboBox<>();
    private final TextField monto = new TextField();
    private final DatePicker fecha = new DatePicker(LocalDate.now());
    private final TextField desc = new TextField();

    public WO_KI_NB_JI_FinanzasView() {
        VBox page = WO_KI_NB_JI_Ui.WO_KI_NB_JI_page();
        page.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pageHeader("finance", "Finanzas",
            "Registra ingresos y gastos; el balance del mes se calcula solo.", WO_KI_NB_JI_selectorMes()));
        HBox listas = new HBox(24, WO_KI_NB_JI_panelMovimientos(), WO_KI_NB_JI_panelCategorias());
        page.getChildren().addAll(stats, WO_KI_NB_JI_formulario(), listas);
        view = page;
        WO_KI_NB_JI_fill();
    }

    private Node WO_KI_NB_JI_selectorMes() {
        Button prev = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("chevron-left", "Mes anterior");
        Button next = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("chevron-right", "Mes siguiente");
        prev.setOnAction(e -> { mes = mes.minusMonths(1); WO_KI_NB_JI_fill(); });
        next.setOnAction(e -> { mes = mes.plusMonths(1); WO_KI_NB_JI_fill(); });
        mesLabel.setMinWidth(130);
        mesLabel.setAlignment(Pos.CENTER);
        HBox box = new HBox(2, prev, mesLabel, next);
        box.getStyleClass().add("month-switch");
        box.setAlignment(Pos.CENTER);
        return box;
    }

    private Node WO_KI_NB_JI_formulario() {
        cat.getItems().setAll(GASTOS);
        cat.setValue("Comida");
        tipo.WO_KI_NB_JI_selectedProperty().addListener((o, a, b) -> {
            cat.getItems().setAll(b.intValue() == 1 ? INGRESOS : GASTOS);
            cat.getSelectionModel().selectFirst();
            tipo.WO_KI_NB_JI_node().getStyleClass().removeAll("seg-income", "seg-expense");
            tipo.WO_KI_NB_JI_node().getStyleClass().add(b.intValue() == 1 ? "seg-income" : "seg-expense");
        });
        tipo.WO_KI_NB_JI_node().getStyleClass().add("seg-expense");
        monto.setPromptText("0,00");
        monto.getStyleClass().add("bare-field");
        Label prefijo = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("S/", "input-prefix");
        prefijo.setMinWidth(Region.USE_PREF_SIZE);
        HBox montoBox = new HBox(8, prefijo, monto);
        WO_KI_NB_JI_Ui.WO_KI_NB_JI_fechaCorta(fecha);
        montoBox.getStyleClass().add("input-with-icon");
        montoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(monto, Priority.ALWAYS);
        desc.setPromptText("Ej. Almuerzo con el equipo");
        Button add = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("add", "Registrar");
        add.setOnAction(e -> WO_KI_NB_JI_registrar(montoBox));
        monto.setOnAction(e -> WO_KI_NB_JI_registrar(montoBox));
        desc.setOnAction(e -> WO_KI_NB_JI_registrar(montoBox));

        VBox fTipo = WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Tipo", tipo.WO_KI_NB_JI_node());
        HBox.setHgrow(fTipo, Priority.NEVER);
        VBox fCat = WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Categoría", cat);
        VBox fMonto = WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Monto", montoBox);
        VBox fFecha = WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Fecha", fecha);
        VBox fDesc = WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Descripción", desc);
        fCat.setPrefWidth(170); fMonto.setPrefWidth(150); fFecha.setPrefWidth(170); fDesc.setPrefWidth(260);
        HBox fila = new HBox(12, fTipo, fCat, fMonto, fFecha, fDesc, add);
        fila.setAlignment(Pos.BOTTOM_LEFT);
        VBox card = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        card.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h3("Nuevo movimiento"), fila);
        return card;
    }

    private void WO_KI_NB_JI_registrar(Node montoBox) {
        double m;
        try { m = WO_KI_NB_JI_Ui.WO_KI_NB_JI_parseNumber(monto.getText()); }
        catch (Exception x) { WO_KI_NB_JI_Anim.WO_KI_NB_JI_shake(montoBox); WO_KI_NB_JI_Notifier.WO_KI_NB_JI_warning("Monto inválido", "Escribe un número, por ejemplo 25,50."); return; }
        if (m <= 0) { WO_KI_NB_JI_Anim.WO_KI_NB_JI_shake(montoBox); WO_KI_NB_JI_Notifier.WO_KI_NB_JI_warning("Monto inválido", "El monto debe ser mayor que cero."); return; }
        if (fecha.getValue() == null) fecha.setValue(LocalDate.now());
        String t = tipo.WO_KI_NB_JI_selected() == 1 ? "INGRESO" : "GASTO";
        if (dao.WO_KI_NB_JI_insertar(new WO_KI_NB_JI_Movimiento(0, t, cat.getValue(), m, fecha.getValue().toString(), desc.getText().trim()))) {
            WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success(t.equals("INGRESO") ? "Ingreso registrado" : "Gasto registrado",
                cat.getValue() + " · " + WO_KI_NB_JI_Ui.WO_KI_NB_JI_money(m));
            monto.clear();
            desc.clear();
            mes = YearMonth.from(fecha.getValue());
            WO_KI_NB_JI_fill();
            monto.requestFocus();
        } else WO_KI_NB_JI_Notifier.WO_KI_NB_JI_error("No se pudo registrar el movimiento", null);
    }

    private Node WO_KI_NB_JI_panelMovimientos() {
        VBox card = new VBox(0, movimientos);
        card.getStyleClass().addAll("card", "card-flush");
        HBox.setHgrow(card, Priority.ALWAYS);
        return card;
    }

    private Node WO_KI_NB_JI_panelCategorias() {
        VBox card = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        card.setPrefWidth(330);
        card.setMinWidth(280);
        card.setMaxWidth(360);
        card.getChildren().add(categorias);
        return card;
    }

    private void WO_KI_NB_JI_fill() {
        String nombreMes = mes.getMonth().getDisplayName(TextStyle.FULL, WO_KI_NB_JI_Ui.ES);
        mesLabel.setText(Character.toUpperCase(nombreMes.charAt(0)) + nombreMes.substring(1) + " " + mes.getYear());
        List<WO_KI_NB_JI_Movimiento> lista = dao.WO_KI_NB_JI_listarMes(mes.toString());
        double ing = 0, gas = 0;
        int nIng = 0, nGas = 0;
        Map<String, Double> porCat = new LinkedHashMap<>();
        for (WO_KI_NB_JI_Movimiento m : lista) {
            if ("INGRESO".equals(m.WO_KI_NB_JI_getTipo())) { ing += m.WO_KI_NB_JI_getMonto(); nIng++; }
            else { gas += m.WO_KI_NB_JI_getMonto(); nGas++; porCat.merge(m.WO_KI_NB_JI_getCategoria(), m.WO_KI_NB_JI_getMonto(), Double::sum); }
        }
        double neto = ing - gas;
        VBox balance = WO_KI_NB_JI_Ui.WO_KI_NB_JI_stat("Balance del mes", WO_KI_NB_JI_Ui.WO_KI_NB_JI_money(neto),
            ing > 0 ? (neto >= 0 ? "Ahorras el " + Math.round(100 * neto / ing) + " % de lo que ingresas" : "Gastas más de lo que ingresas")
                : "Registra tus ingresos para ver tu ahorro", "finance", "inverse");
        balance.getStyleClass().add("stat-inverse");
        stats.getChildren().setAll(
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_stat("Ingresos", WO_KI_NB_JI_Ui.WO_KI_NB_JI_money(ing), nIng + (nIng == 1 ? " movimiento" : " movimientos"), "arrow-down-left", "green"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_stat("Gastos", WO_KI_NB_JI_Ui.WO_KI_NB_JI_money(gas), nGas + (nGas == 1 ? " movimiento" : " movimientos"), "arrow-up-right", "rose"),
            balance);

        // ---- lista de movimientos
        movimientos.getChildren().clear();
        HBox head = new HBox(8, WO_KI_NB_JI_Ui.WO_KI_NB_JI_h3("Movimientos de " + nombreMes), WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(lista.size() + (lista.size() == 1 ? " movimiento" : " movimientos"), "caption"));
        head.setAlignment(Pos.CENTER_LEFT);
        head.setPadding(new Insets(18, 22, 10, 22));
        movimientos.getChildren().add(head);
        if (lista.isEmpty()) {
            movimientos.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_empty("finance", "Sin movimientos este mes",
                "Registra tu primer gasto o ingreso con el formulario de arriba."));
        } else {
            List<Node> filas = new ArrayList<>();
            for (WO_KI_NB_JI_Movimiento m : lista) { Node f = WO_KI_NB_JI_fila(m); movimientos.getChildren().add(f); filas.add(f); }
            WO_KI_NB_JI_Anim.WO_KI_NB_JI_stagger(filas);
        }

        // ---- gastos por categoría
        categorias.getChildren().setAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h3("Gastos por categoría"));
        if (porCat.isEmpty()) {
            categorias.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Aún no hay gastos en " + nombreMes + "."));
            return;
        }
        List<Map.Entry<String, Double>> orden = porCat.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed()).toList();
        HBox stacked = new HBox(2);
        stacked.getStyleClass().add("stacked-bar");
        for (var en : orden) {
            Region seg = new Region();
            seg.getStyleClass().addAll("stacked-seg", "fill-" + WO_KI_NB_JI_color(en.getKey()));
            seg.prefWidthProperty().bind(stacked.widthProperty().multiply(en.getValue() / gas));
            seg.setMinWidth(3);
            stacked.getChildren().add(seg);
        }
        categorias.getChildren().add(stacked);
        for (var en : orden) {
            Region sq = new Region();
            sq.getStyleClass().addAll("legend-dot", "fill-" + WO_KI_NB_JI_color(en.getKey()));
            Label n = new Label(en.getKey());
            n.getStyleClass().add("legend-name");
            HBox.setHgrow(n, Priority.ALWAYS);
            n.setMaxWidth(Double.MAX_VALUE);
            Label pct = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(Math.round(100 * en.getValue() / gas) + " %", "caption");
            Label val = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(WO_KI_NB_JI_Ui.WO_KI_NB_JI_money(en.getValue()), "legend-value");
            val.setMinWidth(96);
            val.setAlignment(Pos.CENTER_RIGHT);
            HBox r = new HBox(10, sq, n, pct, val);
            r.setAlignment(Pos.CENTER_LEFT);
            categorias.getChildren().add(r);
        }
        String top = orden.getFirst().getKey();
        HBox tip = new HBox(10, WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("idea", 16, "tint-teal"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(top + " es tu mayor gasto de " + nombreMes + " (" + Math.round(100 * orden.getFirst().getValue() / gas) + " %).", "tip-text"));
        tip.getStyleClass().add("tip");
        ((Label) tip.getChildren().get(1)).setWrapText(true);
        categorias.getChildren().add(tip);
    }

    private static String WO_KI_NB_JI_color(String c) { return CAT.getOrDefault(c, new String[]{"tag", "slate"})[1]; }
    private static String WO_KI_NB_JI_icono(String c) { return CAT.getOrDefault(c, new String[]{"tag", "slate"})[0]; }

    private Node WO_KI_NB_JI_fila(WO_KI_NB_JI_Movimiento m) {
        boolean ingreso = "INGRESO".equals(m.WO_KI_NB_JI_getTipo());
        StackPane chip = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconChip(WO_KI_NB_JI_icono(m.WO_KI_NB_JI_getCategoria()), WO_KI_NB_JI_color(m.WO_KI_NB_JI_getCategoria()), 40, 18);
        String titulo = m.WO_KI_NB_JI_getDescripcion() == null || m.WO_KI_NB_JI_getDescripcion().isBlank() ? m.WO_KI_NB_JI_getCategoria() : m.WO_KI_NB_JI_getDescripcion();
        Label t = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(titulo, "row-title");
        String cuando;
        try { cuando = WO_KI_NB_JI_Ui.WO_KI_NB_JI_friendlyDate(LocalDate.parse(m.WO_KI_NB_JI_getFecha())); } catch (Exception e) { cuando = m.WO_KI_NB_JI_getFecha(); }
        Label sub = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(m.WO_KI_NB_JI_getCategoria() + " · " + cuando, "row-meta");
        VBox texts = new VBox(3, t, sub);
        HBox.setHgrow(texts, Priority.ALWAYS);
        Label monto = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label((ingreso ? "+ " : "− ") + WO_KI_NB_JI_Ui.WO_KI_NB_JI_money(m.WO_KI_NB_JI_getMonto()), "amount");
        if (ingreso) monto.getStyleClass().add("amount-income");
        Button mas = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("more", "Más opciones");
        MenuItem borrar = new MenuItem("Eliminar movimiento");
        borrar.getStyleClass().add("danger-item");
        borrar.setOnAction(e -> {
            if (WO_KI_NB_JI_Ui.WO_KI_NB_JI_confirm(view, "¿Eliminar este movimiento?", titulo + " · " + WO_KI_NB_JI_Ui.WO_KI_NB_JI_money(m.WO_KI_NB_JI_getMonto()), "Eliminar")
                && dao.WO_KI_NB_JI_eliminar(m.WO_KI_NB_JI_getId())) {
                WO_KI_NB_JI_fill();
                WO_KI_NB_JI_Notifier.WO_KI_NB_JI_deleted("Movimiento eliminado", titulo + " · " + WO_KI_NB_JI_Ui.WO_KI_NB_JI_money(m.WO_KI_NB_JI_getMonto()));
            }
        });
        ContextMenu cm = new ContextMenu(borrar);
        mas.setOnAction(e -> cm.show(mas, javafx.geometry.Side.BOTTOM, 0, 4));
        HBox row = new HBox(14, chip, texts, monto, mas);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("list-row", "with-divider");
        return row;
    }

    private javafx.scene.control.ScrollPane scroll;

    /** La página va dentro de un desplazamiento para ventanas pequeñas o listas largas. */
    public Node WO_KI_NB_JI_getView() {
        if (scroll == null) scroll = WO_KI_NB_JI_Ui.WO_KI_NB_JI_scroll(view);
        return scroll;
    }
}

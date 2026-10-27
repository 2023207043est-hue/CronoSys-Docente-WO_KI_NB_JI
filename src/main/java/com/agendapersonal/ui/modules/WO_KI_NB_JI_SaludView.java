package com.agendapersonal.ui.modules;

import com.agendapersonal.domain.model.WO_KI_NB_JI_RegistroSalud;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_SaludDAO;
import com.agendapersonal.ui.WO_KI_NB_JI_Anim;
import com.agendapersonal.ui.WO_KI_NB_JI_IconFactory;
import com.agendapersonal.ui.WO_KI_NB_JI_Notifier;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Salud: lo de hoy en tarjetas (agua, sueño, ejercicio, peso, ánimo), registro rápido e historial. */
public class WO_KI_NB_JI_SaludView {
    /** Tipo guardado en la BD → etiqueta, unidad, ícono, color y meta diaria (0 = sin meta). */
    private record WO_KI_NB_JI_Tipo(String clave, String nombre, String unidad, String icono, String tono, double meta, String ejemplo) {}
    private static final List<WO_KI_NB_JI_Tipo> TIPOS = List.of(
        new WO_KI_NB_JI_Tipo("AGUA", "Agua", "vasos", "water", "blue", 8, "2"),
        new WO_KI_NB_JI_Tipo("SUENO", "Sueño", "h", "moon", "violet", 8, "7,5"),
        new WO_KI_NB_JI_Tipo("EJERCICIO", "Ejercicio", "min", "activity", "green", 30, "30"),
        new WO_KI_NB_JI_Tipo("PESO", "Peso", "kg", "scale", "teal", 0, "68,4"),
        new WO_KI_NB_JI_Tipo("ANIMO", "Ánimo", "/10", "mood", "amber", 0, "7"));

    private final WO_KI_NB_JI_SaludDAO dao = new WO_KI_NB_JI_SaludDAO();
    private final VBox view;
    private final TilePane tarjetas = new TilePane(16, 16);
    private final VBox historial = new VBox(0);
    private final WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented rango = new WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented("Esta semana", "Este mes", "Todo");
    private final ToggleGroup grupoTipo = new ToggleGroup();
    private final TextField valor = new TextField();
    private final Label unidad = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("", "input-suffix");
    private final DatePicker fecha = new DatePicker(LocalDate.now());
    private final TextField notas = new TextField();
    private WO_KI_NB_JI_Tipo tipo = TIPOS.getFirst();
    private ScrollPane scroll;

    public WO_KI_NB_JI_SaludView() {
        VBox page = WO_KI_NB_JI_Ui.WO_KI_NB_JI_page();
        page.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pageHeader("health", "Salud",
            "Un registro rápido al día: agua, sueño, ejercicio, peso y ánimo."));
        tarjetas.setPrefColumns(5);
        tarjetas.setPrefTileWidth(200);
        tarjetas.setTileAlignment(Pos.TOP_LEFT);
        rango.WO_KI_NB_JI_selectedProperty().addListener((o, a, b) -> WO_KI_NB_JI_fill());
        VBox hist = new VBox(0, historial);
        hist.getStyleClass().addAll("card", "card-flush");
        page.getChildren().addAll(tarjetas, WO_KI_NB_JI_formulario(), hist);
        view = page;
        WO_KI_NB_JI_fill();
    }

    private Node WO_KI_NB_JI_formulario() {
        FlowPane chips = new FlowPane(8, 8);
        for (WO_KI_NB_JI_Tipo t : TIPOS) {
            ToggleButton b = new ToggleButton(t.nombre(), WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon(t.icono(), 16));
            b.getStyleClass().addAll("choice-chip", "choice-" + t.tono());
            b.setToggleGroup(grupoTipo);
            b.setUserData(t);
            b.setOnAction(e -> {
                if (!b.isSelected()) { b.setSelected(true); return; }
                tipo = t;
                WO_KI_NB_JI_actualizarUnidad();
                WO_KI_NB_JI_Anim.WO_KI_NB_JI_pop(b);
            });
            chips.getChildren().add(b);
        }
        ((ToggleButton) chips.getChildren().getFirst()).setSelected(true);

        valor.getStyleClass().add("bare-field");
        HBox valorBox = new HBox(8, valor, unidad);
        valorBox.getStyleClass().add("input-with-icon");
        valorBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(valor, Priority.ALWAYS);
        notas.setPromptText("Ej. Después del desayuno");
        WO_KI_NB_JI_Ui.WO_KI_NB_JI_fechaCorta(fecha);
        unidad.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        Button add = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("add", "Registrar");
        add.setOnAction(e -> WO_KI_NB_JI_registrar(valorBox));
        valor.setOnAction(e -> WO_KI_NB_JI_registrar(valorBox));
        notas.setOnAction(e -> WO_KI_NB_JI_registrar(valorBox));
        WO_KI_NB_JI_actualizarUnidad();

        VBox fValor = WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Cantidad", valorBox);
        VBox fFecha = WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Fecha", fecha);
        VBox fNotas = WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Notas (opcional)", notas);
        fValor.setPrefWidth(170); fFecha.setPrefWidth(180); fNotas.setPrefWidth(320);
        HBox fila = new HBox(12, fValor, fFecha, fNotas, add);
        fila.setAlignment(Pos.BOTTOM_LEFT);
        VBox card = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        card.setSpacing(14);
        card.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h3("Nuevo registro"), chips, fila);
        return card;
    }

    private void WO_KI_NB_JI_actualizarUnidad() {
        unidad.setText(tipo.unidad());
        valor.setPromptText("Ej. " + tipo.ejemplo());
    }

    private void WO_KI_NB_JI_registrar(Node valorBox) {
        double v;
        try { v = WO_KI_NB_JI_Ui.WO_KI_NB_JI_parseNumber(valor.getText()); }
        catch (Exception x) { WO_KI_NB_JI_Anim.WO_KI_NB_JI_shake(valorBox); WO_KI_NB_JI_Notifier.WO_KI_NB_JI_warning("Valor inválido", "Escribe un número, por ejemplo " + tipo.ejemplo() + "."); return; }
        if (v < 0 || ("ANIMO".equals(tipo.clave()) && v > 10)) {
            WO_KI_NB_JI_Anim.WO_KI_NB_JI_shake(valorBox);
            WO_KI_NB_JI_Notifier.WO_KI_NB_JI_warning("Valor fuera de rango", "ANIMO".equals(tipo.clave()) ? "El ánimo va de 0 a 10." : "El valor no puede ser negativo.");
            return;
        }
        if (fecha.getValue() == null) fecha.setValue(LocalDate.now());
        if (dao.WO_KI_NB_JI_insertar(new WO_KI_NB_JI_RegistroSalud(0, tipo.clave(), v, tipo.unidad(), fecha.getValue().toString(), notas.getText().trim()))) {
            WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success(tipo.nombre() + " registrado", WO_KI_NB_JI_num(v) + " " + tipo.unidad() + (fecha.getValue().equals(LocalDate.now()) ? " hoy" : ""));
            valor.clear();
            notas.clear();
            WO_KI_NB_JI_fill();
        } else WO_KI_NB_JI_Notifier.WO_KI_NB_JI_error("No se pudo guardar el registro", null);
    }

    private void WO_KI_NB_JI_fill() {
        List<WO_KI_NB_JI_RegistroSalud> todos = dao.WO_KI_NB_JI_listar(null, 500);
        LocalDate hoy = LocalDate.now();
        String h = hoy.toString();

        // ---- tarjetas de hoy
        tarjetas.getChildren().clear();
        for (WO_KI_NB_JI_Tipo t : TIPOS) {
            List<WO_KI_NB_JI_RegistroSalud> deTipo = todos.stream().filter(r -> t.clave().equals(r.WO_KI_NB_JI_getTipo())).toList();
            List<WO_KI_NB_JI_RegistroSalud> deHoy = deTipo.stream().filter(r -> h.equals(r.WO_KI_NB_JI_getFecha())).toList();
            String valorTxt, nota;
            double progreso = -1;
            switch (t.clave()) {
                case "AGUA", "EJERCICIO" -> {
                    double suma = deHoy.stream().mapToDouble(WO_KI_NB_JI_RegistroSalud::WO_KI_NB_JI_getValor).sum();
                    valorTxt = WO_KI_NB_JI_num(suma) + " " + t.unidad();
                    nota = "Meta: " + WO_KI_NB_JI_num(t.meta()) + " " + t.unidad();
                    progreso = suma / t.meta();
                }
                case "SUENO" -> {
                    if (deHoy.isEmpty()) { valorTxt = "—"; nota = "Registra cómo dormiste"; }
                    else {
                        double v = deHoy.getFirst().WO_KI_NB_JI_getValor();
                        valorTxt = WO_KI_NB_JI_horas(v);
                        nota = v >= t.meta() ? "¡Meta de 8 h cumplida!" : "Meta: 8 h";
                        progreso = v / t.meta();
                    }
                }
                case "PESO" -> {
                    if (deTipo.isEmpty()) { valorTxt = "—"; nota = "Aún sin registros"; }
                    else {
                        WO_KI_NB_JI_RegistroSalud ult = deTipo.getFirst();
                        valorTxt = WO_KI_NB_JI_num(ult.WO_KI_NB_JI_getValor()) + " kg";
                        nota = WO_KI_NB_JI_cambioPeso(deTipo, ult);
                    }
                }
                default -> {
                    if (deHoy.isEmpty()) { valorTxt = "—"; nota = "¿Cómo te sientes hoy?"; }
                    else {
                        double v = deHoy.getFirst().WO_KI_NB_JI_getValor();
                        valorTxt = v >= 8 ? "Muy bien" : v >= 6 ? "Bien" : v >= 4 ? "Regular" : "Bajo";
                        nota = WO_KI_NB_JI_num(v) + " de 10";
                        progreso = v / 10.0;
                    }
                }
            }
            tarjetas.getChildren().add(WO_KI_NB_JI_tarjeta(t, valorTxt, nota, progreso));
        }

        // ---- historial
        LocalDate desde = switch (rango.WO_KI_NB_JI_selected()) { case 0 -> hoy.minusDays(6); case 1 -> hoy.withDayOfMonth(1); default -> LocalDate.MIN; };
        List<WO_KI_NB_JI_RegistroSalud> lista = todos.stream().filter(r -> {
            try { return !LocalDate.parse(r.WO_KI_NB_JI_getFecha()).isBefore(desde); } catch (Exception e) { return true; }
        }).toList();
        historial.getChildren().clear();
        HBox head = new HBox(8, WO_KI_NB_JI_Ui.WO_KI_NB_JI_h3("Historial"), WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), rango.WO_KI_NB_JI_node());
        head.setAlignment(Pos.CENTER_LEFT);
        head.setPadding(new Insets(16, 22, 10, 22));
        historial.getChildren().add(head);
        if (lista.isEmpty()) {
            historial.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_empty("health", "Sin registros en este periodo", "Usa el formulario de arriba para anotar tu día."));
            return;
        }
        GridPane th = WO_KI_NB_JI_gridFila();
        th.add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_overline("Fecha"), 0, 0);
        th.add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_overline("Tipo"), 1, 0);
        th.add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_overline("Valor"), 2, 0);
        th.add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_overline("Notas"), 3, 0);
        th.setPadding(new Insets(4, 22, 8, 22));
        historial.getChildren().add(th);
        List<Node> filas = new ArrayList<>();
        for (WO_KI_NB_JI_RegistroSalud r : lista) { Node f = WO_KI_NB_JI_fila(r); historial.getChildren().add(f); filas.add(f); }
        WO_KI_NB_JI_Anim.WO_KI_NB_JI_stagger(filas);
    }

    private static String WO_KI_NB_JI_cambioPeso(List<WO_KI_NB_JI_RegistroSalud> pesos, WO_KI_NB_JI_RegistroSalud ult) {
        try {
            LocalDate fUlt = LocalDate.parse(ult.WO_KI_NB_JI_getFecha());
            for (WO_KI_NB_JI_RegistroSalud r : pesos) {
                LocalDate f = LocalDate.parse(r.WO_KI_NB_JI_getFecha());
                if (!f.isAfter(fUlt.minusDays(6)) && r != ult) {
                    double d = ult.WO_KI_NB_JI_getValor() - r.WO_KI_NB_JI_getValor();
                    if (Math.abs(d) < 0.05) return "Estable esta semana";
                    return (d > 0 ? "+" : "−") + WO_KI_NB_JI_num(Math.abs(d)) + " kg en una semana";
                }
            }
        } catch (Exception ignored) {}
        return "Último registro: " + WO_KI_NB_JI_Ui.WO_KI_NB_JI_friendlyDate(LocalDate.parse(ult.WO_KI_NB_JI_getFecha())).toLowerCase(Locale.ROOT);
    }

    private VBox WO_KI_NB_JI_tarjeta(WO_KI_NB_JI_Tipo t, String valorTxt, String nota, double progreso) {
        HBox top = new HBox(10, WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconChip(t.icono(), t.tono(), 34, 18), WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(t.nombre(), "stat-label"));
        top.setAlignment(Pos.CENTER_LEFT);
        Label v = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(valorTxt, "stat-value", "stat-value-sm");
        VBox b = new VBox(10, top, v);
        if (progreso >= 0) b.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_bar(progreso, t.tono(), 6));
        b.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(nota, "stat-note"));
        b.getStyleClass().addAll("card", "stat-card");
        b.setPadding(new Insets(18, 20, 18, 20));
        b.setPrefWidth(200);
        b.setMinHeight(150);
        return b;
    }

    private GridPane WO_KI_NB_JI_gridFila() {
        GridPane g = new GridPane();
        g.setHgap(16);
        ColumnConstraints c0 = new ColumnConstraints(130);
        ColumnConstraints c1 = new ColumnConstraints(140);
        ColumnConstraints c2 = new ColumnConstraints(120);
        ColumnConstraints c3 = new ColumnConstraints();
        c3.setHgrow(Priority.ALWAYS);
        ColumnConstraints c4 = new ColumnConstraints(40);
        g.getColumnConstraints().addAll(c0, c1, c2, c3, c4);
        g.setAlignment(Pos.CENTER_LEFT);
        return g;
    }

    private Node WO_KI_NB_JI_fila(WO_KI_NB_JI_RegistroSalud r) {
        WO_KI_NB_JI_Tipo t = TIPOS.stream().filter(x -> x.clave().equals(r.WO_KI_NB_JI_getTipo())).findFirst().orElse(TIPOS.getLast());
        String cuando;
        try { cuando = WO_KI_NB_JI_Ui.WO_KI_NB_JI_friendlyDate(LocalDate.parse(r.WO_KI_NB_JI_getFecha())); } catch (Exception e) { cuando = r.WO_KI_NB_JI_getFecha(); }
        Label pill = WO_KI_NB_JI_Ui.WO_KI_NB_JI_pill(t.nombre(), t.tono());
        pill.setGraphic(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon(t.icono(), 13, "tint-" + t.tono()));
        pill.setGraphicTextGap(6);
        String unidadTxt = r.WO_KI_NB_JI_getUnidad() != null ? r.WO_KI_NB_JI_getUnidad() : t.unidad();
        Label val = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("SUENO".equals(t.clave()) ? WO_KI_NB_JI_horas(r.WO_KI_NB_JI_getValor()) : WO_KI_NB_JI_num(r.WO_KI_NB_JI_getValor()) + " " + unidadTxt, "row-title");
        Label nota = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(r.WO_KI_NB_JI_getNotas() == null || r.WO_KI_NB_JI_getNotas().isBlank() ? "—" : r.WO_KI_NB_JI_getNotas(), "row-meta");
        Button mas = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("more", "Más opciones");
        MenuItem borrar = new MenuItem("Eliminar registro");
        borrar.getStyleClass().add("danger-item");
        borrar.setOnAction(e -> {
            if (WO_KI_NB_JI_Ui.WO_KI_NB_JI_confirm(view, "¿Eliminar este registro?", t.nombre() + " · " + val.getText(), "Eliminar") && dao.WO_KI_NB_JI_eliminar(r.WO_KI_NB_JI_getId())) {
                WO_KI_NB_JI_fill();
                WO_KI_NB_JI_Notifier.WO_KI_NB_JI_deleted("Registro eliminado", t.nombre() + " · " + val.getText());
            }
        });
        ContextMenu cm = new ContextMenu(borrar);
        mas.setOnAction(e -> cm.show(mas, javafx.geometry.Side.BOTTOM, 0, 4));
        GridPane g = WO_KI_NB_JI_gridFila();
        g.add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(cuando, "row-meta-strong"), 0, 0);
        g.add(pill, 1, 0);
        g.add(val, 2, 0);
        g.add(nota, 3, 0);
        g.add(mas, 4, 0);
        g.getStyleClass().addAll("list-row", "with-divider");
        g.setPadding(new Insets(10, 22, 10, 22));
        return g;
    }

    private static String WO_KI_NB_JI_num(double v) {
        if (Math.abs(v - Math.round(v)) < 1e-9) return String.valueOf(Math.round(v));
        return String.format(WO_KI_NB_JI_Ui.ES, "%.1f", v);
    }

    private static String WO_KI_NB_JI_horas(double v) {
        int h = (int) v;
        int m = (int) Math.round((v - h) * 60);
        if (m == 60) { h++; m = 0; }
        return m == 0 ? h + " h" : h + " h " + m + " min";
    }

    public Node WO_KI_NB_JI_getView() {
        if (scroll == null) scroll = WO_KI_NB_JI_Ui.WO_KI_NB_JI_scroll(view);
        return scroll;
    }
}

package com.agendapersonal.ui.modules;

import com.agendapersonal.domain.model.wkniMovimiento;
import com.agendapersonal.infrastructure.repo.wkniMovimientoDAO;
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
public class wkniFinanzasView {
    private static final List<String> wkniGASTOS = List.of("Comida", "Transporte", "Vivienda", "Servicios", "Salud", "Ocio", "Compras", "Otros");
    private static final List<String> wkniINGRESOS = List.of("Sueldo", "Ventas", "Ingreso extra", "Otros");
    private static final Map<String, String[]> wkniCAT = Map.ofEntries(  // ícono, color
        Map.entry("Comida", new String[]{"food", "amber"}), Map.entry("Transporte", new String[]{"transport", "blue"}),
        Map.entry("Vivienda", new String[]{"home", "rose"}), Map.entry("Servicios", new String[]{"bolt", "teal"}),
        Map.entry("Salud", new String[]{"medical", "green"}), Map.entry("Ocio", new String[]{"leisure", "violet"}),
        Map.entry("Compras", new String[]{"shopping", "orange"}), Map.entry("Sueldo", new String[]{"work", "green"}),
        Map.entry("Ventas", new String[]{"coins", "green"}), Map.entry("Ingreso extra", new String[]{"sparkle", "green"}),
        Map.entry("Otros", new String[]{"tag", "slate"}));

    private final wkniMovimientoDAO wkniDao = new wkniMovimientoDAO();
    private final VBox wkniView;
    private YearMonth wkniMes = YearMonth.now();
    private final Label wkniMesLabel = wkniUi.wkniLabel("", "month-label");
    private final HBox wkniStats = new HBox(16);
    private final VBox wkniMovimientos = new VBox(0);
    private final VBox wkniCategorias = new VBox(14);
    private final wkniUi.wkniSegmented wkni_tipo = new wkniUi.wkniSegmented("Gasto", "Ingreso");
    private final ComboBox<String> wkniCat = new ComboBox<>();
    private final TextField wkniMonto = new TextField();
    private final DatePicker wkni_fecha = new DatePicker(LocalDate.now());
    private final TextField wkniDesc = new TextField();

    public wkniFinanzasView() {
        VBox wkniPage = wkniUi.wkniPage();
        wkniPage.getChildren().add(wkniUi.wkniPageHeader("finance", "Finanzas",
            "Registra ingresos y gastos; el balance del mes se calcula solo.", wkniSelectorMes()));
        HBox wkniListas = new HBox(24, wkniPanelMovimientos(), wkniPanelCategorias());
        wkniPage.getChildren().addAll(wkniStats, wkniFormulario(), wkniListas);
        wkniView = wkniPage;
        wkniFill();
    }

    private Node wkniSelectorMes() {
        Button wkniPrev = wkniUi.wkniIconButton("chevron-left", "Mes anterior");
        Button wkniNext = wkniUi.wkniIconButton("chevron-right", "Mes siguiente");
        wkniPrev.setOnAction(wkniE -> { wkniMes = wkniMes.minusMonths(1); wkniFill(); });
        wkniNext.setOnAction(wkniE -> { wkniMes = wkniMes.plusMonths(1); wkniFill(); });
        wkniMesLabel.setMinWidth(130);
        wkniMesLabel.setAlignment(Pos.CENTER);
        HBox wkniBox = new HBox(2, wkniPrev, wkniMesLabel, wkniNext);
        wkniBox.getStyleClass().add("month-switch");
        wkniBox.setAlignment(Pos.CENTER);
        return wkniBox;
    }

    private Node wkniFormulario() {
        wkniCat.getItems().setAll(wkniGASTOS);
        wkniCat.setValue("Comida");
        wkni_tipo.wkniSelectedProperty().addListener((wkniO, wkniA, wkniB) -> {
            wkniCat.getItems().setAll(wkniB.intValue() == 1 ? wkniINGRESOS : wkniGASTOS);
            wkniCat.getSelectionModel().selectFirst();
            wkni_tipo.wkniNode().getStyleClass().removeAll("seg-income", "seg-expense");
            wkni_tipo.wkniNode().getStyleClass().add(wkniB.intValue() == 1 ? "seg-income" : "seg-expense");
        });
        wkni_tipo.wkniNode().getStyleClass().add("seg-expense");
        wkniMonto.setPromptText("0,00");
        wkniMonto.getStyleClass().add("bare-field");
        Label wkniPrefijo = wkniUi.wkniLabel("S/", "input-prefix");
        wkniPrefijo.setMinWidth(Region.USE_PREF_SIZE);
        HBox wkniMontoBox = new HBox(8, wkniPrefijo, wkniMonto);
        wkniUi.wkniFechaCorta(wkni_fecha);
        wkniMontoBox.getStyleClass().add("input-with-icon");
        wkniMontoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(wkniMonto, Priority.ALWAYS);
        wkniDesc.setPromptText("Ej. Almuerzo con el equipo");
        Button wkniAdd = wkniUi.wkniPrimary("add", "Registrar");
        wkniAdd.setOnAction(wkniE -> wkniRegistrar(wkniMontoBox));
        wkniMonto.setOnAction(wkniE -> wkniRegistrar(wkniMontoBox));
        wkniDesc.setOnAction(wkniE -> wkniRegistrar(wkniMontoBox));

        VBox wkniFTipo = wkniUi.wkniField("Tipo", wkni_tipo.wkniNode());
        HBox.setHgrow(wkniFTipo, Priority.NEVER);
        VBox wkniFCat = wkniUi.wkniField("Categoría", wkniCat);
        VBox wkniFMonto = wkniUi.wkniField("Monto", wkniMontoBox);
        VBox wkniFFecha = wkniUi.wkniField("Fecha", wkni_fecha);
        VBox wkniFDesc = wkniUi.wkniField("Descripción", wkniDesc);
        wkniFCat.setPrefWidth(170); wkniFMonto.setPrefWidth(150); wkniFFecha.setPrefWidth(170); wkniFDesc.setPrefWidth(260);
        HBox wkniFila = new HBox(12, wkniFTipo, wkniFCat, wkniFMonto, wkniFFecha, wkniFDesc, wkniAdd);
        wkniFila.setAlignment(Pos.BOTTOM_LEFT);
        VBox wkniCard = wkniUi.wkniCard();
        wkniCard.getChildren().addAll(wkniUi.wkniH3("Nuevo movimiento"), wkniFila);
        return wkniCard;
    }

    private void wkniRegistrar(Node wkniMontoBox) {
        double wkniM;
        try { wkniM = wkniUi.wkniParseNumber(wkniMonto.getText()); }
        catch (Exception wkniX) { wkniAnim.wkniShake(wkniMontoBox); wkniNotifier.wkniWarning("Monto inválido", "Escribe un número, por ejemplo 25,50."); return; }
        if (wkniM <= 0) { wkniAnim.wkniShake(wkniMontoBox); wkniNotifier.wkniWarning("Monto inválido", "El monto debe ser mayor que cero."); return; }
        if (wkni_fecha.getValue() == null) wkni_fecha.setValue(LocalDate.now());
        String wkniT = wkni_tipo.wkniSelected() == 1 ? "INGRESO" : "GASTO";
        if (wkniDao.wkniInsertar(new wkniMovimiento(0, wkniT, wkniCat.getValue(), wkniM, wkni_fecha.getValue().toString(), wkniDesc.getText().trim()))) {
            wkniNotifier.wkniSuccess(wkniT.equals("INGRESO") ? "Ingreso registrado" : "Gasto registrado",
                wkniCat.getValue() + " · " + wkniUi.wkniMoney(wkniM));
            wkniMonto.clear();
            wkniDesc.clear();
            wkniMes = YearMonth.from(wkni_fecha.getValue());
            wkniFill();
            wkniMonto.requestFocus();
        } else wkniNotifier.wkniError("No se pudo registrar el movimiento", null);
    }

    private Node wkniPanelMovimientos() {
        VBox wkniCard = new VBox(0, wkniMovimientos);
        wkniCard.getStyleClass().addAll("card", "card-flush");
        HBox.setHgrow(wkniCard, Priority.ALWAYS);
        return wkniCard;
    }

    private Node wkniPanelCategorias() {
        VBox wkniCard = wkniUi.wkniCard();
        wkniCard.setPrefWidth(330);
        wkniCard.setMinWidth(280);
        wkniCard.setMaxWidth(360);
        wkniCard.getChildren().add(wkniCategorias);
        return wkniCard;
    }

    private void wkniFill() {
        String wkniNombreMes = wkniMes.getMonth().getDisplayName(TextStyle.FULL, wkniUi.wkniES);
        wkniMesLabel.setText(Character.toUpperCase(wkniNombreMes.charAt(0)) + wkniNombreMes.substring(1) + " " + wkniMes.getYear());
        List<wkniMovimiento> wkniLista = wkniDao.wkniListarMes(wkniMes.toString());
        double wkniIng = 0, wkniGas = 0;
        int wkniNIng = 0, wkniNGas = 0;
        Map<String, Double> wkniPorCat = new LinkedHashMap<>();
        for (wkniMovimiento wkniM : wkniLista) {
            if ("INGRESO".equals(wkniM.wkniGetTipo())) { wkniIng += wkniM.wkniGetMonto(); wkniNIng++; }
            else { wkniGas += wkniM.wkniGetMonto(); wkniNGas++; wkniPorCat.merge(wkniM.wkniGetCategoria(), wkniM.wkniGetMonto(), Double::sum); }
        }
        double wkniNeto = wkniIng - wkniGas;
        VBox wkniBalance = wkniUi.wkniStat("Balance del mes", wkniUi.wkniMoney(wkniNeto),
            wkniIng > 0 ? (wkniNeto >= 0 ? "Ahorras el " + Math.round(100 * wkniNeto / wkniIng) + " % de lo que ingresas" : "Gastas más de lo que ingresas")
                : "Registra tus ingresos para ver tu ahorro", "finance", "inverse");
        wkniBalance.getStyleClass().add("stat-inverse");
        wkniStats.getChildren().setAll(
            wkniUi.wkniStat("Ingresos", wkniUi.wkniMoney(wkniIng), wkniNIng + (wkniNIng == 1 ? " movimiento" : " movimientos"), "arrow-down-left", "green"),
            wkniUi.wkniStat("Gastos", wkniUi.wkniMoney(wkniGas), wkniNGas + (wkniNGas == 1 ? " movimiento" : " movimientos"), "arrow-up-right", "rose"),
            wkniBalance);

        // ---- lista de movimientos
        wkniMovimientos.getChildren().clear();
        HBox wkniHead = new HBox(8, wkniUi.wkniH3("Movimientos de " + wkniNombreMes), wkniUi.wkniSpacer(),
            wkniUi.wkniLabel(wkniLista.size() + (wkniLista.size() == 1 ? " movimiento" : " movimientos"), "caption"));
        wkniHead.setAlignment(Pos.CENTER_LEFT);
        wkniHead.setPadding(new Insets(18, 22, 10, 22));
        wkniMovimientos.getChildren().add(wkniHead);
        if (wkniLista.isEmpty()) {
            wkniMovimientos.getChildren().add(wkniUi.wkniEmpty("finance", "Sin movimientos este mes",
                "Registra tu primer gasto o ingreso con el formulario de arriba."));
        } else {
            List<Node> wkniFilas = new ArrayList<>();
            for (wkniMovimiento wkniM : wkniLista) { Node wkniF = wkniFila(wkniM); wkniMovimientos.getChildren().add(wkniF); wkniFilas.add(wkniF); }
            wkniAnim.wkniStagger(wkniFilas);
        }

        // ---- gastos por categoría
        wkniCategorias.getChildren().setAll(wkniUi.wkniH3("Gastos por categoría"));
        if (wkniPorCat.isEmpty()) {
            wkniCategorias.getChildren().add(wkniUi.wkniMuted("Aún no hay gastos en " + wkniNombreMes + "."));
            return;
        }
        List<Map.Entry<String, Double>> wkniOrden = wkniPorCat.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed()).toList();
        HBox wkniStacked = new HBox(2);
        wkniStacked.getStyleClass().add("stacked-bar");
        for (var wkniEn : wkniOrden) {
            Region wkniSeg = new Region();
            wkniSeg.getStyleClass().addAll("stacked-seg", "fill-" + wkniColor(wkniEn.getKey()));
            wkniSeg.prefWidthProperty().bind(wkniStacked.widthProperty().multiply(wkniEn.getValue() / wkniGas));
            wkniSeg.setMinWidth(3);
            wkniStacked.getChildren().add(wkniSeg);
        }
        wkniCategorias.getChildren().add(wkniStacked);
        for (var wkniEn : wkniOrden) {
            Region wkniSq = new Region();
            wkniSq.getStyleClass().addAll("legend-dot", "fill-" + wkniColor(wkniEn.getKey()));
            Label wkniN = new Label(wkniEn.getKey());
            wkniN.getStyleClass().add("legend-name");
            HBox.setHgrow(wkniN, Priority.ALWAYS);
            wkniN.setMaxWidth(Double.MAX_VALUE);
            Label wkniPct = wkniUi.wkniLabel(Math.round(100 * wkniEn.getValue() / wkniGas) + " %", "caption");
            Label wkniVal = wkniUi.wkniLabel(wkniUi.wkniMoney(wkniEn.getValue()), "legend-value");
            wkniVal.setMinWidth(96);
            wkniVal.setAlignment(Pos.CENTER_RIGHT);
            HBox wkniR = new HBox(10, wkniSq, wkniN, wkniPct, wkniVal);
            wkniR.setAlignment(Pos.CENTER_LEFT);
            wkniCategorias.getChildren().add(wkniR);
        }
        String wkniTop = wkniOrden.getFirst().getKey();
        HBox wkniTip = new HBox(10, wkniIconFactory.wkniIcon("idea", 16, "tint-teal"),
            wkniUi.wkniLabel(wkniTop + " es tu mayor gasto de " + wkniNombreMes + " (" + Math.round(100 * wkniOrden.getFirst().getValue() / wkniGas) + " %).", "tip-text"));
        wkniTip.getStyleClass().add("tip");
        ((Label) wkniTip.getChildren().get(1)).setWrapText(true);
        wkniCategorias.getChildren().add(wkniTip);
    }

    private static String wkniColor(String wkniC) { return wkniCAT.getOrDefault(wkniC, new String[]{"tag", "slate"})[1]; }
    private static String wkniIcono(String wkniC) { return wkniCAT.getOrDefault(wkniC, new String[]{"tag", "slate"})[0]; }

    private Node wkniFila(wkniMovimiento wkniM) {
        boolean wkniIngreso = "INGRESO".equals(wkniM.wkniGetTipo());
        StackPane wkniChip = wkniUi.wkniIconChip(wkniIcono(wkniM.wkniGetCategoria()), wkniColor(wkniM.wkniGetCategoria()), 40, 18);
        String wkniTitulo = wkniM.wkniGetDescripcion() == null || wkniM.wkniGetDescripcion().isBlank() ? wkniM.wkniGetCategoria() : wkniM.wkniGetDescripcion();
        Label wkniT = wkniUi.wkniLabel(wkniTitulo, "row-title");
        String wkniCuando;
        try { wkniCuando = wkniUi.wkniFriendlyDate(LocalDate.parse(wkniM.wkniGetFecha())); } catch (Exception wkniE) { wkniCuando = wkniM.wkniGetFecha(); }
        Label wkniSub = wkniUi.wkniLabel(wkniM.wkniGetCategoria() + " · " + wkniCuando, "row-meta");
        VBox wkniTexts = new VBox(3, wkniT, wkniSub);
        HBox.setHgrow(wkniTexts, Priority.ALWAYS);
        Label wkniMonto = wkniUi.wkniLabel((wkniIngreso ? "+ " : "− ") + wkniUi.wkniMoney(wkniM.wkniGetMonto()), "amount");
        if (wkniIngreso) wkniMonto.getStyleClass().add("amount-income");
        Button wkniMas = wkniUi.wkniIconButton("more", "Más opciones");
        MenuItem wkniBorrar = new MenuItem("Eliminar movimiento");
        wkniBorrar.getStyleClass().add("danger-item");
        wkniBorrar.setOnAction(wkniE -> {
            if (wkniUi.wkniConfirm(wkniView, "¿Eliminar este movimiento?", wkniTitulo + " · " + wkniUi.wkniMoney(wkniM.wkniGetMonto()), "Eliminar")
                && wkniDao.wkniEliminar(wkniM.wkniGetId())) {
                wkniFill();
                wkniNotifier.wkniDeleted("Movimiento eliminado", wkniTitulo + " · " + wkniUi.wkniMoney(wkniM.wkniGetMonto()));
            }
        });
        ContextMenu wkniCm = new ContextMenu(wkniBorrar);
        wkniMas.setOnAction(wkniE -> wkniCm.show(wkniMas, javafx.geometry.Side.BOTTOM, 0, 4));
        HBox wkniRow = new HBox(14, wkniChip, wkniTexts, wkniMonto, wkniMas);
        wkniRow.setAlignment(Pos.CENTER_LEFT);
        wkniRow.getStyleClass().addAll("list-row", "with-divider");
        return wkniRow;
    }

    private javafx.scene.control.ScrollPane wkniScroll;

    /** La página va dentro de un desplazamiento para ventanas pequeñas o listas largas. */
    public Node wkniGetView() {
        if (wkniScroll == null) wkniScroll = wkniUi.wkniScroll(wkniView);
        return wkniScroll;
    }
}

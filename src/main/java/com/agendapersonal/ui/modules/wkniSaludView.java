package com.agendapersonal.ui.modules;

import com.agendapersonal.domain.model.wkniRegistroSalud;
import com.agendapersonal.infrastructure.repo.wkniSaludDAO;
import com.agendapersonal.ui.wkniAnim;
import com.agendapersonal.ui.wkniIconFactory;
import com.agendapersonal.ui.wkniNotifier;
import com.agendapersonal.util.wkniUi;
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
public class wkniSaludView {
    /** Tipo guardado en la BD → etiqueta, unidad, ícono, color y meta diaria (0 = sin meta). */
    private record wkniTipo(String wkniClave, String wkniNombre, String wkniUnidad, String wkniIcono, String wkni_tono, double wkniMeta, String wkniEjemplo) {}
    private static final List<wkniTipo> wkniTIPOS = List.of(
        new wkniTipo("AGUA", "Agua", "vasos", "water", "blue", 8, "2"),
        new wkniTipo("SUENO", "Sueño", "h", "moon", "violet", 8, "7,5"),
        new wkniTipo("EJERCICIO", "Ejercicio", "min", "activity", "green", 30, "30"),
        new wkniTipo("PESO", "Peso", "kg", "scale", "teal", 0, "68,4"),
        new wkniTipo("ANIMO", "Ánimo", "/10", "mood", "amber", 0, "7"));

    private final wkniSaludDAO wkniDao = new wkniSaludDAO();
    private final VBox wkniView;
    private final TilePane wkniTarjetas = new TilePane(16, 16);
    private final VBox wkniHistorial = new VBox(0);
    private final wkniUi.wkniSegmented wkniRango = new wkniUi.wkniSegmented("Esta semana", "Este mes", "Todo");
    private final ToggleGroup wkniGrupoTipo = new ToggleGroup();
    private final TextField wkniValor = new TextField();
    private final Label wkniUnidad = wkniUi.wkniLabel("", "input-suffix");
    private final DatePicker wkni_fecha = new DatePicker(LocalDate.now());
    private final TextField wkniNotas = new TextField();
    private wkniTipo wkni_tipo = wkniTIPOS.getFirst();
    private ScrollPane wkniScroll;

    public wkniSaludView() {
        VBox wkniPage = wkniUi.wkniPage();
        wkniPage.getChildren().add(wkniUi.wkniPageHeader("health", "Salud",
            "Un registro rápido al día: agua, sueño, ejercicio, peso y ánimo."));
        wkniTarjetas.setPrefColumns(5);
        wkniTarjetas.setPrefTileWidth(200);
        wkniTarjetas.setTileAlignment(Pos.TOP_LEFT);
        wkniRango.wkniSelectedProperty().addListener((wkniO, wkniA, wkniB) -> wkniFill());
        VBox wkniHist = new VBox(0, wkniHistorial);
        wkniHist.getStyleClass().addAll("card", "card-flush");
        wkniPage.getChildren().addAll(wkniTarjetas, wkniFormulario(), wkniHist);
        wkniView = wkniPage;
        wkniFill();
    }

    private Node wkniFormulario() {
        FlowPane wkniChips = new FlowPane(8, 8);
        for (wkniTipo wkniT : wkniTIPOS) {
            ToggleButton wkniB = new ToggleButton(wkniT.wkniNombre(), wkniIconFactory.wkniIcon(wkniT.wkniIcono(), 16));
            wkniB.getStyleClass().addAll("choice-chip", "choice-" + wkniT.wkni_tono());
            wkniB.setToggleGroup(wkniGrupoTipo);
            wkniB.setUserData(wkniT);
            wkniB.setOnAction(wkniE -> {
                if (!wkniB.isSelected()) { wkniB.setSelected(true); return; }
                wkni_tipo = wkniT;
                wkniActualizarUnidad();
                wkniAnim.wkniPop(wkniB);
            });
            wkniChips.getChildren().add(wkniB);
        }
        ((ToggleButton) wkniChips.getChildren().getFirst()).setSelected(true);

        wkniValor.getStyleClass().add("bare-field");
        HBox wkniValorBox = new HBox(8, wkniValor, wkniUnidad);
        wkniValorBox.getStyleClass().add("input-with-icon");
        wkniValorBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(wkniValor, Priority.ALWAYS);
        wkniNotas.setPromptText("Ej. Después del desayuno");
        wkniUi.wkniFechaCorta(wkni_fecha);
        wkniUnidad.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        Button wkniAdd = wkniUi.wkniPrimary("add", "Registrar");
        wkniAdd.setOnAction(wkniE -> wkniRegistrar(wkniValorBox));
        wkniValor.setOnAction(wkniE -> wkniRegistrar(wkniValorBox));
        wkniNotas.setOnAction(wkniE -> wkniRegistrar(wkniValorBox));
        wkniActualizarUnidad();

        VBox wkniFValor = wkniUi.wkniField("Cantidad", wkniValorBox);
        VBox wkniFFecha = wkniUi.wkniField("Fecha", wkni_fecha);
        VBox wkniFNotas = wkniUi.wkniField("Notas (opcional)", wkniNotas);
        wkniFValor.setPrefWidth(170); wkniFFecha.setPrefWidth(180); wkniFNotas.setPrefWidth(320);
        HBox wkniFila = new HBox(12, wkniFValor, wkniFFecha, wkniFNotas, wkniAdd);
        wkniFila.setAlignment(Pos.BOTTOM_LEFT);
        VBox wkniCard = wkniUi.wkniCard();
        wkniCard.setSpacing(14);
        wkniCard.getChildren().addAll(wkniUi.wkniH3("Nuevo registro"), wkniChips, wkniFila);
        return wkniCard;
    }

    private void wkniActualizarUnidad() {
        wkniUnidad.setText(wkni_tipo.wkniUnidad());
        wkniValor.setPromptText("Ej. " + wkni_tipo.wkniEjemplo());
    }

    private void wkniRegistrar(Node wkniValorBox) {
        double wkniV;
        try { wkniV = wkniUi.wkniParseNumber(wkniValor.getText()); }
        catch (Exception wkniX) { wkniAnim.wkniShake(wkniValorBox); wkniNotifier.wkniWarning("Valor inválido", "Escribe un número, por ejemplo " + wkni_tipo.wkniEjemplo() + "."); return; }
        if (wkniV < 0 || ("ANIMO".equals(wkni_tipo.wkniClave()) && wkniV > 10)) {
            wkniAnim.wkniShake(wkniValorBox);
            wkniNotifier.wkniWarning("Valor fuera de rango", "ANIMO".equals(wkni_tipo.wkniClave()) ? "El ánimo va de 0 a 10." : "El valor no puede ser negativo.");
            return;
        }
        if (wkni_fecha.getValue() == null) wkni_fecha.setValue(LocalDate.now());
        if (wkniDao.wkniInsertar(new wkniRegistroSalud(0, wkni_tipo.wkniClave(), wkniV, wkni_tipo.wkniUnidad(), wkni_fecha.getValue().toString(), wkniNotas.getText().trim()))) {
            wkniNotifier.wkniSuccess(wkni_tipo.wkniNombre() + " registrado", wkniNum(wkniV) + " " + wkni_tipo.wkniUnidad() + (wkni_fecha.getValue().equals(LocalDate.now()) ? " hoy" : ""));
            wkniValor.clear();
            wkniNotas.clear();
            wkniFill();
        } else wkniNotifier.wkniError("No se pudo guardar el registro", null);
    }

    private void wkniFill() {
        List<wkniRegistroSalud> wkniTodos = wkniDao.wkniListar(null, 500);
        LocalDate wkniHoy = LocalDate.now();
        String wkniH = wkniHoy.toString();

        // ---- tarjetas de hoy
        wkniTarjetas.getChildren().clear();
        for (wkniTipo wkniT : wkniTIPOS) {
            List<wkniRegistroSalud> wkniDeTipo = wkniTodos.stream().filter(wkniR -> wkniT.wkniClave().equals(wkniR.wkniGetTipo())).toList();
            List<wkniRegistroSalud> wkniDeHoy = wkniDeTipo.stream().filter(wkniR -> wkniH.equals(wkniR.wkniGetFecha())).toList();
            String wkniValorTxt, wkni_nota;
            double wkniProgreso = -1;
            switch (wkniT.wkniClave()) {
                case "AGUA", "EJERCICIO" -> {
                    double wkniSuma = wkniDeHoy.stream().mapToDouble(wkniRegistroSalud::wkniGetValor).sum();
                    wkniValorTxt = wkniNum(wkniSuma) + " " + wkniT.wkniUnidad();
                    wkni_nota = "Meta: " + wkniNum(wkniT.wkniMeta()) + " " + wkniT.wkniUnidad();
                    wkniProgreso = wkniSuma / wkniT.wkniMeta();
                }
                case "SUENO" -> {
                    if (wkniDeHoy.isEmpty()) { wkniValorTxt = "—"; wkni_nota = "Registra cómo dormiste"; }
                    else {
                        double wkniV = wkniDeHoy.getFirst().wkniGetValor();
                        wkniValorTxt = wkniHoras(wkniV);
                        wkni_nota = wkniV >= wkniT.wkniMeta() ? "¡Meta de 8 h cumplida!" : "Meta: 8 h";
                        wkniProgreso = wkniV / wkniT.wkniMeta();
                    }
                }
                case "PESO" -> {
                    if (wkniDeTipo.isEmpty()) { wkniValorTxt = "—"; wkni_nota = "Aún sin registros"; }
                    else {
                        wkniRegistroSalud wkniUlt = wkniDeTipo.getFirst();
                        wkniValorTxt = wkniNum(wkniUlt.wkniGetValor()) + " kg";
                        wkni_nota = wkniCambioPeso(wkniDeTipo, wkniUlt);
                    }
                }
                default -> {
                    if (wkniDeHoy.isEmpty()) { wkniValorTxt = "—"; wkni_nota = "¿Cómo te sientes hoy?"; }
                    else {
                        double wkniV = wkniDeHoy.getFirst().wkniGetValor();
                        wkniValorTxt = wkniV >= 8 ? "Muy bien" : wkniV >= 6 ? "Bien" : wkniV >= 4 ? "Regular" : "Bajo";
                        wkni_nota = wkniNum(wkniV) + " de 10";
                        wkniProgreso = wkniV / 10.0;
                    }
                }
            }
            wkniTarjetas.getChildren().add(wkniTarjeta(wkniT, wkniValorTxt, wkni_nota, wkniProgreso));
        }

        // ---- historial
        LocalDate wkniDesde = switch (wkniRango.wkniSelected()) { case 0 -> wkniHoy.minusDays(6); case 1 -> wkniHoy.withDayOfMonth(1); default -> LocalDate.MIN; };
        List<wkniRegistroSalud> wkniLista = wkniTodos.stream().filter(wkniR -> {
            try { return !LocalDate.parse(wkniR.wkniGetFecha()).isBefore(wkniDesde); } catch (Exception wkniE) { return true; }
        }).toList();
        wkniHistorial.getChildren().clear();
        HBox wkniHead = new HBox(8, wkniUi.wkniH3("Historial"), wkniUi.wkniSpacer(), wkniRango.wkniNode());
        wkniHead.setAlignment(Pos.CENTER_LEFT);
        wkniHead.setPadding(new Insets(16, 22, 10, 22));
        wkniHistorial.getChildren().add(wkniHead);
        if (wkniLista.isEmpty()) {
            wkniHistorial.getChildren().add(wkniUi.wkniEmpty("health", "Sin registros en este periodo", "Usa el formulario de arriba para anotar tu día."));
            return;
        }
        GridPane wkniTh = wkniGridFila();
        wkniTh.add(wkniUi.wkniOverline("Fecha"), 0, 0);
        wkniTh.add(wkniUi.wkniOverline("Tipo"), 1, 0);
        wkniTh.add(wkniUi.wkniOverline("Valor"), 2, 0);
        wkniTh.add(wkniUi.wkniOverline("Notas"), 3, 0);
        wkniTh.setPadding(new Insets(4, 22, 8, 22));
        wkniHistorial.getChildren().add(wkniTh);
        List<Node> wkniFilas = new ArrayList<>();
        for (wkniRegistroSalud wkniR : wkniLista) { Node wkniF = wkniFila(wkniR); wkniHistorial.getChildren().add(wkniF); wkniFilas.add(wkniF); }
        wkniAnim.wkniStagger(wkniFilas);
    }

    private static String wkniCambioPeso(List<wkniRegistroSalud> wkniPesos, wkniRegistroSalud wkniUlt) {
        try {
            LocalDate wkniFUlt = LocalDate.parse(wkniUlt.wkniGetFecha());
            for (wkniRegistroSalud wkniR : wkniPesos) {
                LocalDate wkniF = LocalDate.parse(wkniR.wkniGetFecha());
                if (!wkniF.isAfter(wkniFUlt.minusDays(6)) && wkniR != wkniUlt) {
                    double wkniD = wkniUlt.wkniGetValor() - wkniR.wkniGetValor();
                    if (Math.abs(wkniD) < 0.05) return "Estable esta semana";
                    return (wkniD > 0 ? "+" : "−") + wkniNum(Math.abs(wkniD)) + " kg en una semana";
                }
            }
        } catch (Exception wkniIgnored) {}
        return "Último registro: " + wkniUi.wkniFriendlyDate(LocalDate.parse(wkniUlt.wkniGetFecha())).toLowerCase(Locale.ROOT);
    }

    private VBox wkniTarjeta(wkniTipo wkniT, String wkniValorTxt, String wkni_nota, double wkniProgreso) {
        HBox wkniTop = new HBox(10, wkniUi.wkniIconChip(wkniT.wkniIcono(), wkniT.wkni_tono(), 34, 18), wkniUi.wkniLabel(wkniT.wkniNombre(), "stat-label"));
        wkniTop.setAlignment(Pos.CENTER_LEFT);
        Label wkniV = wkniUi.wkniLabel(wkniValorTxt, "stat-value", "stat-value-sm");
        VBox wkniB = new VBox(10, wkniTop, wkniV);
        if (wkniProgreso >= 0) wkniB.getChildren().add(wkniUi.wkniBar(wkniProgreso, wkniT.wkni_tono(), 6));
        wkniB.getChildren().add(wkniUi.wkniLabel(wkni_nota, "stat-note"));
        wkniB.getStyleClass().addAll("card", "stat-card");
        wkniB.setPadding(new Insets(18, 20, 18, 20));
        wkniB.setPrefWidth(200);
        wkniB.setMinHeight(150);
        return wkniB;
    }

    private GridPane wkniGridFila() {
        GridPane wkniG = new GridPane();
        wkniG.setHgap(16);
        ColumnConstraints wkniC0 = new ColumnConstraints(130);
        ColumnConstraints wkniC1 = new ColumnConstraints(140);
        ColumnConstraints wkniC2 = new ColumnConstraints(120);
        ColumnConstraints wkniC3 = new ColumnConstraints();
        wkniC3.setHgrow(Priority.ALWAYS);
        ColumnConstraints wkniC4 = new ColumnConstraints(40);
        wkniG.getColumnConstraints().addAll(wkniC0, wkniC1, wkniC2, wkniC3, wkniC4);
        wkniG.setAlignment(Pos.CENTER_LEFT);
        return wkniG;
    }

    private Node wkniFila(wkniRegistroSalud wkniR) {
        wkniTipo wkniT = wkniTIPOS.stream().filter(wkniX -> wkniX.wkniClave().equals(wkniR.wkniGetTipo())).findFirst().orElse(wkniTIPOS.getLast());
        String wkniCuando;
        try { wkniCuando = wkniUi.wkniFriendlyDate(LocalDate.parse(wkniR.wkniGetFecha())); } catch (Exception wkniE) { wkniCuando = wkniR.wkniGetFecha(); }
        Label wkniPill = wkniUi.wkniPill(wkniT.wkniNombre(), wkniT.wkni_tono());
        wkniPill.setGraphic(wkniIconFactory.wkniIcon(wkniT.wkniIcono(), 13, "tint-" + wkniT.wkni_tono()));
        wkniPill.setGraphicTextGap(6);
        String wkniUnidadTxt = wkniR.wkniGetUnidad() != null ? wkniR.wkniGetUnidad() : wkniT.wkniUnidad();
        Label wkniVal = wkniUi.wkniLabel("SUENO".equals(wkniT.wkniClave()) ? wkniHoras(wkniR.wkniGetValor()) : wkniNum(wkniR.wkniGetValor()) + " " + wkniUnidadTxt, "row-title");
        Label wkni_nota = wkniUi.wkniLabel(wkniR.wkniGetNotas() == null || wkniR.wkniGetNotas().isBlank() ? "—" : wkniR.wkniGetNotas(), "row-meta");
        Button wkniMas = wkniUi.wkniIconButton("more", "Más opciones");
        MenuItem wkniBorrar = new MenuItem("Eliminar registro");
        wkniBorrar.getStyleClass().add("danger-item");
        wkniBorrar.setOnAction(wkniE -> {
            if (wkniUi.wkniConfirm(wkniView, "¿Eliminar este registro?", wkniT.wkniNombre() + " · " + wkniVal.getText(), "Eliminar") && wkniDao.wkniEliminar(wkniR.wkniGetId())) {
                wkniFill();
                wkniNotifier.wkniDeleted("Registro eliminado", wkniT.wkniNombre() + " · " + wkniVal.getText());
            }
        });
        ContextMenu wkniCm = new ContextMenu(wkniBorrar);
        wkniMas.setOnAction(wkniE -> wkniCm.show(wkniMas, javafx.geometry.Side.BOTTOM, 0, 4));
        GridPane wkniG = wkniGridFila();
        wkniG.add(wkniUi.wkniLabel(wkniCuando, "row-meta-strong"), 0, 0);
        wkniG.add(wkniPill, 1, 0);
        wkniG.add(wkniVal, 2, 0);
        wkniG.add(wkni_nota, 3, 0);
        wkniG.add(wkniMas, 4, 0);
        wkniG.getStyleClass().addAll("list-row", "with-divider");
        wkniG.setPadding(new Insets(10, 22, 10, 22));
        return wkniG;
    }

    private static String wkniNum(double wkniV) {
        if (Math.abs(wkniV - Math.round(wkniV)) < 1e-9) return String.valueOf(Math.round(wkniV));
        return String.format(wkniUi.wkniES, "%.1f", wkniV);
    }

    private static String wkniHoras(double wkniV) {
        int wkniH = (int) wkniV;
        int wkniM = (int) Math.round((wkniV - wkniH) * 60);
        if (wkniM == 60) { wkniH++; wkniM = 0; }
        return wkniM == 0 ? wkniH + " h" : wkniH + " h " + wkniM + " min";
    }

    public Node wkniGetView() {
        if (wkniScroll == null) wkniScroll = wkniUi.wkniScroll(wkniView);
        return wkniScroll;
    }
}

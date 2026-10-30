package com.agendapersonal.ui.modules.docente;

import com.agendapersonal.domain.model.wkniEstadoAsistencia;
import com.agendapersonal.domain.model.wkniEstudianteClase;
import com.agendapersonal.service.wkniReportesDocente;
import com.agendapersonal.service.wkniResumenCurso;
import com.agendapersonal.service.wkniRiesgoEstudianteService;
import com.agendapersonal.util.wkniUi;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.IntConsumer;

/**
 * Panel del curso: cifras clave, gráficas que se rehacen cada vez que cambia la asistencia o una nota
 * (distribución de asistencia, asistencia por clase, al día vs atrasados) y el semáforo de alertas con el
 * motivo exacto de cada estudiante en riesgo.
 */
final class wkniPanelCurso {
    private final wkniCursoContexto wkniCtx;
    private final IntConsumer wkniIrAPestana;
    private final VBox wkniContenido = new VBox(18);
    private final ScrollPane wkniRoot = wkniUi.wkniScroll(wkniContenido);

    /** @param irAPestana abre otra pestaña del curso (1 = asistencia, 2 = trabajos…). */
    wkniPanelCurso(wkniCursoContexto wkniCtx, IntConsumer wkniIrAPestana) {
        this.wkniCtx = wkniCtx;
        this.wkniIrAPestana = wkniIrAPestana;
        wkniContenido.setPadding(new Insets(16, 6, 16, 4));
        wkniRecargar();
    }

    Node wkniGetView() { return wkniRoot; }

    void wkniRecargar() {
        wkniResumenCurso wkniR = wkniCtx.wkniResumen();
        wkniContenido.getChildren().clear();
        int wkniTotal = wkniR.wkniEstudiantes().size();
        if (wkniTotal == 0) {
            wkniContenido.getChildren().add(wkniUi.wkniEmpty("contacts", "Este curso aún no tiene estudiantes",
                "Agrega tu lista en la pestaña «Estudiantes» (o impórtala de Excel) para ver gráficas y alertas."));
            return;
        }
        long wkniRiesgo = wkniR.wkniContar(wkniRiesgoEstudianteService.wkniNivel.ALTO);
        long wkniAtencion = wkniR.wkniContar(wkniRiesgoEstudianteService.wkniNivel.ATENCION);
        double wkniAsist = wkniR.wkniAsistenciaPromedio();
        double wkniProm = wkniR.wkniPromedioNotas();
        double wkniHoy = wkniR.wkniAsistenciaDelDia(LocalDate.now());

        HBox wkniCifras = new HBox(14,
            wkniDocenteUi.wkniMetrica("Estudiantes", Integer.toString(wkniTotal), wkniR.wkniFechas().size() + " clases registradas", "contacts", "violet"),
            wkniDocenteUi.wkniMetrica("Asistencia promedio", wkniReportesDocente.wkniPct(wkniAsist),
                Double.isNaN(wkniHoy) ? "Hoy aún sin registrar" : "Hoy: " + wkniReportesDocente.wkniPct(wkniHoy), "user-check", "green"),
            wkniDocenteUi.wkniMetrica("Promedio de notas", Double.isNaN(wkniProm) ? "—" : wkniRiesgoEstudianteService.wkniNumero(Math.round(wkniProm * 10) / 10.0),
                "Aprueba con " + wkniRiesgoEstudianteService.wkniNumero(wkniCtx.wkniClase().wkniNotaMinima()), "chart", "blue"),
            wkniDocenteUi.wkniMetrica("En riesgo", Long.toString(wkniRiesgo), wkniAtencion + " más requieren atención", "warning", wkniRiesgo > 0 ? "rose" : "green"));

        GridPane wkniGraficas = new GridPane();
        wkniGraficas.setHgap(14); wkniGraficas.setVgap(14);
        javafx.scene.layout.ColumnConstraints wkniCc = new javafx.scene.layout.ColumnConstraints();
        wkniCc.setPercentWidth(33.33);
        wkniGraficas.getColumnConstraints().addAll(wkniCc, wkniCc, wkniCc);
        wkniGraficas.add(wkniTarjeta("Asistencia acumulada", "Todas las clases registradas", wkniTorta(wkniR)), 0, 0);
        wkniGraficas.add(wkniTarjeta("Asistencia por clase", "Últimas 10 sesiones · % que asistió", wkniBarrasSesiones(wkniR)), 1, 0);
        wkniGraficas.add(wkniTarjeta("Entrega de trabajos", "Estudiantes al día frente a los que se quedan atrás", wkniAlDia(wkniR)), 2, 0);

        wkniContenido.getChildren().addAll(wkniCifras, wkniGraficas, wkniSemaforo(wkniR));
    }

    private VBox wkniTarjeta(String wkniTitulo, String wkniSub, Node wkniGrafico) {
        VBox wkniV = wkniUi.wkniCard();
        wkniV.setPadding(new Insets(16, 16, 10, 16));
        wkniV.getChildren().addAll(wkniUi.wkniH3(wkniTitulo), wkniUi.wkniCaption(wkniSub), wkniGrafico);
        VBox.setVgrow(wkniGrafico, Priority.ALWAYS);
        wkniV.setMaxHeight(Double.MAX_VALUE);
        return wkniV;
    }

    private Node wkniTorta(wkniResumenCurso wkniR) {
        Map<wkniEstadoAsistencia, Integer> wkniD = wkniR.wkniDistribucionAsistencia();
        int wkniTotal = wkniD.values().stream().mapToInt(Integer::intValue).sum();
        if (wkniTotal == 0) return wkniSinDatos("Marca la asistencia para ver la distribución.", 1);
        PieChart wkniPie = new PieChart();
        wkniPie.setLegendVisible(false);
        wkniPie.setLabelsVisible(false);
        wkniPie.setStartAngle(90);
        wkniPie.setPrefSize(240, 210);
        FlowPane wkniLeyenda = new FlowPane(10, 6);
        for (wkniEstadoAsistencia wkniE : wkniEstadoAsistencia.values()) {
            int wkniN = wkniD.get(wkniE);
            if (wkniN == 0) continue;
            PieChart.Data wkniDato = new PieChart.Data(wkniE.wkniEtiqueta(), wkniN);
            wkniPie.getData().add(wkniDato);
            String wkniColor = wkniDocenteUi.wkniColor(wkniE);
            String wkniTexto = wkniE.wkniEtiqueta() + " " + Math.round(100.0 * wkniN / wkniTotal) + " %";
            wkniColorear(wkniDato.getNode(), "-fx-pie-color: " + wkniColor + ";", wkniTexto + " (" + wkniN + ")");
            wkniLeyenda.getChildren().add(wkniDocenteUi.wkniConteo(wkniColor, wkniTexto, wkniDocenteUi.wkniTono(wkniE).equals("danger") ? "rose" : wkniDocenteUi.wkniTono(wkniE)));
        }
        return new VBox(6, wkniPie, wkniLeyenda);
    }

    private Node wkniBarrasSesiones(wkniResumenCurso wkniR) {
        List<LocalDate> wkniFechas = wkniR.wkniFechas();
        if (wkniFechas.isEmpty()) return wkniSinDatos("Aún no hay clases con asistencia.", 1);
        List<LocalDate> wkniUltimas = wkniFechas.subList(Math.max(0, wkniFechas.size() - 10), wkniFechas.size());
        CategoryAxis wkniX = new CategoryAxis();
        NumberAxis wkniY = new NumberAxis(0, 100, 25);
        wkniY.setTickLabelFormatter(new NumberAxis.DefaultFormatter(wkniY, null, " %"));
        BarChart<String, Number> wkniChart = new BarChart<>(wkniX, wkniY);
        wkniChart.setLegendVisible(false);
        wkniChart.setAnimated(false);
        wkniChart.setCategoryGap(6);
        wkniChart.setPrefSize(320, 240);
        XYChart.Series<String, Number> wkniS = new XYChart.Series<>();
        DateTimeFormatter wkniF = DateTimeFormatter.ofPattern("dd/MM");
        for (LocalDate wkniD : wkniUltimas) {
            double wkni_p = wkniR.wkniAsistenciaDelDia(wkniD);
            wkniS.getData().add(new XYChart.Data<>(wkniD.format(wkniF), Double.isNaN(wkni_p) ? 0 : wkni_p));
        }
        wkniChart.getData().add(wkniS);
        for (XYChart.Data<String, Number> wkniDato : wkniS.getData()) {
            double wkni_p = wkniDato.getYValue().doubleValue();
            String wkniColor = wkni_p < wkniRiesgoEstudianteService.wkniASISTENCIA_MINIMA ? "-danger"
                : wkni_p < wkniRiesgoEstudianteService.wkniASISTENCIA_ATENCION ? "#E8B43A" : "-c-green";
            wkniColorear(wkniDato.getNode(), "-fx-bar-fill: " + wkniColor + "; -fx-background-radius: 6 6 0 0;",
                wkniDato.getXValue() + ": " + Math.round(wkni_p) + " % asistió");
        }
        return wkniChart;
    }

    private Node wkniAlDia(wkniResumenCurso wkniR) {
        if (wkniR.wkniEvaluables().stream().noneMatch(wkniA -> wkniA.wkni_fecha() != null && !wkniA.wkni_fecha().isAfter(LocalDate.now())))
            return wkniSinDatos("Aún no hay trabajos vencidos. Créalos en «Trabajos y notas».", 2);
        long wkniAl = wkniR.wkniAlDia();
        long wkniAtras = wkniR.wkniEstudiantes().size() - wkniAl;
        PieChart wkniPie = new PieChart(FXCollections.observableArrayList(
            new PieChart.Data("Al día", wkniAl), new PieChart.Data("Atrasados", wkniAtras)));
        wkniPie.setLegendVisible(false);
        wkniPie.setLabelsVisible(false);
        wkniPie.setStartAngle(90);
        wkniPie.setPrefSize(240, 210);
        int wkniTotal = wkniR.wkniEstudiantes().size();
        wkniColorear(wkniPie.getData().get(0).getNode(), "-fx-pie-color: -c-green;", "Al día: " + wkniAl);
        wkniColorear(wkniPie.getData().get(1).getNode(), "-fx-pie-color: -danger;", "Atrasados: " + wkniAtras);
        HBox wkniLeyenda = new HBox(10,
            wkniDocenteUi.wkniConteo("-c-green", Math.round(100.0 * wkniAl / wkniTotal) + " % al día", "green"),
            wkniDocenteUi.wkniConteo("-danger", Math.round(100.0 * wkniAtras / wkniTotal) + " % atrasados", "rose"));
        Button wkniVer = wkniUi.wkniGhost("arrow-right", "Ver trabajos");
        wkniVer.setOnAction(wkniE -> wkniIrAPestana.accept(2));
        return new VBox(6, wkniPie, wkniLeyenda, wkniVer);
    }

    private Node wkniSemaforo(wkniResumenCurso wkniR) {
        VBox wkniCard = wkniUi.wkniCard();
        List<wkniEstudianteClase> wkniAlertas = wkniR.wkniConAlertas();
        Label wkniTitulo = wkniUi.wkniH3("Semáforo de alertas");
        Label wkniSub = wkniUi.wkniCaption("Revisión automática del historial: cruza faltas, tardanzas, entregas y notas. Se actualiza al marcar asistencia o ingresar notas.");
        wkniSub.setWrapText(true);
        HBox wkniLeyenda = new HBox(8,
            wkniDocenteUi.wkniConteo("-danger", wkniR.wkniContar(wkniRiesgoEstudianteService.wkniNivel.ALTO) + " en riesgo", "rose"),
            wkniDocenteUi.wkniConteo("#E8B43A", wkniR.wkniContar(wkniRiesgoEstudianteService.wkniNivel.ATENCION) + " atención", "amber"),
            wkniDocenteUi.wkniConteo("-c-green", wkniR.wkniContar(wkniRiesgoEstudianteService.wkniNivel.BIEN) + " al día", "green"));
        wkniCard.getChildren().addAll(new HBox(12, new VBox(4, wkniTitulo, wkniSub), wkniUi.wkniSpacer(), wkniLeyenda));
        if (wkniAlertas.isEmpty()) {
            wkniCard.getChildren().add(wkniUi.wkniEmpty("success", "Todo en orden", "Ningún estudiante tiene alertas por ahora."));
            return wkniCard;
        }
        for (wkniEstudianteClase wkniE : wkniAlertas)
            wkniCard.getChildren().add(wkniFilaAlerta(wkniE.wkniNombre(), wkniR.wkniEvaluacion(wkniE.wkniId()), null, () -> wkniReporteEstudianteView.wkniMostrar(wkniRoot, wkniCtx, wkniE)));
        return wkniCard;
    }

    /** Fila del semáforo: bandera, nombre, nivel y motivos. También la usa la pestaña global de alertas. */
    static HBox wkniFilaAlerta(String wkniNombre, wkniRiesgoEstudianteService.wkniEvaluacion wkniEv, String wkniCurso, Runnable wkniAbrir) {
        boolean wkniAlto = wkniEv.wkni_nivel() == wkniRiesgoEstudianteService.wkniNivel.ALTO;
        Node wkniFlag = com.agendapersonal.ui.wkniIconFactory.wkniIcon(wkniAlto ? "flag" : "warning", 20, wkniAlto ? "tint-rose" : "tint-amber");
        Label wkniN = wkniUi.wkniLabel(wkniNombre, "h3");
        HBox wkniTop = new HBox(8, wkniN, wkniUi.wkniPill(wkniEv.wkni_nivel().wkniEtiqueta(), wkniAlto ? "danger" : "amber"));
        if (wkniCurso != null) wkniTop.getChildren().add(wkniUi.wkniPill(wkniCurso, "slate"));
        wkniTop.setAlignment(Pos.CENTER_LEFT);
        FlowPane wkniMotivos = new FlowPane(6, 6);
        for (String wkniM : wkniEv.wkniMotivos()) {
            Label wkniL = new Label("• " + wkniM);
            wkniL.getStyleClass().add("alerta-motivo");
            wkniMotivos.getChildren().add(wkniL);
        }
        VBox wkniTexto = new VBox(4, wkniTop, wkniMotivos);
        HBox.setHgrow(wkniTexto, Priority.ALWAYS);
        Label wkni_datos = wkniUi.wkniCaption("Asistencia " + wkniReportesDocente.wkniPct(wkniEv.wkniPorcentajeAsistencia())
            + (wkniEv.wkni_nota() == null ? "" : " · Nota " + wkniRiesgoEstudianteService.wkniNumero(Math.round(wkniEv.wkni_nota() * 10) / 10.0)));
        HBox wkniFila = new HBox(12, wkniFlag, wkniTexto, wkni_datos);
        if (wkniAbrir != null) {
            Button wkniVer = wkniUi.wkniGhost("arrow-right", wkniCurso == null ? "Reporte" : "Ver");
            wkniVer.setOnAction(wkniX -> wkniAbrir.run());
            wkniFila.getChildren().add(wkniVer);
        }
        wkniFila.setAlignment(Pos.CENTER_LEFT);
        wkniFila.getStyleClass().addAll("alerta-fila", wkniAlto ? "alerta-alto" : "alerta-atencion");
        return wkniFila;
    }

    private Node wkniSinDatos(String wkniTexto, int wkniPestana) {
        Label wkniL = wkniUi.wkniMuted(wkniTexto);
        wkniL.setWrapText(true);
        Button wkniIr = wkniUi.wkniGhost("arrow-right", wkniPestana == 1 ? "Ir a asistencia" : "Ir a trabajos");
        wkniIr.setOnAction(wkniE -> wkniIrAPestana.accept(wkniPestana));
        VBox wkniV = new VBox(10, wkniL, wkniIr);
        wkniV.setAlignment(Pos.CENTER);
        wkniV.setMinHeight(200);
        return wkniV;
    }

    /** Aplica color y tooltip al nodo de un dato del gráfico (puede crearse después de agregarlo). */
    private static void wkniColorear(Node wkniNodo, String wkniEstilo, String wkniTip) {
        if (wkniNodo != null) { wkniNodo.setStyle(wkniEstilo); Tooltip.install(wkniNodo, new Tooltip(wkniTip)); }
    }
}

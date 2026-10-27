package com.agendapersonal.ui.modules.docente;

import com.agendapersonal.domain.model.WO_KI_NB_JI_EstadoAsistencia;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstudianteClase;
import com.agendapersonal.service.WO_KI_NB_JI_ReportesDocente;
import com.agendapersonal.service.WO_KI_NB_JI_ResumenCurso;
import com.agendapersonal.service.WO_KI_NB_JI_RiesgoEstudianteService;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
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
final class WO_KI_NB_JI_PanelCurso {
    private final WO_KI_NB_JI_CursoContexto ctx;
    private final IntConsumer irAPestana;
    private final VBox contenido = new VBox(18);
    private final ScrollPane root = WO_KI_NB_JI_Ui.WO_KI_NB_JI_scroll(contenido);

    /** @param irAPestana abre otra pestaña del curso (1 = asistencia, 2 = trabajos…). */
    WO_KI_NB_JI_PanelCurso(WO_KI_NB_JI_CursoContexto ctx, IntConsumer irAPestana) {
        this.ctx = ctx;
        this.irAPestana = irAPestana;
        contenido.setPadding(new Insets(16, 6, 16, 4));
        WO_KI_NB_JI_recargar();
    }

    Node WO_KI_NB_JI_getView() { return root; }

    void WO_KI_NB_JI_recargar() {
        WO_KI_NB_JI_ResumenCurso r = ctx.WO_KI_NB_JI_resumen();
        contenido.getChildren().clear();
        int total = r.WO_KI_NB_JI_estudiantes().size();
        if (total == 0) {
            contenido.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_empty("contacts", "Este curso aún no tiene estudiantes",
                "Agrega tu lista en la pestaña «Estudiantes» (o impórtala de Excel) para ver gráficas y alertas."));
            return;
        }
        long riesgo = r.WO_KI_NB_JI_contar(WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel.ALTO);
        long atencion = r.WO_KI_NB_JI_contar(WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel.ATENCION);
        double asist = r.WO_KI_NB_JI_asistenciaPromedio();
        double prom = r.WO_KI_NB_JI_promedioNotas();
        double hoy = r.WO_KI_NB_JI_asistenciaDelDia(LocalDate.now());

        HBox cifras = new HBox(14,
            WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_metrica("Estudiantes", Integer.toString(total), r.WO_KI_NB_JI_fechas().size() + " clases registradas", "contacts", "violet"),
            WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_metrica("Asistencia promedio", WO_KI_NB_JI_ReportesDocente.WO_KI_NB_JI_pct(asist),
                Double.isNaN(hoy) ? "Hoy aún sin registrar" : "Hoy: " + WO_KI_NB_JI_ReportesDocente.WO_KI_NB_JI_pct(hoy), "user-check", "green"),
            WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_metrica("Promedio de notas", Double.isNaN(prom) ? "—" : WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(Math.round(prom * 10) / 10.0),
                "Aprueba con " + WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(ctx.clase().notaMinima()), "chart", "blue"),
            WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_metrica("En riesgo", Long.toString(riesgo), atencion + " más requieren atención", "warning", riesgo > 0 ? "rose" : "green"));

        GridPane graficas = new GridPane();
        graficas.setHgap(14); graficas.setVgap(14);
        javafx.scene.layout.ColumnConstraints cc = new javafx.scene.layout.ColumnConstraints();
        cc.setPercentWidth(33.33);
        graficas.getColumnConstraints().addAll(cc, cc, cc);
        graficas.add(WO_KI_NB_JI_tarjeta("Asistencia acumulada", "Todas las clases registradas", WO_KI_NB_JI_torta(r)), 0, 0);
        graficas.add(WO_KI_NB_JI_tarjeta("Asistencia por clase", "Últimas 10 sesiones · % que asistió", WO_KI_NB_JI_barrasSesiones(r)), 1, 0);
        graficas.add(WO_KI_NB_JI_tarjeta("Entrega de trabajos", "Estudiantes al día frente a los que se quedan atrás", WO_KI_NB_JI_alDia(r)), 2, 0);

        contenido.getChildren().addAll(cifras, graficas, WO_KI_NB_JI_semaforo(r));
    }

    private VBox WO_KI_NB_JI_tarjeta(String titulo, String sub, Node grafico) {
        VBox v = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        v.setPadding(new Insets(16, 16, 10, 16));
        v.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h3(titulo), WO_KI_NB_JI_Ui.WO_KI_NB_JI_caption(sub), grafico);
        VBox.setVgrow(grafico, Priority.ALWAYS);
        v.setMaxHeight(Double.MAX_VALUE);
        return v;
    }

    private Node WO_KI_NB_JI_torta(WO_KI_NB_JI_ResumenCurso r) {
        Map<WO_KI_NB_JI_EstadoAsistencia, Integer> d = r.WO_KI_NB_JI_distribucionAsistencia();
        int total = d.values().stream().mapToInt(Integer::intValue).sum();
        if (total == 0) return WO_KI_NB_JI_sinDatos("Marca la asistencia para ver la distribución.", 1);
        PieChart pie = new PieChart();
        pie.setLegendVisible(false);
        pie.setLabelsVisible(false);
        pie.setStartAngle(90);
        pie.setPrefSize(240, 210);
        FlowPane leyenda = new FlowPane(10, 6);
        for (WO_KI_NB_JI_EstadoAsistencia e : WO_KI_NB_JI_EstadoAsistencia.values()) {
            int n = d.get(e);
            if (n == 0) continue;
            PieChart.Data dato = new PieChart.Data(e.WO_KI_NB_JI_etiqueta(), n);
            pie.getData().add(dato);
            String color = WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_color(e);
            String texto = e.WO_KI_NB_JI_etiqueta() + " " + Math.round(100.0 * n / total) + " %";
            WO_KI_NB_JI_colorear(dato.getNode(), "-fx-pie-color: " + color + ";", texto + " (" + n + ")");
            leyenda.getChildren().add(WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_conteo(color, texto, WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_tono(e).equals("danger") ? "rose" : WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_tono(e)));
        }
        return new VBox(6, pie, leyenda);
    }

    private Node WO_KI_NB_JI_barrasSesiones(WO_KI_NB_JI_ResumenCurso r) {
        List<LocalDate> fechas = r.WO_KI_NB_JI_fechas();
        if (fechas.isEmpty()) return WO_KI_NB_JI_sinDatos("Aún no hay clases con asistencia.", 1);
        List<LocalDate> ultimas = fechas.subList(Math.max(0, fechas.size() - 10), fechas.size());
        CategoryAxis x = new CategoryAxis();
        NumberAxis y = new NumberAxis(0, 100, 25);
        y.setTickLabelFormatter(new NumberAxis.DefaultFormatter(y, null, " %"));
        BarChart<String, Number> chart = new BarChart<>(x, y);
        chart.setLegendVisible(false);
        chart.setAnimated(false);
        chart.setCategoryGap(6);
        chart.setPrefSize(320, 240);
        XYChart.Series<String, Number> s = new XYChart.Series<>();
        DateTimeFormatter f = DateTimeFormatter.ofPattern("dd/MM");
        for (LocalDate d : ultimas) {
            double p = r.WO_KI_NB_JI_asistenciaDelDia(d);
            s.getData().add(new XYChart.Data<>(d.format(f), Double.isNaN(p) ? 0 : p));
        }
        chart.getData().add(s);
        for (XYChart.Data<String, Number> dato : s.getData()) {
            double p = dato.getYValue().doubleValue();
            String color = p < WO_KI_NB_JI_RiesgoEstudianteService.ASISTENCIA_MINIMA ? "-danger"
                : p < WO_KI_NB_JI_RiesgoEstudianteService.ASISTENCIA_ATENCION ? "#E8B43A" : "-c-green";
            WO_KI_NB_JI_colorear(dato.getNode(), "-fx-bar-fill: " + color + "; -fx-background-radius: 6 6 0 0;",
                dato.getXValue() + ": " + Math.round(p) + " % asistió");
        }
        return chart;
    }

    private Node WO_KI_NB_JI_alDia(WO_KI_NB_JI_ResumenCurso r) {
        if (r.WO_KI_NB_JI_evaluables().stream().noneMatch(a -> a.fecha() != null && !a.fecha().isAfter(LocalDate.now())))
            return WO_KI_NB_JI_sinDatos("Aún no hay trabajos vencidos. Créalos en «Trabajos y notas».", 2);
        long al = r.WO_KI_NB_JI_alDia();
        long atras = r.WO_KI_NB_JI_estudiantes().size() - al;
        PieChart pie = new PieChart(FXCollections.observableArrayList(
            new PieChart.Data("Al día", al), new PieChart.Data("Atrasados", atras)));
        pie.setLegendVisible(false);
        pie.setLabelsVisible(false);
        pie.setStartAngle(90);
        pie.setPrefSize(240, 210);
        int total = r.WO_KI_NB_JI_estudiantes().size();
        WO_KI_NB_JI_colorear(pie.getData().get(0).getNode(), "-fx-pie-color: -c-green;", "Al día: " + al);
        WO_KI_NB_JI_colorear(pie.getData().get(1).getNode(), "-fx-pie-color: -danger;", "Atrasados: " + atras);
        HBox leyenda = new HBox(10,
            WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_conteo("-c-green", Math.round(100.0 * al / total) + " % al día", "green"),
            WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_conteo("-danger", Math.round(100.0 * atras / total) + " % atrasados", "rose"));
        Button ver = WO_KI_NB_JI_Ui.WO_KI_NB_JI_ghost("arrow-right", "Ver trabajos");
        ver.setOnAction(e -> irAPestana.accept(2));
        return new VBox(6, pie, leyenda, ver);
    }

    private Node WO_KI_NB_JI_semaforo(WO_KI_NB_JI_ResumenCurso r) {
        VBox card = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        List<WO_KI_NB_JI_EstudianteClase> alertas = r.WO_KI_NB_JI_conAlertas();
        Label titulo = WO_KI_NB_JI_Ui.WO_KI_NB_JI_h3("Semáforo de alertas");
        Label sub = WO_KI_NB_JI_Ui.WO_KI_NB_JI_caption("Revisión automática del historial: cruza faltas, tardanzas, entregas y notas. Se actualiza al marcar asistencia o ingresar notas.");
        sub.setWrapText(true);
        HBox leyenda = new HBox(8,
            WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_conteo("-danger", r.WO_KI_NB_JI_contar(WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel.ALTO) + " en riesgo", "rose"),
            WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_conteo("#E8B43A", r.WO_KI_NB_JI_contar(WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel.ATENCION) + " atención", "amber"),
            WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_conteo("-c-green", r.WO_KI_NB_JI_contar(WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel.BIEN) + " al día", "green"));
        card.getChildren().addAll(new HBox(12, new VBox(4, titulo, sub), WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), leyenda));
        if (alertas.isEmpty()) {
            card.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_empty("success", "Todo en orden", "Ningún estudiante tiene alertas por ahora."));
            return card;
        }
        for (WO_KI_NB_JI_EstudianteClase e : alertas)
            card.getChildren().add(WO_KI_NB_JI_filaAlerta(e.nombre(), r.WO_KI_NB_JI_evaluacion(e.id()), null, () -> WO_KI_NB_JI_ReporteEstudianteView.WO_KI_NB_JI_mostrar(root, ctx, e)));
        return card;
    }

    /** Fila del semáforo: bandera, nombre, nivel y motivos. También la usa la pestaña global de alertas. */
    static HBox WO_KI_NB_JI_filaAlerta(String nombre, WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Evaluacion ev, String curso, Runnable abrir) {
        boolean alto = ev.nivel() == WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel.ALTO;
        Node flag = com.agendapersonal.ui.WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon(alto ? "flag" : "warning", 20, alto ? "tint-rose" : "tint-amber");
        Label n = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(nombre, "h3");
        HBox top = new HBox(8, n, WO_KI_NB_JI_Ui.WO_KI_NB_JI_pill(ev.nivel().WO_KI_NB_JI_etiqueta(), alto ? "danger" : "amber"));
        if (curso != null) top.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pill(curso, "slate"));
        top.setAlignment(Pos.CENTER_LEFT);
        FlowPane motivos = new FlowPane(6, 6);
        for (String m : ev.motivos()) {
            Label l = new Label("• " + m);
            l.getStyleClass().add("alerta-motivo");
            motivos.getChildren().add(l);
        }
        VBox texto = new VBox(4, top, motivos);
        HBox.setHgrow(texto, Priority.ALWAYS);
        Label datos = WO_KI_NB_JI_Ui.WO_KI_NB_JI_caption("Asistencia " + WO_KI_NB_JI_ReportesDocente.WO_KI_NB_JI_pct(ev.porcentajeAsistencia())
            + (ev.nota() == null ? "" : " · Nota " + WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(Math.round(ev.nota() * 10) / 10.0)));
        HBox fila = new HBox(12, flag, texto, datos);
        if (abrir != null) {
            Button ver = WO_KI_NB_JI_Ui.WO_KI_NB_JI_ghost("arrow-right", curso == null ? "Reporte" : "Ver");
            ver.setOnAction(x -> abrir.run());
            fila.getChildren().add(ver);
        }
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().addAll("alerta-fila", alto ? "alerta-alto" : "alerta-atencion");
        return fila;
    }

    private Node WO_KI_NB_JI_sinDatos(String texto, int pestana) {
        Label l = WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted(texto);
        l.setWrapText(true);
        Button ir = WO_KI_NB_JI_Ui.WO_KI_NB_JI_ghost("arrow-right", pestana == 1 ? "Ir a asistencia" : "Ir a trabajos");
        ir.setOnAction(e -> irAPestana.accept(pestana));
        VBox v = new VBox(10, l, ir);
        v.setAlignment(Pos.CENTER);
        v.setMinHeight(200);
        return v;
    }

    /** Aplica color y tooltip al nodo de un dato del gráfico (puede crearse después de agregarlo). */
    private static void WO_KI_NB_JI_colorear(Node nodo, String estilo, String tip) {
        if (nodo != null) { nodo.setStyle(estilo); Tooltip.install(nodo, new Tooltip(tip)); }
    }
}

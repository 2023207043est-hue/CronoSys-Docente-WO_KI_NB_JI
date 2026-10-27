package com.agendapersonal.ui.modules.docente;

import com.agendapersonal.domain.model.WO_KI_NB_JI_ActividadClase;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Entrega;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstadoEntrega;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstudianteClase;
import com.agendapersonal.service.WO_KI_NB_JI_ReportesDocente;
import com.agendapersonal.service.WO_KI_NB_JI_ResumenCurso;
import com.agendapersonal.service.WO_KI_NB_JI_RiesgoEstudianteService;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Libro de trabajos: una columna por tarea/actividad/evaluación. En cada celda un clic cambia la entrega
 * (✓ entregado → ⏱ tarde → ✗ no entregado → sin registro) y al lado se escribe la nota (Enter o salir del
 * campo la guarda). La columna "Situación" muestra quién está al día y quién se está quedando atrás.
 */
final class WO_KI_NB_JI_TrabajosPanel {
    private static final DateTimeFormatter CORTA = DateTimeFormatter.ofPattern("dd/MM");

    private final WO_KI_NB_JI_CursoContexto ctx;
    private final Runnable nuevaActividad;
    private final VBox root = WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_panel();
    private final TableView<WO_KI_NB_JI_EstudianteClase> tabla = new TableView<>();
    private final Label resumenTxt = WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("");
    private WO_KI_NB_JI_ResumenCurso resumen;

    WO_KI_NB_JI_TrabajosPanel(WO_KI_NB_JI_CursoContexto ctx, Runnable nuevaActividad) {
        this.ctx = ctx;
        this.nuevaActividad = nuevaActividad;
        Button nuevo = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("add", "Nueva tarea o evaluación");
        nuevo.setOnAction(e -> this.nuevaActividad.run());
        Button excel = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("download", "Exportar a Excel");
        excel.setOnAction(e -> WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_exportarExcel(root, "Trabajos " + ctx.clase(),
            List.of(WO_KI_NB_JI_ReportesDocente.WO_KI_NB_JI_trabajos(resumen), WO_KI_NB_JI_ReportesDocente.WO_KI_NB_JI_riesgo(resumen))));
        Button imprimir = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("print", "Imprimir");
        imprimir.setOnAction(e -> WO_KI_NB_JI_ImpresionTabla.WO_KI_NB_JI_imprimir(root, WO_KI_NB_JI_ReportesDocente.WO_KI_NB_JI_trabajos(resumen)));
        HBox barra = new HBox(10, nuevo, resumenTxt, WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), excel, imprimir);
        barra.setAlignment(Pos.CENTER_LEFT);
        Label ayuda = new Label("Clic en el círculo para marcar la entrega (✓ entregado · ⏱ tarde · ✗ no entregado). "
            + "Escribe la nota al lado y pulsa Enter. Se guarda al instante.");
        ayuda.getStyleClass().add("atajos");
        ayuda.setWrapText(true);

        tabla.getStyleClass().add("asist-tabla");
        tabla.setFixedCellSize(48);
        tabla.setPlaceholder(WO_KI_NB_JI_Ui.WO_KI_NB_JI_empty("tasks", "Sin trabajos todavía",
            "Crea una tarea, actividad o evaluación para registrar entregas y notas de cada estudiante."));
        VBox.setVgrow(tabla, Priority.ALWAYS);
        root.getChildren().addAll(barra, ayuda, tabla);
        WO_KI_NB_JI_recargar();
    }

    Node WO_KI_NB_JI_getView() { return root; }

    void WO_KI_NB_JI_recargar() {
        resumen = ctx.WO_KI_NB_JI_resumen();
        tabla.getColumns().clear();
        if (resumen.WO_KI_NB_JI_evaluables().isEmpty()) { tabla.getItems().clear(); resumenTxt.setText(""); return; }

        TableColumn<WO_KI_NB_JI_EstudianteClase, WO_KI_NB_JI_EstudianteClase> nombre = new TableColumn<>("Estudiante");
        nombre.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        nombre.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(WO_KI_NB_JI_EstudianteClase e, boolean empty) {
                super.updateItem(e, empty);
                setText(null);
                setGraphic(empty || e == null ? null : WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_nombre(e.nombre(), resumen.WO_KI_NB_JI_evaluacion(e.id())));
            }
        });
        nombre.setPrefWidth(270); nombre.setSortable(false);
        tabla.getColumns().add(nombre);

        for (WO_KI_NB_JI_ActividadClase a : resumen.WO_KI_NB_JI_evaluables()) tabla.getColumns().add(WO_KI_NB_JI_columna(a));

        TableColumn<WO_KI_NB_JI_EstudianteClase, WO_KI_NB_JI_EstudianteClase> prom = new TableColumn<>("Promedio");
        prom.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        prom.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(WO_KI_NB_JI_EstudianteClase e, boolean empty) {
                super.updateItem(e, empty);
                setGraphic(null);
                if (empty || e == null) { setText(null); return; }
                double p = resumen.WO_KI_NB_JI_promedioTrabajos(e.id());
                setText(Double.isNaN(p) ? "—" : WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(Math.round(p * 10) / 10.0));
                getStyleClass().remove("nota-baja");
                if (!Double.isNaN(p) && p < ctx.clase().notaMinima()) getStyleClass().add("nota-baja");
            }
        });
        prom.setPrefWidth(90); prom.setSortable(false);
        prom.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
        tabla.getColumns().add(prom);

        TableColumn<WO_KI_NB_JI_EstudianteClase, WO_KI_NB_JI_EstudianteClase> sit = new TableColumn<>("Situación");
        sit.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        sit.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(WO_KI_NB_JI_EstudianteClase e, boolean empty) {
                super.updateItem(e, empty);
                setText(null);
                if (empty || e == null) { setGraphic(null); return; }
                WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Evaluacion ev = resumen.WO_KI_NB_JI_evaluacion(e.id());
                setGraphic(ev.alDia() ? WO_KI_NB_JI_Ui.WO_KI_NB_JI_pill("Al día", "green")
                    : WO_KI_NB_JI_Ui.WO_KI_NB_JI_pill(ev.noEntregados() + (ev.noEntregados() == 1 ? " pendiente" : " pendientes"), "danger"));
            }
        });
        sit.setPrefWidth(130); sit.setSortable(false);
        tabla.getColumns().add(sit);

        tabla.getItems().setAll(resumen.WO_KI_NB_JI_estudiantes());
        long alDia = resumen.WO_KI_NB_JI_alDia();
        int total = resumen.WO_KI_NB_JI_estudiantes().size();
        resumenTxt.setText(total == 0 ? "" : alDia + " de " + total + " al día · " + (total - alDia) + " con trabajos pendientes");
    }

    private TableColumn<WO_KI_NB_JI_EstudianteClase, WO_KI_NB_JI_EstudianteClase> WO_KI_NB_JI_columna(WO_KI_NB_JI_ActividadClase a) {
        boolean vencida = a.fecha() != null && !a.fecha().isAfter(LocalDate.now());
        String titulo = a.titulo().length() > 22 ? a.titulo().substring(0, 21) + "…" : a.titulo();
        TableColumn<WO_KI_NB_JI_EstudianteClase, WO_KI_NB_JI_EstudianteClase> col = new TableColumn<>(titulo + "\n"
            + a.tipo() + (a.fecha() == null ? "" : " · " + a.fecha().format(CORTA)) + (vencida ? "" : " (próx.)"));
        col.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        col.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(WO_KI_NB_JI_EstudianteClase e, boolean empty) {
                super.updateItem(e, empty);
                setText(null);
                if (empty || e == null) { setGraphic(null); return; }
                setGraphic(WO_KI_NB_JI_celda(a, e));
                setAlignment(Pos.CENTER);
            }
        });
        col.setPrefWidth(150); col.setMinWidth(130); col.setSortable(false);
        return col;
    }

    private Node WO_KI_NB_JI_celda(WO_KI_NB_JI_ActividadClase a, WO_KI_NB_JI_EstudianteClase e) {
        WO_KI_NB_JI_Entrega en = resumen.WO_KI_NB_JI_entrega(a.id(), e.id());
        WO_KI_NB_JI_EstadoEntrega st = en == null ? null : en.estado();
        Button chip = new Button(st == null ? "–" : st.WO_KI_NB_JI_simbolo());
        chip.getStyleClass().addAll("entrega-chip", "pill-" + WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_tono(st));
        chip.setFocusTraversable(false);
        chip.setTooltip(new Tooltip((st == null ? "Sin registrar" : st.WO_KI_NB_JI_valor()) + " — clic para cambiar"));
        chip.setOnAction(x -> {
            WO_KI_NB_JI_EstadoEntrega sig = WO_KI_NB_JI_EstadoEntrega.WO_KI_NB_JI_siguiente(st);
            Double nota = sig == WO_KI_NB_JI_EstadoEntrega.NO_ENTREGADO || sig == null ? null : en == null ? null : en.nota();
            WO_KI_NB_JI_guardar(a, e, sig, nota);
        });
        TextField nota = new TextField(en == null || en.nota() == null ? "" : WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(en.nota()));
        nota.setPromptText("nota");
        nota.getStyleClass().add("nota-campo");
        if (en != null && en.nota() != null && en.nota() < ctx.clase().notaMinima()) nota.getStyleClass().add("nota-baja");
        Runnable commit = () -> {
            String previo = en == null || en.nota() == null ? "" : WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(en.nota());
            if (nota.getText().trim().equals(previo)) return;
            try {
                Double v = WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_nota(nota.getText());
                WO_KI_NB_JI_EstadoEntrega estado = v == null ? st : (st == null || st == WO_KI_NB_JI_EstadoEntrega.NO_ENTREGADO ? WO_KI_NB_JI_EstadoEntrega.ENTREGADO : st);
                WO_KI_NB_JI_guardar(a, e, estado, v);
            } catch (NumberFormatException ex) {
                nota.setText(previo);
                WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_info(root, "La nota debe ser un número (por ejemplo 15 o 15.5).");
            }
        };
        nota.setOnAction(x -> commit.run());
        nota.focusedProperty().addListener((o, antes, ahora) -> { if (!ahora) commit.run(); });
        nota.setOnKeyPressed(k -> { if (k.getCode() == KeyCode.ESCAPE) tabla.requestFocus(); });
        HBox h = new HBox(6, chip, nota);
        h.setAlignment(Pos.CENTER);
        return h;
    }

    private void WO_KI_NB_JI_guardar(WO_KI_NB_JI_ActividadClase a, WO_KI_NB_JI_EstudianteClase e, WO_KI_NB_JI_EstadoEntrega st, Double nota) {
        if (!ctx.dao().WO_KI_NB_JI_guardarEntrega(a.id(), e.id(), st, nota)) { WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_error(root, "No se pudo guardar la entrega."); return; }
        javafx.application.Platform.runLater(() -> { WO_KI_NB_JI_recargarDatos(); ctx.WO_KI_NB_JI_cambio(); });
    }

    /** Recarga datos sin reconstruir columnas (mantiene el desplazamiento horizontal). */
    private void WO_KI_NB_JI_recargarDatos() {
        resumen = ctx.WO_KI_NB_JI_resumen();
        tabla.refresh();
        long alDia = resumen.WO_KI_NB_JI_alDia();
        int total = resumen.WO_KI_NB_JI_estudiantes().size();
        resumenTxt.setText(total == 0 ? "" : alDia + " de " + total + " al día · " + (total - alDia) + " con trabajos pendientes");
    }
}

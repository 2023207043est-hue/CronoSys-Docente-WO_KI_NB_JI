package com.agendapersonal.ui.modules.docente;

import com.agendapersonal.domain.model.wkniActividadClase;
import com.agendapersonal.domain.model.wkniEntrega;
import com.agendapersonal.domain.model.wkniEstadoEntrega;
import com.agendapersonal.domain.model.wkniEstudianteClase;
import com.agendapersonal.service.wkniReportesDocente;
import com.agendapersonal.service.wkniResumenCurso;
import com.agendapersonal.service.wkniRiesgoEstudianteService;
import com.agendapersonal.util.wkniUi;
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
final class wkniTrabajosPanel {
    private static final DateTimeFormatter wkniCORTA = DateTimeFormatter.ofPattern("dd/MM");

    private final wkniCursoContexto wkniCtx;
    private final Runnable wkniNuevaActividad;
    private final VBox wkniRoot = wkniDocenteUi.wkniPanel();
    private final TableView<wkniEstudianteClase> wkniTabla = new TableView<>();
    private final Label wkniResumenTxt = wkniUi.wkniMuted("");
    private wkniResumenCurso wkni_resumen;

    wkniTrabajosPanel(wkniCursoContexto wkniCtx, Runnable wkniNuevaActividad) {
        this.wkniCtx = wkniCtx;
        this.wkniNuevaActividad = wkniNuevaActividad;
        Button wkniNuevo = wkniUi.wkniPrimary("add", "Nueva tarea o evaluación");
        wkniNuevo.setOnAction(wkniE -> this.wkniNuevaActividad.run());
        Button wkniExcel = wkniUi.wkniSecondary("download", "Exportar a Excel");
        wkniExcel.setOnAction(wkniE -> wkniDocenteUi.wkniExportarExcel(wkniRoot, "Trabajos " + wkniCtx.wkniClase(),
            List.of(wkniReportesDocente.wkniTrabajos(wkni_resumen), wkniReportesDocente.wkniRiesgo(wkni_resumen))));
        Button wkniImprimir = wkniUi.wkniSecondary("print", "Imprimir");
        wkniImprimir.setOnAction(wkniE -> wkniImpresionTabla.wkniImprimir(wkniRoot, wkniReportesDocente.wkniTrabajos(wkni_resumen)));
        HBox wkniBarra = new HBox(10, wkniNuevo, wkniResumenTxt, wkniUi.wkniSpacer(), wkniExcel, wkniImprimir);
        wkniBarra.setAlignment(Pos.CENTER_LEFT);
        Label wkniAyuda = new Label("Clic en el círculo para marcar la entrega (✓ entregado · ⏱ tarde · ✗ no entregado). "
            + "Escribe la nota al lado y pulsa Enter. Se guarda al instante.");
        wkniAyuda.getStyleClass().add("atajos");
        wkniAyuda.setWrapText(true);

        wkniTabla.getStyleClass().add("asist-tabla");
        wkniTabla.setFixedCellSize(48);
        wkniTabla.setPlaceholder(wkniUi.wkniEmpty("tasks", "Sin trabajos todavía",
            "Crea una tarea, actividad o evaluación para registrar entregas y notas de cada estudiante."));
        VBox.setVgrow(wkniTabla, Priority.ALWAYS);
        wkniRoot.getChildren().addAll(wkniBarra, wkniAyuda, wkniTabla);
        wkniRecargar();
    }

    Node wkniGetView() { return wkniRoot; }

    void wkniRecargar() {
        wkni_resumen = wkniCtx.wkniResumen();
        wkniTabla.getColumns().clear();
        if (wkni_resumen.wkniEvaluables().isEmpty()) { wkniTabla.getItems().clear(); wkniResumenTxt.setText(""); return; }

        TableColumn<wkniEstudianteClase, wkniEstudianteClase> wkniNombre = new TableColumn<>("Estudiante");
        wkniNombre.setCellValueFactory(wkniD -> new ReadOnlyObjectWrapper<>(wkniD.getValue()));
        wkniNombre.setCellFactory(wkniC -> new TableCell<>() {
            @Override protected void updateItem(wkniEstudianteClase wkniE, boolean wkniEmpty) {
                super.updateItem(wkniE, wkniEmpty);
                setText(null);
                setGraphic(wkniEmpty || wkniE == null ? null : wkniDocenteUi.wkniNombre(wkniE.wkniNombre(), wkni_resumen.wkniEvaluacion(wkniE.wkniId())));
            }
        });
        wkniNombre.setPrefWidth(270); wkniNombre.setSortable(false);
        wkniTabla.getColumns().add(wkniNombre);

        for (wkniActividadClase wkniA : wkni_resumen.wkniEvaluables()) wkniTabla.getColumns().add(wkniColumna(wkniA));

        TableColumn<wkniEstudianteClase, wkniEstudianteClase> wkniProm = new TableColumn<>("Promedio");
        wkniProm.setCellValueFactory(wkniD -> new ReadOnlyObjectWrapper<>(wkniD.getValue()));
        wkniProm.setCellFactory(wkniC -> new TableCell<>() {
            @Override protected void updateItem(wkniEstudianteClase wkniE, boolean wkniEmpty) {
                super.updateItem(wkniE, wkniEmpty);
                setGraphic(null);
                if (wkniEmpty || wkniE == null) { setText(null); return; }
                double wkni_p = wkni_resumen.wkniPromedioTrabajos(wkniE.wkniId());
                setText(Double.isNaN(wkni_p) ? "—" : wkniRiesgoEstudianteService.wkniNumero(Math.round(wkni_p * 10) / 10.0));
                getStyleClass().remove("nota-baja");
                if (!Double.isNaN(wkni_p) && wkni_p < wkniCtx.wkniClase().wkniNotaMinima()) getStyleClass().add("nota-baja");
            }
        });
        wkniProm.setPrefWidth(90); wkniProm.setSortable(false);
        wkniProm.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
        wkniTabla.getColumns().add(wkniProm);

        TableColumn<wkniEstudianteClase, wkniEstudianteClase> wkniSit = new TableColumn<>("Situación");
        wkniSit.setCellValueFactory(wkniD -> new ReadOnlyObjectWrapper<>(wkniD.getValue()));
        wkniSit.setCellFactory(wkniC -> new TableCell<>() {
            @Override protected void updateItem(wkniEstudianteClase wkniE, boolean wkniEmpty) {
                super.updateItem(wkniE, wkniEmpty);
                setText(null);
                if (wkniEmpty || wkniE == null) { setGraphic(null); return; }
                wkniRiesgoEstudianteService.wkniEvaluacion wkniEv = wkni_resumen.wkniEvaluacion(wkniE.wkniId());
                setGraphic(wkniEv.wkniAlDia() ? wkniUi.wkniPill("Al día", "green")
                    : wkniUi.wkniPill(wkniEv.wkniNoEntregados() + (wkniEv.wkniNoEntregados() == 1 ? " pendiente" : " pendientes"), "danger"));
            }
        });
        wkniSit.setPrefWidth(130); wkniSit.setSortable(false);
        wkniTabla.getColumns().add(wkniSit);

        wkniTabla.getItems().setAll(wkni_resumen.wkniEstudiantes());
        long wkniAlDia = wkni_resumen.wkniAlDia();
        int wkniTotal = wkni_resumen.wkniEstudiantes().size();
        wkniResumenTxt.setText(wkniTotal == 0 ? "" : wkniAlDia + " de " + wkniTotal + " al día · " + (wkniTotal - wkniAlDia) + " con trabajos pendientes");
    }

    private TableColumn<wkniEstudianteClase, wkniEstudianteClase> wkniColumna(wkniActividadClase wkniA) {
        boolean wkniVencida = wkniA.wkni_fecha() != null && !wkniA.wkni_fecha().isAfter(LocalDate.now());
        String wkniTitulo = wkniA.wkniTitulo().length() > 22 ? wkniA.wkniTitulo().substring(0, 21) + "…" : wkniA.wkniTitulo();
        TableColumn<wkniEstudianteClase, wkniEstudianteClase> wkniCol = new TableColumn<>(wkniTitulo + "\n"
            + wkniA.wkni_tipo() + (wkniA.wkni_fecha() == null ? "" : " · " + wkniA.wkni_fecha().format(wkniCORTA)) + (wkniVencida ? "" : " (próx.)"));
        wkniCol.setCellValueFactory(wkniD -> new ReadOnlyObjectWrapper<>(wkniD.getValue()));
        wkniCol.setCellFactory(wkniC -> new TableCell<>() {
            @Override protected void updateItem(wkniEstudianteClase wkniE, boolean wkniEmpty) {
                super.updateItem(wkniE, wkniEmpty);
                setText(null);
                if (wkniEmpty || wkniE == null) { setGraphic(null); return; }
                setGraphic(wkniCelda(wkniA, wkniE));
                setAlignment(Pos.CENTER);
            }
        });
        wkniCol.setPrefWidth(150); wkniCol.setMinWidth(130); wkniCol.setSortable(false);
        return wkniCol;
    }

    private Node wkniCelda(wkniActividadClase wkniA, wkniEstudianteClase wkniE) {
        wkniEntrega wkniEn = wkni_resumen.wkniEntrega(wkniA.wkniId(), wkniE.wkniId());
        wkniEstadoEntrega wkniSt = wkniEn == null ? null : wkniEn.wkniEstado();
        Button wkniChip = new Button(wkniSt == null ? "–" : wkniSt.wkniSimbolo());
        wkniChip.getStyleClass().addAll("entrega-chip", "pill-" + wkniDocenteUi.wkniTono(wkniSt));
        wkniChip.setFocusTraversable(false);
        wkniChip.setTooltip(new Tooltip((wkniSt == null ? "Sin registrar" : wkniSt.wkniValor()) + " — clic para cambiar"));
        wkniChip.setOnAction(wkniX -> {
            wkniEstadoEntrega wkniSig = wkniEstadoEntrega.wkniSiguiente(wkniSt);
            Double wkni_nota = wkniSig == wkniEstadoEntrega.NO_ENTREGADO || wkniSig == null ? null : wkniEn == null ? null : wkniEn.wkni_nota();
            wkniGuardar(wkniA, wkniE, wkniSig, wkni_nota);
        });
        TextField wkni_nota = new TextField(wkniEn == null || wkniEn.wkni_nota() == null ? "" : wkniRiesgoEstudianteService.wkniNumero(wkniEn.wkni_nota()));
        wkni_nota.setPromptText("nota");
        wkni_nota.getStyleClass().add("nota-campo");
        if (wkniEn != null && wkniEn.wkni_nota() != null && wkniEn.wkni_nota() < wkniCtx.wkniClase().wkniNotaMinima()) wkni_nota.getStyleClass().add("nota-baja");
        Runnable wkniCommit = () -> {
            String wkniPrevio = wkniEn == null || wkniEn.wkni_nota() == null ? "" : wkniRiesgoEstudianteService.wkniNumero(wkniEn.wkni_nota());
            if (wkni_nota.getText().trim().equals(wkniPrevio)) return;
            try {
                Double wkniV = wkniDocenteUi.wkniNota(wkni_nota.getText());
                wkniEstadoEntrega wkniEstado = wkniV == null ? wkniSt : (wkniSt == null || wkniSt == wkniEstadoEntrega.NO_ENTREGADO ? wkniEstadoEntrega.ENTREGADO : wkniSt);
                wkniGuardar(wkniA, wkniE, wkniEstado, wkniV);
            } catch (NumberFormatException wkniEx) {
                wkni_nota.setText(wkniPrevio);
                wkniDocenteUi.wkniInfo(wkniRoot, "La nota debe ser un número (por ejemplo 15 o 15.5).");
            }
        };
        wkni_nota.setOnAction(wkniX -> wkniCommit.run());
        wkni_nota.focusedProperty().addListener((wkniO, wkniAntes, wkniAhora) -> { if (!wkniAhora) wkniCommit.run(); });
        wkni_nota.setOnKeyPressed(wkniK -> { if (wkniK.getCode() == KeyCode.ESCAPE) wkniTabla.requestFocus(); });
        HBox wkniH = new HBox(6, wkniChip, wkni_nota);
        wkniH.setAlignment(Pos.CENTER);
        return wkniH;
    }

    private void wkniGuardar(wkniActividadClase wkniA, wkniEstudianteClase wkniE, wkniEstadoEntrega wkniSt, Double wkni_nota) {
        if (!wkniCtx.wkniDao().wkniGuardarEntrega(wkniA.wkniId(), wkniE.wkniId(), wkniSt, wkni_nota)) { wkniDocenteUi.wkniError(wkniRoot, "No se pudo guardar la entrega."); return; }
        javafx.application.Platform.runLater(() -> { wkniRecargarDatos(); wkniCtx.wkniCambio(); });
    }

    /** Recarga datos sin reconstruir columnas (mantiene el desplazamiento horizontal). */
    private void wkniRecargarDatos() {
        wkni_resumen = wkniCtx.wkniResumen();
        wkniTabla.refresh();
        long wkniAlDia = wkni_resumen.wkniAlDia();
        int wkniTotal = wkni_resumen.wkniEstudiantes().size();
        wkniResumenTxt.setText(wkniTotal == 0 ? "" : wkniAlDia + " de " + wkniTotal + " al día · " + (wkniTotal - wkniAlDia) + " con trabajos pendientes");
    }
}

package com.agendapersonal.ui.modules.docente;

import com.agendapersonal.domain.model.wkniEstadoAsistencia;
import com.agendapersonal.domain.model.wkniEstudianteClase;
import com.agendapersonal.service.wkniReportesDocente;
import com.agendapersonal.service.wkniResumenCurso;
import com.agendapersonal.service.wkniRiesgoEstudianteService;
import com.agendapersonal.service.wkniTablaReporte;
import com.agendapersonal.util.wkniUi;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Registro de asistencia visual.
 * <ul>
 *   <li><b>Registro del día</b>: una fila por estudiante con casillas Presente / Falta / Tardanza / Justificada.
 *       La fila se pinta del color del estado (verde, rojo, amarillo, azul) y se guarda en el acto en la BD
 *       local (sin internet). Atajos de teclado P / F / T / J / Supr con avance automático a la fila siguiente.</li>
 *   <li><b>Vista mensual</b>: matriz estudiantes × fechas; cada celda es clicable y cicla el estado.</li>
 * </ul>
 * Ambas vistas se exportan a Excel con colores y se imprimen tal como se ven.
 */
final class wkniAsistenciaPanel {
    private static final DateTimeFormatter wkniCORTA = DateTimeFormatter.ofPattern("dd/MM");

    private final wkniCursoContexto wkniCtx;
    private final VBox wkniRoot = wkniDocenteUi.wkniPanel();
    private final DatePicker wkniDia = wkniUi.wkniFechaCorta(new DatePicker(LocalDate.now()));
    private final TableView<wkniEstudianteClase> wkniTablaDia = new TableView<>();
    private final TableView<wkniEstudianteClase> wkniTablaMes = new TableView<>();
    private final StackPane wkniContenido = new StackPane();
    private final FlowPane wkniConteos = new FlowPane(8, 8);
    private final StackPane wkniBarraDia = new StackPane();
    private final Label wkniGuardado = new Label();
    private final Label wkniTituloFecha = wkniUi.wkniH3("");
    private final wkniUi.wkniSegmented wkni_vista = new wkniUi.wkniSegmented("Registro del día", "Vista mensual");
    private final HBox wkniAccionesDia;
    private wkniResumenCurso wkni_resumen;

    wkniAsistenciaPanel(wkniCursoContexto wkniCtx) {
        this.wkniCtx = wkniCtx;
        this.wkni_resumen = wkniCtx.wkniResumen();

        // --- fecha: flechas, calendario y "Hoy" (no se registra el futuro)
        wkniDia.setDayCellFactory(wkniDp -> new DateCell() {
            @Override public void updateItem(LocalDate wkni_fecha, boolean wkniVacio) {
                super.updateItem(wkni_fecha, wkniVacio);
                if (wkni_fecha != null) setDisable(wkni_fecha.isAfter(LocalDate.now()));
            }
        });
        wkniDia.setPrefWidth(140);
        wkniDia.valueProperty().addListener((wkniO, wkniA, wkniB) -> {
            if (wkniB == null || wkniB.isAfter(LocalDate.now())) { wkniDia.setValue(wkniA == null ? LocalDate.now() : wkniA); return; }
            wkniRecargar(true);
        });
        Button wkniAntes = wkniUi.wkniIconButton("chevron-left", "Día anterior");
        wkniAntes.setOnAction(wkniE -> wkniDia.setValue(wkniDia.getValue().minusDays(1)));
        Button wkniDespues = wkniUi.wkniIconButton("chevron-right", "Día siguiente");
        wkniDespues.setOnAction(wkniE -> { if (wkniDia.getValue().isBefore(LocalDate.now())) wkniDia.setValue(wkniDia.getValue().plusDays(1)); });
        Button wkniHoy = wkniUi.wkniSecondary("calendar", "Hoy");
        wkniHoy.setOnAction(wkniE -> wkniDia.setValue(LocalDate.now()));

        Button wkniExcel = wkniUi.wkniSecondary("download", "Exportar a Excel");
        wkniExcel.setTooltip(new Tooltip("Libro .xlsx con el registro mensual a colores, el día elegido y el semáforo de riesgo"));
        wkniExcel.setOnAction(wkniE -> wkniExportar());
        Button wkniImprimir = wkniUi.wkniSecondary("print", "Imprimir");
        wkniImprimir.setTooltip(new Tooltip("Imprime la tabla tal como se ve. Elige «Microsoft Print to PDF» para guardarla como PDF."));
        wkniImprimir.setOnAction(wkniE -> wkniImpresionTabla.wkniImprimir(wkniRoot, wkniTablaActual()));

        HBox wkni_fecha = new HBox(6, wkniAntes, wkniDia, wkniDespues, wkniHoy);
        wkni_fecha.setAlignment(Pos.CENTER_LEFT);
        HBox wkniBarra1 = new HBox(12, wkni_fecha, wkni_vista.wkniNode(), wkniUi.wkniSpacer(), wkniExcel, wkniImprimir);
        wkniBarra1.setAlignment(Pos.CENTER_LEFT);

        // --- acciones rápidas del día
        Button wkniTodos = wkniUi.wkniPrimary("check", "Marcar todos presentes");
        wkniTodos.setTooltip(new Tooltip("Marca «Presente» a quienes aún no tienen registro hoy; luego corrige las excepciones."));
        wkniTodos.setOnAction(wkniE -> wkniMarcarSinRegistro(wkniEstadoAsistencia.PRESENTE));
        Button wkniRestoFalta = wkniUi.wkniSecondary("close", "Sin registro → Falta");
        wkniRestoFalta.setOnAction(wkniE -> wkniMarcarSinRegistro(wkniEstadoAsistencia.FALTA));
        Button wkniLimpiar = wkniUi.wkniGhost("delete", "Limpiar día");
        wkniLimpiar.setOnAction(wkniE -> {
            if (wkniUi.wkniConfirm(wkniRoot, "¿Limpiar la asistencia?", "Se quitará lo registrado el " + wkniUi.wkniFormatDateShort(wkniDia.getValue())
                + " en este curso.", "Limpiar")) {
                wkniCtx.wkniDao().wkniQuitarAsistenciaDia(wkniCtx.wkniClase().wkniId(), wkniDia.getValue());
                wkniRecargar(false); wkniAvisarGuardado(); wkniCtx.wkniCambio();
            }
        });
        Label wkniAtajos = new Label("Atajos: P presente · F falta · T tardanza · J justificada · Supr quitar · ↑↓ moverse");
        wkniAtajos.getStyleClass().add("atajos");
        wkniAccionesDia = new HBox(10, wkniTodos, wkniRestoFalta, wkniLimpiar, wkniUi.wkniSpacer(), wkniAtajos);
        wkniAccionesDia.setAlignment(Pos.CENTER_LEFT);

        wkniGuardado.getStyleClass().add("guardado-ok");
        HBox.setHgrow(wkniBarraDia, Priority.ALWAYS);
        wkniBarraDia.setMaxWidth(Double.MAX_VALUE);
        HBox wkniEstado = new HBox(14, wkniTituloFecha, wkniConteos, wkniUi.wkniSpacer(), wkniGuardado);
        wkniEstado.setAlignment(Pos.CENTER_LEFT);
        VBox wkniResumenDia = new VBox(10, wkniEstado, wkniBarraDia);
        wkniResumenDia.getStyleClass().add("card");
        wkniResumenDia.setStyle("-fx-padding: 12 16 12 16;");
        wkniResumenDia.setSpacing(8);

        wkniConstruirTablaDia();
        wkniConstruirTablaMes();
        wkniContenido.getChildren().setAll(wkniTablaDia);
        VBox.setVgrow(wkniContenido, Priority.ALWAYS);
        wkni_vista.wkniSelectedProperty().addListener((wkniO, wkniA, wkniB) -> {
            boolean wkniMes = wkniB.intValue() == 1;
            wkniContenido.getChildren().setAll(wkniMes ? wkniTablaMes : wkniTablaDia);
            wkniAccionesDia.setVisible(!wkniMes); wkniAccionesDia.setManaged(!wkniMes);
            wkniRecargar(true);
        });

        wkniRoot.getChildren().addAll(wkniBarra1, wkniResumenDia, wkniAccionesDia, wkniContenido);
        wkniRecargar(true);
    }

    Node wkniGetView() { return wkniRoot; }

    /** Vuelve a leer la lista y la asistencia (p. ej. tras agregar estudiantes en otra pestaña). */
    void wkniRefrescar() { wkniRecargar(true); }

    // ------------------------------------------------------------------ registro del día

    private void wkniConstruirTablaDia() {
        wkniTablaDia.getStyleClass().add("asist-tabla");
        wkniTablaDia.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        wkniTablaDia.setPlaceholder(wkniUi.wkniEmpty("contacts", "Aún no hay estudiantes",
            "Agrégalos en la pestaña «Estudiantes» o importa tu lista desde Excel."));
        wkniTablaDia.setFixedCellSize(48);

        TableColumn<wkniEstudianteClase, Number> wkniNum = new TableColumn<>("#");
        wkniNum.setCellValueFactory(wkniD -> new ReadOnlyObjectWrapper<>(wkniTablaDia.getItems().indexOf(wkniD.getValue()) + 1));
        wkniNum.setPrefWidth(42); wkniNum.setMaxWidth(50); wkniNum.setSortable(false);
        wkniTablaDia.getColumns().add(wkniNum);

        TableColumn<wkniEstudianteClase, wkniEstudianteClase> wkniNombre = new TableColumn<>("Estudiante");
        wkniNombre.setCellValueFactory(wkniD -> new ReadOnlyObjectWrapper<>(wkniD.getValue()));
        wkniNombre.setCellFactory(wkniC -> new TableCell<>() {
            @Override protected void updateItem(wkniEstudianteClase wkniE, boolean wkniEmpty) {
                super.updateItem(wkniE, wkniEmpty);
                setText(null);
                setGraphic(wkniEmpty || wkniE == null ? null : wkniDocenteUi.wkniNombre(wkniE.wkniNombre(), wkni_resumen.wkniEvaluacion(wkniE.wkniId())));
            }
        });
        wkniNombre.setPrefWidth(300); wkniNombre.setSortable(false);
        wkniTablaDia.getColumns().add(wkniNombre);

        for (wkniEstadoAsistencia wkniSt : wkniEstadoAsistencia.values()) wkniTablaDia.getColumns().add(wkniColumnaCasilla(wkniSt));

        TableColumn<wkniEstudianteClase, wkniEstudianteClase> wkniPct = new TableColumn<>("Asistencia acumulada");
        wkniPct.setCellValueFactory(wkniD -> new ReadOnlyObjectWrapper<>(wkniD.getValue()));
        wkniPct.setCellFactory(wkniC -> new TableCell<>() {
            @Override protected void updateItem(wkniEstudianteClase wkniE, boolean wkniEmpty) {
                super.updateItem(wkniE, wkniEmpty);
                setText(null);
                if (wkniEmpty || wkniE == null) { setGraphic(null); return; }
                wkniRiesgoEstudianteService.wkniEvaluacion wkniEv = wkni_resumen.wkniEvaluacion(wkniE.wkniId());
                double wkni_p = wkniEv.wkniPorcentajeAsistencia();
                String wkni_tono = Double.isNaN(wkni_p) ? "slate" : wkni_p < wkniRiesgoEstudianteService.wkniASISTENCIA_MINIMA ? "rose"
                    : wkni_p < wkniRiesgoEstudianteService.wkniASISTENCIA_ATENCION ? "amber" : "green";
                Label wkniL = new Label(wkniReportesDocente.wkniPct(wkni_p));
                wkniL.setMinWidth(44);
                wkniL.setStyle("-fx-font-weight: bold;");
                StackPane wkniBar = wkniUi.wkniBar(Double.isNaN(wkni_p) ? 0 : wkni_p / 100, wkni_tono, 6);
                HBox.setHgrow(wkniBar, Priority.ALWAYS);
                Label wkniFaltas = wkniUi.wkniCaption(wkniEv.wkniFaltas() + " F · " + wkniEv.wkniTardanzas() + " T");
                wkniFaltas.setMinWidth(64);
                HBox wkniH = new HBox(8, wkniL, wkniBar, wkniFaltas);
                wkniH.setAlignment(Pos.CENTER_LEFT);
                setGraphic(wkniH);
            }
        });
        wkniPct.setPrefWidth(260); wkniPct.setSortable(false);
        wkniTablaDia.getColumns().add(wkniPct);

        // Fila pintada según el estado del día.
        wkniTablaDia.setRowFactory(wkniTv -> new TableRow<>() {
            @Override protected void updateItem(wkniEstudianteClase wkniE, boolean wkniEmpty) {
                super.updateItem(wkniE, wkniEmpty);
                getStyleClass().removeIf(wkniS -> wkniS.startsWith("fila-"));
                if (!wkniEmpty && wkniE != null) {
                    wkniEstadoAsistencia wkniSt = wkniEstadoDe(wkniE);
                    if (wkniSt != null) getStyleClass().add("fila-" + wkniDocenteUi.wkniCss(wkniSt));
                }
            }
        });

        wkniTablaDia.setOnKeyPressed(wkniK -> {
            wkniEstudianteClase wkniSel = wkniTablaDia.getSelectionModel().getSelectedItem();
            if (wkniSel == null) return;
            wkniEstadoAsistencia wkniSt = switch (wkniK.getCode()) {
                case P -> wkniEstadoAsistencia.PRESENTE;
                case F, A -> wkniEstadoAsistencia.FALTA;
                case T -> wkniEstadoAsistencia.TARDANZA;
                case J -> wkniEstadoAsistencia.JUSTIFICADA;
                default -> null;
            };
            boolean wkniBorrar = wkniK.getCode() == KeyCode.DELETE || wkniK.getCode() == KeyCode.BACK_SPACE;
            if (wkniSt == null && !wkniBorrar) return;
            int wkniI = wkniTablaDia.getSelectionModel().getSelectedIndex();
            wkniMarcar(wkniSel, wkniSt);
            if (wkniI + 1 < wkniTablaDia.getItems().size()) {
                wkniTablaDia.getSelectionModel().select(wkniI + 1);
                wkniTablaDia.scrollTo(Math.max(0, wkniI - 3));
            }
            wkniK.consume();
        });
    }

    private TableColumn<wkniEstudianteClase, wkniEstudianteClase> wkniColumnaCasilla(wkniEstadoAsistencia wkniSt) {
        TableColumn<wkniEstudianteClase, wkniEstudianteClase> wkniCol = new TableColumn<>(wkniSt.wkniEtiqueta());
        wkniCol.setCellValueFactory(wkniD -> new ReadOnlyObjectWrapper<>(wkniD.getValue()));
        wkniCol.setCellFactory(wkniC -> new TableCell<>() {
            private final CheckBox wkniCheck = new CheckBox();
            {
                wkniCheck.getStyleClass().addAll("asist-check", "c-" + wkniDocenteUi.wkniCss(wkniSt));
                wkniCheck.setFocusTraversable(false);
                wkniCheck.setTooltip(new Tooltip(wkniSt.wkniEtiqueta() + " (tecla " + wkniSt.wkniLetra() + ")"));
                wkniCheck.setOnAction(wkniE -> {
                    wkniEstudianteClase wkniEst = getItem();
                    if (wkniEst == null) return;
                    wkniMarcar(wkniEst, wkniCheck.isSelected() ? wkniSt : null);
                });
                setAlignment(Pos.CENTER);
            }
            @Override protected void updateItem(wkniEstudianteClase wkniE, boolean wkniEmpty) {
                super.updateItem(wkniE, wkniEmpty);
                setText(null);
                if (wkniEmpty || wkniE == null) { setGraphic(null); return; }
                wkniCheck.setSelected(wkniEstadoDe(wkniE) == wkniSt);
                setGraphic(wkniCheck);
            }
        });
        wkniCol.setPrefWidth(104); wkniCol.setMinWidth(90); wkniCol.setSortable(false);
        wkniCol.setStyle("-fx-alignment: CENTER;");
        return wkniCol;
    }

    private wkniEstadoAsistencia wkniEstadoDe(wkniEstudianteClase wkniE) {
        return wkniEstadoAsistencia.wkniDe(wkni_resumen.wkniHistorial(wkniE.wkniId()).get(wkniDia.getValue()));
    }

    /** Guarda el estado (null = quitar) en la BD local al instante y repinta. */
    private void wkniMarcar(wkniEstudianteClase wkniE, wkniEstadoAsistencia wkniSt) {
        boolean wkniOk = wkniSt == null
            ? wkniCtx.wkniDao().wkniQuitarAsistencia(wkniE.wkniId(), wkniDia.getValue())
            : wkniCtx.wkniDao().wkniGuardarAsistencia(wkniCtx.wkniClase().wkniId(), wkniE.wkniId(), wkniDia.getValue(), wkniSt.wkniValor());
        if (!wkniOk && wkniSt != null) { wkniDocenteUi.wkniError(wkniRoot, "No se pudo guardar la asistencia de " + wkniE.wkniNombre() + "."
            + (wkniCtx.wkniDao().wkniUltimoError() != null ? "\n\nMotivo: " + wkniCtx.wkniDao().wkniUltimoError() : "")); return; }
        wkniRecargar(false);
        wkniAvisarGuardado();
        wkniCtx.wkniCambio();
    }

    private void wkniMarcarSinRegistro(wkniEstadoAsistencia wkniSt) {
        int wkniN = 0;
        for (wkniEstudianteClase wkniE : wkni_resumen.wkniEstudiantes()) {
            if (wkniEstadoDe(wkniE) != null) continue;
            if (wkniCtx.wkniDao().wkniGuardarAsistencia(wkniCtx.wkniClase().wkniId(), wkniE.wkniId(), wkniDia.getValue(), wkniSt.wkniValor())) wkniN++;
        }
        wkniRecargar(false);
        if (wkniN > 0) { wkniAvisarGuardado(); wkniCtx.wkniCambio(); }
        else wkniDocenteUi.wkniInfo(wkniRoot, "Todos los estudiantes ya tienen registro este día.");
    }

    private void wkniAvisarGuardado() {
        wkniGuardado.setText("✓ Guardado en este equipo · " + LocalTime.now().withNano(0));
    }

    // ------------------------------------------------------------------ vista mensual

    private void wkniConstruirTablaMes() {
        wkniTablaMes.getStyleClass().add("asist-tabla");
        wkniTablaMes.setPlaceholder(wkniUi.wkniMuted("Aún no hay estudiantes en este curso."));
        wkniTablaMes.setFixedCellSize(40);
    }

    private void wkniColumnasMes() {
        LocalDate wkniD = wkniDia.getValue();
        LocalDate wkniDesde = wkniD.withDayOfMonth(1), wkniHasta = wkniD.withDayOfMonth(wkniD.lengthOfMonth());
        List<LocalDate> wkniFechas = new ArrayList<>();
        for (LocalDate wkniF : wkni_resumen.wkniFechas()) if (!wkniF.isBefore(wkniDesde) && !wkniF.isAfter(wkniHasta)) wkniFechas.add(wkniF);
        if (!wkniFechas.contains(wkniD)) { wkniFechas.add(wkniD); wkniFechas.sort(null); }

        wkniTablaMes.getColumns().clear();
        TableColumn<wkniEstudianteClase, wkniEstudianteClase> wkniNombre = new TableColumn<>("Estudiante");
        wkniNombre.setCellValueFactory(wkniX -> new ReadOnlyObjectWrapper<>(wkniX.getValue()));
        wkniNombre.setCellFactory(wkniC -> new TableCell<>() {
            @Override protected void updateItem(wkniEstudianteClase wkniE, boolean wkniEmpty) {
                super.updateItem(wkniE, wkniEmpty);
                setText(null);
                setGraphic(wkniEmpty || wkniE == null ? null : wkniDocenteUi.wkniNombre(wkniE.wkniNombre(), wkni_resumen.wkniEvaluacion(wkniE.wkniId())));
            }
        });
        wkniNombre.setPrefWidth(270); wkniNombre.setSortable(false);
        wkniTablaMes.getColumns().add(wkniNombre);

        for (LocalDate wkniF : wkniFechas) {
            String wkniDow = wkniF.getDayOfWeek().getDisplayName(TextStyle.SHORT, wkniUi.wkniES);
            TableColumn<wkniEstudianteClase, wkniEstudianteClase> wkniCol = new TableColumn<>(wkniDow.substring(0, Math.min(2, wkniDow.length())) + "\n" + wkniF.format(wkniCORTA));
            wkniCol.setCellValueFactory(wkniX -> new ReadOnlyObjectWrapper<>(wkniX.getValue()));
            wkniCol.setCellFactory(wkniC -> new TableCell<>() {
                @Override protected void updateItem(wkniEstudianteClase wkniE, boolean wkniEmpty) {
                    super.updateItem(wkniE, wkniEmpty);
                    setText(null);
                    if (wkniEmpty || wkniE == null) { setGraphic(null); return; }
                    wkniEstadoAsistencia wkniSt = wkniEstadoAsistencia.wkniDe(wkni_resumen.wkniHistorial(wkniE.wkniId()).get(wkniF));
                    Label wkniL = new Label(wkniSt == null ? "·" : wkniSt.wkniLetra());
                    wkniL.getStyleClass().add("celda-estado");
                    if (wkniSt != null) wkniL.getStyleClass().add("e-" + wkniDocenteUi.wkniCss(wkniSt));
                    Tooltip.install(wkniL, new Tooltip(wkniE.wkniNombre() + " · " + wkniUi.wkniFormatDateShort(wkniF) + "\n"
                        + (wkniSt == null ? "Sin registro" : wkniSt.wkniEtiqueta()) + "\nClic para cambiar"));
                    wkniL.setOnMouseClicked(wkniM -> {
                        wkniEstadoAsistencia wkniSig = wkniEstadoAsistencia.wkniSiguiente(wkniSt);
                        if (wkniSig == null) wkniCtx.wkniDao().wkniQuitarAsistencia(wkniE.wkniId(), wkniF);
                        else wkniCtx.wkniDao().wkniGuardarAsistencia(wkniCtx.wkniClase().wkniId(), wkniE.wkniId(), wkniF, wkniSig.wkniValor());
                        wkniRecargar(false); wkniAvisarGuardado(); wkniCtx.wkniCambio();
                    });
                    setGraphic(wkniL);
                    setAlignment(Pos.CENTER);
                }
            });
            wkniCol.setPrefWidth(56); wkniCol.setMinWidth(50); wkniCol.setSortable(false);
            if (wkniF.equals(wkniD)) wkniCol.setStyle("-fx-background-color: -accent-soft;");
            wkniTablaMes.getColumns().add(wkniCol);
        }
        wkniTablaMes.getColumns().add(wkniTotal("P", wkniE -> wkniContar(wkniE, wkniEstadoAsistencia.PRESENTE, wkniDesde, wkniHasta)));
        wkniTablaMes.getColumns().add(wkniTotal("F", wkniE -> wkniContar(wkniE, wkniEstadoAsistencia.FALTA, wkniDesde, wkniHasta)));
        wkniTablaMes.getColumns().add(wkniTotal("T", wkniE -> wkniContar(wkniE, wkniEstadoAsistencia.TARDANZA, wkniDesde, wkniHasta)));
        wkniTablaMes.getColumns().add(wkniTotal("J", wkniE -> wkniContar(wkniE, wkniEstadoAsistencia.JUSTIFICADA, wkniDesde, wkniHasta)));
        wkniTablaMes.getColumns().add(wkniTotal("% curso", wkniE -> wkniReportesDocente.wkniPct(wkni_resumen.wkniEvaluacion(wkniE.wkniId()).wkniPorcentajeAsistencia())));
    }

    private TableColumn<wkniEstudianteClase, String> wkniTotal(String wkniTitulo, Function<wkniEstudianteClase, String> wkniValor) {
        TableColumn<wkniEstudianteClase, String> wkniC = new TableColumn<>(wkniTitulo);
        wkniC.setCellValueFactory(wkniX -> new ReadOnlyObjectWrapper<>(wkniValor.apply(wkniX.getValue())));
        wkniC.setPrefWidth(wkniTitulo.length() > 2 ? 76 : 44); wkniC.setSortable(false);
        wkniC.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
        return wkniC;
    }

    private String wkniContar(wkniEstudianteClase wkniE, wkniEstadoAsistencia wkniSt, LocalDate wkniDesde, LocalDate wkniHasta) {
        return Long.toString(wkni_resumen.wkniHistorial(wkniE.wkniId()).subMap(wkniDesde, true, wkniHasta, true).values().stream()
            .filter(wkniV -> wkniEstadoAsistencia.wkniDe(wkniV) == wkniSt).count());
    }

    // ------------------------------------------------------------------ recarga y resumen

    private void wkniRecargar(boolean wkniReconstruirColumnas) {
        wkni_resumen = wkniCtx.wkniResumen();
        wkniTablaDia.getItems().setAll(wkni_resumen.wkniEstudiantes());
        wkniTablaMes.getItems().setAll(wkni_resumen.wkniEstudiantes());
        if (wkni_vista.wkniSelected() == 1 && (wkniReconstruirColumnas || wkniTablaMes.getColumns().isEmpty())) wkniColumnasMes();
        wkniTablaDia.refresh();
        wkniTablaMes.refresh();
        wkniPintarResumen();
    }

    private void wkniPintarResumen() {
        LocalDate wkniD = wkniDia.getValue();
        boolean wkniEsHoy = wkniD.equals(LocalDate.now());
        wkniTituloFecha.setText((wkniEsHoy ? "Hoy, " : "") + wkniUi.wkniFormatDate(wkniD));
        int[] wkniN = new int[4];
        int wkniSin = 0;
        for (wkniEstudianteClase wkniE : wkni_resumen.wkniEstudiantes()) {
            wkniEstadoAsistencia wkniSt = wkniEstadoDe(wkniE);
            if (wkniSt == null) wkniSin++; else wkniN[wkniSt.ordinal()]++;
        }
        wkniConteos.getChildren().setAll(
            wkniDocenteUi.wkniConteo(wkniDocenteUi.wkniColor(wkniEstadoAsistencia.PRESENTE), wkniN[0] + (wkniN[0] == 1 ? " presente" : " presentes"), "green"),
            wkniDocenteUi.wkniConteo(wkniDocenteUi.wkniColor(wkniEstadoAsistencia.FALTA), wkniN[1] + (wkniN[1] == 1 ? " falta" : " faltas"), "rose"),
            wkniDocenteUi.wkniConteo(wkniDocenteUi.wkniColor(wkniEstadoAsistencia.TARDANZA), wkniN[2] + (wkniN[2] == 1 ? " tardanza" : " tardanzas"), "amber"),
            wkniDocenteUi.wkniConteo(wkniDocenteUi.wkniColor(wkniEstadoAsistencia.JUSTIFICADA), wkniN[3] + (wkniN[3] == 1 ? " justificada" : " justificadas"), "blue"));
        if (wkniSin > 0) wkniConteos.getChildren().add(wkniDocenteUi.wkniConteo("-c-slate", wkniSin + " sin registro", "slate"));
        wkniBarraDia.getChildren().setAll(wkniDocenteUi.wkniBarraApilada(new double[]{wkniN[0], wkniN[2], wkniN[3], wkniN[1], wkniSin},
            new String[]{"-c-green", "#E8B43A", "-c-blue", "-danger", "-surface3"}, 10));
        if (wkniGuardado.getText().isEmpty()) wkniGuardado.setText("Se guarda automáticamente al marcar · funciona sin internet");
    }

    // ------------------------------------------------------------------ exportar e imprimir

    private wkniTablaReporte wkniTablaActual() {
        LocalDate wkniD = wkniDia.getValue();
        return wkni_vista.wkniSelected() == 1
            ? wkniReportesDocente.wkniAsistenciaMatriz(wkni_resumen, wkniD.withDayOfMonth(1), wkniD.withDayOfMonth(wkniD.lengthOfMonth()), List.of(wkniD))
            : wkniReportesDocente.wkniAsistenciaDia(wkni_resumen, wkniD);
    }

    private void wkniExportar() {
        LocalDate wkniD = wkniDia.getValue();
        List<wkniTablaReporte> wkniHojas = List.of(
            wkniReportesDocente.wkniAsistenciaMatriz(wkni_resumen, wkniD.withDayOfMonth(1), wkniD.withDayOfMonth(wkniD.lengthOfMonth()), List.of(wkniD)),
            wkniReportesDocente.wkniAsistenciaDia(wkni_resumen, wkniD),
            wkniReportesDocente.wkniRiesgo(wkni_resumen));
        wkniDocenteUi.wkniExportarExcel(wkniRoot, "Asistencia " + wkniCtx.wkniClase() + " " + wkniD.getMonth().getDisplayName(TextStyle.FULL, wkniUi.wkniES)
            + " " + wkniD.getYear(), wkniHojas);
    }
}

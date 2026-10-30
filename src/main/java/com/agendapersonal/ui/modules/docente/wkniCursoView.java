package com.agendapersonal.ui.modules.docente;

import com.agendapersonal.controller.wkniMainController;
import com.agendapersonal.domain.model.wkniActividadClase;
import com.agendapersonal.domain.model.wkniClaseDocente;
import com.agendapersonal.domain.model.wkniEstudianteClase;
import com.agendapersonal.infrastructure.repo.wkniClaseDocenteDAO;
import com.agendapersonal.service.wkniExcelClaseService;
import com.agendapersonal.service.wkniReportesDocente;
import com.agendapersonal.service.wkniResumenCurso;
import com.agendapersonal.service.wkniRiesgoEstudianteService;
import com.agendapersonal.ui.wkniEventViews;
import com.agendapersonal.util.wkniUi;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.function.IntConsumer;

/**
 * Espacio de trabajo de un curso, en pestañas: Panel (gráficas y semáforo), Asistencia, Trabajos y notas,
 * Estudiantes, Planeación y Aula virtual. Recuerda la pestaña abierta al refrescar.
 */
public final class wkniCursoView {
    private final wkniClaseDocente wkniClase;
    private final wkniClaseDocenteDAO wkniDao;
    private final Stage wkniStage;
    private final wkniMainController wkniController;
    private final wkniCursoContexto wkniCtx;
    private final TabPane wkniPestanas = new TabPane();
    private final VBox wkniRoot = new VBox(10);
    private final wkniPanelCurso wkni_panel;
    private final wkniAsistenciaPanel wkniAsistencia;
    private final wkniTrabajosPanel wkniTrabajos;
    private final Tab wkniTPanel, wkniTAsistencia, wkniTTrabajos, wkniTEstudiantes, wkniTPlaneacion, wkniTAula;

    public wkniCursoView(wkniClaseDocente wkniClase, wkniClaseDocenteDAO wkniDao, Stage wkniStage, wkniMainController wkniController, int wkniPestanaInicial,
                     IntConsumer wkniAlCambiarPestana, Runnable wkniAlCambiarDatos, Runnable wkniEditarCurso, Runnable wkniEliminarCurso) {
        this.wkniClase = wkniClase;
        this.wkniDao = wkniDao;
        this.wkniStage = wkniStage;
        this.wkniController = wkniController;
        this.wkniCtx = new wkniCursoContexto(wkniClase, wkniDao, wkniAlCambiarDatos);

        wkni_panel = new wkniPanelCurso(wkniCtx, wkniI -> wkniPestanas.getSelectionModel().select(wkniI));
        wkniAsistencia = new wkniAsistenciaPanel(wkniCtx);
        wkniTrabajos = new wkniTrabajosPanel(wkniCtx, () -> wkniCrearActividad("Tarea"));
        wkniTPanel = wkniTab("Panel", "chart", wkni_panel.wkniGetView());
        wkniTAsistencia = wkniTab("Asistencia", "user-check", wkniAsistencia.wkniGetView());
        wkniTTrabajos = wkniTab("Trabajos y notas", "tasks", wkniTrabajos.wkniGetView());
        wkniTEstudiantes = wkniTab("Estudiantes", "contacts", wkniEstudiantes());
        wkniTPlaneacion = wkniTab("Planeación", "calendar", wkniPlaneacion());
        wkniTAula = wkniTab("Aula virtual", "video", new wkniAulaVirtualPanel(wkniCtx).wkniGetView());
        wkniPestanas.getTabs().setAll(wkniTPanel, wkniTAsistencia, wkniTTrabajos, wkniTEstudiantes, wkniTPlaneacion, wkniTAula);
        wkniPestanas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        wkniPestanas.getSelectionModel().select(Math.max(0, Math.min(wkniPestanaInicial, wkniPestanas.getTabs().size() - 1)));
        // Cada pestaña se pone al día al abrirla: así las gráficas reflejan lo último que se marcó.
        wkniPestanas.getSelectionModel().selectedIndexProperty().addListener((wkniO, wkniA, wkniB) -> {
            wkniAlCambiarPestana.accept(wkniB.intValue());
            Tab wkniT = wkniPestanas.getTabs().get(wkniB.intValue());
            if (wkniT == wkniTPanel) wkni_panel.wkniRecargar();
            else if (wkniT == wkniTAsistencia) wkniAsistencia.wkniRefrescar();
            else if (wkniT == wkniTTrabajos) wkniTrabajos.wkniRecargar();
            else if (wkniT == wkniTEstudiantes) wkniT.setContent(wkniEstudiantes());
            else if (wkniT == wkniTPlaneacion) wkniT.setContent(wkniPlaneacion());
        });
        VBox.setVgrow(wkniPestanas, Priority.ALWAYS);

        Label wkniNombre = wkniUi.wkniH2(wkniClase.toString());
        HBox wkniTitulo = new HBox(10, wkniNombre);
        wkniTitulo.setAlignment(Pos.CENTER_LEFT);
        if (wkniClase.wkniPeriodo() != null) wkniTitulo.getChildren().add(wkniUi.wkniPill(wkniClase.wkniPeriodo(), "slate"));
        wkniTitulo.getChildren().add(wkniUi.wkniPill("Aprueba con " + wkniRiesgoEstudianteService.wkniNumero(wkniClase.wkniNotaMinima()), "blue"));
        Button wkniEditar = wkniUi.wkniGhost("edit", "Editar curso");
        wkniEditar.setOnAction(wkniE -> wkniEditarCurso.run());
        Button wkniEliminar = wkniUi.wkniGhost("delete", "Eliminar");
        wkniEliminar.getStyleClass().add("text-danger-btn");
        wkniEliminar.setOnAction(wkniE -> wkniEliminarCurso.run());
        HBox wkniCabecera = new HBox(10, wkniTitulo, wkniUi.wkniSpacer(), wkniEditar, wkniEliminar);
        wkniCabecera.setAlignment(Pos.CENTER_LEFT);
        wkniRoot.getChildren().addAll(wkniCabecera, wkniPestanas);
    }

    public Node wkniGetView() { return wkniRoot; }

    /** Cifra compacta (ícono, valor y etiqueta en una línea) para el encabezado del Espacio Docente. */
    public static HBox wkniCifra(String wkniIcono, String wkni_tono, String wkniValor, String wkniEtiqueta) {
        Label wkniV = new Label(wkniValor);
        wkniV.getStyleClass().add("cifra-valor");
        Label wkniL = new Label(wkniEtiqueta);
        wkniL.getStyleClass().add("cifra-etiqueta");
        wkniL.setWrapText(true);
        HBox wkniH = new HBox(12, wkniUi.wkniIconChip(wkniIcono, wkni_tono, 38, 19), new VBox(0, wkniV, wkniL));
        wkniH.setAlignment(Pos.CENTER_LEFT);
        wkniH.getStyleClass().addAll("card", "cifra");
        HBox.setHgrow(wkniH, Priority.ALWAYS);
        wkniH.setMaxWidth(Double.MAX_VALUE);
        return wkniH;
    }

    /** Fila del semáforo reutilizada por la pestaña global de alertas. */
    public static HBox wkniFilaAlerta(String wkniNombre, wkniRiesgoEstudianteService.wkniEvaluacion wkniEv, String wkniCurso, Runnable wkniAbrir) {
        return wkniPanelCurso.wkniFilaAlerta(wkniNombre, wkniEv, wkniCurso, wkniAbrir);
    }

    private Tab wkniTab(String wkniTitle, String wkniIcon, Node wkniContent) {
        Tab wkniTab = new Tab(wkniTitle, wkniContent);
        wkniTab.setGraphic(com.agendapersonal.ui.wkniIconFactory.wkniIcon(wkniIcon, 16));
        return wkniTab;
    }

    private void wkniCambio() { wkniCtx.wkniCambio(); }

    // ------------------------------------------------------------------ estudiantes

    private Node wkniEstudiantes() {
        VBox wkniBox = wkniDocenteUi.wkniPanel();
        wkniResumenCurso wkniR = wkniCtx.wkniResumen();
        FilteredList<wkniEstudianteClase> wkniFiltrados = new FilteredList<>(FXCollections.observableArrayList(wkniR.wkniEstudiantes()));
        TableView<wkniEstudianteClase> wkniTable = new TableView<>(wkniFiltrados);
        wkniTable.getStyleClass().add("asist-tabla");
        wkniTable.setFixedCellSize(46);
        wkniTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        wkniTable.setPlaceholder(wkniUi.wkniEmpty("contacts", "Aún no hay estudiantes",
            "Añádelos uno por uno o importa una lista de Excel con la columna «Estudiante» o «Nombre»."));

        TableColumn<wkniEstudianteClase, wkniEstudianteClase> wkniNombre = new TableColumn<>("Estudiante");
        wkniNombre.setCellValueFactory(wkniD -> new ReadOnlyObjectWrapper<>(wkniD.getValue()));
        wkniNombre.setCellFactory(wkniC -> new TableCell<>() {
            @Override protected void updateItem(wkniEstudianteClase wkniE, boolean wkniEmpty) {
                super.updateItem(wkniE, wkniEmpty);
                setText(null);
                setGraphic(wkniEmpty || wkniE == null ? null : wkniDocenteUi.wkniNombre(wkniE.wkniNombre(), wkniR.wkniEvaluacion(wkniE.wkniId())));
            }
        });
        wkniNombre.setPrefWidth(280);
        wkniTable.getColumns().add(wkniNombre);
        wkniTable.getColumns().add(wkniTexto("Correo", wkniE -> wkniE.wkniCorreo() == null ? "" : wkniE.wkniCorreo(), 200));
        wkniTable.getColumns().add(wkniTexto("Nota del curso", wkniE -> wkniE.wkniCalificacion() == null ? "—" : wkniRiesgoEstudianteService.wkniNumero(wkniE.wkniCalificacion()), 110));
        wkniTable.getColumns().add(wkniTexto("Asistencia", wkniE -> wkniReportesDocente.wkniPct(wkniR.wkniEvaluacion(wkniE.wkniId()).wkniPorcentajeAsistencia()), 100));
        TableColumn<wkniEstudianteClase, wkniEstudianteClase> wkniEstado = new TableColumn<>("Situación");
        wkniEstado.setCellValueFactory(wkniD -> new ReadOnlyObjectWrapper<>(wkniD.getValue()));
        wkniEstado.setCellFactory(wkniC -> new TableCell<>() {
            @Override protected void updateItem(wkniEstudianteClase wkniE, boolean wkniEmpty) {
                super.updateItem(wkniE, wkniEmpty);
                setText(null);
                if (wkniEmpty || wkniE == null) { setGraphic(null); return; }
                wkniRiesgoEstudianteService.wkniEvaluacion wkniEv = wkniR.wkniEvaluacion(wkniE.wkniId());
                Label wkni_p = wkniUi.wkniPill(wkniEv.wkni_nivel().wkniEtiqueta(), wkniDocenteUi.wkniTono(wkniEv.wkni_nivel()));
                if (!wkniEv.wkniMotivos().isEmpty()) wkni_p.setTooltip(new Tooltip(String.join("\n", wkniEv.wkniMotivos())));
                setGraphic(wkni_p);
            }
        });
        wkniEstado.setPrefWidth(120);
        wkniTable.getColumns().add(wkniEstado);
        wkniTable.getColumns().add(wkniTexto("Observaciones", wkniE -> wkniE.wkniObservaciones() == null ? "" : wkniE.wkniObservaciones(), 200));
        wkniTable.setRowFactory(wkniTv -> {
            TableRow<wkniEstudianteClase> wkniRow = new TableRow<>();
            wkniRow.setOnMouseClicked(wkniM -> {
                if (wkniM.getButton() == MouseButton.PRIMARY && wkniM.getClickCount() == 2 && !wkniRow.isEmpty()) wkniEditarEstudiante(wkniRow.getItem());
            });
            MenuItem wkniVerReporte = new MenuItem("Ver reporte para el apoderado");
            wkniVerReporte.setOnAction(wkniE -> wkniReporteEstudianteView.wkniMostrar(wkniRoot, wkniCtx, wkniRow.getItem()));
            MenuItem wkniEditarItem = new MenuItem("Editar");
            wkniEditarItem.setOnAction(wkniE -> wkniEditarEstudiante(wkniRow.getItem()));
            ContextMenu wkniMenu = new ContextMenu(wkniVerReporte, wkniEditarItem);
            wkniRow.contextMenuProperty().bind(javafx.beans.binding.Bindings.when(wkniRow.emptyProperty())
                .then((ContextMenu) null).otherwise(wkniMenu));
            return wkniRow;
        });
        VBox.setVgrow(wkniTable, Priority.ALWAYS);

        TextField wkniBuscar = new TextField();
        HBox wkniBusqueda = wkniUi.wkniSearchBox(wkniBuscar, "Buscar estudiante…");
        wkniBusqueda.setMaxWidth(260);
        wkniBuscar.textProperty().addListener((wkniO, wkniA, wkniB) -> {
            String wkniQ = wkniB == null ? "" : wkniB.trim().toLowerCase(Locale.ROOT);
            wkniFiltrados.setPredicate(wkniE -> wkniQ.isEmpty() || wkniE.wkniNombre().toLowerCase(Locale.ROOT).contains(wkniQ)
                || (wkniE.wkniCorreo() != null && wkniE.wkniCorreo().toLowerCase(Locale.ROOT).contains(wkniQ)));
        });

        Button wkniAdd = wkniUi.wkniPrimary("add", "Añadir estudiante");
        wkniAdd.setOnAction(wkniE -> wkniEditarEstudiante(null));
        Button wkniEdit = wkniUi.wkniSecondary("edit", "Editar");
        wkniEdit.setOnAction(wkniE -> {
            wkniEstudianteClase wkniSel = wkniTable.getSelectionModel().getSelectedItem();
            if (wkniSel == null) wkniDocenteUi.wkniInfo(wkniRoot, "Selecciona un estudiante (o haz doble clic sobre su fila)."); else wkniEditarEstudiante(wkniSel);
        });
        Button wkniDelete = wkniUi.wkniGhost("delete", "Eliminar");
        wkniDelete.setOnAction(wkniE -> {
            wkniEstudianteClase wkniSel = wkniTable.getSelectionModel().getSelectedItem();
            if (wkniSel == null) wkniDocenteUi.wkniInfo(wkniRoot, "Selecciona un estudiante primero.");
            else if (wkniUi.wkniConfirm(wkniRoot, "¿Eliminar a " + wkniSel.wkniNombre() + "?", "También se borrarán su asistencia y sus entregas en este curso.", "Eliminar")) {
                wkniDao.wkniEliminarEstudiante(wkniSel.wkniId()); wkniRefrescarEstudiantes();
            }
        });
        Button wkniReporte = wkniUi.wkniSecondary("print", "Reporte individual");
        wkniReporte.setTooltip(new Tooltip("Hoja para el apoderado o tutor: asistencia, trabajos, notas, alertas y firmas"));
        wkniReporte.setOnAction(wkniE -> wkniReporteEstudianteView.wkniMostrar(wkniRoot, wkniCtx, wkniTable.getSelectionModel().getSelectedItem()));
        Button wkniReportes = wkniUi.wkniGhost("copy", "Reportes de todo el curso");
        wkniReportes.setTooltip(new Tooltip("Imprime una hoja por estudiante (reunión de padres, entrega de libretas)"));
        wkniReportes.setOnAction(wkniE -> wkniReporteEstudianteView.wkniImprimirCurso(wkniRoot, wkniCtx));
        Button wkniImportar = wkniUi.wkniSecondary("upload", "Importar Excel");
        wkniImportar.setOnAction(wkniE -> wkniImportarExcel());
        Button wkniExportar = wkniUi.wkniSecondary("download", "Exportar Excel");
        wkniExportar.setOnAction(wkniE -> wkniExportarExcel());
        HBox wkniActions = new HBox(8, wkniAdd, wkniEdit, wkniDelete, wkniUi.wkniSpacer(), wkniBusqueda, wkniImportar, wkniExportar);
        wkniActions.setAlignment(Pos.CENTER_LEFT);
        HBox wkniActions2 = new HBox(8, wkniReporte, wkniReportes);
        wkniActions2.setAlignment(Pos.CENTER_LEFT);
        Label wkniAyuda = new Label(wkniR.wkniEstudiantes().size() + " estudiantes · doble clic para editar · clic derecho para ver su reporte · "
            + "al reimportar un Excel se actualiza por nombre, sin duplicar.");
        wkniAyuda.getStyleClass().add("atajos");
        wkniActions2.getChildren().addAll(wkniUi.wkniSpacer(), wkniAyuda);
        wkniBox.getChildren().addAll(wkniActions, wkniActions2, wkniTable);
        return wkniBox;
    }

    private TableColumn<wkniEstudianteClase, String> wkniTexto(String wkniTitulo, java.util.function.Function<wkniEstudianteClase, String> wkniF, double wkniAncho) {
        TableColumn<wkniEstudianteClase, String> wkniC = new TableColumn<>(wkniTitulo);
        wkniC.setCellValueFactory(wkniD -> new ReadOnlyObjectWrapper<>(wkniF.apply(wkniD.getValue())));
        wkniC.setPrefWidth(wkniAncho);
        return wkniC;
    }

    private void wkniRefrescarEstudiantes() {
        wkniTEstudiantes.setContent(wkniEstudiantes());
        wkniCambio();
    }

    private void wkniEditarEstudiante(wkniEstudianteClase wkniExistente) {
        Dialog<ButtonType> wkniDialog = new Dialog<>();
        wkniDialog.setTitle(wkniExistente == null ? "Añadir estudiante" : "Editar estudiante");
        wkniDialog.setHeaderText(wkniExistente == null ? "Nuevo estudiante en " + wkniClase : wkniExistente.wkniNombre());
        wkniDialog.initOwner(wkniStage);
        ButtonType wkniSave = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        ButtonType wkniOtro = new ButtonType("Guardar y añadir otro", ButtonBar.ButtonData.OTHER);
        if (wkniExistente == null) wkniDialog.getDialogPane().getButtonTypes().addAll(wkniOtro, wkniSave, ButtonType.CANCEL);
        else wkniDialog.getDialogPane().getButtonTypes().addAll(wkniSave, ButtonType.CANCEL);
        TextField wkniNombre = new TextField(); wkniNombre.setPromptText("Nombres y apellidos");
        TextField wkniCorreo = new TextField(); wkniCorreo.setPromptText("correo@ejemplo.com (opcional)");
        TextField wkni_nota = new TextField(); wkni_nota.setPromptText("Opcional");
        TextArea wkniObs = new TextArea(); wkniObs.setPromptText("Observaciones (opcional)"); wkniObs.setPrefRowCount(3); wkniObs.setWrapText(true);
        if (wkniExistente != null) {
            wkniNombre.setText(wkniExistente.wkniNombre());
            wkniCorreo.setText(wkniExistente.wkniCorreo() == null ? "" : wkniExistente.wkniCorreo());
            wkni_nota.setText(wkniExistente.wkniCalificacion() == null ? "" : wkniRiesgoEstudianteService.wkniNumero(wkniExistente.wkniCalificacion()));
            wkniObs.setText(wkniExistente.wkniObservaciones() == null ? "" : wkniExistente.wkniObservaciones());
        }
        VBox wkniForm = new VBox(10, wkniUi.wkniField("Nombre completo", wkniNombre), new HBox(10, wkniUi.wkniField("Correo", wkniCorreo), wkniUi.wkniField("Nota del curso", wkni_nota)),
            wkniUi.wkniField("Observaciones", wkniObs));
        wkniForm.setPadding(new Insets(8, 4, 4, 4));
        wkniForm.setPrefWidth(460);
        wkniDialog.getDialogPane().setContent(wkniForm);
        javafx.application.Platform.runLater(wkniNombre::requestFocus);
        var wkniRes = wkniDialog.showAndWait();
        if (wkniRes.isEmpty() || (wkniRes.get() != wkniSave && wkniRes.get() != wkniOtro)) return;
        if (wkniNombre.getText().isBlank()) { wkniDocenteUi.wkniInfo(wkniRoot, "El nombre es obligatorio."); return; }
        Double wkniGrade;
        try { wkniGrade = wkniDocenteUi.wkniNota(wkni_nota.getText()); }
        catch (NumberFormatException wkniEx) { wkniDocenteUi.wkniInfo(wkniRoot, "La nota debe ser un número (por ejemplo 15 o 15.5)."); return; }
        if (wkniExistente != null && !wkniExistente.wkniNombre().equalsIgnoreCase(wkniNombre.getText().trim())) {
            // Cambio de nombre: se borra el registro viejo solo si el nuevo nombre no choca con otro estudiante.
            boolean wkniChoca = wkniDao.wkniListarEstudiantes(wkniClase.wkniId()).stream()
                .anyMatch(wkniS -> wkniS.wkniId() != wkniExistente.wkniId() && wkniS.wkniNombre().equalsIgnoreCase(wkniNombre.getText().trim()));
            if (wkniChoca) { wkniDocenteUi.wkniInfo(wkniRoot, "Ya existe un estudiante con ese nombre en el curso."); return; }
            if (!wkniRenombrar(wkniExistente.wkniId(), wkniNombre.getText().trim())) { wkniDocenteUi.wkniError(wkniRoot, "No se pudo cambiar el nombre."); return; }
        }
        if (!wkniDao.wkniGuardarEstudiante(wkniClase.wkniId(), wkniNombre.getText(), wkniCorreo.getText(), wkniGrade, wkniObs.getText())) {
            wkniDocenteUi.wkniError(wkniRoot, "No se pudo guardar el estudiante."); return;
        }
        wkniRefrescarEstudiantes();
        if (wkniRes.get() == wkniOtro) wkniEditarEstudiante(null);
    }

    /** Renombra conservando el id (y con él su asistencia y entregas). */
    private boolean wkniRenombrar(int wkniId, String wkniNuevo) {
        try (var wkniC = com.agendapersonal.database.wkniDatabaseConnection.wkniGetConnection();
             var wkni_p = wkniC.prepareStatement("UPDATE estudiante_clase SET nombre=? WHERE id=?")) {
            wkni_p.setString(1, wkniNuevo); wkni_p.setInt(2, wkniId);
            return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("renombrar estudiante", wkniE); return false; }
    }

    private void wkniImportarExcel() {
        FileChooser wkniChooser = new FileChooser();
        wkniChooser.setTitle("Importar lista de estudiantes");
        wkniChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Libro de Excel (*.xlsx)", "*.xlsx"));
        File wkniFile = wkniChooser.showOpenDialog(wkniDocenteUi.wkniVentana(wkniRoot));
        if (wkniFile == null) return;
        try {
            wkniExcelClaseService.wkniResultadoImportacion wkni_resultado = wkniExcelClaseService.wkniImportar(wkniFile.toPath(), wkniClase.wkniId());
            int wkniGuardados = 0;
            for (wkniEstudianteClase wkniE : wkni_resultado.wkniEstudiantes())
                if (wkniDao.wkniGuardarEstudiante(wkniClase.wkniId(), wkniE.wkniNombre(), wkniE.wkniCorreo(), wkniE.wkniCalificacion(), wkniE.wkniObservaciones())) wkniGuardados++;
            wkniRefrescarEstudiantes();
            wkniDocenteUi.wkniInfo(wkniRoot, "Importación completada: " + wkniGuardados + " estudiantes guardados."
                + (wkni_resultado.wkniFilasOmitidas() == 0 ? "" : " " + wkni_resultado.wkniFilasOmitidas() + " filas con nota inválida se omitieron."));
        } catch (Exception wkniEx) { wkniDocenteUi.wkniError(wkniRoot, "No se pudo importar el libro: " + wkniEx.getMessage()); }
    }

    private void wkniExportarExcel() {
        FileChooser wkniChooser = new FileChooser();
        wkniChooser.setTitle("Exportar estudiantes y notas");
        wkniChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Libro de Excel (*.xlsx)", "*.xlsx"));
        wkniChooser.setInitialFileName(wkniDocenteUi.wkniArchivo("Estudiantes " + wkniClase) + ".xlsx");
        File wkniFile = wkniChooser.showSaveDialog(wkniDocenteUi.wkniVentana(wkniRoot));
        if (wkniFile == null) return;
        File wkniDestino = wkniFile.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx") ? wkniFile : new File(wkniFile.getParentFile(), wkniFile.getName() + ".xlsx");
        try {
            wkniExcelClaseService.wkniExportar(wkniDestino.toPath(), wkniClase, wkniDao.wkniListarEstudiantes(wkniClase.wkniId()));
            wkniDocenteUi.wkniInfo(wkniRoot, "Lista exportada a:\n" + wkniDestino.getAbsolutePath() + "\n\nPuedes editarla y volver a importarla.");
        } catch (Exception wkniEx) { wkniDocenteUi.wkniError(wkniRoot, "No se pudo exportar el libro: " + wkniEx.getMessage()); }
    }

    // ------------------------------------------------------------------ planeación

    private Node wkniPlaneacion() {
        VBox wkniBox = wkniDocenteUi.wkniPanel();
        HBox wkniAcciones = new HBox(8);
        for (String wkni_tipo : List.of("Clase", "Material", "Tarea", "Evaluación")) {
            Button wkniB = wkni_tipo.equals("Clase") ? wkniUi.wkniPrimary("add", "Clase") : wkniUi.wkniSecondary("add", wkni_tipo);
            wkniB.setOnAction(wkniE -> wkniCrearActividad(wkni_tipo));
            wkniAcciones.getChildren().add(wkniB);
        }
        Label wkniAyuda = wkniUi.wkniMuted("Las tareas, actividades y evaluaciones aparecen como columnas en «Trabajos y notas» para registrar entregas.");
        wkniAyuda.setWrapText(true);
        wkniBox.getChildren().addAll(wkniAcciones, wkniAyuda);
        List<wkniActividadClase> wkniActividades = wkniDao.wkniListarActividades(wkniClase.wkniId());
        if (wkniActividades.isEmpty()) wkniBox.getChildren().add(wkniUi.wkniEmpty("calendar", "Planeación vacía",
            "Añade tus sesiones, materiales, tareas y evaluaciones con su fecha."));
        LocalDate wkniHoy = LocalDate.now();
        boolean wkniSeparadorProximas = false;
        for (wkniActividadClase wkniA : wkniActividades) {
            if (!wkniSeparadorProximas && wkniA.wkni_fecha() != null && !wkniA.wkni_fecha().isBefore(wkniHoy)) {
                wkniBox.getChildren().add(wkniUi.wkniOverline("PRÓXIMAS"));
                wkniSeparadorProximas = true;
            }
            wkniBox.getChildren().add(wkniFilaActividad(wkniA));
        }
        return wkniUi.wkniScroll(wkniBox);
    }

    private Node wkniFilaActividad(wkniActividadClase wkniA) {
        String wkni_tono = switch (wkniA.wkni_tipo()) {
            case "Evaluación" -> "rose"; case "Tarea" -> "blue"; case "Actividad" -> "violet"; case "Material" -> "amber"; default -> "teal";
        };
        Label wkni_fecha = wkniUi.wkniCaption(wkniA.wkni_fecha() == null ? "Sin fecha" : wkniUi.wkniFormatDate(wkniA.wkni_fecha()));
        HBox wkniTitulo = new HBox(8, wkniUi.wkniLabel(wkniA.wkniTitulo(), "h3"), wkniUi.wkniPill(wkniA.wkni_tipo(), wkni_tono));
        wkniTitulo.setAlignment(Pos.CENTER_LEFT);
        VBox wkniText = new VBox(4, wkniTitulo, wkni_fecha);
        if (wkniA.wkniDescripcion() != null && !wkniA.wkniDescripcion().isBlank()) { Label wkniD = wkniUi.wkniMuted(wkniA.wkniDescripcion()); wkniD.setWrapText(true); wkniText.getChildren().add(wkniD); }
        HBox.setHgrow(wkniText, Priority.ALWAYS);
        HBox wkniRow = new HBox(12, wkniUi.wkniIconChip(wkniIcono(wkniA.wkni_tipo()), wkni_tono, 40, 20), wkniText);
        if (wkniA.wkni_fecha() != null) {
            Button wkniCal = wkniUi.wkniSecondary("calendar", "Agendar");
            wkniCal.setTooltip(new Tooltip("Crear el evento en tu calendario con esta fecha"));
            wkniCal.setOnAction(wkniE -> wkniEventViews.wkniFormEnFechaHora(wkniStage, null, wkniA.wkni_fecha(), null, "Trabajo", wkniController, () -> {}));
            wkniRow.getChildren().add(wkniCal);
        }
        Button wkniRemove = wkniUi.wkniIconButton("delete", "Eliminar");
        wkniRemove.setOnAction(wkniE -> {
            if (wkniUi.wkniConfirm(wkniRoot, "¿Eliminar «" + wkniA.wkniTitulo() + "»?",
                wkniClaseDocenteDAO.wkniTIPOS_EVALUABLES.contains(wkniA.wkni_tipo()) ? "También se borrarán las entregas y notas registradas." : "Se quitará de la planeación.", "Eliminar")) {
                wkniDao.wkniEliminarActividad(wkniA.wkniId());
                wkniTPlaneacion.setContent(wkniPlaneacion());
                wkniCambio();
            }
        });
        wkniRow.getChildren().add(wkniRemove);
        wkniRow.setAlignment(Pos.CENTER_LEFT);
        wkniRow.getStyleClass().add("card");
        wkniRow.setPadding(new Insets(12, 14, 12, 14));
        return wkniRow;
    }

    private static String wkniIcono(String wkni_tipo) {
        return switch (wkni_tipo) { case "Evaluación" -> "tasks"; case "Tarea" -> "notes"; case "Material" -> "folder"; case "Actividad" -> "sparkle"; default -> "book"; };
    }

    private void wkniCrearActividad(String wkniTipoInicial) {
        Dialog<ButtonType> wkniDialog = new Dialog<>();
        wkniDialog.setTitle("Planeación");
        wkniDialog.setHeaderText("Nuevo elemento para " + wkniClase);
        wkniDialog.initOwner(wkniStage);
        ButtonType wkniSave = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        wkniDialog.getDialogPane().getButtonTypes().addAll(wkniSave, ButtonType.CANCEL);
        TextField wkniTitulo = new TextField(); wkniTitulo.setPromptText("Ej.: Práctica calificada 1");
        ComboBox<String> wkni_tipo = new ComboBox<>(FXCollections.observableArrayList("Clase", "Material", "Tarea", "Actividad", "Evaluación"));
        wkni_tipo.setValue(wkniTipoInicial);
        DatePicker wkni_fecha = wkniUi.wkniFechaCorta(new DatePicker(LocalDate.now()));
        TextArea wkniDesc = new TextArea(); wkniDesc.setPromptText("Indicaciones o recordatorio (opcional)"); wkniDesc.setPrefRowCount(3); wkniDesc.setWrapText(true);
        VBox wkniForm = new VBox(10, wkniUi.wkniField("Título", wkniTitulo), new HBox(10, wkniUi.wkniField("Tipo", wkni_tipo), wkniUi.wkniField("Fecha", wkni_fecha)), wkniUi.wkniField("Descripción", wkniDesc));
        wkniForm.setPadding(new Insets(8, 4, 4, 4));
        wkniForm.setPrefWidth(460);
        wkniDialog.getDialogPane().setContent(wkniForm);
        javafx.application.Platform.runLater(wkniTitulo::requestFocus);
        if (wkniDialog.showAndWait().filter(wkniB -> wkniB == wkniSave).isEmpty()) return;
        if (wkniTitulo.getText().isBlank()) { wkniDocenteUi.wkniInfo(wkniRoot, "Escribe el título."); return; }
        if (!wkniDao.wkniCrearActividad(wkniClase.wkniId(), wkniTitulo.getText(), wkni_tipo.getValue(), wkni_fecha.getValue(), wkniDesc.getText())) {
            wkniDocenteUi.wkniError(wkniRoot, "No se pudo guardar."); return;
        }
        wkniTPlaneacion.setContent(wkniPlaneacion());
        wkniTrabajos.wkniRecargar();
        wkniCambio();
    }
}

package com.agendapersonal.ui.modules.docente;

import com.agendapersonal.controller.WO_KI_NB_JI_MainController;
import com.agendapersonal.domain.model.WO_KI_NB_JI_ActividadClase;
import com.agendapersonal.domain.model.WO_KI_NB_JI_ClaseDocente;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstudianteClase;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_ClaseDocenteDAO;
import com.agendapersonal.service.WO_KI_NB_JI_ExcelClaseService;
import com.agendapersonal.service.WO_KI_NB_JI_ReportesDocente;
import com.agendapersonal.service.WO_KI_NB_JI_ResumenCurso;
import com.agendapersonal.service.WO_KI_NB_JI_RiesgoEstudianteService;
import com.agendapersonal.ui.WO_KI_NB_JI_EventViews;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
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
public final class WO_KI_NB_JI_CursoView {
    private final WO_KI_NB_JI_ClaseDocente clase;
    private final WO_KI_NB_JI_ClaseDocenteDAO dao;
    private final Stage stage;
    private final WO_KI_NB_JI_MainController controller;
    private final WO_KI_NB_JI_CursoContexto ctx;
    private final TabPane pestanas = new TabPane();
    private final VBox root = new VBox(10);
    private final WO_KI_NB_JI_PanelCurso panel;
    private final WO_KI_NB_JI_AsistenciaPanel asistencia;
    private final WO_KI_NB_JI_TrabajosPanel trabajos;
    private final Tab tPanel, tAsistencia, tTrabajos, tEstudiantes, tPlaneacion, tAula;

    public WO_KI_NB_JI_CursoView(WO_KI_NB_JI_ClaseDocente clase, WO_KI_NB_JI_ClaseDocenteDAO dao, Stage stage, WO_KI_NB_JI_MainController controller, int pestanaInicial,
                     IntConsumer alCambiarPestana, Runnable alCambiarDatos, Runnable editarCurso, Runnable eliminarCurso) {
        this.clase = clase;
        this.dao = dao;
        this.stage = stage;
        this.controller = controller;
        this.ctx = new WO_KI_NB_JI_CursoContexto(clase, dao, alCambiarDatos);

        panel = new WO_KI_NB_JI_PanelCurso(ctx, i -> pestanas.getSelectionModel().select(i));
        asistencia = new WO_KI_NB_JI_AsistenciaPanel(ctx);
        trabajos = new WO_KI_NB_JI_TrabajosPanel(ctx, () -> WO_KI_NB_JI_crearActividad("Tarea"));
        tPanel = WO_KI_NB_JI_tab("Panel", "chart", panel.WO_KI_NB_JI_getView());
        tAsistencia = WO_KI_NB_JI_tab("Asistencia", "user-check", asistencia.WO_KI_NB_JI_getView());
        tTrabajos = WO_KI_NB_JI_tab("Trabajos y notas", "tasks", trabajos.WO_KI_NB_JI_getView());
        tEstudiantes = WO_KI_NB_JI_tab("Estudiantes", "contacts", WO_KI_NB_JI_estudiantes());
        tPlaneacion = WO_KI_NB_JI_tab("Planeación", "calendar", WO_KI_NB_JI_planeacion());
        tAula = WO_KI_NB_JI_tab("Aula virtual", "video", new WO_KI_NB_JI_AulaVirtualPanel(ctx).WO_KI_NB_JI_getView());
        pestanas.getTabs().setAll(tPanel, tAsistencia, tTrabajos, tEstudiantes, tPlaneacion, tAula);
        pestanas.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        pestanas.getSelectionModel().select(Math.max(0, Math.min(pestanaInicial, pestanas.getTabs().size() - 1)));
        // Cada pestaña se pone al día al abrirla: así las gráficas reflejan lo último que se marcó.
        pestanas.getSelectionModel().selectedIndexProperty().addListener((o, a, b) -> {
            alCambiarPestana.accept(b.intValue());
            Tab t = pestanas.getTabs().get(b.intValue());
            if (t == tPanel) panel.WO_KI_NB_JI_recargar();
            else if (t == tAsistencia) asistencia.WO_KI_NB_JI_refrescar();
            else if (t == tTrabajos) trabajos.WO_KI_NB_JI_recargar();
            else if (t == tEstudiantes) t.setContent(WO_KI_NB_JI_estudiantes());
            else if (t == tPlaneacion) t.setContent(WO_KI_NB_JI_planeacion());
        });
        VBox.setVgrow(pestanas, Priority.ALWAYS);

        Label nombre = WO_KI_NB_JI_Ui.WO_KI_NB_JI_h2(clase.toString());
        HBox titulo = new HBox(10, nombre);
        titulo.setAlignment(Pos.CENTER_LEFT);
        if (clase.periodo() != null) titulo.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pill(clase.periodo(), "slate"));
        titulo.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pill("Aprueba con " + WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(clase.notaMinima()), "blue"));
        Button editar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_ghost("edit", "Editar curso");
        editar.setOnAction(e -> editarCurso.run());
        Button eliminar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_ghost("delete", "Eliminar");
        eliminar.getStyleClass().add("text-danger-btn");
        eliminar.setOnAction(e -> eliminarCurso.run());
        HBox cabecera = new HBox(10, titulo, WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), editar, eliminar);
        cabecera.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().addAll(cabecera, pestanas);
    }

    public Node WO_KI_NB_JI_getView() { return root; }

    /** Cifra compacta (ícono, valor y etiqueta en una línea) para el encabezado del Espacio Docente. */
    public static HBox WO_KI_NB_JI_cifra(String icono, String tono, String valor, String etiqueta) {
        Label v = new Label(valor);
        v.getStyleClass().add("cifra-valor");
        Label l = new Label(etiqueta);
        l.getStyleClass().add("cifra-etiqueta");
        l.setWrapText(true);
        HBox h = new HBox(12, WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconChip(icono, tono, 38, 19), new VBox(0, v, l));
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().addAll("card", "cifra");
        HBox.setHgrow(h, Priority.ALWAYS);
        h.setMaxWidth(Double.MAX_VALUE);
        return h;
    }

    /** Fila del semáforo reutilizada por la pestaña global de alertas. */
    public static HBox WO_KI_NB_JI_filaAlerta(String nombre, WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Evaluacion ev, String curso, Runnable abrir) {
        return WO_KI_NB_JI_PanelCurso.WO_KI_NB_JI_filaAlerta(nombre, ev, curso, abrir);
    }

    private Tab WO_KI_NB_JI_tab(String title, String icon, Node content) {
        Tab tab = new Tab(title, content);
        tab.setGraphic(com.agendapersonal.ui.WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon(icon, 16));
        return tab;
    }

    private void WO_KI_NB_JI_cambio() { ctx.WO_KI_NB_JI_cambio(); }

    // ------------------------------------------------------------------ estudiantes

    private Node WO_KI_NB_JI_estudiantes() {
        VBox box = WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_panel();
        WO_KI_NB_JI_ResumenCurso r = ctx.WO_KI_NB_JI_resumen();
        FilteredList<WO_KI_NB_JI_EstudianteClase> filtrados = new FilteredList<>(FXCollections.observableArrayList(r.WO_KI_NB_JI_estudiantes()));
        TableView<WO_KI_NB_JI_EstudianteClase> table = new TableView<>(filtrados);
        table.getStyleClass().add("asist-tabla");
        table.setFixedCellSize(46);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        table.setPlaceholder(WO_KI_NB_JI_Ui.WO_KI_NB_JI_empty("contacts", "Aún no hay estudiantes",
            "Añádelos uno por uno o importa una lista de Excel con la columna «Estudiante» o «Nombre»."));

        TableColumn<WO_KI_NB_JI_EstudianteClase, WO_KI_NB_JI_EstudianteClase> nombre = new TableColumn<>("Estudiante");
        nombre.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        nombre.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(WO_KI_NB_JI_EstudianteClase e, boolean empty) {
                super.updateItem(e, empty);
                setText(null);
                setGraphic(empty || e == null ? null : WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_nombre(e.nombre(), r.WO_KI_NB_JI_evaluacion(e.id())));
            }
        });
        nombre.setPrefWidth(280);
        table.getColumns().add(nombre);
        table.getColumns().add(WO_KI_NB_JI_texto("Correo", e -> e.correo() == null ? "" : e.correo(), 200));
        table.getColumns().add(WO_KI_NB_JI_texto("Nota del curso", e -> e.calificacion() == null ? "—" : WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(e.calificacion()), 110));
        table.getColumns().add(WO_KI_NB_JI_texto("Asistencia", e -> WO_KI_NB_JI_ReportesDocente.WO_KI_NB_JI_pct(r.WO_KI_NB_JI_evaluacion(e.id()).porcentajeAsistencia()), 100));
        TableColumn<WO_KI_NB_JI_EstudianteClase, WO_KI_NB_JI_EstudianteClase> estado = new TableColumn<>("Situación");
        estado.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        estado.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(WO_KI_NB_JI_EstudianteClase e, boolean empty) {
                super.updateItem(e, empty);
                setText(null);
                if (empty || e == null) { setGraphic(null); return; }
                WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Evaluacion ev = r.WO_KI_NB_JI_evaluacion(e.id());
                Label p = WO_KI_NB_JI_Ui.WO_KI_NB_JI_pill(ev.nivel().WO_KI_NB_JI_etiqueta(), WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_tono(ev.nivel()));
                if (!ev.motivos().isEmpty()) p.setTooltip(new Tooltip(String.join("\n", ev.motivos())));
                setGraphic(p);
            }
        });
        estado.setPrefWidth(120);
        table.getColumns().add(estado);
        table.getColumns().add(WO_KI_NB_JI_texto("Observaciones", e -> e.observaciones() == null ? "" : e.observaciones(), 200));
        table.setRowFactory(tv -> {
            TableRow<WO_KI_NB_JI_EstudianteClase> row = new TableRow<>();
            row.setOnMouseClicked(m -> {
                if (m.getButton() == MouseButton.PRIMARY && m.getClickCount() == 2 && !row.isEmpty()) WO_KI_NB_JI_editarEstudiante(row.getItem());
            });
            MenuItem verReporte = new MenuItem("Ver reporte para el apoderado");
            verReporte.setOnAction(e -> WO_KI_NB_JI_ReporteEstudianteView.WO_KI_NB_JI_mostrar(root, ctx, row.getItem()));
            MenuItem editarItem = new MenuItem("Editar");
            editarItem.setOnAction(e -> WO_KI_NB_JI_editarEstudiante(row.getItem()));
            ContextMenu menu = new ContextMenu(verReporte, editarItem);
            row.contextMenuProperty().bind(javafx.beans.binding.Bindings.when(row.emptyProperty())
                .then((ContextMenu) null).otherwise(menu));
            return row;
        });
        VBox.setVgrow(table, Priority.ALWAYS);

        TextField buscar = new TextField();
        HBox busqueda = WO_KI_NB_JI_Ui.WO_KI_NB_JI_searchBox(buscar, "Buscar estudiante…");
        busqueda.setMaxWidth(260);
        buscar.textProperty().addListener((o, a, b) -> {
            String q = b == null ? "" : b.trim().toLowerCase(Locale.ROOT);
            filtrados.setPredicate(e -> q.isEmpty() || e.nombre().toLowerCase(Locale.ROOT).contains(q)
                || (e.correo() != null && e.correo().toLowerCase(Locale.ROOT).contains(q)));
        });

        Button add = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("add", "Añadir estudiante");
        add.setOnAction(e -> WO_KI_NB_JI_editarEstudiante(null));
        Button edit = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("edit", "Editar");
        edit.setOnAction(e -> {
            WO_KI_NB_JI_EstudianteClase sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_info(root, "Selecciona un estudiante (o haz doble clic sobre su fila)."); else WO_KI_NB_JI_editarEstudiante(sel);
        });
        Button delete = WO_KI_NB_JI_Ui.WO_KI_NB_JI_ghost("delete", "Eliminar");
        delete.setOnAction(e -> {
            WO_KI_NB_JI_EstudianteClase sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_info(root, "Selecciona un estudiante primero.");
            else if (WO_KI_NB_JI_Ui.WO_KI_NB_JI_confirm(root, "¿Eliminar a " + sel.nombre() + "?", "También se borrarán su asistencia y sus entregas en este curso.", "Eliminar")) {
                dao.WO_KI_NB_JI_eliminarEstudiante(sel.id()); WO_KI_NB_JI_refrescarEstudiantes();
            }
        });
        Button reporte = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("print", "Reporte individual");
        reporte.setTooltip(new Tooltip("Hoja para el apoderado o tutor: asistencia, trabajos, notas, alertas y firmas"));
        reporte.setOnAction(e -> WO_KI_NB_JI_ReporteEstudianteView.WO_KI_NB_JI_mostrar(root, ctx, table.getSelectionModel().getSelectedItem()));
        Button reportes = WO_KI_NB_JI_Ui.WO_KI_NB_JI_ghost("copy", "Reportes de todo el curso");
        reportes.setTooltip(new Tooltip("Imprime una hoja por estudiante (reunión de padres, entrega de libretas)"));
        reportes.setOnAction(e -> WO_KI_NB_JI_ReporteEstudianteView.WO_KI_NB_JI_imprimirCurso(root, ctx));
        Button importar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("upload", "Importar Excel");
        importar.setOnAction(e -> WO_KI_NB_JI_importarExcel());
        Button exportar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("download", "Exportar Excel");
        exportar.setOnAction(e -> WO_KI_NB_JI_exportarExcel());
        HBox actions = new HBox(8, add, edit, delete, WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), busqueda, importar, exportar);
        actions.setAlignment(Pos.CENTER_LEFT);
        HBox actions2 = new HBox(8, reporte, reportes);
        actions2.setAlignment(Pos.CENTER_LEFT);
        Label ayuda = new Label(r.WO_KI_NB_JI_estudiantes().size() + " estudiantes · doble clic para editar · clic derecho para ver su reporte · "
            + "al reimportar un Excel se actualiza por nombre, sin duplicar.");
        ayuda.getStyleClass().add("atajos");
        actions2.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), ayuda);
        box.getChildren().addAll(actions, actions2, table);
        return box;
    }

    private TableColumn<WO_KI_NB_JI_EstudianteClase, String> WO_KI_NB_JI_texto(String titulo, java.util.function.Function<WO_KI_NB_JI_EstudianteClase, String> f, double ancho) {
        TableColumn<WO_KI_NB_JI_EstudianteClase, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(f.apply(d.getValue())));
        c.setPrefWidth(ancho);
        return c;
    }

    private void WO_KI_NB_JI_refrescarEstudiantes() {
        tEstudiantes.setContent(WO_KI_NB_JI_estudiantes());
        WO_KI_NB_JI_cambio();
    }

    private void WO_KI_NB_JI_editarEstudiante(WO_KI_NB_JI_EstudianteClase existente) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existente == null ? "Añadir estudiante" : "Editar estudiante");
        dialog.setHeaderText(existente == null ? "Nuevo estudiante en " + clase : existente.nombre());
        dialog.initOwner(stage);
        ButtonType save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        ButtonType otro = new ButtonType("Guardar y añadir otro", ButtonBar.ButtonData.OTHER);
        if (existente == null) dialog.getDialogPane().getButtonTypes().addAll(otro, save, ButtonType.CANCEL);
        else dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        TextField nombre = new TextField(); nombre.setPromptText("Nombres y apellidos");
        TextField correo = new TextField(); correo.setPromptText("correo@ejemplo.com (opcional)");
        TextField nota = new TextField(); nota.setPromptText("Opcional");
        TextArea obs = new TextArea(); obs.setPromptText("Observaciones (opcional)"); obs.setPrefRowCount(3); obs.setWrapText(true);
        if (existente != null) {
            nombre.setText(existente.nombre());
            correo.setText(existente.correo() == null ? "" : existente.correo());
            nota.setText(existente.calificacion() == null ? "" : WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(existente.calificacion()));
            obs.setText(existente.observaciones() == null ? "" : existente.observaciones());
        }
        VBox form = new VBox(10, WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Nombre completo", nombre), new HBox(10, WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Correo", correo), WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Nota del curso", nota)),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Observaciones", obs));
        form.setPadding(new Insets(8, 4, 4, 4));
        form.setPrefWidth(460);
        dialog.getDialogPane().setContent(form);
        javafx.application.Platform.runLater(nombre::requestFocus);
        var res = dialog.showAndWait();
        if (res.isEmpty() || (res.get() != save && res.get() != otro)) return;
        if (nombre.getText().isBlank()) { WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_info(root, "El nombre es obligatorio."); return; }
        Double grade;
        try { grade = WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_nota(nota.getText()); }
        catch (NumberFormatException ex) { WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_info(root, "La nota debe ser un número (por ejemplo 15 o 15.5)."); return; }
        if (existente != null && !existente.nombre().equalsIgnoreCase(nombre.getText().trim())) {
            // Cambio de nombre: se borra el registro viejo solo si el nuevo nombre no choca con otro estudiante.
            boolean choca = dao.WO_KI_NB_JI_listarEstudiantes(clase.id()).stream()
                .anyMatch(s -> s.id() != existente.id() && s.nombre().equalsIgnoreCase(nombre.getText().trim()));
            if (choca) { WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_info(root, "Ya existe un estudiante con ese nombre en el curso."); return; }
            if (!WO_KI_NB_JI_renombrar(existente.id(), nombre.getText().trim())) { WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_error(root, "No se pudo cambiar el nombre."); return; }
        }
        if (!dao.WO_KI_NB_JI_guardarEstudiante(clase.id(), nombre.getText(), correo.getText(), grade, obs.getText())) {
            WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_error(root, "No se pudo guardar el estudiante."); return;
        }
        WO_KI_NB_JI_refrescarEstudiantes();
        if (res.get() == otro) WO_KI_NB_JI_editarEstudiante(null);
    }

    /** Renombra conservando el id (y con él su asistencia y entregas). */
    private boolean WO_KI_NB_JI_renombrar(int id, String nuevo) {
        try (var c = com.agendapersonal.database.WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             var p = c.prepareStatement("UPDATE estudiante_clase SET nombre=? WHERE id=?")) {
            p.setString(1, nuevo); p.setInt(2, id);
            return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("renombrar estudiante", e); return false; }
    }

    private void WO_KI_NB_JI_importarExcel() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Importar lista de estudiantes");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Libro de Excel (*.xlsx)", "*.xlsx"));
        File file = chooser.showOpenDialog(WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_ventana(root));
        if (file == null) return;
        try {
            WO_KI_NB_JI_ExcelClaseService.WO_KI_NB_JI_ResultadoImportacion resultado = WO_KI_NB_JI_ExcelClaseService.WO_KI_NB_JI_importar(file.toPath(), clase.id());
            int guardados = 0;
            for (WO_KI_NB_JI_EstudianteClase e : resultado.estudiantes())
                if (dao.WO_KI_NB_JI_guardarEstudiante(clase.id(), e.nombre(), e.correo(), e.calificacion(), e.observaciones())) guardados++;
            WO_KI_NB_JI_refrescarEstudiantes();
            WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_info(root, "Importación completada: " + guardados + " estudiantes guardados."
                + (resultado.filasOmitidas() == 0 ? "" : " " + resultado.filasOmitidas() + " filas con nota inválida se omitieron."));
        } catch (Exception ex) { WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_error(root, "No se pudo importar el libro: " + ex.getMessage()); }
    }

    private void WO_KI_NB_JI_exportarExcel() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar estudiantes y notas");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Libro de Excel (*.xlsx)", "*.xlsx"));
        chooser.setInitialFileName(WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_archivo("Estudiantes " + clase) + ".xlsx");
        File file = chooser.showSaveDialog(WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_ventana(root));
        if (file == null) return;
        File destino = file.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx") ? file : new File(file.getParentFile(), file.getName() + ".xlsx");
        try {
            WO_KI_NB_JI_ExcelClaseService.WO_KI_NB_JI_exportar(destino.toPath(), clase, dao.WO_KI_NB_JI_listarEstudiantes(clase.id()));
            WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_info(root, "Lista exportada a:\n" + destino.getAbsolutePath() + "\n\nPuedes editarla y volver a importarla.");
        } catch (Exception ex) { WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_error(root, "No se pudo exportar el libro: " + ex.getMessage()); }
    }

    // ------------------------------------------------------------------ planeación

    private Node WO_KI_NB_JI_planeacion() {
        VBox box = WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_panel();
        HBox acciones = new HBox(8);
        for (String tipo : List.of("Clase", "Material", "Tarea", "Evaluación")) {
            Button b = tipo.equals("Clase") ? WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("add", "Clase") : WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("add", tipo);
            b.setOnAction(e -> WO_KI_NB_JI_crearActividad(tipo));
            acciones.getChildren().add(b);
        }
        Label ayuda = WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Las tareas, actividades y evaluaciones aparecen como columnas en «Trabajos y notas» para registrar entregas.");
        ayuda.setWrapText(true);
        box.getChildren().addAll(acciones, ayuda);
        List<WO_KI_NB_JI_ActividadClase> actividades = dao.WO_KI_NB_JI_listarActividades(clase.id());
        if (actividades.isEmpty()) box.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_empty("calendar", "Planeación vacía",
            "Añade tus sesiones, materiales, tareas y evaluaciones con su fecha."));
        LocalDate hoy = LocalDate.now();
        boolean separadorProximas = false;
        for (WO_KI_NB_JI_ActividadClase a : actividades) {
            if (!separadorProximas && a.fecha() != null && !a.fecha().isBefore(hoy)) {
                box.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_overline("PRÓXIMAS"));
                separadorProximas = true;
            }
            box.getChildren().add(WO_KI_NB_JI_filaActividad(a));
        }
        return WO_KI_NB_JI_Ui.WO_KI_NB_JI_scroll(box);
    }

    private Node WO_KI_NB_JI_filaActividad(WO_KI_NB_JI_ActividadClase a) {
        String tono = switch (a.tipo()) {
            case "Evaluación" -> "rose"; case "Tarea" -> "blue"; case "Actividad" -> "violet"; case "Material" -> "amber"; default -> "teal";
        };
        Label fecha = WO_KI_NB_JI_Ui.WO_KI_NB_JI_caption(a.fecha() == null ? "Sin fecha" : WO_KI_NB_JI_Ui.WO_KI_NB_JI_formatDate(a.fecha()));
        HBox titulo = new HBox(8, WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(a.titulo(), "h3"), WO_KI_NB_JI_Ui.WO_KI_NB_JI_pill(a.tipo(), tono));
        titulo.setAlignment(Pos.CENTER_LEFT);
        VBox text = new VBox(4, titulo, fecha);
        if (a.descripcion() != null && !a.descripcion().isBlank()) { Label d = WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted(a.descripcion()); d.setWrapText(true); text.getChildren().add(d); }
        HBox.setHgrow(text, Priority.ALWAYS);
        HBox row = new HBox(12, WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconChip(WO_KI_NB_JI_icono(a.tipo()), tono, 40, 20), text);
        if (a.fecha() != null) {
            Button cal = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("calendar", "Agendar");
            cal.setTooltip(new Tooltip("Crear el evento en tu calendario con esta fecha"));
            cal.setOnAction(e -> WO_KI_NB_JI_EventViews.WO_KI_NB_JI_formEnFechaHora(stage, null, a.fecha(), null, "Trabajo", controller, () -> {}));
            row.getChildren().add(cal);
        }
        Button remove = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("delete", "Eliminar");
        remove.setOnAction(e -> {
            if (WO_KI_NB_JI_Ui.WO_KI_NB_JI_confirm(root, "¿Eliminar «" + a.titulo() + "»?",
                WO_KI_NB_JI_ClaseDocenteDAO.TIPOS_EVALUABLES.contains(a.tipo()) ? "También se borrarán las entregas y notas registradas." : "Se quitará de la planeación.", "Eliminar")) {
                dao.WO_KI_NB_JI_eliminarActividad(a.id());
                tPlaneacion.setContent(WO_KI_NB_JI_planeacion());
                WO_KI_NB_JI_cambio();
            }
        });
        row.getChildren().add(remove);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().add("card");
        row.setPadding(new Insets(12, 14, 12, 14));
        return row;
    }

    private static String WO_KI_NB_JI_icono(String tipo) {
        return switch (tipo) { case "Evaluación" -> "tasks"; case "Tarea" -> "notes"; case "Material" -> "folder"; case "Actividad" -> "sparkle"; default -> "book"; };
    }

    private void WO_KI_NB_JI_crearActividad(String tipoInicial) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Planeación");
        dialog.setHeaderText("Nuevo elemento para " + clase);
        dialog.initOwner(stage);
        ButtonType save = new ButtonType("Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        TextField titulo = new TextField(); titulo.setPromptText("Ej.: Práctica calificada 1");
        ComboBox<String> tipo = new ComboBox<>(FXCollections.observableArrayList("Clase", "Material", "Tarea", "Actividad", "Evaluación"));
        tipo.setValue(tipoInicial);
        DatePicker fecha = WO_KI_NB_JI_Ui.WO_KI_NB_JI_fechaCorta(new DatePicker(LocalDate.now()));
        TextArea desc = new TextArea(); desc.setPromptText("Indicaciones o recordatorio (opcional)"); desc.setPrefRowCount(3); desc.setWrapText(true);
        VBox form = new VBox(10, WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Título", titulo), new HBox(10, WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Tipo", tipo), WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Fecha", fecha)), WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Descripción", desc));
        form.setPadding(new Insets(8, 4, 4, 4));
        form.setPrefWidth(460);
        dialog.getDialogPane().setContent(form);
        javafx.application.Platform.runLater(titulo::requestFocus);
        if (dialog.showAndWait().filter(b -> b == save).isEmpty()) return;
        if (titulo.getText().isBlank()) { WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_info(root, "Escribe el título."); return; }
        if (!dao.WO_KI_NB_JI_crearActividad(clase.id(), titulo.getText(), tipo.getValue(), fecha.getValue(), desc.getText())) {
            WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_error(root, "No se pudo guardar."); return;
        }
        tPlaneacion.setContent(WO_KI_NB_JI_planeacion());
        trabajos.WO_KI_NB_JI_recargar();
        WO_KI_NB_JI_cambio();
    }
}

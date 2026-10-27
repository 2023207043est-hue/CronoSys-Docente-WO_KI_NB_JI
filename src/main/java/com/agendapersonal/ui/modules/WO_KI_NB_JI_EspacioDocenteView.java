package com.agendapersonal.ui.modules;

import com.agendapersonal.controller.WO_KI_NB_JI_MainController;
import com.agendapersonal.domain.model.WO_KI_NB_JI_ClaseDocente;
import com.agendapersonal.model.WO_KI_NB_JI_Evento;
import com.agendapersonal.model.WO_KI_NB_JI_Tarea;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_ClaseDocenteDAO;
import com.agendapersonal.service.WO_KI_NB_JI_DatosPruebaDocente;
import com.agendapersonal.service.WO_KI_NB_JI_MetodoEnfoque;
import com.agendapersonal.service.WO_KI_NB_JI_ReportesDocente;
import com.agendapersonal.service.WO_KI_NB_JI_ResumenCurso;
import com.agendapersonal.service.WO_KI_NB_JI_RiesgoEstudianteService;
import com.agendapersonal.ui.WO_KI_NB_JI_TaskViews;
import com.agendapersonal.ui.WO_KI_NB_JI_ThemeService;
import com.agendapersonal.ui.modules.docente.WO_KI_NB_JI_CursoView;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Espacio Docente: cursos con asistencia visual, trabajos y notas, panel con gráficas, semáforo de riesgo y
 * aula virtual; más pendientes de evaluación, tutorías y bloques de enfoque conectados a la agenda local.
 */
public final class WO_KI_NB_JI_EspacioDocenteView {
    private final WO_KI_NB_JI_MainController controller;
    private final Stage stage;
    private final VBox root = WO_KI_NB_JI_Ui.WO_KI_NB_JI_page();
    private final ScrollPane scroll = WO_KI_NB_JI_Ui.WO_KI_NB_JI_scroll(root);
    private final TabPane tabs = new TabPane();
    private final Runnable abrirCalendario;
    private final WO_KI_NB_JI_ClaseDocenteDAO clasesDao = new WO_KI_NB_JI_ClaseDocenteDAO();
    private final HBox cifras = new HBox(14);
    private final Button datosPrueba = WO_KI_NB_JI_Ui.WO_KI_NB_JI_ghost("sparkle", "");
    private final Tab cursosTab;
    private final Tab alertasTab;
    private final Tab pendientesTab;
    private final StackPane areaCurso = new StackPane();
    private final FlowPane selector = new FlowPane(10, 10);
    private final ToggleGroup grupoCursos = new ToggleGroup();
    private int cursoActual = -1;
    private int pestanaCurso = 0;
    private WO_KI_NB_JI_CursoView cursoView;
    private boolean soloRiesgo = true;

    public WO_KI_NB_JI_EspacioDocenteView(WO_KI_NB_JI_MainController controller, Stage stage, Runnable abrirBloques, Runnable abrirCalendario) {
        this.controller = controller;
        this.stage = stage;
        this.abrirCalendario = abrirCalendario;
        Button planificar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("clock", "Time Blocking");
        planificar.setOnAction(e -> abrirBloques.run());
        datosPrueba.setOnAction(e -> WO_KI_NB_JI_alternarDatosPrueba());
        WO_KI_NB_JI_actualizarBotonPrueba();
        root.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pageHeader("school", "Espacio Docente",
            "Toma asistencia con un clic, registra trabajos y notas, detecta a tiempo a quien se está quedando atrás y organiza tu aula virtual.",
            datosPrueba, planificar));

        root.getChildren().add(cifras);

        cursosTab = WO_KI_NB_JI_tab("Mis cursos", "book", WO_KI_NB_JI_panelCursos());
        alertasTab = WO_KI_NB_JI_tab("Alertas", "warning", new VBox());
        pendientesTab = WO_KI_NB_JI_tab("Pendientes", "tasks", WO_KI_NB_JI_panelEvaluacion());
        tabs.getTabs().setAll(cursosTab, alertasTab, pendientesTab,
            WO_KI_NB_JI_tab("Tutorías", "contacts", WO_KI_NB_JI_panelTutorias()), WO_KI_NB_JI_tab("Enfoque", "clock", WO_KI_NB_JI_panelEnfoque()));
        tabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        tabs.getStyleClass().add("teacher-tabs");
        tabs.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b == alertasTab) alertasTab.setContent(WO_KI_NB_JI_panelAlertas());
            if (b == pendientesTab) pendientesTab.setContent(WO_KI_NB_JI_panelEvaluacion());
        });
        VBox.setVgrow(tabs, Priority.ALWAYS);
        tabs.setMinHeight(620);   // en ventanas bajas la página se desplaza en lugar de aplastar las tablas
        root.getChildren().add(tabs);
        root.setSpacing(16);
        // La página llena la ventana; si no cabe, se desplaza.
        root.minHeightProperty().bind(scroll.heightProperty().subtract(2));
        WO_KI_NB_JI_actualizarCifras();
    }

    public Node WO_KI_NB_JI_getView() { return scroll; }

    private Tab WO_KI_NB_JI_tab(String title, String icon, Node content) {
        Tab tab = new Tab(title, content);
        tab.setGraphic(com.agendapersonal.ui.WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon(icon, 16));
        return tab;
    }

    // ------------------------------------------------------------------ cifras generales

    /** Cifras del encabezado y contador de la pestaña Alertas; se llama cada vez que cambia un dato. */
    private void WO_KI_NB_JI_actualizarCifras() {
        List<WO_KI_NB_JI_ClaseDocente> clases = clasesDao.WO_KI_NB_JI_listarClases();
        int estudiantes = 0, marcadosHoy = 0;
        long riesgo = 0, atencion = 0;
        LocalDate hoy = LocalDate.now();
        for (WO_KI_NB_JI_ClaseDocente c : clases) {
            WO_KI_NB_JI_ResumenCurso r = new WO_KI_NB_JI_ResumenCurso(c, clasesDao, hoy);
            estudiantes += r.WO_KI_NB_JI_estudiantes().size();
            marcadosHoy += (int) r.WO_KI_NB_JI_estudiantes().stream().filter(e -> r.WO_KI_NB_JI_historial(e.id()).containsKey(hoy)).count();
            riesgo += r.WO_KI_NB_JI_contar(WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel.ALTO);
            atencion += r.WO_KI_NB_JI_contar(WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel.ATENCION);
        }
        long pendientes = controller.WO_KI_NB_JI_getTareas().stream().filter(t -> !t.WO_KI_NB_JI_estaCompletada()).count();
        cifras.getChildren().setAll(
            com.agendapersonal.ui.modules.docente.WO_KI_NB_JI_CursoView.WO_KI_NB_JI_cifra("book", "teal", Integer.toString(clases.size()),
                (clases.size() == 1 ? "curso · " : "cursos · ") + estudiantes + " estudiantes"),
            com.agendapersonal.ui.modules.docente.WO_KI_NB_JI_CursoView.WO_KI_NB_JI_cifra("user-check", "green", marcadosHoy + " / " + estudiantes,
                marcadosHoy >= estudiantes && estudiantes > 0 ? "asistencia de hoy: ¡todo registrado!" : "con asistencia registrada hoy"),
            com.agendapersonal.ui.modules.docente.WO_KI_NB_JI_CursoView.WO_KI_NB_JI_cifra("flag", riesgo > 0 ? "rose" : "green", Long.toString(riesgo),
                "en riesgo · " + atencion + " requieren atención"),
            com.agendapersonal.ui.modules.docente.WO_KI_NB_JI_CursoView.WO_KI_NB_JI_cifra("tasks", "blue", Long.toString(pendientes), "pendientes de revisión"));
        alertasTab.setText(riesgo > 0 ? "Alertas (" + riesgo + ")" : "Alertas");
    }

    private void WO_KI_NB_JI_actualizarBotonPrueba() {
        boolean hay = WO_KI_NB_JI_DatosPruebaDocente.WO_KI_NB_JI_hayDatosPrueba(clasesDao);
        datosPrueba.setText(hay ? "Quitar datos de prueba" : "Cargar datos de prueba");
        datosPrueba.setTooltip(new Tooltip(hay ? "Borra solo los cursos marcados «(prueba)»; tus cursos reales no se tocan."
            : "Crea 2 cursos de ejemplo con estudiantes, asistencia, trabajos, notas y aula virtual para probar todo."));
    }

    private void WO_KI_NB_JI_alternarDatosPrueba() {
        if (WO_KI_NB_JI_DatosPruebaDocente.WO_KI_NB_JI_hayDatosPrueba(clasesDao)) {
            if (!WO_KI_NB_JI_Ui.WO_KI_NB_JI_confirm(root, "¿Quitar los datos de prueba?", "Se eliminan los cursos de prueba con sus estudiantes, "
                + "asistencia y notas. Tus cursos reales no se tocan.", "Quitar")) return;
            WO_KI_NB_JI_DatosPruebaDocente.WO_KI_NB_JI_eliminar(clasesDao);
            cursoActual = -1;
        } else {
            int n = WO_KI_NB_JI_DatosPruebaDocente.WO_KI_NB_JI_crear(clasesDao, LocalDate.now());
            cursoActual = clasesDao.WO_KI_NB_JI_listarClases().stream().filter(WO_KI_NB_JI_DatosPruebaDocente::WO_KI_NB_JI_esPrueba).mapToInt(WO_KI_NB_JI_ClaseDocente::id).findFirst().orElse(-1);
            pestanaCurso = 0;
            com.agendapersonal.ui.WO_KI_NB_JI_Toast.WO_KI_NB_JI_mostrar(root, n + " cursos de prueba creados con estudiantes, asistencia y notas.");
        }
        WO_KI_NB_JI_actualizarBotonPrueba();
        WO_KI_NB_JI_refrescarCursos();
        tabs.getSelectionModel().select(cursosTab);
    }

    private void WO_KI_NB_JI_alCambiarDatos() {
        WO_KI_NB_JI_actualizarCifras();
        if (tabs.getSelectionModel().getSelectedItem() == alertasTab) alertasTab.setContent(WO_KI_NB_JI_panelAlertas());
        WO_KI_NB_JI_refrescarSelector();
    }

    // ------------------------------------------------------------------ cursos

    private VBox WO_KI_NB_JI_panelCursos() {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(16, 4, 4, 4));
        selector.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(areaCurso, Priority.ALWAYS);
        panel.getChildren().addAll(selector, areaCurso);
        WO_KI_NB_JI_refrescarCursos();
        return panel;
    }

    /** Reconstruye selector y curso abierto conservando el curso y la pestaña elegidos. */
    private void WO_KI_NB_JI_refrescarCursos() {
        List<WO_KI_NB_JI_ClaseDocente> clases = clasesDao.WO_KI_NB_JI_listarClases();
        if (clases.stream().noneMatch(c -> c.id() == cursoActual)) cursoActual = clases.isEmpty() ? -1 : clases.getFirst().id();
        WO_KI_NB_JI_refrescarSelector();
        if (cursoActual < 0) {
            cursoView = null;
            Button crear = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("add", "Crear mi primer curso");
            crear.setOnAction(e -> WO_KI_NB_JI_dialogoCurso(null));
            Button demo = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("sparkle", "Probar con datos de ejemplo");
            demo.setOnAction(e -> WO_KI_NB_JI_alternarDatosPrueba());
            VBox vacio = WO_KI_NB_JI_Ui.WO_KI_NB_JI_empty("school", "Empieza creando tu primer curso",
                "Luego agrega a tus estudiantes (o impórtalos de Excel) y toma asistencia con un clic. "
                + "Si solo quieres probar, carga los datos de ejemplo.");
            HBox botones = new HBox(10, crear, demo);
            botones.setAlignment(Pos.CENTER);
            vacio.getChildren().add(botones);
            areaCurso.getChildren().setAll(vacio);
            return;
        }
        WO_KI_NB_JI_ClaseDocente clase = clases.stream().filter(c -> c.id() == cursoActual).findFirst().orElseThrow();
        cursoView = new WO_KI_NB_JI_CursoView(clase, clasesDao, stage, controller, pestanaCurso, i -> pestanaCurso = i,
            this::WO_KI_NB_JI_alCambiarDatos, () -> WO_KI_NB_JI_dialogoCurso(clase), () -> WO_KI_NB_JI_eliminarCurso(clase));
        areaCurso.getChildren().setAll(cursoView.WO_KI_NB_JI_getView());
    }

    private void WO_KI_NB_JI_refrescarSelector() {
        List<WO_KI_NB_JI_ClaseDocente> clases = clasesDao.WO_KI_NB_JI_listarClases();
        List<Node> chips = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        for (WO_KI_NB_JI_ClaseDocente c : clases) {
            WO_KI_NB_JI_ResumenCurso r = new WO_KI_NB_JI_ResumenCurso(c, clasesDao, hoy);
            long riesgo = r.WO_KI_NB_JI_contar(WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel.ALTO);
            Label t = new Label(c.toString());
            t.getStyleClass().add("curso-chip-titulo");
            String sub = r.WO_KI_NB_JI_estudiantes().size() + " estudiantes · asist. " + WO_KI_NB_JI_ReportesDocente.WO_KI_NB_JI_pct(r.WO_KI_NB_JI_asistenciaPromedio());
            Label s = new Label(sub);
            s.getStyleClass().add("curso-chip-sub");
            HBox titulo = new HBox(6, t);
            titulo.setAlignment(Pos.CENTER_LEFT);
            if (riesgo > 0) titulo.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pill("⚠ " + riesgo, "danger"));
            if (WO_KI_NB_JI_DatosPruebaDocente.WO_KI_NB_JI_esPrueba(c)) titulo.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pill("prueba", "slate"));
            ToggleButton b = new ToggleButton();
            b.setGraphic(new VBox(3, titulo, s));
            b.getStyleClass().add("curso-chip");
            b.setToggleGroup(grupoCursos);
            b.setSelected(c.id() == cursoActual);
            b.setTooltip(new Tooltip(c.nombre() + (c.periodo() == null ? "" : " · " + c.periodo())));
            b.setOnAction(e -> {
                if (!b.isSelected()) { b.setSelected(true); return; }
                if (cursoActual != c.id()) { cursoActual = c.id(); WO_KI_NB_JI_refrescarCursos(); }
            });
            chips.add(b);
        }
        Button nuevo = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("add", "Nuevo curso");
        nuevo.setOnAction(e -> WO_KI_NB_JI_dialogoCurso(null));
        chips.add(nuevo);
        selector.getChildren().setAll(chips);
    }

    /** Crear (existente = null) o editar un curso, con su nota mínima aprobatoria. */
    private void WO_KI_NB_JI_dialogoCurso(WO_KI_NB_JI_ClaseDocente existente) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(existente == null ? "Nuevo curso" : "Editar curso");
        dialog.setHeaderText(existente == null ? "Agrega un curso al Espacio Docente" : "Datos de " + existente.nombre());
        dialog.initOwner(stage);
        ButtonType save = new ButtonType(existente == null ? "Crear curso" : "Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);
        TextField nombre = WO_KI_NB_JI_campo("Ej.: Matemática I");
        TextField grupo = WO_KI_NB_JI_campo("Ej.: A, 3.° B, Turno mañana");
        TextField periodo = WO_KI_NB_JI_campo("Ej.: 2026-II");
        TextField minima = WO_KI_NB_JI_campo("11");
        minima.setText(WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(existente == null ? WO_KI_NB_JI_ClaseDocente.NOTA_MINIMA_DEFECTO : existente.notaMinima()));
        if (existente != null) {
            nombre.setText(existente.nombre());
            grupo.setText(existente.grupo() == null ? "" : existente.grupo());
            periodo.setText(existente.periodo() == null ? "" : existente.periodo());
        }
        Label ayuda = WO_KI_NB_JI_Ui.WO_KI_NB_JI_caption("La nota mínima se usa para las alertas (escala vigesimal: 11 aprueba; en 0–100 usa 60, en 0–5 usa 3).");
        ayuda.setWrapText(true);
        VBox form = new VBox(10, WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Nombre del curso", nombre), new HBox(10, WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Grupo / sección", grupo),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Periodo", periodo)), WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Nota mínima aprobatoria", minima), ayuda);
        form.setPadding(new Insets(8, 4, 4, 4));
        form.setPrefWidth(440);
        dialog.getDialogPane().setContent(form);
        javafx.application.Platform.runLater(nombre::requestFocus);
        if (dialog.showAndWait().filter(b -> b == save).isEmpty()) return;
        if (nombre.getText().isBlank()) { WO_KI_NB_JI_Ui.WO_KI_NB_JI_alert(root, "Falta el nombre", "Escribe el nombre del curso."); return; }
        double min;
        try { min = Double.parseDouble(minima.getText().trim().replace(',', '.')); }
        catch (NumberFormatException ex) { WO_KI_NB_JI_Ui.WO_KI_NB_JI_alert(root, "Nota mínima inválida", "Escribe un número, por ejemplo 11."); return; }
        if (existente == null) {
            int id = clasesDao.WO_KI_NB_JI_crearClase(nombre.getText(), grupo.getText(), periodo.getText(), min);
            if (id <= 0) { WO_KI_NB_JI_Ui.WO_KI_NB_JI_alert(root, "No se pudo crear", "Revisa los datos e inténtalo de nuevo."); return; }
            cursoActual = id;
            pestanaCurso = 3;   // pestaña Estudiantes: lo siguiente es cargar la lista
        } else if (!clasesDao.WO_KI_NB_JI_actualizarClase(existente.id(), nombre.getText(), grupo.getText(), periodo.getText(), min)) {
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_alert(root, "No se pudo guardar", "Revisa los datos e inténtalo de nuevo."); return;
        }
        WO_KI_NB_JI_refrescarCursos();
        WO_KI_NB_JI_actualizarCifras();
        tabs.getSelectionModel().select(cursosTab);
    }

    private void WO_KI_NB_JI_eliminarCurso(WO_KI_NB_JI_ClaseDocente clase) {
        if (!WO_KI_NB_JI_Ui.WO_KI_NB_JI_confirm(root, "¿Eliminar el curso " + clase + "?",
            "Se borrarán sus estudiantes, asistencia, trabajos, notas y aula virtual. Esta acción no se puede deshacer. "
            + "Si quieres conservar los datos, primero expórtalos a Excel.", "Eliminar curso")) return;
        clasesDao.WO_KI_NB_JI_eliminarClase(clase.id());
        cursoActual = -1;
        WO_KI_NB_JI_actualizarBotonPrueba();
        WO_KI_NB_JI_refrescarCursos();
        WO_KI_NB_JI_actualizarCifras();
    }

    // ------------------------------------------------------------------ alertas globales

    private Node WO_KI_NB_JI_panelAlertas() {
        VBox panel = new VBox(14);
        panel.setPadding(new Insets(16, 6, 16, 4));
        WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented filtro = new WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented("Solo en riesgo", "Todas las alertas");
        filtro.WO_KI_NB_JI_select(soloRiesgo ? 0 : 1);
        filtro.WO_KI_NB_JI_selectedProperty().addListener((o, a, b) -> { soloRiesgo = b.intValue() == 0; alertasTab.setContent(WO_KI_NB_JI_panelAlertas()); });
        Label ayuda = WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Compás revisa solo el historial de cada curso y cruza inasistencias, tardanzas, trabajos no entregados y notas. "
            + "Rojo = en riesgo, ámbar = atención.");
        ayuda.setWrapText(true);
        panel.getChildren().addAll(new HBox(12, WO_KI_NB_JI_Ui.WO_KI_NB_JI_h2("Estudiantes que necesitan tu atención"), WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), filtro.WO_KI_NB_JI_node()), ayuda);
        int mostradas = 0;
        LocalDate hoy = LocalDate.now();
        for (WO_KI_NB_JI_ClaseDocente c : clasesDao.WO_KI_NB_JI_listarClases()) {
            WO_KI_NB_JI_ResumenCurso r = new WO_KI_NB_JI_ResumenCurso(c, clasesDao, hoy);
            for (var e : r.WO_KI_NB_JI_conAlertas()) {
                var ev = r.WO_KI_NB_JI_evaluacion(e.id());
                if (soloRiesgo && ev.nivel() != WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel.ALTO) continue;
                panel.getChildren().add(com.agendapersonal.ui.modules.docente.WO_KI_NB_JI_CursoView.WO_KI_NB_JI_filaAlerta(e.nombre(), ev, c.toString(), () -> {
                    cursoActual = c.id(); pestanaCurso = 0; WO_KI_NB_JI_refrescarCursos(); tabs.getSelectionModel().select(cursosTab);
                }));
                mostradas++;
            }
        }
        if (mostradas == 0) panel.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_empty("success", soloRiesgo ? "Nadie en riesgo" : "Sin alertas",
            "Cuando un estudiante acumule faltas, tardanzas o trabajos sin entregar aparecerá aquí con el motivo."));
        return WO_KI_NB_JI_Ui.WO_KI_NB_JI_scroll(panel);
    }

    // ------------------------------------------------------------------ pendientes, tutorías y enfoque

    private Node WO_KI_NB_JI_panelEvaluacion() {
        VBox panel = WO_KI_NB_JI_section("Pendientes de evaluación", "Tareas de revisión y corrección de tu agenda.");
        Button add = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("add", "Añadir tarea de revisión");
        add.setOnAction(e -> WO_KI_NB_JI_TaskViews.WO_KI_NB_JI_form(null, controller, () -> { controller.WO_KI_NB_JI_cargarTodo(); pendientesTab.setContent(WO_KI_NB_JI_panelEvaluacion()); WO_KI_NB_JI_actualizarCifras(); }));
        panel.getChildren().add(add);
        List<WO_KI_NB_JI_Tarea> tareas = controller.WO_KI_NB_JI_getTareas().stream().filter(t -> !t.WO_KI_NB_JI_estaCompletada()).toList();
        panel.getChildren().add(WO_KI_NB_JI_listadoTareas(tareas, "No hay tareas pendientes. Añade una fecha de entrega o una sesión de revisión."));
        return WO_KI_NB_JI_Ui.WO_KI_NB_JI_scroll(panel);
    }

    private Node WO_KI_NB_JI_panelTutorias() {
        VBox panel = WO_KI_NB_JI_section("Tutorías y atención", "Tus eventos de los próximos 7 días. Reserva aquí la atención a estudiantes y apoderados.");
        panel.getChildren().add(WO_KI_NB_JI_listadoEventosRango(LocalDate.now(), 7,
            "No hay tutorías en los próximos días. Puedes programarlas desde el calendario."));
        Button add = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("calendar", "Ver calendario y crear tutoría");
        add.setOnAction(e -> abrirCalendario.run());
        panel.getChildren().add(add);
        return WO_KI_NB_JI_Ui.WO_KI_NB_JI_scroll(panel);
    }

    private VBox WO_KI_NB_JI_panelEnfoque() {
        VBox panel = WO_KI_NB_JI_section("Preparación concentrada", "Elige una técnica para planificar clases, revisar trabajos o preparar materiales sin interrupciones.");
        ComboBox<WO_KI_NB_JI_MetodoEnfoque> metodo = new ComboBox<>();
        metodo.getItems().addAll(WO_KI_NB_JI_MetodoEnfoque.PREDEFINIDOS);
        if (com.agendapersonal.util.WO_KI_NB_JI_Perfil.WO_KI_NB_JI_esDocente()) metodo.getItems().addAll(WO_KI_NB_JI_MetodoEnfoque.DOCENTES);
        metodo.setValue(metodo.getItems().get(0));
        metodo.setCellFactory(lv -> new ListCell<>() { @Override protected void updateItem(WO_KI_NB_JI_MetodoEnfoque item, boolean empty) { super.updateItem(item, empty); setText(empty || item == null ? null : item.nombre()); }});
        metodo.setButtonCell(metodo.getCellFactory().call(null));
        metodo.setMaxWidth(Double.MAX_VALUE);
        Label resumen = WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted(metodo.getValue().WO_KI_NB_JI_resumen());
        resumen.setWrapText(true);

        ComboBox<String> foco = new ComboBox<>();
        foco.setEditable(true);
        Runnable actualizarFocos = () -> {
            List<String> n = WO_KI_NB_JI_focosDe(metodo.getValue());
            foco.getItems().setAll(n);
            foco.setValue(n.isEmpty() ? null : n.get(0));
        };
        metodo.setOnAction(e -> { resumen.setText(metodo.getValue().WO_KI_NB_JI_resumen()); actualizarFocos.run(); });
        actualizarFocos.run();

        Button empezar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("play", "Iniciar bloque de enfoque");
        empezar.setOnAction(e -> WO_KI_NB_JI_PantallaEnfoque.WO_KI_NB_JI_abrir(metodo.getValue(), foco.getValue(),
            WO_KI_NB_JI_PantallaEnfoque.WO_KI_NB_JI_Importancia.WO_KI_NB_JI_desde(WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_get().WO_KI_NB_JI_settings().WO_KI_NB_JI_str("timeBlocking.importance")), List.of(), (item, hecho) -> {}));
        panel.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Técnica", metodo), resumen, WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("En qué te vas a enfocar", foco), new HBox(10, empezar));
        return panel;
    }

    /** Opciones de enfoque para la técnica elegida: preparar una clase, planificar o corregir una tarea. */
    private List<String> WO_KI_NB_JI_focosDe(WO_KI_NB_JI_MetodoEnfoque m) {
        if (m == null) return List.of();
        if (m.propio()) return List.of("Trabajar en «" + m.nombre() + "»");
        List<String> r = new ArrayList<>();
        if ("docente-preparacion".equals(m.id()) || "docente-planeacion".equals(m.id())) {
            for (WO_KI_NB_JI_ClaseDocente cl : clasesDao.WO_KI_NB_JI_listarClases())
                r.add(("docente-preparacion".equals(m.id()) ? "Preparar «" : "Planificar «") + cl.nombre() + "»");
        }
        if ("docente-evaluacion".equals(m.id())) {
            for (WO_KI_NB_JI_Tarea t : controller.WO_KI_NB_JI_getTareas()) if (!t.WO_KI_NB_JI_estaCompletada()) r.add("Corregir: " + t.WO_KI_NB_JI_getTitulo());
            for (WO_KI_NB_JI_ClaseDocente cl : clasesDao.WO_KI_NB_JI_listarClases())
                for (var a : clasesDao.WO_KI_NB_JI_listarEvaluables(cl.id())) r.add("Corregir: " + a.titulo() + " (" + cl.nombre() + ")");
        }
        return r;
    }

    private VBox WO_KI_NB_JI_section(String title, String description) {
        VBox body = new VBox(14);
        body.setPadding(new Insets(20, 4, 8, 4));
        Label info = WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted(description);
        info.setWrapText(true);
        body.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h2(title), info);
        return body;
    }

    /** Eventos del rango [desde, desde + dias) ordenados por fecha y hora. */
    private VBox WO_KI_NB_JI_listadoEventosRango(LocalDate desde, int dias, String emptyText) {
        VBox list = new VBox(8);
        LocalDate fin = desde.plusDays(dias);
        List<WO_KI_NB_JI_Evento> eventos = controller.WO_KI_NB_JI_getEventos().stream()
            .filter(e -> e.WO_KI_NB_JI_getFecha() != null && !e.WO_KI_NB_JI_getFecha().isBefore(desde) && e.WO_KI_NB_JI_getFecha().isBefore(fin))
            .sorted(java.util.Comparator.comparing(WO_KI_NB_JI_Evento::WO_KI_NB_JI_getFecha)
                .thenComparing(WO_KI_NB_JI_Evento::WO_KI_NB_JI_getHoraInicio, java.util.Comparator.nullsLast(String::compareTo))).toList();
        if (eventos.isEmpty()) list.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted(emptyText));
        for (WO_KI_NB_JI_Evento evento : eventos) {
            Label when = WO_KI_NB_JI_Ui.WO_KI_NB_JI_caption(WO_KI_NB_JI_Ui.WO_KI_NB_JI_formatDateShort(evento.WO_KI_NB_JI_getFecha()) + " · " + evento.WO_KI_NB_JI_getHoraInicio() + "–" + evento.WO_KI_NB_JI_getHoraFin());
            VBox text = new VBox(3, WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(evento.WO_KI_NB_JI_getTitulo(), "body"), when);
            if (evento.WO_KI_NB_JI_getDescripcion() != null && !evento.WO_KI_NB_JI_getDescripcion().isBlank()) text.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted(evento.WO_KI_NB_JI_getDescripcion()));
            HBox row = new HBox(12, WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconChip("calendar", "violet", 36, 18), text);
            row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("card"); row.setPadding(new Insets(12));
            list.getChildren().add(row);
        }
        return list;
    }

    private VBox WO_KI_NB_JI_listadoTareas(List<WO_KI_NB_JI_Tarea> tareas, String emptyText) {
        VBox list = new VBox(8);
        if (tareas.isEmpty()) list.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted(emptyText));
        for (WO_KI_NB_JI_Tarea tarea : tareas) {
            Label due = WO_KI_NB_JI_Ui.WO_KI_NB_JI_caption(tarea.WO_KI_NB_JI_getFecha() == null ? "Sin fecha" : "Entrega: " + WO_KI_NB_JI_Ui.WO_KI_NB_JI_formatDateShort(tarea.WO_KI_NB_JI_getFecha()));
            VBox text = new VBox(3, WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(tarea.WO_KI_NB_JI_getTitulo(), "body"), due);
            if (tarea.WO_KI_NB_JI_getDescripcion() != null && !tarea.WO_KI_NB_JI_getDescripcion().isBlank()) text.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted(tarea.WO_KI_NB_JI_getDescripcion()));
            HBox row = new HBox(12, WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconChip("tasks", "blue", 36, 18), text);
            row.setAlignment(Pos.CENTER_LEFT); row.getStyleClass().add("card"); row.setPadding(new Insets(12));
            list.getChildren().add(row);
        }
        return list;
    }

    private static TextField WO_KI_NB_JI_campo(String prompt) { TextField field = new TextField(); field.setPromptText(prompt); return field; }
}

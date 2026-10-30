package com.agendapersonal.ui.modules;

import com.agendapersonal.controller.wkniMainController;
import com.agendapersonal.domain.model.wkniClaseDocente;
import com.agendapersonal.model.wkniEvento;
import com.agendapersonal.model.wkniTarea;
import com.agendapersonal.infrastructure.repo.wkniClaseDocenteDAO;
import com.agendapersonal.service.wkniDatosPruebaDocente;
import com.agendapersonal.service.wkniMetodoEnfoque;
import com.agendapersonal.service.wkniReportesDocente;
import com.agendapersonal.service.wkniResumenCurso;
import com.agendapersonal.service.wkniRiesgoEstudianteService;
import com.agendapersonal.ui.wkniTaskViews;
import com.agendapersonal.ui.wkniThemeService;
import com.agendapersonal.ui.modules.docente.wkniCursoView;
import com.agendapersonal.util.wkniUi;
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
public final class wkniEspacioDocenteView {
    private final wkniMainController wkniController;
    private final Stage wkniStage;
    private final VBox wkniRoot = wkniUi.wkniPage();
    private final ScrollPane wkniScroll = wkniUi.wkniScroll(wkniRoot);
    private final TabPane wkniTabs = new TabPane();
    private final Runnable wkniAbrirCalendario;
    private final wkniClaseDocenteDAO wkniClasesDao = new wkniClaseDocenteDAO();
    private final HBox wkniCifras = new HBox(14);
    private final Button wkniDatosPrueba = wkniUi.wkniGhost("sparkle", "");
    private final Tab wkniCursosTab;
    private final Tab wkniAlertasTab;
    private final Tab wkniPendientesTab;
    private final StackPane wkniAreaCurso = new StackPane();
    private final FlowPane wkniSelector = new FlowPane(10, 10);
    private final ToggleGroup wkniGrupoCursos = new ToggleGroup();
    private int wkniCursoActual = -1;
    private int wkniPestanaCurso = 0;
    private wkniCursoView wkni_cursoView;
    private boolean wkniSoloRiesgo = true;

    public wkniEspacioDocenteView(wkniMainController wkniController, Stage wkniStage, Runnable wkniAbrirBloques, Runnable wkniAbrirCalendario) {
        this.wkniController = wkniController;
        this.wkniStage = wkniStage;
        this.wkniAbrirCalendario = wkniAbrirCalendario;
        Button wkniPlanificar = wkniUi.wkniSecondary("clock", "Time Blocking");
        wkniPlanificar.setOnAction(wkniE -> wkniAbrirBloques.run());
        wkniDatosPrueba.setOnAction(wkniE -> wkniAlternarDatosPrueba());
        wkniActualizarBotonPrueba();
        wkniRoot.getChildren().add(wkniUi.wkniPageHeader("school", "Espacio Docente",
            "Toma asistencia con un clic, registra trabajos y notas, detecta a tiempo a quien se está quedando atrás y organiza tu aula virtual.",
            wkniDatosPrueba, wkniPlanificar));

        wkniRoot.getChildren().add(wkniCifras);

        wkniCursosTab = wkniTab("Mis cursos", "book", wkniPanelCursos());
        wkniAlertasTab = wkniTab("Alertas", "warning", new VBox());
        wkniPendientesTab = wkniTab("Pendientes", "tasks", wkniPanelEvaluacion());
        wkniTabs.getTabs().setAll(wkniCursosTab, wkniAlertasTab, wkniPendientesTab,
            wkniTab("Tutorías", "contacts", wkniPanelTutorias()), wkniTab("Enfoque", "clock", wkniPanelEnfoque()));
        wkniTabs.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        wkniTabs.getStyleClass().add("teacher-tabs");
        wkniTabs.getSelectionModel().selectedItemProperty().addListener((wkniO, wkniA, wkniB) -> {
            if (wkniB == wkniAlertasTab) wkniAlertasTab.setContent(wkniPanelAlertas());
            if (wkniB == wkniPendientesTab) wkniPendientesTab.setContent(wkniPanelEvaluacion());
        });
        VBox.setVgrow(wkniTabs, Priority.ALWAYS);
        wkniTabs.setMinHeight(620);   // en ventanas bajas la página se desplaza en lugar de aplastar las tablas
        wkniRoot.getChildren().add(wkniTabs);
        wkniRoot.setSpacing(16);
        // La página llena la ventana; si no cabe, se desplaza.
        wkniRoot.minHeightProperty().bind(wkniScroll.heightProperty().subtract(2));
        wkniActualizarCifras();
    }

    public Node wkniGetView() { return wkniScroll; }

    private Tab wkniTab(String wkniTitle, String wkniIcon, Node wkniContent) {
        Tab wkniTab = new Tab(wkniTitle, wkniContent);
        wkniTab.setGraphic(com.agendapersonal.ui.wkniIconFactory.wkniIcon(wkniIcon, 16));
        return wkniTab;
    }

    // ------------------------------------------------------------------ cifras generales

    /** Cifras del encabezado y contador de la pestaña Alertas; se llama cada vez que cambia un dato. */
    private void wkniActualizarCifras() {
        List<wkniClaseDocente> wkniClases = wkniClasesDao.wkniListarClases();
        int wkniEstudiantes = 0, wkniMarcadosHoy = 0;
        long wkniRiesgo = 0, wkniAtencion = 0;
        LocalDate wkniHoy = LocalDate.now();
        for (wkniClaseDocente wkniC : wkniClases) {
            wkniResumenCurso wkniR = new wkniResumenCurso(wkniC, wkniClasesDao, wkniHoy);
            wkniEstudiantes += wkniR.wkniEstudiantes().size();
            wkniMarcadosHoy += (int) wkniR.wkniEstudiantes().stream().filter(wkniE -> wkniR.wkniHistorial(wkniE.wkniId()).containsKey(wkniHoy)).count();
            wkniRiesgo += wkniR.wkniContar(wkniRiesgoEstudianteService.wkniNivel.ALTO);
            wkniAtencion += wkniR.wkniContar(wkniRiesgoEstudianteService.wkniNivel.ATENCION);
        }
        long wkniPendientes = wkniController.wkniGetTareas().stream().filter(wkniT -> !wkniT.wkniEstaCompletada()).count();
        wkniCifras.getChildren().setAll(
            com.agendapersonal.ui.modules.docente.wkniCursoView.wkniCifra("book", "teal", Integer.toString(wkniClases.size()),
                (wkniClases.size() == 1 ? "curso · " : "cursos · ") + wkniEstudiantes + " estudiantes"),
            com.agendapersonal.ui.modules.docente.wkniCursoView.wkniCifra("user-check", "green", wkniMarcadosHoy + " / " + wkniEstudiantes,
                wkniMarcadosHoy >= wkniEstudiantes && wkniEstudiantes > 0 ? "asistencia de hoy: ¡todo registrado!" : "con asistencia registrada hoy"),
            com.agendapersonal.ui.modules.docente.wkniCursoView.wkniCifra("flag", wkniRiesgo > 0 ? "rose" : "green", Long.toString(wkniRiesgo),
                "en riesgo · " + wkniAtencion + " requieren atención"),
            com.agendapersonal.ui.modules.docente.wkniCursoView.wkniCifra("tasks", "blue", Long.toString(wkniPendientes), "pendientes de revisión"));
        wkniAlertasTab.setText(wkniRiesgo > 0 ? "Alertas (" + wkniRiesgo + ")" : "Alertas");
    }

    private void wkniActualizarBotonPrueba() {
        boolean wkniHay = wkniDatosPruebaDocente.wkniHayDatosPrueba(wkniClasesDao);
        wkniDatosPrueba.setText(wkniHay ? "Quitar datos de prueba" : "Cargar datos de prueba");
        wkniDatosPrueba.setTooltip(new Tooltip(wkniHay ? "Borra solo los cursos marcados «(prueba)»; tus cursos reales no se tocan."
            : "Crea 2 cursos de ejemplo con estudiantes, asistencia, trabajos, notas y aula virtual para probar todo."));
    }

    private void wkniAlternarDatosPrueba() {
        if (wkniDatosPruebaDocente.wkniHayDatosPrueba(wkniClasesDao)) {
            if (!wkniUi.wkniConfirm(wkniRoot, "¿Quitar los datos de prueba?", "Se eliminan los cursos de prueba con sus estudiantes, "
                + "asistencia y notas. Tus cursos reales no se tocan.", "Quitar")) return;
            wkniDatosPruebaDocente.wkniEliminar(wkniClasesDao);
            wkniCursoActual = -1;
        } else {
            int wkniN = wkniDatosPruebaDocente.wkniCrear(wkniClasesDao, LocalDate.now());
            wkniCursoActual = wkniClasesDao.wkniListarClases().stream().filter(wkniDatosPruebaDocente::wkniEsPrueba).mapToInt(wkniClaseDocente::wkniId).findFirst().orElse(-1);
            wkniPestanaCurso = 0;
            com.agendapersonal.ui.wkniToast.wkniMostrar(wkniRoot, wkniN + " cursos de prueba creados con estudiantes, asistencia y notas.");
        }
        wkniActualizarBotonPrueba();
        wkniRefrescarCursos();
        wkniTabs.getSelectionModel().select(wkniCursosTab);
    }

    private void wkniAlCambiarDatos() {
        wkniActualizarCifras();
        if (wkniTabs.getSelectionModel().getSelectedItem() == wkniAlertasTab) wkniAlertasTab.setContent(wkniPanelAlertas());
        wkniRefrescarSelector();
    }

    // ------------------------------------------------------------------ cursos

    private VBox wkniPanelCursos() {
        VBox wkni_panel = new VBox(14);
        wkni_panel.setPadding(new Insets(16, 4, 4, 4));
        wkniSelector.setAlignment(Pos.CENTER_LEFT);
        VBox.setVgrow(wkniAreaCurso, Priority.ALWAYS);
        wkni_panel.getChildren().addAll(wkniSelector, wkniAreaCurso);
        wkniRefrescarCursos();
        return wkni_panel;
    }

    /** Reconstruye selector y curso abierto conservando el curso y la pestaña elegidos. */
    private void wkniRefrescarCursos() {
        List<wkniClaseDocente> wkniClases = wkniClasesDao.wkniListarClases();
        if (wkniClases.stream().noneMatch(wkniC -> wkniC.wkniId() == wkniCursoActual)) wkniCursoActual = wkniClases.isEmpty() ? -1 : wkniClases.getFirst().wkniId();
        wkniRefrescarSelector();
        if (wkniCursoActual < 0) {
            wkni_cursoView = null;
            Button wkniCrear = wkniUi.wkniPrimary("add", "Crear mi primer curso");
            wkniCrear.setOnAction(wkniE -> wkniDialogoCurso(null));
            Button wkniDemo = wkniUi.wkniSecondary("sparkle", "Probar con datos de ejemplo");
            wkniDemo.setOnAction(wkniE -> wkniAlternarDatosPrueba());
            VBox wkniVacio = wkniUi.wkniEmpty("school", "Empieza creando tu primer curso",
                "Luego agrega a tus estudiantes (o impórtalos de Excel) y toma asistencia con un clic. "
                + "Si solo quieres probar, carga los datos de ejemplo.");
            HBox wkniBotones = new HBox(10, wkniCrear, wkniDemo);
            wkniBotones.setAlignment(Pos.CENTER);
            wkniVacio.getChildren().add(wkniBotones);
            wkniAreaCurso.getChildren().setAll(wkniVacio);
            return;
        }
        wkniClaseDocente wkniClase = wkniClases.stream().filter(wkniC -> wkniC.wkniId() == wkniCursoActual).findFirst().orElseThrow();
        wkni_cursoView = new wkniCursoView(wkniClase, wkniClasesDao, wkniStage, wkniController, wkniPestanaCurso, wkniI -> wkniPestanaCurso = wkniI,
            this::wkniAlCambiarDatos, () -> wkniDialogoCurso(wkniClase), () -> wkniEliminarCurso(wkniClase));
        wkniAreaCurso.getChildren().setAll(wkni_cursoView.wkniGetView());
    }

    private void wkniRefrescarSelector() {
        List<wkniClaseDocente> wkniClases = wkniClasesDao.wkniListarClases();
        List<Node> wkniChips = new ArrayList<>();
        LocalDate wkniHoy = LocalDate.now();
        for (wkniClaseDocente wkniC : wkniClases) {
            wkniResumenCurso wkniR = new wkniResumenCurso(wkniC, wkniClasesDao, wkniHoy);
            long wkniRiesgo = wkniR.wkniContar(wkniRiesgoEstudianteService.wkniNivel.ALTO);
            Label wkniT = new Label(wkniC.toString());
            wkniT.getStyleClass().add("curso-chip-titulo");
            String wkniSub = wkniR.wkniEstudiantes().size() + " estudiantes · asist. " + wkniReportesDocente.wkniPct(wkniR.wkniAsistenciaPromedio());
            Label wkniS = new Label(wkniSub);
            wkniS.getStyleClass().add("curso-chip-sub");
            HBox wkniTitulo = new HBox(6, wkniT);
            wkniTitulo.setAlignment(Pos.CENTER_LEFT);
            if (wkniRiesgo > 0) wkniTitulo.getChildren().add(wkniUi.wkniPill("⚠ " + wkniRiesgo, "danger"));
            if (wkniDatosPruebaDocente.wkniEsPrueba(wkniC)) wkniTitulo.getChildren().add(wkniUi.wkniPill("prueba", "slate"));
            ToggleButton wkniB = new ToggleButton();
            wkniB.setGraphic(new VBox(3, wkniTitulo, wkniS));
            wkniB.getStyleClass().add("curso-chip");
            wkniB.setToggleGroup(wkniGrupoCursos);
            wkniB.setSelected(wkniC.wkniId() == wkniCursoActual);
            wkniB.setTooltip(new Tooltip(wkniC.wkniNombre() + (wkniC.wkniPeriodo() == null ? "" : " · " + wkniC.wkniPeriodo())));
            wkniB.setOnAction(wkniE -> {
                if (!wkniB.isSelected()) { wkniB.setSelected(true); return; }
                if (wkniCursoActual != wkniC.wkniId()) { wkniCursoActual = wkniC.wkniId(); wkniRefrescarCursos(); }
            });
            wkniChips.add(wkniB);
        }
        Button wkniNuevo = wkniUi.wkniSecondary("add", "Nuevo curso");
        wkniNuevo.setOnAction(wkniE -> wkniDialogoCurso(null));
        wkniChips.add(wkniNuevo);
        wkniSelector.getChildren().setAll(wkniChips);
    }

    /** Crear (existente = null) o editar un curso, con su nota mínima aprobatoria. */
    private void wkniDialogoCurso(wkniClaseDocente wkniExistente) {
        Dialog<ButtonType> wkniDialog = new Dialog<>();
        wkniDialog.setTitle(wkniExistente == null ? "Nuevo curso" : "Editar curso");
        wkniDialog.setHeaderText(wkniExistente == null ? "Agrega un curso al Espacio Docente" : "Datos de " + wkniExistente.wkniNombre());
        wkniDialog.initOwner(wkniStage);
        ButtonType wkniSave = new ButtonType(wkniExistente == null ? "Crear curso" : "Guardar", ButtonBar.ButtonData.OK_DONE);
        wkniDialog.getDialogPane().getButtonTypes().addAll(wkniSave, ButtonType.CANCEL);
        TextField wkniNombre = wkniCampo("Ej.: Matemática I");
        TextField wkniGrupo = wkniCampo("Ej.: A, 3.° B, Turno mañana");
        TextField wkniPeriodo = wkniCampo("Ej.: 2026-II");
        TextField wkniMinima = wkniCampo("11");
        wkniMinima.setText(wkniRiesgoEstudianteService.wkniNumero(wkniExistente == null ? wkniClaseDocente.wkniNOTA_MINIMA_DEFECTO : wkniExistente.wkniNotaMinima()));
        if (wkniExistente != null) {
            wkniNombre.setText(wkniExistente.wkniNombre());
            wkniGrupo.setText(wkniExistente.wkniGrupo() == null ? "" : wkniExistente.wkniGrupo());
            wkniPeriodo.setText(wkniExistente.wkniPeriodo() == null ? "" : wkniExistente.wkniPeriodo());
        }
        Label wkniAyuda = wkniUi.wkniCaption("La nota mínima se usa para las alertas (escala vigesimal: 11 aprueba; en 0–100 usa 60, en 0–5 usa 3).");
        wkniAyuda.setWrapText(true);
        VBox wkniForm = new VBox(10, wkniUi.wkniField("Nombre del curso", wkniNombre), new HBox(10, wkniUi.wkniField("Grupo / sección", wkniGrupo),
            wkniUi.wkniField("Periodo", wkniPeriodo)), wkniUi.wkniField("Nota mínima aprobatoria", wkniMinima), wkniAyuda);
        wkniForm.setPadding(new Insets(8, 4, 4, 4));
        wkniForm.setPrefWidth(440);
        wkniDialog.getDialogPane().setContent(wkniForm);
        javafx.application.Platform.runLater(wkniNombre::requestFocus);
        if (wkniDialog.showAndWait().filter(wkniB -> wkniB == wkniSave).isEmpty()) return;
        if (wkniNombre.getText().isBlank()) { wkniUi.wkniAlert(wkniRoot, "Falta el nombre", "Escribe el nombre del curso."); return; }
        double wkniMin;
        try { wkniMin = Double.parseDouble(wkniMinima.getText().trim().replace(',', '.')); }
        catch (NumberFormatException wkniEx) { wkniUi.wkniAlert(wkniRoot, "Nota mínima inválida", "Escribe un número, por ejemplo 11."); return; }
        if (wkniExistente == null) {
            int wkniId = wkniClasesDao.wkniCrearClase(wkniNombre.getText(), wkniGrupo.getText(), wkniPeriodo.getText(), wkniMin);
            if (wkniId <= 0) { wkniUi.wkniAlert(wkniRoot, "No se pudo crear", "Revisa los datos e inténtalo de nuevo."); return; }
            wkniCursoActual = wkniId;
            wkniPestanaCurso = 3;   // pestaña Estudiantes: lo siguiente es cargar la lista
        } else if (!wkniClasesDao.wkniActualizarClase(wkniExistente.wkniId(), wkniNombre.getText(), wkniGrupo.getText(), wkniPeriodo.getText(), wkniMin)) {
            wkniUi.wkniAlert(wkniRoot, "No se pudo guardar", "Revisa los datos e inténtalo de nuevo."); return;
        }
        wkniRefrescarCursos();
        wkniActualizarCifras();
        wkniTabs.getSelectionModel().select(wkniCursosTab);
    }

    private void wkniEliminarCurso(wkniClaseDocente wkniClase) {
        if (!wkniUi.wkniConfirm(wkniRoot, "¿Eliminar el curso " + wkniClase + "?",
            "Se borrarán sus estudiantes, asistencia, trabajos, notas y aula virtual. Esta acción no se puede deshacer. "
            + "Si quieres conservar los datos, primero expórtalos a Excel.", "Eliminar curso")) return;
        wkniClasesDao.wkniEliminarClase(wkniClase.wkniId());
        wkniCursoActual = -1;
        wkniActualizarBotonPrueba();
        wkniRefrescarCursos();
        wkniActualizarCifras();
    }

    // ------------------------------------------------------------------ alertas globales

    private Node wkniPanelAlertas() {
        VBox wkni_panel = new VBox(14);
        wkni_panel.setPadding(new Insets(16, 6, 16, 4));
        wkniUi.wkniSegmented wkniFiltro = new wkniUi.wkniSegmented("Solo en riesgo", "Todas las alertas");
        wkniFiltro.wkniSelect(wkniSoloRiesgo ? 0 : 1);
        wkniFiltro.wkniSelectedProperty().addListener((wkniO, wkniA, wkniB) -> { wkniSoloRiesgo = wkniB.intValue() == 0; wkniAlertasTab.setContent(wkniPanelAlertas()); });
        Label wkniAyuda = wkniUi.wkniMuted("Compás revisa solo el historial de cada curso y cruza inasistencias, tardanzas, trabajos no entregados y notas. "
            + "Rojo = en riesgo, ámbar = atención.");
        wkniAyuda.setWrapText(true);
        wkni_panel.getChildren().addAll(new HBox(12, wkniUi.wkniH2("Estudiantes que necesitan tu atención"), wkniUi.wkniSpacer(), wkniFiltro.wkniNode()), wkniAyuda);
        int wkniMostradas = 0;
        LocalDate wkniHoy = LocalDate.now();
        for (wkniClaseDocente wkniC : wkniClasesDao.wkniListarClases()) {
            wkniResumenCurso wkniR = new wkniResumenCurso(wkniC, wkniClasesDao, wkniHoy);
            for (var wkniE : wkniR.wkniConAlertas()) {
                var wkniEv = wkniR.wkniEvaluacion(wkniE.wkniId());
                if (wkniSoloRiesgo && wkniEv.wkni_nivel() != wkniRiesgoEstudianteService.wkniNivel.ALTO) continue;
                wkni_panel.getChildren().add(com.agendapersonal.ui.modules.docente.wkniCursoView.wkniFilaAlerta(wkniE.wkniNombre(), wkniEv, wkniC.toString(), () -> {
                    wkniCursoActual = wkniC.wkniId(); wkniPestanaCurso = 0; wkniRefrescarCursos(); wkniTabs.getSelectionModel().select(wkniCursosTab);
                }));
                wkniMostradas++;
            }
        }
        if (wkniMostradas == 0) wkni_panel.getChildren().add(wkniUi.wkniEmpty("success", wkniSoloRiesgo ? "Nadie en riesgo" : "Sin alertas",
            "Cuando un estudiante acumule faltas, tardanzas o trabajos sin entregar aparecerá aquí con el motivo."));
        return wkniUi.wkniScroll(wkni_panel);
    }

    // ------------------------------------------------------------------ pendientes, tutorías y enfoque

    private Node wkniPanelEvaluacion() {
        VBox wkni_panel = wkniSection("Pendientes de evaluación", "Tareas de revisión y corrección de tu agenda.");
        Button wkniAdd = wkniUi.wkniPrimary("add", "Añadir tarea de revisión");
        wkniAdd.setOnAction(wkniE -> wkniTaskViews.wkniForm(null, wkniController, () -> { wkniController.wkniCargarTodo(); wkniPendientesTab.setContent(wkniPanelEvaluacion()); wkniActualizarCifras(); }));
        wkni_panel.getChildren().add(wkniAdd);
        List<wkniTarea> wkniTareas = wkniController.wkniGetTareas().stream().filter(wkniT -> !wkniT.wkniEstaCompletada()).toList();
        wkni_panel.getChildren().add(wkniListadoTareas(wkniTareas, "No hay tareas pendientes. Añade una fecha de entrega o una sesión de revisión."));
        return wkniUi.wkniScroll(wkni_panel);
    }

    private Node wkniPanelTutorias() {
        VBox wkni_panel = wkniSection("Tutorías y atención", "Tus eventos de los próximos 7 días. Reserva aquí la atención a estudiantes y apoderados.");
        wkni_panel.getChildren().add(wkniListadoEventosRango(LocalDate.now(), 7,
            "No hay tutorías en los próximos días. Puedes programarlas desde el calendario."));
        Button wkniAdd = wkniUi.wkniSecondary("calendar", "Ver calendario y crear tutoría");
        wkniAdd.setOnAction(wkniE -> wkniAbrirCalendario.run());
        wkni_panel.getChildren().add(wkniAdd);
        return wkniUi.wkniScroll(wkni_panel);
    }

    private VBox wkniPanelEnfoque() {
        VBox wkni_panel = wkniSection("Preparación concentrada", "Elige una técnica para planificar clases, revisar trabajos o preparar materiales sin interrupciones.");
        ComboBox<wkniMetodoEnfoque> wkniMetodo = new ComboBox<>();
        wkniMetodo.getItems().addAll(wkniMetodoEnfoque.wkniPREDEFINIDOS);
        if (com.agendapersonal.util.wkniPerfil.wkniEsDocente()) wkniMetodo.getItems().addAll(wkniMetodoEnfoque.wkniDOCENTES);
        wkniMetodo.setValue(wkniMetodo.getItems().get(0));
        wkniMetodo.setCellFactory(wkniLv -> new ListCell<>() { @Override protected void updateItem(wkniMetodoEnfoque wkni_item, boolean wkniEmpty) { super.updateItem(wkni_item, wkniEmpty); setText(wkniEmpty || wkni_item == null ? null : wkni_item.wkniNombre()); }});
        wkniMetodo.setButtonCell(wkniMetodo.getCellFactory().call(null));
        wkniMetodo.setMaxWidth(Double.MAX_VALUE);
        Label wkni_resumen = wkniUi.wkniMuted(wkniMetodo.getValue().wkniResumen());
        wkni_resumen.setWrapText(true);

        ComboBox<String> wkniFoco = new ComboBox<>();
        wkniFoco.setEditable(true);
        Runnable wkniActualizarFocos = () -> {
            List<String> wkniN = wkniFocosDe(wkniMetodo.getValue());
            wkniFoco.getItems().setAll(wkniN);
            wkniFoco.setValue(wkniN.isEmpty() ? null : wkniN.get(0));
        };
        wkniMetodo.setOnAction(wkniE -> { wkni_resumen.setText(wkniMetodo.getValue().wkniResumen()); wkniActualizarFocos.run(); });
        wkniActualizarFocos.run();

        Button wkniEmpezar = wkniUi.wkniPrimary("play", "Iniciar bloque de enfoque");
        wkniEmpezar.setOnAction(wkniE -> wkniPantallaEnfoque.wkniAbrir(wkniMetodo.getValue(), wkniFoco.getValue(),
            wkniPantallaEnfoque.wkniImportancia.wkniDesde(wkniThemeService.wkniGet().wkniSettings().wkniStr("timeBlocking.importance")), List.of(), (wkni_item, wkniHecho) -> {}));
        wkni_panel.getChildren().addAll(wkniUi.wkniField("Técnica", wkniMetodo), wkni_resumen, wkniUi.wkniField("En qué te vas a enfocar", wkniFoco), new HBox(10, wkniEmpezar));
        return wkni_panel;
    }

    /** Opciones de enfoque para la técnica elegida: preparar una clase, planificar o corregir una tarea. */
    private List<String> wkniFocosDe(wkniMetodoEnfoque wkniM) {
        if (wkniM == null) return List.of();
        if (wkniM.wkniPropio()) return List.of("Trabajar en «" + wkniM.wkniNombre() + "»");
        List<String> wkniR = new ArrayList<>();
        if ("docente-preparacion".equals(wkniM.wkniId()) || "docente-planeacion".equals(wkniM.wkniId())) {
            for (wkniClaseDocente wkniCl : wkniClasesDao.wkniListarClases())
                wkniR.add(("docente-preparacion".equals(wkniM.wkniId()) ? "Preparar «" : "Planificar «") + wkniCl.wkniNombre() + "»");
        }
        if ("docente-evaluacion".equals(wkniM.wkniId())) {
            for (wkniTarea wkniT : wkniController.wkniGetTareas()) if (!wkniT.wkniEstaCompletada()) wkniR.add("Corregir: " + wkniT.wkniGetTitulo());
            for (wkniClaseDocente wkniCl : wkniClasesDao.wkniListarClases())
                for (var wkniA : wkniClasesDao.wkniListarEvaluables(wkniCl.wkniId())) wkniR.add("Corregir: " + wkniA.wkniTitulo() + " (" + wkniCl.wkniNombre() + ")");
        }
        return wkniR;
    }

    private VBox wkniSection(String wkniTitle, String wkniDescription) {
        VBox wkniBody = new VBox(14);
        wkniBody.setPadding(new Insets(20, 4, 8, 4));
        Label wkniInfo = wkniUi.wkniMuted(wkniDescription);
        wkniInfo.setWrapText(true);
        wkniBody.getChildren().addAll(wkniUi.wkniH2(wkniTitle), wkniInfo);
        return wkniBody;
    }

    /** Eventos del rango [desde, desde + dias) ordenados por fecha y hora. */
    private VBox wkniListadoEventosRango(LocalDate wkniDesde, int wkniDias, String wkniEmptyText) {
        VBox wkniList = new VBox(8);
        LocalDate wkniFin = wkniDesde.plusDays(wkniDias);
        List<wkniEvento> wkniEventos = wkniController.wkniGetEventos().stream()
            .filter(wkniE -> wkniE.wkniGetFecha() != null && !wkniE.wkniGetFecha().isBefore(wkniDesde) && wkniE.wkniGetFecha().isBefore(wkniFin))
            .sorted(java.util.Comparator.comparing(wkniEvento::wkniGetFecha)
                .thenComparing(wkniEvento::wkniGetHoraInicio, java.util.Comparator.nullsLast(String::compareTo))).toList();
        if (wkniEventos.isEmpty()) wkniList.getChildren().add(wkniUi.wkniMuted(wkniEmptyText));
        for (wkniEvento wkni_evento : wkniEventos) {
            Label wkniWhen = wkniUi.wkniCaption(wkniUi.wkniFormatDateShort(wkni_evento.wkniGetFecha()) + " · " + wkni_evento.wkniGetHoraInicio() + "–" + wkni_evento.wkniGetHoraFin());
            VBox wkniText = new VBox(3, wkniUi.wkniLabel(wkni_evento.wkniGetTitulo(), "body"), wkniWhen);
            if (wkni_evento.wkniGetDescripcion() != null && !wkni_evento.wkniGetDescripcion().isBlank()) wkniText.getChildren().add(wkniUi.wkniMuted(wkni_evento.wkniGetDescripcion()));
            HBox wkniRow = new HBox(12, wkniUi.wkniIconChip("calendar", "violet", 36, 18), wkniText);
            wkniRow.setAlignment(Pos.CENTER_LEFT); wkniRow.getStyleClass().add("card"); wkniRow.setPadding(new Insets(12));
            wkniList.getChildren().add(wkniRow);
        }
        return wkniList;
    }

    private VBox wkniListadoTareas(List<wkniTarea> wkniTareas, String wkniEmptyText) {
        VBox wkniList = new VBox(8);
        if (wkniTareas.isEmpty()) wkniList.getChildren().add(wkniUi.wkniMuted(wkniEmptyText));
        for (wkniTarea wkni_tarea : wkniTareas) {
            Label wkniDue = wkniUi.wkniCaption(wkni_tarea.wkniGetFecha() == null ? "Sin fecha" : "Entrega: " + wkniUi.wkniFormatDateShort(wkni_tarea.wkniGetFecha()));
            VBox wkniText = new VBox(3, wkniUi.wkniLabel(wkni_tarea.wkniGetTitulo(), "body"), wkniDue);
            if (wkni_tarea.wkniGetDescripcion() != null && !wkni_tarea.wkniGetDescripcion().isBlank()) wkniText.getChildren().add(wkniUi.wkniMuted(wkni_tarea.wkniGetDescripcion()));
            HBox wkniRow = new HBox(12, wkniUi.wkniIconChip("tasks", "blue", 36, 18), wkniText);
            wkniRow.setAlignment(Pos.CENTER_LEFT); wkniRow.getStyleClass().add("card"); wkniRow.setPadding(new Insets(12));
            wkniList.getChildren().add(wkniRow);
        }
        return wkniList;
    }

    private static TextField wkniCampo(String wkniPrompt) { TextField wkniField = new TextField(); wkniField.setPromptText(wkniPrompt); return wkniField; }
}

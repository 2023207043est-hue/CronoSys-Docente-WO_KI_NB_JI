package com.agendapersonal.ui;

import com.agendapersonal.ui.wkniFontPreferences;

import com.agendapersonal.controller.wkniMainController;
import com.agendapersonal.domain.model.wkniProyecto;
import com.agendapersonal.infrastructure.repo.wkniProyectoDAO;
import com.agendapersonal.model.wkniTarea;
import com.agendapersonal.service.wkniProgresoService;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.Locale;

public final class wkniTaskViews {
    private wkniTaskViews() {}

    /** Aplica el tema y el estilo de formulario de Compás a la ventana del diálogo. */
    private static void wkniStyle(Scene wkniSc) {
        com.agendapersonal.util.wkniUi.wkniDecorarDialogo(wkniSc);
    }

    public static void wkniForm(wkniTarea wkniOld, wkniMainController wkniC, Runnable wkniOnSaved) {
        Stage wkniD = new Stage();
        VBox wkniR = new VBox(10);
        wkniR.setPadding(new javafx.geometry.Insets(20));
        wkniR.setPrefWidth(420);
        TextField wkniTitle = new TextField(wkniOld == null ? "" : wkniOld.wkniGetTitulo());
        TextArea wkniDesc = new TextArea(wkniOld == null ? "" : wkniOld.wkniGetDescripcion());
        DatePicker wkniDate = new DatePicker(wkniOld == null ? LocalDate.now() : wkniOld.wkniGetFecha());
        ComboBox<String> wkniState = new ComboBox<>(), wkniPri = new ComboBox<>();
        wkniState.getItems().addAll("Pendiente", "En progreso", "Completada");
        wkniPri.getItems().addAll("Baja", "Media", "Alta");
        wkniState.setValue(wkniOld == null ? "Pendiente" : wkniOld.wkniGetEstado());
        wkniPri.setValue(wkniOld == null ? "Media" : wkniOld.wkniGetPrioridad());
        // Proyecto: antes se perdía el vínculo al editar una tarea; ahora se conserva y se puede cambiar.
        ComboBox<wkniProyecto> wkniProy = new ComboBox<>();
        wkniProyecto wkniSinProyecto = new wkniProyecto(0, "(Sin lista)", "", "", null, null, null) {
            @Override public String toString() { return wkniGetNombre(); }
        };
        wkniProy.getItems().add(wkniSinProyecto);
        wkniProy.getItems().addAll(wkniC.wkniGetProyectos());
        wkniProy.setValue(wkniSinProyecto);
        if (wkniOld != null && wkniOld.wkniGetProyectoId() != null)
            for (wkniProyecto wkni_p : wkniProy.getItems()) if (wkni_p.wkniGetId() == wkniOld.wkniGetProyectoId()) wkniProy.setValue(wkni_p);
        // Hora opcional, destacada y vínculos con un evento o un bloque de time blocking.
        CheckBox wkniConHora = new CheckBox("Ponerle una hora");
        TextField wkniHora = new TextField(wkniOld != null && wkniOld.wkniGetHora() != null ? wkniOld.wkniGetHora() : "09:00");
        wkniHora.setDisable(!(wkniOld != null && wkniOld.wkniGetHora() != null));
        wkniConHora.setSelected(wkniOld != null && wkniOld.wkniGetHora() != null);
        wkniConHora.selectedProperty().addListener((wkniO, wkniA, wkniV) -> wkniHora.setDisable(!wkniV));
        CheckBox wkniDestacada = new CheckBox("★ Destacar esta tarea");
        wkniDestacada.setSelected(wkniOld != null && wkniOld.wkniIsDestacada());
        ComboBox<String[]> wkni_evento = new ComboBox<>(), wkniBloque = new ComboBox<>();
        wkni_evento.getItems().add(new String[]{"0", "(Sin evento)"});
        for (var wkniEv : wkniC.wkniGetEventos()) wkni_evento.getItems().add(new String[]{String.valueOf(wkniEv.wkniGetId()), wkniEv.wkniGetFecha() + " " + wkniEv.wkniGetHoraInicio() + " · " + wkniEv.wkniGetTitulo()});
        wkniBloque.getItems().add(new String[]{"0", "(Sin bloque)"});
        for (var wkniB : new com.agendapersonal.infrastructure.repo.wkniBloqueDAO().wkniProximos())
            wkniBloque.getItems().add(new String[]{String.valueOf(wkniB.wkniGetId()), wkniB.wkniGetFecha() + " " + wkniB.wkniGetHoraInicio() + "–" + wkniB.wkniGetHoraFin() + " · " + wkniB.wkniGetTitulo()});
        javafx.util.StringConverter<String[]> wkniConv = new javafx.util.StringConverter<>() {
            @Override public String toString(String[] wkniV) { return wkniV == null ? "" : wkniV[1]; }
            @Override public String[] fromString(String wkniS) { return null; }
        };
        wkni_evento.setConverter(wkniConv);
        wkniBloque.setConverter(wkniConv);
        wkni_evento.setValue(wkni_evento.getItems().get(0));
        wkniBloque.setValue(wkniBloque.getItems().get(0));
        if (wkniOld != null && wkniOld.wkniGetEventoId() != null) for (String[] wkniV : wkni_evento.getItems()) if (wkniV[0].equals(String.valueOf(wkniOld.wkniGetEventoId()))) wkni_evento.setValue(wkniV);
        if (wkniOld != null && wkniOld.wkniGetBloqueId() != null) for (String[] wkniV : wkniBloque.getItems()) if (wkniV[0].equals(String.valueOf(wkniOld.wkniGetBloqueId()))) wkniBloque.setValue(wkniV);
        Button wkniSave = new Button(wkniOld == null ? "Guardar" : "Actualizar");
        Button wkniCancel = new Button("Cancelar");
        wkniSave.setOnAction(wkniE -> {
            if (wkniConHora.isSelected() && !wkniHora.getText().trim().matches("([01]?\\d|2[0-3]):[0-5]\\d")) {
                new Alert(Alert.AlertType.WARNING, "La hora debe tener el formato HH:mm, por ejemplo 09:30.").showAndWait();
                return;
            }
            if (wkniTitle.getText().isBlank() || wkniDate.getValue() == null) {
                new Alert(Alert.AlertType.WARNING, "Complete título y fecha.").showAndWait();
                return;
            }
            wkniProyecto wkniPs = wkniProy.getValue();
            Integer wkniPid = (wkniPs == null || wkniPs.wkniGetId() == 0) ? null : wkniPs.wkniGetId();
            wkniTarea wkniT;
            try {
                wkniT = new wkniTarea(wkniOld == null ? 0 : wkniOld.wkniGetId(), wkniTitle.getText().trim(), wkniDesc.getText().trim(),
                    wkniState.getValue(), wkniDate.getValue(), wkniPri.getValue(), wkniPid);
            } catch (IllegalArgumentException wkniEx) {
                new Alert(Alert.AlertType.WARNING, wkniEx.getMessage()).showAndWait();
                return;
            }
            wkniT.wkniSetHora(wkniConHora.isSelected() ? wkniHora.getText().trim() : null);
            wkniT.wkniSetDestacada(wkniDestacada.isSelected());
            wkniT.wkniSetEventoId(wkni_evento.getValue() == null || wkni_evento.getValue()[0].equals("0") ? null : Integer.valueOf(wkni_evento.getValue()[0]));
            wkniT.wkniSetBloqueId(wkniBloque.getValue() == null || wkniBloque.getValue()[0].equals("0") ? null : Integer.valueOf(wkniBloque.getValue()[0]));
            int wkniAntes = wkniProgresoService.wkniPorcentaje(wkniPid, wkniC.wkniGetTareas());
            boolean wkniEraCompletada = wkniOld != null && wkniOld.wkniEstaCompletada();
            boolean wkniOk = wkniOld == null ? wkniC.wkniGuardarTarea(wkniT) : wkniC.wkniActualizarTarea(wkniT);
            if (wkniOk) {
                wkniD.close();
                wkniOnSaved.run();
                wkniNotifier.wkniSuccess(wkniOld == null ? "Tarea creada" : "Tarea actualizada", wkniT.wkniGetTitulo()
                    + (wkniT.wkniGetFecha() != null ? " · " + com.agendapersonal.util.wkniUi.wkniFriendlyDate(wkniT.wkniGetFecha()) : ""));
                if (wkniT.wkniEstaCompletada() != wkniEraCompletada) {
                    int wkniDespues = wkniProgresoService.wkniPorcentaje(wkniPid, wkniC.wkniGetTareas());
                    wkniToast.wkniMostrarGlobal(wkniC.wkniMensajeProgreso(new wkniProgresoService.wkniResultado(wkniPid, wkniAntes, wkniDespues, wkniT.wkniEstaCompletada())));
                }
            }
            else new Alert(Alert.AlertType.ERROR, "No se pudo guardar la tarea.").showAndWait();
        });
        wkniCancel.setOnAction(wkniE -> wkniD.close());
        wkniR.getChildren().addAll(new Label(wkniOld == null ? "Nueva tarea" : "Editar tarea"), new Label("Título"), wkniTitle,
            new Label("Descripción"), wkniDesc, new Label("Fecha"), wkniDate, wkniConHora, wkniHora, new Label("Estado"), wkniState,
            new Label("Prioridad"), wkniPri, new Label("Lista"), wkniProy, wkniDestacada,
            new Label("Vincular a un evento del calendario"), wkni_evento, new Label("Vincular a un bloque de time blocking"), wkniBloque,
            new HBox(8, wkniCancel, wkniSave));
        wkniD.setScene(new Scene(wkniR));
        wkniStyle(wkniD.getScene());
        wkniD.showAndWait();
    }

    public static void wkniManage(wkniMainController wkniC, Runnable wkniOnChanged) {
        Stage wkniD = new Stage();
        VBox wkniR = new VBox(10);
        wkniR.setPadding(new javafx.geometry.Insets(20));
        wkniR.setPrefWidth(760);
        Label wkniTitle = new Label("Gestión de tareas");
        wkniFontPreferences.wkniBind(wkniTitle, FontWeight.BOLD, 20);
        TextField wkniSearch = new TextField();
        wkniSearch.setPromptText("Buscar por título o descripción...");
        ComboBox<String> wkniState = new ComboBox<>();
        wkniState.getItems().addAll("Todos", "Pendiente", "En progreso", "Completada");
        wkniState.setValue("Todos");
        ComboBox<String> wkniPri = new ComboBox<>();
        wkniPri.getItems().addAll("Todas", "Baja", "Media", "Alta");
        wkniPri.setValue("Todas");
        HBox wkniFilters = new HBox(8, wkniSearch, wkniState, wkniPri);
        HBox.setHgrow(wkniSearch, Priority.ALWAYS);
        ListView<wkniTarea> wkniList = new ListView<>();
        Runnable wkniFilter = () -> {
            String wkniQ = wkniSearch.getText().trim().toLowerCase(Locale.ROOT);
            wkniList.getItems().setAll(wkniC.wkniGetTareas().stream()
                .filter(wkniX -> wkniQ.isBlank() || wkniX.wkniGetTitulo().toLowerCase(Locale.ROOT).contains(wkniQ)
                    || (wkniX.wkniGetDescripcion() != null && wkniX.wkniGetDescripcion().toLowerCase(Locale.ROOT).contains(wkniQ)))
                .filter(wkniX -> "Todos".equals(wkniState.getValue()) || wkniX.wkniGetEstado().equals(wkniState.getValue()))
                .filter(wkniX -> "Todas".equals(wkniPri.getValue()) || wkniX.wkniGetPrioridad().equals(wkniPri.getValue())).toList());
        };
        wkniSearch.textProperty().addListener((wkniO, wkniOld, wkniN) -> wkniFilter.run());
        wkniState.setOnAction(wkniE -> wkniFilter.run());
        wkniPri.setOnAction(wkniE -> wkniFilter.run());
        wkniFilter.run();
        // En vivo: re-filtra solo cuando cambian las tareas
        javafx.collections.ListChangeListener<wkniTarea> wkniLive =
            wkniCh -> javafx.application.Platform.runLater(wkniFilter::run);
        wkniC.wkniGetTareas().addListener(wkniLive);
        wkniD.setOnHidden(wkniE -> wkniC.wkniGetTareas().removeListener(wkniLive));
        Button wkniAdd = new Button("Nueva tarea"), wkniEd = new Button("Editar"), wkniDel = new Button("Eliminar"), wkniClose = new Button("Cerrar");
        wkniAdd.setOnAction(wkniE -> { wkniForm(null, wkniC, () -> { wkniFilter.run(); wkniOnChanged.run(); }); });
        wkniEd.setOnAction(wkniE -> {
            wkniTarea wkniX = wkniList.getSelectionModel().getSelectedItem();
            if (wkniX != null) { wkniForm(wkniX, wkniC, () -> { wkniFilter.run(); wkniOnChanged.run(); }); }
        });
        wkniDel.setOnAction(wkniE -> {
            wkniTarea wkniX = wkniList.getSelectionModel().getSelectedItem();
            if (wkniX != null) {
                Alert wkniAl = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar \"" + wkniX.wkniGetTitulo() + "\"?");
                if (wkniAl.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    if (wkniC.wkniEliminarTarea(wkniX.wkniGetId())) wkniNotifier.wkniDeleted("Tarea eliminada", wkniX.wkniGetTitulo());
                    wkniFilter.run();
                    wkniOnChanged.run();
                }
            }
        });
        wkniClose.setOnAction(wkniE -> wkniD.close());
        HBox wkniBs = new HBox(8, wkniAdd, wkniEd, wkniDel, wkniClose);
        wkniBs.setAlignment(Pos.CENTER_RIGHT);
        wkniR.getChildren().addAll(wkniTitle, wkniFilters, wkniList, wkniBs);
        VBox.setVgrow(wkniList, Priority.ALWAYS);
        wkniD.setScene(new Scene(wkniR, 760, 500));
        wkniStyle(wkniD.getScene());
        wkniD.show();
    }
}


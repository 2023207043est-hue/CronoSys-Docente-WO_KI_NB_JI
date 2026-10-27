package com.agendapersonal.ui;

import com.agendapersonal.ui.WO_KI_NB_JI_FontPreferences;

import com.agendapersonal.controller.WO_KI_NB_JI_MainController;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Proyecto;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_ProyectoDAO;
import com.agendapersonal.model.WO_KI_NB_JI_Tarea;
import com.agendapersonal.service.WO_KI_NB_JI_ProgresoService;
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

public final class WO_KI_NB_JI_TaskViews {
    private WO_KI_NB_JI_TaskViews() {}

    /** Aplica el tema y el estilo de formulario de Compás a la ventana del diálogo. */
    private static void WO_KI_NB_JI_style(Scene sc) {
        com.agendapersonal.util.WO_KI_NB_JI_Ui.WO_KI_NB_JI_decorarDialogo(sc);
    }

    public static void WO_KI_NB_JI_form(WO_KI_NB_JI_Tarea old, WO_KI_NB_JI_MainController c, Runnable onSaved) {
        Stage d = new Stage();
        VBox r = new VBox(10);
        r.setPadding(new javafx.geometry.Insets(20));
        r.setPrefWidth(420);
        TextField title = new TextField(old == null ? "" : old.WO_KI_NB_JI_getTitulo());
        TextArea desc = new TextArea(old == null ? "" : old.WO_KI_NB_JI_getDescripcion());
        DatePicker date = new DatePicker(old == null ? LocalDate.now() : old.WO_KI_NB_JI_getFecha());
        ComboBox<String> state = new ComboBox<>(), pri = new ComboBox<>();
        state.getItems().addAll("Pendiente", "En progreso", "Completada");
        pri.getItems().addAll("Baja", "Media", "Alta");
        state.setValue(old == null ? "Pendiente" : old.WO_KI_NB_JI_getEstado());
        pri.setValue(old == null ? "Media" : old.WO_KI_NB_JI_getPrioridad());
        // Proyecto: antes se perdía el vínculo al editar una tarea; ahora se conserva y se puede cambiar.
        ComboBox<WO_KI_NB_JI_Proyecto> proy = new ComboBox<>();
        WO_KI_NB_JI_Proyecto sinProyecto = new WO_KI_NB_JI_Proyecto(0, "(Sin lista)", "", "", null, null, null) {
            @Override public String toString() { return WO_KI_NB_JI_getNombre(); }
        };
        proy.getItems().add(sinProyecto);
        proy.getItems().addAll(c.WO_KI_NB_JI_getProyectos());
        proy.setValue(sinProyecto);
        if (old != null && old.WO_KI_NB_JI_getProyectoId() != null)
            for (WO_KI_NB_JI_Proyecto p : proy.getItems()) if (p.WO_KI_NB_JI_getId() == old.WO_KI_NB_JI_getProyectoId()) proy.setValue(p);
        // Hora opcional, destacada y vínculos con un evento o un bloque de time blocking.
        CheckBox conHora = new CheckBox("Ponerle una hora");
        TextField hora = new TextField(old != null && old.WO_KI_NB_JI_getHora() != null ? old.WO_KI_NB_JI_getHora() : "09:00");
        hora.setDisable(!(old != null && old.WO_KI_NB_JI_getHora() != null));
        conHora.setSelected(old != null && old.WO_KI_NB_JI_getHora() != null);
        conHora.selectedProperty().addListener((o, a, v) -> hora.setDisable(!v));
        CheckBox destacada = new CheckBox("★ Destacar esta tarea");
        destacada.setSelected(old != null && old.WO_KI_NB_JI_isDestacada());
        ComboBox<String[]> evento = new ComboBox<>(), bloque = new ComboBox<>();
        evento.getItems().add(new String[]{"0", "(Sin evento)"});
        for (var ev : c.WO_KI_NB_JI_getEventos()) evento.getItems().add(new String[]{String.valueOf(ev.WO_KI_NB_JI_getId()), ev.WO_KI_NB_JI_getFecha() + " " + ev.WO_KI_NB_JI_getHoraInicio() + " · " + ev.WO_KI_NB_JI_getTitulo()});
        bloque.getItems().add(new String[]{"0", "(Sin bloque)"});
        for (var b : new com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_BloqueDAO().WO_KI_NB_JI_proximos())
            bloque.getItems().add(new String[]{String.valueOf(b.WO_KI_NB_JI_getId()), b.WO_KI_NB_JI_getFecha() + " " + b.WO_KI_NB_JI_getHoraInicio() + "–" + b.WO_KI_NB_JI_getHoraFin() + " · " + b.WO_KI_NB_JI_getTitulo()});
        javafx.util.StringConverter<String[]> conv = new javafx.util.StringConverter<>() {
            @Override public String toString(String[] v) { return v == null ? "" : v[1]; }
            @Override public String[] fromString(String s) { return null; }
        };
        evento.setConverter(conv);
        bloque.setConverter(conv);
        evento.setValue(evento.getItems().get(0));
        bloque.setValue(bloque.getItems().get(0));
        if (old != null && old.WO_KI_NB_JI_getEventoId() != null) for (String[] v : evento.getItems()) if (v[0].equals(String.valueOf(old.WO_KI_NB_JI_getEventoId()))) evento.setValue(v);
        if (old != null && old.WO_KI_NB_JI_getBloqueId() != null) for (String[] v : bloque.getItems()) if (v[0].equals(String.valueOf(old.WO_KI_NB_JI_getBloqueId()))) bloque.setValue(v);
        Button save = new Button(old == null ? "Guardar" : "Actualizar");
        Button cancel = new Button("Cancelar");
        save.setOnAction(e -> {
            if (conHora.isSelected() && !hora.getText().trim().matches("([01]?\\d|2[0-3]):[0-5]\\d")) {
                new Alert(Alert.AlertType.WARNING, "La hora debe tener el formato HH:mm, por ejemplo 09:30.").showAndWait();
                return;
            }
            if (title.getText().isBlank() || date.getValue() == null) {
                new Alert(Alert.AlertType.WARNING, "Complete título y fecha.").showAndWait();
                return;
            }
            WO_KI_NB_JI_Proyecto ps = proy.getValue();
            Integer pid = (ps == null || ps.WO_KI_NB_JI_getId() == 0) ? null : ps.WO_KI_NB_JI_getId();
            WO_KI_NB_JI_Tarea t;
            try {
                t = new WO_KI_NB_JI_Tarea(old == null ? 0 : old.WO_KI_NB_JI_getId(), title.getText().trim(), desc.getText().trim(),
                    state.getValue(), date.getValue(), pri.getValue(), pid);
            } catch (IllegalArgumentException ex) {
                new Alert(Alert.AlertType.WARNING, ex.getMessage()).showAndWait();
                return;
            }
            t.WO_KI_NB_JI_setHora(conHora.isSelected() ? hora.getText().trim() : null);
            t.WO_KI_NB_JI_setDestacada(destacada.isSelected());
            t.WO_KI_NB_JI_setEventoId(evento.getValue() == null || evento.getValue()[0].equals("0") ? null : Integer.valueOf(evento.getValue()[0]));
            t.WO_KI_NB_JI_setBloqueId(bloque.getValue() == null || bloque.getValue()[0].equals("0") ? null : Integer.valueOf(bloque.getValue()[0]));
            int antes = WO_KI_NB_JI_ProgresoService.WO_KI_NB_JI_porcentaje(pid, c.WO_KI_NB_JI_getTareas());
            boolean eraCompletada = old != null && old.WO_KI_NB_JI_estaCompletada();
            boolean ok = old == null ? c.WO_KI_NB_JI_guardarTarea(t) : c.WO_KI_NB_JI_actualizarTarea(t);
            if (ok) {
                d.close();
                onSaved.run();
                WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success(old == null ? "Tarea creada" : "Tarea actualizada", t.WO_KI_NB_JI_getTitulo()
                    + (t.WO_KI_NB_JI_getFecha() != null ? " · " + com.agendapersonal.util.WO_KI_NB_JI_Ui.WO_KI_NB_JI_friendlyDate(t.WO_KI_NB_JI_getFecha()) : ""));
                if (t.WO_KI_NB_JI_estaCompletada() != eraCompletada) {
                    int despues = WO_KI_NB_JI_ProgresoService.WO_KI_NB_JI_porcentaje(pid, c.WO_KI_NB_JI_getTareas());
                    WO_KI_NB_JI_Toast.WO_KI_NB_JI_mostrarGlobal(c.WO_KI_NB_JI_mensajeProgreso(new WO_KI_NB_JI_ProgresoService.WO_KI_NB_JI_Resultado(pid, antes, despues, t.WO_KI_NB_JI_estaCompletada())));
                }
            }
            else new Alert(Alert.AlertType.ERROR, "No se pudo guardar la tarea.").showAndWait();
        });
        cancel.setOnAction(e -> d.close());
        r.getChildren().addAll(new Label(old == null ? "Nueva tarea" : "Editar tarea"), new Label("Título"), title,
            new Label("Descripción"), desc, new Label("Fecha"), date, conHora, hora, new Label("Estado"), state,
            new Label("Prioridad"), pri, new Label("Lista"), proy, destacada,
            new Label("Vincular a un evento del calendario"), evento, new Label("Vincular a un bloque de time blocking"), bloque,
            new HBox(8, cancel, save));
        d.setScene(new Scene(r));
        WO_KI_NB_JI_style(d.getScene());
        d.showAndWait();
    }

    public static void WO_KI_NB_JI_manage(WO_KI_NB_JI_MainController c, Runnable onChanged) {
        Stage d = new Stage();
        VBox r = new VBox(10);
        r.setPadding(new javafx.geometry.Insets(20));
        r.setPrefWidth(760);
        Label title = new Label("Gestión de tareas");
        WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_bind(title, FontWeight.BOLD, 20);
        TextField search = new TextField();
        search.setPromptText("Buscar por título o descripción...");
        ComboBox<String> state = new ComboBox<>();
        state.getItems().addAll("Todos", "Pendiente", "En progreso", "Completada");
        state.setValue("Todos");
        ComboBox<String> pri = new ComboBox<>();
        pri.getItems().addAll("Todas", "Baja", "Media", "Alta");
        pri.setValue("Todas");
        HBox filters = new HBox(8, search, state, pri);
        HBox.setHgrow(search, Priority.ALWAYS);
        ListView<WO_KI_NB_JI_Tarea> list = new ListView<>();
        Runnable filter = () -> {
            String q = search.getText().trim().toLowerCase(Locale.ROOT);
            list.getItems().setAll(c.WO_KI_NB_JI_getTareas().stream()
                .filter(x -> q.isBlank() || x.WO_KI_NB_JI_getTitulo().toLowerCase(Locale.ROOT).contains(q)
                    || (x.WO_KI_NB_JI_getDescripcion() != null && x.WO_KI_NB_JI_getDescripcion().toLowerCase(Locale.ROOT).contains(q)))
                .filter(x -> "Todos".equals(state.getValue()) || x.WO_KI_NB_JI_getEstado().equals(state.getValue()))
                .filter(x -> "Todas".equals(pri.getValue()) || x.WO_KI_NB_JI_getPrioridad().equals(pri.getValue())).toList());
        };
        search.textProperty().addListener((o, old, n) -> filter.run());
        state.setOnAction(e -> filter.run());
        pri.setOnAction(e -> filter.run());
        filter.run();
        // En vivo: re-filtra solo cuando cambian las tareas
        javafx.collections.ListChangeListener<WO_KI_NB_JI_Tarea> live =
            ch -> javafx.application.Platform.runLater(filter::run);
        c.WO_KI_NB_JI_getTareas().addListener(live);
        d.setOnHidden(e -> c.WO_KI_NB_JI_getTareas().removeListener(live));
        Button add = new Button("Nueva tarea"), ed = new Button("Editar"), del = new Button("Eliminar"), close = new Button("Cerrar");
        add.setOnAction(e -> { WO_KI_NB_JI_form(null, c, () -> { filter.run(); onChanged.run(); }); });
        ed.setOnAction(e -> {
            WO_KI_NB_JI_Tarea x = list.getSelectionModel().getSelectedItem();
            if (x != null) { WO_KI_NB_JI_form(x, c, () -> { filter.run(); onChanged.run(); }); }
        });
        del.setOnAction(e -> {
            WO_KI_NB_JI_Tarea x = list.getSelectionModel().getSelectedItem();
            if (x != null) {
                Alert al = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar \"" + x.WO_KI_NB_JI_getTitulo() + "\"?");
                if (al.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    if (c.WO_KI_NB_JI_eliminarTarea(x.WO_KI_NB_JI_getId())) WO_KI_NB_JI_Notifier.WO_KI_NB_JI_deleted("Tarea eliminada", x.WO_KI_NB_JI_getTitulo());
                    filter.run();
                    onChanged.run();
                }
            }
        });
        close.setOnAction(e -> d.close());
        HBox bs = new HBox(8, add, ed, del, close);
        bs.setAlignment(Pos.CENTER_RIGHT);
        r.getChildren().addAll(title, filters, list, bs);
        VBox.setVgrow(list, Priority.ALWAYS);
        d.setScene(new Scene(r, 760, 500));
        WO_KI_NB_JI_style(d.getScene());
        d.show();
    }
}


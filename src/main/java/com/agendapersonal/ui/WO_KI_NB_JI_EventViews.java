package com.agendapersonal.ui;

import com.agendapersonal.ui.WO_KI_NB_JI_FontPreferences;

import com.agendapersonal.controller.WO_KI_NB_JI_MainController;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Calendario;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_CalendarioDAO;
import com.agendapersonal.model.WO_KI_NB_JI_Evento;
import com.agendapersonal.service.WO_KI_NB_JI_AgendaService;
import com.agendapersonal.service.WO_KI_NB_JI_ConflictoHorario;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
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
import javafx.stage.Modality;
import javafx.stage.Stage;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

public final class WO_KI_NB_JI_EventViews {
    private WO_KI_NB_JI_EventViews() {}

    /** Aplica el tema y el estilo de formulario de Compás a la ventana del diálogo. */
    private static void WO_KI_NB_JI_style(Scene sc) {
        com.agendapersonal.util.WO_KI_NB_JI_Ui.WO_KI_NB_JI_decorarDialogo(sc);
    }

    public static void WO_KI_NB_JI_form(Stage owner, WO_KI_NB_JI_Evento old, WO_KI_NB_JI_MainController c, Runnable onSaved) {
        WO_KI_NB_JI_formEnFecha(owner, old, null, c, onSaved);
    }

    /** Nuevo evento con fecha predefinida (clic en un día del calendario). */
    public static void WO_KI_NB_JI_formEnFecha(Stage owner, WO_KI_NB_JI_Evento old, LocalDate preset, WO_KI_NB_JI_MainController c, Runnable onSaved) {
        WO_KI_NB_JI_formEnFechaHora(owner, old, preset, null, null, c, onSaved);
    }

    /**
     * Nuevo evento con fecha, hora de inicio y categoría predefinidas (clic en la cuadrícula horaria
     * del calendario, o "Crear → Trabajo"). Los valores {@code null} usan los predeterminados.
     */
    public static void WO_KI_NB_JI_formEnFechaHora(Stage owner, WO_KI_NB_JI_Evento old, LocalDate preset, String horaInicio,
                                       String categoria, WO_KI_NB_JI_MainController c, Runnable onSaved) {
        Stage d = new Stage();
        if (owner != null) { d.initOwner(owner); d.initModality(Modality.WINDOW_MODAL); }
        VBox r = new VBox(10);
        r.setPadding(new javafx.geometry.Insets(22));
        r.setPrefWidth(430);
        TextField title = new TextField(old == null ? "" : old.WO_KI_NB_JI_getTitulo());
        DatePicker date = new DatePicker(old == null ? (preset != null ? preset : LocalDate.now()) : old.WO_KI_NB_JI_getFecha());
        TextField hi = new TextField(old == null ? (horaInicio != null ? horaInicio : "09:00") : old.WO_KI_NB_JI_getHoraInicio());
        TextField hf = new TextField(old == null ? (horaInicio != null ? WO_KI_NB_JI_masUnaHora(horaInicio) : "10:00") : old.WO_KI_NB_JI_getHoraFin());
        TextArea desc = new TextArea(old == null ? "" : old.WO_KI_NB_JI_getDescripcion());
        ComboBox<String> cat = new ComboBox<>(), pri = new ComboBox<>();
        cat.getItems().addAll("Personal", "Trabajo", "Estudio", "Salud", "Proyecto");
        pri.getItems().addAll("Baja", "Media", "Alta");
        cat.setValue(old == null ? (categoria != null ? categoria : "Personal") : old.WO_KI_NB_JI_getCategoria());
        pri.setValue(old == null ? "Media" : old.WO_KI_NB_JI_getPrioridad());
        ComboBox<String> cal = new ComboBox<>();
        List<WO_KI_NB_JI_Calendario> cals = new WO_KI_NB_JI_CalendarioDAO().WO_KI_NB_JI_listar();
        for (var k : cals) cal.getItems().add(k.WO_KI_NB_JI_getNombre());
        int calSel = 0;
        if (old != null && old.WO_KI_NB_JI_getCalendarioId() != null)
            for (int i = 0; i < cals.size(); i++)
                if (cals.get(i).WO_KI_NB_JI_getId() == old.WO_KI_NB_JI_getCalendarioId()) calSel = i;
        if (!cals.isEmpty()) cal.setValue(cals.get(calSel).WO_KI_NB_JI_getNombre());
        Button save = new Button(old == null ? "Guardar" : "Actualizar");
        Button cancel = new Button("Cancelar");
        save.setOnAction(e -> {
            if (title.getText().isBlank() || date.getValue() == null) {
                new Alert(Alert.AlertType.WARNING, "Complete título y fecha.").showAndWait();
                return;
            }
            int ci = cal.getSelectionModel().getSelectedIndex();
            Integer calId = (ci >= 0 && ci < cals.size()) ? cals.get(ci).WO_KI_NB_JI_getId() : null;
            WO_KI_NB_JI_Evento n = new WO_KI_NB_JI_Evento(old == null ? 0 : old.WO_KI_NB_JI_getId(), title.getText().trim(), date.getValue(),
                hi.getText().trim(), hf.getText().trim(), desc.getText().trim(), cat.getValue(), pri.getValue(), calId);
            if (WO_KI_NB_JI_guardarValidado(n, old == null, c)) { d.close(); onSaved.run(); }
        });
        cancel.setOnAction(e -> d.close());
        r.getChildren().addAll(new Label(old == null ? "Nuevo evento" : "Editar evento"), new Label("Título"), title,
            new Label("Fecha"), date, new Label("Hora inicio / fin"), new HBox(8, hi, hf),
            new Label("Descripción"), desc, new Label("Categoría"), cat, new Label("Prioridad"), pri,
            new Label("Calendario"), cal,
            new HBox(8, cancel, save));
        d.setScene(new Scene(r));
        WO_KI_NB_JI_style(d.getScene());
        d.showAndWait();
    }

    /** "14:00" → "15:00"; si ya es la hora 23 devuelve "23:59" (el evento no cruza la medianoche). */
    private static String WO_KI_NB_JI_masUnaHora(String hi) {
        if (!com.agendapersonal.model.WO_KI_NB_JI_Horas.WO_KI_NB_JI_esValida(hi)) return "10:00";
        var t = com.agendapersonal.model.WO_KI_NB_JI_Horas.WO_KI_NB_JI_parsear(hi);
        return t.getHour() >= 23 ? "23:59" : t.plusHours(1).format(com.agendapersonal.model.WO_KI_NB_JI_Horas.SALIDA);
    }

    /**
     * Valida el evento, detecta cruces con la agenda y guarda.
     * Si hay solapamiento, informa los minutos y deja elegir: ajustar (volver) o forzar.
     * @return true si el evento quedó guardado.
     */
    private static boolean WO_KI_NB_JI_guardarValidado(WO_KI_NB_JI_Evento n, boolean esNuevo, WO_KI_NB_JI_MainController c) {
        var error = WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_validar(n);
        if (error.isPresent()) {
            new Alert(Alert.AlertType.WARNING, error.get()).showAndWait();
            return false;
        }
        List<WO_KI_NB_JI_ConflictoHorario> conflictos = c.WO_KI_NB_JI_conflictosPara(n);
        if (!conflictos.isEmpty()) {
            StringBuilder sb = new StringBuilder("Este evento se cruza con:\n");
            for (WO_KI_NB_JI_ConflictoHorario k : conflictos) sb.append("  • ").append(k.WO_KI_NB_JI_descripcion()).append('\n');
            c.WO_KI_NB_JI_huecoLibre(n.WO_KI_NB_JI_getFecha(), WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_duracionMinutos(n)).ifPresent(h ->
                sb.append("\nHueco libre sugerido ese día: ")
                  .append(WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_hora(h[0])).append(" – ").append(WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_hora(h[1])));
            ButtonType ajustar = new ButtonType("Ajustar horario", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType forzar = new ButtonType("Guardar de todos modos", ButtonBar.ButtonData.OK_DONE);
            Alert al = new Alert(Alert.AlertType.WARNING, sb.toString(), ajustar, forzar);
            al.setTitle("Conflicto de horario");
            al.setHeaderText("Cruce de horarios detectado");
            if (al.showAndWait().orElse(ajustar) != forzar) return false;
        }
        boolean ok = esNuevo ? c.WO_KI_NB_JI_guardarEvento(n) : c.WO_KI_NB_JI_actualizarEvento(n);
        if (!ok) {
            new Alert(Alert.AlertType.ERROR, "No se pudo guardar.").showAndWait();
            return false;
        }
        WO_KI_NB_JI_Toast.WO_KI_NB_JI_mostrarGlobal((esNuevo ? "Evento guardado" : "Evento actualizado")
            + " · duración " + WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_formatearMinutos(WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_duracionMinutos(n)));
        return true;
    }

    /** Reprogramar: cambia solo fecha y horas del evento. */
    public static void WO_KI_NB_JI_reprogramar(Stage owner, WO_KI_NB_JI_Evento e, WO_KI_NB_JI_MainController c, Runnable onSaved) {
        Stage d = new Stage();
        if (owner != null) { d.initOwner(owner); d.initModality(Modality.WINDOW_MODAL); }
        VBox r = new VBox(10);
        r.setPadding(new javafx.geometry.Insets(20));
        r.setPrefWidth(340);
        Label t = new Label("Reprogramar: " + e.WO_KI_NB_JI_getTitulo());
        WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_bind(t, FontWeight.BOLD, 15);
        DatePicker date = new DatePicker(e.WO_KI_NB_JI_getFecha());
        TextField hi = new TextField(e.WO_KI_NB_JI_getHoraInicio());
        TextField hf = new TextField(e.WO_KI_NB_JI_getHoraFin());
        hi.setPromptText("Hora inicio (HH:mm)");
        hf.setPromptText("Hora fin (HH:mm)");
        Button save = new Button("Guardar");
        Button cancel = new Button("Cancelar");
        save.setOnAction(ev -> {
            if (date.getValue() == null || hi.getText().isBlank() || hf.getText().isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Complete fecha y horas.").showAndWait();
                return;
            }
            WO_KI_NB_JI_Evento n = new WO_KI_NB_JI_Evento(e.WO_KI_NB_JI_getId(), e.WO_KI_NB_JI_getTitulo(), date.getValue(),
                hi.getText().trim(), hf.getText().trim(), e.WO_KI_NB_JI_getDescripcion(), e.WO_KI_NB_JI_getCategoria(), e.WO_KI_NB_JI_getPrioridad(),
                e.WO_KI_NB_JI_getCalendarioId());
            if (WO_KI_NB_JI_guardarValidado(n, false, c)) { d.close(); onSaved.run(); }
        });
        cancel.setOnAction(ev -> d.close());
        r.getChildren().addAll(t, new Label("Nueva fecha"), date,
            new Label("Hora inicio / fin"), new HBox(8, hi, hf), new HBox(8, cancel, save));
        d.setScene(new Scene(r));
        WO_KI_NB_JI_style(d.getScene());
        d.showAndWait();
    }

    public static void WO_KI_NB_JI_manage(String initialSearch, WO_KI_NB_JI_MainController c, Stage formOwner, Runnable onChanged) {
        Stage d = new Stage();
        VBox r = new VBox(10);
        r.setPadding(new javafx.geometry.Insets(20));
        r.setPrefWidth(760);
        Label title = new Label("Gestión de eventos");
        WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_bind(title, FontWeight.BOLD, 20);
        TextField search = new TextField(initialSearch);
        search.setPromptText("Buscar por título o descripción...");
        ComboBox<String> cat = new ComboBox<>();
        cat.getItems().addAll("Todas", "Personal", "Trabajo", "Estudio", "Salud", "Proyecto");
        cat.setValue("Todas");
        ComboBox<String> pri = new ComboBox<>();
        pri.getItems().addAll("Todas", "Baja", "Media", "Alta");
        pri.setValue("Todas");
        ComboBox<String> calF = new ComboBox<>();
        calF.getItems().add("Todas");
        List<WO_KI_NB_JI_Calendario> calsF = new WO_KI_NB_JI_CalendarioDAO().WO_KI_NB_JI_listar();
        for (var k : calsF) calF.getItems().add(k.WO_KI_NB_JI_getNombre());
        calF.setValue("Todas");
        HBox filters = new HBox(8, search, cat, pri, calF);
        HBox.setHgrow(search, Priority.ALWAYS);
        ListView<WO_KI_NB_JI_Evento> list = new ListView<>();
        Runnable filter = () -> {
            String q = search.getText().trim().toLowerCase(Locale.ROOT);
            Integer selCal = null;
            if (!"Todas".equals(calF.getValue()))
                for (var k : calsF) if (k.WO_KI_NB_JI_getNombre().equals(calF.getValue())) selCal = k.WO_KI_NB_JI_getId();
            final Integer fCal = selCal;
            list.getItems().setAll(c.WO_KI_NB_JI_getEventos().stream()
                .filter(x -> q.isBlank() || x.WO_KI_NB_JI_getTitulo().toLowerCase(Locale.ROOT).contains(q)
                    || (x.WO_KI_NB_JI_getDescripcion() != null && x.WO_KI_NB_JI_getDescripcion().toLowerCase(Locale.ROOT).contains(q)))
                .filter(x -> "Todas".equals(cat.getValue()) || x.WO_KI_NB_JI_getCategoria().equals(cat.getValue()))
                .filter(x -> "Todas".equals(pri.getValue()) || x.WO_KI_NB_JI_getPrioridad().equals(pri.getValue()))
                .filter(x -> fCal == null || (x.WO_KI_NB_JI_getCalendarioId() != null && x.WO_KI_NB_JI_getCalendarioId().intValue() == fCal.intValue())).toList());
        };
        search.textProperty().addListener((o, old, n) -> filter.run());
        cat.setOnAction(e -> filter.run());
        pri.setOnAction(e -> filter.run());
        calF.setOnAction(e -> filter.run());
        filter.run();
        // En vivo: si llegan datos nuevos (polling u otra ventana), re-filtra solo
        javafx.collections.ListChangeListener<WO_KI_NB_JI_Evento> live =
            ch -> javafx.application.Platform.runLater(filter::run);
        c.WO_KI_NB_JI_getEventos().addListener(live);
        d.setOnHidden(e -> c.WO_KI_NB_JI_getEventos().removeListener(live));
        Button ed = new Button("Editar"), del = new Button("Eliminar"), close = new Button("Cerrar");
        ed.setOnAction(e -> {
            WO_KI_NB_JI_Evento x = list.getSelectionModel().getSelectedItem();
            if (x != null) { WO_KI_NB_JI_form(d, x, c, () -> { filter.run(); onChanged.run(); }); }
        });
        del.setOnAction(e -> {
            WO_KI_NB_JI_Evento x = list.getSelectionModel().getSelectedItem();
            if (x != null) {
                Alert al = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar \"" + x.WO_KI_NB_JI_getTitulo() + "\"?");
                if (al.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    if (c.WO_KI_NB_JI_eliminarEvento(x.WO_KI_NB_JI_getId())) WO_KI_NB_JI_Notifier.WO_KI_NB_JI_deleted("Evento eliminado", x.WO_KI_NB_JI_getTitulo());
                    filter.run();
                    onChanged.run();
                }
            }
        });
        close.setOnAction(e -> d.close());
        HBox bs = new HBox(8, ed, del, close);
        bs.setAlignment(Pos.CENTER_RIGHT);
        r.getChildren().addAll(title, filters, list, bs);
        VBox.setVgrow(list, Priority.ALWAYS);
        d.setScene(new Scene(r, 760, 500));
        WO_KI_NB_JI_style(d.getScene());
        d.show();
    }
}


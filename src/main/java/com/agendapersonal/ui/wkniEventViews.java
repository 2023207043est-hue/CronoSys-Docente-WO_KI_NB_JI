package com.agendapersonal.ui;

import com.agendapersonal.ui.wkniFontPreferences;

import com.agendapersonal.controller.wkniMainController;
import com.agendapersonal.domain.model.wkniCalendario;
import com.agendapersonal.infrastructure.repo.wkniCalendarioDAO;
import com.agendapersonal.model.wkniEvento;
import com.agendapersonal.service.wkniAgendaService;
import com.agendapersonal.service.wkniConflictoHorario;
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

public final class wkniEventViews {
    private wkniEventViews() {}

    /** Aplica el tema y el estilo de formulario de Compás a la ventana del diálogo. */
    private static void wkniStyle(Scene wkniSc) {
        com.agendapersonal.util.wkniUi.wkniDecorarDialogo(wkniSc);
    }

    public static void wkniForm(Stage wkniOwner, wkniEvento wkniOld, wkniMainController wkniC, Runnable wkniOnSaved) {
        wkniFormEnFecha(wkniOwner, wkniOld, null, wkniC, wkniOnSaved);
    }

    /** Nuevo evento con fecha predefinida (clic en un día del calendario). */
    public static void wkniFormEnFecha(Stage wkniOwner, wkniEvento wkniOld, LocalDate wkniPreset, wkniMainController wkniC, Runnable wkniOnSaved) {
        wkniFormEnFechaHora(wkniOwner, wkniOld, wkniPreset, null, null, wkniC, wkniOnSaved);
    }

    /**
     * Nuevo evento con fecha, hora de inicio y categoría predefinidas (clic en la cuadrícula horaria
     * del calendario, o "Crear → Trabajo"). Los valores {@code null} usan los predeterminados.
     */
    public static void wkniFormEnFechaHora(Stage wkniOwner, wkniEvento wkniOld, LocalDate wkniPreset, String wkniHoraInicio,
                                       String wkniCategoria, wkniMainController wkniC, Runnable wkniOnSaved) {
        Stage wkniD = new Stage();
        if (wkniOwner != null) { wkniD.initOwner(wkniOwner); wkniD.initModality(Modality.WINDOW_MODAL); }
        VBox wkniR = new VBox(10);
        wkniR.setPadding(new javafx.geometry.Insets(22));
        wkniR.setPrefWidth(430);
        TextField wkniTitle = new TextField(wkniOld == null ? "" : wkniOld.wkniGetTitulo());
        DatePicker wkniDate = new DatePicker(wkniOld == null ? (wkniPreset != null ? wkniPreset : LocalDate.now()) : wkniOld.wkniGetFecha());
        TextField wkniHi = new TextField(wkniOld == null ? (wkniHoraInicio != null ? wkniHoraInicio : "09:00") : wkniOld.wkniGetHoraInicio());
        TextField wkniHf = new TextField(wkniOld == null ? (wkniHoraInicio != null ? wkniMasUnaHora(wkniHoraInicio) : "10:00") : wkniOld.wkniGetHoraFin());
        TextArea wkniDesc = new TextArea(wkniOld == null ? "" : wkniOld.wkniGetDescripcion());
        ComboBox<String> wkniCat = new ComboBox<>(), wkniPri = new ComboBox<>();
        wkniCat.getItems().addAll("Personal", "Trabajo", "Estudio", "Salud", "Proyecto");
        wkniPri.getItems().addAll("Baja", "Media", "Alta");
        wkniCat.setValue(wkniOld == null ? (wkniCategoria != null ? wkniCategoria : "Personal") : wkniOld.wkniGetCategoria());
        wkniPri.setValue(wkniOld == null ? "Media" : wkniOld.wkniGetPrioridad());
        ComboBox<String> wkniCal = new ComboBox<>();
        List<wkniCalendario> wkniCals = new wkniCalendarioDAO().wkniListar();
        for (var wkniK : wkniCals) wkniCal.getItems().add(wkniK.wkniGetNombre());
        int wkniCalSel = 0;
        if (wkniOld != null && wkniOld.wkniGetCalendarioId() != null)
            for (int wkniI = 0; wkniI < wkniCals.size(); wkniI++)
                if (wkniCals.get(wkniI).wkniGetId() == wkniOld.wkniGetCalendarioId()) wkniCalSel = wkniI;
        if (!wkniCals.isEmpty()) wkniCal.setValue(wkniCals.get(wkniCalSel).wkniGetNombre());
        Button wkniSave = new Button(wkniOld == null ? "Guardar" : "Actualizar");
        Button wkniCancel = new Button("Cancelar");
        wkniSave.setOnAction(wkniE -> {
            if (wkniTitle.getText().isBlank() || wkniDate.getValue() == null) {
                new Alert(Alert.AlertType.WARNING, "Complete título y fecha.").showAndWait();
                return;
            }
            int wkniCi = wkniCal.getSelectionModel().getSelectedIndex();
            Integer wkniCalId = (wkniCi >= 0 && wkniCi < wkniCals.size()) ? wkniCals.get(wkniCi).wkniGetId() : null;
            wkniEvento wkniN = new wkniEvento(wkniOld == null ? 0 : wkniOld.wkniGetId(), wkniTitle.getText().trim(), wkniDate.getValue(),
                wkniHi.getText().trim(), wkniHf.getText().trim(), wkniDesc.getText().trim(), wkniCat.getValue(), wkniPri.getValue(), wkniCalId);
            if (wkniGuardarValidado(wkniN, wkniOld == null, wkniC)) { wkniD.close(); wkniOnSaved.run(); }
        });
        wkniCancel.setOnAction(wkniE -> wkniD.close());
        wkniR.getChildren().addAll(new Label(wkniOld == null ? "Nuevo evento" : "Editar evento"), new Label("Título"), wkniTitle,
            new Label("Fecha"), wkniDate, new Label("Hora inicio / fin"), new HBox(8, wkniHi, wkniHf),
            new Label("Descripción"), wkniDesc, new Label("Categoría"), wkniCat, new Label("Prioridad"), wkniPri,
            new Label("Calendario"), wkniCal,
            new HBox(8, wkniCancel, wkniSave));
        wkniD.setScene(new Scene(wkniR));
        wkniStyle(wkniD.getScene());
        wkniD.showAndWait();
    }

    /** "14:00" → "15:00"; si ya es la hora 23 devuelve "23:59" (el evento no cruza la medianoche). */
    private static String wkniMasUnaHora(String wkniHi) {
        if (!com.agendapersonal.model.wkniHoras.wkniEsValida(wkniHi)) return "10:00";
        var wkniT = com.agendapersonal.model.wkniHoras.wkniParsear(wkniHi);
        return wkniT.getHour() >= 23 ? "23:59" : wkniT.plusHours(1).format(com.agendapersonal.model.wkniHoras.wkniSALIDA);
    }

    /**
     * Valida el evento, detecta cruces con la agenda y guarda.
     * Si hay solapamiento, informa los minutos y deja elegir: ajustar (volver) o forzar.
     * @return true si el evento quedó guardado.
     */
    private static boolean wkniGuardarValidado(wkniEvento wkniN, boolean wkniEsNuevo, wkniMainController wkniC) {
        var wkniError = wkniAgendaService.wkniValidar(wkniN);
        if (wkniError.isPresent()) {
            new Alert(Alert.AlertType.WARNING, wkniError.get()).showAndWait();
            return false;
        }
        List<wkniConflictoHorario> wkniConflictos = wkniC.wkniConflictosPara(wkniN);
        if (!wkniConflictos.isEmpty()) {
            StringBuilder wkniSb = new StringBuilder("Este evento se cruza con:\n");
            for (wkniConflictoHorario wkniK : wkniConflictos) wkniSb.append("  • ").append(wkniK.wkniDescripcion()).append('\n');
            wkniC.wkniHuecoLibre(wkniN.wkniGetFecha(), wkniAgendaService.wkniDuracionMinutos(wkniN)).ifPresent(wkniH ->
                wkniSb.append("\nHueco libre sugerido ese día: ")
                  .append(wkniAgendaService.wkniHora(wkniH[0])).append(" – ").append(wkniAgendaService.wkniHora(wkniH[1])));
            ButtonType wkniAjustar = new ButtonType("Ajustar horario", ButtonBar.ButtonData.CANCEL_CLOSE);
            ButtonType wkniForzar = new ButtonType("Guardar de todos modos", ButtonBar.ButtonData.OK_DONE);
            Alert wkniAl = new Alert(Alert.AlertType.WARNING, wkniSb.toString(), wkniAjustar, wkniForzar);
            wkniAl.setTitle("Conflicto de horario");
            wkniAl.setHeaderText("Cruce de horarios detectado");
            if (wkniAl.showAndWait().orElse(wkniAjustar) != wkniForzar) return false;
        }
        boolean wkniOk = wkniEsNuevo ? wkniC.wkniGuardarEvento(wkniN) : wkniC.wkniActualizarEvento(wkniN);
        if (!wkniOk) {
            new Alert(Alert.AlertType.ERROR, "No se pudo guardar.").showAndWait();
            return false;
        }
        wkniToast.wkniMostrarGlobal((wkniEsNuevo ? "Evento guardado" : "Evento actualizado")
            + " · duración " + wkniAgendaService.wkniFormatearMinutos(wkniAgendaService.wkniDuracionMinutos(wkniN)));
        return true;
    }

    /** Reprogramar: cambia solo fecha y horas del evento. */
    public static void wkniReprogramar(Stage wkniOwner, wkniEvento wkniE, wkniMainController wkniC, Runnable wkniOnSaved) {
        Stage wkniD = new Stage();
        if (wkniOwner != null) { wkniD.initOwner(wkniOwner); wkniD.initModality(Modality.WINDOW_MODAL); }
        VBox wkniR = new VBox(10);
        wkniR.setPadding(new javafx.geometry.Insets(20));
        wkniR.setPrefWidth(340);
        Label wkniT = new Label("Reprogramar: " + wkniE.wkniGetTitulo());
        wkniFontPreferences.wkniBind(wkniT, FontWeight.BOLD, 15);
        DatePicker wkniDate = new DatePicker(wkniE.wkniGetFecha());
        TextField wkniHi = new TextField(wkniE.wkniGetHoraInicio());
        TextField wkniHf = new TextField(wkniE.wkniGetHoraFin());
        wkniHi.setPromptText("Hora inicio (HH:mm)");
        wkniHf.setPromptText("Hora fin (HH:mm)");
        Button wkniSave = new Button("Guardar");
        Button wkniCancel = new Button("Cancelar");
        wkniSave.setOnAction(wkniEv -> {
            if (wkniDate.getValue() == null || wkniHi.getText().isBlank() || wkniHf.getText().isBlank()) {
                new Alert(Alert.AlertType.WARNING, "Complete fecha y horas.").showAndWait();
                return;
            }
            wkniEvento wkniN = new wkniEvento(wkniE.wkniGetId(), wkniE.wkniGetTitulo(), wkniDate.getValue(),
                wkniHi.getText().trim(), wkniHf.getText().trim(), wkniE.wkniGetDescripcion(), wkniE.wkniGetCategoria(), wkniE.wkniGetPrioridad(),
                wkniE.wkniGetCalendarioId());
            if (wkniGuardarValidado(wkniN, false, wkniC)) { wkniD.close(); wkniOnSaved.run(); }
        });
        wkniCancel.setOnAction(wkniEv -> wkniD.close());
        wkniR.getChildren().addAll(wkniT, new Label("Nueva fecha"), wkniDate,
            new Label("Hora inicio / fin"), new HBox(8, wkniHi, wkniHf), new HBox(8, wkniCancel, wkniSave));
        wkniD.setScene(new Scene(wkniR));
        wkniStyle(wkniD.getScene());
        wkniD.showAndWait();
    }

    public static void wkniManage(String wkniInitialSearch, wkniMainController wkniC, Stage wkniFormOwner, Runnable wkniOnChanged) {
        Stage wkniD = new Stage();
        VBox wkniR = new VBox(10);
        wkniR.setPadding(new javafx.geometry.Insets(20));
        wkniR.setPrefWidth(760);
        Label wkniTitle = new Label("Gestión de eventos");
        wkniFontPreferences.wkniBind(wkniTitle, FontWeight.BOLD, 20);
        TextField wkniSearch = new TextField(wkniInitialSearch);
        wkniSearch.setPromptText("Buscar por título o descripción...");
        ComboBox<String> wkniCat = new ComboBox<>();
        wkniCat.getItems().addAll("Todas", "Personal", "Trabajo", "Estudio", "Salud", "Proyecto");
        wkniCat.setValue("Todas");
        ComboBox<String> wkniPri = new ComboBox<>();
        wkniPri.getItems().addAll("Todas", "Baja", "Media", "Alta");
        wkniPri.setValue("Todas");
        ComboBox<String> wkniCalF = new ComboBox<>();
        wkniCalF.getItems().add("Todas");
        List<wkniCalendario> wkniCalsF = new wkniCalendarioDAO().wkniListar();
        for (var wkniK : wkniCalsF) wkniCalF.getItems().add(wkniK.wkniGetNombre());
        wkniCalF.setValue("Todas");
        HBox wkniFilters = new HBox(8, wkniSearch, wkniCat, wkniPri, wkniCalF);
        HBox.setHgrow(wkniSearch, Priority.ALWAYS);
        ListView<wkniEvento> wkniList = new ListView<>();
        Runnable wkniFilter = () -> {
            String wkniQ = wkniSearch.getText().trim().toLowerCase(Locale.ROOT);
            Integer wkniSelCal = null;
            if (!"Todas".equals(wkniCalF.getValue()))
                for (var wkniK : wkniCalsF) if (wkniK.wkniGetNombre().equals(wkniCalF.getValue())) wkniSelCal = wkniK.wkniGetId();
            final Integer wkniFCal = wkniSelCal;
            wkniList.getItems().setAll(wkniC.wkniGetEventos().stream()
                .filter(wkniX -> wkniQ.isBlank() || wkniX.wkniGetTitulo().toLowerCase(Locale.ROOT).contains(wkniQ)
                    || (wkniX.wkniGetDescripcion() != null && wkniX.wkniGetDescripcion().toLowerCase(Locale.ROOT).contains(wkniQ)))
                .filter(wkniX -> "Todas".equals(wkniCat.getValue()) || wkniX.wkniGetCategoria().equals(wkniCat.getValue()))
                .filter(wkniX -> "Todas".equals(wkniPri.getValue()) || wkniX.wkniGetPrioridad().equals(wkniPri.getValue()))
                .filter(wkniX -> wkniFCal == null || (wkniX.wkniGetCalendarioId() != null && wkniX.wkniGetCalendarioId().intValue() == wkniFCal.intValue())).toList());
        };
        wkniSearch.textProperty().addListener((wkniO, wkniOld, wkniN) -> wkniFilter.run());
        wkniCat.setOnAction(wkniE -> wkniFilter.run());
        wkniPri.setOnAction(wkniE -> wkniFilter.run());
        wkniCalF.setOnAction(wkniE -> wkniFilter.run());
        wkniFilter.run();
        // En vivo: si llegan datos nuevos (polling u otra ventana), re-filtra solo
        javafx.collections.ListChangeListener<wkniEvento> wkniLive =
            wkniCh -> javafx.application.Platform.runLater(wkniFilter::run);
        wkniC.wkniGetEventos().addListener(wkniLive);
        wkniD.setOnHidden(wkniE -> wkniC.wkniGetEventos().removeListener(wkniLive));
        Button wkniEd = new Button("Editar"), wkniDel = new Button("Eliminar"), wkniClose = new Button("Cerrar");
        wkniEd.setOnAction(wkniE -> {
            wkniEvento wkniX = wkniList.getSelectionModel().getSelectedItem();
            if (wkniX != null) { wkniForm(wkniD, wkniX, wkniC, () -> { wkniFilter.run(); wkniOnChanged.run(); }); }
        });
        wkniDel.setOnAction(wkniE -> {
            wkniEvento wkniX = wkniList.getSelectionModel().getSelectedItem();
            if (wkniX != null) {
                Alert wkniAl = new Alert(Alert.AlertType.CONFIRMATION, "¿Eliminar \"" + wkniX.wkniGetTitulo() + "\"?");
                if (wkniAl.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                    if (wkniC.wkniEliminarEvento(wkniX.wkniGetId())) wkniNotifier.wkniDeleted("Evento eliminado", wkniX.wkniGetTitulo());
                    wkniFilter.run();
                    wkniOnChanged.run();
                }
            }
        });
        wkniClose.setOnAction(wkniE -> wkniD.close());
        HBox wkniBs = new HBox(8, wkniEd, wkniDel, wkniClose);
        wkniBs.setAlignment(Pos.CENTER_RIGHT);
        wkniR.getChildren().addAll(wkniTitle, wkniFilters, wkniList, wkniBs);
        VBox.setVgrow(wkniList, Priority.ALWAYS);
        wkniD.setScene(new Scene(wkniR, 760, 500));
        wkniStyle(wkniD.getScene());
        wkniD.show();
    }
}


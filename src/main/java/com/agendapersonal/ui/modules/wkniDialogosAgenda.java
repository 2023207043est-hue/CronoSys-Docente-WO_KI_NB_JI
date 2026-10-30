package com.agendapersonal.ui.modules;

import com.agendapersonal.domain.model.wkniContacto;
import com.agendapersonal.domain.model.wkniHabito;
import com.agendapersonal.infrastructure.repo.wkniContactoDAO;
import com.agendapersonal.infrastructure.repo.wkniHabitoDAO;
import javafx.beans.binding.Bindings;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Window;
import java.time.LocalDate;

/** Diálogos rápidos del menú "Crear" para lo que no tiene formulario propio (hábitos y cumpleaños). */
public final class wkniDialogosAgenda {
    private wkniDialogosAgenda() {}

    public static void wkniNuevoHabito(Window wkniDueno, Runnable wkniAlGuardar) {
        Dialog<ButtonType> wkniD = wkniBase(wkniDueno, "Nuevo hábito",
            "Aparecerá cada día en tu calendario para que lo marques como cumplido.");
        TextField wkniNombre = new TextField();
        wkniNombre.setPromptText("Ej. Leer 20 min");
        wkniNombre.setPrefWidth(260);
        ComboBox<String> wkniFrecuencia = new ComboBox<>();
        wkniFrecuencia.getItems().addAll("DIARIO", "SEMANAL");
        wkniFrecuencia.setValue("DIARIO");
        GridPane wkniG = wkniRejilla();
        wkniG.addRow(0, new Label("Nombre"), wkniNombre);
        wkniG.addRow(1, new Label("Frecuencia"), wkniFrecuencia);
        wkniD.getDialogPane().setContent(wkniG);
        Button wkniOk = (Button) wkniD.getDialogPane().lookupButton(ButtonType.OK);
        wkniOk.disableProperty().bind(Bindings.createBooleanBinding(() -> wkniNombre.getText().isBlank(), wkniNombre.textProperty()));
        wkniD.showAndWait().filter(wkniB -> wkniB == ButtonType.OK).ifPresent(wkniB -> {
            boolean wkniGuardado = new wkniHabitoDAO().wkniInsertar(
                new wkniHabito(0, wkniNombre.getText().trim(), "", wkniFrecuencia.getValue(), 1, "#2C7A4B", true));
            if (wkniGuardado) { wkniAlGuardar.run(); com.agendapersonal.ui.wkniNotifier.wkniSuccess("Hábito creado", wkniNombre.getText().trim()); }
            else com.agendapersonal.ui.wkniNotifier.wkniError("No se pudo guardar el hábito", null);
        });
    }

    /** Crea un contacto (visible también en CRM Personal) con su fecha de cumpleaños. */
    public static void wkniNuevoCumple(Window wkniDueno, Runnable wkniAlGuardar) {
        Dialog<ButtonType> wkniD = wkniBase(wkniDueno, "Nuevo cumpleaños",
            "Se repetirá cada año en tu calendario y te avisará al iniciar Compás.");
        TextField wkniNombre = new TextField();
        wkniNombre.setPromptText("Nombre de la persona");
        wkniNombre.setPrefWidth(260);
        DatePicker wkni_fecha = new DatePicker();
        wkni_fecha.setPromptText("Fecha de nacimiento");
        wkni_fecha.setEditable(false);
        wkni_fecha.setValue(LocalDate.now().minusYears(25));
        GridPane wkniG = wkniRejilla();
        wkniG.addRow(0, new Label("Nombre"), wkniNombre);
        wkniG.addRow(1, new Label("Nacimiento"), wkni_fecha);
        wkniD.getDialogPane().setContent(wkniG);
        Button wkniOk = (Button) wkniD.getDialogPane().lookupButton(ButtonType.OK);
        wkniOk.disableProperty().bind(Bindings.createBooleanBinding(
            () -> wkniNombre.getText().isBlank() || wkni_fecha.getValue() == null, wkniNombre.textProperty(), wkni_fecha.valueProperty()));
        wkniD.showAndWait().filter(wkniB -> wkniB == ButtonType.OK).ifPresent(wkniB -> {
            boolean wkniGuardado = new wkniContactoDAO().wkniInsertar(new wkniContacto(0, wkniNombre.getText().trim(), "", "",
                "Personal", wkni_fecha.getValue().toString(), "", false, null));
            if (wkniGuardado) { wkniAlGuardar.run(); com.agendapersonal.ui.wkniNotifier.wkniSuccess("Cumpleaños agregado", wkniNombre.getText().trim() + " · " + com.agendapersonal.util.wkniUi.wkniDayMonth(wkni_fecha.getValue())); }
            else com.agendapersonal.ui.wkniNotifier.wkniError("No se pudo guardar el cumpleaños", null);
        });
    }

    private static Dialog<ButtonType> wkniBase(Window wkniDueno, String wkniTitulo, String wkniCabecera) {
        Dialog<ButtonType> wkniD = new Dialog<>();
        if (wkniDueno != null) wkniD.initOwner(wkniDueno);
        wkniD.setTitle(wkniTitulo);
        wkniD.setHeaderText(wkniCabecera);
        wkniD.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        wkniD.getDialogPane().getStyleClass().add("cs-dialog");
        ((Button) wkniD.getDialogPane().lookupButton(ButtonType.OK)).setText("Guardar");
        ((Button) wkniD.getDialogPane().lookupButton(ButtonType.OK)).getStyleClass().add("btn-primary");
        ((Button) wkniD.getDialogPane().lookupButton(ButtonType.CANCEL)).setText("Cancelar");
        ((Button) wkniD.getDialogPane().lookupButton(ButtonType.CANCEL)).getStyleClass().add("btn-secondary");
        return wkniD;
    }

    private static GridPane wkniRejilla() {
        GridPane wkniG = new GridPane();
        wkniG.setHgap(12);
        wkniG.setVgap(12);
        wkniG.setPadding(new Insets(14));
        return wkniG;
    }
}

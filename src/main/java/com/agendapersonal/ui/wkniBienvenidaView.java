package com.agendapersonal.ui;

import com.agendapersonal.util.wkniPerfil;
import com.agendapersonal.util.wkniUi;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/** Pantalla de bienvenida de la primera vez: pide el nombre (sin contraseña) y lo usa en el saludo. */
public final class wkniBienvenidaView {
    private wkniBienvenidaView() {}

    /** Muestra la bienvenida y espera a que se cierre. Solo aparece la primera vez. */
    public static void wkniMostrarSiHaceFalta() {
        if (wkniPerfil.wkniBienvenidaVista()) return;
        Stage wkniSt = new Stage();
        wkniSt.setTitle("Bienvenido a Compás");
        wkniLogo.wkniAplicarIconos(wkniSt);
        wkniSt.setResizable(false);

        Label wkniHola = new Label("¡Te damos la bienvenida!");
        wkniHola.getStyleClass().add("h1");
        Label wkniSub = new Label("Personaliza tu espacio de trabajo. Todo queda guardado en este equipo.");
        wkniSub.setWrapText(true);
        wkniSub.setStyle("-fx-text-fill:-muted;-fx-text-alignment:center;");
        wkniSub.setAlignment(Pos.CENTER);

        TextField wkniNombre = new TextField();
        wkniNombre.setPromptText("Tu nombre");
        wkniNombre.setMaxWidth(320);

        Label wkniNombreLabel = new Label("¿Cómo te llamas?");
        wkniNombreLabel.getStyleClass().add("h3");
        wkniNombre.setAccessibleText("Tu nombre");

        ToggleGroup wkniGrupoRol = new ToggleGroup();
        ToggleButton wkniEstudiante = wkniOpcionRol(wkniGrupoRol, "book", "Estudiante",
            "Organiza tus cursos, estudio y tareas.");
        ToggleButton wkniDocente = wkniOpcionRol(wkniGrupoRol, "calendar", "Docente",
            "Prepara clases, evaluaciones y tutorías.");
        wkniEstudiante.setSelected(true);
        HBox wkniOpciones = new HBox(12, wkniEstudiante, wkniDocente);
        wkniOpciones.setMaxWidth(440);
        wkniOpciones.setAlignment(Pos.CENTER);

        Button wkniEmpezar = wkniUi.wkniPrimary(null, "Empezar");
        wkniEmpezar.setDefaultButton(true);
        wkniEmpezar.disableProperty().bind(wkniNombre.textProperty().map(wkniT -> wkniPerfil.wkniLimpiar(wkniT).isEmpty()));
        wkniEmpezar.setOnAction(wkniE -> {
            wkniPerfil.wkniGuardarNombre(wkniNombre.getText());
            wkniPerfil.wkniGuardarRol(wkniDocente.isSelected() ? wkniPerfil.wkniDOCENTE : wkniPerfil.wkniESTUDIANTE);
            wkniSt.close();
        });
        Button wkniOmitir = wkniUi.wkniGhost(null, "Omitir por ahora");
        wkniOmitir.setOnAction(wkniE -> { wkniGuardarRol(wkniDocente.isSelected()); wkniPerfil.wkniOmitirBienvenida(); wkniSt.close(); });
        Label wkni_nota = new Label("Puedes cambiar el perfil después en Configuración → Tu perfil.");
        wkni_nota.getStyleClass().add("caption");

        VBox wkniRaiz = new VBox(14, wkniLogo.wkniMark(64), wkniHola, wkniSub, wkniNombreLabel, wkniNombre, wkniOpciones, wkniEmpezar, wkniOmitir, wkni_nota);
        wkniRaiz.setAlignment(Pos.CENTER);
        wkniRaiz.setPadding(new Insets(28, 40, 24, 40));
        wkniRaiz.setStyle("-fx-background-color:-bg;");
        Scene wkniSc = new Scene(wkniRaiz, 570, 600);
        wkniThemeService.wkniGet().wkniApplyTo(wkniSc);
        wkniSt.setScene(wkniSc);
        wkniSt.setOnCloseRequest(wkniE -> { wkniGuardarRol(wkniDocente.isSelected()); wkniPerfil.wkniOmitirBienvenida(); });
        wkniSt.showAndWait();
    }

    private static ToggleButton wkniOpcionRol(ToggleGroup wkniGrupo, String wkniIcono, String wkniNombre, String wkniDescripcion) {
        Label wkniTitulo = new Label(wkniNombre);
        wkniTitulo.getStyleClass().add("h3");
        Label wkniDetalle = new Label(wkniDescripcion);
        wkniDetalle.getStyleClass().add("caption");
        wkniDetalle.setWrapText(true);
        VBox wkniContenido = new VBox(7, wkniIconFactory.wkniIcon(wkniIcono, 22), wkniTitulo, wkniDetalle);
        wkniContenido.setAlignment(Pos.CENTER_LEFT);
        ToggleButton wkniBoton = new ToggleButton();
        wkniBoton.setToggleGroup(wkniGrupo);
        wkniBoton.setGraphic(wkniContenido);
        wkniBoton.setAccessibleText(wkniNombre + ". " + wkniDescripcion);
        wkniBoton.setMaxWidth(Double.MAX_VALUE);
        wkniBoton.setMinHeight(128);
        wkniBoton.setPrefHeight(128);
        wkniBoton.getStyleClass().add("role-choice");
        HBox.setHgrow(wkniBoton, javafx.scene.layout.Priority.ALWAYS);
        return wkniBoton;
    }

    private static void wkniGuardarRol(boolean wkniDocente) {
        wkniPerfil.wkniGuardarRol(wkniDocente ? wkniPerfil.wkniDOCENTE : wkniPerfil.wkniESTUDIANTE);
    }
}

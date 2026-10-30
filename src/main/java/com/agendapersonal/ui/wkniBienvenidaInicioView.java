package com.agendapersonal.ui;

import com.agendapersonal.controller.wkniMainController;
import com.agendapersonal.domain.model.wkniNota;
import com.agendapersonal.infrastructure.repo.wkniNotaDAO;
import com.agendapersonal.model.wkniEvento;
import com.agendapersonal.model.wkniTarea;
import com.agendapersonal.util.wkniPerfil;
import com.agendapersonal.util.wkniUi;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;

/** Saludo al abrir la aplicación, con un recuerdo tomado del contenido que la persona guardó antes. */
public final class wkniBienvenidaInicioView {
    private record wkniRecuerdo(String wkniEtiqueta, String wkniTexto) {}

    private wkniBienvenidaInicioView() {}

    public static void wkniMostrar(Window wkniOwner, wkniMainController wkniController, Runnable wkniAbrirAgenda, Runnable wkniAbrirNotas, Runnable wkniAbrirEspacioDocente) {
        Stage wkniPopup = new Stage();
        if (wkniOwner != null && wkniOwner.isShowing()) wkniPopup.initOwner(wkniOwner);
        wkniPopup.setTitle("Compás — Bienvenida");
        wkniLogo.wkniAplicarIconos(wkniPopup);

        boolean wkniDocente = com.agendapersonal.util.wkniPerfil.wkniEsDocente();
        Label wkniSaludo = wkniUi.wkniLabel(wkniPerfil.wkniSaludo(LocalTime.now().getHour(), wkniPerfil.wkniNombre()) + " 👋", "h1");
        Label wkniSub = wkniUi.wkniMuted(wkniDocente
            ? "Qué bueno verte. Aquí va un pequeño recuerdo de tu espacio y lo que prepara tu docente hoy."
            : "Qué bueno verte. Este es un pequeño recuerdo de tu espacio.");
        wkniSub.setWrapText(true);
        VBox wkniEncabezado = new VBox(5, wkniLogo.wkniMark(48), wkniSaludo, wkniSub);
        wkniEncabezado.setAlignment(Pos.CENTER);

        wkniRecuerdo wkni_recuerdo = wkniRecuerdo(wkniController);
        Label wkniEtiqueta = wkniUi.wkniLabel(wkni_recuerdo.wkniEtiqueta(), "caption");
        Label wkniTexto = wkniUi.wkniLabel(wkni_recuerdo.wkniTexto(), "h3");
        wkniTexto.setWrapText(true);
        VBox wkniTarjeta = wkniUi.wkniCard();
        wkniTarjeta.getChildren().addAll(wkniUi.wkniIconChip("sparkle", "orange", 34, 18), wkniEtiqueta, wkniTexto);

        Button wkniNotas = wkniUi.wkniSecondary("notes", "Abrir notas");
        wkniNotas.setOnAction(wkniE -> { wkniPopup.close(); wkniAbrirNotas.run(); });
        Button wkniAgenda = wkniUi.wkniPrimary("agenda", "Continuar");
        wkniAgenda.setOnAction(wkniE -> { wkniPopup.close(); wkniAbrirAgenda.run(); });
        Button wkniDocenteBtn = null;
        if (wkniDocente) {
            wkniDocenteBtn = wkniUi.wkniSecondary("book", "Espacio Docente");
            wkniDocenteBtn.setOnAction(wkniE -> { wkniPopup.close(); wkniAbrirEspacioDocente.run(); });
        }
        HBox wkniAcciones = new HBox(10);
        wkniAcciones.getChildren().addAll(wkniNotas, wkniUi.wkniSpacer(), wkniAgenda);
        if (wkniDocente) wkniAcciones.getChildren().addAll(wkniUi.wkniSpacer(), wkniDocenteBtn);
        wkniAcciones.setAlignment(Pos.CENTER_LEFT);

        VBox wkniRaiz = new VBox(18, wkniEncabezado, wkniTarjeta, wkniAcciones);
        wkniRaiz.setPadding(new Insets(26));
        wkniRaiz.setPrefWidth(440);
        wkniRaiz.getStyleClass().add("page");
        Scene wkniScene = new Scene(wkniRaiz, 460, 380);
        wkniThemeService.wkniGet().wkniApplyTo(wkniScene);
        wkniPopup.setScene(wkniScene);
        wkniPopup.setResizable(false);
        wkniPopup.show();
        wkniPopup.toFront();
    }

    private static wkniRecuerdo wkniRecuerdo(wkniMainController wkniController) {
        List<wkniNota> wkniNotas;
        try { wkniNotas = new wkniNotaDAO().wkniListar(null); } catch (RuntimeException wkniE) { wkniNotas = List.of(); }
        wkniNota wkni_nota = wkniNotas.stream().max(Comparator.comparing(wkniBienvenidaInicioView::wkniFechaNota,
            Comparator.nullsFirst(Comparator.naturalOrder()))).orElse(null);
        if (wkni_nota != null) {
            String wkniDetalle = wkni_nota.wkniGetContenido() == null ? "" : wkni_nota.wkniGetContenido().replaceAll("\\s+", " ").trim();
            if (wkniDetalle.length() > 150) wkniDetalle = wkniDetalle.substring(0, 147) + "…";
            return new wkniRecuerdo("Una nota que guardaste", wkniDetalle.isBlank() ? wkni_nota.wkniGetTitulo() : wkni_nota.wkniGetTitulo() + " — " + wkniDetalle);
        }

        wkniTarea wkniCompletada = wkniController.wkniGetTareas().stream().filter(wkniTarea::wkniEstaCompletada)
            .max(Comparator.comparing(wkniTarea::wkniGetFecha, Comparator.nullsFirst(Comparator.naturalOrder()))).orElse(null);
        if (wkniCompletada != null) return new wkniRecuerdo("Un logro anterior", "Completaste: " + wkniCompletada.wkniGetTitulo());

        LocalDate wkniHoy = LocalDate.now();
        wkniEvento wkniAnterior = wkniController.wkniGetEventos().stream().filter(wkniE -> wkniE.wkniGetFecha() != null && wkniE.wkniGetFecha().isBefore(wkniHoy))
            .max(Comparator.comparing(wkniEvento::wkniGetFecha)).orElse(null);
        if (wkniAnterior != null) return new wkniRecuerdo("Un evento anterior", wkniAnterior.wkniGetFecha() + " · " + wkniAnterior.wkniGetTitulo());

        return new wkniRecuerdo("Tu primer recuerdo", "Cuando guardes una nota, completes una tarea o registres un evento, Compás te lo recordará aquí.");
    }

    private static LocalDateTime wkniFechaNota(wkniNota wkni_nota) {
        String wkniValor = wkni_nota.wkniGetFechaUpdate() == null || wkni_nota.wkniGetFechaUpdate().isBlank()
            ? wkni_nota.wkniGetFechaCreacion() : wkni_nota.wkniGetFechaUpdate();
        if (wkniValor == null || wkniValor.isBlank()) return null;
        try { return LocalDateTime.parse(wkniValor); }
        catch (DateTimeParseException wkniIgnored) {
            try { return LocalDate.parse(wkniValor).atStartOfDay(); }
            catch (DateTimeParseException wkniIgnoredAgain) { return null; }
        }
    }
}

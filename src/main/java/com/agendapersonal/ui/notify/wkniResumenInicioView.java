package com.agendapersonal.ui.notify;

import com.agendapersonal.domain.model.wkniHabito;
import com.agendapersonal.model.wkniEvento;
import com.agendapersonal.model.wkniTarea;
import com.agendapersonal.service.wkniContactoService;
import com.agendapersonal.service.wkniRecordatorioService.wkniResumen;
import com.agendapersonal.ui.wkniAnim;
import com.agendapersonal.ui.wkniLogo;
import com.agendapersonal.ui.wkniThemeService;
import com.agendapersonal.util.wkniUi;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Consumer;

/**
 * Ventana con el resumen del día (cumpleaños, eventos, tareas, hábitos). Usa los colores del tema
 * activo: antes tenía colores oscuros fijos y no cambiaba con la apariencia.
 */
public final class wkniResumenInicioView {
    private wkniResumenInicioView() {}

    public static void wkniMostrar(Window wkniDueno, wkniResumen wkniR, Runnable wkniIrAgenda, Consumer<Boolean> wkniAlCambiarNoMostrar, boolean wkniAlIniciar) {
        Stage wkniD = new Stage();
        if (wkniDueno != null) wkniD.initOwner(wkniDueno);
        wkniD.setTitle("Compás — Resumen de hoy");
        wkniLogo.wkniAplicarIconos(wkniD);

        Label wkniSaludo = wkniUi.wkniLabel(wkniUi.wkniFormatDateShort(wkniR.wkniHoy()), "h2");
        Label wkniSub = wkniUi.wkniMuted("Esto es lo que tienes para hoy");
        HBox wkniHead = new HBox(14, wkniLogo.wkniMark(40), new VBox(2, wkniSaludo, wkniSub));
        wkniHead.setAlignment(Pos.CENTER_LEFT);
        VBox wkniCuerpo = new VBox(18, wkniHead);
        wkniCuerpo.setPadding(new Insets(22));
        wkniCuerpo.getStyleClass().add("page");

        if (!wkniR.wkniCumples().isEmpty()) {
            VBox wkniS = wkniSeccion("Cumpleaños", "birthday", "rose");
            for (wkniContactoService.wkniCumple wkniC : wkniR.wkniCumples()) {
                String wkniCuando = wkniC.wkniEnDias() == 0 ? "¡Hoy!" : wkniC.wkniEnDias() == 1 ? "Mañana"
                    : "En " + wkniC.wkniEnDias() + " días (" + wkniC.wkni_fecha().format(DateTimeFormatter.ofPattern("d/MM")) + ")";
                String wkniEdad = wkniC.wkniEdad() > 0 ? " · cumple " + wkniC.wkniEdad() : "";
                wkniS.getChildren().add(wkniFila(wkniCuando, wkniC.wkni_contacto().wkniGetNombre() + wkniEdad, "rose", wkniC.wkniEnDias() == 0));
            }
            wkniCuerpo.getChildren().add(wkniS);
        }
        if (!wkniR.wkniEventosHoy().isEmpty()) {
            VBox wkniS = wkniSeccion("Eventos de hoy", "event", "teal");
            for (wkniEvento wkniE : wkniR.wkniEventosHoy()) wkniS.getChildren().add(wkniFilaEvento(wkniE));
            wkniCuerpo.getChildren().add(wkniS);
        }
        if (!wkniR.wkniEventosManana().isEmpty()) {
            VBox wkniS = wkniSeccion("Eventos de mañana", "calendar", "blue");
            for (wkniEvento wkniE : wkniR.wkniEventosManana()) wkniS.getChildren().add(wkniFilaEvento(wkniE));
            wkniCuerpo.getChildren().add(wkniS);
        }
        if (!wkniR.wkniTareasVencidas().isEmpty() || !wkniR.wkniTareasHoy().isEmpty()) {
            VBox wkniS = wkniSeccion("Tareas pendientes", "tasks", "violet");
            for (wkniTarea wkniT : wkniR.wkniTareasVencidas())
                wkniS.getChildren().add(wkniFila("Vencida " + wkniT.wkniGetFecha().format(DateTimeFormatter.ofPattern("d/MM")), wkniT.wkniGetTitulo(), "rose", false));
            for (wkniTarea wkniT : wkniR.wkniTareasHoy()) wkniS.getChildren().add(wkniFila("Para hoy", wkniT.wkniGetTitulo(), "violet", false));
            wkniCuerpo.getChildren().add(wkniS);
        }
        List<wkniHabito> wkniHab = wkniR.wkniHabitosPendientes();
        if (!wkniHab.isEmpty()) {
            VBox wkniS = wkniSeccion("Hábitos de hoy sin marcar", "habits", "green");
            int wkniN = 0;
            for (wkniHabito wkniH : wkniHab) {
                if (wkniN++ == 5) { wkniS.getChildren().add(wkniFila("", "… y " + (wkniHab.size() - 5) + " más", "green", false)); break; }
                wkniS.getChildren().add(wkniFila("Pendiente", wkniH.wkniGetNombre(), "green", false));
            }
            wkniCuerpo.getChildren().add(wkniS);
        }
        if (wkniCuerpo.getChildren().size() == 1)
            wkniCuerpo.getChildren().add(wkniUi.wkniEmpty("success", "Día despejado", "No hay pendientes para hoy. ¡Disfrútalo!"));

        ScrollPane wkniSp = new ScrollPane(wkniCuerpo);
        wkniSp.setFitToWidth(true);
        wkniSp.getStyleClass().add("page-scroll");
        VBox.setVgrow(wkniSp, Priority.ALWAYS);

        CheckBox wkniMostrarAlIniciar = new CheckBox("Mostrar al iniciar");
        wkniMostrarAlIniciar.setSelected(wkniAlIniciar);
        wkniMostrarAlIniciar.setOnAction(wkniE -> wkniAlCambiarNoMostrar.accept(!wkniMostrarAlIniciar.isSelected()));
        Button wkniAgenda = wkniUi.wkniPrimary("agenda", "Ver agenda");
        wkniAgenda.setOnAction(wkniE -> { wkniD.close(); wkniIrAgenda.run(); });
        Button wkniOk = wkniUi.wkniSecondary(null, "Entendido");
        wkniOk.setOnAction(wkniE -> wkniD.close());
        HBox wkniPie = new HBox(10, wkniMostrarAlIniciar, wkniUi.wkniSpacer(), wkniOk, wkniAgenda);
        wkniPie.setAlignment(Pos.CENTER_LEFT);
        wkniPie.getStyleClass().add("dialog-footer");

        VBox wkniRaiz = new VBox(wkniSp, wkniPie);
        wkniRaiz.getStyleClass().add("app-root");
        Scene wkniSc = new Scene(wkniRaiz, 500, 580);
        wkniThemeService.wkniGet().wkniApplyTo(wkniSc);
        wkniD.setScene(wkniSc);
        wkniD.show();
        wkniD.toFront();
        wkniAnim.wkniPageIn(wkniCuerpo);
    }

    private static VBox wkniSeccion(String wkniTitulo, String wkniIcon, String wkni_tono) {
        Label wkniT = wkniUi.wkniLabel(wkniTitulo, "h3");
        HBox wkniEncabezado = new HBox(10, wkniUi.wkniIconChip(wkniIcon, wkni_tono, 30, 16), wkniT);
        wkniEncabezado.setAlignment(Pos.CENTER_LEFT);
        VBox wkniS = new VBox(8, wkniEncabezado);
        return wkniS;
    }

    private static HBox wkniFila(String wkniCuando, String wkniTexto, String wkni_tono, boolean wkniDestacada) {
        Label wkniC = wkniUi.wkniLabel(wkniCuando, "summary-when", "tint-text-" + wkni_tono);
        wkniC.setMinWidth(130);
        Label wkniT = wkniUi.wkniLabel(wkniTexto, "row-title");
        wkniT.setWrapText(true);
        HBox wkniH = new HBox(10, wkniC, wkniT);
        wkniH.setAlignment(Pos.CENTER_LEFT);
        wkniH.getStyleClass().add(wkniDestacada ? "summary-row-hot" : "summary-row");
        if (wkniDestacada) wkniH.getStyleClass().add("chip-" + wkni_tono);
        return wkniH;
    }

    private static HBox wkniFilaEvento(wkniEvento wkniE) {
        return wkniFila(wkniE.wkniGetHoraInicio() + " – " + wkniE.wkniGetHoraFin(), wkniE.wkniGetTitulo(), "teal", false);
    }
}

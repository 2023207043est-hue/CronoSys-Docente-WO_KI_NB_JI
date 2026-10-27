package com.agendapersonal.ui.notify;

import com.agendapersonal.domain.model.WO_KI_NB_JI_Habito;
import com.agendapersonal.model.WO_KI_NB_JI_Evento;
import com.agendapersonal.model.WO_KI_NB_JI_Tarea;
import com.agendapersonal.service.WO_KI_NB_JI_ContactoService;
import com.agendapersonal.service.WO_KI_NB_JI_RecordatorioService.WO_KI_NB_JI_Resumen;
import com.agendapersonal.ui.WO_KI_NB_JI_Anim;
import com.agendapersonal.ui.WO_KI_NB_JI_Logo;
import com.agendapersonal.ui.WO_KI_NB_JI_ThemeService;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
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
public final class WO_KI_NB_JI_ResumenInicioView {
    private WO_KI_NB_JI_ResumenInicioView() {}

    public static void WO_KI_NB_JI_mostrar(Window dueno, WO_KI_NB_JI_Resumen r, Runnable irAgenda, Consumer<Boolean> alCambiarNoMostrar, boolean alIniciar) {
        Stage d = new Stage();
        if (dueno != null) d.initOwner(dueno);
        d.setTitle("Compás — Resumen de hoy");
        WO_KI_NB_JI_Logo.WO_KI_NB_JI_aplicarIconos(d);

        Label saludo = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(WO_KI_NB_JI_Ui.WO_KI_NB_JI_formatDateShort(r.hoy()), "h2");
        Label sub = WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Esto es lo que tienes para hoy");
        HBox head = new HBox(14, WO_KI_NB_JI_Logo.WO_KI_NB_JI_mark(40), new VBox(2, saludo, sub));
        head.setAlignment(Pos.CENTER_LEFT);
        VBox cuerpo = new VBox(18, head);
        cuerpo.setPadding(new Insets(22));
        cuerpo.getStyleClass().add("page");

        if (!r.cumples().isEmpty()) {
            VBox s = WO_KI_NB_JI_seccion("Cumpleaños", "birthday", "rose");
            for (WO_KI_NB_JI_ContactoService.WO_KI_NB_JI_Cumple c : r.cumples()) {
                String cuando = c.enDias() == 0 ? "¡Hoy!" : c.enDias() == 1 ? "Mañana"
                    : "En " + c.enDias() + " días (" + c.fecha().format(DateTimeFormatter.ofPattern("d/MM")) + ")";
                String edad = c.edad() > 0 ? " · cumple " + c.edad() : "";
                s.getChildren().add(WO_KI_NB_JI_fila(cuando, c.contacto().WO_KI_NB_JI_getNombre() + edad, "rose", c.enDias() == 0));
            }
            cuerpo.getChildren().add(s);
        }
        if (!r.eventosHoy().isEmpty()) {
            VBox s = WO_KI_NB_JI_seccion("Eventos de hoy", "event", "teal");
            for (WO_KI_NB_JI_Evento e : r.eventosHoy()) s.getChildren().add(WO_KI_NB_JI_filaEvento(e));
            cuerpo.getChildren().add(s);
        }
        if (!r.eventosManana().isEmpty()) {
            VBox s = WO_KI_NB_JI_seccion("Eventos de mañana", "calendar", "blue");
            for (WO_KI_NB_JI_Evento e : r.eventosManana()) s.getChildren().add(WO_KI_NB_JI_filaEvento(e));
            cuerpo.getChildren().add(s);
        }
        if (!r.tareasVencidas().isEmpty() || !r.tareasHoy().isEmpty()) {
            VBox s = WO_KI_NB_JI_seccion("Tareas pendientes", "tasks", "violet");
            for (WO_KI_NB_JI_Tarea t : r.tareasVencidas())
                s.getChildren().add(WO_KI_NB_JI_fila("Vencida " + t.WO_KI_NB_JI_getFecha().format(DateTimeFormatter.ofPattern("d/MM")), t.WO_KI_NB_JI_getTitulo(), "rose", false));
            for (WO_KI_NB_JI_Tarea t : r.tareasHoy()) s.getChildren().add(WO_KI_NB_JI_fila("Para hoy", t.WO_KI_NB_JI_getTitulo(), "violet", false));
            cuerpo.getChildren().add(s);
        }
        List<WO_KI_NB_JI_Habito> hab = r.habitosPendientes();
        if (!hab.isEmpty()) {
            VBox s = WO_KI_NB_JI_seccion("Hábitos de hoy sin marcar", "habits", "green");
            int n = 0;
            for (WO_KI_NB_JI_Habito h : hab) {
                if (n++ == 5) { s.getChildren().add(WO_KI_NB_JI_fila("", "… y " + (hab.size() - 5) + " más", "green", false)); break; }
                s.getChildren().add(WO_KI_NB_JI_fila("Pendiente", h.WO_KI_NB_JI_getNombre(), "green", false));
            }
            cuerpo.getChildren().add(s);
        }
        if (cuerpo.getChildren().size() == 1)
            cuerpo.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_empty("success", "Día despejado", "No hay pendientes para hoy. ¡Disfrútalo!"));

        ScrollPane sp = new ScrollPane(cuerpo);
        sp.setFitToWidth(true);
        sp.getStyleClass().add("page-scroll");
        VBox.setVgrow(sp, Priority.ALWAYS);

        CheckBox mostrarAlIniciar = new CheckBox("Mostrar al iniciar");
        mostrarAlIniciar.setSelected(alIniciar);
        mostrarAlIniciar.setOnAction(e -> alCambiarNoMostrar.accept(!mostrarAlIniciar.isSelected()));
        Button agenda = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("agenda", "Ver agenda");
        agenda.setOnAction(e -> { d.close(); irAgenda.run(); });
        Button ok = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Entendido");
        ok.setOnAction(e -> d.close());
        HBox pie = new HBox(10, mostrarAlIniciar, WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), ok, agenda);
        pie.setAlignment(Pos.CENTER_LEFT);
        pie.getStyleClass().add("dialog-footer");

        VBox raiz = new VBox(sp, pie);
        raiz.getStyleClass().add("app-root");
        Scene sc = new Scene(raiz, 500, 580);
        WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_get().WO_KI_NB_JI_applyTo(sc);
        d.setScene(sc);
        d.show();
        d.toFront();
        WO_KI_NB_JI_Anim.WO_KI_NB_JI_pageIn(cuerpo);
    }

    private static VBox WO_KI_NB_JI_seccion(String titulo, String icon, String tono) {
        Label t = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(titulo, "h3");
        HBox encabezado = new HBox(10, WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconChip(icon, tono, 30, 16), t);
        encabezado.setAlignment(Pos.CENTER_LEFT);
        VBox s = new VBox(8, encabezado);
        return s;
    }

    private static HBox WO_KI_NB_JI_fila(String cuando, String texto, String tono, boolean destacada) {
        Label c = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(cuando, "summary-when", "tint-text-" + tono);
        c.setMinWidth(130);
        Label t = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(texto, "row-title");
        t.setWrapText(true);
        HBox h = new HBox(10, c, t);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add(destacada ? "summary-row-hot" : "summary-row");
        if (destacada) h.getStyleClass().add("chip-" + tono);
        return h;
    }

    private static HBox WO_KI_NB_JI_filaEvento(WO_KI_NB_JI_Evento e) {
        return WO_KI_NB_JI_fila(e.WO_KI_NB_JI_getHoraInicio() + " – " + e.WO_KI_NB_JI_getHoraFin(), e.WO_KI_NB_JI_getTitulo(), "teal", false);
    }
}

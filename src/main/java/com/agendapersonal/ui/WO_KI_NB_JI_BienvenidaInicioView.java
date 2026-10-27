package com.agendapersonal.ui;

import com.agendapersonal.controller.WO_KI_NB_JI_MainController;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Nota;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_NotaDAO;
import com.agendapersonal.model.WO_KI_NB_JI_Evento;
import com.agendapersonal.model.WO_KI_NB_JI_Tarea;
import com.agendapersonal.util.WO_KI_NB_JI_Perfil;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
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
public final class WO_KI_NB_JI_BienvenidaInicioView {
    private record WO_KI_NB_JI_Recuerdo(String etiqueta, String texto) {}

    private WO_KI_NB_JI_BienvenidaInicioView() {}

    public static void WO_KI_NB_JI_mostrar(Window owner, WO_KI_NB_JI_MainController controller, Runnable abrirAgenda, Runnable abrirNotas, Runnable abrirEspacioDocente) {
        Stage popup = new Stage();
        if (owner != null && owner.isShowing()) popup.initOwner(owner);
        popup.setTitle("Compás — Bienvenida");
        WO_KI_NB_JI_Logo.WO_KI_NB_JI_aplicarIconos(popup);

        boolean docente = com.agendapersonal.util.WO_KI_NB_JI_Perfil.WO_KI_NB_JI_esDocente();
        Label saludo = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(WO_KI_NB_JI_Perfil.WO_KI_NB_JI_saludo(LocalTime.now().getHour(), WO_KI_NB_JI_Perfil.WO_KI_NB_JI_nombre()) + " 👋", "h1");
        Label sub = WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted(docente
            ? "Qué bueno verte. Aquí va un pequeño recuerdo de tu espacio y lo que prepara tu docente hoy."
            : "Qué bueno verte. Este es un pequeño recuerdo de tu espacio.");
        sub.setWrapText(true);
        VBox encabezado = new VBox(5, WO_KI_NB_JI_Logo.WO_KI_NB_JI_mark(48), saludo, sub);
        encabezado.setAlignment(Pos.CENTER);

        WO_KI_NB_JI_Recuerdo recuerdo = WO_KI_NB_JI_recuerdo(controller);
        Label etiqueta = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(recuerdo.etiqueta(), "caption");
        Label texto = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(recuerdo.texto(), "h3");
        texto.setWrapText(true);
        VBox tarjeta = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        tarjeta.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconChip("sparkle", "orange", 34, 18), etiqueta, texto);

        Button notas = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("notes", "Abrir notas");
        notas.setOnAction(e -> { popup.close(); abrirNotas.run(); });
        Button agenda = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("agenda", "Continuar");
        agenda.setOnAction(e -> { popup.close(); abrirAgenda.run(); });
        Button docenteBtn = null;
        if (docente) {
            docenteBtn = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("book", "Espacio Docente");
            docenteBtn.setOnAction(e -> { popup.close(); abrirEspacioDocente.run(); });
        }
        HBox acciones = new HBox(10);
        acciones.getChildren().addAll(notas, WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), agenda);
        if (docente) acciones.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), docenteBtn);
        acciones.setAlignment(Pos.CENTER_LEFT);

        VBox raiz = new VBox(18, encabezado, tarjeta, acciones);
        raiz.setPadding(new Insets(26));
        raiz.setPrefWidth(440);
        raiz.getStyleClass().add("page");
        Scene scene = new Scene(raiz, 460, 380);
        WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_get().WO_KI_NB_JI_applyTo(scene);
        popup.setScene(scene);
        popup.setResizable(false);
        popup.show();
        popup.toFront();
    }

    private static WO_KI_NB_JI_Recuerdo WO_KI_NB_JI_recuerdo(WO_KI_NB_JI_MainController controller) {
        List<WO_KI_NB_JI_Nota> notas;
        try { notas = new WO_KI_NB_JI_NotaDAO().WO_KI_NB_JI_listar(null); } catch (RuntimeException e) { notas = List.of(); }
        WO_KI_NB_JI_Nota nota = notas.stream().max(Comparator.comparing(WO_KI_NB_JI_BienvenidaInicioView::WO_KI_NB_JI_fechaNota,
            Comparator.nullsFirst(Comparator.naturalOrder()))).orElse(null);
        if (nota != null) {
            String detalle = nota.WO_KI_NB_JI_getContenido() == null ? "" : nota.WO_KI_NB_JI_getContenido().replaceAll("\\s+", " ").trim();
            if (detalle.length() > 150) detalle = detalle.substring(0, 147) + "…";
            return new WO_KI_NB_JI_Recuerdo("Una nota que guardaste", detalle.isBlank() ? nota.WO_KI_NB_JI_getTitulo() : nota.WO_KI_NB_JI_getTitulo() + " — " + detalle);
        }

        WO_KI_NB_JI_Tarea completada = controller.WO_KI_NB_JI_getTareas().stream().filter(WO_KI_NB_JI_Tarea::WO_KI_NB_JI_estaCompletada)
            .max(Comparator.comparing(WO_KI_NB_JI_Tarea::WO_KI_NB_JI_getFecha, Comparator.nullsFirst(Comparator.naturalOrder()))).orElse(null);
        if (completada != null) return new WO_KI_NB_JI_Recuerdo("Un logro anterior", "Completaste: " + completada.WO_KI_NB_JI_getTitulo());

        LocalDate hoy = LocalDate.now();
        WO_KI_NB_JI_Evento anterior = controller.WO_KI_NB_JI_getEventos().stream().filter(e -> e.WO_KI_NB_JI_getFecha() != null && e.WO_KI_NB_JI_getFecha().isBefore(hoy))
            .max(Comparator.comparing(WO_KI_NB_JI_Evento::WO_KI_NB_JI_getFecha)).orElse(null);
        if (anterior != null) return new WO_KI_NB_JI_Recuerdo("Un evento anterior", anterior.WO_KI_NB_JI_getFecha() + " · " + anterior.WO_KI_NB_JI_getTitulo());

        return new WO_KI_NB_JI_Recuerdo("Tu primer recuerdo", "Cuando guardes una nota, completes una tarea o registres un evento, Compás te lo recordará aquí.");
    }

    private static LocalDateTime WO_KI_NB_JI_fechaNota(WO_KI_NB_JI_Nota nota) {
        String valor = nota.WO_KI_NB_JI_getFechaUpdate() == null || nota.WO_KI_NB_JI_getFechaUpdate().isBlank()
            ? nota.WO_KI_NB_JI_getFechaCreacion() : nota.WO_KI_NB_JI_getFechaUpdate();
        if (valor == null || valor.isBlank()) return null;
        try { return LocalDateTime.parse(valor); }
        catch (DateTimeParseException ignored) {
            try { return LocalDate.parse(valor).atStartOfDay(); }
            catch (DateTimeParseException ignoredAgain) { return null; }
        }
    }
}

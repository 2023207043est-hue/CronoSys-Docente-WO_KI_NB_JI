package com.agendapersonal.ui.modules.docente;

import com.agendapersonal.domain.model.WO_KI_NB_JI_EstudianteClase;
import com.agendapersonal.domain.model.WO_KI_NB_JI_RecursoClase;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Aula virtual del docente para cada curso: enlace de la clase en vivo (Meet, Zoom, Teams), tablón de
 * anuncios y biblioteca de recursos (enlaces web y archivos del equipo). Todo queda guardado localmente;
 * los anuncios se copian o se envían por correo a la lista del curso con un clic.
 */
final class WO_KI_NB_JI_AulaVirtualPanel {
    private final WO_KI_NB_JI_CursoContexto ctx;
    private final VBox contenido = new VBox(18);
    private final ScrollPane root = WO_KI_NB_JI_Ui.WO_KI_NB_JI_scroll(contenido);

    WO_KI_NB_JI_AulaVirtualPanel(WO_KI_NB_JI_CursoContexto ctx) {
        this.ctx = ctx;
        contenido.setPadding(new Insets(16, 6, 16, 4));
        WO_KI_NB_JI_recargar();
    }

    Node WO_KI_NB_JI_getView() { return root; }

    private void WO_KI_NB_JI_recargar() {
        List<WO_KI_NB_JI_RecursoClase> todos = ctx.dao().WO_KI_NB_JI_listarRecursos(ctx.clase().id());
        contenido.getChildren().setAll(WO_KI_NB_JI_claseEnVivo(todos), WO_KI_NB_JI_columnas(todos));
    }

    // ------------------------------------------------------------------ clase en vivo

    private Node WO_KI_NB_JI_claseEnVivo(List<WO_KI_NB_JI_RecursoClase> todos) {
        WO_KI_NB_JI_RecursoClase vivo = todos.stream().filter(r -> WO_KI_NB_JI_RecursoClase.CLASE_EN_VIVO.equals(r.tipo())).findFirst().orElse(null);
        VBox texto = new VBox(4, WO_KI_NB_JI_Ui.WO_KI_NB_JI_overline("CLASE EN VIVO"));
        HBox acciones = new HBox(10);
        if (vivo == null) {
            texto.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h2("Configura el enlace de tu clase virtual"),
                WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Pega el enlace fijo de Google Meet, Zoom o Teams y entra a tu clase con un clic."));
            Button config = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("link", "Agregar enlace de clase");
            config.setOnAction(e -> WO_KI_NB_JI_dialogoRecurso(WO_KI_NB_JI_RecursoClase.CLASE_EN_VIVO, null));
            acciones.getChildren().add(config);
        } else {
            Label url = WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted(vivo.contenido());
            texto.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h2(vivo.titulo()), url);
            Button entrar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("video", "Entrar a la clase");
            entrar.setOnAction(e -> WO_KI_NB_JI_abrir(vivo.contenido()));
            Button copiar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("copy", "Copiar enlace");
            copiar.setOnAction(e -> WO_KI_NB_JI_copiar(vivo.contenido(), "Enlace copiado. Pégalo en WhatsApp o en el correo del curso."));
            Button cambiar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_ghost("edit", "Cambiar");
            cambiar.setOnAction(e -> WO_KI_NB_JI_dialogoRecurso(WO_KI_NB_JI_RecursoClase.CLASE_EN_VIVO, vivo));
            acciones.getChildren().addAll(entrar, copiar, cambiar);
        }
        HBox.setHgrow(texto, Priority.ALWAYS);
        HBox card = new HBox(18, WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconChip("video", "orange", 56, 26), texto, acciones);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("aula-vivo");
        return card;
    }

    // ------------------------------------------------------------------ anuncios y recursos

    private Node WO_KI_NB_JI_columnas(List<WO_KI_NB_JI_RecursoClase> todos) {
        VBox anuncios = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        Button publicar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("megaphone", "Publicar anuncio");
        publicar.setOnAction(e -> WO_KI_NB_JI_dialogoRecurso(WO_KI_NB_JI_RecursoClase.ANUNCIO, null));
        anuncios.getChildren().add(new HBox(10, new VBox(2, WO_KI_NB_JI_Ui.WO_KI_NB_JI_h3("Tablón de anuncios"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_caption("Avisos para el curso: fechas, cambios de aula, recordatorios.")), WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), publicar));
        List<WO_KI_NB_JI_RecursoClase> lista = todos.stream().filter(r -> WO_KI_NB_JI_RecursoClase.ANUNCIO.equals(r.tipo())).toList();
        if (lista.isEmpty()) anuncios.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_empty("megaphone", "Sin anuncios", "Publica el primero para tenerlo a mano."));
        for (WO_KI_NB_JI_RecursoClase r : lista) anuncios.getChildren().add(WO_KI_NB_JI_anuncio(r));

        VBox recursos = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        Button enlace = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("link", "Enlace");
        enlace.setOnAction(e -> WO_KI_NB_JI_dialogoRecurso(WO_KI_NB_JI_RecursoClase.ENLACE, null));
        Button archivo = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("folder", "Archivo");
        archivo.setOnAction(e -> WO_KI_NB_JI_agregarArchivo());
        recursos.getChildren().add(new HBox(8, new VBox(2, WO_KI_NB_JI_Ui.WO_KI_NB_JI_h3("Recursos y materiales"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_caption("Diapositivas, separatas, videos, carpetas de Drive.")), WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), enlace, archivo));
        List<WO_KI_NB_JI_RecursoClase> mats = todos.stream()
            .filter(r -> WO_KI_NB_JI_RecursoClase.ENLACE.equals(r.tipo()) || WO_KI_NB_JI_RecursoClase.ARCHIVO.equals(r.tipo())).toList();
        if (mats.isEmpty()) recursos.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_empty("folder", "Sin recursos", "Agrega enlaces o archivos de tu computadora."));
        for (WO_KI_NB_JI_RecursoClase r : mats) recursos.getChildren().add(WO_KI_NB_JI_recurso(r));

        HBox.setHgrow(anuncios, Priority.ALWAYS);
        HBox.setHgrow(recursos, Priority.ALWAYS);
        anuncios.setPrefWidth(500);
        recursos.setPrefWidth(440);
        HBox h = new HBox(16, anuncios, recursos);
        h.setAlignment(Pos.TOP_LEFT);
        return h;
    }

    private Node WO_KI_NB_JI_anuncio(WO_KI_NB_JI_RecursoClase r) {
        Label t = WO_KI_NB_JI_Ui.WO_KI_NB_JI_h3(r.titulo());
        Label fecha = WO_KI_NB_JI_Ui.WO_KI_NB_JI_caption(WO_KI_NB_JI_Ui.WO_KI_NB_JI_formatDateShort(r.fecha()) + (r.fijado() ? " · 📌 fijado" : ""));
        Label cuerpo = new Label(r.contenido() == null ? "" : r.contenido());
        cuerpo.setWrapText(true);
        Button fijar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton(r.fijado() ? "favorite-on" : "favorite", r.fijado() ? "Desfijar" : "Fijar arriba");
        fijar.setOnAction(e -> { ctx.dao().WO_KI_NB_JI_fijarRecurso(r.id(), !r.fijado()); WO_KI_NB_JI_recargar(); });
        Button copiar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("copy", "Copiar texto (para WhatsApp)");
        copiar.setOnAction(e -> WO_KI_NB_JI_copiar(WO_KI_NB_JI_textoAnuncio(r), "Anuncio copiado. Pégalo en el grupo de WhatsApp del curso."));
        Button correo = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("mail", "Enviar por correo a los estudiantes");
        correo.setOnAction(e -> WO_KI_NB_JI_enviarCorreo(r));
        Button borrar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("delete", "Eliminar");
        borrar.setOnAction(e -> WO_KI_NB_JI_eliminar(r));
        HBox top = new HBox(8, new VBox(2, t, fecha), WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), fijar, copiar, correo, borrar);
        top.setAlignment(Pos.TOP_LEFT);
        VBox v = new VBox(8, top, cuerpo);
        v.getStyleClass().add("anuncio-card");
        if (r.fijado()) v.getStyleClass().add("anuncio-fijado");
        return v;
    }

    private Node WO_KI_NB_JI_recurso(WO_KI_NB_JI_RecursoClase r) {
        boolean esArchivo = WO_KI_NB_JI_RecursoClase.ARCHIVO.equals(r.tipo());
        Label t = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(r.titulo(), "body");
        t.setStyle("-fx-font-weight: bold;");
        String detalle = esArchivo ? new File(Objects.requireNonNullElse(r.contenido(), "")).getName() : r.contenido();
        Label d = WO_KI_NB_JI_Ui.WO_KI_NB_JI_caption(detalle);
        Button abrir = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(esArchivo ? "folder" : "arrow-up-right", "Abrir");
        abrir.setOnAction(e -> WO_KI_NB_JI_abrir(esArchivo ? new File(r.contenido()).toURI().toString() : r.contenido()));
        Button copiar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("copy", "Copiar");
        copiar.setOnAction(e -> WO_KI_NB_JI_copiar(r.contenido(), "Copiado al portapapeles."));
        Button borrar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("delete", "Quitar");
        borrar.setOnAction(e -> WO_KI_NB_JI_eliminar(r));
        VBox texto = new VBox(2, t, d);
        HBox.setHgrow(texto, Priority.ALWAYS);
        HBox h = new HBox(10, WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconChip(esArchivo ? "folder" : "link", esArchivo ? "amber" : "blue", 36, 18), texto, abrir, copiar, borrar);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().add("anuncio-card");
        return h;
    }

    // ------------------------------------------------------------------ acciones

    private void WO_KI_NB_JI_dialogoRecurso(String tipo, WO_KI_NB_JI_RecursoClase existente) {
        boolean esAnuncio = WO_KI_NB_JI_RecursoClase.ANUNCIO.equals(tipo);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Aula virtual");
        dialog.setHeaderText(esAnuncio ? "Nuevo anuncio para " + ctx.clase()
            : WO_KI_NB_JI_RecursoClase.CLASE_EN_VIVO.equals(tipo) ? "Enlace de la clase en vivo" : "Nuevo enlace");
        if (WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_ventana(root) != null) dialog.initOwner(WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_ventana(root));
        ButtonType ok = new ButtonType(esAnuncio ? "Publicar" : "Guardar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(ok, ButtonType.CANCEL);
        TextField titulo = new TextField(existente == null
            ? (WO_KI_NB_JI_RecursoClase.CLASE_EN_VIVO.equals(tipo) ? "Clase virtual" : "") : existente.titulo());
        titulo.setPromptText(esAnuncio ? "Título del anuncio" : "Nombre (p. ej. Diapositivas unidad 1)");
        TextArea cuerpo = new TextArea(existente == null ? "" : Objects.requireNonNullElse(existente.contenido(), ""));
        TextField url = new TextField(existente == null ? "" : Objects.requireNonNullElse(existente.contenido(), ""));
        url.setPromptText("https://meet.google.com/abc-defg-hij");
        CheckBox fijar = new CheckBox("Fijar arriba");
        VBox form = new VBox(8, WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Título", titulo));
        if (esAnuncio) {
            cuerpo.setPromptText("Escribe el anuncio…");
            cuerpo.setPrefRowCount(5);
            cuerpo.setWrapText(true);
            form.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Mensaje", cuerpo), fijar);
        } else form.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Enlace", url));
        form.setPadding(new Insets(8, 4, 4, 4));
        form.setPrefWidth(460);
        dialog.getDialogPane().setContent(form);
        if (dialog.showAndWait().filter(b -> b == ok).isEmpty()) return;

        String contenido = esAnuncio ? cuerpo.getText() : WO_KI_NB_JI_normalizarUrl(url.getText());
        if (titulo.getText().isBlank()) { WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_info(root, "Escribe un título."); return; }
        if (!esAnuncio && contenido == null) { WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_info(root, "Escribe un enlace válido (por ejemplo https://meet.google.com/…)."); return; }
        if (existente != null) ctx.dao().WO_KI_NB_JI_eliminarRecurso(existente.id());
        boolean fijado = esAnuncio ? fijar.isSelected() : WO_KI_NB_JI_RecursoClase.CLASE_EN_VIVO.equals(tipo);
        if (!ctx.dao().WO_KI_NB_JI_crearRecurso(ctx.clase().id(), tipo, titulo.getText(), contenido, LocalDate.now(), fijado))
            WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_error(root, "No se pudo guardar.");
        WO_KI_NB_JI_recargar();
    }

    private void WO_KI_NB_JI_agregarArchivo() {
        FileChooser fc = new FileChooser();
        fc.setTitle("Agregar archivo al aula virtual");
        File f = fc.showOpenDialog(WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_ventana(root));
        if (f == null) return;
        String nombre = f.getName().contains(".") ? f.getName().substring(0, f.getName().lastIndexOf('.')) : f.getName();
        ctx.dao().WO_KI_NB_JI_crearRecurso(ctx.clase().id(), WO_KI_NB_JI_RecursoClase.ARCHIVO, nombre, f.getAbsolutePath(), LocalDate.now(), false);
        WO_KI_NB_JI_recargar();
    }

    private void WO_KI_NB_JI_eliminar(WO_KI_NB_JI_RecursoClase r) {
        if (WO_KI_NB_JI_Ui.WO_KI_NB_JI_confirm(root, "¿Eliminar «" + r.titulo() + "»?", "Se quitará del aula virtual de este curso"
            + (WO_KI_NB_JI_RecursoClase.ARCHIVO.equals(r.tipo()) ? " (el archivo de tu computadora no se borra)." : "."), "Eliminar")) {
            ctx.dao().WO_KI_NB_JI_eliminarRecurso(r.id());
            WO_KI_NB_JI_recargar();
        }
    }

    private String WO_KI_NB_JI_textoAnuncio(WO_KI_NB_JI_RecursoClase r) {
        return "📢 " + r.titulo() + " — " + ctx.clase() + "\n\n" + Objects.requireNonNullElse(r.contenido(), "");
    }

    /** Abre el programa de correo con el anuncio y los correos del curso en copia oculta. */
    private void WO_KI_NB_JI_enviarCorreo(WO_KI_NB_JI_RecursoClase r) {
        List<WO_KI_NB_JI_EstudianteClase> est = ctx.dao().WO_KI_NB_JI_listarEstudiantes(ctx.clase().id());
        String correos = est.stream().map(WO_KI_NB_JI_EstudianteClase::correo).filter(c -> c != null && c.contains("@"))
            .collect(Collectors.joining(","));
        if (correos.isEmpty()) { WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_info(root, "Ningún estudiante de este curso tiene correo registrado."); return; }
        String asunto = URLEncoder.encode(r.titulo() + " — " + ctx.clase(), StandardCharsets.UTF_8).replace("+", "%20");
        String cuerpo = URLEncoder.encode(Objects.requireNonNullElse(r.contenido(), ""), StandardCharsets.UTF_8).replace("+", "%20");
        WO_KI_NB_JI_abrir("mailto:?bcc=" + correos + "&subject=" + asunto + "&body=" + cuerpo);
    }

    private void WO_KI_NB_JI_abrir(String uri) {
        if (uri == null || uri.isBlank()) return;
        com.agendapersonal.WO_KI_NB_JI_Main.WO_KI_NB_JI_abrirEnSistema(uri);
    }

    private void WO_KI_NB_JI_copiar(String texto, String aviso) {
        ClipboardContent c = new ClipboardContent();
        c.putString(texto == null ? "" : texto);
        Clipboard.getSystemClipboard().setContent(c);
        com.agendapersonal.ui.WO_KI_NB_JI_Toast.WO_KI_NB_JI_mostrar(root, aviso);
    }

    /** Acepta "meet.google.com/x" y le agrega https://; null si está vacío o no parece un enlace. */
    static String WO_KI_NB_JI_normalizarUrl(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        if (t.contains(" ") || !t.contains(".")) return null;
        return t.matches("(?i)^[a-z][a-z0-9+.-]*://.*") ? t : "https://" + t;
    }
}

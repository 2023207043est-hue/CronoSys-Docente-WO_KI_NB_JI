package com.agendapersonal.ui.modules.docente;

import com.agendapersonal.domain.model.wkniEstudianteClase;
import com.agendapersonal.domain.model.wkniRecursoClase;
import com.agendapersonal.util.wkniUi;
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
final class wkniAulaVirtualPanel {
    private final wkniCursoContexto wkniCtx;
    private final VBox wkniContenido = new VBox(18);
    private final ScrollPane wkniRoot = wkniUi.wkniScroll(wkniContenido);

    wkniAulaVirtualPanel(wkniCursoContexto wkniCtx) {
        this.wkniCtx = wkniCtx;
        wkniContenido.setPadding(new Insets(16, 6, 16, 4));
        wkniRecargar();
    }

    Node wkniGetView() { return wkniRoot; }

    private void wkniRecargar() {
        List<wkniRecursoClase> wkniTodos = wkniCtx.wkniDao().wkniListarRecursos(wkniCtx.wkniClase().wkniId());
        wkniContenido.getChildren().setAll(wkniClaseEnVivo(wkniTodos), wkniColumnas(wkniTodos));
    }

    // ------------------------------------------------------------------ clase en vivo

    private Node wkniClaseEnVivo(List<wkniRecursoClase> wkniTodos) {
        wkniRecursoClase wkniVivo = wkniTodos.stream().filter(wkniR -> wkniRecursoClase.wkniCLASE_EN_VIVO.equals(wkniR.wkni_tipo())).findFirst().orElse(null);
        VBox wkniTexto = new VBox(4, wkniUi.wkniOverline("CLASE EN VIVO"));
        HBox wkniAcciones = new HBox(10);
        if (wkniVivo == null) {
            wkniTexto.getChildren().addAll(wkniUi.wkniH2("Configura el enlace de tu clase virtual"),
                wkniUi.wkniMuted("Pega el enlace fijo de Google Meet, Zoom o Teams y entra a tu clase con un clic."));
            Button wkniConfig = wkniUi.wkniPrimary("link", "Agregar enlace de clase");
            wkniConfig.setOnAction(wkniE -> wkniDialogoRecurso(wkniRecursoClase.wkniCLASE_EN_VIVO, null));
            wkniAcciones.getChildren().add(wkniConfig);
        } else {
            Label wkniUrl = wkniUi.wkniMuted(wkniVivo.wkniContenido());
            wkniTexto.getChildren().addAll(wkniUi.wkniH2(wkniVivo.wkniTitulo()), wkniUrl);
            Button wkniEntrar = wkniUi.wkniPrimary("video", "Entrar a la clase");
            wkniEntrar.setOnAction(wkniE -> wkniAbrir(wkniVivo.wkniContenido()));
            Button wkniCopiar = wkniUi.wkniSecondary("copy", "Copiar enlace");
            wkniCopiar.setOnAction(wkniE -> wkniCopiar(wkniVivo.wkniContenido(), "Enlace copiado. Pégalo en WhatsApp o en el correo del curso."));
            Button wkniCambiar = wkniUi.wkniGhost("edit", "Cambiar");
            wkniCambiar.setOnAction(wkniE -> wkniDialogoRecurso(wkniRecursoClase.wkniCLASE_EN_VIVO, wkniVivo));
            wkniAcciones.getChildren().addAll(wkniEntrar, wkniCopiar, wkniCambiar);
        }
        HBox.setHgrow(wkniTexto, Priority.ALWAYS);
        HBox wkniCard = new HBox(18, wkniUi.wkniIconChip("video", "orange", 56, 26), wkniTexto, wkniAcciones);
        wkniCard.setAlignment(Pos.CENTER_LEFT);
        wkniCard.getStyleClass().add("aula-vivo");
        return wkniCard;
    }

    // ------------------------------------------------------------------ anuncios y recursos

    private Node wkniColumnas(List<wkniRecursoClase> wkniTodos) {
        VBox wkniAnuncios = wkniUi.wkniCard();
        Button wkniPublicar = wkniUi.wkniPrimary("megaphone", "Publicar anuncio");
        wkniPublicar.setOnAction(wkniE -> wkniDialogoRecurso(wkniRecursoClase.wkniANUNCIO, null));
        wkniAnuncios.getChildren().add(new HBox(10, new VBox(2, wkniUi.wkniH3("Tablón de anuncios"),
            wkniUi.wkniCaption("Avisos para el curso: fechas, cambios de aula, recordatorios.")), wkniUi.wkniSpacer(), wkniPublicar));
        List<wkniRecursoClase> wkniLista = wkniTodos.stream().filter(wkniR -> wkniRecursoClase.wkniANUNCIO.equals(wkniR.wkni_tipo())).toList();
        if (wkniLista.isEmpty()) wkniAnuncios.getChildren().add(wkniUi.wkniEmpty("megaphone", "Sin anuncios", "Publica el primero para tenerlo a mano."));
        for (wkniRecursoClase wkniR : wkniLista) wkniAnuncios.getChildren().add(wkniAnuncio(wkniR));

        VBox wkniRecursos = wkniUi.wkniCard();
        Button wkniEnlace = wkniUi.wkniSecondary("link", "Enlace");
        wkniEnlace.setOnAction(wkniE -> wkniDialogoRecurso(wkniRecursoClase.wkniENLACE, null));
        Button wkniArchivo = wkniUi.wkniSecondary("folder", "Archivo");
        wkniArchivo.setOnAction(wkniE -> wkniAgregarArchivo());
        wkniRecursos.getChildren().add(new HBox(8, new VBox(2, wkniUi.wkniH3("Recursos y materiales"),
            wkniUi.wkniCaption("Diapositivas, separatas, videos, carpetas de Drive.")), wkniUi.wkniSpacer(), wkniEnlace, wkniArchivo));
        List<wkniRecursoClase> wkniMats = wkniTodos.stream()
            .filter(wkniR -> wkniRecursoClase.wkniENLACE.equals(wkniR.wkni_tipo()) || wkniRecursoClase.wkniARCHIVO.equals(wkniR.wkni_tipo())).toList();
        if (wkniMats.isEmpty()) wkniRecursos.getChildren().add(wkniUi.wkniEmpty("folder", "Sin recursos", "Agrega enlaces o archivos de tu computadora."));
        for (wkniRecursoClase wkniR : wkniMats) wkniRecursos.getChildren().add(wkniRecurso(wkniR));

        HBox.setHgrow(wkniAnuncios, Priority.ALWAYS);
        HBox.setHgrow(wkniRecursos, Priority.ALWAYS);
        wkniAnuncios.setPrefWidth(500);
        wkniRecursos.setPrefWidth(440);
        HBox wkniH = new HBox(16, wkniAnuncios, wkniRecursos);
        wkniH.setAlignment(Pos.TOP_LEFT);
        return wkniH;
    }

    private Node wkniAnuncio(wkniRecursoClase wkniR) {
        Label wkniT = wkniUi.wkniH3(wkniR.wkniTitulo());
        Label wkni_fecha = wkniUi.wkniCaption(wkniUi.wkniFormatDateShort(wkniR.wkni_fecha()) + (wkniR.wkniFijado() ? " · 📌 fijado" : ""));
        Label wkniCuerpo = new Label(wkniR.wkniContenido() == null ? "" : wkniR.wkniContenido());
        wkniCuerpo.setWrapText(true);
        Button wkniFijar = wkniUi.wkniIconButton(wkniR.wkniFijado() ? "favorite-on" : "favorite", wkniR.wkniFijado() ? "Desfijar" : "Fijar arriba");
        wkniFijar.setOnAction(wkniE -> { wkniCtx.wkniDao().wkniFijarRecurso(wkniR.wkniId(), !wkniR.wkniFijado()); wkniRecargar(); });
        Button wkniCopiar = wkniUi.wkniIconButton("copy", "Copiar texto (para WhatsApp)");
        wkniCopiar.setOnAction(wkniE -> wkniCopiar(wkniTextoAnuncio(wkniR), "Anuncio copiado. Pégalo en el grupo de WhatsApp del curso."));
        Button wkniCorreo = wkniUi.wkniIconButton("mail", "Enviar por correo a los estudiantes");
        wkniCorreo.setOnAction(wkniE -> wkniEnviarCorreo(wkniR));
        Button wkniBorrar = wkniUi.wkniIconButton("delete", "Eliminar");
        wkniBorrar.setOnAction(wkniE -> wkniEliminar(wkniR));
        HBox wkniTop = new HBox(8, new VBox(2, wkniT, wkni_fecha), wkniUi.wkniSpacer(), wkniFijar, wkniCopiar, wkniCorreo, wkniBorrar);
        wkniTop.setAlignment(Pos.TOP_LEFT);
        VBox wkniV = new VBox(8, wkniTop, wkniCuerpo);
        wkniV.getStyleClass().add("anuncio-card");
        if (wkniR.wkniFijado()) wkniV.getStyleClass().add("anuncio-fijado");
        return wkniV;
    }

    private Node wkniRecurso(wkniRecursoClase wkniR) {
        boolean wkniEsArchivo = wkniRecursoClase.wkniARCHIVO.equals(wkniR.wkni_tipo());
        Label wkniT = wkniUi.wkniLabel(wkniR.wkniTitulo(), "body");
        wkniT.setStyle("-fx-font-weight: bold;");
        String wkniDetalle = wkniEsArchivo ? new File(Objects.requireNonNullElse(wkniR.wkniContenido(), "")).getName() : wkniR.wkniContenido();
        Label wkniD = wkniUi.wkniCaption(wkniDetalle);
        Button wkniAbrir = wkniUi.wkniSecondary(wkniEsArchivo ? "folder" : "arrow-up-right", "Abrir");
        wkniAbrir.setOnAction(wkniE -> wkniAbrir(wkniEsArchivo ? new File(wkniR.wkniContenido()).toURI().toString() : wkniR.wkniContenido()));
        Button wkniCopiar = wkniUi.wkniIconButton("copy", "Copiar");
        wkniCopiar.setOnAction(wkniE -> wkniCopiar(wkniR.wkniContenido(), "Copiado al portapapeles."));
        Button wkniBorrar = wkniUi.wkniIconButton("delete", "Quitar");
        wkniBorrar.setOnAction(wkniE -> wkniEliminar(wkniR));
        VBox wkniTexto = new VBox(2, wkniT, wkniD);
        HBox.setHgrow(wkniTexto, Priority.ALWAYS);
        HBox wkniH = new HBox(10, wkniUi.wkniIconChip(wkniEsArchivo ? "folder" : "link", wkniEsArchivo ? "amber" : "blue", 36, 18), wkniTexto, wkniAbrir, wkniCopiar, wkniBorrar);
        wkniH.setAlignment(Pos.CENTER_LEFT);
        wkniH.getStyleClass().add("anuncio-card");
        return wkniH;
    }

    // ------------------------------------------------------------------ acciones

    private void wkniDialogoRecurso(String wkni_tipo, wkniRecursoClase wkniExistente) {
        boolean wkniEsAnuncio = wkniRecursoClase.wkniANUNCIO.equals(wkni_tipo);
        Dialog<ButtonType> wkniDialog = new Dialog<>();
        wkniDialog.setTitle("Aula virtual");
        wkniDialog.setHeaderText(wkniEsAnuncio ? "Nuevo anuncio para " + wkniCtx.wkniClase()
            : wkniRecursoClase.wkniCLASE_EN_VIVO.equals(wkni_tipo) ? "Enlace de la clase en vivo" : "Nuevo enlace");
        if (wkniDocenteUi.wkniVentana(wkniRoot) != null) wkniDialog.initOwner(wkniDocenteUi.wkniVentana(wkniRoot));
        ButtonType wkniOk = new ButtonType(wkniEsAnuncio ? "Publicar" : "Guardar", ButtonBar.ButtonData.OK_DONE);
        wkniDialog.getDialogPane().getButtonTypes().addAll(wkniOk, ButtonType.CANCEL);
        TextField wkniTitulo = new TextField(wkniExistente == null
            ? (wkniRecursoClase.wkniCLASE_EN_VIVO.equals(wkni_tipo) ? "Clase virtual" : "") : wkniExistente.wkniTitulo());
        wkniTitulo.setPromptText(wkniEsAnuncio ? "Título del anuncio" : "Nombre (p. ej. Diapositivas unidad 1)");
        TextArea wkniCuerpo = new TextArea(wkniExistente == null ? "" : Objects.requireNonNullElse(wkniExistente.wkniContenido(), ""));
        TextField wkniUrl = new TextField(wkniExistente == null ? "" : Objects.requireNonNullElse(wkniExistente.wkniContenido(), ""));
        wkniUrl.setPromptText("https://meet.google.com/abc-defg-hij");
        CheckBox wkniFijar = new CheckBox("Fijar arriba");
        VBox wkniForm = new VBox(8, wkniUi.wkniField("Título", wkniTitulo));
        if (wkniEsAnuncio) {
            wkniCuerpo.setPromptText("Escribe el anuncio…");
            wkniCuerpo.setPrefRowCount(5);
            wkniCuerpo.setWrapText(true);
            wkniForm.getChildren().addAll(wkniUi.wkniField("Mensaje", wkniCuerpo), wkniFijar);
        } else wkniForm.getChildren().add(wkniUi.wkniField("Enlace", wkniUrl));
        wkniForm.setPadding(new Insets(8, 4, 4, 4));
        wkniForm.setPrefWidth(460);
        wkniDialog.getDialogPane().setContent(wkniForm);
        if (wkniDialog.showAndWait().filter(wkniB -> wkniB == wkniOk).isEmpty()) return;

        String wkniContenido = wkniEsAnuncio ? wkniCuerpo.getText() : wkniNormalizarUrl(wkniUrl.getText());
        if (wkniTitulo.getText().isBlank()) { wkniDocenteUi.wkniInfo(wkniRoot, "Escribe un título."); return; }
        if (!wkniEsAnuncio && wkniContenido == null) { wkniDocenteUi.wkniInfo(wkniRoot, "Escribe un enlace válido (por ejemplo https://meet.google.com/…)."); return; }
        if (wkniExistente != null) wkniCtx.wkniDao().wkniEliminarRecurso(wkniExistente.wkniId());
        boolean wkniFijado = wkniEsAnuncio ? wkniFijar.isSelected() : wkniRecursoClase.wkniCLASE_EN_VIVO.equals(wkni_tipo);
        if (!wkniCtx.wkniDao().wkniCrearRecurso(wkniCtx.wkniClase().wkniId(), wkni_tipo, wkniTitulo.getText(), wkniContenido, LocalDate.now(), wkniFijado))
            wkniDocenteUi.wkniError(wkniRoot, "No se pudo guardar.");
        wkniRecargar();
    }

    private void wkniAgregarArchivo() {
        FileChooser wkniFc = new FileChooser();
        wkniFc.setTitle("Agregar archivo al aula virtual");
        File wkniF = wkniFc.showOpenDialog(wkniDocenteUi.wkniVentana(wkniRoot));
        if (wkniF == null) return;
        String wkniNombre = wkniF.getName().contains(".") ? wkniF.getName().substring(0, wkniF.getName().lastIndexOf('.')) : wkniF.getName();
        wkniCtx.wkniDao().wkniCrearRecurso(wkniCtx.wkniClase().wkniId(), wkniRecursoClase.wkniARCHIVO, wkniNombre, wkniF.getAbsolutePath(), LocalDate.now(), false);
        wkniRecargar();
    }

    private void wkniEliminar(wkniRecursoClase wkniR) {
        if (wkniUi.wkniConfirm(wkniRoot, "¿Eliminar «" + wkniR.wkniTitulo() + "»?", "Se quitará del aula virtual de este curso"
            + (wkniRecursoClase.wkniARCHIVO.equals(wkniR.wkni_tipo()) ? " (el archivo de tu computadora no se borra)." : "."), "Eliminar")) {
            wkniCtx.wkniDao().wkniEliminarRecurso(wkniR.wkniId());
            wkniRecargar();
        }
    }

    private String wkniTextoAnuncio(wkniRecursoClase wkniR) {
        return "📢 " + wkniR.wkniTitulo() + " — " + wkniCtx.wkniClase() + "\n\n" + Objects.requireNonNullElse(wkniR.wkniContenido(), "");
    }

    /** Abre el programa de correo con el anuncio y los correos del curso en copia oculta. */
    private void wkniEnviarCorreo(wkniRecursoClase wkniR) {
        List<wkniEstudianteClase> wkniEst = wkniCtx.wkniDao().wkniListarEstudiantes(wkniCtx.wkniClase().wkniId());
        String wkniCorreos = wkniEst.stream().map(wkniEstudianteClase::wkniCorreo).filter(wkniC -> wkniC != null && wkniC.contains("@"))
            .collect(Collectors.joining(","));
        if (wkniCorreos.isEmpty()) { wkniDocenteUi.wkniInfo(wkniRoot, "Ningún estudiante de este curso tiene correo registrado."); return; }
        String wkniAsunto = URLEncoder.encode(wkniR.wkniTitulo() + " — " + wkniCtx.wkniClase(), StandardCharsets.UTF_8).replace("+", "%20");
        String wkniCuerpo = URLEncoder.encode(Objects.requireNonNullElse(wkniR.wkniContenido(), ""), StandardCharsets.UTF_8).replace("+", "%20");
        wkniAbrir("mailto:?bcc=" + wkniCorreos + "&subject=" + wkniAsunto + "&body=" + wkniCuerpo);
    }

    private void wkniAbrir(String wkniUri) {
        if (wkniUri == null || wkniUri.isBlank()) return;
        com.agendapersonal.wkniMain.wkniAbrirEnSistema(wkniUri);
    }

    private void wkniCopiar(String wkniTexto, String wkni_aviso) {
        ClipboardContent wkniC = new ClipboardContent();
        wkniC.putString(wkniTexto == null ? "" : wkniTexto);
        Clipboard.getSystemClipboard().setContent(wkniC);
        com.agendapersonal.ui.wkniToast.wkniMostrar(wkniRoot, wkni_aviso);
    }

    /** Acepta "meet.google.com/x" y le agrega https://; null si está vacío o no parece un enlace. */
    static String wkniNormalizarUrl(String wkniS) {
        if (wkniS == null || wkniS.isBlank()) return null;
        String wkniT = wkniS.trim();
        if (wkniT.contains(" ") || !wkniT.contains(".")) return null;
        return wkniT.matches("(?i)^[a-z][a-z0-9+.-]*://.*") ? wkniT : "https://" + wkniT;
    }
}

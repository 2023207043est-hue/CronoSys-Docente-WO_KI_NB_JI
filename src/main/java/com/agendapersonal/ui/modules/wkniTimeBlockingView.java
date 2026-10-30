package com.agendapersonal.ui.modules;

import com.agendapersonal.controller.wkniMainController;
import com.agendapersonal.domain.model.wkniBloqueTiempo;
import com.agendapersonal.domain.model.wkniNota;
import com.agendapersonal.infrastructure.repo.wkniBloqueDAO;
import com.agendapersonal.infrastructure.repo.wkniNotaDAO;
import com.agendapersonal.model.wkniTarea;
import com.agendapersonal.service.wkniListaPendientes;
import com.agendapersonal.service.wkniMetodoEnfoque;
import com.agendapersonal.ui.wkniAnim;
import com.agendapersonal.ui.wkniIconFactory;
import com.agendapersonal.ui.wkniNotifier;
import com.agendapersonal.ui.wkniSwitch;
import com.agendapersonal.ui.wkniThemeService;
import com.agendapersonal.util.wkniUi;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Time blocking: reserva bloques de enfoque por día, avisa si chocan con otros bloques o eventos,
 * y configura la sesión de enfoque (método, duración, importancia, pendientes y fondo de la pantalla).
 */
public class wkniTimeBlockingView {
    private static final Map<String, String> wkniTONO = Map.of(
        "Enfoque", "orange", "Reunión", "blue", "Estudio", "violet", "Salud", "green", "Personal", "rose");

    private final wkniMainController wkniC;
    private final wkniBloqueDAO wkniDao = new wkniBloqueDAO();
    private final wkniNotaDAO wkniNotas = new wkniNotaDAO();
    private final VBox wkniView;
    private final DatePicker wkni_fecha = new DatePicker(LocalDate.now());
    private final VBox wkniLista = new VBox(0);
    private final Label wkniTituloDia = wkniUi.wkniLabel("", "h3");

    /** Métodos propios guardados en el perfil. */
    private List<wkniMetodoEnfoque> wkniPropios = new ArrayList<>();
    private ComboBox<wkniMetodoEnfoque> wkniMetodos;
    private Label wkniDescripcion, wkniUso, wkni_resumen;
    private TextField wkniEnfocar;

    public wkniTimeBlockingView(wkniMainController wkniC) {
        this.wkniC = wkniC;
        this.wkniPropios = new ArrayList<>(wkniMetodoEnfoque.wkniDesdeMapas(wkniThemeService.wkniGet().wkniSettings().wkniValue("timeBlocking.presets")));

        VBox wkniPage = wkniUi.wkniPage();
        Button wkniPrev = wkniUi.wkniIconButton("chevron-left", "Día anterior");
        Button wkniNext = wkniUi.wkniIconButton("chevron-right", "Día siguiente");
        Button wkniHoy = wkniUi.wkniSecondary(null, "Hoy");
        wkniPrev.setOnAction(wkniE -> wkni_fecha.setValue(wkni_fecha.getValue().minusDays(1)));
        wkniNext.setOnAction(wkniE -> wkni_fecha.setValue(wkni_fecha.getValue().plusDays(1)));
        wkniHoy.setOnAction(wkniE -> wkni_fecha.setValue(LocalDate.now()));
        wkniUi.wkniFechaCorta(wkni_fecha);
        wkni_fecha.setOnAction(wkniE -> wkniFill());
        wkni_fecha.setPrefWidth(170);
        HBox wkniNav = new HBox(6, wkniPrev, wkni_fecha, wkniNext, wkniHoy);
        wkniNav.setAlignment(Pos.CENTER_RIGHT);
        wkniPage.getChildren().add(wkniUi.wkniPageHeader("clock", "Time blocking",
            "Reserva bloques de enfoque por día y arranca sesiones con reloj, progreso y lista de pendientes.", wkniNav));

        wkniPage.getChildren().addAll(wkniCardSesion(), wkniCardMetodos(), wkniCardFondo(), wkniCardNuevoBloque(), wkniCardLista());

        wkniView = wkniPage;
        wkniFill();
    }

    // ------------------------------------------------------------------ sesión de enfoque

    private VBox wkniCardSesion() {
        wkniMetodos = new ComboBox<>();
        wkniMetodos.getItems().addAll(wkniMetodosBase());
        wkniMetodos.getItems().addAll(wkniPropios);
        wkniMetodos.setValue(wkniMetodos.getItems().get(0));
        wkniMetodos.setCellFactory(wkniFabricaCelda());
        wkniMetodos.setButtonCell(wkniFabricaCelda().call(null));
        wkniMetodos.setMaxWidth(Double.MAX_VALUE);

        wkniDescripcion = wkniUi.wkniMuted("");
        wkniUso = wkniUi.wkniMuted("");
        wkni_resumen = wkniUi.wkniLabel("", "caption");
        wkni_resumen.getStyleClass().add("caption");
        wkniMetodos.setOnAction(wkniE -> wkniVerMetodo());
        wkniVerMetodo();

        wkniEnfocar = new TextField();
        wkniEnfocar.setPromptText("¿En qué te vas a enfocar? (opcional)");

        // Importancia de la sesión
        ComboBox<wkniPantallaEnfoque.wkniImportancia> wkniImp = new ComboBox<>();
        wkniImp.getItems().setAll(wkniPantallaEnfoque.wkniImportancia.values());
        wkniImp.setValue(wkniPantallaEnfoque.wkniImportancia.wkniDesde(wkniSt().wkniStr("timeBlocking.importance")));
        wkniImp.setCellFactory(wkniLv -> new ListCell<>() {
            @Override protected void updateItem(wkniPantallaEnfoque.wkniImportancia wkniI, boolean wkniEmpty) {
                super.updateItem(wkniI, wkniEmpty);
                setText(wkniEmpty || wkniI == null ? null : wkniI.toString());
                setTooltip(wkniEmpty || wkniI == null ? null : new javafx.scene.control.Tooltip(wkniI.wkniAyuda()));
            }
        });
        wkniImp.setOnAction(wkniE -> {
            if (wkniImp.getValue() == null) return;
            wkniSt().wkniSet("timeBlocking.importance", wkniImp.getValue().name());
        });

        // De dónde salen los pendientes
        String[] wkniFuentes = {"ambas", "tareas", "notas", "ninguna"};
        wkniUi.wkniSegmented wkniOrigen = new wkniUi.wkniSegmented("Tareas y notas", "Solo tareas", "Solo notas", "Ninguna");
        int wkniIdx = wkniIndexOf(wkniFuentes, wkniSt().wkniStr("timeBlocking.todoSource"));
        wkniOrigen.wkniSelect(wkniIdx < 0 ? 0 : wkniIdx);
        wkniOrigen.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> {
            wkniSt().wkniSet("timeBlocking.todoSource", wkniFuentes[wkniV.intValue()]);
            wkniResumenPendientes();
        });

        wkniSwitch wkniVerPendientes = new wkniSwitch("Mostrar la lista de pendientes en la pantalla",
            "Puedes marcar las casillas mientras trabajas: se guarda en tus tareas o en tus notas.",
            wkniSt().wkniBool("timeBlocking.showTasks"));
        wkniVerPendientes.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> wkniSt().wkniSet("timeBlocking.showTasks", wkniV));

        wkniSwitch wkniPausar = new wkniSwitch("Pausar el contador si salgo de la pantalla",
            "Se aplica siempre con la importancia Alta o Estricta.", wkniSt().wkniBool("timeBlocking.pauseOnLeave"));
        wkniPausar.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> wkniSt().wkniSet("timeBlocking.pauseOnLeave", wkniV));

        Button wkniIniciar = wkniUi.wkniPrimary("clock", "Iniciar enfoque");
        wkniIniciar.setOnAction(wkniE -> wkniIniciar());

        HBox wkniBotonera = new HBox(10, wkniIniciar, wkniUi.wkniMuted("También puedes cambiar el fondo y los ajustes dentro de la pantalla."));
        wkniBotonera.setAlignment(Pos.CENTER_LEFT);

        VBox wkniCard = wkniUi.wkniCard();
        wkniCard.getChildren().addAll(
            wkniUi.wkniH3("Sesión de enfoque"),
            wkniUi.wkniMuted("Elige un método, la importancia y los pendientes. Se abre una pantalla con reloj, "
                + "barra de progreso, número de sesión y botones de control."),
            wkniUi.wkniField("Método", wkniMetodos), wkniDescripcion, wkniUso, wkni_resumen,
            wkniUi.wkniField("Enfoque", wkniEnfocar),
            wkniUi.wkniField("Importancia de la sesión", wkniImp),
            wkniUi.wkniField("Pendientes de la sesión", wkniOrigen.wkniNode()),
            wkniVerPendientes, wkniPausar,
            wkniBotonera);
        return wkniCard;
    }

    private javafx.util.Callback<ListView<wkniMetodoEnfoque>, ListCell<wkniMetodoEnfoque>> wkniFabricaCelda() {
        return wkniLv -> new ListCell<>() {
            @Override protected void updateItem(wkniMetodoEnfoque wkniM, boolean wkniEmpty) {
                super.updateItem(wkniM, wkniEmpty);
                if (wkniEmpty || wkniM == null) { setText(null); return; }
                setText(wkniM.wkniNombre() + (wkniM.wkniPropio() ? "  · propio" : ""));
                setTooltip(new javafx.scene.control.Tooltip(wkniM.wkniDescripcion()));
            }
        };
    }

    private void wkniVerMetodo() {
        wkniMetodoEnfoque wkniM = wkniMetodos.getValue();
        if (wkniM == null) return;
        wkniDescripcion.setText(wkniM.wkniDescripcion());
        wkniUso.setText("Uso común: " + wkniM.wkniUsoComun());
        wkni_resumen.setText(wkniM.wkniResumen() + (wkniM.wkniTieneRangos() ? "  ·  la duración concreta se sortea al empezar" : ""));
        wkniResumenPendientes();
    }

    /** Añade al resumen cuántos pendientes habrá en la pantalla de bloqueo. */
    private void wkniResumenPendientes() {
        List<wkniListaPendientes.wkniItem> wkniItems = wkniPendientes();
        if (wkniItems.isEmpty()) return;
        long wkniHechos = wkniItems.stream().filter(wkniListaPendientes.wkniItem::wkniHecho).count();
        wkni_resumen.setText(wkni_resumen.getText() + "  ·  " + wkniHechos + " de " + wkniItems.size() + " pendientes listos");
    }

    /** Los pendientes que se mostrarán en la pantalla de bloqueo. */
    private List<wkniListaPendientes.wkniItem> wkniPendientes() {
        List<wkniListaPendientes.wkniItem> wkniLista = new ArrayList<>();
        String wkniFuente = wkniSt().wkniStr("timeBlocking.todoSource");
        if ("ninguna".equals(wkniFuente)) return wkniLista;
        if ("tareas".equals(wkniFuente) || "ambas".equals(wkniFuente)) {
            LocalDate wkniHoy = LocalDate.now();
            for (wkniTarea wkniT : wkniC.wkniGetTareas()) {
                if (wkniT.wkniGetEstadoTipo() == null || wkniT.wkniGetEstadoTipo().wkniEsCompletada()) continue;
                if (wkniT.wkniGetFecha() != null && wkniT.wkniGetFecha().isAfter(wkniHoy)) continue;   // solo lo de hoy y lo vencido
                wkniLista.add(new wkniListaPendientes.wkniItem(wkniT.wkniGetId(), -1, wkniT.wkniGetTitulo(), false, "tarea"));
            }
        }
        if ("notas".equals(wkniFuente) || "ambas".equals(wkniFuente)) {
            wkniLista.addAll(wkniListaPendientes.wkniDesdeNotas(wkniNotas.wkniListar(null)));
        }
        return wkniLista;
    }

    private void wkniIniciar() {
        wkniMetodoEnfoque wkniM = wkniMetodos.getValue();
        if (wkniM == null) return;
        List<wkniListaPendientes.wkniItem> wkniItems = wkniPendientes();
        wkniPantallaEnfoque.wkniImportancia wkniImp = wkniPantallaEnfoque.wkniImportancia.wkniDesde(wkniSt().wkniStr("timeBlocking.importance"));
        wkniPantallaEnfoque.wkniAbrir(wkniM, wkniEnfocar.getText(), wkniImp, wkniItems, this::wkniAlternarPendiente);
    }

    /** Guarda el cambio de un pendiente (tarea completada o casilla de una nota marcada). */
    private void wkniAlternarPendiente(wkniListaPendientes.wkniItem wkni_item, boolean wkniHecho) {
        if ("tarea".equals(wkni_item.wkniOrigen())) {
            for (wkniTarea wkniT : wkniC.wkniGetTareas())
                if (wkniT.wkniGetId() == wkni_item.wkniNotaId()) { wkniC.wkniAlternarCompletada(wkniT); return; }
            return;
        }
        for (wkniNota wkniN : wkniNotas.wkniListar(null)) {
            if (wkniN.wkniGetId() != wkni_item.wkniNotaId()) continue;
            String wkniContenido = wkniListaPendientes.wkniAlternar(wkniN, wkni_item, wkniHecho);
            if (wkniContenido.equals(wkniN.wkniGetContenido())) return;
            wkniN.wkniSetContenido(wkniContenido);
            if (wkniNotas.wkniActualizar(wkniN)) wkniNotifier.wkniSuccess(wkniHecho ? "Pendiente hecho" : "Pendiente devuelto", wkni_item.wkniTexto());
            return;
        }
    }

    // ------------------------------------------------------------------ métodos

    private List<wkniMetodoEnfoque> wkniMetodosBase() {
        List<wkniMetodoEnfoque> wkniBase = new ArrayList<>(wkniMetodoEnfoque.wkniPREDEFINIDOS);
        if (com.agendapersonal.util.wkniPerfil.wkniEsDocente()) wkniBase.addAll(wkniMetodoEnfoque.wkniDOCENTES);
        return wkniBase;
    }

    private VBox wkniCardMetodos() {
        VBox wkniListaMetodos = new VBox(8);

        for (wkniMetodoEnfoque wkniM : wkniMetodosBase()) {
            wkniListaMetodos.getChildren().add(wkniFilaMetodo(wkniM, false));
        }
        for (wkniMetodoEnfoque wkniM : wkniPropios) {
            wkniListaMetodos.getChildren().add(wkniFilaMetodo(wkniM, true));
        }

        // Crear el tuyo (con rangos)
        TextField wkniNom = new TextField();
        wkniNom.setPromptText("Nombre del método");
        TextField wkni_trabajo = new TextField("30"), wkniTrabajoMax = new TextField("30");
        TextField wkniDescanso = new TextField("5"), wkniDescansoMax = new TextField("5");
        TextField wkniSesiones = new TextField("4"), wkniLargo = new TextField("15"), wkniLargoMax = new TextField("15");
        for (TextField wkniT : List.of(wkni_trabajo, wkniTrabajoMax, wkniDescanso, wkniDescansoMax, wkniSesiones, wkniLargo, wkniLargoMax)) {
            wkniT.setPrefColumnCount(4);
        }
        Button wkniCrear = wkniUi.wkniPrimary("add", "Guardar método propio");
        wkniCrear.setOnAction(wkniE -> wkniCrearMetodo(wkniNom, wkni_trabajo, wkniTrabajoMax, wkniDescanso, wkniDescansoMax, wkniSesiones, wkniLargo, wkniLargoMax));

        VBox wkniGrupoTrabajo = new VBox(4, wkniUi.wkniLabel("Trabajo (min)", "caption"), new HBox(6, wkni_trabajo, wkniUi.wkniLabel("a", "caption"), wkniTrabajoMax));
        VBox wkniGrupoDescanso = new VBox(4, wkniUi.wkniLabel("Descanso (min)", "caption"), new HBox(6, wkniDescanso, wkniUi.wkniLabel("a", "caption"), wkniDescansoMax));
        VBox wkniGrupoSesiones = new VBox(4, wkniUi.wkniLabel("Sesiones por ciclo", "caption"), wkniSesiones);
        VBox wkniGrupoLargo = new VBox(4, wkniUi.wkniLabel("Descanso largo (min)", "caption"), new HBox(6, wkniLargo, wkniUi.wkniLabel("a", "caption"), wkniLargoMax));

        HBox wkniFormulario = new HBox(12, wkniUi.wkniField("Nombre", wkniNom), wkniGrupoTrabajo, wkniGrupoDescanso, wkniGrupoSesiones, wkniGrupoLargo, wkniCrear);
        wkniFormulario.setAlignment(Pos.BOTTOM_LEFT);

        VBox wkniCard = wkniUi.wkniCard();
        wkniCard.getChildren().addAll(wkniUi.wkniH3("Métodos de bloqueo"),
            wkniUi.wkniMuted("Cada método tiene su descripción y su uso común. Los tiempos pueden ser un valor fijo "
                + "o un rango (por ejemplo 60 a 90 minutos): al empezar la sesión se sortea uno dentro del rango."),
            wkniListaMetodos, wkniUi.wkniField("Crea el tuyo", wkniFormulario));
        return wkniCard;
    }

    private Node wkniFilaMetodo(wkniMetodoEnfoque wkniM, boolean wkniPropio) {
        Label wkniN = wkniUi.wkniLabel(wkniM.wkniNombre(), "row-title");
        Label wkniD = wkniUi.wkniMuted(wkniM.wkniDescripcion());
        wkniD.setMaxWidth(420);
        Label wkniU = wkniUi.wkniLabel("Uso común: " + wkniM.wkniUsoComun(), "caption");
        VBox wkniTextos = new VBox(3, wkniN, wkniD, wkniU);
        HBox.setHgrow(wkniTextos, Priority.ALWAYS);

        HBox wkniAcciones = new HBox(8);
        Button wkniUsar = wkniUi.wkniSecondary(null, "Usar");
        wkniUsar.setOnAction(wkniE -> { wkniMetodos.setValue(wkniM); wkniVerMetodo(); });
        wkniAcciones.getChildren().add(wkniUsar);
        if (wkniPropio) {
            Button wkniDup = wkniUi.wkniIconButton("copy", "Duplicar este método");
            wkniDup.setOnAction(wkniE -> wkniDuplicar(wkniM));
            Button wkniDel = wkniUi.wkniIconButton("delete", "Eliminar este método");
            wkniDel.setOnAction(wkniE -> wkniEliminarMetodo(wkniM));
            wkniAcciones.getChildren().addAll(wkniDup, wkniDel);
        }
        HBox wkniFila = new HBox(14, wkniTextos, wkniAcciones);
        wkniFila.setAlignment(Pos.CENTER_LEFT);
        wkniFila.getStyleClass().addAll("list-row", "with-divider");
        return wkniFila;
    }

    private void wkniCrearMetodo(TextField wkniNom, TextField wkni_trabajo, TextField wkniTrabajoMax, TextField wkniDescanso,
                            TextField wkniDescansoMax, TextField wkniSesiones, TextField wkniLargo, TextField wkniLargoMax) {
        try {
            int wkniT1 = Integer.parseInt(wkni_trabajo.getText().trim());
            int wkniT2 = Integer.parseInt(wkniTrabajoMax.getText().trim());
            int wkniD1 = Integer.parseInt(wkniDescanso.getText().trim());
            int wkniD2 = Integer.parseInt(wkniDescansoMax.getText().trim());
            int wkniS = Integer.parseInt(wkniSesiones.getText().trim());
            int wkniL1 = Integer.parseInt(wkniLargo.getText().trim());
            int wkniL2 = Integer.parseInt(wkniLargoMax.getText().trim());
            String wkniNombre = wkniNom.getText().trim();
            if (wkniNombre.isEmpty()) { wkniAnim.wkniShake(wkniNom); wkniNotifier.wkniWarning("Falta el nombre", "Ponle un nombre a tu método."); return; }
            wkniMetodoEnfoque wkniM = wkniMetodoEnfoque.wkniPersonalizado(wkniNombre, wkniT1, wkniT2, wkniD1, wkniD2, wkniS, wkniL1, wkniL2);
            wkniPropios.add(wkniM);
            wkniGuardarPropios();
            wkniMetodos.getItems().add(wkniM);
            wkniMetodos.setValue(wkniM);
            wkniVerMetodo();
            wkniNom.clear();
            wkniNotifier.wkniSuccess("Método guardado", wkniM.wkniNombre() + " · " + wkniM.wkniResumen());
        } catch (NumberFormatException wkniEx) {
            wkniNotifier.wkniWarning("Revisa los números", "Usa minutos enteros, por ejemplo 40 y 8.");
        }
    }

    private void wkniDuplicar(wkniMetodoEnfoque wkniM) {
        wkniMetodoEnfoque wkniCopia = wkniM.wkniComoPropio(wkniM.wkniNombre() + " (copia)");
        wkniPropios.add(wkniCopia);
        wkniGuardarPropios();
        wkniMetodos.getItems().add(wkniCopia);
        wkniNotifier.wkniSuccess("Método duplicado", wkniCopia.wkniNombre());
    }

    private void wkniEliminarMetodo(wkniMetodoEnfoque wkniM) {
        if (!wkniUi.wkniConfirm(wkniView, "¿Eliminar el método «" + wkniM.wkniNombre() + "»?", "Solo se borra el método, no las sesiones que ya hiciste.", "Eliminar")) return;
        wkniPropios.removeIf(wkniX -> wkniX.wkniId().equals(wkniM.wkniId()));
        wkniGuardarPropios();
        wkniMetodos.getItems().remove(wkniM);
        if (wkniMetodos.getValue() == null && !wkniMetodos.getItems().isEmpty()) wkniMetodos.setValue(wkniMetodos.getItems().get(0));
        wkniVerMetodo();
        wkniNotifier.wkniDeleted("Método eliminado", wkniM.wkniNombre());
    }

    private void wkniGuardarPropios() {
        List<Object> wkniMapas = new ArrayList<>();
        for (wkniMetodoEnfoque wkniM : wkniPropios) wkniMapas.add(wkniM.wkniAMapa());
        wkniSt().wkniSet("timeBlocking.presets", wkniMapas);
    }

    // ------------------------------------------------------------------ fondo

    private VBox wkniCardFondo() {
        String[] wkniModos = {"degradado", "archivo", "color"};
        wkniUi.wkniSegmented wkniFondo = new wkniUi.wkniSegmented("Degradado del tema", "Imagen o video", "Color");
        wkniFondo.wkniSelect(Math.max(0, wkniIndexOf(wkniModos, wkniSt().wkniStr("timeBlocking.background"))));
        wkniFondo.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> wkniSt().wkniSet("timeBlocking.background", wkniModos[wkniV.intValue()]));

        Label wkniArchivo = wkniUi.wkniLabel(wkniNombreFondo(), "caption");
        Button wkniAgregarFondo = wkniUi.wkniSecondary("grid", "Agregar fondo…");
        wkniAgregarFondo.setOnAction(wkniE -> {
            FileChooser wkniFc = new FileChooser();
            wkniFc.setTitle("Fondo de la pantalla de enfoque");
            wkniFc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes y videos",
                "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp", "*.mp4", "*.m4v", "*.webm", "*.ogv"));
            File wkniF = wkniFc.showOpenDialog(wkniView.getScene() == null ? null : wkniView.getScene().getWindow());
            if (wkniF == null) return;
            wkniSt().wkniSet("timeBlocking.backgroundFile", wkniF.getAbsolutePath());
            wkniSt().wkniSet("timeBlocking.background", "archivo");
            wkniFondo.wkniSelect(1);
            wkniArchivo.setText(wkniF.getName());
        });
        Button wkniQuitar = wkniUi.wkniSecondary(null, "Quitar");
        wkniQuitar.setOnAction(wkniE -> {
            wkniSt().wkniSet("timeBlocking.backgroundFile", null);
            wkniSt().wkniSet("timeBlocking.background", "degradado");
            wkniFondo.wkniSelect(0);
            wkniArchivo.setText("Sin archivo");
        });

        ColorPicker wkniColorFondo = new ColorPicker(javafx.scene.paint.Color.web(
            com.agendapersonal.settings.wkniColors.wkniNormalize(wkniSt().wkniStr("timeBlocking.backgroundColor")) == null
                ? "#1B1E25" : wkniSt().wkniStr("timeBlocking.backgroundColor")));
        wkniColorFondo.setOnAction(wkniE -> {
            var wkniC1 = wkniColorFondo.getValue();
            wkniSt().wkniSet("timeBlocking.backgroundColor", com.agendapersonal.settings.wkniColors.wkniHex(
                (int) Math.round(wkniC1.getRed() * 255), (int) Math.round(wkniC1.getGreen() * 255), (int) Math.round(wkniC1.getBlue() * 255)));
            wkniSt().wkniSet("timeBlocking.background", "color");
            wkniFondo.wkniSelect(2);
        });

        wkniSwitch wkniRepetir = new wkniSwitch("Repetir el video en bucle", "Si el fondo es un video, se repite mientras dure la sesión.",
            wkniSt().wkniBool("timeBlocking.backgroundLoop"));
        wkniRepetir.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> wkniSt().wkniSet("timeBlocking.backgroundLoop", wkniV));

        // Audio local de la sesión (sin internet: un archivo de tu equipo).
        Label wkniAudio = wkniUi.wkniLabel(wkniSt().wkniStr("timeBlocking.audioFile").isBlank()
            ? "Sin audio" : new File(wkniSt().wkniStr("timeBlocking.audioFile")).getName(), "caption");
        Button wkniAgregarAudio = wkniUi.wkniSecondary("music", "Elegir audio…");
        wkniAgregarAudio.setOnAction(wkniE -> {
            FileChooser wkniFc = new FileChooser();
            wkniFc.setTitle("Audio de la sesión de enfoque");
            wkniFc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav", "*.ogg",
                "*.m4a", "*.aac", "*.flac", "*.opus"));
            File wkniF = wkniFc.showOpenDialog(wkniView.getScene() == null ? null : wkniView.getScene().getWindow());
            if (wkniF == null) return;
            wkniSt().wkniSet("timeBlocking.audioFile", wkniF.getAbsolutePath());
            wkniAudio.setText(wkniF.getName());
        });
        Button wkniQuitarAudio = wkniUi.wkniSecondary(null, "Quitar");
        wkniQuitarAudio.setOnAction(wkniE -> {
            wkniSt().wkniSet("timeBlocking.audioFile", null);
            wkniAudio.setText("Sin audio");
        });
        HBox wkniFilaAudio = new HBox(10, wkniAgregarAudio, wkniAudio, wkniQuitarAudio);
        wkniFilaAudio.setAlignment(Pos.CENTER_LEFT);

        HBox wkniFilaFondo = new HBox(10, wkniAgregarFondo, wkniArchivo, wkniQuitar, wkniColorFondo);
        wkniFilaFondo.setAlignment(Pos.CENTER_LEFT);

        VBox wkniCard = wkniUi.wkniCard();
        wkniCard.getChildren().addAll(wkniUi.wkniH3("Fondo y audio de la pantalla de enfoque"),
            wkniUi.wkniMuted("Puedes cambiarlo también desde la propia pantalla, con los botones «Agregar fondo» y «Ajustes»."),
            wkniUi.wkniField("Tipo de fondo", wkniFondo.wkniNode()),
            wkniFilaFondo, wkniRepetir,
            wkniUi.wkniField("Audio de la sesión", wkniFilaAudio),
            wkniUi.wkniMuted("Un archivo de audio de tu equipo: se repite mientras trabajas y calla en los descansos."));
        return wkniCard;
    }

    private String wkniNombreFondo() {
        String wkniRuta = wkniSt().wkniStr("timeBlocking.backgroundFile");
        return wkniRuta.isBlank() ? "Sin archivo" : new File(wkniRuta).getName();
    }

    // ------------------------------------------------------------------ bloques del día

    private VBox wkniCardNuevoBloque() {
        TextField wkniTitulo = new TextField();
        wkniTitulo.setPromptText("Ej. Trabajo profundo en la tesis");
        TextField wkniHi = new TextField("09:00");
        TextField wkniHf = new TextField("11:00");
        ComboBox<String> wkniCat = new ComboBox<>();
        wkniCat.getItems().addAll("Enfoque", "Reunión", "Estudio", "Salud", "Personal");
        wkniCat.setValue("Enfoque");
        Button wkniAdd = wkniUi.wkniPrimary("add", "Bloquear");
        Runnable wkniAgregar = () -> {
            if (wkniTitulo.getText().isBlank()) { wkniAnim.wkniShake(wkniTitulo); wkniNotifier.wkniWarning("Falta el título", "Ponle un nombre al bloque."); return; }
            LocalTime wkniA, wkniB;
            try { wkniA = LocalTime.parse(wkniHi.getText().trim()); wkniB = LocalTime.parse(wkniHf.getText().trim()); }
            catch (DateTimeParseException wkniEx) { wkniAnim.wkniShake(wkniHi); wkniNotifier.wkniWarning("Hora inválida", "Usa el formato HH:mm, por ejemplo 09:30."); return; }
            if (!wkniB.isAfter(wkniA)) { wkniAnim.wkniShake(wkniHf); wkniNotifier.wkniWarning("Revisa las horas", "La hora de fin debe ser posterior al inicio."); return; }
            if (wkni_fecha.getValue() == null) wkni_fecha.setValue(LocalDate.now());
            String wkniF = wkni_fecha.getValue().toString();
            String wkniIni = wkniA.toString(), wkniFin = wkniB.toString();
            if (wkniChoca(wkniF, wkniIni, wkniFin)) { wkniNotifier.wkniWarning("Ese horario ya está ocupado", "Choca con otro bloque o con un evento de tu agenda."); return; }
            if (wkniDao.wkniInsertar(new wkniBloqueTiempo(0, wkniTitulo.getText().trim(), wkniF, wkniIni, wkniFin, wkniCat.getValue(), null))) {
                wkniNotifier.wkniSuccess("Bloque reservado", wkniTitulo.getText().trim() + " · " + wkniIni + "–" + wkniFin);
                wkniTitulo.clear();
                wkniFill();
            } else wkniNotifier.wkniError("No se pudo guardar el bloque", null);
        };
        wkniAdd.setOnAction(wkniE -> wkniAgregar.run());
        wkniTitulo.setOnAction(wkniE -> wkniAgregar.run());
        VBox wkniFT = wkniUi.wkniField("Bloque", wkniTitulo);
        VBox wkniFI = wkniUi.wkniField("Inicio", wkniHi);
        VBox wkniFF = wkniUi.wkniField("Fin", wkniHf);
        VBox wkniFC = wkniUi.wkniField("Categoría", wkniCat);
        wkniFT.setPrefWidth(320); wkniFI.setPrefWidth(110); wkniFF.setPrefWidth(110); wkniFC.setPrefWidth(170);
        HBox wkniFila = new HBox(12, wkniFT, wkniFI, wkniFF, wkniFC, wkniAdd);
        wkniFila.setAlignment(Pos.BOTTOM_LEFT);
        VBox wkniForm = wkniUi.wkniCard();
        wkniForm.getChildren().addAll(wkniUi.wkniH3("Nuevo bloque"), wkniFila);
        return wkniForm;
    }

    private boolean wkniChoca(String wkni_fecha, String wkniHi, String wkniHf) {
        for (var wkniB : wkniDao.wkniPorFecha(wkni_fecha))
            if (wkniHi.compareTo(wkniB.wkniGetHoraFin()) < 0 && wkniB.wkniGetHoraInicio().compareTo(wkniHf) < 0) return true;
        for (var wkniE : wkniC.wkniEventosDelDia(LocalDate.parse(wkni_fecha)))
            if (wkniHi.compareTo(wkniE.wkniGetHoraFin()) < 0 && wkniE.wkniGetHoraInicio().compareTo(wkniHf) < 0) return true;
        return false;
    }

    private VBox wkniCardLista() {
        VBox wkniListaCard = new VBox(0, wkniLista);
        wkniListaCard.getStyleClass().addAll("card", "card-flush");
        return wkniListaCard;
    }

    private void wkniFill() {
        if (wkni_fecha.getValue() == null) wkni_fecha.setValue(LocalDate.now());
        LocalDate wkniD = wkni_fecha.getValue();
        wkniTituloDia.setText(wkniUi.wkniFormatDateShort(wkniD));
        List<wkniBloqueTiempo> wkniBloques = wkniDao.wkniPorFecha(wkniD.toString());
        wkniLista.getChildren().clear();
        HBox wkniHead = new HBox(8, wkniTituloDia, wkniUi.wkniSpacer(),
            wkniUi.wkniLabel(wkniBloques.size() + (wkniBloques.size() == 1 ? " bloque" : " bloques"), "caption"));
        wkniHead.setAlignment(Pos.CENTER_LEFT);
        wkniHead.setPadding(new Insets(18, 22, 10, 22));
        wkniLista.getChildren().add(wkniHead);
        if (wkniBloques.isEmpty()) {
            wkniLista.getChildren().add(wkniUi.wkniEmpty("clock", "Día sin bloques", "Reserva tu primer bloque de enfoque con el formulario de arriba."));
            return;
        }
        List<Node> wkniFilas = new ArrayList<>();
        for (wkniBloqueTiempo wkniB : wkniBloques) { Node wkniN = wkniFila(wkniB); wkniLista.getChildren().add(wkniN); wkniFilas.add(wkniN); }
        wkniAnim.wkniStagger(wkniFilas);
    }

    private Node wkniFila(wkniBloqueTiempo wkniB) {
        String wkni_tono = wkniTONO.getOrDefault(wkniB.wkniGetCategoria(), "slate");
        Region wkniBarra = new Region();
        wkniBarra.getStyleClass().addAll("time-bar", "fill-" + wkni_tono);
        Label wkni_horas = wkniUi.wkniLabel(wkniB.wkniGetHoraInicio() + " – " + wkniB.wkniGetHoraFin(), "time-range");
        wkni_horas.setMinWidth(120);
        Label wkniT = wkniUi.wkniLabel(wkniB.wkniGetTitulo(), "row-title");
        HBox.setHgrow(wkniT, Priority.ALWAYS);
        wkniT.setMaxWidth(Double.MAX_VALUE);
        Button wkniDel = wkniUi.wkniIconButton("delete", "Eliminar bloque");
        wkniDel.setOnAction(wkniE -> {
            if (wkniUi.wkniConfirm(wkniView, "¿Eliminar este bloque?", wkniB.wkniGetTitulo() + " · " + wkniB.wkniGetHoraInicio() + "–" + wkniB.wkniGetHoraFin(), "Eliminar")
                && wkniDao.wkniEliminar(wkniB.wkniGetId())) {
                wkniFill();
                wkniNotifier.wkniDeleted("Bloque eliminado", wkniB.wkniGetTitulo());
            }
        });
        HBox wkniRow = new HBox(14, wkniBarra, wkni_horas, wkniT, wkniUi.wkniPill(wkniB.wkniGetCategoria(), wkni_tono), wkniDel);
        wkniRow.setAlignment(Pos.CENTER_LEFT);
        wkniRow.getStyleClass().addAll("list-row", "with-divider");
        return wkniRow;
    }

    // ------------------------------------------------------------------ utilidades

    private com.agendapersonal.settings.wkniSettingsStore wkniSt() { return wkniThemeService.wkniGet().wkniSettings(); }

    private static int wkniIndexOf(String[] wkniArr, String wkniValor) {
        for (int wkniI = 0; wkniI < wkniArr.length; wkniI++) if (wkniArr[wkniI].equals(wkniValor)) return wkniI;
        return -1;
    }

    public Node wkniGetView() { return wkniUi.wkniScroll(wkniView); }

    /** Fuerza la técnica elegida (p. ej. la preparación de clase desde el inicio del usuario). */
    public void wkniSeleccionar(wkniMetodoEnfoque wkniM) { wkniMetodos.setValue(wkniM); wkniVerMetodo(); }
}

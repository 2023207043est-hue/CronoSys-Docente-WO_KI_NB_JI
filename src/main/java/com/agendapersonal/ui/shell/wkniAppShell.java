package com.agendapersonal.ui.shell;

import com.agendapersonal.controller.wkniMainController;
import com.agendapersonal.infrastructure.db.wkniConfigDAO;
import com.agendapersonal.ui.wkniAnim;
import com.agendapersonal.ui.wkniDashboardView;
import com.agendapersonal.ui.wkniEventViews;
import com.agendapersonal.ui.wkniFontPreferences;
import com.agendapersonal.ui.wkniIconFactory;
import com.agendapersonal.ui.wkniLogo;
import com.agendapersonal.ui.wkniNotifier;
import com.agendapersonal.ui.wkniRelojPrefs;
import com.agendapersonal.ui.wkniSwitch;
import com.agendapersonal.ui.wkniTaskViews;
import com.agendapersonal.ui.wkniThemeService;
import com.agendapersonal.ui.modules.wkniAgendaUnificadaView;
import com.agendapersonal.ui.modules.wkniCalendarioModule;
import com.agendapersonal.ui.modules.wkniCrmView;
import com.agendapersonal.ui.modules.wkniEspacioEstudioView;
import com.agendapersonal.ui.modules.wkniFinanzasView;
import com.agendapersonal.ui.modules.wkniHabitosView;
import com.agendapersonal.ui.modules.wkniNotasView;
import com.agendapersonal.ui.modules.wkniSaludView;
import com.agendapersonal.ui.modules.wkniTareasProyectosView;
import com.agendapersonal.ui.modules.wkniTimeBlockingView;
import com.agendapersonal.ui.modules.wkniEspacioDocenteView;
import com.agendapersonal.ui.notify.wkniCentroAvisos;
import com.agendapersonal.service.wkniMetodoEnfoque;
import com.agendapersonal.util.wkniUi;
import com.agendapersonal.util.wkniPerfil;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.RotateTransition;
import javafx.animation.Timeline;
import javafx.css.PseudoClass;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Popup;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Ventana principal: menú lateral que se puede contraer a solo íconos, barra superior con fecha,
 * cambio rápido de tema y campana de avisos, y una página por módulo (se guardan en caché).
 */
public class wkniAppShell {
    private static final double wkniSIDEBAR_OPEN = 252, wkniSIDEBAR_CLOSED = 76;
    private static final PseudoClass wkniCURRENT = PseudoClass.getPseudoClass("current");

    /** Páginas del menú: clave interna → texto visible, ícono y sección. */
    private record wkniItem(String wkniKey, String wkniLabel, String wkniIcon, String wkniSection) {}
    private static final List<wkniItem> wkniITEMS = List.of(
        new wkniItem("Inicio", "Inicio", "home", "Principal"),
        new wkniItem("Agenda Unificada", "Calendario", "agenda", "Principal"),
        new wkniItem("Time Blocking", "Time blocking", "clock", "Principal"),
        new wkniItem("Espacio de Estudio", "Estudio", "book", "Principal"),
        new wkniItem("Tareas y Proyectos", "Tareas", "tasks", "Organizar"),
        new wkniItem("Notas y PKM", "Notas", "notes", "Organizar"),
        new wkniItem("CRM Personal", "Contactos", "contacts", "Personal"),
        new wkniItem("Hábitos", "Hábitos", "habits", "Personal"),
        new wkniItem("Finanzas", "Finanzas", "finance", "Personal"),
        new wkniItem("Salud", "Salud", "health", "Personal"));
    private static final wkniItem wkniCONFIG = new wkniItem("Configuración", "Configuración", "settings", "Ajustes");

    private List<wkniItem> wkniItemsVisibles() {
        if (!wkniPerfil.wkniEsDocente()) return wkniITEMS;
        return wkniITEMS.stream().map(wkniI -> wkniI.wkniKey().equals("Espacio de Estudio")
            ? new wkniItem(wkniI.wkniKey(), "Espacio Docente", "book", wkniI.wkniSection()) : wkniI).toList();
    }

    /** Cada sección de ajustes corresponde a una página: al cambiarla, la página se rehace. */
    private static final Map<String, String> wkniPAGINA_POR_SECCION = Map.ofEntries(
        Map.entry("home", "Inicio"), Map.entry("calendar", "Agenda Unificada"),
        Map.entry("timeBlocking", "Time Blocking"), Map.entry("study", "Espacio de Estudio"),
        Map.entry("tasks", "Tareas y Proyectos"), Map.entry("notes", "Notas y PKM"),
        Map.entry("contacts", "CRM Personal"), Map.entry("habits", "Hábitos"),
        Map.entry("finance", "Finanzas"), Map.entry("health", "Salud"));

    private final wkniMainController wkniC;
    private final Stage wkniStage;
    private final wkniThemeService wkniTheme = wkniThemeService.wkniGet();
    private final wkniConfigDAO wkniCfg = new wkniConfigDAO();
    private final StackPane wkniRoot = new StackPane();
    private final BorderPane wkniFrame = new BorderPane();
    private final StackPane wkniContent = new StackPane();
    private final VBox wkniToastLayer = new VBox();
    private final Map<String, Node> wkniCache = new HashMap<>();
    private final Map<String, Button> wkniBotones = new LinkedHashMap<>();
    private final List<Node> wkniSoloExpandido = new ArrayList<>();
    private final List<Node> wkniSoloContraido = new ArrayList<>();
    private wkniTimeBlockingView wkniTimeBlocking;
    private VBox wkniSidebar;
    private Button wkniToggleSidebar;
    private Label wkniBreadcrumb;
    private Label wkniDateLabel;
    private boolean wkniCollapsed;
    private String wkniActual;

    public wkniAppShell(wkniMainController wkniC, Stage wkniStage) {
        this.wkniC = wkniC; this.wkniStage = wkniStage;
        wkniRoot.getStyleClass().add("app-root");
        wkniFrame.getStyleClass().add("app-frame");
        wkniContent.getStyleClass().add("content");
        wkniCollapsed = "1".equals(wkniCfg.wkniGet("ui.sidebar.collapsed", "0"));
        wkniFrame.setLeft(wkniNav());
        VBox wkniCenter = new VBox(wkniTopbar(), wkniContent);
        VBox.setVgrow(wkniContent, Priority.ALWAYS);
        wkniFrame.setCenter(wkniCenter);
        wkniNotifier.wkniInstall(wkniToastLayer);
        wkniNotifier.wkniSetEnabled("1".equals(wkniCfg.wkniGet("ui.confirmaciones", "0")));
        wkniRoot.getChildren().addAll(wkniFrame, wkniToastLayer);
        wkniAplicarContraido(false);
        // Al cambiar de perfil, importar o restablecer, las ventanas se vuelven a construir con los ajustes nuevos.
        com.agendapersonal.settings.wkniSettingsStore.wkniGet().wkniAddListener(wkniSec -> {
            if (wkniSec.equals("*")) javafx.application.Platform.runLater(() -> { wkniCache.clear(); wkniGo(wkniActual); });
        });
        wkniGo("Inicio");
    }

    public StackPane wkniGetRoot() { return wkniRoot; }

    /** Atajos: Ctrl+B contrae el menú, Ctrl+Shift+L cambia claro/oscuro. */
    public void wkniInstalarAtajos(Scene wkniScene) {
        wkniScene.getAccelerators().put(new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN), this::wkniToggleSidebar);
        wkniScene.getAccelerators().put(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN), this::wkniPedirSalir);
        wkniScene.getAccelerators().put(new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
            this::wkniCambiarTema);
    }

    /** Navega a un módulo por su clave (lo usan las notificaciones: "Abrir agenda"). */
    public void wkniMostrar(String wkniTitulo) { wkniGo(wkniTitulo); }

    private Node wkniPage(String wkniKey, Supplier<Node> wkniBuild) {
        return wkniCache.computeIfAbsent(wkniKey, wkniK -> wkniBuild.get());
    }

    private Node wkniPanelPage() {
        return wkniPage("Inicio", () -> new wkniDashboardView(wkniC, wkniStage,
            () -> wkniEventViews.wkniForm(wkniStage, null, wkniC, () -> {}),
            wkniQ -> wkniEventViews.wkniManage(wkniQ == null ? "" : wkniQ, wkniC, wkniStage, () -> {}),
            () -> wkniTaskViews.wkniForm(null, wkniC, () -> {}),
            () -> wkniPrepararClase()).wkniGetView());
    }

    /** Abre el Time Blocking con la técnica de preparación de clase (solo para docentes). */
    private Runnable wkniPrepararClase() {
        return () -> {
            wkniPage("Time Blocking", this::wkniCrearTimeBlocking);
            if (wkniTimeBlocking != null) wkniTimeBlocking.wkniSeleccionar(wkniMetodoEnfoque.wkniDOCENTES.getFirst());
            wkniGo("Time Blocking");
        };
    }

    /** Crea el Time Blocking conservando la instancia de la vista para poder seleccionar métodos desde fuera. */
    private Node wkniCrearTimeBlocking() {
        wkniTimeBlocking = new wkniTimeBlockingView(wkniC);
        return wkniTimeBlocking.wkniGetView();
    }

    private void wkniGo(String wkniKey) {
        if (wkniKey.equals("Panel")) wkniKey = "Inicio";
        if (wkniKey.equals("Calendario")) wkniKey = "Agenda Unificada";   // el calendario y la agenda ahora son una sola ventana
        Node wkniN = switch (wkniKey) {
            case "Configuración" -> wkniPage(wkniKey, this::wkniSettingsPage);
            case "Agenda Unificada" -> wkniPage(wkniKey, () -> new wkniAgendaUnificadaView(wkniC, wkniStage).wkniGetView());
            case "Time Blocking" -> wkniPage(wkniKey, this::wkniCrearTimeBlocking);
            case "Espacio de Estudio" -> wkniPage(wkniKey, () -> wkniPerfil.wkniEsDocente()
                ? new wkniEspacioDocenteView(wkniC, wkniStage, () -> wkniGo("Time Blocking"), () -> wkniGo("Agenda Unificada")).wkniGetView()
                : new wkniEspacioEstudioView(wkniC).wkniGetView());
            case "Tareas y Proyectos" -> wkniPage(wkniKey, () -> new wkniTareasProyectosView(wkniC).wkniGetView());
            case "Notas y PKM" -> wkniPage(wkniKey, () -> new wkniNotasView().wkniGetView());
            case "CRM Personal" -> wkniPage(wkniKey, () -> new wkniCrmView().wkniGetView());
            case "Hábitos" -> wkniPage(wkniKey, () -> new wkniHabitosView().wkniGetView());
            case "Finanzas" -> wkniPage(wkniKey, () -> new wkniFinanzasView().wkniGetView());
            case "Salud" -> wkniPage(wkniKey, () -> new wkniSaludView().wkniGetView());
            default -> { wkniKey = "Inicio"; yield wkniPanelPage(); }
        };
        boolean wkniCambia = !wkniKey.equals(wkniActual);
        wkniActual = wkniKey;
        wkniContent.getChildren().setAll(wkniN);
        if (wkniCambia) wkniAnim.wkniPageIn(wkniN);
        wkniBotones.forEach((wkniK, wkniB) -> wkniB.pseudoClassStateChanged(wkniCURRENT, wkniK.equals(wkniActual)));
        wkniItem wkniIt = wkniItemsVisibles().stream().filter(wkniI -> wkniI.wkniKey().equals(wkniActual)).findFirst().orElse(wkniCONFIG);
        wkniBreadcrumb.setText(wkniIt.wkniSection() + "  ›  " + wkniIt.wkniLabel());
    }

    // ------------------------------------------------------------------ menú lateral

    private VBox wkniNav() {
        wkniSidebar = new VBox(4);
        wkniSidebar.getStyleClass().add("sidebar");
        wkniSidebar.setPadding(new Insets(20, 14, 16, 14));
        wkniSidebar.setMinWidth(Region.USE_PREF_SIZE);
        wkniSidebar.setMaxWidth(Region.USE_PREF_SIZE);
        wkniSidebar.setPrefWidth(wkniCollapsed ? wkniSIDEBAR_CLOSED : wkniSIDEBAR_OPEN);

        HBox wkniBrand = wkniLogo.wkniWordmark(36);
        Node wkniMarca = wkniBrand.getChildren().getFirst();
        Node wkniNombre = wkniBrand.getChildren().get(1);
        wkniSoloExpandido.add(wkniNombre);
        wkniBrand.setPadding(new Insets(0, 6, 18, 6));
        wkniBrand.setOnMouseClicked(wkniE -> wkniGo("Inicio"));
        wkniBrand.setCursor(javafx.scene.Cursor.HAND);
        wkniMarca.setAccessibleText("Compás - ir a inicio");
        wkniSidebar.getChildren().add(wkniBrand);

        VBox wkniLista = new VBox(2);
        String wkniSeccion = null;
        List<wkniItem> wkniVisibles = wkniItemsVisibles();
        for (wkniItem wkniIt : wkniVisibles) {
            if (!wkniIt.wkniSection().equals(wkniSeccion)) {
                wkniSeccion = wkniIt.wkniSection();
                Label wkniL = new Label(wkniSeccion.toUpperCase(wkniUi.wkniES));
                wkniL.getStyleClass().add("side-section");
                VBox.setMargin(wkniL, new Insets(14, 10, 6, 12));
                Region wkniLinea = new Region();
                wkniLinea.getStyleClass().add("side-divider");
                VBox.setMargin(wkniLinea, new Insets(12, 14, 8, 14));
                wkniSoloExpandido.add(wkniL);
                wkniSoloContraido.add(wkniLinea);
                if (wkniIt != wkniVisibles.getFirst()) wkniLista.getChildren().addAll(wkniL, wkniLinea);
                else wkniLista.getChildren().addAll(wkniL);
            }
            wkniLista.getChildren().add(wkniNavBtn(wkniIt));
        }
        ScrollPane wkniSc = new ScrollPane(wkniLista);
        wkniSc.setFitToWidth(true);
        wkniSc.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        wkniSc.getStyleClass().add("side-scroll");
        VBox.setVgrow(wkniSc, Priority.ALWAYS);

        wkniToggleSidebar = new Button("Contraer menú");
        wkniToggleSidebar.getStyleClass().addAll("nav-btn", "nav-toggle");
        wkniToggleSidebar.setMaxWidth(Double.MAX_VALUE);
        wkniToggleSidebar.setGraphicTextGap(12);
        wkniToggleSidebar.setMnemonicParsing(false);
        wkniToggleSidebar.setOnAction(wkniE -> wkniToggleSidebar());
        Button wkniConfig = wkniNavBtn(wkniCONFIG);
        wkniSidebar.getChildren().addAll(wkniSc, wkniToggleSidebar, wkniConfig);
        return wkniSidebar;
    }

    private Button wkniNavBtn(wkniItem wkniIt) {
        Button wkniBtn = new Button(wkniIt.wkniLabel(), wkniIconFactory.wkniIcon(wkniIt.wkniIcon(), 19));
        wkniBtn.getStyleClass().add("nav-btn");
        wkniBtn.setGraphicTextGap(12);
        wkniBtn.setMaxWidth(Double.MAX_VALUE);
        wkniBtn.setAlignment(Pos.CENTER_LEFT);
        wkniBtn.setMnemonicParsing(false);
        wkniBtn.setAccessibleText(wkniIt.wkniLabel());
        wkniBtn.setOnAction(wkniE -> wkniGo(wkniIt.wkniKey()));
        wkniBotones.put(wkniIt.wkniKey(), wkniBtn);
        return wkniBtn;
    }

    /** Contrae o expande el menú lateral (también con Ctrl+B). */
    public void wkniAlternarMenu() { wkniToggleSidebar(); }

    private void wkniToggleSidebar() {
        wkniCollapsed = !wkniCollapsed;
        wkniCfg.wkniSet("ui.sidebar.collapsed", wkniCollapsed ? "1" : "0");
        wkniAplicarContraido(true);
    }

    private void wkniAplicarContraido(boolean wkniAnimar) {
        double wkniTarget = wkniCollapsed ? wkniSIDEBAR_CLOSED : wkniSIDEBAR_OPEN;
        wkniSidebar.pseudoClassStateChanged(PseudoClass.getPseudoClass("collapsed"), wkniCollapsed);
        wkniToggleSidebar.setText(wkniCollapsed ? "Expandir menú" : "Contraer menú");
        double wkniIconSize = wkniCollapsed ? 22 : 19;
        wkniToggleSidebar.setGraphic(wkniIconFactory.wkniIcon(wkniCollapsed ? "sidebar-expand" : "sidebar-collapse", wkniIconSize));
        wkniBotones.forEach((wkniKey, wkniButton) -> {
            wkniItem wkni_item = wkniItemsVisibles().stream().filter(wkniCandidate -> wkniCandidate.wkniKey().equals(wkniKey)).findFirst().orElse(wkniCONFIG);
            wkniButton.setGraphic(wkniIconFactory.wkniIcon(wkni_item.wkniIcon(), wkniIconSize));
        });
        Runnable wkniTextos = () -> {
            for (Button wkniB : wkniTodosLosBotones()) {
                wkniB.setContentDisplay(wkniCollapsed ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
                wkniB.setAlignment(wkniCollapsed ? Pos.CENTER : Pos.CENTER_LEFT);
                if (wkniCollapsed) wkniUi.wkniTooltip(wkniB, wkniB.getText()); else wkniB.setTooltip(null);
            }
            wkniSoloExpandido.forEach(wkniN -> { wkniN.setVisible(!wkniCollapsed); wkniN.setManaged(!wkniCollapsed); });
            wkniSoloContraido.forEach(wkniN -> { wkniN.setVisible(wkniCollapsed); wkniN.setManaged(wkniCollapsed); });
        };
        if (!wkniAnimar) { wkniTextos.run(); wkniSidebar.setPrefWidth(wkniTarget); return; }
        if (wkniCollapsed) wkniTextos.run();          // al contraer, primero se ocultan los textos
        Timeline wkniT = new Timeline(new KeyFrame(Duration.millis(240),
            new KeyValue(wkniSidebar.prefWidthProperty(), wkniTarget, Interpolator.SPLINE(0.2, 0.9, 0.2, 1))));
        if (!wkniCollapsed) wkniT.setOnFinished(wkniE -> { wkniTextos.run(); wkniSoloExpandido.forEach(wkniN -> wkniAnim.wkniFadeIn(wkniN, 160)); });
        wkniT.play();
    }

    private void wkniSeleccionarRol(String wkniRol) {
        wkniPerfil.wkniGuardarRol(wkniRol);
        wkniCache.clear(); wkniBotones.clear(); wkniSoloExpandido.clear(); wkniSoloContraido.clear();
        wkniActual = null;
        wkniFrame.setLeft(wkniNav());
        wkniAplicarContraido(false);
        wkniGo("Inicio");
    }

    private List<Button> wkniTodosLosBotones() {
        List<Button> wkniL = new ArrayList<>(wkniBotones.values());
        wkniL.add(wkniToggleSidebar);
        return wkniL;
    }

    // ------------------------------------------------------------------ barra superior

    private HBox wkniTopbar() {
        HBox wkniBar = new HBox(10);
        wkniBar.getStyleClass().add("topbar");
        wkniBar.setAlignment(Pos.CENTER_LEFT);
        wkniBreadcrumb = new Label();
        wkniBreadcrumb.getStyleClass().add("breadcrumb");

        wkniDateLabel = new Label();
        wkniDateLabel.setGraphic(wkniIconFactory.wkniIcon("calendar", 16));
        wkniDateLabel.setGraphicTextGap(8);
        wkniDateLabel.getStyleClass().add("date-chip");
        Runnable wkni_fecha = () -> wkniDateLabel.setText(wkniUi.wkniFormatDateShort(LocalDate.now()));
        wkni_fecha.run();
        Timeline wkniReloj = new Timeline(new KeyFrame(Duration.minutes(1), wkniE -> wkni_fecha.run()));
        wkniReloj.setCycleCount(Timeline.INDEFINITE);
        wkniReloj.play();

        Button wkniTemaBtn = wkniUi.wkniIconButton(wkniTheme.wkniIsDark() ? "sun" : "moon",
            wkniTheme.wkniIsDark() ? "Cambiar a tema claro (Ctrl+Shift+L)" : "Cambiar a tema oscuro (Ctrl+Shift+L)");
        wkniTemaBtn.getStyleClass().add("topbar-button");
        wkniTemaBtn.setOnAction(wkniE -> wkniCambiarTema());
        wkniTheme.wkniRevisionProperty().addListener((wkniO, wkniA, wkniB) -> {
            Node wkniIc = wkniIconFactory.wkniIcon(wkniTheme.wkniIsDark() ? "sun" : "moon", 18);
            wkniTemaBtn.setGraphic(wkniIc);
            wkniUi.wkniTooltip(wkniTemaBtn, wkniTheme.wkniIsDark() ? "Cambiar a tema claro (Ctrl+Shift+L)" : "Cambiar a tema oscuro (Ctrl+Shift+L)");
            RotateTransition wkniR = new RotateTransition(Duration.millis(380), wkniIc);
            wkniR.setFromAngle(-90);
            wkniR.setToAngle(0);
            wkniR.setInterpolator(Interpolator.SPLINE(0.2, 0.9, 0.2, 1));
            wkniR.play();
        });

        Button wkniSalirBtn = wkniUi.wkniIconButton("power", "Salir de Compás (Ctrl+Q)");
        wkniSalirBtn.getStyleClass().add("topbar-button");
        wkniSalirBtn.setOnAction(wkniE -> wkniPedirSalir());
        wkniBar.getChildren().addAll(wkniBreadcrumb, wkniUi.wkniSpacer(), wkniDateLabel, wkniTemaBtn, wkniCampana(), wkniSalirBtn);
        return wkniBar;
    }

    private Runnable wkniAlSalir = () -> {};
    public void wkniSetAlSalir(Runnable wkniR) { wkniAlSalir = wkniR == null ? () -> {} : wkniR; }

    /** Pide confirmación (una sola vez por clic) y sale con la despedida. */
    private void wkniPedirSalir() {
        if (wkniUi.wkniConfirm(wkniRoot, "¿Salir de Compás?", "Dejarás de recibir avisos de eventos hasta que vuelvas a abrirlo. Para seguir con avisos, cierra la ventana con la X y quedará en la bandeja.", "Salir")) wkniAlSalir.run();
    }

    private void wkniCambiarTema() {
        wkniTheme.wkniToggleMode();   // sin aviso: el cambio se ve a simple vista
    }

    /** Campana con contador de avisos no leídos y su historial. */
    private StackPane wkniCampana() {
        Button wkniBell = wkniUi.wkniIconButton("notification", "Avisos recientes");
        wkniBell.getStyleClass().add("topbar-button");
        Label wkniBadge = new Label();
        wkniBadge.getStyleClass().add("bell-badge");
        wkniBadge.setMouseTransparent(true);
        wkniBadge.visibleProperty().bind(wkniNotifier.wkniUnreadProperty().greaterThan(0));
        wkniBadge.textProperty().bind(wkniNotifier.wkniUnreadProperty().asString());
        StackPane wkniS = new StackPane(wkniBell, wkniBadge);
        StackPane.setAlignment(wkniBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(wkniBadge, new Insets(-2, -3, 0, 0));

        Popup wkniPop = new Popup();
        wkniPop.setAutoHide(true);
        ListView<wkniNotifier.wkniEntry> wkniLista = new ListView<>(wkniNotifier.wkniHistory());
        wkniLista.getStyleClass().add("history-list");
        wkniLista.setPrefSize(360, 380);
        wkniLista.setPlaceholder(wkniUi.wkniEmpty("notification", "Sin avisos", "Aquí verás los cambios y recordatorios recientes."));
        DateTimeFormatter wkniHora = DateTimeFormatter.ofPattern("HH:mm");
        wkniLista.setCellFactory(wkniLv -> new ListCell<>() {
            @Override protected void updateItem(wkniNotifier.wkniEntry wkniE, boolean wkniEmpty) {
                super.updateItem(wkniE, wkniEmpty);
                if (wkniEmpty || wkniE == null) { setGraphic(null); setText(null); return; }
                StackPane wkniIc = new StackPane(wkniIconFactory.wkniIcon(wkniE.wkni_tone().wkniIcon(), 16));
                wkniIc.getStyleClass().addAll("toast-badge", wkniE.wkni_tone().wkniCss());
                Label wkniT = new Label(wkniE.wkniTitle());
                wkniT.getStyleClass().add("history-title");
                wkniT.setWrapText(true);
                VBox wkniTx = new VBox(2, wkniT);
                if (wkniE.wkniMessage() != null && !wkniE.wkniMessage().isBlank()) {
                    Label wkniM = new Label(wkniE.wkniMessage());
                    wkniM.getStyleClass().add("history-text");
                    wkniM.setWrapText(true);
                    wkniTx.getChildren().add(wkniM);
                }
                wkniTx.setMaxWidth(250);
                Label wkniH = new Label(wkniE.wkniWhen().format(wkniHora));
                wkniH.getStyleClass().add("caption");
                HBox wkniRow = new HBox(10, wkniIc, wkniTx, wkniUi.wkniSpacer(), wkniH);
                wkniRow.setAlignment(Pos.TOP_LEFT);
                setGraphic(wkniRow);
                setText(null);
            }
        });
        Label wkniTitulo = wkniUi.wkniH3("Avisos recientes");
        Button wkniLimpiar = wkniUi.wkniGhost(null, "Limpiar");
        wkniLimpiar.setOnAction(wkniE -> wkniNotifier.wkniHistory().clear());
        HBox wkniHead = new HBox(8, wkniTitulo, wkniUi.wkniSpacer(), wkniLimpiar);
        wkniHead.setAlignment(Pos.CENTER_LEFT);
        VBox wkni_panel = new VBox(10, wkniHead, wkniLista);
        wkni_panel.getStyleClass().add("popover");
        wkni_panel.setPadding(new Insets(14));
        wkniPop.getContent().add(wkni_panel);
        wkniBell.setOnAction(wkniE -> {
            if (wkniPop.isShowing()) { wkniPop.hide(); return; }
            var wkni_p = wkniBell.localToScreen(wkniBell.getBoundsInLocal());
            wkniPop.show(wkniStage, wkni_p.getMaxX() - 390, wkni_p.getMaxY() + 8);
            wkniAnim.wkniFadeIn(wkni_panel, 160);
            wkniNotifier.wkniMarkAllRead();
        });
        return wkniS;
    }

    // ------------------------------------------------------------------ configuración

    private Node wkniSettingsPage() {
        VBox wkniPage = wkniUi.wkniPage();
        wkniPage.getChildren().add(wkniUi.wkniPageHeader("settings", "Configuración",
            "Ajusta la apariencia y los avisos de Compás a tu gusto."));

        // Perfil
        javafx.scene.control.TextField wkniNombreCampo = new javafx.scene.control.TextField(com.agendapersonal.util.wkniPerfil.wkniNombre());
        wkniNombreCampo.setPromptText("Tu nombre");
        Button wkniGuardarNombre = wkniUi.wkniSecondary(null, "Guardar");
        wkniGuardarNombre.setOnAction(wkniE -> {
            com.agendapersonal.util.wkniPerfil.wkniGuardarNombre(wkniNombreCampo.getText());
            wkniNombreCampo.setText(com.agendapersonal.util.wkniPerfil.wkniNombre());
            wkniNotifier.wkniSuccess("Nombre guardado", null);
        });
        HBox wkniNombreFila = new HBox(10, wkniNombreCampo, wkniGuardarNombre);
        HBox.setHgrow(wkniNombreCampo, Priority.ALWAYS);
        javafx.scene.control.ComboBox<String> wkniRolUsuario = new javafx.scene.control.ComboBox<>();
        wkniRolUsuario.getItems().setAll("Estudiante", "Docente");
        wkniRolUsuario.setValue(wkniPerfil.wkniEsDocente() ? "Docente" : "Estudiante");
        Button wkniGuardarRol = wkniUi.wkniSecondary(null, "Cambiar espacio");
        wkniGuardarRol.setOnAction(wkniE -> {
            String wkniRol = "Docente".equals(wkniRolUsuario.getValue()) ? wkniPerfil.wkniDOCENTE : wkniPerfil.wkniESTUDIANTE;
            if (!wkniRol.equals(wkniPerfil.wkniRol())) wkniSeleccionarRol(wkniRol);
        });
        HBox wkniRolFila = new HBox(10, wkniRolUsuario, wkniGuardarRol);
        wkniRolFila.setAlignment(Pos.CENTER_LEFT);
        VBox wkni_perfil = wkniUi.wkniCard();
        wkni_perfil.getChildren().addAll(wkniUi.wkniH2("Tu perfil"),
            wkniUi.wkniMuted("Compás es solo para ti y se guarda en este equipo. El perfil cambia las herramientas del espacio de trabajo."),
            wkniUi.wkniField("Nombre", wkniNombreFila), wkniUi.wkniField("Usar Compás como", wkniRolFila));

        Button wkniBorrarDatos = wkniUi.wkniSecondary("trash", "Borrar todos mis datos");
        wkniBorrarDatos.setOnAction(wkniE -> wkniConfirmarBorradoTotal());
        VBox wkni_datos = wkniUi.wkniCard();
        wkni_datos.getChildren().addAll(wkniUi.wkniH2("Restablecer Compás"),
            wkniUi.wkniMuted("Borra tareas, eventos, bloques, notas, proyectos, contactos, hábitos, finanzas, salud, perfiles y preferencias. No se puede deshacer; después podrás configurar Compás de nuevo."),
            wkniBorrarDatos);

        // Reloj del inicio
        wkniSwitch wkniRelojVisible = new wkniSwitch("Mostrar el reloj grande", "Aparece en la pantalla de inicio.", wkniRelojPrefs.wkniVisible());
        wkniRelojVisible.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> wkniRelojPrefs.wkniSetVisible(wkniV));
        wkniSwitch wkniReloj24 = new wkniSwitch("Formato de 24 horas", "Si lo apagas se muestra con AM/PM.", wkniRelojPrefs.wkniFormato24());
        wkniReloj24.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> wkniRelojPrefs.wkniSetFormato24(wkniV));
        wkniSwitch wkniRelojSeg = new wkniSwitch("Mostrar segundos", null, wkniRelojPrefs.wkniSegundos());
        wkniRelojSeg.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> wkniRelojPrefs.wkniSetSegundos(wkniV));
        wkniUi.wkniSegmented wkniRelojTam = new wkniUi.wkniSegmented("Pequeño", "Mediano", "Grande");
        wkniRelojTam.wkniSelect(wkniRelojPrefs.wkniTamano().ordinal());
        wkniRelojTam.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> wkniRelojPrefs.wkniSetTamano(wkniRelojPrefs.wkniTamano.values()[wkniV.intValue()]));
        VBox wkniRelojCard = wkniUi.wkniCard();
        wkniRelojCard.getChildren().addAll(wkniUi.wkniH2("Reloj"), wkniRelojVisible, wkniSep(), wkniReloj24, wkniSep(), wkniRelojSeg, wkniSep(),
            wkniUi.wkniField("Tamaño", wkniRelojTam.wkniNode()));

        // Apariencia
        wkniUi.wkniSegmented wkniModo = new wkniUi.wkniSegmented("Claro", "Oscuro");
        wkniModo.wkniButton(0).setGraphic(wkniIconFactory.wkniIcon("sun", 16));
        wkniModo.wkniButton(1).setGraphic(wkniIconFactory.wkniIcon("moon", 16));
        wkniModo.wkniSelect(wkniTheme.wkniIsDark() ? 1 : 0);
        wkniModo.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> {
            wkniTheme.wkniSetMode(wkniV.intValue() == 1 ? wkniThemeService.wkniMode.OSCURO : wkniThemeService.wkniMode.CLARO);
        });
        wkniTheme.wkniRevisionProperty().addListener((wkniO, wkniA, wkniB) -> wkniModo.wkniSelect(wkniTheme.wkniIsDark() ? 1 : 0));

        FlowPane wkniSwatches = new FlowPane(10, 10);
        List<StackPane> wkniSw = new ArrayList<>();
        for (wkniThemeService.wkniAccent wkniAc : wkniThemeService.wkniAccent.values()) {
            Circle wkniDot = new Circle(13);
            wkniDot.setStyle("-fx-fill:" + wkniAc.wkniColor(wkniTheme.wkniIsDark()) + ";");
            Node wkniCheck = wkniIconFactory.wkniIcon("check", 16, "tint-on-accent");
            wkniCheck.setVisible(wkniAc == wkniTheme.wkniAccent());
            StackPane wkniS = new StackPane(wkniDot, wkniCheck);
            wkniS.getStyleClass().add("swatch");
            wkniS.setAccessibleText(wkniAc.toString());
            Label wkniName = new Label(wkniAc.toString());
            wkniName.getStyleClass().add("caption");
            VBox wkniCol = new VBox(6, wkniS, wkniName);
            wkniCol.setAlignment(Pos.CENTER);
            wkniCol.setOnMouseClicked(wkniE -> {
                wkniTheme.wkniSetAccent(wkniAc);
                wkniAnim.wkniPop(wkniS);
            });
            wkniCol.setCursor(javafx.scene.Cursor.HAND);
            wkniS.getProperties().put("accent", wkniAc);
            wkniSw.add(wkniS);
            wkniSwatches.getChildren().add(wkniCol);
        }
        wkniTheme.wkniRevisionProperty().addListener((wkniO, wkniA, wkniB) -> wkniSw.forEach(wkniS -> {
            wkniThemeService.wkniAccent wkniAc = (wkniThemeService.wkniAccent) wkniS.getProperties().get("accent");
            wkniS.getChildren().get(0).setStyle("-fx-fill:" + wkniAc.wkniColor(wkniTheme.wkniIsDark()) + ";");
            wkniS.getChildren().get(1).setVisible(wkniAc == wkniTheme.wkniAccent());
        }));

        VBox wkniApariencia = wkniUi.wkniCard();
        wkniApariencia.getChildren().addAll(wkniUi.wkniH2("Apariencia"),
            wkniUi.wkniMuted("Los cambios se aplican al instante en todas las ventanas y se guardan solos."),
            wkniUi.wkniField("Tema", wkniModo.wkniNode()), wkniUi.wkniField("Color de acento", wkniSwatches));

        // Tipografía
        javafx.scene.control.ComboBox<String> wkniFuente = new javafx.scene.control.ComboBox<>();
        wkniFuente.getItems().setAll(wkniFontPreferences.wkniOptions());
        wkniFuente.setValue(wkniFontPreferences.wkniFamily());
        wkniFuente.setCellFactory(wkniLv -> new ListCell<>() {
            @Override protected void updateItem(String wkniF, boolean wkniEmpty) {
                super.updateItem(wkniF, wkniEmpty);
                setText(wkniEmpty ? null : wkniF + (wkniF != null && wkniFontPreferences.wkniEsIncluida(wkniF) ? "  · incluida" : ""));
                setStyle(wkniEmpty || wkniF == null ? "" : "-fx-font-family:\"" + wkniF + "\";");
            }
        });
        wkniFuente.setOnAction(wkniE -> {
            if (wkniFuente.getValue() == null || wkniFuente.getValue().equals(wkniFontPreferences.wkniFamily())) return;
            wkniFontPreferences.wkniSetFamily(wkniFuente.getValue());
        });
        wkniUi.wkniSegmented wkniTam = new wkniUi.wkniSegmented("Pequeño", "Normal", "Grande", "Muy grande");
        wkniTam.wkniSelect(wkniFontPreferences.wkniSize().ordinal());
        wkniTam.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> {
            wkniFontPreferences.wkniSetSize(wkniFontPreferences.wkniSize.values()[wkniV.intValue()]);
        });
        Label wkniMuestra = new Label("Así se verán tus textos: Reunión con el equipo a las 10:30 · S/ 1.250,00");
        wkniMuestra.getStyleClass().add("font-preview");
        wkniMuestra.setWrapText(true);
        VBox wkniTipografia = wkniUi.wkniCard();
        wkniTipografia.getChildren().addAll(wkniUi.wkniH2("Tipografía"),
            wkniUi.wkniMuted("Las fuentes marcadas como «incluida» vienen con Compás y se ven igual en cualquier PC."),
            wkniUi.wkniField("Fuente", wkniFuente), wkniUi.wkniField("Tamaño del texto", wkniTam.wkniNode()), wkniMuestra);

        // Avisos
        wkniSwitch wkniCambios = new wkniSwitch("Confirmaciones al guardar", "Muestra un mensaje breve al guardar, eliminar o completar algo. Los recordatorios de eventos no dependen de esto.",
            wkniNotifier.wkniIsEnabled());
        wkniCambios.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> {
            wkniNotifier.wkniSetEnabled(wkniV);
            wkniCfg.wkniSet("ui.confirmaciones", wkniV ? "1" : "0");
        });
        wkniSwitch wkniEventos = new wkniSwitch("Avisar cuando empieza un evento",
            "Aparece una notificación en el escritorio justo a la hora de inicio, aunque Compás esté minimizado.",
            !"0".equals(wkniCfg.wkniGet(wkniCentroAvisos.wkniCFG_EVENTOS, "1")));
        wkniEventos.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> wkniCfg.wkniSet(wkniCentroAvisos.wkniCFG_EVENTOS, wkniV ? "1" : "0"));
        wkniSwitch wkniCumples = new wkniSwitch("Avisar cumpleaños del día", "Una sola notificación el mismo día del cumpleaños.",
            !"0".equals(wkniCfg.wkniGet(wkniCentroAvisos.wkniCFG_CUMPLES, "1")));
        wkniCumples.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> wkniCfg.wkniSet(wkniCentroAvisos.wkniCFG_CUMPLES, wkniV ? "1" : "0"));
        wkniSwitch wkni_resumen = new wkniSwitch("Resumen al abrir Compás",
            "Ventana con los pendientes del día al iniciar. También está en el ícono junto al reloj.",
            "1".equals(wkniCfg.wkniGet(wkniCentroAvisos.wkniCFG_RESUMEN, "0")));
        wkni_resumen.wkniSelectedProperty().addListener((wkniO, wkniA, wkniV) -> wkniCfg.wkniSet(wkniCentroAvisos.wkniCFG_RESUMEN, wkniV ? "1" : "0"));
        VBox wkniAvisos = wkniUi.wkniCard();
        wkniAvisos.getChildren().addAll(wkniUi.wkniH2("Avisos"),
            wkniUi.wkniMuted("Los avisos de escritorio solo salen para lo que está pasando en el momento."),
            wkniCambios, wkniSep(), wkniEventos, wkniSep(), wkniCumples, wkniSep(), wkni_resumen);

        // Acerca de
        HBox wkniAbout = new HBox(16, wkniLogo.wkniMark(56), new VBox(4, wkniUi.wkniH2("Compás 1.1"),
            wkniUi.wkniMuted("Tu tiempo, mejor invertido. Atajos: Ctrl+B contrae el menú · Ctrl+Shift+L cambia el tema.")));
        wkniAbout.setAlignment(Pos.CENTER_LEFT);
        VBox wkniAcerca = wkniUi.wkniCard();
        wkniAcerca.getChildren().add(wkniAbout);

        HBox wkniFila = new HBox(20, wkniApariencia, wkniTipografia);
        HBox.setHgrow(wkniApariencia, Priority.ALWAYS);
        HBox.setHgrow(wkniTipografia, Priority.ALWAYS);
        wkniApariencia.setMaxWidth(Double.MAX_VALUE);
        wkniTipografia.setMaxWidth(Double.MAX_VALUE);
        wkniApariencia.setPrefWidth(400);
        wkniTipografia.setPrefWidth(400);
        HBox wkniFila2 = new HBox(20, wkni_perfil, wkniRelojCard);
        HBox.setHgrow(wkni_perfil, Priority.ALWAYS);
        HBox.setHgrow(wkniRelojCard, Priority.ALWAYS);
        wkni_perfil.setPrefWidth(400);
        wkniRelojCard.setPrefWidth(400);
        wkniPage.getChildren().addAll(wkniFila2, wkniFila,
            wkni_datos,
            com.agendapersonal.ui.settings.wkniPersonalizacionCards.wkniTemas(),
            com.agendapersonal.ui.settings.wkniPersonalizacionCards.wkniCrearTema(),
            com.agendapersonal.ui.settings.wkniPersonalizacionCards.wkniForma(),
            com.agendapersonal.ui.settings.wkniPersonalizacionCards.wkniPerfiles(),
            com.agendapersonal.ui.settings.wkniPersonalizacionCards.wkniRespaldo(),
            wkniAvisos, com.agendapersonal.ui.settings.wkniPersonalizacionCards.wkniSilencio(), wkniAcerca);
        return wkniUi.wkniScroll(wkniPage);
    }

    private void wkniConfirmarBorradoTotal() {
        javafx.scene.control.Alert wkniConfirmacion = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.CONFIRMATION);
        wkniConfirmacion.initOwner(wkniStage);
        wkniConfirmacion.setTitle("Borrar todos los datos");
        wkniConfirmacion.setHeaderText("¿Restablecer Compás por completo?");
        wkniConfirmacion.setContentText("Se eliminarán permanentemente tus registros y preferencias. A continuación podrás volver a elegir tu nombre y perfil.");
        javafx.scene.control.ButtonType wkniBorrar = new javafx.scene.control.ButtonType(
            "Borrar todo", javafx.scene.control.ButtonBar.ButtonData.OTHER);
        wkniConfirmacion.getButtonTypes().setAll(wkniBorrar, javafx.scene.control.ButtonType.CANCEL);
        wkniConfirmacion.setOnShown(wkniE -> wkniTheme.wkniApplyTo(wkniConfirmacion.getDialogPane().getScene()));
        if (wkniConfirmacion.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL) != wkniBorrar) return;

        try {
            com.agendapersonal.infrastructure.db.wkniDatabase.wkniBorrarTodosLosDatos();
            wkniTheme.wkniSettings().wkniResetAll();
            wkniC.wkniCargarTodo();
            com.agendapersonal.ui.wkniBienvenidaView.wkniMostrarSiHaceFalta();
            wkniSeleccionarRol(wkniPerfil.wkniRol());

            javafx.scene.control.Alert wkniListo = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION,
                "Compás quedó restablecido. Tus datos anteriores ya se eliminaron.");
            wkniListo.initOwner(wkniStage);
            wkniListo.setTitle("Datos borrados");
            wkniListo.setHeaderText("Restablecimiento completado");
            wkniListo.setOnShown(wkniE -> wkniTheme.wkniApplyTo(wkniListo.getDialogPane().getScene()));
            wkniListo.showAndWait();
        } catch (RuntimeException wkniEx) {
            javafx.scene.control.Alert wkniError = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR,
                "No se pudieron borrar los datos. " + wkniEx.getMessage());
            wkniError.initOwner(wkniStage);
            wkniError.setTitle("No se pudo restablecer Compás");
            wkniError.showAndWait();
        }
    }

    private static Region wkniSep() {
        Region wkniR = new Region();
        wkniR.getStyleClass().add("hairline");
        return wkniR;
    }
}

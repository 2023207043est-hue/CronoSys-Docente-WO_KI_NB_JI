package com.agendapersonal.ui.shell;

import com.agendapersonal.controller.WO_KI_NB_JI_MainController;
import com.agendapersonal.infrastructure.db.WO_KI_NB_JI_ConfigDAO;
import com.agendapersonal.ui.WO_KI_NB_JI_Anim;
import com.agendapersonal.ui.WO_KI_NB_JI_DashboardView;
import com.agendapersonal.ui.WO_KI_NB_JI_EventViews;
import com.agendapersonal.ui.WO_KI_NB_JI_FontPreferences;
import com.agendapersonal.ui.WO_KI_NB_JI_IconFactory;
import com.agendapersonal.ui.WO_KI_NB_JI_Logo;
import com.agendapersonal.ui.WO_KI_NB_JI_Notifier;
import com.agendapersonal.ui.WO_KI_NB_JI_RelojPrefs;
import com.agendapersonal.ui.WO_KI_NB_JI_Switch;
import com.agendapersonal.ui.WO_KI_NB_JI_TaskViews;
import com.agendapersonal.ui.WO_KI_NB_JI_ThemeService;
import com.agendapersonal.ui.modules.WO_KI_NB_JI_AgendaUnificadaView;
import com.agendapersonal.ui.modules.WO_KI_NB_JI_CalendarioModule;
import com.agendapersonal.ui.modules.WO_KI_NB_JI_CrmView;
import com.agendapersonal.ui.modules.WO_KI_NB_JI_EspacioEstudioView;
import com.agendapersonal.ui.modules.WO_KI_NB_JI_FinanzasView;
import com.agendapersonal.ui.modules.WO_KI_NB_JI_HabitosView;
import com.agendapersonal.ui.modules.WO_KI_NB_JI_NotasView;
import com.agendapersonal.ui.modules.WO_KI_NB_JI_SaludView;
import com.agendapersonal.ui.modules.WO_KI_NB_JI_TareasProyectosView;
import com.agendapersonal.ui.modules.WO_KI_NB_JI_TimeBlockingView;
import com.agendapersonal.ui.modules.WO_KI_NB_JI_EspacioDocenteView;
import com.agendapersonal.ui.notify.WO_KI_NB_JI_CentroAvisos;
import com.agendapersonal.service.WO_KI_NB_JI_MetodoEnfoque;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
import com.agendapersonal.util.WO_KI_NB_JI_Perfil;
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
public class WO_KI_NB_JI_AppShell {
    private static final double SIDEBAR_OPEN = 252, SIDEBAR_CLOSED = 76;
    private static final PseudoClass CURRENT = PseudoClass.getPseudoClass("current");

    /** Páginas del menú: clave interna → texto visible, ícono y sección. */
    private record WO_KI_NB_JI_Item(String key, String label, String icon, String section) {}
    private static final List<WO_KI_NB_JI_Item> ITEMS = List.of(
        new WO_KI_NB_JI_Item("Inicio", "Inicio", "home", "Principal"),
        new WO_KI_NB_JI_Item("Agenda Unificada", "Calendario", "agenda", "Principal"),
        new WO_KI_NB_JI_Item("Time Blocking", "Time blocking", "clock", "Principal"),
        new WO_KI_NB_JI_Item("Espacio de Estudio", "Estudio", "book", "Principal"),
        new WO_KI_NB_JI_Item("Tareas y Proyectos", "Tareas", "tasks", "Organizar"),
        new WO_KI_NB_JI_Item("Notas y PKM", "Notas", "notes", "Organizar"),
        new WO_KI_NB_JI_Item("CRM Personal", "Contactos", "contacts", "Personal"),
        new WO_KI_NB_JI_Item("Hábitos", "Hábitos", "habits", "Personal"),
        new WO_KI_NB_JI_Item("Finanzas", "Finanzas", "finance", "Personal"),
        new WO_KI_NB_JI_Item("Salud", "Salud", "health", "Personal"));
    private static final WO_KI_NB_JI_Item CONFIG = new WO_KI_NB_JI_Item("Configuración", "Configuración", "settings", "Ajustes");

    private List<WO_KI_NB_JI_Item> WO_KI_NB_JI_itemsVisibles() {
        if (!WO_KI_NB_JI_Perfil.WO_KI_NB_JI_esDocente()) return ITEMS;
        return ITEMS.stream().map(i -> i.key().equals("Espacio de Estudio")
            ? new WO_KI_NB_JI_Item(i.key(), "Espacio Docente", "book", i.section()) : i).toList();
    }

    /** Cada sección de ajustes corresponde a una página: al cambiarla, la página se rehace. */
    private static final Map<String, String> PAGINA_POR_SECCION = Map.ofEntries(
        Map.entry("home", "Inicio"), Map.entry("calendar", "Agenda Unificada"),
        Map.entry("timeBlocking", "Time Blocking"), Map.entry("study", "Espacio de Estudio"),
        Map.entry("tasks", "Tareas y Proyectos"), Map.entry("notes", "Notas y PKM"),
        Map.entry("contacts", "CRM Personal"), Map.entry("habits", "Hábitos"),
        Map.entry("finance", "Finanzas"), Map.entry("health", "Salud"));

    private final WO_KI_NB_JI_MainController c;
    private final Stage stage;
    private final WO_KI_NB_JI_ThemeService theme = WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_get();
    private final WO_KI_NB_JI_ConfigDAO cfg = new WO_KI_NB_JI_ConfigDAO();
    private final StackPane root = new StackPane();
    private final BorderPane frame = new BorderPane();
    private final StackPane content = new StackPane();
    private final VBox toastLayer = new VBox();
    private final Map<String, Node> cache = new HashMap<>();
    private final Map<String, Button> botones = new LinkedHashMap<>();
    private final List<Node> soloExpandido = new ArrayList<>();
    private final List<Node> soloContraido = new ArrayList<>();
    private WO_KI_NB_JI_TimeBlockingView timeBlocking;
    private VBox sidebar;
    private Button toggleSidebar;
    private Label breadcrumb;
    private Label dateLabel;
    private boolean collapsed;
    private String actual;

    public WO_KI_NB_JI_AppShell(WO_KI_NB_JI_MainController c, Stage stage) {
        this.c = c; this.stage = stage;
        root.getStyleClass().add("app-root");
        frame.getStyleClass().add("app-frame");
        content.getStyleClass().add("content");
        collapsed = "1".equals(cfg.WO_KI_NB_JI_get("ui.sidebar.collapsed", "0"));
        frame.setLeft(WO_KI_NB_JI_nav());
        VBox center = new VBox(WO_KI_NB_JI_topbar(), content);
        VBox.setVgrow(content, Priority.ALWAYS);
        frame.setCenter(center);
        WO_KI_NB_JI_Notifier.WO_KI_NB_JI_install(toastLayer);
        WO_KI_NB_JI_Notifier.WO_KI_NB_JI_setEnabled("1".equals(cfg.WO_KI_NB_JI_get("ui.confirmaciones", "0")));
        root.getChildren().addAll(frame, toastLayer);
        WO_KI_NB_JI_aplicarContraido(false);
        // Al cambiar de perfil, importar o restablecer, las ventanas se vuelven a construir con los ajustes nuevos.
        com.agendapersonal.settings.WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_get().WO_KI_NB_JI_addListener(sec -> {
            if (sec.equals("*")) javafx.application.Platform.runLater(() -> { cache.clear(); WO_KI_NB_JI_go(actual); });
        });
        WO_KI_NB_JI_go("Inicio");
    }

    public StackPane WO_KI_NB_JI_getRoot() { return root; }

    /** Atajos: Ctrl+B contrae el menú, Ctrl+Shift+L cambia claro/oscuro. */
    public void WO_KI_NB_JI_instalarAtajos(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.B, KeyCombination.SHORTCUT_DOWN), this::WO_KI_NB_JI_toggleSidebar);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.Q, KeyCombination.SHORTCUT_DOWN), this::WO_KI_NB_JI_pedirSalir);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
            this::WO_KI_NB_JI_cambiarTema);
    }

    /** Navega a un módulo por su clave (lo usan las notificaciones: "Abrir agenda"). */
    public void WO_KI_NB_JI_mostrar(String titulo) { WO_KI_NB_JI_go(titulo); }

    private Node WO_KI_NB_JI_page(String key, Supplier<Node> build) {
        return cache.computeIfAbsent(key, k -> build.get());
    }

    private Node WO_KI_NB_JI_panelPage() {
        return WO_KI_NB_JI_page("Inicio", () -> new WO_KI_NB_JI_DashboardView(c, stage,
            () -> WO_KI_NB_JI_EventViews.WO_KI_NB_JI_form(stage, null, c, () -> {}),
            q -> WO_KI_NB_JI_EventViews.WO_KI_NB_JI_manage(q == null ? "" : q, c, stage, () -> {}),
            () -> WO_KI_NB_JI_TaskViews.WO_KI_NB_JI_form(null, c, () -> {}),
            () -> WO_KI_NB_JI_prepararClase()).WO_KI_NB_JI_getView());
    }

    /** Abre el Time Blocking con la técnica de preparación de clase (solo para docentes). */
    private Runnable WO_KI_NB_JI_prepararClase() {
        return () -> {
            WO_KI_NB_JI_page("Time Blocking", this::WO_KI_NB_JI_crearTimeBlocking);
            if (timeBlocking != null) timeBlocking.WO_KI_NB_JI_seleccionar(WO_KI_NB_JI_MetodoEnfoque.DOCENTES.getFirst());
            WO_KI_NB_JI_go("Time Blocking");
        };
    }

    /** Crea el Time Blocking conservando la instancia de la vista para poder seleccionar métodos desde fuera. */
    private Node WO_KI_NB_JI_crearTimeBlocking() {
        timeBlocking = new WO_KI_NB_JI_TimeBlockingView(c);
        return timeBlocking.WO_KI_NB_JI_getView();
    }

    private void WO_KI_NB_JI_go(String key) {
        if (key.equals("Panel")) key = "Inicio";
        if (key.equals("Calendario")) key = "Agenda Unificada";   // el calendario y la agenda ahora son una sola ventana
        Node n = switch (key) {
            case "Configuración" -> WO_KI_NB_JI_page(key, this::WO_KI_NB_JI_settingsPage);
            case "Agenda Unificada" -> WO_KI_NB_JI_page(key, () -> new WO_KI_NB_JI_AgendaUnificadaView(c, stage).WO_KI_NB_JI_getView());
            case "Time Blocking" -> WO_KI_NB_JI_page(key, this::WO_KI_NB_JI_crearTimeBlocking);
            case "Espacio de Estudio" -> WO_KI_NB_JI_page(key, () -> WO_KI_NB_JI_Perfil.WO_KI_NB_JI_esDocente()
                ? new WO_KI_NB_JI_EspacioDocenteView(c, stage, () -> WO_KI_NB_JI_go("Time Blocking"), () -> WO_KI_NB_JI_go("Agenda Unificada")).WO_KI_NB_JI_getView()
                : new WO_KI_NB_JI_EspacioEstudioView(c).WO_KI_NB_JI_getView());
            case "Tareas y Proyectos" -> WO_KI_NB_JI_page(key, () -> new WO_KI_NB_JI_TareasProyectosView(c).WO_KI_NB_JI_getView());
            case "Notas y PKM" -> WO_KI_NB_JI_page(key, () -> new WO_KI_NB_JI_NotasView().WO_KI_NB_JI_getView());
            case "CRM Personal" -> WO_KI_NB_JI_page(key, () -> new WO_KI_NB_JI_CrmView().WO_KI_NB_JI_getView());
            case "Hábitos" -> WO_KI_NB_JI_page(key, () -> new WO_KI_NB_JI_HabitosView().WO_KI_NB_JI_getView());
            case "Finanzas" -> WO_KI_NB_JI_page(key, () -> new WO_KI_NB_JI_FinanzasView().WO_KI_NB_JI_getView());
            case "Salud" -> WO_KI_NB_JI_page(key, () -> new WO_KI_NB_JI_SaludView().WO_KI_NB_JI_getView());
            default -> { key = "Inicio"; yield WO_KI_NB_JI_panelPage(); }
        };
        boolean cambia = !key.equals(actual);
        actual = key;
        content.getChildren().setAll(n);
        if (cambia) WO_KI_NB_JI_Anim.WO_KI_NB_JI_pageIn(n);
        botones.forEach((k, b) -> b.pseudoClassStateChanged(CURRENT, k.equals(actual)));
        WO_KI_NB_JI_Item it = WO_KI_NB_JI_itemsVisibles().stream().filter(i -> i.key().equals(actual)).findFirst().orElse(CONFIG);
        breadcrumb.setText(it.section() + "  ›  " + it.label());
    }

    // ------------------------------------------------------------------ menú lateral

    private VBox WO_KI_NB_JI_nav() {
        sidebar = new VBox(4);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPadding(new Insets(20, 14, 16, 14));
        sidebar.setMinWidth(Region.USE_PREF_SIZE);
        sidebar.setMaxWidth(Region.USE_PREF_SIZE);
        sidebar.setPrefWidth(collapsed ? SIDEBAR_CLOSED : SIDEBAR_OPEN);

        HBox brand = WO_KI_NB_JI_Logo.WO_KI_NB_JI_wordmark(36);
        Node marca = brand.getChildren().getFirst();
        Node nombre = brand.getChildren().get(1);
        soloExpandido.add(nombre);
        brand.setPadding(new Insets(0, 6, 18, 6));
        brand.setOnMouseClicked(e -> WO_KI_NB_JI_go("Inicio"));
        brand.setCursor(javafx.scene.Cursor.HAND);
        marca.setAccessibleText("Compás - ir a inicio");
        sidebar.getChildren().add(brand);

        VBox lista = new VBox(2);
        String seccion = null;
        List<WO_KI_NB_JI_Item> visibles = WO_KI_NB_JI_itemsVisibles();
        for (WO_KI_NB_JI_Item it : visibles) {
            if (!it.section().equals(seccion)) {
                seccion = it.section();
                Label l = new Label(seccion.toUpperCase(WO_KI_NB_JI_Ui.ES));
                l.getStyleClass().add("side-section");
                VBox.setMargin(l, new Insets(14, 10, 6, 12));
                Region linea = new Region();
                linea.getStyleClass().add("side-divider");
                VBox.setMargin(linea, new Insets(12, 14, 8, 14));
                soloExpandido.add(l);
                soloContraido.add(linea);
                if (it != visibles.getFirst()) lista.getChildren().addAll(l, linea);
                else lista.getChildren().addAll(l);
            }
            lista.getChildren().add(WO_KI_NB_JI_navBtn(it));
        }
        ScrollPane sc = new ScrollPane(lista);
        sc.setFitToWidth(true);
        sc.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sc.getStyleClass().add("side-scroll");
        VBox.setVgrow(sc, Priority.ALWAYS);

        toggleSidebar = new Button("Contraer menú");
        toggleSidebar.getStyleClass().addAll("nav-btn", "nav-toggle");
        toggleSidebar.setMaxWidth(Double.MAX_VALUE);
        toggleSidebar.setGraphicTextGap(12);
        toggleSidebar.setMnemonicParsing(false);
        toggleSidebar.setOnAction(e -> WO_KI_NB_JI_toggleSidebar());
        Button config = WO_KI_NB_JI_navBtn(CONFIG);
        sidebar.getChildren().addAll(sc, toggleSidebar, config);
        return sidebar;
    }

    private Button WO_KI_NB_JI_navBtn(WO_KI_NB_JI_Item it) {
        Button btn = new Button(it.label(), WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon(it.icon(), 19));
        btn.getStyleClass().add("nav-btn");
        btn.setGraphicTextGap(12);
        btn.setMaxWidth(Double.MAX_VALUE);
        btn.setAlignment(Pos.CENTER_LEFT);
        btn.setMnemonicParsing(false);
        btn.setAccessibleText(it.label());
        btn.setOnAction(e -> WO_KI_NB_JI_go(it.key()));
        botones.put(it.key(), btn);
        return btn;
    }

    /** Contrae o expande el menú lateral (también con Ctrl+B). */
    public void WO_KI_NB_JI_alternarMenu() { WO_KI_NB_JI_toggleSidebar(); }

    private void WO_KI_NB_JI_toggleSidebar() {
        collapsed = !collapsed;
        cfg.WO_KI_NB_JI_set("ui.sidebar.collapsed", collapsed ? "1" : "0");
        WO_KI_NB_JI_aplicarContraido(true);
    }

    private void WO_KI_NB_JI_aplicarContraido(boolean animar) {
        double target = collapsed ? SIDEBAR_CLOSED : SIDEBAR_OPEN;
        sidebar.pseudoClassStateChanged(PseudoClass.getPseudoClass("collapsed"), collapsed);
        toggleSidebar.setText(collapsed ? "Expandir menú" : "Contraer menú");
        double iconSize = collapsed ? 22 : 19;
        toggleSidebar.setGraphic(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon(collapsed ? "sidebar-expand" : "sidebar-collapse", iconSize));
        botones.forEach((key, button) -> {
            WO_KI_NB_JI_Item item = WO_KI_NB_JI_itemsVisibles().stream().filter(candidate -> candidate.key().equals(key)).findFirst().orElse(CONFIG);
            button.setGraphic(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon(item.icon(), iconSize));
        });
        Runnable textos = () -> {
            for (Button b : WO_KI_NB_JI_todosLosBotones()) {
                b.setContentDisplay(collapsed ? ContentDisplay.GRAPHIC_ONLY : ContentDisplay.LEFT);
                b.setAlignment(collapsed ? Pos.CENTER : Pos.CENTER_LEFT);
                if (collapsed) WO_KI_NB_JI_Ui.WO_KI_NB_JI_tooltip(b, b.getText()); else b.setTooltip(null);
            }
            soloExpandido.forEach(n -> { n.setVisible(!collapsed); n.setManaged(!collapsed); });
            soloContraido.forEach(n -> { n.setVisible(collapsed); n.setManaged(collapsed); });
        };
        if (!animar) { textos.run(); sidebar.setPrefWidth(target); return; }
        if (collapsed) textos.run();          // al contraer, primero se ocultan los textos
        Timeline t = new Timeline(new KeyFrame(Duration.millis(240),
            new KeyValue(sidebar.prefWidthProperty(), target, Interpolator.SPLINE(0.2, 0.9, 0.2, 1))));
        if (!collapsed) t.setOnFinished(e -> { textos.run(); soloExpandido.forEach(n -> WO_KI_NB_JI_Anim.WO_KI_NB_JI_fadeIn(n, 160)); });
        t.play();
    }

    private void WO_KI_NB_JI_seleccionarRol(String rol) {
        WO_KI_NB_JI_Perfil.WO_KI_NB_JI_guardarRol(rol);
        cache.clear(); botones.clear(); soloExpandido.clear(); soloContraido.clear();
        actual = null;
        frame.setLeft(WO_KI_NB_JI_nav());
        WO_KI_NB_JI_aplicarContraido(false);
        WO_KI_NB_JI_go("Inicio");
    }

    private List<Button> WO_KI_NB_JI_todosLosBotones() {
        List<Button> l = new ArrayList<>(botones.values());
        l.add(toggleSidebar);
        return l;
    }

    // ------------------------------------------------------------------ barra superior

    private HBox WO_KI_NB_JI_topbar() {
        HBox bar = new HBox(10);
        bar.getStyleClass().add("topbar");
        bar.setAlignment(Pos.CENTER_LEFT);
        breadcrumb = new Label();
        breadcrumb.getStyleClass().add("breadcrumb");

        dateLabel = new Label();
        dateLabel.setGraphic(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("calendar", 16));
        dateLabel.setGraphicTextGap(8);
        dateLabel.getStyleClass().add("date-chip");
        Runnable fecha = () -> dateLabel.setText(WO_KI_NB_JI_Ui.WO_KI_NB_JI_formatDateShort(LocalDate.now()));
        fecha.run();
        Timeline reloj = new Timeline(new KeyFrame(Duration.minutes(1), e -> fecha.run()));
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();

        Button temaBtn = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton(theme.WO_KI_NB_JI_isDark() ? "sun" : "moon",
            theme.WO_KI_NB_JI_isDark() ? "Cambiar a tema claro (Ctrl+Shift+L)" : "Cambiar a tema oscuro (Ctrl+Shift+L)");
        temaBtn.getStyleClass().add("topbar-button");
        temaBtn.setOnAction(e -> WO_KI_NB_JI_cambiarTema());
        theme.WO_KI_NB_JI_revisionProperty().addListener((o, a, b) -> {
            Node ic = WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon(theme.WO_KI_NB_JI_isDark() ? "sun" : "moon", 18);
            temaBtn.setGraphic(ic);
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_tooltip(temaBtn, theme.WO_KI_NB_JI_isDark() ? "Cambiar a tema claro (Ctrl+Shift+L)" : "Cambiar a tema oscuro (Ctrl+Shift+L)");
            RotateTransition r = new RotateTransition(Duration.millis(380), ic);
            r.setFromAngle(-90);
            r.setToAngle(0);
            r.setInterpolator(Interpolator.SPLINE(0.2, 0.9, 0.2, 1));
            r.play();
        });

        Button salirBtn = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("power", "Salir de Compás (Ctrl+Q)");
        salirBtn.getStyleClass().add("topbar-button");
        salirBtn.setOnAction(e -> WO_KI_NB_JI_pedirSalir());
        bar.getChildren().addAll(breadcrumb, WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), dateLabel, temaBtn, WO_KI_NB_JI_campana(), salirBtn);
        return bar;
    }

    private Runnable alSalir = () -> {};
    public void WO_KI_NB_JI_setAlSalir(Runnable r) { alSalir = r == null ? () -> {} : r; }

    /** Pide confirmación (una sola vez por clic) y sale con la despedida. */
    private void WO_KI_NB_JI_pedirSalir() {
        if (WO_KI_NB_JI_Ui.WO_KI_NB_JI_confirm(root, "¿Salir de Compás?", "Dejarás de recibir avisos de eventos hasta que vuelvas a abrirlo. Para seguir con avisos, cierra la ventana con la X y quedará en la bandeja.", "Salir")) alSalir.run();
    }

    private void WO_KI_NB_JI_cambiarTema() {
        theme.WO_KI_NB_JI_toggleMode();   // sin aviso: el cambio se ve a simple vista
    }

    /** Campana con contador de avisos no leídos y su historial. */
    private StackPane WO_KI_NB_JI_campana() {
        Button bell = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("notification", "Avisos recientes");
        bell.getStyleClass().add("topbar-button");
        Label badge = new Label();
        badge.getStyleClass().add("bell-badge");
        badge.setMouseTransparent(true);
        badge.visibleProperty().bind(WO_KI_NB_JI_Notifier.WO_KI_NB_JI_unreadProperty().greaterThan(0));
        badge.textProperty().bind(WO_KI_NB_JI_Notifier.WO_KI_NB_JI_unreadProperty().asString());
        StackPane s = new StackPane(bell, badge);
        StackPane.setAlignment(badge, Pos.TOP_RIGHT);
        StackPane.setMargin(badge, new Insets(-2, -3, 0, 0));

        Popup pop = new Popup();
        pop.setAutoHide(true);
        ListView<WO_KI_NB_JI_Notifier.WO_KI_NB_JI_Entry> lista = new ListView<>(WO_KI_NB_JI_Notifier.WO_KI_NB_JI_history());
        lista.getStyleClass().add("history-list");
        lista.setPrefSize(360, 380);
        lista.setPlaceholder(WO_KI_NB_JI_Ui.WO_KI_NB_JI_empty("notification", "Sin avisos", "Aquí verás los cambios y recordatorios recientes."));
        DateTimeFormatter hora = DateTimeFormatter.ofPattern("HH:mm");
        lista.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(WO_KI_NB_JI_Notifier.WO_KI_NB_JI_Entry e, boolean empty) {
                super.updateItem(e, empty);
                if (empty || e == null) { setGraphic(null); setText(null); return; }
                StackPane ic = new StackPane(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon(e.tone().WO_KI_NB_JI_icon(), 16));
                ic.getStyleClass().addAll("toast-badge", e.tone().WO_KI_NB_JI_css());
                Label t = new Label(e.title());
                t.getStyleClass().add("history-title");
                t.setWrapText(true);
                VBox tx = new VBox(2, t);
                if (e.message() != null && !e.message().isBlank()) {
                    Label m = new Label(e.message());
                    m.getStyleClass().add("history-text");
                    m.setWrapText(true);
                    tx.getChildren().add(m);
                }
                tx.setMaxWidth(250);
                Label h = new Label(e.when().format(hora));
                h.getStyleClass().add("caption");
                HBox row = new HBox(10, ic, tx, WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), h);
                row.setAlignment(Pos.TOP_LEFT);
                setGraphic(row);
                setText(null);
            }
        });
        Label titulo = WO_KI_NB_JI_Ui.WO_KI_NB_JI_h3("Avisos recientes");
        Button limpiar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_ghost(null, "Limpiar");
        limpiar.setOnAction(e -> WO_KI_NB_JI_Notifier.WO_KI_NB_JI_history().clear());
        HBox head = new HBox(8, titulo, WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), limpiar);
        head.setAlignment(Pos.CENTER_LEFT);
        VBox panel = new VBox(10, head, lista);
        panel.getStyleClass().add("popover");
        panel.setPadding(new Insets(14));
        pop.getContent().add(panel);
        bell.setOnAction(e -> {
            if (pop.isShowing()) { pop.hide(); return; }
            var p = bell.localToScreen(bell.getBoundsInLocal());
            pop.show(stage, p.getMaxX() - 390, p.getMaxY() + 8);
            WO_KI_NB_JI_Anim.WO_KI_NB_JI_fadeIn(panel, 160);
            WO_KI_NB_JI_Notifier.WO_KI_NB_JI_markAllRead();
        });
        return s;
    }

    // ------------------------------------------------------------------ configuración

    private Node WO_KI_NB_JI_settingsPage() {
        VBox page = WO_KI_NB_JI_Ui.WO_KI_NB_JI_page();
        page.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pageHeader("settings", "Configuración",
            "Ajusta la apariencia y los avisos de Compás a tu gusto."));

        // Perfil
        javafx.scene.control.TextField nombreCampo = new javafx.scene.control.TextField(com.agendapersonal.util.WO_KI_NB_JI_Perfil.WO_KI_NB_JI_nombre());
        nombreCampo.setPromptText("Tu nombre");
        Button guardarNombre = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Guardar");
        guardarNombre.setOnAction(e -> {
            com.agendapersonal.util.WO_KI_NB_JI_Perfil.WO_KI_NB_JI_guardarNombre(nombreCampo.getText());
            nombreCampo.setText(com.agendapersonal.util.WO_KI_NB_JI_Perfil.WO_KI_NB_JI_nombre());
            WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success("Nombre guardado", null);
        });
        HBox nombreFila = new HBox(10, nombreCampo, guardarNombre);
        HBox.setHgrow(nombreCampo, Priority.ALWAYS);
        javafx.scene.control.ComboBox<String> rolUsuario = new javafx.scene.control.ComboBox<>();
        rolUsuario.getItems().setAll("Estudiante", "Docente");
        rolUsuario.setValue(WO_KI_NB_JI_Perfil.WO_KI_NB_JI_esDocente() ? "Docente" : "Estudiante");
        Button guardarRol = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Cambiar espacio");
        guardarRol.setOnAction(e -> {
            String rol = "Docente".equals(rolUsuario.getValue()) ? WO_KI_NB_JI_Perfil.DOCENTE : WO_KI_NB_JI_Perfil.ESTUDIANTE;
            if (!rol.equals(WO_KI_NB_JI_Perfil.WO_KI_NB_JI_rol())) WO_KI_NB_JI_seleccionarRol(rol);
        });
        HBox rolFila = new HBox(10, rolUsuario, guardarRol);
        rolFila.setAlignment(Pos.CENTER_LEFT);
        VBox perfil = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        perfil.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h2("Tu perfil"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Compás es solo para ti y se guarda en este equipo. El perfil cambia las herramientas del espacio de trabajo."),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Nombre", nombreFila), WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Usar Compás como", rolFila));

        Button borrarDatos = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("trash", "Borrar todos mis datos");
        borrarDatos.setOnAction(e -> WO_KI_NB_JI_confirmarBorradoTotal());
        VBox datos = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        datos.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h2("Restablecer Compás"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Borra tareas, eventos, bloques, notas, proyectos, contactos, hábitos, finanzas, salud, perfiles y preferencias. No se puede deshacer; después podrás configurar Compás de nuevo."),
            borrarDatos);

        // Reloj del inicio
        WO_KI_NB_JI_Switch relojVisible = new WO_KI_NB_JI_Switch("Mostrar el reloj grande", "Aparece en la pantalla de inicio.", WO_KI_NB_JI_RelojPrefs.WO_KI_NB_JI_visible());
        relojVisible.WO_KI_NB_JI_selectedProperty().addListener((o, a, v) -> WO_KI_NB_JI_RelojPrefs.WO_KI_NB_JI_setVisible(v));
        WO_KI_NB_JI_Switch reloj24 = new WO_KI_NB_JI_Switch("Formato de 24 horas", "Si lo apagas se muestra con AM/PM.", WO_KI_NB_JI_RelojPrefs.WO_KI_NB_JI_formato24());
        reloj24.WO_KI_NB_JI_selectedProperty().addListener((o, a, v) -> WO_KI_NB_JI_RelojPrefs.WO_KI_NB_JI_setFormato24(v));
        WO_KI_NB_JI_Switch relojSeg = new WO_KI_NB_JI_Switch("Mostrar segundos", null, WO_KI_NB_JI_RelojPrefs.WO_KI_NB_JI_segundos());
        relojSeg.WO_KI_NB_JI_selectedProperty().addListener((o, a, v) -> WO_KI_NB_JI_RelojPrefs.WO_KI_NB_JI_setSegundos(v));
        WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented relojTam = new WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented("Pequeño", "Mediano", "Grande");
        relojTam.WO_KI_NB_JI_select(WO_KI_NB_JI_RelojPrefs.WO_KI_NB_JI_tamano().ordinal());
        relojTam.WO_KI_NB_JI_selectedProperty().addListener((o, a, v) -> WO_KI_NB_JI_RelojPrefs.WO_KI_NB_JI_setTamano(WO_KI_NB_JI_RelojPrefs.WO_KI_NB_JI_Tamano.values()[v.intValue()]));
        VBox relojCard = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        relojCard.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h2("Reloj"), relojVisible, WO_KI_NB_JI_sep(), reloj24, WO_KI_NB_JI_sep(), relojSeg, WO_KI_NB_JI_sep(),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Tamaño", relojTam.WO_KI_NB_JI_node()));

        // Apariencia
        WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented modo = new WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented("Claro", "Oscuro");
        modo.WO_KI_NB_JI_button(0).setGraphic(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("sun", 16));
        modo.WO_KI_NB_JI_button(1).setGraphic(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("moon", 16));
        modo.WO_KI_NB_JI_select(theme.WO_KI_NB_JI_isDark() ? 1 : 0);
        modo.WO_KI_NB_JI_selectedProperty().addListener((o, a, v) -> {
            theme.WO_KI_NB_JI_setMode(v.intValue() == 1 ? WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_Mode.OSCURO : WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_Mode.CLARO);
        });
        theme.WO_KI_NB_JI_revisionProperty().addListener((o, a, b) -> modo.WO_KI_NB_JI_select(theme.WO_KI_NB_JI_isDark() ? 1 : 0));

        FlowPane swatches = new FlowPane(10, 10);
        List<StackPane> sw = new ArrayList<>();
        for (WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_Accent ac : WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_Accent.values()) {
            Circle dot = new Circle(13);
            dot.setStyle("-fx-fill:" + ac.WO_KI_NB_JI_color(theme.WO_KI_NB_JI_isDark()) + ";");
            Node check = WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("check", 16, "tint-on-accent");
            check.setVisible(ac == theme.WO_KI_NB_JI_accent());
            StackPane s = new StackPane(dot, check);
            s.getStyleClass().add("swatch");
            s.setAccessibleText(ac.toString());
            Label name = new Label(ac.toString());
            name.getStyleClass().add("caption");
            VBox col = new VBox(6, s, name);
            col.setAlignment(Pos.CENTER);
            col.setOnMouseClicked(e -> {
                theme.WO_KI_NB_JI_setAccent(ac);
                WO_KI_NB_JI_Anim.WO_KI_NB_JI_pop(s);
            });
            col.setCursor(javafx.scene.Cursor.HAND);
            s.getProperties().put("accent", ac);
            sw.add(s);
            swatches.getChildren().add(col);
        }
        theme.WO_KI_NB_JI_revisionProperty().addListener((o, a, b) -> sw.forEach(s -> {
            WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_Accent ac = (WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_Accent) s.getProperties().get("accent");
            s.getChildren().get(0).setStyle("-fx-fill:" + ac.WO_KI_NB_JI_color(theme.WO_KI_NB_JI_isDark()) + ";");
            s.getChildren().get(1).setVisible(ac == theme.WO_KI_NB_JI_accent());
        }));

        VBox apariencia = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        apariencia.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h2("Apariencia"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Los cambios se aplican al instante en todas las ventanas y se guardan solos."),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Tema", modo.WO_KI_NB_JI_node()), WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Color de acento", swatches));

        // Tipografía
        javafx.scene.control.ComboBox<String> fuente = new javafx.scene.control.ComboBox<>();
        fuente.getItems().setAll(WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_options());
        fuente.setValue(WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_family());
        fuente.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(String f, boolean empty) {
                super.updateItem(f, empty);
                setText(empty ? null : f + (f != null && WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_esIncluida(f) ? "  · incluida" : ""));
                setStyle(empty || f == null ? "" : "-fx-font-family:\"" + f + "\";");
            }
        });
        fuente.setOnAction(e -> {
            if (fuente.getValue() == null || fuente.getValue().equals(WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_family())) return;
            WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_setFamily(fuente.getValue());
        });
        WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented tam = new WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented("Pequeño", "Normal", "Grande", "Muy grande");
        tam.WO_KI_NB_JI_select(WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_size().ordinal());
        tam.WO_KI_NB_JI_selectedProperty().addListener((o, a, v) -> {
            WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_setSize(WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_Size.values()[v.intValue()]);
        });
        Label muestra = new Label("Así se verán tus textos: Reunión con el equipo a las 10:30 · S/ 1.250,00");
        muestra.getStyleClass().add("font-preview");
        muestra.setWrapText(true);
        VBox tipografia = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        tipografia.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h2("Tipografía"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Las fuentes marcadas como «incluida» vienen con Compás y se ven igual en cualquier PC."),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Fuente", fuente), WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Tamaño del texto", tam.WO_KI_NB_JI_node()), muestra);

        // Avisos
        WO_KI_NB_JI_Switch cambios = new WO_KI_NB_JI_Switch("Confirmaciones al guardar", "Muestra un mensaje breve al guardar, eliminar o completar algo. Los recordatorios de eventos no dependen de esto.",
            WO_KI_NB_JI_Notifier.WO_KI_NB_JI_isEnabled());
        cambios.WO_KI_NB_JI_selectedProperty().addListener((o, a, v) -> {
            WO_KI_NB_JI_Notifier.WO_KI_NB_JI_setEnabled(v);
            cfg.WO_KI_NB_JI_set("ui.confirmaciones", v ? "1" : "0");
        });
        WO_KI_NB_JI_Switch eventos = new WO_KI_NB_JI_Switch("Avisar cuando empieza un evento",
            "Aparece una notificación en el escritorio justo a la hora de inicio, aunque Compás esté minimizado.",
            !"0".equals(cfg.WO_KI_NB_JI_get(WO_KI_NB_JI_CentroAvisos.CFG_EVENTOS, "1")));
        eventos.WO_KI_NB_JI_selectedProperty().addListener((o, a, v) -> cfg.WO_KI_NB_JI_set(WO_KI_NB_JI_CentroAvisos.CFG_EVENTOS, v ? "1" : "0"));
        WO_KI_NB_JI_Switch cumples = new WO_KI_NB_JI_Switch("Avisar cumpleaños del día", "Una sola notificación el mismo día del cumpleaños.",
            !"0".equals(cfg.WO_KI_NB_JI_get(WO_KI_NB_JI_CentroAvisos.CFG_CUMPLES, "1")));
        cumples.WO_KI_NB_JI_selectedProperty().addListener((o, a, v) -> cfg.WO_KI_NB_JI_set(WO_KI_NB_JI_CentroAvisos.CFG_CUMPLES, v ? "1" : "0"));
        WO_KI_NB_JI_Switch resumen = new WO_KI_NB_JI_Switch("Resumen al abrir Compás",
            "Ventana con los pendientes del día al iniciar. También está en el ícono junto al reloj.",
            "1".equals(cfg.WO_KI_NB_JI_get(WO_KI_NB_JI_CentroAvisos.CFG_RESUMEN, "0")));
        resumen.WO_KI_NB_JI_selectedProperty().addListener((o, a, v) -> cfg.WO_KI_NB_JI_set(WO_KI_NB_JI_CentroAvisos.CFG_RESUMEN, v ? "1" : "0"));
        VBox avisos = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        avisos.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h2("Avisos"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Los avisos de escritorio solo salen para lo que está pasando en el momento."),
            cambios, WO_KI_NB_JI_sep(), eventos, WO_KI_NB_JI_sep(), cumples, WO_KI_NB_JI_sep(), resumen);

        // Acerca de
        HBox about = new HBox(16, WO_KI_NB_JI_Logo.WO_KI_NB_JI_mark(56), new VBox(4, WO_KI_NB_JI_Ui.WO_KI_NB_JI_h2("Compás 1.1"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Tu tiempo, mejor invertido. Atajos: Ctrl+B contrae el menú · Ctrl+Shift+L cambia el tema.")));
        about.setAlignment(Pos.CENTER_LEFT);
        VBox acerca = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        acerca.getChildren().add(about);

        HBox fila = new HBox(20, apariencia, tipografia);
        HBox.setHgrow(apariencia, Priority.ALWAYS);
        HBox.setHgrow(tipografia, Priority.ALWAYS);
        apariencia.setMaxWidth(Double.MAX_VALUE);
        tipografia.setMaxWidth(Double.MAX_VALUE);
        apariencia.setPrefWidth(400);
        tipografia.setPrefWidth(400);
        HBox fila2 = new HBox(20, perfil, relojCard);
        HBox.setHgrow(perfil, Priority.ALWAYS);
        HBox.setHgrow(relojCard, Priority.ALWAYS);
        perfil.setPrefWidth(400);
        relojCard.setPrefWidth(400);
        page.getChildren().addAll(fila2, fila,
            datos,
            com.agendapersonal.ui.settings.WO_KI_NB_JI_PersonalizacionCards.WO_KI_NB_JI_temas(),
            com.agendapersonal.ui.settings.WO_KI_NB_JI_PersonalizacionCards.WO_KI_NB_JI_crearTema(),
            com.agendapersonal.ui.settings.WO_KI_NB_JI_PersonalizacionCards.WO_KI_NB_JI_forma(),
            com.agendapersonal.ui.settings.WO_KI_NB_JI_PersonalizacionCards.WO_KI_NB_JI_perfiles(),
            com.agendapersonal.ui.settings.WO_KI_NB_JI_PersonalizacionCards.WO_KI_NB_JI_respaldo(),
            avisos, com.agendapersonal.ui.settings.WO_KI_NB_JI_PersonalizacionCards.WO_KI_NB_JI_silencio(), acerca);
        return WO_KI_NB_JI_Ui.WO_KI_NB_JI_scroll(page);
    }

    private void WO_KI_NB_JI_confirmarBorradoTotal() {
        javafx.scene.control.Alert confirmacion = new javafx.scene.control.Alert(
            javafx.scene.control.Alert.AlertType.CONFIRMATION);
        confirmacion.initOwner(stage);
        confirmacion.setTitle("Borrar todos los datos");
        confirmacion.setHeaderText("¿Restablecer Compás por completo?");
        confirmacion.setContentText("Se eliminarán permanentemente tus registros y preferencias. A continuación podrás volver a elegir tu nombre y perfil.");
        javafx.scene.control.ButtonType borrar = new javafx.scene.control.ButtonType(
            "Borrar todo", javafx.scene.control.ButtonBar.ButtonData.OTHER);
        confirmacion.getButtonTypes().setAll(borrar, javafx.scene.control.ButtonType.CANCEL);
        confirmacion.setOnShown(e -> theme.WO_KI_NB_JI_applyTo(confirmacion.getDialogPane().getScene()));
        if (confirmacion.showAndWait().orElse(javafx.scene.control.ButtonType.CANCEL) != borrar) return;

        try {
            com.agendapersonal.infrastructure.db.WO_KI_NB_JI_Database.WO_KI_NB_JI_borrarTodosLosDatos();
            theme.WO_KI_NB_JI_settings().WO_KI_NB_JI_resetAll();
            c.WO_KI_NB_JI_cargarTodo();
            com.agendapersonal.ui.WO_KI_NB_JI_BienvenidaView.WO_KI_NB_JI_mostrarSiHaceFalta();
            WO_KI_NB_JI_seleccionarRol(WO_KI_NB_JI_Perfil.WO_KI_NB_JI_rol());

            javafx.scene.control.Alert listo = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION,
                "Compás quedó restablecido. Tus datos anteriores ya se eliminaron.");
            listo.initOwner(stage);
            listo.setTitle("Datos borrados");
            listo.setHeaderText("Restablecimiento completado");
            listo.setOnShown(e -> theme.WO_KI_NB_JI_applyTo(listo.getDialogPane().getScene()));
            listo.showAndWait();
        } catch (RuntimeException ex) {
            javafx.scene.control.Alert error = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.ERROR,
                "No se pudieron borrar los datos. " + ex.getMessage());
            error.initOwner(stage);
            error.setTitle("No se pudo restablecer Compás");
            error.showAndWait();
        }
    }

    private static Region WO_KI_NB_JI_sep() {
        Region r = new Region();
        r.getStyleClass().add("hairline");
        return r;
    }
}

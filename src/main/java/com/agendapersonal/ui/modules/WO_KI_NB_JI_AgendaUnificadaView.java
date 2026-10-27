package com.agendapersonal.ui.modules;

import com.agendapersonal.controller.WO_KI_NB_JI_MainController;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Contacto;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Habito;
import com.agendapersonal.infrastructure.db.WO_KI_NB_JI_ConfigDAO;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_HabitoDAO;
import com.agendapersonal.model.WO_KI_NB_JI_Evento;
import com.agendapersonal.model.WO_KI_NB_JI_Horas;
import com.agendapersonal.model.WO_KI_NB_JI_Tarea;
import com.agendapersonal.service.WO_KI_NB_JI_AgendaDatos;
import com.agendapersonal.service.WO_KI_NB_JI_CalendarioUnificadoService;
import com.agendapersonal.service.WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_Datos;
import com.agendapersonal.service.WO_KI_NB_JI_Entrada;
import com.agendapersonal.service.WO_KI_NB_JI_TipoEntrada;
import com.agendapersonal.ui.WO_KI_NB_JI_EventViews;
import com.agendapersonal.ui.WO_KI_NB_JI_IconFactory;
import com.agendapersonal.ui.WO_KI_NB_JI_TaskViews;
import com.agendapersonal.ui.WO_KI_NB_JI_Toast;
import com.agendapersonal.ui.notify.WO_KI_NB_JI_CentroAvisos;
import com.agendapersonal.ui.notify.WO_KI_NB_JI_NotificacionEscritorio;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.Tooltip;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Agenda Unificada: un panel estilo Google Calendar que junta en un solo lugar
 * eventos, trabajos, tareas, hábitos y cumpleaños, y permite mostrarlos u ocultarlos por segmento.
 *
 * Interacción:
 *  - Clic en un hueco de la cuadrícula → nuevo evento a esa hora.
 *  - Evento/trabajo: clic para editar.  Tarea: clic marca/desmarca, clic derecho edita.
 *  - Hábito: clic marca/desmarca ese día.  Cumpleaños: clic muestra los datos del contacto.
 */
public class WO_KI_NB_JI_AgendaUnificadaView {
    private enum WO_KI_NB_JI_Vista {
        DIA("Día"), SEMANA("Semana"), MES("Mes"), AGENDA("Agenda");
        final String etiqueta;
        WO_KI_NB_JI_Vista(String e) { etiqueta = e; }
    }

    private static final double PX_HORA = 60, GUTTER = 58;
    private static final DayOfWeek PRIMER_DIA = DayOfWeek.SUNDAY;   // como en tu Google Calendar
    private static final Locale ES = Locale.of("es", "PE");
    private static final String MUTED = "-muted", TEXTO = "-text", LINEA = "-bord", PANEL = "-surface";

    private final WO_KI_NB_JI_MainController c;
    private final Stage stage;
    private final WO_KI_NB_JI_ConfigDAO cfg = new WO_KI_NB_JI_ConfigDAO();
    private final WO_KI_NB_JI_HabitoDAO habitoDAO = new WO_KI_NB_JI_HabitoDAO();

    private final BorderPane view = new BorderPane();
    private final StackPane centro = new StackPane();
    private final Label rango = new Label();
    private final VBox mini = new VBox(6);
    private final ToggleGroup grupoVista = new ToggleGroup();
    private final Map<WO_KI_NB_JI_Vista, ToggleButton> botonesVista = new EnumMap<>(WO_KI_NB_JI_Vista.class);
    private final Map<WO_KI_NB_JI_TipoEntrada, Label> contadores = new EnumMap<>(WO_KI_NB_JI_TipoEntrada.class);
    private final Set<WO_KI_NB_JI_TipoEntrada> visibles = EnumSet.allOf(WO_KI_NB_JI_TipoEntrada.class);
    private final List<WO_KI_NB_JI_ColumnaDia> columnas = new ArrayList<>();

    private LocalDate cursor = LocalDate.now();
    private YearMonth miniMes = YearMonth.now();
    private WO_KI_NB_JI_Vista vista = WO_KI_NB_JI_Vista.SEMANA;
    private WO_KI_NB_JI_Datos datos = WO_KI_NB_JI_Datos.WO_KI_NB_JI_vacio();
    private String firmaDatos = "";
    private double vvalue = -1;      // posición vertical recordada de la cuadrícula horaria

    public WO_KI_NB_JI_AgendaUnificadaView(WO_KI_NB_JI_MainController c, Stage stage) {
        this.c = c;
        this.stage = stage;
        view.setStyle("-fx-background-color:-bg;");
        view.setTop(WO_KI_NB_JI_barraSuperior());
        view.setLeft(WO_KI_NB_JI_panelIzquierdo());
        view.setCenter(centro);
        BorderPane.setMargin(centro, new Insets(0, 14, 14, 0));

        // se actualiza sola cuando cambian eventos/tareas (incluye el refresco cada 15 s de la BD)
        c.WO_KI_NB_JI_getEventos().addListener((Observable o) -> Platform.runLater(() -> WO_KI_NB_JI_refrescar(false)));
        c.WO_KI_NB_JI_getTareas().addListener((Observable o) -> Platform.runLater(() -> WO_KI_NB_JI_refrescar(false)));
        Timeline cadaMinuto = new Timeline(new KeyFrame(Duration.seconds(60), e -> WO_KI_NB_JI_actualizarAhora()));
        cadaMinuto.setCycleCount(Animation.INDEFINITE);
        cadaMinuto.play();

        botonesVista.get(vista).setSelected(true);
        WO_KI_NB_JI_refrescar(true);
    }

    public BorderPane WO_KI_NB_JI_getView() { return view; }

    // =====================================================================================
    //  Barra superior
    // =====================================================================================

    private HBox WO_KI_NB_JI_barraSuperior() {
        Button hoy = new Button("Hoy");
        hoy.setStyle(WO_KI_NB_JI_estiloContorno());
        hoy.setOnAction(e -> WO_KI_NB_JI_irA(LocalDate.now()));
        Button prev = WO_KI_NB_JI_flecha("arrow-left");
        Button next = WO_KI_NB_JI_flecha("arrow-right");
        prev.setOnAction(e -> WO_KI_NB_JI_mover(-1));
        next.setOnAction(e -> WO_KI_NB_JI_mover(1));
        rango.setStyle("-fx-text-fill:" + TEXTO + ";-fx-font-size:22;");

        HBox seg = new HBox(0);
        seg.setStyle("-fx-background-color:-surface2;-fx-background-radius:10;-fx-padding:2;");
        for (WO_KI_NB_JI_Vista v : WO_KI_NB_JI_Vista.values()) {
            ToggleButton b = new ToggleButton(v.etiqueta);
            b.setToggleGroup(grupoVista);
            WO_KI_NB_JI_estiloSeg(b, false);
            b.selectedProperty().addListener((o, a, sel) -> WO_KI_NB_JI_estiloSeg(b, sel));
            b.setOnAction(e -> {
                b.setSelected(true);                       // no permitir "ninguna" vista
                if (vista != v) { vista = v; WO_KI_NB_JI_refrescar(true); }
            });
            botonesVista.put(v, b);
            seg.getChildren().add(b);
        }
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox bar = new HBox(10, hoy, prev, next, rango, sp, seg);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(12, 16, 10, 16));
        return bar;
    }

    private static String WO_KI_NB_JI_estiloContorno() {
        return "-fx-background-color:transparent;-fx-border-color:" + LINEA + ";-fx-border-radius:18;"
            + "-fx-background-radius:18;-fx-text-fill:" + TEXTO + ";-fx-padding:6 16;-fx-cursor:hand;";
    }

    private static Button WO_KI_NB_JI_flecha(String icon) {
        Button b = new Button();
        b.setGraphic(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_image(icon, 16));
        b.setAccessibleText(icon.equals("arrow-left") ? "Anterior" : "Siguiente");
        b.setStyle("-fx-background-color:transparent;-fx-text-fill:" + TEXTO + ";-fx-cursor:hand;-fx-padding:6 8;");
        return b;
    }

    private static MenuItem WO_KI_NB_JI_menuItem(String text, String icon) {
        MenuItem item = new MenuItem(text);
        item.setGraphic(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_image(icon, 16));
        return item;
    }

    private static void WO_KI_NB_JI_estiloSeg(ToggleButton b, boolean sel) {
        b.setStyle("-fx-background-radius:8;-fx-padding:7 16;-fx-cursor:hand;" + (sel
            ? "-fx-background-color:-accent;-fx-text-fill:-on-accent;-fx-font-weight:bold;"
            : "-fx-background-color:transparent;-fx-text-fill:" + MUTED + ";"));
    }

    // =====================================================================================
    //  Panel izquierdo: Crear + mini calendario + segmentos + notificaciones
    // =====================================================================================

    private Node WO_KI_NB_JI_panelIzquierdo() {
        VBox p = new VBox(16);
        p.setPadding(new Insets(6, 14, 14, 16));
        p.setPrefWidth(280);

        MenuButton crear = new MenuButton("Crear");
        crear.setGraphic(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_image("add", 17));
        crear.setStyle("-fx-background-color:-surface2;-fx-text-fill:" + TEXTO + ";-fx-background-radius:24;"
            + "-fx-border-color:" + LINEA + ";-fx-border-radius:24;-fx-padding:10 22;-fx-font-weight:bold;-fx-cursor:hand;"
            + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.4),8,0,0,2);");
        MenuItem miEvento = WO_KI_NB_JI_menuItem("Evento", "event");
        MenuItem miTrabajo = WO_KI_NB_JI_menuItem("Trabajo", "work");
        MenuItem miTarea = WO_KI_NB_JI_menuItem("Tarea", "tasks");
        MenuItem miHabito = WO_KI_NB_JI_menuItem("Hábito", "habits");
        MenuItem miCumple = WO_KI_NB_JI_menuItem("Cumpleaños", "birthday");
        miEvento.setOnAction(e -> WO_KI_NB_JI_EventViews.WO_KI_NB_JI_formEnFechaHora(stage, null, cursor, null, null, c, () -> WO_KI_NB_JI_refrescar(true)));
        miTrabajo.setOnAction(e -> WO_KI_NB_JI_EventViews.WO_KI_NB_JI_formEnFechaHora(stage, null, cursor, null, "Trabajo", c, () -> WO_KI_NB_JI_refrescar(true)));
        miTarea.setOnAction(e -> WO_KI_NB_JI_TaskViews.WO_KI_NB_JI_form(null, c, () -> WO_KI_NB_JI_refrescar(true)));
        miHabito.setOnAction(e -> WO_KI_NB_JI_DialogosAgenda.WO_KI_NB_JI_nuevoHabito(stage, () -> WO_KI_NB_JI_refrescar(true)));
        miCumple.setOnAction(e -> WO_KI_NB_JI_DialogosAgenda.WO_KI_NB_JI_nuevoCumple(stage, () -> WO_KI_NB_JI_refrescar(true)));
        crear.getItems().addAll(miEvento, miTrabajo, miTarea, miHabito, miCumple);

        p.getChildren().addAll(crear, mini, WO_KI_NB_JI_titulo("Mis calendarios"));
        for (WO_KI_NB_JI_TipoEntrada t : WO_KI_NB_JI_TipoEntrada.values()) p.getChildren().add(WO_KI_NB_JI_filtro(t));
        p.getChildren().addAll(WO_KI_NB_JI_titulo("Notificaciones"), WO_KI_NB_JI_panelNotificaciones());

        ScrollPane sp = new ScrollPane(p);
        sp.setFitToWidth(true);
        sp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        sp.setPrefWidth(300);
        sp.setMinWidth(300);
        sp.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        return sp;
    }

    private static Label WO_KI_NB_JI_titulo(String t) {
        Label l = new Label(t);
        l.setStyle("-fx-text-fill:" + TEXTO + ";-fx-font-weight:bold;-fx-font-size:14;");
        VBox.setMargin(l, new Insets(4, 0, -8, 4));
        return l;
    }

    /** Fila "casilla de color + nombre + contador" que muestra/oculta un segmento. */
    private HBox WO_KI_NB_JI_filtro(WO_KI_NB_JI_TipoEntrada t) {
        Region caja = new Region();
        caja.setMinSize(16, 16);
        caja.setMaxSize(16, 16);
        Label nombre = new Label(t.WO_KI_NB_JI_etiqueta());
        nombre.setStyle("-fx-text-fill:" + TEXTO + ";-fx-font-size:13.5;");
        Label cuenta = new Label();
        cuenta.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12;");
        contadores.put(t, cuenta);
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        HBox fila = new HBox(10, caja, nombre, sp, cuenta);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setPadding(new Insets(6, 10, 6, 10));
        fila.setCursor(Cursor.HAND);
        Runnable pintar = () -> caja.setStyle(visibles.contains(t)
            ? "-fx-background-color:" + t.WO_KI_NB_JI_color() + ";-fx-background-radius:4;"
            : "-fx-background-color:transparent;-fx-border-color:" + t.WO_KI_NB_JI_color() + ";-fx-border-width:2;-fx-border-radius:4;");
        pintar.run();
        fila.setOnMouseEntered(e -> fila.setStyle("-fx-background-color:-accent-soft;-fx-background-radius:8;"));
        fila.setOnMouseExited(e -> fila.setStyle(""));
        fila.setOnMouseClicked(e -> {
            if (!visibles.remove(t)) visibles.add(t);
            pintar.run();
            WO_KI_NB_JI_refrescar(true);
        });
        Tooltip.install(fila, new Tooltip("Mostrar / ocultar " + t.WO_KI_NB_JI_etiqueta().toLowerCase(ES)));
        return fila;
    }

    private VBox WO_KI_NB_JI_panelNotificaciones() {
        // Los avisos de escritorio salen solo en el momento en que empieza un evento (sin antelación).
        CheckBox eventos = new CheckBox("Avisar cuando empieza un evento");
        eventos.setSelected(!"0".equals(cfg.WO_KI_NB_JI_get(WO_KI_NB_JI_CentroAvisos.CFG_EVENTOS, "1")));
        eventos.setOnAction(e -> cfg.WO_KI_NB_JI_set(WO_KI_NB_JI_CentroAvisos.CFG_EVENTOS, eventos.isSelected() ? "1" : "0"));
        CheckBox resumen = new CheckBox("Resumen al iniciar Compás");
        resumen.setSelected("1".equals(cfg.WO_KI_NB_JI_get(WO_KI_NB_JI_CentroAvisos.CFG_RESUMEN, "0")));
        resumen.setOnAction(e -> cfg.WO_KI_NB_JI_set(WO_KI_NB_JI_CentroAvisos.CFG_RESUMEN, resumen.isSelected() ? "1" : "0"));

        Button probar = new Button("Probar notificación");
        probar.setGraphic(WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_image("notification", 16));
        probar.setOnAction(e -> WO_KI_NB_JI_NotificacionEscritorio.WO_KI_NB_JI_mostrar(WO_KI_NB_JI_TipoEntrada.EVENTO, "Notificación de prueba",
            "Así verás tus recordatorios en la pantalla, incluso con Compás minimizado.", 8));

        Label nota = new Label("Al cerrar la ventana, Compás sigue avisando desde la bandeja del sistema. Más opciones en Configuración.");
        nota.setWrapText(true);
        nota.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12;");
        VBox v = new VBox(10, eventos, resumen, probar, nota);
        v.setPadding(new Insets(0, 0, 0, 4));
        return v;
    }

    // ---------- mini calendario ----------

    private void WO_KI_NB_JI_reconstruirMini() {
        mini.getChildren().clear();
        Label t = new Label(WO_KI_NB_JI_cap(miniMes.getMonth().getDisplayName(TextStyle.FULL, ES)) + " de " + miniMes.getYear());
        t.setStyle("-fx-text-fill:" + TEXTO + ";-fx-font-weight:bold;-fx-font-size:14;");
        Region sp = new Region();
        HBox.setHgrow(sp, Priority.ALWAYS);
        Button ant = WO_KI_NB_JI_flecha("arrow-left"), sig = WO_KI_NB_JI_flecha("arrow-right");
        ant.setOnAction(e -> { miniMes = miniMes.minusMonths(1); WO_KI_NB_JI_reconstruirMini(); });
        sig.setOnAction(e -> { miniMes = miniMes.plusMonths(1); WO_KI_NB_JI_reconstruirMini(); });
        HBox cab = new HBox(2, t, sp, ant, sig);
        cab.setAlignment(Pos.CENTER_LEFT);

        GridPane g = new GridPane();
        g.setHgap(2);
        g.setVgap(2);
        String[] ds = {"D", "L", "M", "X", "J", "V", "S"};
        for (int i = 0; i < 7; i++) {
            Label l = new Label(ds[i]);
            l.setMinWidth(30);
            l.setAlignment(Pos.CENTER);
            l.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12;-fx-font-weight:bold;");
            g.add(l, i, 0);
        }
        LocalDate primero = miniMes.atDay(1).with(TemporalAdjusters.previousOrSame(PRIMER_DIA));
        LocalDate hoy = LocalDate.now();
        for (int i = 0; i < 42; i++) {
            LocalDate d = primero.plusDays(i);
            boolean fuera = !YearMonth.from(d).equals(miniMes);
            Button b = new Button(String.valueOf(d.getDayOfMonth()));
            b.setMinSize(30, 30);
            b.setPrefSize(30, 30);
            b.setMaxSize(30, 30);
            String bg, fg, extra = "";
            if (d.equals(hoy)) { bg = "-accent"; fg = "-on-accent"; extra = "-fx-font-weight:bold;"; }
            else if (d.equals(cursor)) { bg = "-accent-soft"; fg = "-text"; extra = "-fx-font-weight:bold;"; }
            else if (WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_hayAlgo(d, datos, visibles)) { bg = "-surface2"; fg = "-text"; }
            else { bg = "transparent"; fg = fuera ? "-muted" : "-text2"; }
            b.setStyle("-fx-background-color:" + bg + ";-fx-text-fill:" + fg + ";-fx-background-radius:15;"
                + "-fx-padding:0;-fx-font-size:12;-fx-cursor:hand;" + extra);
            b.setOnAction(e -> WO_KI_NB_JI_irA(d));
            g.add(b, i % 7, 1 + i / 7);
        }
        mini.getChildren().addAll(cab, g);
    }

    // =====================================================================================
    //  Navegación y refresco
    // =====================================================================================

    private void WO_KI_NB_JI_mover(int dir) {
        cursor = switch (vista) {
            case DIA -> cursor.plusDays(dir);
            case SEMANA -> cursor.plusWeeks(dir);
            case MES, AGENDA -> cursor.plusMonths(dir);
        };
        miniMes = YearMonth.from(cursor);
        WO_KI_NB_JI_refrescar(true);
    }

    private void WO_KI_NB_JI_irA(LocalDate d) {
        cursor = d;
        miniMes = YearMonth.from(d);
        WO_KI_NB_JI_refrescar(true);
    }

    private void WO_KI_NB_JI_irADia(LocalDate d) {
        cursor = d;
        miniMes = YearMonth.from(d);
        vista = WO_KI_NB_JI_Vista.DIA;
        botonesVista.get(WO_KI_NB_JI_Vista.DIA).setSelected(true);
        WO_KI_NB_JI_refrescar(true);
    }

    /** @param forzar true si cambió la vista/fecha/filtros; false cuando solo puede haber cambiado la BD. */
    private void WO_KI_NB_JI_refrescar(boolean forzar) {
        LocalDate desde = cursor.minusMonths(1).withDayOfMonth(1);
        LocalDate hasta = cursor.plusMonths(2).withDayOfMonth(1).minusDays(1);
        datos = WO_KI_NB_JI_AgendaDatos.WO_KI_NB_JI_cargar(c, desde, hasta);
        String firma = WO_KI_NB_JI_firmaDe(datos);
        if (!forzar && firma.equals(firmaDatos)) return;     // nada cambió: no reconstruir (evita parpadeos)
        firmaDatos = firma;

        columnas.clear();
        Node contenido = switch (vista) {
            case DIA -> WO_KI_NB_JI_grillaHoraria(List.of(cursor));
            case SEMANA -> WO_KI_NB_JI_grillaHoraria(WO_KI_NB_JI_semana(cursor));
            case MES -> WO_KI_NB_JI_mesView();
            case AGENDA -> WO_KI_NB_JI_agendaView();
        };
        centro.getChildren().setAll(contenido);
        rango.setText(WO_KI_NB_JI_textoRango());
        WO_KI_NB_JI_reconstruirMini();
        WO_KI_NB_JI_actualizarContadores();
    }

    private static String WO_KI_NB_JI_firmaDe(WO_KI_NB_JI_Datos d) {
        StringBuilder sb = new StringBuilder();
        for (WO_KI_NB_JI_Evento e : d.eventos())
            sb.append(e.WO_KI_NB_JI_getId()).append('|').append(e.WO_KI_NB_JI_getTitulo()).append('|').append(e.WO_KI_NB_JI_getFecha()).append('|')
              .append(e.WO_KI_NB_JI_getHoraInicio()).append('|').append(e.WO_KI_NB_JI_getHoraFin()).append('|').append(e.WO_KI_NB_JI_getCategoria()).append(';');
        for (WO_KI_NB_JI_Tarea t : d.tareas())
            sb.append(t.WO_KI_NB_JI_getId()).append('|').append(t.WO_KI_NB_JI_getTitulo()).append('|').append(t.WO_KI_NB_JI_getFecha()).append('|')
              .append(t.WO_KI_NB_JI_getEstado()).append('|').append(t.WO_KI_NB_JI_getProyectoId()).append(';');
        for (WO_KI_NB_JI_Habito h : d.habitos())
            sb.append(h.WO_KI_NB_JI_getId()).append('|').append(h.WO_KI_NB_JI_getNombre()).append('|').append(h.WO_KI_NB_JI_getFrecuencia()).append('|')
              .append(h.WO_KI_NB_JI_isActivo()).append(';');
        for (WO_KI_NB_JI_Contacto k : d.contactos())
            sb.append(k.WO_KI_NB_JI_getId()).append('|').append(k.WO_KI_NB_JI_getNombre()).append('|').append(k.WO_KI_NB_JI_getCumpleanos()).append(';');
        sb.append(d.habitosCumplidos().hashCode());
        return sb.toString();
    }

    private static List<LocalDate> WO_KI_NB_JI_semana(LocalDate ref) {
        LocalDate ini = ref.with(TemporalAdjusters.previousOrSame(PRIMER_DIA));
        List<LocalDate> r = new ArrayList<>();
        for (int i = 0; i < 7; i++) r.add(ini.plusDays(i));
        return r;
    }

    private List<LocalDate> WO_KI_NB_JI_rangoVisible() {
        List<LocalDate> r = new ArrayList<>();
        switch (vista) {
            case DIA -> r.add(cursor);
            case SEMANA -> r.addAll(WO_KI_NB_JI_semana(cursor));
            case MES -> { YearMonth ym = YearMonth.from(cursor); for (int i = 1; i <= ym.lengthOfMonth(); i++) r.add(ym.atDay(i)); }
            case AGENDA -> { for (int i = 0; i < 30; i++) r.add(cursor.plusDays(i)); }
        }
        return r;
    }

    private String WO_KI_NB_JI_textoRango() {
        return switch (vista) {
            case DIA -> WO_KI_NB_JI_Ui.WO_KI_NB_JI_formatDate(cursor);
            case MES -> WO_KI_NB_JI_cap(cursor.getMonth().getDisplayName(TextStyle.FULL, ES)) + " de " + cursor.getYear();
            case AGENDA -> "Próximos 30 días";
            case SEMANA -> {
                List<LocalDate> s = WO_KI_NB_JI_semana(cursor);
                LocalDate a = s.getFirst(), b = s.getLast();
                if (a.getMonth() == b.getMonth()) yield WO_KI_NB_JI_cap(a.getMonth().getDisplayName(TextStyle.FULL, ES)) + " de " + a.getYear();
                if (a.getYear() == b.getYear()) yield WO_KI_NB_JI_corto(a) + " – " + WO_KI_NB_JI_corto(b) + " de " + a.getYear();
                yield WO_KI_NB_JI_corto(a) + " de " + a.getYear() + " – " + WO_KI_NB_JI_corto(b) + " de " + b.getYear();
            }
        };
    }

    private static String WO_KI_NB_JI_corto(LocalDate d) {
        String m = d.getMonth().getDisplayName(TextStyle.FULL, ES);
        return m.substring(0, Math.min(3, m.length()));
    }

    private void WO_KI_NB_JI_actualizarContadores() {
        List<LocalDate> dias = WO_KI_NB_JI_rangoVisible();
        for (WO_KI_NB_JI_TipoEntrada t : WO_KI_NB_JI_TipoEntrada.values()) {
            int total = 0, hechos = 0;
            for (LocalDate d : dias)
                for (WO_KI_NB_JI_Entrada e : WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_delDia(d, datos, EnumSet.of(t))) {
                    total++;
                    if (e.hecha()) hechos++;
                }
            Label l = contadores.get(t);
            if (l != null) l.setText(total == 0 ? "" : t == WO_KI_NB_JI_TipoEntrada.HABITO ? hechos + "/" + total : String.valueOf(total));
        }
    }

    // =====================================================================================
    //  Vista DÍA / SEMANA: cuadrícula horaria
    // =====================================================================================

    private Node WO_KI_NB_JI_grillaHoraria(List<LocalDate> dias) {
        int n = dias.size();
        LocalDate hoy = LocalDate.now();

        GridPane cab = WO_KI_NB_JI_gridCols(n);
        cab.setPadding(new Insets(0, 14, 0, 0));          // ≈ ancho de la barra de desplazamiento del cuerpo
        GridPane todoDia = WO_KI_NB_JI_gridCols(n);
        todoDia.setPadding(new Insets(0, 14, 6, 0));

        Label zona = new Label(WO_KI_NB_JI_zonaGmt());
        zona.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12;");
        zona.setMaxWidth(Double.MAX_VALUE);
        zona.setAlignment(Pos.BOTTOM_CENTER);
        GridPane.setValignment(zona, javafx.geometry.VPos.BOTTOM);
        cab.add(zona, 0, 0);

        GridPane cuerpo = WO_KI_NB_JI_gridCols(n);
        Pane gutter = new Pane();
        gutter.setMinHeight(24 * PX_HORA);
        gutter.setPrefHeight(24 * PX_HORA);
        for (int h = 1; h < 24; h++) {
            Label l = new Label(WO_KI_NB_JI_etiquetaHora(h));
            l.setPrefWidth(GUTTER - 8);
            l.setAlignment(Pos.CENTER_RIGHT);
            l.setLayoutY(h * PX_HORA - 8);
            l.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12;");
            gutter.getChildren().add(l);
        }
        cuerpo.add(gutter, 0, 0);

        int limite = n == 1 ? 60 : 3;
        for (int i = 0; i < n; i++) {
            LocalDate d = dias.get(i);
            List<WO_KI_NB_JI_Entrada> es = WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_delDia(d, datos, visibles);

            cab.add(WO_KI_NB_JI_cabeceraDia(d, hoy), i + 1, 0);
            todoDia.add(WO_KI_NB_JI_celdaTodoElDia(d, es.stream().filter(WO_KI_NB_JI_Entrada::WO_KI_NB_JI_todoElDia).toList(), limite, n > 1), i + 1, 0);

            WO_KI_NB_JI_ColumnaDia col = new WO_KI_NB_JI_ColumnaDia(d.equals(hoy));
            for (var cr : WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_asignarCarriles(es.stream().filter(x -> !x.WO_KI_NB_JI_todoElDia()).toList()))
                col.WO_KI_NB_JI_agregar(WO_KI_NB_JI_bloque(cr.entrada()), cr.entrada().WO_KI_NB_JI_minutoInicio(), cr.entrada().WO_KI_NB_JI_minutoFin(), cr.carril(), cr.carriles());
            col.setOnMouseClicked(ev -> {
                int h = (int) Math.max(0, Math.min(23, ev.getY() / PX_HORA));
                WO_KI_NB_JI_EventViews.WO_KI_NB_JI_formEnFechaHora(stage, null, d, String.format("%02d:00", h), null, c, () -> WO_KI_NB_JI_refrescar(true));
            });
            if (col.esHoy) col.WO_KI_NB_JI_setAhora(WO_KI_NB_JI_minutoActual());
            columnas.add(col);
            cuerpo.add(col, i + 1, 0);
        }

        ScrollPane sp = new ScrollPane(cuerpo);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        final double objetivo = vvalue;
        sp.vvalueProperty().addListener((o, a, b) -> vvalue = b.doubleValue());
        WO_KI_NB_JI_alTenerViewport(sp, () -> sp.setVvalue(objetivo >= 0 ? objetivo : WO_KI_NB_JI_vvalueParaHora(sp, 7)));

        Region sep = new Region();
        sep.setMinHeight(1);
        sep.setMaxHeight(1);
        sep.setStyle("-fx-background-color:" + LINEA + ";");
        VBox caja = new VBox(cab, todoDia, sep, sp);
        VBox.setVgrow(sp, Priority.ALWAYS);
        caja.setStyle("-fx-background-color:" + PANEL + ";-fx-background-radius:16;");
        return caja;
    }

    private static GridPane WO_KI_NB_JI_gridCols(int n) {
        GridPane g = new GridPane();
        ColumnConstraints g0 = new ColumnConstraints(GUTTER);
        g0.setMinWidth(GUTTER);
        g0.setMaxWidth(GUTTER);
        g.getColumnConstraints().add(g0);
        for (int i = 0; i < n; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.ALWAYS);
            cc.setMinWidth(0);
            cc.setPrefWidth(100);
            cc.setFillWidth(true);
            g.getColumnConstraints().add(cc);
        }
        return g;
    }

    private Node WO_KI_NB_JI_cabeceraDia(LocalDate d, LocalDate hoy) {
        boolean esHoy = d.equals(hoy);
        Label dow = new Label(d.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES).toUpperCase(ES).replace(".", ""));
        dow.setStyle("-fx-text-fill:" + (esHoy ? "-accent" : MUTED) + ";-fx-font-size:12;-fx-font-weight:bold;");
        Label num = new Label(String.valueOf(d.getDayOfMonth()));
        num.setMinSize(40, 40);
        num.setPrefSize(40, 40);
        num.setAlignment(Pos.CENTER);
        num.setStyle(esHoy
            ? "-fx-background-color:-accent;-fx-background-radius:20;-fx-text-fill:-on-accent;-fx-font-size:20;-fx-font-weight:bold;"
            : "-fx-text-fill:" + TEXTO + ";-fx-font-size:20;");
        VBox v = new VBox(2, dow, num);
        v.setAlignment(Pos.CENTER);
        v.setPadding(new Insets(8, 0, 6, 0));
        v.setCursor(Cursor.HAND);
        v.setOnMouseClicked(e -> WO_KI_NB_JI_irADia(d));
        return v;
    }

    /** Fila "todo el día": tareas, hábitos, cumpleaños y eventos sin hora. */
    private Node WO_KI_NB_JI_celdaTodoElDia(LocalDate d, List<WO_KI_NB_JI_Entrada> lista, int limite, boolean agrupar) {
        VBox v = new VBox(2);
        v.setPadding(new Insets(2, 3, 2, 3));
        v.setMinHeight(26);
        v.setStyle("-fx-border-color:transparent transparent transparent " + LINEA + ";-fx-border-width:0 0 0 1;");
        List<Node> chips = WO_KI_NB_JI_chipsDelDia(d, lista, agrupar);
        int n = 0;
        for (Node ch : chips) {
            if (n++ == limite) {
                Label mas = new Label("+" + (chips.size() - limite) + " más");
                mas.setStyle("-fx-text-fill:-accent;-fx-font-size:12;-fx-cursor:hand;-fx-padding:1 6;");
                mas.setOnMouseClicked(e -> WO_KI_NB_JI_irADia(d));
                v.getChildren().add(mas);
                break;
            }
            v.getChildren().add(ch);
        }
        return v;
    }

    /** Un bloque de evento con hora, posicionado por {@link ColumnaDia}. */
    private Node WO_KI_NB_JI_bloque(WO_KI_NB_JI_Entrada e) {
        Label t = new Label(e.titulo());
        t.setWrapText(true);
        t.setStyle("-fx-text-fill:" + TEXTO + ";-fx-font-weight:bold;-fx-font-size:12;");
        Label h = new Label(e.inicio().format(WO_KI_NB_JI_Horas.SALIDA) + " – " + (e.fin() == null ? "" : e.fin().format(WO_KI_NB_JI_Horas.SALIDA)));
        h.setStyle("-fx-text-fill:" + e.tipo().WO_KI_NB_JI_color() + ";-fx-font-size:12;");
        VBox b = new VBox(1, t, h);
        b.setPadding(new Insets(3, 6, 3, 6));
        b.setCursor(Cursor.HAND);
        b.setStyle("-fx-background-color:" + e.tipo().WO_KI_NB_JI_fondo() + ";-fx-background-radius:6;"
            + "-fx-border-color:" + e.tipo().WO_KI_NB_JI_color() + ";-fx-border-width:0 0 0 3;-fx-border-radius:6;");
        Rectangle recorte = new Rectangle();
        recorte.widthProperty().bind(b.widthProperty());
        recorte.heightProperty().bind(b.heightProperty());
        b.setClip(recorte);
        Tooltip.install(b, new Tooltip(e.tipo().WO_KI_NB_JI_etiqueta() + ": " + e.titulo()
            + (e.detalle() == null || e.detalle().isBlank() ? "" : "\n" + e.detalle()) + "\nClic para editar"));
        b.setOnMouseClicked(ev -> { ev.consume(); WO_KI_NB_JI_accion(e, ev); });
        return b;
    }

    private static String WO_KI_NB_JI_etiquetaHora(int h) {
        int h12 = h % 12 == 0 ? 12 : h % 12;
        return h12 + (h < 12 ? " AM" : " PM");
    }

    private static String WO_KI_NB_JI_zonaGmt() {
        int horas = ZonedDateTime.now().getOffset().getTotalSeconds() / 3600;
        return String.format("GMT%+03d", horas);
    }

    private static int WO_KI_NB_JI_minutoActual() {
        LocalTime t = LocalTime.now();
        return t.getHour() * 60 + t.getMinute();
    }

    private void WO_KI_NB_JI_actualizarAhora() {
        int m = WO_KI_NB_JI_minutoActual();
        for (WO_KI_NB_JI_ColumnaDia col : columnas) if (col.esHoy) col.WO_KI_NB_JI_setAhora(m);
    }

    private static double WO_KI_NB_JI_vvalueParaHora(ScrollPane sp, int hora) {
        double max = 24 * PX_HORA - sp.getViewportBounds().getHeight();
        return max <= 0 ? 0 : Math.max(0, Math.min(1, hora * PX_HORA / max));
    }

    /** Ejecuta la acción cuando el ScrollPane ya tiene tamaño (antes no se puede calcular el desplazamiento). */
    private static void WO_KI_NB_JI_alTenerViewport(ScrollPane sp, Runnable r) {
        Bounds b0 = sp.getViewportBounds();
        if (b0 != null && b0.getHeight() > 0) { Platform.runLater(r); return; }
        sp.viewportBoundsProperty().addListener(new ChangeListener<Bounds>() {
            @Override public void changed(ObservableValue<? extends Bounds> o, Bounds a, Bounds b) {
                if (b != null && b.getHeight() > 0) {
                    sp.viewportBoundsProperty().removeListener(this);
                    Platform.runLater(r);
                }
            }
        });
    }

    /** Columna de un día: líneas de hora, bloques posicionados por minuto y línea roja de "ahora". */
    private static final class WO_KI_NB_JI_ColumnaDia extends Pane {
        private record WO_KI_NB_JI_Hueco(Node nodo, int ini, int fin, int carril, int carriles) {}

        final boolean esHoy;
        private final List<WO_KI_NB_JI_Hueco> huecos = new ArrayList<>();
        private final List<Region> lineas = new ArrayList<>();
        private final Region lineaAhora = new Region();
        private final Circle puntoAhora = new Circle(5, Color.web("#E5484D"));
        private int ahora = -1;

        WO_KI_NB_JI_ColumnaDia(boolean esHoy) {
            this.esHoy = esHoy;
            setMinHeight(24 * PX_HORA);
            setPrefHeight(24 * PX_HORA);
            setMaxHeight(24 * PX_HORA);
            setStyle("-fx-border-color:transparent transparent transparent " + LINEA + ";-fx-border-width:0 0 0 1;");
            for (int h = 0; h < 24; h++) {
                Region l = new Region();
                l.setStyle("-fx-background-color:-surface2;");
                l.setMouseTransparent(true);
                lineas.add(l);
                getChildren().add(l);
            }
            lineaAhora.setStyle("-fx-background-color:#E5484D;");
            lineaAhora.setMouseTransparent(true);
            puntoAhora.setMouseTransparent(true);
        }

        void WO_KI_NB_JI_agregar(Node n, int ini, int fin, int carril, int carriles) {
            huecos.add(new WO_KI_NB_JI_Hueco(n, ini, fin, carril, carriles));
            getChildren().add(n);
        }

        void WO_KI_NB_JI_setAhora(int minuto) {
            ahora = minuto;
            if (!getChildren().contains(lineaAhora)) getChildren().addAll(lineaAhora, puntoAhora);
            lineaAhora.toFront();
            puntoAhora.toFront();
            requestLayout();
        }

        @Override protected void layoutChildren() {
            double w = getWidth();
            for (int h = 0; h < lineas.size(); h++) lineas.get(h).resizeRelocate(0, h * PX_HORA, w, 1);
            for (WO_KI_NB_JI_Hueco hu : huecos) {
                double cw = (w - 6) / hu.carriles();
                double x = 2 + hu.carril() * cw;
                double y = hu.ini() * PX_HORA / 60.0;
                double alto = Math.max(20, (hu.fin() - hu.ini()) * PX_HORA / 60.0 - 2);
                hu.nodo().resizeRelocate(x, y, Math.max(10, cw - 2), alto);
            }
            if (ahora >= 0) {
                double y = ahora * PX_HORA / 60.0;
                lineaAhora.resizeRelocate(0, y - 1, w, 2);
                puntoAhora.setCenterX(5);
                puntoAhora.setCenterY(y);
            }
        }
    }

    // =====================================================================================
    //  Vista MES
    // =====================================================================================

    private Node WO_KI_NB_JI_mesView() {
        YearMonth ym = YearMonth.from(cursor);
        LocalDate primero = ym.atDay(1).with(TemporalAdjusters.previousOrSame(PRIMER_DIA));
        int off = (int) ChronoUnit.DAYS.between(primero, ym.atDay(1));
        int semanas = (off + ym.lengthOfMonth() + 6) / 7;
        LocalDate hoy = LocalDate.now();

        GridPane g = new GridPane();
        g.setHgap(1);
        g.setVgap(1);
        g.setStyle("-fx-background-color:" + LINEA + ";-fx-background-radius:16;-fx-padding:1;");
        for (int i = 0; i < 7; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(100.0 / 7);
            cc.setHgrow(Priority.ALWAYS);
            cc.setMinWidth(0);
            g.getColumnConstraints().add(cc);
        }
        g.getRowConstraints().add(new RowConstraints());
        for (int r = 0; r < semanas; r++) {
            RowConstraints rc = new RowConstraints();
            rc.setVgrow(Priority.ALWAYS);
            rc.setPrefHeight(100);
            rc.setMinHeight(60);
            g.getRowConstraints().add(rc);
        }
        for (int i = 0; i < 7; i++) {
            LocalDate d = primero.plusDays(i);
            Label l = new Label(d.getDayOfWeek().getDisplayName(TextStyle.SHORT, ES).toUpperCase(ES).replace(".", ""));
            l.setMaxWidth(Double.MAX_VALUE);
            l.setAlignment(Pos.CENTER);
            l.setPadding(new Insets(8, 0, 8, 0));
            l.setStyle("-fx-background-color:" + PANEL + ";-fx-text-fill:" + MUTED + ";-fx-font-size:12;-fx-font-weight:bold;");
            g.add(l, i, 0);
        }
        for (int i = 0; i < semanas * 7; i++) {
            LocalDate d = primero.plusDays(i);
            boolean delMes = YearMonth.from(d).equals(ym);
            VBox celda = new VBox(2);
            celda.setPadding(new Insets(4));
            celda.setMinSize(0, 0);
            celda.setCursor(Cursor.HAND);
            celda.setStyle("-fx-background-color:" + (delMes ? PANEL : "-bg") + ";");
            Label num = new Label(String.valueOf(d.getDayOfMonth()));
            num.setMinSize(24, 24);
            num.setAlignment(Pos.CENTER);
            num.setStyle(d.equals(hoy)
                ? "-fx-background-color:-accent;-fx-background-radius:12;-fx-text-fill:-on-accent;-fx-font-weight:bold;"
                : "-fx-text-fill:" + (delMes ? TEXTO : "-muted") + ";");
            HBox cabecera = new HBox(num);
            cabecera.setAlignment(Pos.CENTER);
            celda.getChildren().add(cabecera);
            List<Node> chips = WO_KI_NB_JI_chipsDelDia(d, WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_delDia(d, datos, visibles), true);
            int n = 0;
            for (Node ch : chips) {
                if (n++ == 3) {
                    Label mas = new Label("+" + (chips.size() - 3) + " más");
                    mas.setStyle("-fx-text-fill:-accent;-fx-font-size:12;-fx-padding:0 6;");
                    celda.getChildren().add(mas);
                    break;
                }
                celda.getChildren().add(ch);
            }
            celda.setOnMouseClicked(e -> WO_KI_NB_JI_irADia(d));
            g.add(celda, i % 7, 1 + i / 7);
        }
        VBox caja = new VBox(g);
        VBox.setVgrow(g, Priority.ALWAYS);
        return caja;
    }

    // =====================================================================================
    //  Vista AGENDA (lista)
    // =====================================================================================

    private Node WO_KI_NB_JI_agendaView() {
        VBox lista = new VBox(6);
        lista.setPadding(new Insets(16));
        LocalDate hoy = LocalDate.now();
        int dias = 0;
        for (int i = 0; i < 30; i++) {
            LocalDate d = cursor.plusDays(i);
            List<WO_KI_NB_JI_Entrada> es = WO_KI_NB_JI_CalendarioUnificadoService.WO_KI_NB_JI_delDia(d, datos, visibles).stream()
                .filter(e -> e.tipo() != WO_KI_NB_JI_TipoEntrada.HABITO || d.equals(hoy)).toList();   // los hábitos diarios solo se listan hoy
            if (es.isEmpty()) continue;
            dias++;
            String rel = d.equals(hoy) ? "Hoy · " : d.equals(hoy.plusDays(1)) ? "Mañana · " : "";
            Label cab = new Label(rel + WO_KI_NB_JI_Ui.WO_KI_NB_JI_formatDate(d));
            cab.setStyle("-fx-text-fill:" + (d.equals(hoy) ? "-accent" : TEXTO) + ";-fx-font-weight:bold;-fx-font-size:14;");
            VBox.setMargin(cab, new Insets(dias == 1 ? 0 : 12, 0, 2, 0));
            lista.getChildren().add(cab);
            for (WO_KI_NB_JI_Entrada e : es) lista.getChildren().add(WO_KI_NB_JI_filaAgenda(e));
        }
        if (dias == 0) {
            Label vacio = new Label("No hay nada en los próximos 30 días con los calendarios seleccionados.");
            vacio.setStyle("-fx-text-fill:" + MUTED + ";");
            lista.getChildren().add(vacio);
        }
        ScrollPane sp = new ScrollPane(lista);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background:" + PANEL + ";-fx-background-color:" + PANEL + ";-fx-background-radius:16;");
        return sp;
    }

    private Node WO_KI_NB_JI_filaAgenda(WO_KI_NB_JI_Entrada e) {
        Label hora = new Label(e.WO_KI_NB_JI_todoElDia() ? "Todo el día"
            : e.inicio().format(WO_KI_NB_JI_Horas.SALIDA) + " – " + (e.fin() == null ? "" : e.fin().format(WO_KI_NB_JI_Horas.SALIDA)));
        hora.setMinWidth(110);
        hora.setStyle("-fx-text-fill:" + MUTED + ";");
        Region punto = new Region();
        punto.setMinSize(10, 10);
        punto.setMaxSize(10, 10);
        punto.setStyle("-fx-background-color:" + e.tipo().WO_KI_NB_JI_color() + ";-fx-background-radius:5;");
        Label t = new Label(e.titulo());
        t.setStyle("-fx-text-fill:" + TEXTO + ";-fx-font-size:14;");
        Label tipoTexto = new Label(e.tipo().WO_KI_NB_JI_etiqueta()
            + (e.detalle() == null || e.detalle().isBlank() ? "" : " · " + e.detalle()));
        tipoTexto.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12;");
        HBox tipo = new HBox(5, WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_image(e.tipo().WO_KI_NB_JI_icono(), 14), tipoTexto);
        tipo.setAlignment(Pos.CENTER_LEFT);
        tipo.setStyle("-fx-text-fill:" + MUTED + ";-fx-font-size:12;");
        HBox titulo = new HBox(5, WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_image(e.hecha() ? "check" : e.tipo().WO_KI_NB_JI_icono(), 15), t);
        titulo.setAlignment(Pos.CENTER_LEFT);
        VBox textos = new VBox(3, titulo, tipo);
        HBox f = new HBox(12, hora, punto, textos);
        f.setAlignment(Pos.CENTER_LEFT);
        f.setPadding(new Insets(8, 12, 8, 12));
        f.setCursor(Cursor.HAND);
        f.setStyle("-fx-background-color:-surface2;-fx-background-radius:10;");
        if (e.hecha()) f.setOpacity(0.6);
        f.setOnMouseClicked(ev -> WO_KI_NB_JI_accion(e, ev));
        return f;
    }

    // =====================================================================================
    //  Chips y acciones
    // =====================================================================================

    /** Chips de un día; si {@code agrupar}, varios hábitos se resumen en una sola etiqueta. */
    private List<Node> WO_KI_NB_JI_chipsDelDia(LocalDate d, List<WO_KI_NB_JI_Entrada> lista, boolean agrupar) {
        List<Node> r = new ArrayList<>();
        List<WO_KI_NB_JI_Entrada> habitos = lista.stream().filter(e -> e.tipo() == WO_KI_NB_JI_TipoEntrada.HABITO).toList();
        boolean resumir = agrupar && habitos.size() > 1;
        for (WO_KI_NB_JI_Entrada e : lista) {
            if (e.WO_KI_NB_JI_todoElDia() && !(resumir && e.tipo() == WO_KI_NB_JI_TipoEntrada.HABITO)) r.add(WO_KI_NB_JI_chip(e));
            else if (!e.WO_KI_NB_JI_todoElDia() && vista == WO_KI_NB_JI_Vista.MES) r.add(WO_KI_NB_JI_chip(e));      // en el mes también se ven los eventos con hora
        }
        if (resumir) {
            long hechos = habitos.stream().filter(WO_KI_NB_JI_Entrada::hecha).count();
            Label texto = new Label("Hábitos " + hechos + "/" + habitos.size());
            HBox chip = new HBox(4, WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_image("habits", 13), texto);
            chip.setMaxWidth(Double.MAX_VALUE);
            chip.setMinWidth(0);
            chip.setAlignment(Pos.CENTER_LEFT);
            chip.setStyle("-fx-background-color:" + WO_KI_NB_JI_TipoEntrada.HABITO.WO_KI_NB_JI_fondo() + ";-fx-text-fill:" + WO_KI_NB_JI_TipoEntrada.HABITO.WO_KI_NB_JI_color()
                + ";-fx-background-radius:6;-fx-padding:3 6;-fx-font-size:12;-fx-cursor:hand;");
            chip.setOnMouseClicked(ev -> { ev.consume(); WO_KI_NB_JI_irADia(d); });
            Tooltip.install(chip, new Tooltip("Clic para ver y marcar tus hábitos de este día"));
            r.add(chip);
        }
        return r;
    }

    private Node WO_KI_NB_JI_chip(WO_KI_NB_JI_Entrada e) {
        String hora = e.WO_KI_NB_JI_todoElDia() ? "" : e.inicio().format(WO_KI_NB_JI_Horas.SALIDA) + " ";
        Label text = new Label(hora + e.titulo());
        text.setMaxWidth(Double.MAX_VALUE);
        text.setMinWidth(0);
        text.setTextOverrun(OverrunStyle.ELLIPSIS);
        text.setStyle("-fx-text-fill:" + e.tipo().WO_KI_NB_JI_color() + ";-fx-font-size:12;");
        HBox chip = new HBox(4, WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_image(e.hecha() ? "check" : e.tipo().WO_KI_NB_JI_icono(), 13), text);
        chip.setMaxWidth(Double.MAX_VALUE);
        chip.setMinWidth(0);
        HBox.setHgrow(text, Priority.ALWAYS);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setStyle("-fx-background-color:" + e.tipo().WO_KI_NB_JI_fondo()
            + ";-fx-background-radius:6;-fx-padding:3 6;-fx-cursor:hand;");
        if (e.hecha()) chip.setOpacity(0.65);
        Tooltip.install(chip, new Tooltip(e.tipo().WO_KI_NB_JI_etiqueta() + ": " + e.titulo()
            + (e.detalle() == null || e.detalle().isBlank() ? "" : "\n" + e.detalle()) + "\n" + WO_KI_NB_JI_ayuda(e)));
        chip.setOnMouseClicked(ev -> { ev.consume(); WO_KI_NB_JI_accion(e, ev); });
        return chip;
    }

    private static String WO_KI_NB_JI_ayuda(WO_KI_NB_JI_Entrada e) {
        return switch (e.origen()) {
            case WO_KI_NB_JI_Tarea t -> "Clic: marcar/desmarcar · Clic derecho: editar";
            case WO_KI_NB_JI_Habito h -> "Clic: marcar/desmarcar este día";
            case WO_KI_NB_JI_Contacto k -> "Clic: ver datos del contacto";
            default -> "Clic: editar";
        };
    }

    private void WO_KI_NB_JI_accion(WO_KI_NB_JI_Entrada e, MouseEvent ev) {
        boolean secundario = ev.getButton() == MouseButton.SECONDARY;
        Object o = e.origen();
        if (o instanceof WO_KI_NB_JI_Evento evento) {
            WO_KI_NB_JI_EventViews.WO_KI_NB_JI_form(stage, evento, c, () -> WO_KI_NB_JI_refrescar(true));
        } else if (o instanceof WO_KI_NB_JI_Tarea t) {
            if (secundario) WO_KI_NB_JI_TaskViews.WO_KI_NB_JI_form(t, c, () -> WO_KI_NB_JI_refrescar(true));
            else c.WO_KI_NB_JI_alternarCompletada(t).ifPresent(r -> WO_KI_NB_JI_Toast.WO_KI_NB_JI_mostrar(view, c.WO_KI_NB_JI_mensajeProgreso(r)));
        } else if (o instanceof WO_KI_NB_JI_Habito h) {
            WO_KI_NB_JI_marcarHabito(h, e.fecha());
        } else if (o instanceof WO_KI_NB_JI_Contacto k) {
            StringBuilder sb = new StringBuilder(e.detalle().isBlank() ? "" : e.detalle() + "\n");
            if (k.WO_KI_NB_JI_getTelefono() != null && !k.WO_KI_NB_JI_getTelefono().isBlank()) sb.append("Teléfono: ").append(k.WO_KI_NB_JI_getTelefono()).append('\n');
            if (k.WO_KI_NB_JI_getEmail() != null && !k.WO_KI_NB_JI_getEmail().isBlank()) sb.append("Email: ").append(k.WO_KI_NB_JI_getEmail()).append('\n');
            sb.append("\nPuedes editar este contacto en CRM Personal.");
            Alert a = new Alert(Alert.AlertType.INFORMATION, sb.toString());
            a.initOwner(stage);
            a.setTitle("Cumpleaños");
            a.setHeaderText(k.WO_KI_NB_JI_getNombre());
            a.showAndWait();
        }
    }

    private void WO_KI_NB_JI_marcarHabito(WO_KI_NB_JI_Habito h, LocalDate fecha) {
        if (fecha.isAfter(LocalDate.now())) {
            WO_KI_NB_JI_Toast.WO_KI_NB_JI_mostrar(view, "Aún no puedes marcar un hábito de un día futuro");
            return;
        }
        boolean yaHecho = datos.habitosCumplidos().getOrDefault(fecha.toString(), Set.of()).contains(h.WO_KI_NB_JI_getId());
        if (yaHecho) habitoDAO.WO_KI_NB_JI_desmarcar(h.WO_KI_NB_JI_getId(), fecha.toString());
        else habitoDAO.WO_KI_NB_JI_marcar(h.WO_KI_NB_JI_getId(), fecha.toString());
        WO_KI_NB_JI_refrescar(true);
        if (!yaHecho) WO_KI_NB_JI_Toast.WO_KI_NB_JI_mostrar(view, h.WO_KI_NB_JI_getNombre() + " · racha " + habitoDAO.WO_KI_NB_JI_racha(h.WO_KI_NB_JI_getId()) + " días");
    }

    private static String WO_KI_NB_JI_cap(String s) {
        return s == null || s.isEmpty() ? s : s.substring(0, 1).toUpperCase(ES) + s.substring(1);
    }
}


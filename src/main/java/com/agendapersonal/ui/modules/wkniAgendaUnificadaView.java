package com.agendapersonal.ui.modules;

import com.agendapersonal.controller.wkniMainController;
import com.agendapersonal.domain.model.wkniContacto;
import com.agendapersonal.domain.model.wkniHabito;
import com.agendapersonal.infrastructure.db.wkniConfigDAO;
import com.agendapersonal.infrastructure.repo.wkniHabitoDAO;
import com.agendapersonal.model.wkniEvento;
import com.agendapersonal.model.wkniHoras;
import com.agendapersonal.model.wkniTarea;
import com.agendapersonal.service.wkniAgendaDatos;
import com.agendapersonal.service.wkniCalendarioUnificadoService;
import com.agendapersonal.service.wkniCalendarioUnificadoService.wkniDatos;
import com.agendapersonal.service.wkniEntrada;
import com.agendapersonal.service.wkniTipoEntrada;
import com.agendapersonal.ui.wkniEventViews;
import com.agendapersonal.ui.wkniIconFactory;
import com.agendapersonal.ui.wkniTaskViews;
import com.agendapersonal.ui.wkniToast;
import com.agendapersonal.ui.notify.wkniCentroAvisos;
import com.agendapersonal.ui.notify.wkniNotificacionEscritorio;
import com.agendapersonal.util.wkniUi;
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
public class wkniAgendaUnificadaView {
    private enum wkniVista {
        DIA("Día"), SEMANA("Semana"), MES("Mes"), AGENDA("Agenda");
        final String wkniEtiqueta;
        wkniVista(String wkniE) { wkniEtiqueta = wkniE; }
    }

    private static final double wkniPX_HORA = 60, wkniGUTTER = 58;
    private static final DayOfWeek wkniPRIMER_DIA = DayOfWeek.SUNDAY;   // como en tu Google Calendar
    private static final Locale wkniES = Locale.of("es", "PE");
    private static final String wkniMUTED = "-muted", wkniTEXTO = "-text", wkniLINEA = "-bord", wkniPANEL = "-surface";

    private final wkniMainController wkniC;
    private final Stage wkniStage;
    private final wkniConfigDAO wkniCfg = new wkniConfigDAO();
    private final wkniHabitoDAO wkni_habitoDAO = new wkniHabitoDAO();

    private final BorderPane wkniView = new BorderPane();
    private final StackPane wkniCentro = new StackPane();
    private final Label wkniRango = new Label();
    private final VBox wkniMini = new VBox(6);
    private final ToggleGroup wkniGrupoVista = new ToggleGroup();
    private final Map<wkniVista, ToggleButton> wkniBotonesVista = new EnumMap<>(wkniVista.class);
    private final Map<wkniTipoEntrada, Label> wkniContadores = new EnumMap<>(wkniTipoEntrada.class);
    private final Set<wkniTipoEntrada> wkniVisibles = EnumSet.allOf(wkniTipoEntrada.class);
    private final List<wkniColumnaDia> wkniColumnas = new ArrayList<>();

    private LocalDate wkniCursor = LocalDate.now();
    private YearMonth wkniMiniMes = YearMonth.now();
    private wkniVista wkni_vista = wkniVista.SEMANA;
    private wkniDatos wkni_datos = wkniDatos.wkniVacio();
    private String wkniFirmaDatos = "";
    private double wkniVvalue = -1;      // posición vertical recordada de la cuadrícula horaria

    public wkniAgendaUnificadaView(wkniMainController wkniC, Stage wkniStage) {
        this.wkniC = wkniC;
        this.wkniStage = wkniStage;
        wkniView.setStyle("-fx-background-color:-bg;");
        wkniView.setTop(wkniBarraSuperior());
        wkniView.setLeft(wkniPanelIzquierdo());
        wkniView.setCenter(wkniCentro);
        BorderPane.setMargin(wkniCentro, new Insets(0, 14, 14, 0));

        // se actualiza sola cuando cambian eventos/tareas (incluye el refresco cada 15 s de la BD)
        wkniC.wkniGetEventos().addListener((Observable wkniO) -> Platform.runLater(() -> wkniRefrescar(false)));
        wkniC.wkniGetTareas().addListener((Observable wkniO) -> Platform.runLater(() -> wkniRefrescar(false)));
        Timeline wkniCadaMinuto = new Timeline(new KeyFrame(Duration.seconds(60), wkniE -> wkniActualizarAhora()));
        wkniCadaMinuto.setCycleCount(Animation.INDEFINITE);
        wkniCadaMinuto.play();

        wkniBotonesVista.get(wkni_vista).setSelected(true);
        wkniRefrescar(true);
    }

    public BorderPane wkniGetView() { return wkniView; }

    // =====================================================================================
    //  Barra superior
    // =====================================================================================

    private HBox wkniBarraSuperior() {
        Button wkniHoy = new Button("Hoy");
        wkniHoy.setStyle(wkniEstiloContorno());
        wkniHoy.setOnAction(wkniE -> wkniIrA(LocalDate.now()));
        Button wkniPrev = wkniFlecha("arrow-left");
        Button wkniNext = wkniFlecha("arrow-right");
        wkniPrev.setOnAction(wkniE -> wkniMover(-1));
        wkniNext.setOnAction(wkniE -> wkniMover(1));
        wkniRango.setStyle("-fx-text-fill:" + wkniTEXTO + ";-fx-font-size:22;");

        HBox wkniSeg = new HBox(0);
        wkniSeg.setStyle("-fx-background-color:-surface2;-fx-background-radius:10;-fx-padding:2;");
        for (wkniVista wkniV : wkniVista.values()) {
            ToggleButton wkniB = new ToggleButton(wkniV.wkniEtiqueta);
            wkniB.setToggleGroup(wkniGrupoVista);
            wkniEstiloSeg(wkniB, false);
            wkniB.selectedProperty().addListener((wkniO, wkniA, wkniSel) -> wkniEstiloSeg(wkniB, wkniSel));
            wkniB.setOnAction(wkniE -> {
                wkniB.setSelected(true);                       // no permitir "ninguna" vista
                if (wkni_vista != wkniV) { wkni_vista = wkniV; wkniRefrescar(true); }
            });
            wkniBotonesVista.put(wkniV, wkniB);
            wkniSeg.getChildren().add(wkniB);
        }
        Region wkniSp = new Region();
        HBox.setHgrow(wkniSp, Priority.ALWAYS);
        HBox wkniBar = new HBox(10, wkniHoy, wkniPrev, wkniNext, wkniRango, wkniSp, wkniSeg);
        wkniBar.setAlignment(Pos.CENTER_LEFT);
        wkniBar.setPadding(new Insets(12, 16, 10, 16));
        return wkniBar;
    }

    private static String wkniEstiloContorno() {
        return "-fx-background-color:transparent;-fx-border-color:" + wkniLINEA + ";-fx-border-radius:18;"
            + "-fx-background-radius:18;-fx-text-fill:" + wkniTEXTO + ";-fx-padding:6 16;-fx-cursor:hand;";
    }

    private static Button wkniFlecha(String wkniIcon) {
        Button wkniB = new Button();
        wkniB.setGraphic(wkniIconFactory.wkniImage(wkniIcon, 16));
        wkniB.setAccessibleText(wkniIcon.equals("arrow-left") ? "Anterior" : "Siguiente");
        wkniB.setStyle("-fx-background-color:transparent;-fx-text-fill:" + wkniTEXTO + ";-fx-cursor:hand;-fx-padding:6 8;");
        return wkniB;
    }

    private static MenuItem wkniMenuItem(String wkniText, String wkniIcon) {
        MenuItem wkni_item = new MenuItem(wkniText);
        wkni_item.setGraphic(wkniIconFactory.wkniImage(wkniIcon, 16));
        return wkni_item;
    }

    private static void wkniEstiloSeg(ToggleButton wkniB, boolean wkniSel) {
        wkniB.setStyle("-fx-background-radius:8;-fx-padding:7 16;-fx-cursor:hand;" + (wkniSel
            ? "-fx-background-color:-accent;-fx-text-fill:-on-accent;-fx-font-weight:bold;"
            : "-fx-background-color:transparent;-fx-text-fill:" + wkniMUTED + ";"));
    }

    // =====================================================================================
    //  Panel izquierdo: Crear + mini calendario + segmentos + notificaciones
    // =====================================================================================

    private Node wkniPanelIzquierdo() {
        VBox wkni_p = new VBox(16);
        wkni_p.setPadding(new Insets(6, 14, 14, 16));
        wkni_p.setPrefWidth(280);

        MenuButton wkniCrear = new MenuButton("Crear");
        wkniCrear.setGraphic(wkniIconFactory.wkniImage("add", 17));
        wkniCrear.setStyle("-fx-background-color:-surface2;-fx-text-fill:" + wkniTEXTO + ";-fx-background-radius:24;"
            + "-fx-border-color:" + wkniLINEA + ";-fx-border-radius:24;-fx-padding:10 22;-fx-font-weight:bold;-fx-cursor:hand;"
            + "-fx-effect:dropshadow(gaussian,rgba(0,0,0,0.4),8,0,0,2);");
        MenuItem wkniMiEvento = wkniMenuItem("Evento", "event");
        MenuItem wkniMiTrabajo = wkniMenuItem("Trabajo", "work");
        MenuItem wkniMiTarea = wkniMenuItem("Tarea", "tasks");
        MenuItem wkniMiHabito = wkniMenuItem("Hábito", "habits");
        MenuItem wkniMiCumple = wkniMenuItem("Cumpleaños", "birthday");
        wkniMiEvento.setOnAction(wkniE -> wkniEventViews.wkniFormEnFechaHora(wkniStage, null, wkniCursor, null, null, wkniC, () -> wkniRefrescar(true)));
        wkniMiTrabajo.setOnAction(wkniE -> wkniEventViews.wkniFormEnFechaHora(wkniStage, null, wkniCursor, null, "Trabajo", wkniC, () -> wkniRefrescar(true)));
        wkniMiTarea.setOnAction(wkniE -> wkniTaskViews.wkniForm(null, wkniC, () -> wkniRefrescar(true)));
        wkniMiHabito.setOnAction(wkniE -> wkniDialogosAgenda.wkniNuevoHabito(wkniStage, () -> wkniRefrescar(true)));
        wkniMiCumple.setOnAction(wkniE -> wkniDialogosAgenda.wkniNuevoCumple(wkniStage, () -> wkniRefrescar(true)));
        wkniCrear.getItems().addAll(wkniMiEvento, wkniMiTrabajo, wkniMiTarea, wkniMiHabito, wkniMiCumple);

        wkni_p.getChildren().addAll(wkniCrear, wkniMini, wkniTitulo("Mis calendarios"));
        for (wkniTipoEntrada wkniT : wkniTipoEntrada.values()) wkni_p.getChildren().add(wkniFiltro(wkniT));
        wkni_p.getChildren().addAll(wkniTitulo("Notificaciones"), wkniPanelNotificaciones());

        ScrollPane wkniSp = new ScrollPane(wkni_p);
        wkniSp.setFitToWidth(true);
        wkniSp.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        wkniSp.setPrefWidth(300);
        wkniSp.setMinWidth(300);
        wkniSp.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        return wkniSp;
    }

    private static Label wkniTitulo(String wkniT) {
        Label wkniL = new Label(wkniT);
        wkniL.setStyle("-fx-text-fill:" + wkniTEXTO + ";-fx-font-weight:bold;-fx-font-size:14;");
        VBox.setMargin(wkniL, new Insets(4, 0, -8, 4));
        return wkniL;
    }

    /** Fila "casilla de color + nombre + contador" que muestra/oculta un segmento. */
    private HBox wkniFiltro(wkniTipoEntrada wkniT) {
        Region wkniCaja = new Region();
        wkniCaja.setMinSize(16, 16);
        wkniCaja.setMaxSize(16, 16);
        Label wkniNombre = new Label(wkniT.wkniEtiqueta());
        wkniNombre.setStyle("-fx-text-fill:" + wkniTEXTO + ";-fx-font-size:13.5;");
        Label wkniCuenta = new Label();
        wkniCuenta.setStyle("-fx-text-fill:" + wkniMUTED + ";-fx-font-size:12;");
        wkniContadores.put(wkniT, wkniCuenta);
        Region wkniSp = new Region();
        HBox.setHgrow(wkniSp, Priority.ALWAYS);
        HBox wkniFila = new HBox(10, wkniCaja, wkniNombre, wkniSp, wkniCuenta);
        wkniFila.setAlignment(Pos.CENTER_LEFT);
        wkniFila.setPadding(new Insets(6, 10, 6, 10));
        wkniFila.setCursor(Cursor.HAND);
        Runnable wkniPintar = () -> wkniCaja.setStyle(wkniVisibles.contains(wkniT)
            ? "-fx-background-color:" + wkniT.wkniColor() + ";-fx-background-radius:4;"
            : "-fx-background-color:transparent;-fx-border-color:" + wkniT.wkniColor() + ";-fx-border-width:2;-fx-border-radius:4;");
        wkniPintar.run();
        wkniFila.setOnMouseEntered(wkniE -> wkniFila.setStyle("-fx-background-color:-accent-soft;-fx-background-radius:8;"));
        wkniFila.setOnMouseExited(wkniE -> wkniFila.setStyle(""));
        wkniFila.setOnMouseClicked(wkniE -> {
            if (!wkniVisibles.remove(wkniT)) wkniVisibles.add(wkniT);
            wkniPintar.run();
            wkniRefrescar(true);
        });
        Tooltip.install(wkniFila, new Tooltip("Mostrar / ocultar " + wkniT.wkniEtiqueta().toLowerCase(wkniES)));
        return wkniFila;
    }

    private VBox wkniPanelNotificaciones() {
        // Los avisos de escritorio salen solo en el momento en que empieza un evento (sin antelación).
        CheckBox wkniEventos = new CheckBox("Avisar cuando empieza un evento");
        wkniEventos.setSelected(!"0".equals(wkniCfg.wkniGet(wkniCentroAvisos.wkniCFG_EVENTOS, "1")));
        wkniEventos.setOnAction(wkniE -> wkniCfg.wkniSet(wkniCentroAvisos.wkniCFG_EVENTOS, wkniEventos.isSelected() ? "1" : "0"));
        CheckBox wkni_resumen = new CheckBox("Resumen al iniciar Compás");
        wkni_resumen.setSelected("1".equals(wkniCfg.wkniGet(wkniCentroAvisos.wkniCFG_RESUMEN, "0")));
        wkni_resumen.setOnAction(wkniE -> wkniCfg.wkniSet(wkniCentroAvisos.wkniCFG_RESUMEN, wkni_resumen.isSelected() ? "1" : "0"));

        Button wkniProbar = new Button("Probar notificación");
        wkniProbar.setGraphic(wkniIconFactory.wkniImage("notification", 16));
        wkniProbar.setOnAction(wkniE -> wkniNotificacionEscritorio.wkniMostrar(wkniTipoEntrada.EVENTO, "Notificación de prueba",
            "Así verás tus recordatorios en la pantalla, incluso con Compás minimizado.", 8));

        Label wkni_nota = new Label("Al cerrar la ventana, Compás sigue avisando desde la bandeja del sistema. Más opciones en Configuración.");
        wkni_nota.setWrapText(true);
        wkni_nota.setStyle("-fx-text-fill:" + wkniMUTED + ";-fx-font-size:12;");
        VBox wkniV = new VBox(10, wkniEventos, wkni_resumen, wkniProbar, wkni_nota);
        wkniV.setPadding(new Insets(0, 0, 0, 4));
        return wkniV;
    }

    // ---------- mini calendario ----------

    private void wkniReconstruirMini() {
        wkniMini.getChildren().clear();
        Label wkniT = new Label(wkniCap(wkniMiniMes.getMonth().getDisplayName(TextStyle.FULL, wkniES)) + " de " + wkniMiniMes.getYear());
        wkniT.setStyle("-fx-text-fill:" + wkniTEXTO + ";-fx-font-weight:bold;-fx-font-size:14;");
        Region wkniSp = new Region();
        HBox.setHgrow(wkniSp, Priority.ALWAYS);
        Button wkniAnt = wkniFlecha("arrow-left"), wkniSig = wkniFlecha("arrow-right");
        wkniAnt.setOnAction(wkniE -> { wkniMiniMes = wkniMiniMes.minusMonths(1); wkniReconstruirMini(); });
        wkniSig.setOnAction(wkniE -> { wkniMiniMes = wkniMiniMes.plusMonths(1); wkniReconstruirMini(); });
        HBox wkniCab = new HBox(2, wkniT, wkniSp, wkniAnt, wkniSig);
        wkniCab.setAlignment(Pos.CENTER_LEFT);

        GridPane wkniG = new GridPane();
        wkniG.setHgap(2);
        wkniG.setVgap(2);
        String[] wkniDs = {"D", "L", "M", "X", "J", "V", "S"};
        for (int wkniI = 0; wkniI < 7; wkniI++) {
            Label wkniL = new Label(wkniDs[wkniI]);
            wkniL.setMinWidth(30);
            wkniL.setAlignment(Pos.CENTER);
            wkniL.setStyle("-fx-text-fill:" + wkniMUTED + ";-fx-font-size:12;-fx-font-weight:bold;");
            wkniG.add(wkniL, wkniI, 0);
        }
        LocalDate wkniPrimero = wkniMiniMes.atDay(1).with(TemporalAdjusters.previousOrSame(wkniPRIMER_DIA));
        LocalDate wkniHoy = LocalDate.now();
        for (int wkniI = 0; wkniI < 42; wkniI++) {
            LocalDate wkniD = wkniPrimero.plusDays(wkniI);
            boolean wkniFuera = !YearMonth.from(wkniD).equals(wkniMiniMes);
            Button wkniB = new Button(String.valueOf(wkniD.getDayOfMonth()));
            wkniB.setMinSize(30, 30);
            wkniB.setPrefSize(30, 30);
            wkniB.setMaxSize(30, 30);
            String wkniBg, wkniFg, wkniExtra = "";
            if (wkniD.equals(wkniHoy)) { wkniBg = "-accent"; wkniFg = "-on-accent"; wkniExtra = "-fx-font-weight:bold;"; }
            else if (wkniD.equals(wkniCursor)) { wkniBg = "-accent-soft"; wkniFg = "-text"; wkniExtra = "-fx-font-weight:bold;"; }
            else if (wkniCalendarioUnificadoService.wkniHayAlgo(wkniD, wkni_datos, wkniVisibles)) { wkniBg = "-surface2"; wkniFg = "-text"; }
            else { wkniBg = "transparent"; wkniFg = wkniFuera ? "-muted" : "-text2"; }
            wkniB.setStyle("-fx-background-color:" + wkniBg + ";-fx-text-fill:" + wkniFg + ";-fx-background-radius:15;"
                + "-fx-padding:0;-fx-font-size:12;-fx-cursor:hand;" + wkniExtra);
            wkniB.setOnAction(wkniE -> wkniIrA(wkniD));
            wkniG.add(wkniB, wkniI % 7, 1 + wkniI / 7);
        }
        wkniMini.getChildren().addAll(wkniCab, wkniG);
    }

    // =====================================================================================
    //  Navegación y refresco
    // =====================================================================================

    private void wkniMover(int wkniDir) {
        wkniCursor = switch (wkni_vista) {
            case DIA -> wkniCursor.plusDays(wkniDir);
            case SEMANA -> wkniCursor.plusWeeks(wkniDir);
            case MES, AGENDA -> wkniCursor.plusMonths(wkniDir);
        };
        wkniMiniMes = YearMonth.from(wkniCursor);
        wkniRefrescar(true);
    }

    private void wkniIrA(LocalDate wkniD) {
        wkniCursor = wkniD;
        wkniMiniMes = YearMonth.from(wkniD);
        wkniRefrescar(true);
    }

    private void wkniIrADia(LocalDate wkniD) {
        wkniCursor = wkniD;
        wkniMiniMes = YearMonth.from(wkniD);
        wkni_vista = wkniVista.DIA;
        wkniBotonesVista.get(wkniVista.DIA).setSelected(true);
        wkniRefrescar(true);
    }

    /** @param forzar true si cambió la vista/fecha/filtros; false cuando solo puede haber cambiado la BD. */
    private void wkniRefrescar(boolean wkniForzar) {
        LocalDate wkniDesde = wkniCursor.minusMonths(1).withDayOfMonth(1);
        LocalDate wkniHasta = wkniCursor.plusMonths(2).withDayOfMonth(1).minusDays(1);
        wkni_datos = wkniAgendaDatos.wkniCargar(wkniC, wkniDesde, wkniHasta);
        String wkniFirma = wkniFirmaDe(wkni_datos);
        if (!wkniForzar && wkniFirma.equals(wkniFirmaDatos)) return;     // nada cambió: no reconstruir (evita parpadeos)
        wkniFirmaDatos = wkniFirma;

        wkniColumnas.clear();
        Node wkniContenido = switch (wkni_vista) {
            case DIA -> wkniGrillaHoraria(List.of(wkniCursor));
            case SEMANA -> wkniGrillaHoraria(wkniSemana(wkniCursor));
            case MES -> wkniMesView();
            case AGENDA -> wkniAgendaView();
        };
        wkniCentro.getChildren().setAll(wkniContenido);
        wkniRango.setText(wkniTextoRango());
        wkniReconstruirMini();
        wkniActualizarContadores();
    }

    private static String wkniFirmaDe(wkniDatos wkniD) {
        StringBuilder wkniSb = new StringBuilder();
        for (wkniEvento wkniE : wkniD.wkniEventos())
            wkniSb.append(wkniE.wkniGetId()).append('|').append(wkniE.wkniGetTitulo()).append('|').append(wkniE.wkniGetFecha()).append('|')
              .append(wkniE.wkniGetHoraInicio()).append('|').append(wkniE.wkniGetHoraFin()).append('|').append(wkniE.wkniGetCategoria()).append(';');
        for (wkniTarea wkniT : wkniD.wkniTareas())
            wkniSb.append(wkniT.wkniGetId()).append('|').append(wkniT.wkniGetTitulo()).append('|').append(wkniT.wkniGetFecha()).append('|')
              .append(wkniT.wkniGetEstado()).append('|').append(wkniT.wkniGetProyectoId()).append(';');
        for (wkniHabito wkniH : wkniD.wkniHabitos())
            wkniSb.append(wkniH.wkniGetId()).append('|').append(wkniH.wkniGetNombre()).append('|').append(wkniH.wkniGetFrecuencia()).append('|')
              .append(wkniH.wkniIsActivo()).append(';');
        for (wkniContacto wkniK : wkniD.wkniContactos())
            wkniSb.append(wkniK.wkniGetId()).append('|').append(wkniK.wkniGetNombre()).append('|').append(wkniK.wkniGetCumpleanos()).append(';');
        wkniSb.append(wkniD.wkniHabitosCumplidos().hashCode());
        return wkniSb.toString();
    }

    private static List<LocalDate> wkniSemana(LocalDate wkniRef) {
        LocalDate wkniIni = wkniRef.with(TemporalAdjusters.previousOrSame(wkniPRIMER_DIA));
        List<LocalDate> wkniR = new ArrayList<>();
        for (int wkniI = 0; wkniI < 7; wkniI++) wkniR.add(wkniIni.plusDays(wkniI));
        return wkniR;
    }

    private List<LocalDate> wkniRangoVisible() {
        List<LocalDate> wkniR = new ArrayList<>();
        switch (wkni_vista) {
            case DIA -> wkniR.add(wkniCursor);
            case SEMANA -> wkniR.addAll(wkniSemana(wkniCursor));
            case MES -> { YearMonth wkniYm = YearMonth.from(wkniCursor); for (int wkniI = 1; wkniI <= wkniYm.lengthOfMonth(); wkniI++) wkniR.add(wkniYm.atDay(wkniI)); }
            case AGENDA -> { for (int wkniI = 0; wkniI < 30; wkniI++) wkniR.add(wkniCursor.plusDays(wkniI)); }
        }
        return wkniR;
    }

    private String wkniTextoRango() {
        return switch (wkni_vista) {
            case DIA -> wkniUi.wkniFormatDate(wkniCursor);
            case MES -> wkniCap(wkniCursor.getMonth().getDisplayName(TextStyle.FULL, wkniES)) + " de " + wkniCursor.getYear();
            case AGENDA -> "Próximos 30 días";
            case SEMANA -> {
                List<LocalDate> wkniS = wkniSemana(wkniCursor);
                LocalDate wkniA = wkniS.getFirst(), wkniB = wkniS.getLast();
                if (wkniA.getMonth() == wkniB.getMonth()) yield wkniCap(wkniA.getMonth().getDisplayName(TextStyle.FULL, wkniES)) + " de " + wkniA.getYear();
                if (wkniA.getYear() == wkniB.getYear()) yield wkniCorto(wkniA) + " – " + wkniCorto(wkniB) + " de " + wkniA.getYear();
                yield wkniCorto(wkniA) + " de " + wkniA.getYear() + " – " + wkniCorto(wkniB) + " de " + wkniB.getYear();
            }
        };
    }

    private static String wkniCorto(LocalDate wkniD) {
        String wkniM = wkniD.getMonth().getDisplayName(TextStyle.FULL, wkniES);
        return wkniM.substring(0, Math.min(3, wkniM.length()));
    }

    private void wkniActualizarContadores() {
        List<LocalDate> wkniDias = wkniRangoVisible();
        for (wkniTipoEntrada wkniT : wkniTipoEntrada.values()) {
            int wkniTotal = 0, wkniHechos = 0;
            for (LocalDate wkniD : wkniDias)
                for (wkniEntrada wkniE : wkniCalendarioUnificadoService.wkniDelDia(wkniD, wkni_datos, EnumSet.of(wkniT))) {
                    wkniTotal++;
                    if (wkniE.wkniHecha()) wkniHechos++;
                }
            Label wkniL = wkniContadores.get(wkniT);
            if (wkniL != null) wkniL.setText(wkniTotal == 0 ? "" : wkniT == wkniTipoEntrada.HABITO ? wkniHechos + "/" + wkniTotal : String.valueOf(wkniTotal));
        }
    }

    // =====================================================================================
    //  Vista DÍA / SEMANA: cuadrícula horaria
    // =====================================================================================

    private Node wkniGrillaHoraria(List<LocalDate> wkniDias) {
        int wkniN = wkniDias.size();
        LocalDate wkniHoy = LocalDate.now();

        GridPane wkniCab = wkniGridCols(wkniN);
        wkniCab.setPadding(new Insets(0, 14, 0, 0));          // ≈ ancho de la barra de desplazamiento del cuerpo
        GridPane wkniTodoDia = wkniGridCols(wkniN);
        wkniTodoDia.setPadding(new Insets(0, 14, 6, 0));

        Label wkniZona = new Label(wkniZonaGmt());
        wkniZona.setStyle("-fx-text-fill:" + wkniMUTED + ";-fx-font-size:12;");
        wkniZona.setMaxWidth(Double.MAX_VALUE);
        wkniZona.setAlignment(Pos.BOTTOM_CENTER);
        GridPane.setValignment(wkniZona, javafx.geometry.VPos.BOTTOM);
        wkniCab.add(wkniZona, 0, 0);

        GridPane wkniCuerpo = wkniGridCols(wkniN);
        Pane wkniGutter = new Pane();
        wkniGutter.setMinHeight(24 * wkniPX_HORA);
        wkniGutter.setPrefHeight(24 * wkniPX_HORA);
        for (int wkniH = 1; wkniH < 24; wkniH++) {
            Label wkniL = new Label(wkniEtiquetaHora(wkniH));
            wkniL.setPrefWidth(wkniGUTTER - 8);
            wkniL.setAlignment(Pos.CENTER_RIGHT);
            wkniL.setLayoutY(wkniH * wkniPX_HORA - 8);
            wkniL.setStyle("-fx-text-fill:" + wkniMUTED + ";-fx-font-size:12;");
            wkniGutter.getChildren().add(wkniL);
        }
        wkniCuerpo.add(wkniGutter, 0, 0);

        int wkniLimite = wkniN == 1 ? 60 : 3;
        for (int wkniI = 0; wkniI < wkniN; wkniI++) {
            LocalDate wkniD = wkniDias.get(wkniI);
            List<wkniEntrada> wkniEs = wkniCalendarioUnificadoService.wkniDelDia(wkniD, wkni_datos, wkniVisibles);

            wkniCab.add(wkniCabeceraDia(wkniD, wkniHoy), wkniI + 1, 0);
            wkniTodoDia.add(wkniCeldaTodoElDia(wkniD, wkniEs.stream().filter(wkniEntrada::wkniTodoElDia).toList(), wkniLimite, wkniN > 1), wkniI + 1, 0);

            wkniColumnaDia wkniCol = new wkniColumnaDia(wkniD.equals(wkniHoy));
            for (var wkniCr : wkniCalendarioUnificadoService.wkniAsignarCarriles(wkniEs.stream().filter(wkniX -> !wkniX.wkniTodoElDia()).toList()))
                wkniCol.wkniAgregar(wkniBloque(wkniCr.wkni_entrada()), wkniCr.wkni_entrada().wkniMinutoInicio(), wkniCr.wkni_entrada().wkniMinutoFin(), wkniCr.wkni_carril(), wkniCr.wkniCarriles());
            wkniCol.setOnMouseClicked(wkniEv -> {
                int wkniH = (int) Math.max(0, Math.min(23, wkniEv.getY() / wkniPX_HORA));
                wkniEventViews.wkniFormEnFechaHora(wkniStage, null, wkniD, String.format("%02d:00", wkniH), null, wkniC, () -> wkniRefrescar(true));
            });
            if (wkniCol.wkniEsHoy) wkniCol.wkniSetAhora(wkniMinutoActual());
            wkniColumnas.add(wkniCol);
            wkniCuerpo.add(wkniCol, wkniI + 1, 0);
        }

        ScrollPane wkniSp = new ScrollPane(wkniCuerpo);
        wkniSp.setFitToWidth(true);
        wkniSp.setStyle("-fx-background:transparent;-fx-background-color:transparent;");
        final double wkniObjetivo = wkniVvalue;
        wkniSp.vvalueProperty().addListener((wkniO, wkniA, wkniB) -> wkniVvalue = wkniB.doubleValue());
        wkniAlTenerViewport(wkniSp, () -> wkniSp.setVvalue(wkniObjetivo >= 0 ? wkniObjetivo : wkniVvalueParaHora(wkniSp, 7)));

        Region wkniSep = new Region();
        wkniSep.setMinHeight(1);
        wkniSep.setMaxHeight(1);
        wkniSep.setStyle("-fx-background-color:" + wkniLINEA + ";");
        VBox wkniCaja = new VBox(wkniCab, wkniTodoDia, wkniSep, wkniSp);
        VBox.setVgrow(wkniSp, Priority.ALWAYS);
        wkniCaja.setStyle("-fx-background-color:" + wkniPANEL + ";-fx-background-radius:16;");
        return wkniCaja;
    }

    private static GridPane wkniGridCols(int wkniN) {
        GridPane wkniG = new GridPane();
        ColumnConstraints wkniG0 = new ColumnConstraints(wkniGUTTER);
        wkniG0.setMinWidth(wkniGUTTER);
        wkniG0.setMaxWidth(wkniGUTTER);
        wkniG.getColumnConstraints().add(wkniG0);
        for (int wkniI = 0; wkniI < wkniN; wkniI++) {
            ColumnConstraints wkniCc = new ColumnConstraints();
            wkniCc.setHgrow(Priority.ALWAYS);
            wkniCc.setMinWidth(0);
            wkniCc.setPrefWidth(100);
            wkniCc.setFillWidth(true);
            wkniG.getColumnConstraints().add(wkniCc);
        }
        return wkniG;
    }

    private Node wkniCabeceraDia(LocalDate wkniD, LocalDate wkniHoy) {
        boolean wkniEsHoy = wkniD.equals(wkniHoy);
        Label wkniDow = new Label(wkniD.getDayOfWeek().getDisplayName(TextStyle.SHORT, wkniES).toUpperCase(wkniES).replace(".", ""));
        wkniDow.setStyle("-fx-text-fill:" + (wkniEsHoy ? "-accent" : wkniMUTED) + ";-fx-font-size:12;-fx-font-weight:bold;");
        Label wkniNum = new Label(String.valueOf(wkniD.getDayOfMonth()));
        wkniNum.setMinSize(40, 40);
        wkniNum.setPrefSize(40, 40);
        wkniNum.setAlignment(Pos.CENTER);
        wkniNum.setStyle(wkniEsHoy
            ? "-fx-background-color:-accent;-fx-background-radius:20;-fx-text-fill:-on-accent;-fx-font-size:20;-fx-font-weight:bold;"
            : "-fx-text-fill:" + wkniTEXTO + ";-fx-font-size:20;");
        VBox wkniV = new VBox(2, wkniDow, wkniNum);
        wkniV.setAlignment(Pos.CENTER);
        wkniV.setPadding(new Insets(8, 0, 6, 0));
        wkniV.setCursor(Cursor.HAND);
        wkniV.setOnMouseClicked(wkniE -> wkniIrADia(wkniD));
        return wkniV;
    }

    /** Fila "todo el día": tareas, hábitos, cumpleaños y eventos sin hora. */
    private Node wkniCeldaTodoElDia(LocalDate wkniD, List<wkniEntrada> wkniLista, int wkniLimite, boolean wkniAgrupar) {
        VBox wkniV = new VBox(2);
        wkniV.setPadding(new Insets(2, 3, 2, 3));
        wkniV.setMinHeight(26);
        wkniV.setStyle("-fx-border-color:transparent transparent transparent " + wkniLINEA + ";-fx-border-width:0 0 0 1;");
        List<Node> wkniChips = wkniChipsDelDia(wkniD, wkniLista, wkniAgrupar);
        int wkniN = 0;
        for (Node wkniCh : wkniChips) {
            if (wkniN++ == wkniLimite) {
                Label wkniMas = new Label("+" + (wkniChips.size() - wkniLimite) + " más");
                wkniMas.setStyle("-fx-text-fill:-accent;-fx-font-size:12;-fx-cursor:hand;-fx-padding:1 6;");
                wkniMas.setOnMouseClicked(wkniE -> wkniIrADia(wkniD));
                wkniV.getChildren().add(wkniMas);
                break;
            }
            wkniV.getChildren().add(wkniCh);
        }
        return wkniV;
    }

    /** Un bloque de evento con hora, posicionado por {@link ColumnaDia}. */
    private Node wkniBloque(wkniEntrada wkniE) {
        Label wkniT = new Label(wkniE.wkniTitulo());
        wkniT.setWrapText(true);
        wkniT.setStyle("-fx-text-fill:" + wkniTEXTO + ";-fx-font-weight:bold;-fx-font-size:12;");
        Label wkniH = new Label(wkniE.wkniInicio().format(wkniHoras.wkniSALIDA) + " – " + (wkniE.wkniFin() == null ? "" : wkniE.wkniFin().format(wkniHoras.wkniSALIDA)));
        wkniH.setStyle("-fx-text-fill:" + wkniE.wkni_tipo().wkniColor() + ";-fx-font-size:12;");
        VBox wkniB = new VBox(1, wkniT, wkniH);
        wkniB.setPadding(new Insets(3, 6, 3, 6));
        wkniB.setCursor(Cursor.HAND);
        wkniB.setStyle("-fx-background-color:" + wkniE.wkni_tipo().wkniFondo() + ";-fx-background-radius:6;"
            + "-fx-border-color:" + wkniE.wkni_tipo().wkniColor() + ";-fx-border-width:0 0 0 3;-fx-border-radius:6;");
        Rectangle wkniRecorte = new Rectangle();
        wkniRecorte.widthProperty().bind(wkniB.widthProperty());
        wkniRecorte.heightProperty().bind(wkniB.heightProperty());
        wkniB.setClip(wkniRecorte);
        Tooltip.install(wkniB, new Tooltip(wkniE.wkni_tipo().wkniEtiqueta() + ": " + wkniE.wkniTitulo()
            + (wkniE.wkniDetalle() == null || wkniE.wkniDetalle().isBlank() ? "" : "\n" + wkniE.wkniDetalle()) + "\nClic para editar"));
        wkniB.setOnMouseClicked(wkniEv -> { wkniEv.consume(); wkniAccion(wkniE, wkniEv); });
        return wkniB;
    }

    private static String wkniEtiquetaHora(int wkniH) {
        int wkniH12 = wkniH % 12 == 0 ? 12 : wkniH % 12;
        return wkniH12 + (wkniH < 12 ? " AM" : " PM");
    }

    private static String wkniZonaGmt() {
        int wkni_horas = ZonedDateTime.now().getOffset().getTotalSeconds() / 3600;
        return String.format("GMT%+03d", wkni_horas);
    }

    private static int wkniMinutoActual() {
        LocalTime wkniT = LocalTime.now();
        return wkniT.getHour() * 60 + wkniT.getMinute();
    }

    private void wkniActualizarAhora() {
        int wkniM = wkniMinutoActual();
        for (wkniColumnaDia wkniCol : wkniColumnas) if (wkniCol.wkniEsHoy) wkniCol.wkniSetAhora(wkniM);
    }

    private static double wkniVvalueParaHora(ScrollPane wkniSp, int wkniHora) {
        double wkniMax = 24 * wkniPX_HORA - wkniSp.getViewportBounds().getHeight();
        return wkniMax <= 0 ? 0 : Math.max(0, Math.min(1, wkniHora * wkniPX_HORA / wkniMax));
    }

    /** Ejecuta la acción cuando el ScrollPane ya tiene tamaño (antes no se puede calcular el desplazamiento). */
    private static void wkniAlTenerViewport(ScrollPane wkniSp, Runnable wkniR) {
        Bounds wkniB0 = wkniSp.getViewportBounds();
        if (wkniB0 != null && wkniB0.getHeight() > 0) { Platform.runLater(wkniR); return; }
        wkniSp.viewportBoundsProperty().addListener(new ChangeListener<Bounds>() {
            @Override public void changed(ObservableValue<? extends Bounds> wkniO, Bounds wkniA, Bounds wkniB) {
                if (wkniB != null && wkniB.getHeight() > 0) {
                    wkniSp.viewportBoundsProperty().removeListener(this);
                    Platform.runLater(wkniR);
                }
            }
        });
    }

    /** Columna de un día: líneas de hora, bloques posicionados por minuto y línea roja de "ahora". */
    private static final class wkniColumnaDia extends Pane {
        private record wkniHueco(Node wkniNodo, int wkniIni, int wkniFin, int wkni_carril, int wkniCarriles) {}

        final boolean wkniEsHoy;
        private final List<wkniHueco> wkniHuecos = new ArrayList<>();
        private final List<Region> wkniLineas = new ArrayList<>();
        private final Region wkniLineaAhora = new Region();
        private final Circle wkniPuntoAhora = new Circle(5, Color.web("#E5484D"));
        private int wkniAhora = -1;

        wkniColumnaDia(boolean wkniEsHoy) {
            this.wkniEsHoy = wkniEsHoy;
            setMinHeight(24 * wkniPX_HORA);
            setPrefHeight(24 * wkniPX_HORA);
            setMaxHeight(24 * wkniPX_HORA);
            setStyle("-fx-border-color:transparent transparent transparent " + wkniLINEA + ";-fx-border-width:0 0 0 1;");
            for (int wkniH = 0; wkniH < 24; wkniH++) {
                Region wkniL = new Region();
                wkniL.setStyle("-fx-background-color:-surface2;");
                wkniL.setMouseTransparent(true);
                wkniLineas.add(wkniL);
                getChildren().add(wkniL);
            }
            wkniLineaAhora.setStyle("-fx-background-color:#E5484D;");
            wkniLineaAhora.setMouseTransparent(true);
            wkniPuntoAhora.setMouseTransparent(true);
        }

        void wkniAgregar(Node wkniN, int wkniIni, int wkniFin, int wkni_carril, int wkniCarriles) {
            wkniHuecos.add(new wkniHueco(wkniN, wkniIni, wkniFin, wkni_carril, wkniCarriles));
            getChildren().add(wkniN);
        }

        void wkniSetAhora(int wkniMinuto) {
            wkniAhora = wkniMinuto;
            if (!getChildren().contains(wkniLineaAhora)) getChildren().addAll(wkniLineaAhora, wkniPuntoAhora);
            wkniLineaAhora.toFront();
            wkniPuntoAhora.toFront();
            requestLayout();
        }

        @Override protected void layoutChildren() {
            double wkniW = getWidth();
            for (int wkniH = 0; wkniH < wkniLineas.size(); wkniH++) wkniLineas.get(wkniH).resizeRelocate(0, wkniH * wkniPX_HORA, wkniW, 1);
            for (wkniHueco wkniHu : wkniHuecos) {
                double wkniCw = (wkniW - 6) / wkniHu.wkniCarriles();
                double wkniX = 2 + wkniHu.wkni_carril() * wkniCw;
                double wkniY = wkniHu.wkniIni() * wkniPX_HORA / 60.0;
                double wkniAlto = Math.max(20, (wkniHu.wkniFin() - wkniHu.wkniIni()) * wkniPX_HORA / 60.0 - 2);
                wkniHu.wkniNodo().resizeRelocate(wkniX, wkniY, Math.max(10, wkniCw - 2), wkniAlto);
            }
            if (wkniAhora >= 0) {
                double wkniY = wkniAhora * wkniPX_HORA / 60.0;
                wkniLineaAhora.resizeRelocate(0, wkniY - 1, wkniW, 2);
                wkniPuntoAhora.setCenterX(5);
                wkniPuntoAhora.setCenterY(wkniY);
            }
        }
    }

    // =====================================================================================
    //  Vista MES
    // =====================================================================================

    private Node wkniMesView() {
        YearMonth wkniYm = YearMonth.from(wkniCursor);
        LocalDate wkniPrimero = wkniYm.atDay(1).with(TemporalAdjusters.previousOrSame(wkniPRIMER_DIA));
        int wkniOff = (int) ChronoUnit.DAYS.between(wkniPrimero, wkniYm.atDay(1));
        int wkniSemanas = (wkniOff + wkniYm.lengthOfMonth() + 6) / 7;
        LocalDate wkniHoy = LocalDate.now();

        GridPane wkniG = new GridPane();
        wkniG.setHgap(1);
        wkniG.setVgap(1);
        wkniG.setStyle("-fx-background-color:" + wkniLINEA + ";-fx-background-radius:16;-fx-padding:1;");
        for (int wkniI = 0; wkniI < 7; wkniI++) {
            ColumnConstraints wkniCc = new ColumnConstraints();
            wkniCc.setPercentWidth(100.0 / 7);
            wkniCc.setHgrow(Priority.ALWAYS);
            wkniCc.setMinWidth(0);
            wkniG.getColumnConstraints().add(wkniCc);
        }
        wkniG.getRowConstraints().add(new RowConstraints());
        for (int wkniR = 0; wkniR < wkniSemanas; wkniR++) {
            RowConstraints wkniRc = new RowConstraints();
            wkniRc.setVgrow(Priority.ALWAYS);
            wkniRc.setPrefHeight(100);
            wkniRc.setMinHeight(60);
            wkniG.getRowConstraints().add(wkniRc);
        }
        for (int wkniI = 0; wkniI < 7; wkniI++) {
            LocalDate wkniD = wkniPrimero.plusDays(wkniI);
            Label wkniL = new Label(wkniD.getDayOfWeek().getDisplayName(TextStyle.SHORT, wkniES).toUpperCase(wkniES).replace(".", ""));
            wkniL.setMaxWidth(Double.MAX_VALUE);
            wkniL.setAlignment(Pos.CENTER);
            wkniL.setPadding(new Insets(8, 0, 8, 0));
            wkniL.setStyle("-fx-background-color:" + wkniPANEL + ";-fx-text-fill:" + wkniMUTED + ";-fx-font-size:12;-fx-font-weight:bold;");
            wkniG.add(wkniL, wkniI, 0);
        }
        for (int wkniI = 0; wkniI < wkniSemanas * 7; wkniI++) {
            LocalDate wkniD = wkniPrimero.plusDays(wkniI);
            boolean wkniDelMes = YearMonth.from(wkniD).equals(wkniYm);
            VBox wkni_celda = new VBox(2);
            wkni_celda.setPadding(new Insets(4));
            wkni_celda.setMinSize(0, 0);
            wkni_celda.setCursor(Cursor.HAND);
            wkni_celda.setStyle("-fx-background-color:" + (wkniDelMes ? wkniPANEL : "-bg") + ";");
            Label wkniNum = new Label(String.valueOf(wkniD.getDayOfMonth()));
            wkniNum.setMinSize(24, 24);
            wkniNum.setAlignment(Pos.CENTER);
            wkniNum.setStyle(wkniD.equals(wkniHoy)
                ? "-fx-background-color:-accent;-fx-background-radius:12;-fx-text-fill:-on-accent;-fx-font-weight:bold;"
                : "-fx-text-fill:" + (wkniDelMes ? wkniTEXTO : "-muted") + ";");
            HBox wkniCabecera = new HBox(wkniNum);
            wkniCabecera.setAlignment(Pos.CENTER);
            wkni_celda.getChildren().add(wkniCabecera);
            List<Node> wkniChips = wkniChipsDelDia(wkniD, wkniCalendarioUnificadoService.wkniDelDia(wkniD, wkni_datos, wkniVisibles), true);
            int wkniN = 0;
            for (Node wkniCh : wkniChips) {
                if (wkniN++ == 3) {
                    Label wkniMas = new Label("+" + (wkniChips.size() - 3) + " más");
                    wkniMas.setStyle("-fx-text-fill:-accent;-fx-font-size:12;-fx-padding:0 6;");
                    wkni_celda.getChildren().add(wkniMas);
                    break;
                }
                wkni_celda.getChildren().add(wkniCh);
            }
            wkni_celda.setOnMouseClicked(wkniE -> wkniIrADia(wkniD));
            wkniG.add(wkni_celda, wkniI % 7, 1 + wkniI / 7);
        }
        VBox wkniCaja = new VBox(wkniG);
        VBox.setVgrow(wkniG, Priority.ALWAYS);
        return wkniCaja;
    }

    // =====================================================================================
    //  Vista AGENDA (lista)
    // =====================================================================================

    private Node wkniAgendaView() {
        VBox wkniLista = new VBox(6);
        wkniLista.setPadding(new Insets(16));
        LocalDate wkniHoy = LocalDate.now();
        int wkniDias = 0;
        for (int wkniI = 0; wkniI < 30; wkniI++) {
            LocalDate wkniD = wkniCursor.plusDays(wkniI);
            List<wkniEntrada> wkniEs = wkniCalendarioUnificadoService.wkniDelDia(wkniD, wkni_datos, wkniVisibles).stream()
                .filter(wkniE -> wkniE.wkni_tipo() != wkniTipoEntrada.HABITO || wkniD.equals(wkniHoy)).toList();   // los hábitos diarios solo se listan hoy
            if (wkniEs.isEmpty()) continue;
            wkniDias++;
            String wkniRel = wkniD.equals(wkniHoy) ? "Hoy · " : wkniD.equals(wkniHoy.plusDays(1)) ? "Mañana · " : "";
            Label wkniCab = new Label(wkniRel + wkniUi.wkniFormatDate(wkniD));
            wkniCab.setStyle("-fx-text-fill:" + (wkniD.equals(wkniHoy) ? "-accent" : wkniTEXTO) + ";-fx-font-weight:bold;-fx-font-size:14;");
            VBox.setMargin(wkniCab, new Insets(wkniDias == 1 ? 0 : 12, 0, 2, 0));
            wkniLista.getChildren().add(wkniCab);
            for (wkniEntrada wkniE : wkniEs) wkniLista.getChildren().add(wkniFilaAgenda(wkniE));
        }
        if (wkniDias == 0) {
            Label wkniVacio = new Label("No hay nada en los próximos 30 días con los calendarios seleccionados.");
            wkniVacio.setStyle("-fx-text-fill:" + wkniMUTED + ";");
            wkniLista.getChildren().add(wkniVacio);
        }
        ScrollPane wkniSp = new ScrollPane(wkniLista);
        wkniSp.setFitToWidth(true);
        wkniSp.setStyle("-fx-background:" + wkniPANEL + ";-fx-background-color:" + wkniPANEL + ";-fx-background-radius:16;");
        return wkniSp;
    }

    private Node wkniFilaAgenda(wkniEntrada wkniE) {
        Label wkniHora = new Label(wkniE.wkniTodoElDia() ? "Todo el día"
            : wkniE.wkniInicio().format(wkniHoras.wkniSALIDA) + " – " + (wkniE.wkniFin() == null ? "" : wkniE.wkniFin().format(wkniHoras.wkniSALIDA)));
        wkniHora.setMinWidth(110);
        wkniHora.setStyle("-fx-text-fill:" + wkniMUTED + ";");
        Region wkniPunto = new Region();
        wkniPunto.setMinSize(10, 10);
        wkniPunto.setMaxSize(10, 10);
        wkniPunto.setStyle("-fx-background-color:" + wkniE.wkni_tipo().wkniColor() + ";-fx-background-radius:5;");
        Label wkniT = new Label(wkniE.wkniTitulo());
        wkniT.setStyle("-fx-text-fill:" + wkniTEXTO + ";-fx-font-size:14;");
        Label wkniTipoTexto = new Label(wkniE.wkni_tipo().wkniEtiqueta()
            + (wkniE.wkniDetalle() == null || wkniE.wkniDetalle().isBlank() ? "" : " · " + wkniE.wkniDetalle()));
        wkniTipoTexto.setStyle("-fx-text-fill:" + wkniMUTED + ";-fx-font-size:12;");
        HBox wkni_tipo = new HBox(5, wkniIconFactory.wkniImage(wkniE.wkni_tipo().wkniIcono(), 14), wkniTipoTexto);
        wkni_tipo.setAlignment(Pos.CENTER_LEFT);
        wkni_tipo.setStyle("-fx-text-fill:" + wkniMUTED + ";-fx-font-size:12;");
        HBox wkniTitulo = new HBox(5, wkniIconFactory.wkniImage(wkniE.wkniHecha() ? "check" : wkniE.wkni_tipo().wkniIcono(), 15), wkniT);
        wkniTitulo.setAlignment(Pos.CENTER_LEFT);
        VBox wkniTextos = new VBox(3, wkniTitulo, wkni_tipo);
        HBox wkniF = new HBox(12, wkniHora, wkniPunto, wkniTextos);
        wkniF.setAlignment(Pos.CENTER_LEFT);
        wkniF.setPadding(new Insets(8, 12, 8, 12));
        wkniF.setCursor(Cursor.HAND);
        wkniF.setStyle("-fx-background-color:-surface2;-fx-background-radius:10;");
        if (wkniE.wkniHecha()) wkniF.setOpacity(0.6);
        wkniF.setOnMouseClicked(wkniEv -> wkniAccion(wkniE, wkniEv));
        return wkniF;
    }

    // =====================================================================================
    //  Chips y acciones
    // =====================================================================================

    /** Chips de un día; si {@code agrupar}, varios hábitos se resumen en una sola etiqueta. */
    private List<Node> wkniChipsDelDia(LocalDate wkniD, List<wkniEntrada> wkniLista, boolean wkniAgrupar) {
        List<Node> wkniR = new ArrayList<>();
        List<wkniEntrada> wkniHabitos = wkniLista.stream().filter(wkniE -> wkniE.wkni_tipo() == wkniTipoEntrada.HABITO).toList();
        boolean wkniResumir = wkniAgrupar && wkniHabitos.size() > 1;
        for (wkniEntrada wkniE : wkniLista) {
            if (wkniE.wkniTodoElDia() && !(wkniResumir && wkniE.wkni_tipo() == wkniTipoEntrada.HABITO)) wkniR.add(wkniChip(wkniE));
            else if (!wkniE.wkniTodoElDia() && wkni_vista == wkniVista.MES) wkniR.add(wkniChip(wkniE));      // en el mes también se ven los eventos con hora
        }
        if (wkniResumir) {
            long wkniHechos = wkniHabitos.stream().filter(wkniEntrada::wkniHecha).count();
            Label wkniTexto = new Label("Hábitos " + wkniHechos + "/" + wkniHabitos.size());
            HBox wkniChip = new HBox(4, wkniIconFactory.wkniImage("habits", 13), wkniTexto);
            wkniChip.setMaxWidth(Double.MAX_VALUE);
            wkniChip.setMinWidth(0);
            wkniChip.setAlignment(Pos.CENTER_LEFT);
            wkniChip.setStyle("-fx-background-color:" + wkniTipoEntrada.HABITO.wkniFondo() + ";-fx-text-fill:" + wkniTipoEntrada.HABITO.wkniColor()
                + ";-fx-background-radius:6;-fx-padding:3 6;-fx-font-size:12;-fx-cursor:hand;");
            wkniChip.setOnMouseClicked(wkniEv -> { wkniEv.consume(); wkniIrADia(wkniD); });
            Tooltip.install(wkniChip, new Tooltip("Clic para ver y marcar tus hábitos de este día"));
            wkniR.add(wkniChip);
        }
        return wkniR;
    }

    private Node wkniChip(wkniEntrada wkniE) {
        String wkniHora = wkniE.wkniTodoElDia() ? "" : wkniE.wkniInicio().format(wkniHoras.wkniSALIDA) + " ";
        Label wkniText = new Label(wkniHora + wkniE.wkniTitulo());
        wkniText.setMaxWidth(Double.MAX_VALUE);
        wkniText.setMinWidth(0);
        wkniText.setTextOverrun(OverrunStyle.ELLIPSIS);
        wkniText.setStyle("-fx-text-fill:" + wkniE.wkni_tipo().wkniColor() + ";-fx-font-size:12;");
        HBox wkniChip = new HBox(4, wkniIconFactory.wkniImage(wkniE.wkniHecha() ? "check" : wkniE.wkni_tipo().wkniIcono(), 13), wkniText);
        wkniChip.setMaxWidth(Double.MAX_VALUE);
        wkniChip.setMinWidth(0);
        HBox.setHgrow(wkniText, Priority.ALWAYS);
        wkniChip.setAlignment(Pos.CENTER_LEFT);
        wkniChip.setStyle("-fx-background-color:" + wkniE.wkni_tipo().wkniFondo()
            + ";-fx-background-radius:6;-fx-padding:3 6;-fx-cursor:hand;");
        if (wkniE.wkniHecha()) wkniChip.setOpacity(0.65);
        Tooltip.install(wkniChip, new Tooltip(wkniE.wkni_tipo().wkniEtiqueta() + ": " + wkniE.wkniTitulo()
            + (wkniE.wkniDetalle() == null || wkniE.wkniDetalle().isBlank() ? "" : "\n" + wkniE.wkniDetalle()) + "\n" + wkniAyuda(wkniE)));
        wkniChip.setOnMouseClicked(wkniEv -> { wkniEv.consume(); wkniAccion(wkniE, wkniEv); });
        return wkniChip;
    }

    private static String wkniAyuda(wkniEntrada wkniE) {
        return switch (wkniE.wkniOrigen()) {
            case wkniTarea wkniT -> "Clic: marcar/desmarcar · Clic derecho: editar";
            case wkniHabito wkniH -> "Clic: marcar/desmarcar este día";
            case wkniContacto wkniK -> "Clic: ver datos del contacto";
            default -> "Clic: editar";
        };
    }

    private void wkniAccion(wkniEntrada wkniE, MouseEvent wkniEv) {
        boolean wkniSecundario = wkniEv.getButton() == MouseButton.SECONDARY;
        Object wkniO = wkniE.wkniOrigen();
        if (wkniO instanceof wkniEvento wkni_evento) {
            wkniEventViews.wkniForm(wkniStage, wkni_evento, wkniC, () -> wkniRefrescar(true));
        } else if (wkniO instanceof wkniTarea wkniT) {
            if (wkniSecundario) wkniTaskViews.wkniForm(wkniT, wkniC, () -> wkniRefrescar(true));
            else wkniC.wkniAlternarCompletada(wkniT).ifPresent(wkniR -> wkniToast.wkniMostrar(wkniView, wkniC.wkniMensajeProgreso(wkniR)));
        } else if (wkniO instanceof wkniHabito wkniH) {
            wkniMarcarHabito(wkniH, wkniE.wkni_fecha());
        } else if (wkniO instanceof wkniContacto wkniK) {
            StringBuilder wkniSb = new StringBuilder(wkniE.wkniDetalle().isBlank() ? "" : wkniE.wkniDetalle() + "\n");
            if (wkniK.wkniGetTelefono() != null && !wkniK.wkniGetTelefono().isBlank()) wkniSb.append("Teléfono: ").append(wkniK.wkniGetTelefono()).append('\n');
            if (wkniK.wkniGetEmail() != null && !wkniK.wkniGetEmail().isBlank()) wkniSb.append("Email: ").append(wkniK.wkniGetEmail()).append('\n');
            wkniSb.append("\nPuedes editar este contacto en CRM Personal.");
            Alert wkniA = new Alert(Alert.AlertType.INFORMATION, wkniSb.toString());
            wkniA.initOwner(wkniStage);
            wkniA.setTitle("Cumpleaños");
            wkniA.setHeaderText(wkniK.wkniGetNombre());
            wkniA.showAndWait();
        }
    }

    private void wkniMarcarHabito(wkniHabito wkniH, LocalDate wkni_fecha) {
        if (wkni_fecha.isAfter(LocalDate.now())) {
            wkniToast.wkniMostrar(wkniView, "Aún no puedes marcar un hábito de un día futuro");
            return;
        }
        boolean wkniYaHecho = wkni_datos.wkniHabitosCumplidos().getOrDefault(wkni_fecha.toString(), Set.of()).contains(wkniH.wkniGetId());
        if (wkniYaHecho) wkni_habitoDAO.wkniDesmarcar(wkniH.wkniGetId(), wkni_fecha.toString());
        else wkni_habitoDAO.wkniMarcar(wkniH.wkniGetId(), wkni_fecha.toString());
        wkniRefrescar(true);
        if (!wkniYaHecho) wkniToast.wkniMostrar(wkniView, wkniH.wkniGetNombre() + " · racha " + wkni_habitoDAO.wkniRacha(wkniH.wkniGetId()) + " días");
    }

    private static String wkniCap(String wkniS) {
        return wkniS == null || wkniS.isEmpty() ? wkniS : wkniS.substring(0, 1).toUpperCase(wkniES) + wkniS.substring(1);
    }
}


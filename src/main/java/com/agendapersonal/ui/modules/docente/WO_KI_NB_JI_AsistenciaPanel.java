package com.agendapersonal.ui.modules.docente;

import com.agendapersonal.domain.model.WO_KI_NB_JI_EstadoAsistencia;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstudianteClase;
import com.agendapersonal.service.WO_KI_NB_JI_ReportesDocente;
import com.agendapersonal.service.WO_KI_NB_JI_ResumenCurso;
import com.agendapersonal.service.WO_KI_NB_JI_RiesgoEstudianteService;
import com.agendapersonal.service.WO_KI_NB_JI_TablaReporte;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.DateCell;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Registro de asistencia visual.
 * <ul>
 *   <li><b>Registro del día</b>: una fila por estudiante con casillas Presente / Falta / Tardanza / Justificada.
 *       La fila se pinta del color del estado (verde, rojo, amarillo, azul) y se guarda en el acto en la BD
 *       local (sin internet). Atajos de teclado P / F / T / J / Supr con avance automático a la fila siguiente.</li>
 *   <li><b>Vista mensual</b>: matriz estudiantes × fechas; cada celda es clicable y cicla el estado.</li>
 * </ul>
 * Ambas vistas se exportan a Excel con colores y se imprimen tal como se ven.
 */
final class WO_KI_NB_JI_AsistenciaPanel {
    private static final DateTimeFormatter CORTA = DateTimeFormatter.ofPattern("dd/MM");

    private final WO_KI_NB_JI_CursoContexto ctx;
    private final VBox root = WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_panel();
    private final DatePicker dia = WO_KI_NB_JI_Ui.WO_KI_NB_JI_fechaCorta(new DatePicker(LocalDate.now()));
    private final TableView<WO_KI_NB_JI_EstudianteClase> tablaDia = new TableView<>();
    private final TableView<WO_KI_NB_JI_EstudianteClase> tablaMes = new TableView<>();
    private final StackPane contenido = new StackPane();
    private final FlowPane conteos = new FlowPane(8, 8);
    private final StackPane barraDia = new StackPane();
    private final Label guardado = new Label();
    private final Label tituloFecha = WO_KI_NB_JI_Ui.WO_KI_NB_JI_h3("");
    private final WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented vista = new WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented("Registro del día", "Vista mensual");
    private final HBox accionesDia;
    private WO_KI_NB_JI_ResumenCurso resumen;

    WO_KI_NB_JI_AsistenciaPanel(WO_KI_NB_JI_CursoContexto ctx) {
        this.ctx = ctx;
        this.resumen = ctx.WO_KI_NB_JI_resumen();

        // --- fecha: flechas, calendario y "Hoy" (no se registra el futuro)
        dia.setDayCellFactory(dp -> new DateCell() {
            @Override public void updateItem(LocalDate fecha, boolean vacio) {
                super.updateItem(fecha, vacio);
                if (fecha != null) setDisable(fecha.isAfter(LocalDate.now()));
            }
        });
        dia.setPrefWidth(140);
        dia.valueProperty().addListener((o, a, b) -> {
            if (b == null || b.isAfter(LocalDate.now())) { dia.setValue(a == null ? LocalDate.now() : a); return; }
            WO_KI_NB_JI_recargar(true);
        });
        Button antes = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("chevron-left", "Día anterior");
        antes.setOnAction(e -> dia.setValue(dia.getValue().minusDays(1)));
        Button despues = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("chevron-right", "Día siguiente");
        despues.setOnAction(e -> { if (dia.getValue().isBefore(LocalDate.now())) dia.setValue(dia.getValue().plusDays(1)); });
        Button hoy = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("calendar", "Hoy");
        hoy.setOnAction(e -> dia.setValue(LocalDate.now()));

        Button excel = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("download", "Exportar a Excel");
        excel.setTooltip(new Tooltip("Libro .xlsx con el registro mensual a colores, el día elegido y el semáforo de riesgo"));
        excel.setOnAction(e -> WO_KI_NB_JI_exportar());
        Button imprimir = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("print", "Imprimir");
        imprimir.setTooltip(new Tooltip("Imprime la tabla tal como se ve. Elige «Microsoft Print to PDF» para guardarla como PDF."));
        imprimir.setOnAction(e -> WO_KI_NB_JI_ImpresionTabla.WO_KI_NB_JI_imprimir(root, WO_KI_NB_JI_tablaActual()));

        HBox fecha = new HBox(6, antes, dia, despues, hoy);
        fecha.setAlignment(Pos.CENTER_LEFT);
        HBox barra1 = new HBox(12, fecha, vista.WO_KI_NB_JI_node(), WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), excel, imprimir);
        barra1.setAlignment(Pos.CENTER_LEFT);

        // --- acciones rápidas del día
        Button todos = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("check", "Marcar todos presentes");
        todos.setTooltip(new Tooltip("Marca «Presente» a quienes aún no tienen registro hoy; luego corrige las excepciones."));
        todos.setOnAction(e -> WO_KI_NB_JI_marcarSinRegistro(WO_KI_NB_JI_EstadoAsistencia.PRESENTE));
        Button restoFalta = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("close", "Sin registro → Falta");
        restoFalta.setOnAction(e -> WO_KI_NB_JI_marcarSinRegistro(WO_KI_NB_JI_EstadoAsistencia.FALTA));
        Button limpiar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_ghost("delete", "Limpiar día");
        limpiar.setOnAction(e -> {
            if (WO_KI_NB_JI_Ui.WO_KI_NB_JI_confirm(root, "¿Limpiar la asistencia?", "Se quitará lo registrado el " + WO_KI_NB_JI_Ui.WO_KI_NB_JI_formatDateShort(dia.getValue())
                + " en este curso.", "Limpiar")) {
                ctx.dao().WO_KI_NB_JI_quitarAsistenciaDia(ctx.clase().id(), dia.getValue());
                WO_KI_NB_JI_recargar(false); WO_KI_NB_JI_avisarGuardado(); ctx.WO_KI_NB_JI_cambio();
            }
        });
        Label atajos = new Label("Atajos: P presente · F falta · T tardanza · J justificada · Supr quitar · ↑↓ moverse");
        atajos.getStyleClass().add("atajos");
        accionesDia = new HBox(10, todos, restoFalta, limpiar, WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), atajos);
        accionesDia.setAlignment(Pos.CENTER_LEFT);

        guardado.getStyleClass().add("guardado-ok");
        HBox.setHgrow(barraDia, Priority.ALWAYS);
        barraDia.setMaxWidth(Double.MAX_VALUE);
        HBox estado = new HBox(14, tituloFecha, conteos, WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), guardado);
        estado.setAlignment(Pos.CENTER_LEFT);
        VBox resumenDia = new VBox(10, estado, barraDia);
        resumenDia.getStyleClass().add("card");
        resumenDia.setStyle("-fx-padding: 12 16 12 16;");
        resumenDia.setSpacing(8);

        WO_KI_NB_JI_construirTablaDia();
        WO_KI_NB_JI_construirTablaMes();
        contenido.getChildren().setAll(tablaDia);
        VBox.setVgrow(contenido, Priority.ALWAYS);
        vista.WO_KI_NB_JI_selectedProperty().addListener((o, a, b) -> {
            boolean mes = b.intValue() == 1;
            contenido.getChildren().setAll(mes ? tablaMes : tablaDia);
            accionesDia.setVisible(!mes); accionesDia.setManaged(!mes);
            WO_KI_NB_JI_recargar(true);
        });

        root.getChildren().addAll(barra1, resumenDia, accionesDia, contenido);
        WO_KI_NB_JI_recargar(true);
    }

    Node WO_KI_NB_JI_getView() { return root; }

    /** Vuelve a leer la lista y la asistencia (p. ej. tras agregar estudiantes en otra pestaña). */
    void WO_KI_NB_JI_refrescar() { WO_KI_NB_JI_recargar(true); }

    // ------------------------------------------------------------------ registro del día

    private void WO_KI_NB_JI_construirTablaDia() {
        tablaDia.getStyleClass().add("asist-tabla");
        tablaDia.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaDia.setPlaceholder(WO_KI_NB_JI_Ui.WO_KI_NB_JI_empty("contacts", "Aún no hay estudiantes",
            "Agrégalos en la pestaña «Estudiantes» o importa tu lista desde Excel."));
        tablaDia.setFixedCellSize(48);

        TableColumn<WO_KI_NB_JI_EstudianteClase, Number> num = new TableColumn<>("#");
        num.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(tablaDia.getItems().indexOf(d.getValue()) + 1));
        num.setPrefWidth(42); num.setMaxWidth(50); num.setSortable(false);
        tablaDia.getColumns().add(num);

        TableColumn<WO_KI_NB_JI_EstudianteClase, WO_KI_NB_JI_EstudianteClase> nombre = new TableColumn<>("Estudiante");
        nombre.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        nombre.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(WO_KI_NB_JI_EstudianteClase e, boolean empty) {
                super.updateItem(e, empty);
                setText(null);
                setGraphic(empty || e == null ? null : WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_nombre(e.nombre(), resumen.WO_KI_NB_JI_evaluacion(e.id())));
            }
        });
        nombre.setPrefWidth(300); nombre.setSortable(false);
        tablaDia.getColumns().add(nombre);

        for (WO_KI_NB_JI_EstadoAsistencia st : WO_KI_NB_JI_EstadoAsistencia.values()) tablaDia.getColumns().add(WO_KI_NB_JI_columnaCasilla(st));

        TableColumn<WO_KI_NB_JI_EstudianteClase, WO_KI_NB_JI_EstudianteClase> pct = new TableColumn<>("Asistencia acumulada");
        pct.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        pct.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(WO_KI_NB_JI_EstudianteClase e, boolean empty) {
                super.updateItem(e, empty);
                setText(null);
                if (empty || e == null) { setGraphic(null); return; }
                WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Evaluacion ev = resumen.WO_KI_NB_JI_evaluacion(e.id());
                double p = ev.porcentajeAsistencia();
                String tono = Double.isNaN(p) ? "slate" : p < WO_KI_NB_JI_RiesgoEstudianteService.ASISTENCIA_MINIMA ? "rose"
                    : p < WO_KI_NB_JI_RiesgoEstudianteService.ASISTENCIA_ATENCION ? "amber" : "green";
                Label l = new Label(WO_KI_NB_JI_ReportesDocente.WO_KI_NB_JI_pct(p));
                l.setMinWidth(44);
                l.setStyle("-fx-font-weight: bold;");
                StackPane bar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_bar(Double.isNaN(p) ? 0 : p / 100, tono, 6);
                HBox.setHgrow(bar, Priority.ALWAYS);
                Label faltas = WO_KI_NB_JI_Ui.WO_KI_NB_JI_caption(ev.faltas() + " F · " + ev.tardanzas() + " T");
                faltas.setMinWidth(64);
                HBox h = new HBox(8, l, bar, faltas);
                h.setAlignment(Pos.CENTER_LEFT);
                setGraphic(h);
            }
        });
        pct.setPrefWidth(260); pct.setSortable(false);
        tablaDia.getColumns().add(pct);

        // Fila pintada según el estado del día.
        tablaDia.setRowFactory(tv -> new TableRow<>() {
            @Override protected void updateItem(WO_KI_NB_JI_EstudianteClase e, boolean empty) {
                super.updateItem(e, empty);
                getStyleClass().removeIf(s -> s.startsWith("fila-"));
                if (!empty && e != null) {
                    WO_KI_NB_JI_EstadoAsistencia st = WO_KI_NB_JI_estadoDe(e);
                    if (st != null) getStyleClass().add("fila-" + WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_css(st));
                }
            }
        });

        tablaDia.setOnKeyPressed(k -> {
            WO_KI_NB_JI_EstudianteClase sel = tablaDia.getSelectionModel().getSelectedItem();
            if (sel == null) return;
            WO_KI_NB_JI_EstadoAsistencia st = switch (k.getCode()) {
                case P -> WO_KI_NB_JI_EstadoAsistencia.PRESENTE;
                case F, A -> WO_KI_NB_JI_EstadoAsistencia.FALTA;
                case T -> WO_KI_NB_JI_EstadoAsistencia.TARDANZA;
                case J -> WO_KI_NB_JI_EstadoAsistencia.JUSTIFICADA;
                default -> null;
            };
            boolean borrar = k.getCode() == KeyCode.DELETE || k.getCode() == KeyCode.BACK_SPACE;
            if (st == null && !borrar) return;
            int i = tablaDia.getSelectionModel().getSelectedIndex();
            WO_KI_NB_JI_marcar(sel, st);
            if (i + 1 < tablaDia.getItems().size()) {
                tablaDia.getSelectionModel().select(i + 1);
                tablaDia.scrollTo(Math.max(0, i - 3));
            }
            k.consume();
        });
    }

    private TableColumn<WO_KI_NB_JI_EstudianteClase, WO_KI_NB_JI_EstudianteClase> WO_KI_NB_JI_columnaCasilla(WO_KI_NB_JI_EstadoAsistencia st) {
        TableColumn<WO_KI_NB_JI_EstudianteClase, WO_KI_NB_JI_EstudianteClase> col = new TableColumn<>(st.WO_KI_NB_JI_etiqueta());
        col.setCellValueFactory(d -> new ReadOnlyObjectWrapper<>(d.getValue()));
        col.setCellFactory(c -> new TableCell<>() {
            private final CheckBox check = new CheckBox();
            {
                check.getStyleClass().addAll("asist-check", "c-" + WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_css(st));
                check.setFocusTraversable(false);
                check.setTooltip(new Tooltip(st.WO_KI_NB_JI_etiqueta() + " (tecla " + st.WO_KI_NB_JI_letra() + ")"));
                check.setOnAction(e -> {
                    WO_KI_NB_JI_EstudianteClase est = getItem();
                    if (est == null) return;
                    WO_KI_NB_JI_marcar(est, check.isSelected() ? st : null);
                });
                setAlignment(Pos.CENTER);
            }
            @Override protected void updateItem(WO_KI_NB_JI_EstudianteClase e, boolean empty) {
                super.updateItem(e, empty);
                setText(null);
                if (empty || e == null) { setGraphic(null); return; }
                check.setSelected(WO_KI_NB_JI_estadoDe(e) == st);
                setGraphic(check);
            }
        });
        col.setPrefWidth(104); col.setMinWidth(90); col.setSortable(false);
        col.setStyle("-fx-alignment: CENTER;");
        return col;
    }

    private WO_KI_NB_JI_EstadoAsistencia WO_KI_NB_JI_estadoDe(WO_KI_NB_JI_EstudianteClase e) {
        return WO_KI_NB_JI_EstadoAsistencia.WO_KI_NB_JI_de(resumen.WO_KI_NB_JI_historial(e.id()).get(dia.getValue()));
    }

    /** Guarda el estado (null = quitar) en la BD local al instante y repinta. */
    private void WO_KI_NB_JI_marcar(WO_KI_NB_JI_EstudianteClase e, WO_KI_NB_JI_EstadoAsistencia st) {
        boolean ok = st == null
            ? ctx.dao().WO_KI_NB_JI_quitarAsistencia(e.id(), dia.getValue())
            : ctx.dao().WO_KI_NB_JI_guardarAsistencia(ctx.clase().id(), e.id(), dia.getValue(), st.WO_KI_NB_JI_valor());
        if (!ok && st != null) { WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_error(root, "No se pudo guardar la asistencia de " + e.nombre() + "."
            + (ctx.dao().WO_KI_NB_JI_ultimoError() != null ? "\n\nMotivo: " + ctx.dao().WO_KI_NB_JI_ultimoError() : "")); return; }
        WO_KI_NB_JI_recargar(false);
        WO_KI_NB_JI_avisarGuardado();
        ctx.WO_KI_NB_JI_cambio();
    }

    private void WO_KI_NB_JI_marcarSinRegistro(WO_KI_NB_JI_EstadoAsistencia st) {
        int n = 0;
        for (WO_KI_NB_JI_EstudianteClase e : resumen.WO_KI_NB_JI_estudiantes()) {
            if (WO_KI_NB_JI_estadoDe(e) != null) continue;
            if (ctx.dao().WO_KI_NB_JI_guardarAsistencia(ctx.clase().id(), e.id(), dia.getValue(), st.WO_KI_NB_JI_valor())) n++;
        }
        WO_KI_NB_JI_recargar(false);
        if (n > 0) { WO_KI_NB_JI_avisarGuardado(); ctx.WO_KI_NB_JI_cambio(); }
        else WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_info(root, "Todos los estudiantes ya tienen registro este día.");
    }

    private void WO_KI_NB_JI_avisarGuardado() {
        guardado.setText("✓ Guardado en este equipo · " + LocalTime.now().withNano(0));
    }

    // ------------------------------------------------------------------ vista mensual

    private void WO_KI_NB_JI_construirTablaMes() {
        tablaMes.getStyleClass().add("asist-tabla");
        tablaMes.setPlaceholder(WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Aún no hay estudiantes en este curso."));
        tablaMes.setFixedCellSize(40);
    }

    private void WO_KI_NB_JI_columnasMes() {
        LocalDate d = dia.getValue();
        LocalDate desde = d.withDayOfMonth(1), hasta = d.withDayOfMonth(d.lengthOfMonth());
        List<LocalDate> fechas = new ArrayList<>();
        for (LocalDate f : resumen.WO_KI_NB_JI_fechas()) if (!f.isBefore(desde) && !f.isAfter(hasta)) fechas.add(f);
        if (!fechas.contains(d)) { fechas.add(d); fechas.sort(null); }

        tablaMes.getColumns().clear();
        TableColumn<WO_KI_NB_JI_EstudianteClase, WO_KI_NB_JI_EstudianteClase> nombre = new TableColumn<>("Estudiante");
        nombre.setCellValueFactory(x -> new ReadOnlyObjectWrapper<>(x.getValue()));
        nombre.setCellFactory(c -> new TableCell<>() {
            @Override protected void updateItem(WO_KI_NB_JI_EstudianteClase e, boolean empty) {
                super.updateItem(e, empty);
                setText(null);
                setGraphic(empty || e == null ? null : WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_nombre(e.nombre(), resumen.WO_KI_NB_JI_evaluacion(e.id())));
            }
        });
        nombre.setPrefWidth(270); nombre.setSortable(false);
        tablaMes.getColumns().add(nombre);

        for (LocalDate f : fechas) {
            String dow = f.getDayOfWeek().getDisplayName(TextStyle.SHORT, WO_KI_NB_JI_Ui.ES);
            TableColumn<WO_KI_NB_JI_EstudianteClase, WO_KI_NB_JI_EstudianteClase> col = new TableColumn<>(dow.substring(0, Math.min(2, dow.length())) + "\n" + f.format(CORTA));
            col.setCellValueFactory(x -> new ReadOnlyObjectWrapper<>(x.getValue()));
            col.setCellFactory(c -> new TableCell<>() {
                @Override protected void updateItem(WO_KI_NB_JI_EstudianteClase e, boolean empty) {
                    super.updateItem(e, empty);
                    setText(null);
                    if (empty || e == null) { setGraphic(null); return; }
                    WO_KI_NB_JI_EstadoAsistencia st = WO_KI_NB_JI_EstadoAsistencia.WO_KI_NB_JI_de(resumen.WO_KI_NB_JI_historial(e.id()).get(f));
                    Label l = new Label(st == null ? "·" : st.WO_KI_NB_JI_letra());
                    l.getStyleClass().add("celda-estado");
                    if (st != null) l.getStyleClass().add("e-" + WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_css(st));
                    Tooltip.install(l, new Tooltip(e.nombre() + " · " + WO_KI_NB_JI_Ui.WO_KI_NB_JI_formatDateShort(f) + "\n"
                        + (st == null ? "Sin registro" : st.WO_KI_NB_JI_etiqueta()) + "\nClic para cambiar"));
                    l.setOnMouseClicked(m -> {
                        WO_KI_NB_JI_EstadoAsistencia sig = WO_KI_NB_JI_EstadoAsistencia.WO_KI_NB_JI_siguiente(st);
                        if (sig == null) ctx.dao().WO_KI_NB_JI_quitarAsistencia(e.id(), f);
                        else ctx.dao().WO_KI_NB_JI_guardarAsistencia(ctx.clase().id(), e.id(), f, sig.WO_KI_NB_JI_valor());
                        WO_KI_NB_JI_recargar(false); WO_KI_NB_JI_avisarGuardado(); ctx.WO_KI_NB_JI_cambio();
                    });
                    setGraphic(l);
                    setAlignment(Pos.CENTER);
                }
            });
            col.setPrefWidth(56); col.setMinWidth(50); col.setSortable(false);
            if (f.equals(d)) col.setStyle("-fx-background-color: -accent-soft;");
            tablaMes.getColumns().add(col);
        }
        tablaMes.getColumns().add(WO_KI_NB_JI_total("P", e -> WO_KI_NB_JI_contar(e, WO_KI_NB_JI_EstadoAsistencia.PRESENTE, desde, hasta)));
        tablaMes.getColumns().add(WO_KI_NB_JI_total("F", e -> WO_KI_NB_JI_contar(e, WO_KI_NB_JI_EstadoAsistencia.FALTA, desde, hasta)));
        tablaMes.getColumns().add(WO_KI_NB_JI_total("T", e -> WO_KI_NB_JI_contar(e, WO_KI_NB_JI_EstadoAsistencia.TARDANZA, desde, hasta)));
        tablaMes.getColumns().add(WO_KI_NB_JI_total("J", e -> WO_KI_NB_JI_contar(e, WO_KI_NB_JI_EstadoAsistencia.JUSTIFICADA, desde, hasta)));
        tablaMes.getColumns().add(WO_KI_NB_JI_total("% curso", e -> WO_KI_NB_JI_ReportesDocente.WO_KI_NB_JI_pct(resumen.WO_KI_NB_JI_evaluacion(e.id()).porcentajeAsistencia())));
    }

    private TableColumn<WO_KI_NB_JI_EstudianteClase, String> WO_KI_NB_JI_total(String titulo, Function<WO_KI_NB_JI_EstudianteClase, String> valor) {
        TableColumn<WO_KI_NB_JI_EstudianteClase, String> c = new TableColumn<>(titulo);
        c.setCellValueFactory(x -> new ReadOnlyObjectWrapper<>(valor.apply(x.getValue())));
        c.setPrefWidth(titulo.length() > 2 ? 76 : 44); c.setSortable(false);
        c.setStyle("-fx-alignment: CENTER; -fx-font-weight: bold;");
        return c;
    }

    private String WO_KI_NB_JI_contar(WO_KI_NB_JI_EstudianteClase e, WO_KI_NB_JI_EstadoAsistencia st, LocalDate desde, LocalDate hasta) {
        return Long.toString(resumen.WO_KI_NB_JI_historial(e.id()).subMap(desde, true, hasta, true).values().stream()
            .filter(v -> WO_KI_NB_JI_EstadoAsistencia.WO_KI_NB_JI_de(v) == st).count());
    }

    // ------------------------------------------------------------------ recarga y resumen

    private void WO_KI_NB_JI_recargar(boolean reconstruirColumnas) {
        resumen = ctx.WO_KI_NB_JI_resumen();
        tablaDia.getItems().setAll(resumen.WO_KI_NB_JI_estudiantes());
        tablaMes.getItems().setAll(resumen.WO_KI_NB_JI_estudiantes());
        if (vista.WO_KI_NB_JI_selected() == 1 && (reconstruirColumnas || tablaMes.getColumns().isEmpty())) WO_KI_NB_JI_columnasMes();
        tablaDia.refresh();
        tablaMes.refresh();
        WO_KI_NB_JI_pintarResumen();
    }

    private void WO_KI_NB_JI_pintarResumen() {
        LocalDate d = dia.getValue();
        boolean esHoy = d.equals(LocalDate.now());
        tituloFecha.setText((esHoy ? "Hoy, " : "") + WO_KI_NB_JI_Ui.WO_KI_NB_JI_formatDate(d));
        int[] n = new int[4];
        int sin = 0;
        for (WO_KI_NB_JI_EstudianteClase e : resumen.WO_KI_NB_JI_estudiantes()) {
            WO_KI_NB_JI_EstadoAsistencia st = WO_KI_NB_JI_estadoDe(e);
            if (st == null) sin++; else n[st.ordinal()]++;
        }
        conteos.getChildren().setAll(
            WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_conteo(WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_color(WO_KI_NB_JI_EstadoAsistencia.PRESENTE), n[0] + (n[0] == 1 ? " presente" : " presentes"), "green"),
            WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_conteo(WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_color(WO_KI_NB_JI_EstadoAsistencia.FALTA), n[1] + (n[1] == 1 ? " falta" : " faltas"), "rose"),
            WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_conteo(WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_color(WO_KI_NB_JI_EstadoAsistencia.TARDANZA), n[2] + (n[2] == 1 ? " tardanza" : " tardanzas"), "amber"),
            WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_conteo(WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_color(WO_KI_NB_JI_EstadoAsistencia.JUSTIFICADA), n[3] + (n[3] == 1 ? " justificada" : " justificadas"), "blue"));
        if (sin > 0) conteos.getChildren().add(WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_conteo("-c-slate", sin + " sin registro", "slate"));
        barraDia.getChildren().setAll(WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_barraApilada(new double[]{n[0], n[2], n[3], n[1], sin},
            new String[]{"-c-green", "#E8B43A", "-c-blue", "-danger", "-surface3"}, 10));
        if (guardado.getText().isEmpty()) guardado.setText("Se guarda automáticamente al marcar · funciona sin internet");
    }

    // ------------------------------------------------------------------ exportar e imprimir

    private WO_KI_NB_JI_TablaReporte WO_KI_NB_JI_tablaActual() {
        LocalDate d = dia.getValue();
        return vista.WO_KI_NB_JI_selected() == 1
            ? WO_KI_NB_JI_ReportesDocente.WO_KI_NB_JI_asistenciaMatriz(resumen, d.withDayOfMonth(1), d.withDayOfMonth(d.lengthOfMonth()), List.of(d))
            : WO_KI_NB_JI_ReportesDocente.WO_KI_NB_JI_asistenciaDia(resumen, d);
    }

    private void WO_KI_NB_JI_exportar() {
        LocalDate d = dia.getValue();
        List<WO_KI_NB_JI_TablaReporte> hojas = List.of(
            WO_KI_NB_JI_ReportesDocente.WO_KI_NB_JI_asistenciaMatriz(resumen, d.withDayOfMonth(1), d.withDayOfMonth(d.lengthOfMonth()), List.of(d)),
            WO_KI_NB_JI_ReportesDocente.WO_KI_NB_JI_asistenciaDia(resumen, d),
            WO_KI_NB_JI_ReportesDocente.WO_KI_NB_JI_riesgo(resumen));
        WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_exportarExcel(root, "Asistencia " + ctx.clase() + " " + d.getMonth().getDisplayName(TextStyle.FULL, WO_KI_NB_JI_Ui.ES)
            + " " + d.getYear(), hojas);
    }
}

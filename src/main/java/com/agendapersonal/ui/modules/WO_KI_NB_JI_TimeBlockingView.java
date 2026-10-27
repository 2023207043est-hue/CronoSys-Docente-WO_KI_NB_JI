package com.agendapersonal.ui.modules;

import com.agendapersonal.controller.WO_KI_NB_JI_MainController;
import com.agendapersonal.domain.model.WO_KI_NB_JI_BloqueTiempo;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Nota;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_BloqueDAO;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_NotaDAO;
import com.agendapersonal.model.WO_KI_NB_JI_Tarea;
import com.agendapersonal.service.WO_KI_NB_JI_ListaPendientes;
import com.agendapersonal.service.WO_KI_NB_JI_MetodoEnfoque;
import com.agendapersonal.ui.WO_KI_NB_JI_Anim;
import com.agendapersonal.ui.WO_KI_NB_JI_IconFactory;
import com.agendapersonal.ui.WO_KI_NB_JI_Notifier;
import com.agendapersonal.ui.WO_KI_NB_JI_Switch;
import com.agendapersonal.ui.WO_KI_NB_JI_ThemeService;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
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
public class WO_KI_NB_JI_TimeBlockingView {
    private static final Map<String, String> TONO = Map.of(
        "Enfoque", "orange", "Reunión", "blue", "Estudio", "violet", "Salud", "green", "Personal", "rose");

    private final WO_KI_NB_JI_MainController c;
    private final WO_KI_NB_JI_BloqueDAO dao = new WO_KI_NB_JI_BloqueDAO();
    private final WO_KI_NB_JI_NotaDAO notas = new WO_KI_NB_JI_NotaDAO();
    private final VBox view;
    private final DatePicker fecha = new DatePicker(LocalDate.now());
    private final VBox lista = new VBox(0);
    private final Label tituloDia = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("", "h3");

    /** Métodos propios guardados en el perfil. */
    private List<WO_KI_NB_JI_MetodoEnfoque> propios = new ArrayList<>();
    private ComboBox<WO_KI_NB_JI_MetodoEnfoque> metodos;
    private Label descripcion, uso, resumen;
    private TextField enfocar;

    public WO_KI_NB_JI_TimeBlockingView(WO_KI_NB_JI_MainController c) {
        this.c = c;
        this.propios = new ArrayList<>(WO_KI_NB_JI_MetodoEnfoque.WO_KI_NB_JI_desdeMapas(WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_get().WO_KI_NB_JI_settings().WO_KI_NB_JI_value("timeBlocking.presets")));

        VBox page = WO_KI_NB_JI_Ui.WO_KI_NB_JI_page();
        Button prev = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("chevron-left", "Día anterior");
        Button next = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("chevron-right", "Día siguiente");
        Button hoy = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Hoy");
        prev.setOnAction(e -> fecha.setValue(fecha.getValue().minusDays(1)));
        next.setOnAction(e -> fecha.setValue(fecha.getValue().plusDays(1)));
        hoy.setOnAction(e -> fecha.setValue(LocalDate.now()));
        WO_KI_NB_JI_Ui.WO_KI_NB_JI_fechaCorta(fecha);
        fecha.setOnAction(e -> WO_KI_NB_JI_fill());
        fecha.setPrefWidth(170);
        HBox nav = new HBox(6, prev, fecha, next, hoy);
        nav.setAlignment(Pos.CENTER_RIGHT);
        page.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pageHeader("clock", "Time blocking",
            "Reserva bloques de enfoque por día y arranca sesiones con reloj, progreso y lista de pendientes.", nav));

        page.getChildren().addAll(WO_KI_NB_JI_cardSesion(), WO_KI_NB_JI_cardMetodos(), WO_KI_NB_JI_cardFondo(), WO_KI_NB_JI_cardNuevoBloque(), WO_KI_NB_JI_cardLista());

        view = page;
        WO_KI_NB_JI_fill();
    }

    // ------------------------------------------------------------------ sesión de enfoque

    private VBox WO_KI_NB_JI_cardSesion() {
        metodos = new ComboBox<>();
        metodos.getItems().addAll(WO_KI_NB_JI_metodosBase());
        metodos.getItems().addAll(propios);
        metodos.setValue(metodos.getItems().get(0));
        metodos.setCellFactory(WO_KI_NB_JI_fabricaCelda());
        metodos.setButtonCell(WO_KI_NB_JI_fabricaCelda().call(null));
        metodos.setMaxWidth(Double.MAX_VALUE);

        descripcion = WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("");
        uso = WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("");
        resumen = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("", "caption");
        resumen.getStyleClass().add("caption");
        metodos.setOnAction(e -> WO_KI_NB_JI_verMetodo());
        WO_KI_NB_JI_verMetodo();

        enfocar = new TextField();
        enfocar.setPromptText("¿En qué te vas a enfocar? (opcional)");

        // Importancia de la sesión
        ComboBox<WO_KI_NB_JI_PantallaEnfoque.WO_KI_NB_JI_Importancia> imp = new ComboBox<>();
        imp.getItems().setAll(WO_KI_NB_JI_PantallaEnfoque.WO_KI_NB_JI_Importancia.values());
        imp.setValue(WO_KI_NB_JI_PantallaEnfoque.WO_KI_NB_JI_Importancia.WO_KI_NB_JI_desde(WO_KI_NB_JI_st().WO_KI_NB_JI_str("timeBlocking.importance")));
        imp.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(WO_KI_NB_JI_PantallaEnfoque.WO_KI_NB_JI_Importancia i, boolean empty) {
                super.updateItem(i, empty);
                setText(empty || i == null ? null : i.toString());
                setTooltip(empty || i == null ? null : new javafx.scene.control.Tooltip(i.WO_KI_NB_JI_ayuda()));
            }
        });
        imp.setOnAction(e -> {
            if (imp.getValue() == null) return;
            WO_KI_NB_JI_st().WO_KI_NB_JI_set("timeBlocking.importance", imp.getValue().name());
        });

        // De dónde salen los pendientes
        String[] fuentes = {"ambas", "tareas", "notas", "ninguna"};
        WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented origen = new WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented("Tareas y notas", "Solo tareas", "Solo notas", "Ninguna");
        int idx = WO_KI_NB_JI_indexOf(fuentes, WO_KI_NB_JI_st().WO_KI_NB_JI_str("timeBlocking.todoSource"));
        origen.WO_KI_NB_JI_select(idx < 0 ? 0 : idx);
        origen.WO_KI_NB_JI_selectedProperty().addListener((o, a, v) -> {
            WO_KI_NB_JI_st().WO_KI_NB_JI_set("timeBlocking.todoSource", fuentes[v.intValue()]);
            WO_KI_NB_JI_resumenPendientes();
        });

        WO_KI_NB_JI_Switch verPendientes = new WO_KI_NB_JI_Switch("Mostrar la lista de pendientes en la pantalla",
            "Puedes marcar las casillas mientras trabajas: se guarda en tus tareas o en tus notas.",
            WO_KI_NB_JI_st().WO_KI_NB_JI_bool("timeBlocking.showTasks"));
        verPendientes.WO_KI_NB_JI_selectedProperty().addListener((o, a, v) -> WO_KI_NB_JI_st().WO_KI_NB_JI_set("timeBlocking.showTasks", v));

        WO_KI_NB_JI_Switch pausar = new WO_KI_NB_JI_Switch("Pausar el contador si salgo de la pantalla",
            "Se aplica siempre con la importancia Alta o Estricta.", WO_KI_NB_JI_st().WO_KI_NB_JI_bool("timeBlocking.pauseOnLeave"));
        pausar.WO_KI_NB_JI_selectedProperty().addListener((o, a, v) -> WO_KI_NB_JI_st().WO_KI_NB_JI_set("timeBlocking.pauseOnLeave", v));

        Button iniciar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("clock", "Iniciar enfoque");
        iniciar.setOnAction(e -> WO_KI_NB_JI_iniciar());

        HBox botonera = new HBox(10, iniciar, WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("También puedes cambiar el fondo y los ajustes dentro de la pantalla."));
        botonera.setAlignment(Pos.CENTER_LEFT);

        VBox card = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        card.getChildren().addAll(
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_h3("Sesión de enfoque"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Elige un método, la importancia y los pendientes. Se abre una pantalla con reloj, "
                + "barra de progreso, número de sesión y botones de control."),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Método", metodos), descripcion, uso, resumen,
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Enfoque", enfocar),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Importancia de la sesión", imp),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Pendientes de la sesión", origen.WO_KI_NB_JI_node()),
            verPendientes, pausar,
            botonera);
        return card;
    }

    private javafx.util.Callback<ListView<WO_KI_NB_JI_MetodoEnfoque>, ListCell<WO_KI_NB_JI_MetodoEnfoque>> WO_KI_NB_JI_fabricaCelda() {
        return lv -> new ListCell<>() {
            @Override protected void updateItem(WO_KI_NB_JI_MetodoEnfoque m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m == null) { setText(null); return; }
                setText(m.nombre() + (m.propio() ? "  · propio" : ""));
                setTooltip(new javafx.scene.control.Tooltip(m.descripcion()));
            }
        };
    }

    private void WO_KI_NB_JI_verMetodo() {
        WO_KI_NB_JI_MetodoEnfoque m = metodos.getValue();
        if (m == null) return;
        descripcion.setText(m.descripcion());
        uso.setText("Uso común: " + m.usoComun());
        resumen.setText(m.WO_KI_NB_JI_resumen() + (m.WO_KI_NB_JI_tieneRangos() ? "  ·  la duración concreta se sortea al empezar" : ""));
        WO_KI_NB_JI_resumenPendientes();
    }

    /** Añade al resumen cuántos pendientes habrá en la pantalla de bloqueo. */
    private void WO_KI_NB_JI_resumenPendientes() {
        List<WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_Item> items = WO_KI_NB_JI_pendientes();
        if (items.isEmpty()) return;
        long hechos = items.stream().filter(WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_Item::hecho).count();
        resumen.setText(resumen.getText() + "  ·  " + hechos + " de " + items.size() + " pendientes listos");
    }

    /** Los pendientes que se mostrarán en la pantalla de bloqueo. */
    private List<WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_Item> WO_KI_NB_JI_pendientes() {
        List<WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_Item> lista = new ArrayList<>();
        String fuente = WO_KI_NB_JI_st().WO_KI_NB_JI_str("timeBlocking.todoSource");
        if ("ninguna".equals(fuente)) return lista;
        if ("tareas".equals(fuente) || "ambas".equals(fuente)) {
            LocalDate hoy = LocalDate.now();
            for (WO_KI_NB_JI_Tarea t : c.WO_KI_NB_JI_getTareas()) {
                if (t.WO_KI_NB_JI_getEstadoTipo() == null || t.WO_KI_NB_JI_getEstadoTipo().WO_KI_NB_JI_esCompletada()) continue;
                if (t.WO_KI_NB_JI_getFecha() != null && t.WO_KI_NB_JI_getFecha().isAfter(hoy)) continue;   // solo lo de hoy y lo vencido
                lista.add(new WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_Item(t.WO_KI_NB_JI_getId(), -1, t.WO_KI_NB_JI_getTitulo(), false, "tarea"));
            }
        }
        if ("notas".equals(fuente) || "ambas".equals(fuente)) {
            lista.addAll(WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_desdeNotas(notas.WO_KI_NB_JI_listar(null)));
        }
        return lista;
    }

    private void WO_KI_NB_JI_iniciar() {
        WO_KI_NB_JI_MetodoEnfoque m = metodos.getValue();
        if (m == null) return;
        List<WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_Item> items = WO_KI_NB_JI_pendientes();
        WO_KI_NB_JI_PantallaEnfoque.WO_KI_NB_JI_Importancia imp = WO_KI_NB_JI_PantallaEnfoque.WO_KI_NB_JI_Importancia.WO_KI_NB_JI_desde(WO_KI_NB_JI_st().WO_KI_NB_JI_str("timeBlocking.importance"));
        WO_KI_NB_JI_PantallaEnfoque.WO_KI_NB_JI_abrir(m, enfocar.getText(), imp, items, this::WO_KI_NB_JI_alternarPendiente);
    }

    /** Guarda el cambio de un pendiente (tarea completada o casilla de una nota marcada). */
    private void WO_KI_NB_JI_alternarPendiente(WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_Item item, boolean hecho) {
        if ("tarea".equals(item.origen())) {
            for (WO_KI_NB_JI_Tarea t : c.WO_KI_NB_JI_getTareas())
                if (t.WO_KI_NB_JI_getId() == item.notaId()) { c.WO_KI_NB_JI_alternarCompletada(t); return; }
            return;
        }
        for (WO_KI_NB_JI_Nota n : notas.WO_KI_NB_JI_listar(null)) {
            if (n.WO_KI_NB_JI_getId() != item.notaId()) continue;
            String contenido = WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_alternar(n, item, hecho);
            if (contenido.equals(n.WO_KI_NB_JI_getContenido())) return;
            n.WO_KI_NB_JI_setContenido(contenido);
            if (notas.WO_KI_NB_JI_actualizar(n)) WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success(hecho ? "Pendiente hecho" : "Pendiente devuelto", item.texto());
            return;
        }
    }

    // ------------------------------------------------------------------ métodos

    private List<WO_KI_NB_JI_MetodoEnfoque> WO_KI_NB_JI_metodosBase() {
        List<WO_KI_NB_JI_MetodoEnfoque> base = new ArrayList<>(WO_KI_NB_JI_MetodoEnfoque.PREDEFINIDOS);
        if (com.agendapersonal.util.WO_KI_NB_JI_Perfil.WO_KI_NB_JI_esDocente()) base.addAll(WO_KI_NB_JI_MetodoEnfoque.DOCENTES);
        return base;
    }

    private VBox WO_KI_NB_JI_cardMetodos() {
        VBox listaMetodos = new VBox(8);

        for (WO_KI_NB_JI_MetodoEnfoque m : WO_KI_NB_JI_metodosBase()) {
            listaMetodos.getChildren().add(WO_KI_NB_JI_filaMetodo(m, false));
        }
        for (WO_KI_NB_JI_MetodoEnfoque m : propios) {
            listaMetodos.getChildren().add(WO_KI_NB_JI_filaMetodo(m, true));
        }

        // Crear el tuyo (con rangos)
        TextField nom = new TextField();
        nom.setPromptText("Nombre del método");
        TextField trabajo = new TextField("30"), trabajoMax = new TextField("30");
        TextField descanso = new TextField("5"), descansoMax = new TextField("5");
        TextField sesiones = new TextField("4"), largo = new TextField("15"), largoMax = new TextField("15");
        for (TextField t : List.of(trabajo, trabajoMax, descanso, descansoMax, sesiones, largo, largoMax)) {
            t.setPrefColumnCount(4);
        }
        Button crear = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("add", "Guardar método propio");
        crear.setOnAction(e -> WO_KI_NB_JI_crearMetodo(nom, trabajo, trabajoMax, descanso, descansoMax, sesiones, largo, largoMax));

        VBox grupoTrabajo = new VBox(4, WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("Trabajo (min)", "caption"), new HBox(6, trabajo, WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("a", "caption"), trabajoMax));
        VBox grupoDescanso = new VBox(4, WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("Descanso (min)", "caption"), new HBox(6, descanso, WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("a", "caption"), descansoMax));
        VBox grupoSesiones = new VBox(4, WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("Sesiones por ciclo", "caption"), sesiones);
        VBox grupoLargo = new VBox(4, WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("Descanso largo (min)", "caption"), new HBox(6, largo, WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("a", "caption"), largoMax));

        HBox formulario = new HBox(12, WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Nombre", nom), grupoTrabajo, grupoDescanso, grupoSesiones, grupoLargo, crear);
        formulario.setAlignment(Pos.BOTTOM_LEFT);

        VBox card = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        card.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h3("Métodos de bloqueo"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Cada método tiene su descripción y su uso común. Los tiempos pueden ser un valor fijo "
                + "o un rango (por ejemplo 60 a 90 minutos): al empezar la sesión se sortea uno dentro del rango."),
            listaMetodos, WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Crea el tuyo", formulario));
        return card;
    }

    private Node WO_KI_NB_JI_filaMetodo(WO_KI_NB_JI_MetodoEnfoque m, boolean propio) {
        Label n = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(m.nombre(), "row-title");
        Label d = WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted(m.descripcion());
        d.setMaxWidth(420);
        Label u = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("Uso común: " + m.usoComun(), "caption");
        VBox textos = new VBox(3, n, d, u);
        HBox.setHgrow(textos, Priority.ALWAYS);

        HBox acciones = new HBox(8);
        Button usar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Usar");
        usar.setOnAction(e -> { metodos.setValue(m); WO_KI_NB_JI_verMetodo(); });
        acciones.getChildren().add(usar);
        if (propio) {
            Button dup = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("copy", "Duplicar este método");
            dup.setOnAction(e -> WO_KI_NB_JI_duplicar(m));
            Button del = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("delete", "Eliminar este método");
            del.setOnAction(e -> WO_KI_NB_JI_eliminarMetodo(m));
            acciones.getChildren().addAll(dup, del);
        }
        HBox fila = new HBox(14, textos, acciones);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.getStyleClass().addAll("list-row", "with-divider");
        return fila;
    }

    private void WO_KI_NB_JI_crearMetodo(TextField nom, TextField trabajo, TextField trabajoMax, TextField descanso,
                            TextField descansoMax, TextField sesiones, TextField largo, TextField largoMax) {
        try {
            int t1 = Integer.parseInt(trabajo.getText().trim());
            int t2 = Integer.parseInt(trabajoMax.getText().trim());
            int d1 = Integer.parseInt(descanso.getText().trim());
            int d2 = Integer.parseInt(descansoMax.getText().trim());
            int s = Integer.parseInt(sesiones.getText().trim());
            int l1 = Integer.parseInt(largo.getText().trim());
            int l2 = Integer.parseInt(largoMax.getText().trim());
            String nombre = nom.getText().trim();
            if (nombre.isEmpty()) { WO_KI_NB_JI_Anim.WO_KI_NB_JI_shake(nom); WO_KI_NB_JI_Notifier.WO_KI_NB_JI_warning("Falta el nombre", "Ponle un nombre a tu método."); return; }
            WO_KI_NB_JI_MetodoEnfoque m = WO_KI_NB_JI_MetodoEnfoque.WO_KI_NB_JI_personalizado(nombre, t1, t2, d1, d2, s, l1, l2);
            propios.add(m);
            WO_KI_NB_JI_guardarPropios();
            metodos.getItems().add(m);
            metodos.setValue(m);
            WO_KI_NB_JI_verMetodo();
            nom.clear();
            WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success("Método guardado", m.nombre() + " · " + m.WO_KI_NB_JI_resumen());
        } catch (NumberFormatException ex) {
            WO_KI_NB_JI_Notifier.WO_KI_NB_JI_warning("Revisa los números", "Usa minutos enteros, por ejemplo 40 y 8.");
        }
    }

    private void WO_KI_NB_JI_duplicar(WO_KI_NB_JI_MetodoEnfoque m) {
        WO_KI_NB_JI_MetodoEnfoque copia = m.WO_KI_NB_JI_comoPropio(m.nombre() + " (copia)");
        propios.add(copia);
        WO_KI_NB_JI_guardarPropios();
        metodos.getItems().add(copia);
        WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success("Método duplicado", copia.nombre());
    }

    private void WO_KI_NB_JI_eliminarMetodo(WO_KI_NB_JI_MetodoEnfoque m) {
        if (!WO_KI_NB_JI_Ui.WO_KI_NB_JI_confirm(view, "¿Eliminar el método «" + m.nombre() + "»?", "Solo se borra el método, no las sesiones que ya hiciste.", "Eliminar")) return;
        propios.removeIf(x -> x.id().equals(m.id()));
        WO_KI_NB_JI_guardarPropios();
        metodos.getItems().remove(m);
        if (metodos.getValue() == null && !metodos.getItems().isEmpty()) metodos.setValue(metodos.getItems().get(0));
        WO_KI_NB_JI_verMetodo();
        WO_KI_NB_JI_Notifier.WO_KI_NB_JI_deleted("Método eliminado", m.nombre());
    }

    private void WO_KI_NB_JI_guardarPropios() {
        List<Object> mapas = new ArrayList<>();
        for (WO_KI_NB_JI_MetodoEnfoque m : propios) mapas.add(m.WO_KI_NB_JI_aMapa());
        WO_KI_NB_JI_st().WO_KI_NB_JI_set("timeBlocking.presets", mapas);
    }

    // ------------------------------------------------------------------ fondo

    private VBox WO_KI_NB_JI_cardFondo() {
        String[] modos = {"degradado", "archivo", "color"};
        WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented fondo = new WO_KI_NB_JI_Ui.WO_KI_NB_JI_Segmented("Degradado del tema", "Imagen o video", "Color");
        fondo.WO_KI_NB_JI_select(Math.max(0, WO_KI_NB_JI_indexOf(modos, WO_KI_NB_JI_st().WO_KI_NB_JI_str("timeBlocking.background"))));
        fondo.WO_KI_NB_JI_selectedProperty().addListener((o, a, v) -> WO_KI_NB_JI_st().WO_KI_NB_JI_set("timeBlocking.background", modos[v.intValue()]));

        Label archivo = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(WO_KI_NB_JI_nombreFondo(), "caption");
        Button agregarFondo = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("grid", "Agregar fondo…");
        agregarFondo.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Fondo de la pantalla de enfoque");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Imágenes y videos",
                "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp", "*.webp", "*.mp4", "*.m4v", "*.webm", "*.ogv"));
            File f = fc.showOpenDialog(view.getScene() == null ? null : view.getScene().getWindow());
            if (f == null) return;
            WO_KI_NB_JI_st().WO_KI_NB_JI_set("timeBlocking.backgroundFile", f.getAbsolutePath());
            WO_KI_NB_JI_st().WO_KI_NB_JI_set("timeBlocking.background", "archivo");
            fondo.WO_KI_NB_JI_select(1);
            archivo.setText(f.getName());
        });
        Button quitar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Quitar");
        quitar.setOnAction(e -> {
            WO_KI_NB_JI_st().WO_KI_NB_JI_set("timeBlocking.backgroundFile", null);
            WO_KI_NB_JI_st().WO_KI_NB_JI_set("timeBlocking.background", "degradado");
            fondo.WO_KI_NB_JI_select(0);
            archivo.setText("Sin archivo");
        });

        ColorPicker colorFondo = new ColorPicker(javafx.scene.paint.Color.web(
            com.agendapersonal.settings.WO_KI_NB_JI_Colors.WO_KI_NB_JI_normalize(WO_KI_NB_JI_st().WO_KI_NB_JI_str("timeBlocking.backgroundColor")) == null
                ? "#1B1E25" : WO_KI_NB_JI_st().WO_KI_NB_JI_str("timeBlocking.backgroundColor")));
        colorFondo.setOnAction(e -> {
            var c1 = colorFondo.getValue();
            WO_KI_NB_JI_st().WO_KI_NB_JI_set("timeBlocking.backgroundColor", com.agendapersonal.settings.WO_KI_NB_JI_Colors.WO_KI_NB_JI_hex(
                (int) Math.round(c1.getRed() * 255), (int) Math.round(c1.getGreen() * 255), (int) Math.round(c1.getBlue() * 255)));
            WO_KI_NB_JI_st().WO_KI_NB_JI_set("timeBlocking.background", "color");
            fondo.WO_KI_NB_JI_select(2);
        });

        WO_KI_NB_JI_Switch repetir = new WO_KI_NB_JI_Switch("Repetir el video en bucle", "Si el fondo es un video, se repite mientras dure la sesión.",
            WO_KI_NB_JI_st().WO_KI_NB_JI_bool("timeBlocking.backgroundLoop"));
        repetir.WO_KI_NB_JI_selectedProperty().addListener((o, a, v) -> WO_KI_NB_JI_st().WO_KI_NB_JI_set("timeBlocking.backgroundLoop", v));

        // Audio local de la sesión (sin internet: un archivo de tu equipo).
        Label audio = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(WO_KI_NB_JI_st().WO_KI_NB_JI_str("timeBlocking.audioFile").isBlank()
            ? "Sin audio" : new File(WO_KI_NB_JI_st().WO_KI_NB_JI_str("timeBlocking.audioFile")).getName(), "caption");
        Button agregarAudio = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("music", "Elegir audio…");
        agregarAudio.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Audio de la sesión de enfoque");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Audio", "*.mp3", "*.wav", "*.ogg",
                "*.m4a", "*.aac", "*.flac", "*.opus"));
            File f = fc.showOpenDialog(view.getScene() == null ? null : view.getScene().getWindow());
            if (f == null) return;
            WO_KI_NB_JI_st().WO_KI_NB_JI_set("timeBlocking.audioFile", f.getAbsolutePath());
            audio.setText(f.getName());
        });
        Button quitarAudio = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Quitar");
        quitarAudio.setOnAction(e -> {
            WO_KI_NB_JI_st().WO_KI_NB_JI_set("timeBlocking.audioFile", null);
            audio.setText("Sin audio");
        });
        HBox filaAudio = new HBox(10, agregarAudio, audio, quitarAudio);
        filaAudio.setAlignment(Pos.CENTER_LEFT);

        HBox filaFondo = new HBox(10, agregarFondo, archivo, quitar, colorFondo);
        filaFondo.setAlignment(Pos.CENTER_LEFT);

        VBox card = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        card.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h3("Fondo y audio de la pantalla de enfoque"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Puedes cambiarlo también desde la propia pantalla, con los botones «Agregar fondo» y «Ajustes»."),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Tipo de fondo", fondo.WO_KI_NB_JI_node()),
            filaFondo, repetir,
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Audio de la sesión", filaAudio),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Un archivo de audio de tu equipo: se repite mientras trabajas y calla en los descansos."));
        return card;
    }

    private String WO_KI_NB_JI_nombreFondo() {
        String ruta = WO_KI_NB_JI_st().WO_KI_NB_JI_str("timeBlocking.backgroundFile");
        return ruta.isBlank() ? "Sin archivo" : new File(ruta).getName();
    }

    // ------------------------------------------------------------------ bloques del día

    private VBox WO_KI_NB_JI_cardNuevoBloque() {
        TextField titulo = new TextField();
        titulo.setPromptText("Ej. Trabajo profundo en la tesis");
        TextField hi = new TextField("09:00");
        TextField hf = new TextField("11:00");
        ComboBox<String> cat = new ComboBox<>();
        cat.getItems().addAll("Enfoque", "Reunión", "Estudio", "Salud", "Personal");
        cat.setValue("Enfoque");
        Button add = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("add", "Bloquear");
        Runnable agregar = () -> {
            if (titulo.getText().isBlank()) { WO_KI_NB_JI_Anim.WO_KI_NB_JI_shake(titulo); WO_KI_NB_JI_Notifier.WO_KI_NB_JI_warning("Falta el título", "Ponle un nombre al bloque."); return; }
            LocalTime a, b;
            try { a = LocalTime.parse(hi.getText().trim()); b = LocalTime.parse(hf.getText().trim()); }
            catch (DateTimeParseException ex) { WO_KI_NB_JI_Anim.WO_KI_NB_JI_shake(hi); WO_KI_NB_JI_Notifier.WO_KI_NB_JI_warning("Hora inválida", "Usa el formato HH:mm, por ejemplo 09:30."); return; }
            if (!b.isAfter(a)) { WO_KI_NB_JI_Anim.WO_KI_NB_JI_shake(hf); WO_KI_NB_JI_Notifier.WO_KI_NB_JI_warning("Revisa las horas", "La hora de fin debe ser posterior al inicio."); return; }
            if (fecha.getValue() == null) fecha.setValue(LocalDate.now());
            String f = fecha.getValue().toString();
            String ini = a.toString(), fin = b.toString();
            if (WO_KI_NB_JI_choca(f, ini, fin)) { WO_KI_NB_JI_Notifier.WO_KI_NB_JI_warning("Ese horario ya está ocupado", "Choca con otro bloque o con un evento de tu agenda."); return; }
            if (dao.WO_KI_NB_JI_insertar(new WO_KI_NB_JI_BloqueTiempo(0, titulo.getText().trim(), f, ini, fin, cat.getValue(), null))) {
                WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success("Bloque reservado", titulo.getText().trim() + " · " + ini + "–" + fin);
                titulo.clear();
                WO_KI_NB_JI_fill();
            } else WO_KI_NB_JI_Notifier.WO_KI_NB_JI_error("No se pudo guardar el bloque", null);
        };
        add.setOnAction(e -> agregar.run());
        titulo.setOnAction(e -> agregar.run());
        VBox fT = WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Bloque", titulo);
        VBox fI = WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Inicio", hi);
        VBox fF = WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Fin", hf);
        VBox fC = WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Categoría", cat);
        fT.setPrefWidth(320); fI.setPrefWidth(110); fF.setPrefWidth(110); fC.setPrefWidth(170);
        HBox fila = new HBox(12, fT, fI, fF, fC, add);
        fila.setAlignment(Pos.BOTTOM_LEFT);
        VBox form = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        form.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h3("Nuevo bloque"), fila);
        return form;
    }

    private boolean WO_KI_NB_JI_choca(String fecha, String hi, String hf) {
        for (var b : dao.WO_KI_NB_JI_porFecha(fecha))
            if (hi.compareTo(b.WO_KI_NB_JI_getHoraFin()) < 0 && b.WO_KI_NB_JI_getHoraInicio().compareTo(hf) < 0) return true;
        for (var e : c.WO_KI_NB_JI_eventosDelDia(LocalDate.parse(fecha)))
            if (hi.compareTo(e.WO_KI_NB_JI_getHoraFin()) < 0 && e.WO_KI_NB_JI_getHoraInicio().compareTo(hf) < 0) return true;
        return false;
    }

    private VBox WO_KI_NB_JI_cardLista() {
        VBox listaCard = new VBox(0, lista);
        listaCard.getStyleClass().addAll("card", "card-flush");
        return listaCard;
    }

    private void WO_KI_NB_JI_fill() {
        if (fecha.getValue() == null) fecha.setValue(LocalDate.now());
        LocalDate d = fecha.getValue();
        tituloDia.setText(WO_KI_NB_JI_Ui.WO_KI_NB_JI_formatDateShort(d));
        List<WO_KI_NB_JI_BloqueTiempo> bloques = dao.WO_KI_NB_JI_porFecha(d.toString());
        lista.getChildren().clear();
        HBox head = new HBox(8, tituloDia, WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(bloques.size() + (bloques.size() == 1 ? " bloque" : " bloques"), "caption"));
        head.setAlignment(Pos.CENTER_LEFT);
        head.setPadding(new Insets(18, 22, 10, 22));
        lista.getChildren().add(head);
        if (bloques.isEmpty()) {
            lista.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_empty("clock", "Día sin bloques", "Reserva tu primer bloque de enfoque con el formulario de arriba."));
            return;
        }
        List<Node> filas = new ArrayList<>();
        for (WO_KI_NB_JI_BloqueTiempo b : bloques) { Node n = WO_KI_NB_JI_fila(b); lista.getChildren().add(n); filas.add(n); }
        WO_KI_NB_JI_Anim.WO_KI_NB_JI_stagger(filas);
    }

    private Node WO_KI_NB_JI_fila(WO_KI_NB_JI_BloqueTiempo b) {
        String tono = TONO.getOrDefault(b.WO_KI_NB_JI_getCategoria(), "slate");
        Region barra = new Region();
        barra.getStyleClass().addAll("time-bar", "fill-" + tono);
        Label horas = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(b.WO_KI_NB_JI_getHoraInicio() + " – " + b.WO_KI_NB_JI_getHoraFin(), "time-range");
        horas.setMinWidth(120);
        Label t = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(b.WO_KI_NB_JI_getTitulo(), "row-title");
        HBox.setHgrow(t, Priority.ALWAYS);
        t.setMaxWidth(Double.MAX_VALUE);
        Button del = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("delete", "Eliminar bloque");
        del.setOnAction(e -> {
            if (WO_KI_NB_JI_Ui.WO_KI_NB_JI_confirm(view, "¿Eliminar este bloque?", b.WO_KI_NB_JI_getTitulo() + " · " + b.WO_KI_NB_JI_getHoraInicio() + "–" + b.WO_KI_NB_JI_getHoraFin(), "Eliminar")
                && dao.WO_KI_NB_JI_eliminar(b.WO_KI_NB_JI_getId())) {
                WO_KI_NB_JI_fill();
                WO_KI_NB_JI_Notifier.WO_KI_NB_JI_deleted("Bloque eliminado", b.WO_KI_NB_JI_getTitulo());
            }
        });
        HBox row = new HBox(14, barra, horas, t, WO_KI_NB_JI_Ui.WO_KI_NB_JI_pill(b.WO_KI_NB_JI_getCategoria(), tono), del);
        row.setAlignment(Pos.CENTER_LEFT);
        row.getStyleClass().addAll("list-row", "with-divider");
        return row;
    }

    // ------------------------------------------------------------------ utilidades

    private com.agendapersonal.settings.WO_KI_NB_JI_SettingsStore WO_KI_NB_JI_st() { return WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_get().WO_KI_NB_JI_settings(); }

    private static int WO_KI_NB_JI_indexOf(String[] arr, String valor) {
        for (int i = 0; i < arr.length; i++) if (arr[i].equals(valor)) return i;
        return -1;
    }

    public Node WO_KI_NB_JI_getView() { return WO_KI_NB_JI_Ui.WO_KI_NB_JI_scroll(view); }

    /** Fuerza la técnica elegida (p. ej. la preparación de clase desde el inicio del usuario). */
    public void WO_KI_NB_JI_seleccionar(WO_KI_NB_JI_MetodoEnfoque m) { metodos.setValue(m); WO_KI_NB_JI_verMetodo(); }
}

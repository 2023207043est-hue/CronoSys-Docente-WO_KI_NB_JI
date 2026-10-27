package com.agendapersonal.ui.modules;

import com.agendapersonal.controller.WO_KI_NB_JI_MainController;
import com.agendapersonal.domain.model.WO_KI_NB_JI_BloqueTiempo;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Habito;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Nota;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_BloqueDAO;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_HabitoDAO;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_NotaDAO;
import com.agendapersonal.model.WO_KI_NB_JI_Tarea;
import com.agendapersonal.service.WO_KI_NB_JI_AsistenteLocal;
import com.agendapersonal.service.WO_KI_NB_JI_ListaPendientes;
import com.agendapersonal.service.WO_KI_NB_JI_MetodoEnfoque;
import com.agendapersonal.ui.WO_KI_NB_JI_Anim;
import com.agendapersonal.ui.WO_KI_NB_JI_Notifier;
import com.agendapersonal.ui.WO_KI_NB_JI_ThemeService;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Espacio de estudio: un panel a medida con los trozos que tú eliges (enfoque rápido, sugerencias
 * del asistente local, pendientes, bloques del día, hábitos, notas y un resumen del día).
 *
 * <p>Todo funciona sin internet: el asistente lee tus datos y te propone por dónde empezar.</p>
 */
public class WO_KI_NB_JI_EspacioEstudioView {

    /** Los paneles disponibles. El orden en que los guardes es el orden en que se ven. */
    private enum WO_KI_NB_JI_Panel {
        ENFOQUE("enfoque", "Enfoque rápido", "clock"),
        SUGERENCIAS("sugerencias", "Sugerencias", "sparkle"),
        PENDIENTES("pendientes", "Pendientes", "tasks"),
        BLOQUES("bloques", "Bloques de hoy", "calendar"),
        HABITOS("habitos", "Hábitos de hoy", "habits"),
        NOTAS("notas", "Notas rápidas", "notes"),
        RESUMEN("resumen", "Tu día en números", "activity");

        final String id, titulo, icono;
        WO_KI_NB_JI_Panel(String id, String titulo, String icono) { this.id = id; this.titulo = titulo; this.icono = icono; }

        static WO_KI_NB_JI_Panel WO_KI_NB_JI_porId(String id) {
            for (WO_KI_NB_JI_Panel p : values()) if (p.id.equals(id)) return p;
            return null;
        }
    }

    private final WO_KI_NB_JI_MainController c;
    private final WO_KI_NB_JI_NotaDAO notas = new WO_KI_NB_JI_NotaDAO();
    private final WO_KI_NB_JI_BloqueDAO bloques = new WO_KI_NB_JI_BloqueDAO();
    private final WO_KI_NB_JI_HabitoDAO habitos = new WO_KI_NB_JI_HabitoDAO();

    private final VBox view = WO_KI_NB_JI_Ui.WO_KI_NB_JI_page();
    private final VBox zonaPaneles = new VBox(18);
    private final VBox zonaPersonalizar = new VBox(10);
    private ComboBox<WO_KI_NB_JI_MetodoEnfoque> metodoRapido;

    public WO_KI_NB_JI_EspacioEstudioView(WO_KI_NB_JI_MainController c) {
        this.c = c;
        Button personalizar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("settings", "Personalizar el espacio");
        personalizar.setOnAction(e -> WO_KI_NB_JI_alternarPersonalizar());
        view.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_pageHeader("book", "Espacio de estudio",
            "Tu panel de trabajo: elige los bloques que quieres ver y el orden en que aparecen.", personalizar));

        zonaPersonalizar.getStyleClass().addAll("card");
        zonaPersonalizar.setVisible(false);
        zonaPersonalizar.setManaged(false);

        view.getChildren().addAll(zonaPersonalizar, zonaPaneles);
        WO_KI_NB_JI_recargar();
    }

    private com.agendapersonal.settings.WO_KI_NB_JI_SettingsStore WO_KI_NB_JI_st() { return WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_get().WO_KI_NB_JI_settings(); }

    // ------------------------------------------------------------------ paneles

    private List<WO_KI_NB_JI_Panel> WO_KI_NB_JI_panelesActivos() {
        List<String> guardados = WO_KI_NB_JI_st().WO_KI_NB_JI_strings("study.widgets");
        List<WO_KI_NB_JI_Panel> salida = new ArrayList<>();
        for (String id : guardados) {
            WO_KI_NB_JI_Panel p = WO_KI_NB_JI_Panel.WO_KI_NB_JI_porId(id);
            if (p != null && !salida.contains(p)) salida.add(p);
        }
        if (salida.isEmpty()) {                       // la primera vez se muestran los principales
            salida.addAll(List.of(WO_KI_NB_JI_Panel.ENFOQUE, WO_KI_NB_JI_Panel.SUGERENCIAS, WO_KI_NB_JI_Panel.PENDIENTES, WO_KI_NB_JI_Panel.BLOQUES));
        }
        return salida;
    }

    private void WO_KI_NB_JI_guardarPaneles(List<WO_KI_NB_JI_Panel> activos) {
        List<Object> ids = new ArrayList<>();
        for (WO_KI_NB_JI_Panel p : activos) ids.add(p.id);
        WO_KI_NB_JI_st().WO_KI_NB_JI_set("study.widgets", ids);
    }

    private void WO_KI_NB_JI_recargar() {
        zonaPaneles.getChildren().clear();
        for (WO_KI_NB_JI_Panel p : WO_KI_NB_JI_panelesActivos()) {
            Node n = WO_KI_NB_JI_construir(p);
            if (n != null) zonaPaneles.getChildren().add(n);
        }
        if (zonaPersonalizar.isVisible()) WO_KI_NB_JI_pintarPersonalizar();
    }

    private Node WO_KI_NB_JI_construir(WO_KI_NB_JI_Panel p) {
        return switch (p) {
            case ENFOQUE -> WO_KI_NB_JI_panelEnfoque();
            case SUGERENCIAS -> WO_KI_NB_JI_panelSugerencias();
            case PENDIENTES -> WO_KI_NB_JI_panelPendientes();
            case BLOQUES -> WO_KI_NB_JI_panelBloques();
            case HABITOS -> WO_KI_NB_JI_panelHabitos();
            case NOTAS -> WO_KI_NB_JI_panelNotas();
            case RESUMEN -> WO_KI_NB_JI_panelResumen();
        };
    }

    private VBox WO_KI_NB_JI_tarjeta(String titulo, String icono, Node... contenido) {
        HBox head = new HBox(10, WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconChip(icono, "orange", 30, 16), WO_KI_NB_JI_Ui.WO_KI_NB_JI_h3(titulo));
        head.setAlignment(Pos.CENTER_LEFT);
        VBox card = WO_KI_NB_JI_Ui.WO_KI_NB_JI_card();
        card.getChildren().addAll(head);
        card.getChildren().addAll(contenido);
        return card;
    }

    // ------------------------------------------------------------------ Enfoque rápido

    private VBox WO_KI_NB_JI_panelEnfoque() {
        List<WO_KI_NB_JI_MetodoEnfoque> propios = new ArrayList<>(WO_KI_NB_JI_MetodoEnfoque.WO_KI_NB_JI_desdeMapas(WO_KI_NB_JI_st().WO_KI_NB_JI_value("timeBlocking.presets")));
        metodoRapido = new ComboBox<>();
        metodoRapido.getItems().addAll(WO_KI_NB_JI_MetodoEnfoque.PREDEFINIDOS);
        metodoRapido.getItems().addAll(propios);
        metodoRapido.setValue(metodoRapido.getItems().get(0));
        metodoRapido.setMaxWidth(Double.MAX_VALUE);
        javafx.util.Callback<javafx.scene.control.ListView<WO_KI_NB_JI_MetodoEnfoque>, javafx.scene.control.ListCell<WO_KI_NB_JI_MetodoEnfoque>> celdas =
            lv -> new javafx.scene.control.ListCell<>() {
                @Override protected void updateItem(WO_KI_NB_JI_MetodoEnfoque m, boolean empty) {
                    super.updateItem(m, empty);
                    setText(empty || m == null ? null : m.nombre());
                }
            };
        metodoRapido.setCellFactory(celdas);
        metodoRapido.setButtonCell(celdas.call(null));   // si no, el desplegable cerrado muestra el registro entero

        Label resumen = WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("", "caption");
        TextField enfocar = new TextField();
        enfocar.setPromptText("¿En qué te vas a enfocar? (opcional)");
        Button iniciar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("play", "Empezar");
        iniciar.setOnAction(e -> {
            WO_KI_NB_JI_MetodoEnfoque m = metodoRapido.getValue();
            if (m == null) return;
            List<WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_Item> items = WO_KI_NB_JI_pendientesDeHoy();
            WO_KI_NB_JI_PantallaEnfoque.WO_KI_NB_JI_abrir(m, enfocar.getText(),
                WO_KI_NB_JI_PantallaEnfoque.WO_KI_NB_JI_Importancia.WO_KI_NB_JI_desde(WO_KI_NB_JI_st().WO_KI_NB_JI_str("timeBlocking.importance")), items, this::WO_KI_NB_JI_alternarPendiente);
        });

        Runnable pintar = () -> {
            WO_KI_NB_JI_MetodoEnfoque m = metodoRapido.getValue();
            resumen.setText(m == null ? "" : m.WO_KI_NB_JI_resumen());
        };
        metodoRapido.setOnAction(e -> pintar.run());
        pintar.run();

        VBox card = WO_KI_NB_JI_tarjeta("Enfoque rápido", "clock",
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Arranca una sesión sin salir de aquí: reloj, progreso y tus pendientes."),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Método", metodoRapido), resumen,
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_field("Enfoque", enfocar),
            new HBox(10, iniciar));
        return card;
    }

    // ------------------------------------------------------------------ Sugerencias

    private VBox WO_KI_NB_JI_panelSugerencias() {
        WO_KI_NB_JI_AsistenteLocal.WO_KI_NB_JI_Contexto ctx = new WO_KI_NB_JI_AsistenteLocal.WO_KI_NB_JI_Contexto(
            new ArrayList<>(c.WO_KI_NB_JI_getTareas()), notas.WO_KI_NB_JI_listar(null), bloques.WO_KI_NB_JI_porFecha(LocalDate.now().toString()),
            habitos.WO_KI_NB_JI_listar(), habitos.WO_KI_NB_JI_cumplidos(LocalDate.now().toString()), LocalDate.now());
        List<WO_KI_NB_JI_AsistenteLocal.WO_KI_NB_JI_Sugerencia> sugerencias = WO_KI_NB_JI_AsistenteLocal.WO_KI_NB_JI_sugerir(ctx);

        VBox lista = new VBox(8);
        for (WO_KI_NB_JI_AsistenteLocal.WO_KI_NB_JI_Sugerencia s : sugerencias) {
            HBox fila = new HBox(12, WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconChip("idea", s.tono(), 30, 16),
                new VBox(2, WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(s.titulo(), "row-title"), WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted(s.detalle())));
            fila.setAlignment(Pos.TOP_LEFT);
            HBox.setHgrow(fila.getChildren().get(1), Priority.ALWAYS);
            fila.getStyleClass().add("estudio-sugerencia");
            lista.getChildren().add(fila);
        }
        VBox card = WO_KI_NB_JI_tarjeta("Sugerencias", "sparkle",
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Un asistente que funciona en tu equipo: lee tus tareas, notas, bloques y hábitos "
                + "y te dice por dónde empezar. Sin internet y sin enviar nada."),
            lista);
        return card;
    }

    // ------------------------------------------------------------------ Pendientes

    private VBox WO_KI_NB_JI_panelPendientes() {
        List<WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_Item> items = WO_KI_NB_JI_pendientesDeHoy();
        VBox lista = new VBox(6);
        if (items.isEmpty()) {
            lista.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("No tienes pendientes para hoy. Añade casillas a tus notas o crea tareas."));
        }
        for (WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_Item it : items) {
            CheckBox cb = new CheckBox(it.texto());
            cb.setSelected(it.hecho());
            cb.setWrapText(true);
            cb.getStyleClass().add("estudio-pendiente");
            cb.setOnAction(e -> {
                boolean hecho = cb.isSelected();
                WO_KI_NB_JI_alternarPendiente(it, hecho);
                if (hecho) cb.getStyleClass().add("tachado");
                else cb.getStyleClass().remove("tachado");
            });
            if (it.hecho()) cb.getStyleClass().add("tachado");
            lista.getChildren().add(cb);
        }
        VBox card = WO_KI_NB_JI_tarjeta("Pendientes", "tasks",
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Lo que tienes pendiente hoy: tareas de la agenda y casillas de tus notas."), lista);
        return card;
    }

    private List<WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_Item> WO_KI_NB_JI_pendientesDeHoy() {
        List<WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_Item> lista = new ArrayList<>();
        LocalDate hoy = LocalDate.now();
        for (WO_KI_NB_JI_Tarea t : c.WO_KI_NB_JI_getTareas()) {
            if (t.WO_KI_NB_JI_getEstadoTipo() == null || t.WO_KI_NB_JI_getEstadoTipo().WO_KI_NB_JI_esCompletada()) continue;
            if (t.WO_KI_NB_JI_getFecha() != null && t.WO_KI_NB_JI_getFecha().isAfter(hoy)) continue;
            lista.add(new WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_Item(t.WO_KI_NB_JI_getId(), -1, t.WO_KI_NB_JI_getTitulo(), false, "tarea"));
        }
        lista.addAll(WO_KI_NB_JI_ListaPendientes.WO_KI_NB_JI_desdeNotas(notas.WO_KI_NB_JI_listar(null)));
        return lista;
    }

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
            notas.WO_KI_NB_JI_actualizar(n);
            return;
        }
    }

    // ------------------------------------------------------------------ Bloques de hoy

    private VBox WO_KI_NB_JI_panelBloques() {
        List<WO_KI_NB_JI_BloqueTiempo> delDia = bloques.WO_KI_NB_JI_porFecha(LocalDate.now().toString());
        VBox lista = new VBox(6);
        if (delDia.isEmpty()) lista.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Hoy no tienes bloques reservados."));
        for (WO_KI_NB_JI_BloqueTiempo b : delDia) {
            HBox fila = new HBox(10, WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(b.WO_KI_NB_JI_getHoraInicio() + " – " + b.WO_KI_NB_JI_getHoraFin(), "time-range"),
                WO_KI_NB_JI_Ui.WO_KI_NB_JI_label(b.WO_KI_NB_JI_getTitulo(), "row-title"));
            fila.setAlignment(Pos.CENTER_LEFT);
            lista.getChildren().add(fila);
        }
        return WO_KI_NB_JI_tarjeta("Bloques de hoy", "calendar",
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Lo que ya tiene hora hoy."), lista);
    }

    // ------------------------------------------------------------------ Hábitos

    private VBox WO_KI_NB_JI_panelHabitos() {
        List<WO_KI_NB_JI_Habito> lista = habitos.WO_KI_NB_JI_listar();
        Set<Integer> cumplidos = habitos.WO_KI_NB_JI_cumplidos(LocalDate.now().toString());
        VBox filas = new VBox(6);
        if (lista.isEmpty()) filas.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Todavía no tienes hábitos."));
        for (WO_KI_NB_JI_Habito h : lista) {
            CheckBox cb = new CheckBox(h.WO_KI_NB_JI_getNombre());
            cb.setSelected(cumplidos.contains(h.WO_KI_NB_JI_getId()));
            cb.setOnAction(e -> {
                if (cb.isSelected()) habitos.WO_KI_NB_JI_marcar(h.WO_KI_NB_JI_getId(), LocalDate.now().toString());
                else habitos.WO_KI_NB_JI_desmarcar(h.WO_KI_NB_JI_getId(), LocalDate.now().toString());
            });
            filas.getChildren().add(cb);
        }
        return WO_KI_NB_JI_tarjeta("Hábitos de hoy", "habits", filas);
    }

    // ------------------------------------------------------------------ Notas rápidas

    private VBox WO_KI_NB_JI_panelNotas() {
        TextField campo = new TextField();
        campo.setPromptText("Escribe una idea y pulsa Enter…");
        HBox.setHgrow(campo, Priority.ALWAYS);
        Button guardar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Guardar");
        Runnable add = () -> {
            String txt = campo.getText().trim();
            if (txt.isEmpty()) { WO_KI_NB_JI_Anim.WO_KI_NB_JI_shake(campo); return; }
            String hoy = LocalDate.now().toString();
            if (notas.WO_KI_NB_JI_insertar(new WO_KI_NB_JI_Nota(0, txt, "", "idea", "IDEA", hoy, hoy, false))) {
                campo.clear();
                WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success("Idea guardada", txt);
                WO_KI_NB_JI_recargar();
            } else WO_KI_NB_JI_Notifier.WO_KI_NB_JI_error("No se pudo guardar la idea", null);
        };
        guardar.setOnAction(e -> add.run());
        campo.setOnAction(e -> add.run());

        List<WO_KI_NB_JI_Nota> recientes = notas.WO_KI_NB_JI_listar(null);
        VBox lista = new VBox(4);
        for (WO_KI_NB_JI_Nota n : recientes.subList(0, Math.min(5, recientes.size())))
            lista.getChildren().add(WO_KI_NB_JI_Ui.WO_KI_NB_JI_label("• " + n.WO_KI_NB_JI_getTitulo(), "caption"));

        return WO_KI_NB_JI_tarjeta("Notas rápidas", "notes",
            new HBox(8, campo, guardar), lista);
    }

    // ------------------------------------------------------------------ Resumen

    private VBox WO_KI_NB_JI_panelResumen() {
        LocalDate hoy = LocalDate.now();
        int hechas = 0, pendientes = 0;
        for (WO_KI_NB_JI_Tarea t : c.WO_KI_NB_JI_getTareas()) {
            if (t.WO_KI_NB_JI_getEstadoTipo() == null) continue;
            if (t.WO_KI_NB_JI_getEstadoTipo().WO_KI_NB_JI_esCompletada()) hechas++; else pendientes++;
        }
        Set<Integer> cumplidos = habitos.WO_KI_NB_JI_cumplidos(hoy.toString());
        int habitosTotal = habitos.WO_KI_NB_JI_listar().size();
        int bloquesHoy = bloques.WO_KI_NB_JI_porFecha(hoy.toString()).size();

        HBox fila = new HBox(16,
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_stat("Tareas hechas", String.valueOf(hechas), "de " + (hechas + pendientes) + " en total", "success", "green"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_stat("Hábitos de hoy", cumplidos.size() + " / " + habitosTotal, "marcados", "habits", "orange"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_stat("Bloques hoy", String.valueOf(bloquesHoy), "reservados", "calendar", "blue"));
        HBox.setHgrow(fila.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(fila.getChildren().get(1), Priority.ALWAYS);
        HBox.setHgrow(fila.getChildren().get(2), Priority.ALWAYS);
        return WO_KI_NB_JI_tarjeta("Tu día en números", "activity", fila);
    }

    // ------------------------------------------------------------------ personalizar

    private void WO_KI_NB_JI_alternarPersonalizar() {
        boolean visible = !zonaPersonalizar.isVisible();
        zonaPersonalizar.setVisible(visible);
        zonaPersonalizar.setManaged(visible);
        if (visible) WO_KI_NB_JI_pintarPersonalizar();
    }

    private void WO_KI_NB_JI_pintarPersonalizar() {
        zonaPersonalizar.getChildren().clear();
        zonaPersonalizar.getChildren().addAll(WO_KI_NB_JI_Ui.WO_KI_NB_JI_h3("Personaliza tu espacio"),
            WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("Marca los paneles que quieres ver y ordénalos con las flechas. Se guarda en tu perfil."));
        List<WO_KI_NB_JI_Panel> activos = WO_KI_NB_JI_panelesActivos();
        for (WO_KI_NB_JI_Panel p : WO_KI_NB_JI_Panel.values()) {
            boolean activo = activos.contains(p);
            CheckBox cb = new CheckBox(p.titulo);
            cb.setSelected(activo);
            cb.setOnAction(e -> {
                List<WO_KI_NB_JI_Panel> lista = new ArrayList<>(WO_KI_NB_JI_panelesActivos());
                if (cb.isSelected()) { if (!lista.contains(p)) lista.add(p); }
                else lista.remove(p);
                WO_KI_NB_JI_guardarPaneles(lista);
                WO_KI_NB_JI_recargar();
            });
            HBox fila = new HBox(10, cb, WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer());
            if (activo) {
                Button arriba = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("chevron-up", "Subir");
                Button abajo = WO_KI_NB_JI_Ui.WO_KI_NB_JI_iconButton("chevron-down", "Bajar");
                arriba.setOnAction(e -> WO_KI_NB_JI_mover(p, -1));
                abajo.setOnAction(e -> WO_KI_NB_JI_mover(p, 1));
                fila.getChildren().addAll(arriba, abajo);
            }
            fila.setAlignment(Pos.CENTER_LEFT);
            zonaPersonalizar.getChildren().add(fila);
        }
        Button cerrar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary(null, "Listo");
        cerrar.setOnAction(e -> WO_KI_NB_JI_alternarPersonalizar());
        zonaPersonalizar.getChildren().add(cerrar);
    }

    private void WO_KI_NB_JI_mover(WO_KI_NB_JI_Panel p, int delta) {
        List<WO_KI_NB_JI_Panel> lista = new ArrayList<>(WO_KI_NB_JI_panelesActivos());
        int i = lista.indexOf(p);
        int j = i + delta;
        if (i < 0 || j < 0 || j >= lista.size()) return;
        lista.remove(i);
        lista.add(j, p);
        WO_KI_NB_JI_guardarPaneles(lista);
        WO_KI_NB_JI_recargar();
    }

    public Node WO_KI_NB_JI_getView() { return WO_KI_NB_JI_Ui.WO_KI_NB_JI_scroll(view); }
}

package com.agendapersonal.ui.modules;

import com.agendapersonal.controller.wkniMainController;
import com.agendapersonal.domain.model.wkniBloqueTiempo;
import com.agendapersonal.domain.model.wkniHabito;
import com.agendapersonal.domain.model.wkniNota;
import com.agendapersonal.infrastructure.repo.wkniBloqueDAO;
import com.agendapersonal.infrastructure.repo.wkniHabitoDAO;
import com.agendapersonal.infrastructure.repo.wkniNotaDAO;
import com.agendapersonal.model.wkniTarea;
import com.agendapersonal.service.wkniAsistenteLocal;
import com.agendapersonal.service.wkniListaPendientes;
import com.agendapersonal.service.wkniMetodoEnfoque;
import com.agendapersonal.ui.wkniAnim;
import com.agendapersonal.ui.wkniNotifier;
import com.agendapersonal.ui.wkniThemeService;
import com.agendapersonal.util.wkniUi;
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
public class wkniEspacioEstudioView {

    /** Los paneles disponibles. El orden en que los guardes es el orden en que se ven. */
    private enum wkniPanel {
        ENFOQUE("enfoque", "Enfoque rápido", "clock"),
        SUGERENCIAS("sugerencias", "Sugerencias", "sparkle"),
        PENDIENTES("pendientes", "Pendientes", "tasks"),
        BLOQUES("bloques", "Bloques de hoy", "calendar"),
        HABITOS("habitos", "Hábitos de hoy", "habits"),
        NOTAS("notas", "Notas rápidas", "notes"),
        RESUMEN("resumen", "Tu día en números", "activity");

        final String wkniId, wkniTitulo, wkniIcono;
        wkniPanel(String wkniId, String wkniTitulo, String wkniIcono) { this.wkniId = wkniId; this.wkniTitulo = wkniTitulo; this.wkniIcono = wkniIcono; }

        static wkniPanel wkniPorId(String wkniId) {
            for (wkniPanel wkni_p : values()) if (wkni_p.wkniId.equals(wkniId)) return wkni_p;
            return null;
        }
    }

    private final wkniMainController wkniC;
    private final wkniNotaDAO wkniNotas = new wkniNotaDAO();
    private final wkniBloqueDAO wkniBloques = new wkniBloqueDAO();
    private final wkniHabitoDAO wkniHabitos = new wkniHabitoDAO();

    private final VBox wkniView = wkniUi.wkniPage();
    private final VBox wkniZonaPaneles = new VBox(18);
    private final VBox wkniZonaPersonalizar = new VBox(10);
    private ComboBox<wkniMetodoEnfoque> wkniMetodoRapido;

    public wkniEspacioEstudioView(wkniMainController wkniC) {
        this.wkniC = wkniC;
        Button wkniPersonalizar = wkniUi.wkniSecondary("settings", "Personalizar el espacio");
        wkniPersonalizar.setOnAction(wkniE -> wkniAlternarPersonalizar());
        wkniView.getChildren().add(wkniUi.wkniPageHeader("book", "Espacio de estudio",
            "Tu panel de trabajo: elige los bloques que quieres ver y el orden en que aparecen.", wkniPersonalizar));

        wkniZonaPersonalizar.getStyleClass().addAll("card");
        wkniZonaPersonalizar.setVisible(false);
        wkniZonaPersonalizar.setManaged(false);

        wkniView.getChildren().addAll(wkniZonaPersonalizar, wkniZonaPaneles);
        wkniRecargar();
    }

    private com.agendapersonal.settings.wkniSettingsStore wkniSt() { return wkniThemeService.wkniGet().wkniSettings(); }

    // ------------------------------------------------------------------ paneles

    private List<wkniPanel> wkniPanelesActivos() {
        List<String> wkniGuardados = wkniSt().wkniStrings("study.widgets");
        List<wkniPanel> wkniSalida = new ArrayList<>();
        for (String wkniId : wkniGuardados) {
            wkniPanel wkni_p = wkniPanel.wkniPorId(wkniId);
            if (wkni_p != null && !wkniSalida.contains(wkni_p)) wkniSalida.add(wkni_p);
        }
        if (wkniSalida.isEmpty()) {                       // la primera vez se muestran los principales
            wkniSalida.addAll(List.of(wkniPanel.ENFOQUE, wkniPanel.SUGERENCIAS, wkniPanel.PENDIENTES, wkniPanel.BLOQUES));
        }
        return wkniSalida;
    }

    private void wkniGuardarPaneles(List<wkniPanel> wkniActivos) {
        List<Object> wkniIds = new ArrayList<>();
        for (wkniPanel wkni_p : wkniActivos) wkniIds.add(wkni_p.wkniId);
        wkniSt().wkniSet("study.widgets", wkniIds);
    }

    private void wkniRecargar() {
        wkniZonaPaneles.getChildren().clear();
        for (wkniPanel wkni_p : wkniPanelesActivos()) {
            Node wkniN = wkniConstruir(wkni_p);
            if (wkniN != null) wkniZonaPaneles.getChildren().add(wkniN);
        }
        if (wkniZonaPersonalizar.isVisible()) wkniPintarPersonalizar();
    }

    private Node wkniConstruir(wkniPanel wkni_p) {
        return switch (wkni_p) {
            case ENFOQUE -> wkniPanelEnfoque();
            case SUGERENCIAS -> wkniPanelSugerencias();
            case PENDIENTES -> wkniPanelPendientes();
            case BLOQUES -> wkniPanelBloques();
            case HABITOS -> wkniPanelHabitos();
            case NOTAS -> wkniPanelNotas();
            case RESUMEN -> wkniPanelResumen();
        };
    }

    private VBox wkniTarjeta(String wkniTitulo, String wkniIcono, Node... wkniContenido) {
        HBox wkniHead = new HBox(10, wkniUi.wkniIconChip(wkniIcono, "orange", 30, 16), wkniUi.wkniH3(wkniTitulo));
        wkniHead.setAlignment(Pos.CENTER_LEFT);
        VBox wkniCard = wkniUi.wkniCard();
        wkniCard.getChildren().addAll(wkniHead);
        wkniCard.getChildren().addAll(wkniContenido);
        return wkniCard;
    }

    // ------------------------------------------------------------------ Enfoque rápido

    private VBox wkniPanelEnfoque() {
        List<wkniMetodoEnfoque> wkniPropios = new ArrayList<>(wkniMetodoEnfoque.wkniDesdeMapas(wkniSt().wkniValue("timeBlocking.presets")));
        wkniMetodoRapido = new ComboBox<>();
        wkniMetodoRapido.getItems().addAll(wkniMetodoEnfoque.wkniPREDEFINIDOS);
        wkniMetodoRapido.getItems().addAll(wkniPropios);
        wkniMetodoRapido.setValue(wkniMetodoRapido.getItems().get(0));
        wkniMetodoRapido.setMaxWidth(Double.MAX_VALUE);
        javafx.util.Callback<javafx.scene.control.ListView<wkniMetodoEnfoque>, javafx.scene.control.ListCell<wkniMetodoEnfoque>> wkniCeldas =
            wkniLv -> new javafx.scene.control.ListCell<>() {
                @Override protected void updateItem(wkniMetodoEnfoque wkniM, boolean wkniEmpty) {
                    super.updateItem(wkniM, wkniEmpty);
                    setText(wkniEmpty || wkniM == null ? null : wkniM.wkniNombre());
                }
            };
        wkniMetodoRapido.setCellFactory(wkniCeldas);
        wkniMetodoRapido.setButtonCell(wkniCeldas.call(null));   // si no, el desplegable cerrado muestra el registro entero

        Label wkni_resumen = wkniUi.wkniLabel("", "caption");
        TextField wkniEnfocar = new TextField();
        wkniEnfocar.setPromptText("¿En qué te vas a enfocar? (opcional)");
        Button wkniIniciar = wkniUi.wkniPrimary("play", "Empezar");
        wkniIniciar.setOnAction(wkniE -> {
            wkniMetodoEnfoque wkniM = wkniMetodoRapido.getValue();
            if (wkniM == null) return;
            List<wkniListaPendientes.wkniItem> wkniItems = wkniPendientesDeHoy();
            wkniPantallaEnfoque.wkniAbrir(wkniM, wkniEnfocar.getText(),
                wkniPantallaEnfoque.wkniImportancia.wkniDesde(wkniSt().wkniStr("timeBlocking.importance")), wkniItems, this::wkniAlternarPendiente);
        });

        Runnable wkniPintar = () -> {
            wkniMetodoEnfoque wkniM = wkniMetodoRapido.getValue();
            wkni_resumen.setText(wkniM == null ? "" : wkniM.wkniResumen());
        };
        wkniMetodoRapido.setOnAction(wkniE -> wkniPintar.run());
        wkniPintar.run();

        VBox wkniCard = wkniTarjeta("Enfoque rápido", "clock",
            wkniUi.wkniMuted("Arranca una sesión sin salir de aquí: reloj, progreso y tus pendientes."),
            wkniUi.wkniField("Método", wkniMetodoRapido), wkni_resumen,
            wkniUi.wkniField("Enfoque", wkniEnfocar),
            new HBox(10, wkniIniciar));
        return wkniCard;
    }

    // ------------------------------------------------------------------ Sugerencias

    private VBox wkniPanelSugerencias() {
        wkniAsistenteLocal.wkniContexto wkniCtx = new wkniAsistenteLocal.wkniContexto(
            new ArrayList<>(wkniC.wkniGetTareas()), wkniNotas.wkniListar(null), wkniBloques.wkniPorFecha(LocalDate.now().toString()),
            wkniHabitos.wkniListar(), wkniHabitos.wkniCumplidos(LocalDate.now().toString()), LocalDate.now());
        List<wkniAsistenteLocal.wkniSugerencia> wkniSugerencias = wkniAsistenteLocal.wkniSugerir(wkniCtx);

        VBox wkniLista = new VBox(8);
        for (wkniAsistenteLocal.wkniSugerencia wkniS : wkniSugerencias) {
            HBox wkniFila = new HBox(12, wkniUi.wkniIconChip("idea", wkniS.wkni_tono(), 30, 16),
                new VBox(2, wkniUi.wkniLabel(wkniS.wkniTitulo(), "row-title"), wkniUi.wkniMuted(wkniS.wkniDetalle())));
            wkniFila.setAlignment(Pos.TOP_LEFT);
            HBox.setHgrow(wkniFila.getChildren().get(1), Priority.ALWAYS);
            wkniFila.getStyleClass().add("estudio-sugerencia");
            wkniLista.getChildren().add(wkniFila);
        }
        VBox wkniCard = wkniTarjeta("Sugerencias", "sparkle",
            wkniUi.wkniMuted("Un asistente que funciona en tu equipo: lee tus tareas, notas, bloques y hábitos "
                + "y te dice por dónde empezar. Sin internet y sin enviar nada."),
            wkniLista);
        return wkniCard;
    }

    // ------------------------------------------------------------------ Pendientes

    private VBox wkniPanelPendientes() {
        List<wkniListaPendientes.wkniItem> wkniItems = wkniPendientesDeHoy();
        VBox wkniLista = new VBox(6);
        if (wkniItems.isEmpty()) {
            wkniLista.getChildren().add(wkniUi.wkniMuted("No tienes pendientes para hoy. Añade casillas a tus notas o crea tareas."));
        }
        for (wkniListaPendientes.wkniItem wkniIt : wkniItems) {
            CheckBox wkniCb = new CheckBox(wkniIt.wkniTexto());
            wkniCb.setSelected(wkniIt.wkniHecho());
            wkniCb.setWrapText(true);
            wkniCb.getStyleClass().add("estudio-pendiente");
            wkniCb.setOnAction(wkniE -> {
                boolean wkniHecho = wkniCb.isSelected();
                wkniAlternarPendiente(wkniIt, wkniHecho);
                if (wkniHecho) wkniCb.getStyleClass().add("tachado");
                else wkniCb.getStyleClass().remove("tachado");
            });
            if (wkniIt.wkniHecho()) wkniCb.getStyleClass().add("tachado");
            wkniLista.getChildren().add(wkniCb);
        }
        VBox wkniCard = wkniTarjeta("Pendientes", "tasks",
            wkniUi.wkniMuted("Lo que tienes pendiente hoy: tareas de la agenda y casillas de tus notas."), wkniLista);
        return wkniCard;
    }

    private List<wkniListaPendientes.wkniItem> wkniPendientesDeHoy() {
        List<wkniListaPendientes.wkniItem> wkniLista = new ArrayList<>();
        LocalDate wkniHoy = LocalDate.now();
        for (wkniTarea wkniT : wkniC.wkniGetTareas()) {
            if (wkniT.wkniGetEstadoTipo() == null || wkniT.wkniGetEstadoTipo().wkniEsCompletada()) continue;
            if (wkniT.wkniGetFecha() != null && wkniT.wkniGetFecha().isAfter(wkniHoy)) continue;
            wkniLista.add(new wkniListaPendientes.wkniItem(wkniT.wkniGetId(), -1, wkniT.wkniGetTitulo(), false, "tarea"));
        }
        wkniLista.addAll(wkniListaPendientes.wkniDesdeNotas(wkniNotas.wkniListar(null)));
        return wkniLista;
    }

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
            wkniNotas.wkniActualizar(wkniN);
            return;
        }
    }

    // ------------------------------------------------------------------ Bloques de hoy

    private VBox wkniPanelBloques() {
        List<wkniBloqueTiempo> wkniDelDia = wkniBloques.wkniPorFecha(LocalDate.now().toString());
        VBox wkniLista = new VBox(6);
        if (wkniDelDia.isEmpty()) wkniLista.getChildren().add(wkniUi.wkniMuted("Hoy no tienes bloques reservados."));
        for (wkniBloqueTiempo wkniB : wkniDelDia) {
            HBox wkniFila = new HBox(10, wkniUi.wkniLabel(wkniB.wkniGetHoraInicio() + " – " + wkniB.wkniGetHoraFin(), "time-range"),
                wkniUi.wkniLabel(wkniB.wkniGetTitulo(), "row-title"));
            wkniFila.setAlignment(Pos.CENTER_LEFT);
            wkniLista.getChildren().add(wkniFila);
        }
        return wkniTarjeta("Bloques de hoy", "calendar",
            wkniUi.wkniMuted("Lo que ya tiene hora hoy."), wkniLista);
    }

    // ------------------------------------------------------------------ Hábitos

    private VBox wkniPanelHabitos() {
        List<wkniHabito> wkniLista = wkniHabitos.wkniListar();
        Set<Integer> wkniCumplidos = wkniHabitos.wkniCumplidos(LocalDate.now().toString());
        VBox wkniFilas = new VBox(6);
        if (wkniLista.isEmpty()) wkniFilas.getChildren().add(wkniUi.wkniMuted("Todavía no tienes hábitos."));
        for (wkniHabito wkniH : wkniLista) {
            CheckBox wkniCb = new CheckBox(wkniH.wkniGetNombre());
            wkniCb.setSelected(wkniCumplidos.contains(wkniH.wkniGetId()));
            wkniCb.setOnAction(wkniE -> {
                if (wkniCb.isSelected()) wkniHabitos.wkniMarcar(wkniH.wkniGetId(), LocalDate.now().toString());
                else wkniHabitos.wkniDesmarcar(wkniH.wkniGetId(), LocalDate.now().toString());
            });
            wkniFilas.getChildren().add(wkniCb);
        }
        return wkniTarjeta("Hábitos de hoy", "habits", wkniFilas);
    }

    // ------------------------------------------------------------------ Notas rápidas

    private VBox wkniPanelNotas() {
        TextField wkniCampo = new TextField();
        wkniCampo.setPromptText("Escribe una idea y pulsa Enter…");
        HBox.setHgrow(wkniCampo, Priority.ALWAYS);
        Button wkniGuardar = wkniUi.wkniSecondary(null, "Guardar");
        Runnable wkniAdd = () -> {
            String wkniTxt = wkniCampo.getText().trim();
            if (wkniTxt.isEmpty()) { wkniAnim.wkniShake(wkniCampo); return; }
            String wkniHoy = LocalDate.now().toString();
            if (wkniNotas.wkniInsertar(new wkniNota(0, wkniTxt, "", "idea", "IDEA", wkniHoy, wkniHoy, false))) {
                wkniCampo.clear();
                wkniNotifier.wkniSuccess("Idea guardada", wkniTxt);
                wkniRecargar();
            } else wkniNotifier.wkniError("No se pudo guardar la idea", null);
        };
        wkniGuardar.setOnAction(wkniE -> wkniAdd.run());
        wkniCampo.setOnAction(wkniE -> wkniAdd.run());

        List<wkniNota> wkniRecientes = wkniNotas.wkniListar(null);
        VBox wkniLista = new VBox(4);
        for (wkniNota wkniN : wkniRecientes.subList(0, Math.min(5, wkniRecientes.size())))
            wkniLista.getChildren().add(wkniUi.wkniLabel("• " + wkniN.wkniGetTitulo(), "caption"));

        return wkniTarjeta("Notas rápidas", "notes",
            new HBox(8, wkniCampo, wkniGuardar), wkniLista);
    }

    // ------------------------------------------------------------------ Resumen

    private VBox wkniPanelResumen() {
        LocalDate wkniHoy = LocalDate.now();
        int wkniHechas = 0, wkniPendientes = 0;
        for (wkniTarea wkniT : wkniC.wkniGetTareas()) {
            if (wkniT.wkniGetEstadoTipo() == null) continue;
            if (wkniT.wkniGetEstadoTipo().wkniEsCompletada()) wkniHechas++; else wkniPendientes++;
        }
        Set<Integer> wkniCumplidos = wkniHabitos.wkniCumplidos(wkniHoy.toString());
        int wkniHabitosTotal = wkniHabitos.wkniListar().size();
        int wkniBloquesHoy = wkniBloques.wkniPorFecha(wkniHoy.toString()).size();

        HBox wkniFila = new HBox(16,
            wkniUi.wkniStat("Tareas hechas", String.valueOf(wkniHechas), "de " + (wkniHechas + wkniPendientes) + " en total", "success", "green"),
            wkniUi.wkniStat("Hábitos de hoy", wkniCumplidos.size() + " / " + wkniHabitosTotal, "marcados", "habits", "orange"),
            wkniUi.wkniStat("Bloques hoy", String.valueOf(wkniBloquesHoy), "reservados", "calendar", "blue"));
        HBox.setHgrow(wkniFila.getChildren().get(0), Priority.ALWAYS);
        HBox.setHgrow(wkniFila.getChildren().get(1), Priority.ALWAYS);
        HBox.setHgrow(wkniFila.getChildren().get(2), Priority.ALWAYS);
        return wkniTarjeta("Tu día en números", "activity", wkniFila);
    }

    // ------------------------------------------------------------------ personalizar

    private void wkniAlternarPersonalizar() {
        boolean wkniVisible = !wkniZonaPersonalizar.isVisible();
        wkniZonaPersonalizar.setVisible(wkniVisible);
        wkniZonaPersonalizar.setManaged(wkniVisible);
        if (wkniVisible) wkniPintarPersonalizar();
    }

    private void wkniPintarPersonalizar() {
        wkniZonaPersonalizar.getChildren().clear();
        wkniZonaPersonalizar.getChildren().addAll(wkniUi.wkniH3("Personaliza tu espacio"),
            wkniUi.wkniMuted("Marca los paneles que quieres ver y ordénalos con las flechas. Se guarda en tu perfil."));
        List<wkniPanel> wkniActivos = wkniPanelesActivos();
        for (wkniPanel wkni_p : wkniPanel.values()) {
            boolean wkniActivo = wkniActivos.contains(wkni_p);
            CheckBox wkniCb = new CheckBox(wkni_p.wkniTitulo);
            wkniCb.setSelected(wkniActivo);
            wkniCb.setOnAction(wkniE -> {
                List<wkniPanel> wkniLista = new ArrayList<>(wkniPanelesActivos());
                if (wkniCb.isSelected()) { if (!wkniLista.contains(wkni_p)) wkniLista.add(wkni_p); }
                else wkniLista.remove(wkni_p);
                wkniGuardarPaneles(wkniLista);
                wkniRecargar();
            });
            HBox wkniFila = new HBox(10, wkniCb, wkniUi.wkniSpacer());
            if (wkniActivo) {
                Button wkniArriba = wkniUi.wkniIconButton("chevron-up", "Subir");
                Button wkniAbajo = wkniUi.wkniIconButton("chevron-down", "Bajar");
                wkniArriba.setOnAction(wkniE -> wkniMover(wkni_p, -1));
                wkniAbajo.setOnAction(wkniE -> wkniMover(wkni_p, 1));
                wkniFila.getChildren().addAll(wkniArriba, wkniAbajo);
            }
            wkniFila.setAlignment(Pos.CENTER_LEFT);
            wkniZonaPersonalizar.getChildren().add(wkniFila);
        }
        Button wkniCerrar = wkniUi.wkniSecondary(null, "Listo");
        wkniCerrar.setOnAction(wkniE -> wkniAlternarPersonalizar());
        wkniZonaPersonalizar.getChildren().add(wkniCerrar);
    }

    private void wkniMover(wkniPanel wkni_p, int wkniDelta) {
        List<wkniPanel> wkniLista = new ArrayList<>(wkniPanelesActivos());
        int wkniI = wkniLista.indexOf(wkni_p);
        int wkniJ = wkniI + wkniDelta;
        if (wkniI < 0 || wkniJ < 0 || wkniJ >= wkniLista.size()) return;
        wkniLista.remove(wkniI);
        wkniLista.add(wkniJ, wkni_p);
        wkniGuardarPaneles(wkniLista);
        wkniRecargar();
    }

    public Node wkniGetView() { return wkniUi.wkniScroll(wkniView); }
}

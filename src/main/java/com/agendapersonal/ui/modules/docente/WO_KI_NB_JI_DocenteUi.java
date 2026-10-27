package com.agendapersonal.ui.modules.docente;

import com.agendapersonal.domain.model.WO_KI_NB_JI_EstadoAsistencia;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstadoEntrega;
import com.agendapersonal.service.WO_KI_NB_JI_ExcelClaseService;
import com.agendapersonal.service.WO_KI_NB_JI_RiesgoEstudianteService;
import com.agendapersonal.service.WO_KI_NB_JI_TablaReporte;
import com.agendapersonal.ui.WO_KI_NB_JI_IconFactory;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.util.Duration;

import java.io.File;
import java.util.List;
import java.util.Locale;

/** Piezas visuales compartidas por los paneles del Espacio Docente (colores de estado, semáforo, exportar). */
final class WO_KI_NB_JI_DocenteUi {
    private WO_KI_NB_JI_DocenteUi() {}

    /** Sufijo CSS de cada estado de asistencia (fila-…, c-…, e-…). */
    static String WO_KI_NB_JI_css(WO_KI_NB_JI_EstadoAsistencia e) {
        if (e == null) return "sin";
        return switch (e) {
            case PRESENTE -> "presente"; case FALTA -> "falta"; case TARDANZA -> "tardanza"; case JUSTIFICADA -> "justificada";
        };
    }

    /** Color del tema para gráficos y puntos de leyenda. */
    static String WO_KI_NB_JI_color(WO_KI_NB_JI_EstadoAsistencia e) {
        return switch (e) {
            case PRESENTE -> "-c-green"; case FALTA -> "-danger"; case TARDANZA -> "#E8B43A"; case JUSTIFICADA -> "-c-blue";
        };
    }

    /** Tono de píldora (pill-…) para estados y niveles. */
    static String WO_KI_NB_JI_tono(WO_KI_NB_JI_EstadoAsistencia e) {
        if (e == null) return "slate";
        return switch (e) { case PRESENTE -> "green"; case FALTA -> "danger"; case TARDANZA -> "amber"; case JUSTIFICADA -> "blue"; };
    }

    static String WO_KI_NB_JI_tono(WO_KI_NB_JI_EstadoEntrega e) {
        if (e == null) return "slate";
        return switch (e) { case ENTREGADO -> "green"; case TARDE -> "amber"; case NO_ENTREGADO -> "danger"; };
    }

    static String WO_KI_NB_JI_tono(WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel n) {
        return switch (n) { case BIEN -> "green"; case ATENCION -> "amber"; case ALTO -> "danger"; };
    }

    /** Ícono de advertencia junto al nombre (rojo = riesgo, ámbar = atención) con los motivos en el tooltip. */
    static Node WO_KI_NB_JI_bandera(WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Evaluacion ev) {
        if (ev == null || ev.nivel() == WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel.BIEN) return new Region();
        boolean alto = ev.nivel() == WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel.ALTO;
        Node icon = WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_icon("warning", 17, alto ? "tint-rose" : "tint-amber");
        StackPane s = new StackPane(icon);
        s.setMinSize(22, 22);
        Tooltip t = new Tooltip((alto ? "⚠ En riesgo\n" : "Atención\n") + String.join("\n", ev.motivos().stream().map(m -> "• " + m).toList()));
        t.setShowDelay(Duration.millis(120));
        t.setShowDuration(Duration.seconds(30));
        Tooltip.install(s, t);
        return s;
    }

    /** Nombre del estudiante con avatar y bandera de riesgo. */
    static HBox WO_KI_NB_JI_nombre(String nombre, WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Evaluacion ev) {
        Label n = new Label(nombre);
        n.getStyleClass().add("body");
        n.setStyle("-fx-font-weight: bold;");
        HBox box = new HBox(10, WO_KI_NB_JI_Ui.WO_KI_NB_JI_avatar(nombre, 28), n, WO_KI_NB_JI_bandera(ev));
        box.setAlignment(Pos.CENTER_LEFT);
        return box;
    }

    /** Tarjeta de cifra compacta. */
    static VBox WO_KI_NB_JI_metrica(String titulo, String valor, String nota, String icono, String tono) {
        return WO_KI_NB_JI_Ui.WO_KI_NB_JI_stat(titulo, valor, nota, icono, tono);
    }

    /** Píldora con un punto de color y una cifra: "● 18 presentes". */
    static HBox WO_KI_NB_JI_conteo(String colorCss, String texto, String tonoPill) {
        Region punto = new Region();
        punto.getStyleClass().add("leyenda-punto");
        punto.setStyle("-fx-background-color: " + colorCss + ";");
        Label l = new Label(texto);
        HBox h = new HBox(7, punto, l);
        h.setAlignment(Pos.CENTER_LEFT);
        h.getStyleClass().addAll("conteo-chip", "chip-" + tonoPill);
        return h;
    }

    /** Barra apilada de proporciones (p. ej. presentes/faltas/tardanzas del día). */
    static HBox WO_KI_NB_JI_barraApilada(double[] valores, String[] colores, double alto) {
        HBox bar = new HBox();
        bar.setMinHeight(alto); bar.setPrefHeight(alto); bar.setMaxHeight(alto);
        bar.setStyle("-fx-background-color: -surface3; -fx-background-radius: 99;");
        double total = 0;
        for (double v : valores) total += v;
        if (total <= 0) return bar;
        for (int i = 0; i < valores.length; i++) {
            if (valores[i] <= 0) continue;
            Region seg = new Region();
            seg.setStyle("-fx-background-color: " + colores[i] + ";"
                + (i == 0 ? "-fx-background-radius: 99 0 0 99;" : ""));
            seg.prefWidthProperty().bind(bar.widthProperty().multiply(valores[i] / total));
            seg.setMinWidth(0);
            bar.getChildren().add(seg);
        }
        return bar;
    }

    /** Encabezado de sección: título y texto de ayuda. */
    static VBox WO_KI_NB_JI_seccion(String titulo, String ayuda) {
        Label h = WO_KI_NB_JI_Ui.WO_KI_NB_JI_h2(titulo);
        VBox v = new VBox(4, h);
        if (ayuda != null) { Label a = WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted(ayuda); a.setWrapText(true); v.getChildren().add(a); }
        return v;
    }

    static VBox WO_KI_NB_JI_panel() {
        VBox v = new VBox(14);
        v.setPadding(new Insets(16, 4, 8, 4));
        return v;
    }

    // ------------------------------------------------------------------ exportar / imprimir

    /** Pide dónde guardar y exporta las tablas a un .xlsx; avisa el resultado. */
    static void WO_KI_NB_JI_exportarExcel(Node owner, String nombreSugerido, List<WO_KI_NB_JI_TablaReporte> tablas) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exportar a Excel");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Libro de Excel (*.xlsx)", "*.xlsx"));
        chooser.setInitialFileName(WO_KI_NB_JI_archivo(nombreSugerido) + ".xlsx");
        File carpeta = new File(System.getProperty("user.home"), "Documents");
        if (carpeta.isDirectory()) chooser.setInitialDirectory(carpeta);
        File file = chooser.showSaveDialog(WO_KI_NB_JI_ventana(owner));
        if (file == null) return;
        File destino = file.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx") ? file : new File(file.getParentFile(), file.getName() + ".xlsx");
        try {
            WO_KI_NB_JI_ExcelClaseService.WO_KI_NB_JI_exportarReporte(destino.toPath(), tablas);
            Alert a = new Alert(Alert.AlertType.INFORMATION, "Se guardó el archivo:\n" + destino.getAbsolutePath(),
                new ButtonType("Abrir archivo"), ButtonType.CLOSE);
            a.setHeaderText("Exportado a Excel");
            a.setTitle("Compás");
            if (WO_KI_NB_JI_ventana(owner) != null) a.initOwner(WO_KI_NB_JI_ventana(owner));
            a.showAndWait().filter(b -> b.getText().equals("Abrir archivo"))
                .ifPresent(b -> com.agendapersonal.WO_KI_NB_JI_Main.WO_KI_NB_JI_abrirEnSistema(destino.toURI().toString()));
        } catch (Exception ex) {
            WO_KI_NB_JI_error(owner, "No se pudo exportar: " + ex.getMessage() + "\n¿Está el archivo abierto en Excel? Ciérralo e inténtalo de nuevo.");
        }
    }

    static String WO_KI_NB_JI_archivo(String nombre) { return nombre.replaceAll("[\\\\/:*?\"<>|]", "_").trim(); }

    static Window WO_KI_NB_JI_ventana(Node n) { return n == null || n.getScene() == null ? null : n.getScene().getWindow(); }

    static void WO_KI_NB_JI_info(Node owner, String texto) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, texto, ButtonType.OK);
        a.setHeaderText(null); a.setTitle("Compás");
        if (WO_KI_NB_JI_ventana(owner) != null) a.initOwner(WO_KI_NB_JI_ventana(owner));
        a.showAndWait();
    }

    static void WO_KI_NB_JI_error(Node owner, String texto) {
        Alert a = new Alert(Alert.AlertType.ERROR, texto, ButtonType.OK);
        a.setHeaderText("No se completó la operación"); a.setTitle("Compás");
        if (WO_KI_NB_JI_ventana(owner) != null) a.initOwner(WO_KI_NB_JI_ventana(owner));
        a.showAndWait();
    }

    /** Convierte "15", "15,5" o "15.5" en número; null si está vacío. Lanza NumberFormatException si no es válido. */
    static Double WO_KI_NB_JI_nota(String texto) {
        if (texto == null || texto.isBlank()) return null;
        double v = Double.parseDouble(texto.trim().replace(',', '.'));
        if (!Double.isFinite(v) || v < 0) throw new NumberFormatException(texto);
        return v;
    }
}

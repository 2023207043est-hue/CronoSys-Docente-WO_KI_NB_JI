package com.agendapersonal.ui.modules.docente;

import com.agendapersonal.domain.model.wkniEstadoAsistencia;
import com.agendapersonal.domain.model.wkniEstadoEntrega;
import com.agendapersonal.service.wkniExcelClaseService;
import com.agendapersonal.service.wkniRiesgoEstudianteService;
import com.agendapersonal.service.wkniTablaReporte;
import com.agendapersonal.ui.wkniIconFactory;
import com.agendapersonal.util.wkniUi;
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
final class wkniDocenteUi {
    private wkniDocenteUi() {}

    /** Sufijo CSS de cada estado de asistencia (fila-…, c-…, e-…). */
    static String wkniCss(wkniEstadoAsistencia wkniE) {
        if (wkniE == null) return "sin";
        return switch (wkniE) {
            case PRESENTE -> "presente"; case FALTA -> "falta"; case TARDANZA -> "tardanza"; case JUSTIFICADA -> "justificada";
        };
    }

    /** Color del tema para gráficos y puntos de leyenda. */
    static String wkniColor(wkniEstadoAsistencia wkniE) {
        return switch (wkniE) {
            case PRESENTE -> "-c-green"; case FALTA -> "-danger"; case TARDANZA -> "#E8B43A"; case JUSTIFICADA -> "-c-blue";
        };
    }

    /** Tono de píldora (pill-…) para estados y niveles. */
    static String wkniTono(wkniEstadoAsistencia wkniE) {
        if (wkniE == null) return "slate";
        return switch (wkniE) { case PRESENTE -> "green"; case FALTA -> "danger"; case TARDANZA -> "amber"; case JUSTIFICADA -> "blue"; };
    }

    static String wkniTono(wkniEstadoEntrega wkniE) {
        if (wkniE == null) return "slate";
        return switch (wkniE) { case ENTREGADO -> "green"; case TARDE -> "amber"; case NO_ENTREGADO -> "danger"; };
    }

    static String wkniTono(wkniRiesgoEstudianteService.wkniNivel wkniN) {
        return switch (wkniN) { case BIEN -> "green"; case ATENCION -> "amber"; case ALTO -> "danger"; };
    }

    /** Ícono de advertencia junto al nombre (rojo = riesgo, ámbar = atención) con los motivos en el tooltip. */
    static Node wkniBandera(wkniRiesgoEstudianteService.wkniEvaluacion wkniEv) {
        if (wkniEv == null || wkniEv.wkni_nivel() == wkniRiesgoEstudianteService.wkniNivel.BIEN) return new Region();
        boolean wkniAlto = wkniEv.wkni_nivel() == wkniRiesgoEstudianteService.wkniNivel.ALTO;
        Node wkniIcon = wkniIconFactory.wkniIcon("warning", 17, wkniAlto ? "tint-rose" : "tint-amber");
        StackPane wkniS = new StackPane(wkniIcon);
        wkniS.setMinSize(22, 22);
        Tooltip wkniT = new Tooltip((wkniAlto ? "⚠ En riesgo\n" : "Atención\n") + String.join("\n", wkniEv.wkniMotivos().stream().map(wkniM -> "• " + wkniM).toList()));
        wkniT.setShowDelay(Duration.millis(120));
        wkniT.setShowDuration(Duration.seconds(30));
        Tooltip.install(wkniS, wkniT);
        return wkniS;
    }

    /** Nombre del estudiante con avatar y bandera de riesgo. */
    static HBox wkniNombre(String wkniNombre, wkniRiesgoEstudianteService.wkniEvaluacion wkniEv) {
        Label wkniN = new Label(wkniNombre);
        wkniN.getStyleClass().add("body");
        wkniN.setStyle("-fx-font-weight: bold;");
        HBox wkniBox = new HBox(10, wkniUi.wkniAvatar(wkniNombre, 28), wkniN, wkniBandera(wkniEv));
        wkniBox.setAlignment(Pos.CENTER_LEFT);
        return wkniBox;
    }

    /** Tarjeta de cifra compacta. */
    static VBox wkniMetrica(String wkniTitulo, String wkniValor, String wkni_nota, String wkniIcono, String wkni_tono) {
        return wkniUi.wkniStat(wkniTitulo, wkniValor, wkni_nota, wkniIcono, wkni_tono);
    }

    /** Píldora con un punto de color y una cifra: "● 18 presentes". */
    static HBox wkniConteo(String wkniColorCss, String wkniTexto, String wkniTonoPill) {
        Region wkniPunto = new Region();
        wkniPunto.getStyleClass().add("leyenda-punto");
        wkniPunto.setStyle("-fx-background-color: " + wkniColorCss + ";");
        Label wkniL = new Label(wkniTexto);
        HBox wkniH = new HBox(7, wkniPunto, wkniL);
        wkniH.setAlignment(Pos.CENTER_LEFT);
        wkniH.getStyleClass().addAll("conteo-chip", "chip-" + wkniTonoPill);
        return wkniH;
    }

    /** Barra apilada de proporciones (p. ej. presentes/faltas/tardanzas del día). */
    static HBox wkniBarraApilada(double[] wkniValores, String[] wkniColores, double wkniAlto) {
        HBox wkniBar = new HBox();
        wkniBar.setMinHeight(wkniAlto); wkniBar.setPrefHeight(wkniAlto); wkniBar.setMaxHeight(wkniAlto);
        wkniBar.setStyle("-fx-background-color: -surface3; -fx-background-radius: 99;");
        double wkniTotal = 0;
        for (double wkniV : wkniValores) wkniTotal += wkniV;
        if (wkniTotal <= 0) return wkniBar;
        for (int wkniI = 0; wkniI < wkniValores.length; wkniI++) {
            if (wkniValores[wkniI] <= 0) continue;
            Region wkniSeg = new Region();
            wkniSeg.setStyle("-fx-background-color: " + wkniColores[wkniI] + ";"
                + (wkniI == 0 ? "-fx-background-radius: 99 0 0 99;" : ""));
            wkniSeg.prefWidthProperty().bind(wkniBar.widthProperty().multiply(wkniValores[wkniI] / wkniTotal));
            wkniSeg.setMinWidth(0);
            wkniBar.getChildren().add(wkniSeg);
        }
        return wkniBar;
    }

    /** Encabezado de sección: título y texto de ayuda. */
    static VBox wkniSeccion(String wkniTitulo, String wkniAyuda) {
        Label wkniH = wkniUi.wkniH2(wkniTitulo);
        VBox wkniV = new VBox(4, wkniH);
        if (wkniAyuda != null) { Label wkniA = wkniUi.wkniMuted(wkniAyuda); wkniA.setWrapText(true); wkniV.getChildren().add(wkniA); }
        return wkniV;
    }

    static VBox wkniPanel() {
        VBox wkniV = new VBox(14);
        wkniV.setPadding(new Insets(16, 4, 8, 4));
        return wkniV;
    }

    // ------------------------------------------------------------------ exportar / imprimir

    /** Pide dónde guardar y exporta las tablas a un .xlsx; avisa el resultado. */
    static void wkniExportarExcel(Node wkniOwner, String wkniNombreSugerido, List<wkniTablaReporte> wkniTablas) {
        FileChooser wkniChooser = new FileChooser();
        wkniChooser.setTitle("Exportar a Excel");
        wkniChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Libro de Excel (*.xlsx)", "*.xlsx"));
        wkniChooser.setInitialFileName(wkniArchivo(wkniNombreSugerido) + ".xlsx");
        File wkniCarpeta = new File(System.getProperty("user.home"), "Documents");
        if (wkniCarpeta.isDirectory()) wkniChooser.setInitialDirectory(wkniCarpeta);
        File wkniFile = wkniChooser.showSaveDialog(wkniVentana(wkniOwner));
        if (wkniFile == null) return;
        File wkniDestino = wkniFile.getName().toLowerCase(Locale.ROOT).endsWith(".xlsx") ? wkniFile : new File(wkniFile.getParentFile(), wkniFile.getName() + ".xlsx");
        try {
            wkniExcelClaseService.wkniExportarReporte(wkniDestino.toPath(), wkniTablas);
            Alert wkniA = new Alert(Alert.AlertType.INFORMATION, "Se guardó el archivo:\n" + wkniDestino.getAbsolutePath(),
                new ButtonType("Abrir archivo"), ButtonType.CLOSE);
            wkniA.setHeaderText("Exportado a Excel");
            wkniA.setTitle("Compás");
            if (wkniVentana(wkniOwner) != null) wkniA.initOwner(wkniVentana(wkniOwner));
            wkniA.showAndWait().filter(wkniB -> wkniB.getText().equals("Abrir archivo"))
                .ifPresent(wkniB -> com.agendapersonal.wkniMain.wkniAbrirEnSistema(wkniDestino.toURI().toString()));
        } catch (Exception wkniEx) {
            wkniError(wkniOwner, "No se pudo exportar: " + wkniEx.getMessage() + "\n¿Está el archivo abierto en Excel? Ciérralo e inténtalo de nuevo.");
        }
    }

    static String wkniArchivo(String wkniNombre) { return wkniNombre.replaceAll("[\\\\/:*?\"<>|]", "_").trim(); }

    static Window wkniVentana(Node wkniN) { return wkniN == null || wkniN.getScene() == null ? null : wkniN.getScene().getWindow(); }

    static void wkniInfo(Node wkniOwner, String wkniTexto) {
        Alert wkniA = new Alert(Alert.AlertType.INFORMATION, wkniTexto, ButtonType.OK);
        wkniA.setHeaderText(null); wkniA.setTitle("Compás");
        if (wkniVentana(wkniOwner) != null) wkniA.initOwner(wkniVentana(wkniOwner));
        wkniA.showAndWait();
    }

    static void wkniError(Node wkniOwner, String wkniTexto) {
        Alert wkniA = new Alert(Alert.AlertType.ERROR, wkniTexto, ButtonType.OK);
        wkniA.setHeaderText("No se completó la operación"); wkniA.setTitle("Compás");
        if (wkniVentana(wkniOwner) != null) wkniA.initOwner(wkniVentana(wkniOwner));
        wkniA.showAndWait();
    }

    /** Convierte "15", "15,5" o "15.5" en número; null si está vacío. Lanza NumberFormatException si no es válido. */
    static Double wkniNota(String wkniTexto) {
        if (wkniTexto == null || wkniTexto.isBlank()) return null;
        double wkniV = Double.parseDouble(wkniTexto.trim().replace(',', '.'));
        if (!Double.isFinite(wkniV) || wkniV < 0) throw new NumberFormatException(wkniTexto);
        return wkniV;
    }
}

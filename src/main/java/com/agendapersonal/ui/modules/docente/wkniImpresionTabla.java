package com.agendapersonal.ui.modules.docente;

import com.agendapersonal.service.wkniTablaReporte;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageLayout;
import javafx.print.PageOrientation;
import javafx.print.Paper;
import javafx.print.Printer;
import javafx.print.PrinterJob;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.transform.Scale;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Imprime una {@link TablaReporte} con las mismas filas, columnas y colores que se ven en pantalla.
 * Usa el diálogo de impresión del sistema: eligiendo "Microsoft Print to PDF" se guarda como PDF.
 * Las páginas van en horizontal, se escalan al ancho y las filas se reparten en varias hojas.
 */
final class wkniImpresionTabla {
    private static final double wkniALTO_FILA = 22;
    private static final double wkniALTO_CABECERA = 70;
    private static final double wkniALTO_PIE = 22;

    private wkniImpresionTabla() {}

    static void wkniImprimir(Node wkniOwner, wkniTablaReporte wkniTabla) {
        if (Printer.getDefaultPrinter() == null && Printer.getAllPrinters().isEmpty()) {
            wkniDocenteUi.wkniInfo(wkniOwner, "No hay impresoras instaladas. En Windows puedes agregar «Microsoft Print to PDF» para guardar como PDF.");
            return;
        }
        PrinterJob wkniJob = PrinterJob.createPrinterJob();
        if (wkniJob == null) { wkniDocenteUi.wkniError(wkniOwner, "No se pudo iniciar la impresión."); return; }
        if (!wkniJob.showPrintDialog(wkniDocenteUi.wkniVentana(wkniOwner))) { wkniJob.cancelJob(); return; }
        Printer wkniPrinter = wkniJob.getPrinter();
        PageLayout wkniLayout = wkniPrinter.createPageLayout(Paper.A4, PageOrientation.LANDSCAPE, Printer.MarginType.DEFAULT);
        wkniJob.getJobSettings().setPageLayout(wkniLayout);

        double[] wkniAnchos = wkniAnchos(wkniTabla);
        double wkniAnchoTabla = 0;
        for (double wkniA : wkniAnchos) wkniAnchoTabla += wkniA;
        double wkniEscala = Math.min(1, wkniLayout.getPrintableWidth() / wkniAnchoTabla);
        int wkniFilasPorPagina = Math.max(1, (int) ((wkniLayout.getPrintableHeight() / wkniEscala - wkniALTO_CABECERA - wkniALTO_PIE) / wkniALTO_FILA) - 1);

        List<List<List<wkniTablaReporte.wkniCelda>>> wkniPaginas = new ArrayList<>();
        for (int wkniI = 0; wkniI < Math.max(1, wkniTabla.wkniFilas().size()); wkniI += wkniFilasPorPagina)
            wkniPaginas.add(wkniTabla.wkniFilas().subList(Math.min(wkniI, wkniTabla.wkniFilas().size()), Math.min(wkniI + wkniFilasPorPagina, wkniTabla.wkniFilas().size())));

        boolean wkniOk = true;
        for (int wkni_p = 0; wkni_p < wkniPaginas.size() && wkniOk; wkni_p++) {
            Node wkniPagina = wkniPagina(wkniTabla, wkniPaginas.get(wkni_p), wkniAnchos, wkni_p + 1, wkniPaginas.size());
            wkniPagina.getTransforms().add(new Scale(wkniEscala, wkniEscala));
            new Scene(new Group(wkniPagina));   // necesario para que se apliquen tamaños y estilos antes de imprimir
            wkniPagina.applyCss();
            wkniOk = wkniJob.printPage(wkniLayout, wkniPagina);
        }
        if (wkniOk) wkniJob.endJob(); else { wkniJob.cancelJob(); wkniDocenteUi.wkniError(wkniOwner, "La impresora rechazó el trabajo."); }
    }

    /**
     * Imprime páginas ya armadas (una hoja cada una), escaladas para caber completas en la hoja.
     * Se usa para los reportes por estudiante.
     */
    static void wkniImprimirPaginas(Node wkniOwner, List<Node> wkniPaginas, PageOrientation wkniOrientacion) {
        if (wkniPaginas.isEmpty()) return;
        if (Printer.getDefaultPrinter() == null && Printer.getAllPrinters().isEmpty()) {
            wkniDocenteUi.wkniInfo(wkniOwner, "No hay impresoras instaladas. En Windows puedes agregar «Microsoft Print to PDF» para guardar como PDF.");
            return;
        }
        PrinterJob wkniJob = PrinterJob.createPrinterJob();
        if (wkniJob == null) { wkniDocenteUi.wkniError(wkniOwner, "No se pudo iniciar la impresión."); return; }
        if (!wkniJob.showPrintDialog(wkniDocenteUi.wkniVentana(wkniOwner))) { wkniJob.cancelJob(); return; }
        PageLayout wkniLayout = wkniJob.getPrinter().createPageLayout(Paper.A4, wkniOrientacion, Printer.MarginType.DEFAULT);
        wkniJob.getJobSettings().setPageLayout(wkniLayout);
        boolean wkniOk = true;
        for (int wkniI = 0; wkniI < wkniPaginas.size() && wkniOk; wkniI++) {
            Node wkni_p = wkniPaginas.get(wkniI);
            if (wkni_p.getScene() == null) new Scene(new Group(wkni_p));
            wkni_p.applyCss();
            if (wkni_p instanceof javafx.scene.Parent wkniPar) wkniPar.layout();
            double wkniW = wkni_p.prefWidth(-1), wkniH = wkni_p.prefHeight(wkniW);
            double wkniEscala = Math.min(1, Math.min(wkniLayout.getPrintableWidth() / wkniW, wkniLayout.getPrintableHeight() / wkniH));
            Scale wkniS = new Scale(wkniEscala, wkniEscala);
            wkni_p.getTransforms().add(wkniS);
            wkniOk = wkniJob.printPage(wkniLayout, wkni_p);
            wkni_p.getTransforms().remove(wkniS);
        }
        if (wkniOk) wkniJob.endJob(); else { wkniJob.cancelJob(); wkniDocenteUi.wkniError(wkniOwner, "La impresora rechazó el trabajo."); }
    }

    private static Node wkniPagina(wkniTablaReporte wkniT, List<List<wkniTablaReporte.wkniCelda>> wkniFilas, double[] wkniAnchos, int wkniNum, int wkniTotal) {
        Label wkniTitulo = new Label(wkniT.wkniTitulo());
        wkniTitulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1C1F26;");
        Label wkniSub = new Label(wkniT.wkniSubtitulo() == null ? "" : wkniT.wkniSubtitulo());
        wkniSub.setStyle("-fx-font-size: 10px; -fx-text-fill: #555A66;");
        GridPane wkniG = new GridPane();
        wkniG.setStyle("-fx-background-color: white;");
        for (int wkniC = 0; wkniC < wkniT.wkniEncabezados().size(); wkniC++)
            wkniG.add(wkniCelda(wkniT.wkniEncabezados().get(wkniC), wkniAnchos[wkniC], "#E9E7E1", true, wkniC == 0 ? Pos.CENTER_LEFT : Pos.CENTER), wkniC, 0);
        for (int wkniR = 0; wkniR < wkniFilas.size(); wkniR++) {
            List<wkniTablaReporte.wkniCelda> wkniFila = wkniFilas.get(wkniR);
            for (int wkniC = 0; wkniC < wkniFila.size() && wkniC < wkniAnchos.length; wkniC++) {
                wkniTablaReporte.wkniCelda wkniCel = wkniFila.get(wkniC);
                String wkniFondo = wkniFondo(wkniCel.wkni_tono(), wkniR);
                wkniG.add(wkniCelda(wkniCel.wkniTexto(), wkniAnchos[wkniC], wkniFondo, wkniCel.wkni_tono() != wkniTablaReporte.wkniTono.NINGUNO,
                    wkniC == 0 || wkniCel.wkniTexto().length() > 18 ? Pos.CENTER_LEFT : Pos.CENTER), wkniC, wkniR + 1);
            }
        }
        Label wkniPie = new Label("Compás · Espacio Docente · impreso el "
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "   —   página " + wkniNum + " de " + wkniTotal);
        wkniPie.setStyle("-fx-font-size: 9px; -fx-text-fill: #646978;");
        VBox wkniV = new VBox(6, wkniTitulo, wkniSub, wkniG, wkniPie);
        wkniV.setPadding(new Insets(4));
        wkniV.setStyle("-fx-background-color: white;");
        return wkniV;
    }

    private static Label wkniCelda(String wkniTexto, double wkniAncho, String wkniFondo, boolean wkniNegrita, Pos wkniAlineacion) {
        Label wkniL = new Label(wkniTexto == null ? "" : wkniTexto);
        wkniL.setMinSize(wkniAncho, wkniALTO_FILA); wkniL.setPrefSize(wkniAncho, wkniALTO_FILA); wkniL.setMaxSize(wkniAncho, wkniALTO_FILA);
        wkniL.setAlignment(wkniAlineacion);
        wkniL.setPadding(new Insets(0, 5, 0, 5));
        wkniL.setStyle("-fx-font-size: 10px; -fx-text-fill: #1C1F26; -fx-background-color: " + wkniFondo + ";"
            + "-fx-border-color: #C9C6BE; -fx-border-width: 0.5;" + (wkniNegrita ? "-fx-font-weight: bold;" : ""));
        return wkniL;
    }

    private static String wkniFondo(wkniTablaReporte.wkniTono wkni_tono, int wkniFila) {
        return switch (wkni_tono) {
            case VERDE -> "#C6EFCE"; case ROJO -> "#FFC7CE"; case AMARILLO -> "#FFEB9C";
            case AZUL -> "#DDEBF7"; case GRIS -> "#F2F2F2";
            default -> wkniFila % 2 == 0 ? "white" : "#FAFAF8";
        };
    }

    /** Ancho de cada columna según su texto más largo (con límites). */
    private static double[] wkniAnchos(wkniTablaReporte wkniT) {
        int wkniN = wkniT.wkniEncabezados().size();
        double[] wkniA = new double[wkniN];
        for (int wkniC = 0; wkniC < wkniN; wkniC++) wkniA[wkniC] = wkniT.wkniEncabezados().get(wkniC).length();
        for (List<wkniTablaReporte.wkniCelda> wkniF : wkniT.wkniFilas())
            for (int wkniC = 0; wkniC < Math.min(wkniN, wkniF.size()); wkniC++) wkniA[wkniC] = Math.max(wkniA[wkniC], wkniF.get(wkniC).wkniTexto().length());
        for (int wkniC = 0; wkniC < wkniN; wkniC++) wkniA[wkniC] = Math.max(wkniC == 0 ? 160 : 30, Math.min(wkniC == 0 ? 240 : 320, wkniA[wkniC] * 6.2 + 12));
        return wkniA;
    }
}

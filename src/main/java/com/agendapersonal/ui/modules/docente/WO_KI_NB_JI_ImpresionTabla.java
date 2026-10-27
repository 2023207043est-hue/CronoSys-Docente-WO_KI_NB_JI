package com.agendapersonal.ui.modules.docente;

import com.agendapersonal.service.WO_KI_NB_JI_TablaReporte;
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
final class WO_KI_NB_JI_ImpresionTabla {
    private static final double ALTO_FILA = 22;
    private static final double ALTO_CABECERA = 70;
    private static final double ALTO_PIE = 22;

    private WO_KI_NB_JI_ImpresionTabla() {}

    static void WO_KI_NB_JI_imprimir(Node owner, WO_KI_NB_JI_TablaReporte tabla) {
        if (Printer.getDefaultPrinter() == null && Printer.getAllPrinters().isEmpty()) {
            WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_info(owner, "No hay impresoras instaladas. En Windows puedes agregar «Microsoft Print to PDF» para guardar como PDF.");
            return;
        }
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) { WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_error(owner, "No se pudo iniciar la impresión."); return; }
        if (!job.showPrintDialog(WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_ventana(owner))) { job.cancelJob(); return; }
        Printer printer = job.getPrinter();
        PageLayout layout = printer.createPageLayout(Paper.A4, PageOrientation.LANDSCAPE, Printer.MarginType.DEFAULT);
        job.getJobSettings().setPageLayout(layout);

        double[] anchos = WO_KI_NB_JI_anchos(tabla);
        double anchoTabla = 0;
        for (double a : anchos) anchoTabla += a;
        double escala = Math.min(1, layout.getPrintableWidth() / anchoTabla);
        int filasPorPagina = Math.max(1, (int) ((layout.getPrintableHeight() / escala - ALTO_CABECERA - ALTO_PIE) / ALTO_FILA) - 1);

        List<List<List<WO_KI_NB_JI_TablaReporte.WO_KI_NB_JI_Celda>>> paginas = new ArrayList<>();
        for (int i = 0; i < Math.max(1, tabla.filas().size()); i += filasPorPagina)
            paginas.add(tabla.filas().subList(Math.min(i, tabla.filas().size()), Math.min(i + filasPorPagina, tabla.filas().size())));

        boolean ok = true;
        for (int p = 0; p < paginas.size() && ok; p++) {
            Node pagina = WO_KI_NB_JI_pagina(tabla, paginas.get(p), anchos, p + 1, paginas.size());
            pagina.getTransforms().add(new Scale(escala, escala));
            new Scene(new Group(pagina));   // necesario para que se apliquen tamaños y estilos antes de imprimir
            pagina.applyCss();
            ok = job.printPage(layout, pagina);
        }
        if (ok) job.endJob(); else { job.cancelJob(); WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_error(owner, "La impresora rechazó el trabajo."); }
    }

    /**
     * Imprime páginas ya armadas (una hoja cada una), escaladas para caber completas en la hoja.
     * Se usa para los reportes por estudiante.
     */
    static void WO_KI_NB_JI_imprimirPaginas(Node owner, List<Node> paginas, PageOrientation orientacion) {
        if (paginas.isEmpty()) return;
        if (Printer.getDefaultPrinter() == null && Printer.getAllPrinters().isEmpty()) {
            WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_info(owner, "No hay impresoras instaladas. En Windows puedes agregar «Microsoft Print to PDF» para guardar como PDF.");
            return;
        }
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) { WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_error(owner, "No se pudo iniciar la impresión."); return; }
        if (!job.showPrintDialog(WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_ventana(owner))) { job.cancelJob(); return; }
        PageLayout layout = job.getPrinter().createPageLayout(Paper.A4, orientacion, Printer.MarginType.DEFAULT);
        job.getJobSettings().setPageLayout(layout);
        boolean ok = true;
        for (int i = 0; i < paginas.size() && ok; i++) {
            Node p = paginas.get(i);
            if (p.getScene() == null) new Scene(new Group(p));
            p.applyCss();
            if (p instanceof javafx.scene.Parent par) par.layout();
            double w = p.prefWidth(-1), h = p.prefHeight(w);
            double escala = Math.min(1, Math.min(layout.getPrintableWidth() / w, layout.getPrintableHeight() / h));
            Scale s = new Scale(escala, escala);
            p.getTransforms().add(s);
            ok = job.printPage(layout, p);
            p.getTransforms().remove(s);
        }
        if (ok) job.endJob(); else { job.cancelJob(); WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_error(owner, "La impresora rechazó el trabajo."); }
    }

    private static Node WO_KI_NB_JI_pagina(WO_KI_NB_JI_TablaReporte t, List<List<WO_KI_NB_JI_TablaReporte.WO_KI_NB_JI_Celda>> filas, double[] anchos, int num, int total) {
        Label titulo = new Label(t.titulo());
        titulo.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1C1F26;");
        Label sub = new Label(t.subtitulo() == null ? "" : t.subtitulo());
        sub.setStyle("-fx-font-size: 10px; -fx-text-fill: #555A66;");
        GridPane g = new GridPane();
        g.setStyle("-fx-background-color: white;");
        for (int c = 0; c < t.encabezados().size(); c++)
            g.add(WO_KI_NB_JI_celda(t.encabezados().get(c), anchos[c], "#E9E7E1", true, c == 0 ? Pos.CENTER_LEFT : Pos.CENTER), c, 0);
        for (int r = 0; r < filas.size(); r++) {
            List<WO_KI_NB_JI_TablaReporte.WO_KI_NB_JI_Celda> fila = filas.get(r);
            for (int c = 0; c < fila.size() && c < anchos.length; c++) {
                WO_KI_NB_JI_TablaReporte.WO_KI_NB_JI_Celda cel = fila.get(c);
                String fondo = WO_KI_NB_JI_fondo(cel.tono(), r);
                g.add(WO_KI_NB_JI_celda(cel.texto(), anchos[c], fondo, cel.tono() != WO_KI_NB_JI_TablaReporte.WO_KI_NB_JI_Tono.NINGUNO,
                    c == 0 || cel.texto().length() > 18 ? Pos.CENTER_LEFT : Pos.CENTER), c, r + 1);
            }
        }
        Label pie = new Label("Compás · Espacio Docente · impreso el "
            + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) + "   —   página " + num + " de " + total);
        pie.setStyle("-fx-font-size: 9px; -fx-text-fill: #646978;");
        VBox v = new VBox(6, titulo, sub, g, pie);
        v.setPadding(new Insets(4));
        v.setStyle("-fx-background-color: white;");
        return v;
    }

    private static Label WO_KI_NB_JI_celda(String texto, double ancho, String fondo, boolean negrita, Pos alineacion) {
        Label l = new Label(texto == null ? "" : texto);
        l.setMinSize(ancho, ALTO_FILA); l.setPrefSize(ancho, ALTO_FILA); l.setMaxSize(ancho, ALTO_FILA);
        l.setAlignment(alineacion);
        l.setPadding(new Insets(0, 5, 0, 5));
        l.setStyle("-fx-font-size: 10px; -fx-text-fill: #1C1F26; -fx-background-color: " + fondo + ";"
            + "-fx-border-color: #C9C6BE; -fx-border-width: 0.5;" + (negrita ? "-fx-font-weight: bold;" : ""));
        return l;
    }

    private static String WO_KI_NB_JI_fondo(WO_KI_NB_JI_TablaReporte.WO_KI_NB_JI_Tono tono, int fila) {
        return switch (tono) {
            case VERDE -> "#C6EFCE"; case ROJO -> "#FFC7CE"; case AMARILLO -> "#FFEB9C";
            case AZUL -> "#DDEBF7"; case GRIS -> "#F2F2F2";
            default -> fila % 2 == 0 ? "white" : "#FAFAF8";
        };
    }

    /** Ancho de cada columna según su texto más largo (con límites). */
    private static double[] WO_KI_NB_JI_anchos(WO_KI_NB_JI_TablaReporte t) {
        int n = t.encabezados().size();
        double[] a = new double[n];
        for (int c = 0; c < n; c++) a[c] = t.encabezados().get(c).length();
        for (List<WO_KI_NB_JI_TablaReporte.WO_KI_NB_JI_Celda> f : t.filas())
            for (int c = 0; c < Math.min(n, f.size()); c++) a[c] = Math.max(a[c], f.get(c).texto().length());
        for (int c = 0; c < n; c++) a[c] = Math.max(c == 0 ? 160 : 30, Math.min(c == 0 ? 240 : 320, a[c] * 6.2 + 12));
        return a;
    }
}

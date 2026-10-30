package com.agendapersonal.ui.modules.docente;

import com.agendapersonal.domain.model.wkniEstadoEntrega;
import com.agendapersonal.domain.model.wkniEstudianteClase;
import com.agendapersonal.service.wkniReporteEstudiante;
import com.agendapersonal.service.wkniReportesDocente;
import com.agendapersonal.service.wkniResumenCurso;
import com.agendapersonal.service.wkniRiesgoEstudianteService;
import com.agendapersonal.ui.wkniLogo;
import com.agendapersonal.ui.wkniThemeService;
import com.agendapersonal.util.wkniUi;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.print.PageOrientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Reporte individual para el apoderado o el tutor: una hoja A4 con asistencia, trabajos, notas, alertas,
 * observaciones, sugerencias y espacio para firmas. Se ve en una vista previa (con anterior/siguiente para
 * recorrer el curso) y se imprime o guarda como PDF. Usa colores fijos de papel, no los del tema.
 */
final class wkniReporteEstudianteView {
    private static final DateTimeFormatter wkniFECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter wkniDIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final double wkniANCHO = 760;
    private static final String wkniTEXTO = "#1C1F26", wkniSUAVE = "#5B606D", wkniBORDE = "#D9D6CE";

    private wkniReporteEstudianteView() {}

    /** Abre la vista previa del reporte de un estudiante; con las flechas se pasa al siguiente del curso. */
    static void wkniMostrar(Node wkniOwner, wkniCursoContexto wkniCtx, wkniEstudianteClase wkniInicial) {
        wkniResumenCurso wkniR = wkniCtx.wkniResumen();
        List<wkniEstudianteClase> wkniLista = wkniR.wkniEstudiantes();
        if (wkniLista.isEmpty()) { wkniDocenteUi.wkniInfo(wkniOwner, "Este curso aún no tiene estudiantes."); return; }
        int[] wkniI = {Math.max(0, wkniIndice(wkniLista, wkniInicial))};

        Stage wkniSt = new Stage();
        Window wkniW = wkniDocenteUi.wkniVentana(wkniOwner);
        if (wkniW != null) { wkniSt.initOwner(wkniW); wkniSt.initModality(Modality.WINDOW_MODAL); }
        wkniLogo.wkniAplicarIconos(wkniSt);
        StackPane wkniHoja = new StackPane();
        wkniHoja.setPadding(new Insets(24));
        wkniHoja.setStyle("-fx-background-color: -surface2;");
        ScrollPane wkniScroll = new ScrollPane(wkniHoja);
        wkniScroll.setFitToWidth(true);
        Label wkniPos = wkniUi.wkniMuted("");
        wkniPos.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        Button wkniAntes = wkniUi.wkniSecondary("chevron-left", "Anterior");
        Button wkniDespues = wkniUi.wkniSecondary("chevron-right", "Siguiente");
        Runnable wkniPintar = () -> {
            wkniEstudianteClase wkniE = wkniLista.get(wkniI[0]);
            VBox wkniPg = wkniPagina(wkniReporteEstudiante.wkniDe(wkniR, wkniE, LocalDate.now()));
            wkniPg.setStyle(wkniPg.getStyle() + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 14, 0.1, 0, 3);");   // solo en pantalla
            wkniHoja.getChildren().setAll(wkniPg);
            wkniSt.setTitle("Reporte de " + wkniE.wkniNombre() + " — " + wkniCtx.wkniClase());
            wkniPos.setText((wkniI[0] + 1) + " de " + wkniLista.size());
            wkniAntes.setDisable(wkniI[0] == 0);
            wkniDespues.setDisable(wkniI[0] == wkniLista.size() - 1);
            wkniScroll.setVvalue(0);
        };
        wkniAntes.setOnAction(wkniE -> { wkniI[0]--; wkniPintar.run(); });
        wkniDespues.setOnAction(wkniE -> { wkniI[0]++; wkniPintar.run(); });
        Button wkniImprimir = wkniUi.wkniPrimary("print", "Imprimir o guardar PDF");
        wkniImprimir.setOnAction(wkniE -> wkniImpresionTabla.wkniImprimirPaginas(wkniHoja,
            List.of(wkniPagina(wkniReporteEstudiante.wkniDe(wkniR, wkniLista.get(wkniI[0]), LocalDate.now()))), PageOrientation.PORTRAIT));
        Button wkniTodos = wkniUi.wkniSecondary("copy", "Imprimir todo el curso");
        wkniTodos.setOnAction(wkniE -> wkniImprimirCurso(wkniHoja, wkniCtx));
        Button wkniCerrar = wkniUi.wkniGhost("close", "Cerrar");
        wkniCerrar.setOnAction(wkniE -> wkniSt.close());
        HBox wkniBarra = new HBox(10, wkniAntes, wkniPos, wkniDespues, wkniUi.wkniSpacer(), wkniTodos, wkniImprimir, wkniCerrar);
        wkniBarra.setAlignment(Pos.CENTER_LEFT);
        wkniBarra.setPadding(new Insets(12, 16, 12, 16));
        wkniBarra.getStyleClass().add("card");
        VBox wkniRaiz = new VBox(wkniBarra, wkniScroll);
        VBox.setVgrow(wkniScroll, Priority.ALWAYS);
        wkniRaiz.getStyleClass().add("app-root");
        Scene wkniSc = new Scene(wkniRaiz, wkniANCHO + 110, 860);
        wkniThemeService.wkniGet().wkniApplyTo(wkniSc);
        wkniSt.setScene(wkniSc);
        wkniSc.setOnKeyPressed(wkniK -> {
            switch (wkniK.getCode()) {
                case LEFT, PAGE_UP -> { if (!wkniAntes.isDisabled()) wkniAntes.fire(); }
                case RIGHT, PAGE_DOWN -> { if (!wkniDespues.isDisabled()) wkniDespues.fire(); }
                case ESCAPE -> wkniSt.close();
                default -> { }
            }
        });
        wkniPintar.run();
        wkniSt.show();
    }

    /** Un reporte por hoja para todos los estudiantes (reunión de padres, entrega de libretas). */
    static void wkniImprimirCurso(Node wkniOwner, wkniCursoContexto wkniCtx) {
        wkniResumenCurso wkniR = wkniCtx.wkniResumen();
        if (wkniR.wkniEstudiantes().isEmpty()) { wkniDocenteUi.wkniInfo(wkniOwner, "Este curso aún no tiene estudiantes."); return; }
        List<Node> wkniPaginas = new ArrayList<>();
        for (wkniEstudianteClase wkniE : wkniR.wkniEstudiantes()) wkniPaginas.add(wkniPagina(wkniReporteEstudiante.wkniDe(wkniR, wkniE, LocalDate.now())));
        wkniImpresionTabla.wkniImprimirPaginas(wkniOwner, wkniPaginas, PageOrientation.PORTRAIT);
    }

    private static int wkniIndice(List<wkniEstudianteClase> wkniL, wkniEstudianteClase wkniE) {
        if (wkniE == null) return 0;
        for (int wkniK = 0; wkniK < wkniL.size(); wkniK++) if (wkniL.get(wkniK).wkniId() == wkniE.wkniId()) return wkniK;
        return 0;
    }

    // ------------------------------------------------------------------ hoja

    static VBox wkniPagina(wkniReporteEstudiante wkniRep) {
        wkniRiesgoEstudianteService.wkniEvaluacion wkniEv = wkniRep.wkni_evaluacion();
        VBox wkni_p = new VBox(16);
        wkni_p.setPadding(new Insets(34, 40, 30, 40));
        wkni_p.setPrefWidth(wkniANCHO); wkni_p.setMinWidth(wkniANCHO); wkni_p.setMaxWidth(wkniANCHO);
        wkni_p.setStyle("-fx-background-color: white;");

        // Encabezado
        Label wkni_tipo = wkniTxt("REPORTE DEL ESTUDIANTE", 11, true, wkniSUAVE);
        Label wkniNombre = wkniTxt(wkniRep.wkniEstudiante().wkniNombre(), 24, true, wkniTEXTO);
        wkniNombre.setWrapText(true);
        Label wkniCurso = wkniTxt(wkniRep.wkniCurso() + (wkniRep.wkniPeriodo() == null ? "" : " · " + wkniRep.wkniPeriodo()), 13, false, wkniSUAVE);
        VBox wkniIzq = new VBox(3, wkni_tipo, wkniNombre, wkniCurso);
        HBox.setHgrow(wkniIzq, Priority.ALWAYS);
        String[] wkniCol = wkniColorNivel(wkniEv.wkni_nivel());
        Label wkni_nivel = wkniTxt(wkniEv.wkni_nivel().wkniEtiqueta(), 13, true, wkniCol[0]);
        wkni_nivel.setStyle(wkni_nivel.getStyle() + "-fx-background-color:" + wkniCol[1] + "; -fx-background-radius: 99; -fx-padding: 5 14 5 14;");
        VBox wkniDer = new VBox(6, wkni_nivel, wkniTxt("Emitido el " + LocalDate.now().format(wkniFECHA), 11, false, wkniSUAVE));
        wkniDer.setAlignment(Pos.TOP_RIGHT);
        HBox wkniCab = new HBox(16, wkniIzq, wkniDer);
        Region wkniLinea = new Region();
        wkniLinea.setStyle("-fx-background-color: #C64B1C; -fx-min-height: 3; -fx-pref-height: 3; -fx-max-height: 3;");

        // Cifras
        double wkni_nota = wkniEv.wkni_nota() == null ? Double.NaN : wkniEv.wkni_nota();
        HBox wkniCifras = new HBox(10,
            wkniCifra("Asistencia", wkniReportesDocente.wkniPct(wkniRep.wkniPorcentajeAsistencia()), wkniEv.wkniSesiones() + " clases registradas",
                wkniColorPct(wkniRep.wkniPorcentajeAsistencia())),
            wkniCifra("Faltas", Integer.toString(wkniRep.wkniFaltas()), wkniRep.wkniJustificadas() + (wkniRep.wkniJustificadas() == 1 ? " justificada" : " justificadas"), wkniRep.wkniFaltas() > 0 ? "#B3261E" : wkniTEXTO),
            wkniCifra("Tardanzas", Integer.toString(wkniRep.wkniTardanzas()), "llegadas tarde", wkniRep.wkniTardanzas() >= 3 ? "#8A5A00" : wkniTEXTO),
            wkniCifra("Nota", Double.isNaN(wkni_nota) ? "—" : wkniRiesgoEstudianteService.wkniNumero(Math.round(wkni_nota * 10) / 10.0),
                "mínima aprobatoria " + wkniRiesgoEstudianteService.wkniNumero(wkniRep.wkniNotaMinima()),
                Double.isNaN(wkni_nota) ? wkniTEXTO : wkni_nota < wkniRep.wkniNotaMinima() ? "#B3261E" : "#2C7A4B"));

        wkni_p.getChildren().addAll(wkniCab, wkniLinea, wkniCifras, wkniAlertas(wkniRep), wkniAsistencia(wkniRep), wkniTrabajos(wkniRep), wkniNotasDocente(wkniRep), wkniFirmas());
        return wkni_p;
    }

    private static Node wkniAlertas(wkniReporteEstudiante wkniRep) {
        wkniRiesgoEstudianteService.wkniEvaluacion wkniEv = wkniRep.wkni_evaluacion();
        String[] wkniC = wkniColorNivel(wkniEv.wkni_nivel());
        VBox wkniBox = new VBox(6, wkniTxt(wkniEv.wkniMotivos().isEmpty() ? "Sin alertas" : "Puntos de atención", 13, true, wkniC[0]));
        for (String wkniM : wkniEv.wkniMotivos()) wkniBox.getChildren().add(wkniTxt("•  " + wkniM, 12, false, wkniTEXTO));
        if (wkniEv.wkniMotivos().isEmpty()) wkniBox.getChildren().add(wkniTxt("El estudiante no presenta faltas seguidas, trabajos pendientes ni notas bajo la mínima.", 12, false, wkniTEXTO));
        wkniBox.setStyle("-fx-background-color:" + wkniC[1] + "; -fx-border-color:" + wkniC[0] + "; -fx-border-width: 0 0 0 4; -fx-padding: 10 14 10 14;");
        return wkniBox;
    }

    private static Node wkniAsistencia(wkniReporteEstudiante wkniRep) {
        VBox wkniBox = new VBox(8, wkniSeccion("Asistencia"));
        int wkniTotal = wkniRep.wkniPresentes() + wkniRep.wkniFaltas() + wkniRep.wkniTardanzas() + wkniRep.wkniJustificadas();
        if (wkniTotal == 0) { wkniBox.getChildren().add(wkniTxt("Aún no hay clases registradas.", 12, false, wkniSUAVE)); return wkniBox; }
        HBox wkniBarra = new HBox();
        wkniBarra.setPrefHeight(10); wkniBarra.setMaxWidth(Double.MAX_VALUE);
        int[] wkniV = {wkniRep.wkniPresentes(), wkniRep.wkniTardanzas(), wkniRep.wkniJustificadas(), wkniRep.wkniFaltas()};
        String[] wkniCs = {"#2C7A4B", "#E8B43A", "#2F5BB7", "#B3261E"};
        for (int wkniK = 0; wkniK < wkniV.length; wkniK++) {
            if (wkniV[wkniK] == 0) continue;
            Region wkniSeg = new Region();
            wkniSeg.setStyle("-fx-background-color:" + wkniCs[wkniK] + ";");
            wkniSeg.prefWidthProperty().bind(wkniBarra.widthProperty().multiply((double) wkniV[wkniK] / wkniTotal));
            wkniBarra.getChildren().add(wkniSeg);
        }
        HBox wkniLey = new HBox(16, wkniLeyenda("#2C7A4B", wkniRep.wkniPresentes() + " presente(s)"), wkniLeyenda("#B3261E", wkniRep.wkniFaltas() + " falta(s)"),
            wkniLeyenda("#E8B43A", wkniRep.wkniTardanzas() + " tardanza(s)"), wkniLeyenda("#2F5BB7", wkniRep.wkniJustificadas() + " justificada(s)"));
        FlowPane wkniDias = new FlowPane(5, 5);
        for (var wkniEn : wkniRep.wkniHistorial().entrySet()) {
            Label wkniD = wkniTxt(wkniEn.getKey().format(wkniDIA) + " " + wkniEn.getValue().wkniLetra(), 10, true, "white");
            String wkniFondo = switch (wkniEn.getValue()) {
                case PRESENTE -> "#2C7A4B"; case FALTA -> "#B3261E"; case TARDANZA -> "#C99418"; case JUSTIFICADA -> "#2F5BB7";
            };
            wkniD.setStyle(wkniD.getStyle() + "-fx-background-color:" + wkniFondo + "; -fx-background-radius: 5; -fx-padding: 2 6 2 6;");
            wkniDias.getChildren().add(wkniD);
        }
        wkniBox.getChildren().addAll(wkniBarra, wkniLey, wkniTxt("Detalle por fecha (P presente · F falta · T tardanza · J justificada):", 11, false, wkniSUAVE), wkniDias);
        return wkniBox;
    }

    private static Node wkniTrabajos(wkniReporteEstudiante wkniRep) {
        VBox wkniBox = new VBox(8, wkniSeccion("Trabajos y evaluaciones"));
        if (wkniRep.wkniTrabajos().isEmpty()) { wkniBox.getChildren().add(wkniTxt("No hay trabajos registrados en el curso.", 12, false, wkniSUAVE)); return wkniBox; }
        GridPane wkniG = new GridPane();
        String[] wkniEnc = {"Trabajo", "Tipo", "Fecha", "Entrega", "Nota"};
        double[] wkniAn = {300, 100, 80, 120, 80};
        for (int wkniC = 0; wkniC < wkniEnc.length; wkniC++) wkniG.add(wkniCelda(wkniEnc[wkniC], wkniAn[wkniC], "#EFEDE7", true, wkniTEXTO), wkniC, 0);
        int wkniF = 1;
        for (wkniReporteEstudiante.wkniTrabajo wkniT : wkniRep.wkniTrabajos()) {
            String wkni_entrega; String wkniColor;
            if (wkniT.wkniEstado() == null) { wkni_entrega = wkniT.wkniVencido() ? "Sin registrar" : "Próximo"; wkniColor = wkniSUAVE; }
            else {
                wkni_entrega = wkniT.wkniEstado().wkniSimbolo() + " " + wkniT.wkniEstado().wkniValor();
                wkniColor = wkniT.wkniEstado() == wkniEstadoEntrega.ENTREGADO ? "#2C7A4B" : wkniT.wkniEstado() == wkniEstadoEntrega.TARDE ? "#8A5A00" : "#B3261E";
            }
            String wkniFondo = wkniF % 2 == 0 ? "#FAFAF8" : "white";
            wkniG.add(wkniCelda(wkniT.wkniTitulo(), wkniAn[0], wkniFondo, false, wkniTEXTO), 0, wkniF);
            wkniG.add(wkniCelda(wkniT.wkni_tipo(), wkniAn[1], wkniFondo, false, wkniSUAVE), 1, wkniF);
            wkniG.add(wkniCelda(wkniT.wkni_fecha() == null ? "—" : wkniT.wkni_fecha().format(wkniDIA), wkniAn[2], wkniFondo, false, wkniSUAVE), 2, wkniF);
            wkniG.add(wkniCelda(wkni_entrega, wkniAn[3], wkniFondo, true, wkniColor), 3, wkniF);
            boolean wkniBaja = wkniT.wkni_nota() != null && wkniT.wkni_nota() < wkniRep.wkniNotaMinima();
            wkniG.add(wkniCelda(wkniT.wkni_nota() == null ? "—" : wkniRiesgoEstudianteService.wkniNumero(wkniT.wkni_nota()), wkniAn[4], wkniFondo, true, wkniBaja ? "#B3261E" : wkniTEXTO), 4, wkniF);
            wkniF++;
        }
        wkniBox.getChildren().add(wkniG);
        if (!Double.isNaN(wkniRep.wkniPromedioTrabajos()))
            wkniBox.getChildren().add(wkniTxt("Promedio de trabajos: " + wkniRiesgoEstudianteService.wkniNumero(Math.round(wkniRep.wkniPromedioTrabajos() * 10) / 10.0)
                + (wkniRep.wkniEstudiante().wkniCalificacion() == null ? "" : "   ·   Nota del curso: " + wkniRiesgoEstudianteService.wkniNumero(wkniRep.wkniEstudiante().wkniCalificacion())),
                12, true, wkniTEXTO));
        return wkniBox;
    }

    private static Node wkniNotasDocente(wkniReporteEstudiante wkniRep) {
        VBox wkniBox = new VBox(6, wkniSeccion("Observaciones y sugerencias"));
        String wkniObs = wkniRep.wkniEstudiante().wkniObservaciones();
        if (wkniObs != null && !wkniObs.isBlank()) {
            Label wkniO = wkniTxt("Observación del docente: " + wkniObs, 12, false, wkniTEXTO);
            wkniO.setWrapText(true);
            wkniBox.getChildren().add(wkniO);
        }
        for (String wkniS : wkniRep.wkniSugerencias()) {
            Label wkniL = wkniTxt("→  " + wkniS, 12, false, wkniTEXTO);
            wkniL.setWrapText(true);
            wkniBox.getChildren().add(wkniL);
        }
        // Espacio para que el docente escriba a mano.
        for (int wkniK = 0; wkniK < 2; wkniK++) {
            Region wkniR = new Region();
            wkniR.setStyle("-fx-border-color: transparent transparent " + wkniBORDE + " transparent; -fx-min-height: 24;");
            wkniBox.getChildren().add(wkniR);
        }
        return wkniBox;
    }

    private static Node wkniFirmas() {
        HBox wkniH = new HBox(40, wkniFirma("Docente"), wkniFirma("Padre, madre o apoderado"));
        wkniH.setPadding(new Insets(26, 0, 0, 0));
        return wkniH;
    }

    private static Node wkniFirma(String wkniQuien) {
        Region wkniL = new Region();
        wkniL.setStyle("-fx-background-color: " + wkniTEXTO + "; -fx-min-height: 1; -fx-pref-height: 1; -fx-max-height: 1;");
        wkniL.setMaxWidth(Double.MAX_VALUE);
        VBox wkniV = new VBox(4, wkniL, wkniTxt(wkniQuien, 11, false, wkniSUAVE));
        wkniV.setAlignment(Pos.CENTER);
        HBox.setHgrow(wkniV, Priority.ALWAYS);
        return wkniV;
    }

    // ------------------------------------------------------------------ piezas

    private static Label wkniTxt(String wkniT, double wkni_size, boolean wkniBold, String wkniColor) {
        Label wkniL = new Label(wkniT);
        wkniL.setStyle("-fx-font-size: " + wkni_size + "px; -fx-text-fill: " + wkniColor + ";" + (wkniBold ? "-fx-font-weight: bold;" : ""));
        return wkniL;
    }

    private static Node wkniSeccion(String wkniT) {
        Label wkniL = wkniTxt(wkniT, 14, true, wkniTEXTO);
        wkniL.setStyle(wkniL.getStyle() + "-fx-border-color: transparent transparent " + wkniBORDE + " transparent; -fx-padding: 0 0 4 0;");
        wkniL.setMaxWidth(Double.MAX_VALUE);
        return wkniL;
    }

    private static VBox wkniCifra(String wkniTitulo, String wkniValor, String wkni_nota, String wkniColor) {
        VBox wkniV = new VBox(2, wkniTxt(wkniTitulo, 11, true, wkniSUAVE), wkniTxt(wkniValor, 22, true, wkniColor), wkniTxt(wkni_nota, 10, false, wkniSUAVE));
        wkniV.setStyle("-fx-border-color: " + wkniBORDE + "; -fx-border-radius: 8; -fx-padding: 10 12 10 12;");
        HBox.setHgrow(wkniV, Priority.ALWAYS);
        wkniV.setMaxWidth(Double.MAX_VALUE);
        return wkniV;
    }

    private static Node wkniLeyenda(String wkniColor, String wkniTexto) {
        Region wkniPunto = new Region();
        wkniPunto.setStyle("-fx-background-color:" + wkniColor + "; -fx-min-width: 10; -fx-min-height: 10; -fx-max-width: 10; -fx-max-height: 10; -fx-background-radius: 3;");
        HBox wkniH = new HBox(6, wkniPunto, wkniTxt(wkniTexto, 11, false, wkniTEXTO));
        wkniH.setAlignment(Pos.CENTER_LEFT);
        return wkniH;
    }

    private static Label wkniCelda(String wkniT, double wkniAncho, String wkniFondo, boolean wkniBold, String wkniColor) {
        Label wkniL = wkniTxt(wkniT, 11, wkniBold, wkniColor);
        wkniL.setStyle(wkniL.getStyle() + "-fx-background-color:" + wkniFondo + "; -fx-border-color: " + wkniBORDE + "; -fx-border-width: 0 0 1 0; -fx-padding: 5 6 5 6;");
        wkniL.setMinWidth(wkniAncho); wkniL.setPrefWidth(wkniAncho); wkniL.setMaxWidth(wkniAncho);
        return wkniL;
    }

    private static String wkniColorPct(double wkni_p) {
        if (Double.isNaN(wkni_p)) return wkniTEXTO;
        return wkni_p < wkniRiesgoEstudianteService.wkniASISTENCIA_MINIMA ? "#B3261E" : wkni_p < wkniRiesgoEstudianteService.wkniASISTENCIA_ATENCION ? "#8A5A00" : "#2C7A4B";
    }

    /** {texto, fondo} del nivel de riesgo, en colores de papel. */
    private static String[] wkniColorNivel(wkniRiesgoEstudianteService.wkniNivel wkniN) {
        return switch (wkniN) {
            case ALTO -> new String[]{"#B3261E", "#FCE8E6"};
            case ATENCION -> new String[]{"#8A5A00", "#FBF1D9"};
            case BIEN -> new String[]{"#2C7A4B", "#E3F3E8"};
        };
    }
}

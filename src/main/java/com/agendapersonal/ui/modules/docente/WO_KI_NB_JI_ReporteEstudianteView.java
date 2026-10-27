package com.agendapersonal.ui.modules.docente;

import com.agendapersonal.domain.model.WO_KI_NB_JI_EstadoEntrega;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstudianteClase;
import com.agendapersonal.service.WO_KI_NB_JI_ReporteEstudiante;
import com.agendapersonal.service.WO_KI_NB_JI_ReportesDocente;
import com.agendapersonal.service.WO_KI_NB_JI_ResumenCurso;
import com.agendapersonal.service.WO_KI_NB_JI_RiesgoEstudianteService;
import com.agendapersonal.ui.WO_KI_NB_JI_Logo;
import com.agendapersonal.ui.WO_KI_NB_JI_ThemeService;
import com.agendapersonal.util.WO_KI_NB_JI_Ui;
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
final class WO_KI_NB_JI_ReporteEstudianteView {
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final double ANCHO = 760;
    private static final String TEXTO = "#1C1F26", SUAVE = "#5B606D", BORDE = "#D9D6CE";

    private WO_KI_NB_JI_ReporteEstudianteView() {}

    /** Abre la vista previa del reporte de un estudiante; con las flechas se pasa al siguiente del curso. */
    static void WO_KI_NB_JI_mostrar(Node owner, WO_KI_NB_JI_CursoContexto ctx, WO_KI_NB_JI_EstudianteClase inicial) {
        WO_KI_NB_JI_ResumenCurso r = ctx.WO_KI_NB_JI_resumen();
        List<WO_KI_NB_JI_EstudianteClase> lista = r.WO_KI_NB_JI_estudiantes();
        if (lista.isEmpty()) { WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_info(owner, "Este curso aún no tiene estudiantes."); return; }
        int[] i = {Math.max(0, WO_KI_NB_JI_indice(lista, inicial))};

        Stage st = new Stage();
        Window w = WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_ventana(owner);
        if (w != null) { st.initOwner(w); st.initModality(Modality.WINDOW_MODAL); }
        WO_KI_NB_JI_Logo.WO_KI_NB_JI_aplicarIconos(st);
        StackPane hoja = new StackPane();
        hoja.setPadding(new Insets(24));
        hoja.setStyle("-fx-background-color: -surface2;");
        ScrollPane scroll = new ScrollPane(hoja);
        scroll.setFitToWidth(true);
        Label pos = WO_KI_NB_JI_Ui.WO_KI_NB_JI_muted("");
        pos.setMinWidth(javafx.scene.layout.Region.USE_PREF_SIZE);
        Button antes = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("chevron-left", "Anterior");
        Button despues = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("chevron-right", "Siguiente");
        Runnable pintar = () -> {
            WO_KI_NB_JI_EstudianteClase e = lista.get(i[0]);
            VBox pg = WO_KI_NB_JI_pagina(WO_KI_NB_JI_ReporteEstudiante.WO_KI_NB_JI_de(r, e, LocalDate.now()));
            pg.setStyle(pg.getStyle() + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.18), 14, 0.1, 0, 3);");   // solo en pantalla
            hoja.getChildren().setAll(pg);
            st.setTitle("Reporte de " + e.nombre() + " — " + ctx.clase());
            pos.setText((i[0] + 1) + " de " + lista.size());
            antes.setDisable(i[0] == 0);
            despues.setDisable(i[0] == lista.size() - 1);
            scroll.setVvalue(0);
        };
        antes.setOnAction(e -> { i[0]--; pintar.run(); });
        despues.setOnAction(e -> { i[0]++; pintar.run(); });
        Button imprimir = WO_KI_NB_JI_Ui.WO_KI_NB_JI_primary("print", "Imprimir o guardar PDF");
        imprimir.setOnAction(e -> WO_KI_NB_JI_ImpresionTabla.WO_KI_NB_JI_imprimirPaginas(hoja,
            List.of(WO_KI_NB_JI_pagina(WO_KI_NB_JI_ReporteEstudiante.WO_KI_NB_JI_de(r, lista.get(i[0]), LocalDate.now()))), PageOrientation.PORTRAIT));
        Button todos = WO_KI_NB_JI_Ui.WO_KI_NB_JI_secondary("copy", "Imprimir todo el curso");
        todos.setOnAction(e -> WO_KI_NB_JI_imprimirCurso(hoja, ctx));
        Button cerrar = WO_KI_NB_JI_Ui.WO_KI_NB_JI_ghost("close", "Cerrar");
        cerrar.setOnAction(e -> st.close());
        HBox barra = new HBox(10, antes, pos, despues, WO_KI_NB_JI_Ui.WO_KI_NB_JI_spacer(), todos, imprimir, cerrar);
        barra.setAlignment(Pos.CENTER_LEFT);
        barra.setPadding(new Insets(12, 16, 12, 16));
        barra.getStyleClass().add("card");
        VBox raiz = new VBox(barra, scroll);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        raiz.getStyleClass().add("app-root");
        Scene sc = new Scene(raiz, ANCHO + 110, 860);
        WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_get().WO_KI_NB_JI_applyTo(sc);
        st.setScene(sc);
        sc.setOnKeyPressed(k -> {
            switch (k.getCode()) {
                case LEFT, PAGE_UP -> { if (!antes.isDisabled()) antes.fire(); }
                case RIGHT, PAGE_DOWN -> { if (!despues.isDisabled()) despues.fire(); }
                case ESCAPE -> st.close();
                default -> { }
            }
        });
        pintar.run();
        st.show();
    }

    /** Un reporte por hoja para todos los estudiantes (reunión de padres, entrega de libretas). */
    static void WO_KI_NB_JI_imprimirCurso(Node owner, WO_KI_NB_JI_CursoContexto ctx) {
        WO_KI_NB_JI_ResumenCurso r = ctx.WO_KI_NB_JI_resumen();
        if (r.WO_KI_NB_JI_estudiantes().isEmpty()) { WO_KI_NB_JI_DocenteUi.WO_KI_NB_JI_info(owner, "Este curso aún no tiene estudiantes."); return; }
        List<Node> paginas = new ArrayList<>();
        for (WO_KI_NB_JI_EstudianteClase e : r.WO_KI_NB_JI_estudiantes()) paginas.add(WO_KI_NB_JI_pagina(WO_KI_NB_JI_ReporteEstudiante.WO_KI_NB_JI_de(r, e, LocalDate.now())));
        WO_KI_NB_JI_ImpresionTabla.WO_KI_NB_JI_imprimirPaginas(owner, paginas, PageOrientation.PORTRAIT);
    }

    private static int WO_KI_NB_JI_indice(List<WO_KI_NB_JI_EstudianteClase> l, WO_KI_NB_JI_EstudianteClase e) {
        if (e == null) return 0;
        for (int k = 0; k < l.size(); k++) if (l.get(k).id() == e.id()) return k;
        return 0;
    }

    // ------------------------------------------------------------------ hoja

    static VBox WO_KI_NB_JI_pagina(WO_KI_NB_JI_ReporteEstudiante rep) {
        WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Evaluacion ev = rep.evaluacion();
        VBox p = new VBox(16);
        p.setPadding(new Insets(34, 40, 30, 40));
        p.setPrefWidth(ANCHO); p.setMinWidth(ANCHO); p.setMaxWidth(ANCHO);
        p.setStyle("-fx-background-color: white;");

        // Encabezado
        Label tipo = WO_KI_NB_JI_txt("REPORTE DEL ESTUDIANTE", 11, true, SUAVE);
        Label nombre = WO_KI_NB_JI_txt(rep.estudiante().nombre(), 24, true, TEXTO);
        nombre.setWrapText(true);
        Label curso = WO_KI_NB_JI_txt(rep.curso() + (rep.periodo() == null ? "" : " · " + rep.periodo()), 13, false, SUAVE);
        VBox izq = new VBox(3, tipo, nombre, curso);
        HBox.setHgrow(izq, Priority.ALWAYS);
        String[] col = WO_KI_NB_JI_colorNivel(ev.nivel());
        Label nivel = WO_KI_NB_JI_txt(ev.nivel().WO_KI_NB_JI_etiqueta(), 13, true, col[0]);
        nivel.setStyle(nivel.getStyle() + "-fx-background-color:" + col[1] + "; -fx-background-radius: 99; -fx-padding: 5 14 5 14;");
        VBox der = new VBox(6, nivel, WO_KI_NB_JI_txt("Emitido el " + LocalDate.now().format(FECHA), 11, false, SUAVE));
        der.setAlignment(Pos.TOP_RIGHT);
        HBox cab = new HBox(16, izq, der);
        Region linea = new Region();
        linea.setStyle("-fx-background-color: #C64B1C; -fx-min-height: 3; -fx-pref-height: 3; -fx-max-height: 3;");

        // Cifras
        double nota = ev.nota() == null ? Double.NaN : ev.nota();
        HBox cifras = new HBox(10,
            WO_KI_NB_JI_cifra("Asistencia", WO_KI_NB_JI_ReportesDocente.WO_KI_NB_JI_pct(rep.WO_KI_NB_JI_porcentajeAsistencia()), ev.sesiones() + " clases registradas",
                WO_KI_NB_JI_colorPct(rep.WO_KI_NB_JI_porcentajeAsistencia())),
            WO_KI_NB_JI_cifra("Faltas", Integer.toString(rep.faltas()), rep.justificadas() + (rep.justificadas() == 1 ? " justificada" : " justificadas"), rep.faltas() > 0 ? "#B3261E" : TEXTO),
            WO_KI_NB_JI_cifra("Tardanzas", Integer.toString(rep.tardanzas()), "llegadas tarde", rep.tardanzas() >= 3 ? "#8A5A00" : TEXTO),
            WO_KI_NB_JI_cifra("Nota", Double.isNaN(nota) ? "—" : WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(Math.round(nota * 10) / 10.0),
                "mínima aprobatoria " + WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(rep.notaMinima()),
                Double.isNaN(nota) ? TEXTO : nota < rep.notaMinima() ? "#B3261E" : "#2C7A4B"));

        p.getChildren().addAll(cab, linea, cifras, WO_KI_NB_JI_alertas(rep), WO_KI_NB_JI_asistencia(rep), WO_KI_NB_JI_trabajos(rep), WO_KI_NB_JI_notasDocente(rep), WO_KI_NB_JI_firmas());
        return p;
    }

    private static Node WO_KI_NB_JI_alertas(WO_KI_NB_JI_ReporteEstudiante rep) {
        WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Evaluacion ev = rep.evaluacion();
        String[] c = WO_KI_NB_JI_colorNivel(ev.nivel());
        VBox box = new VBox(6, WO_KI_NB_JI_txt(ev.motivos().isEmpty() ? "Sin alertas" : "Puntos de atención", 13, true, c[0]));
        for (String m : ev.motivos()) box.getChildren().add(WO_KI_NB_JI_txt("•  " + m, 12, false, TEXTO));
        if (ev.motivos().isEmpty()) box.getChildren().add(WO_KI_NB_JI_txt("El estudiante no presenta faltas seguidas, trabajos pendientes ni notas bajo la mínima.", 12, false, TEXTO));
        box.setStyle("-fx-background-color:" + c[1] + "; -fx-border-color:" + c[0] + "; -fx-border-width: 0 0 0 4; -fx-padding: 10 14 10 14;");
        return box;
    }

    private static Node WO_KI_NB_JI_asistencia(WO_KI_NB_JI_ReporteEstudiante rep) {
        VBox box = new VBox(8, WO_KI_NB_JI_seccion("Asistencia"));
        int total = rep.presentes() + rep.faltas() + rep.tardanzas() + rep.justificadas();
        if (total == 0) { box.getChildren().add(WO_KI_NB_JI_txt("Aún no hay clases registradas.", 12, false, SUAVE)); return box; }
        HBox barra = new HBox();
        barra.setPrefHeight(10); barra.setMaxWidth(Double.MAX_VALUE);
        int[] v = {rep.presentes(), rep.tardanzas(), rep.justificadas(), rep.faltas()};
        String[] cs = {"#2C7A4B", "#E8B43A", "#2F5BB7", "#B3261E"};
        for (int k = 0; k < v.length; k++) {
            if (v[k] == 0) continue;
            Region seg = new Region();
            seg.setStyle("-fx-background-color:" + cs[k] + ";");
            seg.prefWidthProperty().bind(barra.widthProperty().multiply((double) v[k] / total));
            barra.getChildren().add(seg);
        }
        HBox ley = new HBox(16, WO_KI_NB_JI_leyenda("#2C7A4B", rep.presentes() + " presente(s)"), WO_KI_NB_JI_leyenda("#B3261E", rep.faltas() + " falta(s)"),
            WO_KI_NB_JI_leyenda("#E8B43A", rep.tardanzas() + " tardanza(s)"), WO_KI_NB_JI_leyenda("#2F5BB7", rep.justificadas() + " justificada(s)"));
        FlowPane dias = new FlowPane(5, 5);
        for (var en : rep.historial().entrySet()) {
            Label d = WO_KI_NB_JI_txt(en.getKey().format(DIA) + " " + en.getValue().WO_KI_NB_JI_letra(), 10, true, "white");
            String fondo = switch (en.getValue()) {
                case PRESENTE -> "#2C7A4B"; case FALTA -> "#B3261E"; case TARDANZA -> "#C99418"; case JUSTIFICADA -> "#2F5BB7";
            };
            d.setStyle(d.getStyle() + "-fx-background-color:" + fondo + "; -fx-background-radius: 5; -fx-padding: 2 6 2 6;");
            dias.getChildren().add(d);
        }
        box.getChildren().addAll(barra, ley, WO_KI_NB_JI_txt("Detalle por fecha (P presente · F falta · T tardanza · J justificada):", 11, false, SUAVE), dias);
        return box;
    }

    private static Node WO_KI_NB_JI_trabajos(WO_KI_NB_JI_ReporteEstudiante rep) {
        VBox box = new VBox(8, WO_KI_NB_JI_seccion("Trabajos y evaluaciones"));
        if (rep.trabajos().isEmpty()) { box.getChildren().add(WO_KI_NB_JI_txt("No hay trabajos registrados en el curso.", 12, false, SUAVE)); return box; }
        GridPane g = new GridPane();
        String[] enc = {"Trabajo", "Tipo", "Fecha", "Entrega", "Nota"};
        double[] an = {300, 100, 80, 120, 80};
        for (int c = 0; c < enc.length; c++) g.add(WO_KI_NB_JI_celda(enc[c], an[c], "#EFEDE7", true, TEXTO), c, 0);
        int f = 1;
        for (WO_KI_NB_JI_ReporteEstudiante.WO_KI_NB_JI_Trabajo t : rep.trabajos()) {
            String entrega; String color;
            if (t.estado() == null) { entrega = t.vencido() ? "Sin registrar" : "Próximo"; color = SUAVE; }
            else {
                entrega = t.estado().WO_KI_NB_JI_simbolo() + " " + t.estado().WO_KI_NB_JI_valor();
                color = t.estado() == WO_KI_NB_JI_EstadoEntrega.ENTREGADO ? "#2C7A4B" : t.estado() == WO_KI_NB_JI_EstadoEntrega.TARDE ? "#8A5A00" : "#B3261E";
            }
            String fondo = f % 2 == 0 ? "#FAFAF8" : "white";
            g.add(WO_KI_NB_JI_celda(t.titulo(), an[0], fondo, false, TEXTO), 0, f);
            g.add(WO_KI_NB_JI_celda(t.tipo(), an[1], fondo, false, SUAVE), 1, f);
            g.add(WO_KI_NB_JI_celda(t.fecha() == null ? "—" : t.fecha().format(DIA), an[2], fondo, false, SUAVE), 2, f);
            g.add(WO_KI_NB_JI_celda(entrega, an[3], fondo, true, color), 3, f);
            boolean baja = t.nota() != null && t.nota() < rep.notaMinima();
            g.add(WO_KI_NB_JI_celda(t.nota() == null ? "—" : WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(t.nota()), an[4], fondo, true, baja ? "#B3261E" : TEXTO), 4, f);
            f++;
        }
        box.getChildren().add(g);
        if (!Double.isNaN(rep.promedioTrabajos()))
            box.getChildren().add(WO_KI_NB_JI_txt("Promedio de trabajos: " + WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(Math.round(rep.promedioTrabajos() * 10) / 10.0)
                + (rep.estudiante().calificacion() == null ? "" : "   ·   Nota del curso: " + WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_numero(rep.estudiante().calificacion())),
                12, true, TEXTO));
        return box;
    }

    private static Node WO_KI_NB_JI_notasDocente(WO_KI_NB_JI_ReporteEstudiante rep) {
        VBox box = new VBox(6, WO_KI_NB_JI_seccion("Observaciones y sugerencias"));
        String obs = rep.estudiante().observaciones();
        if (obs != null && !obs.isBlank()) {
            Label o = WO_KI_NB_JI_txt("Observación del docente: " + obs, 12, false, TEXTO);
            o.setWrapText(true);
            box.getChildren().add(o);
        }
        for (String s : rep.sugerencias()) {
            Label l = WO_KI_NB_JI_txt("→  " + s, 12, false, TEXTO);
            l.setWrapText(true);
            box.getChildren().add(l);
        }
        // Espacio para que el docente escriba a mano.
        for (int k = 0; k < 2; k++) {
            Region r = new Region();
            r.setStyle("-fx-border-color: transparent transparent " + BORDE + " transparent; -fx-min-height: 24;");
            box.getChildren().add(r);
        }
        return box;
    }

    private static Node WO_KI_NB_JI_firmas() {
        HBox h = new HBox(40, WO_KI_NB_JI_firma("Docente"), WO_KI_NB_JI_firma("Padre, madre o apoderado"));
        h.setPadding(new Insets(26, 0, 0, 0));
        return h;
    }

    private static Node WO_KI_NB_JI_firma(String quien) {
        Region l = new Region();
        l.setStyle("-fx-background-color: " + TEXTO + "; -fx-min-height: 1; -fx-pref-height: 1; -fx-max-height: 1;");
        l.setMaxWidth(Double.MAX_VALUE);
        VBox v = new VBox(4, l, WO_KI_NB_JI_txt(quien, 11, false, SUAVE));
        v.setAlignment(Pos.CENTER);
        HBox.setHgrow(v, Priority.ALWAYS);
        return v;
    }

    // ------------------------------------------------------------------ piezas

    private static Label WO_KI_NB_JI_txt(String t, double size, boolean bold, String color) {
        Label l = new Label(t);
        l.setStyle("-fx-font-size: " + size + "px; -fx-text-fill: " + color + ";" + (bold ? "-fx-font-weight: bold;" : ""));
        return l;
    }

    private static Node WO_KI_NB_JI_seccion(String t) {
        Label l = WO_KI_NB_JI_txt(t, 14, true, TEXTO);
        l.setStyle(l.getStyle() + "-fx-border-color: transparent transparent " + BORDE + " transparent; -fx-padding: 0 0 4 0;");
        l.setMaxWidth(Double.MAX_VALUE);
        return l;
    }

    private static VBox WO_KI_NB_JI_cifra(String titulo, String valor, String nota, String color) {
        VBox v = new VBox(2, WO_KI_NB_JI_txt(titulo, 11, true, SUAVE), WO_KI_NB_JI_txt(valor, 22, true, color), WO_KI_NB_JI_txt(nota, 10, false, SUAVE));
        v.setStyle("-fx-border-color: " + BORDE + "; -fx-border-radius: 8; -fx-padding: 10 12 10 12;");
        HBox.setHgrow(v, Priority.ALWAYS);
        v.setMaxWidth(Double.MAX_VALUE);
        return v;
    }

    private static Node WO_KI_NB_JI_leyenda(String color, String texto) {
        Region punto = new Region();
        punto.setStyle("-fx-background-color:" + color + "; -fx-min-width: 10; -fx-min-height: 10; -fx-max-width: 10; -fx-max-height: 10; -fx-background-radius: 3;");
        HBox h = new HBox(6, punto, WO_KI_NB_JI_txt(texto, 11, false, TEXTO));
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    private static Label WO_KI_NB_JI_celda(String t, double ancho, String fondo, boolean bold, String color) {
        Label l = WO_KI_NB_JI_txt(t, 11, bold, color);
        l.setStyle(l.getStyle() + "-fx-background-color:" + fondo + "; -fx-border-color: " + BORDE + "; -fx-border-width: 0 0 1 0; -fx-padding: 5 6 5 6;");
        l.setMinWidth(ancho); l.setPrefWidth(ancho); l.setMaxWidth(ancho);
        return l;
    }

    private static String WO_KI_NB_JI_colorPct(double p) {
        if (Double.isNaN(p)) return TEXTO;
        return p < WO_KI_NB_JI_RiesgoEstudianteService.ASISTENCIA_MINIMA ? "#B3261E" : p < WO_KI_NB_JI_RiesgoEstudianteService.ASISTENCIA_ATENCION ? "#8A5A00" : "#2C7A4B";
    }

    /** {texto, fondo} del nivel de riesgo, en colores de papel. */
    private static String[] WO_KI_NB_JI_colorNivel(WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel n) {
        return switch (n) {
            case ALTO -> new String[]{"#B3261E", "#FCE8E6"};
            case ATENCION -> new String[]{"#8A5A00", "#FBF1D9"};
            case BIEN -> new String[]{"#2C7A4B", "#E3F3E8"};
        };
    }
}

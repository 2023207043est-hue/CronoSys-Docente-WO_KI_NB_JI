package com.agendapersonal.ui;

import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ArcTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

/**
 * Logo de Compás: una brújula que también es un reloj, con la aguja saliendo por la abertura del anillo.
 * Se dibuja en vector, así que se ve nítido en cualquier tamaño; los PNG y el .ico del programa se
 * generan con el mismo diseño (carpeta /brand).
 */
public final class wkniLogo {
    public static final Color wkniNARANJA_CLARO = Color.web("#F2884F");
    public static final Color wkniNARANJA_OSCURO = Color.web("#C2451A");
    public static final Color wkniAGUJA = Color.web("#FFD9C2");
    /** Nombre de la aplicación. */
    public static final String wkniNOMBRE = "Compás";
    private static final int[] wkniTAMANOS_ICONO = {16, 24, 32, 48, 64, 128, 256};

    private wkniLogo() {}

    /** Marca cuadrada (azulejo naranja con la brújula en blanco). */
    public static Node wkniMark(double wkniS) {
        Rectangle wkniTile = new Rectangle(wkniS, wkniS);
        wkniTile.setArcWidth(wkniS * 0.56);
        wkniTile.setArcHeight(wkniS * 0.56);
        wkniTile.setFill(new LinearGradient(0, 0, 1, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, wkniNARANJA_CLARO), new Stop(1, wkniNARANJA_OSCURO)));

        // El diseño está en una rejilla de 256×256 (igual que el SVG y los PNG).
        double wkniU = wkniS / 256.0;
        double wkniCx = wkniS / 2, wkniCy = wkniS / 2;

        // El anillo: un arco con la abertura arriba. En JavaFX el constructor es
        // (rx, ry, rotación, x, y, arcoGrande, sentido) y las banderas van al final.
        Path wkniAnillo = new Path(
            new MoveTo(83.28 * wkniU, 64.13 * wkniU),
            new ArcTo(78 * wkniU, 78 * wkniU, 0, 172.72 * wkniU, 64.13 * wkniU, true, false));
        wkniAnillo.setFill(null);
        wkniAnillo.setStroke(Color.WHITE);
        wkniAnillo.setStrokeWidth(26 * wkniU);
        wkniAnillo.setStrokeLineCap(StrokeLineCap.ROUND);

        double wkniPunta = wkniCy - 70 * wkniU, wkniMedio = 16 * wkniU;
        Polygon wkniIzquierda = new Polygon(wkniCx, wkniPunta, wkniCx - wkniMedio, wkniCy, wkniCx, wkniCy);
        wkniIzquierda.setFill(wkniAGUJA);
        Polygon wkniDerecha = new Polygon(wkniCx, wkniPunta, wkniCx + wkniMedio, wkniCy, wkniCx, wkniCy);
        wkniDerecha.setFill(Color.WHITE);

        double wkniCr = 12 * wkniU;
        Circle wkniCentro = new Circle(wkniCx, wkniCy, wkniCr, Color.WHITE);

        Group wkniG = new Group(wkniTile, wkniAnillo, wkniIzquierda, wkniDerecha, wkniCentro);
        wkniG.setMouseTransparent(true);
        wkniG.setAccessibleText(wkniNOMBRE);
        return wkniG;
    }

    /** Marca + nombre ("Comp" en texto normal y "ás" en el color de acento). */
    public static HBox wkniWordmark(double wkniS) {
        Label wkniComp = new Label("Comp");
        wkniComp.getStyleClass().add("brand-name");
        Label wkniAs = new Label("ás");
        wkniAs.getStyleClass().addAll("brand-name", "brand-name-accent");
        HBox wkniName = new HBox(0, wkniComp, wkniAs);
        wkniName.setAlignment(Pos.CENTER_LEFT);
        HBox wkniBox = new HBox(10, wkniMark(wkniS), wkniName);
        wkniBox.setAlignment(Pos.CENTER_LEFT);
        return wkniBox;
    }

    /** Íconos de la ventana (barra de tareas, Alt+Tab) desde /brand. */
    public static List<Image> wkniIconosVentana() {
        List<Image> wkniR = new ArrayList<>();
        for (int wkniT : wkniTAMANOS_ICONO) {
            var wkniUrl = wkniLogo.class.getResource("/brand/compas-" + wkniT + ".png");
            if (wkniUrl != null) wkniR.add(new Image(wkniUrl.toExternalForm()));
        }
        return wkniR;
    }

    public static void wkniAplicarIconos(Stage wkniStage) {
        if (wkniStage != null && wkniStage.getIcons().isEmpty()) wkniStage.getIcons().addAll(wkniIconosVentana());
    }
}

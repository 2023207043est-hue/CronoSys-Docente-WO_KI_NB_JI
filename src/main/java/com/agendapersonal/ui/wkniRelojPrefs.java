package com.agendapersonal.ui;

import com.agendapersonal.infrastructure.db.wkniConfigDAO;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/** Preferencias del reloj grande del inicio (formato 12/24 h, segundos, tamaño, visible). */
public final class wkniRelojPrefs {
    public enum wkniTamano {
        PEQUENO("Pequeño", 40), MEDIANO("Mediano", 64), GRANDE("Grande", 88);
        private final String wkniEtiqueta; private final int wkniPx;
        wkniTamano(String wkniE, int wkniPx) { this.wkniEtiqueta = wkniE; this.wkniPx = wkniPx; }
        public String wkniEtiqueta() { return wkniEtiqueta; }
        public int wkniPx() { return wkniPx; }
        public static wkniTamano wkniDesde(String wkniV) { try { return valueOf(wkniV); } catch (Exception wkniE) { return MEDIANO; } }
    }

    private static final String wkniK_24H = "reloj.h24", wkniK_SEG = "reloj.segundos", wkniK_TAM = "reloj.tamano", wkniK_VISIBLE = "reloj.visible";
    private static final wkniConfigDAO wkniCFG = new wkniConfigDAO();
    private static final IntegerProperty wkniREVISION = new SimpleIntegerProperty(0);
    private wkniRelojPrefs() {}

    /** Cambia cada vez que se modifica una preferencia (el inicio se redibuja solo). */
    public static IntegerProperty wkniRevisionProperty() { return wkniREVISION; }

    public static boolean wkniFormato24() { return !"0".equals(wkniCFG.wkniGet(wkniK_24H, "1")); }
    public static boolean wkniSegundos() { return "1".equals(wkniCFG.wkniGet(wkniK_SEG, "0")); }
    public static boolean wkniVisible() { return !"0".equals(wkniCFG.wkniGet(wkniK_VISIBLE, "1")); }
    public static wkniTamano wkniTamano() { return wkniTamano.wkniDesde(wkniCFG.wkniGet(wkniK_TAM, "MEDIANO")); }

    public static void wkniSetFormato24(boolean wkniV) { wkniGuardar(wkniK_24H, wkniV ? "1" : "0"); }
    public static void wkniSetSegundos(boolean wkniV) { wkniGuardar(wkniK_SEG, wkniV ? "1" : "0"); }
    public static void wkniSetVisible(boolean wkniV) { wkniGuardar(wkniK_VISIBLE, wkniV ? "1" : "0"); }
    public static void wkniSetTamano(wkniTamano wkniT) { wkniGuardar(wkniK_TAM, wkniT.name()); }

    private static void wkniGuardar(String wkniK, String wkniV) { wkniCFG.wkniSet(wkniK, wkniV); wkniREVISION.set(wkniREVISION.get() + 1); }

    /** Patrón de hora: "HH:mm", "HH:mm:ss", "h:mm a" o "h:mm:ss a". */
    public static String wkniPatron(boolean wkniH24, boolean wkniConSegundos) {
        String wkniS = wkniConSegundos ? ":ss" : "";
        return wkniH24 ? "HH:mm" + wkniS : "h:mm" + wkniS + " a";
    }

    public static String wkniFormatear(LocalTime wkniT, boolean wkniH24, boolean wkniConSegundos) {
        return wkniT.format(DateTimeFormatter.ofPattern(wkniPatron(wkniH24, wkniConSegundos), com.agendapersonal.util.wkniUi.wkniES));
    }

    public static String wkniFormatearAhora() { return wkniFormatear(LocalTime.now(), wkniFormato24(), wkniSegundos()); }
}

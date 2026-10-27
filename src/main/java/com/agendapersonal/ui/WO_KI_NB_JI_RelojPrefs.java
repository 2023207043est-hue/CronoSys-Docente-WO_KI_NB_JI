package com.agendapersonal.ui;

import com.agendapersonal.infrastructure.db.WO_KI_NB_JI_ConfigDAO;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/** Preferencias del reloj grande del inicio (formato 12/24 h, segundos, tamaño, visible). */
public final class WO_KI_NB_JI_RelojPrefs {
    public enum WO_KI_NB_JI_Tamano {
        PEQUENO("Pequeño", 40), MEDIANO("Mediano", 64), GRANDE("Grande", 88);
        private final String etiqueta; private final int px;
        WO_KI_NB_JI_Tamano(String e, int px) { this.etiqueta = e; this.px = px; }
        public String WO_KI_NB_JI_etiqueta() { return etiqueta; }
        public int WO_KI_NB_JI_px() { return px; }
        public static WO_KI_NB_JI_Tamano WO_KI_NB_JI_desde(String v) { try { return valueOf(v); } catch (Exception e) { return MEDIANO; } }
    }

    private static final String K_24H = "reloj.h24", K_SEG = "reloj.segundos", K_TAM = "reloj.tamano", K_VISIBLE = "reloj.visible";
    private static final WO_KI_NB_JI_ConfigDAO CFG = new WO_KI_NB_JI_ConfigDAO();
    private static final IntegerProperty REVISION = new SimpleIntegerProperty(0);
    private WO_KI_NB_JI_RelojPrefs() {}

    /** Cambia cada vez que se modifica una preferencia (el inicio se redibuja solo). */
    public static IntegerProperty WO_KI_NB_JI_revisionProperty() { return REVISION; }

    public static boolean WO_KI_NB_JI_formato24() { return !"0".equals(CFG.WO_KI_NB_JI_get(K_24H, "1")); }
    public static boolean WO_KI_NB_JI_segundos() { return "1".equals(CFG.WO_KI_NB_JI_get(K_SEG, "0")); }
    public static boolean WO_KI_NB_JI_visible() { return !"0".equals(CFG.WO_KI_NB_JI_get(K_VISIBLE, "1")); }
    public static WO_KI_NB_JI_Tamano WO_KI_NB_JI_tamano() { return WO_KI_NB_JI_Tamano.WO_KI_NB_JI_desde(CFG.WO_KI_NB_JI_get(K_TAM, "MEDIANO")); }

    public static void WO_KI_NB_JI_setFormato24(boolean v) { WO_KI_NB_JI_guardar(K_24H, v ? "1" : "0"); }
    public static void WO_KI_NB_JI_setSegundos(boolean v) { WO_KI_NB_JI_guardar(K_SEG, v ? "1" : "0"); }
    public static void WO_KI_NB_JI_setVisible(boolean v) { WO_KI_NB_JI_guardar(K_VISIBLE, v ? "1" : "0"); }
    public static void WO_KI_NB_JI_setTamano(WO_KI_NB_JI_Tamano t) { WO_KI_NB_JI_guardar(K_TAM, t.name()); }

    private static void WO_KI_NB_JI_guardar(String k, String v) { CFG.WO_KI_NB_JI_set(k, v); REVISION.set(REVISION.get() + 1); }

    /** Patrón de hora: "HH:mm", "HH:mm:ss", "h:mm a" o "h:mm:ss a". */
    public static String WO_KI_NB_JI_patron(boolean h24, boolean conSegundos) {
        String s = conSegundos ? ":ss" : "";
        return h24 ? "HH:mm" + s : "h:mm" + s + " a";
    }

    public static String WO_KI_NB_JI_formatear(LocalTime t, boolean h24, boolean conSegundos) {
        return t.format(DateTimeFormatter.ofPattern(WO_KI_NB_JI_patron(h24, conSegundos), com.agendapersonal.util.WO_KI_NB_JI_Ui.ES));
    }

    public static String WO_KI_NB_JI_formatearAhora() { return WO_KI_NB_JI_formatear(LocalTime.now(), WO_KI_NB_JI_formato24(), WO_KI_NB_JI_segundos()); }
}

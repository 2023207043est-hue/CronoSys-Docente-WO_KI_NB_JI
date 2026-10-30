package com.agendapersonal.ui;

import com.agendapersonal.settings.wkniSettingsStore;
import javafx.beans.binding.Bindings;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.scene.Parent;
import javafx.scene.control.Labeled;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Tipografía de toda la interfaz: familia y tamaño del texto, guardados en la configuración.
 * Incluye fuentes propias (carpeta {@code /fonts}) para que la app se vea igual en cualquier PC.
 */
public final class wkniFontPreferences {
    /** Fuentes que vienen dentro de Compás (archivo base en /fonts, sin "-Regular.ttf"). */
    private static final String[][] wkniBUNDLED = {
        {"Plus Jakarta Sans", "PlusJakartaSans"},
        {"Inter", "Inter"},
        {"IBM Plex Sans", "IBMPlexSans"},
        {"Source Sans 3", "SourceSans3"},
        {"Roboto", "Roboto"},
        {"JetBrains Mono", "JetBrainsMono"},
    };
    /** Fuentes del sistema (Windows) que también se ofrecen. */
    private static final List<String> wkniSYSTEM = List.of(
        "Segoe UI", "Calibri", "Arial", "Verdana", "Tahoma", "Georgia",
        "Corbel", "Constantia", "Comic Sans MS", "Consolas", "Courier New"
    );

    /** Tamaños de texto: etiqueta visible → tamaño base en px. */
    public enum wkniSize {
        PEQUENO("Pequeño", 12.5), NORMAL("Normal", 14), GRANDE("Grande", 15.5), MUY_GRANDE("Muy grande", 17);
        private final String wkniLabel; private final double wkniPx;
        wkniSize(String wkniLabel, double wkniPx) { this.wkniLabel = wkniLabel; this.wkniPx = wkniPx; }
        public double wkniPx() { return wkniPx; }
        @Override public String toString() { return wkniLabel; }
        public static wkniSize wkniFrom(String wkniV) {
            try { return valueOf(wkniV); } catch (Exception wkniE) { return NORMAL; }
        }
    }

    private static final wkniSettingsStore wkniSTORE = wkniSettingsStore.wkniGet();
    private static boolean wkniLoaded;
    private static final StringProperty wkniFAMILY = new SimpleStringProperty();
    private static final DoubleProperty wkniBASE = new SimpleDoubleProperty();

    static {
        wkniCargarFuentesIncluidas();
        wkniFAMILY.set(wkniNormalize(wkniSTORE.wkniStr("appearance.font")));
        wkniBASE.set(14.0 * wkniSTORE.wkniInteger("appearance.fontScale", 85, 125) / 100.0);
    }

    private wkniFontPreferences() {}

    /** Registra en JavaFX las fuentes incluidas (se llama solo una vez). */
    public static synchronized void wkniCargarFuentesIncluidas() {
        if (wkniLoaded) return;
        wkniLoaded = true;
        for (String[] wkniF : wkniBUNDLED) {
            for (String wkniPeso : new String[]{"Regular", "Bold"}) {
                try (InputStream wkniIn = wkniFontPreferences.class.getResourceAsStream("/fonts/" + wkniF[1] + "-" + wkniPeso + ".ttf")) {
                    if (wkniIn != null) Font.loadFont(wkniIn, 13);
                } catch (Exception wkniIgnored) { /* si falla, se usa una fuente del sistema */ }
            }
        }
    }

    /** Opciones para el selector: primero las incluidas, luego las del sistema que estén instaladas. */
    public static List<String> wkniOptions() {
        Set<String> wkniOut = new LinkedHashSet<>();
        Set<String> wkniInstaladas = new java.util.HashSet<>(Font.getFamilies());
        for (String[] wkniF : wkniBUNDLED) if (wkniInstaladas.contains(wkniF[0])) wkniOut.add(wkniF[0]);
        for (String wkniS : wkniSYSTEM) if (wkniInstaladas.contains(wkniS)) wkniOut.add(wkniS);
        if (wkniOut.isEmpty()) wkniOut.add(Font.getDefault().getFamily());
        return new ArrayList<>(wkniOut);
    }

    public static boolean wkniEsIncluida(String wkniFamily) {
        for (String[] wkniF : wkniBUNDLED) if (wkniF[0].equals(wkniFamily)) return true;
        return false;
    }

    public static String wkniFamily() { return wkniFAMILY.get(); }
    public static StringProperty wkniFamilyProperty() { return wkniFAMILY; }
    public static double wkniBaseSize() { return wkniBASE.get(); }
    public static DoubleProperty wkniBaseSizeProperty() { return wkniBASE; }
    public static wkniSize wkniSize() {
        for (wkniSize wkniS : wkniSize.values()) if (Math.abs(wkniS.wkniPx() - wkniBASE.get()) < 0.1) return wkniS;
        return wkniSize.NORMAL;
    }

    public static void wkniSetFamily(String wkniSelected) {
        wkniFAMILY.set(wkniNormalize(wkniSelected));
        wkniSTORE.wkniSet("appearance.font", wkniFAMILY.get());
        wkniThemeService.wkniGet().wkniRefresh();
    }

    public static void wkniSetSize(wkniSize wkniS) {
        if (wkniS == null) return;
        wkniSetScale((int) Math.round(wkniS.wkniPx() / 14.0 * 100));
    }

    /** Escala de texto en porcentaje (85 a 125). Pequeño/Normal/Grande/Muy grande son atajos de este valor. */
    public static void wkniSetScale(int wkniPercent) {
        int wkni_p = Math.max(85, Math.min(125, wkniPercent));
        wkniBASE.set(14.0 * wkni_p / 100.0);
        wkniSTORE.wkniSet("appearance.fontScale", wkni_p);
        wkniThemeService.wkniGet().wkniRefresh();
    }

    public static int wkniScalePercent() { return (int) Math.round(wkniBASE.get() / 14.0 * 100); }

    /** Recarga familia y escala desde los ajustes (al cambiar de perfil o importar). */
    public static void wkniReload() {
        wkniFAMILY.set(wkniNormalize(wkniSTORE.wkniStr("appearance.font")));
        wkniBASE.set(14.0 * wkniSTORE.wkniInteger("appearance.fontScale", 85, 125) / 100.0);
    }

    // Compatibilidad con el código anterior.
    public static void wkniSetFamily(String wkniSelected, Parent wkniIgnored) { wkniSetFamily(wkniSelected); }
    public static void wkniApply(Parent wkniIgnored) { wkniThemeService.wkniGet().wkniRefresh(); }

    /**
     * Fuente fija (p. ej. un título de 22 px en negrita) que sigue a la familia y al tamaño elegidos.
     * {@code size} está pensado para el tamaño "Normal" (14 px) y se escala con la preferencia.
     */
    public static void wkniBind(Labeled wkniNode, FontWeight wkniWeight, double wkni_size) {
        wkniNode.fontProperty().bind(Bindings.createObjectBinding(
            () -> Font.font(wkniFAMILY.get(), wkniWeight, wkni_size * wkniBASE.get() / 14.0), wkniFAMILY, wkniBASE));
    }

    private static String wkniNormalize(String wkniValue) {
        if (wkniValue == null) return "Plus Jakarta Sans";
        for (String wkniO : wkniOptions()) if (wkniO.equalsIgnoreCase(wkniValue.trim())) return wkniO;
        return wkniOptions().getFirst();
    }
}

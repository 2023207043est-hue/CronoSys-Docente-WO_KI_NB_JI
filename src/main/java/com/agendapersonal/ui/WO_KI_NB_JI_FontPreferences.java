package com.agendapersonal.ui;

import com.agendapersonal.settings.WO_KI_NB_JI_SettingsStore;
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
public final class WO_KI_NB_JI_FontPreferences {
    /** Fuentes que vienen dentro de Compás (archivo base en /fonts, sin "-Regular.ttf"). */
    private static final String[][] BUNDLED = {
        {"Plus Jakarta Sans", "PlusJakartaSans"},
        {"Inter", "Inter"},
        {"IBM Plex Sans", "IBMPlexSans"},
        {"Source Sans 3", "SourceSans3"},
        {"Roboto", "Roboto"},
        {"JetBrains Mono", "JetBrainsMono"},
    };
    /** Fuentes del sistema (Windows) que también se ofrecen. */
    private static final List<String> SYSTEM = List.of(
        "Segoe UI", "Calibri", "Arial", "Verdana", "Tahoma", "Georgia",
        "Corbel", "Constantia", "Comic Sans MS", "Consolas", "Courier New"
    );

    /** Tamaños de texto: etiqueta visible → tamaño base en px. */
    public enum WO_KI_NB_JI_Size {
        PEQUENO("Pequeño", 12.5), NORMAL("Normal", 14), GRANDE("Grande", 15.5), MUY_GRANDE("Muy grande", 17);
        private final String label; private final double px;
        WO_KI_NB_JI_Size(String label, double px) { this.label = label; this.px = px; }
        public double WO_KI_NB_JI_px() { return px; }
        @Override public String toString() { return label; }
        public static WO_KI_NB_JI_Size WO_KI_NB_JI_from(String v) {
            try { return valueOf(v); } catch (Exception e) { return NORMAL; }
        }
    }

    private static final WO_KI_NB_JI_SettingsStore STORE = WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_get();
    private static boolean loaded;
    private static final StringProperty FAMILY = new SimpleStringProperty();
    private static final DoubleProperty BASE = new SimpleDoubleProperty();

    static {
        WO_KI_NB_JI_cargarFuentesIncluidas();
        FAMILY.set(WO_KI_NB_JI_normalize(STORE.WO_KI_NB_JI_str("appearance.font")));
        BASE.set(14.0 * STORE.WO_KI_NB_JI_integer("appearance.fontScale", 85, 125) / 100.0);
    }

    private WO_KI_NB_JI_FontPreferences() {}

    /** Registra en JavaFX las fuentes incluidas (se llama solo una vez). */
    public static synchronized void WO_KI_NB_JI_cargarFuentesIncluidas() {
        if (loaded) return;
        loaded = true;
        for (String[] f : BUNDLED) {
            for (String peso : new String[]{"Regular", "Bold"}) {
                try (InputStream in = WO_KI_NB_JI_FontPreferences.class.getResourceAsStream("/fonts/" + f[1] + "-" + peso + ".ttf")) {
                    if (in != null) Font.loadFont(in, 13);
                } catch (Exception ignored) { /* si falla, se usa una fuente del sistema */ }
            }
        }
    }

    /** Opciones para el selector: primero las incluidas, luego las del sistema que estén instaladas. */
    public static List<String> WO_KI_NB_JI_options() {
        Set<String> out = new LinkedHashSet<>();
        Set<String> instaladas = new java.util.HashSet<>(Font.getFamilies());
        for (String[] f : BUNDLED) if (instaladas.contains(f[0])) out.add(f[0]);
        for (String s : SYSTEM) if (instaladas.contains(s)) out.add(s);
        if (out.isEmpty()) out.add(Font.getDefault().getFamily());
        return new ArrayList<>(out);
    }

    public static boolean WO_KI_NB_JI_esIncluida(String family) {
        for (String[] f : BUNDLED) if (f[0].equals(family)) return true;
        return false;
    }

    public static String WO_KI_NB_JI_family() { return FAMILY.get(); }
    public static StringProperty WO_KI_NB_JI_familyProperty() { return FAMILY; }
    public static double WO_KI_NB_JI_baseSize() { return BASE.get(); }
    public static DoubleProperty WO_KI_NB_JI_baseSizeProperty() { return BASE; }
    public static WO_KI_NB_JI_Size WO_KI_NB_JI_size() {
        for (WO_KI_NB_JI_Size s : WO_KI_NB_JI_Size.values()) if (Math.abs(s.WO_KI_NB_JI_px() - BASE.get()) < 0.1) return s;
        return WO_KI_NB_JI_Size.NORMAL;
    }

    public static void WO_KI_NB_JI_setFamily(String selected) {
        FAMILY.set(WO_KI_NB_JI_normalize(selected));
        STORE.WO_KI_NB_JI_set("appearance.font", FAMILY.get());
        WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_get().WO_KI_NB_JI_refresh();
    }

    public static void WO_KI_NB_JI_setSize(WO_KI_NB_JI_Size s) {
        if (s == null) return;
        WO_KI_NB_JI_setScale((int) Math.round(s.WO_KI_NB_JI_px() / 14.0 * 100));
    }

    /** Escala de texto en porcentaje (85 a 125). Pequeño/Normal/Grande/Muy grande son atajos de este valor. */
    public static void WO_KI_NB_JI_setScale(int percent) {
        int p = Math.max(85, Math.min(125, percent));
        BASE.set(14.0 * p / 100.0);
        STORE.WO_KI_NB_JI_set("appearance.fontScale", p);
        WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_get().WO_KI_NB_JI_refresh();
    }

    public static int WO_KI_NB_JI_scalePercent() { return (int) Math.round(BASE.get() / 14.0 * 100); }

    /** Recarga familia y escala desde los ajustes (al cambiar de perfil o importar). */
    public static void WO_KI_NB_JI_reload() {
        FAMILY.set(WO_KI_NB_JI_normalize(STORE.WO_KI_NB_JI_str("appearance.font")));
        BASE.set(14.0 * STORE.WO_KI_NB_JI_integer("appearance.fontScale", 85, 125) / 100.0);
    }

    // Compatibilidad con el código anterior.
    public static void WO_KI_NB_JI_setFamily(String selected, Parent ignored) { WO_KI_NB_JI_setFamily(selected); }
    public static void WO_KI_NB_JI_apply(Parent ignored) { WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_get().WO_KI_NB_JI_refresh(); }

    /**
     * Fuente fija (p. ej. un título de 22 px en negrita) que sigue a la familia y al tamaño elegidos.
     * {@code size} está pensado para el tamaño "Normal" (14 px) y se escala con la preferencia.
     */
    public static void WO_KI_NB_JI_bind(Labeled node, FontWeight weight, double size) {
        node.fontProperty().bind(Bindings.createObjectBinding(
            () -> Font.font(FAMILY.get(), weight, size * BASE.get() / 14.0), FAMILY, BASE));
    }

    private static String WO_KI_NB_JI_normalize(String value) {
        if (value == null) return "Plus Jakarta Sans";
        for (String o : WO_KI_NB_JI_options()) if (o.equalsIgnoreCase(value.trim())) return o;
        return WO_KI_NB_JI_options().getFirst();
    }
}

package com.agendapersonal.settings;

import java.util.Map;

/**
 * Deriva los tokens de color de la interfaz a partir de los cuatro colores base del tema
 * (fondo, tarjeta, texto y acento). Los temas "claro" y "oscuro" originales no pasan por aquí.
 */
public final class WO_KI_NB_JI_Palette {
    private WO_KI_NB_JI_Palette() {}

    /** Resultado de resolver los ajustes de apariencia. */
    public record WO_KI_NB_JI_Resolved(boolean dark, String bg, String card, String text, String accent, boolean legacy) {}

    /** Resuelve el tema activo: preset + colores personalizados + modo (claro/oscuro/automático). */
    public static WO_KI_NB_JI_Resolved WO_KI_NB_JI_resolve(WO_KI_NB_JI_SettingsStore s, java.time.LocalTime now) {
        WO_KI_NB_JI_ThemePreset t = WO_KI_NB_JI_ThemePreset.WO_KI_NB_JI_byId(s.WO_KI_NB_JI_str("appearance.themeId"));
        String mode = s.WO_KI_NB_JI_choice("appearance.mode", "claro", "oscuro", "automatico");
        boolean auto = s.WO_KI_NB_JI_bool("appearance.autoThemeByTime") || "automatico".equals(mode);
        if (t.WO_KI_NB_JI_isLegacy()) {
            boolean dark;
            if (auto) dark = WO_KI_NB_JI_isNight(s, now);
            else dark = "oscuro".equals(mode) ? true : "claro".equals(mode) ? false : t.dark();
            // El selector de modo manda sobre los dos temas originales.
            t = WO_KI_NB_JI_ThemePreset.WO_KI_NB_JI_byId(dark ? "oscuro" : "claro");
        }
        String bg = WO_KI_NB_JI_custom(s, "bg", t.bg()), card = WO_KI_NB_JI_custom(s, "card", t.card()), text = WO_KI_NB_JI_custom(s, "text", t.text());
        String accent = WO_KI_NB_JI_custom(s, "accent", null);
        boolean legacy = t.WO_KI_NB_JI_isLegacy() && bg.equals(t.bg()) && card.equals(t.card()) && text.equals(t.text());
        if (accent == null) accent = t.WO_KI_NB_JI_isLegacy() ? null : t.accent();
        boolean dark = t.WO_KI_NB_JI_isLegacy() ? t.dark() : WO_KI_NB_JI_Colors.WO_KI_NB_JI_isDark(bg);
        return new WO_KI_NB_JI_Resolved(dark, bg, card, text, accent, legacy);
    }

    private static String WO_KI_NB_JI_custom(WO_KI_NB_JI_SettingsStore s, String key, String def) {
        String v = WO_KI_NB_JI_Colors.WO_KI_NB_JI_normalize(s.WO_KI_NB_JI_str("appearance.colors." + key));
        return v != null ? v : def;
    }

    private static boolean WO_KI_NB_JI_isNight(WO_KI_NB_JI_SettingsStore s, java.time.LocalTime now) {
        int from = WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_minutes(s.WO_KI_NB_JI_str("appearance.autoDarkFrom")), to = WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_minutes(s.WO_KI_NB_JI_str("appearance.autoDarkTo"));
        int m = now.getHour() * 60 + now.getMinute();
        return from <= to ? (m >= from && m < to) : (m >= from || m < to);
    }

    /** Sobrescribe en {@code p} los tokens de superficie, texto y acento a partir de la base. */
    public static void WO_KI_NB_JI_apply(Map<String, String> p, WO_KI_NB_JI_Resolved r) {
        String bg = r.bg(), card = r.card(), text = r.text();
        boolean d = r.dark();
        String toward = d ? "#FFFFFF" : "#000000";
        p.put("-bg", bg);
        p.put("-surface", card);
        p.put("-surface2", WO_KI_NB_JI_Colors.WO_KI_NB_JI_mix(card, toward, d ? 0.05 : 0.04));
        p.put("-surface3", WO_KI_NB_JI_Colors.WO_KI_NB_JI_mix(card, toward, d ? 0.10 : 0.08));
        p.put("-hover", WO_KI_NB_JI_Colors.WO_KI_NB_JI_mix(card, toward, d ? 0.07 : 0.05));
        p.put("-bord", WO_KI_NB_JI_Colors.WO_KI_NB_JI_mix(bg, toward, d ? 0.12 : 0.10));
        p.put("-bord-strong", WO_KI_NB_JI_Colors.WO_KI_NB_JI_mix(bg, toward, d ? 0.22 : 0.18));
        p.put("-text", text);
        p.put("-text2", WO_KI_NB_JI_Colors.WO_KI_NB_JI_mix(text, bg, 0.22));
        p.put("-muted", WO_KI_NB_JI_Colors.WO_KI_NB_JI_mix(text, bg, 0.40));
        p.put("-icon", WO_KI_NB_JI_Colors.WO_KI_NB_JI_mix(text, bg, 0.32));
        p.put("-sidebar", WO_KI_NB_JI_Colors.WO_KI_NB_JI_mix(bg, d ? "#000000" : "#FFFFFF", d ? 0.18 : 0.0));
        p.put("-input", d ? WO_KI_NB_JI_Colors.WO_KI_NB_JI_mix(card, "#FFFFFF", 0.05) : card);
        p.put("-inverse", text);
        p.put("-on-inverse", bg);
        p.put("-on-inverse-muted", WO_KI_NB_JI_Colors.WO_KI_NB_JI_mix(bg, text, 0.30));
        p.put("-shadow", d ? "rgba(0,0,0,0.35)" : "rgba(28,31,38,0.08)");
    }

    /** Aplica el acento (hex) y calcula el texto sobre él y su fondo suave. */
    public static void WO_KI_NB_JI_applyAccent(Map<String, String> p, String accent, String bg, boolean dark) {
        p.put("-accent", accent);
        p.put("-accent-soft", WO_KI_NB_JI_Colors.WO_KI_NB_JI_mix(bg, accent, dark ? 0.22 : 0.14));
        p.put("-on-accent", WO_KI_NB_JI_Colors.WO_KI_NB_JI_onColor(accent));
    }

    /** Avisos discretos de poco contraste (texto sobre fondo y texto sobre acento). Lista vacía = todo bien. */
    public static java.util.List<String> WO_KI_NB_JI_contrastWarnings(String bg, String card, String text, String accent) {
        java.util.List<String> w = new java.util.ArrayList<>();
        if (WO_KI_NB_JI_Colors.WO_KI_NB_JI_contrast(text, bg) < 4.5) w.add("El texto tiene poco contraste con el fondo.");
        if (WO_KI_NB_JI_Colors.WO_KI_NB_JI_contrast(text, card) < 4.5) w.add("El texto tiene poco contraste con las tarjetas.");
        if (WO_KI_NB_JI_Colors.WO_KI_NB_JI_contrast(WO_KI_NB_JI_Colors.WO_KI_NB_JI_onColor(accent), accent) < 4.5) w.add("El texto sobre el acento se lee con dificultad.");
        if (WO_KI_NB_JI_Colors.WO_KI_NB_JI_contrast(accent, bg) < 2.0) w.add("El acento se distingue poco del fondo.");
        return w;
    }
}

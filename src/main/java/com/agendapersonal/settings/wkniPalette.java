package com.agendapersonal.settings;

import java.util.Map;

/**
 * Deriva los tokens de color de la interfaz a partir de los cuatro colores base del tema
 * (fondo, tarjeta, texto y acento). Los temas "claro" y "oscuro" originales no pasan por aquí.
 */
public final class wkniPalette {
    private wkniPalette() {}

    /** Resultado de resolver los ajustes de apariencia. */
    public record wkniResolved(boolean wkniDark, String wkniBg, String wkniCard, String wkniText, String wkni_accent, boolean wkniLegacy) {}

    /** Resuelve el tema activo: preset + colores personalizados + modo (claro/oscuro/automático). */
    public static wkniResolved wkniResolve(wkniSettingsStore wkniS, java.time.LocalTime wkniNow) {
        wkniThemePreset wkniT = wkniThemePreset.wkniById(wkniS.wkniStr("appearance.themeId"));
        String wkni_mode = wkniS.wkniChoice("appearance.mode", "claro", "oscuro", "automatico");
        boolean wkniAuto = wkniS.wkniBool("appearance.autoThemeByTime") || "automatico".equals(wkni_mode);
        if (wkniT.wkniIsLegacy()) {
            boolean wkniDark;
            if (wkniAuto) wkniDark = wkniIsNight(wkniS, wkniNow);
            else wkniDark = "oscuro".equals(wkni_mode) ? true : "claro".equals(wkni_mode) ? false : wkniT.wkniDark();
            // El selector de modo manda sobre los dos temas originales.
            wkniT = wkniThemePreset.wkniById(wkniDark ? "oscuro" : "claro");
        }
        String wkniBg = wkniCustom(wkniS, "bg", wkniT.wkniBg()), wkniCard = wkniCustom(wkniS, "card", wkniT.wkniCard()), wkniText = wkniCustom(wkniS, "text", wkniT.wkniText());
        String wkni_accent = wkniCustom(wkniS, "accent", null);
        boolean wkniLegacy = wkniT.wkniIsLegacy() && wkniBg.equals(wkniT.wkniBg()) && wkniCard.equals(wkniT.wkniCard()) && wkniText.equals(wkniT.wkniText());
        if (wkni_accent == null) wkni_accent = wkniT.wkniIsLegacy() ? null : wkniT.wkni_accent();
        boolean wkniDark = wkniT.wkniIsLegacy() ? wkniT.wkniDark() : wkniColors.wkniIsDark(wkniBg);
        return new wkniResolved(wkniDark, wkniBg, wkniCard, wkniText, wkni_accent, wkniLegacy);
    }

    private static String wkniCustom(wkniSettingsStore wkniS, String wkniKey, String wkniDef) {
        String wkniV = wkniColors.wkniNormalize(wkniS.wkniStr("appearance.colors." + wkniKey));
        return wkniV != null ? wkniV : wkniDef;
    }

    private static boolean wkniIsNight(wkniSettingsStore wkniS, java.time.LocalTime wkniNow) {
        int wkniFrom = wkniSettingsStore.wkniMinutes(wkniS.wkniStr("appearance.autoDarkFrom")), wkniTo = wkniSettingsStore.wkniMinutes(wkniS.wkniStr("appearance.autoDarkTo"));
        int wkniM = wkniNow.getHour() * 60 + wkniNow.getMinute();
        return wkniFrom <= wkniTo ? (wkniM >= wkniFrom && wkniM < wkniTo) : (wkniM >= wkniFrom || wkniM < wkniTo);
    }

    /** Sobrescribe en {@code p} los tokens de superficie, texto y acento a partir de la base. */
    public static void wkniApply(Map<String, String> wkni_p, wkniResolved wkniR) {
        String wkniBg = wkniR.wkniBg(), wkniCard = wkniR.wkniCard(), wkniText = wkniR.wkniText();
        boolean wkniD = wkniR.wkniDark();
        String wkniToward = wkniD ? "#FFFFFF" : "#000000";
        wkni_p.put("-bg", wkniBg);
        wkni_p.put("-surface", wkniCard);
        wkni_p.put("-surface2", wkniColors.wkniMix(wkniCard, wkniToward, wkniD ? 0.05 : 0.04));
        wkni_p.put("-surface3", wkniColors.wkniMix(wkniCard, wkniToward, wkniD ? 0.10 : 0.08));
        wkni_p.put("-hover", wkniColors.wkniMix(wkniCard, wkniToward, wkniD ? 0.07 : 0.05));
        wkni_p.put("-bord", wkniColors.wkniMix(wkniBg, wkniToward, wkniD ? 0.12 : 0.10));
        wkni_p.put("-bord-strong", wkniColors.wkniMix(wkniBg, wkniToward, wkniD ? 0.22 : 0.18));
        wkni_p.put("-text", wkniText);
        wkni_p.put("-text2", wkniColors.wkniMix(wkniText, wkniBg, 0.22));
        wkni_p.put("-muted", wkniColors.wkniMix(wkniText, wkniBg, 0.40));
        wkni_p.put("-icon", wkniColors.wkniMix(wkniText, wkniBg, 0.32));
        wkni_p.put("-sidebar", wkniColors.wkniMix(wkniBg, wkniD ? "#000000" : "#FFFFFF", wkniD ? 0.18 : 0.0));
        wkni_p.put("-input", wkniD ? wkniColors.wkniMix(wkniCard, "#FFFFFF", 0.05) : wkniCard);
        wkni_p.put("-inverse", wkniText);
        wkni_p.put("-on-inverse", wkniBg);
        wkni_p.put("-on-inverse-muted", wkniColors.wkniMix(wkniBg, wkniText, 0.30));
        wkni_p.put("-shadow", wkniD ? "rgba(0,0,0,0.35)" : "rgba(28,31,38,0.08)");
    }

    /** Aplica el acento (hex) y calcula el texto sobre él y su fondo suave. */
    public static void wkniApplyAccent(Map<String, String> wkni_p, String wkni_accent, String wkniBg, boolean wkniDark) {
        wkni_p.put("-accent", wkni_accent);
        wkni_p.put("-accent-soft", wkniColors.wkniMix(wkniBg, wkni_accent, wkniDark ? 0.22 : 0.14));
        wkni_p.put("-on-accent", wkniColors.wkniOnColor(wkni_accent));
    }

    /** Avisos discretos de poco contraste (texto sobre fondo y texto sobre acento). Lista vacía = todo bien. */
    public static java.util.List<String> wkniContrastWarnings(String wkniBg, String wkniCard, String wkniText, String wkni_accent) {
        java.util.List<String> wkniW = new java.util.ArrayList<>();
        if (wkniColors.wkniContrast(wkniText, wkniBg) < 4.5) wkniW.add("El texto tiene poco contraste con el fondo.");
        if (wkniColors.wkniContrast(wkniText, wkniCard) < 4.5) wkniW.add("El texto tiene poco contraste con las tarjetas.");
        if (wkniColors.wkniContrast(wkniColors.wkniOnColor(wkni_accent), wkni_accent) < 4.5) wkniW.add("El texto sobre el acento se lee con dificultad.");
        if (wkniColors.wkniContrast(wkni_accent, wkniBg) < 2.0) wkniW.add("El acento se distingue poco del fondo.");
        return wkniW;
    }
}

package com.agendapersonal.settings;

import java.util.Locale;

/** Utilidades de color sin dependencias de JavaFX: parseo hex, luminancia, contraste y mezcla. */
public final class WO_KI_NB_JI_Colors {
    private WO_KI_NB_JI_Colors() {}

    /** Devuelve "#RRGGBB" en mayúsculas o null si no es un hex válido (#RGB o #RRGGBB). */
    public static String WO_KI_NB_JI_normalize(String hex) {
        if (hex == null) return null;
        String h = hex.trim();
        if (h.startsWith("#")) h = h.substring(1);
        if (h.matches("[0-9a-fA-F]{3}")) h = "" + h.charAt(0) + h.charAt(0) + h.charAt(1) + h.charAt(1) + h.charAt(2) + h.charAt(2);
        if (!h.matches("[0-9a-fA-F]{6}")) return null;
        return "#" + h.toUpperCase(Locale.ROOT);
    }

    public static int[] WO_KI_NB_JI_rgb(String hex) {
        String n = WO_KI_NB_JI_normalize(hex);
        if (n == null) n = "#000000";
        return new int[]{ Integer.parseInt(n.substring(1, 3), 16), Integer.parseInt(n.substring(3, 5), 16), Integer.parseInt(n.substring(5, 7), 16) };
    }

    public static String WO_KI_NB_JI_hex(int r, int g, int b) {
        return String.format(Locale.ROOT, "#%02X%02X%02X", WO_KI_NB_JI_clamp(r), WO_KI_NB_JI_clamp(g), WO_KI_NB_JI_clamp(b));
    }

    private static int WO_KI_NB_JI_clamp(int v) { return Math.max(0, Math.min(255, v)); }

    /** Luminancia relativa WCAG (0 a 1). */
    public static double WO_KI_NB_JI_luminance(String hex) {
        int[] c = WO_KI_NB_JI_rgb(hex);
        double[] l = new double[3];
        for (int k = 0; k < 3; k++) {
            double v = c[k] / 255.0;
            l[k] = v <= 0.03928 ? v / 12.92 : Math.pow((v + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * l[0] + 0.7152 * l[1] + 0.0722 * l[2];
    }

    public static boolean WO_KI_NB_JI_isDark(String hex) { return WO_KI_NB_JI_luminance(hex) < 0.35; }

    /** Razón de contraste WCAG (1 a 21). */
    public static double WO_KI_NB_JI_contrast(String a, String b) {
        double la = WO_KI_NB_JI_luminance(a), lb = WO_KI_NB_JI_luminance(b);
        return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
    }

    /** Texto sobre un color de fondo (por ejemplo el acento): el de mayor contraste entre claro y oscuro. */
    public static String WO_KI_NB_JI_onColor(String bg) {
        String dark = "#16110E", light = "#FFFFFF";
        return WO_KI_NB_JI_contrast(bg, dark) >= WO_KI_NB_JI_contrast(bg, light) ? dark : light;
    }

    /** Mezcla: t=0 devuelve a, t=1 devuelve b. */
    public static String WO_KI_NB_JI_mix(String a, String b, double t) {
        int[] x = WO_KI_NB_JI_rgb(a), y = WO_KI_NB_JI_rgb(b);
        return WO_KI_NB_JI_hex((int) Math.round(x[0] + (y[0] - x[0]) * t), (int) Math.round(x[1] + (y[1] - x[1]) * t), (int) Math.round(x[2] + (y[2] - x[2]) * t));
    }
}

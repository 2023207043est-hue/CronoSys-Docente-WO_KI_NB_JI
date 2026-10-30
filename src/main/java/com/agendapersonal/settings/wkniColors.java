package com.agendapersonal.settings;

import java.util.Locale;

/** Utilidades de color sin dependencias de JavaFX: parseo hex, luminancia, contraste y mezcla. */
public final class wkniColors {
    private wkniColors() {}

    /** Devuelve "#RRGGBB" en mayúsculas o null si no es un hex válido (#RGB o #RRGGBB). */
    public static String wkniNormalize(String wkniHex) {
        if (wkniHex == null) return null;
        String wkniH = wkniHex.trim();
        if (wkniH.startsWith("#")) wkniH = wkniH.substring(1);
        if (wkniH.matches("[0-9a-fA-F]{3}")) wkniH = "" + wkniH.charAt(0) + wkniH.charAt(0) + wkniH.charAt(1) + wkniH.charAt(1) + wkniH.charAt(2) + wkniH.charAt(2);
        if (!wkniH.matches("[0-9a-fA-F]{6}")) return null;
        return "#" + wkniH.toUpperCase(Locale.ROOT);
    }

    public static int[] wkniRgb(String wkniHex) {
        String wkniN = wkniNormalize(wkniHex);
        if (wkniN == null) wkniN = "#000000";
        return new int[]{ Integer.parseInt(wkniN.substring(1, 3), 16), Integer.parseInt(wkniN.substring(3, 5), 16), Integer.parseInt(wkniN.substring(5, 7), 16) };
    }

    public static String wkniHex(int wkniR, int wkniG, int wkniB) {
        return String.format(Locale.ROOT, "#%02X%02X%02X", wkniClamp(wkniR), wkniClamp(wkniG), wkniClamp(wkniB));
    }

    private static int wkniClamp(int wkniV) { return Math.max(0, Math.min(255, wkniV)); }

    /** Luminancia relativa WCAG (0 a 1). */
    public static double wkniLuminance(String wkniHex) {
        int[] wkniC = wkniRgb(wkniHex);
        double[] wkniL = new double[3];
        for (int wkniK = 0; wkniK < 3; wkniK++) {
            double wkniV = wkniC[wkniK] / 255.0;
            wkniL[wkniK] = wkniV <= 0.03928 ? wkniV / 12.92 : Math.pow((wkniV + 0.055) / 1.055, 2.4);
        }
        return 0.2126 * wkniL[0] + 0.7152 * wkniL[1] + 0.0722 * wkniL[2];
    }

    public static boolean wkniIsDark(String wkniHex) { return wkniLuminance(wkniHex) < 0.35; }

    /** Razón de contraste WCAG (1 a 21). */
    public static double wkniContrast(String wkniA, String wkniB) {
        double wkniLa = wkniLuminance(wkniA), wkniLb = wkniLuminance(wkniB);
        return (Math.max(wkniLa, wkniLb) + 0.05) / (Math.min(wkniLa, wkniLb) + 0.05);
    }

    /** Texto sobre un color de fondo (por ejemplo el acento): el de mayor contraste entre claro y oscuro. */
    public static String wkniOnColor(String wkniBg) {
        String wkniDark = "#16110E", wkniLight = "#FFFFFF";
        return wkniContrast(wkniBg, wkniDark) >= wkniContrast(wkniBg, wkniLight) ? wkniDark : wkniLight;
    }

    /** Mezcla: t=0 devuelve a, t=1 devuelve b. */
    public static String wkniMix(String wkniA, String wkniB, double wkniT) {
        int[] wkniX = wkniRgb(wkniA), wkniY = wkniRgb(wkniB);
        return wkniHex((int) Math.round(wkniX[0] + (wkniY[0] - wkniX[0]) * wkniT), (int) Math.round(wkniX[1] + (wkniY[1] - wkniX[1]) * wkniT), (int) Math.round(wkniX[2] + (wkniY[2] - wkniX[2]) * wkniT));
    }
}

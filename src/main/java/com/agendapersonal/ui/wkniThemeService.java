package com.agendapersonal.ui;

import com.agendapersonal.settings.wkniPalette;
import com.agendapersonal.settings.wkniSettingsStore;
import com.agendapersonal.settings.wkniThemePreset;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;
import java.time.LocalTime;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.collections.ListChangeListener;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.effect.ColorAdjust;
import javafx.scene.effect.SepiaTone;
import javafx.stage.PopupWindow;
import javafx.stage.Window;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Tema global de Compás (claro/oscuro + color de acento + tipografía).
 *
 * <p>Antes la paleta se pegaba como estilo en línea solo en la raíz de la ventana principal, así que
 * los diálogos, alertas, avisos de escritorio y el resumen se quedaban con los colores viejos al
 * cambiar de apariencia. Ahora la paleta se genera como una hoja CSS propia y se instala en
 * <b>todas</b> las ventanas abiertas (y en las que se abran después). Al cambiar la apariencia se
 * sustituye esa hoja y JavaFX vuelve a aplicar el CSS en toda la app al instante.</p>
 */
public final class wkniThemeService {
    private static final String wkniTHEME_SHEET_PREFIX = "data:text/css;base64,";
    private static final wkniThemeService wkniINSTANCE = new wkniThemeService();

    public enum wkniMode {
        CLARO("Claro"), OSCURO("Oscuro");
        private final String wkniLabel;
        wkniMode(String wkniLabel) { this.wkniLabel = wkniLabel; }
        @Override public String toString() { return wkniLabel; }
        public static wkniMode wkniFrom(String wkniValue) { return "Claro".equalsIgnoreCase(wkniValue) ? CLARO : OSCURO; }
    }

    /** Colores de acento: tono para tema claro, tono para tema oscuro y fondo suave de cada uno. */
    public enum wkniAccent {
        NARANJA("Naranja", "#C64B1C", "#F08A5D", "#FBE9DF", "#3F2519"),
        AZUL("Azul", "#2F5BB7", "#7EA4F2", "#E6EEFB", "#1E2A42"),
        VERDE("Verde", "#2C7A4B", "#5EC48A", "#E3F3E8", "#1C3527"),
        VIOLETA("Violeta", "#6340B8", "#A98DF0", "#EEE8FB", "#2D2545"),
        ROSADO("Rosado", "#B8326A", "#F08BB4", "#FBE6EF", "#3E1F2D"),
        ROJO("Rojo", "#B3261E", "#F2867E", "#FCE8E6", "#3D1E1D");

        private final String wkniLabel, wkniLight, wkniDark, wkniSoftLight, wkniSoftDark;
        wkniAccent(String wkniLabel, String wkniLight, String wkniDark, String wkniSoftLight, String wkniSoftDark) {
            this.wkniLabel = wkniLabel; this.wkniLight = wkniLight; this.wkniDark = wkniDark; this.wkniSoftLight = wkniSoftLight; this.wkniSoftDark = wkniSoftDark;
        }
        @Override public String toString() { return wkniLabel; }
        public String wkniColor(boolean wkniDarkMode) { return wkniDarkMode ? wkniDark : wkniLight; }
        public String wkniSoft(boolean wkniDarkMode) { return wkniDarkMode ? wkniSoftDark : wkniSoftLight; }
        public static wkniAccent wkniFrom(String wkniValue) {
            try { return valueOf(wkniValue.toUpperCase(Locale.ROOT)); }
            catch (Exception wkniIgnored) { return NARANJA; }
        }
    }

    private final wkniSettingsStore wkniStore = wkniSettingsStore.wkniGet();
    private wkniPalette.wkniResolved wkni_resolved = wkniPalette.wkniResolve(wkniStore, LocalTime.now());
    private wkniAccent wkni_accent = wkniAccent.wkniFrom(wkniStore.wkniStr("appearance.accentId"));
    private Timeline wkniClock;
    private final ReadOnlyIntegerWrapper wkniRevision = new ReadOnlyIntegerWrapper(0);
    private String wkniSheet;
    private boolean wkniInstalled;

    private wkniThemeService() {
        wkniIconFactory.wkniSetDarkMode(wkni_resolved.wkniDark());
        // Cualquier cambio de ajustes de apariencia (o de perfil) se aplica al instante.
        wkniStore.wkniAddListener(wkniSection -> { if (wkniSection.equals("appearance") || wkniSection.equals("*")) wkniRefresh(); });
    }

    public wkniSettingsStore wkniSettings() { return wkniStore; }
    public wkniPalette.wkniResolved wkniResolved() { return wkni_resolved; }

    public static wkniThemeService wkniGet() { return wkniINSTANCE; }

    public wkniMode wkniMode() { return wkni_resolved.wkniDark() ? wkniMode.OSCURO : wkniMode.CLARO; }
    public wkniAccent wkniAccent() { return wkni_accent; }
    public boolean wkniIsDark() { return wkni_resolved.wkniDark(); }
    public wkniThemePreset wkniPreset() { return wkniThemePreset.wkniById(wkniStore.wkniStr("appearance.themeId")); }
    /** Radio de esquinas (0 a 28 px). */
    public int wkniRadius() { return wkniStore.wkniInteger("appearance.radius", 0, 28); }
    public boolean wkniReduceMotion() { return wkniStore.wkniBool("appearance.reduceMotion"); }

    /** Aumenta cada vez que cambia la apariencia (para quien necesite redibujar algo a mano). */
    public ReadOnlyIntegerProperty wkniRevisionProperty() { return wkniRevision.getReadOnlyProperty(); }

    /** Elige Claro u Oscuro (los temas originales) y desactiva el cambio automático por hora. */
    public void wkniSetMode(wkniMode wkniM) {
        if (wkniM == null || wkniM == wkniMode()) return;
        String wkniId = wkniM == wkniMode.OSCURO ? "oscuro" : "claro";
        wkniStore.wkniSet("appearance.autoThemeByTime", false);
        wkniStore.wkniSet("appearance.mode", wkniId);
        wkniStore.wkniSet("appearance.themeId", wkniId);     // dispara refresh() por el oyente
    }

    /** Elige uno de los acentos de la paleta. En los temas originales se usa su tono claro/oscuro. */
    public void wkniSetAccent(wkniAccent wkniA) {
        if (wkniA == null) return;
        wkniStore.wkniSet("appearance.accentId", wkniA.name());
        wkniStore.wkniSet("appearance.colors.accent", wkniPreset().wkniIsLegacy() ? null : wkniA.wkniColor(wkniIsDark()));
    }

    /** Elige cualquier tema predefinido. Quita los colores personalizados para ver el tema tal cual. */
    public void wkniSetTheme(String wkniId) {
        if (!wkniThemePreset.wkniExists(wkniId)) return;
        wkniStore.wkniSet("appearance.colors.bg", null);
        wkniStore.wkniSet("appearance.colors.card", null);
        wkniStore.wkniSet("appearance.colors.text", null);
        wkniStore.wkniSet("appearance.colors.accent", null);
        wkniStore.wkniSet("appearance.mode", wkniThemePreset.wkniById(wkniId).wkniIsLegacy() ? wkniId : "claro");
        wkniStore.wkniSet("appearance.themeId", wkniId);
    }

    public void wkniToggleMode() { wkniSetMode(wkniIsDark() ? wkniMode.CLARO : wkniMode.OSCURO); }

    // Compatibilidad con el código anterior (la raíz ya no hace falta).
    public void wkniSetMode(wkniMode wkniM, Parent wkniIgnored) { wkniSetMode(wkniM); }
    public void wkniSetAccent(wkniAccent wkniA, Parent wkniIgnored) { wkniSetAccent(wkniA); }
    public void wkniApply(Parent wkniIgnored) { wkniRefresh(); }

    /** Engancha el tema a todas las ventanas: las abiertas y las que se creen después. */
    public void wkniInstall() {
        if (wkniInstalled) return;
        wkniInstalled = true;
        wkniSheet = wkniBuildSheet();
        wkniStartClock();
        Window.getWindows().addListener((ListChangeListener<Window>) wkniCh -> {
            while (wkniCh.next()) for (Window wkniW : wkniCh.getAddedSubList()) wkniHook(wkniW);
        });
        for (Window wkniW : Window.getWindows()) wkniHook(wkniW);
    }

    /** Aplica la hoja base y la del tema a una escena concreta (útil antes de mostrarla). */
    public void wkniApplyTo(Scene wkniScene) {
        if (wkniScene == null) return;
        if (wkniSheet == null) wkniSheet = wkniBuildSheet();
        var wkniSheets = wkniScene.getStylesheets();
        wkniSheets.removeIf(wkniS -> wkniS.startsWith(wkniTHEME_SHEET_PREFIX));
        // UNA sola hoja: styles.css y la del tema van juntas. Si se cargaran por separado, JavaFX
        // pierde el valor de las variables propias (-rN, -dv-…) al aplicar la segunda y las esquinas
        // redondeadas no se aplican (y salían avisos de conversión en el log).
        wkniSheets.add(wkniSheet);
        wkniApplyEffects(wkniScene);
    }

    /** Filtro "bajo contraste y luz cálida" para cuidar la vista (solo si el usuario lo activa). */
    private void wkniApplyEffects(Scene wkniScene) {
        Parent wkniRoot = wkniScene.getRoot();
        if (wkniRoot == null) return;
        if (wkniStore.wkniBool("appearance.warmLowContrast")) {
            ColorAdjust wkniAdj = new ColorAdjust();
            wkniAdj.setContrast(-0.08);
            wkniAdj.setSaturation(-0.05);
            wkniAdj.setInput(new SepiaTone(0.14));
            wkniRoot.setEffect(wkniAdj);
        } else if (wkniRoot.getEffect() instanceof ColorAdjust) {
            wkniRoot.setEffect(null);
        }
    }

    /** Regenera la paleta y la sustituye en todas las ventanas abiertas. */
    public void wkniRefresh() {
        Runnable wkniR = () -> {
            wkniFontPreferences.wkniReload();
            wkni_resolved = wkniPalette.wkniResolve(wkniStore, LocalTime.now());
            wkni_accent = wkniAccent.wkniFrom(wkniStore.wkniStr("appearance.accentId"));
            wkniIconFactory.wkniSetDarkMode(wkniIsDark());
            wkniSheet = wkniBuildSheet();
            for (Window wkniW : Window.getWindows()) if (!(wkniW instanceof PopupWindow) && wkniW.getScene() != null) wkniApplyTo(wkniW.getScene());
            wkniRevision.set(wkniRevision.get() + 1);
        };
        if (Platform.isFxApplicationThread()) wkniR.run(); else Platform.runLater(wkniR);
    }

    /** Cada minuto: activa el perfil que toque por horario y el tema claro/oscuro por hora. */
    private void wkniStartClock() {
        if (wkniClock != null) return;
        wkniClock = new Timeline(new KeyFrame(Duration.seconds(60), wkniE -> {
            LocalTime wkniNow = LocalTime.now();
            int wkniDia = java.time.DayOfWeek.from(java.time.LocalDate.now()).getValue();   // 1 = lunes
            if (wkniStore.wkniApplySchedule(wkniNow.getHour() * 60 + wkniNow.getMinute(), wkniDia)) return;   // el cambio de perfil ya refresca
            boolean wkniDark = wkniPalette.wkniResolve(wkniStore, wkniNow).wkniDark();
            if (wkniDark != wkni_resolved.wkniDark()) wkniRefresh();
        }));
        wkniClock.setCycleCount(Animation.INDEFINITE);
        wkniClock.play();
    }

    private void wkniHook(Window wkniW) {
        if (wkniW instanceof PopupWindow) return;          // los menús y popups heredan de su ventana dueña
        if (wkniW.getScene() != null) wkniApplyTo(wkniW.getScene());
        wkniW.sceneProperty().addListener((wkniO, wkniA, wkniS) -> wkniApplyTo(wkniS));
    }

    /** Valor hexadecimal actual de un color del tema (para lo que no se pinta con CSS, p. ej. AWT). */
    public String wkniColor(String wkniToken) {
        return wkniPalette().getOrDefault(wkniToken, "#888888");
    }

    // ---------------------------------------------------------------- paleta

    private Map<String, String> wkniPalette() {
        boolean wkniD = wkniIsDark();
        Map<String, String> wkni_p = new LinkedHashMap<>();
        if (!wkniD) {
            wkni_p.put("-bg", "#F5F4F0");          wkni_p.put("-surface", "#FFFFFF");
            wkni_p.put("-surface2", "#F1F0EB");    wkni_p.put("-surface3", "#E9E7E1");
            wkni_p.put("-hover", "#F1F0EB");       wkni_p.put("-bord", "#E7E5DF");
            wkni_p.put("-bord-strong", "#D6D3CB"); wkni_p.put("-text", "#1C1F26");
            wkni_p.put("-text2", "#4A4F5C");       wkni_p.put("-muted", "#646978");
            wkni_p.put("-icon", "#5B606D");        wkni_p.put("-sidebar", "#F5F4F0");
            wkni_p.put("-input", "#FFFFFF");       wkni_p.put("-inverse", "#1C1F26");
            wkni_p.put("-on-inverse", "#FFFFFF");  wkni_p.put("-on-inverse-muted", "#C3C7D0");
            wkni_p.put("-shadow", "rgba(28,31,38,0.08)");
            wkniTint(wkni_p, "blue", "#2F5BB7", "#E6EEFB");
            wkniTint(wkni_p, "amber", "#8A5A00", "#FBF1D9");
            wkniTint(wkni_p, "violet", "#6340B8", "#EEE8FB");
            wkniTint(wkni_p, "green", "#2C7A4B", "#E3F3E8");
            wkniTint(wkni_p, "teal", "#1F6F6A", "#DFF1F0");
            wkniTint(wkni_p, "rose", "#B23A55", "#FBE6EA");
            wkniTint(wkni_p, "orange", "#B4461A", "#FBE9DF");
            wkniTint(wkni_p, "slate", "#4A4F5C", "#EFEEE9");
            wkni_p.put("-danger", "#B3261E");      wkni_p.put("-danger-soft", "#FCE8E6");
            wkniPrio(wkni_p, "#D64545", "#FCE8E6", "#E07B1F", "#FDEBD8", "#3C9D62", "#E3F3E8");
        } else {
            wkni_p.put("-bg", "#121419");          wkni_p.put("-surface", "#1B1E25");
            wkni_p.put("-surface2", "#22262E");    wkni_p.put("-surface3", "#2B3039");
            wkni_p.put("-hover", "#252930");       wkni_p.put("-bord", "#2C3139");
            wkni_p.put("-bord-strong", "#3B414C"); wkni_p.put("-text", "#ECEDF0");
            wkni_p.put("-text2", "#C3C7D0");       wkni_p.put("-muted", "#9AA0AC");
            wkni_p.put("-icon", "#A9AEB9");        wkni_p.put("-sidebar", "#16181D");
            wkni_p.put("-input", "#22262E");       wkni_p.put("-inverse", "#ECEDF0");
            wkni_p.put("-on-inverse", "#14161B");  wkni_p.put("-on-inverse-muted", "#4A4F5C");
            wkni_p.put("-shadow", "rgba(0,0,0,0.35)");
            wkniTint(wkni_p, "blue", "#8DB0F5", "#1E2A42");
            wkniTint(wkni_p, "amber", "#E9B949", "#3A2F14");
            wkniTint(wkni_p, "violet", "#B9A3F2", "#2D2545");
            wkniTint(wkni_p, "green", "#6FCF97", "#1C3527");
            wkniTint(wkni_p, "teal", "#6BCFC7", "#173634");
            wkniTint(wkni_p, "rose", "#F28BA3", "#3E1F28");
            wkniTint(wkni_p, "orange", "#F4A27A", "#3F2519");
            wkniTint(wkni_p, "slate", "#C3C7D0", "#2A2F38");
            wkni_p.put("-danger", "#F2867E");      wkni_p.put("-danger-soft", "#3D1E1D");
            wkniPrio(wkni_p, "#F2867E", "#3D1E1D", "#F0A04B", "#3F2A14", "#6FCF97", "#1C3527");
        }
        wkni_p.put("-success", wkni_p.get("-c-green"));  wkni_p.put("-success-soft", wkni_p.get("-c-green-soft"));
        wkni_p.put("-warning", wkni_p.get("-c-amber"));  wkni_p.put("-warning-soft", wkni_p.get("-c-amber-soft"));
        wkni_p.put("-info", wkni_p.get("-c-blue"));      wkni_p.put("-info-soft", wkni_p.get("-c-blue-soft"));
        if (!wkni_resolved.wkniLegacy()) wkniPalette.wkniApply(wkni_p, wkni_resolved);
        if (wkni_resolved.wkni_accent() != null) {
            wkniPalette.wkniApplyAccent(wkni_p, wkni_resolved.wkni_accent(), wkni_p.get("-bg"), wkniD);   // el texto sobre el acento se calcula solo
        } else {
            wkni_p.put("-accent", wkni_accent.wkniColor(wkniD));
            wkni_p.put("-accent-soft", wkni_accent.wkniSoft(wkniD));
            wkni_p.put("-on-accent", wkniD ? "#16110E" : "#FFFFFF");
        }
        wkni_p.put("-on-danger", wkniD ? "#1C0E0E" : "#FFFFFF");
        // Colores de los segmentos de la Agenda Unificada (TipoEntrada usa estos nombres).
        wkni_p.put("-t-evento", wkni_p.get("-c-teal"));    wkni_p.put("-t-evento-soft", wkni_p.get("-c-teal-soft"));
        wkni_p.put("-t-trabajo", wkni_p.get("-c-blue"));   wkni_p.put("-t-trabajo-soft", wkni_p.get("-c-blue-soft"));
        wkni_p.put("-t-tarea", wkni_p.get("-c-violet"));   wkni_p.put("-t-tarea-soft", wkni_p.get("-c-violet-soft"));
        wkni_p.put("-t-habito", wkni_p.get("-c-green"));   wkni_p.put("-t-habito-soft", wkni_p.get("-c-green-soft"));
        wkni_p.put("-t-cumple", wkni_p.get("-c-rose"));    wkni_p.put("-t-cumple-soft", wkni_p.get("-c-rose-soft"));
        return wkni_p;
    }

    /** Colores de prioridad: alta = rojo, media = naranja, baja = verde. */
    private static void wkniPrio(Map<String, String> wkni_p, String wkniA, String wkniAS, String wkniM, String wkniMS, String wkniB, String wkniBS) {
        wkni_p.put("-p-alta", wkniA);  wkni_p.put("-p-alta-soft", wkniAS);
        wkni_p.put("-p-media", wkniM); wkni_p.put("-p-media-soft", wkniMS);
        wkni_p.put("-p-baja", wkniB);  wkni_p.put("-p-baja-soft", wkniBS);
    }

    private static void wkniTint(Map<String, String> wkni_p, String wkniName, String wkniFg, String wkniSoft) {
        wkni_p.put("-c-" + wkniName, wkniFg);
        wkni_p.put("-c-" + wkniName + "-soft", wkniSoft);
    }

    private String wkniBuildSheet() {
        StringBuilder wkniVariables = new StringBuilder(".root {\n");
        wkniPalette().forEach((wkniK, wkniV) -> wkniVariables.append("    ").append(wkniK).append(": ").append(wkniV).append(";\n"));
        wkniVariables.append("    -fx-control-inner-background: -input;\n")
            .append("    -fx-control-inner-background-alt: -surface2;\n")
            .append("    -fx-text-background-color: -text;\n")
            .append("    -fx-selection-bar: -accent-soft;\n")
            .append("    -fx-selection-bar-non-focused: -surface3;\n")
            .append("    -fx-selection-bar-text: -text;\n")
            .append("    -fx-accent: -accent;\n")
            .append("    -fx-focus-color: -accent;\n")
            .append("    -fx-faint-focus-color: transparent;\n")
            .append("    -fx-base: -surface;\n")
            .append("    -fx-background: -bg;\n")
            .append("    -fx-font-family: \"").append(wkniFontPreferences.wkniFamily()).append("\";\n")
            .append("    -fx-font-size: ").append(String.format(Locale.ROOT, "%.1f", wkniFontPreferences.wkniBaseSize())).append("px;\n")
            .append("}\n");

        // La hoja base y las variables del tema van en UNA sola hoja: se sustituye el bloque ".root"
        // de styles.css por el del tema activo (si no, el tema no se aplicaría).
        String wkniBase = wkniLeerHojaBase();
        String wkniCss;
        if (wkniBase == null) {
            wkniCss = wkniVariables.toString();                       // sin styles.css en el classpath: solo el tema
        } else {
            String wkniConNumeros = wkniSustituirTokens(wkniBase);
            java.util.regex.Matcher wkniM = java.util.regex.Pattern.compile("(?s)\\.root\\s*\\{.*?\\}").matcher(wkniConNumeros);
            wkniCss = wkniM.find()
                ? wkniConNumeros.substring(0, wkniM.start()) + wkniVariables + wkniConNumeros.substring(wkniM.end())
                : wkniVariables + wkniConNumeros;
        }
        return wkniTHEME_SHEET_PREFIX + Base64.getEncoder().encodeToString(wkniCss.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sustituye los tokens de forma ({@code -r12}, {@code -dv-btn}…) por números literales.
     *
     * <p>Es necesario porque JavaFX solo convierte las variables propias a <i>colores</i>: si se usan
     * como tamaño ({@code -fx-border-radius: -r12}) no se aplican y salen avisos de conversión. Con los
     * números ya puestos, las esquinas redondeadas y los tamaños de los botones sí funcionan, y el
     * ajuste «radio de esquinas» y «densidad» los siguen controlando.</p>
     */
    private String wkniSustituirTokens(String wkniCss) {
        double wkniK = wkniRadius() / 12.0;
        java.util.regex.Matcher wkniM = java.util.regex.Pattern.compile("(?<![\\w-])-r(\\d{1,2})(?![\\w-])").matcher(wkniCss);
        StringBuffer wkniOut = new StringBuffer();
        while (wkniM.find()) {
            int wkniN = Integer.parseInt(wkniM.group(1));
            wkniM.appendReplacement(wkniOut, java.util.regex.Matcher.quoteReplacement(String.valueOf(Math.round(wkniN * wkniK))));
        }
        wkniM.appendTail(wkniOut);
        String wkniS = wkniOut.toString();
        int[] wkniDens = switch (wkniStore.wkniChoice("appearance.density", "compacta", "comoda", "amplia")) {
            case "compacta" -> new int[]{6, 34, 7, 38};
            case "amplia" -> new int[]{12, 46, 13, 50};
            default -> new int[]{9, 40, 10, 44};
        };
        return wkniS.replace("-dv-btn", String.valueOf(wkniDens[0]))
            .replace("-h-btn", String.valueOf(wkniDens[1]))
            .replace("-dv-menu", String.valueOf(wkniDens[2]))
            .replace("-h-menu", String.valueOf(wkniDens[3]));
    }

    /** Contenido de /styles.css (la hoja base). null si no está en el classpath. */
    private static String wkniLeerHojaBase() {
        try (var wkniIn = wkniThemeService.class.getResourceAsStream("/styles.css")) {
            if (wkniIn == null) return null;
            return new String(wkniIn.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception wkniE) {
            return null;
        }
    }
}

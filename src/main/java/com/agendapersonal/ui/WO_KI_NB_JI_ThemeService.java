package com.agendapersonal.ui;

import com.agendapersonal.settings.WO_KI_NB_JI_Palette;
import com.agendapersonal.settings.WO_KI_NB_JI_SettingsStore;
import com.agendapersonal.settings.WO_KI_NB_JI_ThemePreset;
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
public final class WO_KI_NB_JI_ThemeService {
    private static final String THEME_SHEET_PREFIX = "data:text/css;base64,";
    private static final WO_KI_NB_JI_ThemeService INSTANCE = new WO_KI_NB_JI_ThemeService();

    public enum WO_KI_NB_JI_Mode {
        CLARO("Claro"), OSCURO("Oscuro");
        private final String label;
        WO_KI_NB_JI_Mode(String label) { this.label = label; }
        @Override public String toString() { return label; }
        public static WO_KI_NB_JI_Mode WO_KI_NB_JI_from(String value) { return "Claro".equalsIgnoreCase(value) ? CLARO : OSCURO; }
    }

    /** Colores de acento: tono para tema claro, tono para tema oscuro y fondo suave de cada uno. */
    public enum WO_KI_NB_JI_Accent {
        NARANJA("Naranja", "#C64B1C", "#F08A5D", "#FBE9DF", "#3F2519"),
        AZUL("Azul", "#2F5BB7", "#7EA4F2", "#E6EEFB", "#1E2A42"),
        VERDE("Verde", "#2C7A4B", "#5EC48A", "#E3F3E8", "#1C3527"),
        VIOLETA("Violeta", "#6340B8", "#A98DF0", "#EEE8FB", "#2D2545"),
        ROSADO("Rosado", "#B8326A", "#F08BB4", "#FBE6EF", "#3E1F2D"),
        ROJO("Rojo", "#B3261E", "#F2867E", "#FCE8E6", "#3D1E1D");

        private final String label, light, dark, softLight, softDark;
        WO_KI_NB_JI_Accent(String label, String light, String dark, String softLight, String softDark) {
            this.label = label; this.light = light; this.dark = dark; this.softLight = softLight; this.softDark = softDark;
        }
        @Override public String toString() { return label; }
        public String WO_KI_NB_JI_color(boolean darkMode) { return darkMode ? dark : light; }
        public String WO_KI_NB_JI_soft(boolean darkMode) { return darkMode ? softDark : softLight; }
        public static WO_KI_NB_JI_Accent WO_KI_NB_JI_from(String value) {
            try { return valueOf(value.toUpperCase(Locale.ROOT)); }
            catch (Exception ignored) { return NARANJA; }
        }
    }

    private final WO_KI_NB_JI_SettingsStore store = WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_get();
    private WO_KI_NB_JI_Palette.WO_KI_NB_JI_Resolved resolved = WO_KI_NB_JI_Palette.WO_KI_NB_JI_resolve(store, LocalTime.now());
    private WO_KI_NB_JI_Accent accent = WO_KI_NB_JI_Accent.WO_KI_NB_JI_from(store.WO_KI_NB_JI_str("appearance.accentId"));
    private Timeline clock;
    private final ReadOnlyIntegerWrapper revision = new ReadOnlyIntegerWrapper(0);
    private String sheet;
    private boolean installed;

    private WO_KI_NB_JI_ThemeService() {
        WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_setDarkMode(resolved.dark());
        // Cualquier cambio de ajustes de apariencia (o de perfil) se aplica al instante.
        store.WO_KI_NB_JI_addListener(section -> { if (section.equals("appearance") || section.equals("*")) WO_KI_NB_JI_refresh(); });
    }

    public WO_KI_NB_JI_SettingsStore WO_KI_NB_JI_settings() { return store; }
    public WO_KI_NB_JI_Palette.WO_KI_NB_JI_Resolved WO_KI_NB_JI_resolved() { return resolved; }

    public static WO_KI_NB_JI_ThemeService WO_KI_NB_JI_get() { return INSTANCE; }

    public WO_KI_NB_JI_Mode WO_KI_NB_JI_mode() { return resolved.dark() ? WO_KI_NB_JI_Mode.OSCURO : WO_KI_NB_JI_Mode.CLARO; }
    public WO_KI_NB_JI_Accent WO_KI_NB_JI_accent() { return accent; }
    public boolean WO_KI_NB_JI_isDark() { return resolved.dark(); }
    public WO_KI_NB_JI_ThemePreset WO_KI_NB_JI_preset() { return WO_KI_NB_JI_ThemePreset.WO_KI_NB_JI_byId(store.WO_KI_NB_JI_str("appearance.themeId")); }
    /** Radio de esquinas (0 a 28 px). */
    public int WO_KI_NB_JI_radius() { return store.WO_KI_NB_JI_integer("appearance.radius", 0, 28); }
    public boolean WO_KI_NB_JI_reduceMotion() { return store.WO_KI_NB_JI_bool("appearance.reduceMotion"); }

    /** Aumenta cada vez que cambia la apariencia (para quien necesite redibujar algo a mano). */
    public ReadOnlyIntegerProperty WO_KI_NB_JI_revisionProperty() { return revision.getReadOnlyProperty(); }

    /** Elige Claro u Oscuro (los temas originales) y desactiva el cambio automático por hora. */
    public void WO_KI_NB_JI_setMode(WO_KI_NB_JI_Mode m) {
        if (m == null || m == WO_KI_NB_JI_mode()) return;
        String id = m == WO_KI_NB_JI_Mode.OSCURO ? "oscuro" : "claro";
        store.WO_KI_NB_JI_set("appearance.autoThemeByTime", false);
        store.WO_KI_NB_JI_set("appearance.mode", id);
        store.WO_KI_NB_JI_set("appearance.themeId", id);     // dispara refresh() por el oyente
    }

    /** Elige uno de los acentos de la paleta. En los temas originales se usa su tono claro/oscuro. */
    public void WO_KI_NB_JI_setAccent(WO_KI_NB_JI_Accent a) {
        if (a == null) return;
        store.WO_KI_NB_JI_set("appearance.accentId", a.name());
        store.WO_KI_NB_JI_set("appearance.colors.accent", WO_KI_NB_JI_preset().WO_KI_NB_JI_isLegacy() ? null : a.WO_KI_NB_JI_color(WO_KI_NB_JI_isDark()));
    }

    /** Elige cualquier tema predefinido. Quita los colores personalizados para ver el tema tal cual. */
    public void WO_KI_NB_JI_setTheme(String id) {
        if (!WO_KI_NB_JI_ThemePreset.WO_KI_NB_JI_exists(id)) return;
        store.WO_KI_NB_JI_set("appearance.colors.bg", null);
        store.WO_KI_NB_JI_set("appearance.colors.card", null);
        store.WO_KI_NB_JI_set("appearance.colors.text", null);
        store.WO_KI_NB_JI_set("appearance.colors.accent", null);
        store.WO_KI_NB_JI_set("appearance.mode", WO_KI_NB_JI_ThemePreset.WO_KI_NB_JI_byId(id).WO_KI_NB_JI_isLegacy() ? id : "claro");
        store.WO_KI_NB_JI_set("appearance.themeId", id);
    }

    public void WO_KI_NB_JI_toggleMode() { WO_KI_NB_JI_setMode(WO_KI_NB_JI_isDark() ? WO_KI_NB_JI_Mode.CLARO : WO_KI_NB_JI_Mode.OSCURO); }

    // Compatibilidad con el código anterior (la raíz ya no hace falta).
    public void WO_KI_NB_JI_setMode(WO_KI_NB_JI_Mode m, Parent ignored) { WO_KI_NB_JI_setMode(m); }
    public void WO_KI_NB_JI_setAccent(WO_KI_NB_JI_Accent a, Parent ignored) { WO_KI_NB_JI_setAccent(a); }
    public void WO_KI_NB_JI_apply(Parent ignored) { WO_KI_NB_JI_refresh(); }

    /** Engancha el tema a todas las ventanas: las abiertas y las que se creen después. */
    public void WO_KI_NB_JI_install() {
        if (installed) return;
        installed = true;
        sheet = WO_KI_NB_JI_buildSheet();
        WO_KI_NB_JI_startClock();
        Window.getWindows().addListener((ListChangeListener<Window>) ch -> {
            while (ch.next()) for (Window w : ch.getAddedSubList()) WO_KI_NB_JI_hook(w);
        });
        for (Window w : Window.getWindows()) WO_KI_NB_JI_hook(w);
    }

    /** Aplica la hoja base y la del tema a una escena concreta (útil antes de mostrarla). */
    public void WO_KI_NB_JI_applyTo(Scene scene) {
        if (scene == null) return;
        if (sheet == null) sheet = WO_KI_NB_JI_buildSheet();
        var sheets = scene.getStylesheets();
        sheets.removeIf(s -> s.startsWith(THEME_SHEET_PREFIX));
        // UNA sola hoja: styles.css y la del tema van juntas. Si se cargaran por separado, JavaFX
        // pierde el valor de las variables propias (-rN, -dv-…) al aplicar la segunda y las esquinas
        // redondeadas no se aplican (y salían avisos de conversión en el log).
        sheets.add(sheet);
        WO_KI_NB_JI_applyEffects(scene);
    }

    /** Filtro "bajo contraste y luz cálida" para cuidar la vista (solo si el usuario lo activa). */
    private void WO_KI_NB_JI_applyEffects(Scene scene) {
        Parent root = scene.getRoot();
        if (root == null) return;
        if (store.WO_KI_NB_JI_bool("appearance.warmLowContrast")) {
            ColorAdjust adj = new ColorAdjust();
            adj.setContrast(-0.08);
            adj.setSaturation(-0.05);
            adj.setInput(new SepiaTone(0.14));
            root.setEffect(adj);
        } else if (root.getEffect() instanceof ColorAdjust) {
            root.setEffect(null);
        }
    }

    /** Regenera la paleta y la sustituye en todas las ventanas abiertas. */
    public void WO_KI_NB_JI_refresh() {
        Runnable r = () -> {
            WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_reload();
            resolved = WO_KI_NB_JI_Palette.WO_KI_NB_JI_resolve(store, LocalTime.now());
            accent = WO_KI_NB_JI_Accent.WO_KI_NB_JI_from(store.WO_KI_NB_JI_str("appearance.accentId"));
            WO_KI_NB_JI_IconFactory.WO_KI_NB_JI_setDarkMode(WO_KI_NB_JI_isDark());
            sheet = WO_KI_NB_JI_buildSheet();
            for (Window w : Window.getWindows()) if (!(w instanceof PopupWindow) && w.getScene() != null) WO_KI_NB_JI_applyTo(w.getScene());
            revision.set(revision.get() + 1);
        };
        if (Platform.isFxApplicationThread()) r.run(); else Platform.runLater(r);
    }

    /** Cada minuto: activa el perfil que toque por horario y el tema claro/oscuro por hora. */
    private void WO_KI_NB_JI_startClock() {
        if (clock != null) return;
        clock = new Timeline(new KeyFrame(Duration.seconds(60), e -> {
            LocalTime now = LocalTime.now();
            int dia = java.time.DayOfWeek.from(java.time.LocalDate.now()).getValue();   // 1 = lunes
            if (store.WO_KI_NB_JI_applySchedule(now.getHour() * 60 + now.getMinute(), dia)) return;   // el cambio de perfil ya refresca
            boolean dark = WO_KI_NB_JI_Palette.WO_KI_NB_JI_resolve(store, now).dark();
            if (dark != resolved.dark()) WO_KI_NB_JI_refresh();
        }));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void WO_KI_NB_JI_hook(Window w) {
        if (w instanceof PopupWindow) return;          // los menús y popups heredan de su ventana dueña
        if (w.getScene() != null) WO_KI_NB_JI_applyTo(w.getScene());
        w.sceneProperty().addListener((o, a, s) -> WO_KI_NB_JI_applyTo(s));
    }

    /** Valor hexadecimal actual de un color del tema (para lo que no se pinta con CSS, p. ej. AWT). */
    public String WO_KI_NB_JI_color(String token) {
        return WO_KI_NB_JI_palette().getOrDefault(token, "#888888");
    }

    // ---------------------------------------------------------------- paleta

    private Map<String, String> WO_KI_NB_JI_palette() {
        boolean d = WO_KI_NB_JI_isDark();
        Map<String, String> p = new LinkedHashMap<>();
        if (!d) {
            p.put("-bg", "#F5F4F0");          p.put("-surface", "#FFFFFF");
            p.put("-surface2", "#F1F0EB");    p.put("-surface3", "#E9E7E1");
            p.put("-hover", "#F1F0EB");       p.put("-bord", "#E7E5DF");
            p.put("-bord-strong", "#D6D3CB"); p.put("-text", "#1C1F26");
            p.put("-text2", "#4A4F5C");       p.put("-muted", "#646978");
            p.put("-icon", "#5B606D");        p.put("-sidebar", "#F5F4F0");
            p.put("-input", "#FFFFFF");       p.put("-inverse", "#1C1F26");
            p.put("-on-inverse", "#FFFFFF");  p.put("-on-inverse-muted", "#C3C7D0");
            p.put("-shadow", "rgba(28,31,38,0.08)");
            WO_KI_NB_JI_tint(p, "blue", "#2F5BB7", "#E6EEFB");
            WO_KI_NB_JI_tint(p, "amber", "#8A5A00", "#FBF1D9");
            WO_KI_NB_JI_tint(p, "violet", "#6340B8", "#EEE8FB");
            WO_KI_NB_JI_tint(p, "green", "#2C7A4B", "#E3F3E8");
            WO_KI_NB_JI_tint(p, "teal", "#1F6F6A", "#DFF1F0");
            WO_KI_NB_JI_tint(p, "rose", "#B23A55", "#FBE6EA");
            WO_KI_NB_JI_tint(p, "orange", "#B4461A", "#FBE9DF");
            WO_KI_NB_JI_tint(p, "slate", "#4A4F5C", "#EFEEE9");
            p.put("-danger", "#B3261E");      p.put("-danger-soft", "#FCE8E6");
            WO_KI_NB_JI_prio(p, "#D64545", "#FCE8E6", "#E07B1F", "#FDEBD8", "#3C9D62", "#E3F3E8");
        } else {
            p.put("-bg", "#121419");          p.put("-surface", "#1B1E25");
            p.put("-surface2", "#22262E");    p.put("-surface3", "#2B3039");
            p.put("-hover", "#252930");       p.put("-bord", "#2C3139");
            p.put("-bord-strong", "#3B414C"); p.put("-text", "#ECEDF0");
            p.put("-text2", "#C3C7D0");       p.put("-muted", "#9AA0AC");
            p.put("-icon", "#A9AEB9");        p.put("-sidebar", "#16181D");
            p.put("-input", "#22262E");       p.put("-inverse", "#ECEDF0");
            p.put("-on-inverse", "#14161B");  p.put("-on-inverse-muted", "#4A4F5C");
            p.put("-shadow", "rgba(0,0,0,0.35)");
            WO_KI_NB_JI_tint(p, "blue", "#8DB0F5", "#1E2A42");
            WO_KI_NB_JI_tint(p, "amber", "#E9B949", "#3A2F14");
            WO_KI_NB_JI_tint(p, "violet", "#B9A3F2", "#2D2545");
            WO_KI_NB_JI_tint(p, "green", "#6FCF97", "#1C3527");
            WO_KI_NB_JI_tint(p, "teal", "#6BCFC7", "#173634");
            WO_KI_NB_JI_tint(p, "rose", "#F28BA3", "#3E1F28");
            WO_KI_NB_JI_tint(p, "orange", "#F4A27A", "#3F2519");
            WO_KI_NB_JI_tint(p, "slate", "#C3C7D0", "#2A2F38");
            p.put("-danger", "#F2867E");      p.put("-danger-soft", "#3D1E1D");
            WO_KI_NB_JI_prio(p, "#F2867E", "#3D1E1D", "#F0A04B", "#3F2A14", "#6FCF97", "#1C3527");
        }
        p.put("-success", p.get("-c-green"));  p.put("-success-soft", p.get("-c-green-soft"));
        p.put("-warning", p.get("-c-amber"));  p.put("-warning-soft", p.get("-c-amber-soft"));
        p.put("-info", p.get("-c-blue"));      p.put("-info-soft", p.get("-c-blue-soft"));
        if (!resolved.legacy()) WO_KI_NB_JI_Palette.WO_KI_NB_JI_apply(p, resolved);
        if (resolved.accent() != null) {
            WO_KI_NB_JI_Palette.WO_KI_NB_JI_applyAccent(p, resolved.accent(), p.get("-bg"), d);   // el texto sobre el acento se calcula solo
        } else {
            p.put("-accent", accent.WO_KI_NB_JI_color(d));
            p.put("-accent-soft", accent.WO_KI_NB_JI_soft(d));
            p.put("-on-accent", d ? "#16110E" : "#FFFFFF");
        }
        p.put("-on-danger", d ? "#1C0E0E" : "#FFFFFF");
        // Colores de los segmentos de la Agenda Unificada (TipoEntrada usa estos nombres).
        p.put("-t-evento", p.get("-c-teal"));    p.put("-t-evento-soft", p.get("-c-teal-soft"));
        p.put("-t-trabajo", p.get("-c-blue"));   p.put("-t-trabajo-soft", p.get("-c-blue-soft"));
        p.put("-t-tarea", p.get("-c-violet"));   p.put("-t-tarea-soft", p.get("-c-violet-soft"));
        p.put("-t-habito", p.get("-c-green"));   p.put("-t-habito-soft", p.get("-c-green-soft"));
        p.put("-t-cumple", p.get("-c-rose"));    p.put("-t-cumple-soft", p.get("-c-rose-soft"));
        return p;
    }

    /** Colores de prioridad: alta = rojo, media = naranja, baja = verde. */
    private static void WO_KI_NB_JI_prio(Map<String, String> p, String a, String aS, String m, String mS, String b, String bS) {
        p.put("-p-alta", a);  p.put("-p-alta-soft", aS);
        p.put("-p-media", m); p.put("-p-media-soft", mS);
        p.put("-p-baja", b);  p.put("-p-baja-soft", bS);
    }

    private static void WO_KI_NB_JI_tint(Map<String, String> p, String name, String fg, String soft) {
        p.put("-c-" + name, fg);
        p.put("-c-" + name + "-soft", soft);
    }

    private String WO_KI_NB_JI_buildSheet() {
        StringBuilder variables = new StringBuilder(".root {\n");
        WO_KI_NB_JI_palette().forEach((k, v) -> variables.append("    ").append(k).append(": ").append(v).append(";\n"));
        variables.append("    -fx-control-inner-background: -input;\n")
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
            .append("    -fx-font-family: \"").append(WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_family()).append("\";\n")
            .append("    -fx-font-size: ").append(String.format(Locale.ROOT, "%.1f", WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_baseSize())).append("px;\n")
            .append("}\n");

        // La hoja base y las variables del tema van en UNA sola hoja: se sustituye el bloque ".root"
        // de styles.css por el del tema activo (si no, el tema no se aplicaría).
        String base = WO_KI_NB_JI_leerHojaBase();
        String css;
        if (base == null) {
            css = variables.toString();                       // sin styles.css en el classpath: solo el tema
        } else {
            String conNumeros = WO_KI_NB_JI_sustituirTokens(base);
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?s)\\.root\\s*\\{.*?\\}").matcher(conNumeros);
            css = m.find()
                ? conNumeros.substring(0, m.start()) + variables + conNumeros.substring(m.end())
                : variables + conNumeros;
        }
        return THEME_SHEET_PREFIX + Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Sustituye los tokens de forma ({@code -r12}, {@code -dv-btn}…) por números literales.
     *
     * <p>Es necesario porque JavaFX solo convierte las variables propias a <i>colores</i>: si se usan
     * como tamaño ({@code -fx-border-radius: -r12}) no se aplican y salen avisos de conversión. Con los
     * números ya puestos, las esquinas redondeadas y los tamaños de los botones sí funcionan, y el
     * ajuste «radio de esquinas» y «densidad» los siguen controlando.</p>
     */
    private String WO_KI_NB_JI_sustituirTokens(String css) {
        double k = WO_KI_NB_JI_radius() / 12.0;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?<![\\w-])-r(\\d{1,2})(?![\\w-])").matcher(css);
        StringBuffer out = new StringBuffer();
        while (m.find()) {
            int n = Integer.parseInt(m.group(1));
            m.appendReplacement(out, java.util.regex.Matcher.quoteReplacement(String.valueOf(Math.round(n * k))));
        }
        m.appendTail(out);
        String s = out.toString();
        int[] dens = switch (store.WO_KI_NB_JI_choice("appearance.density", "compacta", "comoda", "amplia")) {
            case "compacta" -> new int[]{6, 34, 7, 38};
            case "amplia" -> new int[]{12, 46, 13, 50};
            default -> new int[]{9, 40, 10, 44};
        };
        return s.replace("-dv-btn", String.valueOf(dens[0]))
            .replace("-h-btn", String.valueOf(dens[1]))
            .replace("-dv-menu", String.valueOf(dens[2]))
            .replace("-h-menu", String.valueOf(dens[3]));
    }

    /** Contenido de /styles.css (la hoja base). null si no está en el classpath. */
    private static String WO_KI_NB_JI_leerHojaBase() {
        try (var in = WO_KI_NB_JI_ThemeService.class.getResourceAsStream("/styles.css")) {
            if (in == null) return null;
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return null;
        }
    }
}

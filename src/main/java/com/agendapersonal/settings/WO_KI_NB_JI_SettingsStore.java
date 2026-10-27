package com.agendapersonal.settings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Almacén central de ajustes: JSON versionado con perfiles. No depende de JavaFX ni de la base de
 * datos (usa un {@link Storage}); cualquier valor ausente o inválido cae al valor por defecto.
 * Los datos del usuario (eventos, tareas, notas...) viven en otras tablas y nunca se tocan aquí.
 */
public final class WO_KI_NB_JI_SettingsStore {
    public static final int VERSION = 1;
    public static final String KEY = "settings.json";
    /** Marcador de los archivos exportados. Se conserva el nombre antiguo para que los archivos
     *  exportados antes del cambio de marca sigan importándose. */
    public static final String FILE_TYPE = "cronosys-settings";

    public interface WO_KI_NB_JI_Storage { String WO_KI_NB_JI_get(String key, String def); void WO_KI_NB_JI_set(String key, String value); }

    private static WO_KI_NB_JI_SettingsStore instance;

    /** Instancia global (se conecta a la tabla config la primera vez). */
    public static synchronized WO_KI_NB_JI_SettingsStore WO_KI_NB_JI_get() {
        if (instance == null) instance = new WO_KI_NB_JI_SettingsStore(new WO_KI_NB_JI_ConfigStorage());
        return instance;
    }

    /** Para pruebas. */
    public static synchronized void WO_KI_NB_JI_install(WO_KI_NB_JI_SettingsStore s) { instance = s; }

    private final WO_KI_NB_JI_Storage storage;
    private Map<String, Object> root;
    private final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    public WO_KI_NB_JI_SettingsStore(WO_KI_NB_JI_Storage storage) {
        this.storage = storage;
        this.root = WO_KI_NB_JI_load();
    }

    // ------------------------------------------------------------------ carga y migración

    private Map<String, Object> WO_KI_NB_JI_load() {
        String raw = null;
        try { raw = storage.WO_KI_NB_JI_get(KEY, null); } catch (RuntimeException ignored) { }
        if (raw != null && !raw.isBlank()) {
            try {
                Map<String, Object> r = WO_KI_NB_JI_sanitize(WO_KI_NB_JI_Json.WO_KI_NB_JI_parseObject(raw));
                if (r != null) return r;
            } catch (RuntimeException ignored) { /* ajustes corruptos: se rehacen desde los antiguos */ }
        }
        Map<String, Object> r = WO_KI_NB_JI_migrateLegacy();
        try { storage.WO_KI_NB_JI_set(KEY, WO_KI_NB_JI_Json.WO_KI_NB_JI_write(r)); } catch (RuntimeException ignored) { }
        return r;
    }

    /** Crea ajustes nuevos tomando lo que ya existía en las claves antiguas (ui.theme, ui.accent...). */
    private Map<String, Object> WO_KI_NB_JI_migrateLegacy() {
        Map<String, Object> s = WO_KI_NB_JI_Defaults.WO_KI_NB_JI_settings();
        Map<String, Object> ap = WO_KI_NB_JI_section(s, "appearance");
        String theme = WO_KI_NB_JI_safe("ui.theme", "Claro");
        boolean dark = "Oscuro".equalsIgnoreCase(theme);
        ap.put("themeId", dark ? "oscuro" : "claro");
        ap.put("mode", dark ? "oscuro" : "claro");
        ap.put("accentId", WO_KI_NB_JI_safe("ui.accent", "NARANJA").toUpperCase(Locale.ROOT));
        String font = WO_KI_NB_JI_safe("ui.font", "");
        if (!font.isBlank()) ap.put("font", font);
        ap.put("fontScale", switch (WO_KI_NB_JI_safe("ui.fontSize", "NORMAL")) { case "PEQUENO" -> 89; case "GRANDE" -> 111; case "MUY_GRANDE" -> 121; default -> 100; });
        WO_KI_NB_JI_section(s, "sidebar").put("collapsed", "1".equals(WO_KI_NB_JI_safe("ui.sidebar.collapsed", "0")));
        Map<String, Object> home = WO_KI_NB_JI_section(s, "home");
        home.put("hour24", !"0".equals(WO_KI_NB_JI_safe("reloj.h24", "1")));
        home.put("showSeconds", "1".equals(WO_KI_NB_JI_safe("reloj.segundos", "0")));
        home.put("userName", WO_KI_NB_JI_safe("usuario.nombre", ""));
        Map<String, Object> noti = WO_KI_NB_JI_section(s, "notifications");
        noti.put("confirmations", "1".equals(WO_KI_NB_JI_safe("ui.confirmaciones", "0")));

        Map<String, Object> p = new LinkedHashMap<>();
        p.put("id", "default");
        p.put("name", "Principal");
        p.put("schedule", null);
        p.put("settings", s);
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("version", VERSION);
        r.put("activeProfileId", "default");
        r.put("profiles", new ArrayList<>(List.of(p)));
        return r;
    }

    private String WO_KI_NB_JI_safe(String k, String def) {
        try { String v = storage.WO_KI_NB_JI_get(k, def); return v == null ? def : v; } catch (RuntimeException e) { return def; }
    }

    /** Valida la estructura (no los valores: esos se corrigen al leerlos). null si no es utilizable. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> WO_KI_NB_JI_sanitize(Map<String, Object> r) {
        Object v = r.get("version");
        if (!(v instanceof Number n) || n.intValue() < 1 || n.intValue() > VERSION) return null;
        if (!(r.get("profiles") instanceof List<?> raw)) return null;
        List<Object> profiles = new ArrayList<>();
        for (Object o : raw) {
            if (!(o instanceof Map<?, ?> pm)) continue;
            Map<String, Object> p = (Map<String, Object>) pm;
            if (!(p.get("id") instanceof String id) || id.isBlank()) continue;
            if (!(p.get("name") instanceof String nm) || nm.isBlank()) p.put("name", "Perfil");
            if (!(p.get("settings") instanceof Map)) p.put("settings", new LinkedHashMap<String, Object>());
            if (!WO_KI_NB_JI_validSchedule(p.get("schedule"))) p.put("schedule", null);
            profiles.add(p);
        }
        if (profiles.isEmpty()) return null;
        r.put("profiles", profiles);
        r.put("version", VERSION);
        String active = r.get("activeProfileId") instanceof String a ? a : "";
        boolean ok = false;
        for (Object o : profiles) if (((Map<?, ?>) o).get("id").equals(active)) ok = true;
        if (!ok) r.put("activeProfileId", ((Map<?, ?>) profiles.get(0)).get("id"));
        return r;
    }

    private static boolean WO_KI_NB_JI_validTime(Object o) { return o instanceof String s && s.matches("([01]?\\d|2[0-3]):[0-5]\\d"); }

    /** Un horario es válido si tiene al menos una franja con horas correctas. */
    private static boolean WO_KI_NB_JI_validSchedule(Object o) {
        if (!(o instanceof Map<?, ?> sc)) return false;
        if (sc.get("windows") instanceof List<?> ws) {
            for (Object w : ws)
                if (w instanceof Map<?, ?> wm && WO_KI_NB_JI_validTime(wm.get("from")) && WO_KI_NB_JI_validTime(wm.get("to"))) return true;
            return false;
        }
        return WO_KI_NB_JI_validTime(sc.get("from")) && WO_KI_NB_JI_validTime(sc.get("to"));
    }

    // ------------------------------------------------------------------ lectura tipada

    @SuppressWarnings("unchecked")
    private static Map<String, Object> WO_KI_NB_JI_section(Map<String, Object> settings, String name) {
        return (Map<String, Object>) settings.get(name);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> WO_KI_NB_JI_activeSettings() {
        Map<String, Object> p = WO_KI_NB_JI_profile(WO_KI_NB_JI_activeProfileId());
        return (Map<String, Object>) p.get("settings");
    }

    /** Valor crudo de "seccion.clave" (admite anidados "health.goals.water"); null si no existe en el perfil. */
    public Object WO_KI_NB_JI_raw(String path) { return WO_KI_NB_JI_dig(WO_KI_NB_JI_activeSettings(), path); }

    private static Object WO_KI_NB_JI_dig(Map<String, Object> m, String path) {
        Object cur = m;
        for (String part : path.split("\\.")) {
            if (!(cur instanceof Map<?, ?> mm)) return null;
            cur = mm.get(part);
        }
        return cur;
    }

    private static final Map<String, Object> DEFAULTS = WO_KI_NB_JI_Defaults.WO_KI_NB_JI_settings();

    public Object WO_KI_NB_JI_value(String path) {
        Object v = WO_KI_NB_JI_raw(path);
        return v != null ? v : WO_KI_NB_JI_dig(DEFAULTS, path);
    }

    public String WO_KI_NB_JI_str(String path) {
        Object v = WO_KI_NB_JI_raw(path), d = WO_KI_NB_JI_dig(DEFAULTS, path);
        if (v instanceof String s) return s;
        return d instanceof String ds ? ds : "";
    }

    public boolean WO_KI_NB_JI_bool(String path) {
        Object v = WO_KI_NB_JI_raw(path), d = WO_KI_NB_JI_dig(DEFAULTS, path);
        if (v instanceof Boolean b) return b;
        return d instanceof Boolean db && db;
    }

    /** Entero acotado a [min, max]; si falta o es inválido, el valor por defecto (también acotado). */
    public int WO_KI_NB_JI_integer(String path, int min, int max) {
        Object v = WO_KI_NB_JI_raw(path), d = WO_KI_NB_JI_dig(DEFAULTS, path);
        double x;
        if (v instanceof Number n && !Double.isNaN(n.doubleValue())) x = n.doubleValue();
        else x = d instanceof Number dn ? dn.doubleValue() : min;
        return (int) Math.max(min, Math.min(max, Math.round(x)));
    }

    /** Texto restringido a opciones válidas. */
    public String WO_KI_NB_JI_choice(String path, String... allowed) {
        String v = WO_KI_NB_JI_str(path);
        for (String a : allowed) if (a.equals(v)) return v;
        Object d = WO_KI_NB_JI_dig(DEFAULTS, path);
        return d instanceof String ds ? ds : allowed[0];
    }

    public List<String> WO_KI_NB_JI_strings(String path) {
        Object v = WO_KI_NB_JI_raw(path);
        if (!(v instanceof List<?> l)) v = WO_KI_NB_JI_dig(DEFAULTS, path);
        List<String> out = new ArrayList<>();
        if (v instanceof List<?> l2) for (Object o : l2) if (o instanceof String s) out.add(s);
        return out;
    }

    public Map<String, Object> WO_KI_NB_JI_map(String path) {
        Object v = WO_KI_NB_JI_raw(path);
        Map<String, Object> out = new LinkedHashMap<>();
        if (v instanceof Map<?, ?> m) for (Map.Entry<?, ?> e : m.entrySet()) out.put(String.valueOf(e.getKey()), e.getValue());
        return out;
    }

    // ------------------------------------------------------------------ escritura

    /** Guarda un valor en "seccion.clave" del perfil activo, persiste y avisa a los oyentes. */
    @SuppressWarnings("unchecked")
    public void WO_KI_NB_JI_set(String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> cur = WO_KI_NB_JI_activeSettings();
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = cur.get(parts[i]);
            if (!(next instanceof Map)) { next = new LinkedHashMap<String, Object>(); cur.put(parts[i], next); }
            cur = (Map<String, Object>) next;
        }
        if (value == null) cur.remove(parts[parts.length - 1]); else cur.put(parts[parts.length - 1], value);
        WO_KI_NB_JI_persist();
        WO_KI_NB_JI_fire(parts[0]);
    }

    /** Restablece una sección completa del perfil activo a sus valores por defecto. */
    public void WO_KI_NB_JI_resetSection(String section) {
        WO_KI_NB_JI_activeSettings().remove(section);
        WO_KI_NB_JI_persist();
        WO_KI_NB_JI_fire(section);
    }

    public void WO_KI_NB_JI_addListener(Consumer<String> l) { listeners.add(l); }
    public void WO_KI_NB_JI_removeListener(Consumer<String> l) { listeners.remove(l); }

    private void WO_KI_NB_JI_fire(String section) { for (Consumer<String> l : listeners) { try { l.accept(section); } catch (RuntimeException ignored) { } } }

    private void WO_KI_NB_JI_persist() {
        try { storage.WO_KI_NB_JI_set(KEY, WO_KI_NB_JI_Json.WO_KI_NB_JI_write(root)); } catch (RuntimeException ignored) { }
    }

    // ------------------------------------------------------------------ perfiles

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> WO_KI_NB_JI_profiles() { return (List<Map<String, Object>>) (List<?>) root.get("profiles"); }

    public String WO_KI_NB_JI_activeProfileId() { return (String) root.get("activeProfileId"); }

    public Map<String, Object> WO_KI_NB_JI_profile(String id) {
        for (Map<String, Object> p : WO_KI_NB_JI_profiles()) if (p.get("id").equals(id)) return p;
        return WO_KI_NB_JI_profiles().get(0);
    }

    public String WO_KI_NB_JI_activeProfileName() { return (String) WO_KI_NB_JI_profile(WO_KI_NB_JI_activeProfileId()).get("name"); }

    public void WO_KI_NB_JI_activateProfile(String id) {
        boolean found = false;
        for (Map<String, Object> p : WO_KI_NB_JI_profiles()) if (p.get("id").equals(id)) found = true;
        if (!found || id.equals(WO_KI_NB_JI_activeProfileId())) return;
        root.put("activeProfileId", id);
        WO_KI_NB_JI_persist();
        WO_KI_NB_JI_fire("*");
    }

    public String WO_KI_NB_JI_createProfile(String name) {
        Map<String, Object> p = WO_KI_NB_JI_newProfile(name, WO_KI_NB_JI_Defaults.WO_KI_NB_JI_settings());
        WO_KI_NB_JI_profiles().add(p);
        WO_KI_NB_JI_persist();
        return (String) p.get("id");
    }

    @SuppressWarnings("unchecked")
    public String WO_KI_NB_JI_duplicateProfile(String id) {
        Map<String, Object> src = WO_KI_NB_JI_profile(id);
        Map<String, Object> copy = (Map<String, Object>) WO_KI_NB_JI_Json.WO_KI_NB_JI_parseObject(WO_KI_NB_JI_Json.WO_KI_NB_JI_write(src.get("settings")));
        Map<String, Object> p = WO_KI_NB_JI_newProfile(src.get("name") + " (copia)", copy);
        WO_KI_NB_JI_profiles().add(p);
        WO_KI_NB_JI_persist();
        return (String) p.get("id");
    }

    public void WO_KI_NB_JI_renameProfile(String id, String name) {
        if (name == null || name.isBlank()) return;
        WO_KI_NB_JI_profile(id).put("name", name.trim());
        WO_KI_NB_JI_persist();
    }

    /** Borra un perfil; siempre queda al menos uno. Devuelve false si no se pudo. */
    public boolean WO_KI_NB_JI_deleteProfile(String id) {
        if (WO_KI_NB_JI_profiles().size() <= 1) return false;
        boolean removed = WO_KI_NB_JI_profiles().removeIf(p -> p.get("id").equals(id));
        if (!removed) return false;
        boolean wasActive = id.equals(WO_KI_NB_JI_activeProfileId());
        if (wasActive) root.put("activeProfileId", WO_KI_NB_JI_profiles().get(0).get("id"));
        WO_KI_NB_JI_persist();
        if (wasActive) WO_KI_NB_JI_fire("*");
        return true;
    }

    public void WO_KI_NB_JI_setSchedule(String id, String from, String to) {
        Map<String, Object> p = WO_KI_NB_JI_profile(id);
        if (WO_KI_NB_JI_validTime(from) && WO_KI_NB_JI_validTime(to)) {
            Map<String, Object> sc = new LinkedHashMap<>();
            sc.put("from", from); sc.put("to", to);
            p.put("schedule", sc);
        } else p.put("schedule", null);
        WO_KI_NB_JI_persist();
    }

    /** Una franja horaria ("08:00" a "14:00"). Si la hora de fin es menor, la franja cruza la medianoche. */
    public record WO_KI_NB_JI_Franja(String desde, String hasta) {
        public int WO_KI_NB_JI_desdeMinutos() { return WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_minutes(desde); }
        public int WO_KI_NB_JI_hastaMinutos() { return WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_minutes(hasta); }
        /** ¿La franja cubre ese minuto del día? */
        public boolean WO_KI_NB_JI_contiene(int minuto) {
            int a = WO_KI_NB_JI_desdeMinutos(), b = WO_KI_NB_JI_hastaMinutos();
            if (a == b) return false;
            return a <= b ? (minuto >= a && minuto < b) : (minuto >= a || minuto < b);
        }
    }

    /** Todas las franjas horarias de un perfil (admite el formato antiguo de una sola franja). */
    public List<WO_KI_NB_JI_Franja> WO_KI_NB_JI_franjas(String id) {
        List<WO_KI_NB_JI_Franja> out = new ArrayList<>();
        Object sc = WO_KI_NB_JI_profile(id).get("schedule");
        if (!(sc instanceof Map<?, ?> m)) return out;
        if (m.get("windows") instanceof List<?> ws) {
            for (Object w : ws)
                if (w instanceof Map<?, ?> wm && WO_KI_NB_JI_validTime(wm.get("from")) && WO_KI_NB_JI_validTime(wm.get("to")))
                    out.add(new WO_KI_NB_JI_Franja(String.valueOf(wm.get("from")), String.valueOf(wm.get("to"))));
        } else if (WO_KI_NB_JI_validTime(m.get("from")) && WO_KI_NB_JI_validTime(m.get("to"))) {
            out.add(new WO_KI_NB_JI_Franja(String.valueOf(m.get("from")), String.valueOf(m.get("to"))));
        }
        return out;
    }

    /** Días de la semana del horario (1 = lunes … 7 = domingo). Vacío = todos los días. */
    public List<Integer> WO_KI_NB_JI_dias(String id) {
        List<Integer> out = new ArrayList<>();
        Object sc = WO_KI_NB_JI_profile(id).get("schedule");
        if (sc instanceof Map<?, ?> m && m.get("days") instanceof List<?> ds) {
            for (Object d : ds)
                if (d instanceof Number n && n.intValue() >= 1 && n.intValue() <= 7 && !out.contains(n.intValue()))
                    out.add(n.intValue());
        }
        return out;
    }

    /** Guarda varias franjas y los días de la semana en los que se activan. */
    public void WO_KI_NB_JI_setScheduleWindows(String id, List<WO_KI_NB_JI_Franja> franjas, List<Integer> dias) {
        Map<String, Object> p = WO_KI_NB_JI_profile(id);
        List<Object> ventanas = new ArrayList<>();
        for (WO_KI_NB_JI_Franja f : franjas) {
            if (!WO_KI_NB_JI_validTime(f.desde()) || !WO_KI_NB_JI_validTime(f.hasta())) continue;
            Map<String, Object> w = new LinkedHashMap<>();
            w.put("from", f.desde()); w.put("to", f.hasta());
            ventanas.add(w);
        }
        if (ventanas.isEmpty()) p.put("schedule", null);
        else {
            Map<String, Object> sc = new LinkedHashMap<>();
            sc.put("windows", ventanas);
            sc.put("days", dias == null ? new ArrayList<>() : new ArrayList<>(dias));
            p.put("schedule", sc);
        }
        WO_KI_NB_JI_persist();
    }

    /** Activa el perfil cuyo horario incluya la hora dada (minutos desde 00:00), si hay alguno. */
    public boolean WO_KI_NB_JI_applySchedule(int minuteOfDay) { return WO_KI_NB_JI_applySchedule(minuteOfDay, -1); }

    /**
     * Activa el perfil cuyo horario incluya la hora y el día dados.
     * {@code diaSemana} de 1 (lunes) a 7 (domingo); si es negativo se ignoran los días.
     */
    public boolean WO_KI_NB_JI_applySchedule(int minuteOfDay, int diaSemana) {
        for (Map<String, Object> p : WO_KI_NB_JI_profiles()) {
            String id = String.valueOf(p.get("id"));
            List<WO_KI_NB_JI_Franja> franjas = WO_KI_NB_JI_franjas(id);
            if (franjas.isEmpty()) continue;
            List<Integer> dias = WO_KI_NB_JI_dias(id);
            if (diaSemana > 0 && !dias.isEmpty() && !dias.contains(diaSemana)) continue;
            for (WO_KI_NB_JI_Franja f : franjas) {
                if (!f.WO_KI_NB_JI_contiene(minuteOfDay)) continue;
                if (id.equals(WO_KI_NB_JI_activeProfileId())) return false;
                WO_KI_NB_JI_activateProfile(id);
                return true;
            }
        }
        return false;
    }

    public static int WO_KI_NB_JI_minutes(String hhmm) {
        if (!WO_KI_NB_JI_validTime(hhmm)) return 0;
        String[] t = hhmm.split(":");
        return Integer.parseInt(t[0]) * 60 + Integer.parseInt(t[1]);
    }

    private static Map<String, Object> WO_KI_NB_JI_newProfile(String name, Map<String, Object> settings) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("id", UUID.randomUUID().toString().substring(0, 8));
        p.put("name", name == null || name.isBlank() ? "Perfil" : name.trim());
        p.put("schedule", null);
        p.put("settings", settings);
        return p;
    }

    // ------------------------------------------------------------------ exportar / importar

    /** Exporta el perfil activo o todos a JSON. */
    public String WO_KI_NB_JI_export(boolean all) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("type", FILE_TYPE);
        out.put("version", VERSION);
        List<Object> ps = new ArrayList<>();
        if (all) ps.addAll(WO_KI_NB_JI_profiles()); else ps.add(WO_KI_NB_JI_profile(WO_KI_NB_JI_activeProfileId()));
        out.put("profiles", ps);
        out.put("activeProfileId", all ? WO_KI_NB_JI_activeProfileId() : WO_KI_NB_JI_profile(WO_KI_NB_JI_activeProfileId()).get("id"));
        return WO_KI_NB_JI_Json.WO_KI_NB_JI_pretty(out);
    }

    public record WO_KI_NB_JI_ImportPreview(boolean valid, String message, List<String> profileNames, Map<String, Object> parsed) {}

    /** Valida un archivo sin modificar nada. */
    public static WO_KI_NB_JI_ImportPreview WO_KI_NB_JI_previewImport(String text) {
        Map<String, Object> r;
        try { r = WO_KI_NB_JI_Json.WO_KI_NB_JI_parseObject(text); }
        catch (RuntimeException e) { return new WO_KI_NB_JI_ImportPreview(false, "El archivo no es un JSON válido.", List.of(), null); }
        if (!FILE_TYPE.equals(r.get("type"))) return new WO_KI_NB_JI_ImportPreview(false, "El archivo no es una exportación de Compás.", List.of(), null);
        if (!(r.get("version") instanceof Number n)) return new WO_KI_NB_JI_ImportPreview(false, "El archivo no tiene versión.", List.of(), null);
        if (n.intValue() > VERSION) return new WO_KI_NB_JI_ImportPreview(false, "El archivo es de una versión más nueva (" + n.intValue() + "). Actualiza Compás.", List.of(), null);
        if (n.intValue() < 1) return new WO_KI_NB_JI_ImportPreview(false, "Versión de archivo inválida.", List.of(), null);
        Map<String, Object> clean;
        try { clean = WO_KI_NB_JI_sanitize(r); } catch (RuntimeException e) { clean = null; }
        if (clean == null) return new WO_KI_NB_JI_ImportPreview(false, "El archivo no contiene perfiles válidos.", List.of(), null);
        List<String> names = new ArrayList<>();
        for (Object o : (List<?>) clean.get("profiles")) names.add((String) ((Map<?, ?>) o).get("name"));
        return new WO_KI_NB_JI_ImportPreview(true, "Se añadirán " + names.size() + " perfil(es): " + String.join(", ", names) + ".", names, clean);
    }

    /** Aplica una importación ya validada: añade los perfiles (con ids nuevos) y activa el primero. */
    @SuppressWarnings("unchecked")
    public void WO_KI_NB_JI_applyImport(WO_KI_NB_JI_ImportPreview pv) {
        if (pv == null || !pv.valid()) return;
        String first = null;
        for (Object o : (List<?>) pv.parsed().get("profiles")) {
            Map<String, Object> p = (Map<String, Object>) o;
            Map<String, Object> np = WO_KI_NB_JI_newProfile((String) p.get("name"), (Map<String, Object>) p.get("settings"));
            np.put("schedule", p.get("schedule"));
            WO_KI_NB_JI_profiles().add(np);
            if (first == null) first = (String) np.get("id");
        }
        WO_KI_NB_JI_persist();
        if (first != null) { root.put("activeProfileId", first); WO_KI_NB_JI_persist(); WO_KI_NB_JI_fire("*"); }
    }

    /** Restablece todos los ajustes del perfil activo. */
    public void WO_KI_NB_JI_resetActiveProfile() {
        WO_KI_NB_JI_profile(WO_KI_NB_JI_activeProfileId()).put("settings", WO_KI_NB_JI_Defaults.WO_KI_NB_JI_settings());
        WO_KI_NB_JI_persist();
        WO_KI_NB_JI_fire("*");
    }

    /** Reinicia todos los perfiles y preferencias tras borrar todos los datos de la aplicación. */
    public void WO_KI_NB_JI_resetAll() {
        root = WO_KI_NB_JI_load();
        WO_KI_NB_JI_fire("*");
    }
}

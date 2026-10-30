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
public final class wkniSettingsStore {
    public static final int wkniVERSION = 1;
    public static final String wkniKEY = "settings.json";
    /** Marcador de los archivos exportados. Se conserva el nombre antiguo para que los archivos
     *  exportados antes del cambio de marca sigan importándose. */
    public static final String wkniFILE_TYPE = "cronosys-settings";

    public interface wkniStorage { String wkniGet(String wkniKey, String wkniDef); void wkniSet(String wkniKey, String wkniValue); }

    private static wkniSettingsStore wkniInstance;

    /** Instancia global (se conecta a la tabla config la primera vez). */
    public static synchronized wkniSettingsStore wkniGet() {
        if (wkniInstance == null) wkniInstance = new wkniSettingsStore(new wkniConfigStorage());
        return wkniInstance;
    }

    /** Para pruebas. */
    public static synchronized void wkniInstall(wkniSettingsStore wkniS) { wkniInstance = wkniS; }

    private final wkniStorage wkni_storage;
    private Map<String, Object> wkniRoot;
    private final List<Consumer<String>> wkniListeners = new CopyOnWriteArrayList<>();

    public wkniSettingsStore(wkniStorage wkni_storage) {
        this.wkni_storage = wkni_storage;
        this.wkniRoot = wkniLoad();
    }

    // ------------------------------------------------------------------ carga y migración

    private Map<String, Object> wkniLoad() {
        String wkniRaw = null;
        try { wkniRaw = wkni_storage.wkniGet(wkniKEY, null); } catch (RuntimeException wkniIgnored) { }
        if (wkniRaw != null && !wkniRaw.isBlank()) {
            try {
                Map<String, Object> wkniR = wkniSanitize(wkniJson.wkniParseObject(wkniRaw));
                if (wkniR != null) return wkniR;
            } catch (RuntimeException wkniIgnored) { /* ajustes corruptos: se rehacen desde los antiguos */ }
        }
        Map<String, Object> wkniR = wkniMigrateLegacy();
        try { wkni_storage.wkniSet(wkniKEY, wkniJson.wkniWrite(wkniR)); } catch (RuntimeException wkniIgnored) { }
        return wkniR;
    }

    /** Crea ajustes nuevos tomando lo que ya existía en las claves antiguas (ui.theme, ui.accent...). */
    private Map<String, Object> wkniMigrateLegacy() {
        Map<String, Object> wkniS = wkniDefaults.wkniSettings();
        Map<String, Object> wkniAp = wkniSection(wkniS, "appearance");
        String wkniTheme = wkniSafe("ui.theme", "Claro");
        boolean wkniDark = "Oscuro".equalsIgnoreCase(wkniTheme);
        wkniAp.put("themeId", wkniDark ? "oscuro" : "claro");
        wkniAp.put("mode", wkniDark ? "oscuro" : "claro");
        wkniAp.put("accentId", wkniSafe("ui.accent", "NARANJA").toUpperCase(Locale.ROOT));
        String wkniFont = wkniSafe("ui.font", "");
        if (!wkniFont.isBlank()) wkniAp.put("font", wkniFont);
        wkniAp.put("fontScale", switch (wkniSafe("ui.fontSize", "NORMAL")) { case "PEQUENO" -> 89; case "GRANDE" -> 111; case "MUY_GRANDE" -> 121; default -> 100; });
        wkniSection(wkniS, "sidebar").put("collapsed", "1".equals(wkniSafe("ui.sidebar.collapsed", "0")));
        Map<String, Object> wkniHome = wkniSection(wkniS, "home");
        wkniHome.put("hour24", !"0".equals(wkniSafe("reloj.h24", "1")));
        wkniHome.put("showSeconds", "1".equals(wkniSafe("reloj.segundos", "0")));
        wkniHome.put("userName", wkniSafe("usuario.nombre", ""));
        Map<String, Object> wkniNoti = wkniSection(wkniS, "notifications");
        wkniNoti.put("confirmations", "1".equals(wkniSafe("ui.confirmaciones", "0")));

        Map<String, Object> wkni_p = new LinkedHashMap<>();
        wkni_p.put("id", "default");
        wkni_p.put("name", "Principal");
        wkni_p.put("schedule", null);
        wkni_p.put("settings", wkniS);
        Map<String, Object> wkniR = new LinkedHashMap<>();
        wkniR.put("version", wkniVERSION);
        wkniR.put("activeProfileId", "default");
        wkniR.put("profiles", new ArrayList<>(List.of(wkni_p)));
        return wkniR;
    }

    private String wkniSafe(String wkniK, String wkniDef) {
        try { String wkniV = wkni_storage.wkniGet(wkniK, wkniDef); return wkniV == null ? wkniDef : wkniV; } catch (RuntimeException wkniE) { return wkniDef; }
    }

    /** Valida la estructura (no los valores: esos se corrigen al leerlos). null si no es utilizable. */
    @SuppressWarnings("unchecked")
    private static Map<String, Object> wkniSanitize(Map<String, Object> wkniR) {
        Object wkniV = wkniR.get("version");
        if (!(wkniV instanceof Number wkniN) || wkniN.intValue() < 1 || wkniN.intValue() > wkniVERSION) return null;
        if (!(wkniR.get("profiles") instanceof List<?> wkniRaw)) return null;
        List<Object> wkniProfiles = new ArrayList<>();
        for (Object wkniO : wkniRaw) {
            if (!(wkniO instanceof Map<?, ?> wkniPm)) continue;
            Map<String, Object> wkni_p = (Map<String, Object>) wkniPm;
            if (!(wkni_p.get("id") instanceof String wkniId) || wkniId.isBlank()) continue;
            if (!(wkni_p.get("name") instanceof String wkniNm) || wkniNm.isBlank()) wkni_p.put("name", "Perfil");
            if (!(wkni_p.get("settings") instanceof Map)) wkni_p.put("settings", new LinkedHashMap<String, Object>());
            if (!wkniValidSchedule(wkni_p.get("schedule"))) wkni_p.put("schedule", null);
            wkniProfiles.add(wkni_p);
        }
        if (wkniProfiles.isEmpty()) return null;
        wkniR.put("profiles", wkniProfiles);
        wkniR.put("version", wkniVERSION);
        String wkniActive = wkniR.get("activeProfileId") instanceof String wkniA ? wkniA : "";
        boolean wkniOk = false;
        for (Object wkniO : wkniProfiles) if (((Map<?, ?>) wkniO).get("id").equals(wkniActive)) wkniOk = true;
        if (!wkniOk) wkniR.put("activeProfileId", ((Map<?, ?>) wkniProfiles.get(0)).get("id"));
        return wkniR;
    }

    private static boolean wkniValidTime(Object wkniO) { return wkniO instanceof String wkniS && wkniS.matches("([01]?\\d|2[0-3]):[0-5]\\d"); }

    /** Un horario es válido si tiene al menos una franja con horas correctas. */
    private static boolean wkniValidSchedule(Object wkniO) {
        if (!(wkniO instanceof Map<?, ?> wkniSc)) return false;
        if (wkniSc.get("windows") instanceof List<?> wkniWs) {
            for (Object wkniW : wkniWs)
                if (wkniW instanceof Map<?, ?> wkniWm && wkniValidTime(wkniWm.get("from")) && wkniValidTime(wkniWm.get("to"))) return true;
            return false;
        }
        return wkniValidTime(wkniSc.get("from")) && wkniValidTime(wkniSc.get("to"));
    }

    // ------------------------------------------------------------------ lectura tipada

    @SuppressWarnings("unchecked")
    private static Map<String, Object> wkniSection(Map<String, Object> wkniSettings, String wkniName) {
        return (Map<String, Object>) wkniSettings.get(wkniName);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> wkniActiveSettings() {
        Map<String, Object> wkni_p = wkniProfile(wkniActiveProfileId());
        return (Map<String, Object>) wkni_p.get("settings");
    }

    /** Valor crudo de "seccion.clave" (admite anidados "health.goals.water"); null si no existe en el perfil. */
    public Object wkniRaw(String wkniPath) { return wkniDig(wkniActiveSettings(), wkniPath); }

    private static Object wkniDig(Map<String, Object> wkniM, String wkniPath) {
        Object wkniCur = wkniM;
        for (String wkniPart : wkniPath.split("\\.")) {
            if (!(wkniCur instanceof Map<?, ?> wkniMm)) return null;
            wkniCur = wkniMm.get(wkniPart);
        }
        return wkniCur;
    }

    private static final Map<String, Object> wkniDEFAULTS = wkniDefaults.wkniSettings();

    public Object wkniValue(String wkniPath) {
        Object wkniV = wkniRaw(wkniPath);
        return wkniV != null ? wkniV : wkniDig(wkniDEFAULTS, wkniPath);
    }

    public String wkniStr(String wkniPath) {
        Object wkniV = wkniRaw(wkniPath), wkniD = wkniDig(wkniDEFAULTS, wkniPath);
        if (wkniV instanceof String wkniS) return wkniS;
        return wkniD instanceof String wkniDs ? wkniDs : "";
    }

    public boolean wkniBool(String wkniPath) {
        Object wkniV = wkniRaw(wkniPath), wkniD = wkniDig(wkniDEFAULTS, wkniPath);
        if (wkniV instanceof Boolean wkniB) return wkniB;
        return wkniD instanceof Boolean wkniDb && wkniDb;
    }

    /** Entero acotado a [min, max]; si falta o es inválido, el valor por defecto (también acotado). */
    public int wkniInteger(String wkniPath, int wkniMin, int wkniMax) {
        Object wkniV = wkniRaw(wkniPath), wkniD = wkniDig(wkniDEFAULTS, wkniPath);
        double wkniX;
        if (wkniV instanceof Number wkniN && !Double.isNaN(wkniN.doubleValue())) wkniX = wkniN.doubleValue();
        else wkniX = wkniD instanceof Number wkniDn ? wkniDn.doubleValue() : wkniMin;
        return (int) Math.max(wkniMin, Math.min(wkniMax, Math.round(wkniX)));
    }

    /** Texto restringido a opciones válidas. */
    public String wkniChoice(String wkniPath, String... wkniAllowed) {
        String wkniV = wkniStr(wkniPath);
        for (String wkniA : wkniAllowed) if (wkniA.equals(wkniV)) return wkniV;
        Object wkniD = wkniDig(wkniDEFAULTS, wkniPath);
        return wkniD instanceof String wkniDs ? wkniDs : wkniAllowed[0];
    }

    public List<String> wkniStrings(String wkniPath) {
        Object wkniV = wkniRaw(wkniPath);
        if (!(wkniV instanceof List<?> wkniL)) wkniV = wkniDig(wkniDEFAULTS, wkniPath);
        List<String> wkniOut = new ArrayList<>();
        if (wkniV instanceof List<?> wkniL2) for (Object wkniO : wkniL2) if (wkniO instanceof String wkniS) wkniOut.add(wkniS);
        return wkniOut;
    }

    public Map<String, Object> wkniMap(String wkniPath) {
        Object wkniV = wkniRaw(wkniPath);
        Map<String, Object> wkniOut = new LinkedHashMap<>();
        if (wkniV instanceof Map<?, ?> wkniM) for (Map.Entry<?, ?> wkniE : wkniM.entrySet()) wkniOut.put(String.valueOf(wkniE.getKey()), wkniE.getValue());
        return wkniOut;
    }

    // ------------------------------------------------------------------ escritura

    /** Guarda un valor en "seccion.clave" del perfil activo, persiste y avisa a los oyentes. */
    @SuppressWarnings("unchecked")
    public void wkniSet(String wkniPath, Object wkniValue) {
        String[] wkniParts = wkniPath.split("\\.");
        Map<String, Object> wkniCur = wkniActiveSettings();
        for (int wkniI = 0; wkniI < wkniParts.length - 1; wkniI++) {
            Object wkniNext = wkniCur.get(wkniParts[wkniI]);
            if (!(wkniNext instanceof Map)) { wkniNext = new LinkedHashMap<String, Object>(); wkniCur.put(wkniParts[wkniI], wkniNext); }
            wkniCur = (Map<String, Object>) wkniNext;
        }
        if (wkniValue == null) wkniCur.remove(wkniParts[wkniParts.length - 1]); else wkniCur.put(wkniParts[wkniParts.length - 1], wkniValue);
        wkniPersist();
        wkniFire(wkniParts[0]);
    }

    /** Restablece una sección completa del perfil activo a sus valores por defecto. */
    public void wkniResetSection(String wkniSection) {
        wkniActiveSettings().remove(wkniSection);
        wkniPersist();
        wkniFire(wkniSection);
    }

    public void wkniAddListener(Consumer<String> wkniL) { wkniListeners.add(wkniL); }
    public void wkniRemoveListener(Consumer<String> wkniL) { wkniListeners.remove(wkniL); }

    private void wkniFire(String wkniSection) { for (Consumer<String> wkniL : wkniListeners) { try { wkniL.accept(wkniSection); } catch (RuntimeException wkniIgnored) { } } }

    private void wkniPersist() {
        try { wkni_storage.wkniSet(wkniKEY, wkniJson.wkniWrite(wkniRoot)); } catch (RuntimeException wkniIgnored) { }
    }

    // ------------------------------------------------------------------ perfiles

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> wkniProfiles() { return (List<Map<String, Object>>) (List<?>) wkniRoot.get("profiles"); }

    public String wkniActiveProfileId() { return (String) wkniRoot.get("activeProfileId"); }

    public Map<String, Object> wkniProfile(String wkniId) {
        for (Map<String, Object> wkni_p : wkniProfiles()) if (wkni_p.get("id").equals(wkniId)) return wkni_p;
        return wkniProfiles().get(0);
    }

    public String wkniActiveProfileName() { return (String) wkniProfile(wkniActiveProfileId()).get("name"); }

    public void wkniActivateProfile(String wkniId) {
        boolean wkniFound = false;
        for (Map<String, Object> wkni_p : wkniProfiles()) if (wkni_p.get("id").equals(wkniId)) wkniFound = true;
        if (!wkniFound || wkniId.equals(wkniActiveProfileId())) return;
        wkniRoot.put("activeProfileId", wkniId);
        wkniPersist();
        wkniFire("*");
    }

    public String wkniCreateProfile(String wkniName) {
        Map<String, Object> wkni_p = wkniNewProfile(wkniName, wkniDefaults.wkniSettings());
        wkniProfiles().add(wkni_p);
        wkniPersist();
        return (String) wkni_p.get("id");
    }

    @SuppressWarnings("unchecked")
    public String wkniDuplicateProfile(String wkniId) {
        Map<String, Object> wkniSrc = wkniProfile(wkniId);
        Map<String, Object> wkniCopy = (Map<String, Object>) wkniJson.wkniParseObject(wkniJson.wkniWrite(wkniSrc.get("settings")));
        Map<String, Object> wkni_p = wkniNewProfile(wkniSrc.get("name") + " (copia)", wkniCopy);
        wkniProfiles().add(wkni_p);
        wkniPersist();
        return (String) wkni_p.get("id");
    }

    public void wkniRenameProfile(String wkniId, String wkniName) {
        if (wkniName == null || wkniName.isBlank()) return;
        wkniProfile(wkniId).put("name", wkniName.trim());
        wkniPersist();
    }

    /** Borra un perfil; siempre queda al menos uno. Devuelve false si no se pudo. */
    public boolean wkniDeleteProfile(String wkniId) {
        if (wkniProfiles().size() <= 1) return false;
        boolean wkniRemoved = wkniProfiles().removeIf(wkni_p -> wkni_p.get("id").equals(wkniId));
        if (!wkniRemoved) return false;
        boolean wkniWasActive = wkniId.equals(wkniActiveProfileId());
        if (wkniWasActive) wkniRoot.put("activeProfileId", wkniProfiles().get(0).get("id"));
        wkniPersist();
        if (wkniWasActive) wkniFire("*");
        return true;
    }

    public void wkniSetSchedule(String wkniId, String wkniFrom, String wkniTo) {
        Map<String, Object> wkni_p = wkniProfile(wkniId);
        if (wkniValidTime(wkniFrom) && wkniValidTime(wkniTo)) {
            Map<String, Object> wkniSc = new LinkedHashMap<>();
            wkniSc.put("from", wkniFrom); wkniSc.put("to", wkniTo);
            wkni_p.put("schedule", wkniSc);
        } else wkni_p.put("schedule", null);
        wkniPersist();
    }

    /** Una franja horaria ("08:00" a "14:00"). Si la hora de fin es menor, la franja cruza la medianoche. */
    public record wkniFranja(String wkniDesde, String wkniHasta) {
        public int wkniDesdeMinutos() { return wkniSettingsStore.wkniMinutes(wkniDesde); }
        public int wkniHastaMinutos() { return wkniSettingsStore.wkniMinutes(wkniHasta); }
        /** ¿La franja cubre ese minuto del día? */
        public boolean wkniContiene(int wkniMinuto) {
            int wkniA = wkniDesdeMinutos(), wkniB = wkniHastaMinutos();
            if (wkniA == wkniB) return false;
            return wkniA <= wkniB ? (wkniMinuto >= wkniA && wkniMinuto < wkniB) : (wkniMinuto >= wkniA || wkniMinuto < wkniB);
        }
    }

    /** Todas las franjas horarias de un perfil (admite el formato antiguo de una sola franja). */
    public List<wkniFranja> wkniFranjas(String wkniId) {
        List<wkniFranja> wkniOut = new ArrayList<>();
        Object wkniSc = wkniProfile(wkniId).get("schedule");
        if (!(wkniSc instanceof Map<?, ?> wkniM)) return wkniOut;
        if (wkniM.get("windows") instanceof List<?> wkniWs) {
            for (Object wkniW : wkniWs)
                if (wkniW instanceof Map<?, ?> wkniWm && wkniValidTime(wkniWm.get("from")) && wkniValidTime(wkniWm.get("to")))
                    wkniOut.add(new wkniFranja(String.valueOf(wkniWm.get("from")), String.valueOf(wkniWm.get("to"))));
        } else if (wkniValidTime(wkniM.get("from")) && wkniValidTime(wkniM.get("to"))) {
            wkniOut.add(new wkniFranja(String.valueOf(wkniM.get("from")), String.valueOf(wkniM.get("to"))));
        }
        return wkniOut;
    }

    /** Días de la semana del horario (1 = lunes … 7 = domingo). Vacío = todos los días. */
    public List<Integer> wkniDias(String wkniId) {
        List<Integer> wkniOut = new ArrayList<>();
        Object wkniSc = wkniProfile(wkniId).get("schedule");
        if (wkniSc instanceof Map<?, ?> wkniM && wkniM.get("days") instanceof List<?> wkniDs) {
            for (Object wkniD : wkniDs)
                if (wkniD instanceof Number wkniN && wkniN.intValue() >= 1 && wkniN.intValue() <= 7 && !wkniOut.contains(wkniN.intValue()))
                    wkniOut.add(wkniN.intValue());
        }
        return wkniOut;
    }

    /** Guarda varias franjas y los días de la semana en los que se activan. */
    public void wkniSetScheduleWindows(String wkniId, List<wkniFranja> wkniFranjas, List<Integer> wkniDias) {
        Map<String, Object> wkni_p = wkniProfile(wkniId);
        List<Object> wkniVentanas = new ArrayList<>();
        for (wkniFranja wkniF : wkniFranjas) {
            if (!wkniValidTime(wkniF.wkniDesde()) || !wkniValidTime(wkniF.wkniHasta())) continue;
            Map<String, Object> wkniW = new LinkedHashMap<>();
            wkniW.put("from", wkniF.wkniDesde()); wkniW.put("to", wkniF.wkniHasta());
            wkniVentanas.add(wkniW);
        }
        if (wkniVentanas.isEmpty()) wkni_p.put("schedule", null);
        else {
            Map<String, Object> wkniSc = new LinkedHashMap<>();
            wkniSc.put("windows", wkniVentanas);
            wkniSc.put("days", wkniDias == null ? new ArrayList<>() : new ArrayList<>(wkniDias));
            wkni_p.put("schedule", wkniSc);
        }
        wkniPersist();
    }

    /** Activa el perfil cuyo horario incluya la hora dada (minutos desde 00:00), si hay alguno. */
    public boolean wkniApplySchedule(int wkniMinuteOfDay) { return wkniApplySchedule(wkniMinuteOfDay, -1); }

    /**
     * Activa el perfil cuyo horario incluya la hora y el día dados.
     * {@code diaSemana} de 1 (lunes) a 7 (domingo); si es negativo se ignoran los días.
     */
    public boolean wkniApplySchedule(int wkniMinuteOfDay, int wkniDiaSemana) {
        for (Map<String, Object> wkni_p : wkniProfiles()) {
            String wkniId = String.valueOf(wkni_p.get("id"));
            List<wkniFranja> wkniFranjas = wkniFranjas(wkniId);
            if (wkniFranjas.isEmpty()) continue;
            List<Integer> wkniDias = wkniDias(wkniId);
            if (wkniDiaSemana > 0 && !wkniDias.isEmpty() && !wkniDias.contains(wkniDiaSemana)) continue;
            for (wkniFranja wkniF : wkniFranjas) {
                if (!wkniF.wkniContiene(wkniMinuteOfDay)) continue;
                if (wkniId.equals(wkniActiveProfileId())) return false;
                wkniActivateProfile(wkniId);
                return true;
            }
        }
        return false;
    }

    public static int wkniMinutes(String wkniHhmm) {
        if (!wkniValidTime(wkniHhmm)) return 0;
        String[] wkniT = wkniHhmm.split(":");
        return Integer.parseInt(wkniT[0]) * 60 + Integer.parseInt(wkniT[1]);
    }

    private static Map<String, Object> wkniNewProfile(String wkniName, Map<String, Object> wkniSettings) {
        Map<String, Object> wkni_p = new LinkedHashMap<>();
        wkni_p.put("id", UUID.randomUUID().toString().substring(0, 8));
        wkni_p.put("name", wkniName == null || wkniName.isBlank() ? "Perfil" : wkniName.trim());
        wkni_p.put("schedule", null);
        wkni_p.put("settings", wkniSettings);
        return wkni_p;
    }

    // ------------------------------------------------------------------ exportar / importar

    /** Exporta el perfil activo o todos a JSON. */
    public String wkniExport(boolean wkniAll) {
        Map<String, Object> wkniOut = new LinkedHashMap<>();
        wkniOut.put("type", wkniFILE_TYPE);
        wkniOut.put("version", wkniVERSION);
        List<Object> wkniPs = new ArrayList<>();
        if (wkniAll) wkniPs.addAll(wkniProfiles()); else wkniPs.add(wkniProfile(wkniActiveProfileId()));
        wkniOut.put("profiles", wkniPs);
        wkniOut.put("activeProfileId", wkniAll ? wkniActiveProfileId() : wkniProfile(wkniActiveProfileId()).get("id"));
        return wkniJson.wkniPretty(wkniOut);
    }

    public record wkniImportPreview(boolean wkniValid, String wkniMessage, List<String> wkniProfileNames, Map<String, Object> wkniParsed) {}

    /** Valida un archivo sin modificar nada. */
    public static wkniImportPreview wkniPreviewImport(String wkniText) {
        Map<String, Object> wkniR;
        try { wkniR = wkniJson.wkniParseObject(wkniText); }
        catch (RuntimeException wkniE) { return new wkniImportPreview(false, "El archivo no es un JSON válido.", List.of(), null); }
        if (!wkniFILE_TYPE.equals(wkniR.get("type"))) return new wkniImportPreview(false, "El archivo no es una exportación de Compás.", List.of(), null);
        if (!(wkniR.get("version") instanceof Number wkniN)) return new wkniImportPreview(false, "El archivo no tiene versión.", List.of(), null);
        if (wkniN.intValue() > wkniVERSION) return new wkniImportPreview(false, "El archivo es de una versión más nueva (" + wkniN.intValue() + "). Actualiza Compás.", List.of(), null);
        if (wkniN.intValue() < 1) return new wkniImportPreview(false, "Versión de archivo inválida.", List.of(), null);
        Map<String, Object> wkniClean;
        try { wkniClean = wkniSanitize(wkniR); } catch (RuntimeException wkniE) { wkniClean = null; }
        if (wkniClean == null) return new wkniImportPreview(false, "El archivo no contiene perfiles válidos.", List.of(), null);
        List<String> wkniNames = new ArrayList<>();
        for (Object wkniO : (List<?>) wkniClean.get("profiles")) wkniNames.add((String) ((Map<?, ?>) wkniO).get("name"));
        return new wkniImportPreview(true, "Se añadirán " + wkniNames.size() + " perfil(es): " + String.join(", ", wkniNames) + ".", wkniNames, wkniClean);
    }

    /** Aplica una importación ya validada: añade los perfiles (con ids nuevos) y activa el primero. */
    @SuppressWarnings("unchecked")
    public void wkniApplyImport(wkniImportPreview wkniPv) {
        if (wkniPv == null || !wkniPv.wkniValid()) return;
        String wkniFirst = null;
        for (Object wkniO : (List<?>) wkniPv.wkniParsed().get("profiles")) {
            Map<String, Object> wkni_p = (Map<String, Object>) wkniO;
            Map<String, Object> wkniNp = wkniNewProfile((String) wkni_p.get("name"), (Map<String, Object>) wkni_p.get("settings"));
            wkniNp.put("schedule", wkni_p.get("schedule"));
            wkniProfiles().add(wkniNp);
            if (wkniFirst == null) wkniFirst = (String) wkniNp.get("id");
        }
        wkniPersist();
        if (wkniFirst != null) { wkniRoot.put("activeProfileId", wkniFirst); wkniPersist(); wkniFire("*"); }
    }

    /** Restablece todos los ajustes del perfil activo. */
    public void wkniResetActiveProfile() {
        wkniProfile(wkniActiveProfileId()).put("settings", wkniDefaults.wkniSettings());
        wkniPersist();
        wkniFire("*");
    }

    /** Reinicia todos los perfiles y preferencias tras borrar todos los datos de la aplicación. */
    public void wkniResetAll() {
        wkniRoot = wkniLoad();
        wkniFire("*");
    }
}

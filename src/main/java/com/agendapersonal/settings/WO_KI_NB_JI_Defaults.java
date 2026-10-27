package com.agendapersonal.settings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Valores por defecto de todos los ajustes. Con ellos la app se ve y funciona como antes. */
public final class WO_KI_NB_JI_Defaults {
    private WO_KI_NB_JI_Defaults() {}

    public static Map<String, Object> WO_KI_NB_JI_settings() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("appearance", WO_KI_NB_JI_m(
            "themeId", "claro", "mode", "claro", "accentId", "NARANJA", "colors", WO_KI_NB_JI_m(),   // colors: bg, card, text, accent (vacío = del tema)
            "font", "Plus Jakarta Sans", "fontScale", 100, "density", "comoda", "radius", 12,
            "warmLowContrast", false, "reduceMotion", false, "autoThemeByTime", false,
            "autoDarkFrom", "19:00", "autoDarkTo", "07:00"));
        s.put("sidebar", WO_KI_NB_JI_m("order", new ArrayList<>(), "hidden", new ArrayList<>(), "collapsed", false));
        s.put("home", WO_KI_NB_JI_m("columns", 2, "widgetSize", "mediano", "widgets", new ArrayList<>(), "showGreeting", true, "bigClock", false,
            "lockEdit", false, "userName", "", "phrase", "", "showSeconds", false, "hour24", true));
        s.put("calendar", WO_KI_NB_JI_m("defaultView", "mes", "weekStart", "lunes", "hoursFrom", 6, "hoursTo", 23, "timezone", "", "calendarColors", WO_KI_NB_JI_m(),
            "density", "comoda", "hourHeight", 48, "showWeekends", true, "showWeekNumbers", false, "dimPast", false));
        s.put("timeBlocking", WO_KI_NB_JI_m("presets", new ArrayList<>(), "activePreset", 0, "categories", new ArrayList<>(), "sound", true, "volume", 60,
            "background", "degradado", "backgroundFile", "", "backgroundColor", "#1B1E25", "backgroundLoop", true, "backgroundDim", 35,
            "audioFile", "", "clockSize", "mediano", "showProgress", true, "showDetail", true, "showTasks", true, "todoSource", "ambas",
            "importance", "NORMAL", "pauseOnLeave", false, "notifyEnd", true));
        s.put("study", WO_KI_NB_JI_m("widgets", new ArrayList<>(), "columns", 2));
        s.put("tasks", WO_KI_NB_JI_m("view", "lista", "groupBy", "ninguno", "visibleTabs", List.of("Todas", "Hoy", "Próximas", "Vencidas", "Completadas"),
            "priorityColors", WO_KI_NB_JI_m(), "extraFields", new ArrayList<>(), "subtasks", true, "completedLast", false, "tabCounters", true));
        s.put("notes", WO_KI_NB_JI_m("types", new ArrayList<>(), "defaultTemplate", "", "editorFont", "", "editorWidth", "ancho", "tagColor", "",
            "markdown", true, "autosave", true, "livePreview", false));
        s.put("contacts", WO_KI_NB_JI_m("formFields", new ArrayList<>(), "categories", new ArrayList<>(), "avatar", "iniciales", "sortBy", "nombre",
            "remindEveryDays", 0, "birthdayAlerts", true));
        s.put("habits", WO_KI_NB_JI_m("defaultFrequency", "diaria", "defaultColor", "", "dailyGoal", 3, "view", "semana", "weekStart", "lunes",
            "reminderTime", "", "streakCountsWeekends", true, "confetti", true));
        s.put("finance", WO_KI_NB_JI_m("currency", "S/", "categories", new ArrayList<>(), "monthlyBudget", 0, "numberFormat", "1,234.56",
            "period", "mes", "hideAmounts", false, "budgetAlert", true));
        s.put("health", WO_KI_NB_JI_m("visibleMetrics", new ArrayList<>(), "goals", WO_KI_NB_JI_m("water", 8, "exercise", 30), "weightUnit", "kg",
            "waterUnit", "vasos", "waterReminder", false, "weeklySummary", false));
        s.put("notifications", WO_KI_NB_JI_m("eventStart", true, "birthdays", true, "confirmations", false, "quietHours", WO_KI_NB_JI_m("from", "", "to", "")));
        return s;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> WO_KI_NB_JI_m(Object... kv) {
        Map<String, Object> r = new LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) r.put((String) kv[i], kv[i + 1]);
        return r;
    }
}

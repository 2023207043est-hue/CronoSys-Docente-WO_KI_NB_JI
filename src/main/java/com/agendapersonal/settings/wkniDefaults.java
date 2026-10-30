package com.agendapersonal.settings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Valores por defecto de todos los ajustes. Con ellos la app se ve y funciona como antes. */
public final class wkniDefaults {
    private wkniDefaults() {}

    public static Map<String, Object> wkniSettings() {
        Map<String, Object> wkniS = new LinkedHashMap<>();
        wkniS.put("appearance", wkniM(
            "themeId", "claro", "mode", "claro", "accentId", "NARANJA", "colors", wkniM(),   // colors: bg, card, text, accent (vacío = del tema)
            "font", "Plus Jakarta Sans", "fontScale", 100, "density", "comoda", "radius", 12,
            "warmLowContrast", false, "reduceMotion", false, "autoThemeByTime", false,
            "autoDarkFrom", "19:00", "autoDarkTo", "07:00"));
        wkniS.put("sidebar", wkniM("order", new ArrayList<>(), "hidden", new ArrayList<>(), "collapsed", false));
        wkniS.put("home", wkniM("columns", 2, "widgetSize", "mediano", "widgets", new ArrayList<>(), "showGreeting", true, "bigClock", false,
            "lockEdit", false, "userName", "", "phrase", "", "showSeconds", false, "hour24", true));
        wkniS.put("calendar", wkniM("defaultView", "mes", "weekStart", "lunes", "hoursFrom", 6, "hoursTo", 23, "timezone", "", "calendarColors", wkniM(),
            "density", "comoda", "hourHeight", 48, "showWeekends", true, "showWeekNumbers", false, "dimPast", false));
        wkniS.put("timeBlocking", wkniM("presets", new ArrayList<>(), "activePreset", 0, "categories", new ArrayList<>(), "sound", true, "volume", 60,
            "background", "degradado", "backgroundFile", "", "backgroundColor", "#1B1E25", "backgroundLoop", true, "backgroundDim", 35,
            "audioFile", "", "clockSize", "mediano", "showProgress", true, "showDetail", true, "showTasks", true, "todoSource", "ambas",
            "importance", "NORMAL", "pauseOnLeave", false, "notifyEnd", true));
        wkniS.put("study", wkniM("widgets", new ArrayList<>(), "columns", 2));
        wkniS.put("tasks", wkniM("view", "lista", "groupBy", "ninguno", "visibleTabs", List.of("Todas", "Hoy", "Próximas", "Vencidas", "Completadas"),
            "priorityColors", wkniM(), "extraFields", new ArrayList<>(), "subtasks", true, "completedLast", false, "tabCounters", true));
        wkniS.put("notes", wkniM("types", new ArrayList<>(), "defaultTemplate", "", "editorFont", "", "editorWidth", "ancho", "tagColor", "",
            "markdown", true, "autosave", true, "livePreview", false));
        wkniS.put("contacts", wkniM("formFields", new ArrayList<>(), "categories", new ArrayList<>(), "avatar", "iniciales", "sortBy", "nombre",
            "remindEveryDays", 0, "birthdayAlerts", true));
        wkniS.put("habits", wkniM("defaultFrequency", "diaria", "defaultColor", "", "dailyGoal", 3, "view", "semana", "weekStart", "lunes",
            "reminderTime", "", "streakCountsWeekends", true, "confetti", true));
        wkniS.put("finance", wkniM("currency", "S/", "categories", new ArrayList<>(), "monthlyBudget", 0, "numberFormat", "1,234.56",
            "period", "mes", "hideAmounts", false, "budgetAlert", true));
        wkniS.put("health", wkniM("visibleMetrics", new ArrayList<>(), "goals", wkniM("water", 8, "exercise", 30), "weightUnit", "kg",
            "waterUnit", "vasos", "waterReminder", false, "weeklySummary", false));
        wkniS.put("notifications", wkniM("eventStart", true, "birthdays", true, "confirmations", false, "quietHours", wkniM("from", "", "to", "")));
        return wkniS;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> wkniM(Object... wkniKv) {
        Map<String, Object> wkniR = new LinkedHashMap<>();
        for (int wkniI = 0; wkniI + 1 < wkniKv.length; wkniI += 2) wkniR.put((String) wkniKv[wkniI], wkniKv[wkniI + 1]);
        return wkniR;
    }
}

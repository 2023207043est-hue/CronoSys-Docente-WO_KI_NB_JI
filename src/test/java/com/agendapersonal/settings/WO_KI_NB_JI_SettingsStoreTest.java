package com.agendapersonal.settings;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class WO_KI_NB_JI_SettingsStoreTest {
    static class WO_KI_NB_JI_Mem implements WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_Storage {
        final Map<String, String> m = new HashMap<>();
        public String WO_KI_NB_JI_get(String k, String d) { return m.getOrDefault(k, d); }
        public void WO_KI_NB_JI_set(String k, String v) { m.put(k, v); }
    }

    @Test void WO_KI_NB_JI_migraDesdeClavesAntiguas() {
        WO_KI_NB_JI_Mem mem = new WO_KI_NB_JI_Mem();
        mem.m.put("ui.theme", "Oscuro"); mem.m.put("ui.accent", "AZUL"); mem.m.put("ui.fontSize", "GRANDE");
        mem.m.put("ui.sidebar.collapsed", "1"); mem.m.put("usuario.nombre", "Ana");
        WO_KI_NB_JI_SettingsStore s = new WO_KI_NB_JI_SettingsStore(mem);
        assertEquals("oscuro", s.WO_KI_NB_JI_str("appearance.themeId"));
        assertEquals("AZUL", s.WO_KI_NB_JI_str("appearance.accentId"));
        assertEquals(111, s.WO_KI_NB_JI_integer("appearance.fontScale", 85, 125));
        assertTrue(s.WO_KI_NB_JI_bool("sidebar.collapsed"));
        assertEquals("Ana", s.WO_KI_NB_JI_str("home.userName"));
        assertNotNull(mem.m.get(WO_KI_NB_JI_SettingsStore.KEY));
    }

    @Test void WO_KI_NB_JI_valoresPorDefectoEquivalenALoDeHoy() {
        WO_KI_NB_JI_SettingsStore s = new WO_KI_NB_JI_SettingsStore(new WO_KI_NB_JI_Mem());
        assertEquals("claro", s.WO_KI_NB_JI_str("appearance.themeId"));
        assertEquals("NARANJA", s.WO_KI_NB_JI_str("appearance.accentId"));
        assertEquals(100, s.WO_KI_NB_JI_integer("appearance.fontScale", 85, 125));
        assertEquals(12, s.WO_KI_NB_JI_integer("appearance.radius", 0, 28));
    }

    @Test void WO_KI_NB_JI_ajustesCorruptosNoFallan() {
        WO_KI_NB_JI_Mem mem = new WO_KI_NB_JI_Mem();
        mem.m.put(WO_KI_NB_JI_SettingsStore.KEY, "{esto no es json");
        assertEquals("claro", new WO_KI_NB_JI_SettingsStore(mem).WO_KI_NB_JI_str("appearance.themeId"));
        mem.m.put(WO_KI_NB_JI_SettingsStore.KEY, "{\"version\":99,\"profiles\":[]}");
        assertEquals("claro", new WO_KI_NB_JI_SettingsStore(mem).WO_KI_NB_JI_str("appearance.themeId"));
        mem.m.put(WO_KI_NB_JI_SettingsStore.KEY, "{\"version\":1,\"activeProfileId\":\"x\",\"profiles\":[{\"id\":\"a\",\"name\":\"A\",\"settings\":{\"appearance\":{\"radius\":\"muchisimo\",\"fontScale\":9999,\"themeId\":7,\"density\":\"rara\"}}}]}");
        WO_KI_NB_JI_SettingsStore s = new WO_KI_NB_JI_SettingsStore(mem);
        assertEquals("a", s.WO_KI_NB_JI_activeProfileId());
        assertEquals(12, s.WO_KI_NB_JI_integer("appearance.radius", 0, 28));
        assertEquals(125, s.WO_KI_NB_JI_integer("appearance.fontScale", 85, 125));
        assertEquals("claro", s.WO_KI_NB_JI_str("appearance.themeId"));
        assertEquals("comoda", s.WO_KI_NB_JI_choice("appearance.density", "compacta", "comoda", "amplia"));
    }

    @Test void WO_KI_NB_JI_escrituraPersisteYAvisa() {
        WO_KI_NB_JI_Mem mem = new WO_KI_NB_JI_Mem();
        WO_KI_NB_JI_SettingsStore s = new WO_KI_NB_JI_SettingsStore(mem);
        int[] avisos = {0};
        s.WO_KI_NB_JI_addListener(sec -> avisos[0]++);
        s.WO_KI_NB_JI_set("appearance.radius", 20);
        s.WO_KI_NB_JI_set("health.goals.water", 10);
        assertEquals(2, avisos[0]);
        WO_KI_NB_JI_SettingsStore otra = new WO_KI_NB_JI_SettingsStore(mem);
        assertEquals(20, otra.WO_KI_NB_JI_integer("appearance.radius", 0, 28));
        assertEquals(10, otra.WO_KI_NB_JI_integer("health.goals.water", 0, 50));
        s.WO_KI_NB_JI_resetSection("appearance");
        assertEquals(12, s.WO_KI_NB_JI_integer("appearance.radius", 0, 28));
    }

    @Test void WO_KI_NB_JI_perfilesSiempreQuedaUno() {
        WO_KI_NB_JI_SettingsStore s = new WO_KI_NB_JI_SettingsStore(new WO_KI_NB_JI_Mem());
        assertFalse(s.WO_KI_NB_JI_deleteProfile(s.WO_KI_NB_JI_activeProfileId()));
        String b = s.WO_KI_NB_JI_createProfile("Estudio");
        s.WO_KI_NB_JI_set("appearance.radius", 4);
        s.WO_KI_NB_JI_activateProfile(b);
        assertEquals(12, s.WO_KI_NB_JI_integer("appearance.radius", 0, 28));   // perfil nuevo = valores por defecto
        String c = s.WO_KI_NB_JI_duplicateProfile(b);
        assertEquals("Estudio (copia)", s.WO_KI_NB_JI_profile(c).get("name"));
        assertTrue(s.WO_KI_NB_JI_deleteProfile(b));
        assertEquals("default", s.WO_KI_NB_JI_activeProfileId());
    }

    @Test void WO_KI_NB_JI_horarioDePerfilesCruzandoMedianoche() {
        WO_KI_NB_JI_SettingsStore s = new WO_KI_NB_JI_SettingsStore(new WO_KI_NB_JI_Mem());
        String noche = s.WO_KI_NB_JI_createProfile("Noche");
        s.WO_KI_NB_JI_setSchedule(noche, "19:00", "08:00");
        assertTrue(s.WO_KI_NB_JI_applySchedule(23 * 60));
        assertEquals(noche, s.WO_KI_NB_JI_activeProfileId());
        assertFalse(s.WO_KI_NB_JI_applySchedule(23 * 60 + 30));
        s.WO_KI_NB_JI_setSchedule(noche, "25:00", "08:00");
        assertNull(s.WO_KI_NB_JI_profile(noche).get("schedule"));
    }

    @Test void WO_KI_NB_JI_variasFranjasEnElHorarioDeUnPerfil() {
        WO_KI_NB_JI_SettingsStore s = new WO_KI_NB_JI_SettingsStore(new WO_KI_NB_JI_Mem());
        String estudio = s.WO_KI_NB_JI_createProfile("Estudio");
        s.WO_KI_NB_JI_setScheduleWindows(estudio, List.of(
            new WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_Franja("08:00", "12:00"),
            new WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_Franja("16:00", "18:00")), List.of(1, 2, 3, 4, 5));
        assertEquals(2, s.WO_KI_NB_JI_franjas(estudio).size());
        assertEquals(List.of(1, 2, 3, 4, 5), s.WO_KI_NB_JI_dias(estudio));

        // Lunes (1) a las 09:00 entra el perfil.
        assertTrue(s.WO_KI_NB_JI_applySchedule(9 * 60, 1));
        assertEquals(estudio, s.WO_KI_NB_JI_activeProfileId());
        // Lunes a las 13:00 no hay franja: no cambia.
        s.WO_KI_NB_JI_activateProfile(s.WO_KI_NB_JI_profiles().get(0).get("id").toString());
        assertFalse(s.WO_KI_NB_JI_applySchedule(13 * 60, 1));
        // Sábado (6) no está entre los días marcados.
        assertFalse(s.WO_KI_NB_JI_applySchedule(9 * 60, 6));
        // Sábado sí, si no se marcan días.
        s.WO_KI_NB_JI_setScheduleWindows(estudio, List.of(new WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_Franja("08:00", "12:00")), List.of());
        assertTrue(s.WO_KI_NB_JI_applySchedule(9 * 60, 6));
    }

    @Test void WO_KI_NB_JI_lasFranjasCruzanLaMedianoche() {
        WO_KI_NB_JI_SettingsStore s = new WO_KI_NB_JI_SettingsStore(new WO_KI_NB_JI_Mem());
        String noche = s.WO_KI_NB_JI_createProfile("Noche");
        s.WO_KI_NB_JI_setScheduleWindows(noche, List.of(new WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_Franja("19:00", "08:00")), List.of());
        WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_Franja f = s.WO_KI_NB_JI_franjas(noche).get(0);
        assertTrue(f.WO_KI_NB_JI_contiene(23 * 60));
        assertTrue(f.WO_KI_NB_JI_contiene(2 * 60));
        assertFalse(f.WO_KI_NB_JI_contiene(12 * 60));
        // applySchedule devuelve true solo cuando cambia el perfil activo.
        assertTrue(s.WO_KI_NB_JI_applySchedule(23 * 60, 3));
        assertEquals(noche, s.WO_KI_NB_JI_activeProfileId());
        assertFalse(s.WO_KI_NB_JI_applySchedule(2 * 60, 3));   // ya estaba activo: no cambia nada
    }

    @Test void WO_KI_NB_JI_elHorarioAntiguoSeSigueLeyendo() {
        WO_KI_NB_JI_SettingsStore s = new WO_KI_NB_JI_SettingsStore(new WO_KI_NB_JI_Mem());
        String p = s.WO_KI_NB_JI_createProfile("Antiguo");
        s.WO_KI_NB_JI_setSchedule(p, "09:00", "13:00");
        assertEquals(1, s.WO_KI_NB_JI_franjas(p).size());
        assertEquals("09:00", s.WO_KI_NB_JI_franjas(p).get(0).desde());
        assertTrue(s.WO_KI_NB_JI_applySchedule(10 * 60, 1));
    }

    @Test void WO_KI_NB_JI_exportarEImportar() {
        WO_KI_NB_JI_SettingsStore a = new WO_KI_NB_JI_SettingsStore(new WO_KI_NB_JI_Mem());
        a.WO_KI_NB_JI_set("appearance.themeId", "neon");
        a.WO_KI_NB_JI_set("appearance.colors.accent", "#FF00AA");
        String json = a.WO_KI_NB_JI_export(false);
        WO_KI_NB_JI_SettingsStore b = new WO_KI_NB_JI_SettingsStore(new WO_KI_NB_JI_Mem());
        WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_ImportPreview pv = WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_previewImport(json);
        assertTrue(pv.valid());
        b.WO_KI_NB_JI_applyImport(pv);
        assertEquals("neon", b.WO_KI_NB_JI_str("appearance.themeId"));
        assertEquals("#FF00AA", b.WO_KI_NB_JI_str("appearance.colors.accent"));
        assertEquals(2, b.WO_KI_NB_JI_profiles().size());
    }

    @Test void WO_KI_NB_JI_importacionInvalidaNoCambiaNada() {
        WO_KI_NB_JI_SettingsStore s = new WO_KI_NB_JI_SettingsStore(new WO_KI_NB_JI_Mem());
        for (String malo : new String[]{"hola", "{}", "{\"type\":\"cronosys-settings\",\"version\":5,\"profiles\":[]}", "{\"type\":\"cronosys-settings\",\"version\":1,\"profiles\":[]}", null}) {
            WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_ImportPreview pv = WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_previewImport(malo);
            assertFalse(pv.valid());
            s.WO_KI_NB_JI_applyImport(pv);
        }
        assertEquals(1, s.WO_KI_NB_JI_profiles().size());
    }

    @Test void WO_KI_NB_JI_contrasteYTextoSobreAcento() {
        assertEquals("#FFFFFF", WO_KI_NB_JI_Colors.WO_KI_NB_JI_onColor("#1B2A6B"));
        assertEquals("#16110E", WO_KI_NB_JI_Colors.WO_KI_NB_JI_onColor("#F6D365"));
        assertEquals("#00F0FF", WO_KI_NB_JI_Colors.WO_KI_NB_JI_normalize("#0ff0ff".replace("0ff0ff", "00f0ff")));
        assertNull(WO_KI_NB_JI_Colors.WO_KI_NB_JI_normalize("zzz"));
        assertEquals("#AABBCC", WO_KI_NB_JI_Colors.WO_KI_NB_JI_normalize("abc".equals("abc") ? "#ABC" : ""));
        assertTrue(WO_KI_NB_JI_Colors.WO_KI_NB_JI_contrast("#000000", "#FFFFFF") > 20);
        assertFalse(WO_KI_NB_JI_Palette.WO_KI_NB_JI_contrastWarnings("#FFFFFF", "#FFFFFF", "#EEEEEE", "#F0F0F0").isEmpty());
        assertTrue(WO_KI_NB_JI_Palette.WO_KI_NB_JI_contrastWarnings("#F5F4F0", "#FFFFFF", "#1C1F26", "#C64B1C").isEmpty());
    }

    @Test void WO_KI_NB_JI_resolucionDeTema() {
        WO_KI_NB_JI_SettingsStore s = new WO_KI_NB_JI_SettingsStore(new WO_KI_NB_JI_Mem());
        WO_KI_NB_JI_Palette.WO_KI_NB_JI_Resolved r = WO_KI_NB_JI_Palette.WO_KI_NB_JI_resolve(s, LocalTime.NOON);
        assertTrue(r.legacy());
        assertFalse(r.dark());
        s.WO_KI_NB_JI_set("appearance.autoThemeByTime", true);
        assertTrue(WO_KI_NB_JI_Palette.WO_KI_NB_JI_resolve(s, LocalTime.of(22, 0)).dark());
        assertFalse(WO_KI_NB_JI_Palette.WO_KI_NB_JI_resolve(s, LocalTime.of(12, 0)).dark());
        s.WO_KI_NB_JI_set("appearance.autoThemeByTime", false);
        s.WO_KI_NB_JI_set("appearance.themeId", "neon");
        WO_KI_NB_JI_Palette.WO_KI_NB_JI_Resolved n = WO_KI_NB_JI_Palette.WO_KI_NB_JI_resolve(s, LocalTime.NOON);
        assertTrue(n.dark());
        assertEquals("#00F0FF", n.accent());
        s.WO_KI_NB_JI_set("appearance.colors.bg", "#FFFFFF");
        assertFalse(WO_KI_NB_JI_Palette.WO_KI_NB_JI_resolve(s, LocalTime.NOON).dark());
    }

    @Test void WO_KI_NB_JI_jsonIdaYVuelta() {
        Map<String, Object> o = WO_KI_NB_JI_Json.WO_KI_NB_JI_parseObject("{\"a\":[1,2.5,true,null,\"x\\n\\u00e9\"],\"b\":{\"c\":-3}}");
        assertEquals(o, WO_KI_NB_JI_Json.WO_KI_NB_JI_parseObject(WO_KI_NB_JI_Json.WO_KI_NB_JI_pretty(o)));
        assertThrows(WO_KI_NB_JI_Json.WO_KI_NB_JI_JsonException.class, () -> WO_KI_NB_JI_Json.WO_KI_NB_JI_parse("[1,2"));
    }
}

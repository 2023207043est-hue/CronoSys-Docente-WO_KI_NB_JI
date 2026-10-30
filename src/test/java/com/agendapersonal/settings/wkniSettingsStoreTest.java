package com.agendapersonal.settings;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class wkniSettingsStoreTest {
    static class wkniMem implements wkniSettingsStore.wkniStorage {
        final Map<String, String> m = new HashMap<>();
        public String wkniGet(String k, String d) { return m.getOrDefault(k, d); }
        public void wkniSet(String k, String v) { m.put(k, v); }
    }

    @Test void wkniMigraDesdeClavesAntiguas() {
        wkniMem mem = new wkniMem();
        mem.m.put("ui.theme", "Oscuro"); mem.m.put("ui.accent", "AZUL"); mem.m.put("ui.fontSize", "GRANDE");
        mem.m.put("ui.sidebar.collapsed", "1"); mem.m.put("usuario.nombre", "Ana");
        wkniSettingsStore s = new wkniSettingsStore(mem);
        assertEquals("oscuro", s.wkniStr("appearance.themeId"));
        assertEquals("AZUL", s.wkniStr("appearance.accentId"));
        assertEquals(111, s.wkniInteger("appearance.fontScale", 85, 125));
        assertTrue(s.wkniBool("sidebar.collapsed"));
        assertEquals("Ana", s.wkniStr("home.userName"));
        assertNotNull(mem.m.get(wkniSettingsStore.wkniKEY));
    }

    @Test void wkniValoresPorDefectoEquivalenALoDeHoy() {
        wkniSettingsStore s = new wkniSettingsStore(new wkniMem());
        assertEquals("claro", s.wkniStr("appearance.themeId"));
        assertEquals("NARANJA", s.wkniStr("appearance.accentId"));
        assertEquals(100, s.wkniInteger("appearance.fontScale", 85, 125));
        assertEquals(12, s.wkniInteger("appearance.radius", 0, 28));
    }

    @Test void wkniAjustesCorruptosNoFallan() {
        wkniMem mem = new wkniMem();
        mem.m.put(wkniSettingsStore.wkniKEY, "{esto no es json");
        assertEquals("claro", new wkniSettingsStore(mem).wkniStr("appearance.themeId"));
        mem.m.put(wkniSettingsStore.wkniKEY, "{\"version\":99,\"profiles\":[]}");
        assertEquals("claro", new wkniSettingsStore(mem).wkniStr("appearance.themeId"));
        mem.m.put(wkniSettingsStore.wkniKEY, "{\"version\":1,\"activeProfileId\":\"x\",\"profiles\":[{\"id\":\"a\",\"name\":\"A\",\"settings\":{\"appearance\":{\"radius\":\"muchisimo\",\"fontScale\":9999,\"themeId\":7,\"density\":\"rara\"}}}]}");
        wkniSettingsStore s = new wkniSettingsStore(mem);
        assertEquals("a", s.wkniActiveProfileId());
        assertEquals(12, s.wkniInteger("appearance.radius", 0, 28));
        assertEquals(125, s.wkniInteger("appearance.fontScale", 85, 125));
        assertEquals("claro", s.wkniStr("appearance.themeId"));
        assertEquals("comoda", s.wkniChoice("appearance.density", "compacta", "comoda", "amplia"));
    }

    @Test void wkniEscrituraPersisteYAvisa() {
        wkniMem mem = new wkniMem();
        wkniSettingsStore s = new wkniSettingsStore(mem);
        int[] avisos = {0};
        s.wkniAddListener(sec -> avisos[0]++);
        s.wkniSet("appearance.radius", 20);
        s.wkniSet("health.goals.water", 10);
        assertEquals(2, avisos[0]);
        wkniSettingsStore otra = new wkniSettingsStore(mem);
        assertEquals(20, otra.wkniInteger("appearance.radius", 0, 28));
        assertEquals(10, otra.wkniInteger("health.goals.water", 0, 50));
        s.wkniResetSection("appearance");
        assertEquals(12, s.wkniInteger("appearance.radius", 0, 28));
    }

    @Test void wkniPerfilesSiempreQuedaUno() {
        wkniSettingsStore s = new wkniSettingsStore(new wkniMem());
        assertFalse(s.wkniDeleteProfile(s.wkniActiveProfileId()));
        String b = s.wkniCreateProfile("Estudio");
        s.wkniSet("appearance.radius", 4);
        s.wkniActivateProfile(b);
        assertEquals(12, s.wkniInteger("appearance.radius", 0, 28));   // perfil nuevo = valores por defecto
        String c = s.wkniDuplicateProfile(b);
        assertEquals("Estudio (copia)", s.wkniProfile(c).get("name"));
        assertTrue(s.wkniDeleteProfile(b));
        assertEquals("default", s.wkniActiveProfileId());
    }

    @Test void wkniHorarioDePerfilesCruzandoMedianoche() {
        wkniSettingsStore s = new wkniSettingsStore(new wkniMem());
        String noche = s.wkniCreateProfile("Noche");
        s.wkniSetSchedule(noche, "19:00", "08:00");
        assertTrue(s.wkniApplySchedule(23 * 60));
        assertEquals(noche, s.wkniActiveProfileId());
        assertFalse(s.wkniApplySchedule(23 * 60 + 30));
        s.wkniSetSchedule(noche, "25:00", "08:00");
        assertNull(s.wkniProfile(noche).get("schedule"));
    }

    @Test void wkniVariasFranjasEnElHorarioDeUnPerfil() {
        wkniSettingsStore s = new wkniSettingsStore(new wkniMem());
        String estudio = s.wkniCreateProfile("Estudio");
        s.wkniSetScheduleWindows(estudio, List.of(
            new wkniSettingsStore.wkniFranja("08:00", "12:00"),
            new wkniSettingsStore.wkniFranja("16:00", "18:00")), List.of(1, 2, 3, 4, 5));
        assertEquals(2, s.wkniFranjas(estudio).size());
        assertEquals(List.of(1, 2, 3, 4, 5), s.wkniDias(estudio));

        // Lunes (1) a las 09:00 entra el perfil.
        assertTrue(s.wkniApplySchedule(9 * 60, 1));
        assertEquals(estudio, s.wkniActiveProfileId());
        // Lunes a las 13:00 no hay franja: no cambia.
        s.wkniActivateProfile(s.wkniProfiles().get(0).get("id").toString());
        assertFalse(s.wkniApplySchedule(13 * 60, 1));
        // Sábado (6) no está entre los días marcados.
        assertFalse(s.wkniApplySchedule(9 * 60, 6));
        // Sábado sí, si no se marcan días.
        s.wkniSetScheduleWindows(estudio, List.of(new wkniSettingsStore.wkniFranja("08:00", "12:00")), List.of());
        assertTrue(s.wkniApplySchedule(9 * 60, 6));
    }

    @Test void wkniLasFranjasCruzanLaMedianoche() {
        wkniSettingsStore s = new wkniSettingsStore(new wkniMem());
        String noche = s.wkniCreateProfile("Noche");
        s.wkniSetScheduleWindows(noche, List.of(new wkniSettingsStore.wkniFranja("19:00", "08:00")), List.of());
        wkniSettingsStore.wkniFranja f = s.wkniFranjas(noche).get(0);
        assertTrue(f.wkniContiene(23 * 60));
        assertTrue(f.wkniContiene(2 * 60));
        assertFalse(f.wkniContiene(12 * 60));
        // applySchedule devuelve true solo cuando cambia el perfil activo.
        assertTrue(s.wkniApplySchedule(23 * 60, 3));
        assertEquals(noche, s.wkniActiveProfileId());
        assertFalse(s.wkniApplySchedule(2 * 60, 3));   // ya estaba activo: no cambia nada
    }

    @Test void wkniElHorarioAntiguoSeSigueLeyendo() {
        wkniSettingsStore s = new wkniSettingsStore(new wkniMem());
        String p = s.wkniCreateProfile("Antiguo");
        s.wkniSetSchedule(p, "09:00", "13:00");
        assertEquals(1, s.wkniFranjas(p).size());
        assertEquals("09:00", s.wkniFranjas(p).get(0).wkniDesde());
        assertTrue(s.wkniApplySchedule(10 * 60, 1));
    }

    @Test void wkniExportarEImportar() {
        wkniSettingsStore a = new wkniSettingsStore(new wkniMem());
        a.wkniSet("appearance.themeId", "neon");
        a.wkniSet("appearance.colors.accent", "#FF00AA");
        String json = a.wkniExport(false);
        wkniSettingsStore b = new wkniSettingsStore(new wkniMem());
        wkniSettingsStore.wkniImportPreview pv = wkniSettingsStore.wkniPreviewImport(json);
        assertTrue(pv.wkniValid());
        b.wkniApplyImport(pv);
        assertEquals("neon", b.wkniStr("appearance.themeId"));
        assertEquals("#FF00AA", b.wkniStr("appearance.colors.accent"));
        assertEquals(2, b.wkniProfiles().size());
    }

    @Test void wkniImportacionInvalidaNoCambiaNada() {
        wkniSettingsStore s = new wkniSettingsStore(new wkniMem());
        for (String malo : new String[]{"hola", "{}", "{\"type\":\"cronosys-settings\",\"version\":5,\"profiles\":[]}", "{\"type\":\"cronosys-settings\",\"version\":1,\"profiles\":[]}", null}) {
            wkniSettingsStore.wkniImportPreview pv = wkniSettingsStore.wkniPreviewImport(malo);
            assertFalse(pv.wkniValid());
            s.wkniApplyImport(pv);
        }
        assertEquals(1, s.wkniProfiles().size());
    }

    @Test void wkniContrasteYTextoSobreAcento() {
        assertEquals("#FFFFFF", wkniColors.wkniOnColor("#1B2A6B"));
        assertEquals("#16110E", wkniColors.wkniOnColor("#F6D365"));
        assertEquals("#00F0FF", wkniColors.wkniNormalize("#0ff0ff".replace("0ff0ff", "00f0ff")));
        assertNull(wkniColors.wkniNormalize("zzz"));
        assertEquals("#AABBCC", wkniColors.wkniNormalize("abc".equals("abc") ? "#ABC" : ""));
        assertTrue(wkniColors.wkniContrast("#000000", "#FFFFFF") > 20);
        assertFalse(wkniPalette.wkniContrastWarnings("#FFFFFF", "#FFFFFF", "#EEEEEE", "#F0F0F0").isEmpty());
        assertTrue(wkniPalette.wkniContrastWarnings("#F5F4F0", "#FFFFFF", "#1C1F26", "#C64B1C").isEmpty());
    }

    @Test void wkniResolucionDeTema() {
        wkniSettingsStore s = new wkniSettingsStore(new wkniMem());
        wkniPalette.wkniResolved r = wkniPalette.wkniResolve(s, LocalTime.NOON);
        assertTrue(r.wkniLegacy());
        assertFalse(r.wkniDark());
        s.wkniSet("appearance.autoThemeByTime", true);
        assertTrue(wkniPalette.wkniResolve(s, LocalTime.of(22, 0)).wkniDark());
        assertFalse(wkniPalette.wkniResolve(s, LocalTime.of(12, 0)).wkniDark());
        s.wkniSet("appearance.autoThemeByTime", false);
        s.wkniSet("appearance.themeId", "neon");
        wkniPalette.wkniResolved n = wkniPalette.wkniResolve(s, LocalTime.NOON);
        assertTrue(n.wkniDark());
        assertEquals("#00F0FF", n.wkni_accent());
        s.wkniSet("appearance.colors.bg", "#FFFFFF");
        assertFalse(wkniPalette.wkniResolve(s, LocalTime.NOON).wkniDark());
    }

    @Test void wkniJsonIdaYVuelta() {
        Map<String, Object> o = wkniJson.wkniParseObject("{\"a\":[1,2.5,true,null,\"x\\n\\u00e9\"],\"b\":{\"c\":-3}}");
        assertEquals(o, wkniJson.wkniParseObject(wkniJson.wkniPretty(o)));
        assertThrows(wkniJson.wkniJsonException.class, () -> wkniJson.wkniParse("[1,2"));
    }
}

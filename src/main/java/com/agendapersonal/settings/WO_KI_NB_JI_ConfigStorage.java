package com.agendapersonal.settings;

import com.agendapersonal.infrastructure.db.WO_KI_NB_JI_ConfigDAO;

/** Guarda el JSON de ajustes en la tabla config existente (una sola fila). */
final class WO_KI_NB_JI_ConfigStorage implements WO_KI_NB_JI_SettingsStore.WO_KI_NB_JI_Storage {
    private final WO_KI_NB_JI_ConfigDAO dao = new WO_KI_NB_JI_ConfigDAO();
    @Override public String WO_KI_NB_JI_get(String key, String def) { return dao.WO_KI_NB_JI_get(key, def); }
    @Override public void WO_KI_NB_JI_set(String key, String value) { dao.WO_KI_NB_JI_set(key, value); }
}

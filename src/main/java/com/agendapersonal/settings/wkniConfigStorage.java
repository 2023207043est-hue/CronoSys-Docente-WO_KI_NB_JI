package com.agendapersonal.settings;

import com.agendapersonal.infrastructure.db.wkniConfigDAO;

/** Guarda el JSON de ajustes en la tabla config existente (una sola fila). */
final class wkniConfigStorage implements wkniSettingsStore.wkniStorage {
    private final wkniConfigDAO wkniDao = new wkniConfigDAO();
    @Override public String wkniGet(String wkniKey, String wkniDef) { return wkniDao.wkniGet(wkniKey, wkniDef); }
    @Override public void wkniSet(String wkniKey, String wkniValue) { wkniDao.wkniSet(wkniKey, wkniValue); }
}

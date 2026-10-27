package com.agendapersonal.infrastructure.db;

import com.agendapersonal.database.WO_KI_NB_JI_DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class WO_KI_NB_JI_ConfigDAO {
    public Map<String, String> WO_KI_NB_JI_todas() {
        Map<String, String> m = new HashMap<>();
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("SELECT clave,valor FROM config");
             ResultSet s = p.executeQuery()) {
            while (s.next()) m.put(s.getString(1), s.getString(2));
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("config: ".trim(), e); }
        return m;
    }
    public String WO_KI_NB_JI_get(String clave, String def) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("SELECT valor FROM config WHERE clave=?")) {
            p.setString(1, clave);
            try (ResultSet s = p.executeQuery()) { if (s.next()) return s.getString(1); }
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("config get: ".trim(), e); }
        return def;
    }
    public void WO_KI_NB_JI_set(String clave, String valor) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO config(clave,valor) VALUES(?,?) ON CONFLICT(clave) DO UPDATE SET valor=excluded.valor")) {
            p.setString(1, clave); p.setString(2, valor); p.executeUpdate();
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("config set: ".trim(), e); }
    }
}

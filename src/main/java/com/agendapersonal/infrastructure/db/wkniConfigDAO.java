package com.agendapersonal.infrastructure.db;

import com.agendapersonal.database.wkniDatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

public class wkniConfigDAO {
    public Map<String, String> wkniTodas() {
        Map<String, String> wkniM = new HashMap<>();
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("SELECT clave,valor FROM config");
             ResultSet wkniS = wkni_p.executeQuery()) {
            while (wkniS.next()) wkniM.put(wkniS.getString(1), wkniS.getString(2));
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("config: ".trim(), wkniE); }
        return wkniM;
    }
    public String wkniGet(String wkniClave, String wkniDef) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("SELECT valor FROM config WHERE clave=?")) {
            wkni_p.setString(1, wkniClave);
            try (ResultSet wkniS = wkni_p.executeQuery()) { if (wkniS.next()) return wkniS.getString(1); }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("config get: ".trim(), wkniE); }
        return wkniDef;
    }
    public void wkniSet(String wkniClave, String wkniValor) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("INSERT INTO config(clave,valor) VALUES(?,?) ON CONFLICT(clave) DO UPDATE SET valor=excluded.valor")) {
            wkni_p.setString(1, wkniClave); wkni_p.setString(2, wkniValor); wkni_p.executeUpdate();
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("config set: ".trim(), wkniE); }
    }
}

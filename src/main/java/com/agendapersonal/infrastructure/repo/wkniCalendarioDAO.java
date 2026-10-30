package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.wkniDatabaseConnection;
import com.agendapersonal.domain.model.wkniCalendario;
import java.sql.*;
import java.util.*;

public class wkniCalendarioDAO {
    public List<wkniCalendario> wkniListar() {
        List<wkniCalendario> wkniR = new ArrayList<>();
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("SELECT * FROM calendario ORDER BY id");
             ResultSet wkniS = wkni_p.executeQuery()) {
            while (wkniS.next()) wkniR.add(new wkniCalendario(wkniS.getInt("id"), wkniS.getString("nombre"), wkniS.getString("color")));
        } catch (SQLException wkniX) { com.agendapersonal.util.wkniLog.wkniError("Calendario SELECT: ".trim(), wkniX); }
        return wkniR;
    }

    public boolean wkniInsertar(wkniCalendario wkniK) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("INSERT INTO calendario(nombre,color) VALUES(?,?)")) {
            wkni_p.setString(1, wkniK.wkniGetNombre()); wkni_p.setString(2, wkniK.wkniGetColor());
            return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniX) { com.agendapersonal.util.wkniLog.wkniError("CalendarioDAO", wkniX); return false; }
    }

    public boolean wkniActualizar(wkniCalendario wkniK) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("UPDATE calendario SET nombre=?,color=? WHERE id=?")) {
            wkni_p.setString(1, wkniK.wkniGetNombre()); wkni_p.setString(2, wkniK.wkniGetColor()); wkni_p.setInt(3, wkniK.wkniGetId());
            return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniX) { com.agendapersonal.util.wkniLog.wkniError("CalendarioDAO", wkniX); return false; }
    }

    public boolean wkniEliminar(int wkniId) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("DELETE FROM calendario WHERE id=?")) {
            wkni_p.setInt(1, wkniId);
            return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniX) { com.agendapersonal.util.wkniLog.wkniError("CalendarioDAO", wkniX); return false; }
    }
}

package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.WO_KI_NB_JI_DatabaseConnection;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Calendario;
import java.sql.*;
import java.util.*;

public class WO_KI_NB_JI_CalendarioDAO {
    public List<WO_KI_NB_JI_Calendario> WO_KI_NB_JI_listar() {
        List<WO_KI_NB_JI_Calendario> r = new ArrayList<>();
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM calendario ORDER BY id");
             ResultSet s = p.executeQuery()) {
            while (s.next()) r.add(new WO_KI_NB_JI_Calendario(s.getInt("id"), s.getString("nombre"), s.getString("color")));
        } catch (SQLException x) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("Calendario SELECT: ".trim(), x); }
        return r;
    }

    public boolean WO_KI_NB_JI_insertar(WO_KI_NB_JI_Calendario k) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO calendario(nombre,color) VALUES(?,?)")) {
            p.setString(1, k.WO_KI_NB_JI_getNombre()); p.setString(2, k.WO_KI_NB_JI_getColor());
            return p.executeUpdate() > 0;
        } catch (Exception x) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("CalendarioDAO", x); return false; }
    }

    public boolean WO_KI_NB_JI_actualizar(WO_KI_NB_JI_Calendario k) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE calendario SET nombre=?,color=? WHERE id=?")) {
            p.setString(1, k.WO_KI_NB_JI_getNombre()); p.setString(2, k.WO_KI_NB_JI_getColor()); p.setInt(3, k.WO_KI_NB_JI_getId());
            return p.executeUpdate() > 0;
        } catch (Exception x) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("CalendarioDAO", x); return false; }
    }

    public boolean WO_KI_NB_JI_eliminar(int id) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM calendario WHERE id=?")) {
            p.setInt(1, id);
            return p.executeUpdate() > 0;
        } catch (Exception x) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("CalendarioDAO", x); return false; }
    }
}

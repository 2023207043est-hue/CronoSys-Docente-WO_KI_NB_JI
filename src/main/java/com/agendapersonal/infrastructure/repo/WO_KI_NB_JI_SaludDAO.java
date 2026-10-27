package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.WO_KI_NB_JI_DatabaseConnection;
import com.agendapersonal.domain.model.WO_KI_NB_JI_RegistroSalud;
import java.sql.*;
import java.util.*;

public class WO_KI_NB_JI_SaludDAO {
    public boolean WO_KI_NB_JI_insertar(WO_KI_NB_JI_RegistroSalud r) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO registro_salud(tipo,valor,unidad,fecha,notas) VALUES(?,?,?,?,?)")) {
            p.setString(1, r.WO_KI_NB_JI_getTipo()); p.setDouble(2, r.WO_KI_NB_JI_getValor()); p.setString(3, r.WO_KI_NB_JI_getUnidad());
            p.setString(4, r.WO_KI_NB_JI_getFecha()); p.setString(5, r.WO_KI_NB_JI_getNotas());
            p.executeUpdate(); return true;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("SaludDAO", e); return false; }
    }
    public List<WO_KI_NB_JI_RegistroSalud> WO_KI_NB_JI_listar(String tipo, int limite) {
        List<WO_KI_NB_JI_RegistroSalud> r = new ArrayList<>();
        String q = tipo == null ? "SELECT * FROM registro_salud ORDER BY fecha DESC,id DESC LIMIT ?"
            : "SELECT * FROM registro_salud WHERE tipo=? ORDER BY fecha DESC,id DESC LIMIT ?";
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection(); PreparedStatement p = c.prepareStatement(q)) {
            if (tipo == null) p.setInt(1, limite); else { p.setString(1, tipo); p.setInt(2, limite); }
            try (ResultSet s = p.executeQuery()) {
                while (s.next()) r.add(new WO_KI_NB_JI_RegistroSalud(s.getInt("id"), s.getString("tipo"), s.getDouble("valor"),
                    s.getString("unidad"), s.getString("fecha"), s.getString("notas")));
            }
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("salud: ".trim(), e); }
        return r;
    }
    public boolean WO_KI_NB_JI_eliminar(int id) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM registro_salud WHERE id=?")) {
            p.setInt(1, id); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("SaludDAO", e); return false; }
    }
}

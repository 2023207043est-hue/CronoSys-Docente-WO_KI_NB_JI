package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.WO_KI_NB_JI_DatabaseConnection;
import com.agendapersonal.domain.model.WO_KI_NB_JI_BloqueTiempo;
import java.sql.*;
import java.util.*;

public class WO_KI_NB_JI_BloqueDAO {
    public boolean WO_KI_NB_JI_insertar(WO_KI_NB_JI_BloqueTiempo b) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO bloque_tiempo(titulo,fecha,hora_inicio,hora_fin,categoria,evento_id) VALUES(?,?,?,?,?,?)")) {
            p.setString(1, b.WO_KI_NB_JI_getTitulo()); p.setString(2, b.WO_KI_NB_JI_getFecha()); p.setString(3, com.agendapersonal.model.WO_KI_NB_JI_Horas.WO_KI_NB_JI_normalizar(b.WO_KI_NB_JI_getHoraInicio()));
            p.setString(4, com.agendapersonal.model.WO_KI_NB_JI_Horas.WO_KI_NB_JI_normalizar(b.WO_KI_NB_JI_getHoraFin())); p.setString(5, b.WO_KI_NB_JI_getCategoria());
            if (b.WO_KI_NB_JI_getEventoId() == null) p.setNull(6, Types.INTEGER); else p.setInt(6, b.WO_KI_NB_JI_getEventoId());
            p.executeUpdate(); return true;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("BloqueDAO", e); return false; }
    }
    public List<WO_KI_NB_JI_BloqueTiempo> WO_KI_NB_JI_porFecha(String fecha) {
        List<WO_KI_NB_JI_BloqueTiempo> r = new ArrayList<>();
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM bloque_tiempo WHERE fecha=? ORDER BY hora_inicio")) {
            p.setString(1, fecha);
            try (ResultSet s = p.executeQuery()) {
                while (s.next()) r.add(new WO_KI_NB_JI_BloqueTiempo(s.getInt("id"), s.getString("titulo"), s.getString("fecha"),
                    s.getString("hora_inicio"), s.getString("hora_fin"), s.getString("categoria"),
                    s.getObject("evento_id") == null ? null : s.getInt("evento_id")));
            }
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("bloque: ".trim(), e); }
        return r;
    }
    /** Bloques de hoy en adelante (para vincularlos a una tarea). */
    public List<WO_KI_NB_JI_BloqueTiempo> WO_KI_NB_JI_proximos() {
        List<WO_KI_NB_JI_BloqueTiempo> r = new ArrayList<>();
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM bloque_tiempo WHERE fecha>=? ORDER BY fecha, hora_inicio LIMIT 200")) {
            p.setString(1, java.time.LocalDate.now().toString());
            try (ResultSet s = p.executeQuery()) {
                while (s.next()) r.add(new WO_KI_NB_JI_BloqueTiempo(s.getInt("id"), s.getString("titulo"), s.getString("fecha"),
                    s.getString("hora_inicio"), s.getString("hora_fin"), s.getString("categoria"),
                    s.getObject("evento_id") == null ? null : s.getInt("evento_id")));
            }
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("bloque proximos", e); }
        return r;
    }
    public boolean WO_KI_NB_JI_eliminar(int id) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM bloque_tiempo WHERE id=?")) {
            p.setInt(1, id); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("BloqueDAO", e); return false; }
    }
}

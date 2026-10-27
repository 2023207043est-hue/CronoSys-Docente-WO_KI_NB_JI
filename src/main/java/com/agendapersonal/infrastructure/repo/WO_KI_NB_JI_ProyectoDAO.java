package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.WO_KI_NB_JI_DatabaseConnection;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Proyecto;
import java.sql.*;
import java.util.*;

public class WO_KI_NB_JI_ProyectoDAO {
    public boolean WO_KI_NB_JI_insertar(WO_KI_NB_JI_Proyecto p) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement ps = c.prepareStatement("INSERT INTO proyecto(nombre,descripcion,estado,fecha_inicio,fecha_fin,color) VALUES(?,?,?,?,?,?)")) {
            ps.setString(1, p.WO_KI_NB_JI_getNombre()); ps.setString(2, p.WO_KI_NB_JI_getDescripcion()); ps.setString(3, p.WO_KI_NB_JI_getEstado());
            ps.setString(4, p.WO_KI_NB_JI_getFechaInicio()); ps.setString(5, p.WO_KI_NB_JI_getFechaFin()); ps.setString(6, p.WO_KI_NB_JI_getColor());
            ps.executeUpdate(); return true;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("ProyectoDAO", e); return false; }
    }
    public List<WO_KI_NB_JI_Proyecto> WO_KI_NB_JI_listar() {
        List<WO_KI_NB_JI_Proyecto> r = new ArrayList<>();
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM proyecto ORDER BY id DESC");
             ResultSet s = p.executeQuery()) {
            while (s.next()) r.add(new WO_KI_NB_JI_Proyecto(s.getInt("id"), s.getString("nombre"), s.getString("descripcion"),
                s.getString("estado"), s.getString("fecha_inicio"), s.getString("fecha_fin"), s.getString("color")));
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("proyecto: ".trim(), e); }
        return r;
    }
    public boolean WO_KI_NB_JI_actualizar(WO_KI_NB_JI_Proyecto p) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE proyecto SET nombre=?,descripcion=?,estado=?,fecha_inicio=?,fecha_fin=?,color=? WHERE id=?")) {
            ps.setString(1, p.WO_KI_NB_JI_getNombre()); ps.setString(2, p.WO_KI_NB_JI_getDescripcion()); ps.setString(3, p.WO_KI_NB_JI_getEstado());
            ps.setString(4, p.WO_KI_NB_JI_getFechaInicio()); ps.setString(5, p.WO_KI_NB_JI_getFechaFin()); ps.setString(6, p.WO_KI_NB_JI_getColor());
            ps.setInt(7, p.WO_KI_NB_JI_getId()); return ps.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("ProyectoDAO", e); return false; }
    }
    /** Elimina el proyecto y deja sus tareas sin proyecto (antes quedaban apuntando a un id inexistente). */
    public boolean WO_KI_NB_JI_eliminar(int id) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement suelta = c.prepareStatement("UPDATE tarea SET proyecto_id=NULL WHERE proyecto_id=?");
             PreparedStatement p = c.prepareStatement("DELETE FROM proyecto WHERE id=?")) {
            suelta.setInt(1, id); suelta.executeUpdate();
            p.setInt(1, id); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("ProyectoDAO", e); return false; }
    }
}

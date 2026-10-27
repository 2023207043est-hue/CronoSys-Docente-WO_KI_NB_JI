package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.WO_KI_NB_JI_DatabaseConnection;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Nota;
import java.sql.*;
import java.util.*;

public class WO_KI_NB_JI_NotaDAO {
    public boolean WO_KI_NB_JI_insertar(WO_KI_NB_JI_Nota n) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO nota(titulo,contenido,etiquetas,tipo,fecha_creacion,fecha_update,favorita) VALUES(?,?,?,?,?,?,?)")) {
            p.setString(1, n.WO_KI_NB_JI_getTitulo()); p.setString(2, n.WO_KI_NB_JI_getContenido()); p.setString(3, n.WO_KI_NB_JI_getEtiquetas());
            p.setString(4, n.WO_KI_NB_JI_getTipo()); p.setString(5, n.WO_KI_NB_JI_getFechaCreacion()); p.setString(6, n.WO_KI_NB_JI_getFechaUpdate());
            p.setInt(7, n.WO_KI_NB_JI_isFavorita() ? 1 : 0); p.executeUpdate(); return true;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("NotaDAO", e); return false; }
    }
    public List<WO_KI_NB_JI_Nota> WO_KI_NB_JI_listar(String tipo) {
        List<WO_KI_NB_JI_Nota> r = new ArrayList<>();
        String q = tipo == null ? "SELECT * FROM nota ORDER BY favorita DESC,id DESC" : "SELECT * FROM nota WHERE tipo=? ORDER BY favorita DESC,id DESC";
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection(); PreparedStatement p = c.prepareStatement(q)) {
            if (tipo != null) p.setString(1, tipo);
            try (ResultSet s = p.executeQuery()) {
                while (s.next()) r.add(new WO_KI_NB_JI_Nota(s.getInt("id"), s.getString("titulo"), s.getString("contenido"),
                    s.getString("etiquetas"), s.getString("tipo"), s.getString("fecha_creacion"),
                    s.getString("fecha_update"), s.getInt("favorita") == 1));
            }
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("nota: ".trim(), e); }
        return r;
    }
    public boolean WO_KI_NB_JI_actualizar(WO_KI_NB_JI_Nota n) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE nota SET titulo=?,contenido=?,etiquetas=?,tipo=?,fecha_update=?,favorita=? WHERE id=?")) {
            p.setString(1, n.WO_KI_NB_JI_getTitulo()); p.setString(2, n.WO_KI_NB_JI_getContenido()); p.setString(3, n.WO_KI_NB_JI_getEtiquetas());
            p.setString(4, n.WO_KI_NB_JI_getTipo()); p.setString(5, n.WO_KI_NB_JI_getFechaUpdate()); p.setInt(6, n.WO_KI_NB_JI_isFavorita() ? 1 : 0);
            p.setInt(7, n.WO_KI_NB_JI_getId()); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("NotaDAO", e); return false; }
    }
    public boolean WO_KI_NB_JI_eliminar(int id) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM nota WHERE id=?")) {
            p.setInt(1, id); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("NotaDAO", e); return false; }
    }
}

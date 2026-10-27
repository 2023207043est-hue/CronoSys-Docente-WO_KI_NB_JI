package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.WO_KI_NB_JI_DatabaseConnection;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Contacto;
import java.sql.*;
import java.util.*;

public class WO_KI_NB_JI_ContactoDAO {
    public boolean WO_KI_NB_JI_insertar(WO_KI_NB_JI_Contacto x) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO contacto(nombre,telefono,email,categoria,cumpleanos,notas,favorito,ultimo_contacto,apodo,fechas) VALUES(?,?,?,?,?,?,?,?,?,?)")) {
            p.setString(1, x.WO_KI_NB_JI_getNombre()); p.setString(2, x.WO_KI_NB_JI_getTelefono()); p.setString(3, x.WO_KI_NB_JI_getEmail());
            p.setString(4, x.WO_KI_NB_JI_getCategoria()); p.setString(5, x.WO_KI_NB_JI_getCumpleanos()); p.setString(6, x.WO_KI_NB_JI_getNotas());
            p.setInt(7, x.WO_KI_NB_JI_isFavorito() ? 1 : 0); p.setString(8, x.WO_KI_NB_JI_getUltimoContacto());
            p.setString(9, x.WO_KI_NB_JI_getApodo()); p.setString(10, x.WO_KI_NB_JI_getFechas());
            p.executeUpdate(); return true;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("ContactoDAO", e); return false; }
    }
    public List<WO_KI_NB_JI_Contacto> WO_KI_NB_JI_listar() {
        List<WO_KI_NB_JI_Contacto> r = new ArrayList<>();
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM contacto ORDER BY favorito DESC,nombre");
             ResultSet s = p.executeQuery()) {
            while (s.next()) {
                WO_KI_NB_JI_Contacto x = new WO_KI_NB_JI_Contacto(s.getInt("id"), s.getString("nombre"), s.getString("telefono"),
                    s.getString("email"), s.getString("categoria"), s.getString("cumpleanos"), s.getString("notas"),
                    s.getInt("favorito") == 1, s.getString("ultimo_contacto"));
                x.WO_KI_NB_JI_setApodo(s.getString("apodo"));
                x.WO_KI_NB_JI_setFechas(s.getString("fechas"));
                r.add(x);
            }
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("contacto: ".trim(), e); }
        return r;
    }
    public boolean WO_KI_NB_JI_actualizar(WO_KI_NB_JI_Contacto x) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE contacto SET nombre=?,telefono=?,email=?,categoria=?,cumpleanos=?,notas=?,favorito=?,ultimo_contacto=?,apodo=?,fechas=? WHERE id=?")) {
            p.setString(1, x.WO_KI_NB_JI_getNombre()); p.setString(2, x.WO_KI_NB_JI_getTelefono()); p.setString(3, x.WO_KI_NB_JI_getEmail());
            p.setString(4, x.WO_KI_NB_JI_getCategoria()); p.setString(5, x.WO_KI_NB_JI_getCumpleanos()); p.setString(6, x.WO_KI_NB_JI_getNotas());
            p.setInt(7, x.WO_KI_NB_JI_isFavorito() ? 1 : 0); p.setString(8, x.WO_KI_NB_JI_getUltimoContacto());
            p.setString(9, x.WO_KI_NB_JI_getApodo()); p.setString(10, x.WO_KI_NB_JI_getFechas());
            p.setInt(11, x.WO_KI_NB_JI_getId()); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("ContactoDAO", e); return false; }
    }
    public boolean WO_KI_NB_JI_eliminar(int id) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM contacto WHERE id=?")) {
            p.setInt(1, id); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("ContactoDAO", e); return false; }
    }
}

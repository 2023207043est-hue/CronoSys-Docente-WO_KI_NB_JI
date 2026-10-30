package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.wkniDatabaseConnection;
import com.agendapersonal.domain.model.wkniContacto;
import java.sql.*;
import java.util.*;

public class wkniContactoDAO {
    public boolean wkniInsertar(wkniContacto wkniX) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("INSERT INTO contacto(nombre,telefono,email,categoria,cumpleanos,notas,favorito,ultimo_contacto,apodo,fechas) VALUES(?,?,?,?,?,?,?,?,?,?)")) {
            wkni_p.setString(1, wkniX.wkniGetNombre()); wkni_p.setString(2, wkniX.wkniGetTelefono()); wkni_p.setString(3, wkniX.wkniGetEmail());
            wkni_p.setString(4, wkniX.wkniGetCategoria()); wkni_p.setString(5, wkniX.wkniGetCumpleanos()); wkni_p.setString(6, wkniX.wkniGetNotas());
            wkni_p.setInt(7, wkniX.wkniIsFavorito() ? 1 : 0); wkni_p.setString(8, wkniX.wkniGetUltimoContacto());
            wkni_p.setString(9, wkniX.wkniGetApodo()); wkni_p.setString(10, wkniX.wkniGetFechas());
            wkni_p.executeUpdate(); return true;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("ContactoDAO", wkniE); return false; }
    }
    public List<wkniContacto> wkniListar() {
        List<wkniContacto> wkniR = new ArrayList<>();
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("SELECT * FROM contacto ORDER BY favorito DESC,nombre");
             ResultSet wkniS = wkni_p.executeQuery()) {
            while (wkniS.next()) {
                wkniContacto wkniX = new wkniContacto(wkniS.getInt("id"), wkniS.getString("nombre"), wkniS.getString("telefono"),
                    wkniS.getString("email"), wkniS.getString("categoria"), wkniS.getString("cumpleanos"), wkniS.getString("notas"),
                    wkniS.getInt("favorito") == 1, wkniS.getString("ultimo_contacto"));
                wkniX.wkniSetApodo(wkniS.getString("apodo"));
                wkniX.wkniSetFechas(wkniS.getString("fechas"));
                wkniR.add(wkniX);
            }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("contacto: ".trim(), wkniE); }
        return wkniR;
    }
    public boolean wkniActualizar(wkniContacto wkniX) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("UPDATE contacto SET nombre=?,telefono=?,email=?,categoria=?,cumpleanos=?,notas=?,favorito=?,ultimo_contacto=?,apodo=?,fechas=? WHERE id=?")) {
            wkni_p.setString(1, wkniX.wkniGetNombre()); wkni_p.setString(2, wkniX.wkniGetTelefono()); wkni_p.setString(3, wkniX.wkniGetEmail());
            wkni_p.setString(4, wkniX.wkniGetCategoria()); wkni_p.setString(5, wkniX.wkniGetCumpleanos()); wkni_p.setString(6, wkniX.wkniGetNotas());
            wkni_p.setInt(7, wkniX.wkniIsFavorito() ? 1 : 0); wkni_p.setString(8, wkniX.wkniGetUltimoContacto());
            wkni_p.setString(9, wkniX.wkniGetApodo()); wkni_p.setString(10, wkniX.wkniGetFechas());
            wkni_p.setInt(11, wkniX.wkniGetId()); return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("ContactoDAO", wkniE); return false; }
    }
    public boolean wkniEliminar(int wkniId) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("DELETE FROM contacto WHERE id=?")) {
            wkni_p.setInt(1, wkniId); return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("ContactoDAO", wkniE); return false; }
    }
}

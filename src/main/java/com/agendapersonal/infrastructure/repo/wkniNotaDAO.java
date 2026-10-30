package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.wkniDatabaseConnection;
import com.agendapersonal.domain.model.wkniNota;
import java.sql.*;
import java.util.*;

public class wkniNotaDAO {
    public boolean wkniInsertar(wkniNota wkniN) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("INSERT INTO nota(titulo,contenido,etiquetas,tipo,fecha_creacion,fecha_update,favorita) VALUES(?,?,?,?,?,?,?)")) {
            wkni_p.setString(1, wkniN.wkniGetTitulo()); wkni_p.setString(2, wkniN.wkniGetContenido()); wkni_p.setString(3, wkniN.wkniGetEtiquetas());
            wkni_p.setString(4, wkniN.wkniGetTipo()); wkni_p.setString(5, wkniN.wkniGetFechaCreacion()); wkni_p.setString(6, wkniN.wkniGetFechaUpdate());
            wkni_p.setInt(7, wkniN.wkniIsFavorita() ? 1 : 0); wkni_p.executeUpdate(); return true;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("NotaDAO", wkniE); return false; }
    }
    public List<wkniNota> wkniListar(String wkni_tipo) {
        List<wkniNota> wkniR = new ArrayList<>();
        String wkniQ = wkni_tipo == null ? "SELECT * FROM nota ORDER BY favorita DESC,id DESC" : "SELECT * FROM nota WHERE tipo=? ORDER BY favorita DESC,id DESC";
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection(); PreparedStatement wkni_p = wkniC.prepareStatement(wkniQ)) {
            if (wkni_tipo != null) wkni_p.setString(1, wkni_tipo);
            try (ResultSet wkniS = wkni_p.executeQuery()) {
                while (wkniS.next()) wkniR.add(new wkniNota(wkniS.getInt("id"), wkniS.getString("titulo"), wkniS.getString("contenido"),
                    wkniS.getString("etiquetas"), wkniS.getString("tipo"), wkniS.getString("fecha_creacion"),
                    wkniS.getString("fecha_update"), wkniS.getInt("favorita") == 1));
            }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("nota: ".trim(), wkniE); }
        return wkniR;
    }
    public boolean wkniActualizar(wkniNota wkniN) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("UPDATE nota SET titulo=?,contenido=?,etiquetas=?,tipo=?,fecha_update=?,favorita=? WHERE id=?")) {
            wkni_p.setString(1, wkniN.wkniGetTitulo()); wkni_p.setString(2, wkniN.wkniGetContenido()); wkni_p.setString(3, wkniN.wkniGetEtiquetas());
            wkni_p.setString(4, wkniN.wkniGetTipo()); wkni_p.setString(5, wkniN.wkniGetFechaUpdate()); wkni_p.setInt(6, wkniN.wkniIsFavorita() ? 1 : 0);
            wkni_p.setInt(7, wkniN.wkniGetId()); return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("NotaDAO", wkniE); return false; }
    }
    public boolean wkniEliminar(int wkniId) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("DELETE FROM nota WHERE id=?")) {
            wkni_p.setInt(1, wkniId); return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("NotaDAO", wkniE); return false; }
    }
}

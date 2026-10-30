package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.wkniDatabaseConnection;
import com.agendapersonal.domain.model.wkniRegistroSalud;
import java.sql.*;
import java.util.*;

public class wkniSaludDAO {
    public boolean wkniInsertar(wkniRegistroSalud wkniR) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("INSERT INTO registro_salud(tipo,valor,unidad,fecha,notas) VALUES(?,?,?,?,?)")) {
            wkni_p.setString(1, wkniR.wkniGetTipo()); wkni_p.setDouble(2, wkniR.wkniGetValor()); wkni_p.setString(3, wkniR.wkniGetUnidad());
            wkni_p.setString(4, wkniR.wkniGetFecha()); wkni_p.setString(5, wkniR.wkniGetNotas());
            wkni_p.executeUpdate(); return true;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("SaludDAO", wkniE); return false; }
    }
    public List<wkniRegistroSalud> wkniListar(String wkni_tipo, int wkniLimite) {
        List<wkniRegistroSalud> wkniR = new ArrayList<>();
        String wkniQ = wkni_tipo == null ? "SELECT * FROM registro_salud ORDER BY fecha DESC,id DESC LIMIT ?"
            : "SELECT * FROM registro_salud WHERE tipo=? ORDER BY fecha DESC,id DESC LIMIT ?";
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection(); PreparedStatement wkni_p = wkniC.prepareStatement(wkniQ)) {
            if (wkni_tipo == null) wkni_p.setInt(1, wkniLimite); else { wkni_p.setString(1, wkni_tipo); wkni_p.setInt(2, wkniLimite); }
            try (ResultSet wkniS = wkni_p.executeQuery()) {
                while (wkniS.next()) wkniR.add(new wkniRegistroSalud(wkniS.getInt("id"), wkniS.getString("tipo"), wkniS.getDouble("valor"),
                    wkniS.getString("unidad"), wkniS.getString("fecha"), wkniS.getString("notas")));
            }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("salud: ".trim(), wkniE); }
        return wkniR;
    }
    public boolean wkniEliminar(int wkniId) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("DELETE FROM registro_salud WHERE id=?")) {
            wkni_p.setInt(1, wkniId); return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("SaludDAO", wkniE); return false; }
    }
}

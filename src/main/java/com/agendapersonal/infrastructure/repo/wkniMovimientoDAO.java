package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.wkniDatabaseConnection;
import com.agendapersonal.domain.model.wkniMovimiento;
import java.sql.*;
import java.util.*;

public class wkniMovimientoDAO {
    /** Importe → céntimos enteros (sin errores de coma flotante). */
    static long wkniCentavos(double wkniMonto) { return Math.round(wkniMonto * 100.0); }

    public boolean wkniInsertar(wkniMovimiento wkniM) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("INSERT INTO movimiento(tipo,categoria,monto,monto_centavos,fecha,descripcion) VALUES(?,?,?,?,?,?)")) {
            wkni_p.setString(1, wkniM.wkniGetTipo()); wkni_p.setString(2, wkniM.wkniGetCategoria()); wkni_p.setDouble(3, wkniM.wkniGetMonto()); wkni_p.setLong(4, wkniCentavos(wkniM.wkniGetMonto()));
            wkni_p.setString(5, wkniM.wkniGetFecha()); wkni_p.setString(6, wkniM.wkniGetDescripcion());
            wkni_p.executeUpdate(); return true;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("MovimientoDAO", wkniE); return false; }
    }
    public List<wkniMovimiento> wkniListar(int wkniLimite) {
        List<wkniMovimiento> wkniR = new ArrayList<>();
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("SELECT * FROM movimiento ORDER BY fecha DESC,id DESC LIMIT ?")) {
            wkni_p.setInt(1, wkniLimite);
            try (ResultSet wkniS = wkni_p.executeQuery()) {
                while (wkniS.next()) wkniR.add(new wkniMovimiento(wkniS.getInt("id"), wkniS.getString("tipo"), wkniS.getString("categoria"),
                    wkniS.getLong("monto_centavos") / 100.0, wkniS.getString("fecha"), wkniS.getString("descripcion")));
            }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("mov: ".trim(), wkniE); }
        return wkniR;
    }
    /** Movimientos de un mes (formato "2026-10"), del más reciente al más antiguo. */
    public List<wkniMovimiento> wkniListarMes(String wkniYyyyMM) {
        List<wkniMovimiento> wkniR = new ArrayList<>();
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("SELECT * FROM movimiento WHERE substr(fecha,1,7)=? ORDER BY fecha DESC,id DESC")) {
            wkni_p.setString(1, wkniYyyyMM);
            try (ResultSet wkniS = wkni_p.executeQuery()) {
                while (wkniS.next()) wkniR.add(new wkniMovimiento(wkniS.getInt("id"), wkniS.getString("tipo"), wkniS.getString("categoria"),
                    wkniS.getLong("monto_centavos") / 100.0, wkniS.getString("fecha"), wkniS.getString("descripcion")));
            }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("mov mes: ".trim(), wkniE); }
        return wkniR;
    }
    public double[] wkniBalanceMes(String wkniYyyyMM) {
        double wkniIng = 0, wkniGas = 0;
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("SELECT tipo,SUM(monto_centavos) FROM movimiento WHERE substr(fecha,1,7)=? GROUP BY tipo")) {
            wkni_p.setString(1, wkniYyyyMM);
            try (ResultSet wkniS = wkni_p.executeQuery()) {
                while (wkniS.next()) {
                    if ("INGRESO".equals(wkniS.getString(1))) wkniIng = wkniS.getLong(2) / 100.0; else wkniGas = wkniS.getLong(2) / 100.0;
                }
            }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("balance: ".trim(), wkniE); }
        return new double[]{wkniIng, wkniGas, wkniIng - wkniGas};
    }
    public boolean wkniEliminar(int wkniId) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("DELETE FROM movimiento WHERE id=?")) {
            wkni_p.setInt(1, wkniId); return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("MovimientoDAO", wkniE); return false; }
    }
}

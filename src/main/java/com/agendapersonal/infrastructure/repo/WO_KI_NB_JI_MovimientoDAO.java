package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.WO_KI_NB_JI_DatabaseConnection;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Movimiento;
import java.sql.*;
import java.util.*;

public class WO_KI_NB_JI_MovimientoDAO {
    /** Importe → céntimos enteros (sin errores de coma flotante). */
    static long WO_KI_NB_JI_centavos(double monto) { return Math.round(monto * 100.0); }

    public boolean WO_KI_NB_JI_insertar(WO_KI_NB_JI_Movimiento m) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO movimiento(tipo,categoria,monto,monto_centavos,fecha,descripcion) VALUES(?,?,?,?,?,?)")) {
            p.setString(1, m.WO_KI_NB_JI_getTipo()); p.setString(2, m.WO_KI_NB_JI_getCategoria()); p.setDouble(3, m.WO_KI_NB_JI_getMonto()); p.setLong(4, WO_KI_NB_JI_centavos(m.WO_KI_NB_JI_getMonto()));
            p.setString(5, m.WO_KI_NB_JI_getFecha()); p.setString(6, m.WO_KI_NB_JI_getDescripcion());
            p.executeUpdate(); return true;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("MovimientoDAO", e); return false; }
    }
    public List<WO_KI_NB_JI_Movimiento> WO_KI_NB_JI_listar(int limite) {
        List<WO_KI_NB_JI_Movimiento> r = new ArrayList<>();
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM movimiento ORDER BY fecha DESC,id DESC LIMIT ?")) {
            p.setInt(1, limite);
            try (ResultSet s = p.executeQuery()) {
                while (s.next()) r.add(new WO_KI_NB_JI_Movimiento(s.getInt("id"), s.getString("tipo"), s.getString("categoria"),
                    s.getLong("monto_centavos") / 100.0, s.getString("fecha"), s.getString("descripcion")));
            }
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("mov: ".trim(), e); }
        return r;
    }
    /** Movimientos de un mes (formato "2026-10"), del más reciente al más antiguo. */
    public List<WO_KI_NB_JI_Movimiento> WO_KI_NB_JI_listarMes(String yyyyMM) {
        List<WO_KI_NB_JI_Movimiento> r = new ArrayList<>();
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("SELECT * FROM movimiento WHERE substr(fecha,1,7)=? ORDER BY fecha DESC,id DESC")) {
            p.setString(1, yyyyMM);
            try (ResultSet s = p.executeQuery()) {
                while (s.next()) r.add(new WO_KI_NB_JI_Movimiento(s.getInt("id"), s.getString("tipo"), s.getString("categoria"),
                    s.getLong("monto_centavos") / 100.0, s.getString("fecha"), s.getString("descripcion")));
            }
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("mov mes: ".trim(), e); }
        return r;
    }
    public double[] WO_KI_NB_JI_balanceMes(String yyyyMM) {
        double ing = 0, gas = 0;
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("SELECT tipo,SUM(monto_centavos) FROM movimiento WHERE substr(fecha,1,7)=? GROUP BY tipo")) {
            p.setString(1, yyyyMM);
            try (ResultSet s = p.executeQuery()) {
                while (s.next()) {
                    if ("INGRESO".equals(s.getString(1))) ing = s.getLong(2) / 100.0; else gas = s.getLong(2) / 100.0;
                }
            }
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("balance: ".trim(), e); }
        return new double[]{ing, gas, ing - gas};
    }
    public boolean WO_KI_NB_JI_eliminar(int id) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM movimiento WHERE id=?")) {
            p.setInt(1, id); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("MovimientoDAO", e); return false; }
    }
}

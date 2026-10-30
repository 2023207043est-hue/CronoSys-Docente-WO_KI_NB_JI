package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.wkniDatabaseConnection;
import com.agendapersonal.domain.model.wkniBloqueTiempo;
import java.sql.*;
import java.util.*;

public class wkniBloqueDAO {
    public boolean wkniInsertar(wkniBloqueTiempo wkniB) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("INSERT INTO bloque_tiempo(titulo,fecha,hora_inicio,hora_fin,categoria,evento_id) VALUES(?,?,?,?,?,?)")) {
            wkni_p.setString(1, wkniB.wkniGetTitulo()); wkni_p.setString(2, wkniB.wkniGetFecha()); wkni_p.setString(3, com.agendapersonal.model.wkniHoras.wkniNormalizar(wkniB.wkniGetHoraInicio()));
            wkni_p.setString(4, com.agendapersonal.model.wkniHoras.wkniNormalizar(wkniB.wkniGetHoraFin())); wkni_p.setString(5, wkniB.wkniGetCategoria());
            if (wkniB.wkniGetEventoId() == null) wkni_p.setNull(6, Types.INTEGER); else wkni_p.setInt(6, wkniB.wkniGetEventoId());
            wkni_p.executeUpdate(); return true;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("BloqueDAO", wkniE); return false; }
    }
    public List<wkniBloqueTiempo> wkniPorFecha(String wkni_fecha) {
        List<wkniBloqueTiempo> wkniR = new ArrayList<>();
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("SELECT * FROM bloque_tiempo WHERE fecha=? ORDER BY hora_inicio")) {
            wkni_p.setString(1, wkni_fecha);
            try (ResultSet wkniS = wkni_p.executeQuery()) {
                while (wkniS.next()) wkniR.add(new wkniBloqueTiempo(wkniS.getInt("id"), wkniS.getString("titulo"), wkniS.getString("fecha"),
                    wkniS.getString("hora_inicio"), wkniS.getString("hora_fin"), wkniS.getString("categoria"),
                    wkniS.getObject("evento_id") == null ? null : wkniS.getInt("evento_id")));
            }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("bloque: ".trim(), wkniE); }
        return wkniR;
    }
    /** Bloques de hoy en adelante (para vincularlos a una tarea). */
    public List<wkniBloqueTiempo> wkniProximos() {
        List<wkniBloqueTiempo> wkniR = new ArrayList<>();
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("SELECT * FROM bloque_tiempo WHERE fecha>=? ORDER BY fecha, hora_inicio LIMIT 200")) {
            wkni_p.setString(1, java.time.LocalDate.now().toString());
            try (ResultSet wkniS = wkni_p.executeQuery()) {
                while (wkniS.next()) wkniR.add(new wkniBloqueTiempo(wkniS.getInt("id"), wkniS.getString("titulo"), wkniS.getString("fecha"),
                    wkniS.getString("hora_inicio"), wkniS.getString("hora_fin"), wkniS.getString("categoria"),
                    wkniS.getObject("evento_id") == null ? null : wkniS.getInt("evento_id")));
            }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("bloque proximos", wkniE); }
        return wkniR;
    }
    public boolean wkniEliminar(int wkniId) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("DELETE FROM bloque_tiempo WHERE id=?")) {
            wkni_p.setInt(1, wkniId); return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("BloqueDAO", wkniE); return false; }
    }
}

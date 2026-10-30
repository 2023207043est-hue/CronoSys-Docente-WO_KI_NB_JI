package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.wkniDatabaseConnection;
import com.agendapersonal.domain.model.wkniHabito;
import java.sql.*;
import java.util.*;

public class wkniHabitoDAO {
    public boolean wkniInsertar(wkniHabito wkniH) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("INSERT INTO habito(nombre,descripcion,frecuencia,meta,color,activo) VALUES(?,?,?,?,?,?)")) {
            wkni_p.setString(1, wkniH.wkniGetNombre()); wkni_p.setString(2, wkniH.wkniGetDescripcion()); wkni_p.setString(3, wkniH.wkniGetFrecuencia());
            wkni_p.setInt(4, wkniH.wkniGetMeta()); wkni_p.setString(5, wkniH.wkniGetColor()); wkni_p.setInt(6, wkniH.wkniIsActivo() ? 1 : 0);
            wkni_p.executeUpdate(); return true;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("HabitoDAO", wkniE); return false; }
    }
    public List<wkniHabito> wkniListar() {
        List<wkniHabito> wkniR = new ArrayList<>();
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("SELECT * FROM habito ORDER BY activo DESC,nombre");
             ResultSet wkniS = wkni_p.executeQuery()) {
            while (wkniS.next()) wkniR.add(new wkniHabito(wkniS.getInt("id"), wkniS.getString("nombre"), wkniS.getString("descripcion"),
                wkniS.getString("frecuencia"), wkniS.getInt("meta"), wkniS.getString("color"), wkniS.getInt("activo") == 1));
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("habito: ".trim(), wkniE); }
        return wkniR;
    }
    public boolean wkniActualizar(wkniHabito wkniH) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("UPDATE habito SET nombre=?,descripcion=?,frecuencia=?,meta=?,color=?,activo=? WHERE id=?")) {
            wkni_p.setString(1, wkniH.wkniGetNombre()); wkni_p.setString(2, wkniH.wkniGetDescripcion()); wkni_p.setString(3, wkniH.wkniGetFrecuencia());
            wkni_p.setInt(4, wkniH.wkniGetMeta()); wkni_p.setString(5, wkniH.wkniGetColor()); wkni_p.setInt(6, wkniH.wkniIsActivo() ? 1 : 0);
            wkni_p.setInt(7, wkniH.wkniGetId()); return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("HabitoDAO", wkniE); return false; }
    }
    public boolean wkniEliminar(int wkniId) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("DELETE FROM habito WHERE id=?")) {
            wkni_p.setInt(1, wkniId); return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("HabitoDAO", wkniE); return false; }
    }
    public boolean wkniMarcar(int wkniHabitoId, String wkni_fecha) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("INSERT INTO habito_check(habito_id,fecha,cumplido) VALUES(?,?,1) ON CONFLICT(habito_id,fecha) DO UPDATE SET cumplido=1")) {
            wkni_p.setInt(1, wkniHabitoId); wkni_p.setString(2, wkni_fecha); wkni_p.executeUpdate(); return true;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("HabitoDAO", wkniE); return false; }
    }
    public boolean wkniDesmarcar(int wkniHabitoId, String wkni_fecha) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("DELETE FROM habito_check WHERE habito_id=? AND fecha=?")) {
            wkni_p.setInt(1, wkniHabitoId); wkni_p.setString(2, wkni_fecha); wkni_p.executeUpdate(); return true;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("HabitoDAO", wkniE); return false; }
    }
    public Set<Integer> wkniCumplidos(String wkni_fecha) {
        Set<Integer> wkniR = new HashSet<>();
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("SELECT habito_id FROM habito_check WHERE fecha=? AND cumplido=1")) {
            wkni_p.setString(1, wkni_fecha);
            try (ResultSet wkniS = wkni_p.executeQuery()) { while (wkniS.next()) wkniR.add(wkniS.getInt(1)); }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("check: ".trim(), wkniE); }
        return wkniR;
    }
    /** Checks cumplidos entre dos fechas ISO (inclusive): fecha → ids de hábitos cumplidos ese día. */
    public Map<String, Set<Integer>> wkniCumplidosEntre(String wkniDesde, String wkniHasta) {
        Map<String, Set<Integer>> wkniR = new HashMap<>();
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("SELECT habito_id,fecha FROM habito_check WHERE cumplido=1 AND fecha BETWEEN ? AND ?")) {
            wkni_p.setString(1, wkniDesde); wkni_p.setString(2, wkniHasta);
            try (ResultSet wkniS = wkni_p.executeQuery()) {
                while (wkniS.next()) wkniR.computeIfAbsent(wkniS.getString(2), wkniK -> new HashSet<>()).add(wkniS.getInt(1));
            }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("checks: ".trim(), wkniE); }
        return wkniR;
    }

    public int wkniRacha(int wkniHabitoId) {
        int wkniRacha = 0;
        java.time.LocalDate wkniD = java.time.LocalDate.now();
        Set<String> wkniFechas = new HashSet<>();
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("SELECT fecha FROM habito_check WHERE habito_id=? AND cumplido=1")) {
            wkni_p.setInt(1, wkniHabitoId);
            try (ResultSet wkniS = wkni_p.executeQuery()) { while (wkniS.next()) wkniFechas.add(wkniS.getString(1)); }
        } catch (Exception wkniE) { return 0; }
        // Si hoy todavía no se marcó, la racha sigue viva desde ayer (antes mostraba 0 toda la mañana).
        if (!wkniFechas.contains(wkniD.toString())) wkniD = wkniD.minusDays(1);
        while (wkniFechas.contains(wkniD.minusDays(wkniRacha).toString())) wkniRacha++;
        return wkniRacha;
    }
}

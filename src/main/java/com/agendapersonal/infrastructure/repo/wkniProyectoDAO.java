package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.wkniDatabaseConnection;
import com.agendapersonal.domain.model.wkniProyecto;
import java.sql.*;
import java.util.*;

public class wkniProyectoDAO {
    public boolean wkniInsertar(wkniProyecto wkni_p) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkniPs = wkniC.prepareStatement("INSERT INTO proyecto(nombre,descripcion,estado,fecha_inicio,fecha_fin,color) VALUES(?,?,?,?,?,?)")) {
            wkniPs.setString(1, wkni_p.wkniGetNombre()); wkniPs.setString(2, wkni_p.wkniGetDescripcion()); wkniPs.setString(3, wkni_p.wkniGetEstado());
            wkniPs.setString(4, wkni_p.wkniGetFechaInicio()); wkniPs.setString(5, wkni_p.wkniGetFechaFin()); wkniPs.setString(6, wkni_p.wkniGetColor());
            wkniPs.executeUpdate(); return true;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("ProyectoDAO", wkniE); return false; }
    }
    public List<wkniProyecto> wkniListar() {
        List<wkniProyecto> wkniR = new ArrayList<>();
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("SELECT * FROM proyecto ORDER BY id DESC");
             ResultSet wkniS = wkni_p.executeQuery()) {
            while (wkniS.next()) wkniR.add(new wkniProyecto(wkniS.getInt("id"), wkniS.getString("nombre"), wkniS.getString("descripcion"),
                wkniS.getString("estado"), wkniS.getString("fecha_inicio"), wkniS.getString("fecha_fin"), wkniS.getString("color")));
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("proyecto: ".trim(), wkniE); }
        return wkniR;
    }
    public boolean wkniActualizar(wkniProyecto wkni_p) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkniPs = wkniC.prepareStatement("UPDATE proyecto SET nombre=?,descripcion=?,estado=?,fecha_inicio=?,fecha_fin=?,color=? WHERE id=?")) {
            wkniPs.setString(1, wkni_p.wkniGetNombre()); wkniPs.setString(2, wkni_p.wkniGetDescripcion()); wkniPs.setString(3, wkni_p.wkniGetEstado());
            wkniPs.setString(4, wkni_p.wkniGetFechaInicio()); wkniPs.setString(5, wkni_p.wkniGetFechaFin()); wkniPs.setString(6, wkni_p.wkniGetColor());
            wkniPs.setInt(7, wkni_p.wkniGetId()); return wkniPs.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("ProyectoDAO", wkniE); return false; }
    }
    /** Elimina el proyecto y deja sus tareas sin proyecto (antes quedaban apuntando a un id inexistente). */
    public boolean wkniEliminar(int wkniId) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkniSuelta = wkniC.prepareStatement("UPDATE tarea SET proyecto_id=NULL WHERE proyecto_id=?");
             PreparedStatement wkni_p = wkniC.prepareStatement("DELETE FROM proyecto WHERE id=?")) {
            wkniSuelta.setInt(1, wkniId); wkniSuelta.executeUpdate();
            wkni_p.setInt(1, wkniId); return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("ProyectoDAO", wkniE); return false; }
    }
}

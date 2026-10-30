package com.agendapersonal.dao;

import com.agendapersonal.database.wkniDatabaseConnection;
import com.agendapersonal.model.wkniEstadoTarea;
import com.agendapersonal.model.wkniPrioridad;
import com.agendapersonal.model.wkniTarea;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class wkniTareaDAO {
    public boolean wkniInsertar(wkniTarea wkniT) {
        String wkniQ="INSERT INTO tarea(titulo,descripcion,estado,fecha,prioridad,proyecto_id,hora,destacada,evento_id,bloque_id) VALUES(?,?,?,?,?,?,?,?,?,?)";
        try(Connection wkniC=wkniDatabaseConnection.wkniGetConnection(); PreparedStatement wkni_p=wkniC.prepareStatement(wkniQ)) {
            wkni_p.setString(1,wkniT.wkniGetTitulo()); wkni_p.setString(2,wkniT.wkniGetDescripcion()); wkni_p.setString(3,wkniT.wkniGetEstado());
            wkni_p.setString(4,wkniT.wkniGetFecha().toString()); wkni_p.setString(5,wkniT.wkniGetPrioridad());
            if(wkniT.wkniGetProyectoId()==null) wkni_p.setNull(6,Types.INTEGER); else wkni_p.setInt(6,wkniT.wkniGetProyectoId());
            wkniExtras(wkni_p,wkniT,7);
            wkni_p.executeUpdate(); return true;
        } catch (Exception wkniX) { com.agendapersonal.util.wkniLog.wkniError("TareaDAO", wkniX); return false; }
    }

    public List<wkniTarea> wkniListarTodas() {
        List<wkniTarea> wkniR=new ArrayList<>();
        try(Connection wkniC=wkniDatabaseConnection.wkniGetConnection();
            PreparedStatement wkni_p=wkniC.prepareStatement("SELECT * FROM tarea ORDER BY fecha,id");
            ResultSet wkniS=wkni_p.executeQuery()) {
            while(wkniS.next()) wkniR.add(wkniMap(wkniS));
        } catch(SQLException wkniX){com.agendapersonal.util.wkniLog.wkniError("Tarea SELECT: ".trim(), wkniX);}
        return wkniR;
    }

    public boolean wkniActualizar(wkniTarea wkniT) {
        String wkniQ="UPDATE tarea SET titulo=?,descripcion=?,estado=?,fecha=?,prioridad=?,proyecto_id=?,hora=?,destacada=?,evento_id=?,bloque_id=? WHERE id=?";
        try(Connection wkniC=wkniDatabaseConnection.wkniGetConnection(); PreparedStatement wkni_p=wkniC.prepareStatement(wkniQ)) {
            wkni_p.setString(1,wkniT.wkniGetTitulo()); wkni_p.setString(2,wkniT.wkniGetDescripcion()); wkni_p.setString(3,wkniT.wkniGetEstado());
            wkni_p.setString(4,wkniT.wkniGetFecha().toString()); wkni_p.setString(5,wkniT.wkniGetPrioridad());
            if(wkniT.wkniGetProyectoId()==null) wkni_p.setNull(6,Types.INTEGER); else wkni_p.setInt(6,wkniT.wkniGetProyectoId());
            wkniExtras(wkni_p,wkniT,7);
            wkni_p.setInt(11,wkniT.wkniGetId());
            return wkni_p.executeUpdate()>0;
        } catch (Exception wkniX) { com.agendapersonal.util.wkniLog.wkniError("TareaDAO", wkniX); return false; }
    }

    public boolean wkniEliminar(int wkniId) {
        try(Connection wkniC=wkniDatabaseConnection.wkniGetConnection(); PreparedStatement wkni_p=wkniC.prepareStatement("DELETE FROM tarea WHERE id=?")) {
            wkni_p.setInt(1,wkniId); return wkni_p.executeUpdate()>0;
        } catch (Exception wkniX) { com.agendapersonal.util.wkniLog.wkniError("TareaDAO", wkniX); return false; }
    }

    /** Hora, destacada y vínculos (4 parámetros desde la posición dada). */
    private static void wkniExtras(PreparedStatement wkni_p, wkniTarea wkniT, int wkniI) throws SQLException {
        if(wkniT.wkniGetHora()==null) wkni_p.setNull(wkniI,Types.VARCHAR); else wkni_p.setString(wkniI,wkniT.wkniGetHora());
        wkni_p.setInt(wkniI+1,wkniT.wkniIsDestacada()?1:0);
        if(wkniT.wkniGetEventoId()==null) wkni_p.setNull(wkniI+2,Types.INTEGER); else wkni_p.setInt(wkniI+2,wkniT.wkniGetEventoId());
        if(wkniT.wkniGetBloqueId()==null) wkni_p.setNull(wkniI+3,Types.INTEGER); else wkni_p.setInt(wkniI+3,wkniT.wkniGetBloqueId());
    }

    private wkniTarea wkniMap(ResultSet wkniS)throws SQLException {
        int wkniPid;
        Integer wkniProyectoId = null;
        try { wkniPid = wkniS.getInt("proyecto_id"); if(!wkniS.wasNull()) wkniProyectoId = wkniPid; } catch(Exception wkniIgnored) {}
        wkniTarea wkniT = new wkniTarea(wkniS.getInt("id"),wkniS.getString("titulo"),wkniS.getString("descripcion"),
                null,LocalDate.parse(wkniS.getString("fecha")),null,wkniProyectoId);
        wkniT.wkniSetEstado(wkniEstadoTarea.wkniDesdeBD(wkniS.getString("estado")));        // tolerante con datos antiguos
        wkniT.wkniSetPrioridad(wkniPrioridad.wkniDesdeBD(wkniS.getString("prioridad")));
        try {
            wkniT.wkniSetHora(wkniS.getString("hora"));
            wkniT.wkniSetDestacada(wkniS.getInt("destacada")==1);
            int wkniE=wkniS.getInt("evento_id"); if(!wkniS.wasNull()) wkniT.wkniSetEventoId(wkniE);
            int wkniB=wkniS.getInt("bloque_id"); if(!wkniS.wasNull()) wkniT.wkniSetBloqueId(wkniB);
        } catch(SQLException wkniIgnored) { /* base antigua sin las columnas nuevas */ }
        return wkniT;
    }
}
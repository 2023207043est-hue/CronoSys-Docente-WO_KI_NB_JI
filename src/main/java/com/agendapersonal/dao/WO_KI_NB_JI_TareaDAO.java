package com.agendapersonal.dao;

import com.agendapersonal.database.WO_KI_NB_JI_DatabaseConnection;
import com.agendapersonal.model.WO_KI_NB_JI_EstadoTarea;
import com.agendapersonal.model.WO_KI_NB_JI_Prioridad;
import com.agendapersonal.model.WO_KI_NB_JI_Tarea;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class WO_KI_NB_JI_TareaDAO {
    public boolean WO_KI_NB_JI_insertar(WO_KI_NB_JI_Tarea t) {
        String q="INSERT INTO tarea(titulo,descripcion,estado,fecha,prioridad,proyecto_id,hora,destacada,evento_id,bloque_id) VALUES(?,?,?,?,?,?,?,?,?,?)";
        try(Connection c=WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection(); PreparedStatement p=c.prepareStatement(q)) {
            p.setString(1,t.WO_KI_NB_JI_getTitulo()); p.setString(2,t.WO_KI_NB_JI_getDescripcion()); p.setString(3,t.WO_KI_NB_JI_getEstado());
            p.setString(4,t.WO_KI_NB_JI_getFecha().toString()); p.setString(5,t.WO_KI_NB_JI_getPrioridad());
            if(t.WO_KI_NB_JI_getProyectoId()==null) p.setNull(6,Types.INTEGER); else p.setInt(6,t.WO_KI_NB_JI_getProyectoId());
            WO_KI_NB_JI_extras(p,t,7);
            p.executeUpdate(); return true;
        } catch (Exception x) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("TareaDAO", x); return false; }
    }

    public List<WO_KI_NB_JI_Tarea> WO_KI_NB_JI_listarTodas() {
        List<WO_KI_NB_JI_Tarea> r=new ArrayList<>();
        try(Connection c=WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
            PreparedStatement p=c.prepareStatement("SELECT * FROM tarea ORDER BY fecha,id");
            ResultSet s=p.executeQuery()) {
            while(s.next()) r.add(WO_KI_NB_JI_map(s));
        } catch(SQLException x){com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("Tarea SELECT: ".trim(), x);}
        return r;
    }

    public boolean WO_KI_NB_JI_actualizar(WO_KI_NB_JI_Tarea t) {
        String q="UPDATE tarea SET titulo=?,descripcion=?,estado=?,fecha=?,prioridad=?,proyecto_id=?,hora=?,destacada=?,evento_id=?,bloque_id=? WHERE id=?";
        try(Connection c=WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection(); PreparedStatement p=c.prepareStatement(q)) {
            p.setString(1,t.WO_KI_NB_JI_getTitulo()); p.setString(2,t.WO_KI_NB_JI_getDescripcion()); p.setString(3,t.WO_KI_NB_JI_getEstado());
            p.setString(4,t.WO_KI_NB_JI_getFecha().toString()); p.setString(5,t.WO_KI_NB_JI_getPrioridad());
            if(t.WO_KI_NB_JI_getProyectoId()==null) p.setNull(6,Types.INTEGER); else p.setInt(6,t.WO_KI_NB_JI_getProyectoId());
            WO_KI_NB_JI_extras(p,t,7);
            p.setInt(11,t.WO_KI_NB_JI_getId());
            return p.executeUpdate()>0;
        } catch (Exception x) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("TareaDAO", x); return false; }
    }

    public boolean WO_KI_NB_JI_eliminar(int id) {
        try(Connection c=WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection(); PreparedStatement p=c.prepareStatement("DELETE FROM tarea WHERE id=?")) {
            p.setInt(1,id); return p.executeUpdate()>0;
        } catch (Exception x) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("TareaDAO", x); return false; }
    }

    /** Hora, destacada y vínculos (4 parámetros desde la posición dada). */
    private static void WO_KI_NB_JI_extras(PreparedStatement p, WO_KI_NB_JI_Tarea t, int i) throws SQLException {
        if(t.WO_KI_NB_JI_getHora()==null) p.setNull(i,Types.VARCHAR); else p.setString(i,t.WO_KI_NB_JI_getHora());
        p.setInt(i+1,t.WO_KI_NB_JI_isDestacada()?1:0);
        if(t.WO_KI_NB_JI_getEventoId()==null) p.setNull(i+2,Types.INTEGER); else p.setInt(i+2,t.WO_KI_NB_JI_getEventoId());
        if(t.WO_KI_NB_JI_getBloqueId()==null) p.setNull(i+3,Types.INTEGER); else p.setInt(i+3,t.WO_KI_NB_JI_getBloqueId());
    }

    private WO_KI_NB_JI_Tarea WO_KI_NB_JI_map(ResultSet s)throws SQLException {
        int pid;
        Integer proyectoId = null;
        try { pid = s.getInt("proyecto_id"); if(!s.wasNull()) proyectoId = pid; } catch(Exception ignored) {}
        WO_KI_NB_JI_Tarea t = new WO_KI_NB_JI_Tarea(s.getInt("id"),s.getString("titulo"),s.getString("descripcion"),
                null,LocalDate.parse(s.getString("fecha")),null,proyectoId);
        t.WO_KI_NB_JI_setEstado(WO_KI_NB_JI_EstadoTarea.WO_KI_NB_JI_desdeBD(s.getString("estado")));        // tolerante con datos antiguos
        t.WO_KI_NB_JI_setPrioridad(WO_KI_NB_JI_Prioridad.WO_KI_NB_JI_desdeBD(s.getString("prioridad")));
        try {
            t.WO_KI_NB_JI_setHora(s.getString("hora"));
            t.WO_KI_NB_JI_setDestacada(s.getInt("destacada")==1);
            int e=s.getInt("evento_id"); if(!s.wasNull()) t.WO_KI_NB_JI_setEventoId(e);
            int b=s.getInt("bloque_id"); if(!s.wasNull()) t.WO_KI_NB_JI_setBloqueId(b);
        } catch(SQLException ignored) { /* base antigua sin las columnas nuevas */ }
        return t;
    }
}
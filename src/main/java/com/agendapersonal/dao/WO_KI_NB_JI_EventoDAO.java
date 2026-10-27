package com.agendapersonal.dao;

import com.agendapersonal.database.WO_KI_NB_JI_DatabaseConnection;
import com.agendapersonal.model.WO_KI_NB_JI_Evento;
import com.agendapersonal.model.WO_KI_NB_JI_Horas;
import com.agendapersonal.model.WO_KI_NB_JI_Prioridad;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class WO_KI_NB_JI_EventoDAO {
    public boolean WO_KI_NB_JI_insertar(WO_KI_NB_JI_Evento e) {
        String q="INSERT INTO evento(titulo,fecha,hora_inicio,hora_fin,descripcion,categoria,prioridad,calendario_id) VALUES(?,?,?,?,?,?,?,?)";
        try(Connection c=WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection(); PreparedStatement p=c.prepareStatement(q)) {
            p.setString(1,e.WO_KI_NB_JI_getTitulo()); p.setString(2,e.WO_KI_NB_JI_getFecha().toString()); p.setString(3,WO_KI_NB_JI_Horas.WO_KI_NB_JI_normalizar(e.WO_KI_NB_JI_getHoraInicio()));
            p.setString(4,WO_KI_NB_JI_Horas.WO_KI_NB_JI_normalizar(e.WO_KI_NB_JI_getHoraFin())); p.setString(5,e.WO_KI_NB_JI_getDescripcion()); p.setString(6,e.WO_KI_NB_JI_getCategoria()); p.setString(7,e.WO_KI_NB_JI_getPrioridad());
            if(e.WO_KI_NB_JI_getCalendarioId()==null) p.setNull(8,Types.INTEGER); else p.setInt(8,e.WO_KI_NB_JI_getCalendarioId());
            p.executeUpdate(); return true;
        } catch(SQLException x){com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("Evento INSERT: ".trim(), x); return false;}
    }

    public List<WO_KI_NB_JI_Evento> WO_KI_NB_JI_listarTodos() {
        List<WO_KI_NB_JI_Evento> r=new ArrayList<>();
        try(Connection c=WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
            PreparedStatement p=c.prepareStatement("SELECT * FROM evento ORDER BY fecha,hora_inicio");
            ResultSet s=p.executeQuery()) {
            while(s.next()) r.add(WO_KI_NB_JI_map(s));
        } catch(SQLException x){com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("Evento SELECT: ".trim(), x);}
        return r;
    }

    public List<WO_KI_NB_JI_Evento> WO_KI_NB_JI_listarPorFecha(LocalDate f) {
        List<WO_KI_NB_JI_Evento> r=new ArrayList<>();
        try(Connection c=WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
            PreparedStatement p=c.prepareStatement("SELECT * FROM evento WHERE fecha=? ORDER BY hora_inicio")) {
            p.setString(1,f.toString());
            try(ResultSet s=p.executeQuery()){while(s.next()) r.add(WO_KI_NB_JI_map(s));}
        } catch(SQLException x){com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("Evento fecha: ".trim(), x);}
        return r;
    }

    public boolean WO_KI_NB_JI_actualizar(WO_KI_NB_JI_Evento e) {
        String q="UPDATE evento SET titulo=?,fecha=?,hora_inicio=?,hora_fin=?,descripcion=?,categoria=?,prioridad=?,calendario_id=? WHERE id=?";
        try(Connection c=WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection(); PreparedStatement p=c.prepareStatement(q)) {
            p.setString(1,e.WO_KI_NB_JI_getTitulo()); p.setString(2,e.WO_KI_NB_JI_getFecha().toString()); p.setString(3,WO_KI_NB_JI_Horas.WO_KI_NB_JI_normalizar(e.WO_KI_NB_JI_getHoraInicio()));
            p.setString(4,WO_KI_NB_JI_Horas.WO_KI_NB_JI_normalizar(e.WO_KI_NB_JI_getHoraFin())); p.setString(5,e.WO_KI_NB_JI_getDescripcion()); p.setString(6,e.WO_KI_NB_JI_getCategoria()); p.setString(7,e.WO_KI_NB_JI_getPrioridad());
            if(e.WO_KI_NB_JI_getCalendarioId()==null) p.setNull(8,Types.INTEGER); else p.setInt(8,e.WO_KI_NB_JI_getCalendarioId());
            p.setInt(9,e.WO_KI_NB_JI_getId());
            return p.executeUpdate()>0;
        } catch (Exception x) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("EventoDAO", x); return false; }
    }

    public boolean WO_KI_NB_JI_eliminar(int id) {
        try(Connection c=WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection()) {
            c.setAutoCommit(false);
            try(PreparedStatement b=c.prepareStatement("UPDATE bloque_tiempo SET evento_id=NULL WHERE evento_id=?");
                PreparedStatement p=c.prepareStatement("DELETE FROM evento WHERE id=?")) {
                b.setInt(1,id); b.executeUpdate();
                p.setInt(1,id); boolean ok=p.executeUpdate()>0;
                c.commit(); return ok;
            } catch(SQLException x){ c.rollback(); throw x; }
        } catch(SQLException x){ com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("EventoDAO.eliminar",x); return false; }
    }

    private WO_KI_NB_JI_Evento WO_KI_NB_JI_map(ResultSet s)throws SQLException {
        Integer cal = s.getInt("calendario_id");
        if(s.wasNull()) cal = null;
        WO_KI_NB_JI_Evento ev = new WO_KI_NB_JI_Evento(s.getInt("id"),s.getString("titulo"),LocalDate.parse(s.getString("fecha")),
                s.getString("hora_inicio"),s.getString("hora_fin"),s.getString("descripcion"),
                s.getString("categoria"),null,cal);
        ev.WO_KI_NB_JI_setPrioridad(WO_KI_NB_JI_Prioridad.WO_KI_NB_JI_desdeBD(s.getString("prioridad")));  // tolerante con datos antiguos
        return ev;
    }
}
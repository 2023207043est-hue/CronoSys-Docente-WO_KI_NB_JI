package com.agendapersonal.dao;

import com.agendapersonal.database.wkniDatabaseConnection;
import com.agendapersonal.model.wkniEvento;
import com.agendapersonal.model.wkniHoras;
import com.agendapersonal.model.wkniPrioridad;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class wkniEventoDAO {
    public boolean wkniInsertar(wkniEvento wkniE) {
        String wkniQ="INSERT INTO evento(titulo,fecha,hora_inicio,hora_fin,descripcion,categoria,prioridad,calendario_id) VALUES(?,?,?,?,?,?,?,?)";
        try(Connection wkniC=wkniDatabaseConnection.wkniGetConnection(); PreparedStatement wkni_p=wkniC.prepareStatement(wkniQ)) {
            wkni_p.setString(1,wkniE.wkniGetTitulo()); wkni_p.setString(2,wkniE.wkniGetFecha().toString()); wkni_p.setString(3,wkniHoras.wkniNormalizar(wkniE.wkniGetHoraInicio()));
            wkni_p.setString(4,wkniHoras.wkniNormalizar(wkniE.wkniGetHoraFin())); wkni_p.setString(5,wkniE.wkniGetDescripcion()); wkni_p.setString(6,wkniE.wkniGetCategoria()); wkni_p.setString(7,wkniE.wkniGetPrioridad());
            if(wkniE.wkniGetCalendarioId()==null) wkni_p.setNull(8,Types.INTEGER); else wkni_p.setInt(8,wkniE.wkniGetCalendarioId());
            wkni_p.executeUpdate(); return true;
        } catch(SQLException wkniX){com.agendapersonal.util.wkniLog.wkniError("Evento INSERT: ".trim(), wkniX); return false;}
    }

    public List<wkniEvento> wkniListarTodos() {
        List<wkniEvento> wkniR=new ArrayList<>();
        try(Connection wkniC=wkniDatabaseConnection.wkniGetConnection();
            PreparedStatement wkni_p=wkniC.prepareStatement("SELECT * FROM evento ORDER BY fecha,hora_inicio");
            ResultSet wkniS=wkni_p.executeQuery()) {
            while(wkniS.next()) wkniR.add(wkniMap(wkniS));
        } catch(SQLException wkniX){com.agendapersonal.util.wkniLog.wkniError("Evento SELECT: ".trim(), wkniX);}
        return wkniR;
    }

    public List<wkniEvento> wkniListarPorFecha(LocalDate wkniF) {
        List<wkniEvento> wkniR=new ArrayList<>();
        try(Connection wkniC=wkniDatabaseConnection.wkniGetConnection();
            PreparedStatement wkni_p=wkniC.prepareStatement("SELECT * FROM evento WHERE fecha=? ORDER BY hora_inicio")) {
            wkni_p.setString(1,wkniF.toString());
            try(ResultSet wkniS=wkni_p.executeQuery()){while(wkniS.next()) wkniR.add(wkniMap(wkniS));}
        } catch(SQLException wkniX){com.agendapersonal.util.wkniLog.wkniError("Evento fecha: ".trim(), wkniX);}
        return wkniR;
    }

    public boolean wkniActualizar(wkniEvento wkniE) {
        String wkniQ="UPDATE evento SET titulo=?,fecha=?,hora_inicio=?,hora_fin=?,descripcion=?,categoria=?,prioridad=?,calendario_id=? WHERE id=?";
        try(Connection wkniC=wkniDatabaseConnection.wkniGetConnection(); PreparedStatement wkni_p=wkniC.prepareStatement(wkniQ)) {
            wkni_p.setString(1,wkniE.wkniGetTitulo()); wkni_p.setString(2,wkniE.wkniGetFecha().toString()); wkni_p.setString(3,wkniHoras.wkniNormalizar(wkniE.wkniGetHoraInicio()));
            wkni_p.setString(4,wkniHoras.wkniNormalizar(wkniE.wkniGetHoraFin())); wkni_p.setString(5,wkniE.wkniGetDescripcion()); wkni_p.setString(6,wkniE.wkniGetCategoria()); wkni_p.setString(7,wkniE.wkniGetPrioridad());
            if(wkniE.wkniGetCalendarioId()==null) wkni_p.setNull(8,Types.INTEGER); else wkni_p.setInt(8,wkniE.wkniGetCalendarioId());
            wkni_p.setInt(9,wkniE.wkniGetId());
            return wkni_p.executeUpdate()>0;
        } catch (Exception wkniX) { com.agendapersonal.util.wkniLog.wkniError("EventoDAO", wkniX); return false; }
    }

    public boolean wkniEliminar(int wkniId) {
        try(Connection wkniC=wkniDatabaseConnection.wkniGetConnection()) {
            wkniC.setAutoCommit(false);
            try(PreparedStatement wkniB=wkniC.prepareStatement("UPDATE bloque_tiempo SET evento_id=NULL WHERE evento_id=?");
                PreparedStatement wkni_p=wkniC.prepareStatement("DELETE FROM evento WHERE id=?")) {
                wkniB.setInt(1,wkniId); wkniB.executeUpdate();
                wkni_p.setInt(1,wkniId); boolean wkniOk=wkni_p.executeUpdate()>0;
                wkniC.commit(); return wkniOk;
            } catch(SQLException wkniX){ wkniC.rollback(); throw wkniX; }
        } catch(SQLException wkniX){ com.agendapersonal.util.wkniLog.wkniError("EventoDAO.eliminar",wkniX); return false; }
    }

    private wkniEvento wkniMap(ResultSet wkniS)throws SQLException {
        Integer wkniCal = wkniS.getInt("calendario_id");
        if(wkniS.wasNull()) wkniCal = null;
        wkniEvento wkniEv = new wkniEvento(wkniS.getInt("id"),wkniS.getString("titulo"),LocalDate.parse(wkniS.getString("fecha")),
                wkniS.getString("hora_inicio"),wkniS.getString("hora_fin"),wkniS.getString("descripcion"),
                wkniS.getString("categoria"),null,wkniCal);
        wkniEv.wkniSetPrioridad(wkniPrioridad.wkniDesdeBD(wkniS.getString("prioridad")));  // tolerante con datos antiguos
        return wkniEv;
    }
}
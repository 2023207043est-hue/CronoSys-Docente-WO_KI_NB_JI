package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.wkniDatabaseConnection;
import com.agendapersonal.domain.model.wkniActividadClase;
import com.agendapersonal.domain.model.wkniClaseDocente;
import com.agendapersonal.domain.model.wkniEntrega;
import com.agendapersonal.domain.model.wkniEstadoEntrega;
import com.agendapersonal.domain.model.wkniEstudianteClase;
import com.agendapersonal.domain.model.wkniRecursoClase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Acceso a cursos, listas de clase, notas, actividades, entregas, asistencia y aula virtual docente. */
public final class wkniClaseDocenteDAO {
    public List<wkniClaseDocente> wkniListarClases() {
        List<wkniClaseDocente> wkniSalida = new ArrayList<>();
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("SELECT id,nombre,grupo,periodo,nota_minima FROM clase_docente ORDER BY nombre,grupo");
             ResultSet wkniR = wkni_p.executeQuery()) {
            while (wkniR.next()) wkniSalida.add(new wkniClaseDocente(wkniR.getInt("id"), wkniR.getString("nombre"),
                wkniR.getString("grupo"), wkniR.getString("periodo"), wkniR.getDouble("nota_minima")));
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("listar clases docentes", wkniE); }
        return wkniSalida;
    }

    public int wkniCrearClase(String wkniNombre, String wkniGrupo, String wkniPeriodo) {
        return wkniCrearClase(wkniNombre, wkniGrupo, wkniPeriodo, wkniClaseDocente.wkniNOTA_MINIMA_DEFECTO);
    }

    public int wkniCrearClase(String wkniNombre, String wkniGrupo, String wkniPeriodo, double wkniNotaMinima) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement(
                 "INSERT INTO clase_docente(nombre,grupo,periodo,nota_minima) VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            wkni_p.setString(1, wkniNombre.trim()); wkni_p.setString(2, wkniLimpio(wkniGrupo)); wkni_p.setString(3, wkniLimpio(wkniPeriodo));
            wkni_p.setDouble(4, wkniNotaMinima);
            wkni_p.executeUpdate();
            try (ResultSet wkniR = wkni_p.getGeneratedKeys()) { return wkniR.next() ? wkniR.getInt(1) : 0; }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("crear clase docente", wkniE); return 0; }
    }

    public boolean wkniActualizarClase(int wkniId, String wkniNombre, String wkniGrupo, String wkniPeriodo, double wkniNotaMinima) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("UPDATE clase_docente SET nombre=?,grupo=?,periodo=?,nota_minima=? WHERE id=?")) {
            wkni_p.setString(1, wkniNombre.trim()); wkni_p.setString(2, wkniLimpio(wkniGrupo)); wkni_p.setString(3, wkniLimpio(wkniPeriodo));
            wkni_p.setDouble(4, wkniNotaMinima); wkni_p.setInt(5, wkniId);
            return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("actualizar clase docente", wkniE); return false; }
    }

    public boolean wkniEliminarClase(int wkniId) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("DELETE FROM clase_docente WHERE id=?")) {
            wkni_p.setInt(1, wkniId); return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("eliminar clase docente", wkniE); return false; }
    }

    public List<wkniEstudianteClase> wkniListarEstudiantes(int wkniClaseId) {
        List<wkniEstudianteClase> wkniSalida = new ArrayList<>();
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("""
                 SELECT id,clase_id,nombre,correo,calificacion,observaciones
                 FROM estudiante_clase WHERE clase_id=? ORDER BY nombre COLLATE NOCASE
                 """)) {
            wkni_p.setInt(1, wkniClaseId);
            try (ResultSet wkniR = wkni_p.executeQuery()) {
                while (wkniR.next()) {
                    double wkni_nota = wkniR.getDouble("calificacion");
                    Double wkniNullableNota = wkniR.wasNull() ? null : wkni_nota;
                    wkniSalida.add(new wkniEstudianteClase(wkniR.getInt("id"), wkniR.getInt("clase_id"), wkniR.getString("nombre"),
                        wkniR.getString("correo"), wkniNullableNota, wkniR.getString("observaciones")));
                }
            }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("listar estudiantes de clase", wkniE); }
        return wkniSalida;
    }

    /** Inserta o actualiza por nombre dentro del curso, evitando duplicados al reimportar el Excel. */
    public boolean wkniGuardarEstudiante(int wkniClaseId, String wkniNombre, String wkniCorreo, Double wkniCalificacion, String wkniObservaciones) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("""
                 INSERT INTO estudiante_clase(clase_id,nombre,correo,calificacion,observaciones)
                 VALUES(?,?,?,?,?)
                 ON CONFLICT(clase_id,nombre) DO UPDATE SET
                    correo=excluded.correo, calificacion=excluded.calificacion, observaciones=excluded.observaciones
                 """)) {
            wkni_p.setInt(1, wkniClaseId); wkni_p.setString(2, wkniNombre.trim()); wkni_p.setString(3, wkniLimpio(wkniCorreo));
            if (wkniCalificacion == null) wkni_p.setNull(4, java.sql.Types.REAL); else wkni_p.setDouble(4, wkniCalificacion);
            wkni_p.setString(5, wkniLimpio(wkniObservaciones));
            return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("guardar estudiante de clase", wkniE); return false; }
    }

    /** Cambia solo la calificación (edición rápida desde la tabla); null la borra. */
    public boolean wkniActualizarCalificacion(int wkniEstudianteId, Double wkniCalificacion) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("UPDATE estudiante_clase SET calificacion=? WHERE id=?")) {
            if (wkniCalificacion == null) wkni_p.setNull(1, java.sql.Types.REAL); else wkni_p.setDouble(1, wkniCalificacion);
            wkni_p.setInt(2, wkniEstudianteId);
            return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("actualizar calificación", wkniE); return false; }
    }

    public boolean wkniEliminarEstudiante(int wkniId) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("DELETE FROM estudiante_clase WHERE id=?")) {
            wkni_p.setInt(1, wkniId); return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("eliminar estudiante de clase", wkniE); return false; }
    }

    public List<wkniActividadClase> wkniListarActividades(int wkniClaseId) {
        List<wkniActividadClase> wkniSalida = new ArrayList<>();
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("""
                 SELECT id,clase_id,titulo,tipo,fecha,descripcion FROM actividad_clase
                 WHERE clase_id=? ORDER BY CASE WHEN fecha IS NULL THEN 1 ELSE 0 END,fecha,titulo
                 """)) {
            wkni_p.setInt(1, wkniClaseId);
            try (ResultSet wkniR = wkni_p.executeQuery()) {
                while (wkniR.next()) {
                    String wkni_fecha = wkniR.getString("fecha");
                    wkniSalida.add(new wkniActividadClase(wkniR.getInt("id"), wkniR.getInt("clase_id"), wkniR.getString("titulo"),
                        wkniR.getString("tipo"), wkni_fecha == null || wkni_fecha.isBlank() ? null : LocalDate.parse(wkni_fecha),
                        wkniR.getString("descripcion")));
                }
            }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("listar actividades de clase", wkniE); }
        return wkniSalida;
    }

    public boolean wkniCrearActividad(int wkniClaseId, String wkniTitulo, String wkni_tipo, LocalDate wkni_fecha, String wkniDescripcion) {
        return wkniCrearActividadId(wkniClaseId, wkniTitulo, wkni_tipo, wkni_fecha, wkniDescripcion) > 0;
    }

    /** Igual que {@link #crearActividad} pero devuelve el id generado (0 si falla). */
    public int wkniCrearActividadId(int wkniClaseId, String wkniTitulo, String wkni_tipo, LocalDate wkni_fecha, String wkniDescripcion) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("INSERT INTO actividad_clase(clase_id,titulo,tipo,fecha,descripcion) VALUES(?,?,?,?,?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            wkni_p.setInt(1, wkniClaseId); wkni_p.setString(2, wkniTitulo.trim()); wkni_p.setString(3, wkni_tipo);
            wkni_p.setString(4, wkni_fecha == null ? null : wkni_fecha.toString()); wkni_p.setString(5, wkniLimpio(wkniDescripcion));
            wkni_p.executeUpdate();
            try (ResultSet wkniR = wkni_p.getGeneratedKeys()) { return wkniR.next() ? wkniR.getInt(1) : 0; }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("crear actividad de clase", wkniE); return 0; }
    }

    public boolean wkniEliminarActividad(int wkniId) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("DELETE FROM actividad_clase WHERE id=?")) {
            wkni_p.setInt(1, wkniId); return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("eliminar actividad de clase", wkniE); return false; }
    }

    // ------------------------------------------------------------------ asistencia

    /** Estados de asistencia posibles, en el orden del ciclo al hacer clic. */
    public static final List<String> wkniESTADOS_ASISTENCIA = List.of("Presente", "Falta", "Retardo", "Justificada");

    /** Estadística de asistencia de un estudiante a lo largo del curso. */
    public record wkniEstadisticaAsistencia(int wkniSesiones, int wkniPresentes, int wkniFaltas, int wkniRetardos, int wkniJustificadas) {
        /** % de asistencia (Presente y Retardo cuentan como asistió); NaN si aún no hay registros. */
        public double wkniPorcentaje() {
            if (wkniSesiones == 0) return Double.NaN;
            return 100.0 * (wkniPresentes + wkniRetardos) / wkniSesiones;
        }
    }

    /** Estado guardado de cada estudiante en una fecha: estudianteId → estado. */
    public Map<Integer, String> wkniAsistencia(int wkniClaseId, LocalDate wkni_fecha) {
        Map<Integer, String> wkniSalida = new HashMap<>();
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("SELECT estudiante_id,estado FROM asistencia WHERE clase_id=? AND fecha=?")) {
            wkni_p.setInt(1, wkniClaseId); wkni_p.setString(2, wkni_fecha.toString());
            try (ResultSet wkniR = wkni_p.executeQuery()) { while (wkniR.next()) wkniSalida.put(wkniR.getInt("estudiante_id"), wkniR.getString("estado")); }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("listar asistencia por fecha", wkniE); }
        return wkniSalida;
    }

    /** Guarda (o corrige) la asistencia de un estudiante en una fecha; un solo registro por estudiante y día. */
    public boolean wkniGuardarAsistencia(int wkniClaseId, int wkniEstudianteId, LocalDate wkni_fecha, String wkniEstado) {
        // Acepta el valor guardado o la etiqueta visible ("Tardanza" se guarda como "Retardo").
        var wkniNormalizado = com.agendapersonal.domain.model.wkniEstadoAsistencia.wkniDe(wkniEstado);
        if (wkniNormalizado == null) return false;
        wkniEstado = wkniNormalizado.wkniValor();
        wkniUltimoError = null;
        // UPDATE y, si no había registro, INSERT: funciona aunque la tabla de una BD antigua no tenga
        // el índice único que exige "ON CONFLICT".
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection()) {
            try (PreparedStatement wkniU = wkniC.prepareStatement(
                    "UPDATE asistencia SET estado=?, clase_id=? WHERE estudiante_id=? AND fecha=?")) {
                wkniU.setString(1, wkniEstado); wkniU.setInt(2, wkniClaseId); wkniU.setInt(3, wkniEstudianteId); wkniU.setString(4, wkni_fecha.toString());
                if (wkniU.executeUpdate() > 0) return true;
            }
            try (PreparedStatement wkniI = wkniC.prepareStatement(
                    "INSERT INTO asistencia(clase_id,estudiante_id,fecha,estado) VALUES(?,?,?,?)")) {
                wkniI.setInt(1, wkniClaseId); wkniI.setInt(2, wkniEstudianteId); wkniI.setString(3, wkni_fecha.toString()); wkniI.setString(4, wkniEstado);
                return wkniI.executeUpdate() > 0;
            }
        } catch (Exception wkniE) {
            wkniUltimoError = wkniE.getMessage();
            com.agendapersonal.util.wkniLog.wkniError("guardar asistencia", wkniE);
            return false;
        }
    }

    private volatile String wkniUltimoError;
    /** Motivo técnico del último guardado de asistencia fallido (null si no hubo error). */
    public String wkniUltimoError() { return wkniUltimoError; }

    /** Quita el registro de asistencia de un estudiante en una fecha (vuelve a "sin marcar"). */
    public boolean wkniQuitarAsistencia(int wkniEstudianteId, LocalDate wkni_fecha) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("DELETE FROM asistencia WHERE estudiante_id=? AND fecha=?")) {
            wkni_p.setInt(1, wkniEstudianteId); wkni_p.setString(2, wkni_fecha.toString());
            return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("quitar asistencia", wkniE); return false; }
    }

    /** Borra la asistencia de toda la clase en una fecha ("Limpiar día"). */
    public boolean wkniQuitarAsistenciaDia(int wkniClaseId, LocalDate wkni_fecha) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("DELETE FROM asistencia WHERE clase_id=? AND fecha=?")) {
            wkni_p.setInt(1, wkniClaseId); wkni_p.setString(2, wkni_fecha.toString());
            return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("limpiar asistencia del día", wkniE); return false; }
    }

    /** Estadísticas por estudiante: estudianteId → presentes/faltas/retardos/justificadas del curso. */
    public Map<Integer, wkniEstadisticaAsistencia> wkniEstadisticasAsistencia(int wkniClaseId) {
        Map<Integer, int[]> wkniCrudos = new HashMap<>();
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement(
                 "SELECT estudiante_id,estado,count(*) FROM asistencia WHERE clase_id=? GROUP BY estudiante_id,estado")) {
            wkni_p.setInt(1, wkniClaseId);
            try (ResultSet wkniR = wkni_p.executeQuery()) {
                while (wkniR.next()) {
                    int[] wkniV = wkniCrudos.computeIfAbsent(wkniR.getInt(1), wkniK -> new int[4]);
                    switch (wkniR.getString(2)) {
                        case "Presente" -> wkniV[0]++;
                        case "Falta" -> wkniV[1]++;
                        case "Retardo" -> wkniV[2]++;
                        case "Justificada" -> wkniV[3]++;
                    }
                }
            }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("estadísticas de asistencia", wkniE); }
        Map<Integer, wkniEstadisticaAsistencia> wkniSalida = new HashMap<>();
        wkniCrudos.forEach((wkniId, wkniV) -> wkniSalida.put(wkniId, new wkniEstadisticaAsistencia(wkniV[0] + wkniV[1] + wkniV[2] + wkniV[3], wkniV[0], wkniV[1], wkniV[2], wkniV[3])));
        return wkniSalida;
    }

    /** Cantidad de fechas con asistencia registrada en el curso. */
    public int wkniSesionesAsistencia(int wkniClaseId) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("SELECT count(DISTINCT fecha) FROM asistencia WHERE clase_id=?")) {
            wkni_p.setInt(1, wkniClaseId);
            try (ResultSet wkniR = wkni_p.executeQuery()) { return wkniR.next() ? wkniR.getInt(1) : 0; }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("contar sesiones de asistencia", wkniE); return 0; }
    }

    /** Historial completo: estudianteId → (fecha → estado), en orden cronológico. */
    public Map<Integer, TreeMap<LocalDate, String>> wkniHistorialAsistencia(int wkniClaseId) {
        Map<Integer, TreeMap<LocalDate, String>> wkniSalida = new HashMap<>();
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("SELECT estudiante_id,fecha,estado FROM asistencia WHERE clase_id=?")) {
            wkni_p.setInt(1, wkniClaseId);
            try (ResultSet wkniR = wkni_p.executeQuery()) {
                while (wkniR.next()) wkniSalida.computeIfAbsent(wkniR.getInt(1), wkniK -> new TreeMap<>())
                    .put(LocalDate.parse(wkniR.getString(2)), wkniR.getString(3));
            }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("historial de asistencia", wkniE); }
        return wkniSalida;
    }

    /** Fechas con asistencia registrada en el curso, en orden. */
    public List<LocalDate> wkniFechasAsistencia(int wkniClaseId) {
        List<LocalDate> wkniSalida = new ArrayList<>();
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("SELECT DISTINCT fecha FROM asistencia WHERE clase_id=? ORDER BY fecha")) {
            wkni_p.setInt(1, wkniClaseId);
            try (ResultSet wkniR = wkni_p.executeQuery()) { while (wkniR.next()) wkniSalida.add(LocalDate.parse(wkniR.getString(1))); }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("fechas de asistencia", wkniE); }
        return wkniSalida;
    }

    // ------------------------------------------------------------------ entregas (trabajos y evaluaciones)

    /** Tipos de actividad que se califican por estudiante. */
    public static final List<String> wkniTIPOS_EVALUABLES = List.of("Tarea", "Actividad", "Evaluación");

    /** Actividades evaluables del curso (tareas, actividades y evaluaciones), por fecha. */
    public List<wkniActividadClase> wkniListarEvaluables(int wkniClaseId) {
        return wkniListarActividades(wkniClaseId).stream().filter(wkniA -> wkniTIPOS_EVALUABLES.contains(wkniA.wkni_tipo())).toList();
    }

    /** Entregas del curso: actividadId → (estudianteId → entrega). */
    public Map<Integer, Map<Integer, wkniEntrega>> wkniEntregas(int wkniClaseId) {
        Map<Integer, Map<Integer, wkniEntrega>> wkniSalida = new HashMap<>();
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("""
                 SELECT e.actividad_id,e.estudiante_id,e.estado,e.nota FROM entrega e
                 JOIN actividad_clase a ON a.id=e.actividad_id WHERE a.clase_id=?
                 """)) {
            wkni_p.setInt(1, wkniClaseId);
            try (ResultSet wkniR = wkni_p.executeQuery()) {
                while (wkniR.next()) {
                    double wkniN = wkniR.getDouble(4);
                    Double wkni_nota = wkniR.wasNull() ? null : wkniN;
                    wkniEntrega wkniEn = new wkniEntrega(wkniR.getInt(1), wkniR.getInt(2), wkniEstadoEntrega.wkniDe(wkniR.getString(3)), wkni_nota);
                    wkniSalida.computeIfAbsent(wkniEn.wkniActividadId(), wkniK -> new HashMap<>()).put(wkniEn.wkniEstudianteId(), wkniEn);
                }
            }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("listar entregas", wkniE); }
        return wkniSalida;
    }

    /** Guarda estado y nota de una entrega; sin estado ni nota se borra el registro (con nota se asume entregado). */
    public boolean wkniGuardarEntrega(int wkniActividadId, int wkniEstudianteId, wkniEstadoEntrega wkniEstado, Double wkni_nota) {
        if (wkniEstado == null && wkni_nota == null) return wkniQuitarEntrega(wkniActividadId, wkniEstudianteId);
        wkniEstadoEntrega wkniGuardar = wkniEstado == null ? wkniEstadoEntrega.ENTREGADO : wkniEstado;
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("""
                 INSERT INTO entrega(actividad_id,estudiante_id,estado,nota) VALUES(?,?,?,?)
                 ON CONFLICT(actividad_id,estudiante_id) DO UPDATE SET estado=excluded.estado, nota=excluded.nota
                 """)) {
            wkni_p.setInt(1, wkniActividadId); wkni_p.setInt(2, wkniEstudianteId); wkni_p.setString(3, wkniGuardar.wkniValor());
            if (wkni_nota == null) wkni_p.setNull(4, java.sql.Types.REAL); else wkni_p.setDouble(4, wkni_nota);
            return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("guardar entrega", wkniE); return false; }
    }

    public boolean wkniQuitarEntrega(int wkniActividadId, int wkniEstudianteId) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("DELETE FROM entrega WHERE actividad_id=? AND estudiante_id=?")) {
            wkni_p.setInt(1, wkniActividadId); wkni_p.setInt(2, wkniEstudianteId);
            wkni_p.executeUpdate();
            return true;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("quitar entrega", wkniE); return false; }
    }

    // ------------------------------------------------------------------ aula virtual

    /** Recursos del aula: fijados primero y luego los más recientes. */
    public List<wkniRecursoClase> wkniListarRecursos(int wkniClaseId) {
        List<wkniRecursoClase> wkniSalida = new ArrayList<>();
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("""
                 SELECT id,clase_id,tipo,titulo,contenido,fecha,fijado FROM recurso_clase
                 WHERE clase_id=? ORDER BY fijado DESC, fecha DESC, id DESC
                 """)) {
            wkni_p.setInt(1, wkniClaseId);
            try (ResultSet wkniR = wkni_p.executeQuery()) {
                while (wkniR.next()) wkniSalida.add(new wkniRecursoClase(wkniR.getInt(1), wkniR.getInt(2), wkniR.getString(3), wkniR.getString(4),
                    wkniR.getString(5), LocalDate.parse(wkniR.getString(6)), wkniR.getInt(7) != 0));
            }
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("listar recursos del aula", wkniE); }
        return wkniSalida;
    }

    public boolean wkniCrearRecurso(int wkniClaseId, String wkni_tipo, String wkniTitulo, String wkniContenido, LocalDate wkni_fecha, boolean wkniFijado) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement(
                 "INSERT INTO recurso_clase(clase_id,tipo,titulo,contenido,fecha,fijado) VALUES(?,?,?,?,?,?)")) {
            wkni_p.setInt(1, wkniClaseId); wkni_p.setString(2, wkni_tipo); wkni_p.setString(3, wkniTitulo.trim()); wkni_p.setString(4, wkniLimpio(wkniContenido));
            wkni_p.setString(5, (wkni_fecha == null ? LocalDate.now() : wkni_fecha).toString()); wkni_p.setInt(6, wkniFijado ? 1 : 0);
            return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("crear recurso del aula", wkniE); return false; }
    }

    public boolean wkniFijarRecurso(int wkniId, boolean wkniFijado) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("UPDATE recurso_clase SET fijado=? WHERE id=?")) {
            wkni_p.setInt(1, wkniFijado ? 1 : 0); wkni_p.setInt(2, wkniId);
            return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("fijar recurso del aula", wkniE); return false; }
    }

    public boolean wkniEliminarRecurso(int wkniId) {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection();
             PreparedStatement wkni_p = wkniC.prepareStatement("DELETE FROM recurso_clase WHERE id=?")) {
            wkni_p.setInt(1, wkniId); return wkni_p.executeUpdate() > 0;
        } catch (Exception wkniE) { com.agendapersonal.util.wkniLog.wkniError("eliminar recurso del aula", wkniE); return false; }
    }

    private static String wkniLimpio(String wkniValor) { return wkniValor == null || wkniValor.isBlank() ? null : wkniValor.trim(); }
}

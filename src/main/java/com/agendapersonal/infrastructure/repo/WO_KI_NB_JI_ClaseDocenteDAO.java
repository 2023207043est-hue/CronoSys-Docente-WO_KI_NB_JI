package com.agendapersonal.infrastructure.repo;

import com.agendapersonal.database.WO_KI_NB_JI_DatabaseConnection;
import com.agendapersonal.domain.model.WO_KI_NB_JI_ActividadClase;
import com.agendapersonal.domain.model.WO_KI_NB_JI_ClaseDocente;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Entrega;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstadoEntrega;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstudianteClase;
import com.agendapersonal.domain.model.WO_KI_NB_JI_RecursoClase;

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
public final class WO_KI_NB_JI_ClaseDocenteDAO {
    public List<WO_KI_NB_JI_ClaseDocente> WO_KI_NB_JI_listarClases() {
        List<WO_KI_NB_JI_ClaseDocente> salida = new ArrayList<>();
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("SELECT id,nombre,grupo,periodo,nota_minima FROM clase_docente ORDER BY nombre,grupo");
             ResultSet r = p.executeQuery()) {
            while (r.next()) salida.add(new WO_KI_NB_JI_ClaseDocente(r.getInt("id"), r.getString("nombre"),
                r.getString("grupo"), r.getString("periodo"), r.getDouble("nota_minima")));
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("listar clases docentes", e); }
        return salida;
    }

    public int WO_KI_NB_JI_crearClase(String nombre, String grupo, String periodo) {
        return WO_KI_NB_JI_crearClase(nombre, grupo, periodo, WO_KI_NB_JI_ClaseDocente.NOTA_MINIMA_DEFECTO);
    }

    public int WO_KI_NB_JI_crearClase(String nombre, String grupo, String periodo, double notaMinima) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement(
                 "INSERT INTO clase_docente(nombre,grupo,periodo,nota_minima) VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS)) {
            p.setString(1, nombre.trim()); p.setString(2, WO_KI_NB_JI_limpio(grupo)); p.setString(3, WO_KI_NB_JI_limpio(periodo));
            p.setDouble(4, notaMinima);
            p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) { return r.next() ? r.getInt(1) : 0; }
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("crear clase docente", e); return 0; }
    }

    public boolean WO_KI_NB_JI_actualizarClase(int id, String nombre, String grupo, String periodo, double notaMinima) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE clase_docente SET nombre=?,grupo=?,periodo=?,nota_minima=? WHERE id=?")) {
            p.setString(1, nombre.trim()); p.setString(2, WO_KI_NB_JI_limpio(grupo)); p.setString(3, WO_KI_NB_JI_limpio(periodo));
            p.setDouble(4, notaMinima); p.setInt(5, id);
            return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("actualizar clase docente", e); return false; }
    }

    public boolean WO_KI_NB_JI_eliminarClase(int id) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM clase_docente WHERE id=?")) {
            p.setInt(1, id); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("eliminar clase docente", e); return false; }
    }

    public List<WO_KI_NB_JI_EstudianteClase> WO_KI_NB_JI_listarEstudiantes(int claseId) {
        List<WO_KI_NB_JI_EstudianteClase> salida = new ArrayList<>();
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("""
                 SELECT id,clase_id,nombre,correo,calificacion,observaciones
                 FROM estudiante_clase WHERE clase_id=? ORDER BY nombre COLLATE NOCASE
                 """)) {
            p.setInt(1, claseId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    double nota = r.getDouble("calificacion");
                    Double nullableNota = r.wasNull() ? null : nota;
                    salida.add(new WO_KI_NB_JI_EstudianteClase(r.getInt("id"), r.getInt("clase_id"), r.getString("nombre"),
                        r.getString("correo"), nullableNota, r.getString("observaciones")));
                }
            }
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("listar estudiantes de clase", e); }
        return salida;
    }

    /** Inserta o actualiza por nombre dentro del curso, evitando duplicados al reimportar el Excel. */
    public boolean WO_KI_NB_JI_guardarEstudiante(int claseId, String nombre, String correo, Double calificacion, String observaciones) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("""
                 INSERT INTO estudiante_clase(clase_id,nombre,correo,calificacion,observaciones)
                 VALUES(?,?,?,?,?)
                 ON CONFLICT(clase_id,nombre) DO UPDATE SET
                    correo=excluded.correo, calificacion=excluded.calificacion, observaciones=excluded.observaciones
                 """)) {
            p.setInt(1, claseId); p.setString(2, nombre.trim()); p.setString(3, WO_KI_NB_JI_limpio(correo));
            if (calificacion == null) p.setNull(4, java.sql.Types.REAL); else p.setDouble(4, calificacion);
            p.setString(5, WO_KI_NB_JI_limpio(observaciones));
            return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("guardar estudiante de clase", e); return false; }
    }

    /** Cambia solo la calificación (edición rápida desde la tabla); null la borra. */
    public boolean WO_KI_NB_JI_actualizarCalificacion(int estudianteId, Double calificacion) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE estudiante_clase SET calificacion=? WHERE id=?")) {
            if (calificacion == null) p.setNull(1, java.sql.Types.REAL); else p.setDouble(1, calificacion);
            p.setInt(2, estudianteId);
            return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("actualizar calificación", e); return false; }
    }

    public boolean WO_KI_NB_JI_eliminarEstudiante(int id) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM estudiante_clase WHERE id=?")) {
            p.setInt(1, id); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("eliminar estudiante de clase", e); return false; }
    }

    public List<WO_KI_NB_JI_ActividadClase> WO_KI_NB_JI_listarActividades(int claseId) {
        List<WO_KI_NB_JI_ActividadClase> salida = new ArrayList<>();
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("""
                 SELECT id,clase_id,titulo,tipo,fecha,descripcion FROM actividad_clase
                 WHERE clase_id=? ORDER BY CASE WHEN fecha IS NULL THEN 1 ELSE 0 END,fecha,titulo
                 """)) {
            p.setInt(1, claseId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    String fecha = r.getString("fecha");
                    salida.add(new WO_KI_NB_JI_ActividadClase(r.getInt("id"), r.getInt("clase_id"), r.getString("titulo"),
                        r.getString("tipo"), fecha == null || fecha.isBlank() ? null : LocalDate.parse(fecha),
                        r.getString("descripcion")));
                }
            }
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("listar actividades de clase", e); }
        return salida;
    }

    public boolean WO_KI_NB_JI_crearActividad(int claseId, String titulo, String tipo, LocalDate fecha, String descripcion) {
        return WO_KI_NB_JI_crearActividadId(claseId, titulo, tipo, fecha, descripcion) > 0;
    }

    /** Igual que {@link #crearActividad} pero devuelve el id generado (0 si falla). */
    public int WO_KI_NB_JI_crearActividadId(int claseId, String titulo, String tipo, LocalDate fecha, String descripcion) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("INSERT INTO actividad_clase(clase_id,titulo,tipo,fecha,descripcion) VALUES(?,?,?,?,?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            p.setInt(1, claseId); p.setString(2, titulo.trim()); p.setString(3, tipo);
            p.setString(4, fecha == null ? null : fecha.toString()); p.setString(5, WO_KI_NB_JI_limpio(descripcion));
            p.executeUpdate();
            try (ResultSet r = p.getGeneratedKeys()) { return r.next() ? r.getInt(1) : 0; }
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("crear actividad de clase", e); return 0; }
    }

    public boolean WO_KI_NB_JI_eliminarActividad(int id) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM actividad_clase WHERE id=?")) {
            p.setInt(1, id); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("eliminar actividad de clase", e); return false; }
    }

    // ------------------------------------------------------------------ asistencia

    /** Estados de asistencia posibles, en el orden del ciclo al hacer clic. */
    public static final List<String> ESTADOS_ASISTENCIA = List.of("Presente", "Falta", "Retardo", "Justificada");

    /** Estadística de asistencia de un estudiante a lo largo del curso. */
    public record WO_KI_NB_JI_EstadisticaAsistencia(int sesiones, int presentes, int faltas, int retardos, int justificadas) {
        /** % de asistencia (Presente y Retardo cuentan como asistió); NaN si aún no hay registros. */
        public double WO_KI_NB_JI_porcentaje() {
            if (sesiones == 0) return Double.NaN;
            return 100.0 * (presentes + retardos) / sesiones;
        }
    }

    /** Estado guardado de cada estudiante en una fecha: estudianteId → estado. */
    public Map<Integer, String> WO_KI_NB_JI_asistencia(int claseId, LocalDate fecha) {
        Map<Integer, String> salida = new HashMap<>();
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("SELECT estudiante_id,estado FROM asistencia WHERE clase_id=? AND fecha=?")) {
            p.setInt(1, claseId); p.setString(2, fecha.toString());
            try (ResultSet r = p.executeQuery()) { while (r.next()) salida.put(r.getInt("estudiante_id"), r.getString("estado")); }
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("listar asistencia por fecha", e); }
        return salida;
    }

    /** Guarda (o corrige) la asistencia de un estudiante en una fecha; un solo registro por estudiante y día. */
    public boolean WO_KI_NB_JI_guardarAsistencia(int claseId, int estudianteId, LocalDate fecha, String estado) {
        // Acepta el valor guardado o la etiqueta visible ("Tardanza" se guarda como "Retardo").
        var normalizado = com.agendapersonal.domain.model.WO_KI_NB_JI_EstadoAsistencia.WO_KI_NB_JI_de(estado);
        if (normalizado == null) return false;
        estado = normalizado.WO_KI_NB_JI_valor();
        ultimoError = null;
        // UPDATE y, si no había registro, INSERT: funciona aunque la tabla de una BD antigua no tenga
        // el índice único que exige "ON CONFLICT".
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection()) {
            try (PreparedStatement u = c.prepareStatement(
                    "UPDATE asistencia SET estado=?, clase_id=? WHERE estudiante_id=? AND fecha=?")) {
                u.setString(1, estado); u.setInt(2, claseId); u.setInt(3, estudianteId); u.setString(4, fecha.toString());
                if (u.executeUpdate() > 0) return true;
            }
            try (PreparedStatement i = c.prepareStatement(
                    "INSERT INTO asistencia(clase_id,estudiante_id,fecha,estado) VALUES(?,?,?,?)")) {
                i.setInt(1, claseId); i.setInt(2, estudianteId); i.setString(3, fecha.toString()); i.setString(4, estado);
                return i.executeUpdate() > 0;
            }
        } catch (Exception e) {
            ultimoError = e.getMessage();
            com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("guardar asistencia", e);
            return false;
        }
    }

    private volatile String ultimoError;
    /** Motivo técnico del último guardado de asistencia fallido (null si no hubo error). */
    public String WO_KI_NB_JI_ultimoError() { return ultimoError; }

    /** Quita el registro de asistencia de un estudiante en una fecha (vuelve a "sin marcar"). */
    public boolean WO_KI_NB_JI_quitarAsistencia(int estudianteId, LocalDate fecha) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM asistencia WHERE estudiante_id=? AND fecha=?")) {
            p.setInt(1, estudianteId); p.setString(2, fecha.toString());
            return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("quitar asistencia", e); return false; }
    }

    /** Borra la asistencia de toda la clase en una fecha ("Limpiar día"). */
    public boolean WO_KI_NB_JI_quitarAsistenciaDia(int claseId, LocalDate fecha) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM asistencia WHERE clase_id=? AND fecha=?")) {
            p.setInt(1, claseId); p.setString(2, fecha.toString());
            return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("limpiar asistencia del día", e); return false; }
    }

    /** Estadísticas por estudiante: estudianteId → presentes/faltas/retardos/justificadas del curso. */
    public Map<Integer, WO_KI_NB_JI_EstadisticaAsistencia> WO_KI_NB_JI_estadisticasAsistencia(int claseId) {
        Map<Integer, int[]> crudos = new HashMap<>();
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement(
                 "SELECT estudiante_id,estado,count(*) FROM asistencia WHERE clase_id=? GROUP BY estudiante_id,estado")) {
            p.setInt(1, claseId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    int[] v = crudos.computeIfAbsent(r.getInt(1), k -> new int[4]);
                    switch (r.getString(2)) {
                        case "Presente" -> v[0]++;
                        case "Falta" -> v[1]++;
                        case "Retardo" -> v[2]++;
                        case "Justificada" -> v[3]++;
                    }
                }
            }
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("estadísticas de asistencia", e); }
        Map<Integer, WO_KI_NB_JI_EstadisticaAsistencia> salida = new HashMap<>();
        crudos.forEach((id, v) -> salida.put(id, new WO_KI_NB_JI_EstadisticaAsistencia(v[0] + v[1] + v[2] + v[3], v[0], v[1], v[2], v[3])));
        return salida;
    }

    /** Cantidad de fechas con asistencia registrada en el curso. */
    public int WO_KI_NB_JI_sesionesAsistencia(int claseId) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("SELECT count(DISTINCT fecha) FROM asistencia WHERE clase_id=?")) {
            p.setInt(1, claseId);
            try (ResultSet r = p.executeQuery()) { return r.next() ? r.getInt(1) : 0; }
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("contar sesiones de asistencia", e); return 0; }
    }

    /** Historial completo: estudianteId → (fecha → estado), en orden cronológico. */
    public Map<Integer, TreeMap<LocalDate, String>> WO_KI_NB_JI_historialAsistencia(int claseId) {
        Map<Integer, TreeMap<LocalDate, String>> salida = new HashMap<>();
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("SELECT estudiante_id,fecha,estado FROM asistencia WHERE clase_id=?")) {
            p.setInt(1, claseId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) salida.computeIfAbsent(r.getInt(1), k -> new TreeMap<>())
                    .put(LocalDate.parse(r.getString(2)), r.getString(3));
            }
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("historial de asistencia", e); }
        return salida;
    }

    /** Fechas con asistencia registrada en el curso, en orden. */
    public List<LocalDate> WO_KI_NB_JI_fechasAsistencia(int claseId) {
        List<LocalDate> salida = new ArrayList<>();
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("SELECT DISTINCT fecha FROM asistencia WHERE clase_id=? ORDER BY fecha")) {
            p.setInt(1, claseId);
            try (ResultSet r = p.executeQuery()) { while (r.next()) salida.add(LocalDate.parse(r.getString(1))); }
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("fechas de asistencia", e); }
        return salida;
    }

    // ------------------------------------------------------------------ entregas (trabajos y evaluaciones)

    /** Tipos de actividad que se califican por estudiante. */
    public static final List<String> TIPOS_EVALUABLES = List.of("Tarea", "Actividad", "Evaluación");

    /** Actividades evaluables del curso (tareas, actividades y evaluaciones), por fecha. */
    public List<WO_KI_NB_JI_ActividadClase> WO_KI_NB_JI_listarEvaluables(int claseId) {
        return WO_KI_NB_JI_listarActividades(claseId).stream().filter(a -> TIPOS_EVALUABLES.contains(a.tipo())).toList();
    }

    /** Entregas del curso: actividadId → (estudianteId → entrega). */
    public Map<Integer, Map<Integer, WO_KI_NB_JI_Entrega>> WO_KI_NB_JI_entregas(int claseId) {
        Map<Integer, Map<Integer, WO_KI_NB_JI_Entrega>> salida = new HashMap<>();
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("""
                 SELECT e.actividad_id,e.estudiante_id,e.estado,e.nota FROM entrega e
                 JOIN actividad_clase a ON a.id=e.actividad_id WHERE a.clase_id=?
                 """)) {
            p.setInt(1, claseId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    double n = r.getDouble(4);
                    Double nota = r.wasNull() ? null : n;
                    WO_KI_NB_JI_Entrega en = new WO_KI_NB_JI_Entrega(r.getInt(1), r.getInt(2), WO_KI_NB_JI_EstadoEntrega.WO_KI_NB_JI_de(r.getString(3)), nota);
                    salida.computeIfAbsent(en.actividadId(), k -> new HashMap<>()).put(en.estudianteId(), en);
                }
            }
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("listar entregas", e); }
        return salida;
    }

    /** Guarda estado y nota de una entrega; sin estado ni nota se borra el registro (con nota se asume entregado). */
    public boolean WO_KI_NB_JI_guardarEntrega(int actividadId, int estudianteId, WO_KI_NB_JI_EstadoEntrega estado, Double nota) {
        if (estado == null && nota == null) return WO_KI_NB_JI_quitarEntrega(actividadId, estudianteId);
        WO_KI_NB_JI_EstadoEntrega guardar = estado == null ? WO_KI_NB_JI_EstadoEntrega.ENTREGADO : estado;
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("""
                 INSERT INTO entrega(actividad_id,estudiante_id,estado,nota) VALUES(?,?,?,?)
                 ON CONFLICT(actividad_id,estudiante_id) DO UPDATE SET estado=excluded.estado, nota=excluded.nota
                 """)) {
            p.setInt(1, actividadId); p.setInt(2, estudianteId); p.setString(3, guardar.WO_KI_NB_JI_valor());
            if (nota == null) p.setNull(4, java.sql.Types.REAL); else p.setDouble(4, nota);
            return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("guardar entrega", e); return false; }
    }

    public boolean WO_KI_NB_JI_quitarEntrega(int actividadId, int estudianteId) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM entrega WHERE actividad_id=? AND estudiante_id=?")) {
            p.setInt(1, actividadId); p.setInt(2, estudianteId);
            p.executeUpdate();
            return true;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("quitar entrega", e); return false; }
    }

    // ------------------------------------------------------------------ aula virtual

    /** Recursos del aula: fijados primero y luego los más recientes. */
    public List<WO_KI_NB_JI_RecursoClase> WO_KI_NB_JI_listarRecursos(int claseId) {
        List<WO_KI_NB_JI_RecursoClase> salida = new ArrayList<>();
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("""
                 SELECT id,clase_id,tipo,titulo,contenido,fecha,fijado FROM recurso_clase
                 WHERE clase_id=? ORDER BY fijado DESC, fecha DESC, id DESC
                 """)) {
            p.setInt(1, claseId);
            try (ResultSet r = p.executeQuery()) {
                while (r.next()) salida.add(new WO_KI_NB_JI_RecursoClase(r.getInt(1), r.getInt(2), r.getString(3), r.getString(4),
                    r.getString(5), LocalDate.parse(r.getString(6)), r.getInt(7) != 0));
            }
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("listar recursos del aula", e); }
        return salida;
    }

    public boolean WO_KI_NB_JI_crearRecurso(int claseId, String tipo, String titulo, String contenido, LocalDate fecha, boolean fijado) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement(
                 "INSERT INTO recurso_clase(clase_id,tipo,titulo,contenido,fecha,fijado) VALUES(?,?,?,?,?,?)")) {
            p.setInt(1, claseId); p.setString(2, tipo); p.setString(3, titulo.trim()); p.setString(4, WO_KI_NB_JI_limpio(contenido));
            p.setString(5, (fecha == null ? LocalDate.now() : fecha).toString()); p.setInt(6, fijado ? 1 : 0);
            return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("crear recurso del aula", e); return false; }
    }

    public boolean WO_KI_NB_JI_fijarRecurso(int id, boolean fijado) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("UPDATE recurso_clase SET fijado=? WHERE id=?")) {
            p.setInt(1, fijado ? 1 : 0); p.setInt(2, id);
            return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("fijar recurso del aula", e); return false; }
    }

    public boolean WO_KI_NB_JI_eliminarRecurso(int id) {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection();
             PreparedStatement p = c.prepareStatement("DELETE FROM recurso_clase WHERE id=?")) {
            p.setInt(1, id); return p.executeUpdate() > 0;
        } catch (Exception e) { com.agendapersonal.util.WO_KI_NB_JI_Log.WO_KI_NB_JI_error("eliminar recurso del aula", e); return false; }
    }

    private static String WO_KI_NB_JI_limpio(String valor) { return valor == null || valor.isBlank() ? null : valor.trim(); }
}

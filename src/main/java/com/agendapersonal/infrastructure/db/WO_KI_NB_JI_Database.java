package com.agendapersonal.infrastructure.db;

import com.agendapersonal.database.WO_KI_NB_JI_DatabaseConnection;
import com.agendapersonal.util.WO_KI_NB_JI_Log;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Migración central del esquema con versionado ({@code PRAGMA user_version}).
 * Cada paso se aplica una sola vez, dentro de una transacción. Para cambiar el esquema se AÑADE un paso
 * al final de {@link #PASOS}; nunca se edita uno ya publicado.
 */
public final class WO_KI_NB_JI_Database {
    @FunctionalInterface private interface WO_KI_NB_JI_Paso { void WO_KI_NB_JI_aplicar(Statement s) throws SQLException; }

    private static final String[] DDL_BASE = {
            """
            CREATE TABLE IF NOT EXISTS evento (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                titulo TEXT NOT NULL, fecha TEXT NOT NULL,
                hora_inicio TEXT NOT NULL, hora_fin TEXT NOT NULL,
                descripcion TEXT, categoria TEXT NOT NULL, prioridad TEXT NOT NULL)
            """,
            """
            CREATE TABLE IF NOT EXISTS tarea (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                titulo TEXT NOT NULL, descripcion TEXT,
                estado TEXT NOT NULL, fecha TEXT NOT NULL, prioridad TEXT NOT NULL,
                proyecto_id INTEGER REFERENCES proyecto(id))
            """,
            """
            CREATE TABLE IF NOT EXISTS proyecto (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL, descripcion TEXT, estado TEXT NOT NULL DEFAULT 'Activo',
                fecha_inicio TEXT, fecha_fin TEXT, color TEXT DEFAULT '#2d6cdf')
            """,
            """
            CREATE TABLE IF NOT EXISTS nota (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                titulo TEXT NOT NULL, contenido TEXT, etiquetas TEXT,
                tipo TEXT NOT NULL DEFAULT 'NOTA',
                fecha_creacion TEXT NOT NULL, fecha_update TEXT,
                favorita INTEGER NOT NULL DEFAULT 0)
            """,
            """
            CREATE TABLE IF NOT EXISTS contacto (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL, telefono TEXT, email TEXT,
                categoria TEXT DEFAULT 'Personal', cumpleanos TEXT,
                notas TEXT, favorito INTEGER NOT NULL DEFAULT 0,
                ultimo_contacto TEXT)
            """,
            """
            CREATE TABLE IF NOT EXISTS habito (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL, descripcion TEXT,
                frecuencia TEXT NOT NULL DEFAULT 'DIARIO',
                meta INTEGER NOT NULL DEFAULT 1, color TEXT DEFAULT '#16a34a',
                activo INTEGER NOT NULL DEFAULT 1)
            """,
            """
            CREATE TABLE IF NOT EXISTS habito_check (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                habito_id INTEGER NOT NULL REFERENCES habito(id) ON DELETE CASCADE,
                fecha TEXT NOT NULL, cumplido INTEGER NOT NULL DEFAULT 1,
                UNIQUE(habito_id, fecha))
            """,
            """
            CREATE TABLE IF NOT EXISTS movimiento (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tipo TEXT NOT NULL, categoria TEXT NOT NULL,
                monto REAL NOT NULL, fecha TEXT NOT NULL, descripcion TEXT)
            """,
            """
            CREATE TABLE IF NOT EXISTS registro_salud (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                tipo TEXT NOT NULL, valor REAL NOT NULL, unidad TEXT,
                fecha TEXT NOT NULL, notas TEXT)
            """,
            """
            CREATE TABLE IF NOT EXISTS bloque_tiempo (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                titulo TEXT NOT NULL, fecha TEXT NOT NULL,
                hora_inicio TEXT NOT NULL, hora_fin TEXT NOT NULL,
                categoria TEXT DEFAULT 'Enfoque', evento_id INTEGER REFERENCES evento(id))
            """,
            """
            CREATE TABLE IF NOT EXISTS calendario (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                nombre TEXT NOT NULL, color TEXT NOT NULL DEFAULT '#F67280')
            """,
            """
            CREATE TABLE IF NOT EXISTS config (
                clave TEXT PRIMARY KEY, valor TEXT NOT NULL)
            """
    };

    /** El índice + 1 de cada paso es la versión del esquema que deja la BD. */
    private static final List<WO_KI_NB_JI_Paso> PASOS = List.of(
        // 1: esquema original (compatible con BD creadas por versiones anteriores)
        s -> {
            for (String q : DDL_BASE) s.execute(q);
            WO_KI_NB_JI_agregarColumna(s, "tarea", "proyecto_id", "INTEGER REFERENCES proyecto(id)");
            WO_KI_NB_JI_agregarColumna(s, "evento", "calendario_id", "INTEGER REFERENCES calendario(id)");
            s.execute("INSERT OR IGNORE INTO calendario(id,nombre,color) VALUES(1,'General','#6C5B7B')");
            s.execute("INSERT OR IGNORE INTO calendario(id,nombre,color) VALUES(2,'Personal','#F67280')");
            s.execute("INSERT OR IGNORE INTO calendario(id,nombre,color) VALUES(3,'Trabajo','#355C7D')");
        },
        // 2: horas "9:00" → "09:00" (el orden por texto fallaba) y limpieza de referencias huérfanas
        s -> {
            for (String t : new String[]{"evento", "bloque_tiempo"})
                for (String col : new String[]{"hora_inicio", "hora_fin"})
                    s.execute("UPDATE " + t + " SET " + col + "='0'||" + col + " WHERE " + col + " GLOB '[0-9]:[0-9][0-9]'");
            s.execute("DELETE FROM habito_check WHERE habito_id NOT IN (SELECT id FROM habito)");
            s.execute("UPDATE tarea SET proyecto_id=NULL WHERE proyecto_id IS NOT NULL AND proyecto_id NOT IN (SELECT id FROM proyecto)");
            s.execute("UPDATE evento SET calendario_id=NULL WHERE calendario_id IS NOT NULL AND calendario_id NOT IN (SELECT id FROM calendario)");
            s.execute("UPDATE bloque_tiempo SET evento_id=NULL WHERE evento_id IS NOT NULL AND evento_id NOT IN (SELECT id FROM evento)");
        },
        // 3: dinero en céntimos enteros (REAL acumula errores de redondeo)
        s -> {
            WO_KI_NB_JI_agregarColumna(s, "movimiento", "monto_centavos", "INTEGER NOT NULL DEFAULT 0");
            s.execute("UPDATE movimiento SET monto_centavos=CAST(ROUND(monto*100) AS INTEGER)");
        },
        // 4: tareas con hora opcional, destacadas y vínculos; contactos con apodo y fechas importantes extra
        s -> {
            WO_KI_NB_JI_agregarColumna(s, "tarea", "hora", "TEXT");
            WO_KI_NB_JI_agregarColumna(s, "tarea", "destacada", "INTEGER NOT NULL DEFAULT 0");
            WO_KI_NB_JI_agregarColumna(s, "tarea", "evento_id", "INTEGER");
            WO_KI_NB_JI_agregarColumna(s, "tarea", "bloque_id", "INTEGER");
            WO_KI_NB_JI_agregarColumna(s, "contacto", "apodo", "TEXT");
            WO_KI_NB_JI_agregarColumna(s, "contacto", "fechas", "TEXT");
        },
        // 5: cursos, estudiantes, calificaciones y planeación propia del espacio docente
        s -> {
            s.execute("""
                CREATE TABLE IF NOT EXISTS clase_docente (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    grupo TEXT,
                    periodo TEXT,
                    creada_en TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
            s.execute("""
                CREATE TABLE IF NOT EXISTS estudiante_clase (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    clase_id INTEGER NOT NULL REFERENCES clase_docente(id) ON DELETE CASCADE,
                    nombre TEXT NOT NULL COLLATE NOCASE,
                    correo TEXT,
                    calificacion REAL,
                    observaciones TEXT,
                    UNIQUE(clase_id, nombre)
                )
                """);
            s.execute("""
                CREATE TABLE IF NOT EXISTS actividad_clase (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    clase_id INTEGER NOT NULL REFERENCES clase_docente(id) ON DELETE CASCADE,
                    titulo TEXT NOT NULL,
                    tipo TEXT NOT NULL DEFAULT 'Clase',
                    fecha TEXT,
                    descripcion TEXT
                )
                """);
            s.execute("CREATE INDEX IF NOT EXISTS idx_estudiante_clase ON estudiante_clase(clase_id, nombre)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_actividad_clase_fecha ON actividad_clase(clase_id, fecha)");
        },
        // 6: registro de asistencia por estudiante y fecha (marca con un clic en la pestaña Asistencia)
        s -> {
            s.execute("""
                CREATE TABLE IF NOT EXISTS asistencia (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    clase_id INTEGER NOT NULL REFERENCES clase_docente(id) ON DELETE CASCADE,
                    estudiante_id INTEGER NOT NULL REFERENCES estudiante_clase(id) ON DELETE CASCADE,
                    fecha TEXT NOT NULL,
                    estado TEXT NOT NULL DEFAULT 'Presente',
                    UNIQUE(estudiante_id, fecha)
                )
                """);
            s.execute("CREATE INDEX IF NOT EXISTS idx_asistencia_clase_fecha ON asistencia(clase_id, fecha)");
        },
        // 7: entregas de trabajos por estudiante, aula virtual (anuncios y recursos) y nota mínima por curso
        s -> {
            WO_KI_NB_JI_agregarColumna(s, "clase_docente", "nota_minima", "REAL NOT NULL DEFAULT 11");
            s.execute("""
                CREATE TABLE IF NOT EXISTS entrega (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    actividad_id INTEGER NOT NULL REFERENCES actividad_clase(id) ON DELETE CASCADE,
                    estudiante_id INTEGER NOT NULL REFERENCES estudiante_clase(id) ON DELETE CASCADE,
                    estado TEXT NOT NULL DEFAULT 'Entregado',
                    nota REAL,
                    UNIQUE(actividad_id, estudiante_id)
                )
                """);
            s.execute("""
                CREATE TABLE IF NOT EXISTS recurso_clase (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    clase_id INTEGER NOT NULL REFERENCES clase_docente(id) ON DELETE CASCADE,
                    tipo TEXT NOT NULL,
                    titulo TEXT NOT NULL,
                    contenido TEXT,
                    fecha TEXT NOT NULL,
                    fijado INTEGER NOT NULL DEFAULT 0
                )
                """);
            s.execute("CREATE INDEX IF NOT EXISTS idx_entrega_actividad ON entrega(actividad_id)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_recurso_clase ON recurso_clase(clase_id)");
        },
        // 8: repara 'asistencia' si una BD antigua no tiene UNIQUE(estudiante_id, fecha) (un registro por alumno y día)
        s -> {
            String ddl = null;
            try (ResultSet r = s.executeQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='asistencia'")) {
                if (r.next()) ddl = r.getString(1);
            }
            if (ddl == null) return;
            if (ddl.toLowerCase().replaceAll("\\s+", "").contains("unique(estudiante_id,fecha)")) return;
            s.execute("ALTER TABLE asistencia RENAME TO asistencia_old");
            s.execute("""
                CREATE TABLE asistencia (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    clase_id INTEGER NOT NULL REFERENCES clase_docente(id) ON DELETE CASCADE,
                    estudiante_id INTEGER NOT NULL REFERENCES estudiante_clase(id) ON DELETE CASCADE,
                    fecha TEXT NOT NULL,
                    estado TEXT NOT NULL DEFAULT 'Presente',
                    UNIQUE(estudiante_id, fecha)
                )
                """);
            s.execute("INSERT OR REPLACE INTO asistencia(clase_id,estudiante_id,fecha,estado) "
                + "SELECT clase_id,estudiante_id,fecha,estado FROM asistencia_old ORDER BY id");
            s.execute("DROP TABLE asistencia_old");
            s.execute("DROP INDEX IF EXISTS idx_asistencia_clase_fecha");
            s.execute("CREATE INDEX idx_asistencia_clase_fecha ON asistencia(clase_id, fecha)");
        }
    );

    private WO_KI_NB_JI_Database() {}

    /** Borra todos los registros y preferencias personales y restaura la aplicación al estado inicial. */
    public static void WO_KI_NB_JI_borrarTodosLosDatos() {
        String[] tablas = {"habito_check", "tarea", "bloque_tiempo", "evento", "proyecto", "nota",
            "contacto", "habito", "movimiento", "registro_salud", "entrega", "recurso_clase", "asistencia", "actividad_clase",
            "estudiante_clase", "clase_docente", "calendario", "config"};
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection(); Statement s = c.createStatement()) {
            c.setAutoCommit(false);
            try {
                for (String tabla : tablas) s.executeUpdate("DELETE FROM " + tabla);
                s.executeUpdate("DELETE FROM sqlite_sequence");
                s.execute("INSERT INTO calendario(id,nombre,color) VALUES(1,'General','#6C5B7B'),(2,'Personal','#F67280'),(3,'Trabajo','#355C7D')");
                c.commit();
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            } finally { c.setAutoCommit(true); }
        } catch (Exception e) {
            WO_KI_NB_JI_Log.WO_KI_NB_JI_error("No se pudieron borrar los datos de Compás", e);
            throw new IllegalStateException("No se pudieron borrar los datos: " + e.getMessage(), e);
        }
    }

    public static void WO_KI_NB_JI_migrate() {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection(); Statement s = c.createStatement()) {
            int actual = WO_KI_NB_JI_versionDe(s);
            for (int v = actual; v < PASOS.size(); v++) {
                c.setAutoCommit(false);
                try {
                    PASOS.get(v).WO_KI_NB_JI_aplicar(s);
                    s.execute("PRAGMA user_version=" + (v + 1));
                    c.commit();
                    WO_KI_NB_JI_Log.WO_KI_NB_JI_info("Esquema de la base de datos actualizado a la versión " + (v + 1));
                } catch (SQLException | RuntimeException e) {
                    c.rollback();
                    throw e;
                } finally { c.setAutoCommit(true); }
            }
        } catch (Exception e) {
            WO_KI_NB_JI_Log.WO_KI_NB_JI_error("No se pudo preparar la base de datos (" + WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_dbPath() + ")", e);
            throw new IllegalStateException("No se pudo preparar la base de datos: " + e.getMessage(), e);
        }
    }

    /** Versión actual del esquema (0 = BD nueva). */
    public static int WO_KI_NB_JI_version() {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection(); Statement s = c.createStatement()) { return WO_KI_NB_JI_versionDe(s); }
        catch (SQLException e) { WO_KI_NB_JI_Log.WO_KI_NB_JI_error("version BD", e); return -1; }
    }

    public static int WO_KI_NB_JI_versionObjetivo() { return PASOS.size(); }

    private static int WO_KI_NB_JI_versionDe(Statement s) throws SQLException {
        try (ResultSet r = s.executeQuery("PRAGMA user_version")) { return r.next() ? r.getInt(1) : 0; }
    }

    /** ALTER TABLE ADD COLUMN solo si la columna no existe (antes se ignoraba cualquier error). */
    static void WO_KI_NB_JI_agregarColumna(Statement s, String tabla, String columna, String definicion) throws SQLException {
        try (ResultSet r = s.executeQuery("PRAGMA table_info(" + tabla + ")")) {
            while (r.next()) if (columna.equalsIgnoreCase(r.getString("name"))) return;
        }
        s.execute("ALTER TABLE " + tabla + " ADD COLUMN " + columna + " " + definicion);
    }
}

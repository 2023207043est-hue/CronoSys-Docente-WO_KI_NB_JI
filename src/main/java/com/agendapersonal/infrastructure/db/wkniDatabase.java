package com.agendapersonal.infrastructure.db;

import com.agendapersonal.database.wkniDatabaseConnection;
import com.agendapersonal.util.wkniLog;
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
public final class wkniDatabase {
    @FunctionalInterface private interface wkniPaso { void wkniAplicar(Statement wkniS) throws SQLException; }

    private static final String[] wkniDDL_BASE = {
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
    private static final List<wkniPaso> wkniPASOS = List.of(
        // 1: esquema original (compatible con BD creadas por versiones anteriores)
        wkniS -> {
            for (String wkniQ : wkniDDL_BASE) wkniS.execute(wkniQ);
            wkniAgregarColumna(wkniS, "tarea", "proyecto_id", "INTEGER REFERENCES proyecto(id)");
            wkniAgregarColumna(wkniS, "evento", "calendario_id", "INTEGER REFERENCES calendario(id)");
            wkniS.execute("INSERT OR IGNORE INTO calendario(id,nombre,color) VALUES(1,'General','#6C5B7B')");
            wkniS.execute("INSERT OR IGNORE INTO calendario(id,nombre,color) VALUES(2,'Personal','#F67280')");
            wkniS.execute("INSERT OR IGNORE INTO calendario(id,nombre,color) VALUES(3,'Trabajo','#355C7D')");
        },
        // 2: horas "9:00" → "09:00" (el orden por texto fallaba) y limpieza de referencias huérfanas
        wkniS -> {
            for (String wkniT : new String[]{"evento", "bloque_tiempo"})
                for (String wkniCol : new String[]{"hora_inicio", "hora_fin"})
                    wkniS.execute("UPDATE " + wkniT + " SET " + wkniCol + "='0'||" + wkniCol + " WHERE " + wkniCol + " GLOB '[0-9]:[0-9][0-9]'");
            wkniS.execute("DELETE FROM habito_check WHERE habito_id NOT IN (SELECT id FROM habito)");
            wkniS.execute("UPDATE tarea SET proyecto_id=NULL WHERE proyecto_id IS NOT NULL AND proyecto_id NOT IN (SELECT id FROM proyecto)");
            wkniS.execute("UPDATE evento SET calendario_id=NULL WHERE calendario_id IS NOT NULL AND calendario_id NOT IN (SELECT id FROM calendario)");
            wkniS.execute("UPDATE bloque_tiempo SET evento_id=NULL WHERE evento_id IS NOT NULL AND evento_id NOT IN (SELECT id FROM evento)");
        },
        // 3: dinero en céntimos enteros (REAL acumula errores de redondeo)
        wkniS -> {
            wkniAgregarColumna(wkniS, "movimiento", "monto_centavos", "INTEGER NOT NULL DEFAULT 0");
            wkniS.execute("UPDATE movimiento SET monto_centavos=CAST(ROUND(monto*100) AS INTEGER)");
        },
        // 4: tareas con hora opcional, destacadas y vínculos; contactos con apodo y fechas importantes extra
        wkniS -> {
            wkniAgregarColumna(wkniS, "tarea", "hora", "TEXT");
            wkniAgregarColumna(wkniS, "tarea", "destacada", "INTEGER NOT NULL DEFAULT 0");
            wkniAgregarColumna(wkniS, "tarea", "evento_id", "INTEGER");
            wkniAgregarColumna(wkniS, "tarea", "bloque_id", "INTEGER");
            wkniAgregarColumna(wkniS, "contacto", "apodo", "TEXT");
            wkniAgregarColumna(wkniS, "contacto", "fechas", "TEXT");
        },
        // 5: cursos, estudiantes, calificaciones y planeación propia del espacio docente
        wkniS -> {
            wkniS.execute("""
                CREATE TABLE IF NOT EXISTS clase_docente (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nombre TEXT NOT NULL,
                    grupo TEXT,
                    periodo TEXT,
                    creada_en TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP
                )
                """);
            wkniS.execute("""
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
            wkniS.execute("""
                CREATE TABLE IF NOT EXISTS actividad_clase (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    clase_id INTEGER NOT NULL REFERENCES clase_docente(id) ON DELETE CASCADE,
                    titulo TEXT NOT NULL,
                    tipo TEXT NOT NULL DEFAULT 'Clase',
                    fecha TEXT,
                    descripcion TEXT
                )
                """);
            wkniS.execute("CREATE INDEX IF NOT EXISTS idx_estudiante_clase ON estudiante_clase(clase_id, nombre)");
            wkniS.execute("CREATE INDEX IF NOT EXISTS idx_actividad_clase_fecha ON actividad_clase(clase_id, fecha)");
        },
        // 6: registro de asistencia por estudiante y fecha (marca con un clic en la pestaña Asistencia)
        wkniS -> {
            wkniS.execute("""
                CREATE TABLE IF NOT EXISTS asistencia (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    clase_id INTEGER NOT NULL REFERENCES clase_docente(id) ON DELETE CASCADE,
                    estudiante_id INTEGER NOT NULL REFERENCES estudiante_clase(id) ON DELETE CASCADE,
                    fecha TEXT NOT NULL,
                    estado TEXT NOT NULL DEFAULT 'Presente',
                    UNIQUE(estudiante_id, fecha)
                )
                """);
            wkniS.execute("CREATE INDEX IF NOT EXISTS idx_asistencia_clase_fecha ON asistencia(clase_id, fecha)");
        },
        // 7: entregas de trabajos por estudiante, aula virtual (anuncios y recursos) y nota mínima por curso
        wkniS -> {
            wkniAgregarColumna(wkniS, "clase_docente", "nota_minima", "REAL NOT NULL DEFAULT 11");
            wkniS.execute("""
                CREATE TABLE IF NOT EXISTS entrega (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    actividad_id INTEGER NOT NULL REFERENCES actividad_clase(id) ON DELETE CASCADE,
                    estudiante_id INTEGER NOT NULL REFERENCES estudiante_clase(id) ON DELETE CASCADE,
                    estado TEXT NOT NULL DEFAULT 'Entregado',
                    nota REAL,
                    UNIQUE(actividad_id, estudiante_id)
                )
                """);
            wkniS.execute("""
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
            wkniS.execute("CREATE INDEX IF NOT EXISTS idx_entrega_actividad ON entrega(actividad_id)");
            wkniS.execute("CREATE INDEX IF NOT EXISTS idx_recurso_clase ON recurso_clase(clase_id)");
        },
        // 8: repara 'asistencia' si una BD antigua no tiene UNIQUE(estudiante_id, fecha) (un registro por alumno y día)
        wkniS -> {
            String wkniDdl = null;
            try (ResultSet wkniR = wkniS.executeQuery("SELECT sql FROM sqlite_master WHERE type='table' AND name='asistencia'")) {
                if (wkniR.next()) wkniDdl = wkniR.getString(1);
            }
            if (wkniDdl == null) return;
            if (wkniDdl.toLowerCase().replaceAll("\\s+", "").contains("unique(estudiante_id,fecha)")) return;
            wkniS.execute("ALTER TABLE asistencia RENAME TO asistencia_old");
            wkniS.execute("""
                CREATE TABLE asistencia (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    clase_id INTEGER NOT NULL REFERENCES clase_docente(id) ON DELETE CASCADE,
                    estudiante_id INTEGER NOT NULL REFERENCES estudiante_clase(id) ON DELETE CASCADE,
                    fecha TEXT NOT NULL,
                    estado TEXT NOT NULL DEFAULT 'Presente',
                    UNIQUE(estudiante_id, fecha)
                )
                """);
            wkniS.execute("INSERT OR REPLACE INTO asistencia(clase_id,estudiante_id,fecha,estado) "
                + "SELECT clase_id,estudiante_id,fecha,estado FROM asistencia_old ORDER BY id");
            wkniS.execute("DROP TABLE asistencia_old");
            wkniS.execute("DROP INDEX IF EXISTS idx_asistencia_clase_fecha");
            wkniS.execute("CREATE INDEX idx_asistencia_clase_fecha ON asistencia(clase_id, fecha)");
        }
    );

    private wkniDatabase() {}

    /** Borra todos los registros y preferencias personales y restaura la aplicación al estado inicial. */
    public static void wkniBorrarTodosLosDatos() {
        String[] wkniTablas = {"habito_check", "tarea", "bloque_tiempo", "evento", "proyecto", "nota",
            "contacto", "habito", "movimiento", "registro_salud", "entrega", "recurso_clase", "asistencia", "actividad_clase",
            "estudiante_clase", "clase_docente", "calendario", "config"};
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection(); Statement wkniS = wkniC.createStatement()) {
            wkniC.setAutoCommit(false);
            try {
                for (String wkniTabla : wkniTablas) wkniS.executeUpdate("DELETE FROM " + wkniTabla);
                wkniS.executeUpdate("DELETE FROM sqlite_sequence");
                wkniS.execute("INSERT INTO calendario(id,nombre,color) VALUES(1,'General','#6C5B7B'),(2,'Personal','#F67280'),(3,'Trabajo','#355C7D')");
                wkniC.commit();
            } catch (SQLException | RuntimeException wkniE) {
                wkniC.rollback();
                throw wkniE;
            } finally { wkniC.setAutoCommit(true); }
        } catch (Exception wkniE) {
            wkniLog.wkniError("No se pudieron borrar los datos de Compás", wkniE);
            throw new IllegalStateException("No se pudieron borrar los datos: " + wkniE.getMessage(), wkniE);
        }
    }

    public static void wkniMigrate() {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection(); Statement wkniS = wkniC.createStatement()) {
            int wkniActual = wkniVersionDe(wkniS);
            for (int wkniV = wkniActual; wkniV < wkniPASOS.size(); wkniV++) {
                wkniC.setAutoCommit(false);
                try {
                    wkniPASOS.get(wkniV).wkniAplicar(wkniS);
                    wkniS.execute("PRAGMA user_version=" + (wkniV + 1));
                    wkniC.commit();
                    wkniLog.wkniInfo("Esquema de la base de datos actualizado a la versión " + (wkniV + 1));
                } catch (SQLException | RuntimeException wkniE) {
                    wkniC.rollback();
                    throw wkniE;
                } finally { wkniC.setAutoCommit(true); }
            }
        } catch (Exception wkniE) {
            wkniLog.wkniError("No se pudo preparar la base de datos (" + wkniDatabaseConnection.wkniDbPath() + ")", wkniE);
            throw new IllegalStateException("No se pudo preparar la base de datos: " + wkniE.getMessage(), wkniE);
        }
    }

    /** Versión actual del esquema (0 = BD nueva). */
    public static int wkniVersion() {
        try (Connection wkniC = wkniDatabaseConnection.wkniGetConnection(); Statement wkniS = wkniC.createStatement()) { return wkniVersionDe(wkniS); }
        catch (SQLException wkniE) { wkniLog.wkniError("version BD", wkniE); return -1; }
    }

    public static int wkniVersionObjetivo() { return wkniPASOS.size(); }

    private static int wkniVersionDe(Statement wkniS) throws SQLException {
        try (ResultSet wkniR = wkniS.executeQuery("PRAGMA user_version")) { return wkniR.next() ? wkniR.getInt(1) : 0; }
    }

    /** ALTER TABLE ADD COLUMN solo si la columna no existe (antes se ignoraba cualquier error). */
    static void wkniAgregarColumna(Statement wkniS, String wkniTabla, String wkniColumna, String wkniDefinicion) throws SQLException {
        try (ResultSet wkniR = wkniS.executeQuery("PRAGMA table_info(" + wkniTabla + ")")) {
            while (wkniR.next()) if (wkniColumna.equalsIgnoreCase(wkniR.getString("name"))) return;
        }
        wkniS.execute("ALTER TABLE " + wkniTabla + " ADD COLUMN " + wkniColumna + " " + wkniDefinicion);
    }
}

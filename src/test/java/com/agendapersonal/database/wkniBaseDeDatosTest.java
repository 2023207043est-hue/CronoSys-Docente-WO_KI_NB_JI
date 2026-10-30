package com.agendapersonal.database;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.dao.wkniEventoDAO;
import com.agendapersonal.domain.model.wkniMovimiento;
import com.agendapersonal.infrastructure.db.wkniDatabase;
import com.agendapersonal.infrastructure.repo.wkniHabitoDAO;
import com.agendapersonal.infrastructure.repo.wkniMovimientoDAO;
import com.agendapersonal.model.wkniEvento;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Pruebas de integración con una BD temporal (nunca toca la BD real del usuario). */
class wkniBaseDeDatosTest {

    private static Path wkniNuevaBd() throws Exception {
        Path dir = Files.createTempDirectory("cronosys-test");
        Path db = dir.resolve("prueba.db");
        System.setProperty(wkniDatabaseConnection.wkniPROP_RUTA, db.toString());
        return db;
    }

    private static int wkniContar(String sql) throws Exception {
        try (Connection c = wkniDatabaseConnection.wkniGetConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) { r.next(); return r.getInt(1); }
    }

    @Test void wkniMigraEsquemaNuevoYEsIdempotente() throws Exception {
        wkniNuevaBd();
        wkniDatabase.wkniMigrate();
        assertEquals(wkniDatabase.wkniVersionObjetivo(), wkniDatabase.wkniVersion());
        wkniDatabase.wkniMigrate();                      // segunda vez: no cambia nada ni falla
        assertEquals(wkniDatabase.wkniVersionObjetivo(), wkniDatabase.wkniVersion());
    }

    @Test void wkniClavesForaneasActivasYBorradoEnCascada() throws Exception {
        wkniNuevaBd(); wkniDatabase.wkniMigrate();
        wkniHabitoDAO h = new wkniHabitoDAO();
        try (Connection c = wkniDatabaseConnection.wkniGetConnection(); Statement s = c.createStatement()) {
            s.execute("INSERT INTO habito(nombre) VALUES('Leer')");
            s.execute("INSERT INTO habito_check(habito_id,fecha) VALUES(1,'2026-10-19')");
        }
        assertTrue(h.wkniEliminar(1));
        assertEquals(0, wkniContar("SELECT count(*) FROM habito_check"));   // antes quedaban huérfanos
    }

    @Test void wkniRechazaReferenciaInexistente() throws Exception {
        wkniNuevaBd(); wkniDatabase.wkniMigrate();
        try (Connection c = wkniDatabaseConnection.wkniGetConnection(); Statement s = c.createStatement()) {
            assertThrows(java.sql.SQLException.class,
                () -> s.execute("INSERT INTO tarea(titulo,estado,fecha,prioridad,proyecto_id) VALUES('x','Pendiente','2026-10-19','Media',999)"));
        }
    }

    @Test void wkniNormalizaHorasAlGuardarYOrdenaBien() throws Exception {
        wkniNuevaBd(); wkniDatabase.wkniMigrate();
        wkniEventoDAO dao = new wkniEventoDAO();
        LocalDate d = LocalDate.of(2026, 10, 19);
        assertTrue(dao.wkniInsertar(new wkniEvento("Tarde", d, "10:00", "11:00", "", "Personal", "Media")));
        assertTrue(dao.wkniInsertar(new wkniEvento("Mañana", d, "9:00", "9:30", "", "Personal", "Media")));
        var l = dao.wkniListarPorFecha(d);
        assertEquals("Mañana", l.get(0).wkniGetTitulo());      // "9:00" antes de "10:00"
        assertEquals("09:00", l.get(0).wkniGetHoraInicio());
    }

    @Test void wkniMigracionNormalizaHorasViejas() throws Exception {
        Path db = wkniNuevaBd();
        // BD "vieja" creada por la versión anterior: sin user_version y con horas sin cero
        try (Connection c = wkniDatabaseConnection.wkniGetConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE evento(id INTEGER PRIMARY KEY AUTOINCREMENT,titulo TEXT NOT NULL,fecha TEXT NOT NULL,hora_inicio TEXT NOT NULL,hora_fin TEXT NOT NULL,descripcion TEXT,categoria TEXT NOT NULL,prioridad TEXT NOT NULL)");
            s.execute("INSERT INTO evento(titulo,fecha,hora_inicio,hora_fin,categoria,prioridad) VALUES('a','2026-10-19','9:00','9:30','x','Media')");
        }
        wkniDatabase.wkniMigrate();
        try (Connection c = wkniDatabaseConnection.wkniGetConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT hora_inicio FROM evento")) {
            r.next(); assertEquals("09:00", r.getString(1));
        }
        assertEquals(wkniDatabase.wkniVersionObjetivo(), wkniDatabase.wkniVersion());
    }

    @Test void wkniEliminarEventoDesvinculaBloques() throws Exception {
        wkniNuevaBd(); wkniDatabase.wkniMigrate();
        try (Connection c = wkniDatabaseConnection.wkniGetConnection(); Statement s = c.createStatement()) {
            s.execute("INSERT INTO evento(titulo,fecha,hora_inicio,hora_fin,categoria,prioridad) VALUES('e','2026-10-19','09:00','10:00','x','Media')");
            s.execute("INSERT INTO bloque_tiempo(titulo,fecha,hora_inicio,hora_fin,evento_id) VALUES('b','2026-10-19','09:00','10:00',1)");
        }
        assertTrue(new wkniEventoDAO().wkniEliminar(1));
        assertEquals(1, wkniContar("SELECT count(*) FROM bloque_tiempo WHERE evento_id IS NULL"));
    }

    @Test void wkniDineroSeGuardaEnCentavosSinErrorDeRedondeo() throws Exception {
        wkniNuevaBd(); wkniDatabase.wkniMigrate();
        wkniMovimientoDAO dao = new wkniMovimientoDAO();
        for (int i = 0; i < 10; i++) assertTrue(dao.wkniInsertar(new wkniMovimiento(0, "GASTO", "Comida", 0.1, "2026-10-19", "")));
        assertEquals(100, wkniContar("SELECT SUM(monto_centavos) FROM movimiento"));   // 10 × 10 céntimos
        assertEquals(1.0, dao.wkniBalanceMes("2026-10")[1], 1e-9);
        assertEquals(0.1, dao.wkniListarMes("2026-10").get(0).wkniGetMonto(), 1e-9);
    }
}

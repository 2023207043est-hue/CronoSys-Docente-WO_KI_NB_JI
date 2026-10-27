package com.agendapersonal.database;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.dao.WO_KI_NB_JI_EventoDAO;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Movimiento;
import com.agendapersonal.infrastructure.db.WO_KI_NB_JI_Database;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_HabitoDAO;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_MovimientoDAO;
import com.agendapersonal.model.WO_KI_NB_JI_Evento;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Pruebas de integración con una BD temporal (nunca toca la BD real del usuario). */
class WO_KI_NB_JI_BaseDeDatosTest {

    private static Path WO_KI_NB_JI_nuevaBd() throws Exception {
        Path dir = Files.createTempDirectory("cronosys-test");
        Path db = dir.resolve("prueba.db");
        System.setProperty(WO_KI_NB_JI_DatabaseConnection.PROP_RUTA, db.toString());
        return db;
    }

    private static int WO_KI_NB_JI_contar(String sql) throws Exception {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) { r.next(); return r.getInt(1); }
    }

    @Test void WO_KI_NB_JI_migraEsquemaNuevoYEsIdempotente() throws Exception {
        WO_KI_NB_JI_nuevaBd();
        WO_KI_NB_JI_Database.WO_KI_NB_JI_migrate();
        assertEquals(WO_KI_NB_JI_Database.WO_KI_NB_JI_versionObjetivo(), WO_KI_NB_JI_Database.WO_KI_NB_JI_version());
        WO_KI_NB_JI_Database.WO_KI_NB_JI_migrate();                      // segunda vez: no cambia nada ni falla
        assertEquals(WO_KI_NB_JI_Database.WO_KI_NB_JI_versionObjetivo(), WO_KI_NB_JI_Database.WO_KI_NB_JI_version());
    }

    @Test void WO_KI_NB_JI_clavesForaneasActivasYBorradoEnCascada() throws Exception {
        WO_KI_NB_JI_nuevaBd(); WO_KI_NB_JI_Database.WO_KI_NB_JI_migrate();
        WO_KI_NB_JI_HabitoDAO h = new WO_KI_NB_JI_HabitoDAO();
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection(); Statement s = c.createStatement()) {
            s.execute("INSERT INTO habito(nombre) VALUES('Leer')");
            s.execute("INSERT INTO habito_check(habito_id,fecha) VALUES(1,'2026-10-19')");
        }
        assertTrue(h.WO_KI_NB_JI_eliminar(1));
        assertEquals(0, WO_KI_NB_JI_contar("SELECT count(*) FROM habito_check"));   // antes quedaban huérfanos
    }

    @Test void WO_KI_NB_JI_rechazaReferenciaInexistente() throws Exception {
        WO_KI_NB_JI_nuevaBd(); WO_KI_NB_JI_Database.WO_KI_NB_JI_migrate();
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection(); Statement s = c.createStatement()) {
            assertThrows(java.sql.SQLException.class,
                () -> s.execute("INSERT INTO tarea(titulo,estado,fecha,prioridad,proyecto_id) VALUES('x','Pendiente','2026-10-19','Media',999)"));
        }
    }

    @Test void WO_KI_NB_JI_normalizaHorasAlGuardarYOrdenaBien() throws Exception {
        WO_KI_NB_JI_nuevaBd(); WO_KI_NB_JI_Database.WO_KI_NB_JI_migrate();
        WO_KI_NB_JI_EventoDAO dao = new WO_KI_NB_JI_EventoDAO();
        LocalDate d = LocalDate.of(2026, 10, 19);
        assertTrue(dao.WO_KI_NB_JI_insertar(new WO_KI_NB_JI_Evento("Tarde", d, "10:00", "11:00", "", "Personal", "Media")));
        assertTrue(dao.WO_KI_NB_JI_insertar(new WO_KI_NB_JI_Evento("Mañana", d, "9:00", "9:30", "", "Personal", "Media")));
        var l = dao.WO_KI_NB_JI_listarPorFecha(d);
        assertEquals("Mañana", l.get(0).WO_KI_NB_JI_getTitulo());      // "9:00" antes de "10:00"
        assertEquals("09:00", l.get(0).WO_KI_NB_JI_getHoraInicio());
    }

    @Test void WO_KI_NB_JI_migracionNormalizaHorasViejas() throws Exception {
        Path db = WO_KI_NB_JI_nuevaBd();
        // BD "vieja" creada por la versión anterior: sin user_version y con horas sin cero
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection(); Statement s = c.createStatement()) {
            s.execute("CREATE TABLE evento(id INTEGER PRIMARY KEY AUTOINCREMENT,titulo TEXT NOT NULL,fecha TEXT NOT NULL,hora_inicio TEXT NOT NULL,hora_fin TEXT NOT NULL,descripcion TEXT,categoria TEXT NOT NULL,prioridad TEXT NOT NULL)");
            s.execute("INSERT INTO evento(titulo,fecha,hora_inicio,hora_fin,categoria,prioridad) VALUES('a','2026-10-19','9:00','9:30','x','Media')");
        }
        WO_KI_NB_JI_Database.WO_KI_NB_JI_migrate();
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery("SELECT hora_inicio FROM evento")) {
            r.next(); assertEquals("09:00", r.getString(1));
        }
        assertEquals(WO_KI_NB_JI_Database.WO_KI_NB_JI_versionObjetivo(), WO_KI_NB_JI_Database.WO_KI_NB_JI_version());
    }

    @Test void WO_KI_NB_JI_eliminarEventoDesvinculaBloques() throws Exception {
        WO_KI_NB_JI_nuevaBd(); WO_KI_NB_JI_Database.WO_KI_NB_JI_migrate();
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection(); Statement s = c.createStatement()) {
            s.execute("INSERT INTO evento(titulo,fecha,hora_inicio,hora_fin,categoria,prioridad) VALUES('e','2026-10-19','09:00','10:00','x','Media')");
            s.execute("INSERT INTO bloque_tiempo(titulo,fecha,hora_inicio,hora_fin,evento_id) VALUES('b','2026-10-19','09:00','10:00',1)");
        }
        assertTrue(new WO_KI_NB_JI_EventoDAO().WO_KI_NB_JI_eliminar(1));
        assertEquals(1, WO_KI_NB_JI_contar("SELECT count(*) FROM bloque_tiempo WHERE evento_id IS NULL"));
    }

    @Test void WO_KI_NB_JI_dineroSeGuardaEnCentavosSinErrorDeRedondeo() throws Exception {
        WO_KI_NB_JI_nuevaBd(); WO_KI_NB_JI_Database.WO_KI_NB_JI_migrate();
        WO_KI_NB_JI_MovimientoDAO dao = new WO_KI_NB_JI_MovimientoDAO();
        for (int i = 0; i < 10; i++) assertTrue(dao.WO_KI_NB_JI_insertar(new WO_KI_NB_JI_Movimiento(0, "GASTO", "Comida", 0.1, "2026-10-19", "")));
        assertEquals(100, WO_KI_NB_JI_contar("SELECT SUM(monto_centavos) FROM movimiento"));   // 10 × 10 céntimos
        assertEquals(1.0, dao.WO_KI_NB_JI_balanceMes("2026-10")[1], 1e-9);
        assertEquals(0.1, dao.WO_KI_NB_JI_listarMes("2026-10").get(0).WO_KI_NB_JI_getMonto(), 1e-9);
    }
}

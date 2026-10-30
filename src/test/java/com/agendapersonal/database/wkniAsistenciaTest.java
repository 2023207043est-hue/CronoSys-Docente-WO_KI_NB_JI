package com.agendapersonal.database;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.infrastructure.db.wkniDatabase;
import com.agendapersonal.infrastructure.repo.wkniClaseDocenteDAO;
import com.agendapersonal.domain.model.wkniEstudianteClase;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Registro de asistencia del Espacio Docente con una BD temporal (nunca toca la BD real). */
class wkniAsistenciaTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 26);
    private static final LocalDate MARTES = LocalDate.of(2026, 10, 27);

    private static void wkniNuevaBd() throws Exception {
        Path dir = Files.createTempDirectory("cronosys-test");
        System.setProperty(wkniDatabaseConnection.wkniPROP_RUTA, dir.resolve("prueba.db").toString());
    }

    private static int wkniContar(String sql) throws Exception {
        try (Connection c = wkniDatabaseConnection.wkniGetConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) { r.next(); return r.getInt(1); }
    }

    /** Crea un curso con dos estudiantes y devuelve {claseId, estudiante1Id, estudiante2Id}. */
    private static int[] wkniCursoConEstudiantes(wkniClaseDocenteDAO dao) {
        int claseId = dao.wkniCrearClase("Matemáticas", "A", "2026-I");
        assertTrue(dao.wkniGuardarEstudiante(claseId, "Ana Pérez", null, null, null));
        assertTrue(dao.wkniGuardarEstudiante(claseId, "Luis Gómez", null, null, null));
        List<wkniEstudianteClase> estudiantes = dao.wkniListarEstudiantes(claseId);
        assertEquals(2, estudiantes.size());
        return new int[]{claseId, estudiantes.get(0).wkniId(), estudiantes.get(1).wkniId()};
    }

    @Test void wkniMigraTablaDeAsistenciaYEsIdempotente() throws Exception {
        wkniNuevaBd();
        wkniDatabase.wkniMigrate();
        assertEquals(wkniDatabase.wkniVersionObjetivo(), wkniDatabase.wkniVersion());
        assertTrue(wkniDatabase.wkniVersionObjetivo() >= 6);     // v6 = asistencia (v7 = entregas y aula virtual)
        assertEquals(0, wkniContar("SELECT count(*) FROM asistencia"));
        wkniDatabase.wkniMigrate();
        assertEquals(wkniDatabase.wkniVersionObjetivo(), wkniDatabase.wkniVersion());
    }

    @Test void wkniGuardaUnSoloRegistroPorEstudianteYFecha() throws Exception {
        wkniNuevaBd(); wkniDatabase.wkniMigrate();
        wkniClaseDocenteDAO dao = new wkniClaseDocenteDAO();
        int[] c = wkniCursoConEstudiantes(dao);

        assertTrue(dao.wkniGuardarAsistencia(c[0], c[1], LUNES, "Presente"));
        assertEquals(Map.of(c[1], "Presente"), dao.wkniAsistencia(c[0], LUNES));

        // Corregir el estado no debe duplicar el registro del día.
        assertTrue(dao.wkniGuardarAsistencia(c[0], c[1], LUNES, "Falta"));
        assertEquals(Map.of(c[1], "Falta"), dao.wkniAsistencia(c[0], LUNES));
        assertEquals(1, wkniContar("SELECT count(*) FROM asistencia WHERE estudiante_id=" + c[1]));
        assertTrue(dao.wkniAsistencia(c[0], MARTES).isEmpty());
    }

    @Test void wkniRechazaEstadoInvalido() throws Exception {
        wkniNuevaBd(); wkniDatabase.wkniMigrate();
        wkniClaseDocenteDAO dao = new wkniClaseDocenteDAO();
        int[] c = wkniCursoConEstudiantes(dao);
        assertFalse(dao.wkniGuardarAsistencia(c[0], c[1], LUNES, "Volando"));
        assertTrue(dao.wkniAsistencia(c[0], LUNES).isEmpty());
    }

    @Test void wkniPorcentajeCuentaPresenteYRetardoComoAsistio() throws Exception {
        wkniNuevaBd(); wkniDatabase.wkniMigrate();
        wkniClaseDocenteDAO dao = new wkniClaseDocenteDAO();
        int[] c = wkniCursoConEstudiantes(dao);

        // Ana: presente y falta → 50 %. Luis: presente y retardo → 100 %.
        dao.wkniGuardarAsistencia(c[0], c[1], LUNES, "Presente");
        dao.wkniGuardarAsistencia(c[0], c[2], LUNES, "Presente");
        dao.wkniGuardarAsistencia(c[0], c[1], MARTES, "Falta");
        dao.wkniGuardarAsistencia(c[0], c[2], MARTES, "Retardo");

        Map<Integer, wkniClaseDocenteDAO.wkniEstadisticaAsistencia> stats = dao.wkniEstadisticasAsistencia(c[0]);
        wkniClaseDocenteDAO.wkniEstadisticaAsistencia ana = stats.get(c[1]);
        assertEquals(2, ana.wkniSesiones());
        assertEquals(1, ana.wkniPresentes());
        assertEquals(1, ana.wkniFaltas());
        assertEquals(50.0, ana.wkniPorcentaje(), 0.001);

        wkniClaseDocenteDAO.wkniEstadisticaAsistencia luis = stats.get(c[2]);
        assertEquals(1, luis.wkniRetardos());
        assertEquals(100.0, luis.wkniPorcentaje(), 0.001);

        assertEquals(2, dao.wkniSesionesAsistencia(c[0]));
        assertTrue(Double.isNaN(new wkniClaseDocenteDAO.wkniEstadisticaAsistencia(0, 0, 0, 0, 0).wkniPorcentaje()));
    }

    @Test void wkniLimpiarDiaYBorradoEnCascada() throws Exception {
        wkniNuevaBd(); wkniDatabase.wkniMigrate();
        wkniClaseDocenteDAO dao = new wkniClaseDocenteDAO();
        int[] c = wkniCursoConEstudiantes(dao);
        dao.wkniGuardarAsistencia(c[0], c[1], LUNES, "Presente");
        dao.wkniGuardarAsistencia(c[0], c[2], LUNES, "Falta");

        assertTrue(dao.wkniQuitarAsistenciaDia(c[0], LUNES));
        assertTrue(dao.wkniAsistencia(c[0], LUNES).isEmpty());

        dao.wkniGuardarAsistencia(c[0], c[1], LUNES, "Presente");
        assertTrue(dao.wkniQuitarAsistencia(c[1], LUNES));
        assertTrue(dao.wkniAsistencia(c[0], LUNES).isEmpty());

        // Al eliminar el estudiante o el curso, sus registros de asistencia desaparecen con ellos.
        dao.wkniGuardarAsistencia(c[0], c[1], LUNES, "Presente");
        assertTrue(dao.wkniEliminarEstudiante(c[1]));
        assertEquals(0, wkniContar("SELECT count(*) FROM asistencia"));
    }

    @Test void wkniBorrarTodosLosDatosTambienLimpiaAsistencia() throws Exception {
        wkniNuevaBd(); wkniDatabase.wkniMigrate();
        wkniClaseDocenteDAO dao = new wkniClaseDocenteDAO();
        int[] c = wkniCursoConEstudiantes(dao);
        dao.wkniGuardarAsistencia(c[0], c[1], LUNES, "Presente");
        assertEquals(1, wkniContar("SELECT count(*) FROM asistencia"));

        wkniDatabase.wkniBorrarTodosLosDatos();
        assertEquals(0, wkniContar("SELECT count(*) FROM asistencia"));
        assertEquals(0, wkniContar("SELECT count(*) FROM estudiante_clase"));
    }
}

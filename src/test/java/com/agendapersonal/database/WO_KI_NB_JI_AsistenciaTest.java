package com.agendapersonal.database;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.infrastructure.db.WO_KI_NB_JI_Database;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_ClaseDocenteDAO;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstudianteClase;
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
class WO_KI_NB_JI_AsistenciaTest {

    private static final LocalDate LUNES = LocalDate.of(2026, 10, 26);
    private static final LocalDate MARTES = LocalDate.of(2026, 10, 27);

    private static void WO_KI_NB_JI_nuevaBd() throws Exception {
        Path dir = Files.createTempDirectory("cronosys-test");
        System.setProperty(WO_KI_NB_JI_DatabaseConnection.PROP_RUTA, dir.resolve("prueba.db").toString());
    }

    private static int WO_KI_NB_JI_contar(String sql) throws Exception {
        try (Connection c = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_getConnection(); Statement s = c.createStatement();
             ResultSet r = s.executeQuery(sql)) { r.next(); return r.getInt(1); }
    }

    /** Crea un curso con dos estudiantes y devuelve {claseId, estudiante1Id, estudiante2Id}. */
    private static int[] WO_KI_NB_JI_cursoConEstudiantes(WO_KI_NB_JI_ClaseDocenteDAO dao) {
        int claseId = dao.WO_KI_NB_JI_crearClase("Matemáticas", "A", "2026-I");
        assertTrue(dao.WO_KI_NB_JI_guardarEstudiante(claseId, "Ana Pérez", null, null, null));
        assertTrue(dao.WO_KI_NB_JI_guardarEstudiante(claseId, "Luis Gómez", null, null, null));
        List<WO_KI_NB_JI_EstudianteClase> estudiantes = dao.WO_KI_NB_JI_listarEstudiantes(claseId);
        assertEquals(2, estudiantes.size());
        return new int[]{claseId, estudiantes.get(0).id(), estudiantes.get(1).id()};
    }

    @Test void WO_KI_NB_JI_migraTablaDeAsistenciaYEsIdempotente() throws Exception {
        WO_KI_NB_JI_nuevaBd();
        WO_KI_NB_JI_Database.WO_KI_NB_JI_migrate();
        assertEquals(WO_KI_NB_JI_Database.WO_KI_NB_JI_versionObjetivo(), WO_KI_NB_JI_Database.WO_KI_NB_JI_version());
        assertTrue(WO_KI_NB_JI_Database.WO_KI_NB_JI_versionObjetivo() >= 6);     // v6 = asistencia (v7 = entregas y aula virtual)
        assertEquals(0, WO_KI_NB_JI_contar("SELECT count(*) FROM asistencia"));
        WO_KI_NB_JI_Database.WO_KI_NB_JI_migrate();
        assertEquals(WO_KI_NB_JI_Database.WO_KI_NB_JI_versionObjetivo(), WO_KI_NB_JI_Database.WO_KI_NB_JI_version());
    }

    @Test void WO_KI_NB_JI_guardaUnSoloRegistroPorEstudianteYFecha() throws Exception {
        WO_KI_NB_JI_nuevaBd(); WO_KI_NB_JI_Database.WO_KI_NB_JI_migrate();
        WO_KI_NB_JI_ClaseDocenteDAO dao = new WO_KI_NB_JI_ClaseDocenteDAO();
        int[] c = WO_KI_NB_JI_cursoConEstudiantes(dao);

        assertTrue(dao.WO_KI_NB_JI_guardarAsistencia(c[0], c[1], LUNES, "Presente"));
        assertEquals(Map.of(c[1], "Presente"), dao.WO_KI_NB_JI_asistencia(c[0], LUNES));

        // Corregir el estado no debe duplicar el registro del día.
        assertTrue(dao.WO_KI_NB_JI_guardarAsistencia(c[0], c[1], LUNES, "Falta"));
        assertEquals(Map.of(c[1], "Falta"), dao.WO_KI_NB_JI_asistencia(c[0], LUNES));
        assertEquals(1, WO_KI_NB_JI_contar("SELECT count(*) FROM asistencia WHERE estudiante_id=" + c[1]));
        assertTrue(dao.WO_KI_NB_JI_asistencia(c[0], MARTES).isEmpty());
    }

    @Test void WO_KI_NB_JI_rechazaEstadoInvalido() throws Exception {
        WO_KI_NB_JI_nuevaBd(); WO_KI_NB_JI_Database.WO_KI_NB_JI_migrate();
        WO_KI_NB_JI_ClaseDocenteDAO dao = new WO_KI_NB_JI_ClaseDocenteDAO();
        int[] c = WO_KI_NB_JI_cursoConEstudiantes(dao);
        assertFalse(dao.WO_KI_NB_JI_guardarAsistencia(c[0], c[1], LUNES, "Volando"));
        assertTrue(dao.WO_KI_NB_JI_asistencia(c[0], LUNES).isEmpty());
    }

    @Test void WO_KI_NB_JI_porcentajeCuentaPresenteYRetardoComoAsistio() throws Exception {
        WO_KI_NB_JI_nuevaBd(); WO_KI_NB_JI_Database.WO_KI_NB_JI_migrate();
        WO_KI_NB_JI_ClaseDocenteDAO dao = new WO_KI_NB_JI_ClaseDocenteDAO();
        int[] c = WO_KI_NB_JI_cursoConEstudiantes(dao);

        // Ana: presente y falta → 50 %. Luis: presente y retardo → 100 %.
        dao.WO_KI_NB_JI_guardarAsistencia(c[0], c[1], LUNES, "Presente");
        dao.WO_KI_NB_JI_guardarAsistencia(c[0], c[2], LUNES, "Presente");
        dao.WO_KI_NB_JI_guardarAsistencia(c[0], c[1], MARTES, "Falta");
        dao.WO_KI_NB_JI_guardarAsistencia(c[0], c[2], MARTES, "Retardo");

        Map<Integer, WO_KI_NB_JI_ClaseDocenteDAO.WO_KI_NB_JI_EstadisticaAsistencia> stats = dao.WO_KI_NB_JI_estadisticasAsistencia(c[0]);
        WO_KI_NB_JI_ClaseDocenteDAO.WO_KI_NB_JI_EstadisticaAsistencia ana = stats.get(c[1]);
        assertEquals(2, ana.sesiones());
        assertEquals(1, ana.presentes());
        assertEquals(1, ana.faltas());
        assertEquals(50.0, ana.WO_KI_NB_JI_porcentaje(), 0.001);

        WO_KI_NB_JI_ClaseDocenteDAO.WO_KI_NB_JI_EstadisticaAsistencia luis = stats.get(c[2]);
        assertEquals(1, luis.retardos());
        assertEquals(100.0, luis.WO_KI_NB_JI_porcentaje(), 0.001);

        assertEquals(2, dao.WO_KI_NB_JI_sesionesAsistencia(c[0]));
        assertTrue(Double.isNaN(new WO_KI_NB_JI_ClaseDocenteDAO.WO_KI_NB_JI_EstadisticaAsistencia(0, 0, 0, 0, 0).WO_KI_NB_JI_porcentaje()));
    }

    @Test void WO_KI_NB_JI_limpiarDiaYBorradoEnCascada() throws Exception {
        WO_KI_NB_JI_nuevaBd(); WO_KI_NB_JI_Database.WO_KI_NB_JI_migrate();
        WO_KI_NB_JI_ClaseDocenteDAO dao = new WO_KI_NB_JI_ClaseDocenteDAO();
        int[] c = WO_KI_NB_JI_cursoConEstudiantes(dao);
        dao.WO_KI_NB_JI_guardarAsistencia(c[0], c[1], LUNES, "Presente");
        dao.WO_KI_NB_JI_guardarAsistencia(c[0], c[2], LUNES, "Falta");

        assertTrue(dao.WO_KI_NB_JI_quitarAsistenciaDia(c[0], LUNES));
        assertTrue(dao.WO_KI_NB_JI_asistencia(c[0], LUNES).isEmpty());

        dao.WO_KI_NB_JI_guardarAsistencia(c[0], c[1], LUNES, "Presente");
        assertTrue(dao.WO_KI_NB_JI_quitarAsistencia(c[1], LUNES));
        assertTrue(dao.WO_KI_NB_JI_asistencia(c[0], LUNES).isEmpty());

        // Al eliminar el estudiante o el curso, sus registros de asistencia desaparecen con ellos.
        dao.WO_KI_NB_JI_guardarAsistencia(c[0], c[1], LUNES, "Presente");
        assertTrue(dao.WO_KI_NB_JI_eliminarEstudiante(c[1]));
        assertEquals(0, WO_KI_NB_JI_contar("SELECT count(*) FROM asistencia"));
    }

    @Test void WO_KI_NB_JI_borrarTodosLosDatosTambienLimpiaAsistencia() throws Exception {
        WO_KI_NB_JI_nuevaBd(); WO_KI_NB_JI_Database.WO_KI_NB_JI_migrate();
        WO_KI_NB_JI_ClaseDocenteDAO dao = new WO_KI_NB_JI_ClaseDocenteDAO();
        int[] c = WO_KI_NB_JI_cursoConEstudiantes(dao);
        dao.WO_KI_NB_JI_guardarAsistencia(c[0], c[1], LUNES, "Presente");
        assertEquals(1, WO_KI_NB_JI_contar("SELECT count(*) FROM asistencia"));

        WO_KI_NB_JI_Database.WO_KI_NB_JI_borrarTodosLosDatos();
        assertEquals(0, WO_KI_NB_JI_contar("SELECT count(*) FROM asistencia"));
        assertEquals(0, WO_KI_NB_JI_contar("SELECT count(*) FROM estudiante_clase"));
    }
}

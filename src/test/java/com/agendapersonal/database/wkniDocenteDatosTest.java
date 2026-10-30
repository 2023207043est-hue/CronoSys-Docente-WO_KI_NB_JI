package com.agendapersonal.database;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.domain.model.wkniClaseDocente;
import com.agendapersonal.domain.model.wkniEstadoEntrega;
import com.agendapersonal.domain.model.wkniEstudianteClase;
import com.agendapersonal.domain.model.wkniRecursoClase;
import com.agendapersonal.infrastructure.db.wkniDatabase;
import com.agendapersonal.infrastructure.repo.wkniClaseDocenteDAO;
import com.agendapersonal.service.wkniDatosPruebaDocente;
import com.agendapersonal.service.wkniExcelClaseService;
import com.agendapersonal.service.wkniReportesDocente;
import com.agendapersonal.service.wkniResumenCurso;
import com.agendapersonal.service.wkniRiesgoEstudianteService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/** Entregas, aula virtual, datos de prueba y reportes del Espacio Docente (BD temporal). */
class wkniDocenteDatosTest {
    private static final LocalDate HOY = LocalDate.of(2026, 10, 26);

    private static wkniClaseDocenteDAO wkniNuevaBd() throws Exception {
        Path dir = Files.createTempDirectory("cronosys-docente");
        System.setProperty(wkniDatabaseConnection.wkniPROP_RUTA, dir.resolve("prueba.db").toString());
        wkniDatabase.wkniMigrate();
        return new wkniClaseDocenteDAO();
    }

    @Test void wkniNotaMinimaSeGuardaYSeEdita() throws Exception {
        wkniClaseDocenteDAO dao = wkniNuevaBd();
        int id = dao.wkniCrearClase("Física", "B", null, 13);
        assertEquals(13, dao.wkniListarClases().getFirst().wkniNotaMinima(), 0.001);
        assertTrue(dao.wkniActualizarClase(id, "Física II", "C", "2026-II", 10.5));
        wkniClaseDocente c = dao.wkniListarClases().getFirst();
        assertEquals("Física II", c.wkniNombre());
        assertEquals(10.5, c.wkniNotaMinima(), 0.001);
        int arte = dao.wkniCrearClase("Arte", null, null);
        assertEquals(wkniClaseDocente.wkniNOTA_MINIMA_DEFECTO, dao.wkniListarClases().stream()
            .filter(x -> x.wkniId() == arte).findFirst().orElseThrow().wkniNotaMinima(), 0.001);
    }

    @Test void wkniEntregasSeGuardanCorrigenYBorranEnCascada() throws Exception {
        wkniClaseDocenteDAO dao = wkniNuevaBd();
        int clase = dao.wkniCrearClase("Química", "A", null);
        dao.wkniGuardarEstudiante(clase, "Ana", null, null, null);
        int est = dao.wkniListarEstudiantes(clase).getFirst().wkniId();
        int act = dao.wkniCrearActividadId(clase, "Informe 1", "Tarea", HOY, null);
        dao.wkniCrearActividad(clase, "Sesión 1", "Clase", HOY, null);
        assertEquals(1, dao.wkniListarEvaluables(clase).size());

        assertTrue(dao.wkniGuardarEntrega(act, est, wkniEstadoEntrega.TARDE, 14.0));
        assertTrue(dao.wkniGuardarEntrega(act, est, null, 16.0));   // con nota y sin estado → entregado
        var e = dao.wkniEntregas(clase).get(act).get(est);
        assertEquals(wkniEstadoEntrega.ENTREGADO, e.wkniEstado());
        assertEquals(16.0, e.wkni_nota(), 0.001);

        assertTrue(dao.wkniGuardarEntrega(act, est, null, null));   // sin nada → se borra
        assertTrue(dao.wkniEntregas(clase).isEmpty());

        dao.wkniGuardarEntrega(act, est, wkniEstadoEntrega.NO_ENTREGADO, null);
        assertTrue(dao.wkniEliminarActividad(act));
        assertTrue(dao.wkniEntregas(clase).isEmpty());
    }

    @Test void wkniRecursosDelAulaVirtual() throws Exception {
        wkniClaseDocenteDAO dao = wkniNuevaBd();
        int clase = dao.wkniCrearClase("Historia", null, null);
        dao.wkniCrearRecurso(clase, wkniRecursoClase.wkniANUNCIO, "Viejo", "texto", HOY.minusDays(5), false);
        dao.wkniCrearRecurso(clase, wkniRecursoClase.wkniENLACE, "Drive", "https://drive.google.com/", HOY, false);
        dao.wkniCrearRecurso(clase, wkniRecursoClase.wkniANUNCIO, "Importante", "fijado", HOY.minusDays(9), true);
        List<wkniRecursoClase> r = dao.wkniListarRecursos(clase);
        assertEquals(List.of("Importante", "Drive", "Viejo"), r.stream().map(wkniRecursoClase::wkniTitulo).toList());
        assertTrue(dao.wkniFijarRecurso(r.getFirst().wkniId(), false));
        assertEquals("Drive", dao.wkniListarRecursos(clase).getFirst().wkniTitulo());
        assertTrue(dao.wkniEliminarClase(clase));
        assertTrue(dao.wkniListarRecursos(clase).isEmpty());
    }

    @Test void wkniTardanzaSeGuardaComoRetardo() throws Exception {
        wkniClaseDocenteDAO dao = wkniNuevaBd();
        int clase = dao.wkniCrearClase("Arte", null, null);
        dao.wkniGuardarEstudiante(clase, "Luis", null, null, null);
        int est = dao.wkniListarEstudiantes(clase).getFirst().wkniId();
        assertTrue(dao.wkniGuardarAsistencia(clase, est, HOY, "Tardanza"));
        assertEquals("Retardo", dao.wkniAsistencia(clase, HOY).get(est));
    }

    @Test void wkniDatosDePruebaGeneranAlertasYSeBorranSinTocarCursosReales() throws Exception {
        wkniClaseDocenteDAO dao = wkniNuevaBd();
        int real = dao.wkniCrearClase("Mi curso real", null, "2026-II");
        assertEquals(2, wkniDatosPruebaDocente.wkniCrear(dao, HOY));
        assertTrue(wkniDatosPruebaDocente.wkniHayDatosPrueba(dao));

        wkniClaseDocente mate = dao.wkniListarClases().stream().filter(c -> c.wkniNombre().equals("Matemática I")).findFirst().orElseThrow();
        wkniResumenCurso r = new wkniResumenCurso(mate, dao, HOY);
        assertEquals(14, r.wkniEstudiantes().size());
        assertEquals(12, r.wkniFechas().size());
        assertTrue(r.wkniContar(wkniRiesgoEstudianteService.wkniNivel.ALTO) >= 3, "debe haber varios estudiantes en riesgo");
        assertTrue(r.wkniEstudiantes().stream().anyMatch(e -> r.wkniEvaluacion(e.wkniId()).wkniMotivos().contains("3 faltas consecutivas")));
        assertTrue(r.wkniEstudiantes().stream().anyMatch(e -> r.wkniEvaluacion(e.wkniId()).wkniMotivos().contains("2 trabajos no entregados")));
        assertTrue(r.wkniAlDia() < r.wkniEstudiantes().size());
        assertFalse(dao.wkniListarRecursos(mate.wkniId()).isEmpty());

        assertEquals(2, wkniDatosPruebaDocente.wkniEliminar(dao));
        assertFalse(wkniDatosPruebaDocente.wkniHayDatosPrueba(dao));
        assertEquals(List.of(real), dao.wkniListarClases().stream().map(wkniClaseDocente::wkniId).toList());
    }

    @Test void wkniExportaLibroConVariasHojasYColores() throws Exception {
        wkniClaseDocenteDAO dao = wkniNuevaBd();
        wkniDatosPruebaDocente.wkniCrear(dao, HOY);
        wkniClaseDocente c = dao.wkniListarClases().getFirst();
        wkniResumenCurso r = new wkniResumenCurso(c, dao, HOY);
        Path xlsx = Files.createTempFile("asistencia", ".xlsx");
        wkniExcelClaseService.wkniExportarReporte(xlsx, List.of(
            wkniReportesDocente.wkniAsistenciaMatriz(r, HOY.withDayOfMonth(1), HOY, List.of(HOY)),
            wkniReportesDocente.wkniAsistenciaDia(r, HOY), wkniReportesDocente.wkniTrabajos(r), wkniReportesDocente.wkniRiesgo(r)));
        try (ZipFile zip = new ZipFile(xlsx.toFile())) {
            assertNotNull(zip.getEntry("xl/styles.xml"));
            assertNotNull(zip.getEntry("xl/worksheets/sheet4.xml"));
            String hoja1 = new String(zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml")).readAllBytes());
            assertTrue(hoja1.contains("s=\"5\""), "celdas de Presente en verde");
            String libro = new String(zip.getInputStream(zip.getEntry("xl/workbook.xml")).readAllBytes());
            assertTrue(libro.contains("Semáforo de riesgo"));
        }
        // El libro exportado se puede volver a leer (la primera hoja tiene la columna Estudiante en la fila 4).
        var imp = wkniExcelClaseService.wkniImportar(xlsx, c.wkniId());
        assertEquals(r.wkniEstudiantes().size(), imp.wkniEstudiantes().size());
        wkniEstudianteClase primero = r.wkniEstudiantes().getFirst();
        assertEquals(primero.wkniNombre(), imp.wkniEstudiantes().getFirst().wkniNombre());
    }
}

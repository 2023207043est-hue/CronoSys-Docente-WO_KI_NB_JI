package com.agendapersonal.database;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.domain.model.WO_KI_NB_JI_ClaseDocente;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstadoEntrega;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstudianteClase;
import com.agendapersonal.domain.model.WO_KI_NB_JI_RecursoClase;
import com.agendapersonal.infrastructure.db.WO_KI_NB_JI_Database;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_ClaseDocenteDAO;
import com.agendapersonal.service.WO_KI_NB_JI_DatosPruebaDocente;
import com.agendapersonal.service.WO_KI_NB_JI_ExcelClaseService;
import com.agendapersonal.service.WO_KI_NB_JI_ReportesDocente;
import com.agendapersonal.service.WO_KI_NB_JI_ResumenCurso;
import com.agendapersonal.service.WO_KI_NB_JI_RiesgoEstudianteService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.Test;

/** Entregas, aula virtual, datos de prueba y reportes del Espacio Docente (BD temporal). */
class WO_KI_NB_JI_DocenteDatosTest {
    private static final LocalDate HOY = LocalDate.of(2026, 10, 26);

    private static WO_KI_NB_JI_ClaseDocenteDAO WO_KI_NB_JI_nuevaBd() throws Exception {
        Path dir = Files.createTempDirectory("cronosys-docente");
        System.setProperty(WO_KI_NB_JI_DatabaseConnection.PROP_RUTA, dir.resolve("prueba.db").toString());
        WO_KI_NB_JI_Database.WO_KI_NB_JI_migrate();
        return new WO_KI_NB_JI_ClaseDocenteDAO();
    }

    @Test void WO_KI_NB_JI_notaMinimaSeGuardaYSeEdita() throws Exception {
        WO_KI_NB_JI_ClaseDocenteDAO dao = WO_KI_NB_JI_nuevaBd();
        int id = dao.WO_KI_NB_JI_crearClase("Física", "B", null, 13);
        assertEquals(13, dao.WO_KI_NB_JI_listarClases().getFirst().notaMinima(), 0.001);
        assertTrue(dao.WO_KI_NB_JI_actualizarClase(id, "Física II", "C", "2026-II", 10.5));
        WO_KI_NB_JI_ClaseDocente c = dao.WO_KI_NB_JI_listarClases().getFirst();
        assertEquals("Física II", c.nombre());
        assertEquals(10.5, c.notaMinima(), 0.001);
        int arte = dao.WO_KI_NB_JI_crearClase("Arte", null, null);
        assertEquals(WO_KI_NB_JI_ClaseDocente.NOTA_MINIMA_DEFECTO, dao.WO_KI_NB_JI_listarClases().stream()
            .filter(x -> x.id() == arte).findFirst().orElseThrow().notaMinima(), 0.001);
    }

    @Test void WO_KI_NB_JI_entregasSeGuardanCorrigenYBorranEnCascada() throws Exception {
        WO_KI_NB_JI_ClaseDocenteDAO dao = WO_KI_NB_JI_nuevaBd();
        int clase = dao.WO_KI_NB_JI_crearClase("Química", "A", null);
        dao.WO_KI_NB_JI_guardarEstudiante(clase, "Ana", null, null, null);
        int est = dao.WO_KI_NB_JI_listarEstudiantes(clase).getFirst().id();
        int act = dao.WO_KI_NB_JI_crearActividadId(clase, "Informe 1", "Tarea", HOY, null);
        dao.WO_KI_NB_JI_crearActividad(clase, "Sesión 1", "Clase", HOY, null);
        assertEquals(1, dao.WO_KI_NB_JI_listarEvaluables(clase).size());

        assertTrue(dao.WO_KI_NB_JI_guardarEntrega(act, est, WO_KI_NB_JI_EstadoEntrega.TARDE, 14.0));
        assertTrue(dao.WO_KI_NB_JI_guardarEntrega(act, est, null, 16.0));   // con nota y sin estado → entregado
        var e = dao.WO_KI_NB_JI_entregas(clase).get(act).get(est);
        assertEquals(WO_KI_NB_JI_EstadoEntrega.ENTREGADO, e.estado());
        assertEquals(16.0, e.nota(), 0.001);

        assertTrue(dao.WO_KI_NB_JI_guardarEntrega(act, est, null, null));   // sin nada → se borra
        assertTrue(dao.WO_KI_NB_JI_entregas(clase).isEmpty());

        dao.WO_KI_NB_JI_guardarEntrega(act, est, WO_KI_NB_JI_EstadoEntrega.NO_ENTREGADO, null);
        assertTrue(dao.WO_KI_NB_JI_eliminarActividad(act));
        assertTrue(dao.WO_KI_NB_JI_entregas(clase).isEmpty());
    }

    @Test void WO_KI_NB_JI_recursosDelAulaVirtual() throws Exception {
        WO_KI_NB_JI_ClaseDocenteDAO dao = WO_KI_NB_JI_nuevaBd();
        int clase = dao.WO_KI_NB_JI_crearClase("Historia", null, null);
        dao.WO_KI_NB_JI_crearRecurso(clase, WO_KI_NB_JI_RecursoClase.ANUNCIO, "Viejo", "texto", HOY.minusDays(5), false);
        dao.WO_KI_NB_JI_crearRecurso(clase, WO_KI_NB_JI_RecursoClase.ENLACE, "Drive", "https://drive.google.com/", HOY, false);
        dao.WO_KI_NB_JI_crearRecurso(clase, WO_KI_NB_JI_RecursoClase.ANUNCIO, "Importante", "fijado", HOY.minusDays(9), true);
        List<WO_KI_NB_JI_RecursoClase> r = dao.WO_KI_NB_JI_listarRecursos(clase);
        assertEquals(List.of("Importante", "Drive", "Viejo"), r.stream().map(WO_KI_NB_JI_RecursoClase::titulo).toList());
        assertTrue(dao.WO_KI_NB_JI_fijarRecurso(r.getFirst().id(), false));
        assertEquals("Drive", dao.WO_KI_NB_JI_listarRecursos(clase).getFirst().titulo());
        assertTrue(dao.WO_KI_NB_JI_eliminarClase(clase));
        assertTrue(dao.WO_KI_NB_JI_listarRecursos(clase).isEmpty());
    }

    @Test void WO_KI_NB_JI_tardanzaSeGuardaComoRetardo() throws Exception {
        WO_KI_NB_JI_ClaseDocenteDAO dao = WO_KI_NB_JI_nuevaBd();
        int clase = dao.WO_KI_NB_JI_crearClase("Arte", null, null);
        dao.WO_KI_NB_JI_guardarEstudiante(clase, "Luis", null, null, null);
        int est = dao.WO_KI_NB_JI_listarEstudiantes(clase).getFirst().id();
        assertTrue(dao.WO_KI_NB_JI_guardarAsistencia(clase, est, HOY, "Tardanza"));
        assertEquals("Retardo", dao.WO_KI_NB_JI_asistencia(clase, HOY).get(est));
    }

    @Test void WO_KI_NB_JI_datosDePruebaGeneranAlertasYSeBorranSinTocarCursosReales() throws Exception {
        WO_KI_NB_JI_ClaseDocenteDAO dao = WO_KI_NB_JI_nuevaBd();
        int real = dao.WO_KI_NB_JI_crearClase("Mi curso real", null, "2026-II");
        assertEquals(2, WO_KI_NB_JI_DatosPruebaDocente.WO_KI_NB_JI_crear(dao, HOY));
        assertTrue(WO_KI_NB_JI_DatosPruebaDocente.WO_KI_NB_JI_hayDatosPrueba(dao));

        WO_KI_NB_JI_ClaseDocente mate = dao.WO_KI_NB_JI_listarClases().stream().filter(c -> c.nombre().equals("Matemática I")).findFirst().orElseThrow();
        WO_KI_NB_JI_ResumenCurso r = new WO_KI_NB_JI_ResumenCurso(mate, dao, HOY);
        assertEquals(14, r.WO_KI_NB_JI_estudiantes().size());
        assertEquals(12, r.WO_KI_NB_JI_fechas().size());
        assertTrue(r.WO_KI_NB_JI_contar(WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel.ALTO) >= 3, "debe haber varios estudiantes en riesgo");
        assertTrue(r.WO_KI_NB_JI_estudiantes().stream().anyMatch(e -> r.WO_KI_NB_JI_evaluacion(e.id()).motivos().contains("3 faltas consecutivas")));
        assertTrue(r.WO_KI_NB_JI_estudiantes().stream().anyMatch(e -> r.WO_KI_NB_JI_evaluacion(e.id()).motivos().contains("2 trabajos no entregados")));
        assertTrue(r.WO_KI_NB_JI_alDia() < r.WO_KI_NB_JI_estudiantes().size());
        assertFalse(dao.WO_KI_NB_JI_listarRecursos(mate.id()).isEmpty());

        assertEquals(2, WO_KI_NB_JI_DatosPruebaDocente.WO_KI_NB_JI_eliminar(dao));
        assertFalse(WO_KI_NB_JI_DatosPruebaDocente.WO_KI_NB_JI_hayDatosPrueba(dao));
        assertEquals(List.of(real), dao.WO_KI_NB_JI_listarClases().stream().map(WO_KI_NB_JI_ClaseDocente::id).toList());
    }

    @Test void WO_KI_NB_JI_exportaLibroConVariasHojasYColores() throws Exception {
        WO_KI_NB_JI_ClaseDocenteDAO dao = WO_KI_NB_JI_nuevaBd();
        WO_KI_NB_JI_DatosPruebaDocente.WO_KI_NB_JI_crear(dao, HOY);
        WO_KI_NB_JI_ClaseDocente c = dao.WO_KI_NB_JI_listarClases().getFirst();
        WO_KI_NB_JI_ResumenCurso r = new WO_KI_NB_JI_ResumenCurso(c, dao, HOY);
        Path xlsx = Files.createTempFile("asistencia", ".xlsx");
        WO_KI_NB_JI_ExcelClaseService.WO_KI_NB_JI_exportarReporte(xlsx, List.of(
            WO_KI_NB_JI_ReportesDocente.WO_KI_NB_JI_asistenciaMatriz(r, HOY.withDayOfMonth(1), HOY, List.of(HOY)),
            WO_KI_NB_JI_ReportesDocente.WO_KI_NB_JI_asistenciaDia(r, HOY), WO_KI_NB_JI_ReportesDocente.WO_KI_NB_JI_trabajos(r), WO_KI_NB_JI_ReportesDocente.WO_KI_NB_JI_riesgo(r)));
        try (ZipFile zip = new ZipFile(xlsx.toFile())) {
            assertNotNull(zip.getEntry("xl/styles.xml"));
            assertNotNull(zip.getEntry("xl/worksheets/sheet4.xml"));
            String hoja1 = new String(zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml")).readAllBytes());
            assertTrue(hoja1.contains("s=\"5\""), "celdas de Presente en verde");
            String libro = new String(zip.getInputStream(zip.getEntry("xl/workbook.xml")).readAllBytes());
            assertTrue(libro.contains("Semáforo de riesgo"));
        }
        // El libro exportado se puede volver a leer (la primera hoja tiene la columna Estudiante en la fila 4).
        var imp = WO_KI_NB_JI_ExcelClaseService.WO_KI_NB_JI_importar(xlsx, c.id());
        assertEquals(r.WO_KI_NB_JI_estudiantes().size(), imp.estudiantes().size());
        WO_KI_NB_JI_EstudianteClase primero = r.WO_KI_NB_JI_estudiantes().getFirst();
        assertEquals(primero.nombre(), imp.estudiantes().getFirst().nombre());
    }
}

package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.database.wkniDatabaseConnection;
import com.agendapersonal.domain.model.wkniClaseDocente;
import com.agendapersonal.domain.model.wkniEstadoEntrega;
import com.agendapersonal.domain.model.wkniEstudianteClase;
import com.agendapersonal.infrastructure.db.wkniDatabase;
import com.agendapersonal.infrastructure.repo.wkniClaseDocenteDAO;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class wkniReporteEstudianteTest {
    private static final LocalDate HOY = LocalDate.of(2026, 10, 26);

    @Test void wkniReuneAsistenciaTrabajosYSugerencias() throws Exception {
        System.setProperty(wkniDatabaseConnection.wkniPROP_RUTA, Files.createTempDirectory("rep").resolve("t.db").toString());
        wkniDatabase.wkniMigrate();
        wkniClaseDocenteDAO dao = new wkniClaseDocenteDAO();
        int c = dao.wkniCrearClase("Lógica", "A", "2026-II");
        dao.wkniGuardarEstudiante(c, "Ana", null, null, "Participa poco");
        int e = dao.wkniListarEstudiantes(c).getFirst().wkniId();
        for (int i = 3; i >= 1; i--) dao.wkniGuardarAsistencia(c, e, HOY.minusDays(i), "Falta");
        dao.wkniGuardarAsistencia(c, e, HOY.minusDays(4), "Presente");
        dao.wkniGuardarAsistencia(c, e, HOY.minusDays(5), "Retardo");
        int t1 = dao.wkniCrearActividadId(c, "Práctica 1", "Tarea", HOY.minusDays(7), null);
        int t2 = dao.wkniCrearActividadId(c, "Práctica 2", "Tarea", HOY.minusDays(2), null);
        dao.wkniCrearActividadId(c, "Examen", "Evaluación", HOY.plusDays(5), null);
        dao.wkniGuardarEntrega(t1, e, wkniEstadoEntrega.ENTREGADO, 8.0);
        dao.wkniGuardarEntrega(t2, e, wkniEstadoEntrega.NO_ENTREGADO, null);

        wkniClaseDocente clase = dao.wkniListarClases().getFirst();
        wkniResumenCurso r = new wkniResumenCurso(clase, dao, HOY);
        wkniEstudianteClase ana = r.wkniEstudiantes().getFirst();
        wkniReporteEstudiante rep = wkniReporteEstudiante.wkniDe(r, ana, HOY);

        assertEquals(1, rep.wkniPresentes());
        assertEquals(3, rep.wkniFaltas());
        assertEquals(1, rep.wkniTardanzas());
        assertEquals(5, rep.wkniHistorial().size());
        assertEquals(List.of("Práctica 1", "Práctica 2", "Examen"), rep.wkniTrabajos().stream().map(wkniReporteEstudiante.wkniTrabajo::wkniTitulo).toList());
        assertFalse(rep.wkniTrabajos().get(2).wkniVencido());
        assertEquals(8.0, rep.wkniPromedioTrabajos(), 0.001);
        assertEquals(wkniRiesgoEstudianteService.wkniNivel.ALTO, rep.wkni_evaluacion().wkni_nivel());
        assertTrue(rep.wkniSugerencias().stream().anyMatch(s -> s.contains("inasistencias")));
        assertTrue(rep.wkniSugerencias().stream().anyMatch(s -> s.contains("trabajo pendiente")));
        assertTrue(rep.wkniSugerencias().stream().anyMatch(s -> s.contains("nota mínima de 11")));
    }

    @Test void wkniSinProblemasSugiereMantener() {
        var ev = wkniRiesgoEstudianteService.wkniEvaluar(new java.util.TreeMap<>(), List.of(), List.of(), 16.0, 11);
        assertEquals(List.of("Buen desempeño: mantener la constancia en asistencia y entregas."), wkniReporteEstudiante.wkniSugerencias(ev, 11));
    }
}

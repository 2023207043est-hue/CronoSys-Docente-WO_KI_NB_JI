package com.agendapersonal.service;

import static org.junit.jupiter.api.Assertions.*;

import com.agendapersonal.database.WO_KI_NB_JI_DatabaseConnection;
import com.agendapersonal.domain.model.WO_KI_NB_JI_ClaseDocente;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstadoEntrega;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstudianteClase;
import com.agendapersonal.infrastructure.db.WO_KI_NB_JI_Database;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_ClaseDocenteDAO;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class WO_KI_NB_JI_ReporteEstudianteTest {
    private static final LocalDate HOY = LocalDate.of(2026, 10, 26);

    @Test void WO_KI_NB_JI_reuneAsistenciaTrabajosYSugerencias() throws Exception {
        System.setProperty(WO_KI_NB_JI_DatabaseConnection.PROP_RUTA, Files.createTempDirectory("rep").resolve("t.db").toString());
        WO_KI_NB_JI_Database.WO_KI_NB_JI_migrate();
        WO_KI_NB_JI_ClaseDocenteDAO dao = new WO_KI_NB_JI_ClaseDocenteDAO();
        int c = dao.WO_KI_NB_JI_crearClase("Lógica", "A", "2026-II");
        dao.WO_KI_NB_JI_guardarEstudiante(c, "Ana", null, null, "Participa poco");
        int e = dao.WO_KI_NB_JI_listarEstudiantes(c).getFirst().id();
        for (int i = 3; i >= 1; i--) dao.WO_KI_NB_JI_guardarAsistencia(c, e, HOY.minusDays(i), "Falta");
        dao.WO_KI_NB_JI_guardarAsistencia(c, e, HOY.minusDays(4), "Presente");
        dao.WO_KI_NB_JI_guardarAsistencia(c, e, HOY.minusDays(5), "Retardo");
        int t1 = dao.WO_KI_NB_JI_crearActividadId(c, "Práctica 1", "Tarea", HOY.minusDays(7), null);
        int t2 = dao.WO_KI_NB_JI_crearActividadId(c, "Práctica 2", "Tarea", HOY.minusDays(2), null);
        dao.WO_KI_NB_JI_crearActividadId(c, "Examen", "Evaluación", HOY.plusDays(5), null);
        dao.WO_KI_NB_JI_guardarEntrega(t1, e, WO_KI_NB_JI_EstadoEntrega.ENTREGADO, 8.0);
        dao.WO_KI_NB_JI_guardarEntrega(t2, e, WO_KI_NB_JI_EstadoEntrega.NO_ENTREGADO, null);

        WO_KI_NB_JI_ClaseDocente clase = dao.WO_KI_NB_JI_listarClases().getFirst();
        WO_KI_NB_JI_ResumenCurso r = new WO_KI_NB_JI_ResumenCurso(clase, dao, HOY);
        WO_KI_NB_JI_EstudianteClase ana = r.WO_KI_NB_JI_estudiantes().getFirst();
        WO_KI_NB_JI_ReporteEstudiante rep = WO_KI_NB_JI_ReporteEstudiante.WO_KI_NB_JI_de(r, ana, HOY);

        assertEquals(1, rep.presentes());
        assertEquals(3, rep.faltas());
        assertEquals(1, rep.tardanzas());
        assertEquals(5, rep.historial().size());
        assertEquals(List.of("Práctica 1", "Práctica 2", "Examen"), rep.trabajos().stream().map(WO_KI_NB_JI_ReporteEstudiante.WO_KI_NB_JI_Trabajo::titulo).toList());
        assertFalse(rep.trabajos().get(2).vencido());
        assertEquals(8.0, rep.promedioTrabajos(), 0.001);
        assertEquals(WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_Nivel.ALTO, rep.evaluacion().nivel());
        assertTrue(rep.sugerencias().stream().anyMatch(s -> s.contains("inasistencias")));
        assertTrue(rep.sugerencias().stream().anyMatch(s -> s.contains("trabajo pendiente")));
        assertTrue(rep.sugerencias().stream().anyMatch(s -> s.contains("nota mínima de 11")));
    }

    @Test void WO_KI_NB_JI_sinProblemasSugiereMantener() {
        var ev = WO_KI_NB_JI_RiesgoEstudianteService.WO_KI_NB_JI_evaluar(new java.util.TreeMap<>(), List.of(), List.of(), 16.0, 11);
        assertEquals(List.of("Buen desempeño: mantener la constancia en asistencia y entregas."), WO_KI_NB_JI_ReporteEstudiante.WO_KI_NB_JI_sugerencias(ev, 11));
    }
}

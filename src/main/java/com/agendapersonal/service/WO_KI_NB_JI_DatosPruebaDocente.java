package com.agendapersonal.service;

import com.agendapersonal.domain.model.WO_KI_NB_JI_ClaseDocente;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstadoAsistencia;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstadoEntrega;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstudianteClase;
import com.agendapersonal.domain.model.WO_KI_NB_JI_RecursoClase;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_ClaseDocenteDAO;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Crea cursos de prueba con estudiantes, asistencia, trabajos, notas y aula virtual para probar el
 * Espacio Docente (gráficas, semáforo y alertas incluidas). Los cursos llevan el periodo
 * "… (prueba)" para poder borrarlos sin tocar los cursos reales.
 */
public final class WO_KI_NB_JI_DatosPruebaDocente {
    public static final String MARCA = "(prueba)";

    private static final String[] NOMBRES = {
        "Ana Lucía Quispe Mamani", "Bruno Huamán Torres", "Carla Flores Condori", "Diego Ramos Ccama",
        "Elena Vargas Apaza", "Fernando Chávez Poma", "Gabriela Rojas Ticona", "Hugo Mendoza Larico",
        "Isabel Cutipa Paredes", "Jorge Salazar Coaquira", "Karen Pari Choque", "Luis Alberto Zea Ninaja",
        "María José Sánchez Calla", "Nicolás Torres Yupanqui", "Olga Medina Arpasi", "Pedro Castillo Velásquez",
        "Rosa Gutiérrez Vilca", "Samuel Aguilar Machaca", "Tatiana Luque Cruz", "Víctor Pilco Huanca"
    };

    private WO_KI_NB_JI_DatosPruebaDocente() {}

    public static boolean WO_KI_NB_JI_hayDatosPrueba(WO_KI_NB_JI_ClaseDocenteDAO dao) {
        return dao.WO_KI_NB_JI_listarClases().stream().anyMatch(WO_KI_NB_JI_DatosPruebaDocente::WO_KI_NB_JI_esPrueba);
    }

    public static boolean WO_KI_NB_JI_esPrueba(WO_KI_NB_JI_ClaseDocente c) { return c.periodo() != null && c.periodo().endsWith(MARCA); }

    /** Borra solo los cursos de prueba (sus estudiantes, asistencia y entregas caen en cascada). */
    public static int WO_KI_NB_JI_eliminar(WO_KI_NB_JI_ClaseDocenteDAO dao) {
        int n = 0;
        for (WO_KI_NB_JI_ClaseDocente c : dao.WO_KI_NB_JI_listarClases()) if (WO_KI_NB_JI_esPrueba(c) && dao.WO_KI_NB_JI_eliminarClase(c.id())) n++;
        return n;
    }

    /** Crea dos cursos de prueba y devuelve cuántos se crearon. */
    public static int WO_KI_NB_JI_crear(WO_KI_NB_JI_ClaseDocenteDAO dao, LocalDate hoy) {
        int creados = 0;
        if (WO_KI_NB_JI_curso(dao, hoy, "Matemática I", "A", 14, 7)) creados++;
        if (WO_KI_NB_JI_curso(dao, hoy, "Comunicación", "B", 11, 21)) creados++;
        return creados;
    }

    private static boolean WO_KI_NB_JI_curso(WO_KI_NB_JI_ClaseDocenteDAO dao, LocalDate hoy, String nombre, String grupo, int alumnos, long semilla) {
        String periodo = hoy.getYear() + "-" + (hoy.getMonthValue() <= 7 ? "I" : "II") + " " + MARCA;
        int id = dao.WO_KI_NB_JI_crearClase(nombre, grupo, periodo, WO_KI_NB_JI_ClaseDocente.NOTA_MINIMA_DEFECTO);
        if (id <= 0) return false;
        Random rnd = new Random(semilla);
        int inicio = (int) (semilla % NOMBRES.length);
        for (int i = 0; i < alumnos; i++) {
            String n = NOMBRES[(inicio + i) % NOMBRES.length];
            String correo = n.toLowerCase(java.util.Locale.ROOT).split(" ")[0]
                .replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u') + "." + (i + 1) + "@correo.edu.pe";
            dao.WO_KI_NB_JI_guardarEstudiante(id, n, correo, null, null);
        }
        List<WO_KI_NB_JI_EstudianteClase> est = dao.WO_KI_NB_JI_listarEstudiantes(id);

        // Asistencia de las últimas 12 clases (días hábiles hasta hoy).
        List<LocalDate> dias = new ArrayList<>();
        for (LocalDate d = hoy; dias.size() < 12; d = d.minusDays(1))
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) dias.add(0, d);
        for (int i = 0; i < est.size(); i++) {
            WO_KI_NB_JI_EstudianteClase e = est.get(i);
            for (int k = 0; k < dias.size(); k++) {
                WO_KI_NB_JI_EstadoAsistencia st = WO_KI_NB_JI_estadoPerfil(i, k, dias.size(), rnd);
                if (st != null) dao.WO_KI_NB_JI_guardarAsistencia(id, e.id(), dias.get(k), st.WO_KI_NB_JI_valor());
            }
        }

        // Planeación: trabajos vencidos, uno próximo y elementos de clase.
        String[][] trabajos = nombre.startsWith("Mat")
            ? new String[][]{{"Tarea", "Práctica 1: ecuaciones lineales", "-20"}, {"Evaluación", "Examen parcial I", "-12"},
                             {"Tarea", "Práctica 2: sistemas de ecuaciones", "-5"}, {"Actividad", "Proyecto: estadística del aula", "6"}}
            : new String[][]{{"Tarea", "Ensayo argumentativo", "-18"}, {"Actividad", "Exposición grupal", "-9"},
                             {"Evaluación", "Control de lectura", "-3"}, {"Tarea", "Reseña crítica", "8"}};
        List<Integer> vencidos = new ArrayList<>();
        for (String[] t : trabajos) {
            int dias0 = Integer.parseInt(t[2]);
            int aid = dao.WO_KI_NB_JI_crearActividadId(id, t[1], t[0], hoy.plusDays(dias0), "Trabajo de prueba generado automáticamente.");
            if (aid > 0 && dias0 <= 0) vencidos.add(aid);
        }
        dao.WO_KI_NB_JI_crearActividad(id, "Sesión de repaso", "Clase", hoy.plusDays(2), "Resolver dudas antes de la evaluación.");
        dao.WO_KI_NB_JI_crearActividad(id, "Separata de la unidad 2", "Material", hoy.minusDays(1), null);

        for (int i = 0; i < est.size(); i++) {
            for (int k = 0; k < vencidos.size(); k++) {
                WO_KI_NB_JI_EstadoEntrega st = WO_KI_NB_JI_entregaPerfil(i, k, rnd);
                Double nota = st == WO_KI_NB_JI_EstadoEntrega.NO_ENTREGADO ? null : WO_KI_NB_JI_notaPerfil(i, rnd);
                dao.WO_KI_NB_JI_guardarEntrega(vencidos.get(k), est.get(i).id(), st, nota);
            }
        }

        dao.WO_KI_NB_JI_crearRecurso(id, WO_KI_NB_JI_RecursoClase.CLASE_EN_VIVO, "Clase virtual (Google Meet)", "https://meet.google.com/", hoy, true);
        dao.WO_KI_NB_JI_crearRecurso(id, WO_KI_NB_JI_RecursoClase.ANUNCIO, "Bienvenidos al curso",
            "Recuerden revisar la separata antes de la sesión de repaso. La evaluación es acumulativa.", hoy.minusDays(3), true);
        dao.WO_KI_NB_JI_crearRecurso(id, WO_KI_NB_JI_RecursoClase.ANUNCIO, "Cambio de aula",
            "La clase del viernes será en el laboratorio 2.", hoy.minusDays(1), false);
        dao.WO_KI_NB_JI_crearRecurso(id, WO_KI_NB_JI_RecursoClase.ENLACE, "Repositorio de materiales", "https://drive.google.com/", hoy.minusDays(6), false);
        return true;
    }

    /**
     * Perfiles para que el semáforo tenga casos de cada tipo:
     * 2 = 3 faltas seguidas, 4 = muchas tardanzas, 6 = dejó de asistir, 8 = asistencia baja.
     */
    private static WO_KI_NB_JI_EstadoAsistencia WO_KI_NB_JI_estadoPerfil(int alumno, int clase, int total, Random rnd) {
        boolean ultimas3 = clase >= total - 3;
        switch (alumno) {
            case 2: if (ultimas3) return WO_KI_NB_JI_EstadoAsistencia.FALTA; break;
            case 4: if (clase % 3 == 0) return WO_KI_NB_JI_EstadoAsistencia.TARDANZA; break;
            case 6: return clase >= total - 4 ? WO_KI_NB_JI_EstadoAsistencia.FALTA : WO_KI_NB_JI_EstadoAsistencia.PRESENTE;
            case 8: if (rnd.nextDouble() < 0.4) return WO_KI_NB_JI_EstadoAsistencia.FALTA; break;
            default: break;
        }
        double r = rnd.nextDouble();
        if (r < 0.86) return WO_KI_NB_JI_EstadoAsistencia.PRESENTE;
        if (r < 0.92) return WO_KI_NB_JI_EstadoAsistencia.TARDANZA;
        if (r < 0.97) return WO_KI_NB_JI_EstadoAsistencia.FALTA;
        return WO_KI_NB_JI_EstadoAsistencia.JUSTIFICADA;
    }

    private static WO_KI_NB_JI_EstadoEntrega WO_KI_NB_JI_entregaPerfil(int alumno, int trabajo, Random rnd) {
        if (alumno == 3 && trabajo >= 1) return WO_KI_NB_JI_EstadoEntrega.NO_ENTREGADO;   // 2 trabajos sin entregar
        if (alumno == 6 && trabajo == 2) return WO_KI_NB_JI_EstadoEntrega.NO_ENTREGADO;
        double r = rnd.nextDouble();
        return r < 0.87 ? WO_KI_NB_JI_EstadoEntrega.ENTREGADO : r < 0.97 ? WO_KI_NB_JI_EstadoEntrega.TARDE : WO_KI_NB_JI_EstadoEntrega.NO_ENTREGADO;
    }

    private static Double WO_KI_NB_JI_notaPerfil(int alumno, Random rnd) {
        if (alumno == 6 || alumno == 8) return (double) (6 + rnd.nextInt(4));    // bajo rendimiento ligado a faltas
        return (double) (11 + rnd.nextInt(9));
    }
}

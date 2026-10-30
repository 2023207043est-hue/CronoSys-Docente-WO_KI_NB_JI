package com.agendapersonal.service;

import com.agendapersonal.domain.model.wkniClaseDocente;
import com.agendapersonal.domain.model.wkniEstadoAsistencia;
import com.agendapersonal.domain.model.wkniEstadoEntrega;
import com.agendapersonal.domain.model.wkniEstudianteClase;
import com.agendapersonal.domain.model.wkniRecursoClase;
import com.agendapersonal.infrastructure.repo.wkniClaseDocenteDAO;

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
public final class wkniDatosPruebaDocente {
    public static final String wkniMARCA = "(prueba)";

    private static final String[] wkniNOMBRES = {
        "Ana Lucía Quispe Mamani", "Bruno Huamán Torres", "Carla Flores Condori", "Diego Ramos Ccama",
        "Elena Vargas Apaza", "Fernando Chávez Poma", "Gabriela Rojas Ticona", "Hugo Mendoza Larico",
        "Isabel Cutipa Paredes", "Jorge Salazar Coaquira", "Karen Pari Choque", "Luis Alberto Zea Ninaja",
        "María José Sánchez Calla", "Nicolás Torres Yupanqui", "Olga Medina Arpasi", "Pedro Castillo Velásquez",
        "Rosa Gutiérrez Vilca", "Samuel Aguilar Machaca", "Tatiana Luque Cruz", "Víctor Pilco Huanca"
    };

    private wkniDatosPruebaDocente() {}

    public static boolean wkniHayDatosPrueba(wkniClaseDocenteDAO wkniDao) {
        return wkniDao.wkniListarClases().stream().anyMatch(wkniDatosPruebaDocente::wkniEsPrueba);
    }

    public static boolean wkniEsPrueba(wkniClaseDocente wkniC) { return wkniC.wkniPeriodo() != null && wkniC.wkniPeriodo().endsWith(wkniMARCA); }

    /** Borra solo los cursos de prueba (sus estudiantes, asistencia y entregas caen en cascada). */
    public static int wkniEliminar(wkniClaseDocenteDAO wkniDao) {
        int wkniN = 0;
        for (wkniClaseDocente wkniC : wkniDao.wkniListarClases()) if (wkniEsPrueba(wkniC) && wkniDao.wkniEliminarClase(wkniC.wkniId())) wkniN++;
        return wkniN;
    }

    /** Crea dos cursos de prueba y devuelve cuántos se crearon. */
    public static int wkniCrear(wkniClaseDocenteDAO wkniDao, LocalDate wkniHoy) {
        int wkniCreados = 0;
        if (wkniCurso(wkniDao, wkniHoy, "Matemática I", "A", 14, 7)) wkniCreados++;
        if (wkniCurso(wkniDao, wkniHoy, "Comunicación", "B", 11, 21)) wkniCreados++;
        return wkniCreados;
    }

    private static boolean wkniCurso(wkniClaseDocenteDAO wkniDao, LocalDate wkniHoy, String wkniNombre, String wkniGrupo, int wkniAlumnos, long wkniSemilla) {
        String wkniPeriodo = wkniHoy.getYear() + "-" + (wkniHoy.getMonthValue() <= 7 ? "I" : "II") + " " + wkniMARCA;
        int wkniId = wkniDao.wkniCrearClase(wkniNombre, wkniGrupo, wkniPeriodo, wkniClaseDocente.wkniNOTA_MINIMA_DEFECTO);
        if (wkniId <= 0) return false;
        Random wkniRnd = new Random(wkniSemilla);
        int wkniInicio = (int) (wkniSemilla % wkniNOMBRES.length);
        for (int wkniI = 0; wkniI < wkniAlumnos; wkniI++) {
            String wkniN = wkniNOMBRES[(wkniInicio + wkniI) % wkniNOMBRES.length];
            String wkniCorreo = wkniN.toLowerCase(java.util.Locale.ROOT).split(" ")[0]
                .replace('á', 'a').replace('é', 'e').replace('í', 'i').replace('ó', 'o').replace('ú', 'u') + "." + (wkniI + 1) + "@correo.edu.pe";
            wkniDao.wkniGuardarEstudiante(wkniId, wkniN, wkniCorreo, null, null);
        }
        List<wkniEstudianteClase> wkniEst = wkniDao.wkniListarEstudiantes(wkniId);

        // Asistencia de las últimas 12 clases (días hábiles hasta hoy).
        List<LocalDate> wkniDias = new ArrayList<>();
        for (LocalDate wkniD = wkniHoy; wkniDias.size() < 12; wkniD = wkniD.minusDays(1))
            if (wkniD.getDayOfWeek() != DayOfWeek.SATURDAY && wkniD.getDayOfWeek() != DayOfWeek.SUNDAY) wkniDias.add(0, wkniD);
        for (int wkniI = 0; wkniI < wkniEst.size(); wkniI++) {
            wkniEstudianteClase wkniE = wkniEst.get(wkniI);
            for (int wkniK = 0; wkniK < wkniDias.size(); wkniK++) {
                wkniEstadoAsistencia wkniSt = wkniEstadoPerfil(wkniI, wkniK, wkniDias.size(), wkniRnd);
                if (wkniSt != null) wkniDao.wkniGuardarAsistencia(wkniId, wkniE.wkniId(), wkniDias.get(wkniK), wkniSt.wkniValor());
            }
        }

        // Planeación: trabajos vencidos, uno próximo y elementos de clase.
        String[][] wkniTrabajos = wkniNombre.startsWith("Mat")
            ? new String[][]{{"Tarea", "Práctica 1: ecuaciones lineales", "-20"}, {"Evaluación", "Examen parcial I", "-12"},
                             {"Tarea", "Práctica 2: sistemas de ecuaciones", "-5"}, {"Actividad", "Proyecto: estadística del aula", "6"}}
            : new String[][]{{"Tarea", "Ensayo argumentativo", "-18"}, {"Actividad", "Exposición grupal", "-9"},
                             {"Evaluación", "Control de lectura", "-3"}, {"Tarea", "Reseña crítica", "8"}};
        List<Integer> wkniVencidos = new ArrayList<>();
        for (String[] wkniT : wkniTrabajos) {
            int wkniDias0 = Integer.parseInt(wkniT[2]);
            int wkniAid = wkniDao.wkniCrearActividadId(wkniId, wkniT[1], wkniT[0], wkniHoy.plusDays(wkniDias0), "Trabajo de prueba generado automáticamente.");
            if (wkniAid > 0 && wkniDias0 <= 0) wkniVencidos.add(wkniAid);
        }
        wkniDao.wkniCrearActividad(wkniId, "Sesión de repaso", "Clase", wkniHoy.plusDays(2), "Resolver dudas antes de la evaluación.");
        wkniDao.wkniCrearActividad(wkniId, "Separata de la unidad 2", "Material", wkniHoy.minusDays(1), null);

        for (int wkniI = 0; wkniI < wkniEst.size(); wkniI++) {
            for (int wkniK = 0; wkniK < wkniVencidos.size(); wkniK++) {
                wkniEstadoEntrega wkniSt = wkniEntregaPerfil(wkniI, wkniK, wkniRnd);
                Double wkni_nota = wkniSt == wkniEstadoEntrega.NO_ENTREGADO ? null : wkniNotaPerfil(wkniI, wkniRnd);
                wkniDao.wkniGuardarEntrega(wkniVencidos.get(wkniK), wkniEst.get(wkniI).wkniId(), wkniSt, wkni_nota);
            }
        }

        wkniDao.wkniCrearRecurso(wkniId, wkniRecursoClase.wkniCLASE_EN_VIVO, "Clase virtual (Google Meet)", "https://meet.google.com/", wkniHoy, true);
        wkniDao.wkniCrearRecurso(wkniId, wkniRecursoClase.wkniANUNCIO, "Bienvenidos al curso",
            "Recuerden revisar la separata antes de la sesión de repaso. La evaluación es acumulativa.", wkniHoy.minusDays(3), true);
        wkniDao.wkniCrearRecurso(wkniId, wkniRecursoClase.wkniANUNCIO, "Cambio de aula",
            "La clase del viernes será en el laboratorio 2.", wkniHoy.minusDays(1), false);
        wkniDao.wkniCrearRecurso(wkniId, wkniRecursoClase.wkniENLACE, "Repositorio de materiales", "https://drive.google.com/", wkniHoy.minusDays(6), false);
        return true;
    }

    /**
     * Perfiles para que el semáforo tenga casos de cada tipo:
     * 2 = 3 faltas seguidas, 4 = muchas tardanzas, 6 = dejó de asistir, 8 = asistencia baja.
     */
    private static wkniEstadoAsistencia wkniEstadoPerfil(int wkniAlumno, int wkniClase, int wkniTotal, Random wkniRnd) {
        boolean wkniUltimas3 = wkniClase >= wkniTotal - 3;
        switch (wkniAlumno) {
            case 2: if (wkniUltimas3) return wkniEstadoAsistencia.FALTA; break;
            case 4: if (wkniClase % 3 == 0) return wkniEstadoAsistencia.TARDANZA; break;
            case 6: return wkniClase >= wkniTotal - 4 ? wkniEstadoAsistencia.FALTA : wkniEstadoAsistencia.PRESENTE;
            case 8: if (wkniRnd.nextDouble() < 0.4) return wkniEstadoAsistencia.FALTA; break;
            default: break;
        }
        double wkniR = wkniRnd.nextDouble();
        if (wkniR < 0.86) return wkniEstadoAsistencia.PRESENTE;
        if (wkniR < 0.92) return wkniEstadoAsistencia.TARDANZA;
        if (wkniR < 0.97) return wkniEstadoAsistencia.FALTA;
        return wkniEstadoAsistencia.JUSTIFICADA;
    }

    private static wkniEstadoEntrega wkniEntregaPerfil(int wkniAlumno, int wkni_trabajo, Random wkniRnd) {
        if (wkniAlumno == 3 && wkni_trabajo >= 1) return wkniEstadoEntrega.NO_ENTREGADO;   // 2 trabajos sin entregar
        if (wkniAlumno == 6 && wkni_trabajo == 2) return wkniEstadoEntrega.NO_ENTREGADO;
        double wkniR = wkniRnd.nextDouble();
        return wkniR < 0.87 ? wkniEstadoEntrega.ENTREGADO : wkniR < 0.97 ? wkniEstadoEntrega.TARDE : wkniEstadoEntrega.NO_ENTREGADO;
    }

    private static Double wkniNotaPerfil(int wkniAlumno, Random wkniRnd) {
        if (wkniAlumno == 6 || wkniAlumno == 8) return (double) (6 + wkniRnd.nextInt(4));    // bajo rendimiento ligado a faltas
        return (double) (11 + wkniRnd.nextInt(9));
    }
}

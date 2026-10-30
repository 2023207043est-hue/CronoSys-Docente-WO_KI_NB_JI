package com.agendapersonal.ui.modules.docente;

import com.agendapersonal.domain.model.wkniClaseDocente;
import com.agendapersonal.infrastructure.repo.wkniClaseDocenteDAO;
import com.agendapersonal.service.wkniResumenCurso;

import java.time.LocalDate;

/**
 * Lo que comparten los paneles de un curso: el curso, el acceso a datos y el aviso de "algo cambió"
 * (para que el panel con gráficas, las alertas y las cifras del encabezado se actualicen solos).
 */
public record wkniCursoContexto(wkniClaseDocente wkniClase, wkniClaseDocenteDAO wkniDao, Runnable wkniAlCambiar) {
    /** Recalcula la foto del curso (asistencia, entregas y riesgo) con los datos guardados. */
    public wkniResumenCurso wkniResumen() { return new wkniResumenCurso(wkniClase, wkniDao, LocalDate.now()); }

    public void wkniCambio() { if (wkniAlCambiar != null) wkniAlCambiar.run(); }
}

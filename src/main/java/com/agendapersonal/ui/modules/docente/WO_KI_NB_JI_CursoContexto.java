package com.agendapersonal.ui.modules.docente;

import com.agendapersonal.domain.model.WO_KI_NB_JI_ClaseDocente;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_ClaseDocenteDAO;
import com.agendapersonal.service.WO_KI_NB_JI_ResumenCurso;

import java.time.LocalDate;

/**
 * Lo que comparten los paneles de un curso: el curso, el acceso a datos y el aviso de "algo cambió"
 * (para que el panel con gráficas, las alertas y las cifras del encabezado se actualicen solos).
 */
public record WO_KI_NB_JI_CursoContexto(WO_KI_NB_JI_ClaseDocente clase, WO_KI_NB_JI_ClaseDocenteDAO dao, Runnable alCambiar) {
    /** Recalcula la foto del curso (asistencia, entregas y riesgo) con los datos guardados. */
    public WO_KI_NB_JI_ResumenCurso WO_KI_NB_JI_resumen() { return new WO_KI_NB_JI_ResumenCurso(clase, dao, LocalDate.now()); }

    public void WO_KI_NB_JI_cambio() { if (alCambiar != null) alCambiar.run(); }
}

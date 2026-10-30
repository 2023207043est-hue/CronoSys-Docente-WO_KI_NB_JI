package com.agendapersonal.controller;

import com.agendapersonal.dao.*;
import com.agendapersonal.domain.model.wkniProyecto;
import com.agendapersonal.infrastructure.repo.wkniProyectoDAO;
import com.agendapersonal.model.*;
import com.agendapersonal.service.wkniAgendaService;
import com.agendapersonal.service.wkniConflictoHorario;
import com.agendapersonal.service.wkniProgresoService;
import javafx.application.Platform;
import javafx.collections.*;
import java.time.LocalDate;
import java.util.List;

public final class wkniMainController {
    private final wkniEventoDAO wkni_eventoDAO=new wkniEventoDAO();
    private final wkniTareaDAO wkni_tareaDAO=new wkniTareaDAO();
    private final wkniProyectoDAO wkni_proyectoDAO=new wkniProyectoDAO();
    private final ObservableList<wkniEvento> wkniEventos=FXCollections.observableArrayList();
    private final ObservableList<wkniTarea> wkniTareas=FXCollections.observableArrayList();

    public wkniMainController(){wkniCargarTodo();}
    public void wkniCargarTodo(){wkniEventos.setAll(wkni_eventoDAO.wkniListarTodos()); wkniTareas.setAll(wkni_tareaDAO.wkniListarTodas());}

    /** Equivalente a AJAX: recarga SQLite en hilo de fondo sin congelar la UI. */
    public void wkniCargarTodoAsync() { wkniCargarTodoAsync(null); }

    public void wkniCargarTodoAsync(Runnable wkniOnDone) {
        Thread wkniT = new Thread(() -> {
            List<wkniEvento> wkniEv = wkni_eventoDAO.wkniListarTodos();
            List<wkniTarea> wkniTa = wkni_tareaDAO.wkniListarTodas();
            Platform.runLater(() -> {
                // Solo se reemplazan las listas si algo cambió de verdad: antes se recargaban cada 15 s
                // y las tablas perdían la selección y el desplazamiento aunque nada hubiera cambiado.
                boolean wkniCambioEventos = !wkniFirmaEventos(wkniEv).equals(wkniFirmaEventos(wkniEventos));
                boolean wkniCambioTareas = !wkniFirmaTareas(wkniTa).equals(wkniFirmaTareas(wkniTareas));
                if (wkniCambioEventos) wkniEventos.setAll(wkniEv);
                if (wkniCambioTareas) wkniTareas.setAll(wkniTa);
                if ((wkniCambioEventos || wkniCambioTareas) && wkniAlCambioExterno != null) wkniAlCambioExterno.run();
                if (wkniOnDone != null) wkniOnDone.run();
            });
        }, "db-refresh");
        wkniT.setDaemon(true);
        wkniT.start();
    }

    private Runnable wkniAlCambioExterno;

    /** Se ejecuta cuando la recarga periódica encuentra cambios hechos fuera de esta ventana. */
    public void wkniSetAlCambioExterno(Runnable wkniR) { wkniAlCambioExterno = wkniR; }

    private static List<String> wkniFirmaEventos(List<wkniEvento> wkniL) {
        return wkniL.stream().map(wkniE -> wkniE.wkniGetId() + "|" + wkniE.wkniGetTitulo() + "|" + wkniE.wkniGetFecha() + "|" + wkniE.wkniGetHoraInicio()
            + "|" + wkniE.wkniGetHoraFin() + "|" + wkniE.wkniGetCategoria() + "|" + wkniE.wkniGetPrioridad() + "|" + wkniE.wkniGetDescripcion()
            + "|" + wkniE.wkniGetCalendarioId()).toList();
    }

    private static List<String> wkniFirmaTareas(List<wkniTarea> wkniL) {
        return wkniL.stream().map(wkniT -> wkniT.wkniGetId() + "|" + wkniT.wkniGetTitulo() + "|" + wkniT.wkniGetEstado() + "|" + wkniT.wkniGetFecha()
            + "|" + wkniT.wkniGetPrioridad() + "|" + wkniT.wkniGetProyectoId() + "|" + wkniT.wkniGetDescripcion()).toList();
    }

    public boolean wkniGuardarEvento(wkniEvento wkniE){boolean wkniOk=wkni_eventoDAO.wkniInsertar(wkniE);if(wkniOk)wkniCargarTodo();return wkniOk;}
    public boolean wkniActualizarEvento(wkniEvento wkniE){boolean wkniOk=wkni_eventoDAO.wkniActualizar(wkniE);if(wkniOk)wkniCargarTodo();return wkniOk;}
    public boolean wkniEliminarEvento(int wkniId){boolean wkniOk=wkni_eventoDAO.wkniEliminar(wkniId);if(wkniOk)wkniCargarTodo();return wkniOk;}
    public List<wkniEvento> wkniEventosDelDia(LocalDate wkniF){return wkni_eventoDAO.wkniListarPorFecha(wkniF);}
    public ObservableList<wkniEvento> wkniGetEventos(){return wkniEventos;}

    public boolean wkniGuardarTarea(wkniTarea wkniT){boolean wkniOk=wkni_tareaDAO.wkniInsertar(wkniT);if(wkniOk)wkniCargarTodo();return wkniOk;}
    public boolean wkniActualizarTarea(wkniTarea wkniT){boolean wkniOk=wkni_tareaDAO.wkniActualizar(wkniT);if(wkniOk)wkniCargarTodo();return wkniOk;}
    public boolean wkniEliminarTarea(int wkniId){boolean wkniOk=wkni_tareaDAO.wkniEliminar(wkniId);if(wkniOk)wkniCargarTodo();return wkniOk;}
    public ObservableList<wkniTarea> wkniGetTareas(){return wkniTareas;}

    // ---- Reglas de negocio expuestas a la UI (la lógica vive en service/) ----

    /** Cruces del evento con la agenda actual (vacío si no hay conflicto). */
    public List<wkniConflictoHorario> wkniConflictosPara(wkniEvento wkniE){
        return wkniAgendaService.wkniDetectarConflictos(wkniE, wkniEventos);
    }

    /** Primer hueco libre de {@code minutos} ese día entre las 07:00 y las 22:00. */
    public java.util.Optional<java.time.LocalTime[]> wkniHuecoLibre(LocalDate wkniDia, int wkniMinutos){
        return wkniAgendaService.wkniSugerirHueco(wkniDia, wkniEventos, wkniMinutos,
            java.time.LocalTime.of(7,0), java.time.LocalTime.of(22,0));
    }

    /**
     * Cambia el estado de una tarea, guarda y devuelve el avance antes → después
     * (del proyecto de la tarea, o global si no tiene proyecto). Vacío si no se pudo guardar.
     */
    public java.util.Optional<wkniProgresoService.wkniResultado> wkniCambiarEstadoTarea(wkniTarea wkniT, wkniEstadoTarea wkniNuevo){
        Integer wkniPid=wkniT.wkniGetProyectoId();
        int wkniAntes=wkniProgresoService.wkniPorcentaje(wkniPid,wkniTareas);
        wkniEstadoTarea wkniPrevio=wkniT.wkniGetEstadoTipo();
        wkniT.wkniSetEstado(wkniNuevo);
        if(!wkni_tareaDAO.wkniActualizar(wkniT)){ wkniT.wkniSetEstado(wkniPrevio); return java.util.Optional.empty(); }
        wkniCargarTodo();
        int wkniDespues=wkniProgresoService.wkniPorcentaje(wkniPid,wkniTareas);
        return java.util.Optional.of(new wkniProgresoService.wkniResultado(wkniPid,wkniAntes,wkniDespues,wkniNuevo.wkniEsCompletada()));
    }

    /** Alterna Completada ↔ Pendiente. */
    public java.util.Optional<wkniProgresoService.wkniResultado> wkniAlternarCompletada(wkniTarea wkniT){
        return wkniCambiarEstadoTarea(wkniT, wkniT.wkniEstaCompletada()?wkniEstadoTarea.PENDIENTE:wkniEstadoTarea.COMPLETADA);
    }

    /** Texto del toast de progreso, con el nombre del proyecto si lo tiene. */
    public String wkniMensajeProgreso(wkniProgresoService.wkniResultado wkniR){
        String wkniNombre=null;
        if(wkniR.wkniProyectoId()!=null)
            for(wkniProyecto wkni_p:wkni_proyectoDAO.wkniListar()) if(wkni_p.wkniGetId()==wkniR.wkniProyectoId()) wkniNombre=wkni_p.wkniGetNombre();
        return wkniR.wkniMensaje(wkniNombre);
    }

    public List<wkniProyecto> wkniGetProyectos(){return wkni_proyectoDAO.wkniListar();}
}

package com.agendapersonal.controller;

import com.agendapersonal.dao.*;
import com.agendapersonal.domain.model.WO_KI_NB_JI_Proyecto;
import com.agendapersonal.infrastructure.repo.WO_KI_NB_JI_ProyectoDAO;
import com.agendapersonal.model.*;
import com.agendapersonal.service.WO_KI_NB_JI_AgendaService;
import com.agendapersonal.service.WO_KI_NB_JI_ConflictoHorario;
import com.agendapersonal.service.WO_KI_NB_JI_ProgresoService;
import javafx.application.Platform;
import javafx.collections.*;
import java.time.LocalDate;
import java.util.List;

public final class WO_KI_NB_JI_MainController {
    private final WO_KI_NB_JI_EventoDAO eventoDAO=new WO_KI_NB_JI_EventoDAO();
    private final WO_KI_NB_JI_TareaDAO tareaDAO=new WO_KI_NB_JI_TareaDAO();
    private final WO_KI_NB_JI_ProyectoDAO proyectoDAO=new WO_KI_NB_JI_ProyectoDAO();
    private final ObservableList<WO_KI_NB_JI_Evento> eventos=FXCollections.observableArrayList();
    private final ObservableList<WO_KI_NB_JI_Tarea> tareas=FXCollections.observableArrayList();

    public WO_KI_NB_JI_MainController(){WO_KI_NB_JI_cargarTodo();}
    public void WO_KI_NB_JI_cargarTodo(){eventos.setAll(eventoDAO.WO_KI_NB_JI_listarTodos()); tareas.setAll(tareaDAO.WO_KI_NB_JI_listarTodas());}

    /** Equivalente a AJAX: recarga SQLite en hilo de fondo sin congelar la UI. */
    public void WO_KI_NB_JI_cargarTodoAsync() { WO_KI_NB_JI_cargarTodoAsync(null); }

    public void WO_KI_NB_JI_cargarTodoAsync(Runnable onDone) {
        Thread t = new Thread(() -> {
            List<WO_KI_NB_JI_Evento> ev = eventoDAO.WO_KI_NB_JI_listarTodos();
            List<WO_KI_NB_JI_Tarea> ta = tareaDAO.WO_KI_NB_JI_listarTodas();
            Platform.runLater(() -> {
                // Solo se reemplazan las listas si algo cambió de verdad: antes se recargaban cada 15 s
                // y las tablas perdían la selección y el desplazamiento aunque nada hubiera cambiado.
                boolean cambioEventos = !WO_KI_NB_JI_firmaEventos(ev).equals(WO_KI_NB_JI_firmaEventos(eventos));
                boolean cambioTareas = !WO_KI_NB_JI_firmaTareas(ta).equals(WO_KI_NB_JI_firmaTareas(tareas));
                if (cambioEventos) eventos.setAll(ev);
                if (cambioTareas) tareas.setAll(ta);
                if ((cambioEventos || cambioTareas) && alCambioExterno != null) alCambioExterno.run();
                if (onDone != null) onDone.run();
            });
        }, "db-refresh");
        t.setDaemon(true);
        t.start();
    }

    private Runnable alCambioExterno;

    /** Se ejecuta cuando la recarga periódica encuentra cambios hechos fuera de esta ventana. */
    public void WO_KI_NB_JI_setAlCambioExterno(Runnable r) { alCambioExterno = r; }

    private static List<String> WO_KI_NB_JI_firmaEventos(List<WO_KI_NB_JI_Evento> l) {
        return l.stream().map(e -> e.WO_KI_NB_JI_getId() + "|" + e.WO_KI_NB_JI_getTitulo() + "|" + e.WO_KI_NB_JI_getFecha() + "|" + e.WO_KI_NB_JI_getHoraInicio()
            + "|" + e.WO_KI_NB_JI_getHoraFin() + "|" + e.WO_KI_NB_JI_getCategoria() + "|" + e.WO_KI_NB_JI_getPrioridad() + "|" + e.WO_KI_NB_JI_getDescripcion()
            + "|" + e.WO_KI_NB_JI_getCalendarioId()).toList();
    }

    private static List<String> WO_KI_NB_JI_firmaTareas(List<WO_KI_NB_JI_Tarea> l) {
        return l.stream().map(t -> t.WO_KI_NB_JI_getId() + "|" + t.WO_KI_NB_JI_getTitulo() + "|" + t.WO_KI_NB_JI_getEstado() + "|" + t.WO_KI_NB_JI_getFecha()
            + "|" + t.WO_KI_NB_JI_getPrioridad() + "|" + t.WO_KI_NB_JI_getProyectoId() + "|" + t.WO_KI_NB_JI_getDescripcion()).toList();
    }

    public boolean WO_KI_NB_JI_guardarEvento(WO_KI_NB_JI_Evento e){boolean ok=eventoDAO.WO_KI_NB_JI_insertar(e);if(ok)WO_KI_NB_JI_cargarTodo();return ok;}
    public boolean WO_KI_NB_JI_actualizarEvento(WO_KI_NB_JI_Evento e){boolean ok=eventoDAO.WO_KI_NB_JI_actualizar(e);if(ok)WO_KI_NB_JI_cargarTodo();return ok;}
    public boolean WO_KI_NB_JI_eliminarEvento(int id){boolean ok=eventoDAO.WO_KI_NB_JI_eliminar(id);if(ok)WO_KI_NB_JI_cargarTodo();return ok;}
    public List<WO_KI_NB_JI_Evento> WO_KI_NB_JI_eventosDelDia(LocalDate f){return eventoDAO.WO_KI_NB_JI_listarPorFecha(f);}
    public ObservableList<WO_KI_NB_JI_Evento> WO_KI_NB_JI_getEventos(){return eventos;}

    public boolean WO_KI_NB_JI_guardarTarea(WO_KI_NB_JI_Tarea t){boolean ok=tareaDAO.WO_KI_NB_JI_insertar(t);if(ok)WO_KI_NB_JI_cargarTodo();return ok;}
    public boolean WO_KI_NB_JI_actualizarTarea(WO_KI_NB_JI_Tarea t){boolean ok=tareaDAO.WO_KI_NB_JI_actualizar(t);if(ok)WO_KI_NB_JI_cargarTodo();return ok;}
    public boolean WO_KI_NB_JI_eliminarTarea(int id){boolean ok=tareaDAO.WO_KI_NB_JI_eliminar(id);if(ok)WO_KI_NB_JI_cargarTodo();return ok;}
    public ObservableList<WO_KI_NB_JI_Tarea> WO_KI_NB_JI_getTareas(){return tareas;}

    // ---- Reglas de negocio expuestas a la UI (la lógica vive en service/) ----

    /** Cruces del evento con la agenda actual (vacío si no hay conflicto). */
    public List<WO_KI_NB_JI_ConflictoHorario> WO_KI_NB_JI_conflictosPara(WO_KI_NB_JI_Evento e){
        return WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_detectarConflictos(e, eventos);
    }

    /** Primer hueco libre de {@code minutos} ese día entre las 07:00 y las 22:00. */
    public java.util.Optional<java.time.LocalTime[]> WO_KI_NB_JI_huecoLibre(LocalDate dia, int minutos){
        return WO_KI_NB_JI_AgendaService.WO_KI_NB_JI_sugerirHueco(dia, eventos, minutos,
            java.time.LocalTime.of(7,0), java.time.LocalTime.of(22,0));
    }

    /**
     * Cambia el estado de una tarea, guarda y devuelve el avance antes → después
     * (del proyecto de la tarea, o global si no tiene proyecto). Vacío si no se pudo guardar.
     */
    public java.util.Optional<WO_KI_NB_JI_ProgresoService.WO_KI_NB_JI_Resultado> WO_KI_NB_JI_cambiarEstadoTarea(WO_KI_NB_JI_Tarea t, WO_KI_NB_JI_EstadoTarea nuevo){
        Integer pid=t.WO_KI_NB_JI_getProyectoId();
        int antes=WO_KI_NB_JI_ProgresoService.WO_KI_NB_JI_porcentaje(pid,tareas);
        WO_KI_NB_JI_EstadoTarea previo=t.WO_KI_NB_JI_getEstadoTipo();
        t.WO_KI_NB_JI_setEstado(nuevo);
        if(!tareaDAO.WO_KI_NB_JI_actualizar(t)){ t.WO_KI_NB_JI_setEstado(previo); return java.util.Optional.empty(); }
        WO_KI_NB_JI_cargarTodo();
        int despues=WO_KI_NB_JI_ProgresoService.WO_KI_NB_JI_porcentaje(pid,tareas);
        return java.util.Optional.of(new WO_KI_NB_JI_ProgresoService.WO_KI_NB_JI_Resultado(pid,antes,despues,nuevo.WO_KI_NB_JI_esCompletada()));
    }

    /** Alterna Completada ↔ Pendiente. */
    public java.util.Optional<WO_KI_NB_JI_ProgresoService.WO_KI_NB_JI_Resultado> WO_KI_NB_JI_alternarCompletada(WO_KI_NB_JI_Tarea t){
        return WO_KI_NB_JI_cambiarEstadoTarea(t, t.WO_KI_NB_JI_estaCompletada()?WO_KI_NB_JI_EstadoTarea.PENDIENTE:WO_KI_NB_JI_EstadoTarea.COMPLETADA);
    }

    /** Texto del toast de progreso, con el nombre del proyecto si lo tiene. */
    public String WO_KI_NB_JI_mensajeProgreso(WO_KI_NB_JI_ProgresoService.WO_KI_NB_JI_Resultado r){
        String nombre=null;
        if(r.proyectoId()!=null)
            for(WO_KI_NB_JI_Proyecto p:proyectoDAO.WO_KI_NB_JI_listar()) if(p.WO_KI_NB_JI_getId()==r.proyectoId()) nombre=p.WO_KI_NB_JI_getNombre();
        return r.WO_KI_NB_JI_mensaje(nombre);
    }

    public List<WO_KI_NB_JI_Proyecto> WO_KI_NB_JI_getProyectos(){return proyectoDAO.WO_KI_NB_JI_listar();}
}

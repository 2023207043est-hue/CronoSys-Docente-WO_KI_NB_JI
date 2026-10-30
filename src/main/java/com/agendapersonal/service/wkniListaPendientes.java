package com.agendapersonal.service;

import com.agendapersonal.domain.model.wkniNota;
import com.agendapersonal.model.wkniTarea;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lista de pendientes que se muestra en la pantalla de enfoque.
 *
 * <p>Puede venir de dos sitios: de las <b>tareas</b> de la agenda o de las <b>notas</b>. De las notas se
 * sacan las líneas que parecen casillas: {@code - [ ] algo}, {@code [ ] algo}, {@code ☐ algo} o
 * {@code - [x] algo} (hecha). También se puede marcar una nota entera como pendiente con la
 * etiqueta {@code pendiente} o {@code todo}.</p>
 *
 * <p>Sin interfaz: solo lógica, para poder probarla.</p>
 */
public final class wkniListaPendientes {

    /** Una casilla pendiente. */
    public record wkniItem(int wkniNotaId, int wkniLinea, String wkniTexto, boolean wkniHecho, String wkniOrigen) {
        /** Identificador estable para marcar/desmarcar. */
        public String wkniClave() { return wkniOrigen + ":" + wkniNotaId + ":" + wkniLinea; }
    }

    /** Casillas de una nota: "- [ ] texto", "[x] texto", "☐ texto"… */
    private static final Pattern wkniCASILLA = Pattern.compile(
        "^(?<prefijo>\\s*(?:[-*•‣]\\s*)?)(?<marca>\\[[ xX]\\]|☐|☑)\\s*(?<texto>.*?)\\s*$");

    private wkniListaPendientes() {}

    // ------------------------------------------------------------------ desde notas

    /** Todos los pendientes que aparecen en las notas dadas (hechos y por hacer). */
    public static List<wkniItem> wkniDesdeNotas(List<wkniNota> wkniNotas) {
        List<wkniItem> wkniSalida = new ArrayList<>();
        if (wkniNotas == null) return wkniSalida;
        for (wkniNota wkniN : wkniNotas) {
            String wkniContenido = wkniN.wkniGetContenido();
            if (wkniContenido == null || wkniContenido.isBlank()) continue;
            boolean wkniMarcadaComoPendiente = wkniTieneEtiquetaDePendiente(wkniN);
            String[] wkniLineas = wkniContenido.split("\r?\n", -1);
            boolean wkniEncontroCasilla = false;
            for (int wkniI = 0; wkniI < wkniLineas.length; wkniI++) {
                Matcher wkniM = wkniCASILLA.matcher(wkniLineas[wkniI]);
                if (!wkniM.matches()) continue;
                wkniEncontroCasilla = true;
                String wkniTexto = wkniM.group("texto").trim();
                if (wkniTexto.isEmpty()) continue;
                wkniSalida.add(new wkniItem(wkniN.wkniGetId(), wkniI, wkniTexto, wkniHecha(wkniM.group("marca")), "nota"));
            }
            // Una nota con la etiqueta "pendiente" y sin casillas también cuenta como un pendiente.
            if (wkniMarcadaComoPendiente && !wkniEncontroCasilla) {
                String wkniTitulo = wkniN.wkniGetTitulo() == null ? "" : wkniN.wkniGetTitulo().trim();
                if (!wkniTitulo.isEmpty()) wkniSalida.add(new wkniItem(wkniN.wkniGetId(), -1, wkniTitulo, false, "nota"));
            }
        }
        return wkniSalida;
    }

    /** ¿La nota tiene la etiqueta "pendiente" o "todo"? */
    public static boolean wkniTieneEtiquetaDePendiente(wkniNota wkniN) {
        if (wkniN.wkniGetEtiquetas() == null) return false;
        for (String wkniE : wkniN.wkniGetEtiquetas().split(",")) {
            String wkniT = wkniE.trim().toLowerCase(java.util.Locale.ROOT);
            if (wkniT.equals("pendiente") || wkniT.equals("pendientes") || wkniT.equals("todo") || wkniT.equals("to-do")) return true;
        }
        return false;
    }

    /**
     * Devuelve el contenido de la nota con la casilla del pendiente marcada o desmarcada.
     * Si el pendiente venía de la nota entera (linea -1), no se toca el contenido.
     */
    public static String wkniAlternar(wkniNota wkni_nota, wkniItem wkni_item, boolean wkniHecho) {
        String wkniContenido = wkni_nota.wkniGetContenido();
        if (wkniContenido == null || wkni_item.wkniLinea() < 0) return wkniContenido;
        String[] wkniLineas = wkniContenido.split("\r?\n", -1);
        if (wkni_item.wkniLinea() >= wkniLineas.length) return wkniContenido;
        Matcher wkniM = wkniCASILLA.matcher(wkniLineas[wkni_item.wkniLinea()]);
        if (!wkniM.matches()) return wkniContenido;
        String wkniMarca = wkniM.group("marca");
        // Se respeta el estilo original de la casilla: "[ ]"/"[x]" o "☐"/"☑".
        String wkniNueva = wkniMarca.startsWith("[")
            ? (wkniHecho ? "[x]" : "[ ]")
            : (wkniHecho ? "☑" : "☐");
        wkniLineas[wkni_item.wkniLinea()] = wkniM.group("prefijo") + wkniNueva + " " + wkniM.group("texto");
        return String.join("\n", wkniLineas);
    }

    /** ¿La marca de casilla corresponde a algo ya hecho? */
    private static boolean wkniHecha(String wkniMarca) {
        return "[x]".equalsIgnoreCase(wkniMarca) || "[X]".equals(wkniMarca) || "☑".equals(wkniMarca);
    }

    // ------------------------------------------------------------------ desde tareas

    /** Las tareas pendientes (y las de hoy) sirven también como lista de pendientes. */
    public static List<wkniItem> wkniDesdeTareas(List<wkniTarea> wkniTareas) {
        List<wkniItem> wkniSalida = new ArrayList<>();
        if (wkniTareas == null) return wkniSalida;
        for (wkniTarea wkniT : wkniTareas) {
            if (wkniT.wkniGetEstadoTipo() == null || wkniT.wkniGetEstadoTipo().wkniEsCompletada()) continue;
            wkniSalida.add(new wkniItem(wkniT.wkniGetId(), -1, wkniT.wkniGetTitulo(), false, "tarea"));
        }
        return wkniSalida;
    }

    // ------------------------------------------------------------------ utilidades

    /** Quita las marcas de casilla de un texto para mostrarlo limpio. */
    public static String wkniLimpiar(String wkniTexto) {
        if (wkniTexto == null) return "";
        Matcher wkniM = wkniCASILLA.matcher(wkniTexto.trim());
        return wkniM.matches() ? wkniM.group("texto").trim() : wkniTexto.trim();
    }
}

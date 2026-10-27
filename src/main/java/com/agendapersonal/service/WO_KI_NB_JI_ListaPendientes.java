package com.agendapersonal.service;

import com.agendapersonal.domain.model.WO_KI_NB_JI_Nota;
import com.agendapersonal.model.WO_KI_NB_JI_Tarea;

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
public final class WO_KI_NB_JI_ListaPendientes {

    /** Una casilla pendiente. */
    public record WO_KI_NB_JI_Item(int notaId, int linea, String texto, boolean hecho, String origen) {
        /** Identificador estable para marcar/desmarcar. */
        public String WO_KI_NB_JI_clave() { return origen + ":" + notaId + ":" + linea; }
    }

    /** Casillas de una nota: "- [ ] texto", "[x] texto", "☐ texto"… */
    private static final Pattern CASILLA = Pattern.compile(
        "^(?<prefijo>\\s*(?:[-*•‣]\\s*)?)(?<marca>\\[[ xX]\\]|☐|☑)\\s*(?<texto>.*?)\\s*$");

    private WO_KI_NB_JI_ListaPendientes() {}

    // ------------------------------------------------------------------ desde notas

    /** Todos los pendientes que aparecen en las notas dadas (hechos y por hacer). */
    public static List<WO_KI_NB_JI_Item> WO_KI_NB_JI_desdeNotas(List<WO_KI_NB_JI_Nota> notas) {
        List<WO_KI_NB_JI_Item> salida = new ArrayList<>();
        if (notas == null) return salida;
        for (WO_KI_NB_JI_Nota n : notas) {
            String contenido = n.WO_KI_NB_JI_getContenido();
            if (contenido == null || contenido.isBlank()) continue;
            boolean marcadaComoPendiente = WO_KI_NB_JI_tieneEtiquetaDePendiente(n);
            String[] lineas = contenido.split("\r?\n", -1);
            boolean encontroCasilla = false;
            for (int i = 0; i < lineas.length; i++) {
                Matcher m = CASILLA.matcher(lineas[i]);
                if (!m.matches()) continue;
                encontroCasilla = true;
                String texto = m.group("texto").trim();
                if (texto.isEmpty()) continue;
                salida.add(new WO_KI_NB_JI_Item(n.WO_KI_NB_JI_getId(), i, texto, WO_KI_NB_JI_hecha(m.group("marca")), "nota"));
            }
            // Una nota con la etiqueta "pendiente" y sin casillas también cuenta como un pendiente.
            if (marcadaComoPendiente && !encontroCasilla) {
                String titulo = n.WO_KI_NB_JI_getTitulo() == null ? "" : n.WO_KI_NB_JI_getTitulo().trim();
                if (!titulo.isEmpty()) salida.add(new WO_KI_NB_JI_Item(n.WO_KI_NB_JI_getId(), -1, titulo, false, "nota"));
            }
        }
        return salida;
    }

    /** ¿La nota tiene la etiqueta "pendiente" o "todo"? */
    public static boolean WO_KI_NB_JI_tieneEtiquetaDePendiente(WO_KI_NB_JI_Nota n) {
        if (n.WO_KI_NB_JI_getEtiquetas() == null) return false;
        for (String e : n.WO_KI_NB_JI_getEtiquetas().split(",")) {
            String t = e.trim().toLowerCase(java.util.Locale.ROOT);
            if (t.equals("pendiente") || t.equals("pendientes") || t.equals("todo") || t.equals("to-do")) return true;
        }
        return false;
    }

    /**
     * Devuelve el contenido de la nota con la casilla del pendiente marcada o desmarcada.
     * Si el pendiente venía de la nota entera (linea -1), no se toca el contenido.
     */
    public static String WO_KI_NB_JI_alternar(WO_KI_NB_JI_Nota nota, WO_KI_NB_JI_Item item, boolean hecho) {
        String contenido = nota.WO_KI_NB_JI_getContenido();
        if (contenido == null || item.linea() < 0) return contenido;
        String[] lineas = contenido.split("\r?\n", -1);
        if (item.linea() >= lineas.length) return contenido;
        Matcher m = CASILLA.matcher(lineas[item.linea()]);
        if (!m.matches()) return contenido;
        String marca = m.group("marca");
        // Se respeta el estilo original de la casilla: "[ ]"/"[x]" o "☐"/"☑".
        String nueva = marca.startsWith("[")
            ? (hecho ? "[x]" : "[ ]")
            : (hecho ? "☑" : "☐");
        lineas[item.linea()] = m.group("prefijo") + nueva + " " + m.group("texto");
        return String.join("\n", lineas);
    }

    /** ¿La marca de casilla corresponde a algo ya hecho? */
    private static boolean WO_KI_NB_JI_hecha(String marca) {
        return "[x]".equalsIgnoreCase(marca) || "[X]".equals(marca) || "☑".equals(marca);
    }

    // ------------------------------------------------------------------ desde tareas

    /** Las tareas pendientes (y las de hoy) sirven también como lista de pendientes. */
    public static List<WO_KI_NB_JI_Item> WO_KI_NB_JI_desdeTareas(List<WO_KI_NB_JI_Tarea> tareas) {
        List<WO_KI_NB_JI_Item> salida = new ArrayList<>();
        if (tareas == null) return salida;
        for (WO_KI_NB_JI_Tarea t : tareas) {
            if (t.WO_KI_NB_JI_getEstadoTipo() == null || t.WO_KI_NB_JI_getEstadoTipo().WO_KI_NB_JI_esCompletada()) continue;
            salida.add(new WO_KI_NB_JI_Item(t.WO_KI_NB_JI_getId(), -1, t.WO_KI_NB_JI_getTitulo(), false, "tarea"));
        }
        return salida;
    }

    // ------------------------------------------------------------------ utilidades

    /** Quita las marcas de casilla de un texto para mostrarlo limpio. */
    public static String WO_KI_NB_JI_limpiar(String texto) {
        if (texto == null) return "";
        Matcher m = CASILLA.matcher(texto.trim());
        return m.matches() ? m.group("texto").trim() : texto.trim();
    }
}

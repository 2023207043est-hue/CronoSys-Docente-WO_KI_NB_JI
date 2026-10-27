package com.agendapersonal.util;

import com.agendapersonal.infrastructure.db.WO_KI_NB_JI_ConfigDAO;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/** Datos del dueño de la agenda (solo su nombre; Compás es local y de un solo usuario, no hay contraseña). */
public final class WO_KI_NB_JI_Perfil {
    public static final String CFG_NOMBRE = "usuario.nombre";
    public static final String CFG_BIENVENIDA = "usuario.bienvenida";   // "1" cuando ya se mostró la bienvenida
    public static final String CFG_ROL = "usuario.rol";
    public static final String ESTUDIANTE = "ESTUDIANTE";
    public static final String DOCENTE = "DOCENTE";
    private static final int MAX = 40;
    private static final IntegerProperty REVISION = new SimpleIntegerProperty(0);
    private static final WO_KI_NB_JI_ConfigDAO CFG = new WO_KI_NB_JI_ConfigDAO();
    private WO_KI_NB_JI_Perfil() {}

    /** Cambia cuando se edita el nombre (para que el saludo del inicio se actualice solo). */
    public static IntegerProperty WO_KI_NB_JI_revisionProperty() { return REVISION; }

    public static String WO_KI_NB_JI_nombre() { return CFG.WO_KI_NB_JI_get(CFG_NOMBRE, ""); }
    public static String WO_KI_NB_JI_rol() { return CFG.WO_KI_NB_JI_get(CFG_ROL, ESTUDIANTE); }
    public static boolean WO_KI_NB_JI_esDocente() { return DOCENTE.equals(WO_KI_NB_JI_rol()); }
    public static void WO_KI_NB_JI_guardarRol(String rol) {
        CFG.WO_KI_NB_JI_set(CFG_ROL, DOCENTE.equals(rol) ? DOCENTE : ESTUDIANTE);
        REVISION.set(REVISION.get() + 1);
    }
    public static boolean WO_KI_NB_JI_bienvenidaVista() { return "1".equals(CFG.WO_KI_NB_JI_get(CFG_BIENVENIDA, "0")); }

    public static void WO_KI_NB_JI_guardarNombre(String texto) {
        CFG.WO_KI_NB_JI_set(CFG_NOMBRE, WO_KI_NB_JI_limpiar(texto));
        CFG.WO_KI_NB_JI_set(CFG_BIENVENIDA, "1");
        REVISION.set(REVISION.get() + 1);
    }

    public static void WO_KI_NB_JI_omitirBienvenida() { CFG.WO_KI_NB_JI_set(CFG_BIENVENIDA, "1"); }

    // ---- lógica pura (con pruebas) ----

    /** Recorta, une espacios repetidos y limita el largo. */
    public static String WO_KI_NB_JI_limpiar(String texto) {
        if (texto == null) return "";
        String t = texto.trim().replaceAll("\\s+", " ");
        return t.length() > MAX ? t.substring(0, MAX).trim() : t;
    }

    /** "jason selmouni" → "Jason" (solo la primera palabra, con inicial mayúscula). */
    public static String WO_KI_NB_JI_primerNombre(String nombre) {
        String t = WO_KI_NB_JI_limpiar(nombre);
        if (t.isEmpty()) return "";
        String p = t.split(" ")[0];
        return Character.toUpperCase(p.charAt(0)) + p.substring(1);
    }

    /** "Buenas noches, Jason" (o solo "Buenas noches" si aún no hay nombre). */
    public static String WO_KI_NB_JI_saludo(int hora, String nombre) {
        String base = hora < 12 ? "Buenos días" : hora < 20 ? "Buenas tardes" : "Buenas noches";
        String n = WO_KI_NB_JI_primerNombre(nombre);
        return n.isEmpty() ? base : base + ", " + n;
    }
}

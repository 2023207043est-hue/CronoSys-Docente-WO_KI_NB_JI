package com.agendapersonal.util;

import com.agendapersonal.infrastructure.db.wkniConfigDAO;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;

/** Datos del dueño de la agenda (solo su nombre; Compás es local y de un solo usuario, no hay contraseña). */
public final class wkniPerfil {
    public static final String wkniCFG_NOMBRE = "usuario.nombre";
    public static final String wkniCFG_BIENVENIDA = "usuario.bienvenida";   // "1" cuando ya se mostró la bienvenida
    public static final String wkniCFG_ROL = "usuario.rol";
    public static final String wkniESTUDIANTE = "ESTUDIANTE";
    public static final String wkniDOCENTE = "DOCENTE";
    private static final int wkniMAX = 40;
    private static final IntegerProperty wkniREVISION = new SimpleIntegerProperty(0);
    private static final wkniConfigDAO wkniCFG = new wkniConfigDAO();
    private wkniPerfil() {}

    /** Cambia cuando se edita el nombre (para que el saludo del inicio se actualice solo). */
    public static IntegerProperty wkniRevisionProperty() { return wkniREVISION; }

    public static String wkniNombre() { return wkniCFG.wkniGet(wkniCFG_NOMBRE, ""); }
    public static String wkniRol() { return wkniCFG.wkniGet(wkniCFG_ROL, wkniESTUDIANTE); }
    public static boolean wkniEsDocente() { return wkniDOCENTE.equals(wkniRol()); }
    public static void wkniGuardarRol(String wkniRol) {
        wkniCFG.wkniSet(wkniCFG_ROL, wkniDOCENTE.equals(wkniRol) ? wkniDOCENTE : wkniESTUDIANTE);
        wkniREVISION.set(wkniREVISION.get() + 1);
    }
    public static boolean wkniBienvenidaVista() { return "1".equals(wkniCFG.wkniGet(wkniCFG_BIENVENIDA, "0")); }

    public static void wkniGuardarNombre(String wkniTexto) {
        wkniCFG.wkniSet(wkniCFG_NOMBRE, wkniLimpiar(wkniTexto));
        wkniCFG.wkniSet(wkniCFG_BIENVENIDA, "1");
        wkniREVISION.set(wkniREVISION.get() + 1);
    }

    public static void wkniOmitirBienvenida() { wkniCFG.wkniSet(wkniCFG_BIENVENIDA, "1"); }

    // ---- lógica pura (con pruebas) ----

    /** Recorta, une espacios repetidos y limita el largo. */
    public static String wkniLimpiar(String wkniTexto) {
        if (wkniTexto == null) return "";
        String wkniT = wkniTexto.trim().replaceAll("\\s+", " ");
        return wkniT.length() > wkniMAX ? wkniT.substring(0, wkniMAX).trim() : wkniT;
    }

    /** "jason selmouni" → "Jason" (solo la primera palabra, con inicial mayúscula). */
    public static String wkniPrimerNombre(String wkniNombre) {
        String wkniT = wkniLimpiar(wkniNombre);
        if (wkniT.isEmpty()) return "";
        String wkni_p = wkniT.split(" ")[0];
        return Character.toUpperCase(wkni_p.charAt(0)) + wkni_p.substring(1);
    }

    /** "Buenas noches, Jason" (o solo "Buenas noches" si aún no hay nombre). */
    public static String wkniSaludo(int wkniHora, String wkniNombre) {
        String wkniBase = wkniHora < 12 ? "Buenos días" : wkniHora < 20 ? "Buenas tardes" : "Buenas noches";
        String wkniN = wkniPrimerNombre(wkniNombre);
        return wkniN.isEmpty() ? wkniBase : wkniBase + ", " + wkniN;
    }
}

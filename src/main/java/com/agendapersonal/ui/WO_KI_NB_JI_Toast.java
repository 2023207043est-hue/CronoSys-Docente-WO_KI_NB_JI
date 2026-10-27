package com.agendapersonal.ui;

import javafx.scene.Node;
import javafx.stage.Window;

/**
 * Atajo histórico para mostrar un aviso breve. Ahora todo pasa por {@link Notifier}, que dibuja la
 * tarjeta con los colores del tema activo en la ventana principal (antes usaba colores fijos y
 * podía quedar mal ubicada si la ventana aún no tenía tamaño).
 */
public final class WO_KI_NB_JI_Toast {
    private WO_KI_NB_JI_Toast() {}

    public static void WO_KI_NB_JI_mostrar(Node origen, String mensaje) { WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success(mensaje, null); }

    public static void WO_KI_NB_JI_mostrarGlobal(String mensaje) { WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success(mensaje, null); }

    public static void WO_KI_NB_JI_mostrar(Window ventana, String mensaje) { WO_KI_NB_JI_Notifier.WO_KI_NB_JI_success(mensaje, null); }
}

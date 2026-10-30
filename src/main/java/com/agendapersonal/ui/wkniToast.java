package com.agendapersonal.ui;

import javafx.scene.Node;
import javafx.stage.Window;

/**
 * Atajo histórico para mostrar un aviso breve. Ahora todo pasa por {@link Notifier}, que dibuja la
 * tarjeta con los colores del tema activo en la ventana principal (antes usaba colores fijos y
 * podía quedar mal ubicada si la ventana aún no tenía tamaño).
 */
public final class wkniToast {
    private wkniToast() {}

    public static void wkniMostrar(Node wkniOrigen, String wkniMensaje) { wkniNotifier.wkniSuccess(wkniMensaje, null); }

    public static void wkniMostrarGlobal(String wkniMensaje) { wkniNotifier.wkniSuccess(wkniMensaje, null); }

    public static void wkniMostrar(Window wkniVentana, String wkniMensaje) { wkniNotifier.wkniSuccess(wkniMensaje, null); }
}

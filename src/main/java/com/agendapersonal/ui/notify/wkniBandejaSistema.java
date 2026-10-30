package com.agendapersonal.ui.notify;

import javafx.application.Platform;
import javafx.stage.Stage;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.PopupMenu;
import java.awt.RenderingHints;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.image.BufferedImage;

/**
 * Ícono de Compás en la bandeja del sistema (junto al reloj de Windows).
 * Permite cerrar la ventana y que el programa siga vivo avisando eventos y cumpleaños.
 */
public final class wkniBandejaSistema {
    private TrayIcon wkniIcono;

    public boolean wkniActiva() { return wkniIcono != null; }

    /**
     * @return true si se instaló; false si el sistema no soporta bandeja (entonces cerrar la ventana cierra el programa).
     */
    public boolean wkniInstalar(Stage wkniVentana, Runnable wkniVerResumen, Runnable wkniSalir) {
        try {
            if (!SystemTray.isSupported()) return false;
            PopupMenu wkniMenu = new PopupMenu();
            java.awt.MenuItem wkniAbrir = new java.awt.MenuItem("Abrir Compás");
            java.awt.MenuItem wkni_resumen = new java.awt.MenuItem("Ver resumen de hoy");
            java.awt.MenuItem wkniCerrar = new java.awt.MenuItem("Salir");
            wkniAbrir.addActionListener(wkniE -> Platform.runLater(() -> wkniMostrar(wkniVentana)));
            wkni_resumen.addActionListener(wkniE -> Platform.runLater(wkniVerResumen));
            wkniCerrar.addActionListener(wkniE -> Platform.runLater(wkniSalir));
            wkniMenu.add(wkniAbrir);
            wkniMenu.add(wkni_resumen);
            wkniMenu.addSeparator();
            wkniMenu.add(wkniCerrar);

            wkniIcono = new TrayIcon(wkniCrearImagen(), "Compás", wkniMenu);
            wkniIcono.setImageAutoSize(true);
            wkniIcono.addActionListener(wkniE -> Platform.runLater(() -> wkniMostrar(wkniVentana)));   // doble clic
            SystemTray.getSystemTray().add(wkniIcono);
            return true;
        } catch (Exception | Error wkniEx) {       // sin entorno gráfico AWT, etc.
            System.err.println("Bandeja no disponible: " + wkniEx.getMessage());
            wkniIcono = null;
            return false;
        }
    }

    public static void wkniMostrar(Stage wkniVentana) {
        if (!wkniVentana.isShowing()) wkniVentana.show();
        wkniVentana.setIconified(false);
        wkniVentana.toFront();
        wkniVentana.requestFocus();
    }

    /** Aviso nativo del sistema (globo de Windows); respaldo si la ventana flotante fallara. */
    public void wkniGlobo(String wkniTitulo, String wkniMensaje) {
        if (wkniIcono != null) wkniIcono.displayMessage(wkniTitulo, wkniMensaje, TrayIcon.MessageType.INFO);
    }

    public void wkniQuitar() {
        try {
            if (wkniIcono != null) SystemTray.getSystemTray().remove(wkniIcono);
        } catch (Exception wkniIgnored) {}
        wkniIcono = null;
    }

    /** Logo de Compás (desde /brand); si no se encuentra, se dibuja uno simple por código. */
    private static java.awt.Image wkniCrearImagen() {
        try (var wkniIn = wkniBandejaSistema.class.getResourceAsStream("/brand/compas-64.png")) {
            if (wkniIn != null) return javax.imageio.ImageIO.read(wkniIn);
        } catch (Exception wkniIgnored) { /* se usa el dibujo de respaldo */ }
        return wkniDibujarRespaldo();
    }

    private static BufferedImage wkniDibujarRespaldo() {
        int wkniN = 64;
        BufferedImage wkniImg = new BufferedImage(wkniN, wkniN, BufferedImage.TYPE_INT_ARGB);
        Graphics2D wkniG = wkniImg.createGraphics();
        wkniG.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        wkniG.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        wkniG.setColor(new Color(0xC6, 0x4B, 0x1C));
        wkniG.fillRoundRect(2, 2, wkniN - 4, wkniN - 4, 34, 34);
        wkniG.setColor(Color.WHITE);
        wkniG.setFont(new Font("SansSerif", Font.BOLD, 40));
        var wkniFm = wkniG.getFontMetrics();
        wkniG.drawString("C", (wkniN - wkniFm.stringWidth("C")) / 2, (wkniN - wkniFm.getHeight()) / 2 + wkniFm.getAscent());
        wkniG.dispose();
        return wkniImg;
    }
}

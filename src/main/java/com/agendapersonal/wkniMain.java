package com.agendapersonal;

import com.agendapersonal.controller.wkniMainController;
import com.agendapersonal.database.wkniDatabaseConnection;
import com.agendapersonal.service.wkniDataRefreshService;
import com.agendapersonal.service.wkniTipoEntrada;
import com.agendapersonal.ui.wkniFontPreferences;
import com.agendapersonal.ui.wkniLogo;
import com.agendapersonal.ui.wkniThemeService;
import com.agendapersonal.ui.notify.wkniBandejaSistema;
import com.agendapersonal.ui.notify.wkniCentroAvisos;
import com.agendapersonal.ui.notify.wkniNotificacionEscritorio;
import com.agendapersonal.ui.shell.wkniAppShell;
import javafx.application.Application;
import com.agendapersonal.ui.wkniSplash;
import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.util.Duration;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Entry-point Compás: abre la ventana principal y deja el programa avisando (eventos que empiezan,
 * cumpleaños del día) desde la bandeja del sistema aunque se cierre la ventana.
 */
public class wkniMain extends Application {
    private wkniMainController wkniController;
    private wkniDataRefreshService wkniLive;
    private final wkniBandejaSistema wkniBandeja = new wkniBandejaSistema();
    private wkniCentroAvisos wkniAvisos;
    private wkniAppShell wkniShell;
    private boolean wkniAvisoBandejaMostrado;
    private Stage wkniVentana;
    private wkniSplash wkni_splash;
    private static javafx.application.HostServices wkniHostServices;

    /** Abre una URL o un archivo con el programa predeterminado del sistema (navegador, visor de PDF…). */
    public static void wkniAbrirEnSistema(String wkniUri) {
        if (wkniHostServices != null && wkniUri != null && !wkniUri.isBlank()) wkniHostServices.showDocument(wkniUri);
    }

    @Override public void start(Stage wkniStage) {
        wkniHostServices = getHostServices();
        // Pantalla de carga: se dibuja primero y cada paso pinta su mensaje antes de empezar.
        wkni_splash = wkniSplash.wkniCarga();
        wkni_splash.wkniMostrar();
        long wkniInicio = System.currentTimeMillis();
        Runnable[] wkniPasos = {
            () -> { wkni_splash.wkniAvance("Preparando tus datos…", 0.15);
                    wkniDatabaseConnection.wkniInitializeDatabase(); },
            () -> { wkni_splash.wkniAvance("Cargando fuentes y tema…", 0.45);
                    wkniFontPreferences.wkniCargarFuentesIncluidas();
                    wkniThemeService.wkniGet().wkniInstall(); },
            () -> { wkni_splash.wkniAvance("Preparando tu agenda…", 0.7);
                    wkniController = new wkniMainController();
                    wkniLive = new wkniDataRefreshService(wkniController); },
            () -> { wkni_splash.wkniAvance("Abriendo Compás…", 0.95);
                    // La pantalla de carga se queda al menos un instante para que no parpadee.
                    long wkniResto = Math.max(0, 900 - (System.currentTimeMillis() - wkniInicio));
                    PauseTransition wkni_p = new PauseTransition(Duration.millis(wkniResto));
                    wkni_p.setOnFinished(wkniE -> { wkni_splash.wkniAvance("Listo", 1); wkni_splash.wkniCerrar(() -> {
                        Platform.runLater(() -> {
                            com.agendapersonal.ui.wkniBienvenidaView.wkniMostrarSiHaceFalta();
                            wkniAbrirVentana(wkniStage);
                            Platform.runLater(() -> {
                                com.agendapersonal.ui.wkniBienvenidaInicioView.wkniMostrar(wkniStage, wkniController,
                                    () -> wkniShell.wkniMostrar("Agenda Unificada"), () -> wkniShell.wkniMostrar("Notas y PKM"),
                                    () -> wkniShell.wkniMostrar("Espacio Docente"));
                                Platform.runLater(wkniAvisos::wkniIniciar);
                            });
                        });
                    }); });
                    wkni_p.play(); }
        };
        wkniEjecutar(wkniPasos, 0);
    }

    /** Ejecuta un paso, deja respirar a la interfaz 70 ms para que pinte y sigue con el siguiente. */
    private void wkniEjecutar(Runnable[] wkniPasos, int wkniI) {
        if (wkniI >= wkniPasos.length) return;
        PauseTransition wkni_p = new PauseTransition(Duration.millis(70));
        wkni_p.setOnFinished(wkniE -> {
            try { wkniPasos[wkniI].run(); } catch (RuntimeException wkniEx) { wkniFallo(wkniEx); return; }
            wkniEjecutar(wkniPasos, wkniI + 1);
        });
        wkni_p.play();
    }

    private void wkniFallo(RuntimeException wkniE) {
        wkni_splash.wkniCerrar(null);
        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
            wkniE.getMessage() + "\n\nRevisa el archivo compas.log en:\n" + wkniDatabaseConnection.wkniDataDir()).showAndWait();
        Platform.exit();
    }

    private void wkniAbrirVentana(Stage wkniStage) {
        wkniVentana = wkniStage;
        Platform.setImplicitExit(false);          // cerrar la ventana NO termina el programa (sigue en la bandeja)
        wkniStage.setTitle("Compás");
        wkniLogo.wkniAplicarIconos(wkniStage);
        wkniShell = new wkniAppShell(wkniController, wkniStage);
        Scene wkniScene = new Scene(wkniShell.wkniGetRoot(), 1320, 840);
        wkniThemeService.wkniGet().wkniApplyTo(wkniScene);
        wkniShell.wkniInstalarAtajos(wkniScene);
        wkniStage.setMinWidth(980);
        wkniStage.setMinHeight(640);
        wkniStage.setScene(wkniScene);
        wkniStage.show();
        wkniLive.wkniStart();

        Runnable wkniAbrirAgenda = () -> {
            wkniBandejaSistema.wkniMostrar(wkniStage);
            wkniShell.wkniMostrar("Agenda Unificada");
        };
        wkniAvisos = new wkniCentroAvisos(wkniController, wkniStage, wkniAbrirAgenda);
        wkniNotificacionEscritorio.wkniSetAlAbrir(wkniAbrirAgenda);

        boolean wkniEnBandeja = wkniBandeja.wkniInstalar(wkniStage, wkniAvisos::wkniMostrarResumenAhora, this::wkniSalir);
        wkniShell.wkniSetAlSalir(this::wkniSalir);
        wkniStage.setOnCloseRequest(wkniE -> {
            if (!wkniEnBandeja) { wkniE.consume(); wkniSalir(); return; }
            wkniE.consume();
            wkniStage.hide();
            if (!wkniAvisoBandejaMostrado) {
                wkniAvisoBandejaMostrado = true;
                wkniNotificacionEscritorio.wkniMostrar(wkniTipoEntrada.EVENTO, "Compás sigue activo",
                    "Te avisará cuando empiece un evento. Ábrelo desde el ícono junto al reloj.", 7);
            }
        });
    }

    private boolean wkniSaliendo;

    /** Salida real del programa: la ventana se desvanece, aparece "Hasta pronto" y luego se cierra todo. */
    private void wkniSalir() {
        if (wkniSaliendo) return;
        wkniSaliendo = true;
        Runnable wkniCerrarTodo = () -> {
            if (wkniAvisos != null) wkniAvisos.wkniDetener();
            if (wkniLive != null) wkniLive.wkniStop();
            wkniBandeja.wkniQuitar();
            wkniNotificacionEscritorio.wkniCerrarTodas();
            Platform.exit();
            System.exit(0);
        };
        Platform.runLater(() -> {
            wkniNotificacionEscritorio.wkniCerrarTodas();
            Stage wkniV = wkniVentana;
            if (wkniV == null || !wkniV.isShowing()) { wkniSplash.wkniDespedida(com.agendapersonal.util.wkniPerfil.wkniNombre(), wkniCerrarTodo); return; }
            FadeTransition wkniF = new FadeTransition(Duration.millis(260), wkniV.getScene().getRoot());
            wkniF.setToValue(0);
            wkniF.setOnFinished(wkniE -> { wkniV.hide(); wkniSplash.wkniDespedida(com.agendapersonal.util.wkniPerfil.wkniNombre(), wkniCerrarTodo); });
            wkniF.play();
        });
    }

    public static void main(String[] wkniArgs) { launch(wkniArgs); }
}

package com.agendapersonal;

import com.agendapersonal.controller.WO_KI_NB_JI_MainController;
import com.agendapersonal.database.WO_KI_NB_JI_DatabaseConnection;
import com.agendapersonal.service.WO_KI_NB_JI_DataRefreshService;
import com.agendapersonal.service.WO_KI_NB_JI_TipoEntrada;
import com.agendapersonal.ui.WO_KI_NB_JI_FontPreferences;
import com.agendapersonal.ui.WO_KI_NB_JI_Logo;
import com.agendapersonal.ui.WO_KI_NB_JI_ThemeService;
import com.agendapersonal.ui.notify.WO_KI_NB_JI_BandejaSistema;
import com.agendapersonal.ui.notify.WO_KI_NB_JI_CentroAvisos;
import com.agendapersonal.ui.notify.WO_KI_NB_JI_NotificacionEscritorio;
import com.agendapersonal.ui.shell.WO_KI_NB_JI_AppShell;
import javafx.application.Application;
import com.agendapersonal.ui.WO_KI_NB_JI_Splash;
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
public class WO_KI_NB_JI_Main extends Application {
    private WO_KI_NB_JI_MainController controller;
    private WO_KI_NB_JI_DataRefreshService live;
    private final WO_KI_NB_JI_BandejaSistema bandeja = new WO_KI_NB_JI_BandejaSistema();
    private WO_KI_NB_JI_CentroAvisos avisos;
    private WO_KI_NB_JI_AppShell shell;
    private boolean avisoBandejaMostrado;
    private Stage ventana;
    private WO_KI_NB_JI_Splash splash;
    private static javafx.application.HostServices hostServices;

    /** Abre una URL o un archivo con el programa predeterminado del sistema (navegador, visor de PDF…). */
    public static void WO_KI_NB_JI_abrirEnSistema(String uri) {
        if (hostServices != null && uri != null && !uri.isBlank()) hostServices.showDocument(uri);
    }

    @Override public void start(Stage stage) {
        hostServices = getHostServices();
        // Pantalla de carga: se dibuja primero y cada paso pinta su mensaje antes de empezar.
        splash = WO_KI_NB_JI_Splash.WO_KI_NB_JI_carga();
        splash.WO_KI_NB_JI_mostrar();
        long inicio = System.currentTimeMillis();
        Runnable[] pasos = {
            () -> { splash.WO_KI_NB_JI_avance("Preparando tus datos…", 0.15);
                    WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_initializeDatabase(); },
            () -> { splash.WO_KI_NB_JI_avance("Cargando fuentes y tema…", 0.45);
                    WO_KI_NB_JI_FontPreferences.WO_KI_NB_JI_cargarFuentesIncluidas();
                    WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_get().WO_KI_NB_JI_install(); },
            () -> { splash.WO_KI_NB_JI_avance("Preparando tu agenda…", 0.7);
                    controller = new WO_KI_NB_JI_MainController();
                    live = new WO_KI_NB_JI_DataRefreshService(controller); },
            () -> { splash.WO_KI_NB_JI_avance("Abriendo Compás…", 0.95);
                    // La pantalla de carga se queda al menos un instante para que no parpadee.
                    long resto = Math.max(0, 900 - (System.currentTimeMillis() - inicio));
                    PauseTransition p = new PauseTransition(Duration.millis(resto));
                    p.setOnFinished(e -> { splash.WO_KI_NB_JI_avance("Listo", 1); splash.WO_KI_NB_JI_cerrar(() -> {
                        Platform.runLater(() -> {
                            com.agendapersonal.ui.WO_KI_NB_JI_BienvenidaView.WO_KI_NB_JI_mostrarSiHaceFalta();
                            WO_KI_NB_JI_abrirVentana(stage);
                            Platform.runLater(() -> {
                                com.agendapersonal.ui.WO_KI_NB_JI_BienvenidaInicioView.WO_KI_NB_JI_mostrar(stage, controller,
                                    () -> shell.WO_KI_NB_JI_mostrar("Agenda Unificada"), () -> shell.WO_KI_NB_JI_mostrar("Notas y PKM"),
                                    () -> shell.WO_KI_NB_JI_mostrar("Espacio Docente"));
                                Platform.runLater(avisos::WO_KI_NB_JI_iniciar);
                            });
                        });
                    }); });
                    p.play(); }
        };
        WO_KI_NB_JI_ejecutar(pasos, 0);
    }

    /** Ejecuta un paso, deja respirar a la interfaz 70 ms para que pinte y sigue con el siguiente. */
    private void WO_KI_NB_JI_ejecutar(Runnable[] pasos, int i) {
        if (i >= pasos.length) return;
        PauseTransition p = new PauseTransition(Duration.millis(70));
        p.setOnFinished(e -> {
            try { pasos[i].run(); } catch (RuntimeException ex) { WO_KI_NB_JI_fallo(ex); return; }
            WO_KI_NB_JI_ejecutar(pasos, i + 1);
        });
        p.play();
    }

    private void WO_KI_NB_JI_fallo(RuntimeException e) {
        splash.WO_KI_NB_JI_cerrar(null);
        new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR,
            e.getMessage() + "\n\nRevisa el archivo compas.log en:\n" + WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_dataDir()).showAndWait();
        Platform.exit();
    }

    private void WO_KI_NB_JI_abrirVentana(Stage stage) {
        ventana = stage;
        Platform.setImplicitExit(false);          // cerrar la ventana NO termina el programa (sigue en la bandeja)
        stage.setTitle("Compás");
        WO_KI_NB_JI_Logo.WO_KI_NB_JI_aplicarIconos(stage);
        shell = new WO_KI_NB_JI_AppShell(controller, stage);
        Scene scene = new Scene(shell.WO_KI_NB_JI_getRoot(), 1320, 840);
        WO_KI_NB_JI_ThemeService.WO_KI_NB_JI_get().WO_KI_NB_JI_applyTo(scene);
        shell.WO_KI_NB_JI_instalarAtajos(scene);
        stage.setMinWidth(980);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
        live.WO_KI_NB_JI_start();

        Runnable abrirAgenda = () -> {
            WO_KI_NB_JI_BandejaSistema.WO_KI_NB_JI_mostrar(stage);
            shell.WO_KI_NB_JI_mostrar("Agenda Unificada");
        };
        avisos = new WO_KI_NB_JI_CentroAvisos(controller, stage, abrirAgenda);
        WO_KI_NB_JI_NotificacionEscritorio.WO_KI_NB_JI_setAlAbrir(abrirAgenda);

        boolean enBandeja = bandeja.WO_KI_NB_JI_instalar(stage, avisos::WO_KI_NB_JI_mostrarResumenAhora, this::WO_KI_NB_JI_salir);
        shell.WO_KI_NB_JI_setAlSalir(this::WO_KI_NB_JI_salir);
        stage.setOnCloseRequest(e -> {
            if (!enBandeja) { e.consume(); WO_KI_NB_JI_salir(); return; }
            e.consume();
            stage.hide();
            if (!avisoBandejaMostrado) {
                avisoBandejaMostrado = true;
                WO_KI_NB_JI_NotificacionEscritorio.WO_KI_NB_JI_mostrar(WO_KI_NB_JI_TipoEntrada.EVENTO, "Compás sigue activo",
                    "Te avisará cuando empiece un evento. Ábrelo desde el ícono junto al reloj.", 7);
            }
        });
    }

    private boolean saliendo;

    /** Salida real del programa: la ventana se desvanece, aparece "Hasta pronto" y luego se cierra todo. */
    private void WO_KI_NB_JI_salir() {
        if (saliendo) return;
        saliendo = true;
        Runnable cerrarTodo = () -> {
            if (avisos != null) avisos.WO_KI_NB_JI_detener();
            if (live != null) live.WO_KI_NB_JI_stop();
            bandeja.WO_KI_NB_JI_quitar();
            WO_KI_NB_JI_NotificacionEscritorio.WO_KI_NB_JI_cerrarTodas();
            Platform.exit();
            System.exit(0);
        };
        Platform.runLater(() -> {
            WO_KI_NB_JI_NotificacionEscritorio.WO_KI_NB_JI_cerrarTodas();
            Stage v = ventana;
            if (v == null || !v.isShowing()) { WO_KI_NB_JI_Splash.WO_KI_NB_JI_despedida(com.agendapersonal.util.WO_KI_NB_JI_Perfil.WO_KI_NB_JI_nombre(), cerrarTodo); return; }
            FadeTransition f = new FadeTransition(Duration.millis(260), v.getScene().getRoot());
            f.setToValue(0);
            f.setOnFinished(e -> { v.hide(); WO_KI_NB_JI_Splash.WO_KI_NB_JI_despedida(com.agendapersonal.util.WO_KI_NB_JI_Perfil.WO_KI_NB_JI_nombre(), cerrarTodo); });
            f.play();
        });
    }

    public static void main(String[] args) { launch(args); }
}

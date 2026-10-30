package com.agendapersonal.database;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.SQLException;
import org.sqlite.SQLiteConfig;

/**
 * Conexión a la base de datos de Compás.
 * <ul>
 *   <li>Ubicación fija por usuario ({@code %APPDATA%\CronoSys} en Windows, {@code ~/.cronosys} en otros),
 *       con el nombre histórico de la app para no perder los datos de quien ya la usaba
 *       y sin depender de la carpeta desde la que se abre el programa.</li>
 *   <li>Se puede forzar otra ruta con {@code -Dcronosys.db=ruta} (las pruebas usan un archivo temporal).</li>
 *   <li>Cada conexión activa las claves foráneas (SQLite las ignora por defecto) y espera si la BD está ocupada.</li>
 * </ul>
 */
public class wkniDatabaseConnection {
    public static final String wkniPROP_RUTA = "cronosys.db";
    private wkniDatabaseConnection() {}

    /** Carpeta de datos del usuario (se crea si no existe). */
    public static Path wkniDataDir() {
        Path wkniD;
        String wkniForzada = System.getProperty(wkniPROP_RUTA);
        if (wkniForzada != null && !wkniForzada.isBlank()) {
            Path wkni_p = Paths.get(wkniForzada).toAbsolutePath().getParent();
            wkniD = wkni_p != null ? wkni_p : Paths.get(".").toAbsolutePath();
        } else {
            String wkniAppdata = System.getenv("APPDATA");
            wkniD = wkniAppdata != null && !wkniAppdata.isBlank()
                ? Paths.get(wkniAppdata, "CronoSys")
                : Paths.get(System.getProperty("user.home"), ".cronosys");
        }
        try { Files.createDirectories(wkniD); } catch (IOException wkniIgnored) { /* se reportará al abrir la BD */ }
        return wkniD;
    }

    public static Path wkniDbPath() {
        String wkniForzada = System.getProperty(wkniPROP_RUTA);
        if (wkniForzada != null && !wkniForzada.isBlank()) return Paths.get(wkniForzada).toAbsolutePath();
        return wkniDataDir().resolve("agenda_personal.db");
    }

    public static Connection wkniGetConnection() throws SQLException {
        SQLiteConfig wkniCfg = new SQLiteConfig();
        wkniCfg.enforceForeignKeys(true);
        wkniCfg.setBusyTimeout(5000);
        return wkniCfg.createConnection("jdbc:sqlite:" + wkniDbPath());
    }

    /** Si existe la BD antigua junto al programa y aún no hay una en la carpeta del usuario, la copia (sin borrarla). */
    static void wkniImportarBdAntigua() {
        if (System.getProperty(wkniPROP_RUTA) != null) return;
        Path wkniNueva = wkniDbPath();
        Path wkniVieja = Paths.get("agenda_personal.db").toAbsolutePath();
        if (Files.exists(wkniNueva) || !Files.exists(wkniVieja)) return;
        try {
            Files.copy(wkniVieja, wkniNueva, StandardCopyOption.COPY_ATTRIBUTES);
            com.agendapersonal.util.wkniLog.wkniInfo("Base de datos copiada desde " + wkniVieja + " a " + wkniNueva);
        } catch (IOException wkniE) { com.agendapersonal.util.wkniLog.wkniError("No se pudo copiar la BD antigua", wkniE); }
    }

    public static void wkniInitializeDatabase() {
        wkniImportarBdAntigua();
        com.agendapersonal.infrastructure.db.wkniDatabase.wkniMigrate();
    }
}

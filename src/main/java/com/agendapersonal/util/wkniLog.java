package com.agendapersonal.util;

import com.agendapersonal.database.wkniDatabaseConnection;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

/** Registro mínimo de errores en {@code compas.log} (junto a la base de datos). Nunca lanza excepciones. */
public final class wkniLog {
    private static final long wkniMAX_BYTES = 1_000_000;
    private wkniLog() {}

    public static void wkniError(String wkniDonde, Throwable wkniT) {
        StringWriter wkniSw = new StringWriter();
        if (wkniT != null) wkniT.printStackTrace(new PrintWriter(wkniSw));
        wkniEscribir("ERROR", wkniDonde + (wkniT != null ? ": " + wkniT.getMessage() : "") + (wkniT != null ? "\n" + wkniSw : ""));
    }

    public static void wkniInfo(String wkniMensaje) { wkniEscribir("INFO", wkniMensaje); }

    private static synchronized void wkniEscribir(String wkni_nivel, String wkniTexto) {
        String wkniLinea = LocalDateTime.now() + " [" + wkni_nivel + "] " + wkniTexto + System.lineSeparator();
        System.err.print(wkniLinea);
        try {
            Path wkniF = wkniDatabaseConnection.wkniDataDir().resolve("compas.log");
            if (Files.exists(wkniF) && Files.size(wkniF) > wkniMAX_BYTES) Files.deleteIfExists(wkniF);
            Files.writeString(wkniF, wkniLinea, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException wkniIgnored) { /* el registro nunca debe romper la app */ }
    }
}

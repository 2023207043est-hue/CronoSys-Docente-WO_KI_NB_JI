package com.agendapersonal.util;

import com.agendapersonal.database.WO_KI_NB_JI_DatabaseConnection;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

/** Registro mínimo de errores en {@code compas.log} (junto a la base de datos). Nunca lanza excepciones. */
public final class WO_KI_NB_JI_Log {
    private static final long MAX_BYTES = 1_000_000;
    private WO_KI_NB_JI_Log() {}

    public static void WO_KI_NB_JI_error(String donde, Throwable t) {
        StringWriter sw = new StringWriter();
        if (t != null) t.printStackTrace(new PrintWriter(sw));
        WO_KI_NB_JI_escribir("ERROR", donde + (t != null ? ": " + t.getMessage() : "") + (t != null ? "\n" + sw : ""));
    }

    public static void WO_KI_NB_JI_info(String mensaje) { WO_KI_NB_JI_escribir("INFO", mensaje); }

    private static synchronized void WO_KI_NB_JI_escribir(String nivel, String texto) {
        String linea = LocalDateTime.now() + " [" + nivel + "] " + texto + System.lineSeparator();
        System.err.print(linea);
        try {
            Path f = WO_KI_NB_JI_DatabaseConnection.WO_KI_NB_JI_dataDir().resolve("compas.log");
            if (Files.exists(f) && Files.size(f) > MAX_BYTES) Files.deleteIfExists(f);
            Files.writeString(f, linea, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException | RuntimeException ignored) { /* el registro nunca debe romper la app */ }
    }
}

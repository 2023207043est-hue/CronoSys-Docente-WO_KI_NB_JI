package com.agendapersonal.settings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON mínimo (sin dependencias): objetos → Map, arreglos → List, números → Double/Long. */
public final class wkniJson {
    private wkniJson() {}

    public static final class wkniJsonException extends RuntimeException {
        public wkniJsonException(String wkniM) { super(wkniM); }
    }

    public static Object wkniParse(String wkniS) {
        if (wkniS == null) throw new wkniJsonException("JSON vacío");
        wkniP wkni_p = new wkniP(wkniS);
        wkni_p.wkniWs();
        Object wkniV = wkni_p.wkniValue();
        wkni_p.wkniWs();
        if (wkni_p.wkniI != wkniS.length()) throw new wkniJsonException("Texto sobrante en la posición " + wkni_p.wkniI);
        return wkniV;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> wkniParseObject(String wkniS) {
        Object wkniO = wkniParse(wkniS);
        if (wkniO instanceof Map<?, ?> wkniM) return (Map<String, Object>) wkniM;
        throw new wkniJsonException("Se esperaba un objeto JSON");
    }

    public static String wkniWrite(Object wkniO) { StringBuilder wkniB = new StringBuilder(); wkniW(wkniO, wkniB, 0, false); return wkniB.toString(); }
    public static String wkniPretty(Object wkniO) { StringBuilder wkniB = new StringBuilder(); wkniW(wkniO, wkniB, 0, true); return wkniB.toString(); }

    private static void wkniW(Object wkniO, StringBuilder wkniB, int wkniInd, boolean wkniPretty) {
        if (wkniO == null) { wkniB.append("null"); }
        else if (wkniO instanceof String wkniS) wkniStr(wkniS, wkniB);
        else if (wkniO instanceof Boolean || wkniO instanceof Long || wkniO instanceof Integer) wkniB.append(wkniO);
        else if (wkniO instanceof Number wkniN) {
            double wkniD = wkniN.doubleValue();
            if (wkniD == Math.rint(wkniD) && Math.abs(wkniD) < 1e15) wkniB.append((long) wkniD); else wkniB.append(wkniD);
        } else if (wkniO instanceof Map<?, ?> wkniM) {
            if (wkniM.isEmpty()) { wkniB.append("{}"); return; }
            wkniB.append('{'); boolean wkniFirst = true;
            for (Map.Entry<?, ?> wkniE : wkniM.entrySet()) {
                if (!wkniFirst) wkniB.append(',');
                wkniFirst = false;
                wkniNl(wkniB, wkniInd + 1, wkniPretty);
                wkniStr(String.valueOf(wkniE.getKey()), wkniB); wkniB.append(wkniPretty ? ": " : ":");
                wkniW(wkniE.getValue(), wkniB, wkniInd + 1, wkniPretty);
            }
            wkniNl(wkniB, wkniInd, wkniPretty); wkniB.append('}');
        } else if (wkniO instanceof Iterable<?> wkniIt) {
            boolean wkniFirst = true; wkniB.append('[');
            for (Object wkniX : wkniIt) {
                if (!wkniFirst) wkniB.append(',');
                wkniFirst = false;
                wkniNl(wkniB, wkniInd + 1, wkniPretty);
                wkniW(wkniX, wkniB, wkniInd + 1, wkniPretty);
            }
            if (!wkniFirst) wkniNl(wkniB, wkniInd, wkniPretty);
            wkniB.append(']');
        } else wkniStr(String.valueOf(wkniO), wkniB);
    }

    private static void wkniNl(StringBuilder wkniB, int wkniInd, boolean wkniPretty) {
        if (!wkniPretty) return;
        wkniB.append('\n');
        for (int wkniK = 0; wkniK < wkniInd; wkniK++) wkniB.append("  ");
    }

    private static void wkniStr(String wkniS, StringBuilder wkniB) {
        wkniB.append('"');
        for (int wkniK = 0; wkniK < wkniS.length(); wkniK++) {
            char wkniC = wkniS.charAt(wkniK);
            switch (wkniC) {
                case '"' -> wkniB.append("\\\"");
                case '\\' -> wkniB.append("\\\\");
                case '\n' -> wkniB.append("\\n");
                case '\r' -> wkniB.append("\\r");
                case '\t' -> wkniB.append("\\t");
                default -> { if (wkniC < 0x20) wkniB.append(String.format("\\u%04x", (int) wkniC)); else wkniB.append(wkniC); }
            }
        }
        wkniB.append('"');
    }

    private static final class wkniP {
        final String wkniS; int wkniI;
        wkniP(String wkniS) { this.wkniS = wkniS; }
        void wkniWs() { while (wkniI < wkniS.length() && Character.isWhitespace(wkniS.charAt(wkniI))) wkniI++; }
        char wkniPeek() { if (wkniI >= wkniS.length()) throw new wkniJsonException("Fin inesperado"); return wkniS.charAt(wkniI); }
        void wkniExpect(char wkniC) { if (wkniPeek() != wkniC) throw new wkniJsonException("Se esperaba '" + wkniC + "' en " + wkniI); wkniI++; }

        Object wkniValue() {
            char wkniC = wkniPeek();
            if (wkniC == '{') return wkniObj();
            if (wkniC == '[') return wkniArr();
            if (wkniC == '"') return wkniString();
            if (wkniS.startsWith("true", wkniI)) { wkniI += 4; return Boolean.TRUE; }
            if (wkniS.startsWith("false", wkniI)) { wkniI += 5; return Boolean.FALSE; }
            if (wkniS.startsWith("null", wkniI)) { wkniI += 4; return null; }
            return wkniNumber();
        }
        Map<String, Object> wkniObj() {
            Map<String, Object> wkniM = new LinkedHashMap<>();
            wkniExpect('{'); wkniWs();
            if (wkniPeek() == '}') { wkniI++; return wkniM; }
            while (true) {
                wkniWs(); String wkniK = wkniString(); wkniWs(); wkniExpect(':'); wkniWs();
                wkniM.put(wkniK, wkniValue()); wkniWs();
                char wkniC = wkniPeek(); wkniI++;
                if (wkniC == '}') return wkniM;
                if (wkniC != ',') throw new wkniJsonException("Se esperaba ',' o '}' en " + (wkniI - 1));
            }
        }
        List<Object> wkniArr() {
            List<Object> wkniL = new ArrayList<>();
            wkniExpect('['); wkniWs();
            if (wkniPeek() == ']') { wkniI++; return wkniL; }
            while (true) {
                wkniWs(); wkniL.add(wkniValue()); wkniWs();
                char wkniC = wkniPeek(); wkniI++;
                if (wkniC == ']') return wkniL;
                if (wkniC != ',') throw new wkniJsonException("Se esperaba ',' o ']' en " + (wkniI - 1));
            }
        }
        String wkniString() {
            wkniExpect('"');
            StringBuilder wkniB = new StringBuilder();
            while (true) {
                char wkniC = wkniPeek(); wkniI++;
                if (wkniC == '"') return wkniB.toString();
                if (wkniC != '\\') { wkniB.append(wkniC); continue; }
                char wkniE = wkniPeek(); wkniI++;
                switch (wkniE) {
                    case 'n' -> wkniB.append('\n'); case 't' -> wkniB.append('\t'); case 'r' -> wkniB.append('\r');
                    case 'b' -> wkniB.append('\b'); case 'f' -> wkniB.append('\f');
                    case 'u' -> {
                        if (wkniI + 4 > wkniS.length()) throw new wkniJsonException("Escape \\u incompleto");
                        try { wkniB.append((char) Integer.parseInt(wkniS.substring(wkniI, wkniI + 4), 16)); }
                        catch (NumberFormatException wkniEx) { throw new wkniJsonException("Escape \\u inválido"); }
                        wkniI += 4;
                    }
                    default -> wkniB.append(wkniE);
                }
            }
        }
        Number wkniNumber() {
            int wkniSt = wkniI;
            while (wkniI < wkniS.length() && "+-0123456789.eE".indexOf(wkniS.charAt(wkniI)) >= 0) wkniI++;
            if (wkniSt == wkniI) throw new wkniJsonException("Valor inesperado en " + wkniI);
            String wkniT = wkniS.substring(wkniSt, wkniI);
            try {
                if (wkniT.matches("-?\\d+") && wkniT.length() < 18) return Long.parseLong(wkniT);
                return Double.parseDouble(wkniT);
            } catch (NumberFormatException wkniE) { throw new wkniJsonException("Número inválido: " + wkniT); }
        }
    }
}

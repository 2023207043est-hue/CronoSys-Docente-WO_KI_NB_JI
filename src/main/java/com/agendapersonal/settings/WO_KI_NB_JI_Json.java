package com.agendapersonal.settings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** JSON mínimo (sin dependencias): objetos → Map, arreglos → List, números → Double/Long. */
public final class WO_KI_NB_JI_Json {
    private WO_KI_NB_JI_Json() {}

    public static final class WO_KI_NB_JI_JsonException extends RuntimeException {
        public WO_KI_NB_JI_JsonException(String m) { super(m); }
    }

    public static Object WO_KI_NB_JI_parse(String s) {
        if (s == null) throw new WO_KI_NB_JI_JsonException("JSON vacío");
        WO_KI_NB_JI_P p = new WO_KI_NB_JI_P(s);
        p.WO_KI_NB_JI_ws();
        Object v = p.WO_KI_NB_JI_value();
        p.WO_KI_NB_JI_ws();
        if (p.i != s.length()) throw new WO_KI_NB_JI_JsonException("Texto sobrante en la posición " + p.i);
        return v;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> WO_KI_NB_JI_parseObject(String s) {
        Object o = WO_KI_NB_JI_parse(s);
        if (o instanceof Map<?, ?> m) return (Map<String, Object>) m;
        throw new WO_KI_NB_JI_JsonException("Se esperaba un objeto JSON");
    }

    public static String WO_KI_NB_JI_write(Object o) { StringBuilder b = new StringBuilder(); WO_KI_NB_JI_w(o, b, 0, false); return b.toString(); }
    public static String WO_KI_NB_JI_pretty(Object o) { StringBuilder b = new StringBuilder(); WO_KI_NB_JI_w(o, b, 0, true); return b.toString(); }

    private static void WO_KI_NB_JI_w(Object o, StringBuilder b, int ind, boolean pretty) {
        if (o == null) { b.append("null"); }
        else if (o instanceof String s) WO_KI_NB_JI_str(s, b);
        else if (o instanceof Boolean || o instanceof Long || o instanceof Integer) b.append(o);
        else if (o instanceof Number n) {
            double d = n.doubleValue();
            if (d == Math.rint(d) && Math.abs(d) < 1e15) b.append((long) d); else b.append(d);
        } else if (o instanceof Map<?, ?> m) {
            if (m.isEmpty()) { b.append("{}"); return; }
            b.append('{'); boolean first = true;
            for (Map.Entry<?, ?> e : m.entrySet()) {
                if (!first) b.append(',');
                first = false;
                WO_KI_NB_JI_nl(b, ind + 1, pretty);
                WO_KI_NB_JI_str(String.valueOf(e.getKey()), b); b.append(pretty ? ": " : ":");
                WO_KI_NB_JI_w(e.getValue(), b, ind + 1, pretty);
            }
            WO_KI_NB_JI_nl(b, ind, pretty); b.append('}');
        } else if (o instanceof Iterable<?> it) {
            boolean first = true; b.append('[');
            for (Object x : it) {
                if (!first) b.append(',');
                first = false;
                WO_KI_NB_JI_nl(b, ind + 1, pretty);
                WO_KI_NB_JI_w(x, b, ind + 1, pretty);
            }
            if (!first) WO_KI_NB_JI_nl(b, ind, pretty);
            b.append(']');
        } else WO_KI_NB_JI_str(String.valueOf(o), b);
    }

    private static void WO_KI_NB_JI_nl(StringBuilder b, int ind, boolean pretty) {
        if (!pretty) return;
        b.append('\n');
        for (int k = 0; k < ind; k++) b.append("  ");
    }

    private static void WO_KI_NB_JI_str(String s, StringBuilder b) {
        b.append('"');
        for (int k = 0; k < s.length(); k++) {
            char c = s.charAt(k);
            switch (c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> { if (c < 0x20) b.append(String.format("\\u%04x", (int) c)); else b.append(c); }
            }
        }
        b.append('"');
    }

    private static final class WO_KI_NB_JI_P {
        final String s; int i;
        WO_KI_NB_JI_P(String s) { this.s = s; }
        void WO_KI_NB_JI_ws() { while (i < s.length() && Character.isWhitespace(s.charAt(i))) i++; }
        char WO_KI_NB_JI_peek() { if (i >= s.length()) throw new WO_KI_NB_JI_JsonException("Fin inesperado"); return s.charAt(i); }
        void WO_KI_NB_JI_expect(char c) { if (WO_KI_NB_JI_peek() != c) throw new WO_KI_NB_JI_JsonException("Se esperaba '" + c + "' en " + i); i++; }

        Object WO_KI_NB_JI_value() {
            char c = WO_KI_NB_JI_peek();
            if (c == '{') return WO_KI_NB_JI_obj();
            if (c == '[') return WO_KI_NB_JI_arr();
            if (c == '"') return WO_KI_NB_JI_string();
            if (s.startsWith("true", i)) { i += 4; return Boolean.TRUE; }
            if (s.startsWith("false", i)) { i += 5; return Boolean.FALSE; }
            if (s.startsWith("null", i)) { i += 4; return null; }
            return WO_KI_NB_JI_number();
        }
        Map<String, Object> WO_KI_NB_JI_obj() {
            Map<String, Object> m = new LinkedHashMap<>();
            WO_KI_NB_JI_expect('{'); WO_KI_NB_JI_ws();
            if (WO_KI_NB_JI_peek() == '}') { i++; return m; }
            while (true) {
                WO_KI_NB_JI_ws(); String k = WO_KI_NB_JI_string(); WO_KI_NB_JI_ws(); WO_KI_NB_JI_expect(':'); WO_KI_NB_JI_ws();
                m.put(k, WO_KI_NB_JI_value()); WO_KI_NB_JI_ws();
                char c = WO_KI_NB_JI_peek(); i++;
                if (c == '}') return m;
                if (c != ',') throw new WO_KI_NB_JI_JsonException("Se esperaba ',' o '}' en " + (i - 1));
            }
        }
        List<Object> WO_KI_NB_JI_arr() {
            List<Object> l = new ArrayList<>();
            WO_KI_NB_JI_expect('['); WO_KI_NB_JI_ws();
            if (WO_KI_NB_JI_peek() == ']') { i++; return l; }
            while (true) {
                WO_KI_NB_JI_ws(); l.add(WO_KI_NB_JI_value()); WO_KI_NB_JI_ws();
                char c = WO_KI_NB_JI_peek(); i++;
                if (c == ']') return l;
                if (c != ',') throw new WO_KI_NB_JI_JsonException("Se esperaba ',' o ']' en " + (i - 1));
            }
        }
        String WO_KI_NB_JI_string() {
            WO_KI_NB_JI_expect('"');
            StringBuilder b = new StringBuilder();
            while (true) {
                char c = WO_KI_NB_JI_peek(); i++;
                if (c == '"') return b.toString();
                if (c != '\\') { b.append(c); continue; }
                char e = WO_KI_NB_JI_peek(); i++;
                switch (e) {
                    case 'n' -> b.append('\n'); case 't' -> b.append('\t'); case 'r' -> b.append('\r');
                    case 'b' -> b.append('\b'); case 'f' -> b.append('\f');
                    case 'u' -> {
                        if (i + 4 > s.length()) throw new WO_KI_NB_JI_JsonException("Escape \\u incompleto");
                        try { b.append((char) Integer.parseInt(s.substring(i, i + 4), 16)); }
                        catch (NumberFormatException ex) { throw new WO_KI_NB_JI_JsonException("Escape \\u inválido"); }
                        i += 4;
                    }
                    default -> b.append(e);
                }
            }
        }
        Number WO_KI_NB_JI_number() {
            int st = i;
            while (i < s.length() && "+-0123456789.eE".indexOf(s.charAt(i)) >= 0) i++;
            if (st == i) throw new WO_KI_NB_JI_JsonException("Valor inesperado en " + i);
            String t = s.substring(st, i);
            try {
                if (t.matches("-?\\d+") && t.length() < 18) return Long.parseLong(t);
                return Double.parseDouble(t);
            } catch (NumberFormatException e) { throw new WO_KI_NB_JI_JsonException("Número inválido: " + t); }
        }
    }
}

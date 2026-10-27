package com.agendapersonal.service;

import com.agendapersonal.domain.model.WO_KI_NB_JI_ClaseDocente;
import com.agendapersonal.domain.model.WO_KI_NB_JI_EstudianteClase;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

/** Importa y exporta libros XLSX sencillos para la lista de estudiantes y sus calificaciones. */
public final class WO_KI_NB_JI_ExcelClaseService {
    public record WO_KI_NB_JI_ResultadoImportacion(List<WO_KI_NB_JI_EstudianteClase> estudiantes, int filasOmitidas) {}

    private static final String SPREADSHEET_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String OFFICE_REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String PACKAGE_REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships";
    private WO_KI_NB_JI_ExcelClaseService() {}

    public static void WO_KI_NB_JI_exportar(Path destino, WO_KI_NB_JI_ClaseDocente clase, List<WO_KI_NB_JI_EstudianteClase> estudiantes) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(java.nio.file.Files.newOutputStream(destino), StandardCharsets.UTF_8)) {
            WO_KI_NB_JI_entrada(zip, "[Content_Types].xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                </Types>
                """);
            WO_KI_NB_JI_entrada(zip, "_rels/.rels", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """);
            WO_KI_NB_JI_entrada(zip, "xl/workbook.xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets><sheet name="Estudiantes" sheetId="1" r:id="rId1"/></sheets>
                </workbook>
                """);
            WO_KI_NB_JI_entrada(zip, "xl/_rels/workbook.xml.rels", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                </Relationships>
                """);
            StringBuilder sheet = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>
                  <sheetData>
                """);
            List<String> encabezados = List.of("Estudiante", "Correo", "Calificación", "Observaciones");
            sheet.append("<row r=\"1\">");
            for (int c = 0; c < encabezados.size(); c++) WO_KI_NB_JI_celdaTexto(sheet, c, 1, encabezados.get(c));
            sheet.append("</row>");
            int fila = 2;
            for (WO_KI_NB_JI_EstudianteClase estudiante : estudiantes) {
                sheet.append("<row r=\"").append(fila).append("\">");
                WO_KI_NB_JI_celdaTexto(sheet, 0, fila, estudiante.nombre());
                WO_KI_NB_JI_celdaTexto(sheet, 1, fila, estudiante.correo());
                if (estudiante.calificacion() != null && Double.isFinite(estudiante.calificacion()))
                    sheet.append("<c r=\"").append(WO_KI_NB_JI_referencia(2, fila)).append("\"><v>")
                        .append(Double.toString(estudiante.calificacion())).append("</v></c>");
                else WO_KI_NB_JI_celdaTexto(sheet, 2, fila, "");
                WO_KI_NB_JI_celdaTexto(sheet, 3, fila, estudiante.observaciones());
                sheet.append("</row>");
                fila++;
            }
            sheet.append("</sheetData>");
            if (fila > 2) sheet.append("<autoFilter ref=\"A1:D").append(fila - 1).append("\"/>");
            sheet.append("</worksheet>");
            WO_KI_NB_JI_entrada(zip, "xl/worksheets/sheet1.xml", sheet.toString());
        }
    }

    /**
     * Exporta una o varias tablas (una hoja por tabla) con título, encabezados en negrita, celdas con
     * borde y colores de estado (verde/rojo/amarillo/azul), primera columna ancha y encabezado fijo.
     */
    public static void WO_KI_NB_JI_exportarReporte(Path destino, List<WO_KI_NB_JI_TablaReporte> tablas) throws IOException {
        if (tablas.isEmpty()) throw new IOException("No hay datos para exportar.");
        try (ZipOutputStream zip = new ZipOutputStream(java.nio.file.Files.newOutputStream(destino), StandardCharsets.UTF_8)) {
            StringBuilder tipos = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                """);
            StringBuilder hojas = new StringBuilder();
            StringBuilder rels = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                """);
            java.util.Set<String> usados = new java.util.HashSet<>();
            for (int i = 1; i <= tablas.size(); i++) {
                tipos.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
                String nombre = WO_KI_NB_JI_nombreHoja(tablas.get(i - 1).hoja(), i);
                while (!usados.add(nombre.toLowerCase(Locale.ROOT))) nombre = WO_KI_NB_JI_nombreHoja(nombre.substring(0, Math.min(nombre.length(), 27)) + " " + i, i);
                hojas.append("<sheet name=\"").append(WO_KI_NB_JI_escapar(nombre)).append("\" sheetId=\"").append(i)
                    .append("\" r:id=\"rId").append(i).append("\"/>");
                rels.append("<Relationship Id=\"rId").append(i)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(i).append(".xml\"/>");
            }
            rels.append("<Relationship Id=\"rId").append(tablas.size() + 1)
                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
            tipos.append("</Types>");
            rels.append("</Relationships>");
            WO_KI_NB_JI_entrada(zip, "[Content_Types].xml", tipos.toString());
            WO_KI_NB_JI_entrada(zip, "_rels/.rels", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """);
            WO_KI_NB_JI_entrada(zip, "xl/workbook.xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets>""" + hojas + "</sheets></workbook>");
            WO_KI_NB_JI_entrada(zip, "xl/_rels/workbook.xml.rels", rels.toString());
            WO_KI_NB_JI_entrada(zip, "xl/styles.xml", ESTILOS);
            for (int i = 1; i <= tablas.size(); i++) WO_KI_NB_JI_entrada(zip, "xl/worksheets/sheet" + i + ".xml", WO_KI_NB_JI_hoja(tablas.get(i - 1)));
        }
    }

    // Índices de cellXfs de ESTILOS.
    private static final int XF_TITULO = 1, XF_SUBTITULO = 2, XF_ENCABEZADO = 3, XF_NORMAL = 4,
        XF_VERDE = 5, XF_ROJO = 6, XF_AMARILLO = 7, XF_AZUL = 8, XF_GRIS = 9;

    private static final String ESTILOS = """
        <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
        <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
          <fonts count="4">
            <font><sz val="11"/><name val="Calibri"/></font>
            <font><b/><sz val="11"/><name val="Calibri"/></font>
            <font><b/><sz val="15"/><name val="Calibri"/></font>
            <font><i/><sz val="10"/><color rgb="FF646978"/><name val="Calibri"/></font>
          </fonts>
          <fills count="8">
            <fill><patternFill patternType="none"/></fill>
            <fill><patternFill patternType="gray125"/></fill>
            <fill><patternFill patternType="solid"><fgColor rgb="FFE9E7E1"/><bgColor indexed="64"/></patternFill></fill>
            <fill><patternFill patternType="solid"><fgColor rgb="FFC6EFCE"/><bgColor indexed="64"/></patternFill></fill>
            <fill><patternFill patternType="solid"><fgColor rgb="FFFFC7CE"/><bgColor indexed="64"/></patternFill></fill>
            <fill><patternFill patternType="solid"><fgColor rgb="FFFFEB9C"/><bgColor indexed="64"/></patternFill></fill>
            <fill><patternFill patternType="solid"><fgColor rgb="FFDDEBF7"/><bgColor indexed="64"/></patternFill></fill>
            <fill><patternFill patternType="solid"><fgColor rgb="FFF2F2F2"/><bgColor indexed="64"/></patternFill></fill>
          </fills>
          <borders count="2">
            <border><left/><right/><top/><bottom/><diagonal/></border>
            <border><left style="thin"><color rgb="FFBFBFBF"/></left><right style="thin"><color rgb="FFBFBFBF"/></right><top style="thin"><color rgb="FFBFBFBF"/></top><bottom style="thin"><color rgb="FFBFBFBF"/></bottom><diagonal/></border>
          </borders>
          <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
          <cellXfs count="10">
            <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
            <xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1"/>
            <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1"/>
            <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
            <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>
            <xf numFmtId="0" fontId="1" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center"/></xf>
            <xf numFmtId="0" fontId="1" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center"/></xf>
            <xf numFmtId="0" fontId="1" fillId="5" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center"/></xf>
            <xf numFmtId="0" fontId="1" fillId="6" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center"/></xf>
            <xf numFmtId="0" fontId="0" fillId="7" borderId="1" xfId="0" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center"/></xf>
          </cellXfs>
          <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
        </styleSheet>
        """;

    private static String WO_KI_NB_JI_hoja(WO_KI_NB_JI_TablaReporte t) {
        int columnas = Math.max(1, t.encabezados().size());
        StringBuilder sb = new StringBuilder("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetViews><sheetView workbookViewId="0"><pane xSplit="1" ySplit="4" topLeftCell="B5" activePane="bottomRight" state="frozen"/></sheetView></sheetViews>
              <cols>""");
        sb.append("<col min=\"1\" max=\"1\" width=\"34\" customWidth=\"1\"/>");
        if (columnas > 1) sb.append("<col min=\"2\" max=\"").append(columnas).append("\" width=\"13\" customWidth=\"1\"/>");
        sb.append("</cols><sheetData>");
        sb.append("<row r=\"1\" ht=\"22\" customHeight=\"1\">");
        WO_KI_NB_JI_celda(sb, 0, 1, WO_KI_NB_JI_TablaReporte.WO_KI_NB_JI_Celda.WO_KI_NB_JI_texto(t.titulo()), XF_TITULO);
        sb.append("</row><row r=\"2\">");
        WO_KI_NB_JI_celda(sb, 0, 2, WO_KI_NB_JI_TablaReporte.WO_KI_NB_JI_Celda.WO_KI_NB_JI_texto(t.subtitulo()), XF_SUBTITULO);
        sb.append("</row><row r=\"4\" ht=\"30\" customHeight=\"1\">");
        for (int c = 0; c < t.encabezados().size(); c++) WO_KI_NB_JI_celda(sb, c, 4, WO_KI_NB_JI_TablaReporte.WO_KI_NB_JI_Celda.WO_KI_NB_JI_texto(t.encabezados().get(c)), XF_ENCABEZADO);
        sb.append("</row>");
        int fila = 5;
        for (List<WO_KI_NB_JI_TablaReporte.WO_KI_NB_JI_Celda> celdas : t.filas()) {
            sb.append("<row r=\"").append(fila).append("\">");
            for (int c = 0; c < celdas.size(); c++) WO_KI_NB_JI_celda(sb, c, fila, celdas.get(c), WO_KI_NB_JI_xfDe(celdas.get(c).tono()));
            sb.append("</row>");
            fila++;
        }
        sb.append("</sheetData>");
        if (fila > 5) sb.append("<autoFilter ref=\"A4:").append(WO_KI_NB_JI_referencia(columnas - 1, fila - 1)).append("\"/>");
        sb.append("<pageSetup orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/></worksheet>");
        return sb.toString();
    }

    private static int WO_KI_NB_JI_xfDe(WO_KI_NB_JI_TablaReporte.WO_KI_NB_JI_Tono tono) {
        return switch (tono) {
            case VERDE -> XF_VERDE; case ROJO -> XF_ROJO; case AMARILLO -> XF_AMARILLO;
            case AZUL -> XF_AZUL; case GRIS -> XF_GRIS; default -> XF_NORMAL;
        };
    }

    private static void WO_KI_NB_JI_celda(StringBuilder sb, int columna, int fila, WO_KI_NB_JI_TablaReporte.WO_KI_NB_JI_Celda c, int estilo) {
        if (c.numero() != null && Double.isFinite(c.numero())) {
            sb.append("<c r=\"").append(WO_KI_NB_JI_referencia(columna, fila)).append("\" s=\"").append(estilo).append("\"><v>")
                .append(Double.toString(c.numero())).append("</v></c>");
        } else {
            sb.append("<c r=\"").append(WO_KI_NB_JI_referencia(columna, fila)).append("\" s=\"").append(estilo)
                .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(WO_KI_NB_JI_escapar(c.texto() == null ? "" : c.texto()))
                .append("</t></is></c>");
        }
    }

    /** Excel limita el nombre de hoja a 31 caracteres y prohíbe \ / ? * [ ] : */
    private static String WO_KI_NB_JI_nombreHoja(String nombre, int indice) {
        String n = nombre == null ? "" : nombre.replaceAll("[\\\\/?*\\[\\]:]", " ").trim();
        if (n.isBlank()) n = "Hoja " + indice;
        return n.length() > 31 ? n.substring(0, 31) : n;
    }

    public static WO_KI_NB_JI_ResultadoImportacion WO_KI_NB_JI_importar(Path origen, int claseId) throws IOException {
        try (ZipFile zip = new ZipFile(origen.toFile(), StandardCharsets.UTF_8)) {
            Document workbook = WO_KI_NB_JI_documento(zip, "xl/workbook.xml");
            Element sheet = (Element) workbook.getElementsByTagNameNS(SPREADSHEET_NS, "sheet").item(0);
            if (sheet == null) throw new IOException("El libro no contiene hojas de cálculo.");
            String relationshipId = sheet.getAttributeNS(OFFICE_REL_NS, "id");
            Document rels = WO_KI_NB_JI_documento(zip, "xl/_rels/workbook.xml.rels");
            NodeList relationships = rels.getElementsByTagNameNS(PACKAGE_REL_NS, "Relationship");
            String target = null;
            for (int i = 0; i < relationships.getLength(); i++) {
                Element rel = (Element) relationships.item(i);
                if (relationshipId.equals(rel.getAttribute("Id"))) { target = rel.getAttribute("Target"); break; }
            }
            if (target == null || target.isBlank()) throw new IOException("No se encontró la primera hoja del libro.");
            String sheetPath = WO_KI_NB_JI_resolverRutaHoja(target);
            List<String> shared = zip.getEntry("xl/sharedStrings.xml") == null
                ? List.of() : WO_KI_NB_JI_leerSharedStrings(WO_KI_NB_JI_documento(zip, "xl/sharedStrings.xml"));
            List<List<String>> filas = WO_KI_NB_JI_leerFilas(WO_KI_NB_JI_documento(zip, sheetPath), shared);
            return WO_KI_NB_JI_mapear(filas, claseId);
        } catch (IllegalArgumentException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    private static WO_KI_NB_JI_ResultadoImportacion WO_KI_NB_JI_mapear(List<List<String>> filas, int claseId) {
        int cabecera = -1;
        Map<String, Integer> columnas = new HashMap<>();
        for (int i = 0; i < filas.size(); i++) {
            List<String> fila = filas.get(i);
            Map<String, Integer> posibles = new HashMap<>();
            for (int c = 0; c < fila.size(); c++) {
                String h = WO_KI_NB_JI_normalizar(fila.get(c));
                if (WO_KI_NB_JI_esNombre(h)) posibles.put("nombre", c);
                if (WO_KI_NB_JI_esCorreo(h)) posibles.put("correo", c);
                if (WO_KI_NB_JI_esNota(h)) posibles.put("calificacion", c);
                if (WO_KI_NB_JI_esObservacion(h)) posibles.put("observaciones", c);
            }
            if (posibles.containsKey("nombre")) { cabecera = i; columnas = posibles; break; }
        }
        if (cabecera < 0) throw new IllegalArgumentException("No encontré una columna de estudiante. Usa el encabezado «Estudiante» o «Nombre».");

        List<WO_KI_NB_JI_EstudianteClase> resultado = new ArrayList<>();
        int omitidas = 0;
        for (int r = cabecera + 1; r < filas.size(); r++) {
            List<String> fila = filas.get(r);
            String nombre = WO_KI_NB_JI_valor(fila, columnas.get("nombre")).trim();
            if (nombre.isBlank()) continue;
            String correo = WO_KI_NB_JI_valor(fila, columnas.get("correo")).trim();
            String notaTexto = WO_KI_NB_JI_valor(fila, columnas.get("calificacion")).trim();
            String observaciones = WO_KI_NB_JI_valor(fila, columnas.get("observaciones")).trim();
            Double nota = null;
            if (!notaTexto.isBlank()) {
                try { nota = Double.valueOf(notaTexto.replace(',', '.')); }
                catch (NumberFormatException e) { omitidas++; continue; }
                if (!Double.isFinite(nota)) { omitidas++; continue; }
            }
            resultado.add(new WO_KI_NB_JI_EstudianteClase(0, claseId, nombre, correo.isBlank() ? null : correo,
                nota, observaciones.isBlank() ? null : observaciones));
        }
        return new WO_KI_NB_JI_ResultadoImportacion(List.copyOf(resultado), omitidas);
    }

    private static List<List<String>> WO_KI_NB_JI_leerFilas(Document doc, List<String> shared) throws IOException {
        NodeList rows = doc.getElementsByTagNameNS(SPREADSHEET_NS, "row");
        List<List<String>> filas = new ArrayList<>();
        for (int i = 0; i < rows.getLength(); i++) {
            Element row = (Element) rows.item(i);
            List<String> values = new ArrayList<>();
            NodeList children = row.getChildNodes();
            for (int j = 0; j < children.getLength(); j++) {
                Node node = children.item(j);
                if (!(node instanceof Element cell) || !"c".equals(cell.getLocalName())) continue;
                int col = WO_KI_NB_JI_columna(cell.getAttribute("r"));
                while (values.size() <= col) values.add("");
                values.set(col, WO_KI_NB_JI_leerCelda(cell, shared));
            }
            filas.add(values);
        }
        return filas;
    }

    private static String WO_KI_NB_JI_leerCelda(Element cell, List<String> shared) throws IOException {
        String tipo = cell.getAttribute("t");
        if ("inlineStr".equals(tipo)) return WO_KI_NB_JI_textoDescendiente(cell, "t");
        NodeList values = cell.getElementsByTagNameNS(SPREADSHEET_NS, "v");
        if (values.getLength() == 0) return "";
        String value = values.item(0).getTextContent();
        if ("s".equals(tipo)) {
            try { return shared.get(Integer.parseInt(value)); }
            catch (RuntimeException e) { throw new IOException("El libro contiene una referencia de texto compartido inválida.", e); }
        }
        return value;
    }

    private static List<String> WO_KI_NB_JI_leerSharedStrings(Document doc) {
        NodeList items = doc.getElementsByTagNameNS(SPREADSHEET_NS, "si");
        List<String> salida = new ArrayList<>();
        for (int i = 0; i < items.getLength(); i++) salida.add(WO_KI_NB_JI_textoDescendiente((Element) items.item(i), "t"));
        return salida;
    }

    private static String WO_KI_NB_JI_textoDescendiente(Element raiz, String nombre) {
        NodeList textos = raiz.getElementsByTagNameNS(SPREADSHEET_NS, nombre);
        StringBuilder salida = new StringBuilder();
        for (int i = 0; i < textos.getLength(); i++) salida.append(textos.item(i).getTextContent());
        return salida.toString();
    }

    private static Document WO_KI_NB_JI_documento(ZipFile zip, String ruta) throws IOException {
        ZipEntry entry = zip.getEntry(ruta);
        if (entry == null) throw new IOException("El archivo no parece un Excel .xlsx válido (falta " + ruta + ").");
        try (InputStream in = zip.getInputStream(entry)) {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return f.newDocumentBuilder().parse(in);
        } catch (Exception e) { throw new IOException("No pude leer el contenido de Excel: " + e.getMessage(), e); }
    }

    private static String WO_KI_NB_JI_resolverRutaHoja(String target) {
        String normalized = target.replace('\\', '/');
        if (normalized.startsWith("/")) return normalized.substring(1);
        return Path.of("xl").resolve(normalized).normalize().toString().replace('\\', '/');
    }

    private static int WO_KI_NB_JI_columna(String referencia) {
        int n = 0;
        for (int i = 0; i < referencia.length(); i++) {
            char ch = referencia.charAt(i);
            if (!Character.isLetter(ch)) break;
            n = n * 26 + Character.toUpperCase(ch) - 'A' + 1;
        }
        return Math.max(0, n - 1);
    }

    private static String WO_KI_NB_JI_referencia(int columna, int fila) {
        StringBuilder letras = new StringBuilder();
        for (int n = columna + 1; n > 0; n = (n - 1) / 26) letras.insert(0, (char) ('A' + (n - 1) % 26));
        return letras + Integer.toString(fila);
    }

    private static void WO_KI_NB_JI_celdaTexto(StringBuilder sheet, int columna, int fila, String valor) {
        sheet.append("<c r=\"").append(WO_KI_NB_JI_referencia(columna, fila)).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
            .append(WO_KI_NB_JI_escapar(valor == null ? "" : valor)).append("</t></is></c>");
    }

    private static String WO_KI_NB_JI_escapar(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static void WO_KI_NB_JI_entrada(ZipOutputStream zip, String ruta, String contenido) throws IOException {
        zip.putNextEntry(new ZipEntry(ruta));
        zip.write(contenido.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String WO_KI_NB_JI_valor(List<String> fila, Integer indice) {
        return indice == null || indice < 0 || indice >= fila.size() ? "" : fila.get(indice);
    }

    private static String WO_KI_NB_JI_normalizar(String valor) {
        return Normalizer.normalize(valor == null ? "" : valor, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    private static boolean WO_KI_NB_JI_esNombre(String s) { return List.of("estudiante", "nombre", "alumno", "alumna").contains(s); }
    private static boolean WO_KI_NB_JI_esCorreo(String s) { return List.of("correo", "email", "e-mail", "mail").contains(s); }
    private static boolean WO_KI_NB_JI_esNota(String s) { return List.of("calificacion", "nota", "grade", "puntaje", "nota final").contains(s); }
    private static boolean WO_KI_NB_JI_esObservacion(String s) { return List.of("observaciones", "comentarios", "notes", "comentario").contains(s); }
}

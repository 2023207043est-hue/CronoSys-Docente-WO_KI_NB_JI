package com.agendapersonal.service;

import com.agendapersonal.domain.model.wkniClaseDocente;
import com.agendapersonal.domain.model.wkniEstudianteClase;
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
public final class wkniExcelClaseService {
    public record wkniResultadoImportacion(List<wkniEstudianteClase> wkniEstudiantes, int wkniFilasOmitidas) {}

    private static final String wkniSPREADSHEET_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String wkniOFFICE_REL_NS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String wkniPACKAGE_REL_NS = "http://schemas.openxmlformats.org/package/2006/relationships";
    private wkniExcelClaseService() {}

    public static void wkniExportar(Path wkniDestino, wkniClaseDocente wkniClase, List<wkniEstudianteClase> wkniEstudiantes) throws IOException {
        try (ZipOutputStream wkniZip = new ZipOutputStream(java.nio.file.Files.newOutputStream(wkniDestino), StandardCharsets.UTF_8)) {
            wkniEntrada(wkniZip, "[Content_Types].xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                </Types>
                """);
            wkniEntrada(wkniZip, "_rels/.rels", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """);
            wkniEntrada(wkniZip, "xl/workbook.xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets><sheet name="Estudiantes" sheetId="1" r:id="rId1"/></sheets>
                </workbook>
                """);
            wkniEntrada(wkniZip, "xl/_rels/workbook.xml.rels", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                </Relationships>
                """);
            StringBuilder wkniSheet = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetViews><sheetView workbookViewId="0"><pane ySplit="1" topLeftCell="A2" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>
                  <sheetData>
                """);
            List<String> wkniEncabezados = List.of("Estudiante", "Correo", "Calificación", "Observaciones");
            wkniSheet.append("<row r=\"1\">");
            for (int wkniC = 0; wkniC < wkniEncabezados.size(); wkniC++) wkniCeldaTexto(wkniSheet, wkniC, 1, wkniEncabezados.get(wkniC));
            wkniSheet.append("</row>");
            int wkniFila = 2;
            for (wkniEstudianteClase wkniEstudiante : wkniEstudiantes) {
                wkniSheet.append("<row r=\"").append(wkniFila).append("\">");
                wkniCeldaTexto(wkniSheet, 0, wkniFila, wkniEstudiante.wkniNombre());
                wkniCeldaTexto(wkniSheet, 1, wkniFila, wkniEstudiante.wkniCorreo());
                if (wkniEstudiante.wkniCalificacion() != null && Double.isFinite(wkniEstudiante.wkniCalificacion()))
                    wkniSheet.append("<c r=\"").append(wkniReferencia(2, wkniFila)).append("\"><v>")
                        .append(Double.toString(wkniEstudiante.wkniCalificacion())).append("</v></c>");
                else wkniCeldaTexto(wkniSheet, 2, wkniFila, "");
                wkniCeldaTexto(wkniSheet, 3, wkniFila, wkniEstudiante.wkniObservaciones());
                wkniSheet.append("</row>");
                wkniFila++;
            }
            wkniSheet.append("</sheetData>");
            if (wkniFila > 2) wkniSheet.append("<autoFilter ref=\"A1:D").append(wkniFila - 1).append("\"/>");
            wkniSheet.append("</worksheet>");
            wkniEntrada(wkniZip, "xl/worksheets/sheet1.xml", wkniSheet.toString());
        }
    }

    /**
     * Exporta una o varias tablas (una hoja por tabla) con título, encabezados en negrita, celdas con
     * borde y colores de estado (verde/rojo/amarillo/azul), primera columna ancha y encabezado fijo.
     */
    public static void wkniExportarReporte(Path wkniDestino, List<wkniTablaReporte> wkniTablas) throws IOException {
        if (wkniTablas.isEmpty()) throw new IOException("No hay datos para exportar.");
        try (ZipOutputStream wkniZip = new ZipOutputStream(java.nio.file.Files.newOutputStream(wkniDestino), StandardCharsets.UTF_8)) {
            StringBuilder wkniTipos = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                  <Default Extension="xml" ContentType="application/xml"/>
                  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                """);
            StringBuilder wkniHojas = new StringBuilder();
            StringBuilder wkniRels = new StringBuilder("""
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                """);
            java.util.Set<String> wkniUsados = new java.util.HashSet<>();
            for (int wkniI = 1; wkniI <= wkniTablas.size(); wkniI++) {
                wkniTipos.append("<Override PartName=\"/xl/worksheets/sheet").append(wkniI)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
                String wkniNombre = wkniNombreHoja(wkniTablas.get(wkniI - 1).wkniHoja(), wkniI);
                while (!wkniUsados.add(wkniNombre.toLowerCase(Locale.ROOT))) wkniNombre = wkniNombreHoja(wkniNombre.substring(0, Math.min(wkniNombre.length(), 27)) + " " + wkniI, wkniI);
                wkniHojas.append("<sheet name=\"").append(wkniEscapar(wkniNombre)).append("\" sheetId=\"").append(wkniI)
                    .append("\" r:id=\"rId").append(wkniI).append("\"/>");
                wkniRels.append("<Relationship Id=\"rId").append(wkniI)
                    .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet")
                    .append(wkniI).append(".xml\"/>");
            }
            wkniRels.append("<Relationship Id=\"rId").append(wkniTablas.size() + 1)
                .append("\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>");
            wkniTipos.append("</Types>");
            wkniRels.append("</Relationships>");
            wkniEntrada(wkniZip, "[Content_Types].xml", wkniTipos.toString());
            wkniEntrada(wkniZip, "_rels/.rels", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                </Relationships>
                """);
            wkniEntrada(wkniZip, "xl/workbook.xml", """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                  <sheets>""" + wkniHojas + "</sheets></workbook>");
            wkniEntrada(wkniZip, "xl/_rels/workbook.xml.rels", wkniRels.toString());
            wkniEntrada(wkniZip, "xl/styles.xml", wkniESTILOS);
            for (int wkniI = 1; wkniI <= wkniTablas.size(); wkniI++) wkniEntrada(wkniZip, "xl/worksheets/sheet" + wkniI + ".xml", wkniHoja(wkniTablas.get(wkniI - 1)));
        }
    }

    // Índices de cellXfs de ESTILOS.
    private static final int wkniXF_TITULO = 1, wkniXF_SUBTITULO = 2, wkniXF_ENCABEZADO = 3, wkniXF_NORMAL = 4,
        wkniXF_VERDE = 5, wkniXF_ROJO = 6, wkniXF_AMARILLO = 7, wkniXF_AZUL = 8, wkniXF_GRIS = 9;

    private static final String wkniESTILOS = """
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

    private static String wkniHoja(wkniTablaReporte wkniT) {
        int wkniColumnas = Math.max(1, wkniT.wkniEncabezados().size());
        StringBuilder wkniSb = new StringBuilder("""
            <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
            <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
              <sheetViews><sheetView workbookViewId="0"><pane xSplit="1" ySplit="4" topLeftCell="B5" activePane="bottomRight" state="frozen"/></sheetView></sheetViews>
              <cols>""");
        wkniSb.append("<col min=\"1\" max=\"1\" width=\"34\" customWidth=\"1\"/>");
        if (wkniColumnas > 1) wkniSb.append("<col min=\"2\" max=\"").append(wkniColumnas).append("\" width=\"13\" customWidth=\"1\"/>");
        wkniSb.append("</cols><sheetData>");
        wkniSb.append("<row r=\"1\" ht=\"22\" customHeight=\"1\">");
        wkniCelda(wkniSb, 0, 1, wkniTablaReporte.wkniCelda.wkniTexto(wkniT.wkniTitulo()), wkniXF_TITULO);
        wkniSb.append("</row><row r=\"2\">");
        wkniCelda(wkniSb, 0, 2, wkniTablaReporte.wkniCelda.wkniTexto(wkniT.wkniSubtitulo()), wkniXF_SUBTITULO);
        wkniSb.append("</row><row r=\"4\" ht=\"30\" customHeight=\"1\">");
        for (int wkniC = 0; wkniC < wkniT.wkniEncabezados().size(); wkniC++) wkniCelda(wkniSb, wkniC, 4, wkniTablaReporte.wkniCelda.wkniTexto(wkniT.wkniEncabezados().get(wkniC)), wkniXF_ENCABEZADO);
        wkniSb.append("</row>");
        int wkniFila = 5;
        for (List<wkniTablaReporte.wkniCelda> wkniCeldas : wkniT.wkniFilas()) {
            wkniSb.append("<row r=\"").append(wkniFila).append("\">");
            for (int wkniC = 0; wkniC < wkniCeldas.size(); wkniC++) wkniCelda(wkniSb, wkniC, wkniFila, wkniCeldas.get(wkniC), wkniXfDe(wkniCeldas.get(wkniC).wkni_tono()));
            wkniSb.append("</row>");
            wkniFila++;
        }
        wkniSb.append("</sheetData>");
        if (wkniFila > 5) wkniSb.append("<autoFilter ref=\"A4:").append(wkniReferencia(wkniColumnas - 1, wkniFila - 1)).append("\"/>");
        wkniSb.append("<pageSetup orientation=\"landscape\" fitToWidth=\"1\" fitToHeight=\"0\"/></worksheet>");
        return wkniSb.toString();
    }

    private static int wkniXfDe(wkniTablaReporte.wkniTono wkni_tono) {
        return switch (wkni_tono) {
            case VERDE -> wkniXF_VERDE; case ROJO -> wkniXF_ROJO; case AMARILLO -> wkniXF_AMARILLO;
            case AZUL -> wkniXF_AZUL; case GRIS -> wkniXF_GRIS; default -> wkniXF_NORMAL;
        };
    }

    private static void wkniCelda(StringBuilder wkniSb, int wkniColumna, int wkniFila, wkniTablaReporte.wkniCelda wkniC, int wkniEstilo) {
        if (wkniC.wkniNumero() != null && Double.isFinite(wkniC.wkniNumero())) {
            wkniSb.append("<c r=\"").append(wkniReferencia(wkniColumna, wkniFila)).append("\" s=\"").append(wkniEstilo).append("\"><v>")
                .append(Double.toString(wkniC.wkniNumero())).append("</v></c>");
        } else {
            wkniSb.append("<c r=\"").append(wkniReferencia(wkniColumna, wkniFila)).append("\" s=\"").append(wkniEstilo)
                .append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">").append(wkniEscapar(wkniC.wkniTexto() == null ? "" : wkniC.wkniTexto()))
                .append("</t></is></c>");
        }
    }

    /** Excel limita el nombre de hoja a 31 caracteres y prohíbe \ / ? * [ ] : */
    private static String wkniNombreHoja(String wkniNombre, int wkniIndice) {
        String wkniN = wkniNombre == null ? "" : wkniNombre.replaceAll("[\\\\/?*\\[\\]:]", " ").trim();
        if (wkniN.isBlank()) wkniN = "Hoja " + wkniIndice;
        return wkniN.length() > 31 ? wkniN.substring(0, 31) : wkniN;
    }

    public static wkniResultadoImportacion wkniImportar(Path wkniOrigen, int wkniClaseId) throws IOException {
        try (ZipFile wkniZip = new ZipFile(wkniOrigen.toFile(), StandardCharsets.UTF_8)) {
            Document wkniWorkbook = wkniDocumento(wkniZip, "xl/workbook.xml");
            Element wkniSheet = (Element) wkniWorkbook.getElementsByTagNameNS(wkniSPREADSHEET_NS, "sheet").item(0);
            if (wkniSheet == null) throw new IOException("El libro no contiene hojas de cálculo.");
            String wkniRelationshipId = wkniSheet.getAttributeNS(wkniOFFICE_REL_NS, "id");
            Document wkniRels = wkniDocumento(wkniZip, "xl/_rels/workbook.xml.rels");
            NodeList wkniRelationships = wkniRels.getElementsByTagNameNS(wkniPACKAGE_REL_NS, "Relationship");
            String wkniTarget = null;
            for (int wkniI = 0; wkniI < wkniRelationships.getLength(); wkniI++) {
                Element wkniRel = (Element) wkniRelationships.item(wkniI);
                if (wkniRelationshipId.equals(wkniRel.getAttribute("Id"))) { wkniTarget = wkniRel.getAttribute("Target"); break; }
            }
            if (wkniTarget == null || wkniTarget.isBlank()) throw new IOException("No se encontró la primera hoja del libro.");
            String wkniSheetPath = wkniResolverRutaHoja(wkniTarget);
            List<String> wkniShared = wkniZip.getEntry("xl/sharedStrings.xml") == null
                ? List.of() : wkniLeerSharedStrings(wkniDocumento(wkniZip, "xl/sharedStrings.xml"));
            List<List<String>> wkniFilas = wkniLeerFilas(wkniDocumento(wkniZip, wkniSheetPath), wkniShared);
            return wkniMapear(wkniFilas, wkniClaseId);
        } catch (IllegalArgumentException wkniE) {
            throw new IOException(wkniE.getMessage(), wkniE);
        }
    }

    private static wkniResultadoImportacion wkniMapear(List<List<String>> wkniFilas, int wkniClaseId) {
        int wkniCabecera = -1;
        Map<String, Integer> wkniColumnas = new HashMap<>();
        for (int wkniI = 0; wkniI < wkniFilas.size(); wkniI++) {
            List<String> wkniFila = wkniFilas.get(wkniI);
            Map<String, Integer> wkniPosibles = new HashMap<>();
            for (int wkniC = 0; wkniC < wkniFila.size(); wkniC++) {
                String wkniH = wkniNormalizar(wkniFila.get(wkniC));
                if (wkniEsNombre(wkniH)) wkniPosibles.put("nombre", wkniC);
                if (wkniEsCorreo(wkniH)) wkniPosibles.put("correo", wkniC);
                if (wkniEsNota(wkniH)) wkniPosibles.put("calificacion", wkniC);
                if (wkniEsObservacion(wkniH)) wkniPosibles.put("observaciones", wkniC);
            }
            if (wkniPosibles.containsKey("nombre")) { wkniCabecera = wkniI; wkniColumnas = wkniPosibles; break; }
        }
        if (wkniCabecera < 0) throw new IllegalArgumentException("No encontré una columna de estudiante. Usa el encabezado «Estudiante» o «Nombre».");

        List<wkniEstudianteClase> wkni_resultado = new ArrayList<>();
        int wkniOmitidas = 0;
        for (int wkniR = wkniCabecera + 1; wkniR < wkniFilas.size(); wkniR++) {
            List<String> wkniFila = wkniFilas.get(wkniR);
            String wkniNombre = wkniValor(wkniFila, wkniColumnas.get("nombre")).trim();
            if (wkniNombre.isBlank()) continue;
            String wkniCorreo = wkniValor(wkniFila, wkniColumnas.get("correo")).trim();
            String wkniNotaTexto = wkniValor(wkniFila, wkniColumnas.get("calificacion")).trim();
            String wkniObservaciones = wkniValor(wkniFila, wkniColumnas.get("observaciones")).trim();
            Double wkni_nota = null;
            if (!wkniNotaTexto.isBlank()) {
                try { wkni_nota = Double.valueOf(wkniNotaTexto.replace(',', '.')); }
                catch (NumberFormatException wkniE) { wkniOmitidas++; continue; }
                if (!Double.isFinite(wkni_nota)) { wkniOmitidas++; continue; }
            }
            wkni_resultado.add(new wkniEstudianteClase(0, wkniClaseId, wkniNombre, wkniCorreo.isBlank() ? null : wkniCorreo,
                wkni_nota, wkniObservaciones.isBlank() ? null : wkniObservaciones));
        }
        return new wkniResultadoImportacion(List.copyOf(wkni_resultado), wkniOmitidas);
    }

    private static List<List<String>> wkniLeerFilas(Document wkniDoc, List<String> wkniShared) throws IOException {
        NodeList wkniRows = wkniDoc.getElementsByTagNameNS(wkniSPREADSHEET_NS, "row");
        List<List<String>> wkniFilas = new ArrayList<>();
        for (int wkniI = 0; wkniI < wkniRows.getLength(); wkniI++) {
            Element wkniRow = (Element) wkniRows.item(wkniI);
            List<String> wkniValues = new ArrayList<>();
            NodeList wkniChildren = wkniRow.getChildNodes();
            for (int wkniJ = 0; wkniJ < wkniChildren.getLength(); wkniJ++) {
                Node wkniNode = wkniChildren.item(wkniJ);
                if (!(wkniNode instanceof Element wkniCell) || !"c".equals(wkniCell.getLocalName())) continue;
                int wkniCol = wkniColumna(wkniCell.getAttribute("r"));
                while (wkniValues.size() <= wkniCol) wkniValues.add("");
                wkniValues.set(wkniCol, wkniLeerCelda(wkniCell, wkniShared));
            }
            wkniFilas.add(wkniValues);
        }
        return wkniFilas;
    }

    private static String wkniLeerCelda(Element wkniCell, List<String> wkniShared) throws IOException {
        String wkni_tipo = wkniCell.getAttribute("t");
        if ("inlineStr".equals(wkni_tipo)) return wkniTextoDescendiente(wkniCell, "t");
        NodeList wkniValues = wkniCell.getElementsByTagNameNS(wkniSPREADSHEET_NS, "v");
        if (wkniValues.getLength() == 0) return "";
        String wkniValue = wkniValues.item(0).getTextContent();
        if ("s".equals(wkni_tipo)) {
            try { return wkniShared.get(Integer.parseInt(wkniValue)); }
            catch (RuntimeException wkniE) { throw new IOException("El libro contiene una referencia de texto compartido inválida.", wkniE); }
        }
        return wkniValue;
    }

    private static List<String> wkniLeerSharedStrings(Document wkniDoc) {
        NodeList wkniItems = wkniDoc.getElementsByTagNameNS(wkniSPREADSHEET_NS, "si");
        List<String> wkniSalida = new ArrayList<>();
        for (int wkniI = 0; wkniI < wkniItems.getLength(); wkniI++) wkniSalida.add(wkniTextoDescendiente((Element) wkniItems.item(wkniI), "t"));
        return wkniSalida;
    }

    private static String wkniTextoDescendiente(Element wkniRaiz, String wkniNombre) {
        NodeList wkniTextos = wkniRaiz.getElementsByTagNameNS(wkniSPREADSHEET_NS, wkniNombre);
        StringBuilder wkniSalida = new StringBuilder();
        for (int wkniI = 0; wkniI < wkniTextos.getLength(); wkniI++) wkniSalida.append(wkniTextos.item(wkniI).getTextContent());
        return wkniSalida.toString();
    }

    private static Document wkniDocumento(ZipFile wkniZip, String wkniRuta) throws IOException {
        ZipEntry wkni_entry = wkniZip.getEntry(wkniRuta);
        if (wkni_entry == null) throw new IOException("El archivo no parece un Excel .xlsx válido (falta " + wkniRuta + ").");
        try (InputStream wkniIn = wkniZip.getInputStream(wkni_entry)) {
            DocumentBuilderFactory wkniF = DocumentBuilderFactory.newInstance();
            wkniF.setNamespaceAware(true);
            wkniF.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            wkniF.setFeature("http://xml.org/sax/features/external-general-entities", false);
            wkniF.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            wkniF.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            wkniF.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            return wkniF.newDocumentBuilder().parse(wkniIn);
        } catch (Exception wkniE) { throw new IOException("No pude leer el contenido de Excel: " + wkniE.getMessage(), wkniE); }
    }

    private static String wkniResolverRutaHoja(String wkniTarget) {
        String wkniNormalized = wkniTarget.replace('\\', '/');
        if (wkniNormalized.startsWith("/")) return wkniNormalized.substring(1);
        return Path.of("xl").resolve(wkniNormalized).normalize().toString().replace('\\', '/');
    }

    private static int wkniColumna(String wkniReferencia) {
        int wkniN = 0;
        for (int wkniI = 0; wkniI < wkniReferencia.length(); wkniI++) {
            char wkniCh = wkniReferencia.charAt(wkniI);
            if (!Character.isLetter(wkniCh)) break;
            wkniN = wkniN * 26 + Character.toUpperCase(wkniCh) - 'A' + 1;
        }
        return Math.max(0, wkniN - 1);
    }

    private static String wkniReferencia(int wkniColumna, int wkniFila) {
        StringBuilder wkniLetras = new StringBuilder();
        for (int wkniN = wkniColumna + 1; wkniN > 0; wkniN = (wkniN - 1) / 26) wkniLetras.insert(0, (char) ('A' + (wkniN - 1) % 26));
        return wkniLetras + Integer.toString(wkniFila);
    }

    private static void wkniCeldaTexto(StringBuilder wkniSheet, int wkniColumna, int wkniFila, String wkniValor) {
        wkniSheet.append("<c r=\"").append(wkniReferencia(wkniColumna, wkniFila)).append("\" t=\"inlineStr\"><is><t xml:space=\"preserve\">")
            .append(wkniEscapar(wkniValor == null ? "" : wkniValor)).append("</t></is></c>");
    }

    private static String wkniEscapar(String wkniS) {
        return wkniS.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&apos;");
    }

    private static void wkniEntrada(ZipOutputStream wkniZip, String wkniRuta, String wkniContenido) throws IOException {
        wkniZip.putNextEntry(new ZipEntry(wkniRuta));
        wkniZip.write(wkniContenido.getBytes(StandardCharsets.UTF_8));
        wkniZip.closeEntry();
    }

    private static String wkniValor(List<String> wkniFila, Integer wkniIndice) {
        return wkniIndice == null || wkniIndice < 0 || wkniIndice >= wkniFila.size() ? "" : wkniFila.get(wkniIndice);
    }

    private static String wkniNormalizar(String wkniValor) {
        return Normalizer.normalize(wkniValor == null ? "" : wkniValor, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }

    private static boolean wkniEsNombre(String wkniS) { return List.of("estudiante", "nombre", "alumno", "alumna").contains(wkniS); }
    private static boolean wkniEsCorreo(String wkniS) { return List.of("correo", "email", "e-mail", "mail").contains(wkniS); }
    private static boolean wkniEsNota(String wkniS) { return List.of("calificacion", "nota", "grade", "puntaje", "nota final").contains(wkniS); }
    private static boolean wkniEsObservacion(String wkniS) { return List.of("observaciones", "comentarios", "notes", "comentario").contains(wkniS); }
}

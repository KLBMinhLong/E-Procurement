package com.eprocure.analytics.infrastructure.report;

import com.eprocure.analytics.application.port.out.RenderedReport;
import com.eprocure.analytics.application.port.out.ReportDataset;
import com.eprocure.analytics.application.port.out.ReportDatasetRow;
import com.eprocure.analytics.application.port.out.ReportFileRenderer;
import com.eprocure.analytics.domain.model.report.ReportFilterCriteria;
import com.eprocure.analytics.domain.model.report.ReportFormat;
import com.eprocure.analytics.domain.model.report.ReportJob;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalReportFileRenderer implements ReportFileRenderer {
    private static final int PDF_MAX_DATASET_ROWS = 28;

    private final Path storageDirectory;

    public LocalReportFileRenderer(
            @Value("${eprocure.analytics.report-worker.storage-dir:${java.io.tmpdir}/eprocure-analytics-reports}")
            String storageDirectory) {
        this(Path.of(storageDirectory));
    }

    LocalReportFileRenderer(Path storageDirectory) {
        this.storageDirectory = storageDirectory;
    }

    @Override
    public RenderedReport render(ReportJob job, ReportDataset dataset) {
        try {
            Files.createDirectories(storageDirectory);
            String extension = job.format() == ReportFormat.PDF ? "pdf" : "xlsx";
            Path output = storageDirectory.resolve(job.id() + "-" + job.reportType().name().toLowerCase() + "." + extension)
                    .normalize();
            byte[] bytes = job.format() == ReportFormat.PDF
                    ? renderPdf(job, dataset)
                    : renderXlsx(job, dataset);
            Files.write(output, bytes);
            return new RenderedReport(output.toString(), "/api/v1/reports/jobs/" + job.id() + "/download");
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot render report file", exception);
        }
    }

    private byte[] renderPdf(ReportJob job, ReportDataset dataset) {
        ReportLayoutTemplate template = ReportLayoutTemplate.resolve(job.reportType());
        StringBuilder content = new StringBuilder();
        int y = 760;
        appendText(content, "F2", 18, 72, y, template.title());
        y -= 18;
        appendText(content, "F1", 10, 72, y, template.subtitle());
        y -= 28;
        appendText(content, "F2", 11, 72, y, "Report Metadata");
        y -= 18;
        for (List<String> row : metadataRows(job)) {
            appendText(content, "F2", 9, 72, y, truncate(row.get(0) + ":", 28));
            appendText(content, "F1", 9, 190, y, truncate(row.get(1), 64));
            y -= 14;
        }
        y -= 10;
        appendText(content, "F2", 12, 72, y, template.datasetSectionTitle());
        y -= 18;
        appendText(content, "F2", 9, 72, y, "Metric");
        appendText(content, "F2", 9, 310, y, "Value");
        y -= 14;
        int rowCount = 0;
        for (ReportDatasetRow row : datasetRows(dataset)) {
            if (rowCount >= PDF_MAX_DATASET_ROWS || y < 72) {
                appendText(content, "F1", 9, 72, y, "Additional rows truncated in PDF output.");
                break;
            }
            appendText(content, "F1", 9, 72, y, truncate(row.label(), 46));
            appendText(content, "F1", 9, 310, y, truncate(row.value(), 42));
            y -= 14;
            rowCount++;
        }
        byte[] contentBytes = content.toString().getBytes(StandardCharsets.ISO_8859_1);
        List<byte[]> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.ISO_8859_1),
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>".getBytes(StandardCharsets.ISO_8859_1),
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R /F2 5 0 R >> >> /Contents 6 0 R >>"
                        .getBytes(StandardCharsets.ISO_8859_1),
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>".getBytes(StandardCharsets.ISO_8859_1),
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>".getBytes(StandardCharsets.ISO_8859_1),
                ("<< /Length " + contentBytes.length + " >>\nstream\n" + content + "endstream")
                        .getBytes(StandardCharsets.ISO_8859_1));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeAscii(output, "%PDF-1.4\n");
        List<Integer> offsets = new ArrayList<>();
        for (int index = 0; index < objects.size(); index++) {
            offsets.add(output.size());
            writeAscii(output, (index + 1) + " 0 obj\n");
            writeBytes(output, objects.get(index));
            writeAscii(output, "\nendobj\n");
        }
        int xrefOffset = output.size();
        writeAscii(output, "xref\n0 " + (objects.size() + 1) + "\n");
        writeAscii(output, "0000000000 65535 f \n");
        for (Integer offset : offsets) {
            writeAscii(output, String.format("%010d 00000 n \n", offset));
        }
        writeAscii(output, "trailer\n<< /Size " + (objects.size() + 1) + " /Root 1 0 R >>\n");
        writeAscii(output, "startxref\n" + xrefOffset + "\n%%EOF\n");
        return output.toByteArray();
    }

    private byte[] renderXlsx(ReportJob job, ReportDataset dataset) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            put(zip, "[Content_Types].xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
                      <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
                      <Default Extension="xml" ContentType="application/xml"/>
                      <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
                      <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
                      <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
                    </Types>
                    """);
            put(zip, "_rels/.rels", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
                    </Relationships>
                    """);
            put(zip, "xl/workbook.xml", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
                      <sheets><sheet name="Report" sheetId="1" r:id="rId1"/></sheets>
                    </workbook>
                    """);
            put(zip, "xl/_rels/workbook.xml.rels", """
                    <?xml version="1.0" encoding="UTF-8"?>
                    <Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
                      <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
                      <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
                    </Relationships>
                    """);
            put(zip, "xl/styles.xml", stylesXml());
            put(zip, "xl/worksheets/sheet1.xml", sheetXml(job, dataset));
        }
        return output.toByteArray();
    }

    private String sheetXml(ReportJob job, ReportDataset dataset) {
        List<SheetRow> lines = sheetRows(job, dataset);
        StringBuilder rows = new StringBuilder();
        for (int index = 0; index < lines.size(); index++) {
            int row = index + 1;
            rows.append("<row r=\"").append(row).append("\">");
            List<SheetCell> cells = lines.get(index).cells();
            for (int cellIndex = 0; cellIndex < cells.size(); cellIndex++) {
                SheetCell cell = cells.get(cellIndex);
                rows.append("<c r=\"")
                        .append(columnName(cellIndex))
                        .append(row)
                        .append("\"");
                if (cell.styleIndex() > 0) {
                    rows.append(" s=\"").append(cell.styleIndex()).append("\"");
                }
                rows.append(" t=\"inlineStr\"><is><t>")
                        .append(escapeXml(cell.value()))
                        .append("</t></is></c>");
            }
            rows.append("</row>");
        }
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetViews><sheetView workbookViewId="0"><pane ySplit="8" topLeftCell="A9" activePane="bottomLeft" state="frozen"/></sheetView></sheetViews>
                  <cols>
                    <col min="1" max="1" width="34" customWidth="1"/>
                    <col min="2" max="2" width="58" customWidth="1"/>
                  </cols>
                  <sheetData>%s</sheetData>
                </worksheet>
                """.formatted(rows);
    }

    private List<SheetRow> sheetRows(ReportJob job, ReportDataset dataset) {
        ReportLayoutTemplate template = ReportLayoutTemplate.resolve(job.reportType());
        List<SheetRow> rows = new ArrayList<>();
        rows.add(sheetRow(1, template.title(), ""));
        rows.add(sheetRow(0, template.subtitle(), ""));
        rows.add(sheetRow(0, "", ""));
        rows.add(sheetRow(2, "Report Metadata", ""));
        for (List<String> row : metadataRows(job)) {
            rows.add(sheetRow(0, row.get(0), row.get(1)));
        }
        rows.add(sheetRow(0, "", ""));
        rows.add(sheetRow(2, template.datasetSectionTitle(), ""));
        rows.add(sheetRow(3, "Metric", "Value"));
        for (ReportDatasetRow row : datasetRows(dataset)) {
            rows.add(sheetRow(0, row.label(), row.value()));
        }
        return rows;
    }

    private String stylesXml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <fonts count="2">
                    <font><sz val="11"/><name val="Calibri"/></font>
                    <font><b/><sz val="11"/><name val="Calibri"/></font>
                  </fonts>
                  <fills count="2"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill></fills>
                  <borders count="1"><border/></borders>
                  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
                  <cellXfs count="4">
                    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
                    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
                    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
                    <xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
                  </cellXfs>
                  <cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
                </styleSheet>
                """;
    }

    private List<List<String>> metadataRows(ReportJob job) {
        return List.of(
                List.of("Report type", job.reportType().name()),
                List.of("Format", job.format().name()),
                List.of("Job ID", job.id().toString()),
                List.of("Created at", job.createdAt().toString()),
                List.of("Period filter", periodFilter(job.filterCriteria())),
                List.of("Scope filter", scopeFilter(job.filterCriteria())));
    }

    private String periodFilter(ReportFilterCriteria filters) {
        if (filters.fromDate() != null || filters.toDate() != null) {
            String from = filters.fromDate() == null ? "beginning" : filters.fromDate().toString();
            String to = filters.toDate() == null ? "open end" : filters.toDate().toString();
            return from + " to " + to;
        }
        if (filters.fiscalYear() == null) {
            return "All periods";
        }
        return filters.quarter() == null
                ? "FY " + filters.fiscalYear()
                : "FY " + filters.fiscalYear() + " Q" + filters.quarter();
    }

    private String scopeFilter(ReportFilterCriteria filters) {
        List<String> scopes = new ArrayList<>();
        if (filters.vendorId() != null) {
            scopes.add("Vendor " + filters.vendorId());
        }
        if (filters.categoryCode() != null) {
            scopes.add("Category " + filters.categoryCode());
        }
        return scopes.isEmpty() ? "All vendors and categories" : String.join("; ", scopes);
    }

    private SheetRow sheetRow(int styleIndex, String first, String second) {
        return new SheetRow(List.of(new SheetCell(first, styleIndex), new SheetCell(second, styleIndex)));
    }

    private List<ReportDatasetRow> datasetRows(ReportDataset dataset) {
        if (dataset == null || dataset.rows().isEmpty()) {
            return List.of(new ReportDatasetRow("Dataset source", "No projection data available"));
        }
        return dataset.rows();
    }

    private String columnName(int zeroBasedIndex) {
        return String.valueOf((char) ('A' + zeroBasedIndex));
    }

    private void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private String escapePdf(String value) {
        return sanitizeText(value).replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private void appendText(StringBuilder content, String font, int size, int x, int y, String value) {
        content.append("BT\n/")
                .append(font)
                .append(' ')
                .append(size)
                .append(" Tf\n")
                .append(x)
                .append(' ')
                .append(y)
                .append(" Td\n(")
                .append(escapePdf(value))
                .append(") Tj\nET\n");
    }

    private String escapeXml(String value) {
        return sanitizeText(value)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String sanitizeText(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
    }

    private String truncate(String value, int maxLength) {
        String sanitized = sanitizeText(value);
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength - 3) + "...";
    }

    private void writeAscii(ByteArrayOutputStream output, String value) {
        writeBytes(output, value.getBytes(StandardCharsets.ISO_8859_1));
    }

    private void writeBytes(ByteArrayOutputStream output, byte[] bytes) {
        output.write(bytes, 0, bytes.length);
    }

    private record SheetRow(List<SheetCell> cells) {
    }

    private record SheetCell(String value, int styleIndex) {
    }
}

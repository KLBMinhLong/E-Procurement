package com.eprocure.analytics.infrastructure.report;

import com.eprocure.analytics.application.port.out.RenderedReport;
import com.eprocure.analytics.application.port.out.ReportDataset;
import com.eprocure.analytics.application.port.out.ReportDatasetRow;
import com.eprocure.analytics.application.port.out.ReportFileRenderer;
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
        List<String> lines = reportLines(job, dataset);
        StringBuilder content = new StringBuilder();
        content.append("BT\n/F1 12 Tf\n72 760 Td\n");
        for (String line : lines) {
            content.append("(").append(escapePdf(line)).append(") Tj\n0 -18 Td\n");
        }
        content.append("ET\n");
        byte[] contentBytes = content.toString().getBytes(StandardCharsets.ISO_8859_1);
        List<byte[]> objects = List.of(
                "<< /Type /Catalog /Pages 2 0 R >>".getBytes(StandardCharsets.ISO_8859_1),
                "<< /Type /Pages /Kids [3 0 R] /Count 1 >>".getBytes(StandardCharsets.ISO_8859_1),
                "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 612 792] /Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>"
                        .getBytes(StandardCharsets.ISO_8859_1),
                "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>".getBytes(StandardCharsets.ISO_8859_1),
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
                    </Relationships>
                    """);
            put(zip, "xl/worksheets/sheet1.xml", sheetXml(job, dataset));
        }
        return output.toByteArray();
    }

    private String sheetXml(ReportJob job, ReportDataset dataset) {
        List<List<String>> lines = sheetRows(job, dataset);
        StringBuilder rows = new StringBuilder();
        for (int index = 0; index < lines.size(); index++) {
            int row = index + 1;
            rows.append("<row r=\"").append(row).append("\">");
            List<String> cells = lines.get(index);
            for (int cellIndex = 0; cellIndex < cells.size(); cellIndex++) {
                rows.append("<c r=\"")
                        .append(columnName(cellIndex))
                        .append(row)
                        .append("\" t=\"inlineStr\"><is><t>")
                        .append(escapeXml(cells.get(cellIndex)))
                        .append("</t></is></c>");
            }
            rows.append("</row>");
        }
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                  <sheetData>%s</sheetData>
                </worksheet>
                """.formatted(rows);
    }

    private List<String> reportLines(ReportJob job, ReportDataset dataset) {
        List<String> lines = new ArrayList<>();
        lines.add("eProcure Analytics Report");
        lines.add("Report type: " + job.reportType());
        lines.add("Format: " + job.format());
        lines.add("Job ID: " + job.id());
        lines.add("Created at: " + job.createdAt());
        lines.add("Dataset");
        for (ReportDatasetRow row : datasetRows(dataset)) {
            lines.add(row.label() + ": " + row.value());
        }
        return lines;
    }

    private List<List<String>> sheetRows(ReportJob job, ReportDataset dataset) {
        List<List<String>> rows = new ArrayList<>();
        rows.add(List.of("eProcure Analytics Report", ""));
        rows.add(List.of("Report type", job.reportType().name()));
        rows.add(List.of("Format", job.format().name()));
        rows.add(List.of("Job ID", job.id().toString()));
        rows.add(List.of("Created at", job.createdAt().toString()));
        rows.add(List.of("Metric", "Value"));
        for (ReportDatasetRow row : datasetRows(dataset)) {
            rows.add(List.of(row.label(), row.value()));
        }
        return rows;
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
        return value.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)");
    }

    private String escapeXml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private void writeAscii(ByteArrayOutputStream output, String value) {
        writeBytes(output, value.getBytes(StandardCharsets.ISO_8859_1));
    }

    private void writeBytes(ByteArrayOutputStream output, byte[] bytes) {
        output.write(bytes, 0, bytes.length);
    }
}

package com.eprocure.analytics.infrastructure.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.analytics.application.port.out.ReportDataset;
import com.eprocure.analytics.application.port.out.ReportDatasetRow;
import com.eprocure.analytics.domain.model.report.ReportFilterCriteria;
import com.eprocure.analytics.domain.model.report.ReportFormat;
import com.eprocure.analytics.domain.model.report.ReportJob;
import com.eprocure.analytics.domain.model.report.ReportJobStatus;
import com.eprocure.analytics.domain.model.report.ReportType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.PaneInformation;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LocalReportFileRendererTest {
    private static final Instant NOW = Instant.parse("2026-06-05T04:00:00Z");
    private static final UUID JOB_ID = UUID.fromString("70000000-0000-4000-8000-000000000001");
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("10000000-0000-4000-8000-000000000001");

    @TempDir
    private Path tempDir;

    @Test
    void should_render_pdf_file_when_format_is_pdf() throws Exception {
        LocalReportFileRenderer renderer = new LocalReportFileRenderer(tempDir);

        var rendered = renderer.render(job(ReportFormat.PDF), dataset());

        Path output = Path.of(rendered.storagePath());
        assertThat(output).exists();
        byte[] bytes = Files.readAllBytes(output);
        assertThat(bytes).startsWith("%PDF-1.".getBytes(StandardCharsets.ISO_8859_1));
        try (org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.Loader.loadPDF(bytes)) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            String text = stripper.getText(document);
            assertThat(text).contains("Purchase Order Summary");
            assertThat(text).contains("Issued PO Metrics");
            assertThat(text).contains("Issued PO count");
            assertThat(text).contains("All periods");
        }
        assertThat(rendered.downloadUrl()).isEqualTo("/api/v1/reports/jobs/" + JOB_ID + "/download");
    }

    @Test
    void should_render_xlsx_file_when_format_is_excel() throws Exception {
        LocalReportFileRenderer renderer = new LocalReportFileRenderer(tempDir);

        var rendered = renderer.render(job(ReportFormat.EXCEL), dataset());

        Path output = Path.of(rendered.storagePath());
        assertThat(output).exists();
        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(output))) {
            Sheet sheet = workbook.getSheet("PO SUMMARY");
            DataFormatter formatter = new DataFormatter();

            assertThat(sheet).isNotNull();
            assertThat(text(sheet, formatter, 0, 0)).isEqualTo("Purchase Order Summary");
            assertThat(text(sheet, formatter, 3, 0)).isEqualTo("Report Metadata");
            assertThat(text(sheet, formatter, 11, 0)).isEqualTo("Issued PO Metrics");
            assertThat(text(sheet, formatter, 12, 0)).isEqualTo("Metric");
            assertThat(text(sheet, formatter, 12, 1)).isEqualTo("Value");
            assertThat(text(sheet, formatter, 13, 0)).isEqualTo("Issued PO count");
            assertThat(text(sheet, formatter, 13, 1)).isEqualTo("3");
            assertThat(sheet.getColumnWidth(0)).isEqualTo(34 * 256);
            assertThat(sheet.getColumnWidth(1)).isEqualTo(58 * 256);
            PaneInformation paneInformation = sheet.getPaneInformation();
            assertThat(paneInformation).isNotNull();
            assertThat(paneInformation.isFreezePane()).isTrue();
        }
    }

    @Test
    void should_include_filter_summary_in_xlsx_output() throws Exception {
        LocalReportFileRenderer renderer = new LocalReportFileRenderer(tempDir);

        var rendered = renderer.render(filteredJob(), dataset());

        try (XSSFWorkbook workbook = new XSSFWorkbook(Files.newInputStream(Path.of(rendered.storagePath())))) {
            Sheet sheet = workbook.getSheet("PO SUMMARY");
            DataFormatter formatter = new DataFormatter();

            assertThat(text(sheet, formatter, 8, 1)).isEqualTo("FY 2026 Q2");
            assertThat(text(sheet, formatter, 9, 1)).isEqualTo("Category IT-HARDWARE");
        }
    }

    private String text(Sheet sheet, DataFormatter formatter, int rowIndex, int columnIndex) {
        return formatter.formatCellValue(sheet.getRow(rowIndex).getCell(columnIndex));
    }

    private ReportJob job(ReportFormat format) {
        return ReportJob.queued(
                JOB_ID,
                ReportType.PO_SUMMARY,
                format,
                NOW,
                NOW.plusSeconds(3600),
                ACTOR_ID,
                IDEMPOTENCY_KEY);
    }

    private ReportJob filteredJob() {
        return new ReportJob(
                JOB_ID,
                ReportType.PO_SUMMARY,
                ReportFormat.EXCEL,
                ReportJobStatus.PROCESSING,
                null,
                null,
                null,
                NOW,
                null,
                NOW.plusSeconds(3600),
                ACTOR_ID,
                IDEMPOTENCY_KEY,
                new ReportFilterCriteria(null, null, 2026, 2, null, "IT-HARDWARE"));
    }

    private ReportDataset dataset() {
        return new ReportDataset(List.of(new ReportDatasetRow("Issued PO count", "3")));
    }
}

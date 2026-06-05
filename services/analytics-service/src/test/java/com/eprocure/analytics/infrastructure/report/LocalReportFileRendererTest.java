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
import java.util.zip.ZipFile;
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
        String content = Files.readString(output, StandardCharsets.ISO_8859_1);
        assertThat(content.substring(0, 8)).isEqualTo("%PDF-1.4");
        assertThat(content).contains("Purchase Order Summary");
        assertThat(content).contains("Report Metadata");
        assertThat(content).contains("Issued PO Metrics");
        assertThat(content).contains("Issued PO count");
        assertThat(content).contains("All periods");
        assertThat(rendered.downloadUrl()).isEqualTo("/api/v1/reports/jobs/" + JOB_ID + "/download");
    }

    @Test
    void should_render_xlsx_file_when_format_is_excel() throws Exception {
        LocalReportFileRenderer renderer = new LocalReportFileRenderer(tempDir);

        var rendered = renderer.render(job(ReportFormat.EXCEL), dataset());

        Path output = Path.of(rendered.storagePath());
        assertThat(output).exists();
        try (ZipFile zipFile = new ZipFile(output.toFile())) {
            assertThat(zipFile.getEntry("[Content_Types].xml")).isNotNull();
            assertThat(zipFile.getEntry("xl/worksheets/sheet1.xml")).isNotNull();
            assertThat(zipFile.getEntry("xl/styles.xml")).isNotNull();
            String sheetXml = new String(zipFile.getInputStream(zipFile.getEntry("xl/worksheets/sheet1.xml")).readAllBytes(),
                    StandardCharsets.UTF_8);
            assertThat(sheetXml).contains("<cols>");
            assertThat(sheetXml).contains("Purchase Order Summary");
            assertThat(sheetXml).contains("Report Metadata");
            assertThat(sheetXml).contains("Issued PO Metrics");
            assertThat(sheetXml).contains("Issued PO count");
            assertThat(sheetXml).contains("3");
            String stylesXml = new String(zipFile.getInputStream(zipFile.getEntry("xl/styles.xml")).readAllBytes(),
                    StandardCharsets.UTF_8);
            assertThat(stylesXml).contains("<styleSheet");
            assertThat(stylesXml).contains("<b/>");
        }
    }

    @Test
    void should_include_filter_summary_in_xlsx_output() throws Exception {
        LocalReportFileRenderer renderer = new LocalReportFileRenderer(tempDir);

        var rendered = renderer.render(filteredJob(), dataset());

        try (ZipFile zipFile = new ZipFile(Path.of(rendered.storagePath()).toFile())) {
            String sheetXml = new String(zipFile.getInputStream(zipFile.getEntry("xl/worksheets/sheet1.xml")).readAllBytes(),
                    StandardCharsets.UTF_8);
            assertThat(sheetXml).contains("FY 2026 Q2");
            assertThat(sheetXml).contains("Category IT-HARDWARE");
        }
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

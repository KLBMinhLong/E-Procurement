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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapCollectionDataSource;
import net.sf.jasperreports.engine.util.JRLoader;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalReportFileRenderer implements ReportFileRenderer {
    private static final int PDF_MAX_DATASET_ROWS = 28;

    private final Path storageDirectory;

    public LocalReportFileRenderer(
            @Value("${eprocure.analytics.report-worker.storage-dir:${java.io.tmpdir}/eprocure-analytics-reports}")
            String storageDirectory) {
        this.storageDirectory = Path.of(storageDirectory);
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
        try {
            String templateName = job.reportType().name().toLowerCase().replace('_', '-');
            JasperReport jasperReport = loadJasperReport(templateName);
            if (jasperReport == null) {
                jasperReport = loadJasperReport("default");
            }
            if (jasperReport == null) {
                throw new IllegalStateException("No report template found for '" + templateName + "' or 'default'");
            }

            ReportLayoutTemplate template = ReportLayoutTemplate.resolve(job.reportType());
            Map<String, Object> parameters = new HashMap<>();
            parameters.put("reportTitle", template.title());
            parameters.put("reportSubtitle", template.subtitle());
            parameters.put("datasetSectionTitle", template.datasetSectionTitle());
            parameters.put("jobId", job.id().toString());
            parameters.put("createdAt", job.createdAt().toString());
            parameters.put("periodFilter", periodFilter(job.filterCriteria()));
            parameters.put("scopeFilter", scopeFilter(job.filterCriteria()));
            parameters.put("generatedAt", job.createdAt().toString()); // use job createdAt for deterministic tests
            parameters.put("locale", Locale.getDefault());
            parameters.put("timezone", TimeZone.getDefault());

            List<Map<String, ?>> mapRows = new ArrayList<>();
            int rowNum = 1;
            for (ReportDatasetRow row : datasetRows(dataset)) {
                Map<String, Object> map = new HashMap<>();
                map.put("label", row.label());
                map.put("value", row.value());
                map.put("ROW_NUM", rowNum++);
                mapRows.add(map);
            }
            JRMapCollectionDataSource dataSource = new JRMapCollectionDataSource(mapRows);
            JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource);
            return JasperExportManager.exportReportToPdf(jasperPrint);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render PDF using JasperReports", e);
        }
    }

    private byte[] renderXlsx(ReportJob job, ReportDataset dataset) throws IOException {
        ReportLayoutTemplate template = ReportLayoutTemplate.resolve(job.reportType());
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet(sheetName(job.reportType().name()));
            Map<String, CellStyle> styles = workbookStyles(workbook);
            int rowIndex = 0;

            // Title
            rowIndex = writeMergedRow(sheet, rowIndex, template.title(), styles.get("title"), 24);
            rowIndex = writeMergedRow(sheet, rowIndex, template.subtitle(), styles.get("subtitle"), 18);
            rowIndex++;

            // Metadata section
            rowIndex = writeMergedRow(sheet, rowIndex, "Report Metadata", styles.get("section"), 18);
            for (List<String> row : metadataRows(job)) {
                Row metadataRow = sheet.createRow(rowIndex++);
                writeCell(metadataRow, 0, row.get(0), styles.get("metadataLabel"));
                writeCell(metadataRow, 1, row.get(1), styles.get("metadataValue"));
            }
            rowIndex++;

            // Data section
            List<ReportDatasetRow> rows = datasetRows(dataset);
            rowIndex = writeMergedRow(sheet, rowIndex, template.datasetSectionTitle(), styles.get("section"), 18);
            int headerRowIndex = rowIndex;
            Row header = sheet.createRow(rowIndex++);
            writeCell(header, 0, "Metric", styles.get("tableHeader"));
            writeCell(header, 1, "Value", styles.get("tableHeader"));
            int dataRowNum = 0;
            for (ReportDatasetRow row : rows) {
                Row dataRow = sheet.createRow(rowIndex++);
                boolean evenRow = dataRowNum % 2 == 0;
                writeCell(dataRow, 0, row.label(), evenRow ? styles.get("tableRowEven") : styles.get("tableLabel"));
                writeCell(dataRow, 1, row.value(), evenRow ? styles.get("tableValueEven") : styles.get("tableValue"));
                dataRowNum++;
            }

            // Summary section
            rowIndex++;
            rowIndex = writeMergedRow(sheet, rowIndex, "Summary", styles.get("section"), 18);
            Row countRow = sheet.createRow(rowIndex++);
            writeCell(countRow, 0, "Total rows", styles.get("summaryLabel"));
            writeCell(countRow, 1, String.valueOf(rows.size()), styles.get("summaryValue"));
            rowIndex++;
            writeMergedRow(sheet, rowIndex, "This report is auto-generated from analytics read-model projections.", styles.get("disclaimer"), 14);

            sheet.createFreezePane(0, headerRowIndex + 1);
            sheet.setAutoFilter(new CellRangeAddress(headerRowIndex, headerRowIndex + rows.size(), 0, 1));
            sizeColumns(sheet);
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private Map<String, CellStyle> workbookStyles(Workbook workbook) {
        Map<String, CellStyle> styles = new HashMap<>();
        styles.put("title", style(workbook, font(workbook, (short) 18, true, IndexedColors.DARK_BLUE), null, false));
        styles.put("subtitle", style(workbook, font(workbook, (short) 10, false, IndexedColors.GREY_50_PERCENT), null, false));
        styles.put("confidential", style(workbook, font(workbook, (short) 8, true, IndexedColors.GREY_50_PERCENT), null, false));
        styles.put("section", style(workbook, font(workbook, (short) 11, true, IndexedColors.WHITE), IndexedColors.DARK_BLUE, false));
        styles.put("metadataLabel", style(workbook, font(workbook, (short) 10, true, IndexedColors.GREY_80_PERCENT), IndexedColors.GREY_25_PERCENT, true));
        styles.put("metadataValue", style(workbook, font(workbook, (short) 10, false, IndexedColors.GREY_80_PERCENT), null, true));
        styles.put("tableHeader", style(workbook, font(workbook, (short) 10, true, IndexedColors.WHITE), IndexedColors.DARK_BLUE, true));
        styles.put("tableLabel", style(workbook, font(workbook, (short) 10, false, IndexedColors.GREY_80_PERCENT), null, true));
        styles.put("tableValue", style(workbook, font(workbook, (short) 10, true, IndexedColors.GREY_80_PERCENT), null, true));
        styles.put("tableRowEven", style(workbook, font(workbook, (short) 10, false, IndexedColors.GREY_80_PERCENT), IndexedColors.GREY_25_PERCENT, true));
        styles.put("tableValueEven", style(workbook, font(workbook, (short) 10, true, IndexedColors.GREY_80_PERCENT), IndexedColors.GREY_25_PERCENT, true));
        styles.put("summaryLabel", style(workbook, font(workbook, (short) 10, true, IndexedColors.DARK_BLUE), null, false));
        styles.put("summaryValue", style(workbook, font(workbook, (short) 12, true, IndexedColors.DARK_BLUE), null, false));
        styles.put("disclaimer", style(workbook, font(workbook, (short) 8, false, IndexedColors.GREY_50_PERCENT), null, false));
        return styles;
    }

    private Font font(Workbook workbook, short size, boolean bold, IndexedColors color) {
        Font font = workbook.createFont();
        font.setFontName("Calibri");
        font.setFontHeightInPoints(size);
        font.setBold(bold);
        font.setColor(color.getIndex());
        return font;
    }

    private CellStyle style(Workbook workbook, Font font, IndexedColors fillColor, boolean bordered) {
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        if (fillColor != null) {
            style.setFillForegroundColor(fillColor.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        if (bordered) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setTopBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setRightBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
            style.setLeftBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        }
        return style;
    }

    private int writeMergedRow(Sheet sheet, int rowIndex, String value, CellStyle style, int heightInPoints) {
        Row row = sheet.createRow(rowIndex);
        row.setHeightInPoints(heightInPoints);
        writeCell(row, 0, value, style);
        writeCell(row, 1, "", style);
        sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 1));
        return rowIndex + 1;
    }

    private void writeCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(sanitizeText(value));
        cell.setCellStyle(style);
    }

    private void sizeColumns(Sheet sheet) {
        sheet.setColumnWidth(0, 34 * 256);
        sheet.setColumnWidth(1, 58 * 256);
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

    private List<ReportDatasetRow> datasetRows(ReportDataset dataset) {
        if (dataset == null || dataset.rows().isEmpty()) {
            return List.of(new ReportDatasetRow("Dataset source", "No projection data available"));
        }
        return dataset.rows();
    }

    private String sanitizeText(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
    }

    private String truncate(String value, int maxLength) {
        String sanitized = sanitizeText(value);
        return sanitized.length() <= maxLength ? sanitized : sanitized.substring(0, maxLength - 3) + "...";
    }

    private String sheetName(String reportType) {
        return truncate(reportType.replace('_', ' ').replaceAll("[\\\\/?*\\[\\]:]", " "), 31);
    }

    /**
     * Load a JasperReport by name. Tries pre-compiled .jasper first, then
     * falls back to compiling from .jrxml source at runtime.
     */
    private JasperReport loadJasperReport(String templateName) {
        // 1. Try pre-compiled .jasper (if a build plugin produced it)
        try (InputStream is = getClass().getResourceAsStream("/reports/" + templateName + ".jasper")) {
            if (is != null) {
                return (JasperReport) JRLoader.loadObject(is);
            }
        } catch (Exception ignored) {
            // fall through to .jrxml
        }
        // 2. Compile .jrxml on-the-fly
        try (InputStream is = getClass().getResourceAsStream("/reports/" + templateName + ".jrxml")) {
            if (is != null) {
                return JasperCompileManager.compileReport(is);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to compile report template: " + templateName + ".jrxml", e);
        }
        return null;
    }
}

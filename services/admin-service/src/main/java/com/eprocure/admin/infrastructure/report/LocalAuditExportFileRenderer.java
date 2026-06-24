package com.eprocure.admin.infrastructure.report;

import com.eprocure.admin.application.port.out.AuditExportFileRenderer;
import com.eprocure.admin.application.port.out.RenderedAuditExport;
import com.eprocure.admin.domain.model.AuditExportJob;
import com.eprocure.admin.domain.model.AuditLogEntry;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LocalAuditExportFileRenderer implements AuditExportFileRenderer {
    private static final int EXCEL_CELL_MAX_LENGTH = 32767;
    private static final String[] HEADERS = {
            "Occurred At",
            "Actor ID",
            "Actor Name",
            "Actor Roles",
            "Actor IP",
            "Action",
            "Entity Type",
            "Entity ID",
            "Entity Number",
            "Service",
            "HTTP Method",
            "Endpoint",
            "Request ID",
            "Success",
            "Error Code",
            "Description",
            "Old Value",
            "New Value"
    };

    private final Path storageDirectory;

    public LocalAuditExportFileRenderer(
            @Value("${eprocure.admin.audit-export-worker.storage-dir:${java.io.tmpdir}/eprocure-admin-audit-exports}")
            String storageDirectory) {
        this.storageDirectory = Path.of(storageDirectory);
    }

    @Override
    public RenderedAuditExport render(AuditExportJob job, List<AuditLogEntry> entries) {
        try {
            Files.createDirectories(storageDirectory);
            String fileName = "audit-export-" + job.id() + ".xlsx";
            Path output = storageDirectory.resolve(fileName).normalize();
            writeWorkbook(job, entries, output);
            return new RenderedAuditExport(fileName, output.toString());
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot render audit export file", exception);
        }
    }

    private void writeWorkbook(AuditExportJob job, List<AuditLogEntry> entries, Path output) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             OutputStream outputStream = Files.newOutputStream(output)) {
            Sheet sheet = workbook.createSheet("Audit Log");
            CellStyle titleStyle = titleStyle(workbook);
            CellStyle headerStyle = headerStyle(workbook);
            int rowIndex = 0;

            Row titleRow = sheet.createRow(rowIndex++);
            writeCell(titleRow, 0, "Audit Log Export", titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));

            rowIndex = writeMetadata(sheet, rowIndex, job);
            rowIndex++;

            Row headerRow = sheet.createRow(rowIndex++);
            for (int column = 0; column < HEADERS.length; column++) {
                writeCell(headerRow, column, HEADERS[column], headerStyle);
            }
            int headerRowIndex = headerRow.getRowNum();

            for (AuditLogEntry entry : entries) {
                Row row = sheet.createRow(rowIndex++);
                writeEntry(row, entry);
            }

            sheet.createFreezePane(0, headerRowIndex + 1);
            sheet.setAutoFilter(new CellRangeAddress(headerRowIndex, Math.max(headerRowIndex, rowIndex - 1), 0, HEADERS.length - 1));
            sizeColumns(sheet);
            workbook.write(outputStream);
        }
    }

    private int writeMetadata(Sheet sheet, int rowIndex, AuditExportJob job) {
        writeMetadataRow(sheet, rowIndex++, "Job ID", job.id().toString());
        writeMetadataRow(sheet, rowIndex++, "Status", job.status().name());
        writeMetadataRow(sheet, rowIndex++, "From Time", time(job.fromTime()));
        writeMetadataRow(sheet, rowIndex++, "To Time", time(job.toTime()));
        writeMetadataRow(sheet, rowIndex++, "Filter Actor ID", job.filterActorId().map(Object::toString).orElse(""));
        writeMetadataRow(sheet, rowIndex++, "Filter Entity Type", job.entityType().orElse(""));
        writeMetadataRow(sheet, rowIndex++, "Filter Action", job.action().orElse(""));
        writeMetadataRow(sheet, rowIndex++, "Requested At", time(job.requestedAt()));
        return rowIndex;
    }

    private void writeMetadataRow(Sheet sheet, int rowIndex, String label, String value) {
        Row row = sheet.createRow(rowIndex);
        writeCell(row, 0, label, null);
        writeCell(row, 1, value, null);
    }

    private void writeEntry(Row row, AuditLogEntry entry) {
        int column = 0;
        writeCell(row, column++, time(entry.occurredAt()), null);
        writeCell(row, column++, entry.actor().id().toString(), null);
        writeCell(row, column++, entry.actor().name(), null);
        writeCell(row, column++, String.join(",", entry.actor().roles()), null);
        writeCell(row, column++, entry.actor().ip(), null);
        writeCell(row, column++, entry.action(), null);
        writeCell(row, column++, entry.entityType(), null);
        writeCell(row, column++, entry.entityId().map(Object::toString).orElse(""), null);
        writeCell(row, column++, entry.entityNumber().orElse(""), null);
        writeCell(row, column++, entry.serviceName(), null);
        writeCell(row, column++, entry.httpMethod().orElse(""), null);
        writeCell(row, column++, entry.endpoint().orElse(""), null);
        writeCell(row, column++, entry.requestId().orElse(""), null);
        writeCell(row, column++, Boolean.toString(entry.success()), null);
        writeCell(row, column++, entry.errorCode().orElse(""), null);
        writeCell(row, column++, entry.description().orElse(""), null);
        writeCell(row, column++, entry.oldValueJson().orElse(""), null);
        writeCell(row, column, entry.newValueJson().orElse(""), null);
    }

    private void writeCell(Row row, int columnIndex, String value, CellStyle style) {
        Cell cell = row.createCell(columnIndex);
        cell.setCellValue(truncate(sanitize(value)));
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private CellStyle titleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        return style;
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        style.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private void sizeColumns(Sheet sheet) {
        int[] widths = {24, 38, 24, 32, 20, 28, 20, 38, 24, 20, 16, 42, 32, 12, 18, 48, 64, 64};
        for (int column = 0; column < widths.length; column++) {
            sheet.setColumnWidth(column, widths[column] * 256);
        }
    }

    private String time(Instant value) {
        return value == null ? "" : value.toString();
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
    }

    private String truncate(String value) {
        return value.length() <= EXCEL_CELL_MAX_LENGTH
                ? value
                : value.substring(0, EXCEL_CELL_MAX_LENGTH - 3) + "...";
    }
}

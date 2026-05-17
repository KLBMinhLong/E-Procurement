## SK-28 · JasperReport Template

### Trigger
Agent cần tạo template báo cáo hoặc export PDF/Excel.

### Inputs Required
- Report name
- Params (title, generatedAt, locale, timezone)
- Data rows

### Rules
```
[R1] Template lưu ở resources/reports/{report-name}.jrxml
[R2] Không viết SQL trong JRXML — dữ liệu chuẩn bị ở application layer
[R3] Dùng JRBeanCollectionDataSource cho list
[R4] Tham số bắt buộc: reportTitle, generatedAt, locale, timezone
[R5] Tiền tệ dùng BigDecimal và format theo locale
[R6] Output mặc định PDF; Excel là optional
```

### Template — JRXML (Skeleton)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<jasperReport xmlns="http://jasperreports.sourceforge.net/jasperreports"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
	xsi:schemaLocation="http://jasperreports.sourceforge.net/jasperreports
	http://jasperreports.sourceforge.net/xsd/jasperreport.xsd"
	name="{report-name}" pageWidth="595" pageHeight="842" columnWidth="555"
	leftMargin="20" rightMargin="20" topMargin="20" bottomMargin="20">

	<parameter name="reportTitle" class="java.lang.String"/>
	<parameter name="generatedAt" class="java.time.Instant"/>
	<parameter name="locale" class="java.util.Locale"/>
	<parameter name="timezone" class="java.util.TimeZone"/>

	<field name="prNumber" class="java.lang.String"/>
	<field name="requesterName" class="java.lang.String"/>
	<field name="estimatedTotal" class="java.math.BigDecimal"/>

	<title>
		<band height="40">
			<textField>
				<reportElement x="0" y="0" width="555" height="20"/>
				<textFieldExpression><![CDATA[$P{reportTitle}]]></textFieldExpression>
			</textField>
		</band>
	</title>

	<detail>
		<band height="20">
			<textField>
				<reportElement x="0" y="0" width="120" height="20"/>
				<textFieldExpression><![CDATA[$F{prNumber}]]></textFieldExpression>
			</textField>
		</band>
	</detail>
</jasperReport>
```

### Template — Report Service
```java
@Service
public class ReportService {

	public byte[] exportPurchaseRequestReport(List<ReportRow> rows, Locale locale, TimeZone timezone) {
		try (InputStream template = getClass().getResourceAsStream("/reports/purchase-request.jrxml")) {
			JasperReport jasperReport = JasperCompileManager.compileReport(template);
			JRBeanCollectionDataSource dataSource = new JRBeanCollectionDataSource(rows);

			Map<String, Object> params = new HashMap<>();
			params.put("reportTitle", "Purchase Request Report");
			params.put("generatedAt", Instant.now());
			params.put("locale", locale);
			params.put("timezone", timezone);

			JasperPrint print = JasperFillManager.fillReport(jasperReport, params, dataSource);
			return JasperExportManager.exportReportToPdf(print);
		} catch (Exception ex) {
			throw new ReportExportException(ex.getMessage(), ex);
		}
	}
}
```

### Checklist
```
[ ] Template ở resources/reports
[ ] Không có SQL trong JRXML
[ ] DataSource dùng JRBeanCollectionDataSource
[ ] Params đầy đủ (title, generatedAt, locale, timezone)
```

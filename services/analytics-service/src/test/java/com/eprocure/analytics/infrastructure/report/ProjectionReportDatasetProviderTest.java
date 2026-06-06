package com.eprocure.analytics.infrastructure.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.eprocure.analytics.domain.model.report.ReportFilterCriteria;
import com.eprocure.analytics.domain.model.report.ReportFormat;
import com.eprocure.analytics.domain.model.report.ReportJob;
import com.eprocure.analytics.domain.model.report.ReportJobStatus;
import com.eprocure.analytics.domain.model.report.ReportType;
import com.eprocure.analytics.infrastructure.persistence.entity.ReportDatasetRowDbEntity;
import com.eprocure.analytics.infrastructure.persistence.mapper.ReportDatasetMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProjectionReportDatasetProviderTest {
    private static final UUID JOB_ID = UUID.fromString("70000000-0000-4000-8000-000000000001");
    private static final UUID ACTOR_ID = UUID.fromString("30000000-0000-4000-8000-000000000001");
    private static final UUID IDEMPOTENCY_KEY = UUID.fromString("10000000-0000-4000-8000-000000000001");
    private static final UUID VENDOR_ID = UUID.fromString("40000000-0000-4000-8000-000000000001");
    private static final Instant NOW = Instant.parse("2026-06-05T04:00:00Z");

    @Test
    void should_apply_explicit_date_vendor_and_category_filters_when_loading_po_summary() {
        FakeReportDatasetMapper mapper = new FakeReportDatasetMapper();
        ProjectionReportDatasetProvider provider = new ProjectionReportDatasetProvider(mapper);
        ReportFilterCriteria filters = new ReportFilterCriteria(
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-05"),
                null,
                null,
                VENDOR_ID,
                "IT-HARDWARE");

        var dataset = provider.load(job(ReportType.PO_SUMMARY, filters));

        assertThat(dataset.rows()).hasSize(1);
        assertThat(mapper.poFromInclusive).isEqualTo(Instant.parse("2026-06-01T00:00:00Z"));
        assertThat(mapper.poToExclusive).isEqualTo(Instant.parse("2026-06-06T00:00:00Z"));
        assertThat(mapper.poVendorId).isEqualTo(VENDOR_ID);
        assertThat(mapper.poCategoryCode).isEqualTo("IT-HARDWARE");
    }

    @Test
    void should_convert_fiscal_quarter_to_date_range_when_loading_sla_compliance() {
        FakeReportDatasetMapper mapper = new FakeReportDatasetMapper();
        ProjectionReportDatasetProvider provider = new ProjectionReportDatasetProvider(mapper);
        ReportFilterCriteria filters = new ReportFilterCriteria(null, null, 2026, 2, null, null);

        provider.load(job(ReportType.SLA_COMPLIANCE, filters));

        assertThat(mapper.slaFromInclusive).isEqualTo(Instant.parse("2026-04-01T00:00:00Z"));
        assertThat(mapper.slaToExclusive).isEqualTo(Instant.parse("2026-07-01T00:00:00Z"));
    }

    @Test
    void should_load_cycle_time_dataset_from_projection_mapper() {
        FakeReportDatasetMapper mapper = new FakeReportDatasetMapper();
        ProjectionReportDatasetProvider provider = new ProjectionReportDatasetProvider(mapper);
        ReportFilterCriteria filters = new ReportFilterCriteria(
                null,
                null,
                2026,
                1,
                VENDOR_ID,
                "IT-HARDWARE");

        var dataset = provider.load(job(ReportType.CYCLE_TIME_ANALYSIS, filters));

        assertThat(dataset.rows()).hasSize(1);
        assertThat(mapper.cycleFromInclusive).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(mapper.cycleToExclusive).isEqualTo(Instant.parse("2026-04-01T00:00:00Z"));
        assertThat(mapper.cycleVendorId).isEqualTo(VENDOR_ID);
        assertThat(mapper.cycleCategoryCode).isEqualTo("IT-HARDWARE");
    }

    @Test
    void should_load_vendor_scorecard_dataset_from_projection_mapper() {
        FakeReportDatasetMapper mapper = new FakeReportDatasetMapper();
        ProjectionReportDatasetProvider provider = new ProjectionReportDatasetProvider(mapper);
        ReportFilterCriteria filters = new ReportFilterCriteria(
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-05"),
                null,
                null,
                VENDOR_ID,
                "IT-HARDWARE");

        var dataset = provider.load(job(ReportType.VENDOR_SCORECARD, filters));

        assertThat(dataset.rows()).hasSize(1);
        assertThat(mapper.vendorFromInclusive).isEqualTo(Instant.parse("2026-06-01T00:00:00Z"));
        assertThat(mapper.vendorToExclusive).isEqualTo(Instant.parse("2026-06-06T00:00:00Z"));
        assertThat(mapper.vendorId).isEqualTo(VENDOR_ID);
        assertThat(mapper.vendorCategoryCode).isEqualTo("IT-HARDWARE");
    }

    @Test
    void should_load_spending_by_department_dataset_from_projection_mapper() {
        FakeReportDatasetMapper mapper = new FakeReportDatasetMapper();
        ProjectionReportDatasetProvider provider = new ProjectionReportDatasetProvider(mapper);
        ReportFilterCriteria filters = new ReportFilterCriteria(
                null,
                null,
                2026,
                3,
                VENDOR_ID,
                "IT-HARDWARE");

        var dataset = provider.load(job(ReportType.SPENDING_BY_DEPARTMENT, filters));

        assertThat(dataset.rows()).hasSize(1);
        assertThat(mapper.departmentFromInclusive).isEqualTo(Instant.parse("2026-07-01T00:00:00Z"));
        assertThat(mapper.departmentToExclusive).isEqualTo(Instant.parse("2026-10-01T00:00:00Z"));
        assertThat(mapper.departmentVendorId).isEqualTo(VENDOR_ID);
        assertThat(mapper.departmentCategoryCode).isEqualTo("IT-HARDWARE");
    }

    @Test
    void should_load_budget_vs_plan_dataset_from_projection_mapper() {
        FakeReportDatasetMapper mapper = new FakeReportDatasetMapper();
        ProjectionReportDatasetProvider provider = new ProjectionReportDatasetProvider(mapper);
        ReportFilterCriteria filters = new ReportFilterCriteria(
                null,
                null,
                2026,
                4,
                VENDOR_ID,
                "IT-HARDWARE");

        var dataset = provider.load(job(ReportType.BUDGET_VS_PLAN, filters));

        assertThat(dataset.rows()).hasSize(1);
        assertThat(mapper.budgetFromInclusive).isEqualTo(Instant.parse("2026-10-01T00:00:00Z"));
        assertThat(mapper.budgetToExclusive).isEqualTo(Instant.parse("2027-01-01T00:00:00Z"));
        assertThat(mapper.budgetVendorId).isEqualTo(VENDOR_ID);
        assertThat(mapper.budgetCategoryCode).isEqualTo("IT-HARDWARE");
    }

    @Test
    void should_load_rfq_savings_dataset_from_projection_mapper() {
        FakeReportDatasetMapper mapper = new FakeReportDatasetMapper();
        ProjectionReportDatasetProvider provider = new ProjectionReportDatasetProvider(mapper);
        ReportFilterCriteria filters = new ReportFilterCriteria(
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-05"),
                null,
                null,
                VENDOR_ID,
                "IT-HARDWARE");

        var dataset = provider.load(job(ReportType.RFQ_SAVINGS, filters));

        assertThat(dataset.rows()).hasSize(1);
        assertThat(mapper.rfqFromInclusive).isEqualTo(Instant.parse("2026-06-01T00:00:00Z"));
        assertThat(mapper.rfqToExclusive).isEqualTo(Instant.parse("2026-06-06T00:00:00Z"));
        assertThat(mapper.rfqVendorId).isEqualTo(VENDOR_ID);
        assertThat(mapper.rfqCategoryCode).isEqualTo("IT-HARDWARE");
    }

    @Test
    void should_load_inventory_pending_dataset_from_projection_mapper() {
        FakeReportDatasetMapper mapper = new FakeReportDatasetMapper();
        ProjectionReportDatasetProvider provider = new ProjectionReportDatasetProvider(mapper);
        ReportFilterCriteria filters = new ReportFilterCriteria(
                null,
                null,
                2026,
                2,
                VENDOR_ID,
                "IT-HARDWARE");

        var dataset = provider.load(job(ReportType.INVENTORY_PENDING, filters));

        assertThat(dataset.rows()).hasSize(1);
        assertThat(mapper.inventoryFromInclusive).isEqualTo(Instant.parse("2026-04-01T00:00:00Z"));
        assertThat(mapper.inventoryToExclusive).isEqualTo(Instant.parse("2026-07-01T00:00:00Z"));
        assertThat(mapper.inventoryVendorId).isEqualTo(VENDOR_ID);
        assertThat(mapper.inventoryCategoryCode).isEqualTo("IT-HARDWARE");
    }

    @Test
    void should_load_maverick_spending_dataset_from_projection_mapper() {
        FakeReportDatasetMapper mapper = new FakeReportDatasetMapper();
        ProjectionReportDatasetProvider provider = new ProjectionReportDatasetProvider(mapper);
        ReportFilterCriteria filters = new ReportFilterCriteria(
                LocalDate.parse("2026-06-01"),
                LocalDate.parse("2026-06-05"),
                null,
                null,
                VENDOR_ID,
                "IT-HARDWARE");

        var dataset = provider.load(job(ReportType.MAVERICK_SPENDING, filters));

        assertThat(dataset.rows()).hasSize(1);
        assertThat(mapper.maverickFromInclusive).isEqualTo(Instant.parse("2026-06-01T00:00:00Z"));
        assertThat(mapper.maverickToExclusive).isEqualTo(Instant.parse("2026-06-06T00:00:00Z"));
        assertThat(mapper.maverickVendorId).isEqualTo(VENDOR_ID);
        assertThat(mapper.maverickCategoryCode).isEqualTo("IT-HARDWARE");
    }

    @Test
    void should_load_audit_trail_dataset_from_projection_mapper() {
        FakeReportDatasetMapper mapper = new FakeReportDatasetMapper();
        ProjectionReportDatasetProvider provider = new ProjectionReportDatasetProvider(mapper);
        ReportFilterCriteria filters = new ReportFilterCriteria(null, null, 2026, 1, null, null);

        var dataset = provider.load(job(ReportType.AUDIT_TRAIL, filters));

        assertThat(dataset.rows()).hasSize(1);
        assertThat(mapper.auditFromInclusive).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(mapper.auditToExclusive).isEqualTo(Instant.parse("2026-04-01T00:00:00Z"));
    }

    private ReportJob job(ReportType reportType, ReportFilterCriteria filters) {
        return new ReportJob(
                JOB_ID,
                reportType,
                ReportFormat.PDF,
                ReportJobStatus.PROCESSING,
                null,
                null,
                null,
                NOW,
                null,
                NOW.plusSeconds(3600),
                ACTOR_ID,
                IDEMPOTENCY_KEY,
                filters);
    }

    private static ReportDatasetRowDbEntity row() {
        ReportDatasetRowDbEntity row = new ReportDatasetRowDbEntity();
        row.setLabel("Metric");
        row.setValue("Value");
        return row;
    }

    private static final class FakeReportDatasetMapper implements ReportDatasetMapper {
        private Instant poFromInclusive;
        private Instant poToExclusive;
        private UUID poVendorId;
        private String poCategoryCode;
        private Instant slaFromInclusive;
        private Instant slaToExclusive;
        private Instant cycleFromInclusive;
        private Instant cycleToExclusive;
        private UUID cycleVendorId;
        private String cycleCategoryCode;
        private Instant vendorFromInclusive;
        private Instant vendorToExclusive;
        private UUID vendorId;
        private String vendorCategoryCode;
        private Instant departmentFromInclusive;
        private Instant departmentToExclusive;
        private UUID departmentVendorId;
        private String departmentCategoryCode;
        private Instant budgetFromInclusive;
        private Instant budgetToExclusive;
        private UUID budgetVendorId;
        private String budgetCategoryCode;
        private Instant rfqFromInclusive;
        private Instant rfqToExclusive;
        private UUID rfqVendorId;
        private String rfqCategoryCode;
        private Instant inventoryFromInclusive;
        private Instant inventoryToExclusive;
        private UUID inventoryVendorId;
        private String inventoryCategoryCode;
        private Instant maverickFromInclusive;
        private Instant maverickToExclusive;
        private UUID maverickVendorId;
        private String maverickCategoryCode;
        private Instant auditFromInclusive;
        private Instant auditToExclusive;

        @Override
        public List<ReportDatasetRowDbEntity> findPoSummaryRows(
                Instant fromInclusive,
                Instant toExclusive,
                UUID vendorId,
                String categoryCode) {
            poFromInclusive = fromInclusive;
            poToExclusive = toExclusive;
            poVendorId = vendorId;
            poCategoryCode = categoryCode;
            return List.of(row());
        }

        @Override
        public List<ReportDatasetRowDbEntity> findPrSummaryRows(
                Instant fromInclusive,
                Instant toExclusive,
                UUID vendorId,
                String categoryCode) {
            return List.of(row());
        }

        @Override
        public List<ReportDatasetRowDbEntity> findSlaComplianceRows(
                Instant fromInclusive,
                Instant toExclusive) {
            slaFromInclusive = fromInclusive;
            slaToExclusive = toExclusive;
            return List.of(row());
        }

        @Override
        public List<ReportDatasetRowDbEntity> findThreeWayMatchRows(
                Instant fromInclusive,
                Instant toExclusive,
                UUID vendorId) {
            return List.of(row());
        }

        @Override
        public List<ReportDatasetRowDbEntity> findCycleTimeAnalysisRows(
                Instant fromInclusive,
                Instant toExclusive,
                UUID vendorId,
                String categoryCode) {
            cycleFromInclusive = fromInclusive;
            cycleToExclusive = toExclusive;
            cycleVendorId = vendorId;
            cycleCategoryCode = categoryCode;
            return List.of(row());
        }

        @Override
        public List<ReportDatasetRowDbEntity> findVendorScorecardRows(
                Instant fromInclusive,
                Instant toExclusive,
                UUID vendorId,
                String categoryCode) {
            vendorFromInclusive = fromInclusive;
            vendorToExclusive = toExclusive;
            this.vendorId = vendorId;
            vendorCategoryCode = categoryCode;
            return List.of(row());
        }

        @Override
        public List<ReportDatasetRowDbEntity> findSpendingByDepartmentRows(
                Instant fromInclusive,
                Instant toExclusive,
                UUID vendorId,
                String categoryCode) {
            departmentFromInclusive = fromInclusive;
            departmentToExclusive = toExclusive;
            departmentVendorId = vendorId;
            departmentCategoryCode = categoryCode;
            return List.of(row());
        }

        @Override
        public List<ReportDatasetRowDbEntity> findBudgetVsPlanRows(
                Instant fromInclusive,
                Instant toExclusive,
                UUID vendorId,
                String categoryCode) {
            budgetFromInclusive = fromInclusive;
            budgetToExclusive = toExclusive;
            budgetVendorId = vendorId;
            budgetCategoryCode = categoryCode;
            return List.of(row());
        }

        @Override
        public List<ReportDatasetRowDbEntity> findRfqSavingsRows(
                Instant fromInclusive,
                Instant toExclusive,
                UUID vendorId,
                String categoryCode) {
            rfqFromInclusive = fromInclusive;
            rfqToExclusive = toExclusive;
            rfqVendorId = vendorId;
            rfqCategoryCode = categoryCode;
            return List.of(row());
        }

        @Override
        public List<ReportDatasetRowDbEntity> findInventoryPendingRows(
                Instant fromInclusive,
                Instant toExclusive,
                UUID vendorId,
                String categoryCode) {
            inventoryFromInclusive = fromInclusive;
            inventoryToExclusive = toExclusive;
            inventoryVendorId = vendorId;
            inventoryCategoryCode = categoryCode;
            return List.of(row());
        }

        @Override
        public List<ReportDatasetRowDbEntity> findMaverickSpendingRows(
                Instant fromInclusive,
                Instant toExclusive,
                UUID vendorId,
                String categoryCode) {
            maverickFromInclusive = fromInclusive;
            maverickToExclusive = toExclusive;
            maverickVendorId = vendorId;
            maverickCategoryCode = categoryCode;
            return List.of(row());
        }

        @Override
        public List<ReportDatasetRowDbEntity> findAuditTrailRows(
                Instant fromInclusive,
                Instant toExclusive) {
            auditFromInclusive = fromInclusive;
            auditToExclusive = toExclusive;
            return List.of(row());
        }
    }
}

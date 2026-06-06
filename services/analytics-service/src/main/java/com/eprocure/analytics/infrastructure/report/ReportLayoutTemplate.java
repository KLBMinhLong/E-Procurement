package com.eprocure.analytics.infrastructure.report;

import com.eprocure.analytics.domain.model.report.ReportType;

record ReportLayoutTemplate(
        String title,
        String subtitle,
        String datasetSectionTitle) {

    static ReportLayoutTemplate resolve(ReportType reportType) {
        return switch (reportType) {
            case PO_SUMMARY -> new ReportLayoutTemplate(
                    "Purchase Order Summary",
                    "Issued purchase order projection overview",
                    "Issued PO Metrics");
            case PR_SUMMARY -> new ReportLayoutTemplate(
                    "Purchase Request Summary",
                    "Purchase requests linked to issued purchase orders",
                    "Purchase Request Metrics");
            case SLA_COMPLIANCE -> new ReportLayoutTemplate(
                    "Approval SLA Compliance",
                    "Approval SLA breach projection overview",
                    "SLA Metrics");
            case THREE_WAY_MATCH -> new ReportLayoutTemplate(
                    "Three-Way Match Summary",
                    "Matched invoice projection overview",
                    "Invoice Match Metrics");
            case CYCLE_TIME_ANALYSIS -> new ReportLayoutTemplate(
                    "Cycle Time Analysis",
                    "PR lifecycle projection overview",
                    "Cycle Time Metrics");
            case VENDOR_SCORECARD -> new ReportLayoutTemplate(
                    "Vendor Scorecard",
                    "Vendor performance projection overview",
                    "Vendor Metrics");
            case BUDGET_VS_PLAN -> new ReportLayoutTemplate(
                    "Budget Versus Plan",
                    "Budget utilization projection overview",
                    "Budget Metrics");
            case SPENDING_BY_DEPARTMENT -> new ReportLayoutTemplate(
                    "Spending by Department",
                    "Department spend projection overview",
                    "Spend Metrics");
            case INVENTORY_PENDING -> new ReportLayoutTemplate(
                    "Pending Inventory",
                    "Goods receipt and pending inventory projection overview",
                    "Inventory Metrics");
            case RFQ_SAVINGS -> new ReportLayoutTemplate(
                    "RFQ Savings",
                    "RFQ award savings projection overview",
                    "Savings Metrics");
            case MAVERICK_SPENDING -> new ReportLayoutTemplate(
                    "Maverick Spending",
                    "Policy exception spend projection overview",
                    "Maverick Spend Metrics");
            case AUDIT_TRAIL -> new ReportLayoutTemplate(
                    "Audit Trail",
                    "Procurement audit projection overview",
                    "Audit Metrics");
        };
    }
}

# Timezone and Financial Precision

## Timezone
- DB stores TIMESTAMPTZ in UTC
- Container TZ: Asia/Ho_Chi_Minh
- API returns UTC ISO-8601; FE converts to local time
- SLA uses business hours 08:00-17:30 Mon-Fri

## Money precision
- DB: NUMERIC(19,4)
- Java: BigDecimal
- JSON: string with 4 decimals (avoid float rounding)

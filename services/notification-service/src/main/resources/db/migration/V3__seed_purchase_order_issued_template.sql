INSERT INTO notification.notification_templates (
    code, event_type, channel, language, subject_template, body_template
) VALUES (
    'IN_APP_PO_ISSUED_VI',
    'PO_ISSUED',
    'IN_APP',
    'vi',
    'PO {{poNumber}} đã phát hành',
    'PO {{poNumber}} cho {{vendorName}} đã được phát hành với tổng giá trị {{totalAmount}} {{currency}}.'
)
ON CONFLICT DO NOTHING;

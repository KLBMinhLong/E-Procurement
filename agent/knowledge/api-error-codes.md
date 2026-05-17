# API Error Codes

## Error response format

```json
{
  "success": false,
  "code": "PR_002",
  "message": "Ngan sach phong ban khong du",
  "details": [
    {
      "field": "lineItems[0].quantity",
      "reason": "So luong phai lon hon 0",
      "rejectedValue": -1
    }
  ],
  "data": null,
  "meta": null,
  "timestamp": "2025-01-15T08:30:00.000Z",
  "requestId": "uuid"
}
```

## Codes by service

### Gateway (GW_)
| Code | HTTP | Description |
|---|---|---|
| GW_001 | 429 | Rate limit vuot qua gioi han |
| GW_002 | 401 | x-api-key khong hop le (inter-service) |
| GW_003 | 503 | Service khong kha dung (circuit breaker open) |
| GW_004 | 400 | Request malformed, khong parse duoc |
| GW_005 | 408 | Request timeout |

### IAM (IAM_)
| Code | HTTP | Description |
|---|---|---|
| IAM_001 | 401 | Username hoac password khong dung |
| IAM_002 | 423 | Tai khoan bi khoa (5 lan sai) |
| IAM_003 | 401 | Token khong hop le hoac da bi thu hoi |
| IAM_004 | 403 | Khong co quyen thuc hien thao tac |
| IAM_005 | 401 | Phien dang nhap het han hoac bi invalidate |
| IAM_006 | 401 | Ma 2FA khong dung hoac het han |
| IAM_007 | 401 | Token reset password khong hop le hoac het han |
| IAM_008 | 422 | Password moi khong du manh |
| IAM_009 | 409 | Username hoac email da ton tai |
| IAM_010 | 422 | Email chua duoc xac thuc |
| IAM_011 | 400 | Du lieu dang ky khong hop le |
| IAM_012 | 401 | Google OAuth state/code khong hop le |
| IAM_020 | 422 | Nguoi duoc uy quyen phai cung cap hoac cao hon |
| IAM_021 | 422 | Khong the uy quyen vuot qua quyen cua ban than |
| IAM_022 | 409 | Da co uy quyen dang hoat dong trong khoang thoi gian nay |
| IAM_030 | 404 | Nguoi dung khong ton tai |
| IAM_031 | 404 | Role khong ton tai |
| IAM_032 | 404 | Permission khong ton tai |
| IAM_033 | 404 | Phong ban khong ton tai |
| IAM_034 | 422 | Khong tim thay approver phu hop |
| IAM_035 | 404 | Uy quyen khong ton tai |

### Purchase Request (PR_)
| Code | HTTP | Description |
|---|---|---|
| PR_001 | 404 | Yeu cau mua sam khong ton tai |
| PR_002 | 422 | Ngan sach phong ban khong du |
| PR_003 | 409 | Khong the thuc hien thao tac o trang thai hien tai |
| PR_004 | 403 | Nguoi tao PR khong duoc tu phe duyet PR cua minh |
| PR_005 | 422 | Phong ban vuot gioi han Emergency trong thang |
| PR_006 | 422 | Ly do khan cap toi thieu 100 ky tu |
| PR_007 | 404 | File dinh kem khong ton tai hoac het han |
| PR_008 | 422 | File vuot qua kich thuoc toi da 10MB |
| PR_009 | 422 | Loai file khong duoc phep |
| PR_010 | 400 | So luong phai lon hon 0 |
| PR_011 | 400 | Don gia khong hop le (phai >= 0) |
| PR_012 | 400 | PR phai co it nhat 1 muc hang hoa |
| PR_013 | 400 | Ngay can nhan hang khong duoc trong qua khu |
| PR_014 | 422 | Ly do mua hang toi thieu 50 ky tu |
| PR_015 | 422 | Item tu catalog khong ton tai hoac khong con hoat dong |

### Approval Engine (APR_)
| Code | HTTP | Description |
|---|---|---|
| APR_001 | 403 | Xung dot loi ich, approver khong the duyet request nay |
| APR_002 | 403 | Khong phai approver duoc chi dinh |
| APR_003 | 409 | Task da duoc xu ly truoc do (idempotency) |
| APR_004 | 404 | Approval task khong ton tai |
| APR_005 | 422 | Comment bat buoc khi tu choi (toi thieu 20 ky tu) |
| APR_006 | 422 | Khong the forward cho nguoi co cap thap hon |
| APR_007 | 422 | Approval rule khong ton tai hoac khong con hoat dong |
| APR_008 | 409 | Approval process da hoan thanh, khong the thay doi |
| APR_010 | 422 | Approval rule conditions conflict voi rule khac |

### Finance (FIN_)
| Code | HTTP | Description |
|---|---|---|
| FIN_001 | 404 | Ngan sach khong ton tai |
| FIN_002 | 422 | 3-way match that bai: so luong khong khop |
| FIN_003 | 422 | 3-way match that bai: don gia khong khop |
| FIN_004 | 403 | Vuot ngan sach > 30%, can CEO phe duyet |
| FIN_005 | 422 | Ngan sach chua duoc phe duyet |
| FIN_006 | 404 | Purchase Order khong ton tai |
| FIN_007 | 404 | Hoa don khong ton tai |
| FIN_008 | 409 | Hoa don da duoc xu ly |
| FIN_009 | 422 | Phong ban nguon khong du ngan sach de chuyen |
| FIN_010 | 422 | So tien hoa don vuot qua so tien PO cho phep |

### Inventory (INV_)
| Code | HTTP | Description |
|---|---|---|
| INV_001 | 404 | Item (catalog) khong ton tai |
| INV_002 | 404 | Kho hang khong ton tai |
| INV_003 | 422 | So luong ton kho khong du de cap phat |
| INV_004 | 404 | Goods Receipt khong ton tai |
| INV_005 | 409 | GR da hoan tat, khong the chinh sua |
| INV_006 | 422 | So luong nhan thuc te khong the vuot 10% |
| INV_007 | 422 | Item code da ton tai |

### Vendor (VND_)
| Code | HTTP | Description |
|---|---|---|
| VND_001 | 404 | Vendor khong ton tai |
| VND_002 | 409 | Ma so thue da ton tai |
| VND_003 | 422 | Vendor dang trong danh sach den |
| VND_004 | 404 | RFQ khong ton tai |
| VND_005 | 409 | RFQ da dong hoac da co ket qua |
| VND_006 | 422 | Chua co bao gia nao de chon |
| VND_007 | 422 | Han nop bao gia da het |

### Notification (NTF_)
| Code | HTTP | Description |
|---|---|---|
| NTF_001 | 404 | Thong bao khong ton tai |
| NTF_002 | 422 | Template thong bao khong ton tai |

### System (SYS_)
| Code | HTTP | Description |
|---|---|---|
| SYS_001 | 500 | Loi he thong khong xac dinh |
| SYS_002 | 503 | Dich vu phu thuoc khong kha dung |
| SYS_003 | 500 | Loi ma hoa/giai ma payload |
| SYS_004 | 400 | Payload ma hoa khong hop le hoac bi gia mao |
| SYS_005 | 400 | Thieu hoac sai dinh dang Idempotency-Key bat buoc |

### Validation (VAL_)
| Code | HTTP | Description |
|---|---|---|
| VAL_001 | 400 | Du lieu khong hop le (xem details) |
| VAL_002 | 400 | JSON khong dung format |
| VAL_003 | 400 | Query parameter khong hop le |

## Error handling rules
- 4xx: client error, FE can handle
- 5xx: system error, show generic message
- 422: business validation failed
- 400: technical validation failed
- Never expose stack trace, internal class, SQL errors, or secrets

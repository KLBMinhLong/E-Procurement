## SK-19 · Angular Service & HTTP

### Trigger
Agent tạo service Angular để gọi API cho feature.

### Inputs Required
- Base API URL + resource path
- Request/Response models
- Filter params + pagination

### Rules
```
[R1] Service chỉ gọi API — không chứa UI/route logic
[R2] Encryption interceptor xử lý tự động — service không tự encrypt
[R3] Token gửi qua Cookie (HttpOnly) — không đặt trong header thủ công
[R4] Xử lý error: map HTTP error → AppError (code + messageKey)
[R5] Pagination params: page, size, sort (camelCase)
[R6] Base URL từ environment.ts — không hardcode
```

### Template — Service
```typescript
import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '@env/environment';
import { ApiResponse } from '@core/models/api.model';
import { {Entity}FilterRequest, Create{Entity}Request, {Entity}Response } from '../models/{entity}.model';

@Injectable({ providedIn: 'root' })
export class {Entity}Service {

  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/v1/{resource-path}`;

    getList(filter: {Entity}FilterRequest, page = 1, size = 20):
      Observable<ApiResponse<{Entity}Response[]>> {

    const params = new HttpParams()
      .set('page', page)
      .set('size', size)
      .appendAll(this.filterToParams(filter));

    return this.http.get<ApiResponse<{Entity}Response[]>>(this.baseUrl, { params });
  }

  getById(id: string): Observable<ApiResponse<{Entity}Response>> {
    return this.http.get<ApiResponse<{Entity}Response>>(`${this.baseUrl}/${id}`);
  }

  create(request: Create{Entity}Request): Observable<ApiResponse<{Entity}Response>> {
    return this.http.post<ApiResponse<{Entity}Response>>(this.baseUrl, request);
  }

  submit(id: string): Observable<ApiResponse<{Entity}Response>> {
    return this.http.post<ApiResponse<{Entity}Response>>(`${this.baseUrl}/${id}/submit`, {});
  }

  cancel(id: string, reason: string): Observable<ApiResponse<void>> {
    return this.http.patch<ApiResponse<void>>(`${this.baseUrl}/${id}/cancel`, { reason });
  }

  private filterToParams(filter: {Entity}FilterRequest): Record<string, string> {
    return Object.fromEntries(
      Object.entries(filter)
        .filter(([_, v]) => v !== null && v !== undefined && v !== '')
        .map(([k, v]) => [k, String(v)])
    );
  }
}
```

### Checklist
```
[ ] Base URL từ environment
[ ] Không chứa UI/route logic
[ ] Pagination params đúng format
```

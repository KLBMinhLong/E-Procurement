import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/http/api-tokens';
import { ApiResponse } from '../../../core/models/api-response.model';
import { Warehouse } from '../models/stock.model';

@Injectable({ providedIn: 'root' })
export class WarehouseService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  list(): Observable<ApiResponse<Warehouse[]>> {
    return this.http.get<ApiResponse<Warehouse[]>>(
      `${this.baseUrl}/warehouses`,
      { withCredentials: true }
    );
  }
}

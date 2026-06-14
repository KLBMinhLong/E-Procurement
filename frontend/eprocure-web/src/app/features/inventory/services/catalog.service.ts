import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/http/api.service';
import { ApiResponse, PageMeta } from '../../../core/models/api-response.model';
import {
  CatalogItem,
  CatalogItemFilter,
  CreateCatalogItemRequest,
  UpdateCatalogItemRequest
} from '../models/catalog.model';

@Injectable({ providedIn: 'root' })
export class InventoryCatalogService {
  private readonly api = inject(ApiService);

  list(filter: CatalogItemFilter): Observable<ApiResponse<CatalogItem[]> & { meta: PageMeta }> {
    return this.api.get<CatalogItem[]>(
      '/items',
      {
        q: filter.q,
        category_code: filter.category_code,
        is_active: filter.is_active,
        below_reorder: filter.below_reorder,
        page: filter.page,
        size: filter.size
      }
    ) as Observable<ApiResponse<CatalogItem[]> & { meta: PageMeta }>;
  }

  get(itemCode: string): Observable<ApiResponse<CatalogItem>> {
    return this.api.get<CatalogItem>(`/items/${encodeURIComponent(itemCode)}`);
  }

  create(
    request: CreateCatalogItemRequest,
    idempotencyKey = crypto.randomUUID()
  ): Observable<ApiResponse<CatalogItem>> {
    return this.api.post<CatalogItem>('/items', request, idempotencyKey);
  }

  update(
    itemCode: string,
    request: UpdateCatalogItemRequest,
    idempotencyKey = crypto.randomUUID()
  ): Observable<ApiResponse<CatalogItem>> {
    return this.api.put<CatalogItem>(`/items/${encodeURIComponent(itemCode)}`, request, idempotencyKey);
  }
}

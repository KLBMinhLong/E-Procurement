import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { API_BASE_URL } from '../../../core/http/api-tokens';
import { ApiResponse, PageMeta } from '../../../core/models/api-response.model';
import { CatalogItemDetail, StockEntry, StockMovement } from '../models/stock.model';

export interface CatalogItemFilter {
  page: number;
  size: number;
  q?: string;
  category_code?: string;
  is_active?: boolean;
  below_reorder?: boolean;
}

export interface StockMovementFilter {
  page: number;
  size: number;
  item_code?: string;
  warehouse_id?: string;
  movement_type?: string;
  from_date?: string;
  to_date?: string;
}

export interface IssueOutCommand {
  warehouseId: string;
  prId?: string | null;
  recipientId: string;
  items: {
    itemCode: string;
    quantity: string;
    unit: string;
  }[];
  notes?: string | null;
}

@Injectable({ providedIn: 'root' })
export class StockService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  searchCatalogItems(filter: CatalogItemFilter): Observable<ApiResponse<CatalogItemDetail[]> & { meta: PageMeta }> {
    let params = new HttpParams()
      .set('page', filter.page)
      .set('size', filter.size);

    if (filter.q) params = params.set('q', filter.q);
    if (filter.category_code) params = params.set('category_code', filter.category_code);
    if (filter.is_active !== undefined) params = params.set('is_active', filter.is_active);
    if (filter.below_reorder !== undefined) params = params.set('below_reorder', filter.below_reorder);

    return this.http.get<ApiResponse<CatalogItemDetail[]> & { meta: PageMeta }>(
      `${this.baseUrl}/items`,
      { params, withCredentials: true }
    );
  }

  getItemByCode(itemCode: string): Observable<ApiResponse<CatalogItemDetail>> {
    return this.http.get<ApiResponse<CatalogItemDetail>>(
      `${this.baseUrl}/items/${itemCode}`,
      { withCredentials: true }
    );
  }

  getItemStock(itemCode: string, warehouseId?: string): Observable<ApiResponse<StockEntry[]>> {
    let params = new HttpParams();
    if (warehouseId) params = params.set('warehouse_id', warehouseId);

    return this.http.get<ApiResponse<StockEntry[]>>(
      `${this.baseUrl}/items/${itemCode}/stock`,
      { params, withCredentials: true }
    );
  }

  getWarehouseStock(warehouseId: string, page: number, size: number, belowReorder?: boolean): Observable<ApiResponse<StockEntry[]> & { meta: PageMeta }> {
    let params = new HttpParams()
      .set('page', page)
      .set('size', size);
    
    if (belowReorder !== undefined) params = params.set('below_reorder', belowReorder);

    return this.http.get<ApiResponse<StockEntry[]> & { meta: PageMeta }>(
      `${this.baseUrl}/warehouses/${warehouseId}/stock`,
      { params, withCredentials: true }
    );
  }

  listStockMovements(filter: StockMovementFilter): Observable<ApiResponse<StockMovement[]> & { meta: PageMeta }> {
    let params = new HttpParams()
      .set('page', filter.page)
      .set('size', filter.size);

    if (filter.item_code) params = params.set('item_code', filter.item_code);
    if (filter.warehouse_id) params = params.set('warehouse_id', filter.warehouse_id);
    if (filter.movement_type) params = params.set('movement_type', filter.movement_type);
    if (filter.from_date) params = params.set('from_date', filter.from_date);
    if (filter.to_date) params = params.set('to_date', filter.to_date);

    return this.http.get<ApiResponse<StockMovement[]> & { meta: PageMeta }>(
      `${this.baseUrl}/stock/movements`,
      { params, withCredentials: true }
    );
  }

  issueOutStock(command: IssueOutCommand, idempotencyKey: string): Observable<ApiResponse<{ movements: StockMovement[] }>> {
    return this.http.post<ApiResponse<{ movements: StockMovement[] }>>(
      `${this.baseUrl}/stock/issue-out`,
      command,
      {
        headers: { 'Idempotency-Key': idempotencyKey },
        withCredentials: true
      }
    );
  }
}

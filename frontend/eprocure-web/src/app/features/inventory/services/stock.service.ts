import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { ApiService } from '../../../core/http/api.service';
import { ApiResponse, PageMeta } from '../../../core/models/api-response.model';
import {
  AdjustStockRequest,
  IssueOutStockRequest,
  IssueOutStockResponse,
  ItemStockFilter,
  StockEntry,
  StockMovement,
  StockMovementListFilter,
  WarehouseStockFilter
} from '../models/stock.model';

@Injectable({ providedIn: 'root' })
export class StockService {
  private readonly api = inject(ApiService);

  getItemStock(itemCode: string, filter: ItemStockFilter = {}): Observable<ApiResponse<StockEntry[]>> {
    return this.api.get<StockEntry[]>(
      `/items/${encodeURIComponent(itemCode)}/stock`,
      { warehouse_id: filter.warehouse_id }
    );
  }

  listWarehouseStock(filter: WarehouseStockFilter): Observable<ApiResponse<StockEntry[]> & { meta: PageMeta }> {
    return this.api.get<StockEntry[]>(
      `/warehouses/${encodeURIComponent(filter.warehouseId)}/stock`,
      {
        below_reorder: filter.below_reorder,
        page: filter.page,
        size: filter.size
      }
    ) as Observable<ApiResponse<StockEntry[]> & { meta: PageMeta }>;
  }

  listMovements(filter: StockMovementListFilter): Observable<ApiResponse<StockMovement[]> & { meta: PageMeta }> {
    return this.api.get<StockMovement[]>(
      '/stock/movements',
      {
        item_code: filter.item_code,
        warehouse_id: filter.warehouse_id,
        movement_type: filter.movement_type,
        from_date: filter.from_date,
        to_date: filter.to_date,
        page: filter.page,
        size: filter.size
      }
    ) as Observable<ApiResponse<StockMovement[]> & { meta: PageMeta }>;
  }

  issueOut(
    request: IssueOutStockRequest,
    idempotencyKey = crypto.randomUUID()
  ): Observable<ApiResponse<IssueOutStockResponse>> {
    return this.api.post<IssueOutStockResponse>(
      '/stock/issue-out',
      request,
      idempotencyKey
    );
  }

  adjust(
    request: AdjustStockRequest,
    idempotencyKey = crypto.randomUUID()
  ): Observable<ApiResponse<StockMovement>> {
    return this.api.post<StockMovement>(
      '/stock/adjustment',
      request,
      idempotencyKey
    );
  }
}

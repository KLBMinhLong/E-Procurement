import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../../../core/http/api-tokens';
import { ApiResponse, PageMeta } from '../../../core/models/api-response.model';
import { CatalogCategory, CatalogItem, CatalogItemFilter } from '../models/purchase-request.model';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  getCategories(): Observable<ApiResponse<CatalogCategory[]>> {
    return this.http.get<ApiResponse<CatalogCategory[]>>(
      `${this.baseUrl}/catalog/categories`,
      { withCredentials: true }
    );
  }

  searchItems(
    filter: CatalogItemFilter
  ): Observable<ApiResponse<CatalogItem[]> & { meta: PageMeta }> {
    let params = new HttpParams();
    if (filter.q) params = params.set('q', filter.q);
    if (filter.category_code) params = params.set('category_code', filter.category_code);
    if (filter.page) params = params.set('page', filter.page);
    if (filter.size) params = params.set('size', filter.size);

    return this.http.get<ApiResponse<CatalogItem[]> & { meta: PageMeta }>(
      `${this.baseUrl}/catalog/items`,
      { params, withCredentials: true }
    );
  }
}

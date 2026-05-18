export interface ApiResponse<T> {
  success: boolean;
  code: string;
  message: string | null;
  data: T;
  meta: PageMeta | null;
  timestamp: string;
  requestId: string;
}

export interface PageMeta {
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  isFirst: boolean;
  isLast: boolean;
  sort?: string;
}

export interface ApiErrorDetail {
  field?: string;
  reason: string;
  rejectedValue?: unknown;
}

export type ApiErrorResponse = ApiResponse<never> & {
  details?: ApiErrorDetail[] | null;
};

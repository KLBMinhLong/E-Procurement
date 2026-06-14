export interface CatalogMoney {
  amount: string;
  currency: string;
}

export interface CatalogStockSummary {
  warehouseId: string;
  warehouseName: string;
  quantityOnHand: string;
  unit: string;
}

export interface CatalogItem {
  id: string;
  itemCode: string;
  name: string;
  description: string | null;
  categoryCode: string;
  unit: string;
  unitPrice: CatalogMoney;
  preferredVendorId: string | null;
  reorderPoint: string | null;
  isActive: boolean;
  stockSummary: CatalogStockSummary[];
}

export interface CatalogItemFilter {
  q?: string;
  category_code?: string;
  is_active?: boolean;
  below_reorder?: boolean;
  page: number;
  size: number;
}

export interface CreateCatalogItemRequest {
  itemCode: string;
  name: string;
  description?: string | null;
  categoryCode: string;
  unit: string;
  unitPrice: CatalogMoney;
  preferredVendorId?: string | null;
  reorderPoint?: string | null;
}

export interface UpdateCatalogItemRequest {
  name?: string | null;
  description?: string | null;
  unitPrice?: CatalogMoney | null;
  preferredVendorId?: string | null;
  reorderPoint?: string | null;
  isActive?: boolean | null;
}

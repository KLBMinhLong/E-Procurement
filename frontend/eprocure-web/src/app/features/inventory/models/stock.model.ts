export interface Warehouse {
  id: string;
  code?: string;
  name: string;
  address?: string | null;
  isActive?: boolean;
}

export interface CatalogItemDetail {
  id: string;
  itemCode: string;
  name: string;
  description: string | null;
  categoryCode: string;
  unit: string;
  unitPrice: {
    amount: string;
    currency: string;
  };
  preferredVendorId: string | null;
  reorderPoint: string | null;
  isActive: boolean;
  stockSummary?: {
    warehouseId: string;
    warehouseName: string;
    quantityOnHand: string;
    unit: string;
  }[];
}

export interface StockEntry {
  itemCode: string;
  itemName: string;
  warehouseId: string;
  warehouseName: string;
  quantityOnHand: string;
  unit: string;
  reorderPoint: string | null;
  isBelowReorder: boolean;
  lastUpdated: string;
}

export interface StockMovement {
  id: string;
  itemCode: string;
  itemName: string;
  warehouseId: string;
  movementType: 'RECEIPT_IN' | 'ISSUE_OUT' | 'ADJUSTMENT' | 'TRANSFER';
  quantity: string;
  unit: string;
  balanceAfter: string;
  sourceRefType: string | null;
  sourceRefId: string | null;
  performedBy: {
    id: string;
    fullName: string;
  };
  performedAt: string;
  notes: string | null;
}



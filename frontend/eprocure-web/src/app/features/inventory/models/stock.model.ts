export interface Warehouse {
  id: string;
  code: string;
  name: string;
  address?: string | null;
  isActive?: boolean;
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

export type StockMovementType = 'RECEIPT_IN' | 'ISSUE_OUT' | 'ADJUSTMENT' | 'TRANSFER';

export interface StockMovement {
  id: string;
  itemCode: string;
  itemName: string;
  warehouseId: string;
  movementType: StockMovementType;
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

export interface WarehouseStockFilter {
  warehouseId: string;
  below_reorder?: boolean;
  page: number;
  size: number;
}

export interface ItemStockFilter {
  warehouse_id?: string;
}

export interface StockMovementListFilter {
  item_code?: string;
  warehouse_id?: string;
  movement_type?: StockMovementType;
  from_date?: string;
  to_date?: string;
  page: number;
  size: number;
}

export interface IssueOutStockLineRequest {
  itemCode: string;
  quantity: string;
  unit: string;
}

export interface IssueOutStockRequest {
  warehouseId: string;
  prId?: string | null;
  recipientId: string;
  items: IssueOutStockLineRequest[];
  notes?: string | null;
}

export interface IssueOutStockResponse {
  movements: StockMovement[];
}

export interface AdjustStockRequest {
  warehouseId: string;
  itemCode: string;
  newQuantity: string;
  unit: string;
  reason: string;
}

export interface CompleteGrResponse {
  grStatus: string;
  movementsCreated: number;
  updatedStocks: { itemCode: string; newQuantityOnHand: string }[];
}

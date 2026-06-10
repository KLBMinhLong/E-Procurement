export interface Warehouse {
  id: string;
  code: string;
  name: string;
}

export interface StockEntry {
  itemCode: string;
  itemName: string;
  warehouseId: string;
  warehouseName: string;
  quantityOnHand: string;
  unit: string;
  reorderLevel: string | null;
}

export interface CompleteGrResponse {
  grStatus: string;
  movementsCreated: number;
  updatedStocks: { itemCode: string; newQuantityOnHand: string }[];
}

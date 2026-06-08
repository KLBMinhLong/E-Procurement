import { Warehouse } from './stock.model';

export type GoodsReceiptStatus = 'DRAFT' | 'PARTIAL' | 'COMPLETE' | 'DISCREPANCY';

export interface GrLineItem {
  id?: string;
  poLineItemId: string;
  itemCode: string | null;
  itemName: string;
  orderedQuantity: string;
  receivedQuantity: string;
  rejectedQuantity: string;
  rejectionReason: string | null;
  lotNumber: string | null;
  unit: string;
}

export interface GoodsReceiptDetail {
  id: string;
  grNumber: string;
  po: {
    id: string;
    poNumber: string;
  };
  warehouse: Warehouse;
  warehouseKeeper: {
    id: string;
    fullName: string;
  };
  receivedAt: string;
  status: GoodsReceiptStatus;
  lineItems: GrLineItem[];
  notes: string | null;
  createdAt: string;
}

export interface GoodsReceiptCreateCommand {
  poId: string;
  warehouseId: string;
  receivedAt: string | null;
  lineItems: {
    poLineItemId: string;
    receivedQuantity: string;
    rejectedQuantity: string;
    rejectionReason: string | null;
    lotNumber: string | null;
  }[];
  notes: string | null;
}

export interface GoodsReceiptUpdateCommand {
  lineItems: {
    poLineItemId: string;
    receivedQuantity: string;
    rejectedQuantity: string;
    rejectionReason: string | null;
    lotNumber: string | null;
  }[];
  notes: string | null;
}

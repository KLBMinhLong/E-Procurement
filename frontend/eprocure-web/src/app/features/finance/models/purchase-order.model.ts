import { PageMeta } from '../../../core/models/api-response.model';
import { Money } from '../../procurement/models/purchase-request.model';

export type PurchaseOrderStatus =
  | 'DRAFT'
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'SENT_TO_VENDOR'
  | 'PARTIALLY_RECEIVED'
  | 'FULLY_RECEIVED'
  | 'INVOICED'
  | 'PAID'
  | 'CLOSED'
  | 'CANCELLED';

export type PrConversionStatus = 'PENDING' | 'DELIVERED' | 'FAILED_RETRYABLE' | 'FAILED_EXHAUSTED';

export interface VendorSnapshot {
  id: string;
  name: string;
  email: string | null;
  taxCode: string | null;
}

export interface PurchasingOfficerSnapshot {
  id: string;
  fullName: string | null;
}

export interface QuantityResponse {
  amount: string;
  unit: string;
}

export interface PoLineItem {
  id: string;
  lineNumber: number;
  prLineItemId: string;
  itemName: string;
  categoryCode: string;
  quantity: QuantityResponse;
  unitPrice: string;
  totalPrice: string;
  currency: string;
}

export interface PurchaseOrder {
  id: string;
  poNumber: string;
  prId: string;
  prNumber: string;
  vendor: VendorSnapshot;
  purchasingOfficer: PurchasingOfficerSnapshot;
  status: PurchaseOrderStatus;
  lineItems: PoLineItem[];
  totalAmount: string;
  currency: string;
  deliveryAddress: string | null;
  deliveryDeadline: string | null;
  paymentTerms: string | null;
  prConversionStatus: PrConversionStatus | null;
  issuedAt: string | null;
  sentToVendorAt: string | null;
  createdAt: string;
}

export interface PurchaseOrderListFilter {
  page: number;
  size: number;
  sort: string;
  status?: PurchaseOrderStatus;
  vendor_id?: string;
  pr_id?: string;
  from_date?: string;
  to_date?: string;
}

export interface CreatePurchaseOrderRequest {
  prId: string;
  vendorId: string;
  deliveryAddress: string;
  deliveryDeadline?: string | null;
  paymentTerms?: string | null;
  notes?: string | null;
}

export interface UpdatePurchaseOrderDraftRequest {
  deliveryAddress: string;
  deliveryDeadline?: string | null;
  paymentTerms?: string | null;
}

export interface SendPurchaseOrderRequest {
  additionalNote?: string | null;
}

export interface CancelPurchaseOrderRequest {
  reason: string;
}

export interface PurchaseOrderPage {
  data: PurchaseOrder[];
  meta: PageMeta;
}

export function purchaseOrderMoney(po: PurchaseOrder): Money {
  return { amount: po.totalAmount, currency: po.currency };
}

export function lineTotalMoney(line: PoLineItem): Money {
  return { amount: line.totalPrice, currency: line.currency };
}

export function lineUnitMoney(line: PoLineItem): Money {
  return { amount: line.unitPrice, currency: line.currency };
}

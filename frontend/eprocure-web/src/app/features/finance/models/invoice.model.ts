import { Money } from '../../procurement/models/purchase-request.model';

export type InvoiceStatus =
  | 'PENDING_MATCH'
  | 'MATCHED'
  | 'MISMATCHED'
  | 'APPROVED'
  | 'DISPUTED'
  | 'PAID'
  | 'CANCELLED';

export type MatchStatus = 'MATCHED' | 'MISMATCHED' | 'PARTIAL';

export type PaymentStatus = 'CONFIRMED' | 'VOIDED';

export interface InvoiceVendor {
  id: string;
  name: string;
}

export interface InvoicePurchaseOrder {
  id: string;
  poNumber: string;
}

export interface InvoiceLineItem {
  lineNumber: number;
  poLineItemId: string;
  description: string;
  quantity: string;
  unitPrice: string;
  totalPrice: string;
}

export interface InvoiceMatchResult {
  poMatchStatus: MatchStatus;
  grMatchStatus: MatchStatus;
  qtyVariance: string;
  priceVariance: string;
  matchedAt?: string | null;
}

export interface Invoice {
  id: string;
  invoiceNumber: string;
  vendor: InvoiceVendor;
  po: InvoicePurchaseOrder;
  lineItems: InvoiceLineItem[];
  subtotal: string;
  taxAmount: string;
  totalAmount: string;
  currency: string;
  invoiceDate: string;
  dueDate: string;
  status: InvoiceStatus;
  matchResult: InvoiceMatchResult | null;
  createdAt: string;
}

export interface InvoiceListFilter {
  page: number;
  size: number;
  status?: InvoiceStatus;
  vendor_id?: string;
  po_id?: string;
  overdue_only?: boolean;
}

export interface CreateInvoiceLineItemRequest {
  poLineItemId: string;
  description: string;
  quantity: string;
  unitPrice: string;
  taxRate?: string | null;
}

export interface CreateInvoiceRequest {
  invoiceNumber: string;
  vendorId: string;
  poId: string;
  invoiceDate: string;
  dueDate: string;
  lineItems: CreateInvoiceLineItemRequest[];
  attachmentIds?: string[] | null;
}

export interface InvoiceActionResponse {
  invoiceId: string;
  status: InvoiceStatus;
}

export interface RunInvoiceMatchResponse {
  matchStatus: MatchStatus;
  matchResult: Omit<InvoiceMatchResult, 'matchedAt'>;
  requiresManualReview: boolean;
}

export interface ApproveInvoiceRequest {
  comment?: string | null;
}

export interface DisputeInvoiceRequest {
  reason: string;
}

export interface ConfirmPaymentRequest {
  paymentDate: string;
  paymentReference: string;
  paidAmount?: string | null;
  notes?: string | null;
}

export interface Payment {
  id: string;
  invoiceId: string;
  paymentDate: string;
  paymentReference: string;
  paidAmount: string;
  currency: string;
  notes: string | null;
  status: PaymentStatus;
  confirmedAt: string;
  confirmedBy: string;
}

export function invoiceTotalMoney(invoice: Invoice): Money {
  return { amount: invoice.totalAmount, currency: invoice.currency };
}

export function invoiceSubtotalMoney(invoice: Invoice): Money {
  return { amount: invoice.subtotal, currency: invoice.currency };
}

export function invoiceTaxMoney(invoice: Invoice): Money {
  return { amount: invoice.taxAmount, currency: invoice.currency };
}

export function invoiceLineTotalMoney(invoice: Invoice, line: InvoiceLineItem): Money {
  return { amount: line.totalPrice, currency: invoice.currency };
}

export function invoiceLineUnitMoney(invoice: Invoice, line: InvoiceLineItem): Money {
  return { amount: line.unitPrice, currency: invoice.currency };
}

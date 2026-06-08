import { Money, Quantity } from '../../procurement/models/purchase-request.model';

// ── Vendor ──────────────────────────────────────────────────────────
export type VendorStatus = 'PENDING' | 'APPROVED' | 'SUSPENDED' | 'DEACTIVATED';

export interface VendorContact {
  id: string;
  name: string;
  email: string | null;
  phone: string | null;
  position: string | null;
  isPrimary: boolean;
}

export interface VendorScore {
  quality: number | null;
  delivery: number | null;
  pricing: number | null;
  overall: number | null;
  lastEvaluatedAt: string | null;
}

export interface Vendor {
  id: string;
  vendorCode: string;
  name: string;
  taxCode: string | null;
  email: string | null;
  phone: string | null;
  address: string | null;
  status: VendorStatus;
  categories: string[];
  contacts: VendorContact[];
  score: VendorScore | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface VendorListFilter {
  page: number;
  size: number;
  sort: string;
  status?: VendorStatus;
  q?: string;
  category?: string;
}

export interface CreateVendorRequest {
  name: string;
  taxCode?: string | null;
  email?: string | null;
  phone?: string | null;
  address?: string | null;
  categories: string[];
  contacts?: VendorContact[];
}

// ── RFQ ─────────────────────────────────────────────────────────────
export type RfqStatus = 'OPEN' | 'EVALUATING' | 'AWARDED' | 'CLOSED' | 'CANCELLED';

export interface RfqLineItem {
  id: string;
  lineNumber: number;
  prLineItemId: string;
  itemName: string;
  categoryCode: string;
  quantity: Quantity;
  specifications: string | null;
}

export interface RfqInvitation {
  id: string;
  vendorId: string;
  vendorName: string;
  status: 'INVITED' | 'QUOTED' | 'DECLINED';
  invitedAt: string;
}

export interface Rfq {
  id: string;
  rfqNumber: string;
  prId: string;
  prNumber: string;
  title: string;
  status: RfqStatus;
  lineItems: RfqLineItem[];
  invitations: RfqInvitation[];
  submissionDeadline: string;
  totalEstimatedAmount: Money | null;
  awardedVendorId: string | null;
  awardedVendorName: string | null;
  awardedQuoteId: string | null;
  createdAt: string;
  closedAt: string | null;
}

export interface RfqListFilter {
  page: number;
  size: number;
  sort: string;
  status?: RfqStatus;
  q?: string;
}

export interface CreateRfqRequest {
  prId: string;
  vendorIds: string[];
  submissionDeadline: string;
  notes?: string | null;
}

// ── Quotes ──────────────────────────────────────────────────────────
export interface QuoteLineItem {
  rfqLineItemId: string;
  unitPrice: Money;
  quantity: Quantity;
  totalPrice: Money;
  leadTimeDays: number | null;
  notes: string | null;
}

export interface VendorQuote {
  id: string;
  rfqId: string;
  vendorId: string;
  vendorName: string;
  totalAmount: Money;
  paymentTerms: string | null;
  deliveryTerms: string | null;
  validUntil: string | null;
  lineItems: QuoteLineItem[];
  score: number | null;
  evaluationNote: string | null;
  submittedAt: string;
}

export interface SubmitQuoteRequest {
  vendorId: string;
  lineItems: {
    rfqLineItemId: string;
    unitPrice: string;
    currency: string;
    quantity: string;
    unit: string;
    leadTimeDays?: number | null;
    notes?: string | null;
  }[];
  paymentTerms?: string | null;
  deliveryTerms?: string | null;
  validUntil?: string | null;
}

export interface EvaluateQuoteRequest {
  score: number;
  note?: string | null;
}

export interface AwardRfqRequest {
  awardedQuoteId: string;
  reason?: string | null;
}

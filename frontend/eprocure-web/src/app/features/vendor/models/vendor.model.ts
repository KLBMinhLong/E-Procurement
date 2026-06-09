// ── Vendor ──────────────────────────────────────────────────────────
export type VendorStatus = 'PENDING' | 'APPROVED' | 'BLACKLISTED' | 'INACTIVE';

export interface VendorAddress {
  street: string | null;
  district: string | null;
  city: string | null;
  country: string | null;
}

export interface VendorContact {
  id: string;
  name: string;
  role: string | null;
  email: string;
  phone: string;
  isPrimary: boolean;
}

export interface VendorScorecard {
  qualityScore: number;
  deliveryScore: number;
  priceScore: number;
  responsivenessScore: number;
  overallScore: number;
  lastEvaluatedAt: string | null;
  totalOrders: number;
  onTimeDeliveryRate: number | null;
}

/** Summary row returned by GET /vendors */
export interface VendorSummary {
  id: string;
  vendorCode: string;
  name: string;
  taxCode: string;
  email: string;
  phone: string;
  status: VendorStatus;
  isOnApprovedVendorList: boolean;
  categories: string[];
  overallScore: number | null;
}

/** Full detail returned by GET /vendors/{id} and POST /vendors */
export interface VendorDetail extends VendorSummary {
  address: VendorAddress | null;
  contacts: VendorContact[];
  scorecard: VendorScorecard | null;
  contractIds: string[];
  notes: string | null;
  createdAt: string;
}

export interface VendorListFilter {
  page: number;
  size: number;
  sort: string;
  status?: VendorStatus;
  q?: string;
  category?: string;
  onAvlOnly?: boolean;
}

export interface CreateVendorContactRequest {
  name: string;
  role?: string | null;
  email: string;
  phone: string;
  isPrimary: boolean;
}

export interface CreateVendorRequest {
  name: string;
  taxCode: string;
  email: string;
  phone: string;
  address: VendorAddress;
  categories: string[];
  contacts?: CreateVendorContactRequest[];
  notes?: string | null;
}

export function formatVendorAddress(address: VendorAddress | null | undefined): string {
  if (!address) return '';
  return [address.street, address.district, address.city, address.country]
    .filter((part) => Boolean(part?.trim()))
    .join(', ');
}

// ── RFQ ─────────────────────────────────────────────────────────────
export type RfqStatus = 'OPEN' | 'EVALUATING' | 'AWARDED' | 'CLOSED' | 'CANCELLED';

export interface RfqLineItem {
  id: string;
  itemName: string;
  categoryCode: string;
  quantity: string;
  unit: string;
  specifications: string | null;
}

export interface RfqInvitationVendor {
  id: string;
  name: string;
}

export interface RfqInvitation {
  id: string;
  vendor: RfqInvitationVendor;
  invitedAt: string;
  hasSubmitted: boolean;
  submittedAt: string | null;
}

export interface RfqDetail {
  id: string;
  rfqNumber: string;
  prId: string;
  prNumber: string;
  title: string;
  status: RfqStatus;
  lineItems: RfqLineItem[];
  invitations: RfqInvitation[];
  quotes: VendorQuote[];
  submissionDeadline: string;
  awardedVendorId: string | null;
  awardedQuoteId: string | null;
  awardReason: string | null;
  createdAt: string;
}

export interface RfqListFilter {
  page: number;
  size: number;
  sort: string;
  status?: RfqStatus;
  prId?: string;
}

export interface CreateRfqRequest {
  prId: string;
  title: string;
  submissionDeadline: string;
  invitedVendorIds: string[];
  requirements?: string | null;
}

export function resolveAwardedVendorName(rfq: RfqDetail): string | null {
  if (!rfq.awardedVendorId) return null;
  const invitation = rfq.invitations.find((inv) => inv.vendor.id === rfq.awardedVendorId);
  if (invitation) return invitation.vendor.name;
  const quote = rfq.quotes.find((q) => q.vendorId === rfq.awardedVendorId);
  return quote?.vendorName ?? null;
}

export function formatRfqLineQuantity(item: RfqLineItem): string {
  return `${item.quantity} ${item.unit}`;
}

// ── Quotes ──────────────────────────────────────────────────────────
export interface VendorQuoteLineItem {
  rfqLineItemId: string;
  itemName: string;
  unitPrice: string;
  currency: string;
  quantity: string;
  totalPrice: string;
  deliveryDays: number | null;
  warranty: string | null;
}

export interface VendorQuote {
  id: string;
  rfqId: string;
  vendorId: string;
  vendorName: string;
  lineItems: VendorQuoteLineItem[];
  totalAmount: string;
  currency: string;
  validUntil: string | null;
  paymentTerms: string | null;
  notes: string | null;
  submittedAt: string;
  evaluationScore: number | null;
  evaluationNote: string | null;
}

export function formatQuoteTotal(quote: VendorQuote): string {
  const amount = Number(quote.totalAmount);
  const formatted = Number.isFinite(amount)
    ? new Intl.NumberFormat('vi-VN').format(amount)
    : quote.totalAmount;
  return `${formatted} ${quote.currency}`;
}

export interface SubmitQuoteRequest {
  vendorId: string;
  currency: string;
  validUntil: string;
  lineItems: {
    rfqLineItemId: string;
    unitPrice: string;
    deliveryDays?: number | null;
    warranty?: string | null;
  }[];
  paymentTerms?: string | null;
  notes?: string | null;
}

export interface EvaluateQuoteRequest {
  evaluationScore: number;
  evaluationNote?: string | null;
}

export interface AwardRfqRequest {
  awardedQuoteId: string;
  awardReason: string;
}

export interface AwardRfqResponse {
  awardedVendor: RfqInvitationVendor;
  awardedQuote: VendorQuote;
}

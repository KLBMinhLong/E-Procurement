#!/usr/bin/env node

import { execFileSync } from 'node:child_process';
import { randomUUID } from 'node:crypto';

const args = new Set(process.argv.slice(2));
const config = {
  gatewayUrl: trimSlash(process.env.E15_GATEWAY_URL || 'http://localhost:8080'),
  password: process.env.E15_SMOKE_PASSWORD || '',
  pollTimeoutMs: Number(process.env.E15_POLL_TIMEOUT_MS || 180000),
  pollIntervalMs: Number(process.env.E15_POLL_INTERVAL_MS || 3000),
  requestTimeoutMs: Number(process.env.E15_REQUEST_TIMEOUT_MS || 30000),
  vendorId: process.env.E15_VENDOR_ID || '80000000-0000-0000-0000-000000000101',
  warehouseId: process.env.E15_WAREHOUSE_ID || '81000000-0000-4000-8000-000000000001',
  skipDockerPs: args.has('--skip-docker') || process.env.E15_SKIP_DOCKER_PS === 'true',
  runHealth: !args.has('--flow-only'),
  runFlow: !args.has('--health-only'),
};

const healthTargets = [
  ['gateway', `${config.gatewayUrl}/health`],
  ['iam-service', `${trimSlash(process.env.IAM_SERVICE_URL || 'http://localhost:8081')}/actuator/health`],
  ['purchase-request-service', `${trimSlash(process.env.PR_SERVICE_URL || 'http://localhost:8082')}/actuator/health`],
  ['approval-service', `${trimSlash(process.env.APPROVAL_SERVICE_URL || 'http://localhost:8083')}/actuator/health`],
  ['finance-service', `${trimSlash(process.env.FINANCE_SERVICE_URL || 'http://localhost:8084')}/actuator/health`],
  ['inventory-service', `${trimSlash(process.env.INVENTORY_SERVICE_URL || 'http://localhost:8085')}/actuator/health`],
  ['vendor-service', `${trimSlash(process.env.VENDOR_SERVICE_URL || 'http://localhost:8086')}/actuator/health`],
  ['analytics-service', `${trimSlash(process.env.ANALYTICS_SERVICE_URL || 'http://localhost:8087')}/actuator/health`],
  ['notification-service', `${trimSlash(process.env.NOTIFICATION_SERVICE_URL || 'http://localhost:8088')}/actuator/health`],
];

const requiredComposeServices = [
  'postgres',
  'redis',
  'kafka',
  'kafka-init',
  'keycloak',
  'iam-service',
  'pr-service',
  'approval-service',
  'finance-service',
  'inventory-service',
  'vendor-service',
  'analytics-service',
  'notification-service',
  'nginx-gateway',
];

const roles = {
  requester: ['PR_CREATE', 'PR_VIEW_OWN', 'NOTIFICATION_VIEW_OWN'],
  manager: ['PR_APPROVE_L1', 'PR_VIEW_DEPARTMENT', 'NOTIFICATION_VIEW_OWN'],
  purchasing: ['PO_CREATE', 'PO_SEND_TO_VENDOR', 'PO_VIEW_ALL', 'VENDOR_VIEW', 'REPORT_VIEW'],
  warehouse: ['GR_CREATE', 'GR_VIEW'],
  accountant: ['INVOICE_CREATE', 'INVOICE_MATCH', 'INVOICE_APPROVE', 'PAYMENT_CONFIRM', 'PO_VIEW_ALL'],
};

const smokeCatalogItem = {
  itemCode: 'E15-OFFICE-KIT',
  itemName: 'E15 Smoke Office Package',
  description: 'Runtime smoke package for eProcure baseline verification.',
  categoryCode: 'OFFICE_SUPPLIES',
  unit: 'pcs',
  unitPrice: { amount: '1000000.0000', currency: 'VND' },
  glAccountCode: '6002',
};

const runId = new Date().toISOString().replace(/[-:.TZ]/g, '').slice(0, 14);

main().catch((error) => {
  console.error(`\n[E15-A][FAIL] ${error.message}`);
  if (error.details) {
    console.error(error.details);
  }
  process.exit(1);
});

async function main() {
  banner('E15-A Runtime/API Smoke Pack');
  if (config.runFlow && !config.password) {
    throw new Error('Set E15_SMOKE_PASSWORD before running the mutation flow.');
  }

  if (config.runHealth) {
    await step('Docker compose service health', checkDockerComposeHealth);
    await step('HTTP health endpoints', checkHttpHealth);
  }

  if (config.runFlow) {
    await step('End-to-end API flow', runEndToEndFlow);
  }

  console.log('\n[E15-A][PASS] Smoke pack completed.');
}

async function runEndToEndFlow() {
  const requester = await login('requester');
  const manager = await login('manager');
  const purchasing = await login('purchasing');
  const warehouse = await login('warehouse');
  const accountant = await login('accountant');

  await verifyPermissions(requester, roles.requester);
  await verifyPermissions(manager, roles.manager);
  await verifyPermissions(purchasing, roles.purchasing);
  await verifyPermissions(warehouse, roles.warehouse);
  await verifyPermissions(accountant, roles.accountant);

  const pr = await createPurchaseRequest(requester);
  await submitPurchaseRequest(requester, pr.id);

  const task = await waitForApprovalTask(manager, pr.id);
  await approveTask(manager, task.taskId);
  await waitForPurchaseRequestStatus(requester, pr.id, 'APPROVED');

  const po = await createManualPurchaseOrder(purchasing, pr.id);
  const readyPo = await waitForPoConversion(purchasing, po.id);
  const sentPo = await sendPurchaseOrder(purchasing, readyPo.id);

  const gr = await createGoodsReceiptWhenSnapshotArrives(warehouse, sentPo);
  await completeGoodsReceipt(warehouse, gr.id);

  const invoice = await createInvoice(accountant, sentPo);
  await matchInvoiceWhenGrSnapshotArrives(accountant, invoice.id);
  await approveInvoice(accountant, invoice.id);
  await confirmPayment(accountant, invoice.id, invoice.totalAmount);

  await readNotificationCount(requester);
  await readAnalyticsDashboard(purchasing);
}

async function login(username) {
  console.log(`  [run] login ${username}`);
  const response = await api('/api/v1/auth/login', {
    method: 'POST',
    idempotencyKey: randomUUID(),
    body: { username, password: config.password },
    expected: [200],
  });
  const cookie = extractSessionCookie(response.headers);
  assert(cookie, `Login for ${username} did not return ep_session cookie.`);
  console.log(`  [ok] login ${username}`);
  return { username, cookie };
}

async function verifyPermissions(session, requiredPermissions) {
  const me = dataOf(await api('/api/v1/users/me', { session }));
  const actual = new Set(me.permissions || []);
  const missing = requiredPermissions.filter((permission) => !actual.has(permission));
  assert(missing.length === 0, `${session.username} missing permissions: ${missing.join(', ')}`);
  console.log(`  [ok] permissions ${session.username}`);
}

async function createPurchaseRequest(session) {
  const response = await api('/api/v1/purchase-requests', {
    method: 'POST',
    session,
    idempotencyKey: randomUUID(),
    expected: [201],
    body: {
      title: `E15 smoke purchase request ${runId}`,
      justification:
        'E15 runtime smoke validates the real purchase request, approval, purchase order and finance integration flow.',
      priority: 'NORMAL',
      needByDate: addDaysDate(14),
      lineItems: [
        {
          itemCode: smokeCatalogItem.itemCode,
          itemName: smokeCatalogItem.itemName,
          description: smokeCatalogItem.description,
          categoryCode: smokeCatalogItem.categoryCode,
          quantity: { amount: '2.0000', unit: 'pcs' },
          unitPrice: smokeCatalogItem.unitPrice,
          glAccountCode: smokeCatalogItem.glAccountCode,
          isFromCatalog: true,
        },
      ],
      attachmentIds: [],
    },
  });
  const pr = dataOf(response);
  assert(pr.id, 'Create PR response missing data.id');
  console.log(`  [ok] create PR ${pr.prNumber}`);
  return pr;
}

async function submitPurchaseRequest(session, prId) {
  const response = await api(`/api/v1/purchase-requests/${prId}/submit`, {
    method: 'PATCH',
    session,
    idempotencyKey: randomUUID(),
    expected: [200],
  });
  const submitted = dataOf(response);
  assert(submitted.status === 'SUBMITTED' || submitted.status === 'PENDING_APPROVAL', `Unexpected PR submit status ${submitted.status}`);
  console.log(`  [ok] submit PR ${prId}`);
}

async function waitForApprovalTask(session, prId) {
  return poll(`approval task for PR ${prId}`, async () => {
    const inbox = dataOf(await api('/api/v1/approvals/inbox?page=1&size=20', { session }));
    const task = (inbox.items || []).find((item) => item.entityId === prId);
    if (!task) {
      return null;
    }
    console.log(`  [ok] approval task ${task.taskId}`);
    return task;
  });
}

async function approveTask(session, taskId) {
  const response = await api(`/api/v1/approvals/tasks/${taskId}/approve`, {
    method: 'PATCH',
    session,
    idempotencyKey: randomUUID(),
    expected: [200],
    body: { comment: 'Approved by E15 runtime smoke.' },
  });
  const result = dataOf(response);
  assert(result.completed === true, 'Approval task did not complete the one-step smoke approval.');
  console.log(`  [ok] approve task ${taskId}`);
}

async function waitForPurchaseRequestStatus(session, prId, expectedStatus) {
  return poll(`PR ${prId} status ${expectedStatus}`, async () => {
    const pr = dataOf(await api(`/api/v1/purchase-requests/${prId}`, { session }));
    if (pr.status !== expectedStatus) {
      return null;
    }
    console.log(`  [ok] PR status ${expectedStatus}`);
    return pr;
  });
}

async function createManualPurchaseOrder(session, prId) {
  const response = await api('/api/v1/purchase-orders', {
    method: 'POST',
    session,
    idempotencyKey: randomUUID(),
    expected: [201],
    body: {
      prId,
      vendorId: config.vendorId,
      deliveryAddress: 'eProcure Main Office, Ha Noi',
      deliveryDeadline: addDaysDate(30),
      paymentTerms: 'NET_30',
      notes: 'Created by E15 runtime smoke.',
    },
  });
  const po = dataOf(response);
  assert(po.id && po.lineItems?.length > 0, 'Manual PO response missing id or line items.');
  console.log(`  [ok] create PO ${po.poNumber}`);
  return po;
}

async function waitForPoConversion(session, poId) {
  return poll(`PO ${poId} PR conversion callback`, async () => {
    const po = dataOf(await api(`/api/v1/purchase-orders/${poId}`, { session }));
    if (po.prConversionStatus !== 'DELIVERED') {
      return null;
    }
    console.log(`  [ok] PO PR conversion delivered`);
    return po;
  });
}

async function sendPurchaseOrder(session, poId) {
  const response = await api(`/api/v1/purchase-orders/${poId}/send`, {
    method: 'POST',
    session,
    idempotencyKey: randomUUID(),
    expected: [200],
    body: { additionalNote: 'Sent by E15 runtime smoke.' },
  });
  const po = dataOf(response);
  assert(po.status === 'SENT_TO_VENDOR', `Unexpected PO status after send: ${po.status}`);
  console.log(`  [ok] send PO ${po.poNumber}`);
  return po;
}

async function createGoodsReceiptWhenSnapshotArrives(session, po) {
  const lineItems = po.lineItems.map((line) => ({
    poLineItemId: line.id,
    receivedQuantity: line.quantity.amount,
    rejectedQuantity: '0.0000',
    lotNumber: `LOT-${runId}`,
  }));
  const idempotencyKey = randomUUID();
  return poll(`inventory PO snapshot for PO ${po.id}`, async () => {
    try {
      const response = await api('/api/v1/goods-receipts', {
        method: 'POST',
        session,
        idempotencyKey,
        expected: [201, 200],
        body: {
          poId: po.id,
          warehouseId: config.warehouseId,
          receivedAt: new Date().toISOString(),
          lineItems,
          notes: 'Created by E15 runtime smoke.',
        },
      });
      const gr = dataOf(response);
      assert(gr.id, 'GR response missing id.');
      console.log(`  [ok] create GR ${gr.grNumber}`);
      return gr;
    } catch (error) {
      if (error instanceof ApiError && ['INV_009', 'INV_010', 'SYS_001'].includes(error.code)) {
        return null;
      }
      throw error;
    }
  });
}

async function completeGoodsReceipt(session, grId) {
  const response = await api(`/api/v1/goods-receipts/${grId}/complete`, {
    method: 'POST',
    session,
    idempotencyKey: randomUUID(),
    expected: [200],
  });
  const result = dataOf(response);
  assert(result.grStatus === 'COMPLETE', `Unexpected GR status after complete: ${result.grStatus}`);
  console.log(`  [ok] complete GR ${grId}`);
}

async function createInvoice(session, po) {
  const response = await api('/api/v1/invoices', {
    method: 'POST',
    session,
    idempotencyKey: randomUUID(),
    expected: [201],
    body: {
      invoiceNumber: `INV-SMOKE-${runId}`,
      vendorId: po.vendor.id,
      poId: po.id,
      invoiceDate: addDaysDate(0),
      dueDate: addDaysDate(30),
      lineItems: po.lineItems.map((line) => ({
        poLineItemId: line.id,
        description: line.itemName,
        quantity: line.quantity.amount,
        unitPrice: line.unitPrice,
        taxRate: '0.0000',
      })),
      attachmentIds: [],
    },
  });
  const invoice = dataOf(response);
  assert(invoice.id, 'Invoice response missing id.');
  console.log(`  [ok] create invoice ${invoice.invoiceNumber}`);
  return invoice;
}

async function matchInvoiceWhenGrSnapshotArrives(session, invoiceId) {
  return poll(`finance GR snapshot for invoice ${invoiceId}`, async () => {
    const response = await api(`/api/v1/invoices/${invoiceId}/match`, {
      method: 'POST',
      session,
      idempotencyKey: randomUUID(),
      expected: [200],
    });
    const result = dataOf(response);
    if (result.matchStatus !== 'MATCHED') {
      return null;
    }
    console.log(`  [ok] match invoice ${invoiceId}`);
    return result;
  });
}

async function approveInvoice(session, invoiceId) {
  const response = await api(`/api/v1/invoices/${invoiceId}/approve`, {
    method: 'POST',
    session,
    idempotencyKey: randomUUID(),
    expected: [200],
    body: { comment: 'Approved by E15 runtime smoke.' },
  });
  const result = dataOf(response);
  assert(result.status === 'APPROVED', `Unexpected invoice status after approval: ${result.status}`);
  console.log(`  [ok] approve invoice ${invoiceId}`);
}

async function confirmPayment(session, invoiceId, paidAmount) {
  const response = await api(`/api/v1/invoices/${invoiceId}/confirm-payment`, {
    method: 'POST',
    session,
    idempotencyKey: randomUUID(),
    expected: [200],
    body: {
      paymentDate: addDaysDate(0),
      paymentReference: `PAY-SMOKE-${runId}`,
      paidAmount,
      notes: 'Payment confirmed by E15 runtime smoke.',
    },
  });
  const payment = dataOf(response);
  assert(payment.status === 'CONFIRMED', `Unexpected payment status: ${payment.status}`);
  console.log(`  [ok] confirm payment ${payment.paymentReference}`);
}

async function readNotificationCount(session) {
  const count = dataOf(await api('/api/v1/notifications/count', { session }));
  assert(count !== null && count !== undefined, 'Notification count response missing data.');
  console.log('  [ok] notification count');
}

async function readAnalyticsDashboard(session) {
  dataOf(await api('/api/v1/dashboard/purchasing', { session }));
  const fromDate = addDaysDate(-30);
  const toDate = addDaysDate(1);
  dataOf(await api(`/api/v1/kpi/cycle-time?from_date=${fromDate}&to_date=${toDate}`, { session }));
  console.log('  [ok] analytics dashboard and KPI');
}

async function checkHttpHealth() {
  for (const [name, url] of healthTargets) {
    const response = await fetchWithTimeout(url, { method: 'GET' });
    assert(response.ok, `${name} health failed with HTTP ${response.status}`);
    console.log(`  [ok] ${name}`);
  }
}

function checkDockerComposeHealth() {
  if (config.skipDockerPs) {
    console.log('  [skip] docker compose ps');
    return;
  }
  let output;
  try {
    output = execFileSync('docker', ['compose', 'ps', '--all', '--format', 'json'], {
      encoding: 'utf8',
      timeout: 20000,
    }).trim();
  } catch (error) {
    throw new Error(`docker compose ps failed: ${error.message}`);
  }
  const rows = parseComposeRows(output);
  const byService = new Map(rows.map((row) => [row.Service || row.Name, row]));
  for (const service of requiredComposeServices) {
    const row = byService.get(service);
    assert(row, `docker compose service not found: ${service}`);
    const state = String(row.State || '').toLowerCase();
    const status = String(row.Status || '').toLowerCase();
    const health = String(row.Health || '').toLowerCase();
    const exitCode = row.ExitCode ?? row.ExitCode;
    if (service === 'kafka-init') {
      assert(state === 'exited' || status.includes('exited') || String(exitCode) === '0', 'kafka-init has not completed successfully.');
    } else {
      assert(state === 'running' || status.includes('up'), `${service} is not running: ${row.Status || row.State}`);
      assert(!health || health === 'healthy' || status.includes('healthy'), `${service} is not healthy: ${row.Status || row.Health}`);
    }
    console.log(`  [ok] ${service}`);
  }
}

function parseComposeRows(output) {
  if (!output) {
    return [];
  }
  if (output.startsWith('[')) {
    return JSON.parse(output);
  }
  return output
    .split(/\r?\n/)
    .filter(Boolean)
    .map((line) => JSON.parse(line));
}

async function api(path, options = {}) {
  const method = options.method || 'GET';
  const headers = {
    Accept: 'application/json',
    ...(options.headers || {}),
  };
  if (options.body !== undefined) {
    headers['Content-Type'] = 'application/json';
  }
  if (options.idempotencyKey) {
    headers['Idempotency-Key'] = options.idempotencyKey;
  }
  if (options.session?.cookie) {
    headers.Cookie = options.session.cookie;
  }

  const response = await fetchWithTimeout(`${config.gatewayUrl}${path}`, {
    method,
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });
  const text = await response.text();
  const body = parseBody(text);
  const expected = options.expected || [200];
  if (!expected.includes(response.status)) {
    throw new ApiError(method, path, response.status, body, text);
  }
  return { status: response.status, headers: response.headers, body };
}

async function fetchWithTimeout(url, init) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), config.requestTimeoutMs);
  try {
    return await fetch(url, { ...init, signal: controller.signal });
  } catch (error) {
    const method = init?.method || 'GET';
    if (error.name === 'AbortError') {
      throw new Error(`${method} ${url} aborted after ${config.requestTimeoutMs}ms`);
    }
    throw new Error(`${method} ${url} failed: ${error.message}`);
  } finally {
    clearTimeout(timer);
  }
}

function parseBody(text) {
  if (!text) {
    return null;
  }
  try {
    return JSON.parse(text);
  } catch {
    return text;
  }
}

function dataOf(response) {
  const body = response.body;
  if (body && typeof body === 'object' && 'success' in body) {
    assert(body.success === true, `API returned success=false code=${body.code}`);
    return body.data;
  }
  return body;
}

function extractSessionCookie(headers) {
  const setCookie = typeof headers.getSetCookie === 'function'
    ? headers.getSetCookie().join(',')
    : headers.get('set-cookie') || '';
  const match = setCookie.match(/(?:^|,\s*)ep_session=([^;,]+)/);
  return match ? `ep_session=${match[1]}` : '';
}

async function poll(label, fn) {
  const deadline = Date.now() + config.pollTimeoutMs;
  let lastError = null;
  while (Date.now() < deadline) {
    try {
      const result = await fn();
      if (result) {
        return result;
      }
    } catch (error) {
      lastError = error;
      if (!(error instanceof ApiError)) {
        throw error;
      }
    }
    await delay(config.pollIntervalMs);
  }
  const suffix = lastError ? ` Last error: ${lastError.message}` : '';
  throw new Error(`Timed out waiting for ${label}.${suffix}`);
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function addDaysDate(days) {
  const date = new Date();
  date.setUTCDate(date.getUTCDate() + days);
  return date.toISOString().slice(0, 10);
}

function trimSlash(value) {
  return value.replace(/\/+$/, '');
}

function assert(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

async function step(name, fn) {
  console.log(`\n[E15-A] ${name}`);
  await fn();
}

function banner(title) {
  console.log(`[E15-A] ${title}`);
  console.log(`[E15-A] gateway=${config.gatewayUrl}`);
}

class ApiError extends Error {
  constructor(method, path, status, body, rawText) {
    const code = body && typeof body === 'object' ? body.code : undefined;
    const message = body && typeof body === 'object' ? body.message : undefined;
    super(`${method} ${path} failed with HTTP ${status}${code ? ` code=${code}` : ''}${message ? ` message=${message}` : ''}`);
    this.status = status;
    this.code = code;
    this.details = rawText && rawText.length < 2000 ? rawText : undefined;
  }
}

// ── System Health ────────────────────────────────────────────────
export type ServiceHealthStatus = 'UP' | 'DOWN' | 'DEGRADED' | 'UNKNOWN';
export type OverallHealthStatus = 'UP' | 'DEGRADED' | 'DOWN';

export interface ServiceHealth {
  name: string;
  status: ServiceHealthStatus;
  responseTime: number;
  uptime: string | null;
  lastCheck: string;
}

export interface InfraPostgresHealth {
  status: string;
  connections: number;
  maxConnections: number;
}

export interface InfraRedisHealth {
  status: string;
  usedMemory: string;
  maxMemory: string;
  keyCount: number;
}

export interface KafkaLagGroup {
  group: string;
  lag: number;
}

export interface InfraKafkaHealth {
  status: string;
  topicCount: number;
  lagByGroup: KafkaLagGroup[];
}

export interface InfrastructureHealth {
  postgresql: InfraPostgresHealth;
  redis: InfraRedisHealth;
  kafka: InfraKafkaHealth;
}

export interface SystemHealthData {
  overallStatus: OverallHealthStatus;
  services: ServiceHealth[];
  infrastructure: InfrastructureHealth;
  checkedAt: string;
}

// ── Service Config ──────────────────────────────────────────────
export type ConfigServiceStatus = 'UP' | 'DOWN' | 'DEGRADED' | 'UNKNOWN';

export interface EnvVariable {
  key: string;
  value: string;
  isSensitive: boolean;
  description: string | null;
  lastUpdatedAt: string;
  lastUpdatedBy: string;
}

export interface ServiceConfig {
  serviceName: string;
  displayName: string;
  status: ConfigServiceStatus;
  version: string | null;
  variables: EnvVariable[];
  lastHealthCheck: string | null;
}

export interface ServiceConfigSummary {
  totalServices: number;
  upCount: number;
  downCount: number;
  degradedCount: number;
}

export interface ServiceConfigUpdateRequest {
  variables: { key: string; value: string; isSensitive?: boolean; description?: string | null }[];
  confirmationCode: string;
  requiresRestart?: boolean;
  changeReason: string;
}

export type ConfigActionStatus = 'PENDING_MANUAL_APPLY';

export interface ServiceConfigUpdateResult {
  actionId: string;
  status: ConfigActionStatus;
  updatedCount: number;
  requiresRestart: boolean;
  affectedService: string;
  applied: boolean;
}

export interface ServiceRestartRequest {
  confirmationCode: string;
  reason: string;
}

export interface ServiceRestartResult {
  actionId: string;
  status: ConfigActionStatus;
  estimatedDowntimeSeconds: number;
  triggeredAt: string;
  applied: boolean;
}

export interface EncryptionKeyRotationRequest {
  confirmationCode: string;
  keySize?: 2048 | 4096;
}

export interface EncryptionKeyRotationResult {
  actionId: string;
  status: ConfigActionStatus;
  newKeyVersion: string;
  rotatedAt: string;
  oldKeyRetiredAt: string;
  applied: boolean;
}

// ── Audit Log ───────────────────────────────────────────────────
export interface AuditActor {
  id: string;
  name: string;
  roles: string[];
  ip: string;
}

export interface AuditLogEntry {
  id: number;
  actor: AuditActor;
  action: string;
  entityType: string;
  entityId: string | null;
  entityNumber: string | null;
  occurredAt: string;
  httpMethod: string | null;
  endpoint: string | null;
  requestId: string | null;
  isSuccess: boolean;
  errorCode: string | null;
  oldValue: unknown;
  newValue: unknown;
  description: string | null;
  serviceName: string;
}

export interface AuditLogFilter {
  from_time: string;
  to_time: string;
  actor_id?: string;
  entity_type?: string;
  entity_id?: string;
  action?: string;
  service_name?: string;
  is_success?: boolean;
  page: number;
  size: number;
}

export type AuditExportStatus = 'QUEUED' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface AuditExportJob {
  jobId: string;
  status: AuditExportStatus;
  fromTime: string;
  toTime: string;
  actorId: string | null;
  entityType: string | null;
  action: string | null;
  fileName: string | null;
  downloadUrl: string | null;
  failureReason: string | null;
  requestedAt: string;
  completedAt: string | null;
  expiresAt: string | null;
}

export interface AuditExportRequest {
  fromTime: string;
  toTime: string;
  actorId?: string | null;
  entityType?: string | null;
  action?: string | null;
}

// ── Active Sessions ─────────────────────────────────────────────
export interface ActiveSession {
  sessionId: string;
  userId: string;
  userName: string;
  fullName: string;
  ipAddress: string | null;
  userAgent: string | null;
  createdAt: string;
  lastActivity: string;
  expiresAt: string;
}

export interface SessionListFilter {
  user_id?: string;
  page: number;
  size: number;
}

// ── Catalog Category Admin ──────────────────────────────────────
export interface CatalogCategoryAdmin {
  code: string;
  name: string;
  parentCode: string | null;
  requiresSpecialApproval: boolean;
  specialApproverRole: string | null;
  requiresRfqAbove: string | null;
  isCapex: boolean;
  itemCount: number;
  isDeleted: boolean;
}

export interface CatalogCategoryRequest {
  code: string;
  name: string;
  parentCode?: string | null;
  requiresSpecialApproval?: boolean;
  specialApproverRole?: string | null;
  requiresRfqAbove?: string | null;
  isCapex?: boolean;
}

// ── Department Admin ────────────────────────────────────────────
export interface DepartmentAdmin {
  id: string;
  code: string;
  name: string;
  parentId: string | null;
  headUserId: string | null;
  glAccountPrefix: string | null;
  memberCount: number;
  childCount: number;
  isDeleted: boolean;
}

export interface DepartmentAdminRequest {
  code: string;
  name: string;
  parentId?: string | null;
  headUserId?: string | null;
  glAccountPrefix?: string | null;
}

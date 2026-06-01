export interface AdminUserSummary {
  id: string;
  employeeCode: string;
  username: string;
  email: string;
  fullName: string;
  phone: string | null;
  avatarUrl: string | null;
  status: UserStatus;
  roles: string[];
  departmentId: string | null;
  departmentName: string | null;
  createdAt: string;
}

export interface AdminUserDetail extends AdminUserSummary {
  orgNodeId: string | null;
  permissions: string[];
  twoFactorEnabled: boolean;
  lastLoginAt: string | null;
}

export type UserStatus = 'PENDING_VERIFY' | 'ACTIVE' | 'INACTIVE' | 'LOCKED';

export interface CreateUserRequest {
  employeeCode: string;
  username: string;
  email: string;
  fullName: string;
  phone?: string | null;
  departmentId: string;
  orgNodeId?: string | null;
  roles: string[];
}

export interface UpdateUserRequest {
  fullName?: string;
  phone?: string | null;
  departmentId?: string;
  orgNodeId?: string | null;
}

export interface AssignRolesRequest {
  roles: string[];
}

export interface ChangeUserStatusRequest {
  status: 'ACTIVE' | 'INACTIVE' | 'LOCKED';
  reason?: string | null;
}

export interface AdminRole {
  code: string;
  name: string;
  description: string | null;
  isSystem: boolean;
  permissions?: string[];
}

export interface CreateRolePayload {
  code: string;
  name: string;
  description: string | null;
  permissions: string[];
}

export interface UpdateRolePayload {
  code: string;
  name: string;
  description: string | null;
}

export interface AdminPermission {
  code: string;
  name: string;
  description: string | null;
  module: string;
}

export type NotificationTemplateChannel = 'EMAIL' | 'IN_APP' | 'PUSH';

export interface NotificationTemplate {
  code: string;
  eventType: string;
  channel: NotificationTemplateChannel;
  language: string;
  subjectTemplate: string | null;
  bodyTemplate: string;
  isActive: boolean;
  updatedAt: string;
}

export interface NotificationTemplateFilter {
  channel?: NotificationTemplateChannel | '';
  eventType?: string;
  language?: string;
}

export interface UpdateNotificationTemplateRequest {
  subjectTemplate: string | null;
  bodyTemplate: string;
  isActive: boolean;
}

export interface NotificationTemplatePreview {
  subject: string | null;
  htmlBody: string;
}

export interface AdminDepartment {
  id: string;
  code: string;
  name: string;
  parentCode: string | null;
  managerId: string | null;
  managerName: string | null;
  path: string;
  level: number;
  children?: AdminDepartment[];
}

export interface CreateDepartmentRequest {
  code: string;
  name: string;
  parentCode?: string | null;
  managerId?: string | null;
}

export interface UpdateDepartmentRequest {
  name?: string;
  parentCode?: string | null;
  managerId?: string | null;
}

export interface UserListFilter {
  page: number;
  size: number;
  q?: string;
  departmentId?: string;
  status?: UserStatus | '';
}

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
}

export interface AdminPermission {
  code: string;
  name: string;
  description: string | null;
  module: string;
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

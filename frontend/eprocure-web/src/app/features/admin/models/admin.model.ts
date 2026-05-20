export interface AdminUserSummary {
  id: string;
  username: string;
  email: string;
  fullName: string;
  phone: string | null;
  status: 'PENDING_VERIFY' | 'ACTIVE' | 'INACTIVE' | 'LOCKED';
  roles: string[];
  departmentId: string | null;
  departmentName: string | null;
  createdAt: string;
}

export interface AdminUserDetail extends AdminUserSummary {
  twoFactorEnabled: boolean;
  googleLinked: boolean;
  failedLoginAttempts: number;
  lockedUntil: string | null;
}

export interface CreateUserRequest {
  username: string;
  email: string;
  fullName: string;
  phone?: string | null;
  departmentId: string;
  roles: string[];
}

export interface UpdateUserRequest {
  fullName?: string;
  phone?: string | null;
  departmentId?: string;
  roles?: string[];
}

export interface ChangeUserStatusRequest {
  status: 'ACTIVE' | 'INACTIVE' | 'LOCKED';
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
  status?: 'PENDING_VERIFY' | 'ACTIVE' | 'INACTIVE' | 'LOCKED' | '';
}

export interface DepartmentSummary {
  id: string;
  code: string;
  name: string;
}

export interface UserContext {
  id: string;
  employeeCode: string;
  username: string;
  fullName: string;
  email: string;
  avatarUrl: string | null;
  department: DepartmentSummary | null;
  roles: string[];
  permissions: string[];
  twoFactorEnabled: boolean;
  lastLoginAt: string | null;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface EncryptedRequest {
  encryptedPayload: string;
  encryptedAesKey: string;
  iv: string;
  keyVersion: string;
}

export interface PublicKeyResponse {
  publicKey: string;
  keyVersion: string;
  algorithm: string;
}

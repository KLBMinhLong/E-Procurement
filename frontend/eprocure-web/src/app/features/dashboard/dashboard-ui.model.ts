export type DashboardTab = 'executive' | 'manager' | 'purchasing' | 'requester' | 'reports';

export interface DashboardTabItem {
  id: DashboardTab;
  labelKey: string;
  permissions: string[];
}

export interface DashboardDepartmentOption {
  id: string;
  label: string;
}

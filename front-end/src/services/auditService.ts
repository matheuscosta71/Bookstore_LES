import type { AuditLogRow, Page } from '@/types/api';
import { adminApi } from './api';

export async function listAuditLogs(page = 0, size = 20): Promise<Page<AuditLogRow>> {
  const { data } = await adminApi.get<Page<AuditLogRow>>('/audit-logs', {
    params: { page, size },
  });
  return data;
}

export async function listAuditLogsByEntity(
  entityName: string,
  entityId: string,
): Promise<AuditLogRow[]> {
  const { data } = await adminApi.get<AuditLogRow[]>(`/audit-logs/${entityName}/${entityId}`);
  return data;
}

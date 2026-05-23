import { registerSchema } from '@/utils/schemas';
import type { z } from 'zod';
import type { ValidationResult } from './types';

export type RegisterFormValues = z.infer<typeof registerSchema>;

export function validateRegister(data: unknown): ValidationResult {
  const parsed = registerSchema.safeParse(data);
  if (parsed.success) {
    return { valid: true, errors: {} };
  }
  const errors: Record<string, string> = {};
  for (const issue of parsed.error.issues) {
    const key = issue.path[0];
    if (typeof key === 'string' && !errors[key]) {
      errors[key] = issue.message;
    }
  }
  return { valid: false, errors };
}

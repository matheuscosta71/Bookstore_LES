/**
 * Logs padronizados para debug e suporte.
 * Formato: [matheus-gn] [Escopo][função] mensagem
 * Não usar para dados sensíveis (senha, token, cartão completo, CPF completo em produção).
 */

type Meta = Record<string, unknown> | undefined;

const BRAND = '[matheus-gn]';

function line(scope: string, fn: string, message: string): string {
  return `${BRAND} [${scope}][${fn}] ${message}`;
}

export const appLogger = {
  info(scope: string, fn: string, message: string, meta?: Meta): void {
    if (meta !== undefined) console.info(line(scope, fn, message), meta);
    else console.info(line(scope, fn, message));
  },

  warn(scope: string, fn: string, message: string, meta?: Meta): void {
    if (meta !== undefined) console.warn(line(scope, fn, message), meta);
    else console.warn(line(scope, fn, message));
  },

  error(scope: string, fn: string, message: string, meta?: Meta): void {
    if (meta !== undefined) console.error(line(scope, fn, message), meta);
    else console.error(line(scope, fn, message));
  },

  /** Somente em desenvolvimento — reduz ruído em produção. */
  debug(scope: string, fn: string, message: string, meta?: Meta): void {
    if (!import.meta.env.DEV) return;
    if (meta !== undefined) console.debug(line(scope, fn, message), meta);
    else console.debug(line(scope, fn, message));
  },
};

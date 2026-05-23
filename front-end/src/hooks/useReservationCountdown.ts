import { useEffect, useMemo, useRef, useState } from 'react';

function formatMmSs(totalSeconds: number): string {
  const clamped = Math.max(0, totalSeconds);
  const m = Math.floor(clamped / 60);
  const s = clamped % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
}

/**
 * Contagem regressiva até o fim da reserva (RNF0042).
 * Usa `expiresAtIso` da API quando existir; senão, aproxima com `fallbackExpirationMinutes` + `deadlineKey`.
 * Ao passar do prazo, chama `onElapsed` e repete a cada 2s até o servidor refletir a expiração (recarregar carrinho).
 */
export function useReservationCountdown(
  expiresAtIso: string | undefined,
  /** Item ainda ativo na lista (não expirado no último payload). */
  active: boolean,
  onElapsed?: () => void,
  /** Se a API não enviar `expiresAt`, prazo aproximado a partir do carregamento (por item). */
  fallbackExpirationMinutes?: number,
  /** Estabiliza o fallback quando o item do carrinho muda (ex.: `item.id`). */
  deadlineKey?: string,
) {
  const [tick, setTick] = useState(0);
  const [fallbackEndMs, setFallbackEndMs] = useState<number | null>(null);
  const onElapsedRef = useRef(onElapsed);
  onElapsedRef.current = onElapsed;

  useEffect(() => {
    if (expiresAtIso) {
      setFallbackEndMs(null);
      return;
    }
    if (!active || !fallbackExpirationMinutes || !deadlineKey) {
      setFallbackEndMs(null);
      return;
    }
    setFallbackEndMs(Date.now() + fallbackExpirationMinutes * 60 * 1000);
  }, [expiresAtIso, active, fallbackExpirationMinutes, deadlineKey]);

  const endMs = useMemo(() => {
    if (expiresAtIso) {
      const t = new Date(expiresAtIso).getTime();
      return Number.isNaN(t) ? null : t;
    }
    return fallbackEndMs;
  }, [expiresAtIso, fallbackEndMs]);

  useEffect(() => {
    if (!active || endMs == null) return;
    const id = setInterval(() => setTick((n) => n + 1), 1000);
    return () => clearInterval(id);
  }, [active, endMs]);

  const { remainingSeconds, label } = useMemo(() => {
    if (endMs == null) {
      return { remainingSeconds: null as number | null, label: null as string | null };
    }
    const sec = Math.floor((endMs - Date.now()) / 1000);
    return { remainingSeconds: sec, label: formatMmSs(sec) };
  }, [endMs, tick]);

  const crossedDeadline =
    active && endMs != null && remainingSeconds !== null && remainingSeconds <= 0;

  /**
   * Quando o timer chega a zero, o servidor pode ainda não considerar expirado (desvio de relógio).
   * Disparamos o recarregar do carrinho na transição e repetimos a cada 2s até o pai atualizar (item.expired).
   */
  useEffect(() => {
    if (!crossedDeadline) return;
    onElapsedRef.current?.();
    const id = window.setInterval(() => onElapsedRef.current?.(), 2000);
    return () => window.clearInterval(id);
  }, [crossedDeadline]);

  return {
    remainingSeconds,
    label,
    isPastDeadline:
      active && remainingSeconds !== null && remainingSeconds <= 0,
  };
}

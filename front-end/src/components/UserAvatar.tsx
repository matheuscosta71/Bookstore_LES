import { useState } from 'react';
import { cn } from '@/utils/cn';

function getInitials(fullName: string | undefined | null, email: string | undefined | null): string {
  const n = fullName?.trim();
  if (n) {
    const parts = n.split(/\s+/).filter(Boolean);
    if (parts.length >= 2) {
      const a = parts[0][0];
      const b = parts[parts.length - 1][0];
      return `${a}${b}`.toUpperCase();
    }
    if (parts.length === 1 && parts[0].length >= 1) {
      const w = parts[0];
      return w.length >= 2 ? w.slice(0, 2).toUpperCase() : w[0].toUpperCase();
    }
  }
  const e = email?.trim();
  if (e?.length) return e[0].toUpperCase();
  return '?';
}

function hueFromSeed(seed: string): number {
  let h = 0;
  for (let i = 0; i < seed.length; i++) h = (h * 31 + seed.charCodeAt(i)) >>> 0;
  return h % 360;
}

const sizeClasses = {
  sm: 'h-8 w-8 min-h-8 min-w-8 text-[11px]',
  md: 'h-10 w-10 min-h-10 min-w-10 text-xs',
};

export type UserAvatarProps = {
  fullName?: string | null;
  email?: string | null;
  /** Estabiliza a cor de fundo; costuma ser o id do utilizador */
  seed?: string | null;
  size?: 'sm' | 'md';
  className?: string;
  /** Quando existir API de foto de perfil */
  avatarUrl?: string | null;
};

export function UserAvatar({
  fullName,
  email,
  seed,
  size = 'md',
  className,
  avatarUrl,
}: UserAvatarProps) {
  const [imgFailed, setImgFailed] = useState(false);
  const initials = getInitials(fullName, email);
  const identityKey = seed || email || fullName || '';
  const hasIdentity = Boolean(fullName?.trim() || email?.trim());
  const hue = identityKey ? hueFromSeed(identityKey) : 220;

  const palette = hasIdentity
    ? { backgroundColor: `hsl(${hue}, 42%, 92%)`, color: `hsl(${hue}, 45%, 28%)` }
    : { backgroundColor: 'hsl(220, 14%, 92%)', color: 'hsl(220, 9%, 46%)' };

  const ring = 'ring-2 ring-white';

  if (avatarUrl && !imgFailed) {
    return (
      <span
        className={cn(
          'inline-flex shrink-0 items-center justify-center overflow-hidden rounded-full',
          ring,
          sizeClasses[size],
          className,
        )}
      >
        <img
          src={avatarUrl}
          alt=""
          className="h-full w-full object-cover"
          onError={() => setImgFailed(true)}
        />
      </span>
    );
  }

  return (
    <span
      className={cn(
        'inline-flex shrink-0 items-center justify-center rounded-full font-semibold',
        ring,
        sizeClasses[size],
        className,
      )}
      style={palette}
      aria-hidden
    >
      {hasIdentity ? initials : '?'}
    </span>
  );
}

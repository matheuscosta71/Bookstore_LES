import type { ReactNode } from 'react';

type Props = {
  title: string;
  subtitle?: string;
  action?: ReactNode;
};

export function SectionHeader({ title, subtitle, action }: Props) {
  return (
    <div className="mb-5 flex flex-col gap-2 md:flex-row md:items-end md:justify-between">
      <div>
        <h2 className="font-display text-2xl font-semibold tracking-tight text-ink md:text-3xl">{title}</h2>
        {subtitle && <p className="mt-1 text-sm text-ink-muted md:text-base">{subtitle}</p>}
      </div>
      {action}
    </div>
  );
}

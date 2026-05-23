type Props = {
  title: string;
  description?: string;
  action?: React.ReactNode;
};

export function EmptyState({ title, description, action }: Props) {
  return (
    <div className="rounded-xl border border-dashed border-stone-300 bg-paper-accent/50 px-6 py-16 text-center">
      <p className="font-display text-lg text-ink">{title}</p>
      {description && <p className="mt-2 text-sm text-ink-muted">{description}</p>}
      {action && <div className="mt-6">{action}</div>}
    </div>
  );
}

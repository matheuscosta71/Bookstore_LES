import { cn } from '@/utils/cn';

type Props = {
  value: number;
  min?: number;
  max?: number;
  onChange: (n: number) => void;
  disabled?: boolean;
};

export function QuantitySelector({ value, min = 1, max = 999, onChange, disabled }: Props) {
  return (
    <div className={cn('inline-flex items-center rounded-lg border border-stone-200', disabled && 'opacity-50')}>
      <button
        type="button"
        disabled={disabled || value <= min}
        className="px-3 py-1.5 text-lg leading-none hover:bg-stone-50 disabled:cursor-not-allowed"
        onClick={() => onChange(Math.max(min, value - 1))}
      >
        −
      </button>
      <span className="min-w-[2rem] text-center text-sm font-medium">{value}</span>
      <button
        type="button"
        disabled={disabled || value >= max}
        className="px-3 py-1.5 text-lg leading-none hover:bg-stone-50 disabled:cursor-not-allowed"
        onClick={() => onChange(Math.min(max, value + 1))}
      >
        +
      </button>
    </div>
  );
}

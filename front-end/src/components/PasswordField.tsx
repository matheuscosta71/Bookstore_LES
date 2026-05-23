import { forwardRef, useId, useState, type ComponentPropsWithoutRef } from 'react';
import { cn } from '@/utils/cn';

export type PasswordFieldProps = Omit<ComponentPropsWithoutRef<'input'>, 'type'>;

function EyeIcon({ slash }: { slash: boolean }) {
  return (
    <svg
      xmlns="http://www.w3.org/2000/svg"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth={1.5}
      className="h-5 w-5"
      aria-hidden
    >
      {slash ? (
        <path
          strokeLinecap="round"
          strokeLinejoin="round"
          d="M3.98 8.223A10.477 10.477 0 001.934 12C3.226 16.338 7.244 19.5 12 19.5c.993 0 1.953-.138 2.863-.395M6.228 6.228A10.45 10.45 0 0112 4.5c4.756 0 8.773 3.162 10.065 7.498a10.523 10.523 0 01-4.293 5.774M6.228 6.228L3 3m3.228 3.228l3.65 3.65m7.894 7.894L21 21m-3.228-3.228l-3.65-3.65m0 0a3 3 0 10-4.243-4.243m4.242 4.242L9.88 9.88"
        />
      ) : (
        <>
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            d="M2.036 12.322a1.012 1.012 0 010-.639C3.423 7.51 7.36 4.5 12 4.5c4.638 0 8.573 3.007 9.963 7.178.07.207.07.431 0 .639C20.577 16.49 16.64 19.5 12 19.5c-4.638 0-8.573-3.007-9.963-7.178z"
          />
          <path strokeLinecap="round" strokeLinejoin="round" d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
        </>
      )}
    </svg>
  );
}

/**
 * Campo de senha com botão para alternar visibilidade (acessível: aria-pressed e rótulos em PT).
 */
export const PasswordField = forwardRef<HTMLInputElement, PasswordFieldProps>(function PasswordField(
  { className, autoComplete = 'current-password', id, ...props },
  ref,
) {
  const [visible, setVisible] = useState(false);
  const fallbackId = useId();
  const inputId = id ?? fallbackId;
  const toggleId = `${inputId}-toggle`;

  return (
    <div className="relative w-full">
      <input
        ref={ref}
        id={inputId}
        type={visible ? 'text' : 'password'}
        autoComplete={autoComplete}
        className={cn(
          'w-full rounded-lg border border-stone-200 py-2 pl-3 pr-11 text-sm',
          'focus:border-brand focus:outline-none focus:ring-2 focus:ring-brand/20',
          className,
        )}
        {...props}
      />
      <button
        id={toggleId}
        type="button"
        className={cn(
          'absolute right-1 top-1/2 flex h-9 w-9 -translate-y-1/2 items-center justify-center rounded-md',
          'text-ink-muted transition-colors hover:bg-stone-100 hover:text-ink',
        )}
        aria-label={visible ? 'Ocultar senha' : 'Mostrar senha'}
        aria-controls={inputId}
        aria-pressed={visible}
        onClick={() => setVisible((v) => !v)}
      >
        <EyeIcon slash={visible} />
      </button>
    </div>
  );
});

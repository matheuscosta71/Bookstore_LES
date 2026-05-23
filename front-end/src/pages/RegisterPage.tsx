import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link, useNavigate } from 'react-router-dom';
import { useAppDispatch, useAppSelector } from '@/app/hooks';
import { registerUser, clearError } from '@/features/auth/authSlice';
import { registerSchema } from '@/utils/schemas';
import { PasswordField } from '@/components/PasswordField';
import { ROUTES } from '@/constants/routes';
import { z } from 'zod';

export function RegisterPage() {
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const err = useAppSelector((s) => s.auth.error);
  const status = useAppSelector((s) => s.auth.status);

  const { register, handleSubmit, formState: { errors } } = useForm<z.infer<typeof registerSchema>>({
    resolver: zodResolver(registerSchema),
  });

  return (
    <div>
      <h1 className="font-display text-2xl font-semibold text-ink">Criar conta</h1>
      <p className="mt-1 text-sm text-ink-muted">Senha forte: maiúscula, minúscula, número e símbolo.</p>
      <p className="mt-0.5 text-xs text-ink-muted">RF0021 — cadastro de cliente.</p>
      <form
        className="mt-6 max-h-[70vh] space-y-3 overflow-y-auto pr-1"
        onSubmit={handleSubmit(async (data) => {
          dispatch(clearError());
          await dispatch(registerUser(data)).unwrap();
          navigate(ROUTES.login, {
            replace: true,
            state: { registered: true },
          });
        })}
      >
        <div>
          <label className="text-sm font-medium">Nome completo</label>
          <input {...register('fullName')} className="mt-1 w-full rounded-lg border px-3 py-2 text-sm" />
          {errors.fullName && <p className="text-xs text-red-600">{errors.fullName.message}</p>}
        </div>
        <div>
          <label className="text-sm font-medium">E-mail</label>
          <input type="email" {...register('email')} className="mt-1 w-full rounded-lg border px-3 py-2 text-sm" />
          {errors.email && <p className="text-xs text-red-600">{errors.email.message}</p>}
        </div>
        <div>
          <label className="text-sm font-medium">CPF (11 dígitos)</label>
          <input {...register('cpf')} className="mt-1 w-full rounded-lg border px-3 py-2 text-sm" />
          {errors.cpf && <p className="text-xs text-red-600">{errors.cpf.message}</p>}
        </div>
        <div>
          <label className="text-sm font-medium">Telefone</label>
          <input {...register('phone')} className="mt-1 w-full rounded-lg border px-3 py-2 text-sm" />
          {errors.phone && <p className="text-xs text-red-600">{errors.phone.message}</p>}
        </div>
        <div>
          <label className="text-sm font-medium">Data de nascimento</label>
          <input
            type="date"
            {...register('birthDate')}
            onFocus={(e) => (e.currentTarget as HTMLInputElement & { showPicker?: () => void }).showPicker?.()}
            onClick={(e) => (e.currentTarget as HTMLInputElement & { showPicker?: () => void }).showPicker?.()}
            className="mt-1 w-full rounded-lg border px-3 py-2 text-sm"
          />
          {errors.birthDate && <p className="text-xs text-red-600">{errors.birthDate.message}</p>}
        </div>
        <label className="block">
          <span className="text-sm font-medium">Senha</span>
          <div className="mt-1">
            <PasswordField {...register('password')} autoComplete="new-password" />
          </div>
          {errors.password && <p className="text-xs text-red-600">{errors.password.message}</p>}
        </label>
        <label className="block">
          <span className="text-sm font-medium">Confirmar senha</span>
          <div className="mt-1">
            <PasswordField {...register('confirmPassword')} autoComplete="new-password" />
          </div>
          {errors.confirmPassword && <p className="text-xs text-red-600">{errors.confirmPassword.message}</p>}
        </label>
        {err && <p className="text-sm text-red-600">{err}</p>}
        <button
          type="submit"
          disabled={status === 'loading'}
          className="w-full rounded-xl bg-brand py-3 text-sm font-semibold text-white disabled:opacity-50"
        >
          {status === 'loading' ? 'Cadastrando…' : 'Cadastrar'}
        </button>
      </form>
      <p className="mt-4 text-center text-sm text-ink-muted">
        Já tem conta?{' '}
        <Link to={ROUTES.login} className="font-medium text-brand">
          Entrar
        </Link>
      </p>
    </div>
  );
}

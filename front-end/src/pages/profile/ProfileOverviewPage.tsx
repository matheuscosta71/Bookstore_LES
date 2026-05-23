import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useAppDispatch, useAppSelector } from '@/app/hooks';
import { fetchProfileBundle, updateProfile } from '@/features/customer/customerSlice';
import { loadProfile } from '@/features/auth/authSlice';
import * as customerService from '@/services/customerService';
import { passwordChangeSchema } from '@/utils/schemas';
import { getErrorMessage } from '@/services/api';
import { ConfirmModal } from '@/components/ConfirmModal';
import { MessageModal } from '@/components/MessageModal';
import { PasswordField } from '@/components/PasswordField';

const profileSchema = z.object({
  fullName: z.string().min(2),
  email: z.string().email(),
  cpf: z.string().length(11),
  phone: z.string().min(8),
  birthDate: z.string(),
});

type SimpleFeedback = { kind: 'success' } | { kind: 'error'; message: string };

export function ProfileOverviewPage() {
  const dispatch = useAppDispatch();
  const customerId = useAppSelector((s) => s.auth.customerId)!;
  const profile = useAppSelector((s) => s.customer.profile);
  const [passwordFeedback, setPasswordFeedback] = useState<SimpleFeedback | null>(null);
  const [profileFeedback, setProfileFeedback] = useState<SimpleFeedback | null>(null);
  const [saveConfirmOpen, setSaveConfirmOpen] = useState(false);
  const [pendingProfileSave, setPendingProfileSave] = useState<z.infer<typeof profileSchema> | null>(null);
  const [saveBusy, setSaveBusy] = useState(false);

  const form = useForm<z.infer<typeof profileSchema>>({
    resolver: zodResolver(profileSchema),
  });

  const pwdForm = useForm<z.infer<typeof passwordChangeSchema>>({
    resolver: zodResolver(passwordChangeSchema),
  });

  useEffect(() => {
    dispatch(fetchProfileBundle(customerId));
  }, [dispatch, customerId]);

  useEffect(() => {
    if (profile) {
      form.reset({
        fullName: profile.fullName,
        email: profile.email,
        cpf: profile.cpf,
        phone: profile.phone,
        birthDate: profile.birthDate,
      });
    }
  }, [profile, form]);

  return (
    <div>
      <h1 className="font-display text-2xl font-semibold">Dados pessoais</h1>
      <p className="mt-1 text-xs text-ink-muted">RF0022 — alteração de dados cadastrais.</p>
      <form
        className="mt-6 max-w-lg space-y-3"
        onSubmit={form.handleSubmit((data) => {
          if (!profile) return;
          setPendingProfileSave(data);
          setSaveConfirmOpen(true);
        })}
      >
        <input {...form.register('fullName')} className="w-full rounded border px-3 py-2 text-sm" placeholder="Nome" />
        <input {...form.register('email')} className="w-full rounded border px-3 py-2 text-sm" placeholder="E-mail" />
        <input {...form.register('cpf')} className="w-full rounded border px-3 py-2 text-sm" placeholder="CPF" />
        <input {...form.register('phone')} className="w-full rounded border px-3 py-2 text-sm" placeholder="Telefone" />
        <input
          type="date"
          {...form.register('birthDate')}
          onFocus={(e) => (e.currentTarget as HTMLInputElement & { showPicker?: () => void }).showPicker?.()}
          onClick={(e) => (e.currentTarget as HTMLInputElement & { showPicker?: () => void }).showPicker?.()}
          className="w-full rounded border px-3 py-2 text-sm"
        />
        <div className="flex items-center gap-2 text-sm text-ink-muted">
          <span className="font-medium text-ink">Status:</span>
          <span className={profile?.active ? 'text-green-700' : 'text-red-700'}>
            {profile?.active ? 'Conta ativa' : 'Conta inativa'}
          </span>
        </div>
        <button type="submit" className="rounded-lg bg-brand px-4 py-2 text-sm text-white">
          Salvar alterações
        </button>
      </form>

      <ConfirmModal
        open={saveConfirmOpen}
        title="Confirmar alterações"
        message="Deseja salvar as alterações nos seus dados pessoais?"
        confirmLabel={saveBusy ? 'Salvando…' : 'Salvar'}
        cancelLabel="Cancelar"
        onClose={() => {
          if (saveBusy) return;
          setSaveConfirmOpen(false);
          setPendingProfileSave(null);
        }}
        onConfirm={async () => {
          if (saveBusy || !profile || !pendingProfileSave) return;
          setSaveBusy(true);
          try {
            await dispatch(
              updateProfile({
                id: customerId,
                body: { ...pendingProfileSave, active: profile.active },
              }),
            ).unwrap();
            dispatch(loadProfile(customerId));
            setSaveConfirmOpen(false);
            setPendingProfileSave(null);
            setProfileFeedback({ kind: 'success' });
          } catch (e) {
            setSaveConfirmOpen(false);
            setPendingProfileSave(null);
            setProfileFeedback({ kind: 'error', message: getErrorMessage(e) });
          } finally {
            setSaveBusy(false);
          }
        }}
      />

      <MessageModal
        open={!!profileFeedback}
        title={
          profileFeedback?.kind === 'error'
            ? 'Não foi possível salvar'
            : profileFeedback?.kind === 'success'
              ? 'Dados atualizados'
              : ''
        }
        onClose={() => setProfileFeedback(null)}
      >
        {profileFeedback?.kind === 'success' && (
          <p className="text-green-800">Suas alterações foram salvas com sucesso.</p>
        )}
        {profileFeedback?.kind === 'error' && <p className="text-red-700">{profileFeedback.message}</p>}
      </MessageModal>

      <h2 className="mt-10 font-semibold">Alterar senha (RF0028)</h2>
      <p className="mt-1 text-xs text-ink-muted">Alteração apenas da senha, sem exigir reenvio dos demais dados.</p>
      <form
        className="mt-3 max-w-lg space-y-3"
        onSubmit={pwdForm.handleSubmit(async (data) => {
          try {
            await customerService.changePassword(customerId, data.newPassword);
            pwdForm.reset();
            setPasswordFeedback({ kind: 'success' });
          } catch (e) {
            setPasswordFeedback({ kind: 'error', message: getErrorMessage(e) });
          }
        })}
      >
        <PasswordField
          {...pwdForm.register('newPassword')}
          placeholder="Nova senha"
          autoComplete="new-password"
          className="rounded border text-sm"
        />
        <PasswordField
          {...pwdForm.register('confirm')}
          placeholder="Confirmar"
          autoComplete="new-password"
          className="rounded border text-sm"
        />
        <button type="submit" className="rounded-lg border border-stone-300 px-4 py-2 text-sm">
          Atualizar senha
        </button>
      </form>

      <MessageModal
        open={!!passwordFeedback}
        title={
          passwordFeedback?.kind === 'error'
            ? 'Não foi possível alterar a senha'
            : passwordFeedback?.kind === 'success'
              ? 'Senha alterada'
              : ''
        }
        onClose={() => setPasswordFeedback(null)}
      >
        {passwordFeedback?.kind === 'success' && (
          <p className="text-green-800">A alteração foi concluída com sucesso.</p>
        )}
        {passwordFeedback?.kind === 'error' && <p className="text-red-700">{passwordFeedback.message}</p>}
      </MessageModal>
    </div>
  );
}

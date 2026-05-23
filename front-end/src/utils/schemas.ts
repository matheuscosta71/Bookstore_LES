import { z } from 'zod';

export const loginSchema = z.object({
  email: z.string().email('E-mail inválido'),
  password: z.string().min(1, 'Informe a senha'),
});

const strongPassword = z
  .string()
  .min(8, 'Mínimo 8 caracteres')
  .regex(/[A-Z]/, 'Inclua uma letra maiúscula')
  .regex(/[a-z]/, 'Inclua uma letra minúscula')
  .regex(/[0-9]/, 'Inclua um número')
  .regex(/[^A-Za-z0-9]/, 'Inclua um caractere especial');

export const registerSchema = z
  .object({
    fullName: z.string().min(2, 'Nome obrigatório'),
    email: z.string().email('E-mail inválido'),
    cpf: z.string().regex(/^\d{11}$/, 'CPF com 11 dígitos'),
    phone: z.string().min(8, 'Telefone obrigatório'),
    birthDate: z.string().min(1, 'Data obrigatória'),
    password: strongPassword,
    confirmPassword: z.string(),
  })
  .refine((d) => d.password === d.confirmPassword, {
    message: 'Senhas não conferem',
    path: ['confirmPassword'],
  });

export const addressSchema = z.object({
  nickname: z.string().min(1, 'Apelido obrigatório'),
  street: z.string().min(1, 'Rua obrigatória'),
  number: z.string().min(1, 'Número obrigatório'),
  complement: z.string().optional(),
  neighborhood: z.string().min(1, 'Bairro obrigatório'),
  city: z.string().min(1, 'Cidade obrigatória'),
  state: z
    .string()
    .transform((s) => s.trim().toUpperCase())
    .pipe(z.string().length(2, 'Use a sigla do estado com 2 letras (ex: MG), não o nome completo')),
  zipCode: z
    .string()
    .transform((s) => s.replace(/\D/g, ''))
    .pipe(z.string().length(8, 'CEP deve ter 8 dígitos')),
  type: z.enum(['DELIVERY', 'BILLING']),
});

export const cardSchema = z.object({
  cardholderName: z.string().min(2, 'Nome no cartão'),
  /** Aceita dígitos com ou sem espaços; valida 13–19 dígitos. */
  cardNumber: z
    .string()
    .transform((s) => s.replace(/\D/g, ''))
    .pipe(
      z
        .string()
        .min(13, 'Número do cartão deve ter entre 13 e 19 dígitos')
        .max(19, 'Número do cartão deve ter entre 13 e 19 dígitos'),
    ),
  brand: z.string().min(2, 'Bandeira'),
  expirationMonth: z.coerce.number().min(1, 'Mês entre 1 e 12').max(12, 'Mês entre 1 e 12'),
  expirationYear: z.coerce.number().refine(
    (val) => Number.isFinite(val) && val >= new Date().getFullYear() && val <= 2100,
    {
      message:
        'Ano de validade deve ser o ano em curso ou um ano futuro (cartão vencido ou ano inválido)',
    },
  ),
  preferred: z.boolean().optional(),
});

export const passwordChangeSchema = z
  .object({
    newPassword: strongPassword,
    confirm: z.string(),
  })
  .refine((d) => d.newPassword === d.confirm, {
    message: 'Senhas não conferem',
    path: ['confirm'],
  });

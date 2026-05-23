import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';

const filterSchema = z.object({
  title: z.string(),
  author: z.string(),
  category: z.string(),
  isbn: z.string(),
  sort: z.string(),
});

export type FilterForm = z.infer<typeof filterSchema>;

type Props = {
  defaultValues: FilterForm;
  onApply: (v: FilterForm) => void;
  onClear: () => void;
};

export function FilterPanel({ defaultValues, onApply, onClear }: Props) {
  const { register, handleSubmit, reset } = useForm<FilterForm>({
    resolver: zodResolver(filterSchema),
    defaultValues,
  });

  return (
    <form
      onSubmit={handleSubmit(onApply)}
      className="space-y-3 rounded-xl border border-stone-200 bg-white p-4 shadow-card"
    >
      <h3 className="font-semibold text-ink">Filtros</h3>
      <div>
        <label className="text-xs text-ink-muted">Título</label>
        <input {...register('title')} className="mt-0.5 w-full rounded border border-stone-200 px-2 py-1.5 text-sm" />
      </div>
      <div>
        <label className="text-xs text-ink-muted">Autor</label>
        <input {...register('author')} className="mt-0.5 w-full rounded border border-stone-200 px-2 py-1.5 text-sm" />
      </div>
      <div>
        <label className="text-xs text-ink-muted">Categoria</label>
        <input {...register('category')} className="mt-0.5 w-full rounded border border-stone-200 px-2 py-1.5 text-sm" />
      </div>
      <div>
        <label className="text-xs text-ink-muted">ISBN</label>
        <input {...register('isbn')} className="mt-0.5 w-full rounded border border-stone-200 px-2 py-1.5 text-sm" />
      </div>
      <div>
        <label className="text-xs text-ink-muted">Ordenar</label>
        <select {...register('sort')} className="mt-0.5 w-full rounded border border-stone-200 px-2 py-1.5 text-sm">
          <option value="title,asc">Título A–Z</option>
          <option value="title,desc">Título Z–A</option>
        </select>
      </div>
      <div className="flex gap-2 pt-2">
        <button type="submit" className="flex-1 rounded-lg bg-brand py-2 text-sm font-medium text-white">
          Aplicar
        </button>
        <button
          type="button"
          onClick={() => {
            reset({ title: '', author: '', category: '', isbn: '', sort: 'title,asc' });
            onClear();
          }}
          className="rounded-lg border border-stone-300 px-3 py-2 text-sm"
        >
          Limpar
        </button>
      </div>
    </form>
  );
}

import { useCallback, useEffect, useState, type Dispatch, type SetStateAction } from 'react';
import * as adminBookService from '@/services/adminBookService';
import type { BookLifecycleReasonCode } from '@/services/adminBookService';
import {
  createAuthor,
  createPublisher,
  createSupplier,
  fetchAuthors,
  fetchCategories,
  fetchPricingGroups,
  fetchPublishers,
  fetchSuppliers,
  mergeIdNameSorted,
  type IdName,
} from '@/services/domainCatalogService';
import { fetchBookById, fetchBooks } from '@/services/booksService';
import { getErrorMessage } from '@/services/api';
import type { Book } from '@/types/api';
import { AdminBookCatalogPicker } from '@/components/AdminBookCatalogPicker';
import { ConfirmModal } from '@/components/ConfirmModal';
import { DomainQuickCreateModal } from '@/components/DomainQuickCreateModal';
import { Pagination } from '@/components/Pagination';
import { formatBRL } from '@/utils/format';
import { validateAdminBookExtended, type AdminBookExtendedInput } from '@/validators/bookValidator';
import { normalizeIsbnInput } from '@/validators/isbn';
import { cn } from '@/utils/cn';
import { appLogger } from '@/utils/appLogger';

const INACTIVATION_REASONS: BookLifecycleReasonCode[] = [
  'BAIXA_ROTACAO',
  'CONTEUDO_DESATUALIZADO',
  'OUTRA_INATIVACAO',
];

const ACTIVATION_REASONS: BookLifecycleReasonCode[] = [
  'RETORNO_ESTOQUE',
  'DEMANDA_RENOVADA',
  'OUTRA_ATIVACAO',
];

function emptyBookForm(): AdminBookExtendedInput {
  return {
    title: '',
    authorId: '',
    publisherId: '',
    supplierId: '',
    categoryIds: [],
    publicationYear: new Date().getFullYear(),
    edition: '1ª',
    pageCount: 1,
    synopsis: '',
    heightCm: 1,
    widthCm: 1,
    depthCm: 1,
    weightKg: 0.1,
    barcode: '',
    price: 0,
    costPrice: '',
    pricingGroupId: '',
    isbn: '',
    maxSaleValue: '',
    stockQuantity: 0,
    active: true,
  };
}

function bookToForm(b: Book): AdminBookExtendedInput {
  return {
    title: b.title,
    authorId: b.authorId ?? '',
    publisherId: b.publisherId ?? '',
    supplierId: b.supplierId ?? '',
    categoryIds: (b.categoryIds ?? []).map(String),
    publicationYear: b.publicationYear ?? new Date().getFullYear(),
    edition: b.edition ?? '1ª',
    pageCount: b.pageCount ?? 1,
    synopsis: b.synopsis ?? '',
    heightCm: Number(b.heightCm ?? 1),
    widthCm: Number(b.widthCm ?? 1),
    depthCm: Number(b.depthCm ?? 1),
    weightKg: Number(b.weightKg ?? 0.1),
    barcode: b.barcode ?? '',
    price: Number(b.price),
    costPrice: b.costPrice != null ? Number(b.costPrice) : '',
    pricingGroupId: b.pricingGroupId ?? '',
    isbn: b.isbn,
    maxSaleValue: b.maxSaleValue != null ? Number(b.maxSaleValue) : '',
    stockQuantity: b.stockQuantity,
    active: b.active,
  };
}

function matchAuthorId(authors: IdName[], name: string): string | undefined {
  const n = name.trim().toLowerCase();
  if (!n) return undefined;
  const exact = authors.find((a) => a.name.toLowerCase() === n);
  if (exact) return exact.id;
  return authors.find((a) => n.includes(a.name.toLowerCase()) || a.name.toLowerCase().includes(n))?.id;
}

function matchPublisherId(publishers: IdName[], name: string): string | undefined {
  const n = name.trim().toLowerCase();
  if (!n) return undefined;
  const exact = publishers.find((p) => p.name.toLowerCase() === n);
  if (exact) return exact.id;
  return publishers.find(
    (p) => n.includes(p.name.toLowerCase()) || p.name.toLowerCase().includes(n),
  )?.id;
}

function matchCategoryIds(categories: IdName[], name: string): string[] | undefined {
  const n = name.trim().toLowerCase();
  if (!n) return undefined;
  const exact = categories.find((c) => c.name.toLowerCase() === n);
  if (exact) return [exact.id];
  const partial = categories.find(
    (c) => n.includes(c.name.toLowerCase()) || c.name.toLowerCase().includes(n),
  );
  return partial ? [partial.id] : undefined;
}

function toCreateBody(form: AdminBookExtendedInput): adminBookService.BookCreateBody {
  return {
    title: form.title.trim(),
    authorId: form.authorId,
    publisherId: form.publisherId,
    supplierId: form.supplierId,
    categoryIds: form.categoryIds,
    publicationYear: form.publicationYear,
    edition: form.edition.trim(),
    pageCount: form.pageCount,
    synopsis: form.synopsis.trim(),
    heightCm: form.heightCm,
    widthCm: form.widthCm,
    depthCm: form.depthCm,
    weightKg: form.weightKg,
    barcode: form.barcode.trim(),
    price: form.price,
    costPrice: form.costPrice === '' ? undefined : Number(form.costPrice),
    pricingGroupId: form.pricingGroupId,
    isbn: normalizeIsbnInput(form.isbn),
    maxSaleValue: form.maxSaleValue === '' ? undefined : Number(form.maxSaleValue),
    stockQuantity: form.stockQuantity,
    active: form.active,
  };
}

type BookFormFieldsProps = {
  form: AdminBookExtendedInput;
  setForm: Dispatch<SetStateAction<AdminBookExtendedInput>>;
  errors: Record<string, string>;
  authors: IdName[];
  publishers: IdName[];
  suppliers: IdName[];
  categories: IdName[];
  pricingGroups: IdName[];
  domainLoading: boolean;
  showSalesManagerKey?: boolean;
  salesManagerKey: string;
  onSalesManagerKeyChange: (v: string) => void;
  salesManagerHint?: string;
  onAuthorCreated: (item: IdName) => void;
  onPublisherCreated: (item: IdName) => void;
  onSupplierCreated: (item: IdName) => void;
};

function BookFormFields({
  form,
  setForm,
  errors,
  authors,
  publishers,
  suppliers,
  categories,
  pricingGroups,
  domainLoading,
  showSalesManagerKey = true,
  salesManagerKey,
  onSalesManagerKeyChange,
  salesManagerHint,
  onAuthorCreated,
  onPublisherCreated,
  onSupplierCreated,
}: BookFormFieldsProps) {
  const [quickKind, setQuickKind] = useState<'author' | 'publisher' | 'supplier' | null>(null);
  const sel = 'mt-1 w-full rounded border px-3 py-2 text-sm';
  return (
    <div className="grid gap-3 sm:grid-cols-2">
      <label className="text-sm sm:col-span-2">
        Título *
        <input
          value={form.title}
          onChange={(e) => setForm((s) => ({ ...s, title: e.target.value }))}
          className={cn(sel, errors.title && 'border-red-500')}
        />
        {errors.title && <p className="mt-0.5 text-xs text-red-600">{errors.title}</p>}
      </label>

      <div className="text-sm">
        <span className="font-medium text-ink">Autor (domínio) *</span>
        <div className="mt-1 flex gap-2">
          <select
            value={form.authorId}
            onChange={(e) => setForm((s) => ({ ...s, authorId: e.target.value }))}
            disabled={domainLoading}
            className={cn(sel, 'min-w-0 flex-1', errors.authorId && 'border-red-500')}
          >
            <option value="">— selecione —</option>
            {authors.map((a) => (
              <option key={a.id} value={a.id}>
                {a.name}
              </option>
            ))}
          </select>
          <button
            type="button"
            disabled={domainLoading}
            onClick={() => setQuickKind('author')}
            className="shrink-0 rounded-lg border border-stone-300 bg-white px-2 py-2 text-xs font-medium text-brand hover:bg-stone-50 disabled:opacity-50"
          >
            + Novo autor
          </button>
        </div>
        {errors.authorId && <p className="mt-0.5 text-xs text-red-600">{errors.authorId}</p>}
      </div>

      <div className="text-sm">
        <span className="font-medium text-ink">Editora *</span>
        <div className="mt-1 flex gap-2">
          <select
            value={form.publisherId}
            onChange={(e) => setForm((s) => ({ ...s, publisherId: e.target.value }))}
            disabled={domainLoading}
            className={cn(sel, 'min-w-0 flex-1', errors.publisherId && 'border-red-500')}
          >
            <option value="">— selecione —</option>
            {publishers.map((p) => (
              <option key={p.id} value={p.id}>
                {p.name}
              </option>
            ))}
          </select>
          <button
            type="button"
            disabled={domainLoading}
            onClick={() => setQuickKind('publisher')}
            className="shrink-0 rounded-lg border border-stone-300 bg-white px-2 py-2 text-xs font-medium text-brand hover:bg-stone-50 disabled:opacity-50"
          >
            + Nova editora
          </button>
        </div>
        {errors.publisherId && <p className="mt-0.5 text-xs text-red-600">{errors.publisherId}</p>}
      </div>

      <div className="text-sm">
        <span className="font-medium text-ink">Fornecedor *</span>
        <div className="mt-1 flex gap-2">
          <select
            value={form.supplierId}
            onChange={(e) => setForm((s) => ({ ...s, supplierId: e.target.value }))}
            disabled={domainLoading}
            className={cn(sel, 'min-w-0 flex-1', errors.supplierId && 'border-red-500')}
          >
            <option value="">— selecione —</option>
            {suppliers.map((s) => (
              <option key={s.id} value={s.id}>
                {s.name}
              </option>
            ))}
          </select>
          <button
            type="button"
            disabled={domainLoading}
            onClick={() => setQuickKind('supplier')}
            className="shrink-0 rounded-lg border border-stone-300 bg-white px-2 py-2 text-xs font-medium text-brand hover:bg-stone-50 disabled:opacity-50"
          >
            + Novo fornecedor
          </button>
        </div>
        {errors.supplierId && <p className="mt-0.5 text-xs text-red-600">{errors.supplierId}</p>}
      </div>

      <label className="text-sm">
        Grupo de precificação *
        <select
          value={form.pricingGroupId}
          onChange={(e) => setForm((s) => ({ ...s, pricingGroupId: e.target.value }))}
          disabled={domainLoading}
          className={cn(sel, errors.pricingGroupId && 'border-red-500')}
        >
          <option value="">— selecione —</option>
          {pricingGroups.map((g) => (
            <option key={g.id} value={g.id}>
              {g.name}
            </option>
          ))}
        </select>
        {errors.pricingGroupId && <p className="mt-0.5 text-xs text-red-600">{errors.pricingGroupId}</p>}
      </label>

      <div className="text-sm sm:col-span-2">
        <p className="font-medium">Categorias * (uma ou mais)</p>
        <div className="mt-1 max-h-40 overflow-y-auto rounded border bg-stone-50/50 p-2">
          {categories.map((c) => (
            <label key={c.id} className="flex cursor-pointer items-center gap-2 py-1">
              <input
                type="checkbox"
                checked={form.categoryIds.includes(c.id)}
                onChange={(e) => {
                  const on = e.target.checked;
                  setForm((s) => ({
                    ...s,
                    categoryIds: on
                      ? [...s.categoryIds, c.id]
                      : s.categoryIds.filter((id) => id !== c.id),
                  }));
                }}
              />
              <span>{c.name}</span>
            </label>
          ))}
        </div>
        {errors.categoryIds && <p className="mt-0.5 text-xs text-red-600">{errors.categoryIds}</p>}
      </div>

      <label className="text-sm">
        Ano de publicação *
        <input
          type="number"
          value={form.publicationYear}
          onChange={(e) => setForm((s) => ({ ...s, publicationYear: Number(e.target.value) }))}
          className={cn(sel, errors.publicationYear && 'border-red-500')}
        />
        {errors.publicationYear && <p className="mt-0.5 text-xs text-red-600">{errors.publicationYear}</p>}
      </label>

      <label className="text-sm">
        Edição *
        <input
          value={form.edition}
          onChange={(e) => setForm((s) => ({ ...s, edition: e.target.value }))}
          className={cn(sel, errors.edition && 'border-red-500')}
        />
        {errors.edition && <p className="mt-0.5 text-xs text-red-600">{errors.edition}</p>}
      </label>

      <label className="text-sm">
        Páginas *
        <input
          type="number"
          min={1}
          value={form.pageCount}
          onChange={(e) => setForm((s) => ({ ...s, pageCount: Number(e.target.value) }))}
          className={cn(sel, errors.pageCount && 'border-red-500')}
        />
        {errors.pageCount && <p className="mt-0.5 text-xs text-red-600">{errors.pageCount}</p>}
      </label>

      <label className="text-sm">
        ISBN *
        <input
          value={form.isbn}
          onChange={(e) => setForm((s) => ({ ...s, isbn: e.target.value }))}
          className={cn(sel, errors.isbn && 'border-red-500')}
        />
        {errors.isbn && <p className="mt-0.5 text-xs text-red-600">{errors.isbn}</p>}
      </label>

      <label className="text-sm">
        Código de barras *
        <input
          value={form.barcode}
          onChange={(e) => setForm((s) => ({ ...s, barcode: e.target.value }))}
          className={cn(sel, errors.barcode && 'border-red-500')}
        />
        {errors.barcode && <p className="mt-0.5 text-xs text-red-600">{errors.barcode}</p>}
      </label>

      <label className="text-sm sm:col-span-2">
        Sinopse *
        <textarea
          rows={3}
          value={form.synopsis}
          onChange={(e) => setForm((s) => ({ ...s, synopsis: e.target.value }))}
          className={cn(sel, errors.synopsis && 'border-red-500')}
        />
        {errors.synopsis && <p className="mt-0.5 text-xs text-red-600">{errors.synopsis}</p>}
      </label>

      <p className="text-sm font-medium sm:col-span-2">Dimensões e peso</p>
      <label className="text-sm">
        Altura (cm) *
        <input
          type="number"
          step="0.001"
          min="0.001"
          value={form.heightCm}
          onChange={(e) => setForm((s) => ({ ...s, heightCm: Number(e.target.value) }))}
          className={cn(sel, errors.heightCm && 'border-red-500')}
        />
        {errors.heightCm && <p className="mt-0.5 text-xs text-red-600">{errors.heightCm}</p>}
      </label>
      <label className="text-sm">
        Largura (cm) *
        <input
          type="number"
          step="0.001"
          min="0.001"
          value={form.widthCm}
          onChange={(e) => setForm((s) => ({ ...s, widthCm: Number(e.target.value) }))}
          className={cn(sel, errors.widthCm && 'border-red-500')}
        />
        {errors.widthCm && <p className="mt-0.5 text-xs text-red-600">{errors.widthCm}</p>}
      </label>
      <label className="text-sm">
        Profundidade (cm) *
        <input
          type="number"
          step="0.001"
          min="0.001"
          value={form.depthCm}
          onChange={(e) => setForm((s) => ({ ...s, depthCm: Number(e.target.value) }))}
          className={cn(sel, errors.depthCm && 'border-red-500')}
        />
        {errors.depthCm && <p className="mt-0.5 text-xs text-red-600">{errors.depthCm}</p>}
      </label>
      <label className="text-sm">
        Peso (kg) *
        <input
          type="number"
          step="0.001"
          min="0.001"
          value={form.weightKg}
          onChange={(e) => setForm((s) => ({ ...s, weightKg: Number(e.target.value) }))}
          className={cn(sel, errors.weightKg && 'border-red-500')}
        />
        {errors.weightKg && <p className="mt-0.5 text-xs text-red-600">{errors.weightKg}</p>}
      </label>

      <label className="text-sm">
        Preço de venda *
        <input
          type="number"
          step="0.01"
          value={form.price}
          onChange={(e) => setForm((s) => ({ ...s, price: Number(e.target.value) }))}
          className={cn(sel, errors.price && 'border-red-500')}
        />
        {errors.price && <p className="mt-0.5 text-xs text-red-600">{errors.price}</p>}
      </label>

      <label className="text-sm">
        Custo (opcional)
        <input
          type="number"
          step="0.01"
          value={form.costPrice}
          onChange={(e) =>
            setForm((s) => ({
              ...s,
              costPrice: e.target.value === '' ? '' : Number(e.target.value),
            }))
          }
          className={cn(sel, errors.costPrice && 'border-red-500')}
        />
        {errors.costPrice && <p className="mt-0.5 text-xs text-red-600">{errors.costPrice}</p>}
      </label>

      <label className="text-sm">
        Estoque *
        <input
          type="number"
          min={0}
          step={1}
          value={form.stockQuantity}
          onChange={(e) => setForm((s) => ({ ...s, stockQuantity: Number(e.target.value) }))}
          className={cn(sel, errors.stockQuantity && 'border-red-500')}
        />
        {errors.stockQuantity && <p className="mt-0.5 text-xs text-red-600">{errors.stockQuantity}</p>}
      </label>

      <label className="text-sm">
        Teto de preço (opcional)
        <input
          type="number"
          step="0.01"
          value={form.maxSaleValue}
          onChange={(e) =>
            setForm((s) => ({
              ...s,
              maxSaleValue: e.target.value === '' ? '' : Number(e.target.value),
            }))
          }
          className={cn(sel, errors.maxSaleValue && 'border-red-500')}
        />
        {errors.maxSaleValue && <p className="mt-0.5 text-xs text-red-600">{errors.maxSaleValue}</p>}
      </label>

      <label className="flex items-center gap-2 text-sm sm:col-span-2">
        <input
          type="checkbox"
          checked={form.active}
          onChange={(e) => setForm((s) => ({ ...s, active: e.target.checked }))}
        />
        Ativo na criação
      </label>

      {showSalesManagerKey && (
        <label className="text-sm sm:col-span-2">
          Chave de gerente de vendas (opcional — RN0014 se preço abaixo da margem mínima)
          <input
            value={salesManagerKey}
            onChange={(e) => onSalesManagerKeyChange(e.target.value)}
            className={cn(sel, 'font-mono')}
            placeholder="X-Sales-Manager-Key"
            autoComplete="off"
          />
          {salesManagerHint && <p className="mt-0.5 text-xs text-ink-muted">{salesManagerHint}</p>}
        </label>
      )}

      <DomainQuickCreateModal
        open={quickKind !== null}
        title={
          quickKind === 'author'
            ? 'Novo autor'
            : quickKind === 'publisher'
              ? 'Nova editora'
              : quickKind === 'supplier'
                ? 'Novo fornecedor'
                : ''
        }
        onClose={() => setQuickKind(null)}
        onSubmit={async (name) => {
          if (quickKind === 'author') {
            onAuthorCreated(await createAuthor(name));
          } else if (quickKind === 'publisher') {
            onPublisherCreated(await createPublisher(name));
          } else if (quickKind === 'supplier') {
            onSupplierCreated(await createSupplier(name));
          }
        }}
      />
    </div>
  );
}

type LifecycleDraft = {
  book: Book;
  nextActive: boolean;
  reason: BookLifecycleReasonCode | '';
  justification: string;
};

export function AdminBooksPage() {
  const [listPage, setListPage] = useState(0);
  const [books, setBooks] = useState<Book[]>([]);
  const [totalPages, setTotalPages] = useState(0);
  const [create, setCreate] = useState(emptyBookForm);
  const [editId, setEditId] = useState<string | null>(null);
  const [edit, setEdit] = useState(emptyBookForm);
  const [editSalesKey, setEditSalesKey] = useState('');
  const [authors, setAuthors] = useState<IdName[]>([]);
  const [publishers, setPublishers] = useState<IdName[]>([]);
  const [suppliers, setSuppliers] = useState<IdName[]>([]);
  const [categories, setCategories] = useState<IdName[]>([]);
  const [pricingGroups, setPricingGroups] = useState<IdName[]>([]);
  const [domainLoading, setDomainLoading] = useState(true);
  const [minSalesInput, setMinSalesInput] = useState('0.00');
  const [recalcId, setRecalcId] = useState('');
  const [err, setErr] = useState<string | null>(null);
  const [msg, setMsg] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [createErrors, setCreateErrors] = useState<Record<string, string>>({});
  const [editErrors, setEditErrors] = useState<Record<string, string>>({});
  const [lifecycleDraft, setLifecycleDraft] = useState<LifecycleDraft | null>(null);
  const [deleteBookId, setDeleteBookId] = useState<string | null>(null);
  const [listFilters, setListFilters] = useState({
    title: '',
    author: '',
    category: '',
    isbn: '',
    code: '',
  });

  useEffect(() => {
    let cancelled = false;
    async function loadDomain() {
      setErr(null);
      try {
        const [a, p, s, c, pr] = await Promise.all([
          fetchAuthors(),
          fetchPublishers(),
          fetchSuppliers(),
          fetchCategories(),
          fetchPricingGroups(),
        ]);
        if (!cancelled) {
          setAuthors(a);
          setPublishers(p);
          setSuppliers(s);
          setCategories(c);
          setPricingGroups(pr);
        }
      } catch (e) {
        if (!cancelled) setErr(getErrorMessage(e));
      } finally {
        if (!cancelled) setDomainLoading(false);
      }
    }
    void loadDomain();
    return () => {
      cancelled = true;
    };
  }, []);

  const loadList = useCallback(async (p: number) => {
    setErr(null);
    setLoading(true);
    try {
      const res = await fetchBooks({
        page: p,
        size: 10,
        sort: 'title,asc',
        includeInactive: true,
        title: listFilters.title.trim() || undefined,
        author: listFilters.author.trim() || undefined,
        category: listFilters.category.trim() || undefined,
        isbn: listFilters.isbn.replace(/[-\s]/g, '') || undefined,
        code: listFilters.code.trim() || undefined,
      });
      setBooks(res.content);
      setTotalPages(res.totalPages);
      setListPage(res.number);
    } catch (e) {
      setErr(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }, [listFilters]);

  useEffect(() => {
    void loadList(0);
  }, []);

  async function startEdit(id: string) {
    setErr(null);
    setLoading(true);
    try {
      const b = await fetchBookById(id, { includeInactive: true });
      setEditId(id);
      setEditErrors({});
      setEditSalesKey('');
      setEdit(bookToForm(b));
    } catch (e) {
      setErr(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  async function submitCreate() {
    setErr(null);
    setMsg(null);
    const v = validateAdminBookExtended(create);
    if (!v.valid) {
      appLogger.warn('AdminBooksPage', 'submitCreate', 'Validação do formulário falhou', {
        fields: Object.keys(v.errors),
      });
      setCreateErrors(v.errors);
      return;
    }
    setCreateErrors({});
    setLoading(true);
    try {
      await adminBookService.createBook(toCreateBody(create));
      setCreate(emptyBookForm());
      setMsg('Livro criado.');
      await loadList(listPage);
    } catch (e) {
      setErr(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  async function submitEdit() {
    if (!editId) return;
    setErr(null);
    setMsg(null);
    const v = validateAdminBookExtended(edit);
    if (!v.valid) {
      setEditErrors(v.errors);
      return;
    }
    setEditErrors({});
    setLoading(true);
    try {
      const body: adminBookService.BookUpdateBody = {
        ...toCreateBody(edit),
        active: edit.active,
      };
      await adminBookService.updateBook(editId, body, editSalesKey.trim() || undefined);
      setEditId(null);
      setEditSalesKey('');
      setMsg('Livro atualizado.');
      await loadList(listPage);
    } catch (e) {
      setErr(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  async function remove(id: string) {
    setErr(null);
    setLoading(true);
    try {
      await adminBookService.deleteBook(id);
      setMsg('Livro removido.');
      await loadList(listPage);
    } catch (e) {
      setErr(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  function openLifecycle(b: Book) {
    setLifecycleDraft({
      book: b,
      nextActive: !b.active,
      reason: '',
      justification: '',
    });
  }

  async function confirmLifecycle() {
    if (!lifecycleDraft) return;
    const { book, nextActive, reason, justification } = lifecycleDraft;
    if (!justification.trim() || !reason) {
      setErr('Informe motivo e justificativa.');
      return;
    }
    setErr(null);
    setLoading(true);
    try {
      await adminBookService.patchBookActive(book.id, {
        active: nextActive,
        justification: justification.trim(),
        reason,
      });
      setLifecycleDraft(null);
      setMsg(`Livro ${nextActive ? 'ativado' : 'inativado'}.`);
      await loadList(listPage);
    } catch (e) {
      setErr(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  async function runInactivate() {
    const normalized = minSalesInput.replace(',', '.').trim();
    const minimumSalesValue = Number(normalized);
    if (!Number.isFinite(minimumSalesValue) || minimumSalesValue < 0) {
      setErr('Informe um valor mínimo de vendas válido (ex.: 50.00).');
      return;
    }
    setErr(null);
    setMsg(null);
    setLoading(true);
    try {
      await adminBookService.postInactivateAutomatic(minimumSalesValue);
      setMsg('Inativação automática executada.');
      await loadList(listPage);
    } catch (e) {
      setErr(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  async function runRecalc() {
    const id = recalcId.trim();
    if (!id) return;
    setErr(null);
    setMsg(null);
    setLoading(true);
    try {
      await adminBookService.recalculateSalePrice(id);
      setMsg('Preço recalculado (requer X-Admin-Key).');
      await loadList(listPage);
    } catch (e) {
      setErr(getErrorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  const reasonOptions = lifecycleDraft?.nextActive ? ACTIVATION_REASONS : INACTIVATION_REASONS;
  const bookPendingDelete = deleteBookId ? books.find((b) => b.id === deleteBookId) : undefined;

  return (
    <div className="space-y-10">
      <div>
        <h1 className="font-display text-3xl font-semibold">Livros (gestão)</h1>
        <p className="mt-1 text-sm text-ink-muted">
          Cadastro com domínio (autor, editora, fornecedor, categorias, grupo de precificação), regras de margem
          (chave de gerente) e motivo/justificativa ao ativar ou inativar manualmente.
        </p>
      </div>

      {err && <p className="text-sm text-red-600">{err}</p>}
      {msg && <p className="text-sm text-green-700">{msg}</p>}
      {loading && <p className="text-sm text-ink-muted">Processando…</p>}
      {domainLoading && <p className="text-sm text-ink-muted">Carregando listas de domínio…</p>}

      {lifecycleDraft && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4"
          role="dialog"
          aria-modal="true"
          aria-labelledby="lifecycle-title"
        >
          <div className="w-full max-w-md rounded-xl border bg-white p-4 shadow-lg">
            <h3 id="lifecycle-title" className="font-semibold">
              {lifecycleDraft.nextActive ? 'Ativar' : 'Inativar'} livro
            </h3>
            <p className="mt-1 text-sm text-ink-muted">{lifecycleDraft.book.title}</p>
            <label className="mt-3 block text-sm">
              Motivo *
              <select
                value={lifecycleDraft.reason}
                onChange={(e) =>
                  setLifecycleDraft((d) =>
                    d ? { ...d, reason: e.target.value as BookLifecycleReasonCode | '' } : d,
                  )
                }
                className="mt-1 w-full rounded border px-3 py-2 text-sm"
              >
                <option value="">— selecione —</option>
                {reasonOptions.map((r) => (
                  <option key={r} value={r}>
                    {r}
                  </option>
                ))}
              </select>
            </label>
            <label className="mt-3 block text-sm">
              Justificativa *
              <textarea
                value={lifecycleDraft.justification}
                onChange={(e) =>
                  setLifecycleDraft((d) => (d ? { ...d, justification: e.target.value } : d))
                }
                rows={3}
                className="mt-1 w-full rounded border px-3 py-2 text-sm"
              />
            </label>
            <div className="mt-4 flex flex-wrap gap-2">
              <button
                type="button"
                onClick={() => void confirmLifecycle()}
                className="rounded-lg bg-brand px-4 py-2 text-sm text-white"
              >
                Confirmar
              </button>
              <button
                type="button"
                onClick={() => setLifecycleDraft(null)}
                className="rounded-lg border px-4 py-2 text-sm"
              >
                Cancelar
              </button>
            </div>
          </div>
        </div>
      )}

      <ConfirmModal
        open={!!deleteBookId}
        title="Remover livro"
        message={
          bookPendingDelete
            ? `Tem certeza de que deseja remover “${bookPendingDelete.title}”? Esta ação não pode ser desfeita.`
            : 'Tem certeza de que deseja remover este livro? Esta ação não pode ser desfeita.'
        }
        variant="danger"
        confirmLabel="Remover"
        onClose={() => setDeleteBookId(null)}
        onConfirm={() => {
          const id = deleteBookId;
          setDeleteBookId(null);
          if (id) void remove(id);
        }}
      />

      <section className="rounded-xl border bg-white p-4 shadow-card">
        <h2 className="font-semibold">Inativação automática (RF0013)</h2>
        <div className="mt-3 flex flex-wrap items-end gap-2">
          <label className="text-sm">
            Valor mínimo de vendas
            <input
              type="number"
              step="0.01"
              value={minSalesInput}
              onChange={(e) => setMinSalesInput(e.target.value)}
              className="mt-1 block rounded border px-3 py-2 text-sm"
            />
          </label>
          <button
            type="button"
            onClick={() => void runInactivate()}
            className="rounded-lg border px-4 py-2 text-sm"
          >
            Executar
          </button>
        </div>
      </section>

      <section className="rounded-xl border bg-white p-4 shadow-card">
        <h2 className="font-semibold">Recalcular preço de venda</h2>
        <div className="mt-3 flex flex-wrap gap-2">
          <input
            value={recalcId}
            onChange={(e) => setRecalcId(e.target.value)}
            className="min-w-[260px] rounded border px-3 py-2 font-mono text-sm"
            placeholder="UUID do livro"
          />
          <button
            type="button"
            onClick={() => void runRecalc()}
            className="rounded-lg bg-brand px-4 py-2 text-sm text-white"
          >
            Recalcular
          </button>
        </div>
      </section>

      <section className="rounded-xl border bg-white p-4 shadow-card">
        <h2 className="font-semibold">Novo livro</h2>
        <div className="mt-4 grid gap-6 lg:grid-cols-[1fr_minmax(280px,360px)] lg:items-start">
          <div>
            <BookFormFields
              form={create}
              setForm={setCreate}
              errors={createErrors}
              authors={authors}
              publishers={publishers}
              suppliers={suppliers}
              categories={categories}
              pricingGroups={pricingGroups}
              domainLoading={domainLoading}
              showSalesManagerKey={false}
              salesManagerKey=""
              onSalesManagerKeyChange={() => {}}
              onAuthorCreated={(item) => {
                setAuthors((p) => mergeIdNameSorted(p, item));
                setCreate((s) => ({ ...s, authorId: item.id }));
              }}
              onPublisherCreated={(item) => {
                setPublishers((p) => mergeIdNameSorted(p, item));
                setCreate((s) => ({ ...s, publisherId: item.id }));
              }}
              onSupplierCreated={(item) => {
                setSuppliers((p) => mergeIdNameSorted(p, item));
                setCreate((s) => ({ ...s, supplierId: item.id }));
              }}
            />
            <button
              type="button"
              onClick={() => void submitCreate()}
              disabled={domainLoading}
              className="mt-4 rounded-lg bg-brand px-4 py-2 text-sm text-white disabled:opacity-50"
            >
              Criar
            </button>
          </div>
          <AdminBookCatalogPicker
            disabled={loading || domainLoading}
            formLabel="formulário de novo livro"
            onApply={(d) => {
              setMsg(null);
              const catIds = matchCategoryIds(categories, d.category ?? '');
              setCreate((s) => ({
                ...s,
                title: d.title,
                isbn: d.isbn,
                synopsis: d.synopsis?.trim()
                  ? d.synopsis.trim()
                  : s.synopsis.trim()
                    ? s.synopsis
                    : d.title,
                authorId: matchAuthorId(authors, d.author) ?? s.authorId,
                publisherId: matchPublisherId(publishers, d.publisher ?? '') ?? s.publisherId,
                categoryIds: catIds ?? s.categoryIds,
                pageCount:
                  d.pageCount != null && d.pageCount > 0 ? d.pageCount : s.pageCount,
                publicationYear:
                  d.publicationYear != null && d.publicationYear > 0
                    ? d.publicationYear
                    : s.publicationYear,
                barcode: s.barcode.trim() ? s.barcode : (d.barcode?.trim() || s.barcode),
              }));
              setMsg(
                'Dados do catálogo aplicados (sinopse/páginas/ano quando existirem; editora se houver correspondência; código de barras = ISBN-13 se o campo estiver vazio). Revise dimensões e domínio.',
              );
            }}
          />
        </div>
      </section>

      {editId && (
        <section className="rounded-xl border bg-amber-50 p-4 shadow-card">
          <h2 className="font-semibold">Editar livro</h2>
          <div className="mt-4 grid gap-6 lg:grid-cols-[1fr_minmax(280px,360px)] lg:items-start">
            <div>
              <BookFormFields
                form={edit}
                setForm={setEdit}
                errors={editErrors}
                authors={authors}
                publishers={publishers}
                suppliers={suppliers}
                categories={categories}
                pricingGroups={pricingGroups}
                domainLoading={domainLoading}
                salesManagerKey={editSalesKey}
                onSalesManagerKeyChange={setEditSalesKey}
                salesManagerHint="Se o preço ficar abaixo da margem mínima, informe a mesma chave configurada em app.book.sales-manager-key no backend."
                onAuthorCreated={(item) => {
                  setAuthors((p) => mergeIdNameSorted(p, item));
                  setEdit((s) => ({ ...s, authorId: item.id }));
                }}
                onPublisherCreated={(item) => {
                  setPublishers((p) => mergeIdNameSorted(p, item));
                  setEdit((s) => ({ ...s, publisherId: item.id }));
                }}
                onSupplierCreated={(item) => {
                  setSuppliers((p) => mergeIdNameSorted(p, item));
                  setEdit((s) => ({ ...s, supplierId: item.id }));
                }}
              />
              <div className="mt-4 flex flex-wrap gap-2">
                <button
                  type="button"
                  onClick={() => void submitEdit()}
                  disabled={domainLoading}
                  className="rounded-lg bg-brand px-4 py-2 text-sm text-white disabled:opacity-50"
                >
                  Salvar
                </button>
                <button
                  type="button"
                  onClick={() => {
                    setEditId(null);
                    setEditErrors({});
                    setEditSalesKey('');
                  }}
                  className="rounded-lg border px-4 py-2 text-sm"
                >
                  Cancelar
                </button>
              </div>
            </div>
            <AdminBookCatalogPicker
              disabled={loading || domainLoading}
              formLabel="formulário de edição"
              onApply={(d) => {
                setMsg(null);
                const catIds = matchCategoryIds(categories, d.category ?? '');
                setEdit((s) => ({
                  ...s,
                  title: d.title,
                  isbn: d.isbn,
                  synopsis: d.synopsis?.trim()
                    ? d.synopsis.trim()
                    : s.synopsis.trim()
                      ? s.synopsis
                      : d.title,
                  authorId: matchAuthorId(authors, d.author) ?? s.authorId,
                  publisherId: matchPublisherId(publishers, d.publisher ?? '') ?? s.publisherId,
                  categoryIds: catIds ?? s.categoryIds,
                  pageCount:
                    d.pageCount != null && d.pageCount > 0 ? d.pageCount : s.pageCount,
                  publicationYear:
                    d.publicationYear != null && d.publicationYear > 0
                      ? d.publicationYear
                      : s.publicationYear,
                  barcode: s.barcode.trim() ? s.barcode : (d.barcode?.trim() || s.barcode),
                }));
                setMsg(
                  'Catálogo aplicado à edição (sinopse, páginas, ano, editora correspondente, código de barras por ISBN-13 se vazio).',
                );
              }}
            />
          </div>
        </section>
      )}

      <section>
        <h2 className="font-semibold">Catálogo (RF0015 — filtros combinados)</h2>
        <div className="mt-3 flex flex-wrap gap-2 rounded-lg border bg-stone-50/80 p-3">
          <input
            placeholder="Título"
            value={listFilters.title}
            onChange={(e) => setListFilters((f) => ({ ...f, title: e.target.value }))}
            className="min-w-[140px] flex-1 rounded border px-2 py-1.5 text-sm"
          />
          <input
            placeholder="Autor"
            value={listFilters.author}
            onChange={(e) => setListFilters((f) => ({ ...f, author: e.target.value }))}
            className="min-w-[140px] flex-1 rounded border px-2 py-1.5 text-sm"
          />
          <input
            placeholder="Categoria"
            value={listFilters.category}
            onChange={(e) => setListFilters((f) => ({ ...f, category: e.target.value }))}
            className="min-w-[120px] flex-1 rounded border px-2 py-1.5 text-sm"
          />
          <input
            placeholder="ISBN"
            value={listFilters.isbn}
            onChange={(e) => setListFilters((f) => ({ ...f, isbn: e.target.value }))}
            className="min-w-[140px] rounded border px-2 py-1.5 font-mono text-sm"
          />
          <input
            placeholder="Código"
            value={listFilters.code}
            onChange={(e) => setListFilters((f) => ({ ...f, code: e.target.value }))}
            className="min-w-[120px] rounded border px-2 py-1.5 font-mono text-sm"
          />
          <button
            type="button"
            disabled={loading}
            onClick={() => void loadList(0)}
            className="rounded-lg bg-stone-800 px-3 py-1.5 text-sm text-white disabled:opacity-50"
          >
            Aplicar filtros
          </button>
        </div>
        <ul className="mt-4 space-y-2">
          {books.map((b) => (
            <li
              key={b.id}
              className="flex flex-wrap items-center justify-between gap-2 rounded-lg border bg-white p-3 text-sm shadow-card"
            >
              <div>
                <p className="font-medium">{b.title}</p>
                <p className="text-ink-muted">
                  {formatBRL(b.price)} · {b.stockQuantity} un. · {b.active ? 'ativo' : 'inativo'}
                  {b.code ? ` · código ${b.code}` : ''}
                  {b.categoryNames?.length ? ` · ${b.categoryNames.join(', ')}` : ''}
                </p>
                {b.lastLifecycleReason && (
                  <p className="text-xs text-ink-muted">
                    Último ciclo: {b.lastLifecycleReason}
                    {b.lastLifecycleJustification ? ` — ${b.lastLifecycleJustification}` : ''}
                  </p>
                )}
              </div>
              <div className="flex flex-wrap gap-2">
                <button type="button" onClick={() => void startEdit(b.id)} className="rounded border px-2 py-1">
                  Editar
                </button>
                <button type="button" onClick={() => openLifecycle(b)} className="rounded border px-2 py-1">
                  {b.active ? 'Inativar' : 'Ativar'}
                </button>
                <button
                  type="button"
                  onClick={() => setDeleteBookId(b.id)}
                  className="rounded border border-red-200 px-2 py-1 text-red-700"
                >
                  Excluir
                </button>
              </div>
            </li>
          ))}
        </ul>
        <Pagination page={listPage} totalPages={totalPages} onPageChange={(p) => void loadList(p)} />
      </section>
    </div>
  );
}

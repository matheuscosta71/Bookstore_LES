import { api } from './api';

export type IdName = { id: string; name: string };

/** Insere ou atualiza lista ordenada por nome (para após POST de domínio). */
export function mergeIdNameSorted(prev: IdName[], item: IdName): IdName[] {
  if (prev.some((x) => x.id === item.id)) {
    return [...prev].sort((a, b) => a.name.localeCompare(b.name, 'pt', { sensitivity: 'base' }));
  }
  return [...prev, item].sort((a, b) => a.name.localeCompare(b.name, 'pt', { sensitivity: 'base' }));
}

export async function fetchAuthors(): Promise<IdName[]> {
  const { data } = await api.get<IdName[]>('/domain/authors');
  return data;
}

export async function fetchPublishers(): Promise<IdName[]> {
  const { data } = await api.get<IdName[]>('/domain/publishers');
  return data;
}

export async function fetchSuppliers(): Promise<IdName[]> {
  const { data } = await api.get<IdName[]>('/domain/suppliers');
  return data;
}

export async function fetchCategories(): Promise<IdName[]> {
  const { data } = await api.get<IdName[]>('/domain/categories');
  return data;
}

export async function fetchPricingGroups(): Promise<IdName[]> {
  const { data } = await api.get<IdName[]>('/domain/pricing-groups');
  return data;
}

export async function createAuthor(name: string): Promise<IdName> {
  const { data } = await api.post<IdName>('/authors', { name });
  return data;
}

export async function createPublisher(name: string): Promise<IdName> {
  const { data } = await api.post<IdName>('/publishers', { name });
  return data;
}

export async function createSupplier(name: string): Promise<IdName> {
  const { data } = await api.post<IdName>('/suppliers', { name });
  return data;
}

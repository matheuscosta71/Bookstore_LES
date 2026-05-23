const PT = 'pt-BR';

function capitalizeWord(word: string): string {
  if (!word) return word;
  return word
    .split('-')
    .map((part) => {
      if (!part) return part;
      const first = part.charAt(0).toLocaleUpperCase(PT);
      const rest = part.slice(1).toLocaleLowerCase(PT);
      return first + rest;
    })
    .join('-');
}

/** Exibe nome próprio: primeira letra de cada palavra maiúscula, resto minúsculas (locale pt-BR). */
export function formatPersonDisplayName(name: string): string {
  return name
    .trim()
    .split(/\s+/)
    .map(capitalizeWord)
    .join(' ');
}

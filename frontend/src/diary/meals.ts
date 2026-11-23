const DEFAULT_MEALS = ['Café da manhã', 'Almoço', 'Lanche', 'Jantar', 'Ceia']

export function suggestMeals(existing: string[]): string[] {
  return [...new Set([...existing, ...DEFAULT_MEALS])]
}

import { useQuery } from '@tanstack/react-query'
import { fetchCategories } from '../api/categories'
import { fallbackCategories } from '../data/categories'

const iconBySlug = Object.fromEntries(fallbackCategories.map((c) => [c.slug, c.iconUrl]))

/**
 * Categories from the API (ids are needed for the artwork form), with the bundled icons
 * and list as a fallback while loading or if the backend is unreachable.
 */
export function useCategories() {
  const { data } = useQuery({ queryKey: ['categories'], queryFn: fetchCategories, staleTime: 60 * 60 * 1000 })
  if (!Array.isArray(data) || data.length === 0) return fallbackCategories
  return data.map((c) => ({ ...c, iconUrl: iconBySlug[c.slug] ?? c.iconUrl }))
}

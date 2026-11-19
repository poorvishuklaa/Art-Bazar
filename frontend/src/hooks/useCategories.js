import { useEffect, useState } from 'react'
import { fetchCategories } from '../api/categories'
import { fallbackCategories } from '../data/categories'

/** Loads categories from the API, keeping the bundled list if the backend is unreachable. */
export function useCategories() {
  const [categories, setCategories] = useState(fallbackCategories)

  useEffect(() => {
    let cancelled = false
    fetchCategories()
      .then((data) => {
        if (!cancelled && Array.isArray(data) && data.length > 0) setCategories(data)
      })
      .catch(() => {
        /* backend not running: fallback list stays */
      })
    return () => {
      cancelled = true
    }
  }, [])

  return categories
}

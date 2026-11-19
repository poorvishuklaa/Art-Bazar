import { useEffect, useRef, useState } from 'react'
import { useLocation, useSearchParams } from 'react-router-dom'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { searchArtworks } from '../api/artworks'
import { getErrorMessage } from '../api/client'
import { useCategories } from '../hooks/useCategories'
import { useDebouncedValue } from '../hooks/useDebouncedValue'
import ArtworkGrid, { ArtworkGridSkeleton } from '../components/artwork/ArtworkGrid'
import Pagination from '../components/ui/Pagination'
import EmptyState from '../components/ui/EmptyState'
import Alert from '../components/ui/Alert'
import { SearchIcon } from '../components/ui/Icons'
import { usePageTitle } from '../hooks/usePageTitle'

const PAGE_SIZE = 12

const sorts = [
  { value: 'newest', label: 'Newest first' },
  { value: 'price_asc', label: 'Price: low to high' },
  { value: 'price_desc', label: 'Price: high to low' },
  { value: 'title', label: 'Name: A to Z' },
]

/** Catalog with every filter kept in the URL, so results can be bookmarked and shared. */
const Browse = () => {
  usePageTitle('Browse artworks')
  const [params, setParams] = useSearchParams()
  const location = useLocation()
  const categories = useCategories()
  const searchRef = useRef(null)

  const q = params.get('q') ?? ''
  const category = params.get('category') ?? ''
  const minPrice = params.get('minPrice') ?? ''
  const maxPrice = params.get('maxPrice') ?? ''
  const sort = params.get('sort') ?? 'newest'
  const page = Math.max(Number(params.get('page') ?? 1) - 1, 0)

  const [searchText, setSearchText] = useState(q)
  const debouncedSearch = useDebouncedValue(searchText)
  const [priceDraft, setPriceDraft] = useState({ min: minPrice, max: maxPrice })

  /** Updates some params; any filter change sends you back to page 1. */
  const update = (changes, { keepPage = false } = {}) => {
    setParams(
      (prev) => {
        const next = new URLSearchParams(prev)
        Object.entries(changes).forEach(([k, v]) => (v === '' || v == null ? next.delete(k) : next.set(k, v)))
        if (!keepPage) next.delete('page')
        return next
      },
      { replace: !('page' in changes) },
    )
  }

  // Push the debounced search box into the URL.
  useEffect(() => {
    if (debouncedSearch.trim() !== q) update({ q: debouncedSearch.trim() })
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedSearch])

  // Keep inputs in sync when the URL changes from outside (back button, category link).
  useEffect(() => setSearchText(q), [q])
  useEffect(() => setPriceDraft({ min: minPrice, max: maxPrice }), [minPrice, maxPrice])

  useEffect(() => {
    if (location.state?.focusSearch) searchRef.current?.focus()
  }, [location.state])

  const query = { q, category, minPrice, maxPrice, sort, page, size: PAGE_SIZE }
  const { data, isPending, isError, error, isPlaceholderData } = useQuery({
    queryKey: ['artworks', query],
    queryFn: () => searchArtworks(query),
    placeholderData: keepPreviousData,
  })

  const applyPrice = (e) => {
    e.preventDefault()
    update({ minPrice: priceDraft.min, maxPrice: priceDraft.max })
  }

  const hasFilters = q || category || minPrice || maxPrice
  const chip = (active) =>
    `shrink-0 rounded-full px-4 py-2 text-sm transition ${
      active ? 'bg-brand-500 text-white' : 'bg-white text-slate-brand ring-1 ring-gray-200 hover:ring-brand-500'
    }`

  return (
    <div className="py-10">
      <div className="flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <h1 className="text-3xl font-medium">Browse artworks</h1>
          <p className="mt-1 text-gray-500">Original work, bought directly from the artists who made it.</p>
        </div>
        <label className="relative block w-full sm:w-80">
          <span className="sr-only">Search artworks</span>
          <SearchIcon className="pointer-events-none absolute top-1/2 left-3.5 h-5 w-5 -translate-y-1/2 text-gray-400" />
          <input
            ref={searchRef}
            type="search"
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            placeholder="Search paintings, dolls, decor..."
            className="w-full rounded-full border border-gray-300 py-2.5 pr-4 pl-11 outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
          />
        </label>
      </div>

      <div className="mt-6 flex gap-2 overflow-x-auto pb-2 [&::-webkit-scrollbar]:hidden" role="group" aria-label="Category">
        <button type="button" className={chip(!category)} aria-pressed={!category} onClick={() => update({ category: '' })}>
          All
        </button>
        {categories.map((c) => (
          <button
            key={c.slug}
            type="button"
            className={chip(category === c.slug)}
            aria-pressed={category === c.slug}
            onClick={() => update({ category: c.slug })}
          >
            {c.name}
          </button>
        ))}
      </div>

      <div className="mt-4 flex flex-wrap items-end justify-between gap-4 border-b border-gray-100 pb-5">
        <form onSubmit={applyPrice} className="flex flex-wrap items-end gap-2">
          {[
            ['min', 'Min price (₹)'],
            ['max', 'Max price (₹)'],
          ].map(([key, label]) => (
            <label key={key} className="text-xs text-gray-500">
              {label}
              <input
                type="number"
                min="0"
                inputMode="numeric"
                value={priceDraft[key]}
                onChange={(e) => setPriceDraft((d) => ({ ...d, [key]: e.target.value }))}
                className="mt-1 block w-28 rounded-lg border border-gray-300 px-3 py-2 text-sm text-ink outline-none focus:border-brand-500"
              />
            </label>
          ))}
          <button type="submit" className="rounded-lg bg-ink px-4 py-2 text-sm text-white hover:bg-black">
            Apply
          </button>
          {hasFilters && (
            <button
              type="button"
              onClick={() => update({ q: '', category: '', minPrice: '', maxPrice: '' })}
              className="px-2 py-2 text-sm text-brand-500 hover:underline"
            >
              Clear filters
            </button>
          )}
        </form>

        <label className="text-xs text-gray-500">
          Sort by
          <select
            value={sort}
            onChange={(e) => update({ sort: e.target.value === 'newest' ? '' : e.target.value })}
            className="mt-1 block rounded-lg border border-gray-300 bg-white px-3 py-2 text-sm text-ink outline-none focus:border-brand-500"
          >
            {sorts.map((s) => (
              <option key={s.value} value={s.value}>
                {s.label}
              </option>
            ))}
          </select>
        </label>
      </div>

      <div className="mt-6">
        {isError ? (
          <Alert type="error">{getErrorMessage(error, 'Could not load artworks.')}</Alert>
        ) : isPending ? (
          <ArtworkGridSkeleton />
        ) : data.content.length === 0 ? (
          <EmptyState title="No artworks match your search">Try a different word, category or price range.</EmptyState>
        ) : (
          <>
            <p className="mb-4 text-sm text-gray-500" aria-live="polite">
              {data.totalElements} {data.totalElements === 1 ? 'artwork' : 'artworks'}
            </p>
            <div className={isPlaceholderData ? 'opacity-60 transition' : 'transition'}>
              <ArtworkGrid artworks={data.content} />
            </div>
            <Pagination
              page={data.page}
              totalPages={data.totalPages}
              onChange={(p) => {
                update({ page: p === 0 ? '' : String(p + 1) }, { keepPage: true })
                window.scrollTo({ top: 0, behavior: 'smooth' })
              }}
            />
          </>
        )}
      </div>
    </div>
  )
}

export default Browse

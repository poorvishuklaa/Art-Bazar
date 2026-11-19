import { useEffect, useState } from 'react'
import { useDebouncedValue } from '../../hooks/useDebouncedValue'
import { SearchIcon } from '../ui/Icons'

/** Debounced search input that reports its value 350 ms after typing stops. */
const SearchBox = ({ value, onChange, placeholder, label = 'Search' }) => {
  const [text, setText] = useState(value)
  const debounced = useDebouncedValue(text)
  useEffect(() => {
    if (debounced !== value) onChange(debounced)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debounced])
  return (
    <label className="relative block w-full sm:w-72">
      <span className="sr-only">{label}</span>
      <SearchIcon className="pointer-events-none absolute top-1/2 left-3 h-4 w-4 -translate-y-1/2 text-gray-400" />
      <input
        type="search"
        value={text}
        onChange={(e) => setText(e.target.value)}
        placeholder={placeholder}
        className="w-full rounded-full border border-gray-300 py-2 pr-4 pl-9 text-sm outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100"
      />
    </label>
  )
}

export default SearchBox

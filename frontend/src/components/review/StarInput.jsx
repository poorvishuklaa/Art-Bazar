import { useState } from 'react'
import { StarIcon } from '../ui/Icons'

const labels = ['', 'Poor', 'Fair', 'Good', 'Very good', 'Excellent']

/** 1–5 star picker built on radio buttons, so arrow keys and screen readers work. */
const StarInput = ({ value, onChange, name = 'rating' }) => {
  const [hover, setHover] = useState(0)
  const shown = hover || value
  return (
    <fieldset>
      <legend className="mb-1.5 text-sm font-medium text-gray-700">Your rating</legend>
      <div className="flex items-center gap-3">
        <div className="flex" onMouseLeave={() => setHover(0)}>
          {[1, 2, 3, 4, 5].map((n) => (
            <label key={n} className="cursor-pointer p-0.5" onMouseEnter={() => setHover(n)}>
              <input type="radio" name={name} value={n} checked={value === n} onChange={() => onChange(n)} className="peer sr-only" />
              <StarIcon
                filled={n <= shown}
                className="h-8 w-8 text-amber-400 transition peer-focus-visible:rounded peer-focus-visible:ring-2 peer-focus-visible:ring-sky-brand"
              />
              <span className="sr-only">{n} {n === 1 ? 'star' : 'stars'}, {labels[n]}</span>
            </label>
          ))}
        </div>
        {shown > 0 && <span className="text-sm text-gray-600">{labels[shown]}</span>}
      </div>
    </fieldset>
  )
}

export default StarInput

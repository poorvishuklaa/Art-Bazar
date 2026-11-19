import { StarIcon } from '../ui/Icons'

/** Read-only stars; fractional ratings fill part of a star. The number is given to screen readers. */
const Stars = ({ rating, size = 'h-4 w-4', className = '' }) => (
  <span className={`inline-flex items-center ${className}`} role="img" aria-label={`${rating} out of 5 stars`}>
    {[1, 2, 3, 4, 5].map((i) => {
      const fill = Math.max(0, Math.min(1, rating - (i - 1)))
      return (
        <span key={i} className={`relative ${size}`}>
          <StarIcon filled={false} className={`absolute inset-0 ${size} text-amber-400`} />
          <span className="absolute inset-0 overflow-hidden" style={{ width: `${fill * 100}%` }}>
            <StarIcon className={`${size} text-amber-400`} />
          </span>
        </span>
      )
    })}
  </span>
)

export default Stars

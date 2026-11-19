import { formatPrice } from '../../utils/format'
import { StarIcon } from '../ui/Icons'

const ArtworkCard = ({ artwork }) => (
  <article className="group overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-gray-100 transition hover:-translate-y-1 hover:shadow-lg">
    <div className="aspect-[4/3] overflow-hidden bg-gray-100">
      <img
        src={artwork.imageUrl}
        alt={artwork.title}
        loading="lazy"
        className="h-full w-full object-cover transition duration-500 group-hover:scale-105"
      />
    </div>
    <div className="space-y-2 p-4">
      <div className="flex items-start justify-between gap-3">
        <h3 className="font-medium">{artwork.title}</h3>
        {artwork.averageRating != null && (
          <span className="flex shrink-0 items-center gap-1 text-sm text-amber-500">
            <StarIcon className="h-4 w-4" />
            {artwork.averageRating.toFixed(1)}
          </span>
        )}
      </div>
      <p className="line-clamp-2 text-sm text-gray-500">{artwork.description}</p>
      <div className="flex items-center justify-between pt-1">
        <span className="text-lg font-semibold text-brand-500">{formatPrice(artwork.price)}</span>
        <span className="text-xs text-gray-400">by {artwork.artistName}</span>
      </div>
    </div>
  </article>
)

export default ArtworkCard

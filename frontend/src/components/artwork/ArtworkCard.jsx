import { Link } from 'react-router-dom'
import { formatPrice } from '../../utils/format'

const ArtworkCard = ({ artwork }) => (
  <article className="group relative overflow-hidden rounded-2xl bg-white shadow-sm ring-1 ring-gray-100 transition hover:-translate-y-1 hover:shadow-lg">
    <div className="aspect-[4/3] overflow-hidden bg-gray-100">
      <img
        src={artwork.imageUrl}
        alt={artwork.title}
        loading="lazy"
        className="h-full w-full object-cover transition duration-500 group-hover:scale-105"
      />
    </div>
    <div className="space-y-1.5 p-4">
      <p className="text-xs font-medium tracking-wide text-sky-brand uppercase">{artwork.category.name}</p>
      <h3 className="line-clamp-1 font-medium">
        <Link to={`/artworks/${artwork.id}`} className="after:absolute after:inset-0">
          {artwork.title}
        </Link>
      </h3>
      <p className="line-clamp-1 text-sm text-gray-500">by {artwork.artist.name}</p>
      <div className="flex items-center justify-between pt-1">
        <span className="text-lg font-semibold text-brand-500">{formatPrice(artwork.price)}</span>
        {artwork.stock === 0 && <span className="text-xs font-medium text-gray-400">Sold out</span>}
      </div>
    </div>
  </article>
)

export default ArtworkCard

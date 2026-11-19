import { Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { fetchArtwork, searchArtworks } from '../api/artworks'
import { getErrorMessage } from '../api/client'
import { formatPrice } from '../utils/format'
import ArtworkGrid from '../components/artwork/ArtworkGrid'
import Spinner from '../components/ui/Spinner'
import NotFound from './NotFound'
import Alert from '../components/ui/Alert'

const stockLabel = (stock) => {
  if (stock === 0) return { text: 'Sold out', cls: 'text-gray-500' }
  if (stock === 1) return { text: 'Only one available: this is an original', cls: 'text-amber-600' }
  return { text: `In stock (${stock} available)`, cls: 'text-green-700' }
}

const ArtworkDetail = () => {
  const { id } = useParams()
  const { data: art, isPending, isError, error } = useQuery({ queryKey: ['artwork', id], queryFn: () => fetchArtwork(id) })

  const moreQuery = { artistId: art?.artist.id, size: 5 }
  const more = useQuery({
    queryKey: ['artworks', moreQuery],
    queryFn: () => searchArtworks(moreQuery),
    enabled: !!art,
  })

  if (isPending) {
    return (
      <div className="grid place-items-center py-32">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }
  if (isError) {
    return error.response?.status === 404 ? <NotFound /> : <div className="py-12"><Alert type="error">{getErrorMessage(error)}</Alert></div>
  }

  const stock = stockLabel(art.stock)
  const others = more.data?.content.filter((a) => a.id !== art.id).slice(0, 4) ?? []

  return (
    <div className="py-10">
      <nav aria-label="Breadcrumb" className="mb-6 text-sm text-gray-500">
        <Link to="/artworks" className="hover:text-brand-500">Artworks</Link>
        <span className="mx-2">/</span>
        <Link to={`/artworks?category=${art.category.slug}`} className="hover:text-brand-500">{art.category.name}</Link>
      </nav>

      <div className="grid gap-10 lg:grid-cols-2">
        <div className="overflow-hidden rounded-2xl bg-gray-50 ring-1 ring-gray-100">
          <img src={art.imageUrl} alt={art.title} className="max-h-[70vh] w-full object-contain" />
        </div>

        <div>
          <h1 className="text-3xl leading-tight font-medium sm:text-4xl">{art.title}</h1>
          <p className="mt-3 text-gray-600">
            by{' '}
            <Link to={`/artists/${art.artist.id}`} className="font-medium text-ink hover:text-brand-500">
              {art.artist.name}
            </Link>
            {art.artist.city && <span className="text-gray-400"> · {art.artist.city}</span>}
          </p>

          <p className="mt-6 text-3xl font-semibold text-brand-500">{formatPrice(art.price)}</p>
          <p className={`mt-2 text-sm font-medium ${stock.cls}`}>{stock.text}</p>

          <div className="mt-6 space-y-2">
            <button
              type="button"
              disabled
              className="w-full rounded-full bg-brand-500 py-3.5 font-medium text-white opacity-50 sm:w-auto sm:px-12"
            >
              Add to cart
            </button>
            <p className="text-xs text-gray-400">Checkout is opening soon.</p>
          </div>

          {art.description && (
            <div className="mt-8 border-t border-gray-100 pt-6">
              <h2 className="text-sm font-semibold tracking-wider text-gray-500 uppercase">About this piece</h2>
              <p className="mt-3 leading-relaxed whitespace-pre-line text-gray-700">{art.description}</p>
            </div>
          )}
        </div>
      </div>

      {others.length > 0 && (
        <section className="mt-16">
          <div className="mb-6 flex items-end justify-between">
            <h2 className="text-2xl font-medium">More from {art.artist.name}</h2>
            <Link to={`/artists/${art.artist.id}`} className="text-sm font-medium text-brand-500 hover:underline">
              View profile →
            </Link>
          </div>
          <ArtworkGrid artworks={others} />
        </section>
      )}
    </div>
  )
}

export default ArtworkDetail

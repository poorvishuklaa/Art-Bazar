import { useParams, useSearchParams } from 'react-router-dom'
import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { fetchArtist, searchArtworks } from '../api/artworks'
import { getErrorMessage } from '../api/client'
import { formatDate } from '../utils/format'
import ArtworkGrid, { ArtworkGridSkeleton } from '../components/artwork/ArtworkGrid'
import Pagination from '../components/ui/Pagination'
import EmptyState from '../components/ui/EmptyState'
import Spinner from '../components/ui/Spinner'
import Alert from '../components/ui/Alert'
import NotFound from './NotFound'
import Stars from '../components/review/Stars'

const ArtistProfile = () => {
  const { id } = useParams()
  const [params, setParams] = useSearchParams()
  const page = Math.max(Number(params.get('page') ?? 1) - 1, 0)

  const artist = useQuery({ queryKey: ['artist', id], queryFn: () => fetchArtist(id) })
  const worksQuery = { artistId: id, page, size: 12 }
  const works = useQuery({
    queryKey: ['artworks', worksQuery],
    queryFn: () => searchArtworks(worksQuery),
    placeholderData: keepPreviousData,
    enabled: artist.isSuccess,
  })

  if (artist.isPending) {
    return (
      <div className="grid place-items-center py-32">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }
  if (artist.isError) {
    return artist.error.response?.status === 404 ? <NotFound /> : <div className="py-12"><Alert type="error">{getErrorMessage(artist.error)}</Alert></div>
  }

  const a = artist.data
  return (
    <div className="py-10">
      <header className="flex flex-col gap-6 rounded-2xl bg-brand-50 p-6 sm:flex-row sm:items-center sm:p-8">
        {a.avatarUrl ? (
          <img src={a.avatarUrl} alt="" className="h-24 w-24 rounded-full object-cover" />
        ) : (
          <span className="grid h-24 w-24 shrink-0 place-items-center rounded-full bg-brand-500 text-4xl font-semibold text-white">
            {a.name.charAt(0)}
          </span>
        )}
        <div>
          <h1 className="text-3xl font-medium">{a.name}</h1>
          <p className="mt-1 text-sm text-gray-600">
            {[a.city, `${a.artworkCount} ${a.artworkCount === 1 ? 'artwork' : 'artworks'}`, `Selling since ${formatDate(a.memberSince)}`]
              .filter(Boolean)
              .join(' · ')}
          </p>
          {a.averageRating != null && (
            <p className="mt-2 flex items-center gap-2 text-sm text-gray-600">
              <Stars rating={a.averageRating} />
              <span className="font-medium">{a.averageRating.toFixed(1)}</span>
              <span>from {a.reviewCount} {a.reviewCount === 1 ? 'review' : 'reviews'}</span>
            </p>
          )}
          {a.bio && <p className="mt-3 max-w-2xl text-gray-700">{a.bio}</p>}
        </div>
      </header>

      <section className="mt-10">
        <h2 className="mb-6 text-2xl font-medium">Artworks</h2>
        {works.isPending ? (
          <ArtworkGridSkeleton count={4} />
        ) : works.isError ? (
          <Alert type="error">{getErrorMessage(works.error)}</Alert>
        ) : works.data.content.length === 0 ? (
          <EmptyState title="No artworks listed right now">Check back soon.</EmptyState>
        ) : (
          <>
            <ArtworkGrid artworks={works.data.content} />
            <Pagination
              page={works.data.page}
              totalPages={works.data.totalPages}
              onChange={(p) => setParams(p === 0 ? {} : { page: String(p + 1) })}
            />
          </>
        )}
      </section>
    </div>
  )
}

export default ArtistProfile

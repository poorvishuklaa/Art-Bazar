import { Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import Hero from '../components/home/Hero'
import CategoryStrip from '../components/home/CategoryStrip'
import ArtworkGrid, { ArtworkGridSkeleton } from '../components/artwork/ArtworkGrid'
import Alert from '../components/ui/Alert'
import { useCategories } from '../hooks/useCategories'
import { searchArtworks } from '../api/artworks'
import { getErrorMessage } from '../api/client'
import { usePageTitle } from '../hooks/usePageTitle'

const latestQuery = { sort: 'newest', size: 8 }

const Home = () => {
  usePageTitle(null)
  const categories = useCategories()
  const latest = useQuery({ queryKey: ['artworks', latestQuery], queryFn: () => searchArtworks(latestQuery) })

  return (
    <>
      <Hero />
      <CategoryStrip categories={categories} />

      <section id="artworks" className="scroll-mt-24 pt-10">
        <div className="flex items-end justify-between gap-4">
          <h2 className="text-2xl font-medium sm:text-3xl">Fresh from local artists</h2>
          <Link to="/artworks" className="shrink-0 text-sm font-medium text-brand-500 hover:underline">
            Browse all →
          </Link>
        </div>
        <div className="mt-8">
          {latest.isError ? (
            <Alert type="error">{getErrorMessage(latest.error, 'Could not load artworks.')}</Alert>
          ) : latest.isPending ? (
            <ArtworkGridSkeleton />
          ) : (
            <ArtworkGrid artworks={latest.data.content} />
          )}
        </div>
      </section>
    </>
  )
}

export default Home

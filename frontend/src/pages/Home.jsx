import { useMemo, useState } from 'react'
import Hero from '../components/home/Hero'
import CategoryStrip from '../components/home/CategoryStrip'
import ArtworkGrid from '../components/home/ArtworkGrid'
import { useCategories } from '../hooks/useCategories'
import { sampleArtworks } from '../data/sampleArtworks'

const Home = () => {
  const categories = useCategories()
  const [selected, setSelected] = useState(null)

  const artworks = useMemo(
    () => (selected ? sampleArtworks.filter((a) => a.categorySlug === selected) : sampleArtworks),
    [selected],
  )
  const selectedName = categories.find((c) => c.slug === selected)?.name

  return (
    <>
      <Hero />
      <CategoryStrip categories={categories} selected={selected} onSelect={setSelected} />
      <ArtworkGrid artworks={artworks} title={selectedName ?? 'Artworks from local artists'} />
    </>
  )
}

export default Home

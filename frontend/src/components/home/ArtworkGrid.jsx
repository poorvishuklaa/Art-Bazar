import ArtworkCard from './ArtworkCard'

const ArtworkGrid = ({ artworks, title }) => (
  <section id="artworks" className="scroll-mt-24 pt-10">
    <h2 className="text-2xl font-medium sm:text-3xl">{title}</h2>
    {artworks.length === 0 ? (
      <p className="mt-8 rounded-xl bg-gray-50 p-8 text-center text-gray-500">
        No artworks in this category yet. Check back soon!
      </p>
    ) : (
      <div className="mt-8 grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4">
        {artworks.map((a) => (
          <ArtworkCard key={a.id} artwork={a} />
        ))}
      </div>
    )}
  </section>
)

export default ArtworkGrid

import ArtworkCard from './ArtworkCard'

export const gridClasses = 'grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4'

export const ArtworkGridSkeleton = ({ count = 8 }) => (
  <div className={gridClasses} aria-hidden="true">
    {Array.from({ length: count }, (_, i) => (
      <div key={i} className="animate-pulse overflow-hidden rounded-2xl ring-1 ring-gray-100">
        <div className="aspect-[4/3] bg-gray-100" />
        <div className="space-y-2 p-4">
          <div className="h-3 w-1/4 rounded bg-gray-100" />
          <div className="h-4 w-3/4 rounded bg-gray-100" />
          <div className="h-4 w-1/3 rounded bg-gray-100" />
        </div>
      </div>
    ))}
  </div>
)

const ArtworkGrid = ({ artworks }) => (
  <div className={gridClasses}>
    {artworks.map((a) => (
      <ArtworkCard key={a.id} artwork={a} />
    ))}
  </div>
)

export default ArtworkGrid

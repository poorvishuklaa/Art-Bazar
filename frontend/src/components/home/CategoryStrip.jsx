import { Link } from 'react-router-dom'

const CategoryStrip = ({ categories }) => (
  <section id="explore" className="scroll-mt-24 pt-12">
    <h2 className="text-2xl font-medium sm:text-3xl">Explore our categories</h2>
    <p className="mt-3 max-w-2xl text-gray-600">
      From devotional paintings to hand-stitched dolls, every piece is listed by the artist who made it.
    </p>

    <div className="mt-8 flex gap-6 overflow-x-auto pb-4 sm:gap-8 [&::-webkit-scrollbar]:hidden">
      {categories.map((c) => (
        <Link
          key={c.slug}
          to={`/artworks?category=${c.slug}`}
          className="group flex min-w-[88px] flex-col items-center gap-2 text-center"
        >
          <img
            src={c.iconUrl}
            alt=""
            className="h-20 w-20 rounded-full object-cover p-1 ring-1 ring-gray-200 transition group-hover:ring-4 group-hover:ring-brand-500/60 sm:h-24 sm:w-24"
          />
          <span className="text-sm text-gray-600 group-hover:text-brand-500 sm:text-base">{c.name}</span>
        </Link>
      ))}
    </div>
    <hr className="mt-4 border-gray-200" />
  </section>
)

export default CategoryStrip

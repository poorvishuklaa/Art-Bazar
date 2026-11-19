const CategoryStrip = ({ categories, selected, onSelect }) => (
  <section id="explore" className="scroll-mt-24 pt-12">
    <h2 className="text-2xl font-medium sm:text-3xl">Explore our categories</h2>
    <p className="mt-3 max-w-2xl text-gray-600">
      From devotional paintings to hand-stitched dolls, every piece is listed by the artist who made it.
    </p>

    <div className="mt-8 flex gap-6 overflow-x-auto pb-4 sm:gap-8 [&::-webkit-scrollbar]:hidden">
      {categories.map((c) => {
        const active = selected === c.slug
        return (
          <button
            key={c.slug}
            type="button"
            onClick={() => onSelect(active ? null : c.slug)}
            aria-pressed={active}
            className="group flex min-w-[88px] flex-col items-center gap-2 text-center"
          >
            <img
              src={c.iconUrl}
              alt=""
              className={`h-20 w-20 rounded-full object-cover p-1 transition sm:h-24 sm:w-24 ${
                active ? 'ring-4 ring-brand-500' : 'ring-1 ring-gray-200 group-hover:ring-brand-500/60'
              }`}
            />
            <span className={`text-sm sm:text-base ${active ? 'font-medium text-brand-500' : 'text-gray-600'}`}>
              {c.name}
            </span>
          </button>
        )
      })}
    </div>
    <hr className="mt-4 border-gray-200" />
  </section>
)

export default CategoryStrip

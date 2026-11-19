import { Link } from 'react-router-dom'
import header from '../../assets/header.jpg'

const Hero = () => (
  <section
    className="relative mt-4 flex min-h-[340px] items-end overflow-hidden rounded-2xl bg-cover bg-center sm:min-h-[420px] lg:min-h-[32vw]"
    style={{ backgroundImage: `url(${header})` }}
  >
    <div className="absolute inset-0 bg-gradient-to-t from-black/70 via-black/30 to-transparent" />
    <div className="relative max-w-3xl animate-fade-in space-y-4 p-6 sm:p-10 lg:p-14">
      <h1 className="text-3xl leading-tight font-medium text-white sm:text-5xl lg:text-6xl">
        Transform your home with beautiful art
      </h1>
      <p className="max-w-xl text-sm text-white/90 sm:text-base">
        Discover original paintings, crafts and handmade decor, bought directly from the local artists who made them.
      </p>
      <Link
        to="/#artworks"
        className="inline-block rounded-full bg-white px-7 py-3 text-sm font-medium text-ink transition hover:bg-brand-500 hover:text-white"
      >
        Shop now
      </Link>
    </div>
  </section>
)

export default Hero

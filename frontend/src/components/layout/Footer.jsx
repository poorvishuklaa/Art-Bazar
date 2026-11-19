import { Link } from 'react-router-dom'
import logo from '../../assets/logo.png'
import { FacebookIcon, InstagramIcon, LinkedinIcon } from '../ui/Icons'

const Footer = () => (
  <footer id="footer" className="mt-24 bg-ink text-gray-300">
    <div className="mx-auto grid max-w-7xl gap-10 px-4 py-14 sm:px-6 md:grid-cols-[2fr_1fr_1fr] lg:px-8">
      <div className="space-y-4">
        <div className="flex items-center gap-2">
          <img src={logo} alt="" className="h-10 w-10 rounded-full bg-white object-contain p-0.5" />
          <span className="text-xl font-semibold text-white">Art Bazaar</span>
        </div>
        <p className="max-w-md text-sm leading-relaxed">
          A marketplace where local artists sell paintings, crafts and handmade decor directly to the people who love
          them. No middlemen, just art.
        </p>
        <div className="flex gap-3">
          {[InstagramIcon, FacebookIcon, LinkedinIcon].map((Icon, i) => (
            <span key={i} className="grid h-9 w-9 place-items-center rounded-full border border-gray-600 hover:border-brand-500 hover:text-brand-500">
              <Icon className="h-4 w-4" />
            </span>
          ))}
        </div>
      </div>

      <div>
        <h3 className="mb-4 text-sm font-semibold tracking-wider text-white uppercase">Marketplace</h3>
        <ul className="space-y-2 text-sm">
          <li><Link to="/artworks" className="hover:text-white">Browse artworks</Link></li>
          <li><Link to="/register" className="hover:text-white">Sell your art</Link></li>
          <li><Link to="/login" className="hover:text-white">Sign in</Link></li>
        </ul>
      </div>

      <div>
        <h3 className="mb-4 text-sm font-semibold tracking-wider text-white uppercase">Get in touch</h3>
        <ul className="space-y-2 text-sm">
          <li>hello@artbazaar.example</li>
          <li>Made in India</li>
        </ul>
      </div>
    </div>
    <p className="border-t border-gray-700 py-5 text-center text-xs text-gray-400">
      © {new Date().getFullYear()} Art Bazaar. All rights reserved.
    </p>
  </footer>
)

export default Footer

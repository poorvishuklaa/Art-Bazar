import paintings from '../assets/categories/paintings.png'
import craft from '../assets/categories/craft.png'
import canvasDiy from '../assets/categories/canvas-diy.png'
import dolls from '../assets/categories/dolls.png'
import homeDecor from '../assets/categories/home-decor.png'
import toys from '../assets/categories/toys.png'
import jewellery from '../assets/categories/jewellery.png'

// Shown until the backend responds; slugs match the V2 Flyway seed.
export const fallbackCategories = [
  { slug: 'paintings', name: 'Paintings', iconUrl: paintings },
  { slug: 'craft', name: 'Craft', iconUrl: craft },
  { slug: 'canvas-diy', name: 'Canvas DIY', iconUrl: canvasDiy },
  { slug: 'dolls', name: 'Dolls', iconUrl: dolls },
  { slug: 'home-decor', name: 'Home Decor', iconUrl: homeDecor },
  { slug: 'toys', name: 'Toys', iconUrl: toys },
  { slug: 'jewellery', name: 'Jewellery', iconUrl: jewellery },
]

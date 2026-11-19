// Small inline SVG icons: crisp at any size and coloured with currentColor.
const base = {
  xmlns: 'http://www.w3.org/2000/svg',
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 1.8,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
}

export const SearchIcon = (props) => (
  <svg {...base} {...props}><circle cx="11" cy="11" r="7" /><path d="m20 20-3.5-3.5" /></svg>
)

export const BagIcon = (props) => (
  <svg {...base} {...props}>
    <path d="M6 7h12l1 13H5L6 7Z" /><path d="M9 7V6a3 3 0 0 1 6 0v1" />
  </svg>
)

export const MenuIcon = (props) => (
  <svg {...base} {...props}><path d="M4 7h16M4 12h16M4 17h16" /></svg>
)

export const CloseIcon = (props) => (
  <svg {...base} {...props}><path d="M6 6l12 12M18 6 6 18" /></svg>
)

export const StarIcon = ({ filled = true, ...props }) => (
  <svg {...base} fill={filled ? 'currentColor' : 'none'} strokeWidth={1.4} {...props}>
    <path d="m12 3 2.7 5.6 6.1.9-4.4 4.3 1 6.1L12 17l-5.4 2.9 1-6.1L3.2 9.5l6.1-.9L12 3Z" />
  </svg>
)

export const InstagramIcon = (props) => (
  <svg {...base} {...props}>
    <rect x="3" y="3" width="18" height="18" rx="5" /><circle cx="12" cy="12" r="4" /><circle cx="17.5" cy="6.5" r=".6" fill="currentColor" />
  </svg>
)

export const FacebookIcon = (props) => (
  <svg {...base} {...props}><path d="M15 3h-2.5A3.5 3.5 0 0 0 9 6.5V10H6.5v3.5H9V21h3.5v-7.5H15l.5-3.5h-3V7a1 1 0 0 1 1-1H15V3Z" /></svg>
)

export const LinkedinIcon = (props) => (
  <svg {...base} {...props}>
    <rect x="3" y="3" width="18" height="18" rx="3" /><path d="M8 10v7M8 7v.01M12 17v-4a2 2 0 0 1 4 0v4M12 10v7" />
  </svg>
)

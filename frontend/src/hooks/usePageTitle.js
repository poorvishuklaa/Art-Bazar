import { useEffect } from 'react'

/** Sets the browser tab title, e.g. "Cart · Art Bazaar". Pass a falsy title to show just the site name. */
export function usePageTitle(title) {
  useEffect(() => {
    document.title = title ? `${title} · Art Bazaar` : 'Art Bazaar: art from local artists'
  }, [title])
}

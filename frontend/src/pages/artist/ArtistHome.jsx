import { useEffect } from 'react'
import { useAuth } from '../../hooks/useAuth'
import Alert from '../../components/ui/Alert'

/** Artist landing page. The full dashboard (artworks, orders, sales) builds on this. */
const ArtistHome = () => {
  const { user, reloadUser } = useAuth()

  // Pick up an approval that happened since sign-in.
  useEffect(() => {
    if (!user.artistApproved) reloadUser().catch(() => {})
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  return (
    <div className="py-12">
      <h1 className="text-3xl font-medium">Hello, {user.name.split(' ')[0]}</h1>
      <p className="mt-2 text-gray-500">This is your artist space on Art Bazaar.</p>

      <div className="mt-8 max-w-2xl">
        {user.artistApproved ? (
          <Alert type="success">
            Your artist account is approved. You&apos;ll be able to add artworks and manage orders from here.
          </Alert>
        ) : (
          <Alert type="warning">
            <p className="font-medium">Your account is waiting for approval</p>
            <p className="mt-1">
              Our team reviews every new artist to keep the marketplace genuine. You can browse and buy in the meantime;
              listing artworks unlocks once you&apos;re approved.
            </p>
          </Alert>
        )}
      </div>
    </div>
  )
}

export default ArtistHome

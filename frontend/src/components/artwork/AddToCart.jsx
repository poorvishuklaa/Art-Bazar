import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../../hooks/useAuth'
import { useCart, useCartMutations } from '../../hooks/useCart'
import { getErrorMessage } from '../../api/client'
import { canShop } from '../../utils/roles'
import QuantityStepper from '../ui/QuantityStepper'
import Alert from '../ui/Alert'

const AddToCart = ({ artwork }) => {
  const { user, isAuthenticated, status } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const { cart } = useCart()
  const { add } = useCartMutations()
  const [qty, setQty] = useState(1)
  const [added, setAdded] = useState(false)

  const inCart = cart?.items.find((l) => l.artwork.id === artwork.id)?.quantity ?? 0
  const canAddMore = artwork.stock - inCart

  // Until we know who is signed in, show a placeholder rather than a button that may not apply.
  if (status === 'loading') return <div className="h-[52px] animate-pulse rounded-full bg-gray-100 sm:w-60" aria-hidden="true" />

  if (artwork.stock === 0) {
    return <p className="rounded-xl bg-gray-50 p-4 text-sm text-gray-600">This piece has been sold. Check the artist&apos;s other work below.</p>
  }
  if (isAuthenticated && user.id === artwork.artist.id) {
    return <Alert type="info">This is your artwork, so it can&apos;t be added to your cart.</Alert>
  }
  if (isAuthenticated && !canShop(user)) {
    return <p className="text-sm text-gray-500">Admin accounts can&apos;t place orders.</p>
  }

  const onAdd = () => {
    if (!isAuthenticated) {
      navigate('/login', { state: { from: location } })
      return
    }
    setAdded(false)
    add.mutate({ artworkId: artwork.id, quantity: qty }, { onSuccess: () => { setAdded(true); setQty(1) } })
  }

  return (
    <div className="space-y-3">
      <div className="flex flex-wrap items-center gap-3">
        {artwork.stock > 1 && canAddMore > 0 && <QuantityStepper value={qty} max={canAddMore} onChange={setQty} />}
        <button
          type="button"
          onClick={onAdd}
          disabled={add.isPending || (isAuthenticated && canAddMore <= 0)}
          className="flex-1 rounded-full bg-brand-500 px-10 py-3.5 font-medium text-white transition hover:bg-brand-600 disabled:opacity-50 sm:flex-none"
        >
          {add.isPending ? 'Adding…' : isAuthenticated && canAddMore <= 0 ? 'All available are in your cart' : 'Add to cart'}
        </button>
      </div>
      {!isAuthenticated && <p className="text-xs text-gray-500">You&apos;ll be asked to sign in first.</p>}
      {added && (
        <Alert type="success">
          Added to your cart.{' '}
          <Link to="/cart" className="font-medium underline">
            View cart and check out
          </Link>
        </Alert>
      )}
      {add.isError && <Alert type="error">{getErrorMessage(add.error)}</Alert>}
    </div>
  )
}

export default AddToCart

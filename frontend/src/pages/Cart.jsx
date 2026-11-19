import { Link } from 'react-router-dom'
import { useCart, useCartMutations } from '../hooks/useCart'
import { getErrorMessage } from '../api/client'
import { formatPrice } from '../utils/format'
import QuantityStepper from '../components/ui/QuantityStepper'
import EmptyState from '../components/ui/EmptyState'
import Spinner from '../components/ui/Spinner'
import Alert from '../components/ui/Alert'
import { usePageTitle } from '../hooks/usePageTitle'

const issueText = {
  UNAVAILABLE: 'No longer available. Please remove it.',
  SOLD_OUT: 'Sold out. Please remove it.',
}

const Cart = () => {
  usePageTitle('Your cart')
  const { cart, isPending, isError, error } = useCart()
  const { update, remove } = useCartMutations()
  const mutationError = update.error ?? remove.error

  if (isPending) {
    return (
      <div className="grid place-items-center py-32">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }
  if (isError) return <div className="py-12"><Alert type="error">{getErrorMessage(error)}</Alert></div>

  if (cart.items.length === 0) {
    return (
      <div className="mx-auto max-w-2xl py-16">
        <h1 className="mb-6 text-3xl font-medium">Your cart</h1>
        <EmptyState title="Your cart is empty">
          <Link to="/artworks" className="font-medium text-brand-500 hover:underline">Browse artworks</Link> to find something you love.
        </EmptyState>
      </div>
    )
  }

  const blocked = !cart.readyForCheckout

  return (
    <div className="py-10">
      <h1 className="text-3xl font-medium">Your cart</h1>
      <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_340px]">
        <div className="space-y-3">
          {mutationError && <Alert type="error">{getErrorMessage(mutationError)}</Alert>}
          <ul className="divide-y divide-gray-100 rounded-2xl bg-white ring-1 ring-gray-100">
            {cart.items.map(({ artwork: a, quantity, lineTotal, issue }) => {
              const busy = (update.isPending && update.variables?.artworkId === a.id) || (remove.isPending && remove.variables === a.id)
              return (
                <li key={a.id} className={`flex gap-4 p-4 ${busy ? 'opacity-60' : ''}`}>
                  <Link to={`/artworks/${a.id}`} className="shrink-0">
                    <img src={a.imageUrl} alt="" className="h-24 w-24 rounded-xl object-cover ring-1 ring-gray-100" />
                  </Link>
                  <div className="min-w-0 flex-1">
                    <div className="flex items-start justify-between gap-3">
                      <div className="min-w-0">
                        <Link to={`/artworks/${a.id}`} className="line-clamp-1 font-medium hover:text-brand-500">{a.title}</Link>
                        <p className="text-sm text-gray-500">by {a.artist.name}</p>
                      </div>
                      <p className={`shrink-0 font-semibold ${issue ? 'text-gray-400 line-through' : ''}`}>{formatPrice(lineTotal)}</p>
                    </div>
                    {issue && (
                      <p className="mt-2 text-sm font-medium text-red-600">
                        {issue === 'NOT_ENOUGH_STOCK' ? `Only ${a.stock} left. Lower the quantity to continue.` : issueText[issue]}
                      </p>
                    )}
                    <div className="mt-3 flex items-center justify-between gap-3">
                      {issue === 'UNAVAILABLE' || issue === 'SOLD_OUT' ? (
                        <span />
                      ) : (
                        <QuantityStepper
                          value={quantity}
                          max={Math.max(a.stock, 1)}
                          disabled={busy}
                          onChange={(q) => update.mutate({ artworkId: a.id, quantity: q })}
                        />
                      )}
                      <button
                        type="button"
                        disabled={busy}
                        onClick={() => remove.mutate(a.id)}
                        className="text-sm text-gray-500 hover:text-red-600"
                      >
                        Remove
                      </button>
                    </div>
                  </div>
                </li>
              )
            })}
          </ul>
        </div>

        <aside className="h-fit rounded-2xl bg-gray-50 p-6 lg:sticky lg:top-24">
          <h2 className="text-lg font-medium">Order summary</h2>
          <dl className="mt-4 space-y-2 text-sm">
            <div className="flex justify-between">
              <dt className="text-gray-600">Items ({cart.itemCount})</dt>
              <dd>{formatPrice(cart.subtotal)}</dd>
            </div>
            <div className="flex justify-between">
              <dt className="text-gray-600">Shipping</dt>
              <dd className="text-green-700">Free</dd>
            </div>
            <div className="flex justify-between border-t border-gray-200 pt-3 text-base font-semibold">
              <dt>Total</dt>
              <dd>{formatPrice(cart.subtotal)}</dd>
            </div>
          </dl>
          {blocked ? (
            <p className="mt-6 rounded-lg bg-white p-3 text-sm text-red-600 ring-1 ring-red-100">
              Fix the items marked in red to continue.
            </p>
          ) : (
            <Link
              to="/checkout"
              className="mt-6 block rounded-full bg-brand-500 py-3 text-center font-medium text-white hover:bg-brand-600"
            >
              Proceed to checkout
            </Link>
          )}
          <Link to="/artworks" className="mt-3 block text-center text-sm text-gray-500 hover:text-brand-500">
            Continue shopping
          </Link>
        </aside>
      </div>
    </div>
  )
}

export default Cart

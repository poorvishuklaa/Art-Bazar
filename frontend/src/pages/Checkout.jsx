import { useState } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useCart } from '../hooks/useCart'
import { useAuth } from '../hooks/useAuth'
import { placeOrder } from '../api/orders'
import { getErrorMessage, getFieldErrors } from '../api/client'
import { formatPrice } from '../utils/format'
import FormField from '../components/ui/FormField'
import Alert from '../components/ui/Alert'
import Spinner from '../components/ui/Spinner'
import { usePageTitle } from '../hooks/usePageTitle'

const STATES = [
  'Andhra Pradesh', 'Arunachal Pradesh', 'Assam', 'Bihar', 'Chhattisgarh', 'Goa', 'Gujarat', 'Haryana',
  'Himachal Pradesh', 'Jharkhand', 'Karnataka', 'Kerala', 'Madhya Pradesh', 'Maharashtra', 'Manipur', 'Meghalaya',
  'Mizoram', 'Nagaland', 'Odisha', 'Punjab', 'Rajasthan', 'Sikkim', 'Tamil Nadu', 'Telangana', 'Tripura',
  'Uttar Pradesh', 'Uttarakhand', 'West Bengal', 'Andaman and Nicobar Islands', 'Chandigarh',
  'Dadra and Nagar Haveli and Daman and Diu', 'Delhi', 'Jammu and Kashmir', 'Ladakh', 'Lakshadweep', 'Puducherry',
]

// Mirrors the backend CheckoutRequest rules.
const schema = z.object({
  name: z.string().trim().min(2, 'Enter the recipient’s name').max(100),
  phone: z.string().trim().regex(/^[6-9]\d{9}$/, 'Enter a 10-digit Indian mobile number'),
  address: z.string().trim().min(5, 'Enter the full address').max(500),
  city: z.string().trim().min(2, 'Enter the city').max(100),
  state: z.string().min(1, 'Choose a state'),
  pincode: z.string().trim().regex(/^[1-9]\d{5}$/, 'Enter a valid 6-digit PIN code'),
})

const Checkout = () => {
  usePageTitle('Checkout')
  const { user } = useAuth()
  const { cart, isPending } = useCart()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [serverError, setServerError] = useState('')

  const { register, handleSubmit, setError, formState: { errors } } = useForm({
    resolver: zodResolver(schema),
    defaultValues: { name: user.name, phone: '', address: '', city: '', state: '', pincode: '' },
  })

  const order = useMutation({
    mutationFn: placeOrder,
    onSuccess: (placed) => {
      queryClient.setQueryData(['cart'], { items: [], itemCount: 0, subtotal: 0, readyForCheckout: false })
      queryClient.invalidateQueries({ queryKey: ['orders'] })
      queryClient.invalidateQueries({ queryKey: ['artworks'] }) // stock changed
      queryClient.setQueryData(['order', String(placed.id)], placed)
      navigate(`/orders/${placed.id}/pay`, { replace: true })
    },
    onError: (err) => {
      const fields = getFieldErrors(err)
      Object.entries(fields).forEach(([f, message]) => setError(f, { message }))
      if (Object.keys(fields).length === 0) setServerError(getErrorMessage(err))
      queryClient.invalidateQueries({ queryKey: ['cart'] }) // show which items changed
    },
  })

  if (isPending) {
    return (
      <div className="grid place-items-center py-32">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }
  if (!order.isSuccess && (!cart || cart.items.length === 0)) return <Navigate to="/cart" replace />

  return (
    <div className="py-10">
      <Link to="/cart" className="text-sm text-gray-500 hover:text-brand-500">← Back to cart</Link>
      <h1 className="mt-3 text-3xl font-medium">Checkout</h1>

      <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_360px]">
        <form
          id="checkout-form"
          noValidate
          onSubmit={handleSubmit((values) => {
            setServerError('')
            order.mutate(values)
          })}
          className="space-y-5 rounded-2xl bg-white p-6 ring-1 ring-gray-100"
        >
          <h2 className="text-lg font-medium">Shipping address</h2>
          {serverError && (
            <Alert type="error">
              {serverError}{' '}
              <Link to="/cart" className="font-medium underline">Review your cart</Link>
            </Alert>
          )}
          <div className="grid gap-5 sm:grid-cols-2">
            <FormField label="Full name" autoComplete="name" error={errors.name?.message} {...register('name')} />
            <FormField label="Mobile number" type="tel" inputMode="numeric" autoComplete="tel-national" placeholder="98765 43210" error={errors.phone?.message} {...register('phone')} />
          </div>
          <FormField label="Address" as="textarea" rows={2} autoComplete="street-address" placeholder="House no., street, area" error={errors.address?.message} {...register('address')} />
          <div className="grid gap-5 sm:grid-cols-3">
            <FormField label="City" autoComplete="address-level2" error={errors.city?.message} {...register('city')} />
            <FormField label="State" as="select" autoComplete="address-level1" error={errors.state?.message} {...register('state')}>
              <option value="">Choose…</option>
              {STATES.map((s) => <option key={s}>{s}</option>)}
            </FormField>
            <FormField label="PIN code" inputMode="numeric" autoComplete="postal-code" error={errors.pincode?.message} {...register('pincode')} />
          </div>
        </form>

        <aside className="h-fit rounded-2xl bg-gray-50 p-6 lg:sticky lg:top-24">
          <h2 className="text-lg font-medium">Your order</h2>
          <ul className="mt-4 space-y-3">
            {cart?.items.map(({ artwork: a, quantity, lineTotal }) => (
              <li key={a.id} className="flex items-center gap-3 text-sm">
                <img src={a.imageUrl} alt="" className="h-12 w-12 rounded-lg object-cover" />
                <span className="min-w-0 flex-1">
                  <span className="line-clamp-1">{a.title}</span>
                  <span className="text-gray-500">Qty {quantity}</span>
                </span>
                <span>{formatPrice(lineTotal)}</span>
              </li>
            ))}
          </ul>
          <dl className="mt-4 space-y-2 border-t border-gray-200 pt-4 text-sm">
            <div className="flex justify-between"><dt className="text-gray-600">Shipping</dt><dd className="text-green-700">Free</dd></div>
            <div className="flex justify-between text-base font-semibold"><dt>Total</dt><dd>{formatPrice(cart?.subtotal ?? 0)}</dd></div>
          </dl>
          <button
            type="submit"
            form="checkout-form"
            disabled={order.isPending || !cart?.readyForCheckout}
            className="mt-6 flex w-full items-center justify-center gap-2 rounded-full bg-brand-500 py-3 font-medium text-white hover:bg-brand-600 disabled:opacity-60"
          >
            {order.isPending && <Spinner className="h-4 w-4 border-white border-t-transparent" />}
            Place order &amp; pay
          </button>
          <p className="mt-3 text-center text-xs text-gray-500">Items are reserved for you for 30 minutes while you pay.</p>
        </aside>
      </div>
    </div>
  )
}

export default Checkout

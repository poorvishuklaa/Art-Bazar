import { useState } from 'react'
import { Link, Navigate, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { fetchOrder, payOrder } from '../../api/orders'
import { getErrorMessage } from '../../api/client'
import { formatPrice } from '../../utils/format'
import Alert from '../../components/ui/Alert'
import Spinner from '../../components/ui/Spinner'
import { usePageTitle } from '../../hooks/usePageTitle'

const timeFormat = new Intl.DateTimeFormat('en-IN', { timeStyle: 'short' })

const inputCls =
  'block w-full rounded-lg border border-gray-300 px-3.5 py-2.5 outline-none focus:border-brand-500 focus:ring-2 focus:ring-brand-100'

/** Groups digits as 4-4-4-4 while typing. */
const formatCard = (v) => v.replace(/\D/g, '').slice(0, 16).replace(/(\d{4})(?=\d)/g, '$1 ')
const formatExpiry = (v) => {
  const d = v.replace(/\D/g, '').slice(0, 4)
  return d.length > 2 ? `${d.slice(0, 2)}/${d.slice(2)}` : d
}

/** Simulated payment step. Nothing here talks to a real gateway or stores card details. */
const Payment = () => {
  usePageTitle('Payment')
  const { id } = useParams()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [method, setMethod] = useState('UPI')
  const [upiId, setUpiId] = useState('')
  const [card, setCard] = useState({ number: '', expiry: '', cvv: '' })

  const { data: order, isPending, isError, error } = useQuery({ queryKey: ['order', id], queryFn: () => fetchOrder(id) })

  const pay = useMutation({
    mutationFn: () =>
      payOrder(id, method === 'UPI'
        ? { method, upiId }
        : { method, cardNumber: card.number, cardExpiry: card.expiry, cardCvv: card.cvv }),
    onSuccess: (paid) => {
      navigate(`/orders/${id}`, { replace: true, state: { justPaid: true } })
      queryClient.setQueryData(['order', id], paid)
      queryClient.invalidateQueries({ queryKey: ['orders'] })
    },
    onError: () => queryClient.invalidateQueries({ queryKey: ['order', id] }),
  })

  if (isPending) {
    return (
      <div className="grid place-items-center py-32">
        <Spinner className="h-8 w-8" />
      </div>
    )
  }
  if (isError) return <div className="py-12"><Alert type="error">{getErrorMessage(error, 'Order not found')}</Alert></div>
  // Someone opening the payment page for an order that is already settled. (Not while our own
  // payment is completing: onSuccess navigates with the "just paid" message instead.)
  const settled = order.paymentStatus === 'PAID' || order.status === 'CANCELLED'
  if (settled && !pay.isPending && !pay.isSuccess) return <Navigate to={`/orders/${id}`} replace />

  const tab = (m, label) => (
    <button
      type="button"
      role="tab"
      aria-selected={method === m}
      onClick={() => { setMethod(m); pay.reset() }}
      className={`flex-1 rounded-lg py-2.5 text-sm font-medium transition ${method === m ? 'bg-white shadow-sm ring-1 ring-gray-200' : 'text-gray-500 hover:text-ink'}`}
    >
      {label}
    </button>
  )

  return (
    <div className="mx-auto max-w-lg py-10">
      <p className="text-sm text-gray-500">Order #{order.id}</p>
      <h1 className="mt-1 text-3xl font-medium">Pay {formatPrice(order.totalAmount)}</h1>
      {order.payBy && (
        <p className="mt-2 text-sm text-gray-500">
          Your items are reserved until {timeFormat.format(new Date(order.payBy))}.
        </p>
      )}

      <div className="mt-6">
        <Alert type="info">
          <strong>Test mode:</strong> no real money moves. Any valid-looking details succeed. To see a failed payment, use card{' '}
          <code className="font-mono">4000 0000 0000 0002</code> or UPI ID <code className="font-mono">fail@upi</code>.
        </Alert>
      </div>

      <form
        noValidate
        onSubmit={(e) => {
          e.preventDefault()
          pay.mutate()
        }}
        className="mt-6 space-y-5 rounded-2xl bg-white p-6 ring-1 ring-gray-100"
      >
        <div role="tablist" className="flex gap-1 rounded-xl bg-gray-100 p-1">
          {tab('UPI', 'UPI')}
          {tab('CARD', 'Credit / debit card')}
        </div>

        {pay.isError && <Alert type="error">{getErrorMessage(pay.error)}</Alert>}
        {!pay.isError && order.paymentStatus === 'FAILED' && (
          <Alert type="warning">Your last payment attempt didn&apos;t go through. You can try again.</Alert>
        )}

        {method === 'UPI' ? (
          <label className="block text-sm font-medium text-gray-700">
            UPI ID
            <input className={`${inputCls} mt-1.5 font-normal`} placeholder="yourname@okbank" autoComplete="off" value={upiId} onChange={(e) => setUpiId(e.target.value)} />
          </label>
        ) : (
          <div className="space-y-4">
            <label className="block text-sm font-medium text-gray-700">
              Card number
              <input className={`${inputCls} mt-1.5 font-mono font-normal`} inputMode="numeric" autoComplete="cc-number" placeholder="4242 4242 4242 4242" value={card.number} onChange={(e) => setCard((c) => ({ ...c, number: formatCard(e.target.value) }))} />
            </label>
            <div className="grid grid-cols-2 gap-4">
              <label className="block text-sm font-medium text-gray-700">
                Expiry
                <input className={`${inputCls} mt-1.5 font-normal`} inputMode="numeric" autoComplete="cc-exp" placeholder="MM/YY" value={card.expiry} onChange={(e) => setCard((c) => ({ ...c, expiry: formatExpiry(e.target.value) }))} />
              </label>
              <label className="block text-sm font-medium text-gray-700">
                CVV
                <input className={`${inputCls} mt-1.5 font-normal`} inputMode="numeric" autoComplete="cc-csc" type="password" maxLength={3} placeholder="123" value={card.cvv} onChange={(e) => setCard((c) => ({ ...c, cvv: e.target.value.replace(/\D/g, '') }))} />
              </label>
            </div>
          </div>
        )}

        <button
          type="submit"
          disabled={pay.isPending}
          className="flex w-full items-center justify-center gap-2 rounded-full bg-brand-500 py-3.5 font-medium text-white hover:bg-brand-600 disabled:opacity-60"
        >
          {pay.isPending && <Spinner className="h-4 w-4 border-white border-t-transparent" />}
          Pay {formatPrice(order.totalAmount)}
        </button>
      </form>

      <p className="mt-6 text-center text-sm text-gray-500">
        Changed your mind? <Link to={`/orders/${id}`} className="text-brand-500 hover:underline">View or cancel this order</Link>
      </p>
    </div>
  )
}

export default Payment

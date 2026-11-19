import { describeOrder } from '../../utils/orderStatus'

const tones = {
  gray: 'bg-gray-100 text-gray-600 ring-gray-200',
  red: 'bg-red-50 text-red-700 ring-red-200',
  amber: 'bg-amber-50 text-amber-800 ring-amber-200',
  sky: 'bg-sky-50 text-sky-800 ring-sky-200',
  green: 'bg-green-50 text-green-700 ring-green-200',
}

const OrderStatusBadge = ({ order }) => {
  const { label, tone } = describeOrder(order)
  return <span className={`rounded-full px-2.5 py-0.5 text-xs font-medium ring-1 ${tones[tone]}`}>{label}</span>
}

export default OrderStatusBadge

/** Human-friendly label + colour for an order's combined status. */
export function describeOrder(order) {
  if (order.status === 'CANCELLED') {
    return order.paymentStatus === 'REFUNDED'
      ? { label: 'Cancelled · refunded', tone: 'gray' }
      : { label: 'Cancelled', tone: 'gray' }
  }
  if (order.status === 'PLACED') {
    return order.paymentStatus === 'FAILED'
      ? { label: 'Payment failed', tone: 'red' }
      : { label: 'Awaiting payment', tone: 'amber' }
  }
  const labels = { PAID: 'Paid · preparing', PARTIALLY_SHIPPED: 'Partly shipped', SHIPPED: 'Shipped', DELIVERED: 'Delivered' }
  return { label: labels[order.status] ?? order.status, tone: order.status === 'DELIVERED' ? 'green' : 'sky' }
}

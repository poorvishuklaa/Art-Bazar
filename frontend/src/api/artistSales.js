import api from './client'

export const fetchArtistOrders = (filter = 'to_ship', page = 0) =>
  api.get('/artist/orders', { params: { filter, page, size: 10 } }).then((r) => r.data)
export const updateFulfilment = (orderId, status) =>
  api.patch(`/artist/orders/${orderId}/fulfilment`, { status }).then((r) => r.data)
export const fetchSalesSummary = () => api.get('/artist/sales/summary').then((r) => r.data)

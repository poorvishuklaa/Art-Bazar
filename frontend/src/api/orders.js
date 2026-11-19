import api from './client'

export const placeOrder = (shipping) => api.post('/orders', shipping).then((r) => r.data)
export const payOrder = (id, payment) => api.post(`/orders/${id}/pay`, payment).then((r) => r.data)
export const cancelOrder = (id) => api.post(`/orders/${id}/cancel`).then((r) => r.data)
export const fetchOrders = (page = 0) => api.get('/orders', { params: { page, size: 10 } }).then((r) => r.data)
export const fetchOrder = (id) => api.get(`/orders/${id}`).then((r) => r.data)

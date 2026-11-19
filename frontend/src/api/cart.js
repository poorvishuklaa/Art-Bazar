import api from './client'

export const fetchCart = () => api.get('/cart').then((r) => r.data)
export const addToCart = (artworkId, quantity = 1) => api.post('/cart/items', { artworkId, quantity }).then((r) => r.data)
export const updateCartItem = (artworkId, quantity) => api.put(`/cart/items/${artworkId}`, { quantity }).then((r) => r.data)
export const removeCartItem = (artworkId) => api.delete(`/cart/items/${artworkId}`).then((r) => r.data)

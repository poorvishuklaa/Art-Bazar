import api from './client'

export const fetchReviews = (artworkId, page = 0) =>
  api.get(`/artworks/${artworkId}/reviews`, { params: { page, size: 5 } }).then((r) => r.data)
export const fetchMyReview = (artworkId) => api.get(`/artworks/${artworkId}/reviews/me`).then((r) => r.data)
export const saveMyReview = (artworkId, review) => api.put(`/artworks/${artworkId}/reviews/me`, review).then((r) => r.data)
export const deleteMyReview = (artworkId) => api.delete(`/artworks/${artworkId}/reviews/me`)
export const adminDeleteReview = (reviewId) => api.delete(`/admin/reviews/${reviewId}`)

import api from './client'

export const fetchArtistApplications = (approved) =>
  api.get('/admin/artists', { params: approved === undefined ? {} : { approved } }).then((r) => r.data)

export const setArtistApproval = (userId, approved) =>
  api.patch(`/admin/artists/${userId}/approval`, { approved }).then((r) => r.data)

const clean = (p) => Object.fromEntries(Object.entries(p).filter(([, v]) => v !== '' && v != null))
const multipart = { headers: { 'Content-Type': undefined } }

export const fetchAdminStats = () => api.get('/admin/stats').then((r) => r.data)
export const fetchAdminUsers = (params) => api.get('/admin/users', { params: clean(params) }).then((r) => r.data)
export const setUserEnabled = (id, enabled) => api.patch(`/admin/users/${id}/status`, { enabled }).then((r) => r.data)
export const fetchAdminArtworks = (params) => api.get('/admin/artworks', { params: clean(params) }).then((r) => r.data)
export const setAdminArtworkStatus = (id, status) => api.patch(`/admin/artworks/${id}/status`, { status }).then((r) => r.data)
export const fetchAdminCategories = () => api.get('/admin/categories').then((r) => r.data)
const categoryForm = (name, icon) => {
  const f = new FormData()
  f.append('name', name)
  if (icon) f.append('icon', icon)
  return f
}
export const createCategory = (name, icon) => api.post('/admin/categories', categoryForm(name, icon), multipart).then((r) => r.data)
export const updateCategory = (id, name, icon) => api.put(`/admin/categories/${id}`, categoryForm(name, icon), multipart).then((r) => r.data)
export const deleteCategory = (id) => api.delete(`/admin/categories/${id}`)
export const fetchAdminOrders = (params) => api.get('/admin/orders', { params: clean(params) }).then((r) => r.data)

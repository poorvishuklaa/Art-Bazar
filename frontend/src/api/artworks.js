import api from './client'

const clean = (params) =>
  Object.fromEntries(Object.entries(params).filter(([, v]) => v !== undefined && v !== null && v !== ''))

// ---- public catalog ----
export const searchArtworks = (params) => api.get('/artworks', { params: clean(params) }).then((r) => r.data)
export const fetchArtwork = (id) => api.get(`/artworks/${id}`).then((r) => r.data)
export const fetchArtist = (id) => api.get(`/artists/${id}`).then((r) => r.data)

// ---- signed-in artist ----
export const fetchMyArtworks = (page = 0, size = 24) =>
  api.get('/artist/artworks', { params: { page, size } }).then((r) => r.data)
export const fetchMyArtwork = (id) => api.get(`/artist/artworks/${id}`).then((r) => r.data)

/** values: { title, description, price, stock, categoryId, status }, image: File | undefined */
const toFormData = (values, image) => {
  const form = new FormData()
  Object.entries(clean(values)).forEach(([k, v]) => form.append(k, v))
  if (image) form.append('image', image)
  return form
}

// Let the browser set the multipart boundary.
const multipart = { headers: { 'Content-Type': undefined } }

export const createArtwork = (values, image) =>
  api.post('/artist/artworks', toFormData(values, image), multipart).then((r) => r.data)
export const updateArtwork = (id, values, image) =>
  api.put(`/artist/artworks/${id}`, toFormData(values, image), multipart).then((r) => r.data)
export const setArtworkStatus = (id, status) => api.patch(`/artist/artworks/${id}/status`, { status }).then((r) => r.data)
export const deleteArtwork = (id) => api.delete(`/artist/artworks/${id}`)

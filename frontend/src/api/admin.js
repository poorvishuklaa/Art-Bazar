import api from './client'

export const fetchArtistApplications = (approved) =>
  api.get('/admin/artists', { params: approved === undefined ? {} : { approved } }).then((r) => r.data)

export const setArtistApproval = (userId, approved) =>
  api.patch(`/admin/artists/${userId}/approval`, { approved }).then((r) => r.data)

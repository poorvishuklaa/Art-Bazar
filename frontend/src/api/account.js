import api from './client'

export const fetchAccount = () => api.get('/account').then((r) => r.data)
export const updateAccount = (profile) => api.put('/account', profile).then((r) => r.data)
export const changePassword = (currentPassword, newPassword) =>
  api.post('/account/password', { currentPassword, newPassword }).then((r) => r.data)

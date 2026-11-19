import api from './client'

export const registerRequest = (data) => api.post('/auth/register', data).then((r) => r.data)
export const loginRequest = (data) => api.post('/auth/login', data).then((r) => r.data)
export const logoutRequest = () => api.post('/auth/logout')
export const fetchMe = () => api.get('/auth/me').then((r) => r.data)

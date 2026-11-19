import axios from 'axios'

/**
 * Single axios instance for the whole app. Phase 1 adds the JWT request
 * interceptor and the refresh-on-401 response interceptor here.
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  headers: { 'Content-Type': 'application/json' },
  timeout: 15000,
})

/** Turns an axios error into a message that is safe to show the user. */
export function getErrorMessage(error, fallback = 'Something went wrong. Please try again.') {
  return error?.response?.data?.message ?? fallback
}

export default api

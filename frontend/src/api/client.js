import axios from 'axios'

/**
 * One axios instance for the whole app.
 *
 * - The access token lives only in memory (never localStorage), so injected scripts can't steal it from storage.
 * - The refresh token is an HttpOnly cookie the browser sends to /api/auth/* automatically.
 * - On a 401 the client refreshes once and replays the request; parallel 401s share one refresh call.
 */
const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ?? '/api',
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true,
  timeout: 15000,
})

let accessToken = null
let refreshPromise = null
let onSessionExpired = () => {}

export const setAccessToken = (token) => {
  accessToken = token
}

/** AuthContext registers this so a failed refresh signs the user out of the UI. */
export const setSessionExpiredHandler = (handler) => {
  onSessionExpired = handler
}

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

async function requestRefresh() {
  try {
    return (await api.post('/auth/refresh', null, { skipAuthRefresh: true })).data
  } catch (error) {
    // Another tab rotated the cookie a moment ago; its replacement is already in the cookie jar.
    if (error.response?.data?.code === 'REFRESH_RACE') {
      await wait(400)
      return (await api.post('/auth/refresh', null, { skipAuthRefresh: true })).data
    }
    throw error
  }
}

/** Exchanges the refresh cookie for a new access token. Concurrent callers share one request. */
export function refreshSession() {
  if (!refreshPromise) {
    refreshPromise = requestRefresh()
      .then((data) => {
        setAccessToken(data.accessToken)
        return data
      })
      .finally(() => {
        refreshPromise = null
      })
  }
  return refreshPromise
}

api.interceptors.request.use((config) => {
  if (accessToken && !config.headers.Authorization) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const { config, response } = error
    const isAuthCall = config?.url?.startsWith('/auth/')
    if (response?.status !== 401 || !config || config.skipAuthRefresh || config._retried || isAuthCall) {
      throw error
    }
    try {
      await refreshSession()
    } catch {
      setAccessToken(null)
      onSessionExpired()
      throw error
    }
    config._retried = true
    config.headers.Authorization = `Bearer ${accessToken}`
    return api(config)
  },
)

/** Turns an axios error into a message that is safe to show the user. */
export function getErrorMessage(error, fallback = 'Something went wrong. Please try again.') {
  if (!error?.response) return 'Cannot reach the server. Check your connection and try again.'
  return error.response.data?.message ?? fallback
}

/** Field-level validation errors from the API ({ email: '...', ... }), if any. */
export const getFieldErrors = (error) => error?.response?.data?.fieldErrors ?? {}

export default api

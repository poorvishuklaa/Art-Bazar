import { useCallback, useEffect, useMemo, useState } from 'react'
import { AuthContext } from './auth-context'
import { fetchMe, loginRequest, logoutRequest, registerRequest } from '../api/auth'
import { refreshSession, setAccessToken, setSessionExpiredHandler } from '../api/client'

/**
 * Holds the signed-in user. On first load it tries the refresh cookie, so a page reload
 * keeps the user signed in without ever storing a token in localStorage.
 */
export default function AuthProvider({ children }) {
  const [user, setUser] = useState(null)
  const [status, setStatus] = useState('loading') // loading | authenticated | anonymous

  const applySession = useCallback((data) => {
    setAccessToken(data.accessToken)
    setUser(data.user)
    setStatus('authenticated')
    return data.user
  }, [])

  const clearSession = useCallback(() => {
    setAccessToken(null)
    setUser(null)
    setStatus('anonymous')
  }, [])

  useEffect(() => {
    setSessionExpiredHandler(clearSession)
    refreshSession()
      .then((data) => applySession(data))
      .catch(() => clearSession())
  }, [applySession, clearSession])

  const login = useCallback((credentials) => loginRequest(credentials).then(applySession), [applySession])
  const register = useCallback((details) => registerRequest(details).then(applySession), [applySession])

  const logout = useCallback(async () => {
    try {
      await logoutRequest()
    } finally {
      clearSession()
    }
  }, [clearSession])

  /** Re-reads the user, e.g. to pick up an admin approving this artist. */
  const reloadUser = useCallback(() => fetchMe().then((me) => (setUser(me), me)), [])

  const value = useMemo(
    () => ({ user, status, isAuthenticated: status === 'authenticated', login, register, logout, reloadUser }),
    [user, status, login, register, logout, reloadUser],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

import { createContext, useContext, useState, useCallback, ReactNode } from 'react'
import type { UserDto } from './types'

// ── Types ──────────────────────────────────────────────────────────────────────

interface AuthCtx {
  user: UserDto | null
  login:  (user: UserDto) => void
  logout: () => void
}

// ── Storage helpers ────────────────────────────────────────────────────────────

const SESSION_KEY = 'bpmn_user'

function loadSession(): UserDto | null {
  try {
    const raw = sessionStorage.getItem(SESSION_KEY)
    return raw ? (JSON.parse(raw) as UserDto) : null
  } catch {
    return null
  }
}

function saveSession(user: UserDto) {
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(user))
}

function clearSession() {
  sessionStorage.removeItem(SESSION_KEY)
}

// ── Context ────────────────────────────────────────────────────────────────────

const AuthContext = createContext<AuthCtx>({
  user: null,
  login:  () => {},
  logout: () => {},
})

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<UserDto | null>(loadSession)

  const login = useCallback((u: UserDto) => {
    saveSession(u)
    setUser(u)
  }, [])

  const logout = useCallback(() => {
    clearSession()
    setUser(null)
  }, [])

  return (
    <AuthContext.Provider value={{ user, login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export const useAuth = () => useContext(AuthContext)
export default AuthContext

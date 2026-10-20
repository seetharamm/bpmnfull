import { useState, FormEvent } from 'react'
import toast from 'react-hot-toast'
import { usersApi } from '../api'
import { useAuth } from '../AuthContext'
import Spinner from '../components/Spinner'

export default function LoginPage() {
  const { login } = useAuth()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy]         = useState(false)

  const submit = async (e: FormEvent) => {
    e.preventDefault()
    if (!username.trim() || !password.trim()) {
      toast.error('Username and password are required')
      return
    }
    setBusy(true)
    try {
      // 1. Verify credentials
      const valid = await usersApi.verifyPassword(username.trim(), password)
      if (!valid) {
        toast.error('Invalid username or password')
        return
      }
      // 2. Load full user profile (includes roles, groups, effectiveRoles)
      const user = await usersApi.getByUsername(username.trim())
      login(user)
      toast.success(`Welcome, ${user.displayName ?? user.username}!`)
    } catch (err: any) {
      toast.error(err?.message ?? 'Login failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="login-screen">
      <div className="login-card">

        {/* Logo / branding */}
        <div className="login-logo">
          <span className="login-logo-icon">⚙</span>
          <span className="login-logo-text">BPMN Engine</span>
        </div>

        <h2 className="login-title">Sign in to your account</h2>
        <p className="login-subtitle">Use your existing engine credentials</p>

        <form onSubmit={submit} className="login-form" noValidate>
          <div className="form-group">
            <label htmlFor="login-username">Username</label>
            <input
              id="login-username"
              className="form-control"
              type="text"
              autoComplete="username"
              placeholder="e.g. alice"
              value={username}
              onChange={e => setUsername(e.target.value)}
              disabled={busy}
              autoFocus
            />
          </div>

          <div className="form-group">
            <label htmlFor="login-password">Password</label>
            <input
              id="login-password"
              className="form-control"
              type="password"
              autoComplete="current-password"
              placeholder="••••••••"
              value={password}
              onChange={e => setPassword(e.target.value)}
              disabled={busy}
            />
          </div>

          <button
            type="submit"
            className="btn btn-primary login-submit"
            disabled={busy}
          >
            {busy ? <Spinner size={16} /> : null}
            {busy ? 'Signing in…' : 'Sign in'}
          </button>
        </form>

        <div className="login-hint">
          <span>Demo accounts: </span>
          <code>alice</code>, <code>bob</code>, <code>carol</code> — password <code>Alice@123</code>
        </div>
      </div>
    </div>
  )
}

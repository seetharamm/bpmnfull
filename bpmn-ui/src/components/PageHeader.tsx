import { useState, useRef, useEffect } from 'react'
import { useAuth } from '../AuthContext'
import { useSidebar } from '../SidebarContext'

interface Props {
  title: string
  children?: React.ReactNode
  actions?: React.ReactNode
}

export default function PageHeader({ title, children, actions }: Props) {
  const { open, toggle }    = useSidebar()
  const { user, logout }    = useAuth()
  const [menuOpen, setMenuOpen] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)

  // Close dropdown when clicking outside
  useEffect(() => {
    if (!menuOpen) return
    const handler = (e: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(e.target as Node)) {
        setMenuOpen(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [menuOpen])

  // First effective role for display (e.g. "ADMIN", "HR_MANAGER")
  const primaryRole = user
    ? (user.effectiveRoles?.[0] ?? user.roles?.[0] ?? null)
    : null

  // Initials for avatar
  const initials = user
    ? ((user.firstName?.[0] ?? '') + (user.lastName?.[0] ?? '')).toUpperCase() || user.username.slice(0, 2).toUpperCase()
    : ''

  return (
    <div className="topbar">
      {/* Hamburger toggle */}
      <button
        className={`sidebar-toggle${open ? '' : ' open'}`}
        onClick={toggle}
        title={open ? 'Collapse sidebar' : 'Expand sidebar'}
        aria-label="Toggle sidebar"
      >
        <span /><span /><span />
      </button>

      <h1>{title}</h1>
      {children}
      {actions}

      {/* ── User chip (right side) ── */}
      {user && (
        <div className="topbar-user" ref={menuRef}>
          <button
            className="topbar-user-btn"
            onClick={() => setMenuOpen(v => !v)}
            title="Account menu"
          >
            {/* Avatar circle with initials */}
            <span className="topbar-avatar">{initials}</span>

            <span className="topbar-user-info">
              <span className="topbar-user-name">
                {user.displayName ?? user.username}
              </span>
              {primaryRole && (
                <span className="topbar-user-role">
                  {primaryRole.replace(/_/g, ' ')}
                </span>
              )}
            </span>

            {/* Chevron */}
            <svg className={`topbar-chevron${menuOpen ? ' up' : ''}`}
              width="12" height="12" viewBox="0 0 12 12" fill="none">
              <path d="M2 4l4 4 4-4" stroke="currentColor" strokeWidth="1.5"
                strokeLinecap="round" strokeLinejoin="round" />
            </svg>
          </button>

          {/* Dropdown menu */}
          {menuOpen && (
            <div className="topbar-user-menu">
              <div className="topbar-user-menu-header">
                <div className="topbar-user-menu-name">
                  {user.displayName ?? user.username}
                </div>
                <div className="topbar-user-menu-email">{user.email}</div>
                {user.effectiveRoles && user.effectiveRoles.length > 0 && (
                  <div className="topbar-user-menu-roles">
                    {user.effectiveRoles.map(r => (
                      <span key={r} className="topbar-role-tag">
                        {r.replace(/_/g, ' ')}
                      </span>
                    ))}
                  </div>
                )}
              </div>
              <div className="topbar-user-menu-divider" />
              <button className="topbar-user-menu-item danger" onClick={() => { setMenuOpen(false); logout() }}>
                ⎋ Sign out
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  )
}

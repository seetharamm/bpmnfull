import { useState } from 'react'
import { NavLink, Routes, Route, Navigate } from 'react-router-dom'
import { AuthProvider, useAuth } from './AuthContext'
import SidebarContext from './SidebarContext'
import LoginPage        from './pages/LoginPage'
import DefinitionsPage  from './pages/DefinitionsPage'
import InstancesPage    from './pages/InstancesPage'
import TasksPage        from './pages/TasksPage'
import UsersPage        from './pages/UsersPage'
import RolesPage        from './pages/RolesPage'
import GroupsPage       from './pages/GroupsPage'
import EventsPage       from './pages/EventsPage'
import BioRequestPage   from './pages/BioRequestPage'

const nav = [
  { group: 'Applications', items: [
    { to: '/bio-request', label: 'Bio Request', icon: '🧬' },
  ]},
  { group: 'Engine', items: [
    { to: '/definitions', label: 'Definitions',       icon: '⚙' },
    { to: '/instances',   label: 'Instances',          icon: '▶' },
    { to: '/tasks',       label: 'Tasks',               icon: '✔' },
    { to: '/events',      label: 'Messages & Signals',  icon: '✉' },
  ]},
  { group: 'Identity', items: [
    { to: '/users',  label: 'Users',  icon: '👤' },
    { to: '/roles',  label: 'Roles',  icon: '🔑' },
    { to: '/groups', label: 'Groups', icon: '👥' },
  ]},
]

// ── Protected shell (sidebar + routes) ─────────────────────────────────────────

function Shell() {
  const { user, logout } = useAuth()
  const [open, setOpen]  = useState(true)
  const toggle = () => setOpen(v => !v)

  if (!user) return <Navigate to="/login" replace />

  return (
    <SidebarContext.Provider value={{ open, toggle }}>
      <div className="app">

        {/* ── Sidebar ── */}
        <nav className={`sidebar${open ? '' : ' collapsed'}`}>
          <div className="sidebar-logo">
            <span>⚙</span> BPMN Engine
          </div>

          {nav.map(g => (
            <div key={g.group} className="sidebar-group">
              <div className="sidebar-group-label">{g.group}</div>
              {g.items.map(item => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  className={({ isActive }) => 'sidebar-item' + (isActive ? ' active' : '')}
                >
                  <span style={{ fontSize: 15 }}>{item.icon}</span>
                  {item.label}
                </NavLink>
              ))}
            </div>
          ))}

          {/* Sidebar footer: user info + logout */}
          <div className="sidebar-footer">
            <div className="sidebar-user-name">
              {user.displayName ?? user.username}
            </div>
            <button className="sidebar-logout" onClick={logout} title="Sign out">
              ⎋ Sign out
            </button>
          </div>
        </nav>

        {/* ── Main content ── */}
        <div className={`main-content${open ? '' : ' sidebar-collapsed'}`}>
          <Routes>
            <Route path="/"            element={<Navigate to="/bio-request" replace />} />
            <Route path="/bio-request" element={<BioRequestPage />} />
            <Route path="/definitions" element={<DefinitionsPage />} />
            <Route path="/instances"   element={<InstancesPage />} />
            <Route path="/tasks"       element={<TasksPage />} />
            <Route path="/events"      element={<EventsPage />} />
            <Route path="/users"       element={<UsersPage />} />
            <Route path="/roles"       element={<RolesPage />} />
            <Route path="/groups"      element={<GroupsPage />} />
            {/* Catch-all: redirect to bio-request */}
            <Route path="*"            element={<Navigate to="/bio-request" replace />} />
          </Routes>
        </div>

      </div>
    </SidebarContext.Provider>
  )
}

// ── Root App ───────────────────────────────────────────────────────────────────

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginRoute />} />
        <Route path="/*"     element={<Shell />} />
      </Routes>
    </AuthProvider>
  )
}

/** Redirect already-logged-in users away from /login */
function LoginRoute() {
  const { user } = useAuth()
  return user ? <Navigate to="/bio-request" replace /> : <LoginPage />
}

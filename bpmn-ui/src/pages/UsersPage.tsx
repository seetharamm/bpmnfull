import { useState, useEffect, useCallback } from 'react'
import toast from 'react-hot-toast'
import { usersApi, rolesApi, groupsApi } from '../api'
import type { UserDto, RoleDto, GroupDto, CreateUserRequest, UpdateUserRequest } from '../types'
import PageHeader from '../components/PageHeader'
import Modal from '../components/Modal'
import Spinner from '../components/Spinner'

// ── Create / Edit Modal ───────────────────────────────────────────────────────
function UserFormModal({
  user, allRoles, allGroups, onClose, onSaved,
}: {
  user?: UserDto
  allRoles: RoleDto[]
  allGroups: GroupDto[]
  onClose: () => void
  onSaved: () => void
}) {
  const editing = !!user
  const [form, setForm] = useState<CreateUserRequest & UpdateUserRequest>({
    username: user?.username ?? '',
    email: user?.email ?? '',
    password: '',
    firstName: user?.firstName ?? '',
    lastName: user?.lastName ?? '',
    displayName: user?.displayName ?? '',
    roleNames: user?.roles ?? [],
    groupNames: user?.groups ?? [],
    enabled: user?.enabled ?? true,
    locked: user?.locked ?? false,
  })
  const [busy, setBusy] = useState(false)

  const toggle = (field: 'roleNames' | 'groupNames', val: string) => {
    setForm(f => {
      const arr = (f[field] ?? []) as string[]
      return { ...f, [field]: arr.includes(val) ? arr.filter(x => x !== val) : [...arr, val] }
    })
  }

  const submit = async () => {
    setBusy(true)
    try {
      if (editing) {
        await usersApi.update(user!.id, {
          email: form.email, firstName: form.firstName, lastName: form.lastName,
          displayName: form.displayName, enabled: form.enabled, locked: form.locked,
          newPassword: form.password || undefined,
        })
      } else {
        if (!form.username || !form.email || !form.password) { toast.error('Username, email and password are required'); setBusy(false); return }
        await usersApi.create(form as CreateUserRequest)
      }
      toast.success(editing ? 'User updated' : 'User created')
      onSaved(); onClose()
    } catch (e: any) { toast.error(e.message) }
    finally { setBusy(false) }
  }

  const F = (label: string, key: keyof typeof form, type = 'text', placeholder = '') => (
    <div className="form-group">
      <label>{label}{!editing && ['username', 'email', 'password'].includes(key) ? ' *' : ''}</label>
      <input className="form-control" type={type} placeholder={placeholder}
        value={String(form[key] ?? '')} onChange={e => setForm(f => ({ ...f, [key]: e.target.value }))} />
    </div>
  )

  return (
    <Modal title={editing ? `Edit ${user?.username}` : 'Create User'} onClose={onClose} wide
      footer={<>
        <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" onClick={submit} disabled={busy}>
          {busy && <Spinner size={14} />} {editing ? 'Save' : 'Create'}
        </button>
      </>}>
      {F('Username', 'username')} {F('Email', 'email', 'email')}
      {F('Password' + (editing ? ' (leave blank to keep)' : ''), 'password', 'password')}
      {F('First name', 'firstName')} {F('Last name', 'lastName')} {F('Display name', 'displayName')}
      {editing && (
        <div className="form-group gap-8">
          <label style={{ margin: 0 }}>
            <input type="checkbox" checked={!!form.enabled} onChange={e => setForm(f => ({ ...f, enabled: e.target.checked }))} />
            {' '}Enabled
          </label>
          <label style={{ margin: 0 }}>
            <input type="checkbox" checked={!!form.locked} onChange={e => setForm(f => ({ ...f, locked: e.target.checked }))} />
            {' '}Locked
          </label>
        </div>
      )}
      {!editing && (
        <>
          <div className="form-group">
            <label>Roles</label>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
              {allRoles.map(r => (
                <label key={r.id} style={{ fontSize: 13, display: 'flex', alignItems: 'center', gap: 4 }}>
                  <input type="checkbox" checked={(form.roleNames ?? []).includes(r.name)}
                    onChange={() => toggle('roleNames', r.name)} /> {r.name}
                </label>
              ))}
            </div>
          </div>
          <div className="form-group">
            <label>Groups</label>
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
              {allGroups.map(g => (
                <label key={g.id} style={{ fontSize: 13, display: 'flex', alignItems: 'center', gap: 4 }}>
                  <input type="checkbox" checked={(form.groupNames ?? []).includes(g.name)}
                    onChange={() => toggle('groupNames', g.name)} /> {g.name}
                </label>
              ))}
            </div>
          </div>
        </>
      )}
    </Modal>
  )
}

// ── User Detail Modal ─────────────────────────────────────────────────────────
function UserDetailModal({ user, onClose, onChanged }: { user: UserDto; onClose: () => void; onChanged: () => void }) {
  const [allRoles, setAllRoles] = useState<RoleDto[]>([])
  const [allGroups, setAllGroups] = useState<GroupDto[]>([])
  const [addRole, setAddRole] = useState('')
  const [addGroup, setAddGroup] = useState('')

  useEffect(() => {
    Promise.all([rolesApi.list(), groupsApi.list()]).then(([r, g]) => { setAllRoles(r); setAllGroups(g) })
  }, [])

  const assignRole = async (name: string) => {
    try { await usersApi.assignRole(user.id, name); toast.success(`Role ${name} assigned`); onChanged() }
    catch (e: any) { toast.error(e.message) }
  }
  const revokeRole = async (name: string) => {
    try { await usersApi.revokeRole(user.id, name); toast.success(`Role ${name} revoked`); onChanged() }
    catch (e: any) { toast.error(e.message) }
  }
  const joinGroup = async () => {
    if (!addGroup) return
    try { await usersApi.joinGroup(user.id, addGroup); toast.success(`Joined ${addGroup}`); setAddGroup(''); onChanged() }
    catch (e: any) { toast.error(e.message) }
  }
  const leaveGroup = async (name: string) => {
    try { await usersApi.leaveGroup(user.id, name); toast.success(`Left ${name}`); onChanged() }
    catch (e: any) { toast.error(e.message) }
  }

  return (
    <Modal title={`User: ${user.username}`} onClose={onClose} wide>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
        <div>
          <p><b>Email:</b> {user.email}</p>
          <p><b>Name:</b> {[user.firstName, user.lastName].filter(Boolean).join(' ') || '—'}</p>
          <p><b>Display:</b> {user.displayName ?? '—'}</p>
          <p><b>Enabled:</b> {user.enabled ? '✅' : '❌'} &nbsp;<b>Locked:</b> {user.locked ? '🔒' : '—'}</p>
        </div>
        <div>
          <p className="text-muted" style={{ fontSize: 12 }}>Effective roles:</p>
          <div>{(user.effectiveRoles ?? []).map(r => <span key={r} className="tag">{r}</span>)}</div>
        </div>
      </div>

      <hr style={{ margin: '16px 0', borderColor: 'var(--border)' }} />

      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
        <div>
          <b style={{ fontSize: 12 }}>Direct roles</b>
          <div className="mt-8">
            {user.roles.map(r => (
              <span key={r} className="tag" style={{ marginBottom: 4 }}>
                {r} <button style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--danger)', fontSize: 11 }}
                  onClick={() => revokeRole(r)}>✕</button>
              </span>
            ))}
          </div>
          <div className="gap-8 mt-8">
            <select className="form-control" value={addRole} onChange={e => setAddRole(e.target.value)}>
              <option value="">Add role…</option>
              {allRoles.filter(r => !user.roles.includes(r.name)).map(r => <option key={r.id} value={r.name}>{r.name}</option>)}
            </select>
            <button className="btn btn-ghost btn-sm" onClick={() => addRole && assignRole(addRole)}>+</button>
          </div>
        </div>
        <div>
          <b style={{ fontSize: 12 }}>Groups</b>
          <div className="mt-8">
            {user.groups.map(g => (
              <span key={g} className="tag" style={{ marginBottom: 4 }}>
                {g} <button style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--danger)', fontSize: 11 }}
                  onClick={() => leaveGroup(g)}>✕</button>
              </span>
            ))}
          </div>
          <div className="gap-8 mt-8">
            <select className="form-control" value={addGroup} onChange={e => setAddGroup(e.target.value)}>
              <option value="">Join group…</option>
              {allGroups.filter(g => !user.groups.includes(g.name)).map(g => <option key={g.id} value={g.name}>{g.name}</option>)}
            </select>
            <button className="btn btn-ghost btn-sm" onClick={joinGroup}>+</button>
          </div>
        </div>
      </div>
    </Modal>
  )
}

// ── Main Page ─────────────────────────────────────────────────────────────────
export default function UsersPage() {
  const [users, setUsers] = useState<UserDto[]>([])
  const [allRoles, setAllRoles] = useState<RoleDto[]>([])
  const [allGroups, setAllGroups] = useState<GroupDto[]>([])
  const [loading, setLoading] = useState(true)
  const [page, setPage] = useState(0)
  const [total, setTotal] = useState(0)
  const [showCreate, setShowCreate] = useState(false)
  const [editUser, setEditUser] = useState<UserDto | null>(null)
  const [detailUser, setDetailUser] = useState<UserDto | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [paged, roles, groups] = await Promise.all([usersApi.list(page), rolesApi.list(), groupsApi.list()])
      setUsers(paged.content)
      setTotal(paged.totalElements)
      setAllRoles(roles)
      setAllGroups(groups)
    } catch (e: any) { toast.error(e.message) }
    finally { setLoading(false) }
  }, [page])

  useEffect(() => { load() }, [load])

  const deleteUser = async (u: UserDto) => {
    if (!confirm(`Delete user "${u.username}"?`)) return
    try { await usersApi.delete(u.id); toast.success('User deleted'); load() }
    catch (e: any) { toast.error(e.message) }
  }

  const refreshDetail = async () => {
    if (!detailUser) return
    try { setDetailUser(await usersApi.getById(detailUser.id)) }
    catch { /* ignore */ }
    load()
  }

  return (
    <>
      <PageHeader title={`Users (${total})`}
        actions={<button className="btn btn-primary" onClick={() => setShowCreate(true)}>+ New User</button>} />
      <div className="page">
        <div className="card" style={{ padding: 0 }}>
          {loading ? (
            <div className="empty-state"><Spinner /></div>
          ) : users.length === 0 ? (
            <div className="empty-state"><p>No users found.</p></div>
          ) : (
            <>
              <div className="table-wrap">
                <table>
                  <thead><tr>
                    <th>Username</th><th>Email</th><th>Name</th><th>Status</th>
                    <th>Groups</th><th>Direct roles</th><th>Actions</th>
                  </tr></thead>
                  <tbody>
                    {users.map(u => (
                      <tr key={u.id}>
                        <td><b>{u.username}</b></td>
                        <td className="text-muted">{u.email}</td>
                        <td>{[u.firstName, u.lastName].filter(Boolean).join(' ') || '—'}</td>
                        <td>
                          {u.enabled
                            ? <span className="badge badge-green">enabled</span>
                            : <span className="badge badge-gray">disabled</span>}
                          {u.locked && <span className="badge badge-red" style={{ marginLeft: 4 }}>locked</span>}
                        </td>
                        <td>{u.groups.map(g => <span key={g} className="tag">{g}</span>)}</td>
                        <td>{u.roles.map(r => <span key={r} className="tag">{r}</span>)}</td>
                        <td>
                          <div className="btn-row">
                            <button className="btn btn-ghost btn-sm" onClick={() => setDetailUser(u)}>Details</button>
                            <button className="btn btn-ghost btn-sm" onClick={() => setEditUser(u)}>Edit</button>
                            <button className="btn btn-danger btn-sm" onClick={() => deleteUser(u)}>Delete</button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div style={{ padding: '10px 16px', display: 'flex', gap: 8, alignItems: 'center', borderTop: '1px solid var(--border)' }}>
                <button className="btn btn-ghost btn-sm" disabled={page === 0} onClick={() => setPage(p => p - 1)}>← Prev</button>
                <span style={{ fontSize: 12, color: 'var(--muted)' }}>Page {page + 1}</span>
                <button className="btn btn-ghost btn-sm" disabled={(page + 1) * 20 >= total} onClick={() => setPage(p => p + 1)}>Next →</button>
              </div>
            </>
          )}
        </div>
      </div>

      {showCreate && <UserFormModal allRoles={allRoles} allGroups={allGroups} onClose={() => setShowCreate(false)} onSaved={load} />}
      {editUser   && <UserFormModal user={editUser} allRoles={allRoles} allGroups={allGroups} onClose={() => setEditUser(null)} onSaved={load} />}
      {detailUser && <UserDetailModal user={detailUser} onClose={() => setDetailUser(null)} onChanged={refreshDetail} />}
    </>
  )
}

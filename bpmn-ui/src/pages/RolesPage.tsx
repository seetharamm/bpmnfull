import { useState, useEffect, useCallback } from 'react'
import toast from 'react-hot-toast'
import { rolesApi } from '../api'
import type { RoleDto, CreateRoleRequest } from '../types'
import PageHeader from '../components/PageHeader'
import Modal from '../components/Modal'
import Spinner from '../components/Spinner'

function RoleFormModal({ role, onClose, onSaved }: { role?: RoleDto; onClose: () => void; onSaved: () => void }) {
  const [form, setForm] = useState<CreateRoleRequest>({ name: role?.name ?? '', description: role?.description ?? '' })
  const [busy, setBusy] = useState(false)

  const submit = async () => {
    if (!form.name.trim()) { toast.error('Name required'); return }
    setBusy(true)
    try {
      if (role) { await rolesApi.update(role.id, form); toast.success('Role updated') }
      else { await rolesApi.create(form); toast.success('Role created') }
      onSaved(); onClose()
    } catch (e: any) { toast.error(e.message) }
    finally { setBusy(false) }
  }

  return (
    <Modal title={role ? `Edit role: ${role.name}` : 'Create Role'} onClose={onClose}
      footer={<>
        <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" onClick={submit} disabled={busy}>
          {busy && <Spinner size={14} />} {role ? 'Save' : 'Create'}
        </button>
      </>}>
      <div className="form-group">
        <label>Role name *</label>
        <input className="form-control" placeholder="PROCESS_MANAGER" value={form.name}
          disabled={!!role} onChange={e => setForm(f => ({ ...f, name: e.target.value.toUpperCase() }))} />
      </div>
      <div className="form-group">
        <label>Description</label>
        <textarea className="form-control" rows={3} placeholder="What can this role do…"
          value={form.description ?? ''} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} />
      </div>
    </Modal>
  )
}

export default function RolesPage() {
  const [roles, setRoles] = useState<RoleDto[]>([])
  const [loading, setLoading] = useState(true)
  const [showCreate, setShowCreate] = useState(false)
  const [editRole, setEditRole] = useState<RoleDto | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    try { setRoles(await rolesApi.list()) }
    catch (e: any) { toast.error(e.message) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  const deleteRole = async (r: RoleDto) => {
    if (!confirm(`Delete role "${r.name}"?`)) return
    try { await rolesApi.delete(r.id); toast.success('Role deleted'); load() }
    catch (e: any) { toast.error(e.message) }
  }

  return (
    <>
      <PageHeader title="Roles"
        actions={<button className="btn btn-primary" onClick={() => setShowCreate(true)}>+ New Role</button>} />
      <div className="page">
        <div className="card" style={{ padding: 0 }}>
          {loading ? (
            <div className="empty-state"><Spinner /></div>
          ) : roles.length === 0 ? (
            <div className="empty-state"><p>No roles found.</p></div>
          ) : (
            <div className="table-wrap">
              <table>
                <thead><tr>
                  <th>Name</th><th>Description</th><th>Created</th><th>Actions</th>
                </tr></thead>
                <tbody>
                  {roles.map(r => (
                    <tr key={r.id}>
                      <td><span className="tag">{r.name}</span></td>
                      <td className="text-muted">{r.description ?? '—'}</td>
                      <td className="text-muted">{new Date(r.createdAt).toLocaleDateString()}</td>
                      <td>
                        <div className="btn-row">
                          <button className="btn btn-ghost btn-sm" onClick={() => setEditRole(r)}>Edit</button>
                          <button className="btn btn-danger btn-sm" onClick={() => deleteRole(r)}>Delete</button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>

      {showCreate && <RoleFormModal onClose={() => setShowCreate(false)} onSaved={load} />}
      {editRole   && <RoleFormModal role={editRole} onClose={() => setEditRole(null)} onSaved={load} />}
    </>
  )
}

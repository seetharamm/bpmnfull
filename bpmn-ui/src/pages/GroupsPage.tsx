import { useState, useEffect, useCallback } from 'react'
import toast from 'react-hot-toast'
import { groupsApi, rolesApi } from '../api'
import type { GroupDto, RoleDto, MembershipDto, CreateGroupRequest } from '../types'
import PageHeader from '../components/PageHeader'
import Modal from '../components/Modal'
import Spinner from '../components/Spinner'

// ── Create / Edit Modal ───────────────────────────────────────────────────────
function GroupFormModal({ group, allRoles, onClose, onSaved }: {
  group?: GroupDto; allRoles: RoleDto[]; onClose: () => void; onSaved: () => void
}) {
  const [form, setForm] = useState<CreateGroupRequest>({
    name: group?.name ?? '',
    displayName: group?.displayName ?? '',
    type: group?.type ?? 'ORGANISATIONAL',
    roleNames: group?.roles ?? [],
  })
  const [busy, setBusy] = useState(false)

  const toggleRole = (name: string) => {
    setForm(f => {
      const arr = f.roleNames ?? []
      return { ...f, roleNames: arr.includes(name) ? arr.filter(x => x !== name) : [...arr, name] }
    })
  }

  const submit = async () => {
    if (!form.name.trim()) { toast.error('Name required'); return }
    setBusy(true)
    try {
      if (group) { await groupsApi.update(group.id, form); toast.success('Group updated') }
      else { await groupsApi.create(form); toast.success('Group created') }
      onSaved(); onClose()
    } catch (e: any) { toast.error(e.message) }
    finally { setBusy(false) }
  }

  return (
    <Modal title={group ? `Edit: ${group.name}` : 'Create Group'} onClose={onClose}
      footer={<>
        <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" onClick={submit} disabled={busy}>
          {busy && <Spinner size={14} />} {group ? 'Save' : 'Create'}
        </button>
      </>}>
      <div className="form-group">
        <label>Group name *</label>
        <input className="form-control" placeholder="hr_team" value={form.name} disabled={!!group}
          onChange={e => setForm(f => ({ ...f, name: e.target.value }))} />
      </div>
      <div className="form-group">
        <label>Display name</label>
        <input className="form-control" placeholder="HR Team" value={form.displayName ?? ''}
          onChange={e => setForm(f => ({ ...f, displayName: e.target.value }))} />
      </div>
      <div className="form-group">
        <label>Type</label>
        <select className="form-control" value={form.type ?? ''} onChange={e => setForm(f => ({ ...f, type: e.target.value }))}>
          <option>ORGANISATIONAL</option>
          <option>SECURITY</option>
          <option>FUNCTIONAL</option>
        </select>
      </div>
      <div className="form-group">
        <label>Roles assigned to this group</label>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {allRoles.map(r => (
            <label key={r.id} style={{ fontSize: 13, display: 'flex', alignItems: 'center', gap: 4 }}>
              <input type="checkbox" checked={(form.roleNames ?? []).includes(r.name)}
                onChange={() => toggleRole(r.name)} /> {r.name}
            </label>
          ))}
        </div>
      </div>
    </Modal>
  )
}

// ── Group Detail Modal ────────────────────────────────────────────────────────
function GroupDetailModal({ group, allRoles, onClose, onChanged }: {
  group: GroupDto; allRoles: RoleDto[]; onClose: () => void; onChanged: () => void
}) {
  const [members, setMembers] = useState<MembershipDto[]>([])
  const [loadingM, setLoadingM] = useState(true)
  const [addRole, setAddRole] = useState('')

  const loadMembers = useCallback(async () => {
    setLoadingM(true)
    try { setMembers(await groupsApi.getMembers(group.id)) }
    catch { /* ignore */ }
    finally { setLoadingM(false) }
  }, [group.id])

  useEffect(() => { loadMembers() }, [loadMembers])

  const assignRole = async (name: string) => {
    try { await groupsApi.assignRole(group.id, name); toast.success(`Role ${name} assigned`); onChanged() }
    catch (e: any) { toast.error(e.message) }
  }
  const removeRole = async (name: string) => {
    try { await groupsApi.removeRole(group.id, name); toast.success(`Role ${name} removed`); onChanged() }
    catch (e: any) { toast.error(e.message) }
  }

  return (
    <Modal title={`Group: ${group.name}`} onClose={onClose} wide>
      <div style={{ marginBottom: 12 }}>
        <p><b>Display:</b> {group.displayName ?? '—'} &nbsp;<b>Type:</b> <span className="tag">{group.type ?? '—'}</span></p>
      </div>

      <b style={{ fontSize: 12 }}>Assigned roles</b>
      <div style={{ margin: '8px 0 12px', display: 'flex', flexWrap: 'wrap', gap: 6 }}>
        {group.roles.map(r => (
          <span key={r} className="tag">
            {r}
            <button style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--danger)', fontSize: 11, marginLeft: 4 }}
              onClick={() => removeRole(r)}>✕</button>
          </span>
        ))}
        <div className="gap-8">
          <select className="form-control" style={{ height: 26, fontSize: 12 }} value={addRole} onChange={e => setAddRole(e.target.value)}>
            <option value="">Add role…</option>
            {allRoles.filter(r => !group.roles.includes(r.name)).map(r => <option key={r.id} value={r.name}>{r.name}</option>)}
          </select>
          <button className="btn btn-ghost btn-sm" onClick={() => addRole && assignRole(addRole)}>+</button>
        </div>
      </div>

      <b style={{ fontSize: 12 }}>Members ({members.length})</b>
      {loadingM ? <Spinner /> : members.length === 0 ? (
        <p className="text-muted" style={{ fontSize: 13, marginTop: 8 }}>No members.</p>
      ) : (
        <div className="table-wrap" style={{ marginTop: 8 }}>
          <table>
            <thead><tr><th>Username</th><th>Added by</th><th>Joined</th></tr></thead>
            <tbody>
              {members.map(m => (
                <tr key={m.id}>
                  <td>{m.username}</td>
                  <td className="text-muted">{m.addedBy ?? '—'}</td>
                  <td className="text-muted">{new Date(m.joinedAt).toLocaleDateString()}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Modal>
  )
}

// ── Main Page ─────────────────────────────────────────────────────────────────
export default function GroupsPage() {
  const [groups, setGroups] = useState<GroupDto[]>([])
  const [allRoles, setAllRoles] = useState<RoleDto[]>([])
  const [loading, setLoading] = useState(true)
  const [showCreate, setShowCreate] = useState(false)
  const [editGroup, setEditGroup] = useState<GroupDto | null>(null)
  const [detailGroup, setDetailGroup] = useState<GroupDto | null>(null)
  const [typeFilter, setTypeFilter] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const [gs, rs] = await Promise.all([groupsApi.list(typeFilter || undefined), rolesApi.list()])
      setGroups(gs); setAllRoles(rs)
    } catch (e: any) { toast.error(e.message) }
    finally { setLoading(false) }
  }, [typeFilter])

  useEffect(() => { load() }, [load])

  const refreshDetail = async () => {
    if (!detailGroup) return
    try {
      const fresh = await groupsApi.getById(detailGroup.id)
      setDetailGroup(fresh)
    } catch { /* ignore */ }
    load()
  }

  const deleteGroup = async (g: GroupDto) => {
    if (!confirm(`Delete group "${g.name}"?`)) return
    try { await groupsApi.delete(g.id); toast.success('Group deleted'); load() }
    catch (e: any) { toast.error(e.message) }
  }

  return (
    <>
      <PageHeader title="Groups"
        actions={<button className="btn btn-primary" onClick={() => setShowCreate(true)}>+ New Group</button>} />
      <div className="page">
        <div className="search-row">
          <select className="form-control" style={{ maxWidth: 200 }} value={typeFilter} onChange={e => setTypeFilter(e.target.value)}>
            <option value="">All types</option>
            <option>ORGANISATIONAL</option>
            <option>SECURITY</option>
            <option>FUNCTIONAL</option>
          </select>
          <button className="btn btn-ghost" onClick={load}>↻ Refresh</button>
        </div>
        <div className="card" style={{ padding: 0 }}>
          {loading ? (
            <div className="empty-state"><Spinner /></div>
          ) : groups.length === 0 ? (
            <div className="empty-state"><p>No groups found.</p></div>
          ) : (
            <div className="table-wrap">
              <table>
                <thead><tr>
                  <th>Name</th><th>Display</th><th>Type</th><th>Roles</th><th>Actions</th>
                </tr></thead>
                <tbody>
                  {groups.map(g => (
                    <tr key={g.id}>
                      <td><b>{g.name}</b></td>
                      <td className="text-muted">{g.displayName ?? '—'}</td>
                      <td><span className="tag">{g.type ?? '—'}</span></td>
                      <td>{g.roles.map(r => <span key={r} className="tag">{r}</span>)}</td>
                      <td>
                        <div className="btn-row">
                          <button className="btn btn-ghost btn-sm" onClick={() => setDetailGroup(g)}>Members</button>
                          <button className="btn btn-ghost btn-sm" onClick={() => setEditGroup(g)}>Edit</button>
                          <button className="btn btn-danger btn-sm" onClick={() => deleteGroup(g)}>Delete</button>
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

      {showCreate  && <GroupFormModal allRoles={allRoles} onClose={() => setShowCreate(false)} onSaved={load} />}
      {editGroup   && <GroupFormModal group={editGroup} allRoles={allRoles} onClose={() => setEditGroup(null)} onSaved={load} />}
      {detailGroup && <GroupDetailModal group={detailGroup} allRoles={allRoles} onClose={() => setDetailGroup(null)} onChanged={refreshDetail} />}
    </>
  )
}

import { useState, useEffect, useCallback } from 'react'
import toast from 'react-hot-toast'
import { tasksApi } from '../api'
import type { TaskEntity } from '../types'
import PageHeader from '../components/PageHeader'
import Modal from '../components/Modal'
import StateBadge from '../components/StateBadge'
import Spinner from '../components/Spinner'

// ── Complete Task Modal ───────────────────────────────────────────────────────
function CompleteModal({ task, onClose, onDone }: { task: TaskEntity; onClose: () => void; onDone: () => void }) {
  const [varsJson, setVarsJson] = useState('{}')
  const [busy, setBusy] = useState(false)

  const submit = async () => {
    let vars: Record<string, unknown> = {}
    try { vars = JSON.parse(varsJson) } catch { toast.error('Variables must be valid JSON'); return }
    setBusy(true)
    try {
      await tasksApi.complete(task.id!, vars)
      toast.success('Task completed')
      onDone(); onClose()
    } catch (e: any) { toast.error(e.message) }
    finally { setBusy(false) }
  }

  return (
    <Modal title={`Complete: ${task.name ?? (task.id ? task.id.slice(0, 12) : task.taskDefinitionKey ?? '?')}`} onClose={onClose}
      footer={<>
        <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" onClick={submit} disabled={busy}>
          {busy && <Spinner size={14} />} Complete
        </button>
      </>}>
      <p style={{ marginBottom: 12, fontSize: 13, color: 'var(--muted)' }}>
        Instance: <span className="mono">{task.processInstanceId.slice(0, 12)}…</span>
        {task.candidateGroups && <> · Groups: <span className="mono">{task.candidateGroups}</span></>}
      </p>
      <div className="form-group">
        <label>Output variables (JSON)</label>
        <textarea className="form-control mono" rows={5} value={varsJson}
          onChange={e => setVarsJson(e.target.value)} />
      </div>
    </Modal>
  )
}

// ── Claim Modal ───────────────────────────────────────────────────────────────
function ClaimModal({ task, onClose, onDone }: { task: TaskEntity; onClose: () => void; onDone: () => void }) {
  const [userId, setUserId] = useState('')
  const [busy, setBusy] = useState(false)

  const submit = async () => {
    if (!userId.trim()) { toast.error('User ID required'); return }
    setBusy(true)
    try { await tasksApi.claim(task.id!, userId.trim()); toast.success('Task claimed'); onDone(); onClose() }
    catch (e: any) { toast.error(e.message) }
    finally { setBusy(false) }
  }

  return (
    <Modal title={`Claim: ${task.name ?? (task.id ? task.id.slice(0, 12) : task.taskDefinitionKey ?? '?')}`} onClose={onClose}
      footer={<>
        <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" onClick={submit} disabled={busy}>
          {busy && <Spinner size={14} />} Claim
        </button>
      </>}>
      <div className="form-group">
        <label>User ID / username</label>
        <input className="form-control" placeholder="alice" value={userId}
          onChange={e => setUserId(e.target.value)} />
      </div>
    </Modal>
  )
}

// ── Main Page ─────────────────────────────────────────────────────────────────
export default function TasksPage() {
  const [tasks, setTasks] = useState<TaskEntity[]>([])
  const [loading, setLoading] = useState(true)
  const [completeTask, setCompleteTask] = useState<TaskEntity | null>(null)
  const [claimTask, setClaimTask] = useState<TaskEntity | null>(null)
  const [filterAssignee, setFilterAssignee] = useState('')
  const [filterInstanceId, setFilterInstanceId] = useState('')

  const load = useCallback(async () => {
    setLoading(true)
    try {
      let data: TaskEntity[]
      if (filterAssignee.trim()) {
        data = await tasksApi.byAssignee(filterAssignee.trim())
      } else if (filterInstanceId.trim()) {
        data = await tasksApi.byInstance(filterInstanceId.trim())
      } else {
        data = await tasksApi.listOpen()
      }
      setTasks(data)
    } catch (e: any) { toast.error(e.message) }
    finally { setLoading(false) }
  }, [filterAssignee, filterInstanceId])

  useEffect(() => { const t = setTimeout(load, 300); return () => clearTimeout(t) }, [load])

  return (
    <>
      <PageHeader title="Tasks"
        actions={<button className="btn btn-ghost" onClick={load}>↻ Refresh</button>} />
      <div className="page">
        <div className="search-row">
          <input className="form-control" placeholder="Filter by assignee…"
            value={filterAssignee} onChange={e => { setFilterAssignee(e.target.value); setFilterInstanceId('') }} />
          <input className="form-control" placeholder="Filter by instance ID…"
            value={filterInstanceId} onChange={e => { setFilterInstanceId(e.target.value); setFilterAssignee('') }} />
          <button className="btn btn-ghost" onClick={() => { setFilterAssignee(''); setFilterInstanceId('') }}>Clear</button>
        </div>

        <div className="card" style={{ padding: 0 }}>
          {loading ? (
            <div className="empty-state"><Spinner /></div>
          ) : tasks.length === 0 ? (
            <div className="empty-state"><p>No tasks found.</p></div>
          ) : (
            <div className="table-wrap">
              <table>
                <thead><tr>
                  <th>Name</th><th>State</th><th>Assignee</th><th>Candidate groups</th>
                  <th>Instance</th><th>Created</th><th>Actions</th>
                </tr></thead>
                <tbody>
                  {tasks.map(t => (
                    <tr key={t.id}>
                      <td>{t.name ?? <span className="text-muted mono">{t.id ? t.id.slice(0, 10) + '…' : t.taskDefinitionKey ?? '—'}</span>}</td>
                      <td><StateBadge state={t.state} /></td>
                      <td className="text-muted">{t.assignee ?? '—'}</td>
                      <td>
                        {t.candidateGroups
                          ? t.candidateGroups.split(',').map(g => <span key={g} className="tag">{g.trim()}</span>)
                          : <span className="text-muted">—</span>}
                      </td>
                      <td className="mono truncate" title={t.processInstanceId}>
                        {t.processInstanceId.slice(0, 12)}…
                      </td>
                      <td className="text-muted">{t.createdAt ? new Date(t.createdAt).toLocaleString() : '—'}</td>
                      <td>
                        <div className="btn-row">
                          {!t.assignee && <button className="btn btn-ghost btn-sm" onClick={() => setClaimTask(t)}>Claim</button>}
                          {(t.state === 'CREATED' || t.state === 'ASSIGNED') &&
                            <button className="btn btn-primary btn-sm" onClick={() => setCompleteTask(t)}>Complete</button>}
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

      {completeTask && <CompleteModal task={completeTask} onClose={() => setCompleteTask(null)} onDone={load} />}
      {claimTask    && <ClaimModal task={claimTask} onClose={() => setClaimTask(null)} onDone={load} />}
    </>
  )
}

import { useState, useEffect, useCallback } from 'react'
import toast from 'react-hot-toast'
import { instancesApi, tasksApi } from '../api'
import type { ProcessInstanceResult, TaskEntity } from '../types'
import PageHeader from '../components/PageHeader'
import Modal from '../components/Modal'
import StateBadge from '../components/StateBadge'
import Spinner from '../components/Spinner'

// ── process key for "Create Bio Request" ─────────────────────────────────────
const PROCESS_KEY = 'create-bio-request'

// ── helpers ───────────────────────────────────────────────────────────────────
function fmt(d: string | null) {
  return d ? new Date(d).toLocaleString() : '—'
}

// ── Create Instance Modal ─────────────────────────────────────────────────────
function CreateModal({ onClose, onCreated }: { onClose: () => void; onCreated: (inst: ProcessInstanceResult) => void }) {
  const [bizKey,  setBizKey]  = useState('')
  const [varsRaw, setVarsRaw] = useState('{\n  "requestorName": "",\n  "department": "",\n  "notes": ""\n}')
  const [busy,    setBusy]    = useState(false)

  const submit = async () => {
    let vars: Record<string, unknown> = {}
    try { vars = JSON.parse(varsRaw) } catch { toast.error('Variables must be valid JSON'); return }
    setBusy(true)
    try {
      const inst = await instancesApi.start(PROCESS_KEY, bizKey.trim() || undefined, vars)
      toast.success(`Instance started — ${inst.id.slice(0, 12)}…`)
      onCreated(inst)
      onClose()
    } catch (e: any) { toast.error(e.message) }
    finally { setBusy(false) }
  }

  return (
    <Modal title="Create Bio Request" onClose={onClose}
      footer={<>
        <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" onClick={submit} disabled={busy}>
          {busy && <Spinner size={14} />} Start
        </button>
      </>}>
      <div className="form-group">
        <label>Business Key <span style={{ color: 'var(--muted)', fontWeight: 400 }}>(optional)</span></label>
        <input className="form-control" placeholder="e.g. BIO-2024-001"
          value={bizKey} onChange={e => setBizKey(e.target.value)} />
      </div>
      <div className="form-group">
        <label>Process Variables (JSON)</label>
        <textarea className="form-control mono" rows={7} value={varsRaw}
          onChange={e => setVarsRaw(e.target.value)} />
      </div>
    </Modal>
  )
}

// ── Complete Task Modal ────────────────────────────────────────────────────────
function CompleteModal({ task, onClose, onDone }: { task: TaskEntity; onClose: () => void; onDone: () => void }) {
  const [varsJson, setVarsJson] = useState('{}')
  const [busy,     setBusy]     = useState(false)

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
    <Modal title={`Complete: ${task.name ?? task.taskDefinitionKey ?? task.id?.slice(0, 12) ?? '?'}`} onClose={onClose}
      footer={<>
        <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" onClick={submit} disabled={busy}>
          {busy && <Spinner size={14} />} Complete
        </button>
      </>}>
      <p style={{ marginBottom: 12, fontSize: 13, color: 'var(--muted)' }}>
        Instance: <span className="mono">{task.processInstanceId.slice(0, 12)}…</span>
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
  const [busy,   setBusy]   = useState(false)

  const submit = async () => {
    if (!userId.trim()) { toast.error('User ID required'); return }
    setBusy(true)
    try { await tasksApi.claim(task.id!, userId.trim()); toast.success('Task claimed'); onDone(); onClose() }
    catch (e: any) { toast.error(e.message) }
    finally { setBusy(false) }
  }

  return (
    <Modal title={`Claim: ${task.name ?? task.taskDefinitionKey ?? task.id?.slice(0, 12) ?? '?'}`} onClose={onClose}
      footer={<>
        <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" onClick={submit} disabled={busy}>
          {busy && <Spinner size={14} />} Claim
        </button>
      </>}>
      <div className="form-group">
        <label>User ID / username</label>
        <input className="form-control" placeholder="alice"
          value={userId} onChange={e => setUserId(e.target.value)} />
      </div>
    </Modal>
  )
}

// ── Single task card ──────────────────────────────────────────────────────────
interface TaskCardProps {
  task: TaskEntity
  onClaim:    (t: TaskEntity) => void
  onComplete: (t: TaskEntity) => void
}
function TaskCard({ task, onClaim, onComplete }: TaskCardProps) {
  const canClaim    = !task.assignee && (task.state === 'CREATED')
  const canComplete = task.state === 'CREATED' || task.state === 'ASSIGNED'

  return (
    <div className="bio-task-card">
      <div className="bio-task-card-header">
        <span className="bio-task-name">{task.name ?? task.taskDefinitionKey ?? <em className="text-muted">Unnamed task</em>}</span>
        <StateBadge state={task.state} />
      </div>

      <div className="bio-task-meta">
        <span title="Instance ID">
          🔖 <span className="mono">{task.processInstanceId.slice(0, 12)}…</span>
        </span>
        {task.assignee && <span>👤 {task.assignee}</span>}
        {task.candidateGroups && (
          <span>
            {task.candidateGroups.split(',').map(g => (
              <span key={g} className="tag">{g.trim()}</span>
            ))}
          </span>
        )}
        {task.activityType && (
          <span className="bio-task-type-chip">{task.activityType.replace(/_/g, ' ')}</span>
        )}
        <span title="Created">{fmt(task.createdAt)}</span>
      </div>

      {(canClaim || canComplete) && (
        <div className="bio-task-actions">
          {canClaim    && <button className="btn btn-ghost btn-sm"    onClick={() => onClaim(task)}>Claim</button>}
          {canComplete && <button className="btn btn-primary btn-sm"  onClick={() => onComplete(task)}>Complete</button>}
        </div>
      )}
    </div>
  )
}

// ── Queue column ──────────────────────────────────────────────────────────────
interface QueueProps {
  title:   string
  count:   number
  accent:  string
  loading: boolean
  empty:   string
  children: React.ReactNode
}
function Queue({ title, count, accent, loading, empty, children }: QueueProps) {
  return (
    <div className="bio-queue">
      <div className="bio-queue-header" style={{ borderTopColor: accent }}>
        <span className="bio-queue-title">{title}</span>
        <span className="bio-queue-count" style={{ background: accent }}>{count}</span>
      </div>
      <div className="bio-queue-body">
        {loading ? (
          <div className="bio-queue-empty"><Spinner /></div>
        ) : count === 0 ? (
          <div className="bio-queue-empty">
            <p>{empty}</p>
          </div>
        ) : children}
      </div>
    </div>
  )
}

// ── Main Page ─────────────────────────────────────────────────────────────────
export default function BioRequestPage() {
  const [showCreate,    setShowCreate]    = useState(false)
  const [completeTask,  setCompleteTask]  = useState<TaskEntity | null>(null)
  const [claimTask,     setClaimTask]     = useState<TaskEntity | null>(null)
  const [loading,       setLoading]       = useState(true)
  const [instances,     setInstances]     = useState<ProcessInstanceResult[]>([])
  const [allTasks,      setAllTasks]      = useState<TaskEntity[]>([])

  // Derive queues from allTasks by state
  const openTasks       = allTasks.filter(t => t.state === 'CREATED')
  const inReviewTasks   = allTasks.filter(t => t.state === 'ASSIGNED')
  const completedTasks  = allTasks.filter(t => t.state === 'COMPLETED')

  const load = useCallback(async () => {
    setLoading(true)
    try {
      // Load all running + completed instances for this process key
      const [running, completed] = await Promise.all([
        instancesApi.list('RUNNING'),
        instancesApi.list('COMPLETED'),
      ])

      // Filter to bio-request instances only
      const bioInstances = [...running, ...completed].filter(
        i => i.processDefinitionKey === PROCESS_KEY
      )
      setInstances(bioInstances)

      // Load tasks for every running instance
      const runningBio = running.filter(i => i.processDefinitionKey === PROCESS_KEY)

      const taskArrays = await Promise.all(
        runningBio.map(inst => tasksApi.byInstance(inst.id).catch(() => [] as TaskEntity[]))
      )

      // Also load all open tasks filtered to bio instances
      let openAll: TaskEntity[] = []
      try {
        openAll = await tasksApi.listOpen()
        openAll = openAll.filter(t =>
          bioInstances.some(i => i.id === t.processInstanceId)
        )
      } catch { /* listOpen may be empty — task arrays cover active */ }

      // Merge: tasks from instance queries + open tasks, dedup by id
      const merged = new Map<string, TaskEntity>()
      for (const t of [...taskArrays.flat(), ...openAll]) {
        if (t.id) merged.set(t.id, t)
      }
      setAllTasks([...merged.values()])
    } catch (e: any) { toast.error(e.message) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { load() }, [load])

  const handleCreated = () => { load() }

  return (
    <>
      <PageHeader
        title="Create Bio Request"
        actions={
          <div className="btn-row">
            <button className="btn btn-ghost" onClick={load}>↻ Refresh</button>
            <button className="btn btn-primary" onClick={() => setShowCreate(true)}>＋ New Request</button>
          </div>
        }
      />

      <div className="page">

        {/* ── Summary strip ── */}
        <div className="bio-summary-strip">
          <div className="bio-summary-card">
            <span className="bio-summary-num">{instances.filter(i => i.state === 'RUNNING').length}</span>
            <span className="bio-summary-label">Running</span>
          </div>
          <div className="bio-summary-card">
            <span className="bio-summary-num">{instances.filter(i => i.state === 'COMPLETED').length}</span>
            <span className="bio-summary-label">Completed</span>
          </div>
          <div className="bio-summary-card">
            <span className="bio-summary-num">{openTasks.length}</span>
            <span className="bio-summary-label">Open Tasks</span>
          </div>
          <div className="bio-summary-card">
            <span className="bio-summary-num">{inReviewTasks.length}</span>
            <span className="bio-summary-label">In Review</span>
          </div>
          <div className="bio-summary-card">
            <span className="bio-summary-num">{completedTasks.length}</span>
            <span className="bio-summary-label">Completed Tasks</span>
          </div>
        </div>

        {/* ── Three-column kanban ── */}
        <div className="bio-kanban">

          {/* Open */}
          <Queue
            title="Open Tasks"
            count={openTasks.length}
            accent="#3b82d4"
            loading={loading}
            empty="No open tasks right now."
          >
            {openTasks.map(t => (
              <TaskCard key={t.id} task={t}
                onClaim={setClaimTask} onComplete={setCompleteTask} />
            ))}
          </Queue>

          {/* In Review */}
          <Queue
            title="In Review"
            count={inReviewTasks.length}
            accent="#7c5cd8"
            loading={loading}
            empty="No tasks currently in review."
          >
            {inReviewTasks.map(t => (
              <TaskCard key={t.id} task={t}
                onClaim={setClaimTask} onComplete={setCompleteTask} />
            ))}
          </Queue>

          {/* Completed */}
          <Queue
            title="Completed"
            count={completedTasks.length}
            accent="#16a34a"
            loading={loading}
            empty="No completed tasks yet."
          >
            {completedTasks.map(t => (
              <TaskCard key={t.id} task={t}
                onClaim={setClaimTask} onComplete={setCompleteTask} />
            ))}
          </Queue>

        </div>

        {/* ── Recent instances table ── */}
        <div className="card" style={{ marginTop: 24, padding: 0 }}>
          <div className="card-header" style={{ padding: '14px 16px' }}>
            <span className="card-title">Recent Bio Request Instances</span>
            <span className="text-muted" style={{ fontSize: 12 }}>{instances.length} total</span>
          </div>
          {loading ? (
            <div className="empty-state"><Spinner /></div>
          ) : instances.length === 0 ? (
            <div className="empty-state">
              <p>No instances found. Start a new request above.</p>
            </div>
          ) : (
            <div className="table-wrap">
              <table>
                <thead><tr>
                  <th>Instance ID</th>
                  <th>Business Key</th>
                  <th>State</th>
                  <th>Started</th>
                  <th>Ended</th>
                </tr></thead>
                <tbody>
                  {instances.map(inst => (
                    <tr key={inst.id}>
                      <td className="mono truncate" title={inst.id}>{inst.id.slice(0, 14)}…</td>
                      <td>{inst.businessKey ?? <span className="text-muted">—</span>}</td>
                      <td><StateBadge state={inst.state} /></td>
                      <td className="text-muted">{fmt(inst.startTime)}</td>
                      <td className="text-muted">{fmt(inst.endTime)}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

      </div>

      {/* ── Modals ── */}
      {showCreate   && <CreateModal   onClose={() => setShowCreate(false)}  onCreated={handleCreated} />}
      {completeTask && <CompleteModal task={completeTask} onClose={() => setCompleteTask(null)} onDone={load} />}
      {claimTask    && <ClaimModal    task={claimTask}    onClose={() => setClaimTask(null)}    onDone={load} />}
    </>
  )
}

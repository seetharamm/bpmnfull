import { useState, useEffect, useCallback, useRef } from 'react'
import toast from 'react-hot-toast'
import { instancesApi, definitionsApi, tasksApi, activitiesApi } from '../api'
import type { ProcessInstanceResult, ProcessDefinitionDto, TaskEntity, ActivityInstance } from '../types'
import PageHeader from '../components/PageHeader'
import Modal from '../components/Modal'
import StateBadge from '../components/StateBadge'
import Spinner from '../components/Spinner'
import BpmnDiagram from '../components/BpmnDiagram'
import TaskDetailModal from '../components/TaskDetailModal'

// ─── helpers ──────────────────────────────────────────────────────────────────

function fmt(iso: string | null) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString()
}

function fmtShort(iso: string | null) {
  if (!iso) return '—'
  const d = new Date(iso)
  return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}

function isOverdue(dueDate: string | null) {
  return !!dueDate && new Date(dueDate) < new Date()
}

// Dot icon for activity state (active = no endTime, completed = has endTime)
function ActivityDot({ active, completed }: { active: boolean; completed: boolean }) {
  const icon = completed
    ? <path d="M1 3.5l2 2 3-3" stroke="#22c55e" strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round" />
    : active
      ? <circle cx="3.5" cy="3.5" r="2" fill="#60a5fa" />
      : <circle cx="3.5" cy="3.5" r="2" fill="#cbd5e1" />
  const cls = completed ? 'dot-completed' : active ? 'dot-created' : ''
  return (
    <div className={`task-dot ${cls}`}>
      <svg viewBox="0 0 7 7">{icon}</svg>
    </div>
  )
}

// Dot icon per task state (kept for backward compat)
function TaskDot({ state }: { state: TaskEntity['state'] }) {
  const icon = ({
    CREATED:   <circle cx="3.5" cy="3.5" r="2" fill="#60a5fa" />,
    ASSIGNED:  <circle cx="3.5" cy="3.5" r="2" fill="#8b5cf6" />,
    COMPLETED: <path d="M1 3.5l2 2 3-3" stroke="#22c55e" strokeWidth="1.5" fill="none" strokeLinecap="round" strokeLinejoin="round" />,
    DELETED:   <path d="M1 1l5 5M6 1L1 6" stroke="#ef4444" strokeWidth="1.5" strokeLinecap="round" />,
    PENDING:   <circle cx="3.5" cy="3.5" r="2" fill="#94a3b8" strokeDasharray="1" />,
  } as Record<string, React.ReactNode>)[state]
  const dotClass = state === 'COMPLETED' ? 'dot-completed'
                 : state === 'ASSIGNED'  ? 'dot-assigned'
                 : state === 'CREATED'   ? 'dot-created'
                 : state === 'PENDING'   ? 'dot-pending'
                 : 'dot-deleted'
  return (
    <div className={`task-dot ${dotClass}`}>
      <svg viewBox="0 0 7 7">{icon}</svg>
    </div>
  )
}

// Human-readable activity type label
function activityTypeLabel(type: string): string {
  const map: Record<string, string> = {
    USER_TASK: 'User Task', SERVICE_TASK: 'Service Task', SCRIPT_TASK: 'Script Task',
    RECEIVE_TASK: 'Receive Task', SEND_TASK: 'Send Task', MANUAL_TASK: 'Manual Task',
    BUSINESS_RULE_TASK: 'Business Rule', CALL_ACTIVITY: 'Call Activity',
    SUB_PROCESS: 'Sub-Process', PARALLEL_GATEWAY: 'Parallel Gateway',
    EXCLUSIVE_GATEWAY: 'Exclusive Gateway', INCLUSIVE_GATEWAY: 'Inclusive Gateway',
    EVENT_BASED_GATEWAY: 'Event Gateway',
    START_EVENT: 'Start Event', END_EVENT: 'End Event',
    INTERMEDIATE_CATCH_EVENT: 'Catch Event', INTERMEDIATE_THROW_EVENT: 'Throw Event',
  }
  return map[type] ?? type.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase())
}

// ─── Instance right panel ─────────────────────────────────────────────────────

type PanelTab = 'diagram' | 'tasks' | 'activities'
type TaskFilter = 'all' | 'open' | 'inprogress' | 'done' | 'pending'

interface PanelProps {
  instance: ProcessInstanceResult
  onClose: () => void
  onInstanceChange: (id: string) => void
}

function InstancePanel({ instance, onClose, onInstanceChange }: PanelProps) {
  const [tab, setTab]             = useState<PanelTab>('tasks')
  const [taskFilter, setTaskFilter] = useState<TaskFilter>('all')
  const autoTabRef = useRef(false)   // track whether we've auto-switched once
  const [tasks, setTasks]         = useState<TaskEntity[]>([])
  const [activities, setActivities] = useState<ActivityInstance[]>([])
  const [loadingTasks, setLoadingTasks] = useState(true)
  const [bpmnXml, setBpmnXml]     = useState<string | null>(null)
  const [loadingXml, setLoadingXml] = useState(false)
  const [claimId, setClaimId]     = useState('')
  const [claimUser, setClaimUser] = useState('')
  const [completing, setCompleting] = useState<string | null>(null)
  const [detailTaskId, setDetailTaskId] = useState<string | null>(null)  // task detail modal
  const prevId = useRef('')
  const bpmnLoadedRef = useRef(false)   // ref-guard: prevents re-fetch loop when bpmnXml state changes

  // ── data loading ────────────────────────────────────────────────────────────

  const loadTasks = useCallback(async () => {
    setLoadingTasks(true)
    try {
      const [t, a] = await Promise.all([
        tasksApi.byInstance(instance.id),
        activitiesApi.byInstance(instance.id),
      ])
      setTasks(t)
      setActivities(a)
      // Auto-switch to Activities tab on first load if there are no user tasks
      // but there are activity records, so the user immediately sees data.
      if (!autoTabRef.current) {
        autoTabRef.current = true
        if (t.length === 0 && a.length > 0) {
          setTab('activities')
        } else {
          setTab('tasks')
        }
      }
    } catch (e: any) {
      toast.error('Failed to load tasks: ' + (e?.message ?? 'Unknown error'))
      setTasks([])
      setActivities([])
    } finally {
      setLoadingTasks(false)
    }
  }, [instance.id])

  // Load BPMN XML for the diagram — fetch active version for the process key.
  // Uses a ref guard (not bpmnXml state) in deps to prevent the re-fetch loop
  // that would occur when bpmnXml state change re-creates this callback.
  const loadBpmn = useCallback(async () => {
    if (bpmnLoadedRef.current) return   // already loaded for this instance
    bpmnLoadedRef.current = true
    setLoadingXml(true)
    try {
      // Get active definition for this key (includes bpmnXml field)
      const def: ProcessDefinitionDto = await definitionsApi.getActive(instance.processDefinitionKey)
      if (def.bpmnXml) {
        setBpmnXml(def.bpmnXml)
      } else {
        // Fall back to the raw XML endpoint
        const res = await fetch(`/api/definitions/${def.key}/versions/${def.version}/bpmn`)
        if (!res.ok) throw new Error('Could not fetch BPMN XML')
        setBpmnXml(await res.text())
      }
    } catch (e: any) {
      bpmnLoadedRef.current = false  // allow retry on error
      toast.error('Could not load diagram: ' + e.message)
    } finally {
      setLoadingXml(false)
    }
  }, [instance.processDefinitionKey])   // bpmnXml intentionally removed from deps

  useEffect(() => {
    if (prevId.current !== instance.id) {
      prevId.current = instance.id
      bpmnLoadedRef.current = false   // reset guard when instance changes
      autoTabRef.current = false      // reset auto-tab for new instance
      setTasks([])
      setActivities([])
      setBpmnXml(null)
      setTab('tasks')                 // reset to tasks while loading
      setTaskFilter('all')
    }
    loadTasks()
  }, [loadTasks, instance.id])

  // Load diagram XML when Diagram tab is active
  useEffect(() => {
    if (tab === 'diagram') loadBpmn()
  }, [tab, loadBpmn])

  // ── actions ─────────────────────────────────────────────────────────────────

  const completeTask = async (taskId: string) => {
    setCompleting(taskId)
    try {
      await tasksApi.complete(taskId, {})
      toast.success('Task completed')
      await loadTasks()
      onInstanceChange(instance.id)
    } catch (e: any) { toast.error(e.message) }
    finally { setCompleting(null) }
  }

  const claimTask = async (taskId: string) => {
    const user = claimId === taskId ? claimUser.trim() : ''
    if (!user) { setClaimId(taskId); return }
    try {
      await tasksApi.claim(taskId, user)
      toast.success(`Claimed by ${user}`)
      setClaimId(''); setClaimUser('')
      await loadTasks()
    } catch (e: any) { toast.error(e.message) }
  }

  // ── derived ───────────────────────────────────────────────────────────────────

  // Build a map activityId → TaskEntity for Claim/Complete actions (activities tab)
  const taskByActivityId = new Map<string, TaskEntity>()
  tasks.forEach(t => { if (t.taskDefinitionKey) taskByActivityId.set(t.taskDefinitionKey, t) })

  // Deduplicate activities: keep most-recent row per activityId
  const deduped = new Map<string, typeof activities[0]>()
  activities.forEach(a => {
    const existing = deduped.get(a.activityId)
    if (!existing || new Date(a.startTime) > new Date(existing.startTime)) {
      deduped.set(a.activityId, a)
    }
  })
  const sortedActivities = [...deduped.values()].sort((a, b) =>
    new Date(a.startTime).getTime() - new Date(b.startTime).getTime()
  )

  // User-task groupings for the Tasks tab
  const tasksDone       = tasks.filter(t => t.state === 'COMPLETED')
  const tasksInProgress = tasks.filter(t => t.state === 'ASSIGNED')
  const tasksOpen       = tasks.filter(t => t.state === 'CREATED')
  const tasksDeleted    = tasks.filter(t => t.state === 'DELETED')
  const tasksPending    = tasks.filter(t => t.state === 'PENDING')

  // Filtered list for display
  const filteredTasks = taskFilter === 'open'       ? tasksOpen
                      : taskFilter === 'inprogress' ? tasksInProgress
                      : taskFilter === 'done'        ? tasksDone
                      : taskFilter === 'pending'     ? tasksPending
                      : tasks  // 'all'

  // Progress bar (based on user tasks only when tasks exist, else activities)
  const pctTasks  = tasks.length > 0 ? Math.round((tasksDone.length / tasks.length) * 100) : 0
  const openTasks = tasksOpen.length + tasksInProgress.length

  // Activity tab progress
  const activeActs = sortedActivities.filter(a => a.endTime === null)
  const doneActs   = sortedActivities.filter(a => a.endTime !== null)
  const total      = sortedActivities.length
  const pct        = tasks.length > 0 ? pctTasks
                   : total > 0 ? Math.round((doneActs.length / total) * 100) : 0

  // ── render ───────────────────────────────────────────────────────────────────

  return (
    <div className="instance-panel">

      {/* ── Panel header ── */}
      <div className="panel-header">
        <span className="panel-header-title" title={instance.id}>
          {instance.processDefinitionKey}
          <span className="text-muted" style={{ fontWeight: 400, marginLeft: 6 }}>
            {instance.id.slice(0, 10)}…
          </span>
        </span>
        <StateBadge state={instance.state} />
        <button className="panel-refresh" title="Refresh tasks" onClick={loadTasks}>↻</button>
        <button className="panel-close" onClick={onClose}>✕</button>
      </div>

      {/* ── Instance meta ── */}
      <div style={{ padding: '10px 16px', borderBottom: '1px solid var(--border)' }}>
        <div className="instance-meta" style={{ marginBottom: 0, paddingBottom: 0, border: 'none' }}>
          <div className="instance-meta-item">
            <label>Instance ID</label>
            <span className="mono" style={{ fontSize: 10 }}>{instance.id}</span>
          </div>
          <div className="instance-meta-item">
            <label>Business key</label>
            <span>{instance.businessKey ?? '—'}</span>
          </div>
          <div className="instance-meta-item">
            <label>Started</label>
            <span>{fmtShort(instance.startTime)}</span>
          </div>
          <div className="instance-meta-item">
            <label>Ended</label>
            <span>{fmtShort(instance.endTime)}</span>
          </div>
        </div>

        {/* progress bar */}
        {!loadingTasks && (tasks.length > 0 || total > 0) && (
          <div style={{ marginTop: 10 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 3 }}>
              <span style={{ fontSize: 10, color: 'var(--muted)', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '.5px' }}>
                Task progress
              </span>
              <span style={{ fontSize: 10, color: 'var(--muted)' }}>
                {tasks.length > 0 ? (
                  <>
                    {tasksDone.length}/{tasks.length} done
                    {tasksInProgress.length > 0 && <span style={{ marginLeft: 6, color: '#8b5cf6' }}>• {tasksInProgress.length} in progress</span>}
                    {tasksOpen.length > 0 && <span style={{ marginLeft: 6, color: 'var(--accent)' }}>• {tasksOpen.length} open</span>}
                  </>
                ) : (
                  <>{doneActs.length}/{total} activities</>
                )}
              </span>
            </div>
            <div style={{ height: 5, background: 'var(--border)', borderRadius: 99, overflow: 'hidden' }}>
              <div style={{
                height: '100%',
                width: `${pct}%`,
                background: pct === 100 ? 'var(--success)' : 'var(--accent)',
                borderRadius: 99,
                transition: 'width .3s ease',
              }} />
            </div>
          </div>
        )}
      </div>

      {/* ── Tabs ── */}
      <div className="panel-tabs">
        <button
          className={`panel-tab${tab === 'diagram' ? ' active' : ''}`}
          onClick={() => setTab('diagram')}
        >
          ⬡ Diagram
        </button>
        <button
          className={`panel-tab${tab === 'tasks' ? ' active' : ''}`}
          onClick={() => setTab('tasks')}
        >
          ✔ Tasks {tasks.length > 0 && `(${tasks.length})`}
        </button>
        <button
          className={`panel-tab${tab === 'activities' ? ' active' : ''}`}
          onClick={() => setTab('activities')}
        >
          ⊟ Activities {total > 0 && `(${total})`}
        </button>
      </div>

      {/* ── Diagram tab ── */}
      {tab === 'diagram' && (
        <div className="panel-diagram-body">
          {loadingXml ? (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', flex: 1 }}>
              <Spinner />
            </div>
          ) : bpmnXml ? (
            <BpmnDiagram
              bpmnXml={bpmnXml}
              activities={activities}
              tasks={tasks}
              onTaskClick={() => { setTab('tasks'); setTaskFilter('open') }}
            />
          ) : (
            <div className="empty-state" style={{ padding: '32px 0' }}>
              <p style={{ fontSize: 13 }}>Diagram not available.</p>
            </div>
          )}
        </div>
      )}

      {/* ── Tasks tab (user tasks: open / in progress / done) ── */}
      {tab === 'tasks' && (
        <div className="panel-body">
          {loadingTasks ? (
            <div style={{ display: 'flex', justifyContent: 'center', padding: '32px 0' }}>
              <Spinner />
            </div>
          ) : tasks.length === 0 ? (
            <div className="empty-state" style={{ padding: '24px 0' }}>
              <p style={{ fontSize: 13 }}>No user tasks for this instance.</p>
              {sortedActivities.length > 0 && (
                <button className="btn btn-ghost btn-sm" style={{ marginTop: 8 }}
                  onClick={() => setTab('activities')}>
                  View {sortedActivities.length} activit{sortedActivities.length === 1 ? 'y' : 'ies'} →
                </button>
              )}
            </div>
          ) : (
            <>
              {/* Filter pills */}
              <div className="task-filter-bar">
                {([
                  { key: 'all',        label: 'All',         count: tasks.length },
                  { key: 'open',       label: 'Open',        count: tasksOpen.length },
                  { key: 'inprogress', label: 'In Progress', count: tasksInProgress.length },
                  { key: 'done',       label: 'Done',        count: tasksDone.length },
                  { key: 'pending',    label: 'Pending',     count: tasksPending.length },
                ] as { key: TaskFilter; label: string; count: number }[]).map(f => (
                  <button
                    key={f.key}
                    className={`task-filter-pill${taskFilter === f.key ? ' active' : ''}`}
                    onClick={() => setTaskFilter(f.key)}
                  >
                    {f.label}
                    {f.count > 0 && <span className="task-filter-count">{f.count}</span>}
                  </button>
                ))}
              </div>

              {filteredTasks.length === 0 ? (
                <div className="empty-state" style={{ padding: '16px 0' }}>
                  <p style={{ fontSize: 12 }}>No tasks match this filter.</p>
                </div>
              ) : (
                <ul className="task-timeline">
                  {filteredTasks.map(task => {
                    const isUserTask = !task.activityType || task.activityType === 'USER_TASK'
                    const isOpen   = isUserTask && (task.state === 'CREATED' || task.state === 'ASSIGNED')
                    const overdue  = isOverdue(task.dueDate)
                    const isBusy   = completing === task.id
                    const typeLabel = task.activityType ? activityTypeLabel(task.activityType) : 'User Task'

                    // status badge config
                    const statusCfg = {
                      CREATED:   { bg: '#eff6ff', color: '#1d4ed8', border: '#bfdbfe', label: 'Open' },
                      ASSIGNED:  { bg: '#f5f3ff', color: '#5b21b6', border: '#ddd6fe', label: 'In Progress' },
                      COMPLETED: { bg: '#dcfce7', color: '#15803d', border: '#bbf7d0', label: 'Done' },
                      DELETED:   { bg: '#fee2e2', color: '#b91c1c', border: '#fecaca', label: 'Deleted' },
                      PENDING:   { bg: '#f8fafc', color: '#64748b', border: '#e2e8f0', label: 'Pending' },
                    }[task.state] ?? { bg: '#f1f5f9', color: '#64748b', border: '#e2e8f0', label: task.state }

                    return (
                      <li key={task.id ?? task.taskDefinitionKey} className="task-item"
                        style={{ cursor: task.id ? 'pointer' : 'default', opacity: task.state === 'PENDING' ? 0.6 : 1 }}
                        onClick={() => task.id && setDetailTaskId(task.id)}
                        title={task.id ? 'Click to view task detail' : 'Not yet started'}
                      >
                        <TaskDot state={task.state} />
                        <div className="task-card">
                          <div className="task-card-top">
                            <span className="task-name">{task.name ?? task.taskDefinitionKey ?? (task.id ? task.id.slice(0, 12) : '—')}</span>
                            <div style={{ display: 'flex', gap: 4, alignItems: 'center', flexShrink: 0 }}>
                              {/* Activity type chip — shown for non-user-task types */}
                              {task.activityType && task.activityType !== 'USER_TASK' && (
                                <span style={{
                                  fontSize: 10, padding: '1px 6px', borderRadius: 4,
                                  background: '#f1f5f9', color: '#475569',
                                  border: '1px solid #e2e8f0', whiteSpace: 'nowrap',
                                }}>
                                  {typeLabel}
                                </span>
                              )}
                              <span style={{
                                fontSize: 10, padding: '1px 7px', borderRadius: 4,
                                background: statusCfg.bg, color: statusCfg.color,
                                border: `1px solid ${statusCfg.border}`,
                                fontWeight: 600, whiteSpace: 'nowrap',
                              }}>
                                {statusCfg.label}
                              </span>
                            </div>
                          </div>

                          <div className="task-meta-row">
                            {isUserTask && (task.assignee
                              ? <span title="Assignee">👤 {task.assignee}</span>
                              : <span style={{ color: 'var(--muted)', fontSize: 11 }}>Unassigned</span>)}
                            {task.candidateGroups && <span title="Candidate groups">👥 {task.candidateGroups}</span>}
                            {task.priority !== 50 && <span title="Priority">⚑ P{task.priority}</span>}
                            {task.dueDate && (
                              <span className={overdue && isOpen ? 'task-due-overdue' : ''} title="Due date">
                                🕐 {fmtShort(task.dueDate)}{overdue && isOpen ? ' ⚠' : ''}
                              </span>
                            )}
                            {task.createdAt && <span className="text-muted">Created {fmtShort(task.createdAt)}</span>}
                            {task.completedAt && <span className="text-muted">✓ {fmtShort(task.completedAt)}</span>}
                          </div>

                          {/* Inline claim form — only for real user tasks */}
                          {task.id && isUserTask && claimId === task.id && (
                            <div className="gap-8" style={{ marginBottom: 8 }}>
                              <input className="form-control" style={{ flex: 1 }}
                                placeholder="Username…" value={claimUser}
                                onChange={e => setClaimUser(e.target.value)}
                                onKeyDown={e => {
                                  if (e.key === 'Enter') claimTask(task.id!)
                                  if (e.key === 'Escape') { setClaimId(''); setClaimUser('') }
                                }}
                                autoFocus
                              />
                              <button className="btn btn-primary btn-sm" onClick={e => { e.stopPropagation(); claimTask(task.id!) }}>OK</button>
                              <button className="btn btn-ghost btn-sm" onClick={e => { e.stopPropagation(); setClaimId(''); setClaimUser('') }}>✕</button>
                            </div>
                          )}

                          {task.id && (
                          <div className="task-actions">
                            {/* Claim / Complete only apply to user tasks */}
                            {isOpen && !task.assignee && claimId !== task.id && (
                              <button className="btn btn-ghost btn-sm"
                                onClick={e => { e.stopPropagation(); claimTask(task.id!) }}>Claim</button>
                            )}
                            {isOpen && (
                              <button className="btn btn-primary btn-sm" disabled={isBusy}
                                onClick={e => { e.stopPropagation(); completeTask(task.id!) }}>
                                {isBusy ? <Spinner size={12} /> : '✓ Complete'}
                              </button>
                            )}
                            <button className="btn btn-ghost btn-sm"
                              onClick={e => { e.stopPropagation(); setDetailTaskId(task.id!) }}
                              style={{ marginLeft: 'auto' }}>
                              ⊙ Details
                            </button>
                          </div>
                          )}
                        </div>
                      </li>
                    )
                  })}
                </ul>
              )}
            </>
          )}
        </div>
      )}

      {/* ── Activities tab (all node types) ── */}
      {tab === 'activities' && (
        <div className="panel-body">
          {loadingTasks ? (
            <div style={{ display: 'flex', justifyContent: 'center', padding: '32px 0' }}>
              <Spinner />
            </div>
          ) : sortedActivities.length === 0 ? (
            <div className="empty-state" style={{ padding: '24px 0' }}>
              <p style={{ fontSize: 13 }}>No activity recorded for this instance.</p>
            </div>
          ) : (
            <ul className="task-timeline">
              {sortedActivities.map(act => {
                const active    = act.endTime === null
                const completed = act.endTime !== null
                const userTask  = taskByActivityId.get(act.activityId)
                const isOpen    = userTask && (userTask.state === 'CREATED' || userTask.state === 'ASSIGNED')
                const overdue   = userTask ? isOverdue(userTask.dueDate) : false
                const isBusy    = userTask ? completing === userTask.id : false

                return (
                  <li key={act.id} className="task-item"
                    style={userTask ? { cursor: 'pointer' } : undefined}
                    onClick={userTask ? () => setDetailTaskId(userTask.id) : undefined}
                    title={userTask ? 'Click to view task detail' : undefined}
                  >
                    <ActivityDot active={active} completed={completed} />
                    <div className="task-card">
                      <div className="task-card-top">
                        <span className="task-name">
                          {act.activityName ?? act.activityId}
                        </span>
                        <span style={{
                          fontSize: 10, padding: '1px 6px', borderRadius: 4,
                          background: active ? '#eff6ff' : completed ? '#dcfce7' : '#f1f5f9',
                          color: active ? '#1d4ed8' : completed ? '#15803d' : '#64748b',
                          border: `1px solid ${active ? '#bfdbfe' : completed ? '#bbf7d0' : '#e2e8f0'}`,
                          whiteSpace: 'nowrap',
                        }}>
                          {active ? 'Active' : completed ? 'Done' : 'Pending'}
                        </span>
                      </div>

                      <div className="task-meta-row">
                        <span style={{ color: 'var(--muted)', fontSize: 11 }}>
                          {activityTypeLabel(act.activityType)}
                        </span>
                        {userTask?.assignee && <span title="Assignee">👤 {userTask.assignee}</span>}
                        {userTask?.candidateGroups && <span title="Candidate groups">👥 {userTask.candidateGroups}</span>}
                        {userTask?.dueDate && (
                          <span className={overdue && isOpen ? 'task-due-overdue' : ''} title="Due">
                            🕐 {fmtShort(userTask.dueDate)}{overdue && isOpen ? ' ⚠' : ''}
                          </span>
                        )}
                        <span className="text-muted">Started {fmtShort(act.startTime)}</span>
                        {act.endTime && <span className="text-muted">✓ {fmtShort(act.endTime)}</span>}
                      </div>

                      {/* Inline claim form */}
                      {userTask?.id && claimId === userTask.id && (
                        <div className="gap-8" style={{ marginBottom: 8 }}>
                          <input className="form-control" style={{ flex: 1 }}
                            placeholder="Username…" value={claimUser}
                            onChange={e => setClaimUser(e.target.value)}
                            onKeyDown={e => {
                              if (e.key === 'Enter') claimTask(userTask.id!)
                              if (e.key === 'Escape') { setClaimId(''); setClaimUser('') }
                            }}
                            autoFocus
                          />
                          <button className="btn btn-primary btn-sm" onClick={() => claimTask(userTask.id!)}>OK</button>
                          <button className="btn btn-ghost btn-sm" onClick={() => { setClaimId(''); setClaimUser('') }}>✕</button>
                        </div>
                      )}

                      {/* Claim / Complete + Details */}
                      {userTask?.id && (
                        <div className="task-actions">
                          {isOpen && !userTask.assignee && claimId !== userTask.id && (
                            <button className="btn btn-ghost btn-sm"
                              onClick={e => { e.stopPropagation(); claimTask(userTask.id!) }}>Claim</button>
                          )}
                          {isOpen && (
                            <button className="btn btn-primary btn-sm" disabled={isBusy}
                              onClick={e => { e.stopPropagation(); completeTask(userTask.id!) }}>
                              {isBusy ? <Spinner size={12} /> : '✓ Complete'}
                            </button>
                          )}
                          <button className="btn btn-ghost btn-sm"
                            onClick={e => { e.stopPropagation(); setDetailTaskId(userTask.id!) }}
                            style={{ marginLeft: 'auto' }}>
                            ⊙ Details
                          </button>
                        </div>
                      )}
                    </div>
                  </li>
                )
              })}
            </ul>
          )}
        </div>
      )}

      {/* ── Task detail modal ── */}
      {detailTaskId && (
        <TaskDetailModal
          taskId={detailTaskId}
          onClose={() => setDetailTaskId(null)}
        />
      )}
    </div>
  )
}

// ─── Start Instance Modal ─────────────────────────────────────────────────────

function StartModal({ onClose, onStarted }: { onClose: () => void; onStarted: (i: ProcessInstanceResult) => void }) {
  const [defs, setDefs]               = useState<ProcessDefinitionDto[]>([])
  const [selectedKey, setSelectedKey] = useState('')
  const [businessKey, setBusinessKey] = useState('')
  const [varsJson, setVarsJson]       = useState('{}')
  const [busy, setBusy]               = useState(false)

  useEffect(() => { definitionsApi.list().then(setDefs).catch(() => {}) }, [])

  const submit = async () => {
    if (!selectedKey) { toast.error('Select a process'); return }
    let vars: Record<string, unknown> = {}
    try { vars = JSON.parse(varsJson) } catch { toast.error('Variables must be valid JSON'); return }
    setBusy(true)
    try {
      const r = await instancesApi.start(selectedKey, businessKey || undefined, vars)
      toast.success(`Instance ${r.id.slice(0, 8)}… started`)
      onStarted(r); onClose()
    } catch (e: any) { toast.error(e.message) }
    finally { setBusy(false) }
  }

  return (
    <Modal title="Start Process Instance" onClose={onClose}
      footer={<>
        <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" onClick={submit} disabled={busy}>
          {busy && <Spinner size={14} />} Start
        </button>
      </>}>
      <div className="form-group">
        <label>Process key</label>
        <select className="form-control" value={selectedKey} onChange={e => setSelectedKey(e.target.value)}>
          <option value="">— select —</option>
          {defs.map(d => <option key={d.id} value={d.key}>{d.key}{d.name ? ` — ${d.name}` : ''} (v{d.version})</option>)}
        </select>
      </div>
      <div className="form-group">
        <label>Business key (optional)</label>
        <input className="form-control" placeholder="ORDER-001" value={businessKey}
          onChange={e => setBusinessKey(e.target.value)} />
      </div>
      <div className="form-group">
        <label>Variables (JSON)</label>
        <textarea className="form-control mono" rows={5} value={varsJson}
          onChange={e => setVarsJson(e.target.value)} />
      </div>
    </Modal>
  )
}

// ─── Get Instance Modal ───────────────────────────────────────────────────────

function GetModal({ onClose }: { onClose: () => void }) {
  const [id, setId]         = useState('')
  const [result, setResult] = useState<ProcessInstanceResult | null>(null)
  const [busy, setBusy]     = useState(false)

  const lookup = async () => {
    if (!id.trim()) return
    setBusy(true)
    try { setResult(await instancesApi.get(id.trim())) }
    catch (e: any) { toast.error(e.message) }
    finally { setBusy(false) }
  }

  return (
    <Modal title="Get Instance" onClose={onClose}>
      <div className="form-group gap-8">
        <input className="form-control" placeholder="Instance ID…" value={id}
          onChange={e => setId(e.target.value)} onKeyDown={e => e.key === 'Enter' && lookup()} />
        <button className="btn btn-ghost" onClick={lookup} disabled={busy}>
          {busy ? <Spinner size={14} /> : 'Lookup'}
        </button>
      </div>
      {result && (
        <div className="card">
          <table style={{ fontSize: 13 }}>
            <tbody>
              {Object.entries(result).map(([k, v]) => (
                <tr key={k}>
                  <td style={{ fontWeight: 600, paddingRight: 12, color: 'var(--muted)', whiteSpace: 'nowrap' }}>{k}</td>
                  <td className="mono">{String(v ?? '—')}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </Modal>
  )
}

// ─── Main page ────────────────────────────────────────────────────────────────

export default function InstancesPage() {
  const [instances, setInstances]   = useState<ProcessInstanceResult[]>([])
  const [loading, setLoading]       = useState(false)
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [showStart, setShowStart]   = useState(false)
  const [showGet, setShowGet]       = useState(false)

  const loadInstances = useCallback(async () => {
    setLoading(true)
    try { setInstances(await instancesApi.list()) }
    catch (e: any) { toast.error(e.message) }
    finally { setLoading(false) }
  }, [])

  useEffect(() => { loadInstances() }, [loadInstances])

  const refreshInstance = useCallback(async (id: string) => {
    try {
      const fresh = await instancesApi.get(id)
      setInstances(prev => prev.map(i => i.id === id ? fresh : i))
    } catch { /* ignore */ }
  }, [])

  const selectedInstance = instances.find(i => i.id === selectedId) ?? null

  const handleRowClick = (id: string) =>
    setSelectedId(id)

  const suspend = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation()
    try { await instancesApi.suspend(id); toast.success('Suspended'); setInstances(prev => prev.map(i => i.id === id ? { ...i, state: 'SUSPENDED' } : i)) }
    catch (e: any) { toast.error(e.message) }
  }
  const activate = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation()
    try { await instancesApi.activate(id); toast.success('Activated'); setInstances(prev => prev.map(i => i.id === id ? { ...i, state: 'RUNNING' } : i)) }
    catch (e: any) { toast.error(e.message) }
  }
  const terminate = async (id: string, e: React.MouseEvent) => {
    e.stopPropagation()
    const reason = prompt('Termination reason:', 'Terminated via UI')
    if (reason === null) return
    try { await instancesApi.terminate(id, reason); toast.success('Terminated'); setInstances(prev => prev.map(i => i.id === id ? { ...i, state: 'TERMINATED' } : i)) }
    catch (e: any) { toast.error(e.message) }
  }

  return (
    <>
      <PageHeader title="Process Instances"
        actions={<div className="btn-row">
          <button className="btn btn-ghost" onClick={loadInstances} disabled={loading}>↻ Refresh</button>
          <button className="btn btn-ghost" onClick={() => setShowGet(true)}>🔍 Lookup</button>
          <button className="btn btn-primary" onClick={() => setShowStart(true)}>▶ Start</button>
        </div>} />

      <div className="page" style={{ maxWidth: '100%' }}>
        <div className="instances-layout">

          {/* ── Left: table ── */}
          <div className={`instances-table-col${selectedInstance ? ' panel-open' : ''}`}>
            {loading && instances.length === 0 ? (
              <div className="card"><div className="empty-state"><Spinner size={20} /></div></div>
            ) : instances.length === 0 ? (
              <div className="card">
                <div className="empty-state">
                  <p>No process instances.<br />Start one to get started.</p>
                </div>
              </div>
            ) : (
              <div className="card" style={{ padding: 0 }}>
                <div className="table-wrap">
                  <table>
                    <thead><tr>
                      <th>ID</th><th>Key</th><th>Business key</th>
                      <th>State</th><th>Started</th><th>Actions</th>
                    </tr></thead>
                    <tbody>
                      {instances.map(i => (
                        <tr key={i.id}
                          onClick={() => handleRowClick(i.id)}
                          className={selectedId === i.id ? 'row-selected' : ''}
                          style={{ cursor: 'pointer' }}
                          title="Click to view diagram & tasks"
                        >
                          <td className="mono" style={{ fontSize: 11 }} title={i.id}>{i.id.slice(0, 10)}…</td>
                          <td>{i.processDefinitionKey}</td>
                          <td className="text-muted">{i.businessKey ?? '—'}</td>
                          <td><StateBadge state={i.state} /></td>
                          <td className="text-muted" style={{ whiteSpace: 'nowrap' }}>{fmt(i.startTime)}</td>
                          <td>
                            <div className="btn-row" onClick={e => e.stopPropagation()}>
                              <button className="btn btn-ghost btn-sm" onClick={() => refreshInstance(i.id)} title="Refresh">↻</button>
                              {i.state === 'RUNNING' && <>
                                <button className="btn btn-ghost btn-sm" onClick={e => suspend(i.id, e)}>Suspend</button>
                                <button className="btn btn-danger btn-sm" onClick={e => terminate(i.id, e)}>Terminate</button>
                              </>}
                              {i.state === 'SUSPENDED' &&
                                <button className="btn btn-ghost btn-sm" onClick={e => activate(i.id, e)}>Activate</button>}
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>

          {/* ── Right: diagram + tasks panel ── */}
          {selectedInstance && (
            <InstancePanel
              key={selectedInstance.id}
              instance={selectedInstance}
              onClose={() => setSelectedId(null)}
              onInstanceChange={refreshInstance}
            />
          )}
        </div>
      </div>

      {showStart && (
        <StartModal
          onClose={() => setShowStart(false)}
          onStarted={inst => {
            setInstances(prev => [inst, ...prev])
            setSelectedId(inst.id)   // auto-open the panel on the new instance
            setShowStart(false)
          }}
        />
      )}
      {showGet && <GetModal onClose={() => setShowGet(false)} />}
    </>
  )
}

/**
 * TaskDetailModal
 *
 * Shows a full task detail view in a modal:
 *   • Info tab  — task fields (assignee, priority, dates, form key, description)
 *   • Variables tab — process-scope and local (execution-scoped) variables
 *   • I/O tab    — data input / output associations from the BPMN model
 */

import { useState, useEffect } from 'react'
import { tasksApi } from '../api'
import { useSidebar } from '../SidebarContext'
import type { TaskDetailDto, TaskVariableDto, TaskIoMappingDto } from '../types'
import Spinner from './Spinner'

// ── helpers ───────────────────────────────────────────────────────────────────

function fmt(iso: string | null | undefined) {
  if (!iso) return '—'
  return new Date(iso).toLocaleString()
}

function StatePill({ state }: { state: string }) {
  const cfg: Record<string, { bg: string; color: string; border: string }> = {
    CREATED:   { bg: '#eff6ff', color: '#1d4ed8', border: '#bfdbfe' },
    ASSIGNED:  { bg: '#ede9fe', color: '#5b21b6', border: '#ddd6fe' },
    COMPLETED: { bg: '#dcfce7', color: '#15803d', border: '#bbf7d0' },
    DELETED:   { bg: '#fee2e2', color: '#b91c1c', border: '#fecaca' },
  }
  const s = cfg[state] ?? { bg: '#f1f5f9', color: '#64748b', border: '#e2e8f0' }
  return (
    <span style={{
      fontSize: 11, padding: '2px 8px', borderRadius: 4,
      background: s.bg, color: s.color, border: `1px solid ${s.border}`,
      fontWeight: 600, whiteSpace: 'nowrap',
    }}>{state}</span>
  )
}

// ── Variable table ────────────────────────────────────────────────────────────

function VarTable({ rows }: { rows: TaskVariableDto[] }) {
  if (rows.length === 0) return <p className="empty-vars">No variables.</p>
  return (
    <table className="var-table">
      <thead>
        <tr>
          <th>Name</th>
          <th>Type</th>
          <th>Value</th>
          <th>Updated</th>
        </tr>
      </thead>
      <tbody>
        {rows.map(v => (
          <tr key={v.name}>
            <td><span className="var-name">{v.name}</span></td>
            <td><span className="var-type">{v.type}</span></td>
            <td><span className="var-value">{v.value}</span></td>
            <td style={{ color: 'var(--muted)', fontSize: 11, whiteSpace: 'nowrap' }}>{fmt(v.updatedAt)}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

// ── I/O mapping table ─────────────────────────────────────────────────────────

function IoTable({ rows, emptyMsg }: { rows: TaskIoMappingDto[]; emptyMsg: string }) {
  if (rows.length === 0) return <p className="empty-vars">{emptyMsg}</p>
  return (
    <table className="var-table">
      <thead>
        <tr>
          <th>Source</th>
          <th>Target</th>
          <th>Transformation</th>
        </tr>
      </thead>
      <tbody>
        {rows.map((m, i) => (
          <tr key={i}>
            <td><span className="var-name">{m.source ?? '—'}</span></td>
            <td><span className="var-name">{m.target ?? '—'}</span></td>
            <td style={{ color: 'var(--muted)', fontFamily: 'monospace', fontSize: 11 }}>{m.transformation ?? '—'}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

// ── Main component ────────────────────────────────────────────────────────────

type Tab = 'info' | 'variables' | 'io'

interface Props {
  taskId: string
  onClose: () => void
}

export default function TaskDetailModal({ taskId, onClose }: Props) {
  const { open: sidebarOpen } = useSidebar()
  const [detail, setDetail]   = useState<TaskDetailDto | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError]     = useState<string | null>(null)
  const [tab, setTab]         = useState<Tab>('info')

  useEffect(() => {
    setLoading(true)
    setError(null)
    tasksApi.detail(taskId)
      .then(setDetail)
      .catch(e => setError(e.message))
      .finally(() => setLoading(false))
  }, [taskId])

  // Close on Escape
  useEffect(() => {
    const fn = (e: KeyboardEvent) => { if (e.key === 'Escape') onClose() }
    window.addEventListener('keydown', fn)
    return () => window.removeEventListener('keydown', fn)
  }, [onClose])

  const hasIo = detail && (detail.inputs.length > 0 || detail.outputs.length > 0)

  return (
    <div className={`modal-overlay${sidebarOpen ? '' : ' sidebar-collapsed'}`} onClick={e => { if (e.target === e.currentTarget) onClose() }}>
      <div className="modal task-detail-modal">

        {/* Header */}
        <div className="task-detail-header">
          <div style={{ flex: 1, minWidth: 0 }}>
            <div className="task-detail-title">
              {detail?.name ?? 'Task Detail'}
              {detail && <StatePill state={detail.state} />}
            </div>
            {detail && (
              <div className="task-detail-subtitle">
                {detail.taskDefinitionKey} · {detail.id.slice(0, 16)}…
              </div>
            )}
          </div>
          <button className="modal-close" onClick={onClose} title="Close">✕</button>
        </div>

        {/* Tabs */}
        <div className="task-detail-tabs">
          {(['info', 'variables', 'io'] as Tab[]).map(t => (
            <button
              key={t}
              className={`task-detail-tab${tab === t ? ' active' : ''}`}
              onClick={() => setTab(t)}
            >
              {t === 'info'      && '⊙ Info'}
              {t === 'variables' && `⊟ Variables${detail ? ` (${detail.processVariables.length + detail.localVariables.length})` : ''}`}
              {t === 'io'        && `⇄ I/O${hasIo ? ` (${detail!.inputs.length + detail!.outputs.length})` : ''}`}
            </button>
          ))}
        </div>

        {/* Body */}
        <div className="task-detail-body">
          {loading && (
            <div style={{ display: 'flex', justifyContent: 'center', padding: '40px 0' }}>
              <Spinner />
            </div>
          )}

          {error && (
            <p style={{ color: 'var(--danger)', fontSize: 13, textAlign: 'center', padding: '32px 0' }}>
              {error}
            </p>
          )}

          {detail && !loading && (

            // ── Info tab ──────────────────────────────────────────────────────
            tab === 'info' ? (
              <dl className="task-detail-grid">
                <dt>State</dt>        <dd><StatePill state={detail.state} /></dd>
                <dt>Assignee</dt>     <dd>{detail.assignee ?? '—'}</dd>
                <dt>Owner</dt>        <dd>{detail.owner ?? '—'}</dd>
                <dt>Candidates</dt>   <dd>{detail.candidateUsers || detail.candidateGroups
                  ? [detail.candidateUsers, detail.candidateGroups].filter(Boolean).join(' · ')
                  : '—'}</dd>
                <dt>Priority</dt>     <dd>{detail.priority}</dd>
                <dt>Due date</dt>     <dd>{fmt(detail.dueDate)}</dd>
                <dt>Form key</dt>     <dd style={{ fontFamily: 'monospace' }}>{detail.formKey ?? '—'}</dd>
                <dt>Created</dt>      <dd>{fmt(detail.createdAt)}</dd>
                <dt>Updated</dt>      <dd>{fmt(detail.updatedAt)}</dd>
                <dt>Completed</dt>    <dd>{fmt(detail.completedAt)}</dd>
                <dt>Process key</dt>  <dd style={{ fontFamily: 'monospace', fontSize: 11 }}>{detail.processDefinitionKey ?? '—'}</dd>
                <dt>Instance ID</dt>  <dd style={{ fontFamily: 'monospace', fontSize: 11 }}>{detail.processInstanceId}</dd>
                <dt>Execution ID</dt> <dd style={{ fontFamily: 'monospace', fontSize: 11 }}>{detail.executionId}</dd>
                {detail.description && <>
                  <dt>Description</dt>
                  <dd style={{ whiteSpace: 'pre-wrap', alignItems: 'flex-start', paddingTop: 8 }}>{detail.description}</dd>
                </>}
              </dl>

            // ── Variables tab ─────────────────────────────────────────────────
            ) : tab === 'variables' ? (
              <>
                <p className="section-label">Process variables ({detail.processVariables.length})</p>
                <VarTable rows={detail.processVariables} />
                <p className="section-label" style={{ marginTop: 20 }}>Local variables ({detail.localVariables.length})</p>
                <VarTable rows={detail.localVariables} />
              </>

            // ── I/O tab ───────────────────────────────────────────────────────
            ) : (
              <>
                <p className="section-label">Data inputs ({detail.inputs.length})</p>
                <IoTable rows={detail.inputs} emptyMsg="No data input mappings declared in the BPMN model." />
                <p className="section-label" style={{ marginTop: 20 }}>Data outputs ({detail.outputs.length})</p>
                <IoTable rows={detail.outputs} emptyMsg="No data output mappings declared in the BPMN model." />
              </>
            )
          )}
        </div>

      </div>
    </div>
  )
}

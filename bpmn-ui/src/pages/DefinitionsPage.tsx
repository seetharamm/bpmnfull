import { useState, useEffect, useCallback } from 'react'
import toast from 'react-hot-toast'
import { definitionsApi } from '../api'
import type { ProcessDefinitionDto, ProcessVersionSummaryDto, DeployProcessRequest } from '../types'
import PageHeader from '../components/PageHeader'
import Modal from '../components/Modal'
import Spinner from '../components/Spinner'

// ── Deploy Modal ──────────────────────────────────────────────────────────────
function DeployModal({ onClose, onDeployed }: { onClose: () => void; onDeployed: () => void }) {
  const [form, setForm] = useState<DeployProcessRequest>({ bpmnXml: '', resourceName: '', deployedBy: '', deployMessage: '' })
  const [busy, setBusy] = useState(false)

  const handleFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    const reader = new FileReader()
    reader.onload = ev => setForm(f => ({ ...f, bpmnXml: ev.target?.result as string, resourceName: f.resourceName || file.name }))
    reader.readAsText(file)
  }

  const submit = async () => {
    if (!form.bpmnXml.trim()) { toast.error('BPMN XML is required'); return }
    setBusy(true)
    try {
      const def = await definitionsApi.deploy(form)
      toast.success(`Deployed ${def.key} v${def.version}`)
      onDeployed()
      onClose()
    } catch (e: any) { toast.error(e.message) }
    finally { setBusy(false) }
  }

  return (
    <Modal title="Deploy Process" onClose={onClose} wide
      footer={<>
        <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" onClick={submit} disabled={busy}>
          {busy && <Spinner size={14} />} Deploy
        </button>
      </>}>
      <div className="form-group">
        <label>Upload .bpmn / .xml file</label>
        <input type="file" accept=".bpmn,.xml,.bpmn20.xml" onChange={handleFile} className="form-control" />
      </div>
      <div className="form-group">
        <label>Resource name</label>
        <input className="form-control" placeholder="my-process.bpmn20.xml"
          value={form.resourceName ?? ''} onChange={e => setForm(f => ({ ...f, resourceName: e.target.value }))} />
      </div>
      <div className="form-group">
        <label>Deployed by</label>
        <input className="form-control" placeholder="alice"
          value={form.deployedBy ?? ''} onChange={e => setForm(f => ({ ...f, deployedBy: e.target.value }))} />
      </div>
      <div className="form-group">
        <label>Deploy message / changelog</label>
        <input className="form-control" placeholder="Initial version"
          value={form.deployMessage ?? ''} onChange={e => setForm(f => ({ ...f, deployMessage: e.target.value }))} />
      </div>
      <div className="form-group">
        <label>BPMN 2.0 XML</label>
        <textarea className="form-control mono" rows={10}
          placeholder='<?xml version="1.0"?>...'
          value={form.bpmnXml} onChange={e => setForm(f => ({ ...f, bpmnXml: e.target.value }))} />
      </div>
    </Modal>
  )
}

// ── Version History Modal ─────────────────────────────────────────────────────
function VersionHistoryModal({ defKey, onClose, onChanged }: { defKey: string; onClose: () => void; onChanged: () => void }) {
  const [summary, setSummary] = useState<ProcessVersionSummaryDto | null>(null)
  const [loading, setLoading] = useState(true)

  const load = useCallback(async () => {
    setLoading(true)
    try { setSummary(await definitionsApi.getVersionHistory(defKey)) }
    catch (e: any) { toast.error(e.message) }
    finally { setLoading(false) }
  }, [defKey])

  useEffect(() => { load() }, [load])

  const setActive = async (v: number) => {
    try { await definitionsApi.setActive(defKey, v); toast.success(`v${v} is now active`); load(); onChanged() }
    catch (e: any) { toast.error(e.message) }
  }
  const suspendV = async (v: number) => {
    try { await definitionsApi.suspendVersion(defKey, v); toast.success(`v${v} suspended`); load(); onChanged() }
    catch (e: any) { toast.error(e.message) }
  }
  const resumeV = async (v: number) => {
    try { await definitionsApi.resumeVersion(defKey, v); toast.success(`v${v} resumed`); load(); onChanged() }
    catch (e: any) { toast.error(e.message) }
  }
  const deleteV = async (v: number) => {
    if (!confirm(`Delete ${defKey} v${v}?`)) return
    try { await definitionsApi.deleteVersion(defKey, v); toast.success(`v${v} deleted`); load(); onChanged() }
    catch (e: any) { toast.error(e.message) }
  }
  const downloadBpmn = (v: number) => window.open(definitionsApi.bpmnUrl(defKey, v), '_blank')

  return (
    <Modal title={`Version history — ${defKey}`} onClose={onClose} wide>
      {loading ? <div style={{ textAlign: 'center', padding: 32 }}><Spinner /></div> : summary ? (
        <>
          <div className="card" style={{ marginBottom: 12 }}>
            <div className="gap-8" style={{ flexWrap: 'wrap', fontSize: 13 }}>
              <span><b>Latest:</b> v{summary.latestVersion}</span>
              <span><b>Active:</b> v{summary.activeVersion}</span>
              <span><b>Versions:</b> {summary.totalVersions}</span>
              <span><b>Running instances:</b> {summary.totalRunningInstances}</span>
              {summary.suspended && <span className="badge badge-yellow">ALL SUSPENDED</span>}
            </div>
          </div>
          <div className="table-wrap">
            <table>
              <thead><tr>
                <th>Version</th><th>Status</th><th>Deployed by</th><th>Message</th><th>Deployed at</th><th>Actions</th>
              </tr></thead>
              <tbody>
                {summary.versions.map(v => (
                  <tr key={v.version}>
                    <td><b>v{v.version}</b></td>
                    <td>
                      {v.active && <span className="badge badge-green mr-4">ACTIVE</span>}
                      {v.suspended && <span className="badge badge-yellow">SUSPENDED</span>}
                      {!v.active && !v.suspended && <span className="badge badge-gray">INACTIVE</span>}
                    </td>
                    <td className="text-muted">{v.deployedBy ?? '—'}</td>
                    <td className="truncate">{v.deployMessage ?? '—'}</td>
                    <td className="text-muted">{new Date(v.createdAt).toLocaleString()}</td>
                    <td>
                      <div className="btn-row">
                        {!v.active && <button className="btn btn-ghost btn-sm" onClick={() => setActive(v.version)}>Set active</button>}
                        {v.suspended
                          ? <button className="btn btn-ghost btn-sm" onClick={() => resumeV(v.version)}>Resume</button>
                          : <button className="btn btn-ghost btn-sm" onClick={() => suspendV(v.version)}>Suspend</button>}
                        <button className="btn btn-ghost btn-sm" onClick={() => downloadBpmn(v.version)}>↓ XML</button>
                        <button className="btn btn-danger btn-sm" onClick={() => deleteV(v.version)}>Delete</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      ) : <p className="text-muted">No data</p>}
    </Modal>
  )
}

// ── Start Modal ───────────────────────────────────────────────────────────────
function StartModal({ def, onClose }: { def: ProcessDefinitionDto; onClose: () => void }) {
  const [businessKey, setBusinessKey] = useState('')
  const [varsJson, setVarsJson] = useState('{}')
  const [busy, setBusy] = useState(false)

  const submit = async () => {
    let vars: Record<string, unknown> = {}
    try { vars = JSON.parse(varsJson) } catch { toast.error('Variables must be valid JSON'); return }
    setBusy(true)
    try {
      const result = await definitionsApi.startVersion(def.key, def.version, businessKey || undefined, vars)
      toast.success(`Instance ${result.id.slice(0, 8)}… started`)
      onClose()
    } catch (e: any) { toast.error(e.message) }
    finally { setBusy(false) }
  }

  return (
    <Modal title={`Start ${def.key} v${def.version}`} onClose={onClose}
      footer={<>
        <button className="btn btn-ghost" onClick={onClose}>Cancel</button>
        <button className="btn btn-primary" onClick={submit} disabled={busy}>
          {busy && <Spinner size={14} />} Start
        </button>
      </>}>
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

// ── Main Page ─────────────────────────────────────────────────────────────────
export default function DefinitionsPage() {
  const [defs, setDefs] = useState<ProcessDefinitionDto[]>([])
  const [loading, setLoading] = useState(true)
  const [search, setSearch] = useState('')
  const [showDeploy, setShowDeploy] = useState(false)
  const [histKey, setHistKey] = useState<string | null>(null)
  const [startDef, setStartDef] = useState<ProcessDefinitionDto | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    try { setDefs(await definitionsApi.list(search || undefined)) }
    catch (e: any) { toast.error(e.message) }
    finally { setLoading(false) }
  }, [search])

  useEffect(() => { const t = setTimeout(load, 300); return () => clearTimeout(t) }, [load])

  const suspendAll = async (key: string) => {
    try { await definitionsApi.suspendAll(key); toast.success(`${key} suspended`); load() }
    catch (e: any) { toast.error(e.message) }
  }
  const activateAll = async (key: string) => {
    try { await definitionsApi.activateAll(key); toast.success(`${key} activated`); load() }
    catch (e: any) { toast.error(e.message) }
  }
  const deleteAll = async (key: string) => {
    if (!confirm(`Delete ALL versions of "${key}"?`)) return
    try { await definitionsApi.deleteAll(key); toast.success(`${key} deleted`); load() }
    catch (e: any) { toast.error(e.message) }
  }

  return (
    <>
      <PageHeader title="Process Definitions"
        actions={<button className="btn btn-primary" onClick={() => setShowDeploy(true)}>+ Deploy</button>} />
      <div className="page">
        <div className="search-row">
          <input className="form-control" placeholder="Search by key or name…"
            value={search} onChange={e => setSearch(e.target.value)} />
          <button className="btn btn-ghost" onClick={load}>↻ Refresh</button>
        </div>

        <div className="card" style={{ padding: 0 }}>
          {loading ? (
            <div className="empty-state"><Spinner /></div>
          ) : defs.length === 0 ? (
            <div className="empty-state"><p>No process definitions found.<br />Deploy a BPMN 2.0 XML to get started.</p></div>
          ) : (
            <div className="table-wrap">
              <table>
                <thead><tr>
                  <th>Key</th><th>Name</th><th>Active v</th><th>Status</th>
                  <th>Deployed by</th><th>Created</th><th>Actions</th>
                </tr></thead>
                <tbody>
                  {defs.map(d => (
                    <tr key={d.id}>
                      <td><span className="mono">{d.key}</span></td>
                      <td>{d.name ?? <span className="text-muted">—</span>}</td>
                      <td>v{d.version}</td>
                      <td>
                        {d.suspended
                          ? <span className="badge badge-yellow">SUSPENDED</span>
                          : <span className="badge badge-green">ACTIVE</span>}
                      </td>
                      <td className="text-muted">{d.deployedBy ?? '—'}</td>
                      <td className="text-muted">{new Date(d.createdAt).toLocaleDateString()}</td>
                      <td>
                        <div className="btn-row">
                          <button className="btn btn-primary btn-sm" onClick={() => setStartDef(d)}>▶ Start</button>
                          <button className="btn btn-ghost btn-sm" onClick={() => setHistKey(d.key)}>Versions</button>
                          {d.suspended
                            ? <button className="btn btn-ghost btn-sm" onClick={() => activateAll(d.key)}>Activate</button>
                            : <button className="btn btn-ghost btn-sm" onClick={() => suspendAll(d.key)}>Suspend</button>}
                          <button className="btn btn-danger btn-sm" onClick={() => deleteAll(d.key)}>Delete</button>
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

      {showDeploy && <DeployModal onClose={() => setShowDeploy(false)} onDeployed={load} />}
      {histKey    && <VersionHistoryModal defKey={histKey} onClose={() => setHistKey(null)} onChanged={load} />}
      {startDef   && <StartModal def={startDef} onClose={() => setStartDef(null)} />}
    </>
  )
}

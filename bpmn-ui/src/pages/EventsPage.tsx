import { useState } from 'react'
import toast from 'react-hot-toast'
import { eventsApi } from '../api'
import PageHeader from '../components/PageHeader'
import Spinner from '../components/Spinner'

export default function EventsPage() {
  // Message correlation
  const [msgName, setMsgName] = useState('')
  const [msgBizKey, setMsgBizKey] = useState('')
  const [msgVars, setMsgVars] = useState('{}')
  const [msgBusy, setMsgBusy] = useState(false)

  // Signal broadcast
  const [sigName, setSigName] = useState('')
  const [sigVars, setSigVars] = useState('{}')
  const [sigBusy, setSigBusy] = useState(false)

  const correlate = async () => {
    if (!msgName.trim()) { toast.error('Message name required'); return }
    let vars: Record<string, unknown> = {}
    try { vars = JSON.parse(msgVars) } catch { toast.error('Variables must be valid JSON'); return }
    setMsgBusy(true)
    try { await eventsApi.correlateMessage(msgName.trim(), msgBizKey || undefined, vars); toast.success(`Message "${msgName}" correlated`) }
    catch (e: any) { toast.error(e.message) }
    finally { setMsgBusy(false) }
  }

  const broadcast = async () => {
    if (!sigName.trim()) { toast.error('Signal name required'); return }
    let vars: Record<string, unknown> = {}
    try { vars = JSON.parse(sigVars) } catch { toast.error('Variables must be valid JSON'); return }
    setSigBusy(true)
    try { await eventsApi.broadcastSignal(sigName.trim(), vars); toast.success(`Signal "${sigName}" broadcast`) }
    catch (e: any) { toast.error(e.message) }
    finally { setSigBusy(false) }
  }

  return (
    <>
      <PageHeader title="Messages & Signals" />
      <div className="page">
        {/* Message */}
        <div className="card">
          <div className="card-header">
            <span className="card-title">✉ Correlate Message</span>
          </div>
          <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 14 }}>
            Delivers a named message to a waiting process instance (Message Catch / Receive Task).
          </p>
          <div className="form-group">
            <label>Message name *</label>
            <input className="form-control" placeholder="PaymentReceived" value={msgName}
              onChange={e => setMsgName(e.target.value)} />
          </div>
          <div className="form-group">
            <label>Business key (optional — narrows to one instance)</label>
            <input className="form-control" placeholder="ORDER-001" value={msgBizKey}
              onChange={e => setMsgBizKey(e.target.value)} />
          </div>
          <div className="form-group">
            <label>Variables (JSON)</label>
            <textarea className="form-control mono" rows={4} value={msgVars}
              onChange={e => setMsgVars(e.target.value)} />
          </div>
          <button className="btn btn-primary" onClick={correlate} disabled={msgBusy}>
            {msgBusy ? <Spinner size={14} /> : '✉'} Correlate
          </button>
        </div>

        {/* Signal */}
        <div className="card">
          <div className="card-header">
            <span className="card-title">📡 Broadcast Signal</span>
          </div>
          <p style={{ fontSize: 13, color: 'var(--muted)', marginBottom: 14 }}>
            Broadcasts a named signal to ALL waiting process instances (Signal Catch Events).
          </p>
          <div className="form-group">
            <label>Signal name *</label>
            <input className="form-control" placeholder="CancelOrder" value={sigName}
              onChange={e => setSigName(e.target.value)} />
          </div>
          <div className="form-group">
            <label>Variables (JSON)</label>
            <textarea className="form-control mono" rows={4} value={sigVars}
              onChange={e => setSigVars(e.target.value)} />
          </div>
          <button className="btn btn-primary" onClick={broadcast} disabled={sigBusy}>
            {sigBusy ? <Spinner size={14} /> : '📡'} Broadcast
          </button>
        </div>
      </div>
    </>
  )
}

import React, { useEffect, useState, useCallback } from 'react';
import { requestService } from '../services/requestService';
import '../styles/dashboard.css';

function StatusBadge({ status }) {
  const map = {
    COMPLETED: { cls: 'badge-completed', label: 'Completed' },
    APPROVED: { cls: 'badge-approved', label: 'Approved' },
    REJECTED: { cls: 'badge-rejected', label: 'Rejected' },
  };
  const s = map[status] || { cls: 'badge-pending', label: status || 'Unknown' };
  return <span className={`badge ${s.cls}`}>{s.label}</span>;
}

function DetailModal({ request, onClose }) {
  return (
    <div className="modal-overlay">
      <div className="modal">
        <div className="modal-header">
          <div className="modal-title">Request #{request.id} Details</div>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>
        <div className="modal-body">
          {[
            ['Title', request.title],
            ['Process Type', request.processType],
            ['Priority', request.priority],
            ['Requestor', request.requestorName],
            ['Email', request.requestorEmail],
            ['Status', <StatusBadge status={request.status} />],
            ['Description', request.description],
            ['Analyst Notes', request.analystNotes || '—'],
            ['Approval Comments', request.approvalComments || '—'],
            ['Created', request.createdAt ? new Date(request.createdAt).toLocaleString() : '—'],
            ['Completed', request.completedAt ? new Date(request.completedAt).toLocaleString() : '—'],
          ].map(([label, value]) => (
            <div key={label} style={{ display: 'flex', gap: 12, marginBottom: 12, fontSize: 13 }}>
              <div style={{ width: 140, flexShrink: 0, fontWeight: 500, color: 'var(--color-muted)' }}>{label}</div>
              <div style={{ flex: 1 }}>{value}</div>
            </div>
          ))}
        </div>
        <div className="modal-footer">
          <button className="btn btn-secondary btn-sm" onClick={onClose}>Close</button>
        </div>
      </div>
    </div>
  );
}

export default function CompletedQueue() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState(null);
  const [filter, setFilter] = useState('ALL');

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await requestService.getCompletedQueue();
      setRequests(res.data || []);
    } catch (err) {
      setError('Failed to load completed requests. Please refresh.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  const filtered =
    filter === 'ALL' ? requests : requests.filter((r) => r.status === filter);

  return (
    <div>
      <div className="stat-grid">
        <div className="stat-card success">
          <div className="stat-label">Total Completed</div>
          <div className="stat-value">{requests.length}</div>
        </div>
        <div className="stat-card success">
          <div className="stat-label">Approved</div>
          <div className="stat-value">{requests.filter((r) => r.status === 'APPROVED' || r.status === 'COMPLETED').length}</div>
        </div>
        <div className="stat-card danger">
          <div className="stat-label">Rejected</div>
          <div className="stat-value">{requests.filter((r) => r.status === 'REJECTED').length}</div>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <span className="card-title">Completed Queue</span>
          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            <select
              className="form-control"
              style={{ width: 'auto', padding: '6px 10px', fontSize: 12 }}
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
            >
              <option value="ALL">All Statuses</option>
              <option value="COMPLETED">Completed</option>
              <option value="APPROVED">Approved</option>
              <option value="REJECTED">Rejected</option>
            </select>
            <button className="btn btn-secondary btn-sm" onClick={fetchData} disabled={loading}>
              {loading ? <span className="spinner" /> : '↺ Refresh'}
            </button>
          </div>
        </div>

        {error && <div className="alert alert-error" style={{ margin: '16px 20px 0' }}>{error}</div>}

        {loading ? (
          <div className="loading-wrapper">
            <span className="spinner" /> Loading completed requests…
          </div>
        ) : filtered.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">🏁</div>
            <div className="empty-state-title">No completed requests</div>
            <p>Processed requests will appear here.</p>
          </div>
        ) : (
          <div style={{ overflowX: 'auto' }}>
            <table className="data-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Title</th>
                  <th>Process Type</th>
                  <th>Requestor</th>
                  <th>Priority</th>
                  <th>Status</th>
                  <th>Created</th>
                  <th>Completed</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((r) => (
                  <tr key={r.id}>
                    <td style={{ color: 'var(--color-muted)', fontFamily: 'monospace' }}>{r.id}</td>
                    <td style={{ fontWeight: 500 }}>{r.title}</td>
                    <td>{r.processType}</td>
                    <td>
                      <div>{r.requestorName}</div>
                      <div style={{ fontSize: 11, color: 'var(--color-muted)' }}>{r.requestorEmail}</div>
                    </td>
                    <td style={{ fontSize: 12, fontWeight: 600 }}>{r.priority}</td>
                    <td><StatusBadge status={r.status} /></td>
                    <td style={{ fontSize: 12, color: 'var(--color-muted)' }}>
                      {r.createdAt ? new Date(r.createdAt).toLocaleDateString() : '—'}
                    </td>
                    <td style={{ fontSize: 12, color: 'var(--color-muted)' }}>
                      {r.completedAt ? new Date(r.completedAt).toLocaleDateString() : '—'}
                    </td>
                    <td>
                      <button className="btn btn-secondary btn-sm" onClick={() => setSelected(r)}>
                        View
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {selected && (
        <DetailModal request={selected} onClose={() => setSelected(null)} />
      )}
    </div>
  );
}

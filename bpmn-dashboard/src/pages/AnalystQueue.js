import React, { useEffect, useState, useCallback } from 'react';
import { requestService } from '../services/requestService';
import '../styles/dashboard.css';

function StatusBadge({ status }) {
  const map = {
    PENDING_ANALYSIS: { cls: 'badge-analysis', label: 'Pending Analysis' },
    IN_ANALYSIS: { cls: 'badge-analysis', label: 'In Analysis' },
    PENDING_APPROVAL: { cls: 'badge-approval', label: 'Pending Approval' },
    APPROVED: { cls: 'badge-approved', label: 'Approved' },
    REJECTED: { cls: 'badge-rejected', label: 'Rejected' },
    COMPLETED: { cls: 'badge-completed', label: 'Completed' },
  };
  const s = map[status] || { cls: 'badge-pending', label: status || 'Unknown' };
  return <span className={`badge ${s.cls}`}>{s.label}</span>;
}

function PriorityBadge({ priority }) {
  const colors = { LOW: '#3b82f6', MEDIUM: '#d97706', HIGH: '#ef4444', CRITICAL: '#7c3aed' };
  return (
    <span style={{ color: colors[priority] || 'inherit', fontWeight: 600, fontSize: 12 }}>
      {priority || '—'}
    </span>
  );
}

function AnalysisModal({ request, onClose, onDone }) {
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const submit = async (action) => {
    setSubmitting(true);
    setError('');
    try {
      await requestService.submitAnalysis(request.id, { action, analystNotes: notes });
      onDone();
    } catch (err) {
      setError(err?.response?.data?.message || 'Action failed. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="modal-overlay">
      <div className="modal">
        <div className="modal-header">
          <div className="modal-title">Analyse Request #{request.id}</div>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>
        <div className="modal-body">
          <p style={{ marginBottom: 14, fontSize: 13, color: 'var(--color-muted)' }}>
            <strong>Title:</strong> {request.title}
          </p>
          <p style={{ marginBottom: 14, fontSize: 13, color: 'var(--color-muted)' }}>
            <strong>Description:</strong> {request.description}
          </p>
          {error && <div className="alert alert-error">{error}</div>}
          <div className="form-group">
            <label className="form-label" htmlFor="analystNotes">Analyst Notes</label>
            <textarea
              id="analystNotes"
              className="form-control"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              placeholder="Add analysis notes before forwarding or rejecting…"
              rows={4}
            />
          </div>
        </div>
        <div className="modal-footer">
          <button className="btn btn-secondary btn-sm" onClick={onClose} disabled={submitting}>Cancel</button>
          <button className="btn btn-danger btn-sm" onClick={() => submit('REJECT')} disabled={submitting}>Reject</button>
          <button className="btn btn-primary btn-sm" onClick={() => submit('FORWARD')} disabled={submitting}>
            {submitting ? <span className="spinner" style={{ width: 14, height: 14, borderTopColor: '#fff', borderColor: 'rgba(255,255,255,0.3)' }} /> : 'Forward for Approval'}
          </button>
        </div>
      </div>
    </div>
  );
}

export default function AnalystQueue() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await requestService.getAnalystQueue();
      setRequests(res.data || []);
    } catch (err) {
      setError('Failed to load analyst queue. Please refresh.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleClaim = async (id) => {
    try {
      await requestService.claimAnalystTask(id);
      fetchData();
    } catch {
      alert('Could not claim task. It may have been claimed by another analyst.');
    }
  };

  const handleDone = () => {
    setSelected(null);
    fetchData();
  };

  return (
    <div>
      <div className="stat-grid">
        <div className="stat-card primary">
          <div className="stat-label">Total in Queue</div>
          <div className="stat-value">{requests.length}</div>
        </div>
        <div className="stat-card warning">
          <div className="stat-label">In Analysis</div>
          <div className="stat-value">{requests.filter((r) => r.status === 'IN_ANALYSIS').length}</div>
        </div>
        <div className="stat-card danger">
          <div className="stat-label">High / Critical</div>
          <div className="stat-value">{requests.filter((r) => ['HIGH', 'CRITICAL'].includes(r.priority)).length}</div>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <span className="card-title">Analyst Queue</span>
          <button className="btn btn-secondary btn-sm" onClick={fetchData} disabled={loading}>
            {loading ? <span className="spinner" /> : '↺ Refresh'}
          </button>
        </div>

        {error && <div className="alert alert-error" style={{ margin: '16px 20px 0' }}>{error}</div>}

        {loading ? (
          <div className="loading-wrapper">
            <span className="spinner" /> Loading requests…
          </div>
        ) : requests.length === 0 ? (
          <div className="empty-state">
            <div className="empty-state-icon">📋</div>
            <div className="empty-state-title">No requests in analyst queue</div>
            <p>Submitted requests will appear here.</p>
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
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {requests.map((r) => (
                  <tr key={r.id}>
                    <td style={{ color: 'var(--color-muted)', fontFamily: 'monospace' }}>{r.id}</td>
                    <td style={{ fontWeight: 500 }}>{r.title}</td>
                    <td>{r.processType}</td>
                    <td>
                      <div>{r.requestorName}</div>
                      <div style={{ fontSize: 11, color: 'var(--color-muted)' }}>{r.requestorEmail}</div>
                    </td>
                    <td><PriorityBadge priority={r.priority} /></td>
                    <td><StatusBadge status={r.status} /></td>
                    <td style={{ fontSize: 12, color: 'var(--color-muted)' }}>
                      {r.createdAt ? new Date(r.createdAt).toLocaleDateString() : '—'}
                    </td>
                    <td>
                      <div className="row-actions">
                        {r.status === 'PENDING_ANALYSIS' && (
                          <button className="btn btn-secondary btn-sm" onClick={() => handleClaim(r.id)}>
                            Claim
                          </button>
                        )}
                        {r.status === 'IN_ANALYSIS' && (
                          <button className="btn btn-primary btn-sm" onClick={() => setSelected(r)}>
                            Analyse
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {selected && (
        <AnalysisModal
          request={selected}
          onClose={() => setSelected(null)}
          onDone={handleDone}
        />
      )}
    </div>
  );
}

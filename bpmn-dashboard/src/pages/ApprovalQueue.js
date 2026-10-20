import React, { useEffect, useState, useCallback } from 'react';
import { requestService } from '../services/requestService';
import '../styles/dashboard.css';

function StatusBadge({ status }) {
  const map = {
    PENDING_APPROVAL: { cls: 'badge-approval', label: 'Pending Approval' },
    APPROVED: { cls: 'badge-approved', label: 'Approved' },
    REJECTED: { cls: 'badge-rejected', label: 'Rejected' },
  };
  const s = map[status] || { cls: 'badge-pending', label: status || 'Unknown' };
  return <span className={`badge ${s.cls}`}>{s.label}</span>;
}

function ApprovalModal({ request, onClose, onDone }) {
  const [comments, setComments] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const submit = async (action) => {
    setSubmitting(true);
    setError('');
    try {
      if (action === 'APPROVE') {
        await requestService.approveRequest(request.id, { approvalComments: comments });
      } else {
        await requestService.rejectRequest(request.id, { rejectionReason: comments });
      }
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
          <div className="modal-title">Review Request #{request.id}</div>
          <button className="modal-close" onClick={onClose}>×</button>
        </div>
        <div className="modal-body">
          <p style={{ marginBottom: 8, fontSize: 13 }}>
            <strong>Title:</strong> {request.title}
          </p>
          <p style={{ marginBottom: 8, fontSize: 13 }}>
            <strong>Process Type:</strong> {request.processType}
          </p>
          <p style={{ marginBottom: 8, fontSize: 13 }}>
            <strong>Requestor:</strong> {request.requestorName} ({request.requestorEmail})
          </p>
          {request.analystNotes && (
            <p style={{ marginBottom: 14, fontSize: 13, background: '#f1f5f9', padding: '8px 12px', borderRadius: 6 }}>
              <strong>Analyst Notes:</strong> {request.analystNotes}
            </p>
          )}
          {error && <div className="alert alert-error">{error}</div>}
          <div className="form-group">
            <label className="form-label" htmlFor="approvalComments">Comments</label>
            <textarea
              id="approvalComments"
              className="form-control"
              value={comments}
              onChange={(e) => setComments(e.target.value)}
              placeholder="Add approval or rejection comments…"
              rows={4}
            />
          </div>
        </div>
        <div className="modal-footer">
          <button className="btn btn-secondary btn-sm" onClick={onClose} disabled={submitting}>Cancel</button>
          <button className="btn btn-danger btn-sm" onClick={() => submit('REJECT')} disabled={submitting}>Reject</button>
          <button className="btn btn-success btn-sm" onClick={() => submit('APPROVE')} disabled={submitting}>
            {submitting ? <span className="spinner" style={{ width: 14, height: 14, borderTopColor: '#fff', borderColor: 'rgba(255,255,255,0.3)' }} /> : '✓ Approve'}
          </button>
        </div>
      </div>
    </div>
  );
}

export default function ApprovalQueue() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState(null);

  const fetchData = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const res = await requestService.getApprovalQueue();
      setRequests(res.data || []);
    } catch (err) {
      setError('Failed to load approval queue. Please refresh.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { fetchData(); }, [fetchData]);

  const handleDone = () => {
    setSelected(null);
    fetchData();
  };

  const pendingCount = requests.filter((r) => r.status === 'PENDING_APPROVAL').length;

  return (
    <div>
      <div className="stat-grid">
        <div className="stat-card warning">
          <div className="stat-label">Awaiting Approval</div>
          <div className="stat-value">{pendingCount}</div>
        </div>
        <div className="stat-card success">
          <div className="stat-label">Approved (session)</div>
          <div className="stat-value">{requests.filter((r) => r.status === 'APPROVED').length}</div>
        </div>
        <div className="stat-card danger">
          <div className="stat-label">Rejected (session)</div>
          <div className="stat-value">{requests.filter((r) => r.status === 'REJECTED').length}</div>
        </div>
      </div>

      <div className="card">
        <div className="card-header">
          <span className="card-title">Approval Queue</span>
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
            <div className="empty-state-icon">✅</div>
            <div className="empty-state-title">No requests pending approval</div>
            <p>Requests forwarded by analysts will appear here.</p>
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
                  <th>Analyst Notes</th>
                  <th>Status</th>
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
                    <td style={{ fontSize: 12, fontWeight: 600, color: r.priority === 'CRITICAL' ? '#7c3aed' : r.priority === 'HIGH' ? '#ef4444' : 'inherit' }}>
                      {r.priority}
                    </td>
                    <td style={{ fontSize: 12, color: 'var(--color-muted)', maxWidth: 180 }}>
                      <span style={{ display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                        {r.analystNotes || '—'}
                      </span>
                    </td>
                    <td><StatusBadge status={r.status} /></td>
                    <td>
                      {r.status === 'PENDING_APPROVAL' && (
                        <button className="btn btn-primary btn-sm" onClick={() => setSelected(r)}>
                          Review
                        </button>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {selected && (
        <ApprovalModal
          request={selected}
          onClose={() => setSelected(null)}
          onDone={handleDone}
        />
      )}
    </div>
  );
}

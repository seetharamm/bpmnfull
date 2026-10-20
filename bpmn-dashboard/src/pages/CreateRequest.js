import React, { useState } from 'react';
import { requestService } from '../services/requestService';
import '../styles/dashboard.css';

const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const PROCESS_TYPES = [
  'Loan Application',
  'Employee Onboarding',
  'Purchase Order',
  'Leave Request',
  'IT Access Request',
  'Contract Approval',
  'Incident Report',
  'Change Request',
];

const emptyForm = {
  title: '',
  processType: '',
  priority: 'MEDIUM',
  requestorName: '',
  requestorEmail: '',
  description: '',
  additionalNotes: '',
};

export default function CreateRequest() {
  const [form, setForm] = useState(emptyForm);
  const [errors, setErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [success, setSuccess] = useState(null);
  const [apiError, setApiError] = useState('');

  const validate = () => {
    const e = {};
    if (!form.title.trim()) e.title = 'Title is required.';
    if (!form.processType) e.processType = 'Process type is required.';
    if (!form.requestorName.trim()) e.requestorName = 'Requestor name is required.';
    if (!form.requestorEmail.trim()) {
      e.requestorEmail = 'Email is required.';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.requestorEmail)) {
      e.requestorEmail = 'Enter a valid email address.';
    }
    if (!form.description.trim()) e.description = 'Description is required.';
    return e;
  };

  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
    setErrors((prev) => ({ ...prev, [name]: undefined }));
    setApiError('');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    const errs = validate();
    if (Object.keys(errs).length > 0) {
      setErrors(errs);
      return;
    }
    setSubmitting(true);
    setSuccess(null);
    setApiError('');
    try {
      const res = await requestService.createRequest(form);
      setSuccess(res.data);
      setForm(emptyForm);
    } catch (err) {
      const msg =
        err?.response?.data?.message ||
        err?.response?.data ||
        'Failed to submit request. Please try again.';
      setApiError(typeof msg === 'string' ? msg : JSON.stringify(msg));
    } finally {
      setSubmitting(false);
    }
  };

  const handleReset = () => {
    setForm(emptyForm);
    setErrors({});
    setSuccess(null);
    setApiError('');
  };

  return (
    <div>
      <div className="page-header">
        <div className="page-title">New Process Request</div>
        <div className="page-subtitle">
          Fill in the details below to initiate a BPMN process
        </div>
      </div>

      {success && (
        <div className="alert alert-success">
          ✅ Request <strong>#{success.id || success.requestId || 'submitted'}</strong> created
          successfully and entered the analyst queue.
          <button
            style={{ marginLeft: 'auto', background: 'none', border: 'none', cursor: 'pointer', color: 'inherit', fontSize: '16px', padding: '0 2px' }}
            onClick={() => setSuccess(null)}
          >×</button>
        </div>
      )}

      {apiError && (
        <div className="alert alert-error">
          ⚠️ {apiError}
          <button
            style={{ marginLeft: 'auto', background: 'none', border: 'none', cursor: 'pointer', color: 'inherit', fontSize: '16px', padding: '0 2px' }}
            onClick={() => setApiError('')}
          >×</button>
        </div>
      )}

      <div className="card">
        <div className="card-header">
          <span className="card-title">Request Details</span>
        </div>
        <div className="card-body">
          <form onSubmit={handleSubmit} noValidate>
            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0 20px' }}>
              <div className="form-group">
                <label className="form-label required" htmlFor="title">Request Title</label>
                <input
                  id="title"
                  name="title"
                  type="text"
                  className="form-control"
                  placeholder="Brief title for this request"
                  value={form.title}
                  onChange={handleChange}
                />
                {errors.title && <div className="form-error">{errors.title}</div>}
              </div>

              <div className="form-group">
                <label className="form-label required" htmlFor="processType">Process Type</label>
                <select
                  id="processType"
                  name="processType"
                  className="form-control"
                  value={form.processType}
                  onChange={handleChange}
                >
                  <option value="">— Select process type —</option>
                  {PROCESS_TYPES.map((t) => (
                    <option key={t} value={t}>{t}</option>
                  ))}
                </select>
                {errors.processType && <div className="form-error">{errors.processType}</div>}
              </div>

              <div className="form-group">
                <label className="form-label required" htmlFor="requestorName">Requestor Name</label>
                <input
                  id="requestorName"
                  name="requestorName"
                  type="text"
                  className="form-control"
                  placeholder="Full name"
                  value={form.requestorName}
                  onChange={handleChange}
                />
                {errors.requestorName && <div className="form-error">{errors.requestorName}</div>}
              </div>

              <div className="form-group">
                <label className="form-label required" htmlFor="requestorEmail">Requestor Email</label>
                <input
                  id="requestorEmail"
                  name="requestorEmail"
                  type="email"
                  className="form-control"
                  placeholder="email@company.com"
                  value={form.requestorEmail}
                  onChange={handleChange}
                />
                {errors.requestorEmail && <div className="form-error">{errors.requestorEmail}</div>}
              </div>

              <div className="form-group">
                <label className="form-label" htmlFor="priority">Priority</label>
                <select
                  id="priority"
                  name="priority"
                  className="form-control"
                  value={form.priority}
                  onChange={handleChange}
                >
                  {PRIORITIES.map((p) => (
                    <option key={p} value={p}>{p}</option>
                  ))}
                </select>
              </div>
            </div>

            <div className="form-group">
              <label className="form-label required" htmlFor="description">Description</label>
              <textarea
                id="description"
                name="description"
                className="form-control"
                placeholder="Describe the process request in detail…"
                value={form.description}
                onChange={handleChange}
                rows={4}
              />
              {errors.description && <div className="form-error">{errors.description}</div>}
            </div>

            <div className="form-group">
              <label className="form-label" htmlFor="additionalNotes">Additional Notes</label>
              <textarea
                id="additionalNotes"
                name="additionalNotes"
                className="form-control"
                placeholder="Optional notes, attachments references, or special instructions…"
                value={form.additionalNotes}
                onChange={handleChange}
                rows={3}
              />
            </div>

            <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end', marginTop: '8px' }}>
              <button type="button" className="btn btn-secondary" onClick={handleReset}>
                Reset
              </button>
              <button type="submit" className="btn btn-primary" disabled={submitting}>
                {submitting ? (
                  <>
                    <span className="spinner" style={{ borderTopColor: '#fff', borderColor: 'rgba(255,255,255,0.3)' }} />
                    Submitting…
                  </>
                ) : (
                  'Submit Request'
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}

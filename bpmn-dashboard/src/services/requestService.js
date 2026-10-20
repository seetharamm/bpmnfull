import api from './api';

export const requestService = {
  // Create a new BPMN process request
  createRequest: (payload) => api.post('/requests', payload),

  // Fetch requests for Analyst Queue (status: PENDING_ANALYSIS)
  getAnalystQueue: () => api.get('/requests/analyst-queue'),

  // Fetch requests for Approval Queue (status: PENDING_APPROVAL)
  getApprovalQueue: () => api.get('/requests/approval-queue'),

  // Fetch completed requests (status: COMPLETED)
  getCompletedQueue: () => api.get('/requests/completed'),

  // Claim a task for analysis
  claimAnalystTask: (requestId) => api.post(`/requests/${requestId}/claim`),

  // Submit analyst decision
  submitAnalysis: (requestId, payload) =>
    api.post(`/requests/${requestId}/analyse`, payload),

  // Approve a request
  approveRequest: (requestId, payload) =>
    api.post(`/requests/${requestId}/approve`, payload),

  // Reject a request
  rejectRequest: (requestId, payload) =>
    api.post(`/requests/${requestId}/reject`, payload),

  // Get request detail
  getRequest: (requestId) => api.get(`/requests/${requestId}`),
};

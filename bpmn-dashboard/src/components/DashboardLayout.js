import React, { useState, useEffect } from 'react';
import { Outlet, useLocation } from 'react-router-dom';
import Sidebar from './Sidebar';
import { requestService } from '../services/requestService';
import '../styles/dashboard.css';

const PAGE_META = {
  '/create-request': {
    title: 'Create Request',
    subtitle: 'Submit a new BPMN process request',
  },
  '/analyst-queue': {
    title: 'Analyst Queue',
    subtitle: 'Review and process submitted requests',
  },
  '/approval-queue': {
    title: 'Approval Queue',
    subtitle: 'Approve or reject analysed requests',
  },
  '/completed-queue': {
    title: 'Completed Queue',
    subtitle: 'View approved and completed requests',
  },
};

export default function DashboardLayout() {
  const location = useLocation();
  const meta = PAGE_META[location.pathname] || { title: 'Dashboard', subtitle: '' };

  const [queueCounts, setQueueCounts] = useState({});

  useEffect(() => {
    const fetchCounts = async () => {
      try {
        const [analyst, approval, completed] = await Promise.all([
          requestService.getAnalystQueue(),
          requestService.getApprovalQueue(),
          requestService.getCompletedQueue(),
        ]);
        setQueueCounts({
          '/analyst-queue': analyst.data?.length ?? 0,
          '/approval-queue': approval.data?.length ?? 0,
          '/completed-queue': completed.data?.length ?? 0,
        });
      } catch {
        // counts are non-critical – silent fail
      }
    };
    fetchCounts();
  }, [location.pathname]);

  return (
    <div>
      <Sidebar queueCounts={queueCounts} />
      <header className="topbar">
        <div>
          <div className="topbar-title">{meta.title}</div>
          {meta.subtitle && (
            <div className="topbar-subtitle">{meta.subtitle}</div>
          )}
        </div>
      </header>
      <main className="main-content">
        <Outlet />
      </main>
    </div>
  );
}

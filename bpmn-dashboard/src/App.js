import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './context/AuthContext';
import LoginPage from './pages/LoginPage';
import DashboardLayout from './components/DashboardLayout';
import CreateRequest from './pages/CreateRequest';
import AnalystQueue from './pages/AnalystQueue';
import ApprovalQueue from './pages/ApprovalQueue';
import CompletedQueue from './pages/CompletedQueue';

function ProtectedRoute({ children }) {
  const { user } = useAuth();
  return user ? children : <Navigate to="/login" replace />;
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <Routes>
          <Route path="/login" element={<LoginPage />} />
          <Route
            path="/"
            element={
              <ProtectedRoute>
                <DashboardLayout />
              </ProtectedRoute>
            }
          >
            <Route index element={<Navigate to="/create-request" replace />} />
            <Route path="create-request" element={<CreateRequest />} />
            <Route path="analyst-queue" element={<AnalystQueue />} />
            <Route path="approval-queue" element={<ApprovalQueue />} />
            <Route path="completed-queue" element={<CompletedQueue />} />
          </Route>
          <Route path="*" element={<Navigate to="/login" replace />} />
        </Routes>
      </BrowserRouter>
    </AuthProvider>
  );
}

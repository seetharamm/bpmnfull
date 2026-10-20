# BPMN Portal Dashboard — React Frontend

A React-based dashboard with a persistent sidebar for managing BPMN process requests through their lifecycle.

## Features

| Page | Route | Description |
|---|---|---|
| Login | `/login` | JWT-based login screen |
| Create Request | `/create-request` | Submit a new BPMN process request |
| Analyst Queue | `/analyst-queue` | Claim and analyse submitted requests |
| Approval Queue | `/approval-queue` | Approve or reject analysed requests |
| Completed Queue | `/completed-queue` | View all finalised requests with filter |

## Project Structure

```
src/
├── App.js                      # Router + protected routes
├── index.js                    # Entry point
├── context/
│   └── AuthContext.js          # JWT auth state (login / logout)
├── services/
│   ├── api.js                  # Axios instance with JWT interceptors
│   └── requestService.js       # All BPMN request API calls
├── components/
│   ├── Sidebar.js              # Persistent sidebar with live queue counts
│   └── DashboardLayout.js      # Layout wrapper (sidebar + topbar + outlet)
├── pages/
│   ├── LoginPage.js
│   ├── CreateRequest.js
│   ├── AnalystQueue.js
│   ├── ApprovalQueue.js
│   └── CompletedQueue.js
└── styles/
    ├── global.css              # CSS variables + resets
    └── dashboard.css           # All component styles
```

## Setup

```bash
cd bpmn-dashboard
npm install
npm start          # Starts on http://localhost:3000
```

The dev server proxies `/api/*` requests to `http://localhost:8080` (the Spring Boot API).

## Build

```bash
npm run build      # Output in dist/
```

## API Connection

All API calls go through `src/services/api.js` which:
- Attaches `Authorization: Bearer <token>` from `sessionStorage`
- Redirects to `/login` on any `401` response
- Proxies to the Spring Boot API at `http://localhost:8080`

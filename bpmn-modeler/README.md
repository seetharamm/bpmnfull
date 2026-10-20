# BPMN 2.0 Modeler — Electron Desktop App

A full-featured **BPMN 2.0 desktop modeler** built with [Electron](https://www.electronjs.org/) and [bpmn-js](https://bpmn.io/). Design process diagrams, validate them with the built-in linter, deploy directly to the **bpmn-engine** backend, and manage reusable element templates — all without a browser.

---

## Table of Contents

- [Features](#features)
- [Requirements](#requirements)
- [Quick Start](#quick-start)
- [Keyboard Shortcuts](#keyboard-shortcuts)
- [Application Menu](#application-menu)
- [Properties Panel](#properties-panel)
- [Linter](#linter)
- [XML Editor](#xml-editor)
- [Template Manager](#template-manager)
- [Deploy to Engine](#deploy-to-engine)
- [Settings](#settings)
- [Project Structure](#project-structure)
- [Build](#build)

---

## Features

| Category | Details |
|---|---|
| **Canvas** | Full BPMN 2.0 palette — Tasks, Gateways, Events (all types), Sub-Processes, Pools, Lanes, Call Activities, Transactions, Data Objects, Groups, Text Annotations |
| **Properties panel** | General, Camunda Platform, Timer, Pool/Lane, Sub-Process / Call Activity panels |
| **Linter** | 12 built-in bpmnlint rules; clickable issues jump to element; configurable rule toggles |
| **XML editor** | Side-by-side BPMN XML editor; apply changes without losing layout |
| **Template manager** | Create/edit/delete reusable element templates; persisted to user data |
| **Export** | Save as `.bpmn`, export as `.svg` or `.png` |
| **Deploy** | One-click deploy to the bpmn-engine REST API (`POST /api/processes/deploy`) |
| **Recent files** | Last 10 opened files via File menu |
| **Minimap** | Toggle-able overview minimap |
| **Auto-save** | Optional auto-save every N seconds (configured in Settings) |
| **Dark theme** | Full dark design system |

---

## Requirements

- **Node.js** ≥ 18
- **npm** ≥ 9
- **Electron** 29 (installed via `npm install`)
- *(Optional)* bpmn-engine running at `http://localhost:8080` for deploy

---

## Quick Start

```bash
# 1. Install dependencies (first time only)
cd bpmn-modeler
npm install

# 2. Build the renderer bundle
npm run build

# 3. Launch the desktop app
npm start
```

---

## Keyboard Shortcuts

| Shortcut | Action |
|---|---|
| `Ctrl+N` | New diagram |
| `Ctrl+O` | Open `.bpmn` file |
| `Ctrl+S` | Save |
| `Ctrl+Shift+S` | Save As |
| `Ctrl+Z` | Undo |
| `Ctrl+Y` | Redo |
| `Ctrl+A` | Select All |
| `Delete` / `Backspace` | Delete selected |
| `Ctrl+Shift+F` | Find element by ID or name |
| `Ctrl+Shift+X` | Toggle XML editor panel |
| `Ctrl+Shift+L` | Toggle lint issues panel |
| `Ctrl+Shift+M` | Toggle minimap |
| `Ctrl+Shift+P` | Toggle properties panel |
| `Ctrl+Shift+E` | Export as SVG |
| `Ctrl+Shift+G` | Export as PNG |
| `Ctrl+D` | Deploy to engine |
| `F11` | Toggle fullscreen |
| `Ctrl+=` / `Ctrl+-` | Zoom in / out |
| `Ctrl+0` | Reset zoom to 100% |

---

## Application Menu

### File
- **New** — blank BPMN 2.0 diagram with Start → Task → End scaffold
- **Open** — open a `.bpmn` file from disk
- **Save / Save As** — write current diagram to disk
- **Recent Files** — quick-open last 10 files
- **Export as SVG / PNG** — save a rendering to disk
- **Deploy to Engine** — open deploy dialog

### Edit
- Undo / Redo / Select All / Delete
- Find Element (`Ctrl+Shift+F`) — searches by element ID and label

### View
- Toggle Properties Panel / Lint Panel / XML Editor / Minimap
- Zoom In / Out / Reset
- Fullscreen

### Diagram
- **Templates** — open Custom Template Manager
- **Validate** — run linter and show issues
- **Auto-layout** — trigger `bpmn-auto-layout` if installed

### Help
- About dialog (version, keyboard shortcut reference, links)

---

## Properties Panel

The right-side properties panel shows context-sensitive groups for the selected element:

| Group | Elements |
|---|---|
| **General** | ID, Name, Documentation (all elements) |
| **Camunda Platform** | Assignee, Candidate Users/Groups, Due Date, Form Key, etc. (UserTask); Async before/after, Job Priority, Error Code (all tasks) |
| **Timer** | Timer type (Date / Cycle / Duration), expression with format hints (Timer Events) |
| **Pool / Lane** | isExecutable flag (Participant), Partition Name / Documentation (Lane) |
| **Sub-Process** | triggeredByEvent, isForCompensation, ordering, completionCondition (Sub-Process); calledElement, binding, version, tenantId (CallActivity); transaction method (Transaction) |
| **Custom Templates** | User-defined property groups injected from the Template Manager |

---

## Linter

Rules are powered by [bpmnlint](https://github.com/bpmn-io/bpmnlint). Enabled by default:

| Rule | Severity |
|---|---|
| `label-required` | warn |
| `no-disconnected-flow-nodes` | error |
| `no-implicit-split` | warn |
| `no-overlapping-elements` | warn |
| `single-blank-start-event` | warn |
| `no-complex-gateway` | warn |
| `event-sub-process-typed-start-event` | warn |
| `start-event-required` | warn |
| `end-event-required` | warn |
| `no-gateway-join-fork` | warn |
| `no-undefined-flow-node-refs` | error |
| `sequence-flow-condition-required` | warn |

Click any issue in the lint panel to select and centre the affected element. Toggle rules individually in the **Rules Config** section of the lint panel.

---

## XML Editor

Press `Ctrl+Shift+X` or click **XML** in the toolbar to open the split-panel XML editor.

- **Refresh** — reload XML from the current diagram state
- **Apply** — parse and import the edited XML back into the modeler (preserves undo history)
- Invalid XML shows a parse error inline; the diagram is not modified

---

## Template Manager

Open via **Diagram → Templates** or the toolbar button.

Each template defines:
- **Name** — display name in the properties panel
- **Applies To** — BPMN element type (e.g. `bpmn:Task`)
- **Properties** — JSON array of `{ label, binding: { type, name }, type, value? }` descriptors

Templates are stored in `<userData>/custom-templates.json` and survive app reinstalls as long as the OS user profile is unchanged.

---

## Deploy to Engine

Open via **File → Deploy to Engine**, `Ctrl+D`, or the toolbar **Deploy** button.

| Field | Default | Description |
|---|---|---|
| Engine URL | `http://localhost:8080` | Base URL of the bpmn-engine |
| Process Key | *(from diagram)* | Key used to identify the process definition |
| Deploy Name | *(from diagram)* | Human-readable name |

The modeler sends a `POST /api/processes/deploy` request with:
```json
{
  "processKey": "my-process",
  "deploymentName": "My Process",
  "bpmnXml": "<?xml version=\"1.0\"...>"
}
```
A toast notification confirms success or shows the error message.

---

## Settings

Open via **File → Settings**.

| Setting | Default | Description |
|---|---|---|
| Theme | `dark` | `dark` or `light` |
| Auto-save | `false` | Save diagram automatically |
| Auto-save interval | `60` | Seconds between auto-saves |

Settings persist to `<userData>/settings.json`.

---

## Project Structure

```
bpmn-modeler/
├── index.html                    ← Shell HTML (toolbar, canvas, panels, dialogs)
├── webpack.config.js             ← Bundles src/renderer.js → dist/bundle.js
├── .babelrc                      ← Babel config (preset-env + jsx)
├── package.json
├── assets/                       ← Static assets (icons, etc.)
├── dist/                         ← Webpack output (generated, not committed)
│   └── bundle.js
└── src/
    ├── main.js                   ← Electron main process
    │                               Window, menu, IPC, file I/O, HTTP deploy
    ├── preload.js                ← contextBridge → window.api
    ├── renderer.js               ← Modeler init, all toolbar/IPC wiring
    ├── styles.css                ← Dark theme design system
    ├── features/
    │   ├── linter.js             ← bpmnlint integration, 12 rules, rules config
    │   ├── xml-editor.js         ← Inline BPMN XML editor
    │   └── template-manager.js  ← Custom template CRUD dialog
    └── providers/
        ├── timer-provider.js           ← Timer properties group
        ├── custom-templates-provider.js← User template groups
        ├── pool-provider.js            ← Pool / Lane properties group
        └── subprocess-provider.js      ← Sub-Process / CallActivity group
```

---

## Build

```bash
# Production bundle (outputs dist/bundle.js)
npm run build

# Development watch mode (rebuilds on file changes)
npm run watch

# Launch Electron (requires dist/bundle.js)
npm start
```

> **Packaging** — to produce a distributable `.exe`/`.dmg`/`.AppImage` use [electron-builder](https://www.electron.build/):
> ```bash
> npm install --save-dev electron-builder
> npx electron-builder --win   # Windows installer
> npx electron-builder --mac   # macOS DMG
> npx electron-builder --linux # AppImage
> ```

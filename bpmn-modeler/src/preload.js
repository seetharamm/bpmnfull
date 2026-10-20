const { contextBridge, ipcRenderer } = require('electron');

contextBridge.exposeInMainWorld('api', {
  // ── File I/O ────────────────────────────────────────────────────────────────
  saveFile:       args  => ipcRenderer.invoke('save-file', args),
  saveFileAs:     args  => ipcRenderer.invoke('save-file-as', args),
  openFileDialog: ()    => ipcRenderer.invoke('open-file-dialog'),
  exportSvg:      args  => ipcRenderer.invoke('export-svg', args),
  exportPng:      args  => ipcRenderer.invoke('export-png', args),

  // ── Deploy ──────────────────────────────────────────────────────────────────
  deployBpmn:     args  => ipcRenderer.invoke('deploy-bpmn', args),

  // ── Custom templates ────────────────────────────────────────────────────────
  templatesLoad:    ()       => ipcRenderer.invoke('templates-load'),
  templatesSave:    tmpl     => ipcRenderer.invoke('templates-save', tmpl),
  templatesDelete:  id       => ipcRenderer.invoke('templates-delete', id),
  templatesReorder: list     => ipcRenderer.invoke('templates-reorder', list),

  // ── Settings ────────────────────────────────────────────────────────────────
  settingsLoad: ()      => ipcRenderer.invoke('settings-load'),
  settingsSave: patch   => ipcRenderer.invoke('settings-save', patch),

  // ── Menu events (main → renderer) ──────────────────────────────────────────
  on: (channel, cb) => ipcRenderer.on(channel, (_e, ...args) => cb(...args)),
});

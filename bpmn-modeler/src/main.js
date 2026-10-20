/**
 * Electron main process — BPMN 2.0 Modeler
 *
 * Handles:
 *  - Window lifecycle
 *  - Native application menu with full keyboard shortcuts
 *  - All file I/O (open, save, save-as, export SVG/PNG)
 *  - Custom template CRUD persisted to userData
 *  - HTTP/HTTPS deploy to backend (no CORS restrictions from main process)
 *  - Recent files list (persisted to userData)
 */

const { app, BrowserWindow, Menu, dialog, ipcMain, shell } = require('electron');
const path = require('path');
const fs   = require('fs');
const http  = require('http');
const https = require('https');

// ── Paths ─────────────────────────────────────────────────────────────────────

const userDataPath   = () => app.getPath('userData');
const templatesFile  = () => path.join(userDataPath(), 'custom-templates.json');
const recentFile     = () => path.join(userDataPath(), 'recent-files.json');
const settingsFile   = () => path.join(userDataPath(), 'settings.json');

// ── Recent files ──────────────────────────────────────────────────────────────

const MAX_RECENT = 10;

function loadRecent() {
  try { return JSON.parse(fs.readFileSync(recentFile(), 'utf-8')); }
  catch (_) { return []; }
}

function addRecent(filePath) {
  const list = [filePath, ...loadRecent().filter(p => p !== filePath)].slice(0, MAX_RECENT);
  fs.writeFileSync(recentFile(), JSON.stringify(list, null, 2), 'utf-8');
  return list;
}

// ── Settings ──────────────────────────────────────────────────────────────────

function loadSettings() {
  try { return JSON.parse(fs.readFileSync(settingsFile(), 'utf-8')); }
  catch (_) { return { theme: 'dark', autoSave: false, autoSaveInterval: 60 }; }
}

function saveSettings(settings) {
  fs.writeFileSync(settingsFile(), JSON.stringify(settings, null, 2), 'utf-8');
}

// ── Custom templates ──────────────────────────────────────────────────────────

function loadTemplates() {
  try { return JSON.parse(fs.readFileSync(templatesFile(), 'utf-8')); }
  catch (_) { return []; }
}

function saveTemplates(templates) {
  fs.writeFileSync(templatesFile(), JSON.stringify(templates, null, 2), 'utf-8');
}

// ── Window ────────────────────────────────────────────────────────────────────

let mainWindow;

function createWindow() {
  const settings = loadSettings();

  mainWindow = new BrowserWindow({
    width: 1440,
    height: 900,
    minWidth: 960,
    minHeight: 640,
    title: 'BPMN Modeler',
    backgroundColor: '#1e1e1e',
    webPreferences: {
      nodeIntegration: false,
      contextIsolation: true,
      preload: path.join(__dirname, 'preload.js'),
    },
    show: false,
  });

  mainWindow.loadFile(path.join(__dirname, '..', 'index.html'));

  mainWindow.once('ready-to-show', () => mainWindow.show());

  mainWindow.on('closed', () => { mainWindow = null; });

  buildMenu();
}

// ── Application menu ──────────────────────────────────────────────────────────

function send(channel, ...args) {
  if (mainWindow) mainWindow.webContents.send(channel, ...args);
}

function buildMenu() {
  const recentItems = loadRecent().map(fp => ({
    label: path.basename(fp),
    click: () => {
      try {
        const xml = fs.readFileSync(fp, 'utf-8');
        send('menu-open', { xml, filePath: fp });
      } catch (e) {
        dialog.showMessageBox(mainWindow, { type: 'error', message: `Cannot open file:\n${e.message}` });
      }
    },
  }));

  const template = [
    {
      label: 'File',
      submenu: [
        { label: 'New Diagram',       accelerator: 'CmdOrCtrl+N',         click: () => send('menu-new') },
        { type: 'separator' },
        {
          label: 'Open…',             accelerator: 'CmdOrCtrl+O',
          click: async () => {
            const { canceled, filePaths } = await dialog.showOpenDialog(mainWindow, {
              title: 'Open BPMN File',
              filters: [
                { name: 'BPMN Files', extensions: ['bpmn', 'bpmn20.xml', 'xml'] },
                { name: 'All Files',  extensions: ['*'] },
              ],
              properties: ['openFile'],
            });
            if (!canceled && filePaths[0]) {
              const xml = fs.readFileSync(filePaths[0], 'utf-8');
              addRecent(filePaths[0]);
              send('menu-open', { xml, filePath: filePaths[0] });
              buildMenu(); // refresh recent list
            }
          },
        },
        {
          label: 'Open Recent',
          submenu: recentItems.length > 0
            ? [...recentItems, { type: 'separator' }, { label: 'Clear Recent', click: () => { fs.writeFileSync(recentFile(), '[]'); buildMenu(); } }]
            : [{ label: 'No recent files', enabled: false }],
        },
        { type: 'separator' },
        { label: 'Save',         accelerator: 'CmdOrCtrl+S',       click: () => send('menu-save') },
        { label: 'Save As…',     accelerator: 'CmdOrCtrl+Shift+S', click: () => send('menu-save-as') },
        { type: 'separator' },
        { label: 'Export as SVG',            click: () => send('menu-export-svg') },
        { label: 'Export as PNG',            click: () => send('menu-export-png') },
        { type: 'separator' },
        { label: 'Deploy to Server…',  accelerator: 'CmdOrCtrl+Shift+D', click: () => send('menu-deploy') },
        { type: 'separator' },
        { label: 'Template Manager…', accelerator: 'CmdOrCtrl+Shift+T', click: () => send('menu-templates') },
        { type: 'separator' },
        { role: 'quit' },
      ],
    },
    {
      label: 'Edit',
      submenu: [
        { label: 'Undo',           accelerator: 'CmdOrCtrl+Z',         click: () => send('menu-undo') },
        { label: 'Redo',           accelerator: 'CmdOrCtrl+Shift+Z',   click: () => send('menu-redo') },
        { type: 'separator' },
        { label: 'Select All',     accelerator: 'CmdOrCtrl+A',         click: () => send('menu-select-all') },
        { type: 'separator' },
        { label: 'Find Element…',  accelerator: 'CmdOrCtrl+F',         click: () => send('menu-find') },
      ],
    },
    {
      label: 'View',
      submenu: [
        { label: 'Fit Diagram',    accelerator: 'CmdOrCtrl+Shift+H',   click: () => send('menu-fit') },
        { label: 'Reset Zoom',     accelerator: 'CmdOrCtrl+0',         click: () => send('menu-zoom-reset') },
        { label: 'Zoom In',        accelerator: 'CmdOrCtrl+=',         click: () => send('menu-zoom-in') },
        { label: 'Zoom Out',       accelerator: 'CmdOrCtrl+-',         click: () => send('menu-zoom-out') },
        { type: 'separator' },
        { label: 'Toggle Minimap',            accelerator: 'CmdOrCtrl+M',         click: () => send('menu-minimap') },
        { label: 'Toggle Properties Panel',   accelerator: 'CmdOrCtrl+Shift+E',   click: () => send('menu-toggle-props') },
        { label: 'Toggle XML Editor',         accelerator: 'CmdOrCtrl+Shift+X',   click: () => send('menu-toggle-xml') },
        { label: 'Toggle Validation Panel',   accelerator: 'CmdOrCtrl+Shift+L',   click: () => send('menu-validate') },
        { type: 'separator' },
        { role: 'reload' },
        { role: 'toggleDevTools' },
        { type: 'separator' },
        { role: 'togglefullscreen' },
      ],
    },
    {
      label: 'Diagram',
      submenu: [
        { label: 'Validate Diagram', accelerator: 'CmdOrCtrl+Shift+L', click: () => send('menu-validate') },
        { type: 'separator' },
        { label: 'Auto-layout (horizontal)', click: () => send('menu-layout-h') },
        { label: 'Auto-layout (vertical)',   click: () => send('menu-layout-v') },
        { type: 'separator' },
        { label: 'Show XML…',  accelerator: 'CmdOrCtrl+Shift+X', click: () => send('menu-toggle-xml') },
      ],
    },
    {
      label: 'Help',
      submenu: [
        { label: 'BPMN 2.0 Reference', click: () => shell.openExternal('https://www.omg.org/spec/BPMN/2.0/') },
        { label: 'bpmn-js Documentation', click: () => shell.openExternal('https://bpmn.io/') },
        { type: 'separator' },
        { label: 'About BPMN Modeler', click: () => {
          dialog.showMessageBox(mainWindow, {
            type: 'info',
            title: 'About BPMN Modeler',
            message: 'BPMN 2.0 Process Modeler',
            detail: 'Built with Electron and bpmn-js\nSupports full BPMN 2.0 element set',
          });
        }},
      ],
    },
  ];

  Menu.setApplicationMenu(Menu.buildFromTemplate(template));
}

// ── IPC: File operations ──────────────────────────────────────────────────────

ipcMain.handle('save-file', async (_e, { xml, filePath }) => {
  let target = filePath;
  if (!target) {
    const { canceled, filePath: chosen } = await dialog.showSaveDialog(mainWindow, {
      title: 'Save BPMN File',
      defaultPath: 'diagram.bpmn',
      filters: [{ name: 'BPMN Files', extensions: ['bpmn', 'xml'] }],
    });
    if (canceled) return { saved: false };
    target = chosen;
  }
  fs.writeFileSync(target, xml, 'utf-8');
  addRecent(target);
  buildMenu();
  return { saved: true, filePath: target };
});

ipcMain.handle('save-file-as', async (_e, { xml }) => {
  const { canceled, filePath } = await dialog.showSaveDialog(mainWindow, {
    title: 'Save BPMN File As',
    defaultPath: 'diagram.bpmn',
    filters: [{ name: 'BPMN Files', extensions: ['bpmn', 'xml'] }],
  });
  if (canceled) return { saved: false };
  fs.writeFileSync(filePath, xml, 'utf-8');
  addRecent(filePath);
  buildMenu();
  return { saved: true, filePath };
});

ipcMain.handle('export-svg', async (_e, { svg }) => {
  const { canceled, filePath } = await dialog.showSaveDialog(mainWindow, {
    title: 'Export as SVG',
    defaultPath: 'diagram.svg',
    filters: [{ name: 'SVG Files', extensions: ['svg'] }],
  });
  if (canceled) return { saved: false };
  fs.writeFileSync(filePath, svg, 'utf-8');
  return { saved: true };
});

ipcMain.handle('export-png', async (_e, { dataUrl }) => {
  const { canceled, filePath } = await dialog.showSaveDialog(mainWindow, {
    title: 'Export as PNG',
    defaultPath: 'diagram.png',
    filters: [{ name: 'PNG Images', extensions: ['png'] }],
  });
  if (canceled) return { saved: false };
  const base64 = dataUrl.replace(/^data:image\/png;base64,/, '');
  fs.writeFileSync(filePath, Buffer.from(base64, 'base64'));
  return { saved: true };
});

ipcMain.handle('open-file-dialog', async () => {
  const { canceled, filePaths } = await dialog.showOpenDialog(mainWindow, {
    title: 'Open BPMN File',
    filters: [
      { name: 'BPMN Files', extensions: ['bpmn', 'xml'] },
      { name: 'All Files',  extensions: ['*'] },
    ],
    properties: ['openFile'],
  });
  if (canceled || !filePaths[0]) return null;
  const xml = fs.readFileSync(filePaths[0], 'utf-8');
  addRecent(filePaths[0]);
  buildMenu();
  return { xml, filePath: filePaths[0] };
});

// ── IPC: Templates ────────────────────────────────────────────────────────────

ipcMain.handle('templates-load',    ()          => loadTemplates());
ipcMain.handle('templates-save',    (_e, tmpl)  => { const all = loadTemplates(); const i = all.findIndex(t => t.id === tmpl.id); if (i >= 0) all[i] = tmpl; else all.push(tmpl); saveTemplates(all); return all; });
ipcMain.handle('templates-delete',  (_e, id)    => { const all = loadTemplates().filter(t => t.id !== id); saveTemplates(all); return all; });
ipcMain.handle('templates-reorder', (_e, list)  => { saveTemplates(list); return list; });

// ── IPC: Settings ─────────────────────────────────────────────────────────────

ipcMain.handle('settings-load', ()           => loadSettings());
ipcMain.handle('settings-save', (_e, patch)  => { const s = { ...loadSettings(), ...patch }; saveSettings(s); return s; });

// ── IPC: Deploy BPMN ─────────────────────────────────────────────────────────

ipcMain.handle('deploy-bpmn', async (_e, opts) => {
  const {
    url, method = 'POST', contentType = 'application/xml',
    authType = 'none', authParams = {},
    deploymentName, processKey, tenantId,
    deployMessage, deployedBy,
    xml,
  } = opts;

  function buildAuthHeader() {
    switch (authType) {
      case 'bearer': return authParams.token ? `Bearer ${authParams.token}` : null;
      case 'basic':  return `Basic ${Buffer.from(`${authParams.username||''}:${authParams.password||''}`).toString('base64')}`;
      default:       return null;
    }
  }

  const body = Buffer.from(xml, 'utf-8');
  const headers = {
    'Content-Type': contentType,
    'Content-Length': body.length,
    'Accept': 'application/json, text/plain, */*',
  };

  const authHeader = buildAuthHeader();
  if (authHeader) headers['Authorization'] = authHeader;
  if (authType === 'apikey' && authParams.apiKeyIn === 'header')
    headers[authParams.apiKeyName || 'X-API-Key'] = authParams.apiKeyValue || '';
  if (authType === 'custom' && authParams.headerName)
    headers[authParams.headerName] = authParams.headerValue || '';
  if (deploymentName) headers['X-Deployment-Name'] = deploymentName;
  if (processKey)     headers['X-Process-Key']     = processKey;
  if (tenantId)       headers['X-Tenant-Id']       = tenantId;
  if (deployedBy)     headers['X-Deployed-By']     = deployedBy;
  if (deployMessage)  headers['X-Deploy-Message']  = deployMessage;

  let targetUrl = url;
  if (authType === 'apikey' && authParams.apiKeyIn === 'query') {
    const sep = targetUrl.includes('?') ? '&' : '?';
    targetUrl += `${sep}${encodeURIComponent(authParams.apiKeyName||'apiKey')}=${encodeURIComponent(authParams.apiKeyValue||'')}`;
  }

  return new Promise(resolve => {
    let parsed;
    try { parsed = new URL(targetUrl); }
    catch (e) { return resolve({ ok: false, error: `Invalid URL: ${e.message}` }); }

    const lib = parsed.protocol === 'https:' ? https : http;
    const req = lib.request({
      hostname: parsed.hostname,
      port: parsed.port || (parsed.protocol === 'https:' ? 443 : 80),
      path: parsed.pathname + parsed.search,
      method: method.toUpperCase(),
      headers,
      rejectUnauthorized: false,
    }, res => {
      const chunks = [];
      res.on('data', c => chunks.push(c));
      res.on('end', () => resolve({ ok: res.statusCode < 400, status: res.statusCode, body: Buffer.concat(chunks).toString('utf-8') }));
    });
    req.on('error', e => resolve({ ok: false, error: e.message }));
    req.setTimeout(30000, () => { req.destroy(); resolve({ ok: false, error: 'Request timed out' }); });
    req.write(body);
    req.end();
  });
});

// ── App lifecycle ─────────────────────────────────────────────────────────────

app.whenReady().then(createWindow);

app.on('window-all-closed', () => { if (process.platform !== 'darwin') app.quit(); });

app.on('activate', () => { if (BrowserWindow.getAllWindows().length === 0) createWindow(); });

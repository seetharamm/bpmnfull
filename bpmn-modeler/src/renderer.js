/**
 * renderer.js — BPMN 2.0 Modeler renderer process
 *
 * Initialises bpmn-js, wires up all toolbar buttons, keyboard shortcuts,
 * IPC menu events, deploy dialog, find dialog, linter, XML editor,
 * minimap, and template manager.
 */

// ── bpmn-js core ─────────────────────────────────────────────────────────────
import BpmnModeler from 'bpmn-js/lib/Modeler';
import {
  BpmnPropertiesPanelModule,
  BpmnPropertiesProviderModule,
  CamundaPlatformPropertiesProviderModule,
} from 'bpmn-js-properties-panel';
import CamundaBpmnModdle from 'camunda-bpmn-moddle/resources/camunda.json';
import MinimapModule from 'diagram-js-minimap';

// CSS
import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-js.css';
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css';
import '@bpmn-io/properties-panel/assets/properties-panel.css';
import 'diagram-js-minimap/assets/diagram-js-minimap.css';
import './styles.css';

// ── Feature modules ───────────────────────────────────────────────────────────
import * as Linter          from './features/linter.js';
import * as XmlEditor       from './features/xml-editor.js';
import * as TemplateManager from './features/template-manager.js';

// ── Custom providers ──────────────────────────────────────────────────────────
import TimerProviderModule           from './providers/timer-provider.js';
import CustomTemplatesProviderModule, { setCustomTemplates } from './providers/custom-templates-provider.js';
import PoolProviderModule             from './providers/pool-provider.js';
import SubProcessProviderModule       from './providers/subprocess-provider.js';

// ──────────────────────────────────────────────────────────────────────────────
// Default empty diagram
// ──────────────────────────────────────────────────────────────────────────────

const EMPTY_XML = `<?xml version="1.0" encoding="UTF-8"?>
<bpmn:definitions xmlns:bpmn="http://www.omg.org/spec/BPMN/20100524/MODEL"
  xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
  xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
  xmlns:camunda="http://camunda.org/schema/1.0/bpmn"
  id="Definitions_1"
  targetNamespace="http://bpmn.io/schema/bpmn"
  exporter="BPMN Modeler" exporterVersion="1.0">
  <bpmn:process id="Process_1" isExecutable="true">
    <bpmn:startEvent id="StartEvent_1" name="Start">
      <bpmn:outgoing>Flow_1</bpmn:outgoing>
    </bpmn:startEvent>
    <bpmn:task id="Task_1" name="My First Task">
      <bpmn:incoming>Flow_1</bpmn:incoming>
      <bpmn:outgoing>Flow_2</bpmn:outgoing>
    </bpmn:task>
    <bpmn:endEvent id="EndEvent_1" name="End">
      <bpmn:incoming>Flow_2</bpmn:incoming>
    </bpmn:endEvent>
    <bpmn:sequenceFlow id="Flow_1" sourceRef="StartEvent_1" targetRef="Task_1"/>
    <bpmn:sequenceFlow id="Flow_2" sourceRef="Task_1" targetRef="EndEvent_1"/>
  </bpmn:process>
  <bpmndi:BPMNDiagram id="BPMNDiagram_1">
    <bpmndi:BPMNPlane id="BPMNPlane_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="_BPMNShape_StartEvent_1" bpmnElement="StartEvent_1">
        <dc:Bounds x="172" y="192" width="36" height="36"/>
        <bpmndi:BPMNLabel><dc:Bounds x="176" y="235" width="28" height="14"/></bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="Task_1_di" bpmnElement="Task_1">
        <dc:Bounds x="270" y="170" width="120" height="80"/>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNShape id="EndEvent_1_di" bpmnElement="EndEvent_1">
        <dc:Bounds x="452" y="192" width="36" height="36"/>
        <bpmndi:BPMNLabel><dc:Bounds x="458" y="235" width="24" height="14"/></bpmndi:BPMNLabel>
      </bpmndi:BPMNShape>
      <bpmndi:BPMNEdge id="Flow_1_di" bpmnElement="Flow_1">
        <di:waypoint xmlns:di="http://www.omg.org/spec/DD/20100524/DI" x="208" y="210"/>
        <di:waypoint xmlns:di="http://www.omg.org/spec/DD/20100524/DI" x="270" y="210"/>
      </bpmndi:BPMNEdge>
      <bpmndi:BPMNEdge id="Flow_2_di" bpmnElement="Flow_2">
        <di:waypoint xmlns:di="http://www.omg.org/spec/DD/20100524/DI" x="390" y="210"/>
        <di:waypoint xmlns:di="http://www.omg.org/spec/DD/20100524/DI" x="452" y="210"/>
      </bpmndi:BPMNEdge>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</bpmn:definitions>`;

// ──────────────────────────────────────────────────────────────────────────────
// Modeler init
// ──────────────────────────────────────────────────────────────────────────────

const modeler = new BpmnModeler({
  container: '#canvas',
  propertiesPanel: { parent: '#properties-panel' },
  additionalModules: [
    BpmnPropertiesPanelModule,
    BpmnPropertiesProviderModule,
    CamundaPlatformPropertiesProviderModule,
    TimerProviderModule,
    CustomTemplatesProviderModule,
    PoolProviderModule,
    SubProcessProviderModule,
    MinimapModule,
  ],
  moddleExtensions: { camunda: CamundaBpmnModdle },
});

// ──────────────────────────────────────────────────────────────────────────────
// State
// ──────────────────────────────────────────────────────────────────────────────

let _filePath = null;
let _dirty    = false;

// ──────────────────────────────────────────────────────────────────────────────
// Helpers
// ──────────────────────────────────────────────────────────────────────────────

function setLabel(label, dirty = false) {
  document.getElementById('file-label').textContent = (dirty ? '● ' : '') + label;
  _dirty = dirty;
}

async function openDiagram(xml, filePath = null) {
  try {
    await modeler.importXML(xml);
    modeler.get('canvas').zoom('fit-viewport');
    _filePath = filePath;
    setLabel(filePath ? filePath.split(/[\\/]/).pop() : 'Unsaved diagram', false);
  } catch (e) {
    alert(`Failed to import BPMN:\n${e.message}`);
  }
}

async function getXml() {
  const { xml } = await modeler.saveXML({ format: true });
  return xml;
}

async function save(forceAs = false) {
  const xml = await getXml();
  const result = forceAs
    ? await window.api.saveFileAs({ xml })
    : await window.api.saveFile({ xml, filePath: _filePath });
  if (result.saved) {
    _filePath = result.filePath || _filePath;
    setLabel(_filePath ? _filePath.split(/[\\/]/).pop() : 'Saved', false);
  }
}

// Mark dirty on changes
modeler.on('commandStack.changed', () => {
  if (!_dirty) {
    setLabel(_filePath ? _filePath.split(/[\\/]/).pop() : 'Unsaved diagram', true);
  }
});

// ──────────────────────────────────────────────────────────────────────────────
// Toolbar buttons
// ──────────────────────────────────────────────────────────────────────────────

document.getElementById('btn-new').addEventListener('click', () => openDiagram(EMPTY_XML));

document.getElementById('btn-open').addEventListener('click', async () => {
  const result = await window.api.openFileDialog();
  if (result) openDiagram(result.xml, result.filePath);
});

document.getElementById('btn-save').addEventListener('click', () => save(false));
document.getElementById('btn-undo').addEventListener('click', () => modeler.get('commandStack').undo());
document.getElementById('btn-redo').addEventListener('click', () => modeler.get('commandStack').redo());

document.getElementById('btn-zoom-in').addEventListener('click', () => {
  const c = modeler.get('canvas'); c.zoom(c.zoom() * 1.2);
});
document.getElementById('btn-zoom-out').addEventListener('click', () => {
  const c = modeler.get('canvas'); c.zoom(c.zoom() * 0.8);
});
document.getElementById('btn-zoom-fit').addEventListener('click', () => {
  modeler.get('canvas').zoom('fit-viewport');
});
document.getElementById('btn-zoom-reset').addEventListener('click', () => {
  modeler.get('canvas').zoom(1);
});

document.getElementById('btn-validate').addEventListener('click', async () => {
  if (Linter.isVisible()) { await Linter.run(); }
  else { Linter.toggle(true); await Linter.run(); }
});

document.getElementById('btn-xml').addEventListener('click', () => XmlEditor.toggle());

document.getElementById('btn-minimap').addEventListener('click', () => {
  const mm = modeler.get('minimap');
  if (mm) mm.toggle();
});

document.getElementById('btn-templates').addEventListener('click', () => TemplateManager.open());

document.getElementById('btn-deploy').addEventListener('click', () => showDeploy());

// ──────────────────────────────────────────────────────────────────────────────
// Panel toggle tabs
// ──────────────────────────────────────────────────────────────────────────────

const paletteTab = document.getElementById('palette-tab');
const propsTab   = document.getElementById('props-tab');
const propsPanel = document.getElementById('properties-panel');

function toggleProps() {
  const hidden = propsPanel.classList.toggle('hidden');
  propsTab.classList.toggle('props-hidden', hidden);
  propsTab.querySelector('.pt-icon').innerHTML = hidden ? '&#8249;' : '&#8250;';
}

function togglePalette() {
  const palette = modeler.get('palette');
  if (palette.isOpen()) {
    palette.close();
    paletteTab.classList.add('palette-closed');
    paletteTab.querySelector('.pt-icon').innerHTML = '&#10095;';
  } else {
    palette.open();
    paletteTab.classList.remove('palette-closed');
    paletteTab.querySelector('.pt-icon').innerHTML = '&#10094;';
  }
}

propsTab.addEventListener('click', toggleProps);
paletteTab.addEventListener('click', togglePalette);

modeler.on('palette.changed', ({ open }) => {
  paletteTab.classList.toggle('palette-closed', !open);
  paletteTab.querySelector('.pt-icon').innerHTML = open ? '&#10094;' : '&#10095;';
});

// ──────────────────────────────────────────────────────────────────────────────
// Keyboard shortcuts
// ──────────────────────────────────────────────────────────────────────────────

document.addEventListener('keydown', e => {
  if (e.ctrlKey && e.shiftKey && e.key === 'P') { e.preventDefault(); togglePalette(); }
  if (e.ctrlKey && e.shiftKey && e.key === 'E') { e.preventDefault(); toggleProps(); }
  if (e.ctrlKey && e.shiftKey && e.key === 'L') { e.preventDefault(); document.getElementById('btn-validate').click(); }
  if (e.ctrlKey && e.shiftKey && e.key === 'X') { e.preventDefault(); XmlEditor.toggle(); }
  if (e.ctrlKey && e.key === 'f') { e.preventDefault(); showFind(); }
  if (e.key === 'Escape') { hideDeploy(); hideFind(); }
});

// ──────────────────────────────────────────────────────────────────────────────
// IPC menu events
// ──────────────────────────────────────────────────────────────────────────────

window.api.on('menu-new',          ()     => openDiagram(EMPTY_XML));
window.api.on('menu-open',         data   => openDiagram(data.xml, data.filePath));
window.api.on('menu-save',         ()     => save(false));
window.api.on('menu-save-as',      ()     => save(true));
window.api.on('menu-undo',         ()     => modeler.get('commandStack').undo());
window.api.on('menu-redo',         ()     => modeler.get('commandStack').redo());
window.api.on('menu-select-all',   ()     => modeler.get('selection').select(modeler.get('elementRegistry').getAll()));
window.api.on('menu-fit',          ()     => modeler.get('canvas').zoom('fit-viewport'));
window.api.on('menu-zoom-reset',   ()     => modeler.get('canvas').zoom(1));
window.api.on('menu-zoom-in',      ()     => { const c = modeler.get('canvas'); c.zoom(c.zoom() * 1.2); });
window.api.on('menu-zoom-out',     ()     => { const c = modeler.get('canvas'); c.zoom(c.zoom() * 0.8); });
window.api.on('menu-minimap',      ()     => { const mm = modeler.get('minimap'); if (mm) mm.toggle(); });
window.api.on('menu-toggle-props', ()     => toggleProps());
window.api.on('menu-toggle-xml',   ()     => XmlEditor.toggle());
window.api.on('menu-validate',     ()     => { if (!Linter.isVisible()) Linter.toggle(true); Linter.run(); });
window.api.on('menu-find',         ()     => showFind());
window.api.on('menu-deploy',       ()     => showDeploy());
window.api.on('menu-templates',    ()     => TemplateManager.open());

window.api.on('menu-export-svg', async () => {
  const { svg } = await modeler.saveSVG();
  await window.api.exportSvg({ svg });
});

window.api.on('menu-export-png', async () => {
  const { svg } = await modeler.saveSVG();
  // Convert SVG to PNG via canvas
  const canvas = document.createElement('canvas');
  const img = new Image();
  const blob = new Blob([svg], { type: 'image/svg+xml' });
  const url  = URL.createObjectURL(blob);
  img.onload = async () => {
    canvas.width = img.width || 1200; canvas.height = img.height || 800;
    canvas.getContext('2d').drawImage(img, 0, 0);
    URL.revokeObjectURL(url);
    await window.api.exportPng({ dataUrl: canvas.toDataURL('image/png') });
  };
  img.src = url;
});

// ──────────────────────────────────────────────────────────────────────────────
// Deploy dialog
// ──────────────────────────────────────────────────────────────────────────────

const deployOverlay = document.getElementById('deploy-overlay');
const deployAuthSel = document.getElementById('deploy-auth-type');
const AUTH_DIVS = {
  bearer: document.getElementById('deploy-auth-bearer'),
  basic:  document.getElementById('deploy-auth-basic'),
  apikey: document.getElementById('deploy-auth-apikey'),
  custom: document.getElementById('deploy-auth-custom'),
};

function updateAuthFields() {
  const val = deployAuthSel.value;
  for (const [k, el] of Object.entries(AUTH_DIVS)) el.classList.toggle('hidden', k !== val);
}
deployAuthSel.addEventListener('change', updateAuthFields);

function showDeploy() { deployOverlay.classList.remove('hidden'); document.getElementById('deploy-url').focus(); }
function hideDeploy() { deployOverlay.classList.add('hidden'); clearDeployStatus(); }

function clearDeployStatus() {
  const s = document.getElementById('deploy-status');
  s.textContent = ''; s.className = 'status-msg hidden';
}
function showDeployStatus(msg, type) {
  const s = document.getElementById('deploy-status');
  s.textContent = msg; s.className = `status-msg status-${type}`;
}
function setDeployBusy(busy) {
  document.getElementById('deploy-submit').disabled = busy;
  document.getElementById('deploy-submit-text').textContent = busy ? 'Deploying…' : 'Deploy';
  document.getElementById('deploy-spinner').classList.toggle('hidden', !busy);
}

function gv(id) { return (document.getElementById(id) || {}).value || ''; }

document.getElementById('deploy-submit').addEventListener('click', async () => {
  const url = gv('deploy-url');
  if (!url) { showDeployStatus('Server URL is required.', 'err'); return; }
  clearDeployStatus(); setDeployBusy(true);
  try {
    const authType = gv('deploy-auth-type');
    const authParams = {};
    if (authType === 'bearer') authParams.token = gv('deploy-bearer-token');
    else if (authType === 'basic') { authParams.username = gv('deploy-basic-user'); authParams.password = gv('deploy-basic-pass'); }
    else if (authType === 'apikey') { authParams.apiKeyName = gv('deploy-apikey-name'); authParams.apiKeyValue = gv('deploy-apikey-value'); authParams.apiKeyIn = gv('deploy-apikey-in'); }
    else if (authType === 'custom') { authParams.headerName = gv('deploy-custom-name'); authParams.headerValue = gv('deploy-custom-value'); }

    const contentType  = gv('deploy-content-type');
    const isJsonApi    = contentType === 'application/json';
    const rawXml       = await getXml();

    // If content-type is application/json, wrap in the bpmn-engine DeployProcessRequest format
    const bodyXml = rawXml;
    let finalBody;
    if (isJsonApi) {
      finalBody = JSON.stringify({
        bpmnXml:       rawXml,
        resourceName:  gv('deploy-name') ? gv('deploy-name') + '.bpmn' : 'diagram.bpmn',
        deployedBy:    gv('deploy-deployed-by'),
        deployMessage: gv('deploy-message'),
      });
    }

    const result = await window.api.deployBpmn({
      url, method: gv('deploy-method'),
      contentType: isJsonApi ? 'application/json' : contentType,
      authType, authParams,
      deploymentName: gv('deploy-name'),
      processKey:     gv('deploy-process-key'),
      tenantId:       gv('deploy-tenant-id'),
      deployedBy:     gv('deploy-deployed-by'),
      deployMessage:  gv('deploy-message'),
      xml: isJsonApi ? finalBody : bodyXml,
    });

    if (result.ok) showDeployStatus(`✔ Deployed successfully (HTTP ${result.status}).`, 'ok');
    else showDeployStatus(`✘ ${result.error || `HTTP ${result.status} — ${(result.body || '').slice(0, 200)}`}`, 'err');
  } catch (e) { showDeployStatus(`✘ Error: ${e.message}`, 'err'); }
  finally { setDeployBusy(false); }
});

document.getElementById('deploy-close').addEventListener('click', hideDeploy);
document.getElementById('deploy-cancel').addEventListener('click', hideDeploy);
deployOverlay.addEventListener('click', e => { if (e.target === deployOverlay) hideDeploy(); });

// ──────────────────────────────────────────────────────────────────────────────
// Find / Go-to element dialog
// ──────────────────────────────────────────────────────────────────────────────

const findOverlay = document.getElementById('find-overlay');
const findInput   = document.getElementById('find-input');
const findResults = document.getElementById('find-results');

function showFind() { findOverlay.classList.remove('hidden'); findInput.focus(); findInput.select(); }
function hideFind() { findOverlay.classList.add('hidden'); }

function runFind() {
  const q = findInput.value.trim().toLowerCase();
  findResults.innerHTML = '';
  if (!q) return;
  const elements = modeler.get('elementRegistry').getAll();
  const matches = elements.filter(el => {
    const id   = (el.id || '').toLowerCase();
    const name = (el.businessObject?.name || '').toLowerCase();
    return id.includes(q) || name.includes(q);
  }).slice(0, 20);

  if (!matches.length) {
    const div = document.createElement('div');
    div.className = 'find-empty';
    div.textContent = 'No elements found.';
    findResults.appendChild(div);
    return;
  }
  matches.forEach(el => {
    const row = document.createElement('div');
    row.className = 'find-result-row';
    row.innerHTML = `
      <span class="find-result-type">${el.type.replace('bpmn:', '')}</span>
      <span class="find-result-id">${el.id}</span>
      ${el.businessObject?.name ? `<span class="find-result-name">${el.businessObject.name}</span>` : ''}`;
    row.addEventListener('click', () => {
      modeler.get('selection').select(el);
      modeler.get('canvas').scrollToElement(el);
      hideFind();
    });
    findResults.appendChild(row);
  });
}

findInput.addEventListener('input', runFind);
findInput.addEventListener('keydown', e => { if (e.key === 'Escape') hideFind(); });
document.getElementById('find-close').addEventListener('click', hideFind);
findOverlay.addEventListener('click', e => { if (e.target === findOverlay) hideFind(); });

// ──────────────────────────────────────────────────────────────────────────────
// Linter debounce on diagram change
// ──────────────────────────────────────────────────────────────────────────────

let _lintDebounce;
modeler.on('commandStack.changed', () => {
  if (!Linter.isVisible()) return;
  clearTimeout(_lintDebounce);
  _lintDebounce = setTimeout(() => Linter.run(), 1500);
});

// ──────────────────────────────────────────────────────────────────────────────
// Initialise feature modules
// ──────────────────────────────────────────────────────────────────────────────

const workspace = document.getElementById('workspace');
Linter.init(modeler, workspace);
XmlEditor.init(modeler, document.getElementById('lint-panel') || workspace);

TemplateManager.init(templates => {
  setCustomTemplates(templates);
  // Re-trigger properties panel refresh on current selection
  try {
    const sel = modeler.get('selection').get();
    if (sel.length) { modeler.get('selection').select([]); modeler.get('selection').select(sel); }
  } catch (_) {}
});

// ──────────────────────────────────────────────────────────────────────────────
// Boot
// ──────────────────────────────────────────────────────────────────────────────
openDiagram(EMPTY_XML);

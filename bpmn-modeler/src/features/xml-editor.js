/**
 * XML Editor — live inline BPMN XML editing panel.
 * Pulling XML from the modeler into the editor and pushing edits back.
 */

let _modeler = null;
let _panel = null;
let _textarea = null;
let _visible = false;
let _dirty = false;

export function init(modeler, container) {
  _modeler = modeler;
  _buildPanel(container);

  // Update XML in editor when diagram changes (debounced)
  let _debounce;
  modeler.on('commandStack.changed', () => {
    if (!_visible) return;
    clearTimeout(_debounce);
    _debounce = setTimeout(() => _loadXml(), 800);
  });
}

export function isVisible() { return _visible; }

export async function toggle(force) {
  _visible = force !== undefined ? !!force : !_visible;
  _panel.classList.toggle('xml-hidden', !_visible);
  const btn = document.getElementById('btn-xml');
  if (btn) btn.classList.toggle('active', _visible);
  if (_visible) await _loadXml();
}

async function _loadXml() {
  try {
    const { xml } = await _modeler.saveXML({ format: true });
    _textarea.value = xml;
    _dirty = false;
  } catch (e) {
    _textarea.value = `<!-- Could not serialize: ${e.message} -->`;
  }
}

function _buildPanel(container) {
  _panel = document.createElement('div');
  _panel.id = 'xml-editor-panel';
  _panel.className = 'xml-hidden';

  const header = document.createElement('div');
  header.className = 'xml-header';
  header.innerHTML = `<span class="xml-header-title">BPMN XML</span>`;

  const actions = document.createElement('div');
  actions.className = 'xml-header-actions';

  const applyBtn = document.createElement('button');
  applyBtn.className = 'btn btn-primary btn-sm';
  applyBtn.textContent = '↑ Apply to diagram';
  applyBtn.addEventListener('click', _applyXml);

  const refreshBtn = document.createElement('button');
  refreshBtn.className = 'btn btn-ghost btn-sm';
  refreshBtn.textContent = '↓ Refresh from diagram';
  refreshBtn.addEventListener('click', _loadXml);

  const closeBtn = document.createElement('button');
  closeBtn.className = 'btn btn-ghost btn-sm';
  closeBtn.textContent = '✕';
  closeBtn.addEventListener('click', () => toggle(false));

  actions.appendChild(applyBtn);
  actions.appendChild(refreshBtn);
  actions.appendChild(closeBtn);
  header.appendChild(actions);

  _textarea = document.createElement('textarea');
  _textarea.id = 'xml-textarea';
  _textarea.spellcheck = false;
  _textarea.addEventListener('input', () => { _dirty = true; });

  _panel.appendChild(header);
  _panel.appendChild(_textarea);
  container.insertAdjacentElement('afterend', _panel);
}

async function _applyXml() {
  const xml = _textarea.value.trim();
  if (!xml) return;
  try {
    await _modeler.importXML(xml);
    _modeler.get('canvas').zoom('fit-viewport');
    _dirty = false;
  } catch (e) {
    alert(`Failed to import XML:\n${e.message}`);
  }
}

/**
 * Template Manager — custom element template CRUD.
 * Persisted via Electron IPC to userData/custom-templates.json.
 */

// ── Helpers ───────────────────────────────────────────────────────────────────

function uid() { return Date.now().toString(36) + Math.random().toString(36).slice(2, 7); }

function el(tag, attrs = {}, ...children) {
  const node = document.createElement(tag);
  for (const [k, v] of Object.entries(attrs)) {
    if (k === 'class') node.className = v;
    else if (k.startsWith('on') && typeof v === 'function') node.addEventListener(k.slice(2).toLowerCase(), v);
    else if (k === 'html') node.innerHTML = v;
    else node.setAttribute(k, v);
  }
  children.flat().forEach(c => {
    if (c == null) return;
    node.appendChild(typeof c === 'string' ? document.createTextNode(c) : c);
  });
  return node;
}

const ELEMENT_TYPES = [
  { value: 'bpmn:Task',                    label: 'Task (generic)' },
  { value: 'bpmn:UserTask',               label: 'User Task' },
  { value: 'bpmn:ServiceTask',            label: 'Service Task' },
  { value: 'bpmn:ScriptTask',             label: 'Script Task' },
  { value: 'bpmn:SendTask',               label: 'Send Task' },
  { value: 'bpmn:ReceiveTask',            label: 'Receive Task' },
  { value: 'bpmn:BusinessRuleTask',       label: 'Business Rule Task' },
  { value: 'bpmn:ManualTask',             label: 'Manual Task' },
  { value: 'bpmn:CallActivity',           label: 'Call Activity' },
  { value: 'bpmn:SubProcess',             label: 'Sub-Process' },
  { value: 'bpmn:StartEvent',             label: 'Start Event' },
  { value: 'bpmn:EndEvent',              label: 'End Event' },
  { value: 'bpmn:IntermediateCatchEvent', label: 'Intermediate Catch Event' },
  { value: 'bpmn:IntermediateThrowEvent', label: 'Intermediate Throw Event' },
  { value: 'bpmn:ExclusiveGateway',       label: 'Exclusive Gateway' },
  { value: 'bpmn:ParallelGateway',        label: 'Parallel Gateway' },
  { value: 'bpmn:InclusiveGateway',       label: 'Inclusive Gateway' },
];

const FIELD_TYPES = [
  { value: 'text',     label: 'Text' },
  { value: 'textarea', label: 'Text Area' },
  { value: 'number',   label: 'Number' },
  { value: 'email',    label: 'Email' },
  { value: 'url',      label: 'URL' },
  { value: 'select',   label: 'Dropdown' },
  { value: 'checkbox', label: 'Checkbox' },
];

// ── State ─────────────────────────────────────────────────────────────────────

let _templates = [], _onChange = null, _editingId = null;
let _overlay, _listPanel, _formPanel;

// ── Public API ────────────────────────────────────────────────────────────────

export async function init(onChange) {
  _onChange = onChange;
  _templates = await window.api.templatesLoad();
  _buildSkeleton();
  if (_onChange) _onChange(_templates);
}

export function open() {
  if (!_overlay) _buildSkeleton();
  _editingId = null;
  _renderList();
  _renderEmptyForm();
  _overlay.classList.remove('hidden');
}

export function close() {
  if (_overlay) _overlay.classList.add('hidden');
}

export function getTemplates() { return _templates; }

// ── Skeleton ──────────────────────────────────────────────────────────────────

function _buildSkeleton() {
  if (_overlay) return;
  _overlay = el('div', { class: 'tm-overlay hidden', id: 'tm-overlay' });
  const dlg = el('div', { class: 'tm-dialog' });

  const hdr = el('div', { class: 'tm-header' },
    el('span', { class: 'tm-title' }, 'Template Manager'),
    el('div', { class: 'tm-header-actions' },
      el('button', { class: 'btn btn-primary btn-sm', onclick: () => _openForm(null) }, '+ New Template'),
      el('button', { class: 'tm-close', onclick: close }, '✕'),
    ),
  );

  const body = el('div', { class: 'tm-body' });
  _listPanel = el('div', { class: 'tm-list-panel' });
  _formPanel = el('div', { class: 'tm-form-panel' });
  body.appendChild(_listPanel);
  body.appendChild(_formPanel);
  dlg.appendChild(hdr);
  dlg.appendChild(body);
  _overlay.appendChild(dlg);
  document.body.appendChild(_overlay);

  _overlay.addEventListener('click', e => { if (e.target === _overlay) close(); });
  document.addEventListener('keydown', e => { if (e.key === 'Escape' && !_overlay.classList.contains('hidden')) close(); });
}

// ── List panel ────────────────────────────────────────────────────────────────

function _renderList() {
  _listPanel.innerHTML = '';
  if (!_templates.length) {
    _listPanel.appendChild(el('div', { class: 'tm-empty' },
      el('p', {}, 'No custom templates yet.'),
      el('p', { class: 'tm-empty-hint' }, 'Click "+ New Template" to create one.'),
    ));
    return;
  }
  _templates.forEach((tmpl, idx) => {
    const typeMeta = ELEMENT_TYPES.find(t => t.value === tmpl.elementType);
    const row = el('div', { class: `tm-list-row${_editingId === tmpl.id ? ' active' : ''}`, onclick: () => _openForm(tmpl.id) });
    const info = el('div', { class: 'tm-list-info' },
      el('div', { class: 'tm-list-name' }, tmpl.name || '(unnamed)'),
      el('div', { class: 'tm-list-meta' },
        el('span', { class: 'tm-tag' }, typeMeta?.label ?? tmpl.elementType),
        el('span', { class: 'tm-tag tm-tag-count' }, `${(tmpl.fields||[]).length} field(s)`),
        tmpl.applyToSubtypes ? el('span', { class: 'tm-tag tm-tag-sub' }, 'subtypes') : null,
      ),
    );
    const acts = el('div', { class: 'tm-list-actions' });
    if (idx > 0) acts.appendChild(el('button', { class: 'btn btn-ghost btn-icon btn-sm', onclick: e => { e.stopPropagation(); _reorder(idx, idx - 1); } }, '↑'));
    if (idx < _templates.length - 1) acts.appendChild(el('button', { class: 'btn btn-ghost btn-icon btn-sm', onclick: e => { e.stopPropagation(); _reorder(idx, idx + 1); } }, '↓'));
    acts.appendChild(el('button', { class: 'btn btn-danger btn-sm', onclick: e => { e.stopPropagation(); _delete(tmpl.id); } }, '✕'));
    row.appendChild(info); row.appendChild(acts);
    _listPanel.appendChild(row);
  });
}

// ── Form panel ────────────────────────────────────────────────────────────────

function _openForm(id) { _editingId = id; _renderList(); _renderForm(); }

function _renderEmptyForm() {
  _formPanel.innerHTML = '';
  _formPanel.appendChild(el('div', { class: 'tm-form-empty' },
    el('div', { class: 'tm-form-empty-icon' }, '📋'),
    el('p', {}, 'Select a template to edit, or click "+ New Template".'),
  ));
}

function _renderForm() {
  _formPanel.innerHTML = '';
  const isNew = !_editingId;
  const tmpl = isNew
    ? { id: uid(), name: '', elementType: 'bpmn:Task', applyToSubtypes: false, fields: [] }
    : JSON.parse(JSON.stringify(_templates.find(t => t.id === _editingId) || {}));

  const nameIn  = el('input', { class: 'tm-input', type: 'text', placeholder: 'e.g. Project Metadata', value: tmpl.name || '' });
  const typeEl  = el('select', { class: 'tm-select' });
  ELEMENT_TYPES.forEach(({ value, label }) => {
    const o = document.createElement('option'); o.value = value; o.textContent = label;
    typeEl.appendChild(o);
  });
  typeEl.value = tmpl.elementType || 'bpmn:Task';

  const subtypeCheck = el('input', { type: 'checkbox', class: 'tm-check' });
  subtypeCheck.checked = !!tmpl.applyToSubtypes;

  const fields = (tmpl.fields || []).map(f => ({ ...f }));
  const fieldsContainer = el('div', { class: 'tm-fields-container' });

  function renderFields() {
    fieldsContainer.innerHTML = '';
    if (!fields.length) { fieldsContainer.appendChild(el('div', { class: 'tm-fields-empty' }, 'No fields yet. Click "Add Field".')); return; }
    fields.forEach((field, fi) => {
      const card = el('div', { class: 'tm-field-card' });
      const ftSel = el('select', { class: 'tm-select' });
      FIELD_TYPES.forEach(({ value, label }) => { const o = document.createElement('option'); o.value = value; o.textContent = label; ftSel.appendChild(o); });
      ftSel.value = field.type || 'text';
      ftSel.addEventListener('change', () => { field.type = ftSel.value; renderOptions(); });

      const labelIn = el('input', { class: 'tm-input', type: 'text', placeholder: 'Field label', value: field.label || '' });
      labelIn.addEventListener('input', () => { field.label = labelIn.value; });
      const phIn = el('input', { class: 'tm-input', type: 'text', placeholder: 'Placeholder (optional)', value: field.placeholder || '' });
      phIn.addEventListener('input', () => { field.placeholder = phIn.value; });
      const reqCk = el('input', { type: 'checkbox', class: 'tm-check' }); reqCk.checked = !!field.required;
      reqCk.addEventListener('change', () => { field.required = reqCk.checked; });

      const optArea = el('div', { class: 'tm-options-area' });
      function renderOptions() {
        optArea.innerHTML = '';
        if (field.type !== 'select') return;
        optArea.appendChild(el('div', { class: 'tm-option-header' }, el('span', { class: 'tm-sublabel' }, 'Dropdown options')));
        (field.options || []).forEach((opt, oi) => {
          const vIn = el('input', { class: 'tm-input', type: 'text', placeholder: 'Value', value: opt.value || '' });
          vIn.addEventListener('input', e => { opt.value = e.target.value; });
          const lIn = el('input', { class: 'tm-input', type: 'text', placeholder: 'Label', value: opt.label || '' });
          lIn.addEventListener('input', e => { opt.label = e.target.value; });
          const delBtn = el('button', { class: 'btn btn-danger btn-icon btn-sm', onclick: () => { field.options.splice(oi, 1); renderOptions(); } }, '✕');
          optArea.appendChild(el('div', { class: 'tm-option-row' }, vIn, lIn, delBtn));
        });
        optArea.appendChild(el('button', { class: 'btn btn-ghost btn-sm', onclick: () => { if (!field.options) field.options = []; field.options.push({ value: '', label: '' }); renderOptions(); } }, '+ Add option'));
      }
      renderOptions();

      card.appendChild(el('div', { class: 'tm-field-card-head' },
        el('span', { class: 'tm-field-order' }, `#${fi + 1}`), ftSel, labelIn,
        el('button', { class: 'btn btn-danger btn-icon btn-sm', onclick: () => { fields.splice(fi, 1); renderFields(); } }, '✕'),
      ));
      card.appendChild(el('div', { class: 'tm-field-card-body' },
        el('div', { class: 'tm-field-row' }, el('span', { class: 'tm-sublabel' }, 'Placeholder'), phIn),
        el('div', { class: 'tm-field-row' }, reqCk, el('label', { class: 'tm-sublabel' }, 'Required')),
        optArea,
      ));
      fieldsContainer.appendChild(card);
    });
  }
  renderFields();

  const addFieldBtn = el('button', { class: 'btn btn-ghost btn-sm', onclick: () => { fields.push({ id: uid(), label: '', type: 'text', placeholder: '', required: false }); renderFields(); } }, '+ Add Field');

  async function save() {
    const name = nameIn.value.trim();
    if (!name) { nameIn.focus(); nameIn.classList.add('tm-input-error'); return; }
    nameIn.classList.remove('tm-input-error');
    const payload = { id: tmpl.id, name, elementType: typeEl.value, applyToSubtypes: subtypeCheck.checked, fields: fields.map(f => ({ ...f })) };
    _templates = await window.api.templatesSave(payload);
    _editingId = null;
    if (_onChange) _onChange(_templates);
    _renderList(); _renderEmptyForm();
  }

  _formPanel.appendChild(el('div', { class: 'tm-form-title' }, isNew ? 'New Template' : `Edit: ${tmpl.name || '(unnamed)'}`));
  _formPanel.appendChild(el('div', { class: 'tm-meta-section' },
    el('div', { class: 'tm-form-row' }, el('label', { class: 'tm-label' }, 'Template Name *'), nameIn),
    el('div', { class: 'tm-form-row' }, el('label', { class: 'tm-label' }, 'Element Type'), typeEl),
    el('div', { class: 'tm-form-row-inline' }, subtypeCheck, el('label', { class: 'tm-label' }, 'Apply to sub-types')),
  ));
  _formPanel.appendChild(el('div', { class: 'tm-section-title' }, 'Fields'));
  _formPanel.appendChild(fieldsContainer);
  _formPanel.appendChild(addFieldBtn);
  _formPanel.appendChild(el('div', { class: 'tm-form-footer' },
    el('button', { class: 'btn btn-ghost', onclick: () => { _editingId = null; _renderList(); _renderEmptyForm(); } }, 'Cancel'),
    el('button', { class: 'btn btn-primary', onclick: save }, isNew ? 'Create Template' : 'Save Changes'),
  ));
}

// ── CRUD ──────────────────────────────────────────────────────────────────────

async function _delete(id) {
  const tmpl = _templates.find(t => t.id === id);
  if (!tmpl || !confirm(`Delete template "${tmpl.name}"?`)) return;
  _templates = await window.api.templatesDelete(id);
  if (_editingId === id) { _editingId = null; _renderEmptyForm(); }
  if (_onChange) _onChange(_templates);
  _renderList();
}

async function _reorder(from, to) {
  const arr = _templates.slice();
  const [item] = arr.splice(from, 1);
  arr.splice(to, 0, item);
  _templates = await window.api.templatesReorder(arr);
  if (_onChange) _onChange(_templates);
  _renderList();
}

/**
 * Linter — bpmnlint integration
 * Runs validation rules against the current diagram and renders an annotated
 * results panel. Issues are clickable — clicking selects and scrolls to the element.
 */

import { Linter } from 'bpmnlint';
import startEventRequired   from 'bpmnlint/rules/start-event-required';
import endEventRequired     from 'bpmnlint/rules/end-event-required';
import noDisconnected       from 'bpmnlint/rules/no-disconnected';
import labelRequired        from 'bpmnlint/rules/label-required';
import conditionalFlows     from 'bpmnlint/rules/conditional-flows';
import noImplicitSplit      from 'bpmnlint/rules/no-implicit-split';
import noImplicitEnd        from 'bpmnlint/rules/no-implicit-end';
import superfluousGateway   from 'bpmnlint/rules/superfluous-gateway';
import fakeJoin             from 'bpmnlint/rules/fake-join';
import noDuplicateFlows     from 'bpmnlint/rules/no-duplicate-sequence-flows';
import superfluousLabel     from 'bpmnlint/rules/superfluous-label';
import standardSize         from 'bpmnlint/rules/standard-size';

const RULES = {
  'start-event-required':        { rule: startEventRequired,  default: 'error' },
  'end-event-required':          { rule: endEventRequired,    default: 'error' },
  'no-disconnected':             { rule: noDisconnected,      default: 'error' },
  'label-required':              { rule: labelRequired,       default: 'warn'  },
  'conditional-flows':           { rule: conditionalFlows,    default: 'warn'  },
  'no-implicit-split':           { rule: noImplicitSplit,     default: 'warn'  },
  'no-implicit-end':             { rule: noImplicitEnd,       default: 'warn'  },
  'superfluous-gateway':         { rule: superfluousGateway,  default: 'warn'  },
  'fake-join':                   { rule: fakeJoin,            default: 'warn'  },
  'no-duplicate-sequence-flows': { rule: noDuplicateFlows,    default: 'warn'  },
  'superfluous-label':           { rule: superfluousLabel,    default: 'info'  },
  'standard-size':               { rule: standardSize,        default: 'off'   },
};

const SEV_ORDER = { error: 0, warn: 1, info: 2, off: 3 };
const SEV_ICON  = { error: '✖', warn: '⚠', info: 'ℹ' };

let _modeler = null, _panel = null, _visible = false, _overrides = {};

function severity(name) {
  return _overrides[name] !== undefined ? _overrides[name] : RULES[name].default;
}

// ── Public API ────────────────────────────────────────────────────────────────

export function init(modeler, container) {
  _modeler = modeler;
  _panel = document.createElement('div');
  _panel.id = 'lint-panel';
  _panel.className = 'lint-hidden';
  container.insertAdjacentElement('afterend', _panel);
}

export function isVisible() { return _visible; }

export function toggle(force) {
  _visible = force !== undefined ? !!force : !_visible;
  if (_panel) _panel.classList.toggle('lint-hidden', !_visible);
  const btn = document.getElementById('btn-validate');
  if (btn) btn.classList.toggle('active', _visible);
}

export async function run() {
  if (!_modeler) return [];

  const config = {};
  for (const name of Object.keys(RULES)) {
    const sev = severity(name);
    if (sev !== 'off') config[name] = sev;
  }

  const linter = new Linter({
    config: { rules: config },
    resolver: {
      resolveRule: (pkg, name) => Promise.resolve(RULES[name]?.rule ?? null),
      resolveConfig: () => Promise.resolve({}),
    },
  });

  let root;
  try { root = _modeler.get('canvas').getRootElement(); }
  catch (_) { return []; }
  const definitions = root?.businessObject?.$parent;
  if (!definitions) return [];

  const results = await linter.lint(definitions);
  render(results);
  return results;
}

// ── Render ────────────────────────────────────────────────────────────────────

function render(results) {
  if (!_panel) return;
  _panel.innerHTML = '';

  const all = [];
  for (const [name, reports] of Object.entries(results)) {
    const sev = severity(name);
    if (sev === 'off') continue;
    for (const r of (reports || [])) all.push({ name, sev, r });
  }
  all.sort((a, b) => (SEV_ORDER[a.sev] ?? 9) - (SEV_ORDER[b.sev] ?? 9));

  const errs  = all.filter(x => x.sev === 'error').length;
  const warns = all.filter(x => x.sev === 'warn').length;
  const infos = all.filter(x => x.sev === 'info').length;

  // Header
  const hdr = document.createElement('div');
  hdr.className = 'lint-header';
  hdr.innerHTML = `
    <span class="lint-title">Validation</span>
    <span class="lint-counts">
      ${errs  ? `<span class="lint-badge lint-badge-err">✖ ${errs}</span>` : ''}
      ${warns ? `<span class="lint-badge lint-badge-warn">⚠ ${warns}</span>` : ''}
      ${infos ? `<span class="lint-badge lint-badge-info">ℹ ${infos}</span>` : ''}
      ${all.length === 0 ? '<span class="lint-badge lint-badge-ok">✔ No issues</span>' : ''}
    </span>
    <span class="lint-actions">
      <button class="lint-act-btn" id="lint-rules-btn" title="Configure rules">Rules</button>
      <button class="lint-act-btn" id="lint-close-btn">✕</button>
    </span>`;
  _panel.appendChild(hdr);

  document.getElementById('lint-close-btn').addEventListener('click', () => toggle(false));
  document.getElementById('lint-rules-btn').addEventListener('click', toggleRulesConfig);

  if (all.length === 0) {
    const ok = document.createElement('div');
    ok.className = 'lint-ok';
    ok.textContent = '✔ Diagram passes all active validation rules.';
    _panel.appendChild(ok);
    return;
  }

  const list = document.createElement('div');
  list.className = 'lint-list';
  for (const { name, sev, r } of all) {
    const row = document.createElement('div');
    row.className = `lint-row lint-row-${sev === 'error' ? 'err' : sev}`;
    const id = r.id || r.element?.id;
    row.innerHTML = `
      <span class="lint-icon lint-icon-${sev === 'error' ? 'err' : sev}">${SEV_ICON[sev]}</span>
      <span class="lint-msg">${r.message || '(no message)'}</span>
      <span class="lint-rule-tag">${name}</span>
      ${id ? `<span class="lint-elem-tag">#${id}</span>` : ''}`;
    if (id) {
      row.style.cursor = 'pointer';
      row.addEventListener('click', () => {
        try {
          const el = _modeler.get('elementRegistry').get(id);
          if (el) { _modeler.get('selection').select(el); _modeler.get('canvas').scrollToElement(el); }
        } catch (_) {}
      });
    }
    list.appendChild(row);
  }
  _panel.appendChild(list);
}

// ── Rules config ──────────────────────────────────────────────────────────────

function toggleRulesConfig() {
  const existing = document.getElementById('lint-rules-panel');
  if (existing) { existing.remove(); return; }

  const rp = document.createElement('div');
  rp.id = 'lint-rules-panel';
  rp.className = 'lint-rules-panel';

  const title = document.createElement('div');
  title.className = 'lint-rules-title';
  title.textContent = 'Rule Configuration';
  rp.appendChild(title);

  for (const [name] of Object.entries(RULES)) {
    const row = document.createElement('div');
    row.className = 'lint-rule-row';
    const label = document.createElement('span');
    label.className = 'lint-rule-name';
    label.textContent = name;
    const sel = document.createElement('select');
    sel.className = 'lint-rule-sel';
    ['error', 'warn', 'info', 'off'].forEach(s => {
      const o = document.createElement('option');
      o.value = s; o.textContent = s.charAt(0).toUpperCase() + s.slice(1);
      sel.appendChild(o);
    });
    sel.value = severity(name);
    sel.addEventListener('change', () => { _overrides[name] = sel.value; });
    row.appendChild(label); row.appendChild(sel);
    rp.appendChild(row);
  }

  const actions = document.createElement('div');
  actions.className = 'lint-rules-actions';
  const apply = document.createElement('button');
  apply.className = 'btn btn-primary btn-sm';
  apply.textContent = 'Apply & Re-validate';
  apply.addEventListener('click', () => { rp.remove(); run(); });
  const reset = document.createElement('button');
  reset.className = 'btn btn-ghost btn-sm';
  reset.textContent = 'Reset defaults';
  reset.addEventListener('click', () => { _overrides = {}; rp.remove(); run(); });
  actions.appendChild(reset);
  actions.appendChild(apply);
  rp.appendChild(actions);
  _panel.appendChild(rp);
}

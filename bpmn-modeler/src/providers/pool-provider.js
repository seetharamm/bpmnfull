/**
 * Pool / Participant / Lane properties provider
 *
 * Adds a dedicated "Pool / Lane" group to the properties panel for:
 *  - bpmn:Participant  — process reference, isExecutable, multi-instance
 *  - bpmn:Lane        — partition name, documentation
 *  - bpmn:Process     — process-level is-executable flag
 */

import { html } from 'htm/preact';
import { useService } from 'bpmn-js-properties-panel';

// ── Entry point ───────────────────────────────────────────────────────────────

class PoolProvider {
  constructor(propertiesPanel, injector) {
    propertiesPanel.registerProvider(500, this);
    this._injector = injector;
  }

  getGroups(element) {
    return (groups) => {
      if (isPool(element) || isLane(element)) {
        groups.push(createPoolGroup(element, this._injector));
      }
      return groups;
    };
  }
}

PoolProvider.$inject = ['propertiesPanel', 'injector'];

// ── Helpers ───────────────────────────────────────────────────────────────────

function isPool(element) {
  return element.type === 'bpmn:Participant';
}

function isLane(element) {
  return element.type === 'bpmn:Lane';
}

function createPoolGroup(element, injector) {
  const translate = injector.get('translate');
  const entries   = isPool(element)
    ? poolEntries(element, injector)
    : laneEntries(element, injector);

  return {
    id:      'pool-lane',
    label:   translate(isPool(element) ? 'Pool / Participant' : 'Lane'),
    entries,
  };
}

// ── Pool entries ──────────────────────────────────────────────────────────────

function poolEntries(element, injector) {
  const modeling     = injector.get('modeling');
  const bpmnFactory  = injector.get('bpmnFactory');
  const entries      = [];

  // participant.name handled by general group; add process-specific props
  const bo = element.businessObject;
  const process = bo.processRef;

  if (!process) return entries;

  // isExecutable toggle
  entries.push({
    id:        'isExecutable',
    component: IsExecutableEntry,
    element,
    process,
    modeling,
  });

  return entries;
}

function IsExecutableEntry({ element, process, modeling }) {
  const translate = useService('translate');

  const getValue = () => process.isExecutable || false;
  const setValue = (value) => {
    modeling.updateModdleProperties(element, process, { isExecutable: value });
  };

  return html`
    <div class="bio-properties-panel-entry">
      <label class="bio-properties-panel-label">${translate('Executable')}</label>
      <input
        type="checkbox"
        class="bio-properties-panel-input"
        checked=${getValue()}
        onChange=${(e) => setValue(e.target.checked)}
      />
    </div>
  `;
}

// ── Lane entries ──────────────────────────────────────────────────────────────

function laneEntries(element, injector) {
  const modeling = injector.get('modeling');
  const entries  = [];

  entries.push({
    id:        'lanePartitionName',
    component: LanePartitionEntry,
    element,
    modeling,
  });

  entries.push({
    id:        'laneDocumentation',
    component: LaneDocumentationEntry,
    element,
    modeling,
  });

  return entries;
}

function LanePartitionEntry({ element, modeling }) {
  const translate = useService('translate');
  const bo        = element.businessObject;

  const getValue = () => bo.partitionName || '';
  const setValue = (value) => {
    modeling.updateModdleProperties(element, bo, { partitionName: value });
  };

  return html`
    <div class="bio-properties-panel-entry">
      <label class="bio-properties-panel-label">${translate('Partition Name')}</label>
      <input
        type="text"
        class="bio-properties-panel-input"
        value=${getValue()}
        onInput=${(e) => setValue(e.target.value)}
        placeholder="optional"
      />
    </div>
  `;
}

function LaneDocumentationEntry({ element, modeling }) {
  const translate = useService('translate');
  const bo        = element.businessObject;

  const getValue = () => (bo.documentation && bo.documentation[0] && bo.documentation[0].text) || '';
  const setValue = (value) => {
    const bpmnFactory = useService('bpmnFactory');
    const docs = value ? [bpmnFactory.create('bpmn:Documentation', { text: value })] : [];
    modeling.updateModdleProperties(element, bo, { documentation: docs });
  };

  return html`
    <div class="bio-properties-panel-entry">
      <label class="bio-properties-panel-label">${translate('Documentation')}</label>
      <textarea
        class="bio-properties-panel-input"
        rows="3"
        value=${getValue()}
        onInput=${(e) => setValue(e.target.value)}
        placeholder="Lane description..."
      ></textarea>
    </div>
  `;
}

export default {
  __init__: ['poolProvider'],
  poolProvider: ['type', PoolProvider],
};

/**
 * Sub-Process / Call Activity / Ad-hoc Sub-Process properties provider
 *
 * Adds a dedicated "Sub-Process" group to the properties panel for:
 *  - bpmn:SubProcess         — isForCompensation, triggeredByEvent, isAdHoc
 *  - bpmn:AdHocSubProcess    — completionCondition, ordering, cancelRemainingInstances
 *  - bpmn:CallActivity       — calledElement, binding, version/tenant-id bindings
 *  - bpmn:Transaction        — method (compensate / cancel / store)
 */

import { html } from 'htm/preact';
import { useService } from 'bpmn-js-properties-panel';

// ── Entry point ───────────────────────────────────────────────────────────────

class SubProcessProvider {
  constructor(propertiesPanel, injector) {
    propertiesPanel.registerProvider(500, this);
    this._injector = injector;
  }

  getGroups(element) {
    return (groups) => {
      if (isSubProcess(element) || isCallActivity(element)) {
        groups.push(createSubProcessGroup(element, this._injector));
      }
      return groups;
    };
  }
}

SubProcessProvider.$inject = ['propertiesPanel', 'injector'];

// ── Helpers ───────────────────────────────────────────────────────────────────

function isSubProcess(element) {
  return (
    element.type === 'bpmn:SubProcess' ||
    element.type === 'bpmn:AdHocSubProcess' ||
    element.type === 'bpmn:Transaction'
  );
}

function isCallActivity(element) {
  return element.type === 'bpmn:CallActivity';
}

function createSubProcessGroup(element, injector) {
  const translate = injector.get('translate');

  let label;
  if (element.type === 'bpmn:CallActivity')       label = translate('Call Activity');
  else if (element.type === 'bpmn:Transaction')   label = translate('Transaction');
  else if (element.type === 'bpmn:AdHocSubProcess') label = translate('Ad-hoc Sub-Process');
  else                                            label = translate('Sub-Process');

  return {
    id:      'sub-process',
    label,
    entries: isCallActivity(element)
      ? callActivityEntries(element, injector)
      : subProcessEntries(element, injector),
  };
}

// ── Sub-Process entries ───────────────────────────────────────────────────────

function subProcessEntries(element, injector) {
  const modeling = injector.get('modeling');
  const entries  = [];
  const bo       = element.businessObject;

  entries.push({
    id:        'triggeredByEvent',
    component: ToggleEntry,
    element,
    bo,
    modeling,
    propName:  'triggeredByEvent',
    label:     'Triggered by Event',
  });

  entries.push({
    id:        'isForCompensation',
    component: ToggleEntry,
    element,
    bo,
    modeling,
    propName:  'isForCompensation',
    label:     'For Compensation',
  });

  if (element.type === 'bpmn:AdHocSubProcess') {
    entries.push({
      id:        'cancelRemainingInstances',
      component: ToggleEntry,
      element,
      bo,
      modeling,
      propName:  'cancelRemainingInstances',
      label:     'Cancel Remaining Instances',
    });

    entries.push({
      id:        'ordering',
      component: OrderingEntry,
      element,
      bo,
      modeling,
    });

    entries.push({
      id:        'completionCondition',
      component: ExpressionEntry,
      element,
      bo,
      modeling,
      propName:  'completionCondition',
      label:     'Completion Condition',
      placeholder: '${allTasksCompleted}',
    });
  }

  if (element.type === 'bpmn:Transaction') {
    entries.push({
      id:        'transactionMethod',
      component: TransactionMethodEntry,
      element,
      bo,
      modeling,
    });
  }

  return entries;
}

// ── Call Activity entries ─────────────────────────────────────────────────────

function callActivityEntries(element, injector) {
  const modeling = injector.get('modeling');
  const bo       = element.businessObject;

  return [
    {
      id:        'calledElement',
      component: TextEntry,
      element,
      bo,
      modeling,
      propName:  'calledElement',
      label:     'Called Element',
      placeholder: 'process-key or expression',
    },
    {
      id:        'calledElementBinding',
      component: BindingEntry,
      element,
      bo,
      modeling,
    },
    {
      id:        'calledElementVersion',
      component: TextEntry,
      element,
      bo,
      modeling,
      propName:  'calledElementVersion',
      label:     'Version (if version binding)',
      placeholder: '1',
    },
    {
      id:        'calledElementTenantId',
      component: TextEntry,
      element,
      bo,
      modeling,
      propName:  'calledElementTenantId',
      label:     'Tenant ID',
      placeholder: 'optional',
    },
  ];
}

// ── Reusable entry components ─────────────────────────────────────────────────

function ToggleEntry({ element, bo, modeling, propName, label }) {
  const translate = useService('translate');
  const getValue  = () => bo[propName] || false;
  const setValue  = (v) => modeling.updateModdleProperties(element, bo, { [propName]: v });

  return html`
    <div class="bio-properties-panel-entry">
      <label class="bio-properties-panel-label">${translate(label)}</label>
      <input
        type="checkbox"
        class="bio-properties-panel-input"
        checked=${getValue()}
        onChange=${(e) => setValue(e.target.checked)}
      />
    </div>
  `;
}

function TextEntry({ element, bo, modeling, propName, label, placeholder }) {
  const translate = useService('translate');
  const getValue  = () => bo[propName] || '';
  const setValue  = (v) => modeling.updateModdleProperties(element, bo, { [propName]: v });

  return html`
    <div class="bio-properties-panel-entry">
      <label class="bio-properties-panel-label">${translate(label)}</label>
      <input
        type="text"
        class="bio-properties-panel-input"
        value=${getValue()}
        onInput=${(e) => setValue(e.target.value)}
        placeholder=${placeholder || ''}
      />
    </div>
  `;
}

function ExpressionEntry({ element, bo, modeling, propName, label, placeholder }) {
  const translate = useService('translate');
  const getValue  = () => {
    const val = bo[propName];
    return val ? (val.body || '') : '';
  };
  const setValue  = (v) => {
    const bpmnFactory = useService('bpmnFactory');
    modeling.updateModdleProperties(element, bo, {
      [propName]: v ? bpmnFactory.create('bpmn:FormalExpression', { body: v }) : undefined,
    });
  };

  return html`
    <div class="bio-properties-panel-entry">
      <label class="bio-properties-panel-label">${translate(label)}</label>
      <input
        type="text"
        class="bio-properties-panel-input"
        value=${getValue()}
        onInput=${(e) => setValue(e.target.value)}
        placeholder=${placeholder || ''}
      />
    </div>
  `;
}

function OrderingEntry({ element, bo, modeling }) {
  const translate = useService('translate');
  const getValue  = () => bo.ordering || 'Parallel';
  const setValue  = (v) => modeling.updateModdleProperties(element, bo, { ordering: v });

  return html`
    <div class="bio-properties-panel-entry">
      <label class="bio-properties-panel-label">${translate('Ordering')}</label>
      <select
        class="bio-properties-panel-input"
        value=${getValue()}
        onChange=${(e) => setValue(e.target.value)}
      >
        <option value="Parallel">Parallel</option>
        <option value="Sequential">Sequential</option>
      </select>
    </div>
  `;
}

function BindingEntry({ element, bo, modeling }) {
  const translate = useService('translate');
  const getValue  = () => bo.calledElementBinding || 'latest';
  const setValue  = (v) => modeling.updateModdleProperties(element, bo, { calledElementBinding: v });

  return html`
    <div class="bio-properties-panel-entry">
      <label class="bio-properties-panel-label">${translate('Binding')}</label>
      <select
        class="bio-properties-panel-input"
        value=${getValue()}
        onChange=${(e) => setValue(e.target.value)}
      >
        <option value="latest">Latest</option>
        <option value="deployment">Deployment</option>
        <option value="version">Version</option>
        <option value="versionTag">Version Tag</option>
      </select>
    </div>
  `;
}

function TransactionMethodEntry({ element, bo, modeling }) {
  const translate = useService('translate');
  const getValue  = () => bo.method || '##compensate';
  const setValue  = (v) => modeling.updateModdleProperties(element, bo, { method: v });

  return html`
    <div class="bio-properties-panel-entry">
      <label class="bio-properties-panel-label">${translate('Transaction Method')}</label>
      <select
        class="bio-properties-panel-input"
        value=${getValue()}
        onChange=${(e) => setValue(e.target.value)}
      >
        <option value="##compensate">Compensate</option>
        <option value="##cancel">Cancel</option>
        <option value="##store">Store</option>
      </select>
    </div>
  `;
}

export default {
  __init__: ['subProcessProvider'],
  subProcessProvider: ['type', SubProcessProvider],
};

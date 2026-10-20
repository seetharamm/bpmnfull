/**
 * TimerPropertiesProvider — adds "Timer" group for timer events.
 */

import { h, Fragment } from '@bpmn-io/properties-panel/preact';
import { useState, useCallback, useEffect } from '@bpmn-io/properties-panel/preact/hooks';
import { useService } from 'bpmn-js-properties-panel';
import { is, getBusinessObject } from 'bpmn-js/lib/util/ModelUtil';

const TIMER_TYPES = ['bpmn:StartEvent', 'bpmn:IntermediateCatchEvent', 'bpmn:BoundaryEvent'];
const DEF_TYPES = [
  { value: 'timeDate',     label: 'Date',     ph: '2025-12-31T23:59:59Z' },
  { value: 'timeCycle',    label: 'Cycle',    ph: 'R5/PT1H  or  0 0 12 * * ?' },
  { value: 'timeDuration', label: 'Duration', ph: 'PT15M' },
];
const HINTS = {
  timeDate:     ['ISO 8601: 2025-06-01T09:00:00Z', 'With offset: 2025-06-01T11:00:00+02:00'],
  timeCycle:    ['ISO repeat: R5/PT1H (5×, every hour)', 'No limit: R/PT30M', 'Cron: 0 0 12 * * ?'],
  timeDuration: ['ISO duration: PT15M (15 min)', 'P1DT2H30M (1 day 2h 30m)'],
};

function getTimerDef(element) {
  const defs = getBusinessObject(element).get('eventDefinitions') || [];
  return defs.find(d => is(d, 'bpmn:TimerEventDefinition')) || null;
}
function isTimer(element) {
  return TIMER_TYPES.some(t => is(element, t)) && !!getTimerDef(element);
}
function readDefType(td) {
  if (!td) return '';
  if (td.get('timeDate')     !== undefined) return 'timeDate';
  if (td.get('timeCycle')    !== undefined) return 'timeCycle';
  if (td.get('timeDuration') !== undefined) return 'timeDuration';
  return '';
}
function readExpr(td, type) {
  if (!td || !type) return '';
  return td.get(type)?.get('body') || '';
}

function TimerEntry({ element, id }) {
  const bpmnFactory  = useService('bpmnFactory');
  const commandStack = useService('commandStack');

  const read = () => { const td = getTimerDef(element); return { type: readDefType(td), expr: readExpr(td, readDefType(td)) }; };
  const [state, setState] = useState(read);
  useEffect(() => setState(read()), [element]);

  const setType = useCallback(newType => {
    const td = getTimerDef(element);
    if (!td) return;
    const fe = state.expr ? bpmnFactory.create('bpmn:FormalExpression', { body: state.expr }) : undefined;
    commandStack.execute('element.updateModdleProperties', { element, moddleElement: td, properties: {
      timeDate: newType === 'timeDate' ? fe : undefined,
      timeCycle: newType === 'timeCycle' ? fe : undefined,
      timeDuration: newType === 'timeDuration' ? fe : undefined,
    }});
    setState(s => ({ ...s, type: newType }));
  }, [element, bpmnFactory, commandStack, state.expr]);

  const setExpr = useCallback(val => {
    const td = getTimerDef(element);
    if (!td || !state.type) return;
    const fe = val ? bpmnFactory.create('bpmn:FormalExpression', { body: val }) : undefined;
    commandStack.execute('element.updateModdleProperties', { element, moddleElement: td, properties: { [state.type]: fe }});
  }, [element, bpmnFactory, commandStack, state.type]);

  return h('div', { 'data-entry-id': id, style: 'padding: 8px 0' },
    h('div', { style: 'margin-bottom: 8px; display: flex; gap: 4px; flex-wrap: wrap' },
      DEF_TYPES.map(({ value, label }) =>
        h('button', { key: value, type: 'button',
          style: `padding: 3px 10px; border-radius: 3px; font-size: 11px; cursor: pointer; border: 1px solid ${state.type === value ? '#1e6fff' : '#3a3a3a'}; background: ${state.type === value ? '#1e3a6a' : '#2d2d2d'}; color: ${state.type === value ? '#7eb8ff' : '#ccc'}`,
          onClick: () => setType(value) }, label)
      )
    ),
    state.type && h(Fragment, null,
      h('input', { type: 'text', placeholder: DEF_TYPES.find(d => d.value === state.type)?.ph, value: state.expr,
        style: 'width: 100%; padding: 6px 8px; background: #1e1e1e; border: 1px solid #3a3a3a; border-radius: 3px; color: #ce9178; font-size: 12px; font-family: monospace; margin-bottom: 6px',
        onInput: e => setState(s => ({ ...s, expr: e.target.value })),
        onBlur:  e => setExpr(e.target.value) }),
      h('div', { style: 'font-size: 10px; color: #888; line-height: 1.6' },
        (HINTS[state.type] || []).map((h2, i) => h('div', { key: i }, '→ ' + h2))
      )
    ),
    !state.type && h('div', { style: 'font-size: 11px; color: #888' }, 'Select a timer type above.')
  );
}

class TimerPropertiesProvider {
  constructor(propertiesPanel, injector) {
    propertiesPanel.registerProvider(400, this);
    this._injector = injector;
  }
  getGroups(element) {
    return groups => {
      if (!isTimer(element)) return groups;
      groups.push({ id: 'timer-config', label: 'Timer', shouldOpen: true, entries: [{ id: 'timer-entry', component: TimerEntry }] });
      return groups;
    };
  }
}
TimerPropertiesProvider.$inject = ['propertiesPanel', 'injector'];
export default { __init__: ['timerPropertiesProvider'], timerPropertiesProvider: ['type', TimerPropertiesProvider] };

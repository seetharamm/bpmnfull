/**
 * CustomTemplatesProvider — injects user-defined template groups into the
 * properties panel for any element that matches the template's elementType.
 */
import { h } from '@bpmn-io/properties-panel/preact';
import { useState, useEffect } from '@bpmn-io/properties-panel/preact/hooks';
import { useService } from 'bpmn-js-properties-panel';
import { is } from 'bpmn-js/lib/util/ModelUtil';

let _templates = [];
export function setCustomTemplates(templates) { _templates = templates; }

const DOC_NS = 'custom:metadata';

function readMeta(element) {
  try {
    const doc = element.businessObject?.documentation?.find(d => d.textFormat === DOC_NS);
    return doc ? JSON.parse(doc.text) : {};
  } catch (_) { return {}; }
}

function TemplateEntry({ element, template }) {
  const commandStack = useService('commandStack');
  const bpmnFactory  = useService('bpmnFactory');
  const [values, setValues] = useState(() => readMeta(element));

  useEffect(() => { setValues(readMeta(element)); }, [element]);

  const persist = (key, val) => {
    const next = { ...values, [key]: val };
    setValues(next);
    const bo = element.businessObject;
    if (!bo.documentation) bo.documentation = [];
    let doc = bo.documentation.find(d => d.textFormat === DOC_NS);
    if (!doc) { doc = bpmnFactory.create('bpmn:Documentation', { textFormat: DOC_NS }); bo.documentation.push(doc); }
    commandStack.execute('element.updateModdleProperties', { element, moddleElement: doc, properties: { text: JSON.stringify(next) } });
  };

  return h('div', { style: 'padding: 4px 0' },
    (template.fields || []).map(field =>
      h('div', { key: field.id, style: 'margin-bottom: 10px' },
        h('label', { style: 'display: block; font-size: 11px; font-weight: 600; color: #888; text-transform: uppercase; letter-spacing: .4px; margin-bottom: 4px' },
          field.label + (field.required ? ' *' : '')),
        field.type === 'textarea'
          ? h('textarea', { style: 'width:100%; padding:6px 8px; background:#1e1e1e; border:1px solid #3a3a3a; border-radius:3px; color:#ccc; font-size:12px; resize:vertical; min-height:60px',
              value: values[field.id] || '', placeholder: field.placeholder || '',
              onInput: e => persist(field.id, e.target.value) })
          : field.type === 'select'
          ? h('select', { style: 'width:100%; padding:6px 8px; background:#1e1e1e; border:1px solid #3a3a3a; border-radius:3px; color:#ccc; font-size:12px',
              value: values[field.id] || '', onChange: e => persist(field.id, e.target.value) },
              h('option', { value: '' }, '— select —'),
              (field.options || []).map(o => h('option', { key: o.value, value: o.value }, o.label || o.value)))
          : field.type === 'checkbox'
          ? h('input', { type: 'checkbox', checked: !!values[field.id], onChange: e => persist(field.id, e.target.checked) })
          : h('input', { type: field.type || 'text', style: 'width:100%; padding:6px 8px; background:#1e1e1e; border:1px solid #3a3a3a; border-radius:3px; color:#ccc; font-size:12px',
              value: values[field.id] || '', placeholder: field.placeholder || '',
              onInput: e => persist(field.id, e.target.value) })
      )
    )
  );
}

class CustomTemplatesProvider {
  constructor(propertiesPanel) { propertiesPanel.registerProvider(500, this); }
  getGroups(element) {
    return groups => {
      const matching = _templates.filter(t =>
        is(element, t.elementType) || (t.applyToSubtypes && element.type?.startsWith(t.elementType?.split(':')[0]))
      );
      matching.forEach(tmpl => {
        groups.push({
          id: `custom-tmpl-${tmpl.id}`,
          label: tmpl.name,
          entries: [{ id: `ct-${tmpl.id}`, component: props => TemplateEntry({ ...props, template: tmpl }) }],
        });
      });
      return groups;
    };
  }
}
CustomTemplatesProvider.$inject = ['propertiesPanel'];
export default { __init__: ['customTemplatesProvider'], customTemplatesProvider: ['type', CustomTemplatesProvider] };

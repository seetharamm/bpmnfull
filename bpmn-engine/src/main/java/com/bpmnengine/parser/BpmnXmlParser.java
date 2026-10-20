package com.bpmnengine.parser;

import com.bpmnengine.model.*;
import com.bpmnengine.model.Process;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses a BPMN 2.0 XML document into the {@link BpmnModel} object graph.
 *
 * Supports the full set of BPMN 2.0 elements:
 *   – events (start/end/intermediate/boundary) with all event definitions
 *   – tasks (user/service/script/businessRule/manual/receive/send)
 *   – subProcess, callActivity, transaction, adHocSubProcess
 *   – gateways (exclusive/inclusive/parallel/eventBased/complex)
 *   – sequenceFlows with condition expressions
 *   – multi-instance and standard loop characteristics
 *   – lanes, data objects, messages, signals, errors, escalations
 *   – extension elements (properties, executionListeners, taskListeners)
 */
@Component
public class BpmnXmlParser {

    private static final Logger log = LoggerFactory.getLogger(BpmnXmlParser.class);

    // BPMN 2.0 namespace
    private static final String NS_BPMN   = "http://www.omg.org/spec/BPMN/20100524/MODEL";
    // Activiti extension namespace
    private static final String NS_EXT    = "http://activiti.org/bpmn";
    // Camunda extension namespace (used by bpmn-js / Camunda Modeler exports)
    private static final String NS_CAMUNDA = "http://camunda.org/schema/1.0/bpmn";

    // ──────────────────────────────────────────────────────────────────────────
    // Public API
    // ──────────────────────────────────────────────────────────────────────────

    public BpmnModel parse(String xml) {
        try (InputStream is = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))) {
            return parse(is);
        } catch (Exception e) {
            throw new BpmnParseException("Failed to parse BPMN XML", e);
        }
    }

    public BpmnModel parse(InputStream is) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(is);
            doc.getDocumentElement().normalize();
            return parseDefinitions(doc.getDocumentElement());
        } catch (BpmnParseException e) {
            throw e;
        } catch (Exception e) {
            throw new BpmnParseException("Failed to parse BPMN XML", e);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Definitions
    // ──────────────────────────────────────────────────────────────────────────

    private BpmnModel parseDefinitions(Element root) {
        BpmnModel model = new BpmnModel();
        model.setId(root.getAttribute("id"));
        model.setName(root.getAttribute("name"));
        model.setTargetNamespace(root.getAttribute("targetNamespace"));
        model.setExporter(root.getAttribute("exporter"));
        model.setExporterVersion(root.getAttribute("exporterVersion"));

        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element el)) continue;
            String localName = el.getLocalName();
            if (localName == null) continue;

            switch (localName) {
                case "process"        -> model.getProcesses().add(parseProcess(el));
                case "message"        -> model.getMessages().add(parseMessage(el));
                case "signal"         -> model.getSignals().add(parseSignal(el));
                case "error"          -> model.getErrors().add(parseError(el));
                case "escalation"     -> model.getEscalations().add(parseEscalation(el));
                case "itemDefinition" -> model.getItemDefinitions().add(parseItemDefinition(el));
                default               -> log.trace("Skipping top-level element: {}", localName);
            }
        }
        return model;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Process
    // ──────────────────────────────────────────────────────────────────────────

    private Process parseProcess(Element el) {
        Process process = new Process();
        process.setId(el.getAttribute("id"));
        process.setName(el.getAttribute("name"));
        process.setExecutable(!"false".equals(el.getAttribute("isExecutable")));

        String pt = el.getAttribute("processType");
        if (!pt.isBlank()) {
            process.setProcessType(Process.ProcessType.valueOf(pt.toUpperCase()));
        }

        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node node = children.item(i);
            if (!(node instanceof Element child)) continue;
            String localName = child.getLocalName();
            if (localName == null) continue;

            switch (localName) {
                case "documentation"    -> process.setDocumentation(child.getTextContent().trim());
                case "laneSet"          -> parseLaneSet(child, process);
                case "dataObject"       -> process.getDataObjects().add(parseDataObject(child));
                case "ioSpecification"  -> process.setIoSpecification(parseIoSpecification(child));
                default                 -> {
                    FlowElement fe = parseFlowElement(child);
                    if (fe != null) {
                        process.getFlowElements().add(fe);
                        if (fe instanceof SequenceFlow sf) {
                            process.getSequenceFlows().add(sf);
                        }
                    }
                }
            }
        }
        return process;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Flow Element Dispatcher
    // ──────────────────────────────────────────────────────────────────────────

    private FlowElement parseFlowElement(Element el) {
        String localName = el.getLocalName();
        if (localName == null) return null;

        return switch (localName) {
            // Events
            case "startEvent"              -> parseStartEvent(el);
            case "endEvent"                -> parseEndEvent(el);
            case "intermediateCatchEvent"  -> parseIntermediateCatchEvent(el);
            case "intermediateThrowEvent"  -> parseIntermediateThrowEvent(el);
            case "boundaryEvent"           -> parseBoundaryEvent(el);
            // Tasks
            case "task"                    -> parseTask(el);
            case "userTask"                -> parseUserTask(el);
            case "serviceTask"             -> parseServiceTask(el);
            case "scriptTask"              -> parseScriptTask(el);
            case "businessRuleTask"        -> parseBusinessRuleTask(el);
            case "manualTask"              -> parseManualTask(el);
            case "receiveTask"             -> parseReceiveTask(el);
            case "sendTask"                -> parseSendTask(el);
            case "callActivity"            -> parseCallActivity(el);
            case "subProcess"              -> parseSubProcess(el);
            case "transaction"             -> parseSubProcess(el);   // treated as subProcess
            case "adHocSubProcess"         -> parseSubProcess(el);
            // Gateways
            case "exclusiveGateway"        -> parseExclusiveGateway(el);
            case "inclusiveGateway"        -> parseInclusiveGateway(el);
            case "parallelGateway"         -> parseParallelGateway(el);
            case "eventBasedGateway"       -> parseEventBasedGateway(el);
            case "complexGateway"          -> parseComplexGateway(el);
            // Flow
            case "sequenceFlow"            -> parseSequenceFlow(el);
            default                        -> { log.trace("Unhandled flow element: {}", localName); yield null; }
        };
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers – common attributes
    // ──────────────────────────────────────────────────────────────────────────

    private void applyBaseAttributes(FlowElement fe, Element el) {
        fe.setId(el.getAttribute("id"));
        fe.setName(el.getAttribute("name"));
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (!(n instanceof Element child)) continue;
            String ln = child.getLocalName();
            if ("documentation".equals(ln)) {
                fe.setDocumentation(child.getTextContent().trim());
            } else if ("extensionElements".equals(ln)) {
                fe.setExtensionElements(parseExtensionElements(child));
            } else if ("incoming".equals(ln)) {
                fe.getIncomingFlows().add(child.getTextContent().trim());
            } else if ("outgoing".equals(ln)) {
                fe.getOutgoingFlows().add(child.getTextContent().trim());
            }
        }
    }

    private void applyActivityAttributes(Activity act, Element el) {
        applyBaseAttributes(act, el);
        String async = getExtAttr(el, "async");
        if ("true".equals(async)) act.setAsynchronous(true);
        String exclusive = getExtAttr(el, "exclusive");
        if ("false".equals(exclusive)) act.setExclusive(false);
        act.setForCompensation("true".equals(el.getAttribute("isForCompensation")));

        // Loop characteristics
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (!(n instanceof Element child)) continue;
            String ln = child.getLocalName();
            if ("multiInstanceLoopCharacteristics".equals(ln)) {
                act.setMultiInstanceLoopCharacteristics(parseMultiInstance(child));
            } else if ("standardLoopCharacteristics".equals(ln)) {
                act.setStandardLoopCharacteristics(parseStandardLoop(child));
            } else if ("dataInputAssociation".equals(ln)) {
                act.getDataInputAssociations().add(parseDataAssociation(child));
            } else if ("dataOutputAssociation".equals(ln)) {
                act.getDataOutputAssociations().add(parseDataAssociation(child));
            } else if ("ioSpecification".equals(ln)) {
                act.setIoSpecification(parseIoSpecification(child));
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Events
    // ──────────────────────────────────────────────────────────────────────────

    private StartEvent parseStartEvent(Element el) {
        StartEvent e = new StartEvent();
        applyBaseAttributes(e, el);
        e.setInterrupting(!"false".equals(el.getAttribute("isInterrupting")));
        parseEventDefinitions(el, e);
        return e;
    }

    private EndEvent parseEndEvent(Element el) {
        EndEvent e = new EndEvent();
        applyBaseAttributes(e, el);
        parseEventDefinitions(el, e);
        if (e.hasEventDefinition(TerminateEventDefinition.class)) e.setTerminate(true);
        return e;
    }

    private IntermediateCatchEvent parseIntermediateCatchEvent(Element el) {
        IntermediateCatchEvent e = new IntermediateCatchEvent();
        applyBaseAttributes(e, el);
        parseEventDefinitions(el, e);
        return e;
    }

    private IntermediateThrowEvent parseIntermediateThrowEvent(Element el) {
        IntermediateThrowEvent e = new IntermediateThrowEvent();
        applyBaseAttributes(e, el);
        parseEventDefinitions(el, e);
        return e;
    }

    private BoundaryEvent parseBoundaryEvent(Element el) {
        BoundaryEvent e = new BoundaryEvent();
        applyBaseAttributes(e, el);
        e.setAttachedToRef(el.getAttribute("attachedToRef"));
        e.setCancelActivity(!"false".equals(el.getAttribute("cancelActivity")));
        e.setInterrupting(e.isCancelActivity());
        parseEventDefinitions(el, e);
        return e;
    }

    private void parseEventDefinitions(Element el, Event event) {
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (!(n instanceof Element child)) continue;
            EventDefinition ed = parseEventDefinition(child);
            if (ed != null) event.getEventDefinitions().add(ed);
        }
    }

    private EventDefinition parseEventDefinition(Element el) {
        String ln = el.getLocalName();
        if (ln == null) return null;
        return switch (ln) {
            case "timerEventDefinition"       -> parseTimerEventDefinition(el);
            case "messageEventDefinition"     -> parseMessageEventDefinition(el);
            case "signalEventDefinition"      -> parseSignalEventDefinition(el);
            case "errorEventDefinition"       -> parseErrorEventDefinition(el);
            case "escalationEventDefinition"  -> parseEscalationEventDefinition(el);
            case "compensateEventDefinition"  -> parseCompensateEventDefinition(el);
            case "conditionalEventDefinition" -> parseConditionalEventDefinition(el);
            case "terminateEventDefinition"   -> parseTerminateEventDefinition(el);
            default                           -> null;
        };
    }

    private TimerEventDefinition parseTimerEventDefinition(Element el) {
        TimerEventDefinition ted = new TimerEventDefinition();
        ted.setId(el.getAttribute("id"));
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (!(n instanceof Element child)) continue;
            String ln = child.getLocalName();
            if (ln == null) continue;
            switch (ln) {
                case "timeDate"     -> ted.setTimeDate(child.getTextContent().trim());
                case "timeDuration" -> ted.setTimeDuration(child.getTextContent().trim());
                case "timeCycle"    -> ted.setTimeCycle(child.getTextContent().trim());
            }
        }
        return ted;
    }

    private MessageEventDefinition parseMessageEventDefinition(Element el) {
        MessageEventDefinition med = new MessageEventDefinition();
        med.setId(el.getAttribute("id"));
        med.setMessageRef(el.getAttribute("messageRef"));
        return med;
    }

    private SignalEventDefinition parseSignalEventDefinition(Element el) {
        SignalEventDefinition sed = new SignalEventDefinition();
        sed.setId(el.getAttribute("id"));
        sed.setSignalRef(el.getAttribute("signalRef"));
        return sed;
    }

    private ErrorEventDefinition parseErrorEventDefinition(Element el) {
        ErrorEventDefinition eed = new ErrorEventDefinition();
        eed.setId(el.getAttribute("id"));
        eed.setErrorRef(el.getAttribute("errorRef"));
        eed.setErrorCodeVariable(getExtAttr(el, "errorCodeVariable"));
        eed.setErrorMessageVariable(getExtAttr(el, "errorMessageVariable"));
        return eed;
    }

    private EscalationEventDefinition parseEscalationEventDefinition(Element el) {
        EscalationEventDefinition eed = new EscalationEventDefinition();
        eed.setId(el.getAttribute("id"));
        eed.setEscalationRef(el.getAttribute("escalationRef"));
        return eed;
    }

    private CompensateEventDefinition parseCompensateEventDefinition(Element el) {
        CompensateEventDefinition ced = new CompensateEventDefinition();
        ced.setId(el.getAttribute("id"));
        ced.setActivityRef(el.getAttribute("activityRef"));
        ced.setWaitForCompletion(!"false".equals(el.getAttribute("waitForCompletion")));
        return ced;
    }

    private ConditionalEventDefinition parseConditionalEventDefinition(Element el) {
        ConditionalEventDefinition ced = new ConditionalEventDefinition();
        ced.setId(el.getAttribute("id"));
        Element condEl = getFirstChildByLocalName(el, "condition");
        if (condEl != null) ced.setCondition(condEl.getTextContent().trim());
        return ced;
    }

    private TerminateEventDefinition parseTerminateEventDefinition(Element el) {
        TerminateEventDefinition ted = new TerminateEventDefinition();
        ted.setId(el.getAttribute("id"));
        ted.setTerminateAll("true".equals(el.getAttribute("terminateAll")));
        return ted;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tasks
    // ──────────────────────────────────────────────────────────────────────────

    private TaskElement parseTask(Element el) {
        TaskElement t = new TaskElement();
        applyActivityAttributes(t, el);
        return t;
    }

    private UserTask parseUserTask(Element el) {
        UserTask t = new UserTask();
        applyActivityAttributes(t, el);
        t.setAssignee(getExtAttr(el, "assignee"));
        t.setOwner(getExtAttr(el, "owner"));
        t.setFormKey(getExtAttr(el, "formKey"));
        t.setDueDateExpression(getExtAttr(el, "dueDate"));
        t.setFollowUpDateExpression(getExtAttr(el, "followUpDate"));
        String priority = getExtAttr(el, "priority");
        if (!priority.isBlank()) {
            try { t.setPriority(Integer.parseInt(priority)); } catch (NumberFormatException ignored) {}
        }

        // candidateUsers / candidateGroups: first try as direct extension attributes
        // (camunda:candidateUsers="..." style), then fall back to extensionElements properties
        String directCu = getExtAttr(el, "candidateUsers");
        if (!directCu.isBlank()) {
            List.of(directCu.split(",")).stream().map(String::trim).forEach(t.getCandidateUsers()::add);
        }
        String directCg = getExtAttr(el, "candidateGroups");
        if (!directCg.isBlank()) {
            List.of(directCg.split(",")).stream().map(String::trim).forEach(t.getCandidateGroups()::add);
        }
        if (t.getExtensionElements() != null) {
            String cu = t.getExtensionElements().getProperties().get("candidateUsers");
            if (cu != null) List.of(cu.split(",")).stream().map(String::trim).forEach(t.getCandidateUsers()::add);
            String cg = t.getExtensionElements().getProperties().get("candidateGroups");
            if (cg != null) List.of(cg.split(",")).stream().map(String::trim).forEach(t.getCandidateGroups()::add);
        }
        return t;
    }

    private ServiceTask parseServiceTask(Element el) {
        ServiceTask t = new ServiceTask();
        applyActivityAttributes(t, el);
        t.setImplementation(getExtAttr(el, "class"));
        t.setExpression(getExtAttr(el, "expression"));
        t.setDelegateExpression(getExtAttr(el, "delegateExpression"));
        t.setResultVariable(getExtAttr(el, "resultVariable"));
        t.setTopic(getExtAttr(el, "topic"));
        return t;
    }

    private ScriptTask parseScriptTask(Element el) {
        ScriptTask t = new ScriptTask();
        applyActivityAttributes(t, el);
        t.setScriptFormat(el.getAttribute("scriptFormat"));
        t.setResultVariable(getExtAttr(el, "resultVariable"));
        Element scriptEl = getFirstChildByLocalName(el, "script");
        if (scriptEl != null) t.setScript(scriptEl.getTextContent());
        return t;
    }

    private BusinessRuleTask parseBusinessRuleTask(Element el) {
        BusinessRuleTask t = new BusinessRuleTask();
        applyActivityAttributes(t, el);
        t.setDecisionRef(getExtAttr(el, "decisionRef"));
        t.setResultVariable(getExtAttr(el, "resultVariable"));
        return t;
    }

    private ManualTask parseManualTask(Element el) {
        ManualTask t = new ManualTask();
        applyActivityAttributes(t, el);
        return t;
    }

    private ReceiveTask parseReceiveTask(Element el) {
        ReceiveTask t = new ReceiveTask();
        applyActivityAttributes(t, el);
        t.setMessageRef(el.getAttribute("messageRef"));
        t.setInstantiate("true".equals(el.getAttribute("instantiate")));
        return t;
    }

    private SendTask parseSendTask(Element el) {
        SendTask t = new SendTask();
        applyActivityAttributes(t, el);
        t.setMessageRef(el.getAttribute("messageRef"));
        t.setImplementation(el.getAttribute("implementation"));
        return t;
    }

    private CallActivity parseCallActivity(Element el) {
        CallActivity ca = new CallActivity();
        applyActivityAttributes(ca, el);
        ca.setCalledElement(el.getAttribute("calledElement"));
        ca.setInheritVariables("true".equals(getExtAttr(el, "inheritVariables")));
        return ca;
    }

    private SubProcess parseSubProcess(Element el) {
        SubProcess sp = new SubProcess();
        applyActivityAttributes(sp, el);
        sp.setTriggeredByEvent("true".equals(el.getAttribute("triggeredByEvent")));

        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (!(n instanceof Element child)) continue;
            FlowElement fe = parseFlowElement(child);
            if (fe != null) {
                sp.getFlowElements().add(fe);
                if (fe instanceof SequenceFlow sf) sp.getSequenceFlows().add(sf);
            }
        }
        return sp;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Gateways
    // ──────────────────────────────────────────────────────────────────────────

    private ExclusiveGateway parseExclusiveGateway(Element el) {
        ExclusiveGateway gw = new ExclusiveGateway();
        applyBaseAttributes(gw, el);
        gw.setDefaultFlow(el.getAttribute("default"));
        return gw;
    }

    private InclusiveGateway parseInclusiveGateway(Element el) {
        InclusiveGateway gw = new InclusiveGateway();
        applyBaseAttributes(gw, el);
        gw.setDefaultFlow(el.getAttribute("default"));
        return gw;
    }

    private ParallelGateway parseParallelGateway(Element el) {
        ParallelGateway gw = new ParallelGateway();
        applyBaseAttributes(gw, el);
        return gw;
    }

    private EventBasedGateway parseEventBasedGateway(Element el) {
        EventBasedGateway gw = new EventBasedGateway();
        applyBaseAttributes(gw, el);
        gw.setInstantiate("true".equals(el.getAttribute("instantiate")));
        return gw;
    }

    private ComplexGateway parseComplexGateway(Element el) {
        ComplexGateway gw = new ComplexGateway();
        applyBaseAttributes(gw, el);
        gw.setDefaultFlow(el.getAttribute("default"));
        Element cond = getFirstChildByLocalName(el, "activationCondition");
        if (cond != null) gw.setActivationCondition(cond.getTextContent().trim());
        return gw;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Sequence Flow
    // ──────────────────────────────────────────────────────────────────────────

    private SequenceFlow parseSequenceFlow(Element el) {
        SequenceFlow sf = new SequenceFlow();
        applyBaseAttributes(sf, el);
        sf.setSourceRef(el.getAttribute("sourceRef"));
        sf.setTargetRef(el.getAttribute("targetRef"));
        sf.setDefault("true".equals(el.getAttribute("isDefault")));

        Element condEl = getFirstChildByLocalName(el, "conditionExpression");
        if (condEl != null) {
            sf.setConditionExpression(condEl.getTextContent().trim());
            String lang = condEl.getAttribute("language");
            if (!lang.isBlank()) sf.setConditionLanguage(lang);
        }
        return sf;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Extension Elements
    // ──────────────────────────────────────────────────────────────────────────

    private ExtensionElements parseExtensionElements(Element el) {
        ExtensionElements ext = new ExtensionElements();
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (!(n instanceof Element child)) continue;
            String ln = child.getLocalName();
            if (ln == null) continue;
            switch (ln) {
                case "properties" -> parseProperties(child, ext);
                case "property"   -> {
                    String k = child.getAttribute("name");
                    String v = child.getAttribute("value");
                    ext.getProperties().put(k, v);
                }
                case "executionListener" -> ext.getExecutionListeners().add(parseExecutionListener(child));
                case "taskListener"      -> ext.getTaskListeners().add(parseTaskListener(child));
            }
        }
        return ext;
    }

    private void parseProperties(Element el, ExtensionElements ext) {
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (!(n instanceof Element child)) continue;
            if ("property".equals(child.getLocalName())) {
                ext.getProperties().put(child.getAttribute("name"), child.getAttribute("value"));
            }
        }
    }

    private ExecutionListener parseExecutionListener(Element el) {
        ExecutionListener listener = new ExecutionListener();
        String event = el.getAttribute("event");
        if (!event.isBlank()) listener.setEvent(ExecutionListener.Event.valueOf(event.toUpperCase()));
        listener.setImplementation(el.getAttribute("class"));
        listener.setExpression(el.getAttribute("expression"));
        listener.setDelegateExpression(el.getAttribute("delegateExpression"));
        return listener;
    }

    private TaskListener parseTaskListener(Element el) {
        TaskListener listener = new TaskListener();
        String event = el.getAttribute("event");
        if (!event.isBlank()) {
            try { listener.setEvent(TaskListener.Event.valueOf(event.toUpperCase())); }
            catch (IllegalArgumentException e) { listener.setEvent(TaskListener.Event.ALL); }
        }
        listener.setImplementation(el.getAttribute("class"));
        listener.setExpression(el.getAttribute("expression"));
        listener.setDelegateExpression(el.getAttribute("delegateExpression"));
        return listener;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Loop Characteristics
    // ──────────────────────────────────────────────────────────────────────────

    private MultiInstanceLoopCharacteristics parseMultiInstance(Element el) {
        MultiInstanceLoopCharacteristics mi = new MultiInstanceLoopCharacteristics();
        mi.setSequential("true".equals(el.getAttribute("isSequential")));
        mi.setElementVariable(getExtAttr(el, "elementVariable"));
        mi.setCollectionExpression(getExtAttr(el, "collection"));
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (!(n instanceof Element child)) continue;
            String ln = child.getLocalName();
            if (ln == null) continue;
            switch (ln) {
                case "loopCardinality"    -> mi.setLoopCardinality(child.getTextContent().trim());
                case "completionCondition"-> mi.setCompletionCondition(child.getTextContent().trim());
                case "inputDataItem"      -> mi.setInputDataItem(child.getAttribute("name"));
            }
        }
        return mi;
    }

    private StandardLoopCharacteristics parseStandardLoop(Element el) {
        StandardLoopCharacteristics sl = new StandardLoopCharacteristics();
        sl.setTestBefore("true".equals(el.getAttribute("testBefore")));
        String max = el.getAttribute("loopMaximum");
        if (!max.isBlank()) { try { sl.setLoopMaximum(Integer.parseInt(max)); } catch (NumberFormatException ignored) {} }
        Element condEl = getFirstChildByLocalName(el, "loopCondition");
        if (condEl != null) sl.setLoopCondition(condEl.getTextContent().trim());
        return sl;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Data / IO
    // ──────────────────────────────────────────────────────────────────────────

    private DataObject parseDataObject(Element el) {
        DataObject obj = new DataObject();
        obj.setId(el.getAttribute("id"));
        obj.setName(el.getAttribute("name"));
        obj.setItemSubjectRef(el.getAttribute("itemSubjectRef"));
        obj.setCollection("true".equals(el.getAttribute("isCollection")));
        return obj;
    }

    private DataAssociation parseDataAssociation(Element el) {
        DataAssociation da = new DataAssociation();
        da.setId(el.getAttribute("id"));
        da.setTargetRef(getTextOf(el, "targetRef"));
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (!(n instanceof Element child)) continue;
            if ("sourceRef".equals(child.getLocalName())) da.getSourceRefs().add(child.getTextContent().trim());
            if ("transformation".equals(child.getLocalName())) da.setTransformation(child.getTextContent().trim());
        }
        return da;
    }

    private IoSpecification parseIoSpecification(Element el) {
        IoSpecification io = new IoSpecification();
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (!(n instanceof Element child)) continue;
            String ln = child.getLocalName();
            if (ln == null) continue;
            switch (ln) {
                case "dataInput"  -> { DataInput di = new DataInput();  di.setId(child.getAttribute("id")); di.setName(child.getAttribute("name")); io.getDataInputs().add(di); }
                case "dataOutput" -> { DataOutput dout = new DataOutput(); dout.setId(child.getAttribute("id")); dout.setName(child.getAttribute("name")); io.getDataOutputs().add(dout); }
            }
        }
        return io;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Lane Set
    // ──────────────────────────────────────────────────────────────────────────

    private void parseLaneSet(Element el, Process process) {
        NodeList children = el.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (!(n instanceof Element child) || !"lane".equals(child.getLocalName())) continue;
            Lane lane = new Lane();
            lane.setId(child.getAttribute("id"));
            lane.setName(child.getAttribute("name"));
            NodeList laneChildren = child.getChildNodes();
            for (int j = 0; j < laneChildren.getLength(); j++) {
                Node ln = laneChildren.item(j);
                if (ln instanceof Element lEl && "flowNodeRef".equals(lEl.getLocalName())) {
                    lane.getFlowNodeRefs().add(lEl.getTextContent().trim());
                }
            }
            process.getLanes().add(lane);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Global elements
    // ──────────────────────────────────────────────────────────────────────────

    private Message parseMessage(Element el) {
        Message m = new Message();
        m.setId(el.getAttribute("id"));
        m.setName(el.getAttribute("name"));
        m.setItemRef(el.getAttribute("itemRef"));
        return m;
    }

    private Signal parseSignal(Element el) {
        Signal s = new Signal();
        s.setId(el.getAttribute("id"));
        s.setName(el.getAttribute("name"));
        return s;
    }

    private BpmnError parseError(Element el) {
        BpmnError e = new BpmnError();
        e.setId(el.getAttribute("id"));
        e.setName(el.getAttribute("name"));
        e.setErrorCode(el.getAttribute("errorCode"));
        return e;
    }

    private Escalation parseEscalation(Element el) {
        Escalation e = new Escalation();
        e.setId(el.getAttribute("id"));
        e.setName(el.getAttribute("name"));
        e.setEscalationCode(el.getAttribute("escalationCode"));
        return e;
    }

    private ItemDefinition parseItemDefinition(Element el) {
        ItemDefinition id = new ItemDefinition();
        id.setId(el.getAttribute("id"));
        id.setStructureRef(el.getAttribute("structureRef"));
        id.setCollection("true".equals(el.getAttribute("isCollection")));
        return id;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Utilities
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * Returns the value of a namespace-extended attribute, checking both the
     * Activiti namespace and the Camunda namespace so that BPMN files exported
     * by either modeller are handled transparently.
     */
    private String getExtAttr(Element el, String localName) {
        String v = el.getAttributeNS(NS_EXT, localName);
        if (v == null || v.isBlank()) {
            v = el.getAttributeNS(NS_CAMUNDA, localName);
        }
        return v != null ? v : "";
    }

    private Element getFirstChildByLocalName(Element parent, String localName) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node n = children.item(i);
            if (n instanceof Element el && localName.equals(el.getLocalName())) return el;
        }
        return null;
    }

    private String getTextOf(Element parent, String localName) {
        Element child = getFirstChildByLocalName(parent, localName);
        return child != null ? child.getTextContent().trim() : null;
    }
}

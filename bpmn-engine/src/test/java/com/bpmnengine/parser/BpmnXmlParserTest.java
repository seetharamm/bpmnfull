package com.bpmnengine.parser;

import com.bpmnengine.model.*;
import com.bpmnengine.model.Process;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class BpmnXmlParserTest {

    private BpmnXmlParser parser;

    @BeforeEach
    void setUp() { parser = new BpmnXmlParser(); }

    // ─── Minimal valid BPMN ──────────────────────────────────────────────────

    static final String MINIMAL_BPMN = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                     targetNamespace="http://test"
                     id="def1">
          <process id="myProcess" name="My Process" isExecutable="true">
            <startEvent id="start" name="Start"/>
            <sequenceFlow id="sf1" sourceRef="start" targetRef="end"/>
            <endEvent id="end" name="End"/>
          </process>
        </definitions>
        """;

    @Test
    void parsesMinimalBpmn() {
        BpmnModel model = parser.parse(MINIMAL_BPMN);
        assertThat(model.getProcesses()).hasSize(1);
        Process process = model.getProcesses().get(0);
        assertThat(process.getId()).isEqualTo("myProcess");
        assertThat(process.isExecutable()).isTrue();
        assertThat(process.getFlowElements()).hasSize(3); // start, sf, end
        assertThat(process.getSequenceFlows()).hasSize(1);
    }

    // ─── All gateway types ───────────────────────────────────────────────────

    static final String GATEWAY_BPMN = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                     targetNamespace="http://test">
          <process id="gateways" isExecutable="true">
            <startEvent id="start"/>
            <exclusiveGateway id="xor" default="sf2"/>
            <parallelGateway id="and"/>
            <inclusiveGateway id="or"/>
            <eventBasedGateway id="ebg"/>
            <complexGateway id="complex"/>
            <endEvent id="end"/>
            <sequenceFlow id="sf1" sourceRef="start" targetRef="xor"/>
            <sequenceFlow id="sf2" sourceRef="xor" targetRef="and">
              <conditionExpression>${amount > 100}</conditionExpression>
            </sequenceFlow>
            <sequenceFlow id="sf3" sourceRef="and" targetRef="end"/>
          </process>
        </definitions>
        """;

    @Test
    void parsesAllGatewayTypes() {
        BpmnModel model = parser.parse(GATEWAY_BPMN);
        Process proc = model.getProcesses().get(0);

        assertThat(proc.getFlowElement("xor")).isInstanceOf(ExclusiveGateway.class);
        assertThat(((ExclusiveGateway) proc.getFlowElement("xor")).getDefaultFlow()).isEqualTo("sf2");
        assertThat(proc.getFlowElement("and")).isInstanceOf(ParallelGateway.class);
        assertThat(proc.getFlowElement("or")).isInstanceOf(InclusiveGateway.class);
        assertThat(proc.getFlowElement("ebg")).isInstanceOf(EventBasedGateway.class);
        assertThat(proc.getFlowElement("complex")).isInstanceOf(ComplexGateway.class);

        SequenceFlow sf2 = (SequenceFlow) proc.getFlowElement("sf2");
        assertThat(sf2.getConditionExpression()).isEqualTo("${amount > 100}");
    }

    // ─── Timer events ────────────────────────────────────────────────────────

    static final String TIMER_BPMN = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                     targetNamespace="http://test">
          <process id="timers" isExecutable="true">
            <startEvent id="timerStart">
              <timerEventDefinition>
                <timeCycle>R3/PT1H</timeCycle>
              </timerEventDefinition>
            </startEvent>
            <intermediateCatchEvent id="wait">
              <timerEventDefinition>
                <timeDuration>PT30M</timeDuration>
              </timerEventDefinition>
            </intermediateCatchEvent>
            <endEvent id="end">
              <terminateEventDefinition/>
            </endEvent>
            <sequenceFlow id="sf1" sourceRef="timerStart" targetRef="wait"/>
            <sequenceFlow id="sf2" sourceRef="wait" targetRef="end"/>
          </process>
        </definitions>
        """;

    @Test
    void parsesTimerAndTerminateEvents() {
        BpmnModel model = parser.parse(TIMER_BPMN);
        Process proc = model.getProcesses().get(0);

        StartEvent timerStart = (StartEvent) proc.getFlowElement("timerStart");
        assertThat(timerStart.hasEventDefinition(TimerEventDefinition.class)).isTrue();
        TimerEventDefinition ted = (TimerEventDefinition) timerStart.getFirstEventDefinition();
        assertThat(ted.getTimeCycle()).isEqualTo("R3/PT1H");

        IntermediateCatchEvent ice = (IntermediateCatchEvent) proc.getFlowElement("wait");
        TimerEventDefinition iceTimer = (TimerEventDefinition) ice.getFirstEventDefinition();
        assertThat(iceTimer.getTimeDuration()).isEqualTo("PT30M");

        EndEvent end = (EndEvent) proc.getFlowElement("end");
        assertThat(end.hasEventDefinition(TerminateEventDefinition.class)).isTrue();
    }

    // ─── Message & Signal events ─────────────────────────────────────────────

    static final String MSG_SIG_BPMN = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                     xmlns:activiti="http://activiti.org/bpmn"
                     targetNamespace="http://test">
          <message id="msg1" name="OrderReceived"/>
          <signal  id="sig1" name="CancelOrder"/>
          <process id="events" isExecutable="true">
            <startEvent id="msgStart">
              <messageEventDefinition messageRef="msg1"/>
            </startEvent>
            <intermediateCatchEvent id="sigCatch">
              <signalEventDefinition signalRef="sig1"/>
            </intermediateCatchEvent>
            <endEvent id="errorEnd">
              <errorEventDefinition errorRef="err1" activiti:errorCodeVariable="code"/>
            </endEvent>
            <sequenceFlow id="sf1" sourceRef="msgStart" targetRef="sigCatch"/>
            <sequenceFlow id="sf2" sourceRef="sigCatch" targetRef="errorEnd"/>
          </process>
        </definitions>
        """;

    @Test
    void parsesMessageAndSignalEvents() {
        BpmnModel model = parser.parse(MSG_SIG_BPMN);
        assertThat(model.getMessages()).hasSize(1);
        assertThat(model.getSignals()).hasSize(1);

        Process proc = model.getProcesses().get(0);
        StartEvent msgStart = (StartEvent) proc.getFlowElement("msgStart");
        assertThat(msgStart.hasEventDefinition(MessageEventDefinition.class)).isTrue();

        IntermediateCatchEvent sigCatch = (IntermediateCatchEvent) proc.getFlowElement("sigCatch");
        assertThat(sigCatch.hasEventDefinition(SignalEventDefinition.class)).isTrue();
    }

    // ─── Task types ──────────────────────────────────────────────────────────

    static final String TASKS_BPMN = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                     xmlns:activiti="http://activiti.org/bpmn"
                     targetNamespace="http://test">
          <process id="tasks" isExecutable="true">
            <startEvent id="start"/>
            <userTask id="ut1" name="Approve" activiti:assignee="john" activiti:formKey="approval-form"/>
            <serviceTask id="st1" activiti:class="com.example.MyDelegate"/>
            <scriptTask id="sc1" scriptFormat="javascript">
              <script>var x = 1 + 1;</script>
            </scriptTask>
            <businessRuleTask id="brt1" activiti:decisionRef="approvalDmn"/>
            <receiveTask id="rt1" messageRef="msg1"/>
            <callActivity id="ca1" calledElement="SubProcess"/>
            <endEvent id="end"/>
            <sequenceFlow id="sf1" sourceRef="start" targetRef="ut1"/>
            <sequenceFlow id="sf2" sourceRef="ut1" targetRef="st1"/>
            <sequenceFlow id="sf3" sourceRef="st1" targetRef="sc1"/>
            <sequenceFlow id="sf4" sourceRef="sc1" targetRef="brt1"/>
            <sequenceFlow id="sf5" sourceRef="brt1" targetRef="rt1"/>
            <sequenceFlow id="sf6" sourceRef="rt1" targetRef="ca1"/>
            <sequenceFlow id="sf7" sourceRef="ca1" targetRef="end"/>
          </process>
        </definitions>
        """;

    @Test
    void parsesAllTaskTypes() {
        BpmnModel model = parser.parse(TASKS_BPMN);
        Process proc = model.getProcesses().get(0);

        UserTask ut = (UserTask) proc.getFlowElement("ut1");
        assertThat(ut.getAssignee()).isEqualTo("john");
        assertThat(ut.getFormKey()).isEqualTo("approval-form");

        ServiceTask st = (ServiceTask) proc.getFlowElement("st1");
        assertThat(st.getImplementation()).isEqualTo("com.example.MyDelegate");

        ScriptTask sc = (ScriptTask) proc.getFlowElement("sc1");
        assertThat(sc.getScriptFormat()).isEqualTo("javascript");
        assertThat(sc.getScript()).contains("var x = 1 + 1");

        BusinessRuleTask brt = (BusinessRuleTask) proc.getFlowElement("brt1");
        assertThat(brt.getDecisionRef()).isEqualTo("approvalDmn");

        ReceiveTask rt = (ReceiveTask) proc.getFlowElement("rt1");
        assertThat(rt.getMessageRef()).isEqualTo("msg1");

        CallActivity ca = (CallActivity) proc.getFlowElement("ca1");
        assertThat(ca.getCalledElement()).isEqualTo("SubProcess");
    }

    // ─── Boundary events ─────────────────────────────────────────────────────

    static final String BOUNDARY_BPMN = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                     targetNamespace="http://test">
          <process id="boundary" isExecutable="true">
            <startEvent id="start"/>
            <userTask id="task1" name="Do Work"/>
            <boundaryEvent id="be1" attachedToRef="task1" cancelActivity="true">
              <timerEventDefinition>
                <timeDuration>PT1H</timeDuration>
              </timerEventDefinition>
            </boundaryEvent>
            <boundaryEvent id="be2" attachedToRef="task1" cancelActivity="false">
              <errorEventDefinition errorRef="err1"/>
            </boundaryEvent>
            <endEvent id="end"/>
            <sequenceFlow id="sf1" sourceRef="start" targetRef="task1"/>
            <sequenceFlow id="sf2" sourceRef="task1" targetRef="end"/>
            <sequenceFlow id="sf3" sourceRef="be1" targetRef="end"/>
          </process>
        </definitions>
        """;

    @Test
    void parsesBoundaryEvents() {
        BpmnModel model = parser.parse(BOUNDARY_BPMN);
        Process proc = model.getProcesses().get(0);

        BoundaryEvent be1 = (BoundaryEvent) proc.getFlowElement("be1");
        assertThat(be1.getAttachedToRef()).isEqualTo("task1");
        assertThat(be1.isCancelActivity()).isTrue();
        assertThat(be1.hasEventDefinition(TimerEventDefinition.class)).isTrue();

        BoundaryEvent be2 = (BoundaryEvent) proc.getFlowElement("be2");
        assertThat(be2.isCancelActivity()).isFalse();
        assertThat(be2.hasEventDefinition(ErrorEventDefinition.class)).isTrue();
    }

    // ─── Multi-instance ──────────────────────────────────────────────────────

    static final String MULTI_INSTANCE_BPMN = """
        <?xml version="1.0" encoding="UTF-8"?>
        <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                     xmlns:activiti="http://activiti.org/bpmn"
                     targetNamespace="http://test">
          <process id="mi" isExecutable="true">
            <startEvent id="start"/>
            <userTask id="miTask" name="Review">
              <multiInstanceLoopCharacteristics isSequential="false"
                    activiti:collection="reviewers"
                    activiti:elementVariable="reviewer">
                <completionCondition>${nrOfCompletedInstances >= 3}</completionCondition>
              </multiInstanceLoopCharacteristics>
            </userTask>
            <endEvent id="end"/>
            <sequenceFlow id="sf1" sourceRef="start" targetRef="miTask"/>
            <sequenceFlow id="sf2" sourceRef="miTask" targetRef="end"/>
          </process>
        </definitions>
        """;

    @Test
    void parsesMultiInstanceTask() {
        BpmnModel model = parser.parse(MULTI_INSTANCE_BPMN);
        Process proc = model.getProcesses().get(0);
        UserTask task = (UserTask) proc.getFlowElement("miTask");
        MultiInstanceLoopCharacteristics mi = task.getMultiInstanceLoopCharacteristics();
        assertThat(mi).isNotNull();
        assertThat(mi.isSequential()).isFalse();
        assertThat(mi.getCollectionExpression()).isEqualTo("reviewers");
        assertThat(mi.getElementVariable()).isEqualTo("reviewer");
        assertThat(mi.getCompletionCondition()).contains("nrOfCompletedInstances");
    }

    // ─── SubProcess ──────────────────────────────────────────────────────────

    @Test
    void parsesSubProcess() {
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
                         targetNamespace="http://test">
              <process id="main" isExecutable="true">
                <startEvent id="start"/>
                <subProcess id="sub" name="My SubProcess">
                  <startEvent id="subStart"/>
                  <endEvent id="subEnd"/>
                  <sequenceFlow id="innerSf" sourceRef="subStart" targetRef="subEnd"/>
                </subProcess>
                <endEvent id="end"/>
                <sequenceFlow id="sf1" sourceRef="start" targetRef="sub"/>
                <sequenceFlow id="sf2" sourceRef="sub" targetRef="end"/>
              </process>
            </definitions>
            """;
        BpmnModel model = parser.parse(xml);
        Process proc = model.getProcesses().get(0);
        SubProcess sp = (SubProcess) proc.getFlowElement("sub");
        assertThat(sp).isNotNull();
        assertThat(sp.getFlowElements()).hasSize(3);
        assertThat(sp.getSequenceFlows()).hasSize(1);
    }
}

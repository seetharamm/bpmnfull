package com.bpmnengine.engine;

import com.bpmnengine.engine.handler.*;
import com.bpmnengine.engine.expression.JuelExpressionEvaluator;
import com.bpmnengine.model.*;
import com.bpmnengine.model.Process;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.List;

class ExclusiveGatewayHandlerTest {

    private final JuelExpressionEvaluator evaluator = new JuelExpressionEvaluator();
    private final ExclusiveGatewayHandler handler = new ExclusiveGatewayHandler(evaluator);

    @Test
    void takesFirstTrueCondition() {
        // Build a simple process model with an XOR gateway and two conditional flows
        ExclusiveGateway gw = new ExclusiveGateway();
        gw.setId("xor1");

        SequenceFlow sf1 = new SequenceFlow();
        sf1.setId("sf1"); sf1.setSourceRef("xor1"); sf1.setTargetRef("taskA");
        sf1.setConditionExpression("${score >= 80}");

        SequenceFlow sf2 = new SequenceFlow();
        sf2.setId("sf2"); sf2.setSourceRef("xor1"); sf2.setTargetRef("taskB");
        sf2.setConditionExpression("${score < 80}");

        Process process = buildProcess(gw, sf1, sf2);
        ExecutionContext ctx = buildCtx(process);
        ctx.setVariable("score", 90);
        ctx.setCurrentFlowElement(gw);

        ExecutionOutcome outcome = handler.execute(ctx, gw);

        assertThat(outcome.isWait()).isFalse();
        assertThat(outcome.isEnd()).isFalse();
        assertThat(outcome.getOutgoingFlows()).hasSize(1);
        assertThat(outcome.getOutgoingFlows().get(0).getId()).isEqualTo("sf1");
    }

    @Test
    void fallsBackToDefaultFlow() {
        ExclusiveGateway gw = new ExclusiveGateway();
        gw.setId("xor2");
        gw.setDefaultFlow("sfDefault");

        SequenceFlow sfCond = new SequenceFlow();
        sfCond.setId("sfCond"); sfCond.setSourceRef("xor2"); sfCond.setTargetRef("taskA");
        sfCond.setConditionExpression("${amount > 9999}");

        SequenceFlow sfDefault = new SequenceFlow();
        sfDefault.setId("sfDefault"); sfDefault.setSourceRef("xor2"); sfDefault.setTargetRef("taskB");

        Process process = buildProcess(gw, sfCond, sfDefault);
        ExecutionContext ctx = buildCtx(process);
        ctx.setVariable("amount", 100);
        ctx.setCurrentFlowElement(gw);

        ExecutionOutcome outcome = handler.execute(ctx, gw);
        assertThat(outcome.getOutgoingFlows().get(0).getId()).isEqualTo("sfDefault");
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Process buildProcess(ExclusiveGateway gw, SequenceFlow... flows) {
        Process p = new Process();
        p.setId("p1");
        p.getFlowElements().add(gw);
        for (SequenceFlow sf : flows) {
            p.getFlowElements().add(sf);
            p.getSequenceFlows().add(sf);
        }
        return p;
    }

    private ExecutionContext buildCtx(Process process) {
        ExecutionContext ctx = new ExecutionContext("exec1", "pi1", "def1", "p1");
        ctx.setProcess(process);
        ctx.setBpmnModel(new BpmnModel());
        return ctx;
    }
}

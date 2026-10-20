package com.bpmnengine.engine.handler;

import com.bpmnengine.engine.ExecutionContext;
import com.bpmnengine.engine.ExecutionOutcome;
import com.bpmnengine.engine.expression.ExpressionEvaluator;
import com.bpmnengine.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import javax.script.*;
import java.util.List;
import java.util.Map;

/**
 * Handles &lt;scriptTask&gt; by evaluating a script (JavaScript via Nashorn/GraalJS).
 */
@Component
public class ScriptTaskHandler implements FlowElementHandler {

    private static final Logger log = LoggerFactory.getLogger(ScriptTaskHandler.class);

    private final ExpressionEvaluator expressionEvaluator;

    public ScriptTaskHandler(ExpressionEvaluator expressionEvaluator) {
        this.expressionEvaluator = expressionEvaluator;
    }

    @Override
    public boolean supports(FlowElement element) {
        return element instanceof ScriptTask;
    }

    @Override
    public ExecutionOutcome execute(ExecutionContext ctx, FlowElement element) {
        ScriptTask scriptTask = (ScriptTask) element;
        String script = scriptTask.getScript();

        if (script != null && !script.isBlank()) {
            try {
                ScriptEngineManager manager = new ScriptEngineManager();
                ScriptEngine engine = manager.getEngineByName(scriptTask.getScriptFormat());

                if (engine == null) {
                    log.warn("Script engine '{}' not found; trying 'javascript'", scriptTask.getScriptFormat());
                    engine = manager.getEngineByName("javascript");
                }

                if (engine != null) {
                    Bindings bindings = engine.createBindings();
                    // Expose all process variables to the script
                    Map<String, Object> vars = ctx.getAllVariables();
                    if (vars != null) bindings.putAll(vars);
                    bindings.put("execution", ctx);

                    Object result = engine.eval(script, bindings);

                    if (scriptTask.getResultVariable() != null && result != null) {
                        ctx.setVariable(scriptTask.getResultVariable(), result);
                    }
                    // Write back modified variables
                    bindings.forEach((k, v) -> {
                        if (!k.equals("execution")) ctx.setVariable(k, v);
                    });
                } else {
                    log.error("No script engine available for format: {}", scriptTask.getScriptFormat());
                }
            } catch (ScriptException e) {
                throw new com.bpmnengine.engine.EngineException(
                        "Script execution failed in task '" + scriptTask.getId() + "': " + e.getMessage(), e);
            }
        }

        List<SequenceFlow> flows = ctx.getProcess().getOutgoingFlows(scriptTask.getId());
        return flows.isEmpty() ? ExecutionOutcome.endProcess() : ExecutionOutcome.continueOn(flows.get(0));
    }
}

package com.bpmnengine.engine.handler;

import com.bpmnengine.engine.EngineException;
import com.bpmnengine.model.FlowElement;
import org.springframework.stereotype.Component;
import java.util.List;

/**
 * Finds the appropriate {@link FlowElementHandler} for a given element.
 */
@Component
public class FlowElementHandlerRegistry {

    private final List<FlowElementHandler> handlers;

    public FlowElementHandlerRegistry(List<FlowElementHandler> handlers) {
        this.handlers = handlers;
    }

    public FlowElementHandler getHandler(FlowElement element) {
        return handlers.stream()
                .filter(h -> h.supports(element))
                .findFirst()
                .orElseThrow(() -> new EngineException(
                        "No handler registered for element: " + element.getElementType()
                        + " id=" + element.getId()));
    }
}

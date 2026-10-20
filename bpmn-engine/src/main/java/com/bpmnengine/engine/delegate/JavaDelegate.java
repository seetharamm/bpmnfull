package com.bpmnengine.engine.delegate;

import com.bpmnengine.engine.ExecutionContext;

/**
 * User-implemented interface for {@code &lt;serviceTask activiti:class="..."&gt;}.
 * Implement this in your application and set the class name on the serviceTask.
 */
@FunctionalInterface
public interface JavaDelegate {
    void execute(ExecutionContext execution) throws Exception;
}

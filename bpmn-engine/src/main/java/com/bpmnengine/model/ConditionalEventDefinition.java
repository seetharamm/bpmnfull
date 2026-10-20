package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** &lt;conditionalEventDefinition&gt; */
@Data @EqualsAndHashCode(callSuper = true)
public class ConditionalEventDefinition extends EventDefinition {
    private String condition;
    private String conditionLanguage = "juel";
}

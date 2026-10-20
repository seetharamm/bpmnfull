package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** &lt;messageEventDefinition&gt; */
@Data @EqualsAndHashCode(callSuper = true)
public class MessageEventDefinition extends EventDefinition {
    private String messageRef; // references a &lt;message&gt; id
    private String messageName;
    private String operationRef;
}

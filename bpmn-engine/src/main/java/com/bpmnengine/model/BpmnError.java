package com.bpmnengine.model;

import lombok.Data;

/** BPMN 2.0 &lt;error&gt; global element. */
@Data public class BpmnError { private String id; private String name; private String errorCode; private String itemSubjectRef; }

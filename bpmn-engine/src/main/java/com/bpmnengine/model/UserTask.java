package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.ArrayList;
import java.util.List;

/**
 * BPMN 2.0 &lt;userTask&gt;.
 */
@Data @EqualsAndHashCode(callSuper = true)
public class UserTask extends Activity {

    private String assignee;
    private String owner;
    private List<String> candidateUsers = new ArrayList<>();
    private List<String> candidateGroups = new ArrayList<>();
    private String dueDateExpression;
    private String followUpDateExpression;
    private int priority = 50;
    private String formKey;
    private String category;

    /** &lt;documentation&gt; used as task description. */
    private String taskDescription;

    public UserTask() { this.elementType = BpmnElementType.USER_TASK; }
}

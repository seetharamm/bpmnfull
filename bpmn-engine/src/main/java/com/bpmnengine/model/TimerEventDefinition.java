package com.bpmnengine.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

/** &lt;timerEventDefinition&gt; — supports ISO 8601 date, duration, and repeating intervals. */
@Data @EqualsAndHashCode(callSuper = true)
public class TimerEventDefinition extends EventDefinition {
    /** e.g. 2024-01-01T00:00:00Z — fires at an absolute time. */
    private String timeDate;
    /** e.g. PT1H — fires after a duration. */
    private String timeDuration;
    /** e.g. R3/PT1H — repeating cycle. */
    private String timeCycle;
}

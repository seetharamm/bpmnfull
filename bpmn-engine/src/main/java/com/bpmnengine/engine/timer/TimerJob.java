package com.bpmnengine.engine.timer;

import com.bpmnengine.engine.ProcessEngineImpl;
import com.bpmnengine.persistence.entity.JobEntity;
import com.bpmnengine.persistence.repository.JobRepository;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Quartz job that fires a BPMN timer event.
 */
@Component
public class TimerJob implements Job {

    private static final Logger log = LoggerFactory.getLogger(TimerJob.class);

    @Autowired private ProcessEngineImpl engine;
    @Autowired private JobRepository     jobRepo;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();
        String jobId = data.getString("bpmnJobId");
        if (jobId == null) {
            log.warn("TimerJob fired without bpmnJobId");
            return;
        }
        log.info("Timer fired for BPMN job: {}", jobId);
        try {
            engine.triggerTimerJob(jobId);
        } catch (Exception e) {
            log.error("Failed to trigger BPMN timer job {}", jobId, e);
            throw new JobExecutionException(e, false);
        }
    }
}

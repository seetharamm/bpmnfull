package com.bpmnengine.engine.timer;

import com.bpmnengine.persistence.entity.JobEntity;
import com.bpmnengine.persistence.repository.JobRepository;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.UUID;

/**
 * Schedules BPMN timer jobs via Quartz.
 *
 * Supports:
 *   date:2024-01-01T00:00:00Z          — fire at an ISO instant
 *   duration:PT1H                       — fire after ISO 8601 duration
 *   cycle:R3/PT1H or cycle:0/0/1 * * ? — repeating ISO interval or cron
 */
@Service
public class TimerScheduler {

    private static final Logger log = LoggerFactory.getLogger(TimerScheduler.class);

    private final Scheduler    scheduler;
    private final JobRepository jobRepo;

    public TimerScheduler(Scheduler scheduler, JobRepository jobRepo) {
        this.scheduler = scheduler;
        this.jobRepo   = jobRepo;
    }

    /**
     * Schedule a BPMN timer job.
     *
     * @param jobEntity the persisted engine job
     * @param config    the timer config string (date:..., duration:..., cycle:...)
     */
    public void scheduleTimer(JobEntity jobEntity, String config) {
        try {
            TriggerBuilder<Trigger> triggerBuilder = TriggerBuilder.newTrigger()
                    .withIdentity("timer-" + jobEntity.getId(), "bpmn-timers");

            if (config.startsWith("date:")) {
                Instant fireAt = Instant.parse(config.substring(5));
                triggerBuilder.startAt(Date.from(fireAt));

            } else if (config.startsWith("duration:")) {
                Duration duration = Duration.parse(config.substring(9));
                Instant fireAt = Instant.now().plus(duration);
                triggerBuilder.startAt(Date.from(fireAt));

            } else if (config.startsWith("cycle:")) {
                String cycleStr = config.substring(6);
                if (cycleStr.startsWith("R")) {
                    // ISO 8601 repeating interval: R3/PT1H
                    parseIsoCycle(cycleStr, triggerBuilder);
                } else {
                    // Assume cron expression
                    triggerBuilder
                            .startNow()
                            .withSchedule(CronScheduleBuilder.cronSchedule(cycleStr));
                }
            } else {
                log.warn("Unknown timer config format: {}", config);
                return;
            }

            JobDetail jobDetail = JobBuilder.newJob(TimerJob.class)
                    .withIdentity("timer-job-" + jobEntity.getId(), "bpmn-timers")
                    .usingJobData("bpmnJobId", jobEntity.getId())
                    .storeDurably(false)
                    .build();

            Trigger trigger = triggerBuilder.forJob(jobDetail).build();
            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Scheduled timer for job {} config={}", jobEntity.getId(), config);

        } catch (SchedulerException e) {
            log.error("Failed to schedule timer for job {}", jobEntity.getId(), e);
        }
    }

    /** Cancel a previously scheduled timer. */
    public void cancelTimer(String jobEntityId) {
        try {
            scheduler.deleteJob(JobKey.jobKey("timer-job-" + jobEntityId, "bpmn-timers"));
        } catch (SchedulerException e) {
            log.warn("Could not cancel timer for {}: {}", jobEntityId, e.getMessage());
        }
    }

    private void parseIsoCycle(String cycleStr, TriggerBuilder<Trigger> tb) {
        // R3/PT1H → repeat 3 times every 1 hour
        // R/PT5M  → repeat forever every 5 minutes
        String[] parts = cycleStr.split("/");
        int repeatCount;
        if (parts[0].equals("R") || parts[0].equals("R0")) {
            repeatCount = SimpleTrigger.REPEAT_INDEFINITELY;
        } else {
            repeatCount = Integer.parseInt(parts[0].substring(1)) - 1; // first fire is included
        }
        Duration interval = Duration.parse(parts[1]);
        tb.startNow().withSchedule(
                SimpleScheduleBuilder.simpleSchedule()
                        .withRepeatCount(repeatCount)
                        .withIntervalInMilliseconds(interval.toMillis())
        );
    }
}

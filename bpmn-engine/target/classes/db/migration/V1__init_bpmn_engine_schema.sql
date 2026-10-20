-- ============================================================
-- V1__init_bpmn_engine_schema.sql
-- BPMN Engine core tables
-- ============================================================

-- -------------------------------------------------------
-- Process Definition
-- -------------------------------------------------------
CREATE TABLE process_definition (
    id               VARCHAR(64)  NOT NULL PRIMARY KEY,
    process_key      VARCHAR(255) NOT NULL,
    version          INT          NOT NULL DEFAULT 1,
    proc_name        VARCHAR(255),
    description      TEXT,
    bpmn_xml         TEXT         NOT NULL,
    resource_name    VARCHAR(255),
    deployment_id    VARCHAR(64),
    suspended        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_pd_key_version UNIQUE (process_key, version)
);

-- -------------------------------------------------------
-- Deployment
-- -------------------------------------------------------
CREATE TABLE deployment (
    id           VARCHAR(64)  NOT NULL PRIMARY KEY,
    deploy_name  VARCHAR(255),
    deployed_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -------------------------------------------------------
-- Process Instance
-- -------------------------------------------------------
CREATE TABLE process_instance (
    id                    VARCHAR(64)  NOT NULL PRIMARY KEY,
    business_key          VARCHAR(255),
    process_definition_id VARCHAR(64)  NOT NULL,
    process_definition_key VARCHAR(255) NOT NULL,
    parent_instance_id    VARCHAR(64),
    super_execution_id    VARCHAR(64),
    state                 VARCHAR(32)  NOT NULL,
    start_time            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time              TIMESTAMP,
    duration_ms           BIGINT,
    start_user_id         VARCHAR(255),
    delete_reason         TEXT,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pi_pd FOREIGN KEY (process_definition_id) REFERENCES process_definition(id)
);

-- -------------------------------------------------------
-- Execution (token / path of execution)
-- -------------------------------------------------------
CREATE TABLE execution (
    id                    VARCHAR(64)  NOT NULL PRIMARY KEY,
    process_instance_id   VARCHAR(64)  NOT NULL,
    parent_execution_id   VARCHAR(64),
    current_flow_node_id  VARCHAR(255),
    is_scope              BOOLEAN      NOT NULL DEFAULT FALSE,
    is_concurrent         BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active             BOOLEAN      NOT NULL DEFAULT TRUE,
    is_event_scope        BOOLEAN      NOT NULL DEFAULT FALSE,
    state                 VARCHAR(32)  NOT NULL,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_exec_pi FOREIGN KEY (process_instance_id) REFERENCES process_instance(id)
);

-- -------------------------------------------------------
-- Process Variable
-- -------------------------------------------------------
CREATE TABLE process_variable (
    id                  VARCHAR(64)  NOT NULL PRIMARY KEY,
    process_instance_id VARCHAR(64)  NOT NULL,
    execution_id        VARCHAR(64),
    var_name            VARCHAR(255) NOT NULL,
    var_type            VARCHAR(32)  NOT NULL,
    text_value          TEXT,
    long_value          BIGINT,
    double_value        DOUBLE PRECISION,
    bytes_value         BYTEA,
    is_concurrent_local BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_pv_pi FOREIGN KEY (process_instance_id) REFERENCES process_instance(id),
    CONSTRAINT uq_pv_pi_name UNIQUE (process_instance_id, var_name)
);

-- -------------------------------------------------------
-- Task (UserTask)
-- -------------------------------------------------------
CREATE TABLE task (
    id                    VARCHAR(64)  NOT NULL PRIMARY KEY,
    task_name             VARCHAR(255),
    description           TEXT,
    execution_id          VARCHAR(64)  NOT NULL,
    process_instance_id   VARCHAR(64)  NOT NULL,
    process_definition_id VARCHAR(64)  NOT NULL,
    task_definition_key   VARCHAR(255),
    assignee              VARCHAR(255),
    owner                 VARCHAR(255),
    candidate_users       TEXT,
    candidate_groups      TEXT,
    due_date              TIMESTAMP,
    follow_up_date        TIMESTAMP,
    priority              INT          NOT NULL DEFAULT 50,
    state                 VARCHAR(32)  NOT NULL,
    form_key              VARCHAR(255),
    category              VARCHAR(255),
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at          TIMESTAMP,
    CONSTRAINT fk_task_exec FOREIGN KEY (execution_id) REFERENCES execution(id)
);

-- -------------------------------------------------------
-- Job (async / timer)
-- -------------------------------------------------------
CREATE TABLE job (
    id                    VARCHAR(64)  NOT NULL PRIMARY KEY,
    job_type              VARCHAR(32)  NOT NULL,
    execution_id          VARCHAR(64)  NOT NULL,
    process_instance_id   VARCHAR(64)  NOT NULL,
    process_definition_id VARCHAR(64)  NOT NULL,
    handler_type          VARCHAR(255),
    handler_config        TEXT,
    duedate               TIMESTAMP,
    repeat_pattern        VARCHAR(255),
    retries               INT          NOT NULL DEFAULT 3,
    exception_message     TEXT,
    exception_stacktrace  TEXT,
    suspended             BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_job_exec FOREIGN KEY (execution_id) REFERENCES execution(id)
);

-- -------------------------------------------------------
-- Event Subscription (message / signal / conditional)
-- -------------------------------------------------------
CREATE TABLE event_subscription (
    id                    VARCHAR(64)  NOT NULL PRIMARY KEY,
    event_type            VARCHAR(32)  NOT NULL,
    event_name            VARCHAR(255),
    execution_id          VARCHAR(64)  NOT NULL,
    process_instance_id   VARCHAR(64),
    process_definition_id VARCHAR(64),
    activity_id           VARCHAR(255),
    configuration         TEXT,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_es_exec FOREIGN KEY (execution_id) REFERENCES execution(id)
);

-- -------------------------------------------------------
-- History - Process Instance
-- -------------------------------------------------------
CREATE TABLE hist_process_instance (
    id                    VARCHAR(64)  NOT NULL PRIMARY KEY,
    process_definition_id VARCHAR(64)  NOT NULL,
    process_definition_key VARCHAR(255),
    process_definition_version INT,
    business_key          VARCHAR(255),
    parent_instance_id    VARCHAR(64),
    state                 VARCHAR(32)  NOT NULL,
    start_time            TIMESTAMP,
    end_time              TIMESTAMP,
    duration_ms           BIGINT,
    start_user_id         VARCHAR(255),
    delete_reason         TEXT,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -------------------------------------------------------
-- History - Activity Instance
-- -------------------------------------------------------
CREATE TABLE hist_activity_instance (
    id                    VARCHAR(64)  NOT NULL PRIMARY KEY,
    activity_id           VARCHAR(255) NOT NULL,
    activity_name         VARCHAR(255),
    activity_type         VARCHAR(64),
    execution_id          VARCHAR(64)  NOT NULL,
    process_instance_id   VARCHAR(64)  NOT NULL,
    process_definition_id VARCHAR(64)  NOT NULL,
    process_definition_key VARCHAR(255),
    start_time            TIMESTAMP,
    end_time              TIMESTAMP,
    duration_ms           BIGINT,
    assignee              VARCHAR(255),
    task_id               VARCHAR(64),
    called_process_instance_id VARCHAR(64),
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -------------------------------------------------------
-- History - Variable Instance
-- -------------------------------------------------------
CREATE TABLE hist_variable_instance (
    id                  VARCHAR(64)  NOT NULL PRIMARY KEY,
    process_instance_id VARCHAR(64)  NOT NULL,
    execution_id        VARCHAR(64),
    activity_instance_id VARCHAR(64),
    var_name            VARCHAR(255) NOT NULL,
    var_type            VARCHAR(32)  NOT NULL,
    text_value          TEXT,
    long_value          BIGINT,
    double_value        DOUBLE PRECISION,
    bytes_value         BYTEA,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- -------------------------------------------------------
-- Incident
-- -------------------------------------------------------
CREATE TABLE incident (
    id                    VARCHAR(64)  NOT NULL PRIMARY KEY,
    incident_type         VARCHAR(255) NOT NULL,
    execution_id          VARCHAR(64)  NOT NULL,
    activity_id           VARCHAR(255),
    process_instance_id   VARCHAR(64)  NOT NULL,
    process_definition_id VARCHAR(64)  NOT NULL,
    cause_incident_id     VARCHAR(64),
    root_cause_incident_id VARCHAR(64),
    configuration         TEXT,
    incident_message      TEXT,
    state                 VARCHAR(32)  NOT NULL DEFAULT 'OPEN',
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at           TIMESTAMP
);

-- -------------------------------------------------------
-- Quartz scheduler tables
-- -------------------------------------------------------
CREATE TABLE QRTZ_JOB_DETAILS (
    SCHED_NAME        VARCHAR(120) NOT NULL,
    JOB_NAME          VARCHAR(200) NOT NULL,
    JOB_GROUP         VARCHAR(200) NOT NULL,
    DESCRIPTION       VARCHAR(250),
    JOB_CLASS_NAME    VARCHAR(250) NOT NULL,
    IS_DURABLE        BOOLEAN      NOT NULL,
    IS_NONCONCURRENT  BOOLEAN      NOT NULL,
    IS_UPDATE_DATA    BOOLEAN      NOT NULL,
    REQUESTS_RECOVERY BOOLEAN      NOT NULL,
    JOB_DATA          BYTEA,
    PRIMARY KEY (SCHED_NAME, JOB_NAME, JOB_GROUP)
);
CREATE TABLE QRTZ_TRIGGERS (
    SCHED_NAME     VARCHAR(120) NOT NULL,
    TRIGGER_NAME   VARCHAR(200) NOT NULL,
    TRIGGER_GROUP  VARCHAR(200) NOT NULL,
    JOB_NAME       VARCHAR(200) NOT NULL,
    JOB_GROUP      VARCHAR(200) NOT NULL,
    DESCRIPTION    VARCHAR(250),
    NEXT_FIRE_TIME BIGINT,
    PREV_FIRE_TIME BIGINT,
    PRIORITY       INTEGER,
    TRIGGER_STATE  VARCHAR(16)  NOT NULL,
    TRIGGER_TYPE   VARCHAR(8)   NOT NULL,
    START_TIME     BIGINT       NOT NULL,
    END_TIME       BIGINT,
    CALENDAR_NAME  VARCHAR(200),
    MISFIRE_INSTR  SMALLINT,
    JOB_DATA       BYTEA,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, JOB_NAME, JOB_GROUP) REFERENCES QRTZ_JOB_DETAILS(SCHED_NAME, JOB_NAME, JOB_GROUP)
);
CREATE TABLE QRTZ_SIMPLE_TRIGGERS (
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(200) NOT NULL,
    TRIGGER_GROUP   VARCHAR(200) NOT NULL,
    REPEAT_COUNT    BIGINT       NOT NULL,
    REPEAT_INTERVAL BIGINT       NOT NULL,
    TIMES_TRIGGERED BIGINT       NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);
CREATE TABLE QRTZ_CRON_TRIGGERS (
    SCHED_NAME      VARCHAR(120) NOT NULL,
    TRIGGER_NAME    VARCHAR(200) NOT NULL,
    TRIGGER_GROUP   VARCHAR(200) NOT NULL,
    CRON_EXPRESSION VARCHAR(120) NOT NULL,
    TIME_ZONE_ID    VARCHAR(80),
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);
CREATE TABLE QRTZ_BLOB_TRIGGERS (
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_NAME  VARCHAR(200) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    BLOB_DATA     BYTEA,
    PRIMARY KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP),
    FOREIGN KEY (SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP) REFERENCES QRTZ_TRIGGERS(SCHED_NAME, TRIGGER_NAME, TRIGGER_GROUP)
);
CREATE TABLE QRTZ_CALENDARS (
    SCHED_NAME    VARCHAR(120) NOT NULL,
    CALENDAR_NAME VARCHAR(200) NOT NULL,
    CALENDAR      BYTEA        NOT NULL,
    PRIMARY KEY (SCHED_NAME, CALENDAR_NAME)
);
CREATE TABLE QRTZ_PAUSED_TRIGGER_GRPS (
    SCHED_NAME    VARCHAR(120) NOT NULL,
    TRIGGER_GROUP VARCHAR(200) NOT NULL,
    PRIMARY KEY (SCHED_NAME, TRIGGER_GROUP)
);
CREATE TABLE QRTZ_FIRED_TRIGGERS (
    SCHED_NAME        VARCHAR(120) NOT NULL,
    ENTRY_ID          VARCHAR(95)  NOT NULL,
    TRIGGER_NAME      VARCHAR(200) NOT NULL,
    TRIGGER_GROUP     VARCHAR(200) NOT NULL,
    INSTANCE_NAME     VARCHAR(200) NOT NULL,
    FIRED_TIME        BIGINT       NOT NULL,
    SCHED_TIME        BIGINT       NOT NULL,
    PRIORITY          INTEGER      NOT NULL,
    STATE             VARCHAR(16)  NOT NULL,
    JOB_NAME          VARCHAR(200),
    JOB_GROUP         VARCHAR(200),
    IS_NONCONCURRENT  BOOLEAN,
    REQUESTS_RECOVERY BOOLEAN,
    PRIMARY KEY (SCHED_NAME, ENTRY_ID)
);
CREATE TABLE QRTZ_SCHEDULER_STATE (
    SCHED_NAME        VARCHAR(120) NOT NULL,
    INSTANCE_NAME     VARCHAR(200) NOT NULL,
    LAST_CHECKIN_TIME BIGINT       NOT NULL,
    CHECKIN_INTERVAL  BIGINT       NOT NULL,
    PRIMARY KEY (SCHED_NAME, INSTANCE_NAME)
);
CREATE TABLE QRTZ_LOCKS (
    SCHED_NAME VARCHAR(120) NOT NULL,
    LOCK_NAME  VARCHAR(40)  NOT NULL,
    PRIMARY KEY (SCHED_NAME, LOCK_NAME)
);

-- Indexes
CREATE INDEX idx_pi_pd     ON process_instance(process_definition_id);
CREATE INDEX idx_pi_state  ON process_instance(state);
CREATE INDEX idx_exec_pi   ON execution(process_instance_id);
CREATE INDEX idx_pv_pi     ON process_variable(process_instance_id);
CREATE INDEX idx_task_pi   ON task(process_instance_id);
CREATE INDEX idx_task_assignee ON task(assignee);
CREATE INDEX idx_job_exec  ON job(execution_id);
CREATE INDEX idx_job_due   ON job(duedate);
CREATE INDEX idx_es_exec   ON event_subscription(execution_id);
CREATE INDEX idx_es_type_name ON event_subscription(event_type, event_name);
CREATE INDEX idx_hist_pi_key ON hist_process_instance(process_definition_key);
CREATE INDEX idx_hist_ai_exec ON hist_activity_instance(execution_id);

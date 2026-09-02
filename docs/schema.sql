-- ---------------------------------------------------------------------------
-- PulseOps database schema (REFERENCE ONLY)
--
-- The running application uses Hibernate `ddl-auto: update` to create these
-- tables, so this file is documentation, not the source of truth. A production
-- deployment would switch to `ddl-auto: validate` and manage this with Flyway.
-- ---------------------------------------------------------------------------

CREATE TABLE services (
    name         VARCHAR(64)  NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    tier         VARCHAR(16)  NOT NULL,          -- EDGE | APPLICATION | DATA
    description  VARCHAR(256),
    PRIMARY KEY (name)                            -- natural key: the service name
);

CREATE TABLE service_dependencies (
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    source_service VARCHAR(64) NOT NULL,          -- "source depends on target"
    target_service VARCHAR(64) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_service_dependency UNIQUE (source_service, target_service),
    CONSTRAINT fk_dep_source FOREIGN KEY (source_service) REFERENCES services (name),
    CONSTRAINT fk_dep_target FOREIGN KEY (target_service) REFERENCES services (name)
);

CREATE TABLE deployments (
    id          BIGINT      NOT NULL AUTO_INCREMENT,
    service     VARCHAR(64) NOT NULL,
    version     VARCHAR(64),
    deployed_at TIMESTAMP   NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_deployments_deployed_at (deployed_at),
    INDEX idx_deployments_service (service)
);

CREATE TABLE events (
    id            BIGINT       NOT NULL AUTO_INCREMENT,
    occurred_at   TIMESTAMP    NOT NULL,          -- producer clock
    ingested_at   TIMESTAMP    NOT NULL,          -- server clock
    service       VARCHAR(64)  NOT NULL,
    host          VARCHAR(64),
    event_type    VARCHAR(40)  NOT NULL,
    severity      VARCHAR(16)  NOT NULL,
    metric        VARCHAR(64),
    metric_value  DOUBLE,
    message       VARCHAR(1000) NOT NULL,
    deployment_id BIGINT,                          -- soft FK to deployments.id
    PRIMARY KEY (id),
    INDEX idx_events_occurred_at (occurred_at),    -- correlation window scan + filter
    INDEX idx_events_service (service),            -- GET /api/events?service=
    INDEX idx_events_event_type (event_type)       -- GET /api/events?eventType=
);

CREATE TABLE incidents (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    title               VARCHAR(200) NOT NULL,
    severity            VARCHAR(16)  NOT NULL,     -- LOW | MEDIUM | HIGH | CRITICAL
    status              VARCHAR(16)  NOT NULL,     -- OPEN | INVESTIGATING | RESOLVED
    started_at          TIMESTAMP    NOT NULL,     -- earliest correlated event
    resolved_at         TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL,
    updated_at          TIMESTAMP    NOT NULL,
    confidence_score    DOUBLE       NOT NULL,     -- correlation cluster strength
    probable_root_cause VARCHAR(128),
    root_cause_score    DOUBLE,
    correlation_summary VARCHAR(2000),
    version             BIGINT,                     -- optimistic lock
    PRIMARY KEY (id),
    INDEX idx_incidents_status (status),
    INDEX idx_incidents_started_at (started_at)
);

-- Many-to-many between incidents and events, carrying the score that linked them.
CREATE TABLE incident_events (
    id                BIGINT NOT NULL AUTO_INCREMENT,
    incident_id       BIGINT NOT NULL,
    event_id          BIGINT NOT NULL,
    correlation_score DOUBLE NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_incident_event UNIQUE (incident_id, event_id),   -- idempotent attach
    CONSTRAINT fk_ie_incident FOREIGN KEY (incident_id) REFERENCES incidents (id),
    CONSTRAINT fk_ie_event    FOREIGN KEY (event_id)    REFERENCES events (id),
    INDEX idx_incident_events_incident (incident_id),
    INDEX idx_incident_events_event (event_id)
);

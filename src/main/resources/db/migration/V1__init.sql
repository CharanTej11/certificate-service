-- Certificate Service schema. Timestamps are UTC (stored as timestamptz).

-- ---------------------------------------------------------------------------
-- People
-- ---------------------------------------------------------------------------
CREATE TABLE people (
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    version    BIGINT       NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- Programmes
-- ---------------------------------------------------------------------------
CREATE TABLE programmes (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- Designs
-- ---------------------------------------------------------------------------
CREATE TABLE designs (
    id         UUID PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    content    TEXT         NOT NULL,
    active     BOOLEAN      NOT NULL DEFAULT TRUE,
    version    BIGINT       NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------------------------------------------------------------------------
-- Programme <-> Design versioned link. Never overwritten; history is kept.
-- ---------------------------------------------------------------------------
CREATE TABLE programme_design_versions (
    id             UUID PRIMARY KEY,
    programme_id   UUID        NOT NULL REFERENCES programmes (id),
    design_id      UUID        NOT NULL REFERENCES designs (id),
    version        INTEGER     NOT NULL,
    effective_from TIMESTAMPTZ NOT NULL,
    effective_to   TIMESTAMPTZ,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_pdv_programme_version UNIQUE (programme_id, version)
);

CREATE INDEX idx_pdv_programme_effective ON programme_design_versions (programme_id, effective_from);
-- At most one open (current) version per programme.
CREATE UNIQUE INDEX uq_pdv_one_open_per_programme
    ON programme_design_versions (programme_id)
    WHERE effective_to IS NULL;

-- ---------------------------------------------------------------------------
-- Tracks
-- ---------------------------------------------------------------------------
CREATE TABLE tracks (
    id          UUID PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    version     BIGINT       NOT NULL DEFAULT 0,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE track_programmes (
    id           UUID PRIMARY KEY,
    track_id     UUID        NOT NULL REFERENCES tracks (id),
    programme_id UUID        NOT NULL REFERENCES programmes (id),
    position     INTEGER     NOT NULL,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_track_programme UNIQUE (track_id, programme_id)
);

CREATE INDEX idx_track_programmes_track ON track_programmes (track_id);

-- ---------------------------------------------------------------------------
-- Eligibility rules (one per programme). Configuration-driven.
-- ---------------------------------------------------------------------------
CREATE TABLE programme_rules (
    id           UUID PRIMARY KEY,
    programme_id UUID        NOT NULL REFERENCES programmes (id),
    rule_type    VARCHAR(32) NOT NULL,
    threshold    INTEGER,
    version      BIGINT      NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_programme_rule UNIQUE (programme_id)
);

-- ---------------------------------------------------------------------------
-- Certificates. Every display value is an immutable snapshot captured at issue.
-- ---------------------------------------------------------------------------
CREATE TABLE certificates (
    id                             UUID PRIMARY KEY,
    public_code                    VARCHAR(32)  NOT NULL,
    person_id                      UUID         NOT NULL REFERENCES people (id),
    programme_id                   UUID         NOT NULL REFERENCES programmes (id),
    track_id                       UUID         REFERENCES tracks (id),
    design_id                      UUID         REFERENCES designs (id),
    programme_design_version_id    UUID         REFERENCES programme_design_versions (id),
    person_name_snapshot           VARCHAR(255) NOT NULL,
    programme_name_snapshot        VARCHAR(255) NOT NULL,
    programme_description_snapshot VARCHAR(2000),
    design_name_snapshot           VARCHAR(255),
    design_content_snapshot        TEXT,
    track_name_snapshot            VARCHAR(255),
    route_type                     VARCHAR(16)  NOT NULL,
    route_key                      VARCHAR(64)  NOT NULL,
    issued_at                      TIMESTAMPTZ  NOT NULL,
    status                         VARCHAR(16)  NOT NULL,
    cancellation_reason            VARCHAR(1000),
    cancelled_at                   TIMESTAMPTZ,
    CONSTRAINT uq_certificate_public_code UNIQUE (public_code)
);

-- One LIVE certificate per person + programme + route. Cancelled rows are
-- excluded, so a certificate may be re-issued after cancellation.
CREATE UNIQUE INDEX uq_certificate_live_route
    ON certificates (person_id, programme_id, route_type, route_key)
    WHERE status = 'LIVE';

-- High-volume read path: a person's certificates, newest first.
CREATE INDEX idx_certificates_person_issued ON certificates (person_id, issued_at DESC);
CREATE INDEX idx_certificates_programme ON certificates (programme_id);

-- ---------------------------------------------------------------------------
-- Bulk issuing bookkeeping.
-- ---------------------------------------------------------------------------
CREATE TABLE bulk_certificate_jobs (
    id           UUID PRIMARY KEY,
    programme_id UUID        NOT NULL REFERENCES programmes (id),
    track_id     UUID        REFERENCES tracks (id),
    total_count  INTEGER     NOT NULL,
    issued_count INTEGER     NOT NULL DEFAULT 0,
    failed_count INTEGER     NOT NULL DEFAULT 0,
    status       VARCHAR(32),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    completed_at TIMESTAMPTZ
);

CREATE TABLE bulk_certificate_job_items (
    id             UUID PRIMARY KEY,
    job_id         UUID        NOT NULL REFERENCES bulk_certificate_jobs (id),
    person_id      UUID,
    status         VARCHAR(32) NOT NULL,
    certificate_id UUID        REFERENCES certificates (id),
    reason         VARCHAR(1000),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_bulk_items_job ON bulk_certificate_job_items (job_id);

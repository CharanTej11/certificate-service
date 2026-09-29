package com.example.certificates.entity;

import com.example.certificates.enums.RuleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

/**
 * The eligibility rule configured for a programme. One rule per programme.
 * Changing threshold/type here is a live configuration change — no redeploy.
 */
@Entity
@Table(name = "programme_rules")
public class ProgrammeRule extends Auditable {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "programme_id", nullable = false, unique = true)
    private UUID programmeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false)
    private RuleType ruleType;

    /** Null for ANYONE; required for MINIMUM_SCORE and ALL_TASKS_DONE. */
    @Column(name = "threshold")
    private Integer threshold;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected ProgrammeRule() {
    }

    public ProgrammeRule(UUID id, UUID programmeId, RuleType ruleType, Integer threshold) {
        this.id = id;
        this.programmeId = programmeId;
        this.ruleType = ruleType;
        this.threshold = threshold;
    }

    public UUID getId() {
        return id;
    }

    public UUID getProgrammeId() {
        return programmeId;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public Integer getThreshold() {
        return threshold;
    }

    public void update(RuleType ruleType, Integer threshold) {
        this.ruleType = ruleType;
        this.threshold = threshold;
    }
}

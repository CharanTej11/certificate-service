package com.example.certificates.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

@Entity
@Table(name = "designs")
public class Design extends Auditable {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    /** Opaque design content: HTML, template string, file name — never rendered here. */
    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected Design() {
    }

    public Design(UUID id, String name, String content) {
        this.id = id;
        this.name = name;
        this.content = content;
        this.active = true;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getContent() {
        return content;
    }

    public boolean isActive() {
        return active;
    }

    public void disable() {
        this.active = false;
    }
}

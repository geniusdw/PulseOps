package com.pulseops.topology.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * A directed edge {@code source -> target} meaning "source depends on target"
 * (a request to {@code source} may fan out to {@code target}).
 *
 * <p>Modelled as its own entity rather than a JPA {@code @ManyToMany} because
 * the edge is a first-class concept the correlation engine reasons about, and a
 * join <em>table</em> with no entity would make that awkward. The
 * {@code (source_service, target_service)} unique constraint prevents duplicate
 * edges.
 */
@Entity
@Table(name = "service_dependencies",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_service_dependency",
                columnNames = {"source_service", "target_service"}))
public class ServiceDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_service", length = 64, nullable = false)
    private String source;

    @Column(name = "target_service", length = 64, nullable = false)
    private String target;

    protected ServiceDependency() {
    }

    public ServiceDependency(String source, String target) {
        this.source = source;
        this.target = target;
    }

    public Long getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getTarget() {
        return target;
    }
}

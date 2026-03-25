package com.prashant.propertysearch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Represents an evaluation of a property, including details such as market value, associated
 * property reference, and additional notes. This entity is used to store and manage evaluations
 * of properties in the system.
 *
 * This class extends {@link AuditableEntity} to include auditing fields such as createdBy,
 * updatedBy, createdAt, and modifiedAt.
 *
 * Constraints:
 * - The property field is mandatory and establishes a many-to-one relationship with the Property entity.
 * - The market value field is mandatory and is stored as a decimal with precision and scale settings.
 * - The notes field is optional with a maximum character limit to restrict the input size.
 *
 * @author prashant
 */
@Entity
@Table(name = "property_evaluations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class PropertyEvaluation extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal marketValue;

    @Column(columnDefinition = "TEXT")
    @Size(max = 5000)
    private String notes;
}

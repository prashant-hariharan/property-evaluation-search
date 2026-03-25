package com.prashant.propertysearch.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * Represents a property entity with details such as title, address, city, area, and other
 * specific attributes related to the property. This entity is used to store and manage property
 * details in the system.
 *
 * This class extends {@link AuditableEntity} to include auditing fields such as createdBy,
 * updatedBy, createdAt, and modifiedAt.

 *
 * Constraints:
 * - Non-null constraints are applied to all required fields to ensure data integrity.
 * - Length and size constraints are applied to fields like title, address, city, postalCode, and description.
 *
 * @author prashant
 */
@Entity
@Table(name = "properties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class Property extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @EqualsAndHashCode.Include
    private UUID id;

    @Column(nullable = false, length = 255)
    @Size(max = 255)
    private String title;

    @Column(nullable = false, length = 255)
    private String address;

    @Column(nullable = false, length = 100)
    private String city;

    @Column(nullable = false, length = 20)
    private String postalCode;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal areaInSquareMeter;

    @Column(nullable = false)
    private Integer constructionYear;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PropertyType propertyType;

    @Column(columnDefinition = "TEXT")
    @Size(max = 2000)
    private String description;
}

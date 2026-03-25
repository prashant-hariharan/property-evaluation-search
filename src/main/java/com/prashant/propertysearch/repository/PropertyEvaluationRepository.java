package com.prashant.propertysearch.repository;

import com.prashant.propertysearch.entity.PropertyEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropertyEvaluationRepository extends JpaRepository<PropertyEvaluation, UUID> {
    List<PropertyEvaluation> findAllByPropertyIdOrderByCreatedAtDesc(UUID propertyId);
    Optional<PropertyEvaluation> findTopByPropertyIdOrderByCreatedAtDescIdDesc(UUID propertyId);

    @Query("""
            select pe
            from PropertyEvaluation pe
            where pe.property.id in :propertyIds
              and pe.createdAt = (
                select max(pe2.createdAt)
                from PropertyEvaluation pe2
                where pe2.property.id = pe.property.id
              )
            """)
    List<PropertyEvaluation> findLatestByPropertyIds(@Param("propertyIds") List<UUID> propertyIds);
}

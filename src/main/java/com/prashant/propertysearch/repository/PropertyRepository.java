package com.prashant.propertysearch.repository;

import com.prashant.propertysearch.entity.Property;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface PropertyRepository extends JpaRepository<Property, UUID> {
    @Query("select p.id from Property p")
    Page<UUID> findAllPropertyIds(Pageable pageable);
}

package com.robert.instrumentresultsservice.repository;

import com.robert.instrumentresultsservice.domain.MeasurementDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeasurementDefinitionRepository extends JpaRepository<MeasurementDefinition, Long> {

    Optional<MeasurementDefinition> findByCode(String code);

    List<MeasurementDefinition> findAllByIsActiveTrueOrderByNameAsc();
}

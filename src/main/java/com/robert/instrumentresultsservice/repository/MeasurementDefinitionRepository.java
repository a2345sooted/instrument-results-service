package com.robert.instrumentresultsservice.repository;

import com.robert.instrumentresultsservice.domain.MeasurementDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MeasurementDefinitionRepository extends JpaRepository<MeasurementDefinition, Long> {

    Optional<MeasurementDefinition> findByCode(String code);

    List<MeasurementDefinition> findAllByIsActiveTrueOrderByNameAsc();

    @Query("""
        select md
        from InstrumentMeasurement im
        join im.measurementDefinition md
        where im.instrument.id = :instrumentId
    """)
    List<MeasurementDefinition> findByInstrumentId(@Param("instrumentId") Long instrumentId);
}

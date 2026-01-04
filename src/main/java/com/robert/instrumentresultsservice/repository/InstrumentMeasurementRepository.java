package com.robert.instrumentresultsservice.repository;

import com.robert.instrumentresultsservice.domain.InstrumentMeasurement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InstrumentMeasurementRepository extends JpaRepository<InstrumentMeasurement, Long> {

    @Query("""
        select im
        from InstrumentMeasurement im
        join fetch im.measurementDefinition md
        where im.instrument.id = :instrumentId
        order by im.displayOrder asc
    """)
    List<InstrumentMeasurement> findRequiredByInstrumentId(@Param("instrumentId") Long instrumentId);

    @Query("""
        select im
        from InstrumentMeasurement im
        join fetch im.measurementDefinition md
        where im.instrument.code = :instrumentCode
        order by im.displayOrder asc
    """)
    List<InstrumentMeasurement> findRequiredByInstrumentCode(@Param("instrumentCode") String instrumentCode);
}

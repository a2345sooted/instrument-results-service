package com.robert.instrumentresultsservice.repository;

import com.robert.instrumentresultsservice.domain.Measurement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MeasurementRepository extends JpaRepository<Measurement, Long> {

    /**
     * Load all measurements for a run.
     */
    List<Measurement> findByInstrumentRunId(Long instrumentRunId);

    /**
     * Check if any measurements already exist for a run.
     * Useful to prevent double-submission.
     */
    boolean existsByInstrumentRunId(Long instrumentRunId);
}

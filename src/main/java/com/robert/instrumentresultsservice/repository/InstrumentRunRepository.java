package com.robert.instrumentresultsservice.repository;

import com.robert.instrumentresultsservice.domain.InstrumentRun;
import com.robert.instrumentresultsservice.domain.InstrumentRunStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstrumentRunRepository extends JpaRepository<InstrumentRun, Long> {

    List<InstrumentRun> findByInstrumentIdOrderByCreatedAtDesc(Long instrumentId);

    List<InstrumentRun> findByStatusOrderByCreatedAtDesc(InstrumentRunStatus status);
}

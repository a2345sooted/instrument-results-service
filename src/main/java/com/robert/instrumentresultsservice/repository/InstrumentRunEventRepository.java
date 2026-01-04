package com.robert.instrumentresultsservice.repository;

import com.robert.instrumentresultsservice.domain.InstrumentRunEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InstrumentRunEventRepository extends JpaRepository<InstrumentRunEvent, Long> {

    List<InstrumentRunEvent> findByInstrumentRunIdOrderByCreatedAtAsc(Long instrumentRunId);
}

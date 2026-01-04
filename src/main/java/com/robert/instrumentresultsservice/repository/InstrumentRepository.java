package com.robert.instrumentresultsservice.repository;

import com.robert.instrumentresultsservice.domain.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

    Optional<Instrument> findByCode(String code);

    List<Instrument> findAllByIsActiveTrueOrderByNameAsc();

    // Useful if you prefer code list for dropdowns
    List<Instrument> findAllByIsActiveTrueOrderByCodeAsc();
}

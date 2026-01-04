package com.robert.instrumentresultsservice.service;

import com.robert.instrumentresultsservice.api.dto.InstrumentSummary;
import com.robert.instrumentresultsservice.domain.Instrument;
import com.robert.instrumentresultsservice.repository.InstrumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InstrumentQueryServiceImpl implements InstrumentQueryService {

    private final InstrumentRepository instrumentRepository;

    public InstrumentQueryServiceImpl(InstrumentRepository instrumentRepository) {
        this.instrumentRepository = instrumentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstrumentSummary> listActiveInstruments() {
        List<Instrument> instruments =
                instrumentRepository.findAllByIsActiveTrueOrderByNameAsc();

        return instruments.stream()
                .map(i -> new InstrumentSummary(
                        i.getId(),
                        i.getCode(),
                        i.getName()
                ))
                .toList();
    }
}

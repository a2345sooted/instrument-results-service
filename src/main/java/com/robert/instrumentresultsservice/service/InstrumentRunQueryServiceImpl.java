package com.robert.instrumentresultsservice.service;

import com.robert.instrumentresultsservice.repository.InstrumentRunRepository;
import com.robert.instrumentresultsservice.service.result.InstrumentRunListItem;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class InstrumentRunQueryServiceImpl implements InstrumentRunQueryService {

    private final InstrumentRunRepository instrumentRunRepository;

    public InstrumentRunQueryServiceImpl(InstrumentRunRepository instrumentRunRepository) {
        this.instrumentRunRepository = instrumentRunRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<InstrumentRunListItem> getAllRuns() {
        return instrumentRunRepository
                .findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(r -> new InstrumentRunListItem(
                        r.getId(),
                        r.getExternalReference(),
                        r.getStatus(),
                        r.getCreatedAt()
                ))
                .toList();
    }
}

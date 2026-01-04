package com.robert.instrumentresultsservice.service;

import com.robert.instrumentresultsservice.domain.Instrument;
import com.robert.instrumentresultsservice.domain.InstrumentRun;
import com.robert.instrumentresultsservice.domain.InstrumentRunEvent;
import com.robert.instrumentresultsservice.domain.InstrumentRunEventType;
import com.robert.instrumentresultsservice.domain.InstrumentRunStatus;
import com.robert.instrumentresultsservice.service.result.InstrumentRunCreated;
import com.robert.instrumentresultsservice.repository.InstrumentRepository;
import com.robert.instrumentresultsservice.repository.InstrumentRunEventRepository;
import com.robert.instrumentresultsservice.repository.InstrumentRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class InstrumentRunServiceImpl implements InstrumentRunService {

    private final InstrumentRepository instrumentRepository;
    private final InstrumentRunRepository instrumentRunRepository;
    private final InstrumentRunEventRepository instrumentRunEventRepository;

    public InstrumentRunServiceImpl(
            InstrumentRepository instrumentRepository,
            InstrumentRunRepository instrumentRunRepository,
            InstrumentRunEventRepository instrumentRunEventRepository
    ) {
        this.instrumentRepository = instrumentRepository;
        this.instrumentRunRepository = instrumentRunRepository;
        this.instrumentRunEventRepository = instrumentRunEventRepository;
    }

    @Override
    @Transactional
    public InstrumentRunCreated createRun(
            String instrumentCode,
            UUID createdByClientId,
            String externalReference
    ) {
        // 1) Validate instrument exists
        Instrument instrument = instrumentRepository
                .findByCode(instrumentCode)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Instrument not found: " + instrumentCode
                        )
                );

        // 2) Create run
        InstrumentRun run = new InstrumentRun();
        run.setInstrument(instrument);
        run.setCreatedByClientId(createdByClientId);
        run.setExternalReference(externalReference);
        run.setStatus(InstrumentRunStatus.CREATED);

        run = instrumentRunRepository.save(run);

        // 3) Emit CREATED audit event
        InstrumentRunEvent event = new InstrumentRunEvent();
        event.setInstrumentRun(run);
        event.setEventType(InstrumentRunEventType.CREATED);

        instrumentRunEventRepository.save(event);

        // 4) Return domain result
        return new InstrumentRunCreated(
                run.getId(),
                instrument.getCode(),
                run.getStatus(),
                run.getCreatedAt()
        );
    }
}

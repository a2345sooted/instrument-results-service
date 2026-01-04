package com.robert.instrumentresultsservice.service;

import com.robert.instrumentresultsservice.domain.*;
import com.robert.instrumentresultsservice.repository.*;
import com.robert.instrumentresultsservice.service.result.InstrumentRunCreated;
import com.robert.instrumentresultsservice.service.result.InstrumentRunDetails;
import com.robert.instrumentresultsservice.service.result.RequiredMeasurement;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InstrumentRunServiceImpl implements InstrumentRunService {

    private final InstrumentRepository instrumentRepository;
    private final InstrumentRunRepository instrumentRunRepository;
    private final InstrumentRunEventRepository instrumentRunEventRepository;
    private final MeasurementRepository measurementRepository;
    private final MeasurementDefinitionRepository measurementDefinitionRepository;
    private final InstrumentMeasurementRepository instrumentMeasurementRepository;
    private final ProcessingService processingService;


    public InstrumentRunServiceImpl(
            InstrumentRepository instrumentRepository,
            InstrumentRunRepository instrumentRunRepository,
            MeasurementDefinitionRepository measurementDefinitionRepository,
            MeasurementRepository measurementRepository,
            InstrumentRunEventRepository instrumentRunEventRepository,
            InstrumentMeasurementRepository instrumentMeasurementRepository,
            ProcessingService processingService
    ) {
        this.instrumentRepository = instrumentRepository;
        this.instrumentRunRepository = instrumentRunRepository;
        this.measurementDefinitionRepository = measurementDefinitionRepository;
        this.measurementRepository = measurementRepository;
        this.instrumentRunEventRepository = instrumentRunEventRepository;
        this.instrumentMeasurementRepository = instrumentMeasurementRepository;
        this.processingService = processingService;
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

    @Override
    @Transactional
    public InstrumentRunDetails submitMeasurements(
            Long instrumentRunId,
            Map<String, BigDecimal> measurementsByCode,
            UUID submittedByClientId
    ) {
        // 1) Load run
        InstrumentRun run = instrumentRunRepository.findById(instrumentRunId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Instrument run not found: " + instrumentRunId
                        )
                );

        // 2) Enforce state
        if (run.getStatus() != InstrumentRunStatus.CREATED) {
            throw new IllegalStateException(
                    "Measurements already submitted for run: " + instrumentRunId
            );
        }

        // (Optional but recommended) basic input guardrails
        if (measurementsByCode == null || measurementsByCode.isEmpty()) {
            throw new IllegalArgumentException("No measurements submitted for run: " + instrumentRunId);
        }

        // 3) Load measurement definitions for this instrument
        Map<String, MeasurementDefinition> definitionsByCode =
                measurementDefinitionRepository
                        .findByInstrumentId(run.getInstrument().getId())
                        .stream()
                        .collect(Collectors.toMap(
                                MeasurementDefinition::getCode,
                                d -> d
                        ));

        // 4) Validate ALL codes (and values) before persisting anything
        for (Map.Entry<String, BigDecimal> entry : measurementsByCode.entrySet()) {
            String code = entry.getKey();

            if (!definitionsByCode.containsKey(code)) {
                throw new IllegalArgumentException("Unknown measurement code: " + code);
            }

            // Optional: reject null values (BigDecimal should be present)
            if (entry.getValue() == null) {
                throw new IllegalArgumentException("Measurement value is required for code: " + code);
            }
        }

        // 5) Persist measurements (now safe)
        for (Map.Entry<String, BigDecimal> entry : measurementsByCode.entrySet()) {
            MeasurementDefinition definition = definitionsByCode.get(entry.getKey());

            Measurement measurement = new Measurement();
            measurement.setInstrumentRun(run);
            measurement.setMeasurementDefinition(definition);
            measurement.setValue(entry.getValue());

            measurementRepository.save(measurement);
        }

        // 6) Update run
        OffsetDateTime now = OffsetDateTime.now();
        run.setStatus(InstrumentRunStatus.MEASUREMENTS_SUBMITTED);
        run.setMeasurementsSubmittedAt(now);
        run.setMeasurementsSubmittedByClientId(submittedByClientId);

        instrumentRunRepository.save(run);

        // 7) Emit audit event
        InstrumentRunEvent event = new InstrumentRunEvent();
        event.setInstrumentRun(run);
        event.setEventType(InstrumentRunEventType.MEASUREMENTS_SUBMITTED);
        instrumentRunEventRepository.save(event);

        // 8) Kick off async processing AFTER COMMIT
        Long runId = run.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                processingService.processRunAsync(runId);
            }
        });

        return getRunById(run.getId());
    }


    @Override
    @Transactional(readOnly = true)
    public InstrumentRunDetails getRunById(Long instrumentRunId) {
        // 1) Load run with instrument
        InstrumentRun run = instrumentRunRepository.findById(instrumentRunId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Instrument run not found: " + instrumentRunId
                        )
                );

        // Force instrument fetch
        Instrument instrument = run.getInstrument();

        // 2) Load submitted measurements (if any)
        Map<String, BigDecimal> submittedValuesByCode =
                measurementRepository.findByInstrumentRunId(instrumentRunId)
                        .stream()
                        .collect(Collectors.toMap(
                                m -> m.getMeasurementDefinition().getCode(),
                                Measurement::getValue
                        ));

        // 3) Load required measurements for this instrument (already ordered by displayOrder)
        List<RequiredMeasurement> requiredMeasurements =
                instrumentMeasurementRepository
                        .findRequiredByInstrumentId(instrument.getId())
                        .stream()
                        .map(im -> new RequiredMeasurement(
                                im.getMeasurementDefinition().getCode(),
                                im.getMeasurementDefinition().getName(),
                                im.getMeasurementDefinition().getUnit(),
                                im.getDisplayOrder(),
                                submittedValuesByCode.get(im.getMeasurementDefinition().getCode())
                        ))
                        .toList();

        // 4) Build result
        return new InstrumentRunDetails(
                run.getId(),
                instrument.getCode(),
                instrument.getName(),
                run.getCreatedByClientId(),
                run.getExternalReference(),
                run.getStatus(),
                run.getMeasurementsSubmittedAt(),
                run.getMeasurementsSubmittedByClientId(),
                run.getProcessingStartedAt(),
                run.getProcessingCompletedAt(),
                run.getErrorCode(),
                run.getErrorMessage(),
                run.getProcessResult(),
                run.getCreatedAt(),
                run.getUpdatedAt(),
                requiredMeasurements
        );
    }
}

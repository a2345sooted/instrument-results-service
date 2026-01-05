package com.robert.instrumentresultsservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.robert.instrumentresultsservice.domain.InstrumentRun;
import com.robert.instrumentresultsservice.domain.InstrumentRunEvent;
import com.robert.instrumentresultsservice.domain.InstrumentRunEventType;
import com.robert.instrumentresultsservice.domain.InstrumentRunStatus;
import com.robert.instrumentresultsservice.repository.InstrumentRunEventRepository;
import com.robert.instrumentresultsservice.repository.InstrumentRunRepository;
import com.robert.instrumentresultsservice.service.result.ProcessResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class ProcessingServiceImpl implements ProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ProcessingServiceImpl.class);

    private final InstrumentRunRepository instrumentRunRepository;
    private final InstrumentRunEventRepository instrumentRunEventRepository;
    private final ObjectMapper objectMapper;
    private final long processingDelayMs;

    public ProcessingServiceImpl(
            InstrumentRunRepository instrumentRunRepository,
            InstrumentRunEventRepository instrumentRunEventRepository,
            ObjectMapper objectMapper,
            @Value("${processing.delay.ms:3000}") long processingDelayMs
    ) {
        this.instrumentRunRepository = instrumentRunRepository;
        this.instrumentRunEventRepository = instrumentRunEventRepository;
        this.objectMapper = objectMapper;
        this.processingDelayMs = processingDelayMs;
    }

    @Override
    @Async("runProcessingExecutor")
    public void processRunAsync(Long runId) {
        log.info("Starting async processing for run {}", runId);

        InstrumentRun run = instrumentRunRepository.findById(runId).orElse(null);
        if (run == null) {
            log.warn("Run {} not found; cannot process", runId);
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        run.setStatus(InstrumentRunStatus.PROCESSING);
        run.setProcessingStartedAt(now);
        run.setProcessingCompletedAt(null);
        run.setErrorCode(null);
        run.setErrorMessage(null);

        instrumentRunRepository.save(run);
        saveEvent(run, InstrumentRunEventType.PROCESSING_STARTED, null);

        try {
            Thread.sleep(processingDelayMs);

            OffsetDateTime done = OffsetDateTime.now();
            run.setStatus(InstrumentRunStatus.SUCCEEDED);
            run.setProcessingCompletedAt(done);
            run.setProcessResult(new ProcessResult("stub-result"));
            run.setErrorCode(null);
            run.setErrorMessage(null);

            instrumentRunRepository.save(run);
            saveEvent(run, InstrumentRunEventType.PROCESSING_COMPLETED, null);

            log.info("Completed async processing for run {}", runId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            markFailed(run, "INTERRUPTED", "Processing thread interrupted");
            log.error("Processing interrupted for run {}", runId, e);
        } catch (Exception e) {
            markFailed(run, "UNEXPECTED_ERROR", e.getMessage());
            log.error("Processing failed for run {}", runId, e);
        }
    }

    private void markFailed(InstrumentRun run, String errorCode, String errorMessage) {
        OffsetDateTime done = OffsetDateTime.now();

        run.setStatus(InstrumentRunStatus.FAILED);
        run.setProcessingCompletedAt(done);
        run.setErrorCode(errorCode);
        run.setErrorMessage(errorMessage);

        instrumentRunRepository.save(run);

        ObjectNode details = objectMapper.createObjectNode();
        details.put("errorCode", errorCode);
        details.put("errorMessage", errorMessage);

        saveEvent(run, InstrumentRunEventType.PROCESSING_FAILED, details);
    }

    private void saveEvent(
            InstrumentRun run,
            InstrumentRunEventType type,
            ObjectNode details
    ) {
        InstrumentRunEvent event = new InstrumentRunEvent();
        event.setInstrumentRun(run);
        event.setEventType(type);
        event.setDetails(details);
        instrumentRunEventRepository.save(event);
    }
}

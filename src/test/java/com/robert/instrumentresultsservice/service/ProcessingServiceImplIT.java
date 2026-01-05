package com.robert.instrumentresultsservice.service;

import com.robert.instrumentresultsservice.TestcontainersConfiguration;
import com.robert.instrumentresultsservice.domain.*;
import com.robert.instrumentresultsservice.repository.InstrumentRepository;
import com.robert.instrumentresultsservice.repository.InstrumentRunEventRepository;
import com.robert.instrumentresultsservice.repository.InstrumentRunRepository;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class ProcessingServiceImplIT {

    @Autowired ProcessingService processingService;

    @Autowired InstrumentRepository instrumentRepository;
    @Autowired InstrumentRunRepository instrumentRunRepository;
    @Autowired InstrumentRunEventRepository instrumentRunEventRepository;

    @Test
    void processRunAsync_happyPath_updatesRunToSucceeded_setsTimestamps_setsProcessResult_andEmitsEvents() {
        // Arrange: instrument + run in DB
        Instrument instrument = new Instrument();
        instrument.setCode("CBC-1000");
        instrument.setName("CBC Analyzer");
        instrument = instrumentRepository.save(instrument);

        InstrumentRun run = new InstrumentRun();
        run.setInstrument(instrument);
        run.setCreatedByClientId(UUID.randomUUID());
        run.setExternalReference("EXT-PROC-1");
        run.setStatus(InstrumentRunStatus.MEASUREMENTS_SUBMITTED); // any non-null is fine; processing overwrites
        run = instrumentRunRepository.save(run);

        Long runId = run.getId();
        assertNotNull(runId);

        // Act: kick off async processing
        processingService.processRunAsync(runId);

        // Wait until SUCCEEDED (test delay is 100ms, but allow slack for CI)
        Awaitility.await()
                .atMost(Duration.ofSeconds(5))
                .pollInterval(Duration.ofMillis(50))
                .untilAsserted(() -> {
                    InstrumentRun refreshed = instrumentRunRepository.findById(runId)
                            .orElseThrow(() -> new AssertionError("Run disappeared"));

                    assertEquals(InstrumentRunStatus.SUCCEEDED, refreshed.getStatus());
                    assertNotNull(refreshed.getProcessingStartedAt());
                    assertNotNull(refreshed.getProcessingCompletedAt());
                    assertNull(refreshed.getErrorCode());
                    assertNull(refreshed.getErrorMessage());
                    assertNotNull(refreshed.getProcessResult());
                    assertEquals("stub-result", refreshed.getProcessResult().result());
                });

        // Assert: events exist (in order)
        List<InstrumentRunEvent> events =
                instrumentRunEventRepository.findByInstrumentRunIdOrderByCreatedAtAsc(runId);

        assertTrue(events.size() >= 2, "Expected at least 2 processing events");

        // We only care that STARTED appears before COMPLETED
        int startedIdx = indexOf(events, InstrumentRunEventType.PROCESSING_STARTED);
        int completedIdx = indexOf(events, InstrumentRunEventType.PROCESSING_COMPLETED);

        assertTrue(startedIdx >= 0, "Missing PROCESSING_STARTED event");
        assertTrue(completedIdx >= 0, "Missing PROCESSING_COMPLETED event");
        assertTrue(startedIdx < completedIdx, "Expected STARTED before COMPLETED");
    }

    @Test
    void processRunAsync_runNotFound_doesNotCreateEvents() {
        // Arrange: pick an ID that doesn't exist
        long missingRunId = 9_999_999L;

        long eventsBefore = instrumentRunEventRepository.count();

        // Act
        processingService.processRunAsync(missingRunId);

        // Assert: since it returns immediately, no need for Awaitility here
        long eventsAfter = instrumentRunEventRepository.count();
        assertEquals(eventsBefore, eventsAfter, "Expected no events to be created for a missing run");
    }


    private static int indexOf(List<InstrumentRunEvent> events, InstrumentRunEventType type) {
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getEventType() == type) return i;
        }
        return -1;
    }
}

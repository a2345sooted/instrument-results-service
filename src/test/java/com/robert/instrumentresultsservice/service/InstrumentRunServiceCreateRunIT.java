package com.robert.instrumentresultsservice.service;

import com.robert.instrumentresultsservice.TestcontainersConfiguration;
import com.robert.instrumentresultsservice.domain.Instrument;
import com.robert.instrumentresultsservice.domain.InstrumentRun;
import com.robert.instrumentresultsservice.domain.InstrumentRunEvent;
import com.robert.instrumentresultsservice.domain.InstrumentRunEventType;
import com.robert.instrumentresultsservice.domain.InstrumentRunStatus;
import com.robert.instrumentresultsservice.repository.InstrumentRepository;
import com.robert.instrumentresultsservice.repository.InstrumentRunEventRepository;
import com.robert.instrumentresultsservice.repository.InstrumentRunRepository;
import com.robert.instrumentresultsservice.service.result.InstrumentRunCreated;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class InstrumentRunServiceCreateRunIT {

    @Autowired InstrumentRunService instrumentRunService;

    @Autowired InstrumentRepository instrumentRepository;
    @Autowired InstrumentRunRepository instrumentRunRepository;
    @Autowired InstrumentRunEventRepository instrumentRunEventRepository;

    @Test
    @Transactional
    void createRun_persistsRunAndCreatedEvent_andReturnsCreatedDto() {
        // Arrange: instrument exists (createRun looks it up by code)
        Instrument instrument = new Instrument();
        instrument.setCode("CBC-1000");
        instrument.setName("CBC Analyzer");
        instrumentRepository.save(instrument);

        UUID clientId = UUID.randomUUID();
        String externalRef = "EXT-ABC-123";

        // Act
        InstrumentRunCreated created = instrumentRunService.createRun("CBC-1000", clientId, externalRef);

        // Assert: returned DTO
        assertNotNull(created);
        assertNotNull(created.runId());
        assertEquals("CBC-1000", created.instrumentCode());
        assertEquals(InstrumentRunStatus.CREATED, created.status());
        assertNotNull(created.createdAt());

        Long runId = created.runId();

        // Assert: run row exists
        InstrumentRun run = instrumentRunRepository.findById(runId)
                .orElseThrow(() -> new AssertionError("Expected run to exist in DB"));

        assertEquals(InstrumentRunStatus.CREATED, run.getStatus());
        assertEquals(clientId, run.getCreatedByClientId());
        assertEquals(externalRef, run.getExternalReference());
        assertNotNull(run.getCreatedAt());
        assertNotNull(run.getInstrument());
        assertEquals("CBC-1000", run.getInstrument().getCode());

        // Assert: audit event row exists (use repo method)
        List<InstrumentRunEvent> events =
                instrumentRunEventRepository.findByInstrumentRunIdOrderByCreatedAtAsc(runId);

        assertFalse(events.isEmpty(), "Expected at least one event for runId=" + runId);
        assertTrue(
                events.stream().anyMatch(e -> e.getEventType() == InstrumentRunEventType.CREATED),
                "Expected a CREATED event for runId=" + runId
        );
    }

    @Test
    @Transactional
    void createRun_instrumentNotFound_throwsIllegalArgumentException_andDoesNotPersistRunOrEvent() {
        // Arrange
        UUID clientId = UUID.randomUUID();
        String externalRef = "EXT-DOES-NOT-MATTER";
        String missingInstrumentCode = "DOES-NOT-EXIST";

        // Act + Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> instrumentRunService.createRun(missingInstrumentCode, clientId, externalRef)
        );

        assertTrue(ex.getMessage().contains("Instrument not found: " + missingInstrumentCode));

        assertTrue(instrumentRunEventRepository.findAll().isEmpty(), "Expected no events to be created");
        assertTrue(instrumentRunRepository.findAll().isEmpty(), "Expected no runs to be created");
    }
}

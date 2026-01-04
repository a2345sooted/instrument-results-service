package com.robert.instrumentresultsservice.service;

import com.robert.instrumentresultsservice.domain.Instrument;
import com.robert.instrumentresultsservice.domain.InstrumentRun;
import com.robert.instrumentresultsservice.domain.InstrumentRunEvent;
import com.robert.instrumentresultsservice.domain.InstrumentRunEventType;
import com.robert.instrumentresultsservice.domain.InstrumentRunStatus;
import com.robert.instrumentresultsservice.repository.InstrumentMeasurementRepository;
import com.robert.instrumentresultsservice.repository.InstrumentRepository;
import com.robert.instrumentresultsservice.repository.InstrumentRunEventRepository;
import com.robert.instrumentresultsservice.repository.InstrumentRunRepository;
import com.robert.instrumentresultsservice.repository.MeasurementDefinitionRepository;
import com.robert.instrumentresultsservice.repository.MeasurementRepository;
import com.robert.instrumentresultsservice.service.result.InstrumentRunCreated;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstrumentRunServiceImplCreateRunTest {

    @Mock InstrumentRepository instrumentRepository;
    @Mock InstrumentRunRepository instrumentRunRepository;
    @Mock MeasurementDefinitionRepository measurementDefinitionRepository;
    @Mock MeasurementRepository measurementRepository;
    @Mock InstrumentRunEventRepository instrumentRunEventRepository;
    @Mock InstrumentMeasurementRepository instrumentMeasurementRepository;
    @Mock ProcessingService processingService;

    @InjectMocks InstrumentRunServiceImpl service;

    @Test
    void createRun_happyPath_persistsRunAndCreatedEvent_returnsResult() {
        // Arrange
        String instrumentCode = "CBC-1000";
        UUID clientId = UUID.randomUUID();
        String externalRef = "EXT-ABC-123";
        OffsetDateTime createdAt = OffsetDateTime.parse("2026-01-04T12:34:56-06:00");

        Instrument instrument = new Instrument();
        instrument.setCode(instrumentCode);

        when(instrumentRepository.findByCode(instrumentCode)).thenReturn(Optional.of(instrument));

        // return a "persisted" run object (mock) so we can control id/createdAt without relying on JPA callbacks
        InstrumentRun persistedRun = mock(InstrumentRun.class);
        when(persistedRun.getId()).thenReturn(42L);
        when(persistedRun.getStatus()).thenReturn(InstrumentRunStatus.CREATED);
        when(persistedRun.getCreatedAt()).thenReturn(createdAt);

        when(instrumentRunRepository.save(any(InstrumentRun.class))).thenReturn(persistedRun);

        // Act
        InstrumentRunCreated result = service.createRun(instrumentCode, clientId, externalRef);

        // Assert: run saved with the right inputs
        ArgumentCaptor<InstrumentRun> runCaptor = ArgumentCaptor.forClass(InstrumentRun.class);
        verify(instrumentRunRepository).save(runCaptor.capture());

        InstrumentRun runToSave = runCaptor.getValue();
        assertSame(instrument, runToSave.getInstrument());
        assertEquals(clientId, runToSave.getCreatedByClientId());
        assertEquals(externalRef, runToSave.getExternalReference());
        assertEquals(InstrumentRunStatus.CREATED, runToSave.getStatus());

        // Assert: CREATED event persisted and points at the *persistedRun* returned by save()
        ArgumentCaptor<InstrumentRunEvent> eventCaptor = ArgumentCaptor.forClass(InstrumentRunEvent.class);
        verify(instrumentRunEventRepository).save(eventCaptor.capture());

        InstrumentRunEvent savedEvent = eventCaptor.getValue();
        assertEquals(InstrumentRunEventType.CREATED, savedEvent.getEventType());
        assertSame(persistedRun, savedEvent.getInstrumentRun());

        // Assert: returned domain result
        assertEquals(new InstrumentRunCreated(42L, instrumentCode, InstrumentRunStatus.CREATED, createdAt), result);

        verify(instrumentRepository).findByCode(instrumentCode);
        verifyNoMoreInteractions(instrumentRepository, instrumentRunRepository, instrumentRunEventRepository);
    }

    @Test
    void createRun_instrumentNotFound_throws_andDoesNotPersistAnything() {
        // Arrange
        String instrumentCode = "NOPE";
        when(instrumentRepository.findByCode(instrumentCode)).thenReturn(Optional.empty());

        // Act + Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.createRun(instrumentCode, UUID.randomUUID(), "EXT")
        );
        assertTrue(ex.getMessage().contains("Instrument not found: " + instrumentCode));

        verify(instrumentRepository).findByCode(instrumentCode);
        verifyNoInteractions(instrumentRunRepository, instrumentRunEventRepository);
    }
}

package com.robert.instrumentresultsservice.service;

import com.robert.instrumentresultsservice.domain.Instrument;
import com.robert.instrumentresultsservice.domain.InstrumentRun;
import com.robert.instrumentresultsservice.domain.InstrumentRunEvent;
import com.robert.instrumentresultsservice.domain.InstrumentRunEventType;
import com.robert.instrumentresultsservice.domain.InstrumentRunStatus;
import com.robert.instrumentresultsservice.domain.MeasurementDefinition;
import com.robert.instrumentresultsservice.repository.InstrumentMeasurementRepository;
import com.robert.instrumentresultsservice.repository.InstrumentRunEventRepository;
import com.robert.instrumentresultsservice.repository.InstrumentRunRepository;
import com.robert.instrumentresultsservice.repository.MeasurementDefinitionRepository;
import com.robert.instrumentresultsservice.repository.MeasurementRepository;
import com.robert.instrumentresultsservice.service.result.InstrumentRunDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstrumentRunServiceImplSubmitMeasurementsTest {

    @Mock InstrumentRunRepository instrumentRunRepository;
    @Mock MeasurementDefinitionRepository measurementDefinitionRepository;
    @Mock MeasurementRepository measurementRepository;
    @Mock InstrumentRunEventRepository instrumentRunEventRepository;
    @Mock InstrumentMeasurementRepository instrumentMeasurementRepository; // not used directly here (we stub getRunById)
    @Mock ProcessingService processingService;

    // We spy the service so we can stub getRunById(), keeping this test focused on submitMeasurements()
    @Spy @InjectMocks
    InstrumentRunServiceImpl service;

    @BeforeEach
    void initTxSync() {
        // submitMeasurements registers an afterCommit hook; in a pure unit test we must initialize synchronization manually
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTxSync() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void submitMeasurements_happyPath_persistsMeasurements_updatesRun_emitsEvent_returnsDetails_andProcessesAfterCommit() {
        // Arrange
        Long runId = 123L;
        UUID submittedBy = UUID.randomUUID();

        Instrument instrument = mock(Instrument.class);
        when(instrument.getId()).thenReturn(10L);

        InstrumentRun run = mock(InstrumentRun.class);
        when(run.getId()).thenReturn(runId);
        when(run.getInstrument()).thenReturn(instrument);
        when(run.getStatus()).thenReturn(InstrumentRunStatus.CREATED);

        when(instrumentRunRepository.findById(runId)).thenReturn(Optional.of(run));

        MeasurementDefinition defA = mock(MeasurementDefinition.class);
        when(defA.getCode()).thenReturn("A");

        MeasurementDefinition defB = mock(MeasurementDefinition.class);
        when(defB.getCode()).thenReturn("B");

        when(measurementDefinitionRepository.findByInstrumentId(10L))
                .thenReturn(List.of(defA, defB));

        Map<String, BigDecimal> measurements = new LinkedHashMap<>();
        measurements.put("A", new BigDecimal("1.23"));
        measurements.put("B", new BigDecimal("4.56"));

        InstrumentRunDetails expectedDetails = mock(InstrumentRunDetails.class);
        doReturn(expectedDetails).when(service).getRunById(runId);

        // Act
        InstrumentRunDetails result = service.submitMeasurements(runId, measurements, submittedBy);

        // Assert: measurements persisted (count)
        verify(measurementRepository, times(2)).save(any());

        // Assert: run updated + saved
        verify(run).setStatus(InstrumentRunStatus.MEASUREMENTS_SUBMITTED);
        verify(run).setMeasurementsSubmittedByClientId(submittedBy);
        verify(run).setMeasurementsSubmittedAt(any(OffsetDateTime.class));
        verify(instrumentRunRepository).save(run);

        // Assert: event emitted
        ArgumentCaptor<InstrumentRunEvent> eventCaptor = ArgumentCaptor.forClass(InstrumentRunEvent.class);
        verify(instrumentRunEventRepository).save(eventCaptor.capture());

        InstrumentRunEvent savedEvent = eventCaptor.getValue();
        assertEquals(InstrumentRunEventType.MEASUREMENTS_SUBMITTED, savedEvent.getEventType());
        assertSame(run, savedEvent.getInstrumentRun());

        // Assert: returns details from getRunById
        assertSame(expectedDetails, result);
        verify(service).getRunById(runId);

        // Assert: afterCommit hook registered and triggers async processing
        List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
        assertFalse(syncs.isEmpty(), "Expected an afterCommit synchronization to be registered.");

        // Fire afterCommit manually (simulates successful transaction commit)
        syncs.forEach(TransactionSynchronization::afterCommit);
        verify(processingService).processRunAsync(runId);
    }

    @Test
    void submitMeasurements_runNotFound_throwsIllegalArgumentException() {
        // Arrange
        Long runId = 999L;
        when(instrumentRunRepository.findById(runId)).thenReturn(Optional.empty());

        // Act + Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.submitMeasurements(runId, Map.of("A", BigDecimal.ONE), UUID.randomUUID())
        );

        assertTrue(ex.getMessage().contains("Instrument run not found: " + runId));
        verify(instrumentRunRepository).findById(runId);
        verifyNoInteractions(measurementDefinitionRepository, measurementRepository, instrumentRunEventRepository, processingService);
    }

    @Test
    void submitMeasurements_wrongStatus_throwsIllegalStateException_andDoesNotPersistAnything() {
        // Arrange
        Long runId = 123L;

        InstrumentRun run = mock(InstrumentRun.class);
        when(run.getStatus()).thenReturn(InstrumentRunStatus.MEASUREMENTS_SUBMITTED);

        when(instrumentRunRepository.findById(runId)).thenReturn(Optional.of(run));

        // Act + Assert
        IllegalStateException ex = assertThrows(
                IllegalStateException.class,
                () -> service.submitMeasurements(runId, Map.of("A", BigDecimal.ONE), UUID.randomUUID())
        );

        assertTrue(ex.getMessage().contains("Measurements already submitted for run: " + runId));

        verify(instrumentRunRepository).findById(runId);
        verifyNoInteractions(measurementDefinitionRepository, measurementRepository, instrumentRunEventRepository, processingService);
    }

    @Test
    void submitMeasurements_unknownMeasurementCode_throws_andDoesNotPersistOrUpdateOrEmitOrSchedule() {
        // Arrange
        Long runId = 123L;
        UUID submittedBy = UUID.randomUUID();

        Instrument instrument = mock(Instrument.class);
        when(instrument.getId()).thenReturn(10L);

        InstrumentRun run = mock(InstrumentRun.class);
        when(run.getInstrument()).thenReturn(instrument);
        when(run.getStatus()).thenReturn(InstrumentRunStatus.CREATED);

        when(instrumentRunRepository.findById(runId)).thenReturn(Optional.of(run));

        MeasurementDefinition defA = mock(MeasurementDefinition.class);
        when(defA.getCode()).thenReturn("A");

        when(measurementDefinitionRepository.findByInstrumentId(10L))
                .thenReturn(List.of(defA));

        Map<String, BigDecimal> measurements = new LinkedHashMap<>();
        measurements.put("A", BigDecimal.ONE);
        measurements.put("UNKNOWN", new BigDecimal("9.99"));

        // Act + Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.submitMeasurements(runId, measurements, submittedBy)
        );
        assertTrue(ex.getMessage().contains("Unknown measurement code: UNKNOWN"));

        // Now: validation happens before persistence, so nothing is saved
        verify(measurementRepository, never()).save(any());

        // And still: no run/event/processing work
        verify(instrumentRunRepository, never()).save(any());
        verify(instrumentRunEventRepository, never()).save(any());
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
        verifyNoInteractions(processingService);
    }


    @Test
    void submitMeasurements_emptyMeasurements_throwsIllegalArgumentException_andDoesNotQueryDefinitionsOrPersist() {
        // Arrange
        Long runId = 123L;

        InstrumentRun run = mock(InstrumentRun.class);
        when(run.getStatus()).thenReturn(InstrumentRunStatus.CREATED);
        when(instrumentRunRepository.findById(runId)).thenReturn(Optional.of(run));

        // Act + Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.submitMeasurements(runId, Collections.emptyMap(), UUID.randomUUID())
        );
        assertTrue(ex.getMessage().contains("No measurements submitted for run: " + runId));

        // Assert: only loads run, nothing else
        verify(instrumentRunRepository).findById(runId);
        verifyNoMoreInteractions(instrumentRunRepository);

        verifyNoInteractions(
                measurementDefinitionRepository,
                measurementRepository,
                instrumentRunEventRepository,
                processingService
        );
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
    }


    @Test
    void submitMeasurements_nullValueForCode_throwsIllegalArgumentException_andDoesNotPersistOrUpdateOrSchedule() {
        // Arrange
        Long runId = 123L;

        Instrument instrument = mock(Instrument.class);
        when(instrument.getId()).thenReturn(10L);

        InstrumentRun run = mock(InstrumentRun.class);
        when(run.getInstrument()).thenReturn(instrument);
        when(run.getStatus()).thenReturn(InstrumentRunStatus.CREATED);

        when(instrumentRunRepository.findById(runId)).thenReturn(Optional.of(run));

        MeasurementDefinition defA = mock(MeasurementDefinition.class);
        when(defA.getCode()).thenReturn("A");

        when(measurementDefinitionRepository.findByInstrumentId(10L))
                .thenReturn(List.of(defA));

        Map<String, BigDecimal> measurements = new LinkedHashMap<>();
        measurements.put("A", null);

        // Act + Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.submitMeasurements(runId, measurements, UUID.randomUUID())
        );
        assertTrue(ex.getMessage().contains("Measurement value is required for code: A"));

        // Nothing persisted / updated / scheduled
        verify(measurementRepository, never()).save(any());
        verify(instrumentRunRepository, never()).save(any());
        verify(instrumentRunEventRepository, never()).save(any());
        assertTrue(TransactionSynchronizationManager.getSynchronizations().isEmpty());
        verifyNoInteractions(processingService);
    }


}

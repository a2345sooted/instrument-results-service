package com.robert.instrumentresultsservice.service;

import com.robert.instrumentresultsservice.domain.*;
import com.robert.instrumentresultsservice.repository.*;
import com.robert.instrumentresultsservice.service.result.InstrumentRunDetails;
import com.robert.instrumentresultsservice.service.result.RequiredMeasurement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstrumentRunServiceImplGetRunByIdTest {

    @Mock InstrumentRunRepository instrumentRunRepository;
    @Mock MeasurementRepository measurementRepository;
    @Mock InstrumentMeasurementRepository instrumentMeasurementRepository;

    // Other service deps (not used by getRunById) but needed for constructor/injection
    @Mock InstrumentRepository instrumentRepository;
    @Mock MeasurementDefinitionRepository measurementDefinitionRepository;
    @Mock InstrumentRunEventRepository instrumentRunEventRepository;
    @Mock ProcessingService processingService;

    @InjectMocks
    InstrumentRunServiceImpl service;

    @Test
    void getRunById_happyPath_buildsRequiredMeasurements_andInjectsSubmittedValues() {
        // Arrange
        Long runId = 123L;

        Instrument instrument = mock(Instrument.class);
        when(instrument.getId()).thenReturn(10L);
        when(instrument.getCode()).thenReturn("INST-1");
        when(instrument.getName()).thenReturn("Instrument 1");

        UUID createdBy = UUID.randomUUID();

        InstrumentRun run = mock(InstrumentRun.class);
        when(run.getId()).thenReturn(runId);
        when(run.getInstrument()).thenReturn(instrument);
        when(run.getCreatedByClientId()).thenReturn(createdBy);
        when(run.getExternalReference()).thenReturn("ext-abc");
        when(run.getStatus()).thenReturn(InstrumentRunStatus.CREATED);
        when(run.getCreatedAt()).thenReturn(OffsetDateTime.parse("2026-01-04T12:00:00-06:00"));
        when(run.getUpdatedAt()).thenReturn(OffsetDateTime.parse("2026-01-04T12:00:00-06:00"));

        when(instrumentRunRepository.findById(runId)).thenReturn(Optional.of(run));

        // Submitted measurements: only A submitted
        MeasurementDefinition defA = mock(MeasurementDefinition.class);
        when(defA.getCode()).thenReturn("A");

        Measurement mA = mock(Measurement.class);
        when(mA.getMeasurementDefinition()).thenReturn(defA);
        when(mA.getValue()).thenReturn(new BigDecimal("1.23"));

        when(measurementRepository.findByInstrumentRunId(runId)).thenReturn(List.of(mA));

        // Required measurement list: A then B (service assumes repo already ordered)
        MeasurementDefinition reqDefA = mock(MeasurementDefinition.class);
        when(reqDefA.getCode()).thenReturn("A");
        when(reqDefA.getName()).thenReturn("Alpha");
        when(reqDefA.getUnit()).thenReturn("mg");

        InstrumentMeasurement reqA = mock(InstrumentMeasurement.class);
        when(reqA.getMeasurementDefinition()).thenReturn(reqDefA);
        when(reqA.getDisplayOrder()).thenReturn(1);

        MeasurementDefinition reqDefB = mock(MeasurementDefinition.class);
        when(reqDefB.getCode()).thenReturn("B");
        when(reqDefB.getName()).thenReturn("Beta");
        when(reqDefB.getUnit()).thenReturn("ml");

        InstrumentMeasurement reqB = mock(InstrumentMeasurement.class);
        when(reqB.getMeasurementDefinition()).thenReturn(reqDefB);
        when(reqB.getDisplayOrder()).thenReturn(2);

        when(instrumentMeasurementRepository.findRequiredByInstrumentId(10L))
                .thenReturn(List.of(reqA, reqB));

        // Act
        InstrumentRunDetails details = service.getRunById(runId);

        // Assert: core fields
        assertNotNull(details);
        assertEquals(runId, details.id());
        assertEquals("INST-1", details.instrumentCode());
        assertEquals("Instrument 1", details.instrumentName());
        assertEquals(createdBy, details.createdByClientId());
        assertEquals("ext-abc", details.externalReference());
        assertEquals(InstrumentRunStatus.CREATED, details.status());

        // Assert: required measurements mapped in order, and submitted value injected for A only
        List<RequiredMeasurement> req = details.requiredMeasurements();
        assertEquals(2, req.size());

        assertEquals("A", req.get(0).code());
        assertEquals("Alpha", req.get(0).name());
        assertEquals("mg", req.get(0).unit());
        assertEquals(1, req.get(0).displayOrder());
        assertEquals(new BigDecimal("1.23"), req.get(0).submittedValue());

        assertEquals("B", req.get(1).code());
        assertEquals("Beta", req.get(1).name());
        assertEquals("ml", req.get(1).unit());
        assertEquals(2, req.get(1).displayOrder());
        assertNull(req.get(1).submittedValue());

        // Assert: repository calls
        verify(instrumentRunRepository).findById(runId);
        verify(measurementRepository).findByInstrumentRunId(runId);
        verify(instrumentMeasurementRepository).findRequiredByInstrumentId(10L);

        // Assert: no unrelated deps touched
        verifyNoInteractions(instrumentRunEventRepository, processingService, measurementDefinitionRepository, instrumentRepository);
    }

    @Test
    void getRunById_runNotFound_throwsIllegalArgumentException_andDoesNotQueryMeasurementsOrRequired() {
        // Arrange
        Long runId = 999L;
        when(instrumentRunRepository.findById(runId)).thenReturn(Optional.empty());

        // Act + Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.getRunById(runId)
        );
        assertTrue(ex.getMessage().contains("Instrument run not found: " + runId));

        verify(instrumentRunRepository).findById(runId);
        verifyNoInteractions(measurementRepository, instrumentMeasurementRepository);
    }

    @Test
    void getRunById_noSubmittedMeasurements_requiredMeasurementsHaveNullSubmittedValue() {
        // Arrange
        Long runId = 123L;

        Instrument instrument = mock(Instrument.class);
        when(instrument.getId()).thenReturn(10L);
        when(instrument.getCode()).thenReturn("INST-1");
        when(instrument.getName()).thenReturn("Instrument 1");

        InstrumentRun run = mock(InstrumentRun.class);
        when(run.getId()).thenReturn(runId);
        when(run.getInstrument()).thenReturn(instrument);

        when(instrumentRunRepository.findById(runId)).thenReturn(Optional.of(run));
        when(measurementRepository.findByInstrumentRunId(runId)).thenReturn(List.of());

        MeasurementDefinition reqDefA = mock(MeasurementDefinition.class);
        when(reqDefA.getCode()).thenReturn("A");
        when(reqDefA.getName()).thenReturn("Alpha");
        when(reqDefA.getUnit()).thenReturn("mg");

        InstrumentMeasurement reqA = mock(InstrumentMeasurement.class);
        when(reqA.getMeasurementDefinition()).thenReturn(reqDefA);
        when(reqA.getDisplayOrder()).thenReturn(1);

        when(instrumentMeasurementRepository.findRequiredByInstrumentId(10L))
                .thenReturn(List.of(reqA));

        // Act
        InstrumentRunDetails details = service.getRunById(runId);

        // Assert
        assertNotNull(details);
        assertEquals(1, details.requiredMeasurements().size());
        assertNull(details.requiredMeasurements().get(0).submittedValue());

        verify(instrumentRunRepository).findById(runId);
        verify(measurementRepository).findByInstrumentRunId(runId);
        verify(instrumentMeasurementRepository).findRequiredByInstrumentId(10L);
    }
}

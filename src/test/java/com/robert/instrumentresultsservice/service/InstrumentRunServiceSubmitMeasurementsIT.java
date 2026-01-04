package com.robert.instrumentresultsservice.service;

import com.robert.instrumentresultsservice.TestcontainersConfiguration;
import com.robert.instrumentresultsservice.domain.*;
import com.robert.instrumentresultsservice.repository.*;
import com.robert.instrumentresultsservice.service.result.InstrumentRunCreated;
import com.robert.instrumentresultsservice.service.result.InstrumentRunDetails;
import com.robert.instrumentresultsservice.service.result.RequiredMeasurement;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class InstrumentRunServiceSubmitMeasurementsIT {

    @Autowired InstrumentRunService instrumentRunService;

    @Autowired InstrumentRepository instrumentRepository;
    @Autowired InstrumentRunRepository instrumentRunRepository;

    @Autowired MeasurementDefinitionRepository measurementDefinitionRepository;
    @Autowired MeasurementRepository measurementRepository;

    @Autowired InstrumentMeasurementRepository instrumentMeasurementRepository;

    @Test
    @Transactional
    void submitMeasurements_happyPath_persistsMeasurements_updatesRun_andGetRunByIdReflectsSubmittedValues() {
        // Arrange: instrument
        Instrument instrument = new Instrument();
        instrument.setCode("CBC-1000");
        instrument.setName("CBC Analyzer");
        instrument = instrumentRepository.save(instrument);

        // Arrange: definitions
        MeasurementDefinition defA = new MeasurementDefinition();
        defA.setCode("A");
        defA.setName("Alpha");
        defA.setUnit("mg");
        defA = measurementDefinitionRepository.save(defA);

        MeasurementDefinition defB = new MeasurementDefinition();
        defB.setCode("B");
        defB.setName("Beta");
        defB.setUnit("ml");
        defB = measurementDefinitionRepository.save(defB);

        // Arrange: required measurement links (ordered)
        InstrumentMeasurement imA = new InstrumentMeasurement();
        imA.setInstrument(instrument);
        imA.setMeasurementDefinition(defA);
        imA.setDisplayOrder(1);

        InstrumentMeasurement imB = new InstrumentMeasurement();
        imB.setInstrument(instrument);
        imB.setMeasurementDefinition(defB);
        imB.setDisplayOrder(2);

        instrumentMeasurementRepository.saveAll(List.of(imA, imB));

        // Create run
        UUID clientId = UUID.randomUUID();
        InstrumentRunCreated created = instrumentRunService.createRun("CBC-1000", clientId, "EXT-1");
        Long runId = created.runId();

        // Act: submit
        UUID submittedBy = UUID.randomUUID();
        Map<String, BigDecimal> measurements = new LinkedHashMap<>();
        measurements.put("A", new BigDecimal("1.23"));
        measurements.put("B", new BigDecimal("4.56"));

        InstrumentRunDetails details =
                instrumentRunService.submitMeasurements(runId, measurements, submittedBy);

        // Assert: run updated
        InstrumentRun run = instrumentRunRepository.findById(runId)
                .orElseThrow(() -> new AssertionError("Expected run to exist"));

        assertEquals(InstrumentRunStatus.MEASUREMENTS_SUBMITTED, run.getStatus());
        assertNotNull(run.getMeasurementsSubmittedAt());
        assertEquals(submittedBy, run.getMeasurementsSubmittedByClientId());

        // Assert: measurement rows exist
        assertEquals(2, measurementRepository.findByInstrumentRunId(runId).size());

        // Assert: details shows required measurements with submitted values
        assertNotNull(details);
        assertEquals(runId, details.id());
        assertEquals("CBC-1000", details.instrumentCode());
        assertEquals(InstrumentRunStatus.MEASUREMENTS_SUBMITTED, details.status());

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
        assertEquals(new BigDecimal("4.56"), req.get(1).submittedValue());
    }

    @Test
    @Transactional
    void submitMeasurements_unknownCode_throws_andDoesNotPersistMeasurements_orUpdateRun() {
        // Arrange: instrument
        Instrument instrument = new Instrument();
        instrument.setCode("CBC-1000");
        instrument.setName("CBC Analyzer");
        instrument = instrumentRepository.save(instrument);

        // Arrange: definition A only, required
        MeasurementDefinition defA = new MeasurementDefinition();
        defA.setCode("A");
        defA.setName("Alpha");
        defA.setUnit("mg");
        defA = measurementDefinitionRepository.save(defA);

        InstrumentMeasurement imA = new InstrumentMeasurement();
        imA.setInstrument(instrument);
        imA.setMeasurementDefinition(defA);
        imA.setDisplayOrder(1);
        instrumentMeasurementRepository.save(imA);

        // Create run
        InstrumentRunCreated created =
                instrumentRunService.createRun("CBC-1000", UUID.randomUUID(), "EXT-2");
        Long runId = created.runId();

        // Act + Assert
        Map<String, BigDecimal> measurements = new LinkedHashMap<>();
        measurements.put("A", new BigDecimal("1.23"));
        measurements.put("UNKNOWN", new BigDecimal("9.99"));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> instrumentRunService.submitMeasurements(runId, measurements, UUID.randomUUID())
        );
        assertTrue(ex.getMessage().contains("Unknown measurement code: UNKNOWN"));

        // Because service validates first: no measurements saved
        assertTrue(measurementRepository.findByInstrumentRunId(runId).isEmpty());

        // Run should remain CREATED (no update)
        InstrumentRun run = instrumentRunRepository.findById(runId)
                .orElseThrow(() -> new AssertionError("Expected run to exist"));
        assertEquals(InstrumentRunStatus.CREATED, run.getStatus());
        assertNull(run.getMeasurementsSubmittedAt());
        assertNull(run.getMeasurementsSubmittedByClientId());
    }
}

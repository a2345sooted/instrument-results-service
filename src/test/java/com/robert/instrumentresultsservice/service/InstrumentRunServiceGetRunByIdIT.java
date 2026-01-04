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
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class InstrumentRunServiceGetRunByIdIT {

    @Autowired InstrumentRunService instrumentRunService;

    @Autowired InstrumentRepository instrumentRepository;
    @Autowired MeasurementDefinitionRepository measurementDefinitionRepository;
    @Autowired InstrumentMeasurementRepository instrumentMeasurementRepository;

    @Autowired InstrumentRunRepository instrumentRunRepository;
    @Autowired MeasurementRepository measurementRepository;

    @Test
    @Transactional
    void getRunById_returnsRequiredMeasurementsInOrder_andInjectsSubmittedValues() {
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

        // Arrange: required links (ordered by displayOrder)
        InstrumentMeasurement imA = new InstrumentMeasurement();
        imA.setInstrument(instrument);
        imA.setMeasurementDefinition(defA);
        imA.setDisplayOrder(1);

        InstrumentMeasurement imB = new InstrumentMeasurement();
        imB.setInstrument(instrument);
        imB.setMeasurementDefinition(defB);
        imB.setDisplayOrder(2);

        instrumentMeasurementRepository.saveAll(List.of(imA, imB));

        // Arrange: run
        UUID createdBy = UUID.randomUUID();
        InstrumentRunCreated created = instrumentRunService.createRun("CBC-1000", createdBy, "EXT-GET-1");
        Long runId = created.runId();

        InstrumentRun run = instrumentRunRepository.findById(runId)
                .orElseThrow(() -> new AssertionError("Expected run to exist"));

        // Arrange: persist ONLY one submitted measurement (A)
        Measurement mA = new Measurement();
        mA.setInstrumentRun(run);
        mA.setMeasurementDefinition(defA);
        mA.setValue(new BigDecimal("1.23"));
        measurementRepository.save(mA);

        // Act
        InstrumentRunDetails details = instrumentRunService.getRunById(runId);

        // Assert: core fields
        assertNotNull(details);
        assertEquals(runId, details.id());
        assertEquals("CBC-1000", details.instrumentCode());
        assertEquals("CBC Analyzer", details.instrumentName());
        assertEquals(createdBy, details.createdByClientId());
        assertEquals("EXT-GET-1", details.externalReference());
        assertEquals(InstrumentRunStatus.CREATED, details.status());

        // Assert: required measurements list
        List<RequiredMeasurement> req = details.requiredMeasurements();
        assertEquals(2, req.size());

        // order should match displayOrder from repo query
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
    }

    @Test
    @Transactional
    void getRunById_runNotFound_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> instrumentRunService.getRunById(999_999L)
        );
        assertTrue(ex.getMessage().contains("Instrument run not found"));
    }
}
